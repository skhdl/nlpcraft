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

package org.nlpcraft.nlp.enrichers.geo

import org.nlpcraft._
import org.nlpcraft.geo._
import org.nlpcraft.json.NCJson
import org.nlpcraft.nlp._
import org.nlpcraft.nlp.enrichers.NCNlpEnricher
import org.nlpcraft.geo.NCGeoLocationKind._
import org.nlpcraft.nlp.pos.NCPennTreebank
import org.nlpcraft.nlp.wordnet.NCWordNetManager

import scala.collection._

/**
 * Geo-location enricher.
 */
object NCGeoEnricher extends NCNlpEnricher("Geo enricher") {
    // US states that conflict with commonly used English words (lower case set).
    private final val US_CONFLICT_STATES = Set("hi", "or", "in", "ok", "as", "me")

    // Words used to express one location is inside another one.
    // Example is San Francisco of USA or Paris in France.
    private final val IN_WORDS =
        immutable.HashSet(",", "in", "within", "inside", "of", "inside of", "within of", "wherein")

    // USA large cities configuration file.
    private final val US_TOP_PATH = "geo/us_top.json"

    // World large cities configuration file.
    private final val WORLD_TOP_PATH = "geo/world_top.json"

    // Common word exceptions configuration folder.
    private final val EXCEPTIONS_PATH = "geo/exceptions"

    @throws[NCE]
    private[geo] final val LOCATIONS: Map[String, Set[NCGeoEntry]] = NCGeoManager.getModel.synonyms

    // GEO names matched with common english words and user defined exception GEO names.
    // Note that 'ignore case' parameter set as false because DLGeoLocationKind definition (CITY ect)
    @throws[NCE]
    // TODO: refactor... incomprehensible!
    private final val COMMONS: Map[NCGeoLocationKind, Set[String]]  =
        G.getFilesResources(EXCEPTIONS_PATH).
            flatMap(f ⇒
                NCJson.extractResource[immutable.Map[String, immutable.Set[String]]](f, ignoreCase = false).
                    map(p ⇒ NCGeoLocationKind.withName(p._1.toUpperCase) → p._2)
            ).groupBy(_._1).map(p ⇒ p._1 → p._2.flatMap(_._2).toSet).map(p ⇒ p._1 → p._2.map(_.toLowerCase))

    // JSON extractor for largest cities.
    case class TopCity(name: String, region: String)

    private def glue(s: String*): String = s.map(_.toLowerCase).mkString("|")

    private final val TOP_USA: Set[String] =
        NCJson.extractResource[List[TopCity]](US_TOP_PATH, ignoreCase = true).
            map(city ⇒ glue(city.name, city.region)).toSet

    private final val TOP_WORLD: Set[String] =
        NCJson.extractResource[List[TopCity]](WORLD_TOP_PATH, ignoreCase = true).
            map(city ⇒ glue(city.name, city.region)).toSet

    private def isConflictName(name: String): Boolean =
        US_CONFLICT_STATES.contains(name.toLowerCase) && name.exists(_.isLower)

    @throws[NCE]
    override def enrich(ns: NCNlpSentence) {
        // This stage must not be 1st enrichment stage.
        assume(ns.nonEmpty)

        for (toks ← ns.tokenMix(withQuoted = true)) {
            val len = toks.map(_.words).sum

            def mkNote(kind: NCGeoLocationKind, seq: (String, Any)*): NCNlpSentenceNote =
                NCNlpSentenceNote(toks.map(_.index), "nlp:geo", Seq("kind" → kind.toString, "length" → len) ++ seq :_*)

            def make(e: NCGeoEntry): NCNlpSentenceNote =
                e match {
                    case x: NCGeoMetro ⇒
                        mkNote(
                            METRO,
                            "metro" → x.name
                        )

                    case x: NCGeoContinent ⇒
                        mkNote(
                            CONTINENT,
                            "continent" → x.name
                        )

                    case x: NCGeoSubContinent ⇒
                        mkNote(
                            SUBCONTINENT,
                            "continent" → x.continent.name,
                            "subcontinent" → x.name
                        )

                    case x: NCGeoCountry ⇒
                        mkNote(
                            COUNTRY,
                            "continent" → x.subContinent.continent.name,
                            "subcontinent" → x.subContinent.name,
                            "country" → x.name
                        )

                    case x: NCGeoRegion ⇒
                        mkNote(
                            REGION,
                            "continent" → x.country.subContinent.continent.name,
                            "subcontinent" → x.country.subContinent.name,
                            "country" → x.country.name,
                            "region" → x.name
                        )

                    case x: NCGeoCity ⇒
                        mkNote(
                            CITY,
                            "continent" → x.region.country.subContinent.continent.name,
                            "subcontinent" → x.region.country.subContinent.name,
                            "country" → x.region.country.name,
                            "region" → x.region.name,
                            "city" → x.name,
                            "latitude" → x.latitude,
                            "longitude" → x.longitude
                        )
                    case _ ⇒ throw new AssertionError(s"Unexpected data: $e")
                }

            def addAll(locs: Set[NCGeoEntry]): Unit =
                for (loc ← locs) {
                    val note = make(loc)

                    toks.foreach(t ⇒ t.add(note))

                    // Other types(JJ etc) and quoted word are not re-marked.
                    toks.filter(t ⇒ !NCPennTreebank.NOUNS_POS.contains(t.pos) && t.pos != "FW").
                        foreach(t ⇒ t.getNlpNote += "pos" → NCPennTreebank.SYNTH_POS)
                }

            LOCATIONS.get(toks.map(_.normText).mkString(" ")) match {
                case Some(locs) ⇒
                    // If multiple token match - add it.
                    if (toks.length > 1)
                        addAll(locs)
                    else {
                        // Only one token - toks.length == 1
                        val t = toks.head

                        def isLocation: Boolean =
                            t.ne match {
                                case Some(ne) ⇒ ne == "LOCATION"
                                case None ⇒ false
                            }

                        // If LOCATION or noun - add it.
                        if (NCPennTreebank.NOUNS_POS.contains(t.pos) || isLocation)
                            addAll(locs)
                        // If US state - add it.
                        else
                            // For now - simply ignore abbreviations for US states that
                            // conflict with commonly used English words. User will have to
                            // use full names.
                            if (!isConflictName(t.origText)) {
                                def isTopCity(g: NCGeoCity): Boolean = {
                                    val name = glue(g.name, g.region.name)

                                    TOP_USA.contains(name) || TOP_WORLD.contains(name)
                                }

                                addAll(locs.collect {
                                    case g: NCGeoContinent ⇒ g
                                    case g: NCGeoSubContinent ⇒ g
                                    case g: NCGeoCountry ⇒ g
                                    case g: NCGeoMetro ⇒ g
                                    case g: NCGeoRegion if g.country.name == "united states" ⇒ g
                                    case g: NCGeoCity if isTopCity(g) ⇒ g
                                })
                            }
                         // In all other cases - ignore one-token match.
                    }
                case None ⇒
                    // Case sensitive synonyms.
                    LOCATIONS.get(toks.map(_.origText).mkString(" ")) match {
                        case Some(locs) ⇒ addAll(locs)
                        case None ⇒
                            // If there is no direct match try to convert JJs to NNs and re-check
                            // for a possible match, e.g. "american" ⇒ "america".
                            if (toks.size == 1) {
                                val tok = toks.head

                                if (NCPennTreebank.JJS_POS.contains(tok.pos)) {
                                    var endLoop = false

                                    for (noun ← NCWordNetManager.getNNsForJJ(tok.normText); if !endLoop) {
                                        def onResult(locs: Set[NCGeoEntry]): Unit = {
                                            addAll(locs)
                                            endLoop = true
                                        }

                                        LOCATIONS.get(noun) match {
                                            case Some(locs) ⇒ onResult(locs)
                                            case None ⇒
                                                LOCATIONS.get(noun.toLowerCase) match {
                                                    case Some(locs) ⇒ onResult(locs)
                                                    case None ⇒ // No-op.
                                                }
                                        }
                                    }
                                }
                            }
                    }
            }
        }

        collapse(ns)
    }

    private def getValue(note: NCNlpSentenceNote, key: String): String = note(key).asInstanceOf[String]
    private def getValueOpt(note: NCNlpSentenceNote, key: String): Option[String] = note.get(key) match {
        case Some(s) ⇒ Some(s.asInstanceOf[String])
        case None ⇒ None
    }

    private def getName(kind: NCGeoLocationKind, note: NCNlpSentenceNote): String =
        kind match {
            case METRO ⇒ getValue(note, "metro")
            case CONTINENT ⇒ getValue(note, "continent")
            case SUBCONTINENT ⇒ getValue(note, "subcontinent")
            case COUNTRY ⇒ getValue(note, "country")
            case REGION ⇒ getValue(note, "region")
            case CITY ⇒ getValue(note, "city")
        }

    private def getKind(note: NCNlpSentenceNote): NCGeoLocationKind = NCGeoLocationKind.withName(getValue(note, "kind"))

    private def isChild(note: NCNlpSentenceNote, parent: NCNlpSentenceNote): Boolean = {
        def same(n: String) = getValue(note, n) == getValue(parent, n)

        val nKind = getKind(note)
        val pKind = getKind(parent)

        if (nKind != pKind)
            nKind match {
                case CITY ⇒
                    pKind match {
                        case REGION ⇒ same("country") && same("region")
                        case COUNTRY ⇒ same("country")
                        case _ ⇒ false
                    }
                case REGION ⇒
                    pKind match {
                        case COUNTRY ⇒ same("country")
                        case SUBCONTINENT ⇒ same("subcontinent")
                        case CONTINENT ⇒ same("continent")
                        case _ ⇒ false
                    }
                case COUNTRY ⇒
                    pKind match {
                        case CONTINENT ⇒ same("continent")
                        case SUBCONTINENT ⇒ same("subcontinent")
                        case _ ⇒ false
                    }
                case CONTINENT ⇒ false
                case METRO ⇒ false
            }
        else
            false
    }

    private def note2String(n: NCNlpSentenceNote): String = {
        def x(name: String): String = n.getOrElse(name, "").asInstanceOf[String]

        val kind = x("kind")

        val content =
            Seq(
                x("continent"), x("subcontinent"), x("country"), x("region"), x("city"), x("metro")
            ).
            filter(!_.isEmpty).
            mkString("|")

        s"$kind $content"
    }

    @throws[NCE]
    private def collapse(ns: NCNlpSentence) {
        // Candidates for excluding.
        // GEO names matched with common words. (Single words only)
        val excls = new mutable.HashSet[NCNlpSentenceNote]() ++ ns.getNotes("nlp:geo").filter(note ⇒ {
            val kind = getKind(note)

            COMMONS.get(kind) match {
                // GEO is common word defined directly or via synonym.
                case Some(cs) ⇒
                    cs.contains(getName(kind, note)) ||
                    cs.contains(
                        ns.
                            filter(t ⇒ t.index >= note.tokenFrom && t.index <= note.tokenTo).
                            filter(!_.isStopword).
                            map(_.normText).
                            mkString(" ")
                    )
                case None ⇒ false
            }
        })

        // Also added tokens with very short GEO names (with length is 1)
        excls ++= ns.getNotes("nlp:geo").filter(note ⇒ getName(getKind(note), note).length == 1)

        def removeNote(n: NCNlpSentenceNote):Unit =
            ns.removeNote(n.id)

        // Check that city is inside country or region.
        // When true - remove larger location note and replace with
        // enlarged more detailed location note.
        def checkExtendNote(first: NCNlpSentenceNote, second: NCNlpSentenceNote, small: NCNlpSentenceNote, big: NCNlpSentenceNote) {
            if (isChild(small, big) || small == big) {
                logger.debug(s"Extending $small and swallow $big.")

                val note = small.clone(
                    first.tokenIndexes ++ second.tokenIndexes, first.wordIndexes ++ second.wordIndexes
                )

                removeNote(second)
                removeNote(first)

                // GEO names matched with common words shouldn't be excluded if they specified by valid GEO parents.
                excls -= first

                ns.
                    filter(t ⇒ t.index >= first.tokenFrom && t.index <= second.tokenTo && !t.isStopword).
                    foreach(_.add(note))
            }
        }

        // Finds two collapsible neighboring location entities, takes more detailed one,
        // removes less detailed one, and enlarges the remaining (more detailed) one to
        // "cover" the tokens originally occupied by both entities.
        def enlarge(withOverlap: Boolean) {
            val locs = ns.getNotes("nlp:geo")

            locs.foreach(p ⇒ {
                // Get holders after the end of this one immediately or separated by IN_WORDS strings.
                locs.filter(x ⇒
                    if (x.tokenFrom > p.tokenFrom) {
                        val strBetween = ns.
                            filter(t ⇒ t.index > p.tokenTo && t.index < x.tokenFrom).
                            map(_.normText).mkString(" ")

                        ((x.tokenFrom <= p.tokenTo) && (x.tokenTo > p.tokenTo) &&
                            (x.tokenFrom != p.tokenFrom) && withOverlap) ||
                            (x.tokenFrom == p.tokenTo + 1) ||
                            ((x.tokenFrom > p.tokenTo + 1) && IN_WORDS.contains(strBetween))
                    }
                    else
                        false
                ).foreach(z ⇒ {
                    if (getKind(p) > getKind(z))
                        checkExtendNote(p, z, p, z) // 'a' is smaller and more detailed.
                    else
                        checkExtendNote(p, z, z, p)
                })
            })
        }

        // Do two iterations for cases like San Francisco, CA USA.
        // Second pass glues together results from the first
        // pass (San Francisco CA) and (CA USA).
        enlarge(false)
        enlarge(true)

        excls.foreach(e ⇒ removeNote(e))

        // Calculate a weight to rank locations.
        // ------------------------------------
        // Most important is note length. A less important is a kind of a note.
        // In US cities prevail over states, in the rest of the world - country over region
        // and over city. Across cities more important are world top cities and than US top
        // cities.

        def calcWeight(note: NCNlpSentenceNote): Seq[Int] = {
            def get(name: String): String = getValue(note, name)
            def getOpt(name: String): Option[String] = getValueOpt(note, name)
            val kind = getKind(note)

            // Most important factor - length of catch tokens.
            val lenFactor = ns.filter(t ⇒ t.index >= note.tokenFrom && t.index <= note.tokenTo).count(!_.isStopword)

            // If location is a city - top world cities get 20 additional points while US top
            // cities get 10 point. Note that 'DLGeoLocationKind' enumeration value ID is used
            // for scoring (city has bigger ID than region and country).
            val topsFactor = kind match {
                case CITY ⇒
                    val cityReg = glue(get("city"), get("region"))

                    if (TOP_WORLD.contains(cityReg))
                        2
                    else if (TOP_USA.contains(cityReg))
                        1
                    else
                        0
                case _ ⇒ 0
            }

            val usaFactor = getOpt("country") match {
                case Some(v) ⇒ if (v == "united states") 1 else 0
                case None ⇒ 0
            }

            // Note length has higher priority, than goes type of location
            // So, country > region > city for other countries).
            val kindFactor =
                kind match {
                    case CITY ⇒ 0
                    case REGION ⇒ 1
                    case METRO ⇒ 2
                    case COUNTRY ⇒ 3
                    case SUBCONTINENT ⇒ 4
                    case CONTINENT ⇒ 5

                    case _ ⇒ throw new AssertionError(s"Unexpected kind: $kind")
                }

            Seq(lenFactor, topsFactor, kindFactor, usaFactor)
        }

        case class Holder(note: NCNlpSentenceNote, kind: NCGeoLocationKind, weight: Seq[Int])

        for (tok ← ns) {
            val sorted = tok.
                getNotes("nlp:geo").
                map(n ⇒ Holder(n, getKind(n), calcWeight(n))).
                toSeq.
                sortBy(
                    -_.weight.
                        reverse.
                        zipWithIndex.map { case (v, idx) ⇒ v * Math.pow(10, idx) }.sum
                )

            if (sorted.nonEmpty) {
                val sortedByKind = sorted.groupBy(_.kind)

                // Keeps best candidates for each GEO kind.
                val remainHs = sortedByKind.
                    unzip._2.
                    flatMap(hsByKind ⇒ Seq(hsByKind.head) ++ hsByKind.tail.filter(_.weight == hsByKind.head.weight)).
                    toSeq

                sorted.diff(remainHs).foreach(p ⇒ removeNote(p.note))
            }
        }
    }
}
