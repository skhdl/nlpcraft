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
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.probe.mgrs.conn

import java.io.{EOFException, IOException, InterruptedIOException}
import java.net.{InetAddress, NetworkInterface, Socket}
import java.util.concurrent.CountDownLatch
import java.util.{Properties, TimeZone}
import java.util.concurrent.atomic.AtomicInteger

import org.nlpcraft.crypto._
import org.nlpcraft.probe._
import org.nlpcraft.probe.mgrs.cmd.NCCommandManager
import org.nlpcraft.probe.mgrs.deploy.NCDeployManager
import org.nlpcraft.probe.mgrs.exit.NCExitManager
import org.nlpcraft.probe.mgrs.model.NCModelManager
import org.nlpcraft.socket._
import org.nlpcraft.{NCLifecycle, _}

import scala.collection.mutable

/**
  * Probe down/up link connection manager.
  */
object NCProbeConnectionManager extends NCProbeManager("Connection manager 2") {
    // Uplink retry timeout.
    private final val RETRY_TIMEOUT = 10 * 1000
    // SO_TIMEOUT.
    private final val SO_TIMEOUT = 5 * 1000
    // Ping timeout.
    private final val PING_TIMEOUT = 5 * 1000
    
    // Internal probe GUID.
    final val PROBE_GUID = G.genGuid()
    
    // Internal semaphores.
    private val stopSem: AtomicInteger = new AtomicInteger(1)
    
    private final val sysProps: Properties = System.getProperties
    private final val localHost: InetAddress = InetAddress.getLocalHost
    private var hwAddrs: String = _
    
    // Holding probe-to-server queue.
    private val p2sQueue = mutable.Queue.empty[Serializable]
    
    // Control thread.
    private var ctrlThread: Thread = _
    
    
    /**
      *
      */
    protected def isStopping: Boolean = stopSem.intValue() == 0
    
    /**
      *
      */
    protected def setStopping(): Unit = stopSem.set(0)
    
    /**
      * Schedules message for sending to the server.
      *
      * @param msg Message to send to server.
      */
    def send(msg: NCProbeMessage): Unit = {
        // Set probe identification for each message, if necessary.
        msg.setProbeToken(config.getToken)
        msg.setProbeId(config.getId)
        msg.setProbeGuid(PROBE_GUID)
    
        p2sQueue.synchronized {
            if (!isStopping) {
                p2sQueue += msg
    
                p2sQueue.notifyAll()
            }
            else
                logger.trace(s"Message sending ignored b/c of stopping: $msg")
        }
    }
    
    class HandshakeError(msg: String) extends RuntimeException(msg)
    
    /**
      * Opens probe-to-server socket.
      */
    @throws[Exception]
    private def openP2SSocket(): NCSocket = {
        val (host, port) = G.splitEndpoint(config.getUpLink)
        val cryptoKey = NCCipher.makeTokenKey(config.getToken)
    
        logger.info(s"Opening P2S link to '$host:$port'")
    
        // Connect down socket.
        val sock = NCSocket(new Socket(host, port), host)
    
        sock.write(G.makeSha256Hash(config.getToken)) // Hash.
        sock.write(NCProbeMessage( // Handshake.
            // Type.
            "INIT_HANDSHAKE",
        
            // Payload.
            // Probe identification.
            "PROBE_TOKEN" → config.getToken,
            "PROBE_ID" → config.getId,
            "PROBE_GUID" → PROBE_GUID
        ), cryptoKey)
    
        val resp = sock.read[NCProbeMessage](cryptoKey) // Get handshake response.
    
        def err(msg: String) = throw new HandshakeError(msg)
    
        resp.getType match {
            case "P2S_PROBE_OK" ⇒ logger.info("  |=⇒ P2S handshake OK.") // Bingo!
            case "P2S_PROBE_NOT_FOUND" ⇒ err("Probe failed to start due to unknown error.")
            case _ ⇒ err(s"Unexpected server message (you may need to update the probe): ${resp.getType}")
        }
    
        sock
    }
    
    /**
      * Opens server-to-probe socket.
      */
    @throws[Exception]
    private def openS2PSocket(): NCSocket = {
        val netItf = NetworkInterface.getByInetAddress(localHost)
    
        hwAddrs = ""
    
        if (netItf != null) {
            val addrs = netItf.getHardwareAddress
        
            if (addrs != null)
                hwAddrs = addrs.foldLeft("")((s, b) ⇒ s + (if (s == "") f"$b%02X" else f"-$b%02X"))
        }
    
        val (host, port) = G.splitEndpoint(config.getDownLink)
        val cryptoKey = NCCipher.makeTokenKey(config.getToken)
        val ver = NCProbeVersion.getCurrent
        val tmz = TimeZone.getDefault
    
        logger.info(s"Opening S2P link to '$host:$port'")
    
        def err(msg: String) = throw new HandshakeError(msg)
    
        // Connect down socket.
        val sock = NCSocket(new Socket(host, port), host)
    
        sock.write(G.makeSha256Hash(config.getToken)) // Hash, sent clear text.
    
        sock.read[NCProbeMessage]().getType match { // Get hash check response.
            case "S2P_HASH_CHECK_OK" ⇒
                sock.write(NCProbeMessage( // Handshake.
                    // Type.
                    "INIT_HANDSHAKE",
        
                    // Payload.
                    // Probe identification.
                    "PROBE_TOKEN" → config.getToken,
                    "PROBE_ID" → config.getId,
                    "PROBE_GUID" → PROBE_GUID,
        
                    // Handshake data,
                    "PROBE_API_DATE" → ver.date,
                    "PROBE_API_VER" → ver.version,
                    "PROBE_OS_VER" → sysProps.getProperty("os.version"),
                    "PROBE_OS_NAME" → sysProps.getProperty("os.name"),
                    "PROBE_OS_ARCH" → sysProps.getProperty("os.arch"),
                    "PROBE_START_TSTAMP" → System.currentTimeMillis(),
                    "PROBE_TMZ_ID" → tmz.getID,
                    "PROBE_TMZ_ABBR" → tmz.getDisplayName(false, TimeZone.SHORT),
                    "PROBE_TMZ_NAME" → tmz.getDisplayName(),
                    "PROBE_SYS_USERNAME" → sysProps.getProperty("user.name"),
                    "PROBE_JAVA_VER" → sysProps.getProperty("java.version"),
                    "PROBE_JAVA_VENDOR" → sysProps.getProperty("java.vendor"),
                    "PROBE_HOST_NAME" → localHost.getHostName,
                    "PROBE_HOST_ADDR" → localHost.getHostAddress,
                    "PROBE_HW_ADDR" → hwAddrs,
                    "PROBE_MODELS_DS" → NCDeployManager.getDescriptors.toList.map(d ⇒ (d.getId, d.getName, d.getVersion)),
                    "PROBE_MODELS_USAGE" → NCModelManager.getAllUsage
                ), cryptoKey)
    
                val resp = sock.read[NCProbeMessage](cryptoKey) // Get handshake response.
    
                resp.getType match {
                    case "S2P_PROBE_MULTIPLE_INSTANCES" ⇒ err(
                        "Duplicate probes ID detected. Each probe has to have a unique ID.")
                        
                    case "S2P_PROBE_DUP_MODEL" ⇒ err(
                        s"Attempt to deploy model with duplicate ID: ${resp.data[String]("PROBE_MODEL_ID")}")
                        
                    case "S2P_PROBE_NOT_FOUND" ⇒ err(
                        "Probe failed to start due to unknown error.")
                        
                    case "S2P_PROBE_VERSION_MISMATCH" ⇒ err(
                        s"Probe version is unsupported: ${ver.version}")
                        
                    case "S2P_PROBE_OK" ⇒ logger.info("  |=⇒ S2P handshake OK.") // Bingo!
                    
                    case _ ⇒ err(s"Unknown server message (you need to update the probe): ${resp.getType}")
                }
    
                sock

            case "S2P_HASH_CHECK_UNKNOWN" ⇒ err(s"Unknown probe token: ${config.getToken}.")
        }
    }
    
    /**
      *
      */
    private def abort(): Unit = {
        // Make sure to exit & stop this thread.
        ctrlThread.interrupt()
        
        // Exit the probe with error code.
        NCExitManager.fail()
    }
    
    /**
      *
      * @return
      */
    override def start(): NCLifecycle = {
        require(NCCommandManager.isStarted)
        require(NCModelManager.isStarted)
        
        ctrlThread = G.mkThread("probe-ctrl-thread") { t ⇒
            var p2sSock: NCSocket = null
            var s2pSock: NCSocket = null
            
            var p2sThread: Thread = null
            var s2pThread: Thread = null
    
            /**
              *
              */
            def closeAll(): Unit = {
                G.stopThread(p2sThread)
                G.stopThread(s2pThread)
    
                p2sThread = null
                s2pThread = null

                if (p2sSock != null) p2sSock.close()
                if (s2pSock != null) s2pSock.close()
        
                p2sSock = null
                s2pSock = null
            }
            
            /**
              * 
              */
            def timeout(): Unit =
                if (!t.isInterrupted) G.ignoreInterrupt {
                    Thread.sleep(RETRY_TIMEOUT)
                }
            
            val cryptoKey = NCCipher.makeTokenKey(config.getToken)
            
            while (!t.isInterrupted)
                try {
                    logger.info(s"Establishing server connection to [" +
                        s"s2p=${config.getUpLink}, " +
                        s"p2s=${config.getDownLink}" +
                    s"]")

                    s2pSock = openS2PSocket()
                    p2sSock = openP2SSocket()

                    s2pSock.socket.setSoTimeout(SO_TIMEOUT)

                    val latch = new CountDownLatch(1)

                    /**
                      *
                      * @param caller Caller thread to interrupt.
                      * @param msg Error message.
                      */
                    def exit(caller: Thread, msg : String): Unit = {
                        logger.info(msg)
    
                        caller.interrupt() // Interrupt current calling thread.

                        latch.countDown()
                    }

                    s2pThread = G.mkThread("probe-s2p-link") { t ⇒
                        // Main reading loop.
                        while (!t.isInterrupted)
                            try
                                NCCommandManager.processServerMessage(s2pSock.read[NCProbeMessage](cryptoKey))
                            catch {
                                case _: InterruptedIOException | _: InterruptedException ⇒ ()
                                case _: EOFException ⇒ exit(t, s"S2P server connection closed.")
                                case e: Exception ⇒ exit(t, s"S2P connection failed: ${e.getMessage}")
                            }
                    }
                    
                    p2sThread = G.mkThread("probe-p2s-link") { t ⇒
                        while (!t.isInterrupted)
                            try {
                                p2sQueue.synchronized {
                                    if (p2sQueue.isEmpty) {
                                        p2sQueue.wait(PING_TIMEOUT)
                                        
                                        if (!p2sThread.isInterrupted && p2sQueue.isEmpty) {
                                            val pingMsg = NCProbeMessage("P2S_PING")
                                            
                                            pingMsg.setProbeToken(config.getToken)
                                            pingMsg.setProbeId(config.getId)
                                            pingMsg.setProbeGuid(PROBE_GUID)
                                            
                                            p2sSock.write(pingMsg, cryptoKey)
                                        }
                                    }
                                    else {
                                        val msg = p2sQueue.head

                                        // Write head first (without actually removing from queue).
                                        p2sSock.write(msg, cryptoKey)

                                        // If sent ok - remove from queue.
                                        p2sQueue.dequeue()
                                    }
                                }
                            }
                            catch {
                                case _: InterruptedIOException | _: InterruptedException ⇒ ()
                                case _: EOFException ⇒ exit(t, s"P2S server connection closed.")
                                case e: Exception ⇒ exit(t, s"P2S connection failed: ${e.getMessage}")
                            }
                    }

                    // Bingo - start downlink and uplink!
                    s2pThread.start()
                    p2sThread.start()

                    logger.info("Server connection OK.")
                    
                    while (!t.isInterrupted && latch.getCount > 0) G.ignoreInterrupt {
                        latch.await()
                    }
                    
                    closeAll()

                    if (!isStopping) {
                        logger.info(s"Server connection closed (retrying in ${RETRY_TIMEOUT / 1000}s).")
                        
                        timeout()
                    }
                    else
                        logger.info(s"Server connection closed.")
                }
                catch {
                    case e: HandshakeError ⇒
                        // Clean up.
                        closeAll()
    
                        // Ack the handshake error message.
                        logger.error(s"!!! Failed during server connection handshake (aborting).")

                        if (e.getMessage != null)
                            logger.error(s"!!! ${e.getMessage}")

                        abort()

                    case e: IOException ⇒
                        // Clean up.
                        closeAll()

                        // Ack the IO error message.
                        if (e.getMessage != null)
                            logger.error(s"Failed to establish server connection (retrying in ${RETRY_TIMEOUT / 1000}s): ${e.getMessage}")
                        else
                            logger.error(s"Failed to establish server connection (retrying in ${RETRY_TIMEOUT / 1000}s).")

                        timeout()

                    case e: Exception ⇒
                        // Clean up.
                        closeAll()
    
                        // Ack the error message.
                        logger.error("Unexpected error establishing server connection (aborting).", e)
    
                        abort()
                }

            closeAll()
        }
        
        ctrlThread.start()
    
        super.start()
    }
    
    /**
      *
      */
    override def stop(): Unit = {
        setStopping()
    
        G.stopThread(ctrlThread)
        
        super.stop()
    }
}
