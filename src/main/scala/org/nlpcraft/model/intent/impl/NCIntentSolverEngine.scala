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

package org.nlpcraft.model.intent.impl

import java.util.function.Function
import java.util.{ArrayList ⇒ JArrayList, List ⇒ JList}

import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.lang3.tuple.Pair
import org.nlpcraft.common._
import org.nlpcraft.common.ascii.NCAsciiTable
import org.nlpcraft.model.intent.NCIntentSolver._
import org.nlpcraft.model.intent.NCIntentSolverContext
import org.nlpcraft.model.utils.NCTokenUtils._
import org.nlpcraft.model.{NCQueryResult, NCSentence, NCToken}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Intent solver that finds the best matching intent given user sentence.
  */
object NCIntentSolverEngine extends LazyLogging {
    private class Weight extends Ordered[Weight] {
        private val weights: Array[Int] = new Array(5)

        /**
          *
          * @param w0
          * @param w1
          * @param w2
          * @param w3
          * @param w4
          */
        def this(w0: Int, w1: Int, w2: Int, w3: Int, w4: Int) = {
            this()

            weights(0) = w0
            weights(1) = w1
            weights(2) = w2
            weights(3) = w3
            weights(4) = w4
        }

        /**
          * Sets specific weight at a given index.
          *
          * @param idx
          * @param w
          */
        def setWeight(idx: Int, w: Int): Unit =
            weights(idx) = w
    
        /**
          *
          * @param that
          * @return
          */
        def ++=(that: Weight): Weight = {
            for (i ← 0 until 5)
                this.setWeight(i, this.weights(i) + that.weights(i))
            
            this
        }
    
        /**
          *
          * @param that
          * @return
          */
        override def compare(that: Weight): Int = {
            var res = 0
    
            for ((i1, i2) ← this.weights.zip(that.weights) if res == 0)
                res = Integer.compare(i1, i2)
    
            res
        }
    
        override def toString: String =
            s"Weight (${weights(0)}, ${weights(1)}, ${weights(2)}, ${weights(3)}, ${weights(4)})"
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
      * @param exactMatch
      */
    private case class IntentMatch(
        tokGrps: List[List[UseToken]],
        weight: Weight,
        intent: INTENT,
        exactMatch: Boolean
    )

    /**
      *
      * @param t
      * @return
      */
    private def getGroup(t: NCToken): String = if (t.getGroup != null) t.getGroup else ""
    
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
        conv: JList[NCToken],
        intents: JList[Pair[INTENT, Function[NCIntentSolverContext, NCQueryResult]]]): JList[NCIntentSolverResult] = {
        case class MatchHolder(
            intentMatch: IntentMatch, // Match.
            callback: Function[NCIntentSolverContext, NCQueryResult], // Callback function.
            variant: NCIntentSolverVariant, // Variant used for the match.
            variantIdx: Int // Variant index.
        )
        
        val matches = mutable.ArrayBuffer.empty[MatchHolder]

        // Find all matches across all intents and sentence variants.
        for ((vrn, vrnIdx) ← sen.getVariants.zipWithIndex) {
            val availToks = vrn.filter(!isStopWord(_))
            
            matches.appendAll(
                intents.flatMap(pair ⇒ {
                    val intent = pair.getLeft
                    val callback = pair.getRight
                    
                    // Isolated sentence tokens.
                    val senToks = Seq.empty[UseToken] ++ availToks.map(UseToken(false, false, _))
                    val senTokGroups = availToks.map(getGroup).toSet

                    // Isolated conversation tokens.
                    val convToks =
                        if (intent.isIncludeConversation)
                            Seq.empty[UseToken] ++
                                // We shouldn't mix tokens with same group from conversation history and processed sentence.
                                conv.filter(t ⇒ !senTokGroups.contains(getGroup(t))).map(UseToken(false, true, _))
                        else
                            Seq.empty[UseToken]
    
                    // Solve intent in isolation.
                    solveIntent(intent, senToks, convToks, vrnIdx) match {
                        case Some(intentMatch) ⇒ Some(MatchHolder(intentMatch, callback, NCIntentSolverVariant(vrn), vrnIdx + 1))
                        case None ⇒ None
                    }
                })
            )
        }

        val sorted =
            matches.sortWith((m1: MatchHolder, m2: MatchHolder) ⇒
                // 1. First with maximum weight.
                m1.intentMatch.weight.compare(m2.intentMatch.weight) match {
                    case x1 if x1 < 0 ⇒ false
                    case x1 if x1 > 0 ⇒ true
                    case x1 ⇒
                        require(x1 == 0)

                        // 2. First with maximum variant.
                        m1.variant.compareTo(m2.variant) match {
                            case x2 if x2 < 0 ⇒ false
                            case x2 if x2 > 0 ⇒ true
                            case x2 ⇒
                                require(x2 == 0)

                                // 3. Tries to compare by 'weight' of system tokens fields.
                                // It can be applied only for same tokens types.
                                // First with maximum weight.
                                Integer.compare(
                                    calcSystemTokensWeight(m1.variant), calcSystemTokensWeight(m2.variant)
                                ) match {
                                    case x3 if x3 < 0 ⇒ false
                                    case x3 if x3 > 0 ⇒ true
                                    case x3 ⇒
                                        // Default, no matter, any value.
                                        require(x3 == 0)

                                        def calcHash(m: MatchHolder): Int =
                                            m.variant.tokens.map(t ⇒
                                                s"${t.getId}${t.getGroup}${t.getValue}${t.getMetadata.getString("NLP_NORMTEXT")}"
                                            ).mkString("").hashCode

                                        // Order doesn't make sense here.
                                        // It is just to provide deterministic result for the matches with the same weight.
                                        calcHash(m1) > calcHash(m2)
                                }
                        }
                }
            )

        if (sorted.nonEmpty) {
            val tbl = NCAsciiTable("Variant", "Intent", "Tokens")

            sorted.foreach(m ⇒
                tbl += (
                    s"#${m.variantIdx}",
                    m.intentMatch.intent.getId,
                    mkPickTokens(m.intentMatch)
                )
            )

            tbl.info(logger, Some(s"Found matching intents (sorted from best to worst match):"))
        }
        else
            logger.info("No matching intent found.")

        sorted.map(m ⇒
            NCIntentSolverResult(
                m.intentMatch.intent.getId,
                m.callback,
                new JArrayList(m.intentMatch.tokGrps.map(lst ⇒ new JArrayList(lst.map(_.tok)))),
                m.intentMatch.exactMatch,
                m.variant,
                m.variantIdx
            )
        )
    }

    /**
      *
      * @param v
      * @return
      */
    private def calcSystemTokensWeight(v: NCIntentSolverVariant): Int = {
        val x = v.tokens.asScala.map(t ⇒
            if (isGeo(t))
                t.getMetadata.getInteger("GEO_WEIGHT")
            else if (isNumeric(t))
                t.getMetadata.getInteger("NUM_WEIGHT")
            else if (isDate(t))
                t.getMetadata.getInteger("DATE_WEIGHT")
            else if (isCoordinate(t))
                t.getMetadata.getInteger("COORDINATE_WEIGHT")
            else if (isFunction(t))
                t.getMetadata.getInteger("FUNCTION_WEIGHT")
            else
                0
        ).sum

        println("v="+v.tokens.asScala)
        println("x="+x)
        println()

        x
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
        convToks: Seq[UseToken],
        varIdx: Int): Option[IntentMatch] = {
        val intentW = new Weight()
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
                        intentW ++= termMatch.weight
                        intentGrps += termMatch.toks
                        
                        lastTermMatch = termMatch
                    }
                    
                case None ⇒
                    // Term is missing. Stop further terms processing for this intent.
                    // This intent cannot be matched.
                    abort = true
            }
        }
        
        val intentId = intent.getId
        
        if (abort) {
            logger.info(s"Intent '$intentId' didn't match because of missing term (variant #$varIdx).")
    
            // 1. Match was aborted.
            None
        }
        else if (senToks.exists(tok ⇒ !tok.used && tok.tok.isUserDefined)) {
            logger.info(s"Intent '$intentId' didn't match because of not exact match (variant #$varIdx).")

            // 2. Not an exact match with user tokens.
            None
        }
        else if (!senToks.exists(tok ⇒ tok.used && !tok.conv)) {
            logger.info(s"Intent '$intentId' didn't match because all tokens came from conversation (variant #$varIdx).")

            // 3. All tokens came from history.
            None
        }
        else {
            val exactMatch = !senToks.exists(tok ⇒ !tok.used && !isFreeWord(tok.tok))
            
            intentW.setWeight(0, if (exactMatch) 1 else 0)

            Some(
                IntentMatch(
                    tokGrps = intentGrps.toList,
                    weight = intentW,
                    intent = intent,
                    exactMatch = exactMatch
                )
            )
        }
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
        convToks: Seq[UseToken]): Option[TermMatch] = {
        var termToks = List.empty[UseToken]
        var termWeight = new Weight()
        var abort = false

        for (item ← term.getItems if !abort)
            solveItem(item, senToks, convToks) match {
                case Some(t) ⇒
                    termToks = termToks ::: t._1
                    termWeight ++= t._2
                case None ⇒
                    abort = true
            }
        
        if (abort)
            None
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
        convToks: Seq[UseToken]
    ): Option[(List[UseToken], Weight)] = {
        // Algorithm is "hungry", i.e. it will fetch all tokens satisfying item's predicate
        // in entire sentence even if these tokens are separated by other already used tokens
        // and conversation will be used only to get to the 'max' number of the item.
    
        var itemToks = List.empty[UseToken]
        val min = item.getMin
        val max = item.getMax
        val itemW = Array(0, 0, 0) // Total 3-part items weight.

        /**
          *
          * @param from Collection to collect tokens from.
          * @param maxLen Maximum number of tokens to collect.
          */
        def collect(from: Iterable[UseToken], maxLen: Int): Unit =
            for (tok ← from.filter(!_.used) if itemToks.lengthCompare(maxLen) < 0)
                item.getPattern.apply(tok.tok) match {
                    case p if p.getLeft ⇒ // Item satisfying given token found.
                        itemToks :+= tok
                        
                        val w = p.getRight // Item's weight for this token.
    
                        itemW(0) += w(0)
                        itemW(1) += w(1)
                        itemW(2) += w(2)
                    case _ ⇒
                }

        // Collect to the 'max', if possible.
        collect(senToks, max)
        
        collect(convToks, max)

        if (itemToks.lengthCompare(min) < 0) // We couldn't collect even 'min' tokens.
            None
        else if (itemToks.isEmpty) { // Item is optional and no tokens collected (valid result).
            require(min == 0)
            
            Some(itemToks → new Weight())
        }
        else { // We've collected some tokens.
            // Youngest first.
            val convSrvReqIds = convToks.map(_.tok.getServerRequestId).distinct

            // Specificity weight ('1' if conversation wasn't used, -'index of conversation depth' if wasn't).
            // (It is better to be not from conversation or be youngest tokens from conversation)
            val specW = -itemToks.map(t ⇒ convSrvReqIds.indexOf(t.tok.getServerRequestId)).sum

            itemToks.foreach(_.used = true) // Mark tokens as used.

            Some(itemToks → new Weight(0/* set later */, specW, itemW(0), itemW(1), itemW(2)))
        }
    }
    
    /**
      * Special method to use Scala side logging from Java call.
      *
      * @param name
      * @param intent
      */
    def ackNewIntent(name: String, intent: INTENT): Unit =
        logger.trace(s"Intent added for '$name' solver: $intent")
}
