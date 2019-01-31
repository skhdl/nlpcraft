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

package org.nlpcraft.probe

import java.io._
import java.net.{InetSocketAddress, ServerSocket, Socket, SocketTimeoutException}
import java.security.Key
import java.util.{Timer, TimerTask}
import java.util.concurrent.{ExecutorService, Executors}
import java.util.concurrent.atomic.AtomicBoolean

import org.nlpcraft.NCLifecycle
import org.nlpcraft._
import org.nlpcraft.ascii.NCAsciiTable
import org.nlpcraft.mdo.{NCDataSourceMdo, NCProbeMdo, NCProbeModelMdo, NCUserMdo}
import org.nlpcraft.nlp.NCNlpSentence
import org.nlpcraft.notification.NCNotificationManager
import org.nlpcraft.plugin.NCPluginManager
import org.nlpcraft.plugin.apis.NCProbeAuthenticationPlugin
import org.nlpcraft.proclog.NCProcessLogManager
import org.nlpcraft.query.NCQueryManager
import org.nlpcraft.socket.NCSocket

import scala.collection.{Map, mutable}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Probe manager.
  */
object NCProbeManager extends NCLifecycle("Probe manager") {
    private final val PROBES_ACK_FREQ_MS = 10 * 60 * 1000
    
    // Type safe and eager configuration container.
    private[probe] object Config extends NCConfigurable {
        private val p2sLink = G.splitEndpoint(hocon.getString("probe.links.p2s"))
        private val s2pLink = G.splitEndpoint(hocon.getString("probe.links.s2p"))
        
        val p2sHost: String = p2sLink._1
        val p2sPort: Int = p2sLink._2
        val s2pHost: String = s2pLink._1
        val s2pPort: Int = s2pLink._2
        
        val poolSize: Int = hocon.getInt("probe.poolSize")
        val reconnectTimeoutMs: Long = hocon.getLong("probe.reconnectTimeoutMs")
        val pingTimeoutMs: Long = hocon.getLong("probe.pingTimeoutMs")
        val soTimeoutMs: Int = hocon.getInt("probe.soTimeoutMs")
        
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
        timestamp: Long = G.nowUtcMs()
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
    
    private final val EC = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
    )
    
    private var p2sSrv: Thread = _
    private var s2pSrv: Thread = _
    private var pingSrv: Thread = _
    
    // All known probes keyed by probe key.
    private val probes = mutable.HashMap.empty[ProbeKey, ProbeHolder]
    // All probes pending complete handshake keyed by probe key.
    private val pending = mutable.HashMap.empty[ProbeKey, ProbeHolder]
    
    private var pool: ExecutorService = _
    private var isStopping: AtomicBoolean = _
    private var authPlugin: NCProbeAuthenticationPlugin = _
    private var timer: Timer = _
    
    /**
      *
      * @return
      */
    override def start(): NCLifecycle = {
        ensureStopped()
        
        Config.check()
        
        timer = new Timer()
    
        isStopping = new AtomicBoolean(false)
    
        authPlugin = NCPluginManager.getProbeAuthenticationPlugin
    
        pool = Executors.newFixedThreadPool(Config.poolSize)
    
        p2sSrv = startServer("p2s-srv", Config.p2sHost, Config.p2sPort, p2sHandler)
        s2pSrv = startServer("s2p-srv", Config.s2pHost, Config.s2pPort, s2pHandler)
    
        p2sSrv.start()
        s2pSrv.start()
    
        pingSrv = G.mkThread("probe-pinger") { t ⇒
            while (!t.isInterrupted) {
                G.sleep(Config.pingTimeoutMs)
            
                val pingMsg = NCProbeMessage("S2P_PING")
            
                probes.synchronized { probes.values }.map(_.probeKey).foreach(sendToProbe(_, pingMsg))
            }
        }
    
        pingSrv.start()
        
        timer.schedule(new TimerTask() {
            override def run(): Unit = {
                ackStats()
            }
        }, PROBES_ACK_FREQ_MS)
    
        super.start()
    }
    
    /**
      *
      */
    override def stop(): Unit = {
        checkStopping()
        
        timer.cancel()
    
        isStopping = new AtomicBoolean(true)
    
        G.shutdownPool(pool)
    
        G.stopThread(pingSrv)
        G.stopThread(p2sSrv)
        G.stopThread(s2pSrv)
    
        super.stop()
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
    private def sendToProbe(probeKey: ProbeKey, probeMsg: NCProbeMessage): Unit = {
        val (sock, cryptoKey) = probes.synchronized {
            probes.get(probeKey) match {
                case None ⇒ (null, null)
                case Some(h) ⇒ (h.s2pSocket, h.cryptoKey)
            }
        }
        
        if (sock != null)
            Future {
                try {
                    sock.write(probeMsg, cryptoKey)
                }
                catch {
                    case _: EOFException ⇒
                        logger.trace(s"Probe closed connection: $probeKey")
                        
                        closeAndRemoveHolder(probeKey)
        
                    case e: Throwable ⇒
                        logger.error(s"S2P socket error [" +
                            s"sock=$sock, " +
                            s"probeKey=$probeKey, " +
                            s"probeMsg=$probeMsg" +
                            s"error=${e.getLocalizedMessage}" +
                            s"]")
                        
                        closeAndRemoveHolder(probeKey)
                }
            }
        else
            logger.warn(s"Sending message to unknown probe (ignoring) [" +
                s"probeKey=$probeKey, " +
                s"probeMsg=$probeMsg" +
                s"]")
    }
    
    /**
      * Starts a server (thread) with given name, local bind port and processing function.
      *
      * @param name Server name.
      * @param host Local host/IP to bind.
      * @param port Local port to bind.
      * @param fn Function.
      */
    private def startServer(name: String, host: String, port: Int, fn: NCSocket ⇒ Unit): Thread =
        G.mkThread(s"probe-mgr-$name") { t ⇒
            var srv: ServerSocket = null
            
            while (!t.isInterrupted)
                try {
                    srv = new ServerSocket()
                    
                    srv.bind(new InetSocketAddress(host, port))
                    
                    logger.trace(s"'$name' server is listening on [" +
                        s"host=$host, " +
                        s"port=$port" +
                        s"]")
                    
                    srv.setSoTimeout(Config.soTimeoutMs)
                    
                    while (!t.isInterrupted) {
                        var sock: Socket = null
                        
                        try {
                            sock = srv.accept()
                            
                            logger.trace(s"'$name' server accepted new connection.")
                        }
                        catch {
                            case _: InterruptedIOException ⇒ // No-op.
                            // Note that server socket must be closed and created again.
                            // So, error should be thrown.
                            case e: Exception ⇒
                                G.close(sock)
                                
                                throw e
                        }
                        
                        if (sock != null) {
                            val fut = Future {
                                fn(NCSocket(sock, sock.getRemoteSocketAddress.toString))
                            }
                            
                            fut.onFailure {
                                case e: NCE ⇒ logger.warn(e.getMessage, e)
                                case e: Throwable ⇒ logger.warn(s"Ignoring socket error: ${e.getLocalizedMessage}", e)
                            }
                        }
                    }
                }
                catch {
                    case e: Exception ⇒
                        if (!isStopping.get) {
                            // Release socket asap.
                            G.close(srv)
                            
                            val ms = Config.reconnectTimeoutMs
                            
                            // Server socket error must be logged.
                            logger.warn(s"'$name' server error, re-starting in ${ms / 1000} sec.", e)
                            
                            G.sleep(ms)
                        }
                }
                finally {
                    G.close(srv)
                }
        }
    
    /**
      * Processes socket for receiving messages from a probe.
      *
      * @param sock Up-link (probe-to-server) socket to process.
      */
    @throws[NCE]
    @throws[IOException]
    private def p2sHandler(sock: NCSocket): Unit = {
        // Read header token hash message.
        val tokHash = sock.read[String]()
        
        val cryptoKey = authPlugin.acquireKey(tokHash).getOrElse(
            throw new NCE(s"Rejecting probe connection due to unknown probe token hash: $tokHash")
        )
    
        // Read handshake probe message.
        val hsMsg = sock.read[NCProbeMessage](cryptoKey)
    
        require(hsMsg.getType == "INIT_HANDSHAKE")
    
        // Probe key components.
        val probeTkn = hsMsg.getProbeToken
        val probeId = hsMsg.getProbeId
        val probeGuid = hsMsg.getProbeGuid
    
        logger.info(s"P2S handshake message received [" +
            s"probeToken=$probeTkn, " +
            s"probeId=$probeId, " +
            s"proveGuid=$probeGuid" +
            s"]")
    
        val probeKey = ProbeKey(probeTkn, probeId, probeGuid)
    
        val threadName = "probe-p2s-" + probeId.toLowerCase + "-" + probeGuid.toLowerCase
    
        val p2sThread = G.mkThread(threadName) { t ⇒
            try {
                sock.socket.setSoTimeout(Config.soTimeoutMs)
            
                while (!t.isInterrupted)
                    try {
                        processMessageFromProbe(sock.read[NCProbeMessage](cryptoKey))
                    }
                    catch {
                        case _: SocketTimeoutException ⇒ ()
                        case _: InterruptedException ⇒ () // Normal thread interruption.
                        case _: InterruptedIOException ⇒ () // Normal thread interruption.
                    
                        case _: EOFException ⇒
                            logger.info(s"Probe closed p2s connection: $probeKey")
                        
                            t.interrupt()
                    
                        case e: Throwable ⇒
                            logger.info(s"Error reading probe p2s socket (${e.getMessage}): $probeKey")
                        
                            t.interrupt()
                    }
            }
            finally {
                closeAndRemoveHolder(probeKey)
            }
        }
    
        def respond(typ: String): Unit = {
            val msg = NCProbeMessage(typ)
        
            logger.trace(s"Sending to probe ($typ): $msg")
        
            sock.write(msg, cryptoKey)
        }
    
        // Update probe holder.
        val holder = pending.synchronized {
            pending.remove(probeKey) match {
                case None ⇒ () // Probe has been removed already?
                    respond("P2S_PROBE_NOT_FOUND")
                
                    null
            
                case Some(h) ⇒
                    h.p2sThread = p2sThread
                    h.p2sSocket = sock
                
                    h
            }
        }
    
        if (holder != null)
            probes.synchronized {
                probes += probeKey → holder
            
                addProbeToTable(mkProbeTable, holder).info(logger, Some("\nNew probe registered:"))
            
                ackStats()
            
                // Bingo!
                respond("P2S_PROBE_OK")
            
                p2sThread.start()
            }
    }
    
    /**
      *
      * @param probeKey Probe key.
      */
    private def isMultipleProbeRegistrations(probeKey: ProbeKey): Boolean =
        probes.synchronized {
            probes.values.count(p ⇒
                p.probeKey.probeToken == probeKey.probeToken &&
                    p.probeKey.probeId == probeKey.probeId
            ) > 1
        }
    
    /**
      * Processes socket for sending messages to a probe.
      *
      * @param sock S2P socket to process.
      */
    @throws[NCE]
    @throws[IOException]
    private def s2pHandler(sock: NCSocket): Unit = {
        // Read header probe token hash message.
        val tokHash = sock.read[String]()
        
        val cryptoKey = authPlugin.acquireKey(tokHash) match {
            case Some(key) ⇒
                sock.write(NCProbeMessage("S2P_HASH_CHECK_OK"))
                
                key

            case None ⇒
                sock.write(NCProbeMessage("S2P_HASH_CHECK_UNKNOWN"))
    
                throw new NCE(s"Rejecting probe connection due to unknown probe token hash: $tokHash")
        }
        
        // Read handshake probe message.
        val hsMsg = sock.read[NCProbeMessage](cryptoKey)
    
        require(hsMsg.getType == "INIT_HANDSHAKE")
    
        // Probe key components.
        val probeTkn = hsMsg.getProbeToken
        val probeId = hsMsg.getProbeId
        val probeGuid = hsMsg.getProbeGuid
    
        val probeKey = ProbeKey(probeTkn, probeId, probeGuid)
    
        logger.info(s"S2P handshake received [" +
            s"probeToken=$probeTkn, " +
            s"probeId=$probeId, " +
            s"proveGuid=$probeGuid" +
            s"]")
    
        def respond(typ: String, pairs: (String, Serializable)*): Unit = {
            val msg = NCProbeMessage(typ, pairs:_*)
        
            logger.trace(s"Sending to probe ($typ): $msg")
        
            sock.write(msg, cryptoKey)
        }
    
        if (isMultipleProbeRegistrations(probeKey))
            respond("S2P_PROBE_MULTIPLE_INSTANCES")
        else {
            val probeApiVer = hsMsg.data[String]("PROBE_API_VERSION")
            val srvApiVer = NCVersion.getCurrent
            
            if (probeApiVer != srvApiVer.version)
                respond("S2P_PROBE_VERSION_MISMATCH")
            else {
                val models =
                    hsMsg.data[List[(String, String, String)]]("PROBE_MODELS_DS").
                        map { case (dsId, dsName, dsVer) ⇒
                            NCProbeModelMdo(
                                id = dsId,
                                name = dsName,
                                version = dsVer
                            )
                        }.toSet
    
                probes.synchronized {
                    // Check that this probe's models haven't been already deployed
                    // by another probe - in which case reject this probe.
                    // NOTE: model can be deployed only once by a probe.
                    models.find(mdl ⇒ probes.values.flatMap(_.probe.models).exists(_ == mdl))
                } match {
                    case Some(m) ⇒
                        // Send direct message here.
                        respond("S2P_PROBE_DUP_MODEL", "PROBE_MODEL_ID" → m.id)
        
                    case None ⇒
                        val probeApiDate = hsMsg.data[java.time.LocalDate]("PROBE_API_DATE")
                        
                        val holder = ProbeHolder(
                            probeKey,
                            NCProbeMdo(
                                probeToken = hsMsg.data[String]("PROBE_TOKEN"),
                                probeId = hsMsg.data[String]("PROBE_ID"),
                                probeGuid = probeGuid,
                                probeApiVersion = probeApiVer,
                                probeApiDate = probeApiDate,
                                osVersion = hsMsg.data[String]("PROBE_OS_VER"),
                                osName = hsMsg.data[String]("PROBE_OS_NAME"),
                                osArch = hsMsg.data[String]("PROBE_OS_ARCH"),
                                startTstamp = new java.sql.Timestamp(hsMsg.data[Long]("PROBE_START_TSTAMP")),
                                tmzId = hsMsg.data[String]("PROBE_TMZ_ID"),
                                tmzAbbr = hsMsg.data[String]("PROBE_TMZ_ABBR"),
                                tmzName = hsMsg.data[String]("PROBE_TMZ_NAME"),
                                userName = hsMsg.data[String]("PROBE_SYS_USERNAME"),
                                javaVersion = hsMsg.data[String]("PROBE_JAVA_VER"),
                                javaVendor = hsMsg.data[String]("PROBE_JAVA_VENDOR"),
                                hostName = hsMsg.data[String]("PROBE_HOST_NAME"),
                                hostAddr = hsMsg.data[String]("PROBE_HOST_ADDR"),
                                macAddr = hsMsg.dataOpt[String]("PROBE_HW_ADDR").getOrElse(""),
                                models = models
                            ),
                            null, // No P2S socket yet.
                            sock,
                            null, // No P2S thread yet.
                            cryptoKey
                        )
            
                        pending.synchronized {
                            pending += probeKey → holder
                        }
            
                        // Bingo!
                        respond("S2P_PROBE_OK")
                }
            }
        }
    }
    
    /**
      * Processes the messages received from the probe.
      *
      * @param probeMsg Probe's message to process.
      */
    private def processMessageFromProbe(probeMsg: NCProbeMessage): Unit = {
        val probeKey = ProbeKey(
            probeMsg.getProbeToken,
            probeMsg.getProbeId,
            probeMsg.getProbeGuid
        )
        
        val knownProbe = probes.synchronized {
            probes.contains(probeKey)
        }
        
        if (!knownProbe)
            logger.error(s"Received message from unknown probe (ignoring): $probeKey]")
        else {
            val typ = probeMsg.getType
            
            typ match {
                case "P2S_PING" ⇒ ()
                
                case "P2S_ASK_RESULT" ⇒
                    val srvReqId = probeMsg.data[String]("srvReqId")
                    
                    try {
                        val errOpt = probeMsg.dataOpt[String]("error")
                        val resTypeOpt = probeMsg.dataOpt[String]("resType")
                        val resBodyOpt = probeMsg.dataOpt[String]("resBody")
                        
                        if (errOpt.isDefined) { // Error.
                            val err = errOpt.get
                            
                            NCQueryManager.setError(
                                srvReqId,
                                err
                            )
    
                            logger.trace(s"Error result processed [srvReqId=$srvReqId, error=$err]")
                        }
                        else { // OK result.
                            require(resTypeOpt.isDefined && resBodyOpt.isDefined, "Result defined")
    
                            val resType = resTypeOpt.get
                            val resBody = resBodyOpt.get
    
                            NCQueryManager.setResult(
                                srvReqId,
                                resType,
                                resBody
                            )
    
                            logger.trace(s"OK result processed [srvReqId=$srvReqId]")
                        }
                    }
                    catch {
                        case e: Throwable ⇒
                            logger.error(s"Failed to process probe message: $typ", e)
        
                            NCQueryManager.setError(
                                srvReqId,
                                "Processing failed due to a system error."
                            )
                    }
                    
                
                case _ ⇒
                    logger.error(s"Received unrecognized probe message (ignoring): $probeMsg")
            }
        }
    }
    
    /**
      *
      * @param probeGuid
      * @return
      */
    private def getProbeForGuid(probeGuid: String): Option[ProbeHolder] =
        probes.synchronized {
            probes.values.find(_.probeKey.probeGuid == probeGuid)
        }
    
    /**
      *
      * @param modelId
      * @return
      */
    private def getProbeForModelId(modelId: String): Option[ProbeHolder] =
        probes.synchronized {
            probes.values.find(_.probe.models.exists(_.id == modelId))
        }
    
    /**
      *
      */
    private def ackStats(): Unit = {
        val tbl = mkProbeTable
    
        probes.synchronized { probes.values }.toSeq.sortBy(_.timestamp).foreach(addProbeToTable(tbl, _))
        
        tbl.info(logger, Some(s"\nRegistered Probes Statistics (total ${probes.size}):"))
    }
    
    /**
      *
      * @return
      */
    private def mkProbeTable: NCAsciiTable =  {
        val tbl = NCAsciiTable()
        
        tbl #= (
            "Probe ID",
            "OS",
            "Timezone",
            "API ver.",
            "Uptime",
            "Models"
        )
        
        tbl
    }
    
    /**
      *
      * @param tbl ASCII table to add to.
      * @param hol Probe holder to add.
      */
    private def addProbeToTable(tbl: NCAsciiTable, hol: ProbeHolder): NCAsciiTable = {
        val delta = (G.nowUtcMs() / 1000) - (hol.timestamp / 1000)
        
        tbl += (
            hol.probe.probeId,
            s"${hol.probe.osName} ver. ${hol.probe.osVersion}",
            s"${hol.probe.tmzAbbr}, ${hol.probe.tmzId}",
            s"${hol.probe.probeApiVersion}",
            s"${delta / 3600}:${(delta % 3600) / 60}:${delta % 60}",
            s"${hol.probe.models.size}"
        )
        
        tbl
    }

    /**
      *
      * @param srvReqId
      * @param usr
      * @param ds
      * @param txt
      * @param nlpSen
      * @param usrAgent
      * @param rmtAddr
      * @param isTest
      */
    @throws[NCE]
    def askProbe(
        srvReqId: String,
        usr: NCUserMdo,
        ds: NCDataSourceMdo,
        txt: String,
        nlpSen: NCNlpSentence,
        usrAgent: Option[String],
        rmtAddr: Option[String],
        isTest: Boolean): Unit = {
        ensureStarted()
        
        val senMeta =
            Map(
                "NORMTEXT" → nlpSen.text,
                "USER_AGENT" → usrAgent.orNull,
                "REMOTE_ADDR" → rmtAddr.orNull,
                "RECEIVE_TSTAMP" → G.nowUtcMs(),
                "FIRST_NAME" → usr.firstName,
                "LAST_NAME" → usr.lastName,
                "EMAIL" → usr.email,
                "SIGNUP_DATE" → usr.createdOn.getTime,
                "IS_ADMIN" → usr.isAdmin,
                "AVATAR_URL" → usr.avatarUrl
            ).map(p ⇒ p._1 → p._2.asInstanceOf[java.io.Serializable])
        
        getProbeForModelId(ds.modelId) match {
            case Some(holder) ⇒
                sendToProbe(
                    holder.probeKey,
                    NCProbeMessage("S2P_ASK",
                        "srvReqId" → srvReqId,
                        "txt" → txt,
                        "nlpSen" → nlpSen.asInstanceOf[java.io.Serializable],
                        "senMeta" → senMeta.asInstanceOf[java.io.Serializable],
                        "userId" → usr.id,
                        "dsId" → ds.id,
                        "dsModelId" → ds.modelId,
                        "dsName" → ds.name,
                        "dsDesc" → ds.shortDesc,
                        "dsModelCfg" → ds.modelConfig.orNull,
                        "test" → isTest
                    )
                )

                NCProcessLogManager.updateProbe(
                    srvReqId,
                    holder.probe
                )

            case None ⇒ throw new NCE(s"Unknown model ID: ${ds.modelId}")
        }
    }
    
    /**
      *
      * @param probeGuid
      */
    @throws[NCE]
    def stopProbe(probeGuid: String): Unit = {
        ensureStarted()
        
        getProbeForGuid(probeGuid) match {
            case Some(holder) ⇒
                val probeKey = holder.probeKey
                
                sendToProbe(
                    probeKey,
                    NCProbeMessage("S2P_STOP_PROBE")
                )
    
                // Notification.
                NCNotificationManager.addEvent("NC_PROBE_STOP",
                    "probeGuid" → probeKey.probeGuid,
                    "probeId" → probeKey.probeId,
                    "probeToken" → probeKey.probeToken
                )

            case None ⇒ throw new NCE(s"Unknown probe GUID: $probeGuid")
        }
    }
    
    /**
      *
      * @param probeGuid
      */
    @throws[NCE]
    def restartProbe(probeGuid: String): Unit = {
        ensureStarted()
    
        getProbeForGuid(probeGuid) match {
            case Some(holder) ⇒
                val probeKey = holder.probeKey
                
                sendToProbe(
                    probeKey,
                    NCProbeMessage("S2P_RESTART_PROBE")
                )
    
                // Notification.
                NCNotificationManager.addEvent("NC_PROBE_RESTART",
                    "probeGuid" → probeKey.probeGuid,
                    "probeId" → probeKey.probeId,
                    "probeToken" → probeKey.probeToken
                )
                
            case None ⇒ throw new NCE(s"Unknown probe GUID: $probeGuid")
        }
    }
    
    /**
      * Gets all active probes.
      * 
      * @return
      */
    @throws[NCE]
    def getAllProbes: Seq[NCProbeMdo] = {
        ensureStarted()
    
        probes.synchronized { probes.values }.map(_.probe).toSeq
    }
    
    /**
      *
      * @param usrId
      * @param dsId
      */
    @throws[NCE]
    def clearConversation(usrId: Long, dsId: Long): Unit = {
        ensureStarted()
        
        val msg = NCProbeMessage("S2P_CLEAR_CONV",
            "usrId" → usrId,
            "dsId" → dsId
        )
    
        // Ping all probes.
        probes.synchronized { probes.values }.map(_.probeKey).foreach(sendToProbe(_, msg))
    
        // Notification.
        NCNotificationManager.addEvent("NC_CLEAR_CONV",
            "usrId" → usrId,
            "dsId" → dsId
        )
    }
}
