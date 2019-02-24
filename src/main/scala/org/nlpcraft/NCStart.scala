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

import org.nlpcraft.probe.NCProbe
import org.nlpcraft.server.NCServer

/**
  * Server or probe command line starter.
  */
object NCStart extends App {
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
          * @param msg
          */
        def error(msg: String): Unit = {
            System.err.println(msg)

            System.exit(1)
        }

        if (idxSrv != 0 && idxPrb != 0)
            error("ERROR: either '-server' or '-probe' parameter must be set.")
        else if (idxSrv == 0 && idxPrb == 0)
            error("ERROR: both '-server' and '-probe' parameters cannot be set.")
        else if (idxSrv != 0 && idxPrb == 0)
            NCProbe.main(removeParam(idxPrb))
        else if (idxSrv == 0 && idxPrb != 0)
            NCServer.main(removeParam(idxSrv))
    }

    execute()
}
