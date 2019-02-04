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

import java.net.InetAddress
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.google.gson.Gson
import org.apache.http.client.methods.HttpPost
import org.nlpcraft.plugin.apis.NCNotificationPlugin
import org.nlpcraft.{NCConfigurable, _}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
  * Notification plugin using buffered HTTP REST push to a set of pre-configured endpoints.
  */
object NCRestPushNotificationPlugin extends NCNotificationPlugin {
    case class Event(
        name: String,
        params: Seq[(String, Any)],
        tstamp: Long,
        host: String
    )
    
    // Configuration prefix.
    private final val CFG = "org.nlpcraft.notification.plugins.restpush.NCRestPushNotificationPlugin"

    private final val EC = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
    )

    private final val GSON = new Gson

    // Bounded buffer of events to be flushed.
    private val evts: ArrayBuffer[Event] = new ArrayBuffer[Event](Config.maxBufferSize)

    // Local host.
    private val localhost: String = InetAddress.getLocalHost.toString

    @volatile private var timer: ScheduledExecutorService = _

    private object Config extends NCConfigurable {
        val endpoints: List[String] = hocon.getStringList(s"$CFG.endpoints").asScala.toList
        val flushMsec: Long = hocon.getLong(s"$CFG.flushSecs") * 1000
        val maxBufferSize: Int = hocon.getInt(s"$CFG.maxBufferSize")
        val period: Long = hocon.getInt(s"$CFG.period")
        
        override def check(): Unit = {
            require(flushMsec > 0 , s"flush interval ($flushMsec) must be > 0")
            require(maxBufferSize > 0 , s"maximum buffer size ($maxBufferSize) must be > 0")
            require(period > 0 , s"check period ($period) must be > 0")
            require(endpoints.nonEmpty, s"at least one REST endpoint is required")

            // Endpoints are not validated to simplify communication protocol.
        }
    }
    
    Config.check()

    override def start(): Unit = {
        super.start()

        timer = Executors.newSingleThreadScheduledExecutor

        timer.scheduleWithFixedDelay(() ⇒ flush(), Config.period, Config.period, TimeUnit.MILLISECONDS)

        logger.info("Notification timer started.")
    }

    override def stop(): Unit = {
        if (timer != null) {
            timer.shutdown()

            try
                timer.awaitTermination(Long.MaxValue, TimeUnit.MILLISECONDS)
            catch {
                case e: InterruptedException ⇒ logger.warn("Failed to await notification timer.")
            }

            timer = null
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
        val size =
            evts.synchronized {
                evts += Event(evtName, params, G.nowUtcMs(), localhost)

                evts.size
            }

        if (evts.size > Config.maxBufferSize)
            flush()
    }
    
    /**
      * Flushes accumulated events, if any, to the registered REST endpoints.
      */
    private def flush(): Unit = {
        val copy = mutable.ArrayBuffer.empty[Event]
        
        evts.synchronized {
            copy ++= evts
        }

        Config.endpoints.map(ep ⇒ {
            Future {
                val post = new HttpPost(ep)

            }
        })

        // TODO: add push to each configured endpoint in a separate thread.
    }
}
