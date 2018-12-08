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

package org.nlpcraft.notification

import java.net.URLEncoder

import org.apache.commons.lang3.StringEscapeUtils._
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.email.NCEmailManager
import org.nlpcraft.ignite.NCIgniteNlpCraft
import org.nlpcraft.mdo.NCUserMdo
import org.nlpcraft.sms.NCSmsManager
import org.nlpcraft.{NCConfigurable, NCE, NCLifecycle}

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.Future
import scala.io.Source
import scala.language.postfixOps

/**
  * Notification manager.
  */
object NCNotificationManager extends NCLifecycle("CORE notification manager") with NCIgniteNlpCraft {
    // TODO: skh (host)
    // TODO: templates
    // TODO: word xxx
    private final val WEB_HOST_PORT = if (IS_PROD) "www.xxx.com" else "localhost:8080"
    
    private[notification] final val EMAIL_TPL: String =
        read("templates/email-template.html")
    
    /**
      * Reads file.
      *
      * @param fileName File name.
      */
    private def read(fileName: String): String =
        Source.fromInputStream(getClass.getResourceAsStream(fileName), "UTF8").mkString
    
    /**
      *
      * @param fileName Classpath-local file name.
      * @return
      */
    private def mkEmailHtml(fileName: String): String =
        EMAIL_TPL.replace("{{BODY}}", read(fileName))
    
    private[notification] final val NEW_USER_WELCOME_EMAIL = mkEmailHtml("templates/new-user-welcome-email.html")
    private[notification] final val NEW_COMPANY_WELCOME_EMAIL = mkEmailHtml("templates/new-company-welcome-email.html")
    private[notification] final val USER_GRANT_ADMIN_EMAIL = mkEmailHtml("templates/user-grant-admin-email.html")
    private[notification] final val USER_REVOKE_ADMIN_EMAIL = mkEmailHtml("templates/user-revoke-admin-email.html")
    private[notification] final val USER_ENABLED_EMAIL = mkEmailHtml("templates/user-enabled-email.html")
    private[notification] final val USER_DISABLED_EMAIL = mkEmailHtml("templates/user-disabled-email.html")
    private[notification] final val PASSWORD_RESET_VERIFY_EMAIL = mkEmailHtml("templates/password-reset-verification.html")
    private[notification] final val PASSWORD_CHANGE_EMAIL = mkEmailHtml("templates/password-change.html")
    private[notification] final val USER_DELETE_EMAIL = mkEmailHtml("templates/user-delete-email.html")
    private[notification] final val USER_DELETE_BY_ADMIN_EMAIL = mkEmailHtml("templates/user-delete-by-admin-email.html")
    private[notification] final val RESULT_READY_EMAIL = mkEmailHtml("templates/result-ready.html")
    private[notification] final val NEW_PENDING_EMAIL = mkEmailHtml("templates/new-pending.html")
    
    // Type safe and eager settings containers.
    private object Cfg extends NCConfigurable {
        val intAddrFrom: String = hocon.getString("notification.internal.email.from")
        val intAddrTo: String = hocon.getString("notification.internal.email.to")
        val noreplyAddr: String = hocon.getString("notification.internal.email.noreply")
        val intSmsTo: Seq[String] = hocon.getStringList("notification.internal.sms.to")
        
        override def check(): Unit = {
            // No-op.
        }
    }
    
    Cfg.check()
    
    /**
      *
      * @param usrId User ID.
      * @param task Task to execute with the user.
      */
    private def forUserWith(usrId: Long)(task: NCUserMdo ⇒ Unit): Unit =
        NCPsql.sql {
            NCDbManager.getUser(usrId) match {
                case Some(usr) ⇒ task(usr)
                case None ⇒ logger.error(s"User not found (skipping notification): $usrId")
            }
        }
    
    /**
      * Sends internal mail.
      *
      * @param subj Subject.
      * @param msg Message.
      * @param html HTML or not.
      */
    private[notification] def sendInternalEmail(subj: String, msg: String, html: Boolean = false): Future[Unit] = {
        NCEmailManager.sendEmail(
            Cfg.intAddrFrom,
            subj,
            msg,
            html,
            Seq(Cfg.intAddrTo),
            (e: Throwable) ⇒ logger.error(s"Error sending internal email.", e)
        )
    }
    
    /**
      * Sends public mail (HTML only).
      *
      * @param subj Subject.
      * @param msg Message.
      * @param to To.
      */
    private[notification] def sendPublicEmail(subj: String, msg: String, to: String): Future[Unit] = {
        NCEmailManager.sendHtmlEmail(
            Cfg.noreplyAddr,
            subj,
            msg,
            to
        )
    }
    
    /**
      * Sends SMS notification to configured recipients.
      *
      * @param msg Message.
      */
    private def sendInternalSms(msg: String): Unit = {
        Cfg.intSmsTo.foreach(to ⇒ NCSmsManager.send(to, msg, (e: NCE) ⇒ logger.error(s"SMS to '$to' was not sent.", e)))
    }
    
    /**
      * Notification on user account deletion. Note that user's name and email are provided directly
      * since the user is deleted by the time this function is called.
      *
      * @param firstName User's first name.
      * @param lastName User's last name.
      * @param email User's email.
      */
    @throws[NCE]
    def onUserDelete(firstName: String, lastName: String, email: String): Unit = {
        ensureStarted()
        
        sendPublicEmail(
            "Account deleted",
            USER_DELETE_EMAIL.replace("{{NAME}}", firstName.trim().capitalize),
            email
        )
    }
    
    /**
      * Notification on user account deletion by the admin. Note that user's name and email are provided directly
      * since the user is deleted by the time this function is called.
      *
      * @param firstName User's first name.
      * @param lastName User's last name.
      * @param email User's email.
      */
    @throws[NCE]
    def onUserDeleteByAdmin(firstName: String, lastName: String, email: String): Unit = {
        ensureStarted()
        
        sendPublicEmail(
            "ABCDE account deleted",
            USER_DELETE_BY_ADMIN_EMAIL.replace("{{NAME}}", firstName.trim().capitalize),
            email
        )
    }
    
    /**
      * Callback on uptime monitor failure.
      *
      * @param errMsg Uptime monitor error message.
      */
    @throws[NCE]
    def onUptimeFailure(errMsg: String): Unit = {
        ensureStarted()
        
        sendInternalEmail("Uptime monitor failed", s"Uptime monitor failed: <b>$errMsg</b>", html = true)
        sendInternalSms(s"Uptime error: $errMsg")
    }
    
    /**
      * Processes user feedback event.
      *
      * @param userId User ID.
      * @param msg Message of feedback.
      */
    @throws[NCE]
    def onGeneralFeedback(userId: Long, msg: String): Unit = {
        ensureStarted()
        
        forUserWith(userId) { usr ⇒
            val usrName = s"${usr.firstName.trim().capitalize} ${usr.lastName.trim().capitalize}"
            
            sendInternalEmail(s"General feedback from user: $usrName, ${usr.email}", msg)
        }
    }
    
    /**
      * Callback on company deletion.
      *
      * @param compName Company name.
      */
    @throws[NCE]
    def onCompanyDelete(compName: String): Unit = {
        ensureStarted()
        
        sendInternalEmail(s"Company deleted: $compName", "EOM")
    }
    
    /**
      *
      * @param adminFirstName Administrator first name.
      * @param adminLastName Administrator last name.
      * @param adminEmail Administrator email.
      * @param compName Company name.
      * @param firstName New user's first name.
      * @param lastName New user's last name.
      * @param email New user's email.
      * @param passwd Temporary one-time password.
      */
    @throws[NCE]
    def onNewUserAdded(
        adminFirstName: String,
        adminLastName: String,
        adminEmail: String,
        compName: String,
        firstName: String,
        lastName: String,
        email: String,
        passwd: String): Unit = {
        ensureStarted()
        
        val adminName = s"${adminFirstName.trim().capitalize} ${adminLastName.trim().capitalize}"
        val usrName = s"${firstName.trim().capitalize} ${lastName.trim().capitalize}"
        
        // Send welcome email to the user.
        sendPublicEmail(
            "Welcome to ABCDE",
            NEW_USER_WELCOME_EMAIL.
                replace("{{URL}}", WEB_HOST_PORT).
                replace("{{ADMIN_NAME}}", adminName).
                replace("{{ADMIN_EMAIL}}", adminEmail).
                replace("{{PASSWD}}", passwd).
                replace("{{EMAIL}}", email).
                replace("{{EMAIL_ENC}}", URLEncoder.encode(email, "UTF-8")).
                replace("{{NAME}}", firstName.trim().capitalize),
            email
        )
    
        sendInternalEmail(s"New user added: $usrName, $compName", "EOM")
    }

    // TODO: skh
    private def newRestPickToken(email: String) = ???

    /**
      * Notifies user about the answer that was delayed due to curation.
      * 
      * @param question
      * @param firstName
      * @param email
      * @param srvReqId
      */
    @throws[NCE]
    def onNewResultReady(
        question: String,
        firstName: String,
        email: String,
        srvReqId: String
        ): Unit = {
        ensureStarted()
        
        sendPublicEmail(
            "Your answer is ready",
            RESULT_READY_EMAIL.
                replace("{{URL}}", WEB_HOST_PORT).
                replace("{{NAME}}", firstName.trim().capitalize).
                replace("{{PICK}}", newRestPickToken(email)).
                replace("{{SRV_REQ_ID}}", srvReqId).
                replace("{{QUESTION}}", escapeHtml4(question)),
            email
        )
    }
    
    /**
      * Notifies admin user about new pending requests.
      * 
      * @param firstName Admin's first name.
      * @param lastName Admin's last name.
      * @param email Admin's email.
      */
    @throws[NCE]
    def onNewPending(
        firstName: String,
        lastName: String,
        email: String
    ): Unit = {
        ensureStarted()
        
        sendPublicEmail(
            "ABCDE pending question",
            NEW_PENDING_EMAIL.
                replace("{{URL}}", WEB_HOST_PORT).
                replace("{{PICK}}", newRestPickToken(email)).
                replace("{{NAME}}", firstName.trim().capitalize),
            email
        )
    }

    /**
      *
      * @param compName Company name.
      * @param firstName New user's first name.
      * @param lastName New user's last name.
      * @param email New user's email.
      */
    @throws[NCE]
    def onNewCompanyAdded(
        compName: String,
        firstName: String,
        lastName: String,
        email: String): Unit = {
        ensureStarted()
        
        // Send welcome email to the user.
        sendPublicEmail(
            "Welcome to ABCDE",
            NEW_COMPANY_WELCOME_EMAIL.
                replace("{{URL}}", WEB_HOST_PORT).
                replace("{{EMAIL_ENC}}", URLEncoder.encode(email, "UTF-8")).
                replace("{{NAME}}", firstName.trim().capitalize),
            email
        )
        
        sendInternalEmail(s"New company added: $compName", "EOM")
    }
    
    /**
      *
      * @param firstName User's first name.
      * @param email User's email.
      */
    @throws[NCE]
    def onUserGrantAdmin(firstName: String, email: String): Unit = {
        ensureStarted()
        
        sendPublicEmail(
            "ABCDE account update",
            USER_GRANT_ADMIN_EMAIL.replace("{{NAME}}", firstName.trim().capitalize),
            email
        )
    }
    
    /**
      * Password reset verification code sending.
      *
      * @param email Email to send the code to.
      * @param firstName User name.
      * @param code Verification code to send.
      */
    def onPasswordResetVerification(email: String, firstName: String, code: String): Unit = {
        ensureStarted()
        
        sendPublicEmail(
            s"ABCDE password reset verification code: $code",
            PASSWORD_RESET_VERIFY_EMAIL.
                replace("{{NAME}}", firstName.trim().capitalize).
                replace("{{CODE}}", code),
            email
        )
    }
    
    /**
      *
      * @param firstName User's first name.
      * @param email User's email.
      */
    @throws[NCE]
    def onUserDisabled(firstName: String, email: String): Unit = {
        ensureStarted()
        
        sendPublicEmail(
            "ABCDE account update",
            USER_DISABLED_EMAIL.replace("{{NAME}}", firstName.trim().capitalize),
            email
        )
    }
    
    /**
      * Password change notification.
      *
      * @param email Email to send notification to.
      * @param firstName User name.
      */
    def onPasswordChange(email: String, firstName: String): Unit = {
        ensureStarted()
        
        sendPublicEmail(
            s"ABCDE password changed",
            PASSWORD_CHANGE_EMAIL.
                replace("{{NAME}}", firstName.trim().capitalize),
            email
        )
    }
    
    /**
      *
      * @param firstName User's first name.
      * @param email User's email.
      */
    @throws[NCE]
    def onUserRevokeAdmin(firstName: String, email: String): Unit = {
        ensureStarted()
        
        sendPublicEmail(
            "ABCDE account update",
            USER_REVOKE_ADMIN_EMAIL.replace("{{NAME}}", firstName.trim().capitalize),
            email
        )
    }
    
    /**
      *
      * @param firstName User's first name.
      * @param email User's email.
      */
    @throws[NCE]
    def onUserEnabled(firstName: String, email: String): Unit = {
        ensureStarted()
        
        sendPublicEmail(
            "ABCDE account update",
            USER_ENABLED_EMAIL.replace("{{NAME}}", firstName.trim().capitalize),
            email
        )
    }
}
