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

package org.nlpcraft.qrystate

import org.apache.ignite.IgniteCache
import org.nlpcraft.apicodes.NCApiStatusCode._
import org.nlpcraft.cache.NCCacheKey
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.ignite.NCIgniteGeos
import org.nlpcraft.ignite.NCIgniteHelpers._
import org.nlpcraft.mdllib.NCToken
import org.nlpcraft.mdo.NCQueryStateMdo
import org.nlpcraft.notification.NCNotificationManager
import org.nlpcraft.tx.NCTxManager
import org.nlpcraft.{NCLifecycle, _}

import scala.collection.JavaConverters._
import scala.util.control.Exception._

/**
  * Query state machine.
  */
object NCQueryStateManager extends NCLifecycle("SERVER query state manager") with NCIgniteGeos {
    private final val ASK_WAIT_LINGUIST_STR = ASK_WAIT_LINGUIST.toString
    private final val ASK_READY_STR = ASK_READY.toString
    
    @volatile private var cache: IgniteCache[String/*Server request ID*/, NCQueryStateMdo] = _
    @volatile private var notifier: Thread = _
    
    /**
      * Stops this component.
      */
    override def stop(): Unit = {
        checkStopping()
        
        cache = null
    
        if (geosSegment == "rest" && notifier != null)
            G.stopThread(notifier)
        
        super.stop()
    }
    
    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        ensureStopped()
    
        catching(wrapIE) {
            cache = geos.cache[String/*Server request ID*/, NCQueryStateMdo]("core-qry-state-cache")
        }
        
        // Start notifier thread only on REST server.
        if (geosSegment == "rest") {
            notifier = G.mkThread("query-state-notifier") { t ⇒
                val NOTIFIER_DELAY = 15 * 1000 // 15 seconds.
                
                while (!t.isInterrupted) {
                    catching(logIE(logger)) {
                        NCTxManager.startTx {
                            val resNotifies = cache.asScala.map(_.getValue).filter(_.resNotifyDue)
                            val curNotifies = cache.asScala.map(_.getValue).filter(_.curNotifyDue)
                            
                            for (x ← resNotifies) {
                                notifyOnResult(x)
                                
                                x.resNotifyDue = false
                                
                                cache += x.srvReqId → x
                            }
    
                            for (x ← curNotifies) {
                                notifyOnCuration(x)
        
                                x.curNotifyDue = false
        
                                cache += x.srvReqId → x
                            }
                        }
                    }
                    
                    G.sleep(NOTIFIER_DELAY)
                }
            }
            
            notifier.start()
        }
    
        super.start()
    }
    
    /**
      * 
      * @param srvReqId Server request ID.
      * @param usrAgent User agent string.
      * @param dsId Data source ID.
      * @param modelId Data source model ID.
      * @param usrId User ID.
      * @param txt Text.
      * @param origin Origin.
      * @param rmtAddr Remote address.
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
        origin: String,
        rmtAddr: String,
        test: Boolean
    ): Unit = {
        ensureStarted()

        catching(wrapIE) {
            NCTxManager.startTx {
                NCPsql.sql {
                    NCDbManager.getUser(usrId) match {
                        case Some(usr) ⇒
                            val email = usr.email
                            val compId = usr.companyId
                            
                            val now = System.currentTimeMillis()

                            val mdo = NCQueryStateMdo(
                                srvReqId,
                                test,
                                userAgent = usrAgent,
                                companyId = compId,
                                dsId = dsId,
                                modelId = modelId,
                                userId = usrId,
                                email = email,
                                status = ASK_WAIT_REGISTERED.toString, // Initial status.
                                origText = txt,
                                origin = origin,
                                createTstamp = now,
                                updateTstamp = now
                            )
                            cache += srvReqId → mdo
                            
                            // Add input history in case it is a new request.
                            NCDbManager.addHistory(
                                usrId,
                                srvReqId,
                                txt,
                                dsId,
                                usrAgent,
                                origin,
                                rmtAddr,
                                ASK_WAIT_REGISTERED,
                                test
                            )

                        case None ⇒ throw new NCE(s"Unknown user ID: $usrId")
                    }
                }
            }
        }
    }
    
    /**
      *
      * @param mdo
      */
    private def notifyOnResult(mdo: NCQueryStateMdo): Unit =
        // Only send email notification if request was curated and therefore delayed.
        if (mdo.lingOp.isDefined)
            NCPsql.sql {
                NCDbManager.getUser(mdo.userId) match {
                    case Some(usr) ⇒  NCNotificationManager.onNewResultReady(
                        question = mdo.origText,
                        firstName = usr.firstName,
                        email = usr.email,
                        srvReqId = mdo.srvReqId
                    )
                    
                    case None ⇒ logger.error(s"Unknown user ID: ${mdo.userId}")
                }
            }
    
    /**
      *
      * @param mdo
      */
    private def notifyOnCuration(mdo: NCQueryStateMdo): Unit =
        NCPsql.sql {
            NCDbManager.getAdminsForCompany(mdo.companyId).foreach(usr ⇒ {
                NCNotificationManager.onNewPending(
                    firstName = usr.firstName,
                    lastName = usr.lastName,
                    email = usr.email
                )
            })
        }

    /**
      * Tests whether or not given request is being tracked.
      *
      * @param srvReqId Server request ID.
      */
    @throws[NCE]
    def isTracked(srvReqId: String): Boolean = {
        ensureStarted()

        catching(wrapIE) {
            NCTxManager.startTx {
                cache.containsKey(srvReqId)
            }
        }
    }

    /**
      *
      * @param srvReqId
      * @param resType
      * @param resBody
      * @param respType
      * @param resMetadata
      * @param lingUsrId
      * @param lingOp
      */
    @throws[NCE]
    def setResult(
        srvReqId: String,
        resType: String,
        resBody: String,
        respType: String,
        resMetadata: Option[Map[String, Object]] = None,
        lingUsrId: Option[Long] = None,
        lingOp: Option[String] = None
    ): Unit = {
        ensureStarted()

        catching(wrapIE) {
            NCTxManager.startTx {
                cache(srvReqId) match {
                    case Some(copy) ⇒
                        copy.resultType = Some(resType)
                        copy.resultBody = Some(resBody)
                        copy.responseType = respType
                        copy.resultMetadata = resMetadata
                        copy.status = ASK_READY.toString
                        copy.lingUserId = lingUsrId
                        copy.lingOp = lingOp
                        copy.error = None
                        copy.updateTstamp = System.currentTimeMillis()
                        copy.resNotifyDue = true
    
                        cache += srvReqId → copy

                        NCPsql.sql {
                            NCDbManager.updateHistory(
                                srvReqId = srvReqId,
                                resType = Some(resType),
                                resBody = Some(resBody),
                                lingUsrId = lingUsrId,
                                lingOp = lingOp,
                                status = Some(ASK_READY)
                            )
                        }

                    // Safely ignore missing status (cancelled before).
                    case None ⇒ ignore(srvReqId)
                }
            }
        }
    }
    
    /**
      *
      * @param modelId ID of the model being undeployed.
      * @param companyId Company ID of the model being undeployed.
      */
    def setModelUndeploy(modelId: String, companyId: Long): Unit = {
        ensureStarted()
     
        catching(wrapIE) {
            NCTxManager.startTx {
                for (e ← cache.asScala) {
                    val copy = NCQueryStateMdo(e.getValue)
                    
                    if (copy.modelId == modelId && copy.companyId == companyId && copy.status != ASK_READY.toString) {
                        copy.error = Some("Data source model is no longer available.")
                        copy.status = ASK_READY.toString
                        copy.updateTstamp = System.currentTimeMillis()
                        copy.resNotifyDue = true
                        copy.responseType = "ERROR"
                        
                        cache += copy.srvReqId → copy
    
                        NCPsql.sql {
                            NCDbManager.updateHistory(
                                srvReqId = copy.srvReqId,
                                error = copy.error
                            )
                        }
                    }
                }
            }
        }
    }

    /**
      *
      * @param srvReqId Server request ID.
      * @param error Error message.
      * @param respType Response type.
      * @param status New status.
      * @param curateJson JSON request explanation.
      * @param lingUsrId Last linguist user ID.
      * @param lingOp Last linguist operation.
      * @param resMetadata Metadata.
      */
    @throws[NCE]
    def setError(
        srvReqId: String,
        error: Option[String],
        respType: String,
        status: NCApiStatusCode = ASK_READY,
        curateJson: Option[String] = None,
        lingUsrId: Option[Long] = None,
        lingOp: Option[String] = None,
        resMetadata: Option[Map[String, Object]] = None
    ): Unit = {
        ensureStarted()

        require(status == ASK_READY || status == ASK_WAIT_LINGUIST)

        catching(wrapIE) {
            NCTxManager.startTx {
                cache(srvReqId) match {
                    case Some(copy) ⇒
                        copy.error = error
                        copy.responseType = respType
                        copy.status = status.toString
                        copy.lingUserId = lingUsrId
                        copy.lingOp = lingOp
                        copy.curateJson = curateJson
                        copy.resultMetadata = resMetadata
                        copy.updateTstamp = System.currentTimeMillis()
    
                        if (status == ASK_WAIT_LINGUIST)
                            copy.curNotifyDue = true
                        else
                            copy.resNotifyDue = true
    
                        cache += srvReqId → copy
    
                        NCPsql.sql {
                            NCDbManager.updateHistory(
                                srvReqId = srvReqId,
                                error = error,
                                lingUsrId = lingUsrId,
                                lingOp = lingOp,
                                status = Some(status)
                            )
                        }

                    // Safely ignore missing status (cancelled before).
                    case None ⇒ ignore(srvReqId)
                }
            }
        }
    }

    /**
      *
      * @param srvReqId Server request ID.
      * @param respType State type.
      * @param resMetadata Metadata.
      */
    @throws[NCE]
    def setReady(srvReqId: String, respType: String, resMetadata: Option[Map[String, Object]] = None): Unit = {
        ensureStarted()

        catching(wrapIE) {
            NCTxManager.startTx {
                cache(srvReqId) match {
                    case Some(copy) ⇒
                        copy.status = ASK_READY.toString
                        copy.updateTstamp = System.currentTimeMillis()
                        copy.resultMetadata = resMetadata
                        copy.responseType = respType
                        copy.error = None

                        cache += srvReqId → copy

                        NCPsql.sql {
                            NCDbManager.updateHistory(
                                srvReqId = srvReqId,
                                status = Some(ASK_READY)
                            )
                        }

                    // Safely ignore missing status (cancelled before).
                    case None ⇒ ignore(srvReqId)
                }
            }
        }
    }

    /**
      *
      * @param srvReqId Server request ID.
      * @param curateTxt Text.
      * @param curateHintOpt Optional hint.
      * @param lingUsrId Last linguist user ID.
      * @param lingOp Last linguist operation.
      */
    @throws[NCE]
    def setCuration(
        srvReqId: String,
        curateTxt: String,
        curateHintOpt: Option[String] = None,
        lingUsrId: Long,
        lingOp: String
    ): Unit = {
        ensureStarted()

        catching(wrapIE) {
            NCTxManager.startTx {
                cache(srvReqId) match {
                    case Some(copy) ⇒
                        copy.curateText = Some(curateTxt)
                        copy.curateHint = curateHintOpt
                        copy.lingUserId = Some(lingUsrId)
                        copy.lingOp = Some(lingOp)
                        copy.updateTstamp = System.currentTimeMillis()
                        copy.responseType = "RESP_CURATION"

                        cache += srvReqId → copy
    
                        NCPsql.sql {
                            NCDbManager.updateHistory(
                                srvReqId = srvReqId,
                                curateTxt = copy.curateText,
                                curateHint = copy.curateHint,
                                lingUsrId = Some(lingUsrId),
                                lingOp = Some(lingOp)
                            )
                        }

                    // Safely ignore missing status (cancelled before).
                    case None ⇒ ignore(srvReqId)
                }
            }
        }
    }

    /**
      *
      * @param srvReqId Server request ID.
      * @param cacheKeyOpt Cache key.
      *
      */
    @throws[NCE]
    def setCacheKey(srvReqId: String, cacheKeyOpt: Option[NCCacheKey]): Unit = {
        ensureStarted()

        catching(wrapIE) {
            NCTxManager.startTx {
                cache(srvReqId) match {
                    case Some(copy) ⇒
                        copy.cacheKey = cacheKeyOpt
                        copy.updateTstamp = System.currentTimeMillis()
    
                        cache += srvReqId → copy
        
                    // Safely ignore missing status (cancelled before).
                    case None ⇒ ignore(srvReqId)
                }
            }
        }
    }

    /**
      *
      * @param srvReqId Server request ID.
      * @param toks Tokens.
      */
    @throws[NCE]
    def setTokens(srvReqId: String, toks: Option[Seq[NCToken]]): Unit = {
        ensureStarted()

        catching(wrapIE) {
            NCTxManager.startTx {
                cache(srvReqId) match {
                    case Some(copy) ⇒
                        copy.tokens = toks

                        if (copy.origTokens.isEmpty)
                            copy.origTokens = toks

                        copy.updateTstamp = System.currentTimeMillis()

                        cache += srvReqId → copy

                    // Safely ignore missing status (cancelled before).
                    case None ⇒ ignore(srvReqId)
                }
            }
        }
    }

    /**
      *
      * @param srvReqId Server request ID.
      * @param probeId Probe ID to set.
      *
      */
    @throws[NCE]
    def setProbeId(srvReqId: String, probeId: String): Unit = {
        ensureStarted()
        
        catching(wrapIE) {
            NCTxManager.startTx {
                cache(srvReqId) match {
                    case Some(copy) ⇒
                        copy.probeId = Some(probeId)
                        copy.updateTstamp = System.currentTimeMillis()
                        
                        cache += srvReqId → copy
                    
                    // Safely ignore missing status (cancelled before).
                    case None ⇒ ignore(srvReqId)
                }
            }
        }
    }

    /**
      *
      * @param srvReqId Server request ID.
      * @param cacheId Optional cache ID.
      */
    @throws[NCE]
    def setCacheId(srvReqId: String, cacheId: Option[Long]): Unit = {
        ensureStarted()

        catching(wrapIE) {
            NCTxManager.startTx {
                cache(srvReqId) match {
                    case Some(copy) ⇒
                        copy.cacheId = cacheId
                        copy.updateTstamp = System.currentTimeMillis()
    
                        cache += srvReqId → copy
    
                        NCPsql.sql {
                            NCDbManager.updateHistory(
                                srvReqId = srvReqId,
                                cacheId = cacheId
                            )
                        }
        
                    // Safely ignore missing status (cancelled before).
                    case None ⇒ ignore(srvReqId)
                }
            }
        }
    }

    /**
      * @param srvReqId Server request ID.
      */
    def getOpt(srvReqId: String): Option[NCQueryStateMdo] = {
        catching(wrapIE) {
            NCTxManager.startTx {
                cache(srvReqId)
            }
        }
    }

    /**
      *
      * @param srvReqId Server request ID.
      */
    @throws[NCE]
    def delist(srvReqId: String): Boolean = {
        ensureStarted()
    
        catching(wrapIE) {
            NCTxManager.startTx {
                cache -= srvReqId
            }
        }
    }

    /**
      *
      * @param srvReqIds Server request IDs.
      */
    @throws[NCE]
    def delist(srvReqIds: Set[String]): Unit = {
        ensureStarted()

        catching(wrapIE) {
            NCTxManager.startTx {
                cache --= srvReqIds
            }
        }
    }


    /**
      * 
      * @param srvReqId
      */
    private def ignore(srvReqId: String): Unit =
        logger.warn(s"Server request not found - safely ignoring (expired or cancelled): $srvReqId")
    
    /**
      *
      * @param usrId User ID.
      * @param withTest Test flag.
      * @return
      */
    @throws[NCE]
    def pending(usrId: Long, withTest: Boolean): (Long, Seq[NCQueryStateMdo]) = {
        ensureStarted()

        catching(wrapIE) {
            NCTxManager.startTx {
                NCPsql.sql {
                    NCDbManager.getUser(usrId) match {
                        case Some(usr) ⇒
                            val compId = usr.companyId

                            // Get entries for this user or all entries waiting for curation for this
                            // company if the this user is an admin.
                            val seq = cache.asScala.map(_.getValue).filter(mdo ⇒ {
                                (withTest || !mdo.test) &&
                                (
                                    mdo.userId == usrId ||
                                    (usr.isAdmin && mdo.companyId == compId && mdo.status == ASK_WAIT_LINGUIST_STR)
                                )
                            }).toSeq
                            
                            // Only admins can ask for pending curation.
                            if (usr.isAdmin)
                                for (x ← seq.filter(_.status == ASK_WAIT_LINGUIST_STR)) {
                                    cache(x.srvReqId) match {
                                        case Some(copy) ⇒
                                            copy.curNotifyDue = false
                
                                            cache += copy.srvReqId → copy
            
                                        // Safely ignore missing status (cancelled before).
                                        case None ⇒ ignore(x.srvReqId)
                                    }
                                }
    
                            for (x ← seq.filter(_.status == ASK_READY_STR)) {
                                cache(x.srvReqId) match {
                                    case Some(copy) ⇒
                                        copy.resNotifyDue = false
                
                                        cache += copy.srvReqId → copy
            
                                    // Safely ignore missing status (cancelled before).
                                    case None ⇒ ignore(x.srvReqId)
                                }
                            }
                            
                            var hash = 0L
    
                            for (v ← seq.flatMap(x ⇒ Seq(x.srvReqId, x.status, x.updateTstamp)))
                                hash = hash * 31 + v.hashCode()

                            (hash, seq)

                        case None ⇒ throw new NCE(s"Unknown user ID: $usrId")
                    }
                }
            }
        }
    }
}
