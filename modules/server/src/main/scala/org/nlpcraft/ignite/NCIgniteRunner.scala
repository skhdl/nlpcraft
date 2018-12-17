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

package org.nlpcraft.ignite

import com.typesafe.scalalogging.LazyLogging
import org.apache.ignite.{IgniteException, Ignition}
import org.nlpcraft._

import scala.sys.SystemProperties

/**
 * Provides Ignite runner.
 */
object NCIgniteRunner extends LazyLogging {
    /**
     * Starts Ignite node with given Ignite install directory and configuration path,
     * executes given function and stops the node.
     *
     * @param cfgRes XML resource for Ignite node.
     * @param body Function to execute on running Ignite node.
     */
    @throws[IgniteException]
    def runWith(cfgRes: String, body: ⇒ Unit) {
        val sysProps = new SystemProperties

        // Set up Ignite system properties.
        sysProps.put("IGNITE_PERFORMANCE_SUGGESTIONS_DISABLED", "true")
        sysProps.put("IGNITE_ANSI_OFF", "false")
        sysProps.put("IGNITE_QUIET", sysProps.get("IGNITE_QUIET").getOrElse(true).toString)
        sysProps.put("IGNITE_UPDATE_NOTIFIER", "false")
        sysProps.put("java.net.preferIPv4Stack", "true")

        // Start Ignite node.
        val ignite = Ignition.start(G.getStream(cfgRes))

        try {
            body
        }
        finally
            Ignition.stop(ignite.name(), true)
    }
}
