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

package org.nlpcraft.common.nlp

import java.io.{Serializable ⇒ JSerializable}
import java.util.{List ⇒ JList}

import org.nlpcraft.common._
import org.nlpcraft.common.ascii._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.language.implicitConversions

/**
  * Sentence token note is a typed map of KV pairs.
  *
  * @param id Internal ID.
  */
class NCNlpSentenceNote(
    val id: String,
    val values: mutable.HashMap[String, JSerializable] = mutable.HashMap.empty[String, JSerializable]
) extends java.io.Serializable with NCAsciiLike {
    import NCNlpSentenceNote._

    // These properties should be cloned as they are auto-set when new clone
    // is created.
    private final val SKIP_CLONE = Set(
        "unid",
        "minIndex",
        "maxIndex",
        "wordIndexes",
        "wordLength",
        "tokMinIndex",
        "tokMaxIndex",
        "tokWordIndexes",
        "contiguous",
        "sparsity"
    )

    private val hash: Int = id.hashCode()

    this.put("unid", this.id)

    // Shortcuts for mandatory fields. (Immutable fields)
    lazy val noteType: String = this("noteType").asInstanceOf[String]
    lazy val tokenFrom: Int = this("tokMinIndex").asInstanceOf[Int] // First index.
    lazy val tokenTo: Int = this("tokMaxIndex").asInstanceOf[Int] // Last index.
    lazy val tokenIndexes: Seq[Int] = this("tokWordIndexes").asInstanceOf[JList[Int]].asScala // Includes 1st and last indices too.
    lazy val wordIndexes: Seq[Int] = this("wordIndexes").asInstanceOf[JList[Int]].asScala // Includes 1st and last indices too.
    lazy val sparsity: Int = this("sparsity").asInstanceOf[Int]
    lazy val isContiguous: Boolean = this("contiguous").asInstanceOf[Boolean]
    lazy val isDirect: Boolean = this("direct").asInstanceOf[Boolean]
    lazy val isUser: Boolean = !noteType.startsWith("nlp:")
    lazy val isSystem: Boolean = noteType.startsWith("nlp:")
    lazy val isNlp: Boolean = noteType == "nlp:nlp"

    // Typed getter.
    def data[T](key: String): T = this(key).asInstanceOf[T]
    def dataOpt[T](key: String): Option[T] = this.get(key).asInstanceOf[Option[T]]

    override def equals(obj: Any): Boolean = obj match {
        case h: NCNlpSentenceNote ⇒ h.id == id
        case _ ⇒ false
    }

    override def hashCode(): Int = hash

    /**
      * Clones this note.
      */
    def clone(indexes: Seq[Int], wordIndexes: Seq[Int], params: (String, Any)*): NCNlpSentenceNote =
        NCNlpSentenceNote(
            id,
            indexes,
            Some(wordIndexes),
            noteType,
            this.filter(p ⇒ !SKIP_CLONE.contains(p._1)).toSeq ++ params:_*
        )

    /**
      *
      * @return
      */
    override def toAscii: String =
        this.iterator.toSeq.sortBy(_._1).foldLeft(NCAsciiTable("Key", "Value"))((t, p) ⇒ t += p).toString

    /**
      *
      * @return
      */
    override def toString: String =
        this.toSeq.filter(_._1 != "unid").sortBy(t ⇒ { // Don't show internal ID.
            val typeSort = t._1 match {
                case "noteType" ⇒ 1
                case _ ⇒ Math.abs(t._1.hashCode)
            }
            (typeSort, t._1)
        }).map(p ⇒ s"${p._1}=${p._2}").mkString("NLP note [", ", ", "]")
}

object NCNlpSentenceNote {
    implicit def getValues(x: NCNlpSentenceNote): mutable.HashMap[String, JSerializable] = x.values

    /**
      * Sparsity depth (or rank) as sum of all gaps in indexes. Gap is a non-consecutive index.
      *
      * @param idx Sequence of indexes.
      * @return
      */
    private def calcSparsity(idx: Seq[Int]): Int =
        idx.zipWithIndex.tail.map {  case (v, i) ⇒ Math.abs(v - idx(i - 1)) }.sum - idx.length + 1

    /**
      * Creates new note with given parameters.
      *
      * @param id Internal ID.
      * @param indexes Indexes in the sentence.
      * @param wordIndexesOpt Word indexes. Optional.
      * @param typ Type of the node.
      * @param params Parameters.
      */
    def apply(
        id: String,
        indexes: Seq[Int],
        wordIndexesOpt: Option[Seq[Int]],
        typ: String,
        params: (String, Any)*
    ): NCNlpSentenceNote = {
        def calc(seq: Seq[Int]): (Int, Int, Int, JList[Int], Int) =
            (calcSparsity(seq), seq.min, seq.max, seq.asJava, seq.length)

        val (sparsity, tokMinIndex, tokMaxIndex, tokWordIndexes, len) = calc(wordIndexesOpt.getOrElse(indexes))

        new NCNlpSentenceNote(
            id,
            mutable.HashMap[String, JSerializable]((
            params :+
               ("noteType" → typ) :+
               ("tokMinIndex" → indexes.min) :+
               ("tokMaxIndex" → indexes.max) :+
               ("tokWordIndexes" → indexes.asJava) :+
               ("minIndex" → tokMinIndex) :+
               ("maxIndex" → tokMaxIndex) :+
               ("wordIndexes" → tokWordIndexes) :+
               ("wordLength" → len) :+
               ("sparsity" → sparsity) :+
               ("contiguous" → (sparsity == 0))
            ).map(p ⇒ p._1 → p._2.asInstanceOf[JSerializable]): _*)
        )
    }

    /**
      * Creates new note with given parameters.
      *
      * @param indexes Indexes in the sentence.
      * @param typ Type of the node.
      * @param params Parameters.
      */
    def apply(indexes: Seq[Int], typ: String, params: (String, Any)*): NCNlpSentenceNote =
        apply(U.genGuid(), indexes, None, typ, params: _*)

    /**
      * Creates new note with given parameters.
      * @param indexes Indexes in the sentence.
      * @param wordIndexes Word indexes in the sentence.
      * @param typ Type of the node.
      * @param params Parameters.
      */
    def apply(indexes: Seq[Int], wordIndexes: Seq[Int], typ: String, params: (String, Any)*): NCNlpSentenceNote =
        apply(U.genGuid(), indexes, Some(wordIndexes), typ, params: _*)
}

