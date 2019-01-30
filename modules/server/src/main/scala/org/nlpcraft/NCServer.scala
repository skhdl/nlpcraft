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
    protected val COPYRIGHT = /*@copyright*/"Copyright (C) DataLingvo, Inc."

    // Version number. Can be changed at build time.
    protected val VER = /*@version*/"0.5.0"

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
        val dur = s"[${G.format((currentTime - startMsec) / 1000.0, 2)}s]"

        val tbl = NCAsciiTable()

        tbl.margin(top = 1, bottom = 1)

        tbl += s"${name()} started $dur"

        tbl.info(logger)
    }
}
