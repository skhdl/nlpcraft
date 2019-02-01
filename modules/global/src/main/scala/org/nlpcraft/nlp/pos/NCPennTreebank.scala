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

package org.nlpcraft.nlp.pos

import scala.collection.immutable.HashMap

/**
 * Penn Treebank POS helper.
 */
object NCPennTreebank {
    // http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html
    private final val PENN_TREEBANK = HashMap[String, String] (
        "CC" → "Coordinating conjunction",
        "CD" → "Cardinal number",
        "DT" → "Determiner",
        "EX" → "Existential there",
        "FW" → "Foreign word",
        "IN" → "Preposition or sub. conjunction",
        "JJ" → "Adjective",
        "JJR" → "Adjective, comparative",
        "JJS" → "Adjective, superlative",
        "LS" → "List item marker",
        "MD" → "Modal",
        "NN" → "Noun, singular or mass",
        "NNS" → "Noun, plural",
        "NNP" → "Proper noun, singular",
        "NNPS" → "Proper noun, plural",
        "PDT" → "Predeterminer",
        "POS" → "Possessive ending",
        "PRP" → "Personal pronoun",
        "PRP$" → "Possessive pronoun",
        "RB" → "Adverb",
        "RBR" → "Adverb, comparative",
        "RBS" → "Adverb, superlative",
        "RP" → "Particle",
        "SYM" → "Symbol",
        "TO" → "To",
        "UH" → "Interjection",
        "VB" → "Verb, base form",
        "VBD" → "Verb, past tense",
        "VBG" → "Verb, gerund or present part",
        "VBN" → "Verb, past participle",
        "VBP" → "Verb, non-3rd person sing. present",
        "VBZ" → "Verb, 3rd person sing. present",
        "WDT" → "Wh-determiner",
        "WP" → "Wh-pronoun",
        "WP$" → "Possessive wh-pronoun",
        "WRB" → "Wh-adverb"
    )

    // Synthetic token.
    final val SYNTH_POS = "---"
    final val SYNTH_POS_DESC = "Synthetic tag"

    // Useful POS tags sets.
    final val NOUNS_POS = Seq("NN", "NNS", "NNP", "NNPS")
    final val VERBS_POS = Seq("VB", "VBD", "VBG", "VBN", "VBP", "VBZ")
    final val WHS_POS = Seq("WDT", "WP", "WP$", "WRB")
    final val JJS_POS = Seq("JJ", "JJR", "JJS")

    // Accessors.
    def description(tag: String): Option[String] = if (isSynthetic(tag)) Some(SYNTH_POS_DESC) else PENN_TREEBANK.get(tag)
    def contains(tag: String): Boolean = PENN_TREEBANK.contains(tag)
    def isSynthetic(tag: String): Boolean =  tag == SYNTH_POS
}
