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
import org.nlpcraft2.NCConfigurable
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContextExecutor, Future}


object NCRestManager extends NCLifecycle("REST manager") with App {
    // Needed to run the route.
    private lazy implicit val SYSTEM: ActorSystem = ActorSystem()
    private lazy implicit val MATERIALIZER: ActorMaterializer = ActorMaterializer()
    // Needed for the future map/flatmap in the end and future in fetchItem and saveOrder
    private lazy implicit val CTX: ExecutionContextExecutor = SYSTEM.dispatcher

    private lazy val PUB = "pub" / "v1"

    private var bindFut: Future[Http.ServerBinding] = _

    private[rest] object Config extends NCConfigurable {
        var server: String = hocon.getString("rest.host")
        var port: Int = hocon.getInt("rest.port")

        override def check(): Unit = {
            println("server="+server)
            println("port="+port)

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
        val route: Route = {
            post {
                path(PUB / "signin") {
                    case class Signin(probeToken: String, email: String)
                    case class Response(status: String, accessToken: String)

                    implicit val i1: RootJsonFormat[Response] = jsonFormat2(Response)
                    implicit val i2: RootJsonFormat[Signin] = jsonFormat2(Signin)

                    entity(as[Signin]) { s ⇒
                        NCLoginManager.getAdminAccessToken(s.probeToken, s.email) match {
                            case Some(tkn) ⇒ complete(Response(PUB_API_OK.toString, tkn))
                            case None ⇒ throw AuthFailure()
                        }
                    }
                }
            }
        }

        bindFut = Http().bindAndHandle(route, Config.server, Config.port)

        logger.info(s"REST initialized [host=${Config.server}, port=${Config.port}]")

        super.start()
    }

    /**
      * Stops this component.
      */
    override def stop(): Unit = {
        super.stop()

        if (bindFut != null)
            bindFut.flatMap(_.unbind()).onComplete(_ ⇒ SYSTEM.terminate())
    }
}
