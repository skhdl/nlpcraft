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

import org.nlpcraft.common.NCLifecycle
import org.nlpcraft.common.nlp.core.opennlp.NCOpenNlp
import org.nlpcraft.common.nlp.core.stanford.NCNlpStanford
import org.nlpcraft.server.NCConfigurable

/**
  * OpenNLP manager.
  */
object NCNlpManager extends NCLifecycle("Apache Open NLP manager") with NCNlpCore {
    private object Config extends NCConfigurable {
        val engine: String = getString("nlp.engine")

        override def check(): Unit = require(engine == "stanford" || engine == "opennlp")
    }

    Config.check()

    @volatile var nlp: NCNlpCore = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        nlp =
            Config.engine match {
                case "stanford" ⇒ NCNlpStanford
                case "opennlp" ⇒  NCOpenNlp
                case _ ⇒ throw new AssertionError(s"Unexpected engine: ${Config.engine}")
            }

        nlp.start()

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

        nlp.parse(sen)
    }

    /**
      * Tokenizes given sentence.
      *
      * @param sen Sentence text.
      * @return Tokens.
      */
    def tokenize(sen: String): Seq[String] = {
        ensureStarted()

        nlp.tokenize(sen)
    }

    /**
      * Stems given word or a sequence of words which will be tokenized before.
      *
      * @param words One or more words to stemmatize.
      * @return Sentence with stemmed words.
      */
    def stem(words: String): String = {
        ensureStarted()

        nlp.stem(words)
    }

    /**
      * Stemmatizes sequence of words.
      *
      * @param words Sequence of words to stemmatize.
      */
    def stemSeq(words: Iterable[String]): Seq[String] = {
        ensureStarted()

        nlp.stemSeq(words)
    }
}
