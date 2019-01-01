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

package org.nlpcraft.nlp.enrichers

import org.nlpcraft.NCLifecycle
import org.nlpcraft.nlp.enrichers.basenlp.NCBaseNlpEnricher
import org.nlpcraft.nlp.enrichers.date.NCDateEnricher
import org.nlpcraft.nlp.enrichers.numeric.NCNumericEnricher
import org.nlpcraft.nlp.enrichers.quote.NCQuoteEnricher
import org.nlpcraft.nlp.enrichers.stopword.NCStopWordEnricher

/**
  * Enrichment pipeline manager.
  */
object NCNlpEnricherManager extends NCLifecycle("Enrichment manager") {
    /**
      * Starts this manager.
      */
    override def start(): NCLifecycle = {
        NCBaseNlpEnricher.start()
        NCDateEnricher.start()
        NCStopWordEnricher.start()
        NCQuoteEnricher.start()
        NCNumericEnricher.start()
        
        super.start()
    }
    
    /**
      * Stops this manager.
      */
    override def stop(): Unit = {
        NCNumericEnricher.stop()
        NCQuoteEnricher.stop()
        NCStopWordEnricher.stop()
        NCDateEnricher.stop()
        NCBaseNlpEnricher.stop()
        
        super.stop()
    }
}
