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

package org.nlpcraft.probe

import java.util.regex.Pattern

import org.nlpcraft.mdllib._
import org.nlpcraft.nlp.NCNlpSentenceToken

import scala.collection.mutable.ArrayBuffer

/**
  * Synonym element type.
  */
object NCSynonymChunkKind extends Enumeration {
    type NCSynonymChunkKind = Value
    
    val TEXT: Value = Value // Simple word.
    val POS: Value = Value // PoS tag.
    val REGEX: Value = Value // Regular expression.
}

import org.nlpcraft.probe.NCSynonymChunkKind._

/**
  *
  * @param kind
  * @param origText
  * @param wordStem
  * @param posTag
  * @param regex
  */
case class NCSynonymChunk(
    kind: NCSynonymChunkKind,
    origText: String,
    wordStem: String = null,
    posTag: String = null,
    regex: Pattern = null
) {
    override def toString = s"($origText|$kind)"
}

/**
  *
  * @param isElementId Is this an implicit element ID synonym?
  *     In this case chunks contain the element ID.
  * @param isValueName Is this an implicit value name synonym?
  *     In this case chunks contain value name.
  * @param isDirect Direct or permutated synonym flag.
  * @param value Optional value name if this is a value synonym.
  */
class NCSynonym(
    val isElementId: Boolean,
    val isValueName: Boolean,
    val isDirect: Boolean,
    val value: String = null
) extends ArrayBuffer[NCSynonymChunk] with Ordered[NCSynonym] {
    require((isElementId && !isValueName && value == null) || !isElementId)
    require((isValueName && value != null) || !isValueName)
    
    /**
      *
      * @param toks
      * @return
      */
    @throws[ReflectiveOperationException]
    def isMatch(toks: Seq[NCNlpSentenceToken], toksStemsHash: Int, toksStems: String): Boolean = {
        require(toks != null)
        
        if (toks.isEmpty || size != toks.size || isTextOnly && toksStemsHash != stemsHash)
            false
        else if (isTextOnly)
            toksStemsHash == stemsHash && toksStems == stems
        else
            // Same length.
            toks.zip(this).forall {
                case (tok, chunk) ⇒
                    chunk.kind match {
                        case TEXT ⇒ chunk.wordStem == tok.stem
                        case POS ⇒ chunk.posTag == tok.pos
                        case REGEX ⇒
                            chunk.regex.matcher(tok.origText).matches() || chunk.regex.matcher(tok.normText).matches()
                        case _ ⇒ throw new AssertionError()
                    }
            }
    }
    
    lazy val isTextOnly: Boolean = forall(_.kind == TEXT)
    lazy val posChunks: Int = count(_.kind == POS)
    lazy val regexChunks: Int = count(_.kind == REGEX)
    lazy val isValueSynonym: Boolean = value != null
    lazy val stems: String = map(_.wordStem).mkString(" ")
    lazy val stemsHash: Int = stems.hashCode
    
    override def toString(): String = mkString(" ")

    // Orders synonyms from least to most significant.
    override def compare(that: NCSynonym): Int = {
        def compareIsValueSynonym(): Int =
            isValueSynonym match {
                case true if !that.isValueSynonym ⇒ 1
                case false if that.isValueSynonym ⇒ -1
                case _ ⇒ 0
            }
        
        if (that == null)
            1
        else
            isElementId match {
                case true if !that.isElementId ⇒ 1
                case false if that.isElementId ⇒ -1
                case true if that.isElementId ⇒ 0
                case _ ⇒ // None are element IDs.
                    if (length > that.length)
                        1
                    else if (length < that.length)
                        -1
                    else { // Equal length in chunks.
                        if (isDirect && !that.isDirect)
                            1
                        else if (!isDirect && that.isDirect)
                            -1
                        else // Both direct or indirect.
                            isTextOnly match {
                                case true if !that.isTextOnly ⇒ 1
                                case false if that.isTextOnly ⇒ -1
                                case true if that.isTextOnly ⇒ compareIsValueSynonym()
                                case _ ⇒
                                    val posRegexCnt = posChunks + regexChunks
                                    val thatPosRegexCnt = that.posChunks + that.regexChunks
            
                                    // Less PoS or regex chunks means less uncertainty, i.e. larger weight.
                                    if (posRegexCnt < thatPosRegexCnt)
                                        1
                                    else if (posRegexCnt > thatPosRegexCnt)
                                        -1
                                    else
                                        0
                            }
                    }
        }
    }

    override def canEqual(other: Any): Boolean = other.isInstanceOf[NCSynonym]

    override def equals(other: Any): Boolean = other match {
        case that: NCSynonym =>
            super.equals(that) &&
                (that canEqual this) &&
                isTextOnly == that.isTextOnly &&
                posChunks == that.posChunks &&
                regexChunks == that.regexChunks &&
                isValueSynonym == that.isValueSynonym &&
                isElementId == that.isElementId &&
                isValueName == that.isValueName &&
                value == that.value
        case _ => false
    }

    override def hashCode(): Int = {
        val state = Seq(super.hashCode(), isTextOnly, posChunks, regexChunks, isValueSynonym, isElementId, isValueName, value)
        state.map(p ⇒ if (p == null) 0 else p.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }
}

object NCSynonym {
    /**
      *
      * @param isElementId
      * @param isValueName
      * @param isDirect
      * @param value
      * @param chunks
      * @return
      */
    def apply(isElementId: Boolean, isValueName: Boolean, isDirect: Boolean, value: String, chunks: Seq[NCSynonymChunk]): NCSynonym = {
        var syn = new NCSynonym(isElementId, isValueName, isDirect, value)
        
        syn ++= chunks
        
        syn
    }
}

/**
  *
  * @param model Decorated model.
  * @param triviaStems Trivia stems.
  * @param synonyms Fast-access synonyms map (excluding dynamic ones).
  * @param excludedSynonyms Fast-access excluded synonyms map (excluding dynamic ones).
  * @param additionalStopWordsStems Stemmatized additional stopwords.
  * @param excludedStopWordsStems Stemmatized excluded stopwords.
  * @param suspiciousWordsStems Stemmatized suspicious stopwords.
  * @param elements Map of model elements.
  */
case class NCModelDecorator(
    model: NCModel,
    triviaStems: Map[String/*Trivia group*/, Set[String]],
    synonyms: Map[String/*Element ID*/, Map[Int/*Synonym length*/, Seq[NCSynonym]]], // Fast access map.
    excludedSynonyms: Map[String/*Element ID*/, Map[Int/*Synonym length*/, Seq[NCSynonym]]], // Fast access map.
    additionalStopWordsStems: Set[String],
    excludedStopWordsStems: Set[String],
    suspiciousWordsStems: Set[String],
    elements: Map[String/*Element ID*/, NCElement]
) extends java.io.Serializable {
    override def toString: String = {
        val ds = model.getDescriptor
        
        s"Probe model decorator [id=${ds.getId}, name=${ds.getName}, version=${ds.getVersion}]"
    }
}