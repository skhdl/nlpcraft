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

package org.nlpcraft.notification.plugins.restpush

import java.util
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.google.gson.Gson
import org.apache.commons.validator.routines.UrlValidator
import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.nlpcraft.plugin.apis.NCNotificationPlugin
import org.nlpcraft.{NCConfigurable, _}

import scala.collection.JavaConverters._

/**
  * Notification plugin using buffered HTTP REST push to a set of pre-configured endpoints.
  */
object NCRestPushNotificationPlugin extends NCNotificationPlugin {
    case class Event(
        name: String,
        params: Seq[(String, Any)],
        tstamp: Long,
        internalIp: String,
        externalIp: String
    )

    // Configuration prefix.
    private final val CFG = "org.nlpcraft.notification.plugins.restpush.NCRestPushNotificationPlugin"

    private object Config extends NCConfigurable {
        val endpoints: List[String] = hocon.getStringList(s"$CFG.endpoints").asScala.toList
        val flushMsec: Long = hocon.getLong(s"$CFG.flushSecs") * 1000
        val maxBufferSize: Int = hocon.getInt(s"$CFG.maxBufferSize")
        val batchSize: Int = hocon.getInt(s"$CFG.batchSize")

        override def check(): Unit = {
            val urlVal = new UrlValidator(Array("http","https"))

            endpoints.foreach(ep ⇒ require(urlVal.isValid(ep), s"Invalid endpoint: $ep"))

            require(flushMsec > 0 , s"flush interval ($flushMsec) must be > 0")
            require(maxBufferSize > 0 , s"maximum buffer size ($maxBufferSize) must be > 0")
            require(endpoints.nonEmpty, s"at least one REST endpoint is required")
        }
    }

    Config.check()

    private final val GSON = new Gson

    // Bounded buffer of events to be flushed.
    private final val queues = Config.endpoints.map(ep ⇒ ep → new util.LinkedList[Event]()).toMap

    // Local hosts.
    private final val intlIp = G.getInternalIp
    private final val extIp = G.getExternalIp

    private final val httpClient = HttpClients.createDefault

    @volatile private var timers: Seq[ScheduledExecutorService] = _

    override def start(): Unit = {
        super.start()

        timers = Config.endpoints.map(ep ⇒ {
            val timer = Executors.newSingleThreadScheduledExecutor

            timer.scheduleWithFixedDelay(() ⇒ flush(ep), Config.flushMsec, Config.flushMsec, TimeUnit.MILLISECONDS)

            timer
        })

        logger.info(s"Notification timers started: ${Config.endpoints.length}")
    }

    override def stop(): Unit = {
        if (timers != null) {
            timers.foreach(_.shutdown())

            timers.foreach(timer ⇒
                try
                    timer.awaitTermination(Long.MaxValue, TimeUnit.MILLISECONDS)
                catch {
                    case _: InterruptedException ⇒ logger.warn("Failed to await notification timer.")
                }
            )

            timers = null
        }

        super.stop()
    }

    /**
      * Adds event with given name and optional parameters to the buffer. Buffer will be pushed to configured
      * endpoints periodically.
      *
      * @param evtName Event name.
      * @param params Optional set of named parameters.
      */
    override def onEvent(evtName: String, params: (String, Any)*): Unit = {
        val evt = Event(evtName, params, G.nowUtcMs(), intlIp, extIp)

        queues.values.foreach(queue ⇒
            // Note, that between batches sending endpoint queues can be oversized.
            // It is developed for simplifying logic. They are cleared by timer.
            queue.synchronized { queue.add(evt) }
        )
    }

    /**
      * Sends events batch to endpoint and clear endpoint queue if sending successful.
      *
      * @param ep Endpoint.
      * @param queue Endpoint queue.
      * @param batch Batch to send.
      */
    private def send(ep: String, queue: java.util.LinkedList[Event], batch: java.util.List[Event]): Unit = {
        val post = new HttpPost(ep)

        try {
            post.setHeader("Content-Type", "application/json")
            post.setEntity(new StringEntity(GSON.toJson(batch)))

            httpClient.execute(post, new ResponseHandler[Unit] {
                override def handleResponse(resp: HttpResponse): Unit = {
                    val code = resp.getStatusLine.getStatusCode

                    if (code != 200)
                        throw new NCE(s"Unexpected result code [endpoint=$ep, code=$code]")
                }
            })

            logger.debug(s"Request sent [endpoint=$ep, batchSize=${batch.size()}]")

            queue.synchronized {
                (0 until batch.size()).foreach(_ ⇒ queue.removeFirst())
            }
        }
        finally
            post.releaseConnection()
    }

    /**
      * Flash accumulated endpoints events.
      *
      * @param ep Endpoint.
      */
    private def flush(ep: String): Unit = {
        val queue = queues(ep)

        val copy: util.List[Event] = queue.synchronized {
            val overSize = queue.size() - Config.maxBufferSize

            (0 until overSize).foreach(_ ⇒ {
                val deleted = queue.removeFirst()

                logger.warn(s"Event lost because too long queue [endpoint=$ep, event=$deleted]")
            })

            new util.ArrayList(queue)
        }

        if (!copy.isEmpty)
            try {
                val size = copy.size()

                val n = size / Config.batchSize
                val delta = size % Config.batchSize

                var i = 0

                while (i < n) {
                    send(ep, queue, copy.subList(i * n, Config.batchSize))

                    i = i + 1
                }

                if (delta != 0)
                    send(ep, queue, copy.subList(n * Config.batchSize, delta))
            }
            catch {
                case e: Exception ⇒ logger.warn(s"Error during flush data to: $ep", e)
            }
    }
}
