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

package org.nlpcraft.common.nlp.core

import java.io.BufferedInputStream

import opennlp.tools.stemmer.{PorterStemmer, Stemmer}
import opennlp.tools.tokenize.{Tokenizer, TokenizerME, TokenizerModel}
import org.nlpcraft.common.{NCLifecycle, _}
import resource.managed

import scala.language.{implicitConversions, postfixOps}

/**
 *  NLP core manager.
 */
object NCNlpCoreManager extends NCLifecycle("Core NLP manager") {
    @volatile private var tokenizer: Tokenizer = _
    @volatile private var stemmer: Stemmer = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        tokenizer =
            managed(new BufferedInputStream(U.getStream("opennlp/en-token.bin"))) acquireAndGet { in ⇒
                new TokenizerME(new TokenizerModel(in))
            }

        stemmer = new PorterStemmer

        super.start()
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
      * Stems given word.
      *
      * @param word Word to stemmatize.
      * @return Stemmed word.
      */
    def stemWord(word: String): String = {
        ensureStarted()

        this.synchronized {
            stemmer.stem(word).toString
        }
    }

    /**
      * Tokenizes given sentence.
      *
      * @param sen Sentence text.
      * @return Tokens.
      */
    def tokenize(sen: String): Seq[NCNlpCoreToken] = {
        ensureStarted()

        this.synchronized { tokenizer.tokenizePos(sen) }.
            toSeq.map(s ⇒ NCNlpCoreToken(s.getCoveredText(sen).toString, s.getStart, s.getEnd, s.length()))
    }
}
