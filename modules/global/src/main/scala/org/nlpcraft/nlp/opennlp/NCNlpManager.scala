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

package org.nlpcraft.nlp.opennlp

import java.io.BufferedInputStream

import opennlp.tools.lemmatizer.DictionaryLemmatizer
import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.postag.{POSModel, POSTagger, POSTaggerME}
import opennlp.tools.stemmer.{PorterStemmer, Stemmer}
import opennlp.tools.tokenize.{Tokenizer, TokenizerME, TokenizerModel}
import org.nlpcraft.{G, NCLifecycle}
import resource.managed

import scala.collection.Seq

/**
  * OpenNLP manager.
  */
object NCNlpManager extends NCLifecycle("OpenNLP manager") {
    @volatile private var tokenizer: Tokenizer = _
    @volatile private var tagger: POSTagger = _
    @volatile private var lemmatizer: DictionaryLemmatizer = _
    @volatile private var nameFinder: NameFinderME = _
    @volatile private var stemmer: Stemmer = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        tokenizer =
            managed(new BufferedInputStream(G.getStream("opennlp/en-token.bin"))) acquireAndGet { in ⇒
                new TokenizerME(new TokenizerModel(in))
            }

        tagger =
            managed(new BufferedInputStream(G.getStream("opennlp/en-pos-maxent.bin"))) acquireAndGet { in ⇒
                new POSTaggerME(new POSModel(in))
            }

        nameFinder =
            managed(new BufferedInputStream(G.getStream("opennlp/en-ner-location.bin"))) acquireAndGet { in ⇒
                new NameFinderME(new TokenNameFinderModel(in))
            }

        lemmatizer =
            managed(new BufferedInputStream(G.getStream("opennlp/en-lemmatizer.dict"))) acquireAndGet { in ⇒
                new DictionaryLemmatizer(in)
            }

        stemmer = new PorterStemmer

        super.start()
    }

    /**
      * Parses given sentence.
      *
      * @param sen Sentence text.
      * @return Parsed tokens.
      */
    def parse(sen: String): Seq[NCNlpWord] = {
        ensureStarted()

        // Can be optimized.
        val (spans, words, poses, lemmas) =
            this.synchronized {
                val spans = tokenizer.tokenizePos(sen)
                val words = spans.map(_.getCoveredText(sen).toString)
                val poses = tagger.tag(words)

                require(spans.length == poses.length)

                val lemmas = lemmatizer.lemmatize(words, poses)

                require(spans.length == lemmas.length)

                (spans, words, poses, lemmas)
            }

        spans.zip(words).zip(poses).zip(lemmas).map { case (((span, word), pos), lemma) ⇒
            val normalWord = word.toLowerCase

            NCNlpWord(
                word = word,
                normalWord = normalWord,
                // "0" is flag that lemma cannot be obtained for some reasons.
                lemma = if (lemma == "O") None else Some(lemma),
                stem = stemmer.stem(normalWord).toString,
                pos = pos,
                start = span.getStart,
                end = span.getEnd,
                length = span.length
            )
        }
    }

    /**
      * Tokenizes given sentence.
      *
      * @param sen Sentence text.
      * @return Tokens.
      */
    def tokenize(sen: String): Seq[String] = {
        ensureStarted()

        this.synchronized { tokenizer.tokenize(sen) }
    }

    /**
      * Stems given word or a sequence of words which will be tokenized before.
      *
      * @param words One or more words to stemmatize.
      * @return Sentence with stemmed words.
      */
    def stem(words: String): String = {
        ensureStarted()

        val seq = this.synchronized {
            tokenizer.tokenizePos(words).map(span ⇒ (span, stemmer.stem(span.getCoveredText(words).toString)))
        }

        seq.zipWithIndex.map { case ((span, stem), idx) ⇒
            idx match {
                case 0 ⇒ stem
                // Suppose there aren't multiple spaces.
                case _ ⇒ if (seq(idx - 1)._1.getEnd <  span.getStart) s" $stem" else stem
            }
        }.mkString("")
    }

    /**
      * Gets indexes for words which detected as GEO locations.
      * Note that OpenNLP can only detect location in a specific case
      * (`Moscow` detected, `MOSCOW` or `moscow` is not detected).
      *
      * @param words Words
      * @return Indexes list.
      */
    def findLocations(words: Seq[String]): Seq[Int] = {
        ensureStarted()

        this.
            synchronized { nameFinder.find(words.toArray) }.
            flatMap(p ⇒ Range.inclusive(p.getStart, p.getEnd - 1))
    }
}
