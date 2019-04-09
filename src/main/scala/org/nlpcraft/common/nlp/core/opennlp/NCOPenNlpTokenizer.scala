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

package org.nlpcraft.common.nlp.core.opennlp

import java.io.BufferedInputStream

import opennlp.tools.tokenize.{Tokenizer, TokenizerME, TokenizerModel}
import org.nlpcraft.common.nlp.core.{NCNlpCoreToken, NCNlpTokenizer}
import org.nlpcraft.common.{NCLifecycle, _}
import resource.managed

import scala.language.{implicitConversions, postfixOps}

/**
  * OpenNLP tokenizer implementation.
  */
object NCOPenNlpTokenizer extends NCLifecycle("Open NLP tokenizer") with NCNlpTokenizer {
    @volatile private var tokenizer: Tokenizer = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        tokenizer =
            managed(new BufferedInputStream(U.getStream("opennlp/en-token.bin"))) acquireAndGet { in ⇒
                new TokenizerME(new TokenizerModel(in))
            }

        super.start()
    }

    override def tokenize(sen: String): Seq[NCNlpCoreToken] = {
        ensureStarted()

        this.synchronized { tokenizer.tokenizePos(sen) }.
            toSeq.map(s ⇒ NCNlpCoreToken(s.getCoveredText(sen).toString, s.getStart, s.getEnd, s.length()))
    }
}
