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

import org.nlpcraft.common.{NCE, NCLifecycle}

import scala.language.{implicitConversions, postfixOps}
import scala.reflect.runtime.universe._

/**
 *  NLP core manager.
 */
object NCNlpCoreManager extends NCLifecycle(s"Core NLP manager") {
    @volatile private var engine: String = _
    @volatile private var tokenizer: NCNlpTokenizer = _

    /**
      * Implementation of Porter's stemming algorithm.
      *
      * See http://snowball.tartarus.org/algorithms/porter/stemmer.html
      * for description of the algorithm itself.
      *
      * Implementation is based on work by Evgeny Kotelnikov 'evgeny.kotelnikov@gmail.com'
      */
    object NCNlpPorterStemmer  {
        /**
          * Gets a stem for given word.
          * This function wrapped implementation to skip stemmatize 'short' words.
          *
          * @param word A word to get stem for.
          */
        def stem(word: String): String = if (word.length <= 2) word.toLowerCase else stem0(word)

        /**
          * Gets a stem for given word.
          *
          * @param word A word to get stem for.
          */
        private def stem0(word: String): String = {
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

            stem.toString
        }

        // Pattern that is matched against the lemma.
        private case class Pattern(cond: Condition, sfx: String)

        // Condition, that is checked against the beginning of the lemma.
        private case class Condition(f: Word ⇒ Boolean) {
            def + = Pattern(this, _: String)
            def unary_~ : Condition = this
            def ~ : Condition = this
            def and(condition: Condition) = Condition(word ⇒ f(word) && condition.f(word))
            def or(condition: Condition) = Condition(word ⇒ f(word) || condition.f(word))
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

            def endsWith: String ⇒ Boolean = w endsWith

            def +(sfx: String) = new Word(w + sfx)

            def satisfies: Condition ⇒ Boolean = (_: Condition).f(this)

            def hasConsonantAt(pos: Int): Boolean =
                (w.indices contains pos) && (w(pos) match {
                    case 'a' | 'e' | 'i' | 'o' | 'u' ⇒ false
                    case 'y' if hasConsonantAt(pos + 1) ⇒ false
                    case _ ⇒ true
                })

            def hasVowelAt: Int ⇒ Boolean = !hasConsonantAt(_: Int)

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

    /**
      *
      * @param engine
      */
    def setEngine(engine: String): Unit = this.engine = engine

    /**
      *
      * @return
      */
    def getEngine: String = engine

    override def start(): NCLifecycle = {
        require(engine != null)

        val mirror = runtimeMirror(getClass.getClassLoader)

        def mkInstance(name: String): NCNlpTokenizer =
            try
                mirror.reflectModule(mirror.staticModule(name)).instance.asInstanceOf[NCNlpTokenizer]
            catch {
                case e: Throwable ⇒ throw new NCE(s"Error initializing class: $name", e)
            }

        tokenizer =
            engine match {
                case "stanford" ⇒ mkInstance("org.nlpcraft.common.nlp.core.stanford.NCStanfordTokenizer")
                // NCOPenNlpTokenizer added via reflection just for symmetry.
                case "opennlp" ⇒ mkInstance("org.nlpcraft.common.nlp.core.opennlp.NCOPenNlpTokenizer")

                case _ ⇒ throw new AssertionError(s"Unexpected engine: $engine")
            }

        logger.info(s"NLP engined configured: $engine")

        tokenizer.start()

        super.start()
    }

    /**
      * Stems given word or a sequence of words which will be tokenized before.
      *
      * @param words One or more words to stemmatize.
      * @return Sentence with stemmed words.
      */
    def stem(words: String): String = {
        ensureStarted()

        val seq = tokenizer.tokenize(words).map(p ⇒ p → NCNlpPorterStemmer.stem(p.token))

        seq.zipWithIndex.map { case ((tok, stem), idx) ⇒
            idx match {
                case 0 ⇒ stem
                // Suppose there aren't multiple spaces.
                case _ ⇒ if (seq(idx - 1)._1.to + 1 < tok.from) s" $stem" else stem
            }
        }.mkString("")
    }

    /**
      * Stems given word.
      *
      * @param word Word to stemmatize.
      * @return Stemmed word.
      */
    def stemWord(word: String): String = {
        ensureStarted()

        NCNlpPorterStemmer.stem(word)
    }

    /**
      * Tokenizes given sentence.
      *
      * @param sen Sentence text.
      * @return Tokens.
      */
    def tokenize(sen: String): Seq[NCNlpCoreToken] = {
        ensureStarted()

        tokenizer.tokenize(sen)
    }
}