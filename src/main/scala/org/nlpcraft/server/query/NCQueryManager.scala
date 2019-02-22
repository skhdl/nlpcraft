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

package org.nlpcraft.server.query

import java.sql.Timestamp

import org.apache.ignite.IgniteCache
import org.nlpcraft._
import org.nlpcraft.server.apicodes.NCApiStatusCode._
import org.nlpcraft.server.ds.NCDsManager
import org.nlpcraft.server.endpoints.NCEndpointManager
import org.nlpcraft.server.ignite.NCIgniteHelpers._
import org.nlpcraft.server.ignite.NCIgniteNLPCraft
import org.nlpcraft.server.mdo.NCQueryStateMdo
import org.nlpcraft.server.nlp.enrichers.NCNlpEnricherManager
import org.nlpcraft.server.notification.NCNotificationManager
import org.nlpcraft.server.probe.NCProbeManager
import org.nlpcraft.server.proclog.NCProcessLogManager
import org.nlpcraft.server.tx.NCTxManager
import org.nlpcraft.server.user.NCUserManager

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.Exception._

/**
  * Query state machine.
  */
object NCQueryManager extends NCLifecycle("Query manager") with NCIgniteNLPCraft {
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
      * @param usrAgent
      * @param rmtAddr
      * @return
      */
    @throws[NCE]
    def ask(
        usrId: Long,
        txt: String,
        dsId: Long,
        isTest: Boolean,
        usrAgent: Option[String],
        rmtAddr: Option[String]
    ): String = {
        ensureStarted()
        
        val txt0 = txt.trim()
        
        val rcvTstamp = new Timestamp(G.nowUtcMs())
        
        // Check user.
        val usr = NCUserManager.getUser(usrId).getOrElse(throw new NCE(s"Unknown user ID: $usrId"))
    
        // Check data source.
        val ds = NCDsManager.getDataSource(dsId).getOrElse(throw new NCE(s"Unknown data source ID: $dsId"))
        
        // Check input length.
        if (txt0.split(" ").length > MAX_WORDS)
            throw new NCE(s"User input is too long (max is $MAX_WORDS words).")
        
        val srvReqId = G.genGuid()
    
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
                userAgent = usrAgent,
                remoteAddress = rmtAddr,
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
            usrAgent.orNull,
            rmtAddr.orNull,
            rcvTstamp
        )

        val fut = Future {
            NCNotificationManager.addEvent("NC_NEW_QRY",
                "userId" → usrId,
                "dsId" → dsId,
                "modelId" → ds.modelId,
                "txt" → txt0,
                "userAgent" → usrAgent,
                "rmtAddr" → rmtAddr,
                "isTest" → isTest
            )
    
            // Enrich the user input and send it to the probe.
            NCProbeManager.askProbe(
                srvReqId,
                usr,
                ds,
                txt0,
                NCNlpEnricherManager.enrich(txt0),
                usrAgent,
                rmtAddr,
                isTest
            )
        }
        
        fut onFailure {
            case e: Throwable ⇒
                logger.error(s"System error processing query: ${e.getLocalizedMessage}", e)
                
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
        
        val now = new Timestamp(G.nowUtcMs())
    
        val found = catching(wrapIE) {
            cache(srvReqId) match {
                case Some(copy) ⇒
                    copy.updateTstamp = now
                    copy.status = QRY_READY
                    copy.error = Some(errMsg)

                    cache += srvReqId → copy

                    processEndpoint(copy.userId, ep ⇒ NCEndpointManager.addNotification(copy, ep))

                    true

                case None ⇒
                    // Safely ignore missing status (cancelled before).
                    ignore(srvReqId)

                    false
            }
        }
        
        if (found) {
            NCProcessLogManager.updateReady(
                srvReqId,
                now,
                errMsg = Some(errMsg)
            )
            
            NCNotificationManager.addEvent("NC_ERROR_QRY",
                "srvReqId" → srvReqId,
                "errMsg" → errMsg
            )
        }
    }
    
    /**
      * 
      * @param srvReqId
      * @param resType
      * @param resBody
      */
    @throws[NCE]
    def setResult(srvReqId: String, resType: String, resBody: String): Unit = {
        ensureStarted()
        
        val now = new Timestamp(G.nowUtcMs())
        
        val found = catching(wrapIE) {
            cache(srvReqId) match {
                case Some(copy) ⇒
                    copy.updateTstamp = now
                    copy.status = QRY_READY
                    copy.resultType = Some(resType)
                    copy.resultBody = Some(resBody)

                    cache += srvReqId → copy

                    processEndpoint(copy.userId, ep ⇒ NCEndpointManager.addNotification(copy, ep))

                    true
                case None ⇒
                    // Safely ignore missing status (cancelled before).
                    ignore(srvReqId)

                    false
            }
        }
        
        if (found) {
            NCProcessLogManager.updateReady(
                srvReqId,
                now,
                resType = Some(resType),
                resBody = Some(resBody)
            )
            
            NCNotificationManager.addEvent("NC_RESULT_QRY",
                "srvReqId" → srvReqId,
                "resType" → resType,
                "resBody" → resBody
            )
        }
    }

    /**
      *
      * @param srvReqId
      */
    private def ignore(srvReqId: String): Unit =
        logger.warn(s"Server request not found - safely ignoring (expired or cancelled): $srvReqId")

    /**
      * Executes function with endpoint if it is found for user.
      *
      * @param usrId USer ID.
      * @param f Function.
      */
    private def processEndpoint(usrId: Long, f: String ⇒ Unit): Unit =
        NCUserManager.getUserEndpoint(usrId) match {
            case Some(ep) ⇒ f(ep)
            case None ⇒ // No-op
        }

    /**
      *
      * @param srvReqIds
      */
    @throws[NCE]
    def cancel(srvReqIds: Set[String]): Unit = {
        ensureStarted()

        val now = new Timestamp(G.nowUtcMs())

        val userSrvReqIds =
            catching(wrapIE) {
                NCTxManager.startTx {
                    cache --= srvReqIds

                    srvReqIds.
                        flatMap(srvReqId ⇒ cache(srvReqId)).
                        groupBy(_.userId).
                        map { case (usrId, data) ⇒ usrId → data.map(_.srvReqId) }

                }
            }
        userSrvReqIds.foreach {
            case (usrId, usrSrvReqIds) ⇒ processEndpoint(usrId, _ ⇒ NCEndpointManager.cancelNotifications(usrSrvReqIds))
        }

        for (srvReqId ← srvReqIds) {
            NCProcessLogManager.updateCancel(srvReqId, now)

            NCNotificationManager.addEvent("NC_CANCEL_QRY",
                "srvReqId" → srvReqId
            )
        }
    }
    
    /**
      *
      */
    @throws[NCE]
    def check(usrId: Long): Seq[NCQueryStateMdo] = {
        ensureStarted()

        val usr = NCUserManager.getUser(usrId).getOrElse(throw new NCE(s"Unknown user ID: $usrId"))

        catching(wrapIE) {
            (if (usr.isAdmin) cache.values else cache.values.filter(_.userId == usrId)).toSeq
        }
    }

    /**
      *
      * @param srvReqIds
      */
    @throws[NCE]
    def get(srvReqIds: Set[String]): Set[NCQueryStateMdo] = {
        ensureStarted()

        catching(wrapIE) {
            srvReqIds.flatMap(id ⇒ cache(id))
        }
    }

    /**
      *
      * @param srvReqId
      */
    @throws[NCE]
    def contains(srvReqId: String): Boolean = {
        ensureStarted()

        catching(wrapIE) {
            cache.containsKey(srvReqId)
        }
    }
}
