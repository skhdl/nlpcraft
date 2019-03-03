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

import scala.collection._
import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions

/**
  * Parsed NLP sentence is a collection of tokens. Each token is a collection of notes and
  * each note is a collection of KV pairs.
  */
class NCNlpSentence(
    val text: String,
    override val tokens: ArrayBuffer[NCNlpSentenceToken] = new ArrayBuffer[NCNlpSentenceToken](16)
) extends NCNlpSentenceTokenBuffer(tokens) with java.io.Serializable {
    // Deep copy.
    override def clone(): NCNlpSentence = new NCNlpSentence(text, tokens.map(_.clone()))

    /**
      * Utility method that gets set of notes for given note type collected from
      * tokens in this sentence. Notes are sorted in the same order they appear
      * in this sentence.
      *
      * @param noteType Note type.
      */
    def getNotes(noteType: String): IndexedSeq[NCNlpSentenceNote] = this.flatMap(_.getNotes(noteType)).distinct.toIndexedSeq
    
    /**
      * Utility method that removes note with given ID from all tokens in this sentence.
      * No-op if such note wasn't found.
      *
      * @param id Note ID.
      */
    def removeNote(id: String): Unit = this.foreach(_.remove(id))

    /**
      * Utility method that removes notes with given IDs from all tokens in this sentence.
      * No-op if such note wasn't found.
      *
      * @param id Note ID.
      */
    def removeNotes(ids: Iterable[String]): Unit = this.foreach(_.remove(ids))

}

object NCNlpSentence {
    implicit def toTokens(x: NCNlpSentence): ArrayBuffer[NCNlpSentenceToken] = x.tokens
}