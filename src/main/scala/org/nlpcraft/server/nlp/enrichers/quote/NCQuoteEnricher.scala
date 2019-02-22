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

package org.nlpcraft.server.nlp.enrichers.quote

import org.nlpcraft._
import org.nlpcraft.nlp._
import org.nlpcraft.nlp.pos.NCPennTreebank
import org.nlpcraft.server.nlp.enrichers.NCNlpEnricher
import org.nlpcraft.server.nlp.enrichers.NCNlpEnricher._

import scala.collection._

/**
 * Quote enricher.
 */
object NCQuoteEnricher extends NCNlpEnricher("Quote enricher") {
    @throws[NCE]
    override def enrich(ns: NCNlpSentence) {
        // Clone input sentence.
        val copy = ns.clone()

        // Clear the tokens in the input sentence.
        ns.clear()

        var inQuotes = false
        var buf = mutable.IndexedSeq.empty[NCNlpSentenceToken]
        var tokIdx = 0

        var quotesToks = copy.filter(isQuote)

        val size = quotesToks.size

        // Skips last quote if there is odd quotes quantity.
        if (size % 2 != 0)
            quotesToks = quotesToks.take(size - 1)

        for (tok ← copy)
            if (quotesToks.contains(tok))
                // Assuming a closing quote.
                if (inQuotes) {
                    // Add closing quote to the buffer.
                    buf :+= tok

                    // Skip empty quoted content.
                    if (buf.size > 2) {
                        // Retain indexes in the original text based on
                        // opening and closing quotes.
                        val startIdx = buf.head.startCharIndex
                        val endIdx = buf.last.endCharIndex

                        // Trim the buffer from opening and closing quotes.
                        val trimBuf = buf.tail.dropRight(1)

                        // NOTE: we drop (ignore) 'ne' and 'nne' notes.
                        val nlpNote = NCNlpSentenceNote(
                            Seq(tokIdx),
                            "nlp:nlp",
                            "index" → tokIdx,
                            "pos" → NCPennTreebank.SYNTH_POS,
                            "posDesc" → NCPennTreebank.SYNTH_POS_DESC,
                            "lemma" → mkSumString(trimBuf, (t: NCNlpSentenceToken) ⇒ t.lemma),
                            "origText" → mkSumString(trimBuf, (t: NCNlpSentenceToken) ⇒ t.origText),
                            "normText" → mkSumString(trimBuf, (t: NCNlpSentenceToken) ⇒ t.normText),
                            "stem" → mkSumString(trimBuf, (t: NCNlpSentenceToken) ⇒ t.stem),
                            "start" → startIdx,
                            "end" → endIdx,
                            "length" → (trimBuf.map(_.wordLength).sum + trimBuf.length - 1),
                            "quoted" → true,
                            "stopWord" → false,
                            "bracketed" → false,
                            "direct" → trimBuf.forall(_.isDirect)
                        )

                        // New compound token (made out of buffer's content).
                        val newTok = NCNlpSentenceToken(tokIdx)

                        newTok.add(nlpNote)

                        // Add new compound token to the sentence.
                        ns += newTok

                        tokIdx += 1
                    }

                    // Mark as NOT in quotes.
                    inQuotes = false

                    // Clear the buffer.
                    buf = mutable.IndexedSeq.empty[NCNlpSentenceToken]
                }
                // Assume an opening quote.
                else {
                    assume(buf.isEmpty)

                    // Mark as in quotes.
                    inQuotes = true

                    // Add beginning quote to the new buffer.
                    buf :+= tok
                }
            else {
                if (inQuotes)
                    buf :+= tok
                else {
                    val newTok = tok.clone(tokIdx)

                    // Override indexes from CoreNLP enricher.
                    val nlpNote = newTok.getNlpNote

                    // NLP is single note.
                    newTok.remove(nlpNote.id)
                    newTok.add(nlpNote.clone(Seq(tokIdx), Seq(tokIdx), "index" → tokIdx, "quoted" → false))

                    // It shouldn't care about other kind of notes because
                    // CORE and QUOTE enrichers are first in processing.
                    ns += newTok

                    tokIdx += 1
                }
            }
    }
}
