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

package org.nlpcraft.mdllib.intent.impl

import java.util.{ArrayList ⇒ JArrayList, List ⇒ JList, Set ⇒ JSet}

import org.nlpcraft.ascii.NCAsciiTable
import org.nlpcraft.mdllib.intent.NCIntentSolver._
import org.nlpcraft.mdllib.utils.NCTokenUtils._
import org.nlpcraft.mdllib._
import org.nlpcraft._
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.lang3.tuple.Pair
import org.nlpcraft.mdllib.{NCSentence, NCToken, NCVariant}

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  * TODO: add description.
  */
object NCIntentSolverEngine extends NCDebug with LazyLogging {
    /**
      * Immutable compound 4-part weight.
      *
      * @param weights
      */
    private class Weight(private val weights: Array[Int]) extends Ordered[Weight] {
        require(weights.length == 4)
    
        private lazy val hash = weights(3) * 17 + (weights(2) * 31 + (weights(1) * 47 + weights(0)))
    
        /**
          *
          * @param w0 1st (most) important weight.
          * @param w1 2nd important weight.
          * @param w2 3rd important weight.
          * @param w3 4th (least) important weight.
          */
        def this(w0: Int, w1: Int, w2: Int, w3: Int) {
            this(Array(w0, w1, w2, w3))
        }
    
        /**
          * 
          */
        def this() {
            this(Array(0, 0, 0, 0))
        }
    
        /**
          * 
          * @param idx
          * @return
          */
        def apply(idx: Int): Int = weights(idx)
    
        /**
          * 
          * @param that
          * @return
          */
        def +(that: Weight): Weight = new Weight(
            weights(0) + that(0),
            weights(1) + that(1),
            weights(2) + that(2),
            weights(3) + that(3)
        )

        override def compare(that: Weight): Int =
            if (weights(0) < that(0))
                -1
            else if (weights(0) > that(0))
                1
            else {
                if (weights(1) < that(1))
                    -1
                else if (weights(1) > that(1))
                    1
                else {
                    if (weights(2) < that(2))
                        -1
                    else if (weights(2) > that(2))
                        1
                    else {
                        if (weights(3) < that(3))
                            -1
                        else if (weights(3) > that(3))
                            1
                        else
                            0
                    }
                }
            }
    
        override def hashCode(): Int = hash
    
        override def equals(obj: Any): Boolean =
            obj match {
                case that: Weight ⇒
                    that(0) == weights(0) &&
                    that(1) == weights(1) &&
                    that(2) == weights(2) &&
                    that(3) == weights(3)
                case _ ⇒ false
            }
    
        override def toString: String = s"[${weights(0)},${weights(1)},${weights(2)},${weights(3)}]"
    }
    
    /**
      * 
      * @param used
      * @param tok
      */
    private case class UseToken(
        var used: Boolean,
        var conv: Boolean,
        tok: NCToken
    )
    
    /**
      *
      * @param toks
      * @param weight
      */
    private case class TermMatch(
        toks: List[UseToken],
        weight: Weight
    ) {
        lazy val minIndex: Int = getTokenIndex(toks.map(_.tok).minBy(getTokenIndex))
        lazy val maxIndex: Int = getTokenIndex(toks.map(_.tok).maxBy(getTokenIndex))
    }

    /**
      *
      * @param tokGrps
      * @param weight
      * @param intent
      * @param termNouns
      * @param exactMatch
      */
    private case class IntentMatch(
        tokGrps: List[List[UseToken]],
        weight: Weight,
        intent: INTENT,
        termNouns: List[String],
        exactMatch: Boolean
    )
    
    /**
      * Main entry point for intent engine.
      * 
      * @param sen Sentence to solve against.
      * @param conv Conversation STM.
      * @param intents Set of intents to match for.
      * @return
      */
    @throws[NCE]
    def solve(
        sen: NCSentence,
        conv: JSet[NCToken],
        intents: JList[Pair[INTENT, IntentCallback]]): JList[NCIntentSolverResult] = {
        case class MatchHolder(
            intentMatch: IntentMatch, // Match.
            callback: IntentCallback, // Callback function.
            variant: NCVariant, // Variant used for the match.
            variantIdx: Int // Variant index.
        )
        
        val matches = mutable.ArrayBuffer.empty[MatchHolder]

        // Find all matches across all intents and sentence variants.
        for ((vrn, vrnIdx) ← sen.variants().zipWithIndex) {
            val availToks = vrn.getTokens.filter(!isStopWord(_))
            
            matches.appendAll(
                intents.flatMap(pair ⇒ {
                    val intent = pair.getLeft
                    val callback = pair.getRight
                    
                    // Isolated sentence tokens.
                    val senToks = Seq.empty[UseToken] ++ availToks.map(UseToken(false, false, _))
                    // Isolated conversation tokens.
                    val convToks =
                        if (intent.isIncludeConversation)
                            Set.empty[UseToken] ++ conv.map(UseToken(false, true, _))
                        else
                            Set.empty[UseToken]
    
                    // Solve intent in isolation.
                    solveIntent(intent, senToks, convToks, vrnIdx) match {
                        case Some(intentMatch) ⇒ Some(MatchHolder(intentMatch, callback, vrn, vrnIdx + 1))
                        case None ⇒ None
                    }
                })
            )
        }
    
        val results = mutable.ArrayBuffer.empty[NCIntentSolverResult]
    
        if (matches.nonEmpty) {
            // Retain only matches with minimal number of term nouns (including zero)
            // and out of those pick only the ones with maximum weight.
            val weightedMatches = {
                val minTermNouns = matches.minBy(_.intentMatch.termNouns.size).intentMatch.termNouns.size
                val maxWeight = matches.maxBy(_.intentMatch.weight).intentMatch.weight

                matches.
                    // Retain only ones with minimal number of term nouns.
                    filter(_.intentMatch.termNouns.lengthCompare(minTermNouns) == 0).
                    // Retain only the ones with maximum weight.
                    filter(_.intentMatch.weight == maxWeight)
            }

            val cnt = weightedMatches.size

            // Got to have at least one match here.
            require(cnt >= 1)
            
            // If there are more than one match - pick one from the most fitting
            // variant based on the variant's natural order.
            val theMatch = weightedMatches.maxBy(_.variant)

            results += NCIntentSolverResult(
                theMatch.intentMatch.intent.getId,
                theMatch.callback,
                new JArrayList(theMatch.intentMatch.tokGrps.map(lst ⇒ new JArrayList(lst.map(_.tok)))),
                new JArrayList(theMatch.intentMatch.termNouns),
                theMatch.intentMatch.exactMatch,
                theMatch.variant
            )

            if (!IS_PROBE_SILENT) {
                val tbl = NCAsciiTable("Pick", "Variant", "Matching Intent")
                
                if (cnt > 1) {
                    matches.foreach(m ⇒ {
                        val pick = m == theMatch

                        tbl += (
                            if (pick) " =>" else "",
                            s"#${m.variantIdx}",
                            if (pick) mkPickTokens(m.intentMatch) else m.intentMatch.intent
                        )
                    })

                    tbl.trace(logger, Some(s"Multiple matching intents found, best one picked :"))
                }
                else {
                    tbl += (
                        " =>",
                        s"#${theMatch.variantIdx}",
                        mkPickTokens(theMatch.intentMatch)
                    )
                    
                    tbl.trace(logger, Some(s"Found matching intent:"))
                }
            }
        }
        else if (!IS_PROBE_SILENT)
            logger.trace("No matching intent found.")

        results
    }
    
    /**
      *
      * @param im
      * @return
      */
    private def mkPickTokens(im: IntentMatch): List[String] = {
        val buf = mutable.ListBuffer.empty[String]
        
        buf += im.intent.toString
        
        var grpIdx = 0
        
        for (grp ← im.tokGrps) {
            buf += s"  Group #$grpIdx"
            
            grpIdx += 1
            
            if (grp.nonEmpty) {
                var tokIdx = 0
                
                for (tok ← grp) {
                    val conv = if (tok.conv) "(conv) " else ""
                    
                    buf += s"    #$tokIdx: $conv${tok.tok}"
                    
                    tokIdx += 1
                }
            }
            else
                buf += "    <empty>"
        }
        
        buf.toList
    }
    
    /**
      *
      * @param intent
      * @param senToks
      * @param convToks
      * @return
      */
    private def solveIntent(
        intent: INTENT,
        senToks: Seq[UseToken],
        convToks: Set[UseToken],
        varIdx: Int): Option[IntentMatch] = {
        var missedTermNouns = List.empty[String]
        var intentWeight = new Weight()
        val intentGrps = mutable.ListBuffer.empty[List[UseToken]]
        var abort = false
        
        val ordered = intent.isOrdered
        var lastTermMatch: TermMatch = null
        
        for (term ← intent.getTerms if !abort) {
            solveTerm(
                term,
                senToks,
                convToks
            ) match {
                case Some(termMatch) ⇒
                    if (ordered && lastTermMatch != null && lastTermMatch.maxIndex > termMatch.maxIndex)
                        abort = true
                    else {
                        // Term is found.
                        // Add its weight and grab its tokens.
                        intentWeight += termMatch.weight
                        intentGrps += termMatch.toks
                        
                        lastTermMatch = termMatch
                    }
                    
                case None ⇒
                    if (term.getTermNoun != null)
                        // Term is missing but we can still ask for it.
                        missedTermNouns ::= term.getTermNoun
                    else
                        // Term is missing and we can't ask for it.
                        // Stop further terms processing for this intent.
                        // This intent cannot be matched.
                        abort = true
            }
        }
        
        if (abort) {
            if (!IS_PROBE_SILENT)
                logger.trace(s"Intent didn't match because of missing term (variant #$varIdx): $intent")
    
            // 1. Match was aborted.
            None
        }
        else if (senToks.exists(tok ⇒ !tok.used && tok.tok.isUserDefined)) {
            if (!IS_PROBE_SILENT)
                logger.trace(s"Intent didn't match because of not exact match (variant #$varIdx): $intent")

            // 2. Not an exact match with user tokens.
            None
        }
        else if (!senToks.exists(tok ⇒ tok.used && !tok.conv)) {
            if (!IS_PROBE_SILENT)
                logger.trace(s"Intent didn't match because all tokens came from conversation (variant #$varIdx): $intent")

            // 3. All tokens came from history.
            None
        }
        else
            Some(IntentMatch(
                intentGrps.toList,
                intentWeight,
                intent,
                missedTermNouns,
                !senToks.exists(tok ⇒ !tok.used && !isFreeWord(tok.tok))
            ))
    }
    
    /**
      * 
      * @param term
      * @param convToks
      * @param senToks
      * @return
      */
    @throws[NCE]
    private def solveTerm(
        term: TERM,
        senToks: Seq[UseToken],
        convToks: Set[UseToken]): Option[TermMatch] = {
        var termToks = List.empty[UseToken]
        var termWeight = new Weight()
        var abort = false

        for (item ← term.getItems if !abort)
            solveItem(item, senToks, convToks) match {
                case Some(t) ⇒
                    termToks = termToks ::: t._1
                    termWeight += t._2
                    
                case None ⇒
                    abort = true
            }
        
        if (abort) {
            if (term.getTermNoun != null)
                // Even though the term is not found we can still ask for it
                // and therefore we need to rollback tokens used by already processed items
                // so that they can be used by other terms that can still be found further.
                termToks.foreach(_.used = false)
            
            None
        }
        else
            Some(TermMatch(termToks, termWeight))
    }
    
    /**
      *
      * @param item
      * @param senToks
      * @param convToks
      * @return
      */
    @throws[NCE]
    private def solveItem(
        item: ITEM,
        senToks: Seq[UseToken],
        convToks: Set[UseToken]): Option[(List[UseToken], Weight)] = {
        // Algorithm is "hungry", i.e. it will fetch all tokens satisfying item's predicate
        // in entire sentence even if these tokens are separated by other already used tokens
        // and conversation will be used only to get to the 'max' number of the item.
    
        var itemToks = List.empty[UseToken]
        val min = item.getMin
        val max = item.getMax
        val iw = Array(0, 0, 0) // Total 3-part items weight.
    
        /**
          *
          * @param from Collection to collect tokens from.
          * @param maxLen Maximum number of tokens to collect.
          */
        def collect(from: Iterable[UseToken], maxLen: Int): Boolean = {
            var found = false
            
            for (tok ← from.filter(!_.used) if itemToks.lengthCompare(maxLen) < 0)
                item.getPattern.apply(tok.tok) match {
                    case p if p.getLeft ⇒ // Item satisfying given token found.
                        itemToks :+= tok
                        
                        val w = p.getRight // Item's weight for this token.
    
                        iw(0) += w(0)
                        iw(1) += w(1)
                        iw(2) += w(2)
                        
                        found = true
                        
                    case _ ⇒
                }
            
            found
        }
    
        // Collect to the 'max', if possible.
        collect(senToks, max)
        
        // Specificity weight ('-1' if conversation was used, '1' if not).
        // Note that '-1' weight will counterbalance '1' when summed up, i.e. if a term has items
        // with specificity weight '1' and '-1' the total specificity weight for this term will be '0'.
        val w0 = if (collect(convToks, max)) -1 else 1
        
        if (itemToks.lengthCompare(min) < 0) // We couldn't collect even 'min' tokens.
            None
        else if (itemToks.isEmpty) { // Item is optional and no tokens collected (valid result).
            require(min == 0)
            
            Some(itemToks → new Weight())
        }
        else { // We've collected some tokens.
            itemToks.foreach(_.used = true) // Mark tokens as used.
    
            Some(itemToks → new Weight(w0, iw(0), iw(1), iw(2)))
        }
    }
    
    /**
      * Special method to use Scala side logging from Java call.
      *
      * @param name
      * @param intent
      */
    def ackNewIntent(name: String, intent: INTENT): Unit =
        if (!IS_PROBE_SILENT)
            logger.info(s"Intent added for '$name' solver: $intent")
}
