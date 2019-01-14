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

package org.nlpcraft.probe.mgrs.nlp

import org.nlpcraft.nlp._
import org.nlpcraft.probe.NCModelDecorator
import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft._

import scala.collection._

/**
 * Base class for NLP enricher.
 */
abstract class NCProbeEnricher(name: String) extends NCLifecycle(name) with LazyLogging {
    /**
      * Processes this NLP sentence.
      *
      * @param mdl Model decorator.
      * @param ns NLP sentence to enrich.
      */
    @throws[NCE]
    def enrich(mdl: NCModelDecorator, ns: NCNlpSentence)
 
    // Utility functions.
    final protected def toStemKey(toks: Seq[NCNlpSentenceToken]): String = toks.map(_.stem).mkString(" ")
    final protected def toLemmaKey(toks: Seq[NCNlpSentenceToken]): String = toks.map(_.lemma).mkString(" ")
    final protected def toValueKey(toks: Seq[NCNlpSentenceToken]): String = toks.map(_.origText.toLowerCase).mkString(" ")
    final protected def toOriginalKey(toks: Seq[NCNlpSentenceToken]): String = toks.map(_.origText).mkString(" ")
}