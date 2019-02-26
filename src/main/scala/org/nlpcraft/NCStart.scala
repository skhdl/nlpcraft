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
import org.nlpcraft.common.version.NCVersion
import org.nlpcraft.probe.NCProbe
import org.nlpcraft.server.NCServer

/**
  * Server or probe command line starter.
  */
object NCStart extends App with LazyLogging {
    /**
      *
      */
    private def execute(): Unit = {
        val seq = args.toSeq

        val idxSrv = seq.indexWhere(_ == "-server")
        val idxPrb = seq.indexWhere(_ == "-probe")

        /**
          *
          * @param idx
          * @return
          */
        def removeParam(idx: Int): Array[String] = seq.drop(1).toArray

        /**
          *
          * @param msgs
          */
        def error(msgs: String*): Unit = {
            val NL = System getProperty "line.separator"
            val ver = NCVersion.getCurrent
    
            val s = NL +
                raw"    _   ____      ______           ______   $NL" +
                raw"   / | / / /___  / ____/________ _/ __/ /_  $NL" +
                raw"  /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/  $NL" +
                raw" / /|  / / /_/ / /___/ /  / /_/ / __/ /_    $NL" +
                raw"/_/ |_/_/ .___/\____/_/   \__,_/_/  \__/    $NL" +
                raw"       /_/                                  $NL$NL" +
                s"Version: ${ver.version}$NL" +
                raw"${NCVersion.copyright}$NL"
    
            logger.info(s)
            
            for (msg ← msgs)
                logger.error(msg)
    
            logger.info("Usage:")
            logger.info("  Use '-server' argument to start server.")
            logger.info("  Use '-probe' argument to start probe.")

            System.exit(1)
        }

        if (idxSrv != 0 && idxPrb != 0)
            error("Either '-server' or '-probe' argument must be set.")
        else if (idxSrv == 0 && idxPrb == 0)
            error("Both '-server' and '-probe' arguments cannot be set.")
        else if (idxSrv != 0 && idxPrb == 0)
            NCProbe.main(removeParam(idxPrb))
        else if (idxSrv == 0 && idxPrb != 0)
            NCServer.main(removeParam(idxSrv))
    }

    execute()
}
