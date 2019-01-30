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

import java.io.IOException
import java.net.{InetAddress, NetworkInterface}
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft._
import org.nlpcraft.ascii.NCAsciiTable
import org.nlpcraft.nlp.dict.NCDictionaryManager
import org.nlpcraft.nlp.numeric.NCNumericManager
import org.nlpcraft.nlp.opennlp.NCNlpManager
import org.nlpcraft.probe.dev.NCProbeConfig
import org.nlpcraft.probe.mgrs.cmd.NCCommandManager
import org.nlpcraft.probe.mgrs.conn.NCProbeConnectionManager
import org.nlpcraft.probe.mgrs.deploy.NCDeployManager
import org.nlpcraft.probe.mgrs.exit.NCExitManager
import org.nlpcraft.probe.mgrs.model.NCModelManager
import org.nlpcraft.probe.mgrs.nlp.NCProbeNlpManager
import org.nlpcraft.probe.mgrs.nlp.conversation.NCConversationManager
import org.nlpcraft.probe.mgrs.nlp.enrichers.context.NCContextEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.coordinates.NCCoordinatesEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.dictionary.NCDictionaryEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.function.NCFunctionEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.model.NCModelEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.stopword.NCStopWordEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.suspicious.NCSuspiciousNounsEnricher
import org.nlpcraft.probe.mgrs.nlp.post.{NCPostChecker, NCPostEnrichCollapser, NCPostEnricher}
import org.nlpcraft.probe.mgrs.nlp.pre.NCNlpPreChecker
import org.nlpcraft.version.NCVersionManager

import scala.compat.Platform._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.control.Exception._

/**
  * Basic probe runner to be used by both command line and dev in-process apps.
  */
object NCProbeRunner extends LazyLogging with NCDebug {
    private final val VER = /*@version*/"0.5.0"
    private final val BUILD: String = /*@build*/new SimpleDateFormat("MMddyyyy").format(new Date())
    
    private val startMsec = currentTime
    
    /**
      * Prints ASCII-logo.
      */
    private def asciiLogo(cfg: NCProbeConfig) {
        val NL = System getProperty "line.separator"
        
        val copyright = s"Copyright (C) DataLingvo, Inc."
        
        val s = NL +
            raw"    _   ____      ______           ______   $NL" +
            raw"   / | / / /___  / ____/________ _/ __/ /_  $NL" +
            raw"  /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/  $NL" +
            raw" / /|  / / /_/ / /___/ /  / /_/ / __/ /_    $NL" +
            raw"/_/ |_/_/ .___/\____/_/   \__,_/_/  \__/    $NL" +
            raw"       /_/                                  $NL$NL" +
            s"Data Probe$NL" +
            s"Version: $VER - $BUILD$NL" +
            raw"$copyright$NL"
        
        println(s)
    }
    
    /**
      * Asks server start.
      */
    private def ackStart() {
        val dur = s"[${G.format((currentTime - startMsec) / 1000.0, 2)} sec]"
        
        val tbl = NCAsciiTable()
        
        tbl.margin(top = 1)
        
        tbl += s"Probe started $dur"
        
        tbl.info(logger)
    }
    
    /**
      *
      * @param cfg Probe configuration.
      */
    private def ackConfig(cfg: NCProbeConfig): Unit = {
        val tbl = NCAsciiTable()
        
        val ver = NCProbeVersion.getCurrent
        
        def nvl(obj: Any): String = if (obj == null) "" else obj.toString
        
        tbl += ("Probe ID", cfg.getId)
        tbl += ("Probe Token", cfg.getToken)
        tbl += ("API Version", ver.version + ", " + ver.date.toString)
        tbl += ("Down-Link", cfg.getDownLink)
        tbl += ("Up-Link", cfg.getUpLink)
        tbl += ("In-Process Provider", if (cfg.getProvider == null) nvl(null) else cfg.getProvider.getClass)
        tbl += ("JARs Folder", nvl(cfg.getJarsFolder))
        
        tbl.info(logger, Some("Probe Configuration:"))
        
        logger.info("Set '-DNLPCRAFT_PROBE_SILENT=true' JVM system property to turn off verbose probe logging.")
    }

    private def askVersion(cfg: NCProbeConfig): Unit = {
        val ver = NCProbeVersion.getCurrent
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

        val f =
            NCVersionManager.askVersion(
                cfg.getVersionUrl,
                "probe",
                Map(
                    "PROBE_API_DATE" → ver.date,
                    "PROBE_API_VERSION" → ver.version,
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
                )
            )

        f.onSuccess { case m ⇒
            logger.info("Version information")

            m.foreach { case (key, v) ⇒ logger.info(s"$key=$v")}
        }

        f.onFailure {
            case e: Throwable ⇒ logger.warn(s"Error reading version: ${e.getMessage}")
        }
    }
    
    /**
      *
      * @param cfg Probe configuration to start with.
      * @return
      */
    def startProbe(cfg: NCProbeConfig): Int = {
        asciiLogo(cfg)
        ackConfig(cfg)
        askVersion(cfg)
        
        catching(classOf[Throwable]) either startManagers(cfg) match {
            case Left(e) ⇒
                e match {
                    case x: NCException ⇒ logger.error(s"Failed to start probe due to: ${x.getMessage}")
                    case x: Throwable ⇒ logger.trace("Unexpected error.", x.printStackTrace())
                }
                
                logger.error(s"Probe exit code: ${NCExitManager.EXIT_FAIL}")
    
                NCExitManager.EXIT_FAIL
              
            // No exception on managers start - continue...
            case _ ⇒
                ackStart()
    
                // Block & wait for the exit.
                val exitCode = NCExitManager.awaitExit()
                
                if (exitCode == NCExitManager.EXIT_OK || exitCode == NCExitManager.RESTART) {
                    if (exitCode == NCExitManager.RESTART)
                        logger.info("Probe is re-starting...")
                    else
                        logger.info(s"Probe is stopping...")
                }
                else
                    logger.error(s"Probe exit code: $exitCode")
                
                try
                    stopManagers()
                catch {
                    case _: Throwable ⇒ () // Ignore.
                }
    
                exitCode
        }
    }
    
    /**
      *
      * @param cfg Configuration to start probe with.
      * @return
      */
    private def startManagers(cfg: NCProbeConfig): Unit = {
        // Order is important!
        NCNlpManager.start()
        NCNumericManager.start()
        NCExitManager .startWithConfig(cfg)
        NCDeployManager.startWithConfig(cfg)
        NCModelManager.startWithConfig(cfg)
        NCCommandManager.startWithConfig(cfg)
        NCDictionaryManager.start()
        NCStopWordEnricher.start()
        NCModelEnricher.start()
        NCFunctionEnricher.start()
        NCSuspiciousNounsEnricher.start()
        NCNlpPreChecker.start()
        NCPostEnrichCollapser.start()
        NCPostEnricher.start()
        NCPostChecker.start()
        NCContextEnricher.start()
        NCDictionaryEnricher.start()
        NCCoordinatesEnricher.start()
        NCConversationManager.start()
        NCProbeNlpManager.startWithConfig(cfg)
        NCProbeConnectionManager.startWithConfig(cfg)
    }
    
    /**
      * 
      */
    private def stopManagers(): Unit = {
        // Order is important!
        NCProbeConnectionManager.stop()
        NCProbeNlpManager.stop()
        NCConversationManager.stop()
        NCCoordinatesEnricher.stop()
        NCDictionaryEnricher.stop()
        NCContextEnricher.stop()
        NCPostChecker.stop()
        NCPostEnricher.stop()
        NCPostEnrichCollapser.stop()
        NCNlpPreChecker.stop()
        NCSuspiciousNounsEnricher.stop()
        NCFunctionEnricher.stop()
        NCModelEnricher.stop()
        NCStopWordEnricher.stop()
        NCDictionaryManager.stop()
        NCCommandManager.stop()
        NCModelManager.stop()
        NCDeployManager.stop()
        NCExitManager.stop()
        NCNumericManager.stop()
        NCNlpManager.stop()
    }
}
