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

package org.nlpcraft.common.nlp.log

import java.text.SimpleDateFormat
import java.util.{Date, List ⇒ JList}

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.common.ascii._
import org.nlpcraft.common.nlp._

import scala.collection.JavaConverters._
import scala.collection._

/**
 * Utility service that provides supporting functionality for ASCII rendering.
 */
object NCNlpAsciiLogger extends LazyLogging {
    case class NoteMetadata(noteType: String, filtered: Seq[String], isFull: Boolean)
    
    // Order and sorting of notes for ASCII output.
    private final val NOTE_TYPES = Seq[String](
        "nlp:nlp",
        "nlp:geo",
        "nlp:date",
        "nlp:num",
        "nlp:function",
        "nlp:coordinate"
    )

    // Filters for notes types. If filter is not set all columns will display.
    private final val NOTE_COLUMNS = Map[String, Seq[String]](
        "nlp:nlp" → Seq(
            "index",
            "origText",
            "pos",
            "quoted",
            "stopWord",
            "dict",
            "wordIndexes",
            "direct",
            "sparsity"
        )
    )
    
    private final val FMT = new SimpleDateFormat("yyyy/MM/dd")

    private final val SORT: Map[String, Map[String, Int]] =
        Map(
            "nlp:geo" → Seq("kind", "city", "latitude", "longitude", "state", "country", "subcontinent", "continent", "metro"),
            "nlp:date" → Seq("from", "to", "periods"),
            "nlp:function" → Seq("type", "length", "indexes")
        ).map(p ⇒ p._1 → p._2.zipWithIndex.map(p ⇒ p._1 → p._2).toMap)

    private def format(l: Long): String = FMT.format(new Date(l))

    /**
     * Filters and sorts keys pairs to visually group notes logically.
     *
     * @param pairs Sequence of note key/note value key pairs.
     */
    private def filterKeysPairs(pairs: Seq[(String, String)]): Seq[NoteMetadata] = {
        val seq =
            pairs.map(_._1).distinct.map(p ⇒ p → pairs.filter(_._1 == p).map(_._2)).
                sortBy(p ⇒ {
                    val idx = NOTE_TYPES.indexWhere(_ == p._1)

                    if (idx >= 0) idx else Integer.MAX_VALUE
                })

        seq.map(s ⇒ {
            val t = s._1

            val (filtered, isFull) =
                if (t.startsWith("nlp:"))
                    NOTE_COLUMNS.get(t) match {
                        case Some(fs) ⇒ (s._2.filter(fs.contains).sortBy(p ⇒ fs.indexWhere(_ == p)), false)
                        case None ⇒ (Seq.empty[String], true)
                    }
                else
                    (Seq.empty[String], true)

            NoteMetadata(t, filtered, isFull)
        })
    }
    
    /**
      * Normalize header.
      *
      * @param h Header.
      */
    private def normalizeHeader(h: String): String = h.replaceAll("nlp:", "")

    /**
     *
     * @param md Notes.
     */
    private def mkTable(md: Seq[NoteMetadata]): NCAsciiTable =
        NCAsciiTable(md.flatMap(h ⇒
            if (h.isFull)
                Seq(normalizeHeader(h.noteType))
            else
                h.filtered.map(p ⇒ s"${normalizeHeader(h.noteType)}:${p.toLowerCase}")
        ): _*)
    
    private def note2String(note: NCNlpSentenceNote): String = {
        val sorted: Seq[(String, java.io.Serializable)] =
            SORT.get(note.noteType) match {
                case Some(map) ⇒ note.toSeq.sortBy(p ⇒ map.getOrElse(p._1, Int.MaxValue))
                case None ⇒ note.toSeq
            }
    
        def vals2String(seq: Seq[(String, java.io.Serializable)]): String = {
            def getValue(name: String): java.io.Serializable = {
                val found = seq.find(_._1 == name)
            
                // Fail-fast in case of programmatic errors.
                require(found.isDefined, s"Invalid note value: $name")
            
                found.get._2
            }
        
            def mkMore(incl: Boolean): String = if (incl) ">=" else ">"
            def mkLess(incl: Boolean): String = if (incl) "<=" else "<"
            def mkValue(name: String, fractionalField: String): String = {
                val d = getValue(name).asInstanceOf[Double]

                if (getValue(fractionalField).asInstanceOf[Boolean]) d.toString else d.toInt.toString
            }
            def mkBool(name: String): Boolean = getValue(name).asInstanceOf[Boolean]
            def mkString(name: String): String = getValue(name).toString
            def mkJListString(name: String): String =
                getValue(name).asInstanceOf[java.util.List[String]].asScala.mkString(",")
            def mkDate(name: String): String = format(getValue(name).asInstanceOf[Long])
        
            def getValueOpt(name: String): Option[java.io.Serializable] =
                seq.find(_._1 == name) match {
                    case Some(x) ⇒ Some(x._2)
                    case None ⇒ None
                }
        
            def mkStringOpt(name: String): Option[String] =
                getValueOpt(name) match {
                    case Some(jv) ⇒ Some(jv.toString)
                    case None ⇒ None
                }
    
            note.noteType match {
                case "nlp:geo" ⇒
                    def get(name: String): String = mkStringOpt(name).getOrElse("")
                
                    val content =
                        Seq(
                            get("country"),
                            get("region"),
                            get("city"),
                            get("latitude"),
                            get("longitude"),
                            get("metro")
                        ).filter(!_.isEmpty).mkString("|")
                
                    s"${mkString("kind")} $content"
                    
                case "nlp:date" ⇒
                    val from = mkDate("from")
                    val to = mkDate("to")
                    val ps = mkJListString("periods")
                
                    val r = s"$from:$to"
                
                    s"range=$r, periods=$ps"

                case "nlp:function" ⇒
                    val t = mkString("type")
                    val idxs = getValue("indexes").asInstanceOf[JList[Int]].asScala.mkString(",")

                    s"type=$t, indexes=[$idxs]"

                case "nlp:coordinate" ⇒ s"${getValue("latitude")} and ${getValue("longitude")}"

                case "nlp:num" ⇒
                    val from = mkValue("from", "isFractional")
                    val to = mkValue("to", "isFractional")
                    val fromIncl = mkBool("fromIncl")
                    val toIncl = mkBool("toIncl")
                    val isRangeCond = mkBool("isRangeCondition")
                    val isEqCond = mkBool("isEqualCondition")
                    val isNotEqCond = mkBool("isNotEqualCondition")
                    val isFromNegInf = mkBool("isFromNegativeInfinity")
                    val isToPosInf = mkBool("isToPositiveInfinity")

                    val x1 = if (isFromNegInf) "-Infinity" else from
                    val x2 = if (isToPosInf) "+Infinity" else to

                    var s =
                        if (isRangeCond)
                            s"${mkMore(fromIncl)}$x1 && ${mkLess(toIncl)}$x2"
                        else if (isEqCond)
                            s"=$x1"
                        else {
                            assert(isNotEqCond)

                            s"!=$x1"
                        }

                    s = getValueOpt("index") match {
                        case Some(idx) ⇒ s"$s, index=$idx"
                        case None ⇒ s
                    }

                    s = getValueOpt("unit") match {
                        case Some(u) ⇒ s"$s, unit=$u(${getValue("unitType")})"
                        case None ⇒ s
                    }

                    s
                // User tokens.
                case _ ⇒
                    seq.map(p ⇒ s"${p._1}=${if (p._2 == null) "null" else {p._2}.toString}").mkString(", ")
            }
        }
    
        val v = if (sorted.lengthCompare(1) > 0) vals2String(sorted) else sorted.map(p ⇒ s"${p._2}").mkString(", ")
    
        if (note.tokenFrom < note.tokenTo)
            s"$v ${s"<${note.tokenFrom} to ${note.tokenTo}, id=${note.id}>"}"
        else
            s"$v"
    }

    private def mkCells(hs: Seq[NoteMetadata], t: NCNlpSentenceToken): Seq[String] = {
        def filter(h: NoteMetadata) = t.filter(_.noteType == h.noteType)
        
        hs.flatMap(h ⇒
            if (h.isFull)
                Seq(filter(h).map(p ⇒ note2String(p)).mkString(", "))
            else
                h.filtered.
                    map(p ⇒ filter(h).filter(_.contains(p)).map(n ⇒ n(p)).mkString(", "))
        )
    }
    
    /**
      * Prepares table to print.
      */
    def prepareTable(sen: NCNlpSentence): NCAsciiTable = {
        val md = filterKeysPairs(sen.flatMap(t ⇒ t.map(n ⇒ for (vk ← n.keys) yield n.noteType → vk)).flatten.distinct)
        
        val tbl = mkTable(md)
        
        for (t ← sen) tbl += (mkCells(md, t): _*)
        
        tbl
    }
}