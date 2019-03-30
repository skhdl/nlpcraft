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

package org.nlpcraft.probe.mgrs.nlp.enrichers.dictionary

import org.nlpcraft.common._
import org.nlpcraft.common.NCLifecycle
import org.nlpcraft.common.nlp._
import org.nlpcraft.common.nlp.core.NCNlpManager
import org.nlpcraft.common.nlp.dict._
import org.nlpcraft.probe.mgrs.NCModelDecorator
import org.nlpcraft.probe.mgrs.nlp.NCProbeEnricher

/**
  * Dictionary enricher.
  *
  * This enricher must be used after all enrichers which can manipulate 'quote' and 'stopword' notes of token.
  */
object NCDictionaryEnricher extends NCProbeEnricher("Dictionary enricher") {
    @volatile private var swearWords: Set[String] = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        swearWords =
            U.readTextResource(s"badfilter/swear_words.txt", "UTF-8", logger).
                map(NCNlpManager.stem).
                toSet

        super.start()
    }

    @throws[NCE]
    override def enrich(mdl: NCModelDecorator, ns: NCNlpSentence): Unit = {
        ns.foreach(t ⇒ {
            // Dictionary.
            val nlpNote = t.getNlpNote

            // Single letters seems suspiciously.
            nlpNote += "dict" → (NCDictionaryManager.contains(t.lemma) && t.lemma.length > 1)

            // English.
            nlpNote += "english" → t.origText.matches("""[\s\w\p{Punct}]+""")

            // Swearwords.
            nlpNote += "swear" → swearWords.contains(t.stem)
        })
    }
}