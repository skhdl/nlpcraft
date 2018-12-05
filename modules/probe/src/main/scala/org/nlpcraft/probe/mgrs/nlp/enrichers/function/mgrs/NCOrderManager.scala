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

package org.nlpcraft.probe.mgrs.nlp.enrichers.function.mgrs

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft._

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
        G.readTextResource(s"sort/$file", "UTF-8", logger).toSet
}