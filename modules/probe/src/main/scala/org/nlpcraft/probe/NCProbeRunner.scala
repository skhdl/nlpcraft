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

import java.text.SimpleDateFormat
import java.time.Year
import java.util.Date

import org.nlpcraft._
import org.nlpcraft.ascii.NCAsciiTable
import org.nlpcraft.nlp.dict.NCDictionaryManager
import org.nlpcraft.probe.mgrs.cmd.NCCommandManager
import org.nlpcraft.probe.mgrs.conn.NCProbeConnectionManager
import org.nlpcraft.probe.mgrs.deploy.NCDeployManager
import org.nlpcraft.probe.mgrs.exit.NCExitManager
import org.nlpcraft.probe.mgrs.model.NCModelManager
import org.nlpcraft.probe.mgrs.nlp.conversation.NCConversationManager
import org.nlpcraft.probe.mgrs.nlp.enrichers.coordinates.NCCoordinatesEnricher
import org.nlpcraft.nlp.numeric.NCNumericsManager
import org.nlpcraft.probe.mgrs.nlp.enrichers.context.NCContextEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.dictionary.NCDictionaryEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.function.NCFunctionEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.model.NCModelEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.stopword.NCStopWordEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.suspicious.NCSuspiciousNounsEnricher
import org.nlpcraft.probe.mgrs.nlp.post.NCPostEnrichCollapser
import org.nlpcraft.probe.mgrs.nlp.post.NCPostEnricher
import org.nlpcraft.probe.mgrs.nlp.post.NCPostChecker
import org.nlpcraft.probe.mgrs.nlp.pre.NCNlpPreChecker
import org.nlpcraft.probe.mgrs.nlp.NCProbeNlpManager
import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.nlp.stem.NCStemmerManager
import org.nlpcraft.probe.dev.NCProbeConfig

import scala.compat.Platform._
import scala.util.control.Exception._

/**
  * Basic probe runner to be used by both command line and dev in-process apps.
  */
object NCProbeRunner extends LazyLogging with NCDebug {
    private final val VER = /*@version*/"x.x.x"
    private final val BUILD: String = /*@build*/new SimpleDateFormat("MMddyyyy").format(new Date())
    
    private val startMsec = currentTime
    
    /**
      * Prints ASCII-logo.
      */
    private def asciiLogo(cfg: NCProbeConfig) {
        val NL = System getProperty "line.separator"
        
        val copyright = s"Copyright (C) NlpCraft Project."
        
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
        tbl += ("Email", nvl(cfg.getEmail))
        tbl += ("In-Process Provider", if (cfg.getProvider == null) nvl(null) else cfg.getProvider.getClass)
        tbl += ("JARs Folder", nvl(cfg.getJarsFolder))
        
        tbl.info(logger, Some("Probe Configuration:"))
        
        logger.info("Set '-DNLPCRAFT_PROBE_SILENT=true' JVM system property to turn off verbose probe logging.")
    }
    
    /**
      *
      * @param cfg Probe configuration to start with.
      * @return
      */
    def startProbe(cfg: NCProbeConfig): Int = {
        asciiLogo(cfg)
        ackConfig(cfg)
        
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
        NCStemmerManager.start()
        NCNumericsManager.start()
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
        NCNumericsManager.stop()
        NCStemmerManager.stop()
    }
}
