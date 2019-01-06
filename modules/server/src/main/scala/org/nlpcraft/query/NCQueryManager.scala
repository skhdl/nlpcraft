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
 * Software:    NlpCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
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
object NCQueryManager extends NCLifecycle("Query manager") with NCIgniteNlpCraft {
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
      *
      * @param usrId
      * @param txt
      * @param dsId
      * @param isTest
      */
    @throws[NCE]
    def ask(
        usrId: Long,
        txt: String,
        dsId: Long,
        isTest: Boolean
    ): String = {
        ensureStarted()
        
        ""
    }
    
    /**
      *
      * @param srvReqId
      * @param error
      */
    @throws[NCE]
    def reject(
        srvReqId: String,
        error: String
    ): Unit = {
        ensureStarted()
    }
    
    /**
      *
      * @param usrId
      * @param dsId
      */
    @throws[NCE]
    def clearConversation(
        usrId: Long,
        dsId: Long
    ): Unit = {
        ensureStarted()
    }

    /**
      *
      * @param srvReqId
      * @param curateTxt
      * @param curateHint
      */
    @throws[NCE]
    def curate(
        srvReqId: String,
        curateTxt: String,
        curateHint: String): Unit = {
        ensureStarted()
    }
    
    /**
      *
      * @param srvReqId
      * @param talkback
      */
    @throws[NCE]
    def talkback(
        srvReqId: String,
        talkback: String
    ): Unit = {
        ensureStarted()
    }
    
    /**
      *
      * @param srvReqIds
      */
    @throws[NCE]
    def cancel(srvReqIds: List[String]): Unit = {
        ensureStarted()
    }
    
    /**
      *
      */
    @throws[NCE]
    def check(): Unit = {
        ensureStarted()
    }
    
    /**
      *
      */
    @throws[NCE]
    def pending(): Unit = {
        ensureStarted()
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
    private def enlist(
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
                                status = QRY_ENLISTED, // Initial status.
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
                                QRY_ENLISTED,
                                test
                            )
                        
                        case None ⇒ throw new NCE(s"Unknown user ID: $usrId")
                    }
                }
            }
        }
    }
}
