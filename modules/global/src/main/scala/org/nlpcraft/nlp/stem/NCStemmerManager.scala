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

package org.nlpcraft.nlp.stem

// Ready for Scala 2.11
import scala.language.postfixOps
import scala.language.implicitConversions
import org.nlpcraft._
import resource._
import java.io._

import org.nlpcraft.NCLifecycle

/**
 * Implementation of Porter's stemming algorithm.
 *
 * See http://snowball.tartarus.org/algorithms/porter/stemmer.html
 * for description of the algorithm itself.
 *
 * Implementation is based on work by Evgeny Kotelnikov 'evgeny.kotelnikov@gmail.com'
 */
object NCStemmerManager extends NCLifecycle("Stem manager") {
    // Path for local stem cache (outside of Git root).
    private final val CACHE_PATH = G.homeFileName(".nlpcraft_stem_cache")

    // Stem cache auto-saving period.
    private val TIMER_PERIOD = 5000

    // Stem cache type alias.
    private type Cache = java.util.concurrent.ConcurrentHashMap[String, String]

    // Persistent local lemma cache.
    @volatile private var cache: Cache = _

    // Timer for periodic saving of stem cache into file.
    @volatile private var timer: java.util.Timer = _

    /**
     * Loads stem cache from the local file.
     */
    @throws[IOException]
    private def loadCache(): Cache = {
        logger.trace(s"Loading stem cache from: $CACHE_PATH")

        val file = new File(CACHE_PATH)

        def emptyCache = new Cache()

        if (file.exists())
            try
                managed(new ObjectInputStream(new FileInputStream(file))) acquireAndGet {
                    _.readObject().asInstanceOf[Cache]
                }
            catch {
                case _: IOException ⇒
                    logger.warn(s"Failed to read stem cache from (safely ignoring): $CACHE_PATH")

                    // Ignore if we fail to delete at this point.
                    file.delete()

                    emptyCache
            }
        else
            emptyCache
    }

    /**
     * This method should be periodically called to flush stem cache
     * to the disk - so that it can be reloaded on next startup.
     */
    def saveCache(): Unit = {
        ensureStarted()

        logger.trace(s"Saving stem cache to: $CACHE_PATH")

        if (cache != null)
            try
                for (out ← managed(new ObjectOutputStream(new FileOutputStream(new File(CACHE_PATH))))) out.writeObject(cache)
            catch {
                case _: IOException ⇒ logger.warn(s"Failed to save stem cache file: $CACHE_PATH")
            }
    }

    /**
     * Starts manager.
     */
    override def start(): NCLifecycle = {
        ensureStopped()
        
        cache = loadCache()

        // Task calling save method.
        val task = new java.util.TimerTask {
            private var lastSz = cache.size

            override def run(): Unit = {
                cache.size match {
                    case sz if sz != lastSz ⇒ lastSz = sz; saveCache()
                    case _ ⇒ ()
                }
            }
        }

        timer = new java.util.Timer("stem-cache-auto-saver")

        timer.schedule(task, TIMER_PERIOD, TIMER_PERIOD)

        logger.info(s"Stem cache ${TIMER_PERIOD / 1000}s auto-saver started.")
    
        super.start()
    }

    /**
     * Stops the stemmer.
     */
    override def stop(): Unit = {
        checkStopping()

        // To make sure that any data will not be lost when the system stopped.
        saveCache()

        if (timer != null)
            timer.cancel()

        timer = null

        cache = null

        super.stop()
    }

    /**
     * Gets a stem for given word.
     * This function wrapped implementation to skip stemmatize 'short' words.
     *
     * @param word A word to get stem for.
     */
    def stem(word: String): String = {
        ensureStarted()

        if (word.length <= 2) word.toLowerCase else stem0(word)
    }
    
    /**
      * Stemmatizes given word or a sequence of words.
      *
      * @param s Word or a sequence of words to stemmatize.
      * @return
      */
    def stems(s: String): String = {
        s.indexOf(' ') match {
            case -1 ⇒ stem(s)
            case _ ⇒ G.tokenizeSpace(s).map(_.trim).filter(_.nonEmpty).map(stem).mkString(" ")
        }
    }

    /**
     * Gets a stem for given word.
     *
     * @param word A word to get stem for.
     */
    private def stem0(word: String): String = {
        cache.get(word) match {
            case s: String ⇒ s
            case null ⇒
                // Deal with plurals and past participles.
                var stem = new Word(word).replace(
                    "sses" → "ss",
                    "ies" → "i",
                    "ss" → "ss",
                    "s" → ""
                )

                if ((stem matchedBy ((~v ~) + "ed")) || (stem matchedBy ((~v ~) + "ing")))
                    stem = stem.
                        replace(~v ~)(
                            "ed" → "",
                            "ing" → ""
                        ).
                        replace(
                            "at" → "ate",
                            "bl" → "ble",
                            "iz" → "ize",
                            (~d and not(~L or ~S or ~Z)) → singleLetter,
                            (m == 1 and ~o) → "e"
                        )
                else
                    stem = stem.replace(
                        ((m > 0) + "eed") → "ee"
                    )

                stem = stem.
                    replace(
                        ((~v ~) + "y") → "i"
                    ).
                    replace(m > 0)(
                        "ational" → "ate",
                        "tional" → "tion",
                        "enci" → "ence",
                        "anci" → "ance",
                        "izer" → "ize",
                        "abli" → "able",
                        "alli" → "al",
                        "entli" → "ent",
                        "eli" → "e",
                        "ousli" → "ous",
                        "ization" → "ize",
                        "ation" → "ate",
                        "ator" → "ate",
                        "alism" → "al",
                        "iveness" → "ive",
                        "fulness" → "ful",
                        "ousness" → "ous",
                        "aliti" → "al",
                        "iviti" → "ive",
                        "biliti" → "ble"
                    ).
                    replace(m > 0)(
                        "icate" → "ic",
                        "ative" → "",
                        "alize" → "al",
                        "iciti" → "ic",
                        "ical" → "ic",
                        "ful" → "", "ness" → ""
                    ).
                    replace(m > 1)(
                        "al" → "",
                        "ance" → "",
                        "ence" → "",
                        "er" → "",
                        "ic" → "",
                        "able" → "",
                        "ible" → "",
                        "ant" → "",
                        "ement" → "",
                        "ment" → "",
                        "ent" → "",
                        ((~S or ~T) + "ion") → "",
                        "ou" → "",
                        "ism" → "",
                        "ate" → "",
                        "iti" → "",
                        "ous" → "",
                        "ive" → "",
                        "ize" → ""
                    )

                // Tide up a little bit.
                stem = stem replace(((m > 1) + "e") → "", ((m == 1 and not(~o)) + "e") → "")
                stem = stem replace ((m > 1 and ~d and ~L) → singleLetter)

                val s = stem.toString

                cache.put(word, s)

                s
        }
    }

    // Pattern that is matched against the lemma.
    private case class Pattern(cond: Condition, sfx: String)

    // Condition, that is checked against the beginning of the lemma.
    private case class Condition(f: Word ⇒ Boolean) {
        def + = Pattern(this, _: String)
        def unary_~ : Condition = this
        def ~ : Condition = this
        def and(condition: Condition) = Condition((word) ⇒ f(word) && condition.f(word))
        def or(condition: Condition) = Condition((word) ⇒ f(word) || condition.f(word))
    }

    private final val EMPTY_COND = Condition(_ ⇒ true)

    private def not: Condition ⇒ Condition = {
        case Condition(f) ⇒ Condition(!f(_))
    }

    private val S = Condition(_ endsWith "s")
    private val Z = Condition(_ endsWith "z")
    private val L = Condition(_ endsWith "l")
    private val T = Condition(_ endsWith "t")
    private val d = Condition(_.endsWithCC)
    private val o = Condition(_.endsWithCVC)
    private val v = Condition(_.containsVowels)

    private object m {
        def >(measure: Int) = Condition(_.measure > measure)
        def ==(measure: Int) = Condition(_.measure == measure)
    }

    private case class StemBuilder(build: Word ⇒ Word)

    private def suffixStemBuilder(sfx: String) = StemBuilder(_ + sfx)

    private val singleLetter = StemBuilder(_ trimSuffix 1)

    private class Word(s: String) {
        private val w = s.toLowerCase

        def trimSuffix(sfxLen: Int) = new Word(w substring(0, w.length - sfxLen))

        def endsWith: (String) ⇒ Boolean = w endsWith

        def +(sfx: String) = new Word(w + sfx)

        def satisfies: (Condition) ⇒ Boolean = (_: Condition).f(this)

        def hasConsonantAt(pos: Int): Boolean =
            (w.indices contains pos) && (w(pos) match {
                case 'a' | 'e' | 'i' | 'o' | 'u' ⇒ false
                case 'y' if hasConsonantAt(pos + 1) ⇒ false
                case _ ⇒ true
            })

        def hasVowelAt: (Int) ⇒ Boolean = !hasConsonantAt(_: Int)

        def containsVowels: Boolean = w.indices exists hasVowelAt

        def endsWithCC: Boolean =
            (w.length > 1) &&
                (w(w.length - 1) == w(w.length - 2)) &&
                hasConsonantAt(w.length - 1)

        def endsWithCVC: Boolean =
            (w.length > 2) &&
                hasConsonantAt(w.length - 1) &&
                hasVowelAt(w.length - 2) &&
                hasConsonantAt(w.length - 3) &&
                !(Set('w', 'x', 'y') contains w(w.length - 2))


        def measure: Int = w.indices.count(pos ⇒ hasVowelAt(pos) && hasConsonantAt(pos + 1))

        def matchedBy: Pattern ⇒ Boolean = {
            case Pattern(cond, sfx) ⇒ endsWith(sfx) && (trimSuffix(sfx.length) satisfies cond)
        }

        def replace(replaces: (Pattern, StemBuilder)*): Word = {
            for ((ptrn, builder) ← replaces if matchedBy(ptrn))
                return builder build trimSuffix(ptrn.sfx.length)

            this
        }

        def replace(cmnCond: Condition)(replaces: (Pattern, StemBuilder)*): Word =
            replace(replaces map {
                case (Pattern(cond, sfx), builder) ⇒ (Pattern(cmnCond and cond, sfx), builder)
            }: _*)

        override def toString: String = w
    }

    // Implicits.
    private implicit def c1[P, SB](r: (P, SB))(implicit ev1: P ⇒ Pattern, ev2: SB ⇒ StemBuilder): (Pattern, StemBuilder) = (r._1, r._2)
    private implicit def c2: String ⇒ Pattern = Pattern(EMPTY_COND, _)
    private implicit def c3: Condition ⇒ Pattern = Pattern(_, "")
    private implicit def c4: String ⇒ StemBuilder = suffixStemBuilder
}
