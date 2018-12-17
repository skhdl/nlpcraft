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
import org.nlpcraft.ignite.NCIgniteHelpers._
import org.nlpcraft.ignite.NCIgniteNlpCraft
import org.nlpcraft._

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
                    
                        logger.trace(s"Access token timed out: ${ses.acsToken}")
                    }
                }
            }
        },
        Config.scannerMs, Config.scannerMs)
    
        logger.info(f"Access tokens will be scanned for timeout every ${Config.accessTokenExpireTimeoutMins} min.")
        
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
}
