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

package org.nlpcraft.nlp.dict

import org.nlpcraft.nlp.dict.NCDictType._
import org.nlpcraft._
import org.nlpcraft.{NCE, NCLifecycle}

/**
 * English dictionary.
 */
object NCDictionary extends NCLifecycle("CORE NLP dictionary") {
    // Mapping between dictionary type and its configuration file name.
    private val dictFiles: Map[NCDictType, String] =
        Map(
            WORD → "354984si.ngl",
            WORD_PROPER_NOUN → "21986na.mes",
            WORD_ACRONYM → "6213acro.nym",
            WORD_TOP_1000 → "10001fr.equ",
            WORD_COMMON → "74550com.mon"
        )

    // All dictionary types should be configured.
    require(NCDictType.values.forall(dictFiles.contains))

    @throws[NCE]
    private val dicts: Map[NCDictType, Set[String]] = dictFiles.map(p ⇒ {
        val wordType = p._1
        val path = p._2

        // Reads single words only.
        def read = G.readTextResource(s"moby/$path", "iso-8859-1", logger).filter(!_.contains(" ")).toSet

        val words =
            wordType match {
                // Skips proper nouns for this dictionary type.
                case WORD_COMMON ⇒ read.filter(_.head.isLower)
                case _ ⇒ read
            }

        wordType → words.map(_.toLowerCase)
    })

    // Summary dictionary for all types.
    private val full: Set[String] = dicts.flatMap(_._2).toSet

    /**
     * Checks if given lemma found in any dictionary.
     *
     * @param lemma Lemma cto check for containment.
     */
    def contains(lemma: String): Boolean = {
        ensureStarted()

        full.contains(lemma)
    }

    /**
     * Checks if given lemma found in specified dictionary type.
     *
     * @param dictType Dictionary type.
     * @param lemma Lemma cto check for containment.
     */
    def contains(dictType: NCDictType, lemma: String): Boolean = {
        ensureStarted()

        dicts(dictType).contains(lemma)
    }

    /**
     * Gets all word of dictionary.
     *
     * @param dictType Dictionary type.
     */
    def get(dictType: NCDictType): Set[String] = {
        ensureStarted()

        dicts(dictType)
    }
}