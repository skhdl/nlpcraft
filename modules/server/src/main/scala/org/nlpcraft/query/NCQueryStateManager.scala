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

package org.nlpcraft.query

import org.apache.ignite.IgniteCache
import org.nlpcraft.ignite.NCIgniteHelpers._
import org.nlpcraft._
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.ignite.NCIgniteNlpCraft
import org.nlpcraft.mdo.NCQueryStateMdo
import org.nlpcraft.tx.NCTxManager

import scala.util.control.Exception._
import org.nlpcraft.apicodes.NCApiStatusCode._

/**
  * Query state machine.
  */
object NCQueryStateManager extends NCLifecycle("Query state manager") with NCIgniteNlpCraft {
    @volatile private var cache: IgniteCache[String/*Server request ID*/, NCQueryStateMdo] = _
    
    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        ensureStopped()
        
        catching(wrapIE) {
            cache = ignite.cache[String/*Server request ID*/, NCQueryStateMdo]("qry-state-cache")
        }
        
        require(cache != null)
        
        super.start()
    }
    
    /**
      * Enlists given server request into state machine.
      * 
      * @param srvReqId Server request ID.
      * @param usrAgent User agent string.
      * @param dsId Data source ID.
      * @param modelId Data source model ID.
      * @param usrId User ID.
      * @param txt Text.
      * @param test Test flag.
      */
    @throws[NCE]
    def enlist(
        srvReqId: String,
        usrAgent: String,
        dsId: Long,
        modelId: String,
        usrId: Long,
        txt: String,
        test: Boolean
    ): Unit = {
        ensureStarted()
        
        catching(wrapIE) {
            NCTxManager.startTx {
                NCPsql.sql {
                    NCDbManager.getUser(usrId) match {
                        case Some(usr) ⇒
                            val email = usr.email
                            
                            val now = System.currentTimeMillis()
                            
                            val mdo = NCQueryStateMdo(
                                srvReqId,
                                test,
                                dsId = dsId,
                                modelId = modelId,
                                userId = usrId,
                                email = email,
                                status = ASK_WAIT_ENLISTED, // Initial status.
                                origText = txt,
                                createTstamp = now,
                                updateTstamp = now
                            )
                            
                            cache += srvReqId → mdo
                            
                            // Add processing log.
                            NCDbManager.addProcessingLog(
                                usrId,
                                srvReqId,
                                txt,
                                dsId,
                                ASK_WAIT_ENLISTED,
                                test
                            )
                        
                        case None ⇒ throw new NCE(s"Unknown user ID: $usrId")
                    }
                }
            }
        }
    }
}
