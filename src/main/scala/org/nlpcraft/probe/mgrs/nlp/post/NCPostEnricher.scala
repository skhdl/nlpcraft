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

package org.nlpcraft.probe.mgrs.nlp.post

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft._
import org.nlpcraft.nlp.{NCNlpSentence => Sentence, NCNlpSentenceToken => Token}
import org.nlpcraft.probe.mgrs.NCModelDecorator

import scala.annotation.tailrec
import scala.collection.Seq

/**
  * Post-enricher.
  */
object NCPostEnricher extends NCLifecycle("Post-enricher") with LazyLogging {
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
