package org.nlpcraft2

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.G
import org.nlpcraft.ascii.NCAsciiTable

import scala.compat.Platform.currentTime

/**
 * Basic NlpCraft server component trait.
 */
trait NCServer extends LazyLogging {
    // Copyright blurb.
    protected val COPYRIGHT = /*@copyright*/"Copyright (C) NlpCraft Project."

    // Version number.
    protected val VER = /*@version*/"x.x.x"

    // Build number.
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
            s"Data Probe$NL" +
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
