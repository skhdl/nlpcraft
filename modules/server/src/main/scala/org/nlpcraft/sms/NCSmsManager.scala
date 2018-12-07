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

package org.nlpcraft.sms

import java.util.concurrent.LinkedBlockingQueue

import com.twilio.Twilio
import com.twilio.`type`.PhoneNumber
import com.twilio.rest.api.v2010.account.Message
import org.nlpcraft.{NCConfigurable, NCE, NCLifecycle, _}

/**
 * SMS sender.
 */
object NCSmsManager extends NCLifecycle("Core SMS manager") {
    // Type safe and eager settings containers.
    // 'Config' object can be used in tests to provide manual overrides to
    // Hocon-based configuration.
    private[sms] object Config extends NCConfigurable {
        var accountSid: String = hocon.getString("sms.account.sid")
        var authToken: String = hocon.getString("sms.auth.tkn")
        var from: String = hocon.getString("sms.from")
        var delaySec: Long = hocon.getLong("sms.delay.sec")
        val maxQueueSize: Int = hocon.getInt("sms.max.queue.size")
    
        override def check(): Unit = {
            // No-op.
        }
    }

    Config.check()

    case class Holder(to: String, message: String, errCallback: NCE ⇒ Unit)

    private val queue = new LinkedBlockingQueue[Holder](Config.maxQueueSize)

    @volatile private var sender: Thread = _

    /**
     * Starts manager.
     */
    override def start(): NCLifecycle = {
        ensureStopped()
        
        Twilio.init(Config.accountSid, Config.authToken)
        
        sender = G.mkThread("sms-sender-thread") { t ⇒
            val delay = Config.delaySec * 1000

            while (!t.isInterrupted) {
                queue.synchronized {
                    if (!queue.isEmpty) Some(queue.poll()) else None
                } match {
                    case Some(h) ⇒
                        try {
                            Message.creator(
                                Config.accountSid,
                                new PhoneNumber(h.to),
                                new PhoneNumber(Config.from),
                                h.message
                            ).create()
    
                            logger.trace(s"SMS sent to '${h.to}' with text: '${h.message}'")
                        }
                        catch {
                            // Catches all exceptions to avoid stopping thread.
                            case e: Throwable ⇒
                                // TODO: add failover logic.
                                logger.error(s"SMS not sent to: '${h.to}', text: '${h.message}'", e)

                                h.errCallback(new NCE("Error sending SMS.", e))
                        }
                    case None ⇒ // No-op.
                }

                // Sleep for a timeout.
                G.sleep(delay)
            }

            queue.synchronized {
                if (!queue.isEmpty)
                    logger.warn(s"Lost un-sent SMS message(s): ${queue.size}")
            }
        }

        sender.start()

        super.start()
    }

    /**
     * Stops the manager.
     */
    override def stop(): Unit = {
        checkStopping()

        sender.interrupt()
        sender.join()

        // Delete unsent messages.
        queue.clear()

        super.stop()
    }

    /**
     * Asynchronously sends SMS. Returns `true` if message was successfully added to queue,
     * `false` otherwise.
     *
     * @param to Recipient phone number.
     * @param msg Text message.
     * @param errCallback Callback in case of exception.
     */
    def send(to: String, msg: String, errCallback: NCE ⇒ Unit): Boolean = {
        ensureStarted()
        
        val res = queue.synchronized {
            queue.offer(Holder(to, msg, errCallback))
        }

        if (res)
            logger.trace(s"SMS to '$to' added to the queue.")
        else
            logger.error(s"SMS lost due to maximum queue size: ${Config.maxQueueSize}")

        res
    }
}