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
 * Software:    NLPCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.server.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, _}
import akka.stream.ActorMaterializer
import com.google.gson.Gson
import org.apache.commons.validator.routines.UrlValidator
import org.nlpcraft.common.{NCException, NCLifecycle, _}
import org.nlpcraft.server.NCConfigurable
import org.nlpcraft.server.apicodes.NCApiStatusCode._
import org.nlpcraft.server.ds.NCDsManager
import org.nlpcraft.server.mdo.NCUserMdo
import org.nlpcraft.server.notification.NCNotificationManager
import org.nlpcraft.server.probe.NCProbeManager
import org.nlpcraft.server.query.NCQueryManager
import org.nlpcraft.server.user.NCUserManager
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * REST manager.
  */
object NCRestManager extends NCLifecycle("REST manager") {
    // Akka intestines.
    private implicit val SYSTEM: ActorSystem = ActorSystem()
    private implicit val MATERIALIZER: ActorMaterializer = ActorMaterializer()
    private implicit val CTX: ExecutionContextExecutor = SYSTEM.dispatcher
    
    // Current REST API version (simple increment number), not a semver based.
    private final val API_VER = 1

    private final val GSON = new Gson()

    private val API = "api" / s"v$API_VER"

    private var bindFut: Future[Http.ServerBinding] = _

    private final val urlVal = new UrlValidator(Array("http", "https"), UrlValidator.ALLOW_LOCAL_URLS)

    private object Config extends NCConfigurable {
        final val prefix = "server.rest"
        
        val host: String = getString(s"$prefix.host")
        val port: Int = getInt(s"$prefix.port")

        override def check(): Unit = {
            if (!(port > 0 && port < 65535))
                abortError(s"Configuration property port '$prefix.port' must be > 0 and < 65535: $port")
            if (host == null)
                abortError(s"Configuration property port '$prefix.host' must be specified.")
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

    class ArgsException(msg: String) extends NCE(msg)
    case class OutOfRangeField(fn: String, max: Int)
        extends ArgsException(s"API field '$fn' value exceeded max length of $max.")
    case class InvalidField(fn: String) extends ArgsException(s"API invalid field '$fn'")
    case class EmptyField(fn: String, max: Int) extends ArgsException(s"API field '$fn' value cannot be empty.")
    case class XorFields(f1: String, f2: String)
        extends ArgsException(s"Only one API field must be defined: '$f1' or '$f2'")

    private implicit def handleErrors: ExceptionHandler =
        ExceptionHandler {
            case e: AccessTokenFailure ⇒
                val errMsg = e.getLocalizedMessage

                NCNotificationManager.addEvent("NC_UNKNOWN_ACCESS_TOKEN",
                    "errMsg" → errMsg
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

            case e: ArgsException ⇒
                val errMsg = e.getLocalizedMessage

                NCNotificationManager.addEvent("NC_INVALID_FIELD")

                complete(StatusCodes.BadRequest, errMsg)

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
                
                logger.error(s"Unexpected error: $errMsg", e)
                
                complete(StatusCodes.BadRequest, errMsg)
            
            // Unexpected errors.
            case e: Throwable ⇒
                val errMsg = e.getLocalizedMessage
    
                NCNotificationManager.addEvent("NC_UNEXPECTED_ERROR",
                    "exception" → e.getClass.getSimpleName,
                    "errMsg" → errMsg
                )
    
                logger.error(s"Unexpected system error: $errMsg", e)
                
                complete(InternalServerError, errMsg)
        }

    /**
      *
      * @param acsTkn Access token to check.
      * @param shouldBeAdmin Admin flag.
      */
    @throws[NCE]
    private def authenticate0(acsTkn: String, shouldBeAdmin: Boolean): NCUserMdo =
        NCUserManager.getUserForAccessToken(acsTkn) match {
            case None ⇒ throw AccessTokenFailure(acsTkn)
            case Some(usr) ⇒
                if (shouldBeAdmin && !usr.isAdmin)
                    throw AdminRequired(usr.email)

                usr
        }
    
    /**
      *
      * @param acsTkn Access token to check.
      */
    @throws[NCE]
    private def authenticate(acsTkn: String): NCUserMdo = authenticate0(acsTkn, false)

    /**
      *
      * @param acsTkn Access token to check.
      */
    @throws[NCE]
    private def authenticateAsAdmin(acsTkn: String): NCUserMdo = authenticate0(acsTkn, true)

    /**
      * Checks length of field value.
      *
      * @param name Field name.
      * @param v Field value.
      * @param maxLen Maximum length.
      * @param minLen Minimum length.
      */
    @throws[OutOfRangeField]
    private def checkLength(name: String, v: String, maxLen: Int, minLen: Int = 1): Unit =
        if (v.length > maxLen)
            throw OutOfRangeField(name, maxLen)
        else if (v.length < minLen)
            throw EmptyField(name, minLen)

    /**
      * Checks length of field value.
      *
      * @param name Field name.
      * @param v Field value.
      * @param maxLen Maximum length.
      * @param minLen Minimum length.
      */
    @throws[OutOfRangeField]
    private def checkLengthOpt(name: String, v: Option[String], maxLen: Int, minLen: Int = 1): Unit =
        if (v.isDefined)
            checkLength(name, v.get, maxLen, minLen)

    /**
      * Checks operation permissions and gets user ID.
      *
      * @param initiatorUsr Operation initiator.
      * @param usrIdOpt User ID. Optional.
      */
    @throws[AdminRequired]
    private def getUserId(initiatorUsr: NCUserMdo, usrIdOpt: Option[Long]): Long =
        usrIdOpt match {
            case Some(userId) ⇒
                if (!initiatorUsr.isAdmin)
                    throw AdminRequired(initiatorUsr.email)

                userId
            case None ⇒ initiatorUsr.id
        }

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        val routes: Route = post {
            /**/path(API / "ask") {
                case class Req(
                    acsTok: String,
                    txt: String,
                    dsId: Option[Long],
                    mdlId: Option[String]
                )
                case class Res(
                    status: String,
                    srvReqId: String
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat4(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)
                    checkLength("txt", req.txt, 1024)
                    checkLengthOpt("mdlId", req.mdlId, 32)

                    if (!(req.dsId.isDefined ^ req.mdlId.isDefined))
                        throw XorFields("dsId", "mdlId")

                    val userId = authenticate(req.acsTok).id

                    optionalHeaderValueByName("User-Agent") { userAgent ⇒
                        extractClientIP { remoteAddr ⇒
                            val tmpDsId =
                                req.mdlId match {
                                    case Some(mdlId) ⇒ Some(NCDsManager.addTempDataSource(mdlId))
                                    case None ⇒ None
                                }

                            try {
                                val newSrvReqId =
                                    NCQueryManager.ask(
                                        userId,
                                        req.txt,
                                        tmpDsId.getOrElse(req.dsId.get),
                                        userAgent,
                                        remoteAddr.toOption match {
                                            case Some(a) ⇒ Some(a.getHostAddress)
                                            case None ⇒ None
                                        }
                                    )

                                complete {
                                    Res(API_OK, newSrvReqId)
                                }
                            }
                            finally {
                                tmpDsId match {
                                    case Some(id) ⇒ NCDsManager.deleteDataSource(id)
                                    case None ⇒ // No-op.
                                }
                            }
                        }
                    }
                }
            } ~
            /**/path(API / "cancel") {
                case class Req(
                    acsTok: String,
                    srvReqIds: Option[Set[String]]
                )
                case class Res(
                    status: String
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)

                    val initiatorUsr = authenticate(req.acsTok)

                    if (!initiatorUsr.isAdmin) {
                        val states = req.srvReqIds match {
                            case Some(srvReqIds) ⇒ NCQueryManager.get(srvReqIds)
                            case None ⇒ NCQueryManager.get(initiatorUsr.id)

                        }
                        
                        if (states.exists(_.userId != initiatorUsr.id))
                            throw AdminRequired(initiatorUsr.email)
                    }

                    req.srvReqIds match {
                        case Some(srvReqIds) ⇒ NCQueryManager.cancel(srvReqIds)
                        case None ⇒ NCQueryManager.cancel(initiatorUsr.id)
                    }

                    complete {
                        Res(API_OK)
                    }
                }
            } ~
            /**/path(API / "check") {
                case class Req(
                    acsTok: String
                )
                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat1(Req)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)

                    val userId = authenticate(req.acsTok).id

                    val states =
                        NCQueryManager.check(userId).map(p ⇒
                            Map(
                                "srvReqId" → p.srvReqId,
                                "txt" → p.text,
                                "usrId" → p.userId,
                                "dsId" → p.dsId,
                                "mdlId" → p.modelId,
                                "probeId" → p.probeId.orNull,
                                "status" → p.status,
                                "resType" → p.resultType.orNull,
                                "resBody" → (
                                    if (p.resultBody.isDefined &&
                                        p.resultType.isDefined &&
                                        p.resultType.get == "json"
                                    )
                                        U.js2Map(p.resultBody.get)
                                    else
                                        p.resultBody.orNull
                                ),
                                "error" → p.error.orNull,
                                "errorCode" → p.errorCode.map(Integer.valueOf).orNull,
                                "createTstamp" → p.createTstamp.getTime,
                                "updateTstamp" → p.updateTstamp.getTime
                            ).filter(_._2 != null).asJava
                    )

                    // We have to use GSON (not spray) here to serialize `resBody` field.
                    val js = GSON.toJson(Map("status" → API_OK.toString, "states" → states.asJava).asJava)

                    complete(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, js)))
                }
            } ~
            /**/path(API / "clear" / "conversation") {
                case class Req(
                    acsTok: String,
                    dsId: Long
                )
                case class Res(
                    status: String
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)

                    val userId = authenticate(req.acsTok).id

                    NCProbeManager.clearConversation(userId, req.dsId)

                    complete {
                        Res(API_OK)
                    }
                }
            } ~
            /**/path(API / "user" / "add") {
                case class Req(
                    // Caller.
                    acsTok: String,

                    // New user.
                    email: String,
                    passwd: String,
                    firstName: String,
                    lastName: String,
                    avatarUrl: Option[String],
                    isAdmin: Boolean
                )
                case class Res(
                    status: String,
                    id: Long
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat7(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)
                    checkLength("email", req.email, 64)
                    checkLength("passwd", req.passwd, 64)
                    checkLength("firstName", req.firstName, 64)
                    checkLength("lastName", req.lastName, 64)
                    checkLengthOpt("avatarUrl", req.avatarUrl, 512000)

                    authenticateAsAdmin(req.acsTok)

                    val id = NCUserManager.addUser(
                        req.email,
                        req.passwd,
                        req.firstName,
                        req.lastName,
                        req.avatarUrl,
                        req.isAdmin
                    )

                    complete {
                        Res(API_OK, id)
                    }
                }
            } ~
            /**/path(API / "user" / "delete") {
                case class Req(
                    acsTok: String,
                    id: Option[Long]
                )
                case class Res(
                    status: String
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)

                    val initiatorUsr = authenticate(req.acsTok)
                    val usrId = getUserId(initiatorUsr, req.id)

                    NCUserManager.deleteUser(usrId)

                    complete {
                        Res(API_OK)
                    }
                }
            } ~
            /**/path(API / "user" / "update") {
                case class Req(
                    // Caller.
                    acsTok: String,

                    // Update user.
                    id: Option[Long],
                    firstName: String,
                    lastName: String,
                    avatarUrl: Option[String],
                    isAdmin: Boolean // TODO: remove
                )
                case class Res(
                    status: String
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat6(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)
                    checkLength("firstName", req.firstName, 64)
                    checkLength("lastName", req.lastName, 64)
                    checkLengthOpt("avatarUrl", req.avatarUrl, 512000)

                    val initiatorUsr = authenticate(req.acsTok)
                    val usrId = getUserId(initiatorUsr, req.id)

                    NCUserManager.updateUser(
                        usrId,
                        req.firstName,
                        req.lastName,
                        req.avatarUrl,
                        req.isAdmin // TODO: remove
                    )

                    complete {
                        Res(API_OK)
                    }
                }
            } ~
            /**/path(API / "passwd" / "reset") {
                case class Req(
                    // Caller.
                    acsTok: String,
                    id: Option[Long],
                    newPasswd: String
                )
                case class Res(
                    status: String
                )
        
                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat3(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)
        
                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)
                    checkLength("newPasswd", req.newPasswd, 64)
            
                    val initiatorUsr = authenticate(req.acsTok)
                    val usrId = getUserId(initiatorUsr, req.id)
            
                    NCUserManager.resetPassword(
                        usrId,
                        req.newPasswd
                    )
            
                    complete {
                        Res(API_OK)
                    }
                }
            } ~
            /**/path(API / "signout") {
                case class Req(
                    acsTok: String
                )
                case class Res(
                    status: String
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat1(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)

                    authenticate(req.acsTok)

                    NCUserManager.signout(req.acsTok)

                    complete {
                        Res(API_OK)
                    }
                }
            } ~
            /**/path(API / "signin") {
                case class Req(
                    email: String,
                    passwd: String
                )
                case class Res(
                    status: String,
                    acsTok: String
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)

                // NOTE: no authentication requires on signin.

                entity(as[Req]) { req ⇒
                    checkLength("email", req.email, 64)
                    checkLength("passwd", req.passwd, 64)

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
                    acsTok: String
                )
                case class ResUser(
                    id: Long,
                    email: String,
                    firstName: String,
                    lastName: String,
                    avatarUrl: Option[String],
                    lastDsId: Long,
                    isAdmin: Boolean
                )
                case class Res(
                    status: String,
                    users: Seq[ResUser]
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat1(Req)
                implicit val usrFmt: RootJsonFormat[ResUser] = jsonFormat7(ResUser)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)

                    authenticateAsAdmin(req.acsTok)

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
            /**/path(API / "endpoint" / "register") {
                case class Req(
                    acsTok: String,
                    endpoint: String
                )
                case class Res(
                    status: String
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)
                    checkLength("endpoint", req.endpoint, 2083)

                    if (!urlVal.isValid(req.endpoint))
                        throw InvalidField(req.endpoint)

                    val usrId = authenticate(req.acsTok).id

                    NCUserManager.registerEndpoint(usrId, req.endpoint)

                    complete {
                        Res(API_OK)
                    }
                }
            } ~
            /**/path(API / "endpoint" / "remove") {
                case class Req(
                    acsTok: String
                )
                case class Res(
                    status: String
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat1(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)

                    val usrId = authenticate(req.acsTok).id

                    NCUserManager.removeEndpoint(usrId)

                    complete {
                        Res(API_OK)
                    }
                }
            } ~
            /**/path(API / "ds" / "add") {
                case class Req(
                    // Caller.
                    acsTok: String,

                    // Data source.
                    name: String,
                    shortDesc: String,
                    mdlId: String,
                    mdlName: String,
                    mdlVer: String,
                    mdlCfg: Option[String]
                )
                case class Res(
                    status: String,
                    id: Long
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat7(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)
                    checkLength("name", req.name, 128)
                    checkLength("shortDesc", req.shortDesc, 128)
                    checkLength("mdlId", req.mdlId, 32)
                    checkLength("mdlName", req.mdlName, 64)
                    checkLength("mdlVer", req.mdlVer, 16)
                    checkLengthOpt("mdlCfg", req.mdlCfg, 512000)

                    authenticateAsAdmin(req.acsTok)

                    val id = NCDsManager.addDataSource(
                        req.name,
                        req.shortDesc,
                        req.mdlId,
                        req.mdlName,
                        req.mdlVer,
                        req.mdlCfg
                    )

                    complete {
                        Res(API_OK, id)
                    }
                }
            } ~
            /**/path(API / "ds" / "update") {
                case class Req(
                    // Caller.
                    acsTok: String,

                    // Update data source.
                    id: Long,
                    name: String,
                    shortDesc: String
                )
                case class Res(
                    status: String
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat4(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)

                entity(as[Req]) { req ⇒
                    authenticateAsAdmin(req.acsTok)

                    checkLength("acsTok", req.acsTok, 256)
                    checkLength("name", req.name, 128)
                    checkLength("shortDesc", req.shortDesc, 128)

                    NCDsManager.updateDataSource(
                        req.id,
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
                    acsTok: String
                )
                case class ResDs(
                    id: Long,
                    name: String,
                    shortDesc: String,
                    mdlId: String,
                    mdlName: String,
                    mdlVer: String,
                    mdlCfg: Option[String]
                )
                case class Res(
                    status: String,
                    dataSources: Seq[ResDs]
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat1(Req)
                implicit val usrFmt: RootJsonFormat[ResDs] = jsonFormat7(ResDs)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)

                    authenticateAsAdmin(req.acsTok)

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
                    acsTok: String,
                    id: Long
                )
                case class Res(
                    status: String
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat2(Req)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat1(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)

                    authenticateAsAdmin(req.acsTok)

                    NCDsManager.deleteDataSource(req.id)

                    complete {
                        Res(API_OK)
                    }
                }
            } ~
            /**/path(API / "probe" / "all") {
                case class Req(
                    acsTok: String
                )
                case class Model(
                    id: String,
                    name: String,
                    version: String
                )
                case class Probe(
                    probeToken: String,
                    probeId: String,
                    probeGuid: String,
                    probeApiVersion: String,
                    probeApiDate: String,
                    osVersion: String,
                    osName: String,
                    osArch: String,
                    startTstamp: Long,
                    tmzId: String,
                    tmzAbbr: String,
                    tmzName: String,
                    userName: String,
                    javaVersion: String,
                    javaVendor: String,
                    hostName: String,
                    hostAddr: String,
                    macAddr: String,
                    models: Set[Model]
                )
                case class Res(
                    status: String,
                    probes: Seq[Probe]
                )

                implicit val reqFmt: RootJsonFormat[Req] = jsonFormat1(Req)
                implicit val mdlFmt: RootJsonFormat[Model] = jsonFormat3(Model)
                implicit val probFmt: RootJsonFormat[Probe] = jsonFormat19(Probe)
                implicit val resFmt: RootJsonFormat[Res] = jsonFormat2(Res)

                entity(as[Req]) { req ⇒
                    checkLength("acsTok", req.acsTok, 256)

                    authenticateAsAdmin(req.acsTok)

                    val probeLst = NCProbeManager.getAllProbes.map(mdo ⇒ Probe(
                        mdo.probeToken,
                        mdo.probeId,
                        mdo.probeGuid,
                        mdo.probeApiVersion,
                        mdo.probeApiDate.toString,
                        mdo.osVersion,
                        mdo.osName,
                        mdo.osArch,
                        mdo.startTstamp.getTime,
                        mdo.tmzId,
                        mdo.tmzAbbr,
                        mdo.tmzName,
                        mdo.userName,
                        mdo.javaVersion,
                        mdo.javaVendor,
                        mdo.hostName,
                        mdo.hostAddr,
                        mdo.macAddr,
                        mdo.models.map(m ⇒ Model(
                            m.id,
                            m.name,
                            m.version
                        ))
                    ))

                    complete {
                        Res(API_OK, probeLst)
                    }
                }
            }
        }

        bindFut = Http().bindAndHandle(routes, Config.host, Config.port)
        
        val url = s"${Config.host}:${Config.port}"
        
        bindFut.onFailure {
            case _ ⇒
                logger.info(s"REST server failed to start on '$url'.")
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
