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

package org.nlpcraft.probe

import java.util.concurrent.Executors

import org.nlpcraft.NCLifecycle
import org.nlpcraft._

import scala.concurrent.ExecutionContext

/**
  * Probe manager.
  */
object NCProbeManager extends NCLifecycle("Probe manager") {
    // Type safe and eager configuration container.
    private[probe] object Config extends NCConfigurable {
        private val p2sLink = G.splitEndpoint(hocon.getString("links.p2s"))
        private val s2pLink = G.splitEndpoint(hocon.getString("links.s2p"))
        
        val p2sHost: String = p2sLink._1
        val p2sPort: Int = p2sLink._2
        val s2pHost: String = s2pLink._1
        val s2pPort: Int = s2pLink._2
        
        val poolSize: Int = hocon.getInt("probe.poolSize")
        val reconnectTimeoutMs: Long = hocon.getLong("probe.reconnectTimeoutMs")
        val pingTimeoutMs: Long = hocon.getLong("probe.pingTimeoutMs")
        val soTimeoutMs: Long = hocon.getLong("probe.soTimeoutMs")
        
        override def check(): Unit = {
            assert(p2sPort >= 0 && p2sPort <= 65535, s"P2S port ($p2sPort) must be >= 0 and <= 65535")
            assert(s2pPort >= 0 && s2pPort <= 65535, s"S2P port ($s2pPort) must be >= 0 and <= 65535")
            assert(reconnectTimeoutMs > 0, s"Reconnect time must be > 0")
            assert(poolSize > 0, s"Pool size must be > 0")
            assert(soTimeoutMs > 0, s"SO_TIMEOUT must be > 0")
            assert(pingTimeoutMs > 0, s"Ping timeout must be > 0")
        }
    }
    
    private final val EC = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
    )
    
    /**
      *
      * @return
      */
    override def start(): NCLifecycle = {
        super.start()
    }
    
    /**
      *
      * @param probeGuid
      */
    @throws[NCE]
    def stopProbe(probeGuid: String): Unit = {
        ensureStarted()
    }
    
    /**
      *
      * @param probeGuid
      */
    @throws[NCE]
    def restartProbe(probeGuid: String): Unit = {
        ensureStarted()
    }
    
    /**
      *
      * @param usrId
      * @param dsId
      */
    @throws[NCE]
    def clearConversation(usrId: Long, dsId: Long): Unit = {
        ensureStarted()
    }
    
    /**
      *
      */
    override def stop(): Unit = {
        super.stop()
    }
}
