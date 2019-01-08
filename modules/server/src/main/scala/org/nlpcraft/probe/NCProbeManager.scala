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
 * Software:    NlpCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.probe

import java.io._
import java.security.Key
import org.nlpcraft.NCLifecycle
import org.nlpcraft._
import org.nlpcraft.mdo.NCProbeMdo
import org.nlpcraft.socket.NCSocket
import scala.collection.mutable

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
    
    // Compound probe key.
    private case class ProbeKey(
        probeToken: String, // Probe token.
        probeId: String, // Unique probe ID.
        probeGuid: String // Runtime unique ID (to disambiguate different instances of the same probe).
    ) {
        override def toString: String = s"Probe key [" +
            s"probeId=$probeId, " +
            s"probeGuid=$probeGuid, " +
            s"probeToken=$probeToken" +
            s"]"
    }
    
    // Immutable probe holder.
    private case class ProbeHolder(
        probeKey: ProbeKey,
        probe: NCProbeMdo,
        var p2sSocket: NCSocket,
        var s2pSocket: NCSocket,
        var p2sThread: Thread, // Separate thread listening for messages from the probe.
        cryptoKey: Key, // Encryption key.
        timestamp: Long = System.currentTimeMillis()
    ) {
        /**
          *
          */
        def close(): Unit = {
            if (p2sThread != null)
                G.stopThread(p2sThread)
            
            if (s2pSocket != null)
                s2pSocket.close()
            
            if (p2sSocket != null)
                p2sSocket.close()
        }
    }
    
    private var p2sSrv: Thread = _
    private var s2pSrv: Thread = _
    private var pingSrv: Thread = _
    
    // All known probes keyed by probe key.
    private val probes = mutable.HashMap.empty[ProbeKey, ProbeHolder]
    // All probes pending complete handshake keyed by probe key.
    private val pending = mutable.HashMap.empty[ProbeKey, ProbeHolder]
    
    /**
      *
      * @return
      */
    override def start(): NCLifecycle = {
        super.start()
    }
    
    /**
      *
      * @param probeKey Probe key.
      */
    private def closeAndRemoveHolder(probeKey: ProbeKey): Unit = {
        // Check pending queue first.
        pending.synchronized { pending.remove(probeKey) } match {
            case None ⇒
                // Check active probes second.
                probes.synchronized { probes.remove(probeKey) } match {
                    case None ⇒
                    case Some(holder) ⇒
                        holder.close()
            
                        //                val modelsToUndeploy = mutable.ArrayBuffer.empty[(String/*probe ID*/, Long/*company ID*/)]
                        //
                        //                modelsToUndeploy ++= holder.probe.models.map(p ⇒ p.id → p.companyId)
                        //
                        //                // Fail all pending requests for lost models.
                        //                modelsToUndeploy.foreach {
                        //                    case (modelId, companyId) ⇒ DLQueryStateManager.setModelUndeploy(modelId, companyId)
                        //                }
                        //
                        //                usages.synchronized {
                        //                    usages --= modelsToUndeploy
                        //                }
            
                        logger.info(s"Probe closed and removed: $probeKey")
                }

            case Some(hld) ⇒
                hld.close()
                
                logger.info(s"Pending probe closed and removed: $probeKey")
        }
    }
    
    /**
      *
      * @param probeKey Probe key.
      * @param probeMsg Probe message to send.
      */
    @throws[NCE]
    @throws[IOException]
    private def sendToProbe(probeKey: ProbeKey, probeMsg: NCProbeMessage): Boolean = {
        val (sock, cryptoKey) = probes.synchronized {
            probes.get(probeKey) match {
                case None ⇒ (null, null)
                case Some(h) ⇒ (h.s2pSocket, h.cryptoKey)
            }
        }
        
        if (sock != null)
            try {
                sock.write(probeMsg, cryptoKey)
                
                true
            }
            catch {
                case _: EOFException ⇒
                    logger.trace(s"Probe closed connection: $probeKey")
                    
                    closeAndRemoveHolder(probeKey)
                    
                    false
                
                case e: Throwable ⇒
                    logger.error(s"S2P socket error [" +
                        s"sock=$sock, " +
                        s"probeKey=$probeKey, " +
                        s"probeMsg=$probeMsg" +
                        s"error=${e.getLocalizedMessage}" +
                        s"]")
                    
                    closeAndRemoveHolder(probeKey)
                    
                    false
            }
        else {
            logger.warn(s"Sending message to unknown probe (ignoring) [" +
                s"probeKey=$probeKey, " +
                s"probeMsg=$probeMsg" +
                s"]")
            
            false
        }
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
