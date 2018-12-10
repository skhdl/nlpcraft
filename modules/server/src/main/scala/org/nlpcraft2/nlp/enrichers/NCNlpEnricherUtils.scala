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

package org.nlpcraft2.nlp.enrichers

import org.nlpcraft.nlp.NCNlpSentenceToken

import scala.collection.mutable

/**
  * Common utility functionality for enrichers.
  */
object NCNlpEnricherUtils {
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
    
    // Skips spaces after left and before right brackets.
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
