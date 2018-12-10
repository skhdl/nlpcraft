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

package org.nlpcraft2.nlp.enrichers.date.tools

import org.nlpcraft._
import org.nlpcraft.ignite.NCIgniteRunner
import org.nlpcraft.makro.NCMacroParser
import org.nlpcraft.nlp.numeric.NCNumericsGenerator
import org.nlpcraft.util.NCGlobals
import org.nlpcraft2.nlp.NCCoreNlp

/**
  * Pre-built time ranges generator.
  */
object NCTimeGenerator extends App {
    private final lazy val LABELS_HOURS = mkMacro("h", "hr", "h.", "hr.", "hours", "o'clock")
    private final lazy val LABELS_MINS = mkMacro("m", "min", "mins", "m.", "min.", "mins.", "minute", "minutes")

    private final lazy val AM = mkMacro("am", "a.m.")
    private final lazy val PM = mkMacro("pm", "p.m.")
    private final lazy val SPACE_OPT = mkMacro(" ", "*")

    private final val PARSER = NCMacroParser()

    private def mkMacro(seq: String*): String = s"{${seq.mkString("|")}}"
    private def lpad(i: Int): String = if (i < 10) s"0$i" else s"$i"
    private def mkLpad(i: Int): Seq[String] = if (i < 10) Seq(lpad(i), s"$i") else Seq(s"$i")
    private def combine[T](hSeq: Seq[T], mSeq: Seq[T]): Seq[(T, T)] = for (h ← hSeq; m ← mSeq) yield (h, m)

    /**
      * Generates predefined times configuration with stematized keys.
      *
      * @param f Output file path.
      * @param times Times.
      * @param save Save function.
      */
    private def generate(f: String, times: Seq[(String, (Int, Int))], save: (String, Int, Int) ⇒ String): Unit = {
        println(s"Started: $f")

        println(s"Generated combinations: ${times.size}")

        // Batch request.
        val keysStems =  times.map(_._1).grouped(10000).toSeq.flatMap(p ⇒ NCCoreNlp.stemmatizeSeq(p))

        require(keysStems.lengthCompare(times.size) == 0)

        val timesStems = keysStems.zip(times.map(_._2))

        val timeStemsMap = timesStems.groupBy(_._1).map(p ⇒ p._1 → p._2.map(_._2).distinct)

        val dups = timeStemsMap.filter(_._2.lengthCompare(1) > 0)

        if (dups.nonEmpty) {
            println(s"Generated combinations duplicated.")

            dups.foreach(println)

            require(false)
        }

        println(s"Saved stemmed combinations: ${timeStemsMap.size}")

        G.mkTextFile(
            f,
            timeStemsMap.
                map { case (s, seq) ⇒
                    require(seq.lengthCompare(1) == 0)

                    s → seq.head
                }.
                toSeq.
                sortBy { case (s, (h, m)) ⇒ (h, m, s) }.
                map { case (s, (h, m)) ⇒ save(s, h, m) },
            sort = false
        )

        G.gzipPath(f)

        println(s"Finished: $f")
    }

    /**
      * Generates hours and minutes combinations.
      */
    private def generateTimesPast4Now(): Seq[(String, (Int, Int))] = {
        val hSeq = (1 to 24).map(i ⇒ i) ++ Seq(36, 48, 72)
        val mSeq = (1 to 120).map(i ⇒ i)

        val forPrefix = mkMacro("for ", "*")
        val last = mkMacro("last", "previous")

        hSeq.flatMap(h ⇒ PARSER.expand(s"$forPrefix$last $h$SPACE_OPT$LABELS_HOURS").map(l ⇒ l → (h, 0))) ++
        mSeq.flatMap(m ⇒ PARSER.expand(s"$forPrefix$last $m$SPACE_OPT$LABELS_MINS").map(l ⇒ l → (0, m)))
    }

    /**
      * Generates times combinations.
      */
    private def generateTimesParts(): Seq[(String, (Int, Int))] = {
        val hours = for (i ← 0 until 24) yield i
        val mins = for (i ← 0 until 60) yield i

        combine(hours, mins).flatMap { case (h24, m) ⇒
            // Standard.
            var x =
                combine(mkLpad(h24), mkLpad(m)).flatMap { case (h24s, ms) ⇒
                    PARSER.expand(s"$h24s$SPACE_OPT$LABELS_HOURS $ms$SPACE_OPT$LABELS_MINS") ++
                        PARSER.expand(s"$h24s:$ms")
                }

            if (m == 0)
                x ++= mkLpad(h24).flatMap(h24s ⇒ PARSER.expand(s"$h24s$SPACE_OPT$LABELS_HOURS"))

            // Numerics as words.
            val hW = NCNumericsGenerator.toWords(h24)
            val mW = NCNumericsGenerator.toWords(m)

            x ++= PARSER.expand(s"$hW $LABELS_HOURS $mW $LABELS_MINS")

            if (m == 0)
                x ++= PARSER.expand(s"$hW $LABELS_HOURS")

            // AM / PM.
            if (h24 <= 12) {
                x ++= combine(mkLpad(h24), mkLpad(m)).flatMap { case (h24s, ms) ⇒
                    PARSER.expand(s"$h24s$SPACE_OPT$AM $ms$SPACE_OPT$LABELS_MINS") ++
                    PARSER.expand(s"$h24s$SPACE_OPT$AM $LABELS_HOURS $ms$SPACE_OPT$LABELS_MINS") ++
                    PARSER.expand(s"$h24s$SPACE_OPT$AM:$ms")
                }

                if (m == 0)
                    x ++= mkLpad(h24).flatMap(h24s ⇒
                        PARSER.expand(s"$h24s$SPACE_OPT$AM") ++
                        PARSER.expand(s"$h24s$SPACE_OPT$AM $LABELS_HOURS")
                    )
            }
            else {
                x ++= combine(mkLpad(h24 - 12), mkLpad(m)).flatMap { case (h12s, ms) ⇒
                    PARSER.expand(s"$h12s$SPACE_OPT$PM $ms$SPACE_OPT$LABELS_MINS") ++
                    PARSER.expand(s"$h12s$SPACE_OPT$PM $LABELS_HOURS $ms$SPACE_OPT$LABELS_MINS") ++
                    PARSER.expand(s"$h12s$SPACE_OPT$PM:$ms")
                }

                if (m == 0)
                    x ++= mkLpad(h24 - 12).flatMap(h12s ⇒
                        PARSER.expand(s"$h12s$SPACE_OPT$PM") ++
                        PARSER.expand(s"$h12s$SPACE_OPT$PM $LABELS_HOURS")
                    )
            }

            x.map(s ⇒ s → (h24, m))
        }
    }

    NCIgniteRunner.runWith("ignite/noop-srv.xml", {
        NCCoreNlp.start()

        try {
            val p = NCGlobals.mkPath(s"modules/core/src/main/resources/time")

            generate(
                s"$p/times.txt",
                generateTimesParts(),
                (s: String, h: Int, m: Int) ⇒ s"$s | ${lpad(h)}:${lpad(m)}"
            )

            generate(
                s"$p/times_periods.txt",
                generateTimesPast4Now(),
                (s: String, h: Int, m: Int) ⇒ s"$s | ${lpad(h)}:${lpad(m)}-now"
            )
        }
        finally {
            NCCoreNlp.stop()
        }
    })
}