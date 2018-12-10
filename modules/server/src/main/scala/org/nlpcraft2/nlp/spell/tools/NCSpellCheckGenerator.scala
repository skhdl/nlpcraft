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

package org.nlpcraft2.nlp.spell.tools

import java.io.{File, PrintStream}

import net.liftweb.json.Extraction._
import net.liftweb.json._
import org.nlpcraft._
import resource._

import scala.collection._

/**
 * Generator for spell checker from Wiki.
 */
object NCSpellCheckGenerator extends App {
    private val VARIANT_OF = "variant of"

    private val DIR_IN: String = s"modules/core/src/main/scala/${G.toPath(this)}"
    private val DIR_OUT: String = s"modules/core/src/main/resources/spell"

    private val SPEC_CASES: Map[String, String] =
        G.readPath(s"$DIR_IN/exceptions.txt", "UTF8").map(_.trim).filter(!_.isEmpty).map(line ⇒ {
            val m = line.takeWhile(_ != '(')
            val w = line.drop(m.length + 1).takeWhile(_ != ')')

            m.trim → w.trim
        }).toMap

    private def wordsCount(s: String): Int = s.split(" ").map(_.trim).count(!_.isEmpty)

    private def read(f: File): Seq[(String, String)] =
        G.readFile(f, "UTF8").map(_.trim).filter(!_.isEmpty).flatMap(line ⇒ {
            val miss = line.takeWhile(_ != '(').trim

            SPEC_CASES.get(miss) match {
                case Some(word) ⇒ Some(miss → word)
                case None ⇒
                    val missWords = wordsCount(miss)
                    val corrFull = line.drop(miss.length).trim

                    if (corrFull.count(_ == '(') > 1)
                        println(s"Content of second bracket skipped, line: $line.")

                    val corr = corrFull.drop(1).takeWhile(_ != ')')

                    // ", " is used to avoid separating lines like: 5,000m (5,000 m, 5,000 meters).
                    corr.split(", ").map(_.trim).filter(!_.isEmpty).toSeq.flatMap(p ⇒ {
                        // donut (acceptable variant of doughnut); dramatise (variant of dramatize)
                        val idx = p.indexOf(VARIANT_OF)

                        // Filter texts like: planned [plan]; can not, can't, cannot; cant may be correct.
                        val word =
                            (if (idx >= 0)
                                p.takeRight(p.length - idx - VARIANT_OF.length)
                            else
                                p
                            ).takeWhile(ch ⇒ ch != ';' && ch != '[').trim

                        // To skip like: aka (a.k.a., AKA); a.k.a (a.k.a.)
                        if (miss.toLowerCase == word.toLowerCase)
                            Seq.empty[(String, String)]
                        else if (Math.abs(missWords - wordsCount(word)) > 1 || word.contains("?")) {
                            println(s"Some content skipped, line: $line\n\tvalue: $word.")

                            Seq.empty[(String, String)]
                        }
                        else {
                            def split(s: String): Seq[String] = s.split(", ").map(_.trim)

                            val buf = mutable.Buffer.empty[(String, String)]

                            for (m ← split(miss); w ← split(word))
                                buf += m → w

                            buf
                        }
                    })
            }
        }).toSeq

    private def process() {
        val files = new File(s"$DIR_IN/wiki").listFiles().filter(_.getName.endsWith(".txt")).map(f ⇒ f).toSeq

        val data = files.flatMap(read)

        case class Holder(correct: String, misspellings: Seq[String])

        // 1. Misspelling 2. Correct.
        val hs = data.map(_._2).distinct.map(v ⇒
            Holder(v, data.filter(_._2 == v).map(_._1).sortBy(p ⇒ p))
        ).sortBy(_.correct)

        // Required for Lift JSON processing.
        implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats

        val out = "dictionary.json"

        managed(new PrintStream(new File(s"$DIR_OUT/$out"))) acquireAndGet { ps ⇒
            ps.println(pretty(render(decompose(hs))))
        }

        println(s"Files generated OK: $out.")
    }

    process()
}