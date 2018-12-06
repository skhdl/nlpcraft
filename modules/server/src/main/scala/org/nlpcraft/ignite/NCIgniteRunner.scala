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

import java.util.Objects

import com.typesafe.scalalogging.LazyLogging
import org.apache.ignite.{IgniteException, Ignition}
import org.nlpcraft._

import scala.collection.JavaConverters._
import scala.sys.SystemProperties

/**
 * Provides Ignite runner.
 */
object NCIgniteRunner extends LazyLogging {
    // Segment attribute name.
    private final val SGM_ATTR_NAME = "geos.segment"

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

        // Start Ignite node.
        val ignite = Ignition.start(G.getStream(cfgRes))

        try {
            val sgm = ignite.cluster.localNode.attribute[String](SGM_ATTR_NAME)

            assume(sgm != null, s"Node attribute '$SGM_ATTR_NAME' is not set.")

            def checkStarted(sgm: String): Unit =
                if (!ignite.cluster().nodes().asScala.exists(x ⇒ Objects.equals(x.attributes.get("geos.segment"), sgm)))
                    throw new IgniteException(s"Server is not started: $sgm")

            // `adm` and `share` servers should always be started first in the cluster.
            checkStarted("adm")

            if (sgm != "adm")
                checkStarted("share")

            // Ack segment.
            logger.info(s"GEOS segment: $sgm")

            body
        }
        finally
            Ignition.stop(ignite.name(), true)
    }
}
