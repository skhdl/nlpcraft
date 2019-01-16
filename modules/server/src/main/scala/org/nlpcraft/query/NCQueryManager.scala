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
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
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
import org.nlpcraft._
import org.nlpcraft.apicodes.NCApiStatusCode._
import org.nlpcraft.ds.NCDsManager
import org.nlpcraft.ignite.NCIgniteHelpers._
import org.nlpcraft.ignite.NCIgniteNlpCraft
import org.nlpcraft.mdo.NCQueryStateMdo
import org.nlpcraft.nlp.enrichers.NCNlpEnricherManager
import org.nlpcraft.notification.NCNotificationManager
import org.nlpcraft.probe.NCProbeManager
import org.nlpcraft.proclog.NCProcessLogManager
import org.nlpcraft.tx.NCTxManager
import org.nlpcraft.user.NCUserManager

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.Exception._

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
      * @param userAgent
      * @param remoteAddr
      * @throws
      * @return
      */
    @throws[NCE]
    def ask(
        usrId: Long,
        txt: String,
        dsId: Long,
        isTest: Boolean,
        userAgent: Option[String],
        remoteAddr: Option[String]
    ): String = {
        ensureStarted()
        
        val txt0 = txt.trim()
        
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
        if (txt0.split(" ").length > MAX_WORDS)
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
                    text = txt0,
                    userAgent = userAgent,
                    remoteAddress = remoteAddr,
                    createTstamp = rcvTstamp,
                    updateTstamp = rcvTstamp
                )
            }

            // Add processing log.
            NCProcessLogManager.newEntry(
                usrId,
                srvReqId,
                txt0,
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
                "txt" → txt0,
                "isTest" → isTest
            )
    
            // Enrich the user input and send it to the probe.
            NCProbeManager.forwardToProbe(usr, ds, txt0, NCNlpEnricherManager.enrich(txt0))
        }
        
        fut onFailure {
            case e: Throwable ⇒
                logger.error(s"System error processing query: ${e.getLocalizedMessage}")
                
                setError(srvReqId, "Processing failed due to a system error.")
                
        }
        
        srvReqId
    }
    
    /**
      *
      * @param srvReqId
      * @param errMsg
      */
    @throws[NCE]
    def setError(srvReqId: String, errMsg: String): Unit = {
        ensureStarted()
        
        val now = System.currentTimeMillis()
    
        val found = catching(wrapIE) {
            NCTxManager.startTx {
                cache(srvReqId) match {
                    case Some(copy) ⇒
                        copy.updateTstamp = now
                        copy.status = QRY_READY
                        copy.error = Some(errMsg)
    
                        cache += srvReqId → copy
                        
                        true
                
                    case None ⇒
                        // Safely ignore missing status (cancelled before).
                        ignore(srvReqId)
                        
                        false
                }
            }
        }
        
        if (found)
            NCProcessLogManager.updateReady(
                srvReqId,
                now,
                errMsg = Some(errMsg)
            )
    }
    
    /**
      * 
      * @param srvReqId
      * @param resType
      * @param resBody
      * @param resMeta
      */
    @throws[NCE]
    def setResult(srvReqId: String, resType: String, resBody: String, resMeta: Map[String, Object]): Unit = {
        ensureStarted()
        
        val now = System.currentTimeMillis()
        
        val found = catching(wrapIE) {
            NCTxManager.startTx {
                cache(srvReqId) match {
                    case Some(copy) ⇒
                        copy.updateTstamp = now
                        copy.status = QRY_READY
                        copy.resultType = Some(resType)
                        copy.resultBody = Some(resBody)
                        copy.resultMetadata = Some(resMeta)
                        
                        cache += srvReqId → copy
                        
                        true
                    
                    case None ⇒
                        // Safely ignore missing status (cancelled before).
                        ignore(srvReqId)
                        
                        false
                }
            }
        }
        
        if (found)
            NCProcessLogManager.updateReady(
                srvReqId,
                now,
                resType = Some(resType),
                resBody = Some(resBody)
            )
    }

    /**
      *
      * @param srvReqId
      */
    private def ignore(srvReqId: String): Unit =
        logger.warn(s"Server request not found - safely ignoring (expired or cancelled): $srvReqId")
    
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
        
        setError(srvReqId, error)
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
        
        // TODO
    }
    
    /**
      *
      * @param srvReqIds
      */
    @throws[NCE]
    def cancel(srvReqIds: List[String]): Unit = {
        ensureStarted()
        
        val now = System.currentTimeMillis()
    
        NCTxManager.startTx {
            for (srvReqId ← srvReqIds) {
                catching(wrapIE) {
                    cache -= srvReqId
                }
                
                NCProcessLogManager.updateCancel(
                    srvReqId,
                    now
                )
            }
        }
    }
    
    /**
      *
      */
    @throws[NCE]
    def check(): Unit = {
        ensureStarted()
    
        // TODO
    }
}
