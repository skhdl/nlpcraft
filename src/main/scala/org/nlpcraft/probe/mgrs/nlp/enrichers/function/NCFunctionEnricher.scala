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

package org.nlpcraft.probe.mgrs.nlp.enrichers.function

import org.nlpcraft.common.NCLifecycle
import org.nlpcraft.common.makro.NCMacroParser
import org.nlpcraft.common.nlp._
import org.nlpcraft.common.nlp.core.NCNlpManager
import org.nlpcraft.probe.mgrs.NCModelDecorator
import org.nlpcraft.probe.mgrs.nlp.NCProbeEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.function.NCFunction._
import org.nlpcraft.probe.mgrs.nlp.enrichers.function.mgrs._

import scala.collection.JavaConverters._
import scala.collection._

/**
  * Functions enricher.
  *
  * This enricher must be used after Model elements enricher.
  */
object NCFunctionEnricher extends NCProbeEnricher("Function enricher") {
    case class ComplexHolder(function: NCFunction, headStem: String, allStems: Set[String])
    case class SortHolder(asc: Boolean, byPart: Seq[NCNlpSentenceToken], orderPart: Seq[NCNlpSentenceToken])
    case class TypedFunction(function: NCFunction, isNumeric: Boolean)

    private var simpleBeforeFuncs: Map[String, TypedFunction] = _
    private var complexFuncs: Set[ComplexHolder] = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        val macros = NCMacroParser()

        simpleBeforeFuncs = {
            val m = mutable.HashMap.empty[String, TypedFunction]

            def add(f: NCFunction, isNumeric: Boolean, syns: String*): Unit =
                syns.
                    flatMap(macros.expand).
                    map(p ⇒ p.split(" ").map(_.trim).map(NCNlpManager.stem).mkString(" ")).
                    foreach(s ⇒ m += s → TypedFunction(f, isNumeric))

            add(SUM, isNumeric = true, "{sum|summary} {of|*} {data|value|*}")
            add(MAX, isNumeric = true, "{max|maximum} {of|*} {data|value|*}")
            add(MIN, isNumeric = true, "{min|minimum|minimal} {of|*} {data|value|*}")
            add(AVG, isNumeric = true, "{avg|average} {of|*} {data|value|*}")
            add(GROUP, isNumeric = false, "{group|grouped} {of|by|with|for}")

            m
        }

        complexFuncs = {
            val set = mutable.HashSet.empty[ComplexHolder]

            /**
              *
              * @param f Function ID.
              * @param head Mandatory "start" word(s).
              * @param tails All possible "connecting" words (zero or more can be in the sentence).
              */
            def add(f: NCFunction, head: String, tails: Seq[String]): Unit = {
                require(!head.contains(" "))

                val tailsSeq = tails.toSet.map(NCNlpManager.stem)

                macros.expand(head).map(NCNlpManager.stem).foreach(s ⇒ set += ComplexHolder(f, s, tailsSeq + s))
            }

            val tails = Seq(
                "to",
                "and",
                "for",
                "with",
                "in",
                "of",
                "between",
                "statistic",
                "stats",
                "info",
                "metrics",
                "information",
                "data"
            )

            add(COMPARE, "{compare|comparing|contrast|difference|match|vs}", tails)
            add(CORRELATION, "{correlation|relationship}", tails)

            set
        }

        // Validation.
        require(
            (simpleBeforeFuncs.values ++ complexFuncs.map(_.function) ++ Seq(SORT, LIMIT)).
                toSeq.distinct.lengthCompare(NCFunction.values.size) == 0
        )

        super.start()
    }

    /**
      * Processes this NLP sentence.
      *
      * @param mdl Model decorator.
      * @param ns NLP sentence to enrich.
      */
    override def enrich(mdl: NCModelDecorator, ns: NCNlpSentence): Unit = {
        val limitMgr = NCLimitManager(ns)

        val buf = mutable.Buffer.empty[Set[NCNlpSentenceToken]]

        def areSuitableTokens(toks: Seq[NCNlpSentenceToken]): Boolean =
            toks.forall(t ⇒ !t.isQuoted && !t.isBracketed) && !buf.exists(_.exists(t ⇒ toks.contains(t)))

        val seq = ns.tokenMixWithStopWords()

        // Tries to grab tokens direct way.
        // Example: A, B, C ⇒ ABC, AB, BC .. (AB will be processed first)
        for (toks ← seq if areSuitableTokens(toks))
            if (isComplex(toks) || isSort(toks) || isSimpleBefore(toks))
                buf += toks.toSet

        // Tries to grab tokens reverse way.
        // Example: A, B, C ⇒ ABC, BC, AB .. (BC will be processed first)
        for (toks ← seq.sortBy(p ⇒ (-p.size, -p.head.index)) if areSuitableTokens(toks))
            if (isLimit(toks, limitMgr))
                buf += toks.toSet
    }

    /**
      * Calculates tokens length.
      *
      * @param toks Tokens.
      */
    private def sumWords(toks: Seq[NCNlpSentenceToken]): Int = toks.map(_.words).sum

    /**
      * Gets `is-complex` conditions processing flag.
      *
      * @param toks Tokens.
      */
    private def isComplex(toks: Seq[NCNlpSentenceToken]): Boolean = {
        def isUser(t: NCNlpSentenceToken): Boolean = t.exists(!_.isUser)
        def isDate(t: NCNlpSentenceToken): Boolean = t.exists(_.noteType == "nlp:date")
        def isGeo(t: NCNlpSentenceToken): Boolean = t.exists(_.noteType == "nlp:geo")

        val fTks = toks.filter(isNotMeaningful)
        val fTksStms = fTks.map(_.stem)
        val eTks = toks.filter(isMeaningful)

        var ok = false

        if (eTks.forall(t ⇒ t.isTypeOf("nlp:function") || isUser(t) || isDate(t) || isGeo(t)))
            complexFuncs.flatMap(cf ⇒
                if (fTksStms.forall(cf.allStems.contains) && fTksStms.contains(cf.headStem)) Some(cf) else None
            ).headOption match {
                case Some(cf) ⇒
                    val head = fTks.
                        find(_.stem == cf.headStem).
                        getOrElse(throw new AssertionError("Missed token."))

                    val elemTks = toks.filter(isMeaningful)

                    mark(cf.function, Seq(head), sumWords(fTks), if (elemTks.nonEmpty) Some(elemTks) else None)
                    markStop(fTks.filter(_ != head))

                    ok = true
                case None ⇒ // No-op.
            }

        ok
    }

    /**
      * Gets `is-sort` condition processing flag.
      *
      * @param toks Tokens.
      */
    private def isSort(toks: Seq[NCNlpSentenceToken]): Boolean = {
        def isSort0: Boolean =
            (for (n ← toks.length until 0 by -1) yield toks.sliding(n)).flatten.flatMap(seq ⇒
                if (NCOrderManager.sortByWords.contains(toStemKey(seq))) {
                    val others = toks.filter(t ⇒ !seq.contains(t))

                    if (others.isEmpty)
                        Some(SortHolder(asc = true, byPart = seq, orderPart = Seq.empty))
                    else {
                        if (others.flatten.count(_.isUser) == 1) {
                            val otherNonProcStr = toStemKey(others.filter(isNotMeaningful).filter(!_.isStopword))

                            NCOrderManager.sortOrderWords.get(otherNonProcStr) match {
                                case Some(asc) ⇒ Some(SortHolder(asc, byPart = seq, orderPart = others))
                                case None ⇒ Some(SortHolder(asc = true, byPart = seq, orderPart = Seq.empty))
                            }
                        }
                        else
                            None
                    }
                }
                else
                    None
            ).headOption match {
                case Some(oh) ⇒
                    val elemTks = toks.filter(isMeaningful)

                    if (elemTks.nonEmpty) {
                        mark(
                            SORT,
                            oh.byPart,
                            sumWords(oh.byPart) + sumWords(oh.orderPart),
                            Some(elemTks),
                            "asc" → oh.asc
                        )

                        markStop(oh.orderPart)

                        true
                    }
                    else
                        false
                case None ⇒ false
            }

        def isVacuous0(sortToks: Seq[NCNlpSentenceToken], ascToks: Seq[NCNlpSentenceToken]): Boolean = {
            Seq((true, true), (true, false), (false, true), (false, false)).
                toStream.
                flatMap { case (withStopSort, withStopAsc) ⇒
                    val sort = toStemKey(sortToks, withStopSort)
                    val asc = toStemKey(ascToks, withStopAsc)

                    if (
                        NCOrderManager.sortByWords.contains(sort) &&
                        (ascToks.isEmpty || NCOrderManager.sortOrderWords.contains(asc))
                    ) {
                        mark(
                            SORT,
                            toks,
                            sumWords(toks),
                            None,
                            "asc" → NCOrderManager.sortOrderWords.getOrElse(asc, true)
                        )

                        Some(true)
                    }
                    else
                        None
                }.headOption.
            getOrElse(false)
        }

        def isVacuous(withStop: Boolean): Boolean =
            (0 to toks.length).
                toStream.
                flatMap(i ⇒
                    if (isVacuous0(toks.take(i), toks.drop(i)) || isVacuous0(toks.drop(i), toks.take(i)))
                        Some(true)
                    else
                        None
                ).
                headOption.
                getOrElse(false)

        isSort0 || isVacuous(true) || isVacuous(false)
    }

    /**
      * Gets `is-limit` conditions processing flag.
      *
      * @param toks Tokens.
      * @param limitMgr Limits manager.
      */
    def isLimit(toks: Seq[NCNlpSentenceToken], limitMgr: NCLimitManager): Boolean = {
        def process(limits: Seq[NCNlpSentenceToken], elems: Option[Seq[NCNlpSentenceToken]], allowSimpleNums: Boolean): Boolean =
            limitMgr.get(limits) match {
                case Some(limitData) ⇒
                    if (!limitData.simpleNumeric || allowSimpleNums) {
                        val params = mutable.ArrayBuffer.empty[(String, Any)] ++ Seq("limit" → limitData.limit)

                        if (limitData.asc.isDefined)
                            params ++= Seq("asc" → limitData.asc.get)

                        mark(LIMIT, toks, sumWords(toks), elems, params: _*)

                        true
                    }
                    else
                        false
                case None ⇒ false
            }

        def isLimit0(withStops: Boolean): Boolean = {
            val limitToks = if (withStops) toks else toks.filter(!_.isStopword)
            val userNotes = toks.flatten.filter(_.isUser)

            userNotes.size match {
                case 1 ⇒
                    val userNoteId = userNotes.head.id
                    val limCands = limitToks.filter(t ⇒ !t.contains(userNoteId))

                    process(limCands, Some(limitToks.diff(limCands)), true)
                case _ ⇒ false
            }
        }

        def isVacuous(withStops: Boolean): Boolean =
            process(if (withStops) toks else toks.filter(!_.isStopword), None, false)

        isLimit0(true) || isLimit0(false) || isVacuous(true) || isVacuous(false)
    }

    /**
      * Gets `is-simple` conditions processing flag.
      *
      * @param toks Tokens.
      */
    private def isSimpleBefore(toks: Seq[NCNlpSentenceToken]): Boolean = {
        def isSimpleBefore0: Boolean = {
            val before = toks.takeWhile(isNotMeaningful)

            var ok = false

            if (before.nonEmpty)
                simpleBeforeFuncs.get(toStemKey(before)) match {
                    case Some(f) ⇒
                        val after = toks.drop(before.length)

                        after.find(p ⇒ if (f.isNumeric) p.exists(_.isUser) else isMeaningful(p)) match {
                            case Some(aggrCand) ⇒
                                if (
                                    aggrCand.count(_.isUser) >= 1 &&
                                        after.filter(_ != aggrCand).forall(t ⇒ !t.isQuoted && t.isStopword || t.pos == "IN")
                                ) {
                                    val elemTks = clearStopsAround(after)

                                    if (elemTks.nonEmpty) {
                                        mark(f.function, before, sumWords(before), Some(elemTks))

                                        ok = true
                                    }
                                }

                            case None ⇒ // No-op.
                        }

                    case None ⇒ // No-op.
                }

            ok
        }

        def isVacuous(withStop: Boolean): Boolean =
            simpleBeforeFuncs.get(toStemKey(toks, withStop)) match {
                case Some(f) ⇒
                    mark(f.function, toks, sumWords(toks), None)

                    true
                case None ⇒ false
            }

        isSimpleBefore0 || isVacuous(true) || isVacuous(false)
    }

    private def toStemKey(toks: Seq[NCNlpSentenceToken], withStop: Boolean): String =
        toStemKey(if (withStop) toks else toks.filter(!_.isStopword))

    /**
      * Clears heads and tails stop words.
      *
      * @param toks Tokens
      */
    private def clearStopsAround(toks: Seq[NCNlpSentenceToken]): Seq[NCNlpSentenceToken] = {
        def clear(toks: Seq[NCNlpSentenceToken]): Seq[NCNlpSentenceToken] = toks.dropWhile(t ⇒ t.isStopword && isNotMeaningful(t))

        clear(clear(toks).reverse).reverse
    }

    /**
      * Marks tokens.
      *
      * @param f Function.
      * @param toks Functions tokens.
      * @param len Calculated length.
      * @param elemToks Elements tokens.
      * @param optArgs Additional parameters. Optional.
      */
    private def mark(
        f: NCFunction, toks: Seq[NCNlpSentenceToken], len: Int, elemToks: Option[Seq[NCNlpSentenceToken]], optArgs: (String, Any)*
    ): Unit = {
        val note = NCNlpSentenceNote(
            toks.map(_.index),
            "nlp:function",
            Seq(
                "type" → f.toString,
                "length" → len,
                "indexes" → elemToks.getOrElse(Seq.empty).map(_.index).asJava
            ) ++ optArgs: _*
        )

        toks.foreach(_.add(note))
    }

    /**
      * Gets `meaningful ` flag.
      *
      * @param t Token.
      */
    private def isMeaningful(t: NCNlpSentenceToken): Boolean = t.exists(!_.isNlp)

    /**
     * Gets not `meaningful ` flag.
     *
     * @param t Token.
     */
    private def isNotMeaningful(t: NCNlpSentenceToken): Boolean = !isMeaningful(t)

    /**
      * Marks tokens as stop words.
      *
      * @param toks Tokens.
      */
    private def markStop(toks: Seq[NCNlpSentenceToken]): Unit = toks.foreach(_.getNlpNote += "stopWord" → true)
}
