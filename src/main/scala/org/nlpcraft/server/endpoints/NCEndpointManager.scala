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
import org.apache.ignite.cache.query.SqlQuery
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
        val delaysMs: Seq[Long] = getLongList(s"$prefix.delaysSecs").toSeq.map(p ⇒ p * 1000)
        val delaysCnt: Int = delaysMs.size

        override def check(): Unit = {
            if (maxQueueSize <= 0)
                abortError(s"Configuration parameter '$prefix.queue.maxSize' must > 0: $maxQueueSize")
            if (maxQueueUserSize <= 0)
                abortError(s"Configuration parameter '$prefix.queue.maxPerUserSize' must > 0: $maxQueueUserSize")
            if (maxQueueCheckPeriodMs <= 0)
                abortError(s"Configuration parameter '$prefix.queue.checkPeriodMins' must > 0: $maxQueueCheckPeriodMs")
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

    @volatile private var sleepTime = Long.MaxValue

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
                    while (!thread.isInterrupted) {
                        mux.synchronized {
                            val t = sleepTime

                            if (t > 0)
                                mux.wait(t)

                            sleepTime = Long.MaxValue
                        }

                        val t = U.nowUtcMs()

                        val query: SqlQuery[String, NCEndpointCacheValue] =
                            new SqlQuery(
                                classOf[NCEndpointCacheValue],
                                "SELECT * FROM NCEndpointCacheValue WHERE sendTime <= ?"
                            )

                        query.setArgs(List(t).map(_.asInstanceOf[java.lang.Object]): _*)

                        val readyData =
                            catching(wrapIE) {
                                cache.query(query).getAll.asScala.map(p ⇒ p.getKey → p.getValue).toMap
                            }

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
                        logger.warn(s"Query state notifications dropped due to per-use queue size limit: $srvReqIds")

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

                        logger.warn(s"Query state notifications dropped due to overall queue size limit: $srvReqIds")

                        cache --= srvReqIds
                    }
                }
            }
        }
        catch {
            case e: Throwable ⇒ logger.error("Query notification GC error.", e)
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
                                throw new NCE(s"Unexpected query state endpoint send HTTP response [" +
                                    s"userId=$usrId, " +
                                    s"endpoint=$ep, " +
                                    s"code=$code" +
                                s"]")
                        }
                    }
                )
            }
            finally
                post.releaseConnection()
        },
        {
            case e: Exception ⇒
                val t = U.nowUtcMs()

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
                    val sleepTime = sendAgain.map(_._2.getSendTime).min - U.nowUtcMs()

                    mux.synchronized {
                        this.sleepTime = sleepTime

                        mux.notifyAll()
                    }
                }

                logger.warn(
                    s"Error sending notification " +
                        s"[userId=$usrId" +
                        s", endpoint=$ep" +
                        s", sendAgain=${sendAgain.size}" +
                        s", error=${e.getLocalizedMessage}" +
                        s"]"
                )
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
                        new NCEndpointCacheValue(state, ep, t, 0, t, state.userId, state.srvReqId)
                }

                mux.synchronized {
                    mux.notifyAll()
                }
            },
            {
                case e: Exception ⇒
                    logger.error(s"Failed to add query state notification [state=$state, ep=$ep]", e)
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
                    logger.error(s"Failed to cancel query state notification [srvReqIds=$srvReqIds]", e)
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
                    )

                query.setArgs(List(usrId).map(_.asInstanceOf[java.lang.Object]): _*)

                catching(wrapIE) {
                    NCTxManager.startTx {
                        cache --= cache.query(query).getAll.asScala.map(_.getKey).toSet
                    }
                }
            },
            {
                case e: Exception ⇒
                    logger.error(s"Failed to cancel query state notification [usrId=$usrId]", e)
            }
        )
    }
}
