/*
 * “Commons Clause” License, https://commonsclause.com/
 *
 * The Software is provided to you by the Licensor under the License,
 * as defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software:    NlpCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
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
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, _}
import akka.stream.ActorMaterializer
import org.nlpcraft.apicodes.NCApiStatusCode._
import org.nlpcraft.user.NCUserManager
import org.nlpcraft.{NCConfigurable, NCE, NCException, NCLifecycle}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContextExecutor, Future}
import org.nlpcraft.ds.NCDsManager
import org.nlpcraft.ignite._
import org.nlpcraft.notification.NCNotificationManager
import org.nlpcraft.query.NCQueryManager

object NCRestManager extends NCLifecycle("REST manager") with NCIgniteNlpCraft {
    // Akka intestines.
    private implicit val SYSTEM: ActorSystem = ActorSystem()
    private implicit val MATERIALIZER: ActorMaterializer = ActorMaterializer()
    private implicit val CTX: ExecutionContextExecutor = SYSTEM.dispatcher
    
    // Current REST API version (simple increment number), not a semver based.
    private final val API_VER = 1

    private val API = "api" / s"v$API_VER"

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
    case class AccessTokenFailure(acsTkn: String) extends NCE(s"Unknown access token: $acsTkn")
    case class SignInFailure(email: String) extends NCE(s"Invalid user credentials for: $email")
    case class AdminRequired(email: String) extends NCE(s"Admin privileges required for: $email")
    case class NotImplemented() extends NCE("Not implemented.")
    
    private implicit def handleErrors: ExceptionHandler =
        ExceptionHandler {
            case e: AccessTokenFailure ⇒
                val errMsg = e.getLocalizedMessage
    
                NCNotificationManager.addEvent("NC_UNKNOWN_ACCESS_TOKEN",
                    "errMsg" → errMsg,
                    "acsTok" → e.acsTkn
                )
                
                complete(StatusCodes.Unauthorized, errMsg)
                
            case e: SignInFailure ⇒
                val errMsg = e.getLocalizedMessage
    
                NCNotificationManager.addEvent("NC_SIGNIN_FAILURE",
                    "errMsg" → errMsg,
                    "email" → e.email
                )

                complete(StatusCodes.Unauthorized, errMsg)
                
            case e: NotImplemented ⇒
                val errMsg = e.getLocalizedMessage
    
                NCNotificationManager.addEvent("NC_NOT_IMPLEMENTED")

                complete(StatusCodes.NotImplemented, errMsg)
                
            case e: AdminRequired ⇒
                val errMsg = e.getLocalizedMessage
    
                NCNotificationManager.addEvent("NC_ADMIN_REQUIRED",
                    "errMsg" → errMsg,
                    "email" → e.email
                )

                complete(StatusCodes.Forbidden, errMsg)
            
            // General exception.
            case e: NCException ⇒
                val errMsg = e.getLocalizedMessage
                
                NCNotificationManager.addEvent("NC_ERROR", "errMsg" → errMsg)
                
                complete(StatusCodes.BadRequest, errMsg)
            
            // Unexpected errors.
            case e: Throwable ⇒
                val errMsg = e.getLocalizedMessage
    
                NCNotificationManager.addEvent("NC_UNEXPECTED_ERROR",
                    "exception" → e.getClass.getSimpleName,
                    "errMsg" → errMsg
                )
                
                complete(InternalServerError, errMsg)
        }
    
    /**
      *
      * @param acsTkn Access token to check.
      */
    @throws[NCE]
    private def authenticate(acsTkn: String): Unit = {
        if (!NCUserManager.checkAccessToken(acsTkn))
            throw AccessTokenFailure(acsTkn)
    }
    
    /**
      *
      * @param acsTkn Access token to check.
      */
    @throws[NCE]
    private def authenticateAsAdmin(acsTkn: String): Unit =
        NCUserManager.getUserForAccessToken(acsTkn) match {
            case None ⇒ throw AccessTokenFailure(acsTkn)
            case Some(usr) ⇒ if (!usr.isAdmin) throw AdminRequired(usr.email)
        }
    
    /**
      *
      * @param acsTkn Access token.
      * @return
      */
    @throws[NCE]
    private def getUserId(acsTkn: String): Long =
        NCUserManager.getUserIdForAccessToken(acsTkn).getOrElse { throw AccessTokenFailure(acsTkn) }

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        val routes: Route = {
            post {
                path(API / "ask") {
                    case class Req(
                        accessToken: String,
                        txt: String,
                        dsId: Long,
                        isTest: Option[Boolean]
                    )
                    case class Res(
                        status: String,
                        srvReqId: String
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat4(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticate(req.accessToken)
        
                        val newSrvReqId = NCQueryManager.ask(
                            getUserId(req.accessToken),
                            req.txt,
                            req.dsId,
                            req.isTest.getOrElse(false)
                        )
        
                        complete {
                            Res(API_OK, newSrvReqId)
                        }
                    }
                } ~
                path(API / "cancel") {
                    case class Req(
                        accessToken: String,
                        srvReqIds: List[String]
                    )
                    case class Res(
                        status: String
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticate(req.accessToken)
        
                        NCQueryManager.cancel(
                            req.srvReqIds
                        )
        
                        complete {
                            Res(API_OK)
                        }
                    }
                } ~
                path(API / "check") {
                    case class Req(
                        accessToken: String
                    )
                    case class QueryState(
                        srvReqId: String,
                        usrId: Long,
                        dsId: Long,
                        modelId: Long,
                        probeId: Option[Long],
                        status: String,
                        resType: Option[String],
                        resBody: Option[String],
                        error: Option[String],
                        createTstamp: Long,
                        updateTstamp: Long
                    )
                    case class Res(
                        status: String,
                        users: List[QueryState]
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat1(Req)
                    implicit val usrFmt: RootJsonFormat[QueryState] = jsonFormat11(QueryState)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticate(req.accessToken)
                        
                        // TODO
        
                        complete {
                            Res(API_OK, Nil)
                        }
                    }
                } ~
                path(API / "clear" / "conversation") {
                    case class Req(
                        accessToken: String,
                        dsId: Long
                    )
                    case class Res(
                        status: String
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticateAsAdmin(req.accessToken)
        
                        NCQueryManager.clearConversation(
                            getUserId(req.accessToken),
                            req.dsId
                        )
        
                        complete {
                            Res(API_OK)
                        }
                    }
                } ~
                /**/path(API / "user" / "add") {
                    case class Req(
                        // Caller.
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
                /**/path(API / "user" / "passwd" / "reset") {
                    case class Req(
                        // Caller.
                        accessToken: String, // Administrator.
        
                        usrId: Long, // ID of the user to reset password for.
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
                /**/path(API / "user" / "delete") {
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
                /**/path(API / "user" / "update") {
                    case class Req(
                        // Caller.
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
                /**/path(API / "user" / "signup") {
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
                /**/path(API / "user" / "signout") {
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
                /**/path(API / "user" / "signin") {
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
                            case None ⇒ throw SignInFailure(req.email) // Email is unknown (user hasn't signed up).
                            case Some(acsTkn) ⇒ complete {
                                Res(API_OK, acsTkn)
                            }
                        }
                    }
                } ~
                /**/path(API / "user" / "all") {
                    case class Req(
                        // Caller.
                        accessToken: String
                    )
                    case class ResUser(
                        id: Long,
                        email: String,
                        firstName: String,
                        lastName: String,
                        avatarUrl: String,
                        lastDsId: Long,
                        isAdmin: Boolean
                    )
                    case class Res(
                        status: String,
                        users: List[ResUser]
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat1(Req)
                    implicit val usrFmt: RootJsonFormat[ResUser] = jsonFormat7(ResUser)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticateAsAdmin(req.accessToken)
        
                        val usrLst = NCUserManager.getAllUsers.map(mdo ⇒ ResUser(
                            mdo.id,
                            mdo.email,
                            mdo.firstName,
                            mdo.lastName,
                            mdo.avatarUrl,
                            mdo.lastDsId,
                            mdo.isAdmin
                        ))
        
                        complete {
                            Res(API_OK, usrLst)
                        }
                    }
                } ~
                /**/path(API / "ds" / "add") {
                    case class Req(
                        // Caller.
                        accessToken: String,
        
                        // Data source.
                        name: String,
                        shortDesc: String,
                        mdlId: String,
                        mdlName: String,
                        mdlVer: String,
                        mdlCfg: String
                    )
                    case class Res(
                        status: String,
                        dsId: Long
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat7(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticateAsAdmin(req.accessToken)
        
                        val newDsId = NCDsManager.addDataSource(
                            req.name,
                            req.shortDesc,
                            req.mdlId,
                            req.mdlName,
                            req.mdlVer,
                            req.mdlCfg
                        )
        
                        complete {
                            Res(API_OK, newDsId)
                        }
                    }
                } ~
                /**/path(API / "ds" / "update") {
                    case class Req(
                        // Caller.
                        accessToken: String,
        
                        // Update data source.
                        dsId: Long,
                        name: String,
                        shortDesc: String
                    )
                    case class Res(
                        status: String
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat4(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticateAsAdmin(req.accessToken)
        
                        NCDsManager.updateDataSource(
                            req.dsId,
                            req.name,
                            req.shortDesc
                        )
        
                        complete {
                            Res(API_OK)
                        }
                    }
                } ~
                /**/path(API / "ds" / "all") {
                    case class Req(
                        // Caller.
                        accessToken: String
                    )
                    case class ResDs(
                        id: Long,
                        name: String,
                        shortDesc: String,
                        mdlId: String,
                        mdlName: String,
                        mdlVer: String,
                        mdlCfg: String
                    )
                    case class Res(
                        status: String,
                        dataSources: List[ResDs]
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat1(Req)
                    implicit val usrFmt: RootJsonFormat[ResDs] = jsonFormat7(ResDs)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticateAsAdmin(req.accessToken)
        
                        val dsLst = NCDsManager.getAllDataSources.map(mdo ⇒ ResDs(
                            mdo.id,
                            mdo.name,
                            mdo.shortDesc,
                            mdo.modelId,
                            mdo.modelName,
                            mdo.modelVersion,
                            mdo.modelConfig
                        ))
        
                        complete {
                            Res(API_OK, dsLst)
                        }
                    }
                } ~
                /**/path(API / "ds" / "delete") {
                    case class Req(
                        accessToken: String,
                        dsId: Long
                    )
                    case class Res(
                        status: String
                    )
    
                    implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                    implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)
    
                    entity(as[Req]) { req ⇒
                        authenticateAsAdmin(req.accessToken)
        
                        NCDsManager.deleteDataSource(req.dsId)
        
                        complete {
                            Res(API_OK)
                        }
                    }
                } ~
                path(API / "probe" / "stop") {
                    throw NotImplemented()
                } ~
                path(API / "probe" / "restart") {
                    throw NotImplemented()
                } ~
                path(API / "probe" / "all") {
                    throw NotImplemented()
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
