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
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.probe.mgrs.nlp.pre

import org.nlpcraft.NCLifecycle
import org.nlpcraft.nlp.NCNlpSentence
import org.nlpcraft.probe.NCModelDecorator
import com.typesafe.scalalogging.LazyLogging
import org.apache.tika.langdetect.OptimaizeLangDetector

/**
  * Pre-checker.
  */
object NCNlpPreChecker extends NCLifecycle("PROBE pre-checker") with LazyLogging {
    // Create new language finder singleton.
    private val langFinder = new OptimaizeLangDetector()
    
    // Initialize language finder.
    langFinder.loadModels()
    
    @throws[NCNlpPreException]
    def validate(mdl: NCModelDecorator, ns: NCNlpSentence) {
        val model = mdl.model
        
        if (!model.isNotLatinCharsetAllowed && !ns.text.matches("""[\s\w\p{Punct}]+"""))
            throw NCNlpPreException("ALLOW_NON_LATIN_CHARSET")
    
        if (!model.isNonEnglishAllowed && !langFinder.detect(ns.text).isLanguage("en"))
            throw NCNlpPreException("ALLOW_NON_ENGLISH")
    
        if (!model.isNoNounsAllowed && !ns.exists(_.pos.startsWith("n")))
            throw NCNlpPreException("ALLOW_NO_NOUNS")
    
        if (model.getMinWords > ns.map(_.wordLength).sum)
            throw NCNlpPreException("MIN_WORDS")
    
        if (ns.size > model.getMaxTokens)
            throw NCNlpPreException("MAX_TOKENS")
    }
}
