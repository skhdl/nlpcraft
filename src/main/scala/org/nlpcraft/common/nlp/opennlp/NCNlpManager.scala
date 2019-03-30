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

package org.nlpcraft.common.nlp.opennlp

import java.io.BufferedInputStream

import opennlp.tools.lemmatizer.DictionaryLemmatizer
import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.postag.{POSModel, POSTagger, POSTaggerME}
import opennlp.tools.stemmer.{PorterStemmer, Stemmer}
import opennlp.tools.tokenize.{Tokenizer, TokenizerME, TokenizerModel}
import org.nlpcraft.common._
import org.nlpcraft.common.NCLifecycle
import resource.managed

import scala.collection.Seq

/**
  * OpenNLP manager.
  */
object NCNlpManager extends NCLifecycle("OpenNLP manager") {
    @volatile private var tokenizer: Tokenizer = _
    @volatile private var tagger: POSTagger = _
    @volatile private var lemmatizer: DictionaryLemmatizer = _
    @volatile private var stemmer: Stemmer = _
    @volatile private var nerFinders: Map[NameFinderME, String] = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        tokenizer =
            managed(new BufferedInputStream(U.getStream("opennlp/en-token.bin"))) acquireAndGet { in ⇒
                new TokenizerME(new TokenizerModel(in))
            }

        tagger =
            managed(new BufferedInputStream(U.getStream("opennlp/en-pos-maxent.bin"))) acquireAndGet { in ⇒
                new POSTaggerME(new POSModel(in))
            }

        def getNer(name: String): NameFinderME =
            managed(new BufferedInputStream(U.getStream(s"opennlp/$name"))) acquireAndGet { in ⇒
                new NameFinderME(new TokenNameFinderModel(in))
            }

        nerFinders = Map(
            getNer("en-ner-location.bin") → "LOCATION",
            getNer("en-ner-money.bin") → "MONEY",
            getNer("en-ner-person.bin") → "PERSON",
            getNer("en-ner-organization.bin") → "ORGANIZATION",
            getNer("en-ner-date.bin") → "DATE",
            getNer("en-ner-time.bin") → "TIME",
            getNer("en-ner-percentage.bin") → "PERCENTAGE"
        )

        lemmatizer =
            managed(new BufferedInputStream(U.getStream("opennlp/en-lemmatizer.dict"))) acquireAndGet { in ⇒
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

                var lemmas = lemmatizer.lemmatize(words, poses).toSeq

                require(spans.length == lemmas.length)

                // Hack.
                // For some reasons lemmatizer dictionary (en-lemmatizer.dict) marks some words with non-existent POS 'NNN'
                // Valid POS list: https://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html
                // Example of dictionary records:
                // ...
                // time	JJ	time
                // time	NNN	time
                // ...
                // time-ball NN	time-ball
                // ...
                val suspIdxs: Seq[Int] =
                    lemmas.
                        zip(poses).
                        zipWithIndex.flatMap {
                            // "0" is flag that lemma cannot be obtained for some reasons.
                            case ((lemma, pos), i) ⇒ if (lemma == "O" && pos == "NN") Some(i) else None
                        }

                if (suspIdxs.nonEmpty) {
                    val fixes: Map[Int, String] =
                        lemmatizer.
                            lemmatize(suspIdxs.map(i ⇒ words(i)).toArray, suspIdxs.map(_ ⇒ "NNN").toArray).
                            zipWithIndex.
                            flatMap { case (lemma, i) ⇒ if (lemma != "0") Some(suspIdxs(i) → lemma) else None }.toMap

                    lemmas = lemmas.zipWithIndex.map { case (lemma, idx) ⇒ fixes.getOrElse(idx, lemma) }
                }

                (spans, words, poses, lemmas)
            }

        val ners: Map[Array[Int], String] =
            this.
                synchronized {
                    val res = nerFinders.map {
                        case (finder, name) ⇒
                            finder.find(words).flatMap(p ⇒ Range.inclusive(p.getStart, p.getEnd - 1)) → name
                    }

                    nerFinders.keySet.foreach(_.clearAdaptiveData())

                    res
                }

        spans.zip(words).zip(poses).zip(lemmas).zipWithIndex.map { case ((((span, word), pos), lemma), idx) ⇒
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
                length = span.length,
                ners.flatMap { case (idxs, name) ⇒ if (idxs.contains(idx)) Some(name) else None }.toSet
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
      * Stemmatizes sequence of words.
      *
      * @param words Sequence of words to stemmatize.
      */
    def stemSeq(words: Iterable[String]): Seq[String] = {
        ensureStarted()

        words.map(stem).toSeq
    }
}