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

import java.util

import org.nlpcraft.model.NCToken
import org.nlpcraft.model.utils.NCTokenUtils.{getSparsity, getWordLength, isDirectSynonym, isFreeWord, isStopWord}

import scala.collection.JavaConverters._

/**
  * Sentence variant.
  */
case class NCIntentSolverVariant(tokens: util.List[NCToken]) extends Ordered[NCIntentSolverVariant] {
    private val (userToks, wordCnt, avgWordsPerTok, totalSparsity, totalUserDirect)  = calcWeight()

    /**
      * Calculates weight components.
      */
    private def calcWeight(): (Int, Int, Float, Int, Int) = {
        var userToks = 0 // More is better.
        var wordCnt = 0
        var avgWordsPerTok = 0f
        var totalSparsity = 0 // Less is better.
        var totalUserDirect = 0

        var tokCnt = 0

        for (tok ← tokens.asScala if !isFreeWord(tok) && !isStopWord(tok)) {
            wordCnt += getWordLength(tok)
            totalSparsity += getSparsity(tok)

            if (tok.isUserDefined) {
                userToks += 1

                if (isDirectSynonym(tok))
                    totalUserDirect += 1
            }

            tokCnt += 1
        }

        avgWordsPerTok = if (wordCnt > 0) tokCnt.toFloat / wordCnt else 0

        (userToks, wordCnt, avgWordsPerTok, totalSparsity, totalUserDirect)
    }

    override def compare(v: NCIntentSolverVariant): Int =
        if (userToks > v.userToks) 1
        else if (userToks < v.userToks) -1

        else if (wordCnt > v.wordCnt) 1
        else if (wordCnt < v.wordCnt) -1

        else if (totalUserDirect > v.totalUserDirect) 1
        else if (totalUserDirect < v.totalUserDirect) -1

        else if (avgWordsPerTok > v.avgWordsPerTok) 1
        else if (avgWordsPerTok < v.avgWordsPerTok) -1

        else Integer.compare(v.totalSparsity, totalSparsity)

    override def toString: String =
        s"Variant " +
        s"[userToks=$userToks" +
        s", wordCnt=$wordCnt" +
        s", totalUserDirect=$totalUserDirect" +
        s", avgWordsPerTok=$avgWordsPerTok" +
        s", sparsity=$totalSparsity" +
        s", toks=$tokens" +
        "]"
}