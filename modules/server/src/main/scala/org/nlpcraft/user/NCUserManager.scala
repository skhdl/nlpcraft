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

package org.nlpcraft.user

import java.util.{Timer, TimerTask}

import org.nlpcraft._
import org.apache.commons.validator.routines.EmailValidator
import org.apache.ignite.{IgniteCache, IgniteException}
import org.nlpcraft.blowfish.NCBlowfishHasher
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.ignite.NCIgniteNlpCraft
import org.nlpcraft.notification.NCNotificationManager

import scala.collection.JavaConverters._
import scala.util.control.Exception._

/**
  * User management (signup, add, delete, update) manager.
  */
object NCUserManager extends NCLifecycle("User manager") with NCIgniteNlpCraft with NCDebug {
    // Static email validator.
    private final val EMAIL_VALIDATOR = EmailValidator.getInstance()
    
    // Caches.
    @volatile private var signinCache: IgniteCache[String, SigninSession] = _
    // Access token timeout scanner.
    @volatile private var scanner: Timer = _

    // Session holder.
    private case class SigninSession(
        acsToken: String,
        userId: Long,
        signinMs: Long,
        lastAccessMs: Long
    )

    private object Config extends NCConfigurable {
        val pwdPoolBlowup: Int = hocon.getInt("user.pwdPoolBlowup")
        val timeoutScannerFreqMins: Int = hocon.getInt("user.timeoutScannerFreqMins")
        val accessTokenExpireTimeoutMins: Int = hocon.getInt("user.accessTokenExpireTimeoutMins")

        lazy val scannerMs: Int = timeoutScannerFreqMins * 60 * 1000
        lazy val expireMs: Int = accessTokenExpireTimeoutMins * 60 * 1000

        override def check(): Unit = {
            require(pwdPoolBlowup > 1 , s"password pool blowup ($pwdPoolBlowup) must be > 1")
            require(timeoutScannerFreqMins > 0 , s"timeout scanner frequency ($timeoutScannerFreqMins) must be > 0")
            require(accessTokenExpireTimeoutMins > 0 , s"access token expire timeout ($accessTokenExpireTimeoutMins) must be > 0")
        }
    }
    
    Config.check()
    
    /**
      * Starts this manager.
      */
    override def start(): NCLifecycle = {
        ensureStopped()

        signinCache = nlpcraft.cache[String, SigninSession]("user-signin-cache")

        require(signinCache != null)

        scanner = new Timer("timeout-scanner")

        scanner.scheduleAtFixedRate(
            new TimerTask() {
                def run() {
                    val now = System.currentTimeMillis()

                    // Check access tokens for expiration.
                    ignoring(classOf[IgniteException]) {
                        for (ses ← signinCache.asScala.map(_.getValue) if now - ses.lastAccessMs >= Config.expireMs) {
                            signinCache.remove(ses.acsToken)

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
            },
            Config.scannerMs,
            Config.scannerMs
        )

        NCPsql.sql { NCDbManager.addDefaultUser() }

        logger.info(s"Access tokens will be scanned for timeout every ${Config.timeoutScannerFreqMins} min.")
        logger.info(s"Access tokens inactive for ${Config.accessTokenExpireTimeoutMins} min will be invalidated.")

        super.start()
    }

    /**
      * Stops this manager.
      */
    override def stop(): Unit = {
        if (scanner != null)
            scanner.cancel()

        scanner = null
        signinCache = null

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
            signinCache.getAndRemove(acsTok) match {
                case null ⇒ // No-op.
                case ses ⇒
                    // Notification.
                    NCNotificationManager.addEvent("NC_USER_SIGNED_OUT",
                        "accessToken" → ses.acsToken,
                        "userId" → ses.userId,
                        "signinMs" → ses.signinMs,
                        "lastAccessMs" → ses.lastAccessMs
                    )

                    logger.info(s"User signed out: $ses")
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
    def getUserIdForAccessToken(acsTkn: String): Option[Long] = {
        ensureStarted()

        catching(wrapIE) {
            signinCache.get(acsTkn) match {
                case null ⇒
                    None
                case ses: SigninSession ⇒
                    val now = System.currentTimeMillis()

                    // Update login session.
                    signinCache.put(acsTkn, SigninSession(
                        acsTkn,
                        userId = ses.userId,
                        signinMs = ses.signinMs,
                        lastAccessMs = now
                    ))

                    Some(ses.userId) // Bingo!
            }
        }
    }

    /**
      * Checks if given access token is valid.
      *
      * @param acsTkn Access token.
      * @return
      */
    @throws[NCE]
    def checkAccessToken(acsTkn: String): Boolean = {
        ensureStarted()

        getUserIdForAccessToken(acsTkn).isDefined
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

        NCPsql.sql {
            NCDbManager.getUserByEmail(G.normalizeEmail(email)) match {
                case None ⇒ None
                case Some(usr) ⇒
                    if (!NCDbManager.isKnownPasswordHash(NCBlowfishHasher.hash(passwd, usr.passwordSalt)))
                        None
                    else {
                        catching(wrapIE) {
                            val newAcsTkn = signinCache.asScala.find(entry ⇒ entry.getValue.userId == usr.id) match {
                                case Some(entry) ⇒
                                    logger.info(s"User already signed in - reusing access token [" +
                                        s"email=${usr.email}, " +
                                        s"firstName=${usr.firstName}, " +
                                        s"lastName=${usr.lastName}" +
                                        s"]")

                                    entry.getValue.acsToken // Already signed in.
                                case None ⇒
                                    val acsTkn = NCBlowfishHasher.hash(G.genGuid())
                                    val now = System.currentTimeMillis()

                                    catching(wrapIE) {
                                        signinCache.put(acsTkn,
                                            SigninSession(
                                                acsTkn,
                                                usr.id,
                                                now,
                                                now
                                            )
                                        )
                                    }

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
        avatarUrl: String,
        isAdmin: Boolean
    ) : Unit = {
        ensureStarted()

        NCPsql.sql {
            NCDbManager.getUser(usrId) match {
                case None ⇒ throw new NCE(s"Unknown user ID: $usrId")
                case _ ⇒
                    NCDbManager.updateUser(
                        usrId,
                        avatarUrl,
                        firstName,
                        lastName,
                        isAdmin
                    )

                    // Notification.
                    NCNotificationManager.addEvent("NC_USER_UPDATE",
                        "userId" → usrId,
                        "firstName" → firstName,
                        "lastName" → lastName,
                        "isAdmin" → isAdmin
                    )

            }
        }
    }

    /**
      *
      * @param usrId
      * @return
      */
    @throws[NCE]
    def deleteUser(usrId: Long) : Unit = {
        ensureStarted()

        NCPsql.sql {
            NCDbManager.getUser(usrId) match {
                case None ⇒ throw new NCE(s"Unknown user ID: $usrId")
                case Some(usr) ⇒
                    NCDbManager.deleteUser(usrId)

                    // Notification.
                    NCNotificationManager.addEvent("NC_USER_DELETE",
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

        NCPsql.sql {
            NCDbManager.getUser(usrId) match {
                case None ⇒ throw new NCE(s"Unknown user ID: $usrId")
                case Some(usr) ⇒
                    val salt = NCBlowfishHasher.hash(usr.email)

                    // Add actual hash for the password.
                    // NOTE: we don't "stir up" password pool for password resets.
                    NCDbManager.addPasswordHash(NCBlowfishHasher.hash(newPasswd, salt))

                    // Notification.
                    NCNotificationManager.addEvent("NC_USER_PASSWD_RESET",
                        "userId" → usrId
                    )
            }
        }
    }

    /**
      *
      * @param usrId
      * @param newUsrEmail
      * @param newUsrPasswd
      * @param newUsrFirstName
      * @param newUsrLastName
      * @param newUsrAvatarUrl
      * @param newUsrIsAdmin
      * @return
      */
    @throws[NCE]
    def addUser(
        usrId: Long,
        newUsrEmail: String,
        newUsrPasswd: String,
        newUsrFirstName: String,
        newUsrLastName: String,
        newUsrAvatarUrl: String,
        newUsrIsAdmin: Boolean
    ) : Long = {
        ensureStarted()

        val normEmail = G.normalizeEmail(newUsrEmail)

        if (!EMAIL_VALIDATOR.isValid(normEmail))
            throw new NCE(s"New user email is invalid: $normEmail")

        NCPsql.sql {
            if (NCDbManager.getUserByEmail(normEmail).isDefined)
                throw new NCE(s"User with this email already exists: $normEmail")

            val salt = NCBlowfishHasher.hash(normEmail)

            // Add new user.
            val newUsrId = NCDbManager.addUser(
                newUsrFirstName,
                newUsrLastName,
                normEmail,
                salt,
                newUsrAvatarUrl,
                newUsrIsAdmin
            )

            // Add actual hash for the password.
            NCDbManager.addPasswordHash(NCBlowfishHasher.hash(newUsrPasswd, salt))

            // "Stir up" password pool with each user.
            (0 to Math.round((Math.random() * Config.pwdPoolBlowup) + Config.pwdPoolBlowup).toInt).foreach(_ ⇒
                NCDbManager.addPasswordHash(NCBlowfishHasher.hash(G.genGuid()))
            )

            // Notification.
            NCNotificationManager.addEvent("NC_USER_ADD",
                "addByUserId" → usrId,
                "firstName" → newUsrFirstName,
                "lastName" → newUsrLastName,
                "email" → normEmail
            )

            newUsrId
        }
    }

    /**
      *
      * @param email
      * @param passwd
      * @param firstName
      * @param lastName
      * @param avatarUrl
      * @throws NCException Thrown in case of any signup errors.
      * @return Newly created user ID.
      */
    @throws[NCE]
    def signup(
        email: String,
        passwd: String,
        firstName: String,
        lastName: String,
        avatarUrl: String
    ): Long = {
        ensureStarted()
        
        val normEmail = G.normalizeEmail(email)
    
        if (!EMAIL_VALIDATOR.isValid(normEmail))
            throw new NCE(s"New user email is invalid: $normEmail")
    
        NCPsql.sql {
            if (NCDbManager.getUserByEmail(normEmail).isDefined)
                throw new NCE(s"User email already exists: $normEmail")
    
            val salt = NCBlowfishHasher.hash(normEmail)
    
            // Add new user.
            val usrId = NCDbManager.addUser(
                firstName,
                lastName,
                normEmail,
                salt,
                avatarUrl,
                isAdmin = true
            )
        
            // Add actual hash for the password.
            NCDbManager.addPasswordHash(NCBlowfishHasher.hash(passwd, salt))
        
            // "Stir up" password pool with each user.
            (0 to Math.round((Math.random() * Config.pwdPoolBlowup) + Config.pwdPoolBlowup).toInt).foreach(_ ⇒
                NCDbManager.addPasswordHash(NCBlowfishHasher.hash(G.genGuid()))
            )
        
            // Notification.
            NCNotificationManager.addEvent("NC_SIGNUP",
                "firstName" → firstName,
                "lastName" → lastName,
                "email" → normEmail
            )
        
            usrId
        }
    }
}
