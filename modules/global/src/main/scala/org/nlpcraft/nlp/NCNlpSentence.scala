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

package org.nlpcraft.nlp

import org.nlpcraft._
import scala.collection._

/**
  * Parsed NLP sentence is a collection of tokens. Each token is a collection of notes and
  * each note is a collection of KV pairs.
  */
class NCNlpSentence(val text: String) extends mutable.ArrayBuffer[NCNlpSentenceToken] with Serializable {
    override def clone(): NCNlpSentence = {
        val t = new NCNlpSentence(text)
        
        t ++= this.map(t ⇒ t.clone(t.index))
        
        t
    }
    
    /**
      * Utility method that gets set of notes for given note type collected from
      * tokens in this sentence. Notes are sorted in the same order they appear
      * in this sentence.
      *
      * @param noteType Note type.
      */
    def getNotes(noteType: String): IndexedSeq[NCNlpSentenceNote] = flatMap(_.getNotes(noteType)).distinct.toIndexedSeq
    
    /**
      * Utility method that removes note with given ID from all tokens in this sentence.
      * No-op if such note wasn't found.
      *
      * @param id Note ID.
      */
    def removeNote(id: String): Unit = foreach(_.remove(id))
    
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
    ): IndexedSeq[IndexedSeq[NCNlpSentenceToken]] = {
        val toks = filter(t ⇒ stopWords || (!stopWords && !t.isStopword))
        
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
    def tokenMixWithStopWords(maxLen: Int = Integer.MAX_VALUE, withQuoted: Boolean = false):
        IndexedSeq[IndexedSeq[NCNlpSentenceToken]] = {
        /**
          * Gets all combinations for sequence of mandatory tokens with stop-words and without.
          *
          * Example:
          * 'A (stop), B, C(stop) → [A, B, C]; [A, B]; [B, C], [B]
          * 'A, B(stop), C(stop) → [A, B, C]; [A, B]; [A, C], [A].
          *
          * @param toks Tokens.
          */
        def permutations(toks: Seq[NCNlpSentenceToken]): IndexedSeq[IndexedSeq[NCNlpSentenceToken]] = {
            type IIOT = IndexedSeq[IndexedSeq[Option[NCNlpSentenceToken]]]
            
            def multiple(seq: IIOT, t: NCNlpSentenceToken): IIOT =
                if (seq.isEmpty)
                    if (t.isStopword) IndexedSeq(IndexedSeq(Some(t)), IndexedSeq(None)) else IndexedSeq(IndexedSeq(Some(t)))
                else {
                    (for (subSeq ← seq) yield subSeq :+ Some(t)) ++
                        (if (t.isStopword) for (subSeq ← seq) yield subSeq :+ None else Seq.empty)
                }
            
            var res: IIOT = IndexedSeq.empty
            
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
    
    /**
      * Gets tokens filtered for given POS tags.
      *
      * @param pos List of POS tags to filter on.
      */
    @throws[NCE]
    def byPos(pos: String*): IndexedSeq[NCNlpSentenceToken] =
        // Every token must have POS tag note.
        filter(t ⇒ pos.contains(t.pos))
}