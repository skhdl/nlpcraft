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

package org.nlpcraft.probe.mgrs.nlp.enrichers.function.mgrs

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.common._

/**
  * Order manager helper.
  */
object NCOrderManager extends LazyLogging {
    val sortByWords: Set[String] = readSort("sort_by_words.txt")
    val sortOrderWords: Map[String, Boolean] =
        readSort("sort_order_words.txt").map(p ⇒ {
            val seq = p.split(":").toSeq.map(_.trim)

            require(seq.lengthCompare(2) == 0)

            seq.head → (seq.last == "true")
        }).toMap

    /**
      * Reads configuration. NOTE: stemmatisation is done by auto-generator already.
      *
      * @param file File.
      */
    private def readSort(file: String): Set[String] =
        U.readTextResource(s"sort/$file", "UTF-8", logger).toSet
}