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

package org.nlpcraft.server.probe

import java.io._
import java.net.{InetSocketAddress, ServerSocket, Socket, SocketTimeoutException}
import java.security.Key
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ExecutorService, Executors}

import org.nlpcraft.common.ascii.NCAsciiTable
import org.nlpcraft.common.nlp.NCNlpSentence
import org.nlpcraft.common.socket.NCSocket
import org.nlpcraft.common.version.NCVersion
import org.nlpcraft.common.{NCLifecycle, _}
import org.nlpcraft.probe.mgrs.NCProbeMessage
import org.nlpcraft.server.NCConfigurable
import org.nlpcraft.server.mdo.{NCDataSourceMdo, NCProbeMdo, NCProbeModelMdo, NCUserMdo}
import org.nlpcraft.server.notification.NCNotificationManager
import org.nlpcraft.server.plugin.NCPluginManager
import org.nlpcraft.server.plugin.apis.NCProbeAuthenticationPlugin
import org.nlpcraft.server.proclog.NCProcessLogManager
import org.nlpcraft.server.query.NCQueryManager

import scala.collection.{Map, mutable}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Probe manager.
  */
object NCProbeManager extends NCLifecycle("Probe manager") {
    // Type safe and eager configuration container.
    private[probe] object Config extends NCConfigurable {
        final val prefix = "server.probe"
        
        private val dnLink = U.splitEndpoint(getString(s"$prefix.links.downLink"))
        private val upLink = U.splitEndpoint(getString(s"$prefix.links.upLink"))
        
        val dnHost: String = dnLink._1
        val dnPort: Int = dnLink._2
        
        val upHost: String = upLink._1
        val upPort: Int = upLink._2
        
        val poolSize: Int = getInt(s"$prefix.poolSize")
        val reconnectTimeoutMs: Long = getLong(s"$prefix.reconnectTimeoutMs")
        val pingTimeoutMs: Long = getLong(s"$prefix.pingTimeoutMs")
        val soTimeoutMs: Int = getInt(s"$prefix.soTimeoutMs")
        
        override def check(): Unit = {
            if (!(dnPort >= 0 && dnPort <= 65535))
                abortError(s"Configuration property '$prefix.links.upLink' must be >= 0 and <= 65535: $dnPort")
            if (!(upPort >= 0 && upPort <= 65535))
                abortError(s"Configuration property '$prefix.links.downLink' must be >= 0 and <= 65535: $upPort")
            if (reconnectTimeoutMs <= 0)
                abortError(s"Configuration property '$prefix.reconnectTimeoutMs' must be > 0: $reconnectTimeoutMs")
            if (poolSize <= 0)
                abortError(s"Configuration property '$prefix.poolSize' must be > 0: $poolSize")
            if (soTimeoutMs <= 0)
                abortError(s"Configuration property '$prefix.soTimeoutMs' must be > 0: $soTimeoutMs")
            if (pingTimeoutMs <= 0)
                abortError(s"Configuration property '$prefix.pingTimeoutMs' timeout must be > 0: $pingTimeoutMs")
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
        var dnSocket: NCSocket,
        var upSocket: NCSocket,
        var dnThread: Thread, // Separate thread listening for messages from the probe.
        cryptoKey: Key, // Encryption key.
        timestamp: Long = U.nowUtcMs()
    ) {
        /**
          *
          */
        def close(): Unit = {
            if (dnThread != null)
                U.stopThread(dnThread)
            
            if (upSocket != null)
                upSocket.close()
            
            if (dnSocket != null)
                dnSocket.close()
        }
    }
    
    private var dnSrv: Thread = _
    private var upSrv: Thread = _
    private var pingSrv: Thread = _
    
    // All known probes keyed by probe key.
    private val probes = mutable.HashMap.empty[ProbeKey, ProbeHolder]
    // All probes pending complete handshake keyed by probe key.
    private val pending = mutable.HashMap.empty[ProbeKey, ProbeHolder]
    
    private var pool: ExecutorService = _
    private var isStopping: AtomicBoolean = _
    private var authPlugin: NCProbeAuthenticationPlugin = _
    
    /**
      *
      * @return
      */
    override def start(): NCLifecycle = {
        ensureStopped()
        
        Config.check()
        
        isStopping = new AtomicBoolean(false)
    
        authPlugin = NCPluginManager.getProbeAuthenticationPlugin
    
        pool = Executors.newFixedThreadPool(Config.poolSize)
    
        dnSrv = startServer("Downlink", Config.dnHost, Config.dnPort, downLinkHandler)
        upSrv = startServer("Uplink", Config.upHost, Config.upPort, upLinkHandler)
    
        dnSrv.start()
        upSrv.start()
    
        pingSrv = U.mkThread("probe-pinger") { t ⇒
            while (!t.isInterrupted) {
                U.sleep(Config.pingTimeoutMs)
            
                val pingMsg = NCProbeMessage("S2P_PING")
            
                probes.synchronized { probes.values }.map(_.probeKey).foreach(sendToProbe(_, pingMsg))
            }
        }
    
        pingSrv.start()
        
        super.start()
    }
    
    /**
      *
      */
    override def stop(): Unit = {
        checkStopping()
        
        isStopping = new AtomicBoolean(true)
    
        U.shutdownPools(pool)
    
        U.stopThread(pingSrv)
        U.stopThread(dnSrv)
        U.stopThread(upSrv)
    
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
                case Some(h) ⇒ (h.upSocket, h.cryptoKey)
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
                        logger.error(s"Uplink socket error [" +
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
        new Thread(s"probe-mgr-${name.toLowerCase}") {
            private var srv: ServerSocket = null
            @volatile private var stopped = false

            override def isInterrupted: Boolean = super.isInterrupted || stopped

            override def interrupt(): Unit = {
                super.interrupt()

                U.close(srv)

                stopped = true
            }

            override def run(): Unit = {
                logger.trace(s"Thread started: $name")

                try {
                    body()

                    logger.trace(s"Thread exited: $name")
                }
                catch {
                    case _: InterruptedException ⇒ logger.trace(s"Thread interrupted: $name")
                    case e: Throwable ⇒ logger.error(s"Unexpected error during thread execution: $name", e)
                }
                finally
                    stopped = true
            }

            private def body(): Unit =
                while (!isInterrupted)
                    try {
                        srv = new ServerSocket()

                        srv.bind(new InetSocketAddress(host, port))

                        logger.trace(s"$name server is listening on '$host:$port'")

                        srv.setSoTimeout(Config.soTimeoutMs)

                        while (!isInterrupted) {
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
                                    U.close(sock)

                                    throw e
                            }

                            if (sock != null) {
                                val fut = Future {
                                    fn(NCSocket(sock, sock.getRemoteSocketAddress.toString))
                                }

                                fut.onFailure {
                                    case e: NCE ⇒ logger.warn(e.getMessage, e)
                                    case _: EOFException ⇒ () // Just ignoring.
                                    case e: Throwable ⇒ logger.warn(s"Ignoring socket error: ${e.getLocalizedMessage}")
                                }
                            }
                        }
                    }
                    catch {
                        case e: Exception ⇒
                            if (!isStopping.get) {
                                // Release socket asap.
                                U.close(srv)

                                val ms = Config.reconnectTimeoutMs

                                // Server socket error must be logged.
                                logger.warn(s"$name server error, re-starting in ${ms / 1000} sec.", e)

                                U.sleep(ms)
                            }
                    }
                    finally {
                        U.close(srv)
                    }
        }
    
    /**
      * Processes socket for downlink messages.
      *
      * @param sock Downlink socket.
      */
    @throws[NCE]
    @throws[IOException]
    private def downLinkHandler(sock: NCSocket): Unit = {
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
    
        logger.info(s"Downlink handshake message received [" +
            s"probeToken=$probeTkn, " +
            s"probeId=$probeId, " +
            s"proveGuid=$probeGuid" +
            s"]")
    
        val probeKey = ProbeKey(probeTkn, probeId, probeGuid)
    
        val threadName = "probe-downlink-" + probeId.toLowerCase + "-" + probeGuid.toLowerCase
    
        val p2sThread = U.mkThread(threadName) { t ⇒
            try {
                sock.socket.setSoTimeout(Config.soTimeoutMs)
            
                while (!t.isInterrupted)
                    try {
                        processMessageFromProbe(sock.read[NCProbeMessage](cryptoKey))
                    }
                    catch {
                        // Normal thread interruption.
                        case _: SocketTimeoutException | _: InterruptedException | _: InterruptedIOException ⇒ ()
                    
                        case _: EOFException ⇒
                            logger.info(s"Probe closed downlink connection: $probeKey")
                        
                            t.interrupt()
                    
                        case e: Throwable ⇒
                            logger.info(s"Error reading probe downlink socket (${e.getMessage}): $probeKey")
                        
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
                    h.dnThread = p2sThread
                    h.dnSocket = sock
                
                    h
            }
        }
    
        if (holder != null)
            probes.synchronized {
                probes += probeKey → holder
            
                addProbeToTable(mkProbeTable, holder).info(logger, Some("New probe registered:"))
            
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
      * Processes socket for uplink messages.
      *
      * @param sock Uplink socket.
      */
    @throws[NCE]
    @throws[IOException]
    private def upLinkHandler(sock: NCSocket): Unit = {
        // Read header probe token hash message.
        val tokHash = sock.read[String]()
    
        var cryptoKey: Key = null
    
        def respond(typ: String, pairs: (String, Serializable)*): Unit = {
            val msg = NCProbeMessage(typ, pairs:_*)
        
            logger.trace(s"Sending to probe ($typ): $msg")
        
            sock.write(msg, cryptoKey)
        }
    
        cryptoKey = authPlugin.acquireKey(tokHash) match {
            case Some(key) ⇒
                respond("S2P_HASH_CHECK_OK")
                
                key

            case None ⇒
                respond("S2P_HASH_CHECK_UNKNOWN")
    
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
    
        logger.info(s"Uplink handshake received [" +
            s"probeToken=$probeTkn, " +
            s"probeId=$probeId, " +
            s"proveGuid=$probeGuid" +
            s"]")
    
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
                            null, // No downlink socket yet.
                            sock,
                            null, // No downlink thread yet.
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
      * @param modelId
      * @return
      */
    private def getProbeForModelId(modelId: String): Option[ProbeHolder] =
        probes.synchronized {
            probes.values.find(_.probe.models.exists(_.id == modelId))
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
        val delta = (U.nowUtcMs() / 1000) - (hol.timestamp / 1000)
        
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
                "RECEIVE_TSTAMP" → U.nowUtcMs(),
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
