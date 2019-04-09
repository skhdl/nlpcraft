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

package org.nlpcraft.probe.mgrs.conn

import java.io.{EOFException, IOException, InterruptedIOException}
import java.net.{InetAddress, NetworkInterface, Socket}
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.{Properties, TimeZone}

import org.nlpcraft.common.crypto._
import org.nlpcraft.probe.mgrs.{NCProbeLifecycle, NCProbeMessage}
import org.nlpcraft.probe.mgrs.cmd.NCCommandManager
import org.nlpcraft.probe.mgrs.deploy.NCDeployManager
import org.nlpcraft.probe.mgrs.model.NCModelManager
import org.nlpcraft.common.socket._
import org.nlpcraft.common.version.NCVersion
import org.nlpcraft.common._
import org.nlpcraft.common.nlp.core.NCNlpCoreManager

import scala.collection.mutable

/**
  * Probe down/up link connection manager.
  */
object NCConnectionManager extends NCProbeLifecycle("Connection manager") {
    // Uplink retry timeout.
    private final val RETRY_TIMEOUT = 10 * 1000
    // SO_TIMEOUT.
    private final val SO_TIMEOUT = 5 * 1000
    // Ping timeout.
    private final val PING_TIMEOUT = 5 * 1000
    
    // Internal probe GUID.
    final val PROBE_GUID = U.genGuid()
    
    // Internal semaphores.
    private val stopSem: AtomicInteger = new AtomicInteger(1)
    
    private final val sysProps: Properties = System.getProperties
    private final val localHost: InetAddress = InetAddress.getLocalHost
    private var hwAddrs: String = _
    
    // Holding downlink queue.
    private val dnLinkQueue = mutable.Queue.empty[Serializable]
    
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
        msg.setProbeToken(config.token)
        msg.setProbeId(config.id)
        msg.setProbeGuid(PROBE_GUID)
    
        dnLinkQueue.synchronized {
            if (!isStopping) {
                dnLinkQueue += msg
    
                dnLinkQueue.notifyAll()
            }
            else
                logger.trace(s"Message sending ignored b/c of stopping: $msg")
        }
    }
    
    class HandshakeError(msg: String) extends RuntimeException(msg)
    
    /**
      * Opens down link socket.
      */
    @throws[Exception]
    private def openDownLinkSocket(): NCSocket = {
        val (host, port) = U.splitEndpoint(config.downLink)
        
        val cryptoKey = NCCipher.makeTokenKey(config.token)
    
        logger.trace(s"Opening downlink to '$host:$port'")
    
        // Connect down socket.
        val sock = NCSocket(new Socket(host, port), host)
    
        sock.write(U.makeSha256Hash(config.token)) // Hash.
        sock.write(NCProbeMessage( // Handshake.
            // Type.
            "INIT_HANDSHAKE",
        
            // Payload.
            // Probe identification.
            "PROBE_TOKEN" → config.token,
            "PROBE_ID" → config.id,
            "PROBE_GUID" → PROBE_GUID
        ), cryptoKey)
    
        val resp = sock.read[NCProbeMessage](cryptoKey) // Get handshake response.
    
        def err(msg: String) = throw new HandshakeError(msg)
    
        resp.getType match {
            case "P2S_PROBE_OK" ⇒ logger.trace("Downlink handshake OK.") // Bingo!
            case "P2S_PROBE_NOT_FOUND" ⇒ err("Probe failed to start due to unknown error.")
            case _ ⇒ err(s"Unexpected server message: ${resp.getType}")
        }
    
        sock
    }
    
    /**
      * Opens uplink socket.
      */
    @throws[Exception]
    private def openUplinkSocket(): NCSocket = {
        val netItf = NetworkInterface.getByInetAddress(localHost)
    
        hwAddrs = ""
    
        if (netItf != null) {
            val addrs = netItf.getHardwareAddress
        
            if (addrs != null)
                hwAddrs = addrs.foldLeft("")((s, b) ⇒ s + (if (s == "") f"$b%02X" else f"-$b%02X"))
        }
    
        val (host, port) = U.splitEndpoint(config.upLink)
        
        val cryptoKey = NCCipher.makeTokenKey(config.token)
        
        logger.trace(s"Opening uplink to '$host:$port'")
    
        // Connect down socket.
        val sock = NCSocket(new Socket(host, port), host)
    
        sock.write(U.makeSha256Hash(config.token)) // Hash, sent clear text.
    
        val hashResp = sock.read[NCProbeMessage]()

        hashResp.getType match { // Get hash check response.
            case "S2P_HASH_CHECK_OK" ⇒
                val ver = NCVersion.getCurrent
                val tmz = TimeZone.getDefault
    
                val srvNlpEng =
                    hashResp.getOrElse(
                        "NLP_ENGINE",
                        throw new HandshakeError("NLP engine parameter missed in response.")
                    )

                val probeNlpEng = NCNlpCoreManager.getEngine

                if (srvNlpEng != probeNlpEng)
                    logger.warn(s"Invalid NLP engines configuration [server=$srvNlpEng, probe=$probeNlpEng]")

                sock.write(NCProbeMessage( // Handshake.
                    // Type.
                    "INIT_HANDSHAKE",
        
                    // Payload.
                    // Probe identification.
                    "PROBE_TOKEN" → config.token,
                    "PROBE_ID" → config.id,
                    "PROBE_GUID" → PROBE_GUID,
        
                    // Handshake data,
                    "PROBE_API_DATE" → ver.date,
                    "PROBE_API_VERSION" → ver.version,
                    "PROBE_OS_VER" → sysProps.getProperty("os.version"),
                    "PROBE_OS_NAME" → sysProps.getProperty("os.name"),
                    "PROBE_OS_ARCH" → sysProps.getProperty("os.arch"),
                    "PROBE_START_TSTAMP" → U.nowUtcMs(),
                    "PROBE_TMZ_ID" → tmz.getID,
                    "PROBE_TMZ_ABBR" → tmz.getDisplayName(false, TimeZone.SHORT),
                    "PROBE_TMZ_NAME" → tmz.getDisplayName(),
                    "PROBE_SYS_USERNAME" → sysProps.getProperty("user.name"),
                    "PROBE_JAVA_VER" → sysProps.getProperty("java.version"),
                    "PROBE_JAVA_VENDOR" → sysProps.getProperty("java.vendor"),
                    "PROBE_HOST_NAME" → localHost.getHostName,
                    "PROBE_HOST_ADDR" → localHost.getHostAddress,
                    "PROBE_HW_ADDR" → hwAddrs,
                    "PROBE_MODELS_DS" → NCDeployManager.getDescriptors.toList.map(d ⇒ (d.getId, d.getName, d.getVersion))
                ), cryptoKey)
    
                val resp = sock.read[NCProbeMessage](cryptoKey) // Get handshake response.
                
                def err(msg: String) = throw new HandshakeError(msg)
    
                resp.getType match {
                    case "S2P_PROBE_MULTIPLE_INSTANCES" ⇒ err("Duplicate probes ID detected. Each probe has to have a unique ID.")
                    case "S2P_PROBE_NOT_FOUND" ⇒ err("Probe failed to start due to unknown error.")
                    case "S2P_PROBE_VERSION_MISMATCH" ⇒ err(s"Server does not support probe version: ${ver.version}")
                    case "S2P_PROBE_OK" ⇒ logger.trace("Uplink handshake OK.") // Bingo!
                    case _ ⇒ err(s"Unknown server message: ${resp.getType}")
                }
    
                sock

            case "S2P_HASH_CHECK_UNKNOWN" ⇒ throw new HandshakeError(s"Sever does not recognize probe token: ${config.token}.")
        }
    }
    
    /**
      *
      */
    private def abort(): Unit = {
        // Make sure to exit & stop this thread.
        ctrlThread.interrupt()
        
        // Exit the probe with error code.
        System.exit(1)
    }
    
    /**
      *
      * @return
      */
    override def start(): NCLifecycle = {
        require(NCCommandManager.isStarted)
        require(NCModelManager.isStarted)
        
        ctrlThread = U.mkThread("probe-ctrl-thread") { t ⇒
            var dnSock: NCSocket = null
            var upSock: NCSocket = null
            
            var dnThread: Thread = null
            var upThread: Thread = null
    
            /**
              *
              */
            def closeAll(): Unit = {
                U.stopThread(dnThread)
                U.stopThread(upThread)
    
                dnThread = null
                upThread = null

                if (dnSock != null) dnSock.close()
                if (upSock != null) upSock.close()
        
                dnSock = null
                upSock = null
            }
            
            /**
              * 
              */
            def timeout(): Unit =
                if (!t.isInterrupted) U.ignoreInterrupt {
                    Thread.sleep(RETRY_TIMEOUT)
                }
            
            val cryptoKey = NCCipher.makeTokenKey(config.token)
            
            while (!t.isInterrupted)
                try {
                    logger.info(s"Establishing server connection to [" +
                        s"uplink=${config.upLink}, " +
                        s"downlink=${config.downLink}" +
                    s"]")

                    upSock = openUplinkSocket()
                    dnSock = openDownLinkSocket()

                    upSock.socket.setSoTimeout(SO_TIMEOUT)

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

                    upThread = U.mkThread("probe-uplink") { t ⇒
                        // Main reading loop.
                        while (!t.isInterrupted)
                            try
                                NCCommandManager.processServerMessage(upSock.read[NCProbeMessage](cryptoKey))
                            catch {
                                case _: InterruptedIOException | _: InterruptedException ⇒ ()
                                case _: EOFException ⇒ exit(t, s"Uplink server connection closed.")
                                case e: Exception ⇒ exit(t, s"Uplink connection failed: ${e.getMessage}")
                            }
                    }
                    
                    dnThread = U.mkThread("probe-downlink") { t ⇒
                        while (!t.isInterrupted)
                            try {
                                dnLinkQueue.synchronized {
                                    if (dnLinkQueue.isEmpty) {
                                        dnLinkQueue.wait(PING_TIMEOUT)
                                        
                                        if (!dnThread.isInterrupted && dnLinkQueue.isEmpty) {
                                            val pingMsg = NCProbeMessage("P2S_PING")
                                            
                                            pingMsg.setProbeToken(config.token)
                                            pingMsg.setProbeId(config.id)
                                            pingMsg.setProbeGuid(PROBE_GUID)
                                            
                                            dnSock.write(pingMsg, cryptoKey)
                                        }
                                    }
                                    else {
                                        val msg = dnLinkQueue.head

                                        // Write head first (without actually removing from queue).
                                        dnSock.write(msg, cryptoKey)

                                        // If sent ok - remove from queue.
                                        dnLinkQueue.dequeue()
                                    }
                                }
                            }
                            catch {
                                case _: InterruptedIOException | _: InterruptedException ⇒ ()
                                case _: EOFException ⇒ exit(t, s"Downlink server connection closed.")
                                case e: Exception ⇒ exit(t, s"Downlink connection failed: ${e.getMessage}")
                            }
                    }

                    // Bingo - start downlink and uplink!
                    upThread.start()
                    dnThread.start()

                    logger.info("Server connection established.")
                    
                    while (!t.isInterrupted && latch.getCount > 0) U.ignoreInterrupt {
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
    
                        if (e.getMessage != null)
                            logger.error(e.getMessage)
    
                        // Ack the handshake error message.
                        logger.error(s"Failed during server connection handshake (aborting).")
    
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
    
        U.stopThread(ctrlThread)
        
        super.stop()
    }
}
