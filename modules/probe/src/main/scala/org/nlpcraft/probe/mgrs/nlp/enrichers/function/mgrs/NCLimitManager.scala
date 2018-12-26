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

package org.nlpcraft.probe.mgrs.nlp.enrichers.function.mgrs

import org.nlpcraft.makro.{NCMacroParser => Parser}
import org.nlpcraft.nlp.numeric.NCNumericsManager
import org.nlpcraft.nlp.opennlp.NCNlpManager
import org.nlpcraft.nlp.{NCNlpSentence => Sentence, NCNlpSentenceToken => Token}

/**
  * Limit data container.
  *
  * @param limit Limit value.
  * @param asc `asc` direction flag.
  * @param simpleNumeric `simpleNumeric` flag.
  */
case class NCLimitData(limit: Double, asc: Option[Boolean], simpleNumeric: Boolean)

/**
  * Limit manager helper.
  */
object NCLimitManager {
    private final val DFLT_LIMIT = 10

    // Note that single words only supported now in code.
    private final val FUZZY_NUMS: Map[String, Int] = stemmatizeWords(Map(
        "few" → 3,
        "several" → 3,
        "handful" → 5,
        "single" → 1,
        "some" → 3,
        "couple" → 2
    ))

    // Note that single words only supported now in code.
    private final val SORT_WORDS: Map[String, Boolean] = stemmatizeWords(Map(
        "top" → false,
        "most" → false,
        "first" → false,
        "bottom" → true,
        "last" → true
    ))

    private final val TOP_WORDS: Seq[String] = Seq(
        "top",
        "most",
        "bottom",
        "first",
        "last"
    ).map(NCNlpManager.stem)

    private final val POST_WORDS: Seq[String] = Seq(
        "total",
        "all together",
        "overall"
    ).map(NCNlpManager.stem)

    // It designates:
    // - digits (like `25`),
    // - word numbers (like `twenty two`) or
    // - fuzzy numbers (like `few`).
    private final val CD = "[CD]"

    // Macros: SORT_WORDS, TOP_WORDS, POST_WORDS
    private final val MACROS: Map[String, Iterable[String]] = Map(
        "SORT_WORDS" → SORT_WORDS.keys,
        "TOP_WORDS" → TOP_WORDS,
        "POST_WORDS" → POST_WORDS
    )

    // Possible elements:
    // - Any macros,
    // - Special symbol CD (which designates obvious number or fuzzy number word)
    // - Any simple word.
    // Note that `CD` is optional (DFLT_LIMIT will be used)
    private final val SYNONYMS = Seq(
        s"<TOP_WORDS> {of|*} {$CD|*} {<POST_WORDS>|*}",
        s"$CD of",
        s"$CD <POST_WORDS>",
        s"<POST_WORDS> $CD"
    )

    private val limits: Seq[String] = {
        // Few numbers cannot be in on template.
        require(SYNONYMS.forall(_.split(" ").map(_.trim).count(_ == CD) < 2))

        def toMacros(seq: Iterable[String]): String = seq.mkString("|")

        val parser = Parser(MACROS.map { case(name, seq) ⇒ s"<$name>" → s"{${toMacros(seq)}}"})

        // Duplicated elements is not a problem.
        SYNONYMS.flatMap(parser.expand).distinct
    }

    /**
      * Group of neighbouring tokens. All of them numbers or all of the not.
      *
      * @param tokens Tokens.
      * @param number Tokens numeric value. Optional.
      */
    case class Group(tokens: Seq[Token], number: Option[Int]) {
        def stem: String = number match {
            case Some(_) ⇒ CD
            case None ⇒ tokens.map(_.stem).mkString(" ")
        }

        def index: Int = tokens.head.index
    }

    /**
      * Neighbouring groups.
      *
      * @param groups Groups.
      */
    case class GroupsHolder(groups: Seq[Group]) {
        def tokens: Seq[Token] = groups.flatMap(_.tokens)
        def limit: Int = {
            val numElems = groups.filter(_.number.isDefined)

            numElems.size match {
                case 0 ⇒ DFLT_LIMIT
                case 1 ⇒ numElems.head.number.get
                case _ ⇒ throw new AssertionError(s"Unexpected numeric count in template: ${numElems.size}")
            }
        }

        def asc: Boolean = {
            val sorts: Seq[Boolean] = tokens.map(_.stem).flatMap(SORT_WORDS.get)

            sorts.size match {
                case 1 ⇒ sorts.head
                case _ ⇒ false
            }
        }

        def stem: String = groups.map(_.stem).mkString(" ")
    }

    /**
      * Stemmatises map's keys.
      *
      * @param m Map.
      */
    private def stemmatizeWords[T](m: Map[String, T]): Map[String, T] = m.map(p ⇒ NCNlpManager.stem(p._1) → p._2)

    /**
      * Tries to parse number.
      *
      * @param t Token.
      */
    private def parseNum(t: Token): Option[Int] =
        t.nne match {
            case Some(nne) ⇒
                try
                    Some(java.lang.Double.parseDouble(nne).toInt)
                catch {
                    case _: NumberFormatException ⇒ None
                }

            case None ⇒ FUZZY_NUMS.get(t.stem)
        }
}

import org.nlpcraft.probe.mgrs.nlp.enrichers.function.mgrs.NCLimitManager._

/**
  * Limit manager.
  */
case class NCLimitManager(ns: Sentence) {
    private val map: Map[Seq[Token], GroupsHolder] = {
        // All groups combinations.
        val tks2Nums: Seq[(Token, Option[Int])] = ns.filter(!_.isStopword).map(t ⇒ t → parseNum(t))

        // Tokens: A;  B;  20;  C;  twenty; two, D
        // NERs  : -;  -;  20;  -;  22;     22;  -
        // Groups: (A) → -; (B) → -; (20) → 20; (C) → -; (twenty, two) → 22; (D) → -;
        val groups: Seq[Group] = tks2Nums.zipWithIndex.groupBy { case ((_, numOpt), idx) ⇒
            // Groups by artificial flag.
            // Flag is first index of independent token.
            // Tokens:  A;  B;  20;  C;  twenty; two, D
            // Indexes  0;  1;  2;   3;  4;      4;   6
            if (idx == 0)
                0
            else {
                // Finds last another.
                var i = idx

                while (i > 0 && numOpt.isDefined && tks2Nums(i - 1)._2 == numOpt)
                    i  = i - 1

                i
            }
        }.
            // Converts from artificial group to tokens groups (Seq[Token], Option[Int])
            map { case (_, gs) ⇒ gs.map { case (seq, _) ⇒ seq } }.
            map(seq ⇒ Group(seq.map { case(t, _) ⇒ t }, seq.head._2)).
            // Converts to sequence and sorts.
            toSeq.sortBy(_.index)

        (for (n ← groups.length until 0 by -1) yield groups.sliding(n).map(GroupsHolder)).
            flatten.
            map(p ⇒ p.tokens → p).
            toMap
    }

    private val nums = NCNumericsManager.find(ns).filter(_.unit.isEmpty)

    private def trySimpleNumeric(toks: Seq[Token]): Option[NCLimitData] = {
        val mToks = toks.filter(!_.isStopword)

        // Given argument should start from numeric and ended by used token
        // Also, we don't need check that is it single.
        nums.find(n ⇒ mToks.startsWith(n.tokens) && mToks.drop(n.tokens.length).flatten.forall(_.isUser)) match {
            case Some(num) ⇒ Some(NCLimitData(num.value, None, true))
            case None ⇒ None
        }
    }

    /**
      * Gets optional limit data container.
      *
      * Note that tokens which provided as argument contains one user token.
      *
      * @param toks Tokens.
      */
    def get(toks: Seq[Token]): Option[NCLimitData] =
        map.get(toks) match {
            case Some(g) ⇒
                if (limits.contains(g.stem))
                    Some(NCLimitData(g.limit, Some(g.asc), false))
                else
                    trySimpleNumeric(toks)
            case None ⇒ None
        }
}