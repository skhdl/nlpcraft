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

package org.nlpcraft.user

import java.util.{Timer, TimerTask}

import org.apache.commons.validator.routines.EmailValidator
import org.apache.ignite.cache.CachePeekMode
import org.apache.ignite.{IgniteAtomicSequence, IgniteCache}
import org.nlpcraft._
import org.nlpcraft.blowfish.NCBlowfishHasher
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.endpoints.NCEndpointManager
import org.nlpcraft.ignite.NCIgniteHelpers._
import org.nlpcraft.ignite.NCIgniteNLPCraft
import org.nlpcraft.mdo.NCUserMdo
import org.nlpcraft.notification.NCNotificationManager
import org.nlpcraft.tx.NCTxManager

import scala.collection.JavaConverters._
import scala.util.control.Exception._

/**
  * User management (signup, add, delete, update) manager.
  */
object NCUserManager extends NCLifecycle("User manager") with NCIgniteNLPCraft {
    // Static email validator.
    private final val EMAIL_VALIDATOR = EmailValidator.getInstance()

    // Caches.
    @volatile private var userCache: IgniteCache[Either[Long, String], NCUserMdo] = _
    @volatile private var tokenSigninCache: IgniteCache[String, SigninSession] = _
    @volatile private var idSigninCache: IgniteCache[Long, String] = _

    @volatile private var usersSeq: IgniteAtomicSequence = _

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
        lazy val scannerMs: Int = timeoutScannerFreqMins * 60 * 1000
        lazy val expireMs: Int = accessTokenExpireTimeoutMins * 60 * 1000
        val pwdPoolBlowup: Int = hocon.getInt("user.pwdPoolBlowup")
        val timeoutScannerFreqMins: Int = hocon.getInt("user.timeoutScannerFreqMins")
        val accessTokenExpireTimeoutMins: Int = hocon.getInt("user.accessTokenExpireTimeoutMins")

        override def check(): Unit = {
            require(pwdPoolBlowup > 1,
                s"Configuration parameter 'user.pwdPoolBlowup' must be > 1")
            require(timeoutScannerFreqMins > 0,
                s"Configuration parameter 'user.timeoutScannerFreqMins' must be > 0")
            require(accessTokenExpireTimeoutMins > 0,
                s"Configuration parameter 'user.accessTokenExpireTimeoutMins' must be > 0")
        }
    }

    Config.check()

    /**
      * Starts this manager.
      */
    override def start(): NCLifecycle = {
        ensureStopped()

        usersSeq = NCPsql.sqlNoTx {
            ignite.atomicSequence(
                "usersSeq",
                NCDbManager.getMaxColumnValue("nc_user", "id").getOrElse(0),
                true
            )
        }

        catching(wrapIE) {
            userCache = ignite.cache[Either[Long, String], NCUserMdo]("user-cache")
            tokenSigninCache = ignite.cache[String, SigninSession]("user-token-signin-cache")
            idSigninCache = ignite.cache[Long, String]("user-id-signin-cache")

            require(userCache != null)
            require(tokenSigninCache != null)
            require(idSigninCache != null)

            userCache.localLoadCache(null)
        }

        scanner = new Timer("timeout-scanner")

        scanner.scheduleAtFixedRate(
            new TimerTask() {
                def run() {
                    try {
                        val now = G.nowUtcMs()

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
                                        "accessToken" → ses.acsToken,
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

        catching(wrapIE) {
            if (userCache.localEntries(CachePeekMode.ALL).asScala.isEmpty)
                try
                    addDefaultUser()
                catch {
                    case e: NCE ⇒ logger.error(s"Failed to add default admin user: ${e.getLocalizedMessage}")
                }
        }

        super.start()
    }

    /**
      * Adds default user.
      */
    @throws[NCE]
    private def addDefaultUser(): Unit = {
        val email = "admin@admin.com"
        val pswd = "admin"

        addUser0(
            email,
            pswd,
            "Hermann",
            "Minkowski",
            avatarUrl = None,
            isAdmin = true,
            event = None
        )

        logger.info(s"Default admin user ($email/$pswd) created.")
    }

    /**
      * Gets the list of all current users.
      */
    @throws[NCE]
    def getAllUsers: Seq[NCUserMdo] = {
        ensureStarted()

        catching(wrapIE) {
            // Users can be duplicated by their keys (ID and email)
            userCache.localEntries(CachePeekMode.ALL).asScala.toSeq.map(_.getValue).distinct
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
        userCache = null

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
                            "accessToken" → ses.acsToken,
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
                    val now = G.nowUtcMs()

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

        catching(wrapIE) {
            userCache(Left(usrId))
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
                userCache(Right(G.normalizeEmail(email))) match {
                    case Some(usr) ⇒
                        NCPsql.sql {
                            if (!NCDbManager.isKnownPasswordHash(NCBlowfishHasher.hash(passwd, usr.passwordSalt)))
                                None
                            else {
                                val newAcsTkn = tokenSigninCache.asScala.find(entry ⇒ entry.getValue.userId == usr.id) match {
                                    case Some(entry) ⇒
                                        logger.info(s"User already signed in - reusing access token [" +
                                            s"email=${usr.email}, " +
                                            s"firstName=${usr.firstName}, " +
                                            s"lastName=${usr.lastName}" +
                                            s"]")

                                        entry.getValue.acsToken // Already signed in.
                                    case None ⇒
                                        val acsTkn = NCBlowfishHasher.hash(G.genGuid())
                                        val now = G.nowUtcMs()

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
      * @param isAdmin
      * @return
      */
    @throws[NCE]
    def updateUser(
        usrId: Long,
        firstName: String,
        lastName: String,
        avatarUrl: Option[String],
        isAdmin: Boolean
    ): Unit = {
        ensureStarted()

        val idKey = Left(usrId)

        catching(wrapIE) {
            NCTxManager.startTx {
                val usr = userCache(idKey).getOrElse(throw new NCE(s"Unknown user ID: $usrId"))

                val mdo = NCUserMdo(
                    usr.id,
                    usr.email,
                    firstName,
                    lastName,
                    avatarUrl,
                    usr.passwordSalt,
                    isAdmin,
                    usr.createdOn
                )

                userCache += idKey → mdo
                userCache += Right(usr.email) → mdo
            }
        }

        // Notification.
        NCNotificationManager.addEvent("NC_USER_UPDATE",
            "userId" → usrId,
            "firstName" → firstName,
            "lastName" → lastName,
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

        val idKey = Left(usrId)

        catching(wrapIE) {
            NCTxManager.startTx {
                val usr = userCache(idKey).getOrElse(throw new NCE(s"Unknown user ID: $usrId"))

                userCache -= idKey
                userCache -= Right(usr.email)

                // Notification.
                NCNotificationManager.addEvent("NC_USER_DELETE",
                    "userId" → usrId,
                    "firstName" → usr.firstName,
                    "lastName" → usr.lastName,
                    "email" → usr.email
                )
            }
        }
    }

    /**
      *
      * @param usrId ID of the user to reset password for.
      * @param newPasswd New password to set.
      */
    @throws[NCE]
    def resetPassword(usrId: Long, newPasswd: String): Unit = {
        ensureStarted()

        catching(wrapIE) {
            val usr = userCache(Left(usrId)).getOrElse(throw new NCE(s"Unknown user ID: $usrId"))

            val salt = NCBlowfishHasher.hash(usr.email)

            NCPsql.sql {
                // Add actual hash for the password.
                // NOTE: we don't "stir up" password pool for password resets.
                NCDbManager.addPasswordHash(NCBlowfishHasher.hash(newPasswd, salt))
            }

            // Notification.
            NCNotificationManager.addEvent("NC_USER_PASSWD_RESET",
                "userId" → usrId
            )
        }
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

        val id = addUser0(email, pswd, firstName, lastName, avatarUrl, isAdmin, Some("NC_USER_ADD"))

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
      * @throws NCException Thrown in case of any signup errors.
      * @return Newly created user ID.
      */
    @throws[NCE]
    def signup(
        email: String,
        pswd: String,
        firstName: String,
        lastName: String,
        avatarUrl: Option[String]
    ): Long = {
        ensureStarted()

        val id = addUser0(email, pswd, firstName, lastName, avatarUrl, true, Some("NC_SIGNUP"))

        logger.info(s"User $email signup ok.")

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
      * @param event
      * @return
      */
    @throws[NCE]
    def addUser0(
        email: String,
        pswd: String,
        firstName: String,
        lastName: String,
        avatarUrl: Option[String],
        isAdmin: Boolean,
        event: Option[String]
    ): Long = {
        val normEmail = G.normalizeEmail(email)

        if (!EMAIL_VALIDATOR.isValid(normEmail))
            throw new NCE(s"New user email is invalid: $normEmail")

        val emailKey = Right(normEmail)

        val (newUsrId, salt) =
            catching(wrapIE) {
                NCTxManager.startTx {
                    if (userCache.containsKey(emailKey))
                        throw new NCE(s"User with this email already exists: $normEmail")

                    val newUsrId = usersSeq.incrementAndGet()
                    val salt = NCBlowfishHasher.hash(normEmail)

                    val mdo = NCUserMdo(
                        newUsrId,
                        normEmail,
                        firstName,
                        lastName,
                        avatarUrl,
                        salt,
                        isAdmin
                    )

                    userCache += Left(newUsrId) → mdo
                    userCache += emailKey → mdo

                    (newUsrId, salt)
                }
            }

        NCPsql.sql {
            // Add actual hash for the password.
            NCDbManager.addPasswordHash(NCBlowfishHasher.hash(pswd, salt))

            // "Stir up" password pool with each user.
            (0 to Math.round((Math.random() * Config.pwdPoolBlowup) + Config.pwdPoolBlowup).toInt).foreach(_ ⇒
                NCDbManager.addPasswordHash(NCBlowfishHasher.hash(G.genGuid()))
            )

            event match {
                case Some(e) ⇒
                    // Notification.
                    NCNotificationManager.addEvent(
                        e,
                        "userId" → newUsrId,
                        "firstName" → firstName,
                        "lastName" → lastName,
                        "email" → normEmail
                    )
                case None ⇒ // No-op.
            }

            newUsrId
        }
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
