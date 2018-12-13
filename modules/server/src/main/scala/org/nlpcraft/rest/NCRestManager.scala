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
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, _}
import akka.stream.ActorMaterializer
import org.nlpcraft.apicodes.NCApiStatusCode._
import org.nlpcraft.login.NCLoginManager
import org.nlpcraft.{NCE, NCLifecycle}
import org.nlpcraft.NCConfigurable
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContextExecutor, Future}

object NCRestManager extends NCLifecycle("REST manager") {
    // Akka intestines.
    private implicit val SYSTEM: ActorSystem = ActorSystem()
    private implicit val MATERIALIZER: ActorMaterializer = ActorMaterializer()
    private implicit val CTX: ExecutionContextExecutor = SYSTEM.dispatcher

    private val API = "api" / "v1"

    private var bindFut: Future[Http.ServerBinding] = _

    private object Config extends NCConfigurable {
        var server: String = hocon.getString("rest.host")
        var port: Int = hocon.getInt("rest.port")

        override def check(): Unit = {
            require(port > 0 && port < 65535)
        }
    }

    Config.check()

    case class AuthFailure() extends NCE("Authentication failed.")

    private implicit def handleErrors: ExceptionHandler =
        ExceptionHandler {
            case _ : AuthFailure ⇒ complete(NotAcceptable, "Authentication error")
            case e: Throwable ⇒ complete(InternalServerError, e.getMessage)
        }

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        val routes: Route = {
            post {
                path(API / "signin") {
                    case class Req(
                        probeToken: String,
                        email: String
                    )
                    case class Res(
                        status: String,
                        accessToken: String
                    )

                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)

                    entity(as[Req]) { req ⇒
                        headerValueByName("User-Agent") { userAgent ⇒
                            NCLoginManager.getAdminAccessToken(req.probeToken, req.email, userAgent) match {
                                case Some(tkn) ⇒ complete(Res(API_OK.toString, tkn))
                                case None ⇒ throw AuthFailure()
                            }
                        }
                    }
                }
            }
        }

        bindFut = Http().bindAndHandle(routes, Config.server, Config.port)

        logger.info(s"REST server listens on ${Config.server}:${Config.port}")

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
