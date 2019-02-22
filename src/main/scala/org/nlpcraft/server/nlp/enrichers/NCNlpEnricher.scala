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

package org.nlpcraft.server.nlp.enrichers

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.common._
import org.nlpcraft.common.NCLifecycle
import org.nlpcraft.common.nlp._

import scala.collection._

/**
  * Base trait for all NLP enricher.
  */
abstract class NCNlpEnricher(name: String) extends NCLifecycle(name) with LazyLogging {
    /**
      * Attempts to enrich given NLP sentence in an isolation.
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

object NCNlpEnricher {
    // Penn Treebank POS tags for opening & closing quotes.
    private val Q_POS = Set("``", "''")
    
    // Stanford POS for opening & closing brackets.
    // NOTE: it's different from standard Penn Treebank.
    private val LB_POS = "-LRB-"
    private val RB_POS = "-RRB-"
    
    def isQuote(t: NCNlpSentenceToken): Boolean = Q_POS.contains(t.pos)
    
    def isLBR(t: NCNlpSentenceToken): Boolean = t.pos == LB_POS
    def isRBR(t: NCNlpSentenceToken): Boolean = t.pos == RB_POS
    def isBR(t: NCNlpSentenceToken): Boolean = isLBR(t) || isRBR(t)
    
    /**
      * Skips spaces after left and before right brackets.
      *
      * @param toks
      * @param get
      */
    def mkSumString(toks: Seq[NCNlpSentenceToken], get: NCNlpSentenceToken ⇒ String): String = {
        val buf = mutable.Buffer.empty[String]
        
        val n = toks.size
        
        toks.zipWithIndex.foreach(p ⇒ {
            val t = p._1
            val idx = p._2
            
            buf += get(t)
            
            def isNextTokenRB: Boolean = idx <= n - 2 && isRBR(toks(idx + 1))
            def isLast: Boolean = idx == n - 1
            def isSolidWithNext: Boolean = idx <= n - 2 && t.endCharIndex == toks(idx + 1).startCharIndex
            
            if (
                !isLBR(t) &&
                    !isNextTokenRB &&
                    !isLast &&
                    !isSolidWithNext
            )
                buf += " "
        })
        
        buf.mkString
    }
}
