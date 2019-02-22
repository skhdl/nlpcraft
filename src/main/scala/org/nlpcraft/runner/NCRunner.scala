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

package org.nlpcraft.runner

import org.nlpcraft.probe.NCProbe
import org.nlpcraft.server.NCServerApplication

/**
  * Components runner. TODO:
  */
object NCRunner extends App {
    val seq = args.toSeq
    val minSeq = seq.map(_.toLowerCase)

    def getPosition(words: String*): Option[Int] = {
        val lcWords = words.map(_.toLowerCase).toSet

        val idxs = minSeq.zipWithIndex.flatMap { case (lcWord, idx) ⇒
            if (lcWords.contains(lcWord)) Some(idx) else None
        }

        // TODO: errors text.
        idxs.size match {
            case 0 ⇒ None
            case 1 ⇒ Some(idxs.head)
            case _ ⇒ throw new IllegalArgumentException(s"Unexpected arguments: ${seq.mkString(", ")}")
        }
    }

    val idxSrv = getPosition("-s", "-server", "s", "server")
    val idxProve = getPosition("-p", "-probe", "p", "probe")

    def skip(idx: Int): Array[String] = (seq.slice(0, idx) ++ seq.slice(idx + 1, seq.length)).toArray

    // TODO: errors text.
    if (idxSrv.isEmpty && idxProve.isEmpty)
        throw new IllegalArgumentException("Server or probe parameter must be set")
    else if (idxSrv.nonEmpty && idxProve.nonEmpty)
        throw new IllegalArgumentException("Server or probe parameter must be set")
    else if (idxSrv.isEmpty && idxProve.nonEmpty)
        NCProbe.main(skip(idxProve.get))
    else if (idxSrv.nonEmpty && idxProve.isEmpty)
        NCServerApplication.main(skip(idxSrv.get))
}
