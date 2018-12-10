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

package org.nlpcraft2.nlp.enrichers

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.nlp.{NCNlpSentence, NCNlpSentenceToken}
import org.nlpcraft.{NCE, NCLifecycle}

import scala.collection._

/**
 * Base trait for all NLP enricher.
 */
abstract class NCNlpEnricher(name: String) extends NCLifecycle(name) with LazyLogging {
    /**
      * Processes given NLP sentence in an isolation.
      *
      * @param ns NLP sentence to enrich.
      */
    @throws[NCE]
    def enrich(ns: NCNlpSentence)
    
    // Utility functions.
    final protected def toStemKey(toks: Seq[NCNlpSentenceToken]): String = toks.map(_.stem).mkString(" ")
    final protected def toLemmaKey(toks: Seq[NCNlpSentenceToken]): String = toks.map(_.lemma).mkString(" ")
    final protected def toValueKey(toks: Seq[NCNlpSentenceToken]): String = toks.map(_.origText.toLowerCase).mkString(" ")
    final protected def toOriginalKey(toks: Seq[NCNlpSentenceToken]): String = toks.map(_.origText).mkString(" ")
}