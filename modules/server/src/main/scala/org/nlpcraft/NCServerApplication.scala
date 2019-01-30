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

package org.nlpcraft

import java.io.IOException
import java.net.{InetAddress, NetworkInterface}
import java.util.TimeZone

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.ds.NCDsManager
import org.nlpcraft.geo.NCGeoManager
import org.nlpcraft.rest.NCRestManager
import org.nlpcraft.util.NCGlobals
import org.nlpcraft.ignite.NCIgniteServer
import org.nlpcraft.nlp.dict.NCDictionaryManager
import org.nlpcraft.nlp.enrichers._
import org.nlpcraft.nlp.numeric.NCNumericManager
import org.nlpcraft.nlp.opennlp.NCNlpManager
import org.nlpcraft.nlp.preproc.NCPreProcessManager
import org.nlpcraft.nlp.spell.NCSpellCheckManager
import org.nlpcraft.nlp.synonym.NCSynonymManager
import org.nlpcraft.nlp.wordnet.NCWordNetManager
import org.nlpcraft.notification.NCNotificationManager
import org.nlpcraft.plugin.NCPluginManager
import org.nlpcraft.probe.NCProbeManager
import org.nlpcraft.proclog.NCProcessLogManager
import org.nlpcraft.query.NCQueryManager
import org.nlpcraft.user.NCUserManager
import org.nlpcraft.tx.NCTxManager
import org.nlpcraft.version.NCVersionManager

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
 * Main server entry-point.
 */
object NCServerApplication extends NCIgniteServer("ignite.xml") with LazyLogging {
    override def name() = "NlpCraft Server"

    // Starts all managers.
    private def startComponents(): Unit = {
        NCPluginManager.start()
        NCTxManager.start()
        NCDbManager.start()
        NCProcessLogManager.start()
        NCWordNetManager.start()
        NCDictionaryManager.start()
        NCSpellCheckManager.start()
        NCSynonymManager.start()
        NCPreProcessManager.start()
        NCGeoManager.start()
        NCNlpManager.start()
        NCNumericManager.start()
        NCNlpEnricherManager.start()
        NCNotificationManager.start()
        NCUserManager.start()
        NCDsManager.start()
        NCProbeManager.start()
        NCQueryManager.start()
        NCRestManager.start()
    }

    /**
      * Initializes server without blocking thread.
      */
    private[nlpcraft] def initialize() {
        startComponents()

        // Ack server start.
        ackStart()
        askVersion()
    }


    /**
      * Ask server version.
      */
    private def askVersion() {
        val tmz = TimeZone.getDefault
        val sysProps = System.getProperties

        var localHost: InetAddress = null
        var netItf: NetworkInterface = null

        try {
            localHost = InetAddress.getLocalHost

            netItf = NetworkInterface.getByInetAddress(localHost)
        }
        catch {
            case e: IOException ⇒ logger.warn(s"IO error during getting probe info: ${e.getMessage}")
        }

        var hwAddrs = ""

        if (netItf != null) {
            val addrs = netItf.getHardwareAddress

            if (addrs != null)
                hwAddrs = addrs.foldLeft("")((s, b) ⇒ s + (if (s == "") f"$b%02X" else f"-$b%02X"))
        }

        if (netItf != null) {
            val addrs = netItf.getHardwareAddress

            if (addrs != null)
                hwAddrs = addrs.foldLeft("")((s, b) ⇒ s + (if (s == "") f"$b%02X" else f"-$b%02X"))
        }

        implicit val ec: ExecutionContextExecutor = ExecutionContext.global

        // TODO:
        val f =
            NCVersionManager.askVersion(
                "-",
                //cfg.getVersionUrl,
                "server",
                Map(
                    //                    "PROBE_API_DATE" → ver.date,
                    //                    "PROBE_API_VERSION" → ver.version,
                    "PROBE_OS_VER" → sysProps.getProperty("os.version"),
                    "PROBE_OS_NAME" → sysProps.getProperty("os.name"),
                    "PROBE_OS_ARCH" → sysProps.getProperty("os.arch"),
                    "PROBE_START_TSTAMP" → G.nowUtcMs(),
                    "PROBE_TMZ_ID" → tmz.getID,
                    "PROBE_TMZ_ABBR" → tmz.getDisplayName(false, TimeZone.SHORT),
                    "PROBE_TMZ_NAME" → tmz.getDisplayName(),
                    "PROBE_SYS_USERNAME" → sysProps.getProperty("user.name"),
                    "PROBE_JAVA_VER" → sysProps.getProperty("java.version"),
                    "PROBE_JAVA_VENDOR" → sysProps.getProperty("java.vendor"),
                    "PROBE_HOST_NAME" → localHost.getHostName,
                    "PROBE_HOST_ADDR" → localHost.getHostAddress,
                    "PROBE_HW_ADDR" → hwAddrs
                ).map(p ⇒ p._1 → (if (p._2 != null) p._2.toString else null))
            )

        f.onSuccess { case m ⇒
            logger.info("Version information")

            m.foreach { case (key, v) ⇒ logger.info(s"$key=$v")}
        }

        f.onFailure {
            case e: IOException ⇒ logger.warn(s"Error reading version: ${e.getMessage}")
            case e: Throwable ⇒ logger.warn(s"Error reading version: ${e.getMessage}", e)
        }
    }

    // Stops all managers.
    private def stopComponents(): Unit = {
        NCRestManager.stop()
        NCQueryManager.stop()
        NCDsManager.stop()
        NCUserManager.stop()
        NCNotificationManager.stop()
        NCNlpEnricherManager.stop()
        NCNumericManager.stop()
        NCNlpManager.stop()
        NCGeoManager.stop()
        NCPreProcessManager.stop()
        NCSynonymManager.stop()
        NCSpellCheckManager.stop()
        NCDictionaryManager.stop()
        NCWordNetManager.stop()
        NCProcessLogManager.stop()
        NCDbManager.stop()
        NCTxManager.stop()
        NCPluginManager.stop()
    }

    /**
     * Stops the server by counting down (i.e. releasing) the lifecycle latch.
     */
    override def stop(): Unit = {
        stopComponents()

        super.stop()
    }

    /**
     * Code to execute within Ignite node.
     */
    override def start() {
        super.start()

        initialize()

        try
            NCGlobals.ignoreInterrupt {
                lifecycle.await()
            }
        finally
            stop()
    }
}
