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

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.common.U
import org.nlpcraft.common.ascii.NCAsciiTable
import org.nlpcraft.common.version.NCVersion
import org.nlpcraft.server.ignite.NCIgniteNLPCraft

import scala.compat.Platform.currentTime

/**
  * NlpCraft server app.
  */
class NCServer2 extends App with NCIgniteNLPCraft with LazyLogging {
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
        
        
    }
    
    start()
}
