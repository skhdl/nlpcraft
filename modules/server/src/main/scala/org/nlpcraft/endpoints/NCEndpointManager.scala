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

import com.google.gson.Gson
import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.ignite.IgniteCache
import org.apache.ignite.cache.query.SqlQuery
import org.nlpcraft.ignite.NCIgniteNLPCraft
import org.nlpcraft.mdo.NCQueryStateMdo
import org.nlpcraft.query.NCQueryManager
import org.nlpcraft.util.NCGlobals
import org.nlpcraft.{NCConfigurable, NCE, NCLifecycle}
import resource.managed

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Endpoints manager.
  */
object NCEndpointManager extends NCLifecycle("Endpoints manager") with NCIgniteNLPCraft {
    private object Config extends NCConfigurable {
        val maxQueueSize: Int = hocon.getInt("endpoint.max.queue.size")
        val maxQueueCheckPeriodMs: Long = hocon.getLong("endpoint.max.queue.check.period.mins") * 60 * 1000
        val delaysMs: Seq[Long] = hocon.getLongList("endpoint.delaysSecs").toSeq.map(p ⇒ p * 1000)
        val delaysCnt: Int = delaysMs.size

        override def check(): Unit = {
            require(maxQueueSize > 0, s"Parameter `maxQueueSize` must be positive: $maxQueueSize")
            require(maxQueueCheckPeriodMs > 0, s"Parameter `maxQueueCheckPeriodMs` must be positive: $maxQueueCheckPeriodMs")
            require(delaysMs.nonEmpty, s"Parameters `delaysMs` shouldn't be empty")

            delaysMs.foreach(delayMs ⇒ require(delayMs > 0, s"Parameter `delayMs` must be positive: $delayMs"))
        }
    }

    Config.check()

    case class Value(state: NCQueryStateMdo, endpoint: String, sendTime: Long, attempts: Int, createdOn: Long)

    private final val GSON = new Gson
    private final val mux = new Object

    @volatile private var sleepTime = Long.MaxValue

    @volatile private var cache: IgniteCache[String, Value] = _
    @volatile private var sender: Thread = _
    @volatile private var cleaner: Thread = _
    @volatile private var httpClient: CloseableHttpClient = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        cache = ignite.cache[String, Value]("endpoint-notification-cache")

        require(cache != null)

        httpClient = HttpClients.createDefault

        sender =
            NCGlobals.mkThread("endpoint-notifier-sender-thread") {
                thread ⇒ {
                    while (!thread.isInterrupted) {
                        mux.synchronized {
                            val t = sleepTime

                            mux.wait(t)
                        }

                        val sql: SqlQuery[String, Value] = new SqlQuery(classOf[Value], "time >= ")

                        val data4Send =
                            managed { cache.query(sql.setArgs(Array(now()))) } acquireAndGet { cursor ⇒
                                cursor.map(p ⇒ p.getKey → p.getValue).toMap
                            }

                        data4Send.
                            groupBy { case (_, v) ⇒ (v.state.userId, v.endpoint) }.
                            foreach { case ((usrId, ep), data) ⇒ send(usrId, ep, data.values.toSeq) }
                    }
                }
            }

        sender.start()

        cleaner =
            NCGlobals.mkThread("endpoint-notifier-queue-cleaner-thread") {
                thread ⇒ {
                    while (!thread.isInterrupted) {
                        mux.synchronized {
                            mux.wait(Config.maxQueueCheckPeriodMs)
                        }

                        val srvReqIds = cache.
                            asScala.
                            groupBy(_.getValue.state.userId).
                            filter { case (_, data) ⇒ data.toSeq.size > Config.maxQueueSize }.
                            flatMap {
                                case (_, data) ⇒
                                    data.toSeq.sortBy(-_.getValue.createdOn).drop(Config.maxQueueSize).map(_.getKey)
                            }

                        cache.removeAll(srvReqIds.toSet.asJava)

                        logger.warn(s"Requests deleted because queue is too big: $srvReqIds")
                    }
                }
            }

        cleaner.start()

        super.start()
    }

    /**
      * Stops this component.
      */
    override def stop(): Unit = {
        NCGlobals.stopThread(cleaner)
        NCGlobals.stopThread(sender)

        cache = null
        httpClient = null

        super.stop()
    }

    private def now(): Long = System.currentTimeMillis()

    private def send(usrId: Long, ep: String, values: Seq[Value]): Unit = {
        // Should be the same as REST '/check' response.
        case class QueryStateJs(
            srvReqId: String,
            usrId: Long,
            dsId: Long,
            mdlId: String,
            probeId: Option[String],
            status: String,
            resType: Option[String],
            resBody: Option[String],
            error: Option[String],
            createTstamp: Long,
            updateTstamp: Long
        )

        val seq = values.map(p ⇒
            QueryStateJs(
                p.state.srvReqId,
                p.state.userId,
                p.state.dsId,
                p.state.modelId,
                p.state.probeId,
                p.state.status,
                p.state.resultType,
                p.state.resultBody,
                p.state.error,
                p.state.createTstamp.getTime,
                p.state.updateTstamp.getTime
            )
        )

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
                finally
                    post.releaseConnection()
            },
            {
                case e: Exception ⇒
                    val t = now()

                    var added = false

                    var minDelay = Long.MaxValue

                    values.foreach(v ⇒ {
                        if (NCQueryManager.has(v.state.srvReqId)) {
                            val delay =
                                if (v.attempts < Config.delaysCnt) Config.delaysMs(v.attempts) else Config.delaysMs.last

                            if (delay < minDelay)
                                minDelay = delay

                            cache.put(
                                v.state.srvReqId,
                                Value(v.state, v.endpoint, t + delay, v.attempts + 1, v.createdOn)
                            )

                            added = true
                        }
                    })

                    if (added)
                        mux.synchronized {
                            sleepTime = minDelay

                            mux.notifyAll()
                        }

                    logger.warn(
                        s"Error sending notification [userId=$usrId, endpoint=$ep, error=${e.getLocalizedMessage}]"
                    )
            },
            (_: Unit) ⇒ {
                cache.removeAll(seq.map(_.srvReqId).toSet.asJava)

                logger.trace(s"Endpoint notification sent [userId=$usrId, endpoint=$ep]")
            }
        )
    }

    /**
      * Adds event for processing.
      *
      * @param state Query state.
      * @param ep Endpoint.
      */
    def addNotification(state: NCQueryStateMdo, ep: String): Unit = {
        require(state != null)

        ensureStarted()

        val t = now()

        cache.put(state.srvReqId, Value(state = state, endpoint = ep, sendTime = t, attempts = 0, createdOn = t))

        mux.synchronized {
            mux.notifyAll()
        }
    }

    /**
      * Cancel notification for given server request ID.
      *
      * @param usrId User ID.
      * @param srvReqId Server request ID.
      */
    def cancelNotification(usrId: Long, srvReqId: String): Unit = {
        require(srvReqId != null)

        ensureStarted()

        cache.get(srvReqId) match {
            case null ⇒ // No-op.
            case v ⇒
                if (v.state.srvReqId == srvReqId)
                    cache.remove(srvReqId)
                else
                    logger.error(s"Attempt to remove invalid request data [usrId=$usrId, srvReqId=$srvReqId]")
        }
    }

    /**
      * Cancel notifications for given user ID and endpoint.
      *
      * @param usrId User ID.
      * @param ep Endpoint.
      */
    def cancelNotifications(usrId: Long, ep: String): Unit = {
        require(ep != null)

        ensureStarted()

        val sql: SqlQuery[String, Value] = new SqlQuery(classOf[Value], "state.userId = ? AND endpoint = ?")

        val m =
            managed { cache.query(sql.setArgs(Array(usrId, ep))) } acquireAndGet { cursor ⇒
                cursor.map(p ⇒ p.getKey → p.getValue).toMap
            }

        cache.removeAll(m.keySet)
    }
}
