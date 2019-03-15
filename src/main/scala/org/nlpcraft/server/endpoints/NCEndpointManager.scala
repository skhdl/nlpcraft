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

package org.nlpcraft.server.endpoints

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.google.gson.Gson
import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.ignite.IgniteCache
import org.apache.ignite.cache.query.{SqlFieldsQuery, SqlQuery}
import org.nlpcraft.common.{NCLifecycle, _}
import org.nlpcraft.server.NCConfigurable
import org.nlpcraft.server.ignite.NCIgniteHelpers._
import org.nlpcraft.server.ignite.NCIgniteInstance
import org.nlpcraft.server.mdo.NCQueryStateMdo
import org.nlpcraft.server.query.NCQueryManager
import org.nlpcraft.server.tx.NCTxManager

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.Exception.catching

/**
  * Query result notification endpoints manager.
  */
object NCEndpointManager extends NCLifecycle("Endpoints manager") with NCIgniteInstance {
    private object Config extends NCConfigurable {
        final val prefix = "server.endpoint"
        
        val maxQueueSize: Int = getInt(s"$prefix.queue.maxSize")
        val maxQueueUserSize: Int = getInt(s"$prefix.queue.maxPerUserSize")
        val maxQueueCheckPeriodMs: Long = getLong(s"$prefix.queue.checkPeriodMins") * 60 * 1000
        val lifeTimeMins: Long = getLong(s"$prefix.lifetimeMins")
        val lifeTimeMs: Long = lifeTimeMins * 60 * 1000
        val delaysMs: Seq[Long] = getLongList(s"$prefix.delaysSecs").toSeq.map(_ * 1000)
        val delaysCnt: Int = delaysMs.size

        override def check(): Unit = {
            if (maxQueueSize <= 0)
                abortError(s"Configuration parameter '$prefix.queue.maxSize' must > 0: $maxQueueSize")
            if (maxQueueUserSize <= 0)
                abortError(s"Configuration parameter '$prefix.queue.maxPerUserSize' must > 0: $maxQueueUserSize")
            if (maxQueueCheckPeriodMs <= 0)
                abortError(s"Configuration parameter '$prefix.queue.checkPeriodMins' must > 0: $maxQueueCheckPeriodMs")
            if (lifeTimeMins <= 0)
                abortError(s"Configuration parameter '$prefix.lifetimeMins' must > 0: $lifeTimeMins")
            if (delaysMs.isEmpty)
                abortError(s"Configuration parameter '$prefix.delaysSecs' cannot be empty.")
            delaysMs.foreach(delayMs ⇒
                if (delayMs <= 0)
                    abortError(s"Configuration parameter '$prefix.delaysSecs' must contain only positive values: $delayMs")
            )
        }
    }

    Config.check()

    // Should be the same as REST '/check' response.
    // Note, it cannot be declared inside methods because GSON requirements.
    case class QueryStateJs(
        srvReqId: String,
        txt: String,
        usrId: Long,
        dsId: Long,
        mdlId: String,
        probeId: String,
        status: String,
        resType: String,
        resBody: Object,
        error: String,
        createTstamp: Long,
        updateTstamp: Long
    )

    private final val mux = new Object

    private final val GSON = new Gson

    @volatile private var work = false

    @volatile private var cache: IgniteCache[String, NCEndpointCacheValue] = _
    @volatile private var sender: Thread = _
    @volatile private var cleaner: ScheduledExecutorService = _
    @volatile private var httpCli: CloseableHttpClient = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        catching(wrapIE) {
            cache = ignite.cache[String, NCEndpointCacheValue]("endpoint-cache")
        }

        require(cache != null)

        httpCli = HttpClients.createDefault

        sender =
            U.mkThread("endpoint-sender-thread") {
                thread ⇒ {
                    var sleepTime = 0L

                    while (!thread.isInterrupted) {
                        try {
                            mux.synchronized {
                                if (sleepTime > 0 && !work)
                                    mux.wait(sleepTime)

                                work = false
                            }

                            val now = U.nowUtcMs()

                            val readyQry: SqlQuery[String, NCEndpointCacheValue] =
                                new SqlQuery(
                                    classOf[NCEndpointCacheValue],
                                    "SELECT * FROM NCEndpointCacheValue WHERE sendTime <= ? AND processed = FALSE"
                                ).setArgs(List(now).map(_.asInstanceOf[java.lang.Object]): _*)

                            val readyData =
                                catching(wrapIE) {
                                    cache.query(readyQry).getAll.asScala.map(p ⇒ p.getKey → p.getValue).toMap
                                }

                            logger.trace(s"Endpoint records to send: ${readyData.size}")

                            if (readyData.nonEmpty) {
                                val processed = readyData.values.map(p ⇒ {
                                    p.getSrvReqId →
                                        new NCEndpointCacheValue(
                                            p.getState,
                                            p.getEndpoint,
                                            p.getSendTime,
                                            p.getAttempts,
                                            p.getCreatedOn,
                                            p.getUserId,
                                            p.getSrvReqId,
                                            true
                                        )
                                }).toMap

                                NCTxManager.startTx {
                                    cache ++= processed
                                }

                                readyData.
                                    groupBy { case (_ , v) ⇒ (v.getUserId, v.getEndpoint)}.
                                    foreach { case ((usrId, ep), data) ⇒
                                        val values = data.values.toSeq

                                        require(values.nonEmpty)

                                        send(usrId, ep, values)
                                    }
                            }

                            val minTime =
                                cache.query(new SqlFieldsQuery(
                                "SELECT IFNULL(MIN(sendTime), 0) FROM NCEndpointCacheValue WHERE sendTime > ?"
                                ).setArgs(List(now).map(_.asInstanceOf[java.lang.Object]): _*)).
                            getAll.asScala.head.asScala.head.asInstanceOf[Long]

                            sleepTime = if (minTime != 0) minTime - now else Long.MaxValue
                        }
                        catch {
                            case e: Throwable ⇒ logger.error("Notifications sending error.", e)
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
    
    /**
      * Checks and reduces if necessary queue size, for each user and in general.
      */
    private def clean(): Unit =
        try {
            // Clears cache for each user.
            val query: SqlQuery[String, NCEndpointCacheValue] =
                new SqlQuery(
                    classOf[NCEndpointCacheValue],
                        s"""
                        |SELECT v.*
                        |FROM
                        |    (SELECT DISTINCT userId FROM NCEndpointCacheValue) u,
                        |    (SELECT
                        |        _key,
                        |        _val,
                        |        userId
                        |    FROM NCEndpointCacheValue v1
                        |    WHERE
                        |        v1.srvReqId NOT IN (
                        |            SELECT v2.srvReqId
                        |            FROM NCEndpointCacheValue v2
                        |            WHERE v2.userId = v1.userId
                        |            ORDER BY v2.createdOn DESC
                        |            LIMIT ${Config.maxQueueUserSize}
                        |        )
                        |    ) v
                        |WHERE u.userId = v.userId
                        """.stripMargin
                )

            catching(wrapIE) {
                NCTxManager.startTx {
                    val srvReqIds = cache.query(query).getAll.asScala.map(_.getKey).toSet

                    if (srvReqIds.nonEmpty) {
                        logger.warn(s"Endpoint notifications dropped due to per-user queue size limit: $srvReqIds")

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

                        logger.warn(s"Endpoint notifications dropped due to overall queue size limit: $srvReqIds")

                        cache --= srvReqIds
                    }
                }
            }
        }
        catch {
            case e: Throwable ⇒ logger.error("Endpoint notification unexpected error.", e)
        }

    /**
      * Sends states events to user endpoint.
      *
      * @param usrId User ID.
      * @param ep Endpoint.
      * @param values Cached values.
      */
    private def send(usrId: Long, ep: String, values: Seq[NCEndpointCacheValue]): Unit = {
        val map = values.map(p ⇒ {
            val s = p.getState

            val v =
                QueryStateJs(
                    s.srvReqId,
                    s.text,
                    s.userId,
                    s.dsId,
                    s.modelId,
                    s.probeId.orNull,
                    s.status,
                    s.resultType.orNull,
                    if (s.resultBody.isDefined && s.resultType.isDefined && s.resultType.get == "json")
                        U.js2Map(s.resultBody.get)
                    else
                        s.resultBody.orNull,
                    s.error.orNull,
                    s.createTstamp.getTime,
                    s.updateTstamp.getTime
                )

            s.srvReqId → v
        }).toMap

        U.asFuture(
        _ ⇒ {
            val post = new HttpPost(ep)

            try {
                post.setHeader("Content-Type", "application/json")
                post.setEntity(new StringEntity(GSON.toJson(map.values.asJava), "UTF-8"))

                httpCli.execute(
                    post,
                    new ResponseHandler[Unit] {
                        override def handleResponse(resp: HttpResponse): Unit = {
                            val code = resp.getStatusLine.getStatusCode

                            if (code != 200)
                                throw new NCE(s"Unexpected HTTP response during endpoint sending [" +
                                    s"userId=$usrId, " +
                                    s"endpoint=$ep, " +
                                    s"code=$code" +
                                    s"]"
                                )
                        }
                    }
                )
            }
            finally
                post.releaseConnection()
        },
        {
            case e: Exception ⇒
                try {
                    val now = U.nowUtcMs()

                    val delIds = map.keySet.filter(v ⇒ !NCQueryManager.contains(v))

                    var wakeUp = false

                    NCTxManager.startTx {
                        cache --= delIds

                        var candidates =
                            (map.keySet -- delIds).flatMap(srvReqId ⇒
                                cache(srvReqId) match {
                                    case Some(v) ⇒
                                        val i = v.getAttempts
                                        val time = if (i < Config.delaysCnt) Config.delaysMs(i) else Config.delaysMs.last
                                        val s = v.getState

                                        Some(
                                            srvReqId →
                                                new NCEndpointCacheValue(
                                                    s,
                                                    v.getEndpoint,
                                                    now + time,
                                                    i + 1,
                                                    v.getCreatedOn,
                                                    s.userId,
                                                    s.srvReqId,
                                                    false
                                                )
                                        )
                                    case None ⇒ None
                                }
                            ).toMap

                        if (candidates.nonEmpty) {
                            val min = now - Config.lifeTimeMs

                            val delIds = candidates.filter(_._2.getCreatedOn < min).keys

                            if (delIds.nonEmpty) {
                                logger.warn(s"Endpoint notifications dropped due to timeout: ${delIds.mkString(", ")}")

                                cache --= delIds.toSet
                                candidates --= delIds
                            }

                            if (candidates.nonEmpty) {
                                cache ++= candidates

                                val firstErrsIds = candidates.filter(_._2.getAttempts == 1).keySet

                                if (firstErrsIds.nonEmpty)
                                    logger.warn(
                                        s"Failed to send endpoint notification, first attempt " +
                                            s"[userId=$usrId" +
                                            s", endpoint=$ep" +
                                            s", lifeTimeMins=${Config.lifeTimeMins}" +
                                            s", srvReqIds=${firstErrsIds.mkString(", ")}" +
                                            s", error=${e.getLocalizedMessage}" +
                                            s"]"
                                    )

                                wakeUp = true
                            }
                        }
                    }

                    if (wakeUp)
                        mux.synchronized {
                            work = true

                            mux.notifyAll()
                        }
                }
                catch {
                    case e: Exception ⇒ logger.error("Unexpected error during endpoint sending.", e)
                }
        },
        (_: Unit) ⇒ {
            catching(wrapIE) {
                NCTxManager.startTx {
                    cache --= map.keySet
                }
            }

            logger.trace(
                s"Endpoint notifications sent [userId=$usrId, endpoint=$ep, srvReqIds=${map.keySet.mkString(", ")}]"
            )
        })
    }

    /**
      * Stops this component.
      */
    override def stop(): Unit = {
        U.shutdownPools(cleaner)
        U.stopThread(sender)

        cache = null
        httpCli = null

        super.stop()
    }

    /**
      * Adds event for asynchronous notification.
      *
      * @param state Query state.
      * @param ep Endpoint.
      */
    def addNotification(state: NCQueryStateMdo, ep: String): Unit = {
        require(state != null)
        
        ensureStarted()

        U.asFuture(
            _ ⇒ {
                val t = U.nowUtcMs()

                catching(wrapIE) {
                    cache +=
                        state.srvReqId →
                        new NCEndpointCacheValue(
                            state, ep, t, 0, t, state.userId, state.srvReqId, false
                        )
                }

                mux.synchronized {
                    work = true

                    mux.notifyAll()
                }
            },
            {
                case e: Exception ⇒
                    logger.error(s"Failed to add endpoint notification [state=$state, ep=$ep]", e)
            }
        )
    }

    /**
      * Cancel notifications for given server request IDs.
      *
      * @param srvReqIds Server request IDs.
      */
    def cancelNotifications(srvReqIds: Set[String]): Unit = {
        require(srvReqIds != null)

        ensureStarted()

        U.asFuture(
            _ ⇒ {
                catching(wrapIE) {
                    NCTxManager.startTx {
                        cache --= srvReqIds
                    }
                }
            },
            {
                case e: Exception ⇒
                    logger.error(s"Failed to cancel endpoint notification [srvReqIds=$srvReqIds]", e)
            }
        )
    }

    /**
      * Cancel notifications for given user ID and its endpoint.
      *
      * @param usrId User ID.
      */
    def cancelNotifications(usrId: Long): Unit = {
        ensureStarted()

        U.asFuture(
            _ ⇒ {
                val query: SqlQuery[String, NCEndpointCacheValue] =
                    new SqlQuery(
                        classOf[NCEndpointCacheValue],
                        "SELECT * FROM NCEndpointCacheValue WHERE userId = ?"
                    ).setArgs(List(usrId).map(_.asInstanceOf[java.lang.Object]): _*)

                val srvReqIds = cache.query(query).getAll.asScala.map(_.getKey).toSet

                if (srvReqIds.nonEmpty)
                    catching(wrapIE) {
                        NCTxManager.startTx {
                            cache --= srvReqIds
                        }
                    }
            },
            {
                case e: Exception ⇒
                    logger.error(s"Failed to cancel endpoint notification [usrId=$usrId]", e)
            }
        )
    }
}