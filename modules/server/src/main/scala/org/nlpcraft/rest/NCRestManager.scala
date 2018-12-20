/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.rest

import akka.actor.ActorSystem
import akka.actor.Status.Failure
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, _}
import akka.stream.ActorMaterializer
import org.nlpcraft.apicodes.NCApiStatusCode._
import org.nlpcraft.user.NCUserManager
import org.nlpcraft.{NCE, NCLifecycle}
import org.nlpcraft.NCConfigurable
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContextExecutor, Future}
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.ignite._

object NCRestManager extends NCLifecycle("REST manager") with NCIgniteNlpCraft {
    // Akka intestines.
    private implicit val SYSTEM: ActorSystem = ActorSystem()
    private implicit val MATERIALIZER: ActorMaterializer = ActorMaterializer()
    private implicit val CTX: ExecutionContextExecutor = SYSTEM.dispatcher

    private val API = "api" / "v1"

    private var bindFut: Future[Http.ServerBinding] = _

    private object Config extends NCConfigurable {
        var host: String = hocon.getString("rest.host")
        var port: Int = hocon.getInt("rest.port")

        override def check(): Unit = {
            require(port > 0 && port < 65535, s"port ($port) must be > 0 and < 65535")
        }
    }

    Config.check()
    
    /*
     * General control exception.
     * Note that these classes must be public because scala 2.11 internal errors (compilations problems).
     */
    case class AuthFailure() extends NCE("Authentication failed.")
    case class AdminRequired() extends NCE("Admin privileges required.")
    
    private implicit def handleErrors: ExceptionHandler =
        ExceptionHandler {
            case e : AuthFailure ⇒ complete(Unauthorized, e.getLocalizedMessage)
            case e : AdminRequired ⇒ complete(Forbidden, e.getLocalizedMessage)
            case e: Throwable ⇒
                val errMsg = e.getLocalizedMessage
                
                logger.error(s"REST error (${e.getClass.getSimpleName}) => $errMsg")
                
                complete(InternalServerError, errMsg)
        }
    
    /**
      *
      * @param acsTkn Access token to check.
      */
    @throws[NCE]
    private def authenticate(acsTkn: String): Unit = {
        if (!NCUserManager.checkAccessToken(acsTkn))
            throw AuthFailure()
    }
    
    /**
      *
      * @param acsTkn Access token to check.
      */
    @throws[NCE]
    private def authenticateAsAdmin(acsTkn: String): Unit =
        NCUserManager.getUserIdForAccessToken(acsTkn) match {
            case None ⇒ throw AuthFailure()
            case Some(usrId) ⇒ NCPsql.sql {
                NCDbManager.getUser(usrId) match {
                    case None ⇒ throw AuthFailure()
                    case Some(usr) ⇒ if (!usr.isAdmin) throw AdminRequired()
                }
            }
        }
    
    /**
      * 
      * @param acsTkn Access token.
      * @return
      */
    @throws[NCE]
    private def getUserId(acsTkn: String): Long =
        NCUserManager.getUserIdForAccessToken(acsTkn).getOrElse { throw AuthFailure() }

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        val routes: Route = {
            post {
                path(API / "ask") {
                    throw AuthFailure()
                } ~
                path(API / "reject") {
                    throw AuthFailure()
                } ~
                path(API / "cancel") {
                    throw AuthFailure()
                } ~
                path(API / "cancel" / "all") {
                    throw AuthFailure()
                } ~
                path(API / "curate") {
                    throw AuthFailure()
                } ~
                path(API / "talkback") {
                    throw AuthFailure()
                } ~
                path(API / "check") {
                    throw AuthFailure()
                } ~
                path(API / "user" / "add") {
                    case class Req(
                        // Current user.
                        accessToken: String,
                        
                        // New user.
                        email: String,
                        passwd: String,
                        firstName: String,
                        lastName: String,
                        avatarUrl: String,
                        isAdmin: Boolean
                    )
                    case class Res(
                        status: String,
                        userId: Long
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat7(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticateAsAdmin(req.accessToken)
                        
                        val newUsrId = NCUserManager.addUser(
                            getUserId(req.accessToken),
                            req.email,
                            req.passwd,
                            req.firstName,
                            req.lastName,
                            req.avatarUrl,
                            req.isAdmin
                        )
                        
                        complete {
                            Res(API_OK, newUsrId)
                        }
                    }
                } ~
                path(API / "user" / "passwd" / "reset") {
                    case class Req(
                        // Current user.
                        accessToken: String,
        
                        // New user.
                        usrId: Long,
                        newPasswd: String
                    )
                    case class Res(
                        status: String
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat3(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticateAsAdmin(req.accessToken)
        
                        NCUserManager.resetPassword(
                            req.usrId,
                            req.newPasswd
                        )
        
                        complete {
                            Res(API_OK)
                        }
                    }
                } ~
                path(API / "user" / "delete") {
                    case class Req(
                        accessToken: String,
                        userId: Long
                    )
                    case class Res(
                        status: String
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticateAsAdmin(req.accessToken)
    
                        NCUserManager.deleteUser(getUserId(req.accessToken))
    
                        complete {
                            Res(API_OK)
                        }
                    }
                } ~
                path(API / "user" / "update") {
                    case class Req(
                        // Current user.
                        accessToken: String,
        
                        // Update user.
                        userId: Long,
                        passwd: String,
                        firstName: String,
                        lastName: String,
                        avatarUrl: String,
                        isAdmin: Boolean
                    )
                    case class Res(
                        status: String
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat7(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticateAsAdmin(req.accessToken)
    
                        NCUserManager.updateUser(
                            req.userId,
                            req.firstName,
                            req.lastName,
                            req.avatarUrl,
                            req.isAdmin
                        )
        
                        complete {
                            Res(API_OK)
                        }
                    }
                } ~
                path(API / "user" / "signup") {
                    case class Req(
                        email: String,
                        passwd: String,
                        firstName: String,
                        lastName: String,
                        avatarUrl: String
                    )
                    case class Res(
                        status: String
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat5(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)
                    
                    // NOTE: no authentication requires on signup.
    
                    entity(as[Req]) { req ⇒
                        NCUserManager.signup(
                            req.email,
                            req.passwd,
                            req.firstName,
                            req.lastName,
                            req.avatarUrl
                        )
    
                        complete {
                            Res(API_OK)
                        }
                    }
                } ~
                path(API / "user" / "signout") {
                    case class Req(
                        accessToken: String
                    )
                    case class Res(
                        status: String
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat1(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticate(req.accessToken)
    
                        NCUserManager.signout(req.accessToken)
                        
                        complete {
                            Res(API_OK)
                        }
                    }
                } ~
                path(API / "user" / "signin") {
                    case class Req(
                        email: String,
                        passwd: String
                    )
                    case class Res(
                        status: String,
                        accessToken: String
                    )

                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)
    
                    // NOTE: no authentication requires on signin.
    
                    entity(as[Req]) { req ⇒
                        NCUserManager.signin(
                            req.email,
                            req.passwd
                        ) match {
                            case None ⇒ throw AuthFailure() // Email is unknown (user hasn't signed up).
                            case Some(acsTkn) ⇒ complete {
                                Res(API_OK, acsTkn)
                            }
                        }
                    }
                } ~
                path(API / "user" / "all") {
                    throw AuthFailure()
                } ~
                path(API / "ds" / "add") {
                    throw AuthFailure()
                } ~
                path(API / "ds" / "update") {
                    throw AuthFailure()
                } ~
                path(API / "ds" / "all") {
                    throw AuthFailure()
                } ~
                path(API / "ds" / "delete") {
                    throw AuthFailure()
                } ~
                path(API / "probe" / "stop") {
                    throw AuthFailure()
                } ~
                path(API / "probe" / "restart") {
                    throw AuthFailure()
                } ~
                path(API / "probe" / "all") {
                    throw AuthFailure()
                }
            }
        }

        bindFut = Http().bindAndHandle(routes, Config.host, Config.port)
        
        val url = s"${Config.host}:${Config.port}"
        
        bindFut.onFailure {
            case _ ⇒
                logger.info(
                    s"REST server failed to start on '$url'. " +
                    s"Use 'NLPCRAFT_CONFIG_FILE' system property to provide custom configuration file with correct REST host and port."
                )
        }
    
        bindFut.onSuccess {
            case _ ⇒ logger.info(s"REST server is listening on '$url'.")
        }

        super.start()
    }

    /**
      * Stops this component.
      */
    override def stop(): Unit = {
        if (bindFut != null)
            bindFut.flatMap(_.unbind()).onComplete(_ ⇒ SYSTEM.terminate())

        super.stop()
    }
}
