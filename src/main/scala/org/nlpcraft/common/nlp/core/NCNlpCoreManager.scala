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

import org.nlpcraft.common.{NCE, NCLifecycle}

import scala.language.{implicitConversions, postfixOps}
import scala.reflect.runtime.universe._

/**
 *  NLP core manager.
 */
object NCNlpCoreManager extends NCLifecycle(s"Core NLP manager") {
    @volatile private var engine: String = _
    @volatile private var tokenizer: NCNlpTokenizer = _

    /**
      *
      * @param engine
      */
    def setEngine(engine: String): Unit = this.engine = engine

    /**
      *
      * @return
      */
    def getEngine: String = engine

    override def start(): NCLifecycle = {
        require(engine != null)

        val mirror = runtimeMirror(getClass.getClassLoader)

        def mkInstance(name: String): NCNlpTokenizer =
            try
                mirror.reflectModule(mirror.staticModule(name)).instance.asInstanceOf[NCNlpTokenizer]
            catch {
                case e: Throwable ⇒ throw new NCE(s"Error initializing class: $name", e)
            }

        tokenizer =
            engine match {
                case "stanford" ⇒ mkInstance("org.nlpcraft.common.nlp.core.stanford.NCStanfordTokenizer")
                // NCOPenNlpTokenizer added via reflection just for symmetry.
                case "opennlp" ⇒ mkInstance("org.nlpcraft.common.nlp.core.opennlp.NCOPenNlpTokenizer")

                case _ ⇒ throw new AssertionError(s"Unexpected engine: $engine")
            }

        logger.info(s"NLP engined configured: $engine")

        tokenizer.start()

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

        val seq = tokenizer.tokenize(words).map(p ⇒ p → NCNlpPorterStemmer.stem(p.token))

        seq.zipWithIndex.map { case ((tok, stem), idx) ⇒
            idx match {
                case 0 ⇒ stem
                // Suppose there aren't multiple spaces.
                case _ ⇒ if (seq(idx - 1)._1.to + 1 < tok.from) s" $stem" else stem
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

        NCNlpPorterStemmer.stem(word)
    }

    /**
      * Tokenizes given sentence.
      *
      * @param sen Sentence text.
      * @return Tokens.
      */
    def tokenize(sen: String): Seq[NCNlpCoreToken] = {
        ensureStarted()

        tokenizer.tokenize(sen)
    }
}