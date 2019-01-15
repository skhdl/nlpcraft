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
 * Software:    NlpCraft
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

package org.nlpcraft.probe.mgrs.nlp.enrichers.suspicious

import org.nlpcraft.nlp._
import org.nlpcraft.probe.NCModelDecorator
import org.nlpcraft.probe.mgrs.nlp.NCProbeEnricher

/**
  * Suspicious words enricher.
  */
object NCSuspiciousNounsEnricher extends NCProbeEnricher("Suspicious nouns enricher") {
    /**
      * 
      * @param mdl Model decorator.
      * @param ns NLP sentence to enrich.
      */
    override def enrich(mdl: NCModelDecorator, ns: NCNlpSentence): Unit = {
        ensureStarted()
    
        ns.
            filter(t ⇒ mdl.suspiciousWordsStems.contains(t.stem)).
            foreach(_.getNlpNote += "suspNoun" → true)
    }
}
