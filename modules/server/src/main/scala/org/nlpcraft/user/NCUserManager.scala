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
  * User management (signup, add, delete, update) manager.
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
                throw new NCE(s"User email already exists: $normEmail")
        
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
        ensureStarted()
        
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
