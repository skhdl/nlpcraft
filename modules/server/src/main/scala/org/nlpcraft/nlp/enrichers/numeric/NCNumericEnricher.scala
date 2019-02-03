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

package org.nlpcraft.nlp.enrichers.numeric

import org.nlpcraft._
import org.nlpcraft.nlp._
import org.nlpcraft.nlp.enrichers.NCNlpEnricher
import org.nlpcraft.nlp.numeric._
import org.nlpcraft.nlp.opennlp.NCNlpManager

import scala.collection._

/**
 * Numeric enricher.
 */
object NCNumericEnricher extends NCNlpEnricher("Numeric enricher") {
    val MAX_VALUE: Double = Double.MaxValue
    val MIN_VALUE: Double = Double.MinValue

    object T extends Enumeration {
        type T = Value

        val MORE: Value = Value
        val MORE_OR_EQUAL: Value = Value
        val LESS: Value = Value
        val LESS_OR_EQUAL: Value = Value
        val EQUAL: Value = Value
        val NOT_EQUAL: Value = Value
        val BETWEEN_INCLUSIVE: Value = Value
        val BETWEEN_EXCLUSIVE: Value = Value
    }

    import T._

    case class P(text: String, length: Int, prepositionType: T)

    private final val NUM_PREFIX_STOPS = NCNlpManager.stemSeq(Seq(
        "is",
        "was",
        "were",
        "are",
        "with value",
        "might be",
        "would be",
        "has value",
        "can be",
        "should be",
        "must be"
    ))

    // Note, that some sequences duplicated (like '==' and '= =') to be independent from tokenizer.
    // Note also that we account for frequent misspelling of 'then' vs. 'than'.
    private val BEFORE_PREPS: Map[String, P] =
        mkMap(Seq(
            "more than",
            "more then",
            "more",

            "greater than",
            "greater then",
            "greater",

            "larger than",
            "larger then",
            "larger",

            "bigger than",
            "bigger then",
            "bigger",

            ">"
        ), MORE) ++
        mkMap(Seq(
            "more or equal to",
            "more or equal",
            "more than or equal",
            "more than or equal to",
            "more then or equal",
            "more then or equal to",

            "greater or equal to",
            "greater or equal",
            "greater than or equal",
            "greater than or equal to",
            "greater then or equal",
            "greater then or equal to",

            "larger or equal to",
            "larger or equal",
            "larger than or equal",
            "larger than or equal to",
            "larger then or equal",
            "larger then or equal to",

            "bigger or equal to",
            "bigger or equal",
            "bigger than or equal",
            "bigger than or equal to",
            "bigger then or equal",
            "bigger then or equal to",

            "not less than",
            "not less then",
            "not less",
            "no less than",
            "no less then",
            "no less",

            ">=",
            "> ="
        ), MORE_OR_EQUAL) ++
        mkMap(Seq(
            "less than",
            "less then",
            "less",
            "<"
        ), LESS) ++
        mkMap(Seq(
            "less or equal to",
            "less than or equal to",
            "less than or equal",
            "less then or equal to",
            "less then or equal",
            "less or equal",

            "smaller or equal to",
            "smaller than or equal to",
            "smaller than or equal",
            "smaller then or equal to",
            "smaller then or equal",
            "smaller or equal",

            "no more than",
            "no more then",
            "no more",

            "no greater than",
            "no greater then",
            "no greater",
            "not greater than",
            "not greater then",
            "not greater",

            "<=",
            "< ="
        ), LESS_OR_EQUAL) ++
        mkMap(Seq(
            "same as",
            "equal to",
            "equal",
            "= =",
            "==",
            "="
        ), EQUAL) ++
        mkMap(Seq(
            "not",
            "not equal to",
            "not equal",
            "not same as",
            "!=",
            "! =",
            "<>",
            "< >"
        ), NOT_EQUAL)

    // Supported prepositions which contains one word only.
    private val BETWEEN_PREPS: Map[(String, String), T] = Map(
        ("between", "and") → BETWEEN_EXCLUSIVE,
        ("from", "to") → BETWEEN_INCLUSIVE,
        ("since", "to") → BETWEEN_INCLUSIVE,
        ("since", "till") → BETWEEN_INCLUSIVE,
        ("from", "till") → BETWEEN_INCLUSIVE
    )

    private def mkMap(seq: Seq[String], c: T): Map[String, P] =
        seq.map(s ⇒ s → P(s, s.split(" ").length, c)).toMap

    private def toString(seq: Seq[NCNlpSentenceToken], sep: String = " ", stem: Boolean = false) =
        seq.map(t ⇒ if (stem) t.stem else t.normText).mkString(sep)

    private def mkNote(
        toks: Seq[NCNlpSentenceToken],
        from: Double,
        fromIncl: Boolean,
        fromFractional: Boolean,
        to: Double,
        toIncl: Boolean,
        toFractional: Boolean,
        unitOpt: Option[NCNumericUnit]
    ): NCNlpSentenceNote = {
        val params = mutable.ArrayBuffer.empty[(String, Any)] ++
            Seq(
                "from" → from,
                "fromIncl" → fromIncl,
                "to" → to,
                "toIncl" → toIncl,
                "isFractional" → (fromFractional || toFractional),
                "isRangeCondition" → (from != to),
                "isEqualCondition" → (from == to && fromIncl && toIncl),
                "isNotEqualCondition" → (from == to && !fromIncl && !toIncl),
                "isFromNegativeInfinity" → (from == MIN_VALUE),
                "isToPositiveInfinity" → (to == MAX_VALUE),
                "length" → toks.map(_.words).sum
            )

        unitOpt match {
            case Some(unit) ⇒
                params += "unit" → unit.name
                params += "unitType" → unit.unitType
            case None ⇒ // No-op.
        }
    
        NCNlpSentenceNote(toks.map(_.index), "nlp:num", params:_*)
    }

    @throws[NCE]
    override def enrich(ns: NCNlpSentence) {
        val nums = NCNumericManager.find(ns)

        val toksStopFree = ns.filter(!_.isStopword)

        val processed = mutable.Buffer.empty[NCNlpSentenceToken]

        // Adds complex 'condition' notes.
        // Note that all complex prepositions contains from single words only.
        for (ps ← nums.sliding(2) if !processed.exists(p ⇒ ps.flatMap(_.tokens).contains(p)); p ← BETWEEN_PREPS) {
            val num1 = ps.head
            val num2 = ps.last

            val ts1 = num1.tokens
            val d1 = num1.value
            val isF1 = num1.isFractional

            val ts2 = num2.tokens
            val d2 = num2.value
            val isF2 = num2.isFractional


            // Part of sentence started from first part of preposition.
            val subLine = toString(toksStopFree.takeRight(toksStopFree.length - toksStopFree.indexOf(ts1.head) + 1))

            // Line represents complex condition (from num1 to num2)
            val condTxt = Seq(p._1._1, toString(ts1), p._1._2, toString(ts2)).mkString(" ")

            if (subLine.startsWith(condTxt)) {
                def getBefore(ts: Seq[NCNlpSentenceToken]): NCNlpSentenceToken = toksStopFree(toksStopFree.indexOf(ts.head) - 1)

                val prepToks = Seq(getBefore(ts1)) ++ ts1 ++ Seq(getBefore(ts2)) ++ ts2

                val badRange = num1.unit.isDefined && num2.unit.isDefined && num1.unit != num2.unit

                if (!badRange) {
                    val unit =
                        if (num1.unit.isDefined && num2.unit.isEmpty)
                            num1.unit
                        else if (num1.unit.isEmpty && num2.unit.isDefined)
                            num2.unit
                        else if (num1.unit.isEmpty && num2.unit.isEmpty)
                            None
                        else{
                            require(num1.unit == num2.unit)

                            num1.unit
                        }

                    val note = p._2 match {
                        case BETWEEN_EXCLUSIVE ⇒
                            mkNote(
                                prepToks,
                                d1,
                                fromIncl = false,
                                fromFractional = isF1,
                                to = d2,
                                toIncl = false,
                                toFractional = isF2,
                                unit
                            )
                        case BETWEEN_INCLUSIVE ⇒
                            mkNote(
                                prepToks,
                                d1,
                                fromIncl = true,
                                fromFractional = isF1,
                                to = d2,
                                toIncl = true,
                                toFractional = isF2,
                                unit
                            )
                        case _ ⇒ throw new AssertionError(s"Illegal note type: ${p._2}.")
                    }

                    prepToks.foreach(_.add(note))

                    processed ++= ts1
                    processed ++= ts2
                }
            }
        }

        // Special case - processing with words which were defined as stop words before.
        // Example: symbol '!' for condition '! ='.
        // Adds simple 'condition' notes.
        for (num ← nums) {
            def process(candidates: Seq[NCNlpSentenceToken]): Unit =
                if (!processed.exists(num.tokens.contains)) {
                    val strBuf = toString(candidates)

                    val preps: Seq[(String, P)] = BEFORE_PREPS.filter(p ⇒ strBuf.endsWith(p._1)).toSeq.sortBy(-_._2.length)

                    if (preps.nonEmpty) {
                        val prep = preps.head._2
                        val toks = candidates.takeRight(prep.length) ++ num.tokens

                        processed ++= toks

                        val note =
                            prep.prepositionType match {
                                case MORE ⇒
                                    mkNote(
                                        toks,
                                        num.value,
                                        fromIncl = false,
                                        fromFractional = num.isFractional,
                                        to = MAX_VALUE,
                                        toIncl = true,
                                        toFractional = num.isFractional,
                                        num.unit
                                    )
                                case MORE_OR_EQUAL ⇒
                                    mkNote(
                                        toks,
                                        num.value,
                                        fromIncl = true,
                                        fromFractional = num.isFractional,
                                        to = MAX_VALUE,
                                        toIncl = true,
                                        toFractional = num.isFractional,
                                        num.unit
                                    )
                                case LESS ⇒
                                    mkNote(
                                        toks,
                                        MIN_VALUE,
                                        fromIncl = true,
                                        fromFractional = num.isFractional,
                                        to = num.value,
                                        toIncl = false,
                                        toFractional = num.isFractional,
                                        num.unit
                                    )
                                case LESS_OR_EQUAL ⇒
                                    mkNote(
                                        toks,
                                        MIN_VALUE,
                                        fromIncl = true,
                                        fromFractional = num.isFractional,
                                        to = num.value,
                                        toIncl = true,
                                        toFractional = num.isFractional,
                                        num.unit
                                    )
                                case EQUAL ⇒
                                    mkNote(
                                        toks,
                                        num.value,
                                        fromIncl = true,
                                        fromFractional = num.isFractional,
                                        to = num.value,
                                        toIncl = true,
                                        toFractional = num.isFractional,
                                        num.unit
                                    )
                                case NOT_EQUAL ⇒
                                    mkNote(
                                        toks,
                                        num.value,
                                        fromIncl = false,
                                        fromFractional = num.isFractional,
                                        to = num.value,
                                        toIncl = false,
                                        toFractional = num.isFractional,
                                        num.unit
                                    )
                                case _ ⇒ throw new AssertionError(s"Illegal note type: ${prep.prepositionType}.")
                            }

                        toks.foreach(_.add(note))
                    }
            }

            val toks = ns.takeWhile(_ != num.tokens.head)

            process(toks)
            process(toks.filter(!_.isStopword))
        }

        // Numeric without conditions.
        for (num ← nums if !processed.exists(num.tokens.contains)) {
            val note = mkNote(
                num.tokens,
                num.value,
                fromIncl = true,
                num.isFractional,
                num.value,
                toIncl = true,
                num.isFractional,
                num.unit
            )

            processed ++= num.tokens

            num.tokens.foreach(_.add(note))
        }

        // Try to find words from configured list before numeric condition and mark them as STOP words.
        ns.getNotes("nlp:num").foreach(numNote ⇒ {
            val before = ns.filter(_.index < numNote.tokenFrom)

            before.indices.map(i ⇒ before.drop(i)).find(
                seq ⇒ seq.forall(
                    t ⇒ t.isStopword ||
                    (!t.isBracketed && !t.isQuoted)) &&
                    NUM_PREFIX_STOPS.contains(toString(seq.filter(!_.isStopword), stem = true)
                )
            ) match {
                case Some(seq) ⇒ seq.filter(!_.isStopword).foreach(_.getNlpNote += "stopWord" → true)
                case None ⇒ // No-op.
            }
        })
    }
}