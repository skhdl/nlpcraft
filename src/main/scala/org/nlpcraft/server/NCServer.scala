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

package org.nlpcraft.server

import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.common._
import org.nlpcraft.common.ascii.NCAsciiTable
import org.nlpcraft.common.nlp.dict.NCDictionaryManager
import org.nlpcraft.common.nlp.numeric.NCNumericManager
import org.nlpcraft.common.nlp.opennlp.NCNlpManager
import org.nlpcraft.common.version._
import org.nlpcraft.server.db.NCDbManager
import org.nlpcraft.server.ds.NCDsManager
import org.nlpcraft.server.endpoints.NCEndpointManager
import org.nlpcraft.server.geo.NCGeoManager
import org.nlpcraft.server.ignite.{NCIgniteInstance, NCIgniteRunner}
import org.nlpcraft.server.nlp.enrichers.NCNlpEnricherManager
import org.nlpcraft.server.nlp.preproc.NCPreProcessManager
import org.nlpcraft.server.nlp.spell.NCSpellCheckManager
import org.nlpcraft.server.nlp.synonym.NCSynonymManager
import org.nlpcraft.server.nlp.wordnet.NCWordNetManager
import org.nlpcraft.server.notification.NCNotificationManager
import org.nlpcraft.server.plugin.NCPluginManager
import org.nlpcraft.server.probe.NCProbeManager
import org.nlpcraft.server.proclog.NCProcessLogManager
import org.nlpcraft.server.query.NCQueryManager
import org.nlpcraft.server.rest.NCRestManager
import org.nlpcraft.server.tx.NCTxManager
import org.nlpcraft.server.user.NCUserManager

import scala.compat.Platform.currentTime
import scala.util.control.Exception.{catching, ignoring}

/**
  * NlpCraft server app.
  */
object NCServer extends App with NCIgniteInstance with LazyLogging {
    /**
      * Prints ASCII-logo.
      */
    private def asciiLogo() {
        val NL = System getProperty "line.separator"
        val ver = NCVersion.getCurrent
        
        val s = NL +
            raw"    _   ____      ______           ______   $NL" +
            raw"   / | / / /___  / ____/________ _/ __/ /_  $NL" +
            raw"  /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/  $NL" +
            raw" / /|  / / /_/ / /___/ /  / /_/ / __/ /_    $NL" +
            raw"/_/ |_/_/ .___/\____/_/   \__,_/_/  \__/    $NL" +
            raw"       /_/                                  $NL$NL" +
            s"Server$NL" +
            s"Version: ${ver.version}$NL" +
            raw"${NCVersion.copyright}$NL"
        
        logger.info(s)
    }
    
    /**
      * Starts all managers.
      */
    private def startManagers(): Unit = {
        NCVersionManager.start()
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
        NCEndpointManager.start()
        NCRestManager.start()
    }
    
    /**
      * Checks server version for update.
      */
    private def checkVersion(): Unit =
        NCVersionManager.checkForUpdates(
            "server",
            // Additional parameters.
            Map(
                "IGNITE_VERSION" → ignite.version().toString,
                "IGNITE_CLUSTER_SIZE" → ignite.cluster().nodes().size()
            )
        )
    
    /**
      * Stops all managers.
      */
    private def stopManagers(): Unit = {
        Seq(
            NCRestManager,
            NCEndpointManager,
            NCQueryManager,
            NCDsManager,
            NCUserManager,
            NCNotificationManager,
            NCNlpEnricherManager,
            NCNumericManager,
            NCNlpManager,
            NCGeoManager,
            NCPreProcessManager,
            NCSynonymManager,
            NCSpellCheckManager,
            NCDictionaryManager,
            NCWordNetManager,
            NCProcessLogManager,
            NCDbManager,
            NCTxManager,
            NCPluginManager,
            NCVersionManager
        ).foreach(p ⇒
            try
                p.stop()
            catch {
                case e: Exception ⇒ logger.warn(s"Error stopping manager.", e)
            }
        )
    }
    
    /**
      * Acks server start.
      */
    protected def ackStart() {
        val dur = s"[${U.format((currentTime - executionStart) / 1000.0, 2)}s]"
        
        val tbl = NCAsciiTable()
        
        tbl.margin(top = 1, bottom = 1)
        
        tbl += s"Server started $dur"
        
        tbl.info(logger)
    }
    
    /**
      *
      */
    private def start(): Unit = {
        // Fetch custom config file path, if any.
        args.find(_.startsWith("-config=")) match {
            case None ⇒ ()
            case Some(s) ⇒ System.setProperty("NLPCRAFT_CONFIG_FILE", s.substring("-config=".length))
        }
    
        asciiLogo()
        
        // Check upfront that configuration is provided.
        new NCConfigurable {}.check()

        val lifecycle = new CountDownLatch(1)
    
        catching(classOf[Throwable]) either startManagers() match {
            case Left(e) ⇒ // Exception.
                e match {
                    case x: NCException ⇒ logger.error(s"Failed to start server.", x)
                    case x: Throwable ⇒ logger.error("Failed to start server due to unexpected error.", x)
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

                        lifecycle.countDown()
                    }
                })

                U.ignoreInterrupt {
                    lifecycle.await()
                }
        }
    }

    NCIgniteRunner.runWith(
        args.find(_.startsWith("-igniteConfig=")) match {
            case None ⇒ null // Will use default on the classpath 'ignite.xml'.
            case Some(s) ⇒ s.substring("-config=".length)
        },
        start()
    )
}
