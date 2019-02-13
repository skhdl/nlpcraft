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

package org.nlpcraft.endpoints

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.google.gson.Gson
import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.ignite.IgniteCache
import org.apache.ignite.cache.query.SqlQuery
import org.nlpcraft.ignite.NCIgniteHelpers._
import org.nlpcraft.ignite.NCIgniteNLPCraft
import org.nlpcraft.mdo.NCQueryStateMdo
import org.nlpcraft.query.NCQueryManager
import org.nlpcraft.util.NCGlobals
import org.nlpcraft.{NCConfigurable, NCE, NCLifecycle, _}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.Exception.catching

/**
  * Endpoints manager.
  */
object NCEndpointManager extends NCLifecycle("Endpoints manager") with NCIgniteNLPCraft {
    private object Config extends NCConfigurable {
        val maxQueueSize: Int = hocon.getInt("endpoint.max.queue.size")
        val maxQueueUserSize: Int = hocon.getInt("endpoint.max.queue.per.user.size")
        val maxQueueCheckPeriodMs: Long = hocon.getLong("endpoint.max.queue.check.period.mins") * 60 * 1000
        val delaysMs: Seq[Long] = hocon.getLongList("endpoint.delaysSecs").toSeq.map(p ⇒ p * 1000)
        val delaysCnt: Int = delaysMs.size

        override def check(): Unit = {
            require(maxQueueSize > 0, s"Parameter `maxQueueSize` must be positive: $maxQueueSize")
            require(maxQueueUserSize > 0, s"Parameter `maxQueueUserSize` must be positive: $maxQueueUserSize")
            require(maxQueueCheckPeriodMs > 0, s"Parameter `maxQueueCheckPeriodMs` must be positive: $maxQueueCheckPeriodMs")
            require(delaysMs.nonEmpty, s"Parameters `delaysMs` shouldn't be empty")

            delaysMs.foreach(delayMs ⇒ require(delayMs > 0, s"Parameter `delayMs` must be positive: $delayMs"))
        }
    }

    Config.check()

    // Should be the same as REST '/check' response.
    // Note, it cannot be declared inside methods because GSON requirements.
    case class QueryStateJs(
        srvReqId: String,
        usrId: Long,
        dsId: Long,
        mdlId: String,
        probeId: String,
        status: String,
        resType: String,
        resBody: String,
        error: String,
        createTstamp: Long,
        updateTstamp: Long
    )

    private final val mux = new Object

    private final val GSON = new Gson

    @volatile private var sleepTime = Long.MaxValue
    @volatile private var cache: IgniteCache[String, NCEndpointCacheValue] = _
    @volatile private var sender: Thread = _
    @volatile private var cleaner: ScheduledExecutorService = _
    @volatile private var httpClient: CloseableHttpClient = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        cache = ignite.cache[String, NCEndpointCacheValue]("endpoint-cache")

        require(cache != null)

        httpClient = HttpClients.createDefault

        sender =
            NCGlobals.mkThread("endpoint-notifier-sender-thread") {
                thread ⇒ {
                    while (!thread.isInterrupted) {
                        mux.synchronized {
                            val t = sleepTime

                            if (t > 0)
                                mux.wait(t)

                            sleepTime = Long.MaxValue
                        }

                        val t = now()

                        val query: SqlQuery[String, NCEndpointCacheValue] =
                            new SqlQuery(
                                classOf[NCEndpointCacheValue],
                                "SELECT * FROM NCEndpointCacheValue WHERE sendTime <= ?"
                            )

                        query.setArgs(List(t).map(_.asInstanceOf[java.lang.Object]): _*)

                        val readyData = cache.query(query).getAll.asScala.map(p ⇒ p.getKey → p.getValue).toMap

                        logger.trace(s"Records for sending: ${readyData.size}")

                        readyData.
                            groupBy(_._2.getUserId).
                            foreach { case (usrId, data) ⇒
                                val values = data.values.toSeq

                                require(values.nonEmpty)

                                send(usrId, values.head.getEndpoint, values)
                            }
                    }
                }
            }

        sender.start()

        cleaner = Executors.newSingleThreadScheduledExecutor

        cleaner.scheduleWithFixedDelay(
            () ⇒ clean(), Config.maxQueueCheckPeriodMs, Config.maxQueueCheckPeriodMs, TimeUnit.MILLISECONDS
        )

        super.start()
    }

    private def now(): Long = System.currentTimeMillis()

    private def clean(): Unit = {
        // Clears cache for each user.
        val query: SqlQuery[String, NCEndpointCacheValue] =
            new SqlQuery(
                classOf[NCEndpointCacheValue],
                    s"""
                    |SELECT v.*
                    |FROM
                    |    (SELECT DISTINCT userId FROM NCEndpointCacheValue) u,
                    |    (SELECT v1.* FROM v1
                    |    WHERE
                    |        v.srvReqId NOT IN (
                    |            SELECT v2.srvReqId
                    |            FROM NCEndpointCacheValue v2
                    |            WHERE v2.userId = v1.userId
                    |            ORDER BY v2.createdOn DESC
                    |            LIMIT ${Config.maxQueueUserSize}
                    |        )
                    |    ) v
                    """.stripMargin
            )

        val srvReqIds = cache.query(query).getAll.asScala.map(_.getKey).toSet

        if (srvReqIds.nonEmpty) {
            logger.warn(s"Too big users endpoints queue. Some notifications deleted: $srvReqIds")

            cache --= srvReqIds
        }

        // Clears summary cache.
        if (cache.size() > Config.maxQueueSize) {
            val query: SqlQuery[String, NCEndpointCacheValue] =
                new SqlQuery(
                    classOf[NCEndpointCacheValue],
                        s"""
                        |SELECT *
                        |FROM NCEndpointCacheValue
                        |WHERE srvReqId NOT IN (
                        |    SELECT srvReqId
                        |    FROM NCEndpointCacheValue
                        |    ORDER BY createdOn DESC
                        |    LIMIT ${Config.maxQueueSize}
                        |)
                        """.stripMargin
                )

            val srvReqIds = cache.query(query).getAll.asScala.map(_.getKey).toSet

            logger.warn(s"Too big summary endpoints queue. Some notifications deleted: $srvReqIds")

            cache --= srvReqIds
        }
    }

    private def send(usrId: Long, ep: String, values: Seq[NCEndpointCacheValue]): Unit = {
        val seq = values.map(p ⇒ {
            val s = p.getState

            QueryStateJs(
                s.srvReqId,
                s.userId,
                s.dsId,
                s.modelId,
                s.probeId.orNull,
                s.status,
                s.resultType.orNull,
                s.resultBody.orNull,
                s.error.orNull,
                s.createTstamp.getTime,
                s.updateTstamp.getTime
            )
        })

        NCGlobals.asFuture(
        _ ⇒ {
            val post = new HttpPost(ep)

            try {
                post.setHeader("Content-Type", "application/json")
                post.setEntity(new StringEntity(GSON.toJson(seq.asJava)))

                httpClient.execute(
                    post,
                    new ResponseHandler[Unit] {
                        override def handleResponse(resp: HttpResponse): Unit = {
                            val code = resp.getStatusLine.getStatusCode

                            if (code != 200)
                                throw new NCE(s"Unexpected result [userId=$usrId, endpoint=$ep, code=$code]")
                        }
                    }
                )
            }
            finally {
                post.releaseConnection()
            }
        },
        {
            case e: Exception ⇒
                val t = now()

                val sendAgain =
                    values.flatMap(v ⇒
                        if (NCQueryManager.contains(v.getSrvReqId)) {
                            val i = v.getAttempts
                            val delay = if (i < Config.delaysCnt) Config.delaysMs(i) else Config.delaysMs.last
                            val s = v.getState

                            val nextValue = new NCEndpointCacheValue(
                                s, v.getEndpoint, t + delay, i + 1, v.getCreatedOn, s.userId, s.srvReqId
                            )

                            Some(v.getSrvReqId → nextValue)
                        }
                        else
                            None
                    ).toMap

                if (sendAgain.nonEmpty) {
                    val sleepTime = sendAgain.map(_._2.getSendTime).min - now()

                    mux.synchronized {
                        this.sleepTime = sleepTime

                        mux.notifyAll()
                    }
                }

                logger.warn(
                    s"Error sending notification " +
                        s"[userId=$usrId" +
                        s", endpoint=$ep" +
                        s", returnedToCache=${sendAgain.size}" +
                        s", error=${e.getLocalizedMessage}" +
                        s"]"
                )
        },
        (_: Unit) ⇒ {
            val set = seq.map(_.srvReqId).toSet

            cache --= set

            logger.trace(s"Endpoint notifications sent [userId=$usrId, endpoint=$ep, srvReqIds=$set]")
        }
        )
    }

    /**
      * Stops this component.
      */
    override def stop(): Unit = {
        if (cleaner != null) {
            try
                cleaner.awaitTermination(Long.MaxValue, TimeUnit.MILLISECONDS)
            catch {
                case _: InterruptedException ⇒ logger.warn("Failed to await cleaner timer.")
            }

            cleaner = null
        }

        NCGlobals.stopThread(sender)

        cache = null
        httpClient = null

        super.stop()
    }

    /**
      * Adds event for processing.
      * Note that is executes cache processing in separated thread to not be involved into another transactions.
      *
      * @param state Query state.
      * @param ep Endpoint.
      */
    def addNotification(state: NCQueryStateMdo, ep: String): Unit = {
        require(state != null)
        ensureStarted()

        NCGlobals.asFuture(
            _ ⇒ {
                logger.trace(
                    s"User endpoint notification [userId=${state.userId}, endpoint=$ep, srvReqId=${state.srvReqId}]"
                )

                val t = now()

                catching(wrapIE) {
                    cache +=
                        state.srvReqId →
                        new NCEndpointCacheValue(state, ep, t, 0, t, state.userId, state.srvReqId)
                }

                mux.synchronized {
                    mux.notifyAll()
                }
            },
            {
                case e: Exception ⇒ logger.error(s"Error adding notification [state=$state, ep=$ep]", e)
            }
        )
    }

    /**
      * Cancel notifications for given server request IDs.
      * Note that is executes cache processing in separated thread to not be involved into another transactions.
      * Note that doesn't provide 100% guarantee that notification for server request ID will not be sent.
      *
      * @param srvReqIds Server request IDs.
      */
    def cancelNotification(srvReqIds: Set[String]): Unit = {
        require(srvReqIds != null)

        ensureStarted()

        NCGlobals.asFuture(
            _ ⇒ {
                catching(wrapIE) {
                    cache --= srvReqIds
                }

                logger.trace(s"User endpoint notification cancel [srvReqIds=$srvReqIds]")
            },
            {
                case e: Exception ⇒ logger.error(s"Error cancelling notification [srvReqIds=$srvReqIds]", e)
            }
        )
    }

    /**
      * Cancel notifications for given user ID and endpoint.
      * Note that is executes cache processing in separated thread to not be involved into another transactions.
      * Note that it doesn't provide 100% guarantee that notifications from given user will never be sent.
      *
      * @param usrId User ID.
      */
    def cancelNotifications(usrId: Long): Unit = {
        ensureStarted()

        NCGlobals.asFuture(
            _ ⇒
                catching(wrapIE) {
                    val query: SqlQuery[String, NCEndpointCacheValue] =
                        new SqlQuery(
                            classOf[NCEndpointCacheValue],
                            "SELECT * FROM NCEndpointCacheValue WHERE userId = ?"
                        )

                    query.setArgs(List(usrId).map(_.asInstanceOf[java.lang.Object]): _*)

                    val srvIds = cache.query(query).getAll.asScala.map(_.getKey)

                    logger.trace(s"User endpoint notifications cancel [userId=$usrId, removedReqCnt=${srvIds.size}]")

                    cache --= srvIds.toSet
                },
            {
                case e: Exception ⇒ logger.error(s"Error cancelling notifications [usrId=$usrId]", e)
            }
        )
    }
}
