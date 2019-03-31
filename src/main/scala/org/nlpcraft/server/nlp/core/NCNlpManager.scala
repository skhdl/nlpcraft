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

package org.nlpcraft.server.nlp.core

import org.nlpcraft.common.NCLifecycle
import org.nlpcraft.server.NCConfigurable
import org.nlpcraft.server.nlp.core.opennlp.NCOpenNlpParser
import org.nlpcraft.server.nlp.core.stanford.NCStanfordParser

/**
  * OpenNLP manager.
  */
object NCNlpManager extends NCLifecycle("Server NLP manager") with NCNlpParser {
    private object Config extends NCConfigurable {
        val engine: String = getString("server.nlp.engine")

        override def check(): Unit = require(engine == "stanford" || engine == "opennlp")
    }

    Config.check()

    @volatile var parser: NCNlpParser = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        parser =
            Config.engine match {
                case "stanford" ⇒ NCStanfordParser
                case "opennlp" ⇒  NCOpenNlpParser
                case _ ⇒ throw new AssertionError(s"Unexpected engine: ${Config.engine}")
            }

        logger.info(s"NLP engined configured: ${Config.engine}")

        parser.start()

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

        parser.parse(sen)
    }
}
