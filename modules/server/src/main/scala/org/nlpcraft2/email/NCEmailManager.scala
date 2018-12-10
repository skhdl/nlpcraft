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

package org.nlpcraft2.email

import java.util.concurrent.Executors
import java.util.{Date, Properties}

import javax.mail._
import javax.mail.internet.{InternetAddress, MimeMessage}
import org.nlpcraft._
import org.nlpcraft.NCLifecycle
import org.nlpcraft2.NCConfigurable

import scala.concurrent._

/**
 * SMTP email sender.
 */
object NCEmailManager extends NCLifecycle("SERVER email manager") {
    // Type safe and eager settings container.
    private object Config extends NCConfigurable {
        val host: String = hocon.getString("smtp.host")
        val port: Int = hocon.getInt("smtp.port")
        val ssl: Boolean = hocon.getBoolean("smtp.ssl")
        val startTls: Boolean = hocon.getBoolean("smtp.starttls")
        val username: String = hocon.getString("smtp.username")
        val passwd: String = hocon.getString("smtp.password")
    
        override def check(): Unit = {
            require(port > 0 && port < 65535)
        }
    }

    Config.check()
    
    // Special one-thread execution context for email sending.
    // One-by-one execution policy to avoid limit hitting at SMTP server.
    private val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
    
    /**
     * Synchronously sends email.
     *
     * @param host SMTP host.
     * @param port SMTP port.
     * @param ssl SMTP SSL.
     * @param startTls Start TLS flag.
     * @param username SMTP user name.
     * @param passwd SMTP password.
     * @param from From email.
     * @param subj Email subject.
     * @param body Email body.
     * @param html HTML format flag.
     * @param addrs Addresses to send email to.
     * @throws MessagingException Thrown in case when sending email failed.
     */
    @throws[MessagingException]
    private def sendEmail0(
        host: String,
        port: Int,
        ssl: Boolean,
        startTls: Boolean,
        username: String,
        passwd: String,
        from: String,
        subj: String,
        body: String,
        html: Boolean,
        addrs: Seq[String]) {
        val p = new Properties

        p.setProperty("mail.transport.protocol", "smtp")
        p.setProperty("mail.smtp.host", host)
        p.setProperty("mail.smtp.port", port.toString)

        if (ssl) p.setProperty("mail.smtp.ssl", "true")
        if (startTls) p.setProperty("mail.smtp.starttls.enable", "true")

        val ses =
            if (username != null && !username.isEmpty) {
                p.setProperty("mail.smtp.auth", "true")

                Session.getInstance(p, new Authenticator {
                    override def getPasswordAuthentication = new PasswordAuthentication(username, passwd)
                })
            }
            else
                Session.getInstance(p, null)

        val email = new MimeMessage(ses)

        email.setFrom(new InternetAddress(from))
        email.setSubject(subj)
        email.setSentDate(new Date)

        if (html)
            email.setText(body, "UTF-8", "html")
        else
            email.setText(body, "UTF-8")

        email.setRecipients(Message.RecipientType.TO, Array[Address](addrs.map(a ⇒ new InternetAddress(a)): _*))

        Transport.send(email)
    }

    /**
     * Asynchronously sends HTML email.
     *
     * @param from FROM email.
     * @param subj Email subject.
     * @param body Email body.
     * @param to TO address.
     */
    def sendHtmlEmail(from: String, subj: String, body: String, to: String): Future[Unit] = {
        sendEmail(
            from,
            subj,
            body,
            html = true,
            Seq(to),
            logger.error(s"Failed to send HTML email [to=$to, subj=$subj]", _)
        )
    }

    /**
     * Asynchronously sends plain text email.
     *
     * @param from FROM email address.
     * @param subj Email subject.
     * @param body Email body.
     * @param to TO email address.
     */
    def sendPlainEmail(from: String, subj: String, body: String, to: String): Future[Unit] = {
        sendEmail(
            from,
            subj,
            body,
            html = false,
            Seq(to),
            logger.error(s"Failed to send plain email [to=$to, subj=$subj]", _)
        )
    }

    /**
      * Asynchronously sends email with default configuration parameters.
      *
      * @param from From email.
      * @param subj Email subject.
      * @param body Email body.
      * @param html HTML format flag.
      * @param addrs Addresses to send email to.
      * @param onFailure Callback in case of exception.
      * @param onSuccess Callback in case of successful sending.
      */
    def sendEmail(
        from: String,
        subj: String,
        body: String,
        html: Boolean,
        addrs: Seq[String],
        onFailure: Throwable ⇒ Unit,
        onSuccess: Unit ⇒ Unit = _ ⇒ ()): Future[Unit] = {
        ensureStarted()
        
        G.asFuture(
            _ ⇒ sendEmail0(
                Config.host,
                Config.port,
                Config.ssl,
                Config.startTls,
                Config.username,
                Config.passwd,
                from,
                subj,
                body,
                html,
                addrs
            ),
            onFailure,
            onSuccess
        )(ec)
    }
}
