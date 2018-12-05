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

package org.nlpcraft.probe.mgrs.nlp.post

import org.nlpcraft.nlp.{NCNlpSentence ⇒ Sentence, NCNlpSentenceToken ⇒ Token}
import org.nlpcraft.probe.NCModelDecorator
import org.nlpcraft._
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec
import scala.collection.Seq

/**
  * Post-enricher.
  */
object NCPostEnricher extends NCLifecycle("PROBE post-enricher") with LazyLogging {
    // Prepositions should be marked as stop word in the of processing.
    // They can be used for numbers and dates periods calculations.
    private final val STOP_BEFORE_STOP_EXT: Seq[String] = Seq("DT", "PRP", "PRP$", "WDT", "WP", "WP$", "WRB", "TO", "IN")
    
    /**
      * Marks words before stop words.
      *
      * @param ns Sentence.
      * @param stopPoses Stop POSes.
      * @param lastIdx Last index.
      * @param isException Function which return `stop word exception` flag.
      */
    @tailrec
    private def markBefore(
        ns: Sentence, stopPoses: Seq[String], lastIdx: Int, isException : Seq[Token] ⇒ Boolean
    ): Boolean = {
        var stop = true
        
        for (
            (tok, idx) ← ns.zipWithIndex
            if idx != lastIdx &&
                !tok.isStopword &&
                !isException(Seq(tok)) &&
                stopPoses.contains(tok.pos) &&
                ns(idx + 1).isStopword) {
            tok.getNlpNote += "stopWord" → true

            stop = false
        }
        
        if (stop) true else markBefore(ns, stopPoses, lastIdx, isException)
    }
    
    /**
      * Process this NLP sentence (again) after all other enriches have run.
      *
      * @param mdl Model.
      * @param ns NLP sentence to enrich.
      */
    @throws[NCE]
    def postEnrich(mdl: NCModelDecorator, ns: Sentence): Unit =
        markBefore(
            ns,
            STOP_BEFORE_STOP_EXT,
            ns.size - 1,
            (toks: Seq[Token]) ⇒ mdl.excludedStopWordsStems.contains(toks.map(_.stem).mkString(" "))
        )
}
