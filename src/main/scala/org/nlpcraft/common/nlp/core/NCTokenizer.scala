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

package org.nlpcraft.common.nlp.core

import scala.collection.mutable.ArrayBuffer

/**
  * Tokenizer
  */
object NCTokenizer {
    /**
      * Tokenizes model element ID or synonym value name that act as **implicit**
      * synonyms. It tries to mimic Stanford's PTBTokenizer which is unavailable on the probe itself.
      *
      * @param s Either model element ID or synonym value name.
      * @param group Whether or not to group non-letter or digit characters into one token.
      */
    def tokenize(s: String, group: Boolean): Seq[NCNlpCoreToken] = {
        val len = s.length()
        val tokBuf = new StringBuilder()
        var i = 0
        val toks = ArrayBuffer.empty[(String, Int)]
        var f = false

        def addToken(): Unit = {
            val x = tokBuf.toString.trim

            if (x.nonEmpty)
                toks += x → (i - 1)

            tokBuf.setLength(0)
        }

        while (i < len) {
            val ch = s.charAt(i)

            if (!ch.isLetterOrDigit) {
                if ((group && !f) || !group)
                    addToken()

                f = true
            }
            else {
                if (f)
                    addToken()

                f = false
            }

            tokBuf += ch

            i += 1
        }

        addToken()

        var isPrevTick = false

        // Fix special case of handling "'s" by CoreNLP.
        val x = for ((tok, idx) ← toks.filter(_._1.nonEmpty)) yield {
            if (tok.toLowerCase == "s" && isPrevTick) {
                isPrevTick = false
                Some("'s" → idx)
            }
            else if (tok == "'") {
                isPrevTick = true
                None
            }
            else {
                isPrevTick = false

                Some(tok → idx)
            }
        }

        x.flatten.map { case (tok, idx) ⇒ {
            val len = tok.length

            NCNlpCoreToken(tok, idx - len + 1, idx, len)
        }}
    }

    /**
      *
      * @param s
      * @return
      */
    def tokenize(s: String): Seq[NCNlpCoreToken] = tokenize(s, false)
}
