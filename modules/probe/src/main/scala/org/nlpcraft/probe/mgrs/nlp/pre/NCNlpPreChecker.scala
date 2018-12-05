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
