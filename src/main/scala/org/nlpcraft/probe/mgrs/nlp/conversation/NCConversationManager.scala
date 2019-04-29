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

package org.nlpcraft.probe.mgrs.nlp.conversation

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import org.nlpcraft.common._
import org.nlpcraft.probe.mgrs.NCProbeLifecycle

import scala.collection._
import scala.concurrent.duration._

/**
  * Conversation manager.
  */
object NCConversationManager extends NCProbeLifecycle("Conversation manager") {
    case class Key(userId: Long, mdlId: String)
    case class Value(conversation: NCConversation, var time: Long = 0)

    // No reason to have it configurable by users...
    private final val CHECK_PERIOD = 5.minutes.toMillis

    // Cleaner is implemented to avoid OOM. Used timeout is just big enough value.
    // To simplify configuration it is not related with conversation usage time of another services.
    private final val TIMEOUT = 1.hour.toMillis

    private final val convs = mutable.HashMap.empty[Key, Value]

    @volatile private var cleaner: ScheduledExecutorService = _

    override def start(): NCLifecycle = {
        cleaner = Executors.newSingleThreadScheduledExecutor

        cleaner.scheduleWithFixedDelay(() ⇒ clean(), CHECK_PERIOD, CHECK_PERIOD, TimeUnit.MILLISECONDS)

        super.start()
    }

    override def stop(): Unit = {
        super.stop()

        U.shutdownPools(cleaner)
    }

    /**
      *
      */
    private def clean(): Unit = {
        val min = U.nowUtcMs() - TIMEOUT

        convs.synchronized {
            convs --= convs.filter(_._2.time < min).keySet
        }
    }

    /**
      * Gets conversation for given key.
      *
      * @param usrId User ID.
      * @param mdlId Model ID.
      * @return New or existing conversation.
      */
    def get(usrId: Long, mdlId: String): NCConversation = {
        ensureStarted()

        convs.synchronized {
            val v = convs.getOrElseUpdate(Key(usrId, mdlId), Value(NCConversation(usrId, mdlId)))

            v.time = U.nowUtcMs()

            v
        }.conversation
    }
}
