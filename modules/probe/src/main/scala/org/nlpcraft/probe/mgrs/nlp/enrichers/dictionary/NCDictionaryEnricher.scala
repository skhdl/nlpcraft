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

package org.nlpcraft.probe.mgrs.nlp.enrichers.dictionary

import org.nlpcraft._
import org.nlpcraft.nlp._
import org.nlpcraft.nlp.dict._
import org.nlpcraft.nlp.stem._
import org.nlpcraft.probe.NCModelDecorator
import org.nlpcraft.probe.mgrs.nlp.NCProbeEnricher

/**
  * Dictionary enricher.
  *
  * This enricher must be used after all enrichers which can manipulate 'quote' and 'stopword' notes of token.
  */
object NCDictionaryEnricher extends NCProbeEnricher("PROBE dictionary enricher") {
    @volatile private var swearWords: Set[String] = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        swearWords =
            G.readTextResource(s"badfilter/swear_words.txt", "UTF-8", logger).
                map(NCStemmerManager.stems).
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