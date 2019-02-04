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

package org.nlpcraft.nlp.preproc

import org.nlpcraft._
import org.nlpcraft.nlp.spell.NCSpellCheckManager

import scala.collection._

/**
  * Centralized pre-processor for raw text coming from user.
  */
object NCPreProcessManager extends NCLifecycle("Pre-process manager") {
    // List of unambiguous contractions.
    private final val CONTRACTIONS: Map[String, Seq[String]] =
        Map[String, String](
            "aren't" → "are not",
            "can't" → "cannot",
            "aren't" → "are not",
            "can't" → "cannot",
            "could've" → "could have",
            "couldn't" → "could not",
            "didn't" → "did not",
            "doesn't" → "does not",
            "don't" → "do not",
            "hadn't" → "had not",
            "hasn't" → "has not",
            "haven't" → "have not",
            "he'll" → "he will",
            "how'd" → "how did",
            "how'll" → "how will",
            "i'll" → "I will",
            "i'm" → "I am",
            "i've" → "I have",
            "isn't" → "is not",
            "it'll" → "it will",
            "let's" → "let us",
            "ma'am" → "madam",
            "might've" → "might have",
            "must've" → "must have",
            "needn't" → "need not",
            "o'clock" → "of the clock",
            "shan't" → "shall not",
            "she'll" → "she will",
            "should've" → "should have",
            "shouldn't" → "should not",
            "they'll" → "they will",
            "they're" → "they are",
            "they've" → "they have",
            "wasn't" → "was not",
            "we'll" → "we will",
            "we're" → "we are",
            "we've" → "we have",
            "weren't" → "were not",
            "what'll" → "what will",
            "what're" → "what are",
            "where'd" → "where did",
            "where've" → "where have",
            "who'll" → "who will",
            "won't" → "will not",
            "would've" → "would have",
            "wouldn't" → "would not",
            "y'all" → "you are all",
            "you'll" → "you will",
            "you're" → "you are",
            "you've" → "you have"
        ).map(p ⇒ p._1 → p._2.split(' ').toSeq)
    
    /**
      * Replaces contractions. Note that this method can change text case. It forces lower case on replacements.
      *
      * @param sen Input sentence.
      * @return
      */
    private def replaceContractions(sen: Seq[String]): Seq[String] =
        sen.flatMap(s ⇒ {
            CONTRACTIONS.get(s.toLowerCase) match {
                case Some(seq) ⇒ seq
                case None ⇒ Seq(s)
            }
        })
    
    /**
      *
      * @param sen Input sentence.
      * @param spellCheck Spell check flag.
      * @return
      */
    private def collect(sen: Seq[String], spellCheck: Boolean): String =
        if (spellCheck)
            sen.map(NCSpellCheckManager.check).map(_.trim).filter(!_.isEmpty).mkString(" ")
        else
            sen.map(_.trim).filter(!_.isEmpty).mkString(" ")
    
    /**
      * Performs all pre-processing and normalizes the given input raw text.
      *
      * @param rawTxt Raw text to normalize.
      * @param spellCheck Using spell checking flag.
      * @return Normalized, pre-processed text.
      */
    def normalize(rawTxt: String, spellCheck: Boolean = true): String = {
        ensureStarted()
        
        // Fix Apple/MacOS smart quotes & dashes.
        val s0 = rawTxt.trim().
            replace('‘', '\'').
            replace('’', '\'').
            replace('”', '"').
            replace('”', '"').
            replace('—', '-')
        
        collect(
            replaceContractions(
                collect(
                    s0.split(' ').toSeq,
                    spellCheck
                )
                    .split(' ').toSeq
            
            ),
            spellCheck = false
        )
    }
}
