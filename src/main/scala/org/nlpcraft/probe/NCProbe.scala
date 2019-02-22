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

package org.nlpcraft.probe

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.ascii.NCAsciiTable
import org.nlpcraft.nlp.dict.NCDictionaryManager
import org.nlpcraft.nlp.numeric.NCNumericManager
import org.nlpcraft.nlp.opennlp.NCNlpManager
import org.nlpcraft.probe.mgrs.cmd.NCCommandManager
import org.nlpcraft.probe.mgrs.conn.NCProbeConnectionManager
import org.nlpcraft.probe.mgrs.deploy.NCDeployManager
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
import org.nlpcraft.version.{NCVersion, NCVersionManager}
import org.nlpcraft.{G, NCException}

import scala.collection.JavaConverters._
import scala.compat.Platform.currentTime
import scala.util.control.Exception._

/**
  * Data probe.
  */
object NCProbe extends App with LazyLogging {
    object Config {
        // If configuration file path is passed on - always use it.
        // Otherwise, check local and external on classpath 'probe.conf' files.
        private val hocon: Config = args.find(_.startsWith("-config=")) match {
            case None ⇒
                ConfigFactory.
                    parseFile(new java.io.File("probe.conf")).
                    withFallback(ConfigFactory.load("probe.conf"))
                
            case Some(s) ⇒
                ConfigFactory.
                    parseFile(new java.io.File(s.substring("-config=".length)))
        }
        
        if (!hocon.hasPath("probe"))
            throw new IllegalStateException(
                "No configuration found. " +
                "Place 'probe.conf' config file in the same folder or use '-config=path' to set alternative path to config file."
            )
    
        /**
          * Null or empty check.
          *
          * @param s String to check.
          */
        private def isEmpty(s: String) = s == null || s.isEmpty
    
        /**
          * Checks endpoint validity.
          *
          * @param name Name of the endpoint property.
          * @param ep endpoint to check.
          */
        @throws[IllegalArgumentException]
        private def checkEndpoint(name: String, ep: String): Unit = {
            val prop = s"Configuration property '$name'"

            if (isEmpty(ep))
                throw new IllegalArgumentException(s"$prop cannot be null or empty.")
            
            val idx = ep.indexOf(':')
            
            if (idx == -1)
                throw new IllegalArgumentException(s"$prop is invalid: $ep ('host:port' or 'ip-addr:port' format).")
            
            try {
                val port = ep.substring(idx + 1).toInt
            
                // 0 to 65536
                if (port < 0 || port > 65536)
                    throw new IllegalArgumentException(s"$prop port is out of [0, 65536) range in: $port.")
            }
            catch {
                case _: NumberFormatException ⇒
                    throw new IllegalArgumentException(s"$prop port is invalid in: $ep.")
            }
        }
    
        val id: String = hocon.getString("probe.id")
        val token: String = hocon.getString("probe.token")
        val upLink: String = hocon.getString("probe.upLink") // server-to-probe data pipe (uplink).
        val downLink: String = hocon.getString("probe.downLink") // probe-to-server data pipe (downlink).
        val jarsFolder: String = if (hocon.getIsNull("probe.jarsFolder")) null else hocon.getString("probe.jarsFolder")
        val modelProviders: List[String] = hocon.getStringList("probe.modelProviders").asScala.toList
    
        /**
          * 
          */
        def check(): Unit = {
            if (isEmpty(id))
                throw new IllegalArgumentException("Configuration property 'probe.id' cannot be empty.")
            if (isEmpty(token))
                throw new IllegalArgumentException("Configuration property 'probe.token' cannot be empty.")
            
            checkEndpoint("probe.upLink", upLink)
            checkEndpoint("probe.downLink", downLink)
            
            if (jarsFolder == null && modelProviders.isEmpty)
                throw new IllegalArgumentException("Either 'probe.jarsFolder' or 'probe.modelProviders' " +
                    "configuration property must be provided.")
        }
    }
    
    /**
      * Prints ASCII-logo.
      */
    private def asciiLogo() {
        val NL = System getProperty "line.separator"
        
        val copyright = s"Copyright (C) DataLingvo, Inc."
        val ver = NCVersion.getCurrent
        
        val s = NL +
            raw"    _   ____      ______           ______   $NL" +
            raw"   / | / / /___  / ____/________ _/ __/ /_  $NL" +
            raw"  /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/  $NL" +
            raw" / /|  / / /_/ / /___/ /  / /_/ / __/ /_    $NL" +
            raw"/_/ |_/_/ .___/\____/_/   \__,_/_/  \__/    $NL" +
            raw"       /_/                                  $NL$NL" +
            s"Data Probe$NL" +
            s"Version: ${ver.version}$NL" +
            raw"$copyright$NL"
        
        println(s)
    }
    
    /**
      *
      */
    private def ackConfig(): Unit = {
        val tbl = NCAsciiTable()
        
        val ver = NCVersion.getCurrent
        
        def nvl(obj: Any): String = if (obj == null) "" else obj.toString
        
        tbl += ("Probe ID", Config.id)
        tbl += ("Probe Token", Config.token)
        tbl += ("API Version", ver.version + ", " + ver.date.toString)
        tbl += ("Down-Link", Config.downLink)
        tbl += ("Up-Link", Config.upLink)
        tbl += ("Model providers", Config.modelProviders.size)
        tbl += ("JARs Folder", nvl(Config.jarsFolder))
        
        tbl.info(logger, Some("Probe Configuration:"))
        
        logger.info("Set '-DNLPCRAFT_PROBE_VERBOSE=true' JVM system property to turn on verbose probe logging.")
    }
    
    /**
      * Asks server start.
      */
    private def ackStart() {
        val dur = s"[${G.format((currentTime - executionStart) / 1000.0, 2)} sec]"
        
        val tbl = NCAsciiTable()
        
        tbl.margin(top = 1)
        
        tbl += s"Probe started $dur"
        
        tbl.info(logger)
    }
    
    /**
      *
      * @param cfg Configuration to start probe with.
      * @return
      */
    private def startManagers(cfg: Config.type): Unit = {
        // Order is important!
        NCVersionManager.start()
        NCNlpManager.start()
        NCNumericManager.start()
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
        NCNumericManager.stop()
        NCNlpManager.stop()
        NCVersionManager.stop()
    }
    
    /**
      *
      */
    private def checkVersion(): Unit =
        NCVersionManager.checkForUpdates(
            "probe",
            // Additional parameters.
            Map(
                "PROBE_ID" → Config.id,
                "PROBE_MODELS_NUM" → NCModelManager.getAllModels.size
            )
        )
    
    private def start(): Unit = {
        asciiLogo()
        ackConfig()
    
        catching(classOf[Throwable]) either startManagers(Config) match {
            case Left(e) ⇒ // Exception.
                e match {
                    case x: NCException ⇒ logger.error(s"Failed to start probe.", x)
                    case x: Throwable ⇒ logger.error("Failed to start probe due to unexpected error.", x)
                }
                
                System.exit(1)

            case _ ⇒ // Managers started OK.
                ackStart()
                checkVersion()
                
                Runtime.getRuntime.addShutdownHook(new Thread() {
                    override def run(): Unit = {
                        ignoring(classOf[Throwable]) {
                            stopManagers()
                        }
                    }
                })
    
                // Wait indefinitely.
                ignoring(classOf[InterruptedException]) {
                    Thread.currentThread().join()
                }
        }
    }
    
    start()
}
