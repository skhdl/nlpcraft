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

package org.nlpcraft.probe.mgrs.cmd

import java.io.Serializable

import org.nlpcraft._
import org.nlpcraft.mdllib.NCToken
import org.nlpcraft.nlp.NCNlpSentence
import org.nlpcraft.probe.mgrs.exit.NCExitManager
import org.nlpcraft.probe.mgrs.nlp.conversation.NCConversationManager
import org.nlpcraft.probe.mgrs.nlp.NCProbeNlpManager
import org.nlpcraft.probe.{NCProbeManager, NCProbeMessage}

/**
  * Probe commands processor.
  */
object NCCommandManager extends NCProbeManager("PROBE commands manager") with NCDebug {
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

                case "S2P_STOP_PROBE" ⇒
                    NCExitManager.exit()

                case "S2P_RESTART_PROBE" ⇒
                    NCExitManager.restart()

                case "S2P_CLEAR_CONV" ⇒
                    NCConversationManager.get(msg.data[Long]("usrId"), msg.data[Long]("dsId")).clear(_ ⇒ true)
                
                case "S2P_ASK" ⇒
                    NCProbeNlpManager.ask(
                        srvReqId = msg.data[String]("srvReqId"),
                        origTxt = msg.data[String]("origTxt"),
                        curateTxt = msg.dataOpt[String]("curateTxt"),
                        curateHint = msg.dataOpt[String]("curateHint"),
                        origTokens = msg.dataOpt[Seq[NCToken]]("origTokens"),
                        nlpSen = msg.data[NCNlpSentence]("sentence"),
                        usrId = msg.data[Long]("userId"),
                        senMeta = msg.data[Map[String, Serializable]]("senMeta"),
                        dsId = msg.data[Long]("dsId"),
                        dsModelId = msg.data[String]("dsModelId"),
                        dsName = msg.data[String]("dsName"),
                        dsDesc = msg.data[String]("dsDesc"),
                        dsModelCfg = msg.data[String]("dsModelCfg"),
                        cacheable = msg.data[Boolean]("cacheable"),
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
