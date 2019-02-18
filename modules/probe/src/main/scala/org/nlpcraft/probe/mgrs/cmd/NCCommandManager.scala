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

package org.nlpcraft.probe.mgrs.cmd

import java.io.Serializable

import org.nlpcraft._
import org.nlpcraft.nlp.NCNlpSentence
import org.nlpcraft.probe.mgrs.exit.NCExitManager
import org.nlpcraft.probe.mgrs.nlp.conversation.NCConversationManager
import org.nlpcraft.probe.mgrs.nlp.NCProbeNlpManager
import org.nlpcraft.probe.{NCProbeManager, NCProbeMessage}

/**
  * Probe commands processor.
  */
object NCCommandManager extends NCProbeManager("Commands manager") with NCDebug {
    /**
      *
      * @param msg Server message to process.
      */
    def processServerMessage(msg: NCProbeMessage): Unit = {
        ensureStarted()
    
        if (msg.getType != "S2P_PING")
            logger.trace(s"Probe server message received: $msg")
        
        try
            msg.getType match {
                case "S2P_PING" ⇒ ()

                case "S2P_CLEAR_CONV" ⇒
                    NCConversationManager.get(
                        msg.data[Long]("usrId"),
                        msg.data[Long]("dsId")
                    ).clear(_ ⇒ true)
                
                case "S2P_ASK" ⇒
                    NCProbeNlpManager.ask(
                        srvReqId = msg.data[String]("srvReqId"),
                        txt = msg.data[String]("txt"),
                        nlpSen = msg.data[NCNlpSentence]("nlpSen"),
                        usrId = msg.data[Long]("userId"),
                        senMeta = msg.data[Map[String, Serializable]]("senMeta"),
                        dsId = msg.data[Long]("dsId"),
                        dsModelId = msg.data[String]("dsModelId"),
                        dsName = msg.data[String]("dsName"),
                        dsDesc = msg.data[String]("dsDesc"),
                        dsModelCfg = msg.dataOpt[String]("dsModelCfg").orNull,
                        test = msg.data[Boolean]("test")
                    )

                case _ ⇒
                    logger.error(s"Received unknown server message (you need to update the probe): ${msg.getType}")
            }
        catch {
            case e: Throwable ⇒ logger.error(s"Error while processing server message (ignoring): ${msg.getType}", e)
        }
    }
}
