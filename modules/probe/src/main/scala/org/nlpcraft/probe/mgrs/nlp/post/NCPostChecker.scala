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

package org.nlpcraft.probe.mgrs.nlp.post

import org.nlpcraft.NCLifecycle
import org.nlpcraft.nlp.{NCNlpSentence ⇒ Sentence}
import org.nlpcraft.probe.NCModelDecorator
import com.typesafe.scalalogging.LazyLogging

/**
  * Post checker.
  */
object NCPostChecker extends NCLifecycle("Post-checker") with LazyLogging {
    @throws[NCPostException]
    def validate(mdl: NCModelDecorator, ns: Sentence) {
        val types = ns.flatten.filter(!_.isNlp).map(_.noteType).distinct
        val overlapNotes = ns.map(tkn ⇒ types.flatMap(tp ⇒ tkn.getNotes(tp))).filter(_.size > 1).flatten
    
        if (overlapNotes.nonEmpty)
            throw NCPostException("OVERLAP_NOTES")
    
        val model = mdl.model
    
        if (!model.isNoUserTokensAllowed && !ns.exists(_.exists(!_.noteType.startsWith("nlp:"))))
            throw NCPostException("ALLOW_NO_USER_TOKENS")
    
        if (!model.isSwearWordsAllowed && ns.exists(_.getNlpValueOpt[Boolean]("swear").getOrElse(false)))
            throw NCPostException("ALLOW_SWEAR_WORDS")
        
        if (model.getMinNonStopwords > ns.count(!_.isStopword))
            throw NCPostException("MIN_NON_STOPWORDS")
    
        if (model.getMinTokens > ns.size)
            throw NCPostException("MIN_TOKENS")
    
        if (model.getMaxUnknownWords < ns.count(t ⇒ t.isSimpleWord && !t.isSynthetic && !t.isKnownWord))
            throw NCPostException("MAX_UNKNOWN_WORDS")
    
        if (model.getMaxSuspiciousWords < ns.count(_.getNlpValueOpt[Boolean]("suspNoun").getOrElse(false)))
            throw NCPostException("MAX_SUSPICIOUS_WORDS")
    
        if (model.getMaxFreeWords < ns.count(_.isSimpleWord))
            throw NCPostException("MAX_FREE_WORDS")
    
        var n = ns.getNotes("nlp:date").size
    
        if (n > model.getMaxDateTokens)
            throw NCPostException("MAX_DATE_TOKENS")
        if (n < model.getMinDateTokens)
            throw NCPostException("MIN_DATE_TOKENS")
    
        n = ns.getNotes("nlp:num").size
    
        if (n > model.getMaxNumTokens)
            throw NCPostException("MAX_NUM_TOKENS")
        if (n < model.getMinNumTokens)
            throw NCPostException("MIN_NUM_TOKENS")
    
        n = ns.getNotes("nlp:geo").size
    
        if (n > model.getMaxGeoTokens)
            throw NCPostException("MAX_GEO_TOKENS")
        if (n < model.getMinGeoTokens)
            throw NCPostException("MIN_GEO_TOKENS")
    
        n = ns.getNotes("nlp:function").size
    
        if (n > model.getMaxFunctionTokens)
            throw NCPostException("MAX_FUNCTION_TOKENS")
        if (n < model.getMinFunctionTokens)
            throw NCPostException("MIN_FUNCTION_TOKENS")
    }
}
