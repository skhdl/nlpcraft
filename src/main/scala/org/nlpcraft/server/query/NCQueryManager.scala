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

import org.apache.ignite.IgniteCache
import org.nlpcraft.common.{NCLifecycle, _}
import org.nlpcraft.server.apicodes.NCApiStatusCode._
import org.nlpcraft.server.endpoints.{NCEndpointCacheKey, NCEndpointManager}
import org.nlpcraft.server.ignite.NCIgniteHelpers._
import org.nlpcraft.server.ignite.NCIgniteInstance
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
object NCQueryManager extends NCLifecycle("Query manager") with NCIgniteInstance {
    @volatile private var cache: IgniteCache[String/*Server request ID*/, NCQueryStateMdo] = _
    
    private final val SYNC_ASK_MUX = new Object()
    private final val SYNC_ASK_MAX_WAIT_MS = 20 * 1000 // 20 secs.

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
      * Synchronous handler for `/test/ask` REST call.
      *
      * @param usrId
      * @param txt
      * @param mdlId
      * @param usrAgent
      * @param rmtAddr
      * @param data
      * @return
      */
    @throws[NCE]
    def syncAsk(
        usrId: Long,
        txt: String,
        mdlId: String,
        usrAgent: Option[String],
        rmtAddr: Option[String],
        data: Option[String]
    ): NCQueryStateMdo = {
        ensureStarted()
    
        val srvReqId = U.genGuid()
    
        spawnAskFuture(srvReqId, usrId, txt, mdlId, usrAgent, rmtAddr, data)
    
        var qryState: NCQueryStateMdo = null
        
        val start = System.currentTimeMillis()
        
        catching(wrapIE) {
            var found = false
            
            while (!found)
                cache.values.filter(x ⇒ x.srvReqId == "1"/*srvReqId*/ && x.status == QRY_READY.toString).lastOption match {
                    case None ⇒
                        if (System.currentTimeMillis() - start > SYNC_ASK_MAX_WAIT_MS)
                            throw new NCE(s"Synchronous call timed out (max wait is ${SYNC_ASK_MAX_WAIT_MS}ms).")
    
                        // Busy wait.
                        SYNC_ASK_MUX.synchronized {
                            SYNC_ASK_MUX.wait(1000)
                        }
                        
                    case Some(x) ⇒
                        found = true // Break from the wait.
                        qryState = x
                }
    
            cancel0(Right(Set(srvReqId)))
        }
    
        qryState
    }

    /**
      * Asynchronous handler for `/ask` REST call.
      *
      * @param usrId
      * @param txt
      * @param mdlId
      * @param usrAgent
      * @param rmtAddr
      * @param data
      * @return
      */
    @throws[NCE]
    def asyncAsk(
        usrId: Long,
        txt: String,
        mdlId: String,
        usrAgent: Option[String],
        rmtAddr: Option[String],
        data: Option[String]
    ): String = {
        ensureStarted()

        val srvReqId = U.genGuid()

        spawnAskFuture(srvReqId, usrId, txt, mdlId, usrAgent, rmtAddr, data)
        
        srvReqId
    }
    
    /**
      * @param srvReqId
      * @param usrId
      * @param txt
      * @param mdlId
      * @param usrAgent
      * @param rmtAddr
      * @param data
      * @return
      */
    @throws[NCE]
    private def spawnAskFuture(
        srvReqId: String,
        usrId: Long,
        txt: String,
        mdlId: String,
        usrAgent: Option[String],
        rmtAddr: Option[String],
        data: Option[String]
    ): Unit = {
        val txt0 = txt.trim()
        
        val rcvTstamp = U.nowUtcTs()
        
        // Check user.
        val usr = NCUserManager.getUser(usrId).getOrElse(throw new NCE(s"Unknown user ID: $usrId"))
        
        // Check input length.
        if (txt0.split(" ").length > MAX_WORDS)
            throw new NCE(s"User input is too long (max is $MAX_WORDS words).")
        
        catching(wrapIE) {
            // Enlist for tracking.
            cache += srvReqId → NCQueryStateMdo(
                srvReqId,
                modelId = mdlId,
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
            mdlId,
            QRY_ENLISTED,
            usrAgent.orNull,
            rmtAddr.orNull,
            rcvTstamp,
            data.orNull
        )
        
        Future {
            NCNotificationManager.addEvent("NC_NEW_QRY",
                "userId" → usrId,
                "modelId" → mdlId,
                "txt" → txt0,
                "userAgent" → usrAgent,
                "rmtAddr" → rmtAddr,
                "data" → data
            )
            
            logger.info(s"New request received [" +
                s"txt='$txt0', " +
                s"usr=${usr.firstName} ${usr.lastName} (${usr.email}), " +
                s"mdlId=$mdlId" +
                s"]")
            
            // Enrich the user input and send it to the probe.
            NCProbeManager.askProbe(
                srvReqId,
                usr,
                mdlId,
                txt0,
                NCNlpEnricherManager.enrich(txt0),
                usrAgent,
                rmtAddr,
                data
            )
        } onFailure {
            case e: NCE ⇒
                logger.info(s"Query processing failed: ${e.getLocalizedMessage}", e)
            
                setError(
                    srvReqId,
                    e.getLocalizedMessage,
                    NCErrorCodes.SYSTEM_ERROR
                )
        
            case e: Throwable ⇒
                logger.error(s"System error processing query: ${e.getLocalizedMessage}", e)
            
                setError(
                    srvReqId,
                    "Processing failed due to a system error.",
                    NCErrorCodes.UNEXPECTED_ERROR
                )
        }
    }

    /**
      *
      * @param srvReqId
      * @param errMsg
      * @param errCode
      */
    @throws[NCE]
    def setError(srvReqId: String, errMsg: String, errCode: Int): Unit = {
        ensureStarted()
        
        val now = U.nowUtcTs()
    
        val found = catching(wrapIE) {
            cache(srvReqId) match {
                case Some(copy) ⇒
                    copy.updateTstamp = now
                    copy.status = QRY_READY
                    copy.error = Some(errMsg)
                    copy.errorCode = Some(errCode)

                    cache += srvReqId → copy

                    processEndpoints(copy.userId, eps ⇒ NCEndpointManager.addNotification(copy, eps))

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
        
        val now = U.nowUtcTs()
        
        val found = catching(wrapIE) {
            cache(srvReqId) match {
                case Some(copy) ⇒
                    copy.updateTstamp = now
                    copy.status = QRY_READY
                    copy.resultType = Some(resType)
                    copy.resultBody = Some(resBody)

                    cache += srvReqId → copy

                    processEndpoints(copy.userId, eps ⇒ NCEndpointManager.addNotification(copy, eps))

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
    private def processEndpoints(usrId: Long, f: Set[String] ⇒ Unit): Unit = f(NCUserManager.getUserEndpoints(usrId))

    /**
      *
      * @param arg User ID or server request IDs.
      */
    @throws[NCE]
    def cancel0(arg: Either[Long, Set[String]]): Unit = {
        ensureStarted()

        val now = U.nowUtcTs()

        val (srvReqIds, userSrvReqIds) =
            catching(wrapIE) {
                NCTxManager.startTx {
                    val srvReqIds =
                        if (arg.isLeft)
                            cache.values.filter(_.userId == arg.left.get).map(_.srvReqId).toSet
                        else
                            arg.right.get

                    cache --= srvReqIds

                    val userSrvReqIds =
                        srvReqIds.
                            flatMap(srvReqId ⇒ cache(srvReqId)).
                            groupBy(_.userId).
                            map { case (usrId, data) ⇒ usrId → data.map(_.srvReqId) }

                    (srvReqIds, userSrvReqIds)
                }
            }

        userSrvReqIds.foreach {
            case (usrId, usrSrvReqIds) ⇒
                processEndpoints(
                    usrId, _ ⇒
                    NCEndpointManager.cancelNotifications(usrId, (k: NCEndpointCacheKey) ⇒
                        usrSrvReqIds.contains(k.getSrvReqId)
                    )
                )
        }

        for (srvReqId ← srvReqIds) {
            NCProcessLogManager.updateCancel(srvReqId, now)

            NCNotificationManager.addEvent(
                "NC_CANCEL_QRY",
                "srvReqId" → srvReqId
            )
        }
    }

    /**
      * Handler for `/cancel` REST call.
      *
      * @param srvReqIds Server request IDs.
      */
    @throws[NCE]
    def cancel(srvReqIds: Set[String]): Unit = cancel0(Right(srvReqIds))

    /**
      * Handler for `/cancel` REST call.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def cancel(usrId: Long): Unit = cancel0(Left(usrId))

    /**
      * Handler for `/check` REST call.
      * 
      * @param usrId
      * @param srvReqIdsOpt
      * @param maxRowsOpt
      */
    @throws[NCE]
    def check(usrId: Long, srvReqIdsOpt: Option[Set[String]], maxRowsOpt: Option[Int]): Seq[NCQueryStateMdo] = {
        ensureStarted()

        val usr = NCUserManager.getUser(usrId).getOrElse(throw new NCE(s"Unknown user ID: $usrId"))

        catching(wrapIE) {
            var vals = if (usr.isAdmin) cache.values else cache.values.filter(_.userId == usrId)

            srvReqIdsOpt match {
                case Some(srvReqIds) ⇒ vals = vals.filter(p ⇒ srvReqIds.contains(p.srvReqId))
                case None ⇒ // No-op.
            }

            val res = vals.toSeq.sortBy(-_.createTstamp.getTime)

            maxRowsOpt match {
                case Some(maxRows) ⇒ res.take(maxRows)
                case None ⇒ res
            }
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
      * @param usrId
      */
    @throws[NCE]
    def get(usrId: Long): Set[NCQueryStateMdo] = {
        ensureStarted()

        catching(wrapIE) {
            cache.values.filter(_.userId == usrId).toSet
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
