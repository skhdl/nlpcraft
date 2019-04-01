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

package org.nlpcraft.server.nlp.enrichers.basenlp

import org.nlpcraft.common._
import org.nlpcraft.common.nlp.pos.NCPennTreebank
import org.nlpcraft.common.nlp.{NCNlpSentence, NCNlpSentenceNote, NCNlpSentenceToken}
import org.nlpcraft.server.nlp.core.NCNlpManager
import org.nlpcraft.server.nlp.enrichers.NCNlpEnricher

import scala.collection._

/**
  * Base NLP enricher.
  */
object NCBaseNlpEnricher extends NCNlpEnricher("NLP enricher") {
    // http://www.vidarholen.net/contents/interjections/
    private final val INTERJECTIONS =
        Set(
            "aah", "aaah", "aaaahh", "aha", "a-ha", "ahem",
            "ahh", "ahhh", "argh", "augh", "aww", "aw",
            "awww", "aww", "aw", "ohh", "ahh", "aw",
            "oh", "bah", "boo", "booh", "brr", "brrrr",
            "duh", "eek", "eeeek", "eep", "eh", "huh",
            "eh", "huh", "eyh", "eww", "ugh", "ewww",
            "gah", "gee", "grr", "grrrr", "hmm", "hm",
            "hmmmm", "humph", "harumph", "huh", "hurrah", "hooray",
            "huzzah", "ich", "yuck", "yak", "meh", "eh",
            "mhm", "mmhm", "uh-hu", "mm", "mmm", "mmh",
            "muahaha", "mwahaha", "bwahaha", "nah", "nuh-uh", "nuh-hu",
            "nuhuh", "oh", "ooh-la-la", "oh-lala", "ooh", "oooh",
            "oomph", "umph", "oops", "ow", "oww", "ouch",
            "oy", "oi", "oyh", "oy", "oyvay", "oy-vay",
            "pew", "pee-yew", "pff", "pffh", "pssh", "pfft",
            "phew", "psst", "sheesh", "jeez", "shh", "hush",
            "shush", "shoo", "tsk-tsk", "tut-tut", "uh-hu", "uhuh",
            "mhm", "uh-oh", "oh-oh", "uh-uh", "unh-unh", "uhh",
            "uhm", "err", "wee", "whee", "weee", "whoa",
            "wow", "yahoo", "yippie", "yay", "yeah", "yeeeeaah",
            "yee-haw", "yeehaw", "yoo-hoo", "yoohoo", "yuh-uh", "yuh-hu",
            "yuhuh", "yuck", "ich", "blech", "bleh", "zing",
            "ba-dum-tss", "badum-tish"
        ).map(_.toLowerCase)
    
    // The acronyms stand for (Left|Right) (Round|Square|Curly) Bracket.
    // http://www.cis.upenn.edu/~treebank/tokenization.html
    private final val BRACKETS = Map(
        "-LRB-" → "(",
        "-RRB-" → ")",
        "-LSB-" → "[",
        "-RSB-" → "]",
        "-LCB-" → "{",
        "-RCB-" → "}"
    )
    
    @throws[NCE]
    override def enrich(ns: NCNlpSentence) {
        // This must be 1st enricher in the pipeline.
        assume(ns.isEmpty)
        
        var idx = 0
        
        for (word ← NCNlpManager.parse(ns.text)) {
            val value = word.word.toLowerCase
            val origTxt = word.word
            
            val tok = NCNlpSentenceToken(idx)
            
            // Override interjection (UH) analysis.
            // (INTERJECTIONS and lemma are should be in lowercase.)
            val pos = if (INTERJECTIONS.contains(word.lemma)) "UH" else word.pos

            var seq = mutable.ArrayBuffer(
                "lemma" → processBracket(word.lemma),
                "index" → idx,
                "pos" → pos,
                "origText" → processBracket(origTxt),
                "normText" → processBracket(value),
                "charLength" → value.length,
                "stem" → word.stem,
                "posDesc" → NCPennTreebank.description(pos).getOrElse(pos),
                "start" → word.start,
                "end" → word.end,
                "quoted" → false,
                "stopWord" → false,
                "bracketed" → false,
                "direct" → true
            )

            if (word.ne.isDefined)
                seq += "ne" → word.ne.get

            if (word.nne.isDefined)
                seq += "nne" → word.nne.get

            tok.add(NCNlpSentenceNote(Seq(idx), "nlp:nlp", seq:_*))
            
            // Add new token to NLP sentence.
            ns += tok
            
            idx += 1
        }
    }
    
    private def processBracket(s: String): String = BRACKETS.getOrElse(s.toUpperCase, s)
}
