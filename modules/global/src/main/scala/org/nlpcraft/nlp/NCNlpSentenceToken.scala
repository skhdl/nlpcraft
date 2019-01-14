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
 * Software:    NlpCraft
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

package org.nlpcraft.nlp

import org.nlpcraft.nlp.pos._
import scala.collection.mutable
import java.util.{List ⇒ JList}
import scala.collection.JavaConverters._

/**
  * NLP token is a collection of NLP notes associated with that token.
  */
case class NCNlpSentenceToken(index: Int) extends mutable.HashSet[NCNlpSentenceNote] with Serializable {
    private var nlpNote: NCNlpSentenceNote = _

    /**
      * Simple word is a non synthetic word that's also not part of any domain-specific note type.
      */
    // TODO: review all usage places. How is it correlated with user tokens?
    def isSimpleWord: Boolean = this.size == 1

    def words: Int = origText.split(" ").length

    // Shortcuts for some frequently used *mandatory* notes.
    def normText: String = getNlpValue[String]("normText")
    def startCharIndex: Int = getNlpValue[Int]("start").intValue() // Start character index.
    def endCharIndex: Int = getNlpValue[Int]("end").intValue() // End character index.
    def origText: String = getNlpValue[String]("origText")
    def wordLength: Int = getNlpValue[Int]("wordLength").intValue()
    def wordIndexes: Seq[Int] = getNlpValue[JList[Int]]("wordIndexes").asScala
    def pos: String = getNlpValue[String]("pos")
    def posDescription: String = getNlpValue[String]( "posDesc")
    def lemma: String = getNlpValue[String]("lemma")
    def stem: String = getNlpValue[String]("stem")
    def isStopword: Boolean = getNlpValue[Boolean]("stopWord")
    def isBracketed: Boolean = getNlpValue[Boolean]("bracketed")
    def isDirect: Boolean = getNlpValue[Boolean]("direct")
    def isQuoted: Boolean = getNlpValue[Boolean]("quoted")
    def isSynthetic: Boolean = NCPennTreebank.isSynthetic(pos)
    def isKnownWord: Boolean = getNlpValue[Boolean]("dict")

    /**
      *
      * @param noteType Note type.
      */
    def getNotes(noteType: String): mutable.Set[NCNlpSentenceNote] = filter(_.noteType == noteType)

    /**
      * Clones note.
      */
    def clone(index: Int): NCNlpSentenceToken = {
        val t = NCNlpSentenceToken(index)

        t ++= this

        t
    }

    /**
      * Removes note with given ID. No-op if ID wasn't found.
      *
      * @param id Note ID.
      */
    def remove(id: String): Unit = retain(_.id != id)

    /**
      * Tests whether or not this token contains note with given ID.
      */
    def contains(id: String): Boolean = exists(_.id == id)

    /**
      *
      * @param noteType Note type.
      * @param noteName Note name.
      */
    def getNoteOpt(noteType: String, noteName: String): Option[NCNlpSentenceNote] = {
        val ns = getNotes(noteType).filter(_.contains(noteName))

        ns.size match {
            case 0 ⇒ None
            case 1 ⇒ Some(ns.head)
            case _ ⇒ throw new AssertionError(s"Multiple notes found [type=$noteType, name=$noteName, token=$this]")
        }
    }

    /**
      * Gets note with given type and name.
      *
      * @param noteType Note type.
      * @param noteName Note name.
      */
    def getNote(noteType: String, noteName: String): NCNlpSentenceNote =
        getNoteOpt(noteType, noteName) match {
            case Some(n) ⇒ n
            case None ⇒ throw new AssertionError(s"Note not found [type=$noteType, name=$noteName, token=$this]")
        }

    /**
      * Gets NLP note.
      */
    def getNlpNote: NCNlpSentenceNote = {
        require(nlpNote != null)

        nlpNote
    }

    /**
      *
      * @param noteName Note name.
      * @tparam T Type of the note value.
      */
    def getNlpValueOpt[T: Manifest](noteName: String): Option[T] =
        getNlpNote.get(noteName) match {
            case Some(v) ⇒ Some(v.asInstanceOf[T])
            case None ⇒ None
        }

    /**
      *
      * @param noteName Note name.
      * @tparam T Type of the note value.
      */
    def getNlpValue[T: Manifest](noteName: String): T = getNlpNote(noteName).asInstanceOf[T]

    /**
      * Tests if this token has any notes of given type(s).
      *
      * @param nodeTypes Note type(s) to check.
      */
    def isTypeOf(nodeTypes: String*): Boolean = {
        var found = false

        for (noteType ← nodeTypes if !found)
            if (getNotes(noteType).nonEmpty)
                found = true

        found
    }

    override def +=(elem: NCNlpSentenceNote): NCNlpSentenceToken.this.type = {
        if (elem.isNlp)
            nlpNote = elem

        super.+=(elem)
    }

    override def add(elem: NCNlpSentenceNote): Boolean = {
        val res = super.add(elem)

        if (res && elem.isNlp)
            nlpNote = elem

        res
    }

    override def toString(): String =
        this.toSeq.sortBy(t ⇒ (if (t.isNlp) 0 else 1, t.noteType)).mkString("NLP token [", "|", "]")
}
