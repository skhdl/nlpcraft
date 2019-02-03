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

package org.nlpcraft.nlp.spell

import org.nlpcraft._
import org.nlpcraft.json.NCJson

import scala.collection._

/**
  * Basic dictionary-based spell checker.
  */
object NCSpellCheckManager extends NCLifecycle("Spellcheck manager") {
    case class Record(correct: String, misspellings: Seq[String])
    
    private val dict: Map[String, String] = (
        for (rec ← NCJson.extractResource[List[Record]]("spell/dictionary.json", ignoreCase = true)) yield {
            for (v ← rec.misspellings) yield v → rec.correct
        }
        )
        .flatten.toMap
    
    private def isWordUpper(s: String): Boolean = s.forall(_.isUpper)
    private def isHeadUpper(s: String): Boolean = s.head.isUpper
    private def split(s: String): Seq[String] = s.split(" ").filter(!_.isEmpty)
    private def processCase(s: String, sample: String): String =
        if (isWordUpper(sample))
            s.toUpperCase
        else if (isHeadUpper(sample))
            s.capitalize
        else
            s // Full lower case by default.
    
    /**
      * Gets correctly spelled word for a given one (if correction exists in predefined dictionary).
      * Returns the same word if it's correctly spelled or correction isn't available.
      *
      * NOTE: this method will retain the case of the 1st letter.
      *
      * @param in Word to check.
      */
    def check(in: String): String = {
        ensureStarted()
        
        dict.get(in.toLowerCase) match {
            case None ⇒ in
            case Some(out) ⇒
                val inSeq = split(in)
                val outSeq = split(out)
                
                if (inSeq.lengthCompare(outSeq.size) == 0)
                    outSeq.zip(inSeq).map(p ⇒ processCase(p._1, p._2)).mkString(" ")
                else
                    processCase(out, in)
        }
    }
}
