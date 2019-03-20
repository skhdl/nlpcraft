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

package org.nlpcraft.server.user

import java.util.{Timer, TimerTask}

import org.apache.commons.validator.routines.EmailValidator
import org.apache.ignite.{IgniteAtomicSequence, IgniteCache}
import org.nlpcraft.common.blowfish.NCBlowfishHasher
import org.nlpcraft.common.{NCLifecycle, _}
import org.nlpcraft.server.NCConfigurable
import org.nlpcraft.server.endpoints.NCEndpointManager
import org.nlpcraft.server.ignite.NCIgniteHelpers._
import org.nlpcraft.server.ignite.NCIgniteInstance
import org.nlpcraft.server.mdo.NCUserMdo
import org.nlpcraft.server.notification.NCNotificationManager
import org.nlpcraft.server.sql.{NCSql, NCSqlManager}
import org.nlpcraft.server.tx.NCTxManager

import scala.collection.JavaConverters._
import scala.util.control.Exception._

/**
  * User CRUD manager.
  */
object NCUserManager extends NCLifecycle("User manager") with NCIgniteInstance {
    // Static email validator.
    private final val EMAIL_VALIDATOR = EmailValidator.getInstance()

    // Caches.
    @volatile private var tokenSigninCache: IgniteCache[String, SigninSession] = _
    @volatile private var idSigninCache: IgniteCache[Long, String] = _

    @volatile private var usersSeq: IgniteAtomicSequence = _
    @volatile private var pswdSeq: IgniteAtomicSequence = _

    // Access token timeout scanner.
    @volatile private var scanner: Timer = _
    
    // Session holder.
    private case class SigninSession(
        acsToken: String,
        userId: Long,
        signinMs: Long,
        lastAccessMs: Long,
        endpoint: Option[String]
    )

    private object Config extends NCConfigurable {
        final val prefix = "server.user"
        
        val pwdPoolBlowup: Int = getInt(s"$prefix.pwdPoolBlowup")
        val timeoutScannerFreqMins: Int = getInt(s"$prefix.timeoutScannerFreqMins")
        val accessTokenExpireTimeoutMins: Int = getInt(s"$prefix.accessTokenExpireTimeoutMins")
    
        lazy val scannerMs: Int = timeoutScannerFreqMins * 60 * 1000
        lazy val expireMs: Int = accessTokenExpireTimeoutMins * 60 * 1000

        override def check(): Unit = {
            if (pwdPoolBlowup <= 1)
                abortError(s"Configuration parameter '$prefix.pwdPoolBlowup' must be > 1")
            if (timeoutScannerFreqMins <= 0)
                abortError(s"Configuration parameter '$prefix.timeoutScannerFreqMins' must be > 0")
            if (accessTokenExpireTimeoutMins <= 0)
                abortError(s"Configuration parameter '$prefix.accessTokenExpireTimeoutMins' must be > 0")
        }
    }

    Config.check()

    /**
      * Starts this manager.
      */
    override def start(): NCLifecycle = {
        ensureStopped()

        catching(wrapIE) {
            usersSeq = NCSql.mkSeq(ignite, "usersSeq", "nc_user", "id")
            pswdSeq = NCSql.mkSeq(ignite, "pswdSeq", "passwd_pool", "id")

            tokenSigninCache = ignite.cache[String, SigninSession]("user-token-signin-cache")
            idSigninCache = ignite.cache[Long, String]("user-id-signin-cache")

            require(tokenSigninCache != null)
            require(idSigninCache != null)
        }

        scanner = new Timer("timeout-scanner")

        scanner.scheduleAtFixedRate(
            new TimerTask() {
                def run() {
                    try {
                        val now = U.nowUtcMs()

                        // Check access tokens for expiration.
                        catching(wrapIE) {
                            NCTxManager.startTx {
                                for (ses ← tokenSigninCache.asScala.map(_.getValue)
                                     if now - ses.lastAccessMs >= Config.expireMs
                                ) {
                                    tokenSigninCache -= ses.acsToken
                                    idSigninCache -= ses.userId

                                    ses.endpoint match {
                                        case Some(_) ⇒ NCEndpointManager.cancelNotifications(ses.userId)
                                        case None ⇒ // No-op.
                                    }

                                    // Notification.
                                    NCNotificationManager.addEvent("NC_ACCESS_TOKEN_TIMEDOUT",
                                        "acsTok" → ses.acsToken,
                                        "userId" → ses.userId,
                                        "signinMs" → ses.signinMs,
                                        "lastAccessMs" → ses.lastAccessMs
                                    )

                                    logger.trace(s"Access token timed out: ${ses.acsToken}")
                                }
                            }
                        }
                    }
                    catch {
                        case e: Throwable ⇒ logger.error("Error during timeout scanner process.", e)
                    }
                }
            },
            Config.scannerMs,
            Config.scannerMs
        )

        logger.info(s"Access tokens will be scanned for timeout every ${Config.timeoutScannerFreqMins} min.")
        logger.info(s"Access tokens inactive for ${Config.accessTokenExpireTimeoutMins} min will be invalidated.")

        NCSql.sql {
            if (!NCSql.exists("nc_user"))
                try {
                    val email = "admin@admin.com"
                    val pswd = "admin"

                    addUser0(
                        email,
                        pswd,
                        "Hermann",
                        "Minkowski",
                        avatarUrl = None,
                        isAdmin = true
                    )

                    logger.info(s"Default admin user ($email/$pswd) created.")

                }
                catch {
                    case e: NCE ⇒ logger.error(s"Failed to add default admin user: ${e.getLocalizedMessage}", e)
                }
        }

        super.start()
    }

    /**
      * Gets the list of all current users.
      */
    @throws[NCE]
    def getAllUsers: Seq[NCUserMdo] = {
        ensureStarted()

        NCSql.sql {
            NCSqlManager.getAllUsers
        }
    }

    /**
      * Gets flag which indicates there are another admin users in the system or not.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def isOtherAdminsExist(usrId: Long): Boolean = {
        ensureStarted()

        NCSql.sql {
            NCSqlManager.isOtherAdminsExist(usrId)
        }
    }


    /**
      * Stops this manager.
      */
    override def stop(): Unit = {
        if (scanner != null)
            scanner.cancel()

        scanner = null
        
        tokenSigninCache = null
        idSigninCache = null

        super.stop()
    }

    /**
      *
      * @param acsTok Access token to sign out.
      */
    @throws[NCE]
    def signout(acsTok: String): Unit = {
        ensureStarted()

        catching(wrapIE) {
            NCTxManager.startTx {
                tokenSigninCache -== acsTok match {
                    case Some(ses) ⇒
                        idSigninCache -= ses.userId

                        NCEndpointManager.cancelNotifications(ses.userId)

                        // Notification.
                        NCNotificationManager.addEvent("NC_USER_SIGNED_OUT",
                            "acsTok" → ses.acsToken,
                            "userId" → ses.userId,
                            "signinMs" → ses.signinMs,
                            "lastAccessMs" → ses.lastAccessMs
                        )

                        logger.info(s"User signed out: ${ses.userId}")
                    case None ⇒ // No-op.
                }
            }
        }
    }

    /**
      * Gets user ID associated with active access token, if any.
      *
      * @param acsTkn Access token.
      * @return
      */
    @throws[NCE]
    def getUserForAccessToken(acsTkn: String): Option[NCUserMdo] = {
        ensureStarted()

        getUserIdForAccessToken(acsTkn).flatMap(getUser)
    }

    /**
      * Gets user ID associated with active access token, if any.
      *
      * @param acsTkn Access token.
      * @return
      */
    @throws[NCE]
    def getUserIdForAccessToken(acsTkn: String): Option[Long] = {
        ensureStarted()

        catching(wrapIE) {
            tokenSigninCache(acsTkn) match {
                case Some(ses) ⇒
                    val now = U.nowUtcMs()

                    // Update login session.
                    tokenSigninCache += acsTkn → SigninSession(acsTkn, ses.userId, ses.signinMs, now, ses.endpoint)

                    Some(ses.userId) // Bingo!
                case None ⇒ None
            }
        }
    }

    /**
      * Gets user for given user ID.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def getUser(usrId: Long): Option[NCUserMdo] = {
        ensureStarted()

        NCSql.sql {
            NCSqlManager.getUser(usrId)
        }
    }

    /**
      * Gets user for given user ID.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def getUserEndpoint(usrId: Long): Option[String] = {
        ensureStarted()

        catching(wrapIE) {
            idSigninCache(usrId) match {
                case Some(tok) ⇒
                    tokenSigninCache(tok) match {
                        case Some(ses) ⇒ ses.endpoint
                        case None ⇒ None

                    }
                case None ⇒ None
            }
        }
    }

    /**
      *
      * @param email User email (as username).
      * @param passwd User password.
      * @return
      */
    @throws[NCE]
    def signin(email: String, passwd: String): Option[String] = {
        ensureStarted()
        
        catching(wrapIE) {
            NCTxManager.startTx {
                NCSql.sql {
                    NCSqlManager.getUserByEmail(email)
                } match {
                    case Some(usr) ⇒
                        NCSql.sql {
                            if (!NCSqlManager.isKnownPasswordHash(NCBlowfishHasher.hash(passwd, usr.passwordSalt)))
                                None
                            else {
                                val newAcsTkn = tokenSigninCache.asScala.find(entry ⇒ entry.getValue.userId == usr.id) match {
                                    case Some(entry) ⇒ entry.getValue.acsToken // Already signed in.
                                    case None ⇒
                                        val acsTkn = U.genGuid()
                                        val now = U.nowUtcMs()

                                        tokenSigninCache += acsTkn → SigninSession(acsTkn, usr.id, now, now, None)

                                        idSigninCache += usr.id → acsTkn

                                        acsTkn
                                }

                                // Notification.
                                NCNotificationManager.addEvent("NC_USER_SIGNED_IN",
                                    "userId" → usr.id,
                                    "firstName" → usr.firstName,
                                    "lastName" → usr.lastName,
                                    "email" → usr.email
                                )

                                logger.info(s"User signed in [" +
                                    s"email=${usr.email}, " +
                                    s"firstName=${usr.firstName}, " +
                                    s"lastName=${usr.lastName}" +
                                    s"]")

                                Some(newAcsTkn)
                            }
                        }
                    case None ⇒ None
                }
            }
        }
    }

    /**
      *
      * @param usrId
      * @param firstName
      * @param lastName
      * @param avatarUrl
      * @return
      */
    @throws[NCE]
    def updateUser(
        usrId: Long,
        firstName: String,
        lastName: String,
        avatarUrl: Option[String]
    ): Unit = {
        ensureStarted()

        NCSql.sql {
            val n =
                NCSqlManager.updateUser(
                    usrId,
                    firstName,
                    lastName,
                    avatarUrl
                )

            if (n == 0)
                throw new NCE(s"Unknown user ID: $usrId")
        }

        // Notification.
        NCNotificationManager.addEvent("NC_USER_UPDATE",
            "userId" → usrId,
            "firstName" → firstName,
            "lastName" → lastName
        )
    }

    /**
      *
      * @param usrId
      * @param firstName
      * @param lastName
      * @param avatarUrl
      * @return
      */
    @throws[NCE]
    def updateUserPermissions(usrId: Long, isAdmin: Boolean): Unit = {
        ensureStarted()

        NCSql.sql {
            val n = NCSqlManager.updateUser(usrId, isAdmin)

            if (n == 0)
                throw new NCE(s"Unknown user ID: $usrId")
        }

        // Notification.
        NCNotificationManager.addEvent("NC_USER_UPDATE",
            "userId" → usrId,
            "isAdmin" → isAdmin
        )
    }

    /**
      *
      * @param usrId
      * @return
      */
    @throws[NCE]
    def deleteUser(usrId: Long): Unit = {
        ensureStarted()

        val usr =
            NCSql.sql {
                val usr = NCSqlManager.getUser(usrId).getOrElse(throw new NCE(s"Unknown user ID: $usrId"))
                
                NCSqlManager.deleteUser(usr.id)

                usr
            }

        // Notification.
        NCNotificationManager.addEvent("NC_USER_DELETE",
            "userId" → usrId,
            "firstName" → usr.firstName,
            "lastName" → usr.lastName,
            "email" → usr.email
        )
    }

    /**
      *
      * @param usrId ID of the user to reset password for.
      * @param newPasswd New password to set.
      */
    @throws[NCE]
    def resetPassword(usrId: Long, newPasswd: String): Unit = {
        ensureStarted()

        NCSql.sql {
            val usr = NCSqlManager.getUser(usrId).getOrElse(throw new NCE(s"Unknown user ID: $usrId"))

            val salt = NCBlowfishHasher.hash(usr.email)

            // Add actual hash for the password.
            // NOTE: we don't "stir up" password pool for password resets.
            NCSqlManager.addPasswordHash(pswdSeq.incrementAndGet(), NCBlowfishHasher.hash(newPasswd, salt))
        }

        // Notification.
        NCNotificationManager.addEvent("NC_USER_PASSWD_RESET",
            "userId" → usrId
        )
    }

    /**
      *
      * @param email
      * @param pswd
      * @param firstName
      * @param lastName
      * @param avatarUrl
      * @param isAdmin
      * @return
      */
    @throws[NCE]
    def addUser(
        email: String,
        pswd: String,
        firstName: String,
        lastName: String,
        avatarUrl: Option[String],
        isAdmin: Boolean
    ): Long = {
        ensureStarted()

        val id =
            NCSql.sql {
                addUser0(email, pswd, firstName, lastName, avatarUrl, isAdmin)
            }

        NCNotificationManager.addEvent(
            "NC_USER_ADD",
            "userId" → id,
            "firstName" → firstName,
            "lastName" → lastName,
            "email" → email
        )

        logger.info(s"User $email created.")

        id
    }

    /**
      *
      * @param email
      * @param pswd
      * @param firstName
      * @param lastName
      * @param avatarUrl
      * @param isAdmin
      */
    @throws[NCE]
    def addUser0(
        email: String,
        pswd: String,
        firstName: String,
        lastName: String,
        avatarUrl: Option[String],
        isAdmin: Boolean
    ): Long = {
        val normEmail = U.normalizeEmail(email)

        if (!EMAIL_VALIDATOR.isValid(normEmail))
            throw new NCE(s"New user email is invalid: $normEmail")

        if (NCSqlManager.getUserByEmail(normEmail).isDefined)
            throw new NCE(s"User with this email already exists: $normEmail")

        val newUsrId = usersSeq.incrementAndGet()
        val salt = NCBlowfishHasher.hash(normEmail)

        NCSqlManager.addUser(
            newUsrId,
            normEmail,
            firstName,
            lastName,
            avatarUrl,
            salt,
            isAdmin
        )

        // Add actual hash for the password.
        NCSqlManager.addPasswordHash(pswdSeq.incrementAndGet(), NCBlowfishHasher.hash(pswd, salt))

        // "Stir up" password pool with each user.
        (0 to Math.round((Math.random() * Config.pwdPoolBlowup) + Config.pwdPoolBlowup).toInt).foreach(_ ⇒
            NCSqlManager.addPasswordHash(pswdSeq.incrementAndGet(), NCBlowfishHasher.hash(U.genGuid()))
        )

        newUsrId
    }

    /**
      * Updates endpoint for user session.
      * 
      * @param usrId User ID.
      * @param epOpt Endpoint.
      */
    private def updateEndpoint(usrId: Long, epOpt: Option[String]): Unit =
        catching(wrapIE) {
            idSigninCache(usrId) match {
                case Some(acsToken) ⇒
                    tokenSigninCache(acsToken) match {
                        case Some(ses) ⇒ Some(ses)
                        case None ⇒
                            logger.error(s"Token cache not found for: $acsToken")

                            None
                    }

                case None ⇒
                    logger.trace(s"User cache not found for: $usrId")

                    None
            }
        } match {
            case Some(ses) ⇒
                tokenSigninCache +=
                    ses.acsToken → SigninSession(ses.acsToken, ses.userId, ses.signinMs, ses.lastAccessMs, epOpt
                )

            case None ⇒ // No-op.
        }

    /**
      * Registers session level user endpoint.
      *
      * @param usrId User ID.
      * @param ep Endpoint URL.
      */
    def registerEndpoint(usrId: Long, ep: String): Unit = {
        ensureStarted()

        updateEndpoint(usrId, Some(ep))
    
        // Notification.
        NCNotificationManager.addEvent("NC_USER_ADD_ENDPOINT",
            "userId" → usrId,
            "endpoint" → ep
        )
    }

    /**
      * De-registers session level user endpoint if some was registered
      *
      * @param usrId User ID.
      */
    def removeEndpoint(usrId: Long): Unit = {
        ensureStarted()

        updateEndpoint(usrId, None)
    
        // Notification.
        NCNotificationManager.addEvent("NC_USER_REMOVE_ENDPOINT",
            "userId" → usrId
        )
    }
}
