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

import org.nlpcraft._
import org.apache.commons.validator.routines.EmailValidator
import org.nlpcraft.blowfish.NCBlowfishHasher
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.notification.NCNotificationManager

/**
  * Signup manager.
  */
object NCUserManager extends NCLifecycle("User manager") with NCAdminToken {
    // Static email validator.
    private final val EMAIL_VALIDATOR = EmailValidator.getInstance()
    
    private object Config extends NCConfigurable {
        val pwdPoolBlowup = hocon.getInt("user.pwdPoolBlowup")
        
        override def check(): Unit = {
            require(pwdPoolBlowup > 1 , s"password pool blowup ($pwdPoolBlowup) must be > 1")
        }
    }
    
    Config.check()
    
    /**
      *
      * @param adminToken
      * @param email
      * @param passwd
      * @param firstName
      * @param lastName
      * @param avatarUrl
      * @throws NCException Thrown in case of any signup errors.
      * @return Newly creates user ID.
      */
    @throws[NCE]
    def signup(
        adminToken: String,
        email: String,
        passwd: String,
        firstName: String,
        lastName: String,
        avatarUrl: String
    ): Long = {
        if (adminToken != goldFinger)
            throw new NCE(s"Admin token is invalid: $adminToken")
        
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
                email,
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
                "email" → email
            )
        
            usrId
        }
    }
}
