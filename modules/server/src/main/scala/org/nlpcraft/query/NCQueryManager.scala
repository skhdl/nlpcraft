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
import org.nlpcraft.ds.NCDsManager
import org.nlpcraft.nlp.enrichers.NCNlpEnricherManager
import org.nlpcraft.notification.NCNotificationManager
import org.nlpcraft.proclog.NCProcessLogManager
import org.nlpcraft.user.NCUserManager

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Query state machine.
  */
object NCQueryManager extends NCLifecycle("Query manager") with NCIgniteNlpCraft {
    @volatile private var cache: IgniteCache[String/*Server request ID*/, NCQueryStateMdo] = _
    
    private final val MAX_WORDS = 100
    
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
        
        val origTxt = txt.trim()
        
        val rcvTstamp = System.currentTimeMillis()
        
        // Check user.
        val usr = NCUserManager.getUser(usrId) match {
            case Some(x) ⇒ x
            case None ⇒ throw new NCE(s"Unknown user ID: $usrId")
        }
    
        // Check data source.
        val ds = NCDsManager.getDataSource(dsId) match {
            case Some(x) ⇒ x
            case None ⇒ throw new NCE(s"Unknown data source ID: $dsId")
        }
        
        // Check input length.
        if (origTxt.split(" ").length > MAX_WORDS)
            throw new NCE(s"User input is too long (max is $MAX_WORDS words).")
        
        val srvReqId = G.genGuid()
    
        NCTxManager.startTx {
            catching(wrapIE) {
                // Enlist for tracking.
                cache += srvReqId → NCQueryStateMdo(
                    srvReqId,
                    isTest,
                    dsId = dsId,
                    modelId = ds.modelId,
                    userId = usrId,
                    email = usr.email,
                    status = QRY_ENLISTED, // Initial status.
                    origText = origTxt,
                    createTstamp = rcvTstamp,
                    updateTstamp = rcvTstamp
                )
            }

            // Add processing log.
            NCProcessLogManager.newEntry(
                usrId,
                srvReqId,
                origTxt,
                dsId,
                ds.modelId,
                QRY_ENLISTED,
                isTest,
                rcvTstamp
            )
        }
    
        val fut = Future {
            NCNotificationManager.addEvent("NC_NEW_QRY",
                "userId" → usrId,
                "dsId" → dsId,
                "modelId" → ds.modelId,
                "txt" → origTxt,
                "isTest" → isTest
            )
            
            val nlpSen = NCNlpEnricherManager.enrich(origTxt)
    
            // TODO: send to the probe
        }
        
        fut onFailure {
            case e: Throwable ⇒
                logger.error(s"System error processing query: ${e.getLocalizedMessage}")
                
                setErrorResult(srvReqId, "Processing failed due to a system error.")
                
        }
        
        srvReqId
    }
    
    /**
      *
      * @param srvReqId
      * @param errMsg
      */
    @throws[NCE]
    def setErrorResult(srvReqId: String, errMsg: String): Unit = {
        ensureStarted()
        
        // TODO
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
}
