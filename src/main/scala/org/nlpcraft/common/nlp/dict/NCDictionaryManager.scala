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

package org.nlpcraft.common.nlp.dict

import org.nlpcraft.common._
import org.nlpcraft.common.NCLifecycle
import org.nlpcraft.common.nlp.dict.NCDictionaryType._

/**
 * English dictionary.
 */
object NCDictionaryManager extends NCLifecycle("Dictionary manager") {
    // Mapping between dictionary type and its configuration file name.
    private val dictFiles: Map[NCDictionaryType, String] =
        Map(
            WORD → "354984si.ngl",
            WORD_PROPER_NOUN → "21986na.mes",
            WORD_ACRONYM → "6213acro.nym",
            WORD_TOP_1000 → "10001fr.equ",
            WORD_COMMON → "74550com.mon"
        )

    // All dictionary types should be configured.
    require(NCDictionaryType.values.forall(dictFiles.contains))

    @throws[NCE]
    private val dicts: Map[NCDictionaryType, Set[String]] = dictFiles.map(p ⇒ {
        val wordType = p._1
        val path = p._2

        // Reads single words only.
        def read = U.readResource(s"moby/$path", "iso-8859-1", logger).
            filter(!_.contains(" ")).toSet

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
    def contains(dictType: NCDictionaryType, lemma: String): Boolean = {
        ensureStarted()

        dicts(dictType).contains(lemma)
    }

    /**
     * Gets all word of dictionary.
     *
     * @param dictType Dictionary type.
     */
    def get(dictType: NCDictionaryType): Set[String] = {
        ensureStarted()

        dicts(dictType)
    }
}