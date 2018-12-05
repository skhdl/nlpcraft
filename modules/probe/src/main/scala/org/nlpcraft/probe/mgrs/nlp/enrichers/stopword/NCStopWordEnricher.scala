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

package org.nlpcraft.probe.mgrs.nlp.enrichers.stopword

import org.nlpcraft.nlp.NCNlpSentence
import org.nlpcraft.probe.NCModelDecorator
import org.nlpcraft.probe.mgrs.nlp.NCProbeEnricher

/**
  * Stop words enricher.
  */
object NCStopWordEnricher extends NCProbeEnricher("PROBE stopwords enricher") {
    /**
      * Processes this NLP sentence in an isolation.
      *
      * @param ns NLP sentence to enrich.
      */
    override def enrich(mdl: NCModelDecorator, ns: NCNlpSentence): Unit = {
        ensureStarted()
        
        def mark(stems: Set[String], f: Boolean): Unit =
            ns.filter(t ⇒ stems.contains(t.stem)).foreach(t ⇒ t.getNlpNote += "stopWord" → f)
    
        mark(mdl.excludedStopWordsStems, f = false)
        mark(mdl.additionalStopWordsStems, f = true)
    }
}
