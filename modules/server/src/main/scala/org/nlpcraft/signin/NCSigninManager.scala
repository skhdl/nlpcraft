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

package org.nlpcraft.signin

import java.util.{Timer, TimerTask}

import org.apache.ignite.{IgniteCache, IgniteException}
import org.nlpcraft.ignite.NCIgniteNlpCraft
import org.nlpcraft._
import org.nlpcraft.blowfish.NCBlowfishHasher
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.notification.NCNotificationManager

import scala.collection.JavaConverters._
import scala.util.control.Exception._

/**
  * TODO: add description.
  */
object NCSigninManager extends NCLifecycle("Signin manager") with NCIgniteNlpCraft with NCDebug {
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
        val timeoutScannerFreqMins = hocon.getInt("signin.timeoutScannerFreqMins")
        val accessTokenExpireTimeoutMins = hocon.getInt("signin.accessTokenExpireTimeoutMins")
        
        override def check(): Unit = {
            require(timeoutScannerFreqMins > 1 , s"timeout scanner frequency ($timeoutScannerFreqMins) must be > 1")
            require(accessTokenExpireTimeoutMins > 1 , s"access token expire timeout ($accessTokenExpireTimeoutMins) must be > 1")
        }
        
        lazy val scannerMs = timeoutScannerFreqMins * 60 * 1000
        lazy val expireMs = accessTokenExpireTimeoutMins * 60 * 1000
    }
    
    Config.check()
    
    /**
      * Starts this manager.
      */
    override def start(): NCLifecycle = {
        ensureStopped()
    
        signinCache = nlpcraft.cache[String, SigninSession]("signin-cache")
    
        require(signinCache != null)
    
        scanner = new Timer("timeout-scanner")
    
        scanner.scheduleAtFixedRate(new TimerTask() {
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
        Config.scannerMs, Config.scannerMs)
    
        logger.info(s"Access tokens will be scanned for timeout every ${Config.accessTokenExpireTimeoutMins} min.")
        
        super.start()
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
                    NCNotificationManager.addEvent("NC_ACCESS_TOKEN_SIGNED_OUT",
                        "accessToken" → ses.acsToken,
                        "userId" → ses.userId,
                        "signinMs" → ses.signinMs,
                        "lastAccessMs" → ses.lastAccessMs
                    )
                    
                    logger.info(s"Access token signed out: $ses")
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
      * Stops this manager.
      */
    override def stop(): Unit = {
        if (scanner != null)
            scanner.cancel()
    
        scanner = null
        signinCache = null
        
        super.stop()
    }
}
