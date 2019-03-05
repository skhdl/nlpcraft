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

package org.nlpcraft.server.ignite

import com.typesafe.scalalogging.LazyLogging
import org.apache.ignite.{IgniteException, Ignition}
import org.nlpcraft.common._
import java.io.File

import scala.sys.SystemProperties

/**
 * Provides Ignite runner.
 */
object NCIgniteRunner extends LazyLogging {
    /**
      * Starts Ignite node.
      *
      * @param cfgPath Full path for configuration file or `null` for default on the
      *               class path `ignite.xml` file.
      * @param body Function to execute on running Ignite node.
      */
    @throws[NCE]
    def runWith(cfgPath: String, body: ⇒ Unit) {
        val sysProps = new SystemProperties

        // Set up Ignite system properties.
        sysProps.put("IGNITE_PERFORMANCE_SUGGESTIONS_DISABLED", "true")
        sysProps.put("IGNITE_ANSI_OFF", "false")
        sysProps.put("IGNITE_QUIET", sysProps.get("IGNITE_QUIET").getOrElse(true).toString)
        sysProps.put("IGNITE_UPDATE_NOTIFIER", "false")
        sysProps.put("java.net.preferIPv4Stack", "true")

        val ignite =
            try {
                // Start Ignite node.
                val ignite =
                    if (cfgPath != null)
                        // 1. Higher priority. It is defined.
                        Ignition.start(cfgPath)
                    else {
                        val cfgFile = "ignite.xml"

                        // 2. Tries to find config in the same folder with JAR.
                        val cfg = new File(cfgFile)

                        if (cfg.exists() && cfg.isFile)
                            Ignition.start(cfg.getAbsolutePath)
                        else {
                            // 3. Tries to start with config from JAR.
                            val stream = U.getStream(cfgFile)

                            if (stream == null)
                                throw new NCE(s"Resource not found: $cfgFile")

                            Ignition.start(stream)
                        }
                    }

                ignite.cluster().active(true)

                ignite
            }
            catch {
                case e: IgniteException ⇒ throw new NCE(s"Ignite error: ${e.getMessage}", e)
            }

            try
                body
            finally
                Ignition.stop(ignite.name(), true)
    }
}
