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

package org.nlpcraft.common.nlp

import scala.collection.mutable.ArrayBuffer
import scala.collection.{Seq, IndexedSeq ⇒ IdxSeq}
import scala.language.implicitConversions

/**
  *
  * @param tokens Initial buffer.
  */
class NCNlpSentenceTokenBuffer(val tokens: ArrayBuffer[NCNlpSentenceToken] = new ArrayBuffer[NCNlpSentenceToken](16)) extends java.io.Serializable {
    /** Stringified stems. */
    lazy val stems: String = tokens.map(_.stem).mkString(" ")

    /** Stem-based hashcode. */
    lazy val stemsHash: Int = stems.hashCode()

    type SSOT = IdxSeq[IdxSeq[Option[NCNlpSentenceToken]]]
    type SST = IdxSeq[IdxSeq[NCNlpSentenceToken]]

    /**
      * Gets all sequential permutations of tokens in this NLP sentence.
      *
      * For example, if NLP sentence contains "a, b, c, d" tokens, then
      * this function will return the sequence of following token sequences in this order:
      * "a b c d"
      * "a b c"
      * "b c d"
      * "a b"
      * "b c"
      * "c d"
      * "a"
      * "b"
      * "c"
      * "d"
      *
      * NOTE: this method will not return any permutations with a quoted token.
      *
      * @param stopWords Whether or not include tokens marked as stop words.
      * @param maxLen Maximum number of tokens in the sequence.
      * @param withQuoted Whether or not to include quoted tokens.
      */
    def tokenMix(
        stopWords: Boolean = false,
        maxLen: Int = Integer.MAX_VALUE,
        withQuoted: Boolean = false
    ): SST = {
        val toks = tokens.filter(t ⇒ stopWords || (!stopWords && !t.isStopword))

        val res = (for (n ← toks.length until 0 by -1 if n <= maxLen) yield toks.sliding(n)).flatten

        if (withQuoted) res else res.filter(!_.exists(_.isQuoted))
    }

    /**
      * Gets all sequential permutations of tokens in this NLP sentence.
      * This method is like a 'tokenMix', but with all combinations of stop-words (with and without)
      *
      * @param maxLen Maximum number of tokens in the sequence.
      * @param withQuoted Whether or not to include quoted tokens.
      */
    def tokenMixWithStopWords(maxLen: Int = Integer.MAX_VALUE, withQuoted: Boolean = false): SST = {
        /**
          * Gets all combinations for sequence of mandatory tokens with stop-words and without.
          *
          * Example:
          * 'A (stop), B, C(stop) → [A, B, C]; [A, B]; [B, C], [B]
          * 'A, B(stop), C(stop) → [A, B, C]; [A, B]; [A, C], [A].
          *
          * @param toks Tokens.
          */
        def permutations(toks: Seq[NCNlpSentenceToken]): SST = {
            def multiple(seq: SSOT, t: NCNlpSentenceToken): SSOT =
                if (seq.isEmpty)
                    if (t.isStopword) IdxSeq(IdxSeq(Some(t)), IdxSeq(None)) else IdxSeq(IdxSeq(Some(t)))
                else {
                    (for (subSeq ← seq) yield subSeq :+ Some(t)) ++
                        (if (t.isStopword) for (subSeq ← seq) yield subSeq :+ None else Seq.empty)
                }

            var res: SSOT = IdxSeq.empty

            for (t ← toks)
                res = multiple(res, t)

            res.map(_.flatten).filter(_.nonEmpty)
        }

        tokenMix(stopWords = true, maxLen, withQuoted).
            flatMap(permutations).
            filter(_.nonEmpty).
            distinct.
            sortBy(seq ⇒ (-seq.length, seq.head.index))
    }

    override def clone(): NCNlpSentenceTokenBuffer =
        new NCNlpSentenceTokenBuffer(new ArrayBuffer[NCNlpSentenceToken](tokens.size) ++ tokens.clone())
}

object NCNlpSentenceTokenBuffer {
    implicit def toTokens(x: NCNlpSentenceTokenBuffer): ArrayBuffer[NCNlpSentenceToken] = x.tokens

    def apply(toks: Iterable[NCNlpSentenceToken]): NCNlpSentenceTokenBuffer =
        new NCNlpSentenceTokenBuffer(new ArrayBuffer[NCNlpSentenceToken](toks.size) ++ toks)
}
