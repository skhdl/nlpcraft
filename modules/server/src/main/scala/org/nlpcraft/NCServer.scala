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

package org.nlpcraft

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.ascii.NCAsciiTable
import scala.compat.Platform._

/**
 * Basic NlpCraft server component trait.
 */
trait NCServer extends LazyLogging {
    // Copyright blurb. Can be changed at build time.
    protected val COPYRIGHT = /*@copyright*/"Copyright (C) NlpCraft Project."

    // Version number. Can be changed at build time.
    protected val VER = /*@version*/"x.x.x"

    // Build number. Can be changed at build time.
    protected val BUILD: String = /*@build*/new SimpleDateFormat("MMddyyyy").format(new Date())

    private val startMsec = currentTime

    /**
     * Lifecycle latch for the server.
     */
    val lifecycle = new CountDownLatch(1)

    /**
     * Stops the server by counting down (i.e. releasing) the lifecycle latch.
     */
    def stop(): Unit = {
        lifecycle.countDown()

        logger.info(s"Server stopped: ${name()}")
    }

    /**
     * Starts the server.
     */
    def start(): Unit = {
        sys.props.put("NLPCRAFT_SERVER_NAME", name())

        asciiLogo()
    }

    /**
     * Gets name of the server for logging purposes.
     *
     * @return Descriptive name of the server.
     */
    def name(): String

    /**
     * Prints ASCII-logo.
     */
    private def asciiLogo() {
        val NL = System getProperty "line.separator"

        val s = NL +
            raw"    _   ____      ______           ______   $NL" +
            raw"   / | / / /___  / ____/________ _/ __/ /_  $NL" +
            raw"  /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/  $NL" +
            raw" / /|  / / /_/ / /___/ /  / /_/ / __/ /_    $NL" +
            raw"/_/ |_/_/ .___/\____/_/   \__,_/_/  \__/    $NL" +
            raw"       /_/                                  $NL$NL" +
            s"${name()}$NL" +
            s"Version: $VER - $BUILD$NL" +
            raw"$COPYRIGHT$NL"

        logger.info(s)
    }

    /**
     * Acks server start.
     */
    protected def ackStart() {
        val dur = s"[${G.format((currentTime - startMsec) / 1000.0, 2)} sec]"

        val tbl = NCAsciiTable()

        tbl.margin(top = 1, bottom = 1)

        tbl += s"${name()} started $dur"

        tbl.info(logger)
    }
}
