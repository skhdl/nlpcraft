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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import opennlp.tools.lemmatizer.DictionaryLemmatizer
import opennlp.tools.postag.{POSModel, POSTagger, POSTaggerME}
import opennlp.tools.tokenize.{Tokenizer, TokenizerME, TokenizerModel}
import org.nlpcraft.{NCLifecycle, _}
import resource.managed

import scala.collection._
import scala.language.implicitConversions

/**
  * Sentence word.
  *
  * @param word Original word.
  * @param lemma Lemma.
  * @param pos POS.
  * @param start From index.
  * @param end To index.
  */
case class NCCoreWord(word: String, lemma: Option[String], pos: String, start: Int, end: Int, length: Int)

/**
  * Nlp Manager.
  */
object NCCoreNlp extends NCLifecycle("Core nlp manager") {
    @volatile private var tokenizer: Tokenizer = _
    @volatile private var tagger: POSTagger = _
    @volatile private var lemmatizer: DictionaryLemmatizer = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        tokenizer =
            managed(G.getStream("opennlp/en-token.bin")) acquireAndGet { in ⇒
                new TokenizerME(new TokenizerModel(in))
            }

        tagger =
            managed(G.getStream("opennlp/en-pos-maxent.bin")) acquireAndGet { in ⇒
                new POSTaggerME(new POSModel(in))
            }

        val bytes = new ByteArrayOutputStream()
        val nl = "\n".getBytes("UTF-8")

        managed(G.getStream("opennlp/en-lemmatizer.dict.gz")) acquireAndGet { in ⇒
            G.readGzipResource(in, "UTF-8").foreach(p ⇒ {
                bytes.write(p.getBytes("UTF-8"))
                bytes.write(nl)
            })
        }

        lemmatizer = new DictionaryLemmatizer(new ByteArrayInputStream(bytes.toByteArray))

        super.start()
    }

    /**
      * Parses given sentence.
      *
      * @param sen Sentence text.
      * @return Parsed tokens.
      */
    def parse(sen: String): Seq[NCCoreWord] = {
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
            NCCoreWord(
                word = word,
                lemma = if (lemma == "O") None else Some(lemma),
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
    def tokenize(sen: String): Seq[String] = tokenizer.tokenize(sen)
}