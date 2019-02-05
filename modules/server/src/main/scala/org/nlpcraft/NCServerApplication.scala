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

package org.nlpcraft

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.ds.NCDsManager
import org.nlpcraft.geo.NCGeoManager
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
import org.nlpcraft.rest.NCRestManager
import org.nlpcraft.tx.NCTxManager
import org.nlpcraft.user.NCUserManager
import org.nlpcraft.util.NCGlobals
import org.nlpcraft.version.NCVersionManager

/**
  * Main server entry-point.
  */
object NCServerApplication extends NCIgniteServer("ignite.xml") with LazyLogging {
    override def name() = "NLPCraft Server"

    /**
      * Code to execute within Ignite node.
      */
    override def start() {
        super.start()

        initialize()

        try {
            NCGlobals.ignoreInterrupt {
                lifecycle.await()
            }
        }
        finally {
            stop()
        }
    }

    /**
      * Initializes server without blocking thread.
      */
    private[nlpcraft] def initialize() {
        startComponents()

        // Ack server start.
        ackStart()
        checkVersion()
    }

    // Starts all managers.
    private def startComponents(): Unit = {
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
      * Stops the server by counting down (i.e. releasing) the lifecycle latch.
      */
    override def stop(): Unit = {
        stopComponents()

        super.stop()
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
        NCVersionManager.stop()
    }
}
