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
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.probe.mgrs.nlp.post

import java.util.{List ⇒ JList}
import java.io.{Serializable ⇒ JSerializable}

import org.nlpcraft._
import org.nlpcraft.nlp.pos._
import org.nlpcraft.nlp._
import org.nlpcraft.probe.NCModelDecorator
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.collection._
import scala.collection.mutable.{HashMap ⇒ HMap}

/**
  * This collapser handles several tasks:
  * - "overall" collapsing after all other individual collapsers had their turn.
  * - Special further enrichment of tokens like linking, etc.
  *
  * In all cases of overlap (full or partial) - the "longest" note wins. In case of overlap and equal
  * lengths - the winning note is chosen based on this priority.
  */
object NCPostEnrichCollapser extends NCLifecycle("Post-enrich collapser") with LazyLogging {
    /**
      *
      * @param mdl
      * @param ns
      * @return
      */
    @throws[NCE]
    def collapse(mdl: NCModelDecorator, ns: NCNlpSentence): Seq[NCNlpSentence] = {
        ensureStarted()

        def get(toks: Seq[NCNlpSentenceToken]): Seq[NCNlpSentenceNote] = toks.flatten.filter(!_.isNlp).distinct

        val userNotesTypes = get(ns).map(_.noteType).distinct

        val delCombs = get(ns).flatMap(n ⇒ get(ns.slice(n.tokenFrom, n.tokenTo + 1)).filter(_ != n)).distinct

        if (delCombs.nonEmpty) {
            val deleted = mutable.ArrayBuffer.empty[Seq[NCNlpSentenceNote]]

            val sens =
                (1 to delCombs.size).
                    flatMap(delCombs.combinations).
                    sortBy(_.size).
                    flatMap(delComb ⇒
                        // Already processed with less subset of same deleted tokens.
                        if (!deleted.exists(_.forall(delComb.contains))) {
                            val nsClone = ns.clone()

                            delComb.map(_.id).foreach(nsClone.removeNote)

                            // Has overlapped notes for some tokens.
                            if (!nsClone.exists(_.count(!_.isNlp) > 1)) {
                                deleted += delComb

                                collapse(nsClone, userNotesTypes)

                                Some(nsClone)
                            }
                            else
                                None
                        }
                        else
                            None
                    )

            // Removes sentences which have only one difference - 'direct' flag of their user tokens.
            // `Direct` sentences have higher priority.
            case class Key(sysNotes: Seq[HMap[String, JSerializable]], userNotes: Seq[HMap[String, JSerializable]])
            case class Value(sentence: NCNlpSentence, directCount: Int)

            val m = HMap.empty[Key, Value]

            sens.map(sen ⇒ {
                val sysNotes = sen.flatten.filter(_.isSystem)
                val nlpNotes = sen.flatten.filter(_.isNlp)
                val userNotes = sen.flatten.filter(_.isUser)

                def get(seq:Seq[NCNlpSentenceNote], keys2Skip: String*): Seq[HMap[String, JSerializable]] =
                    seq.map(p ⇒ {
                        val m = p.clone()

                        // We have to delete some keys to have possibility to compare sentences.
                        m.remove("unid")
                        m.remove("direct")

                        m
                    })
                (Key(get(sysNotes), get(userNotes)), sen, nlpNotes.map(p ⇒ if (p.isDirect) 0 else 1).sum)
            }).
                foreach { case (key, sen, directCnt) ⇒
                    m.get(key) match {
                        case Some(v) ⇒
                            // Best sentence is sentence with `direct` synonyms.
                            if (v.directCount > directCnt)
                                m += key → Value(sen, directCnt)
                        case None ⇒ m += key → Value(sen, directCnt)
                    }
                }

            m.values.map(_.sentence).toSeq
        }
        else {
            collapse(ns, userNotesTypes)

            Seq(ns)
        }
    }

    /**
      * Fix tokens positions.
      * @param ns Sentence.
      * @param userNotesTypes Token types.
      */
    private def collapse(ns: NCNlpSentence, userNotesTypes: Seq[String]): Unit = {
        ns.
            filter(!_.isSimpleWord).
            filter(_.isStopword).
            flatten.
            filter(_.isNlp).
            foreach(_ += "stopWord" → false)

        val history = mutable.ArrayBuffer.empty[(Int, Int)]

        userNotesTypes.foreach(typ ⇒ zipNotes(ns, typ, userNotesTypes, history))

        unionStops(ns, userNotesTypes, history)

        fixIndexesReferences("nlp:function", ns, history)

        // Validation (all indexes calculated well)
        require(
            !ns.
                flatten.
                exists(n ⇒ ns.filter(_.wordIndexes.exists(n.wordIndexes.contains)).exists(p ⇒ !p.contains(n.id))),
            s"Invalid sentence:\n" +
                ns.map(t ⇒
                    // Human readable invalid sentence for debugging.
                    s"${t.origText}(${t.index})[${t.map(n ⇒ s"${n.noteType}-${n.tokenFrom}:${n.tokenTo}").mkString("|")}]"
                ).mkString("\n")
        )
    }

    /**
      * Fixes notes with references to other notes indexes.
      *
      * @param noteType Note type.
      * @param ns Sentence.
      * @param history Indexes transformation history.
      */
    private def fixIndexesReferences(noteType: String, ns: NCNlpSentence, history: Seq[(Int, Int)]): Unit =
        ns.filter(_.isTypeOf(noteType)).foreach(t ⇒
            t.getNoteOpt(noteType, "indexes") match {
                case Some(n) ⇒
                    val indexes = n.data[JList[Int]]("indexes").asScala

                    var fixed = indexes

                    history.foreach { case (idxOld, idxNew) ⇒ fixed = fixed.map(i ⇒ if (i == idxOld) idxNew else i) }

                    fixed = fixed.distinct

                    if (indexes != fixed) {
                        n += "indexes" → fixed.asJava.asInstanceOf[java.io.Serializable]

                        def x(seq: Seq[Int]): String = s"[${seq.mkString(", ")}]"

                        logger.trace(s"`$noteType` note indexes fixed [old=${x(indexes)}}, new=${x(fixed)}]")
                    }
                case None ⇒ // No-op.
            }
        )

    /**
      * Zip notes with same type.
      *
      * @param ns Sentence.
      * @param nType Notes type.
      * @param userNotesTypes Notes types.
      * @param history Indexes transformation history.
      */
    private def zipNotes(
        ns: NCNlpSentence, nType: String, userNotesTypes: Seq[String], history: mutable.ArrayBuffer[(Int, Int)]): Unit = {
        val nts = ns.getNotes(nType).filter(n ⇒ n.tokenFrom != n.tokenTo).sortBy(_.tokenFrom)

        val overlapped =
            nts.flatMap(n ⇒ n.tokenFrom to n.tokenTo).map(ns(_)).exists(
                t ⇒ userNotesTypes.map(pt ⇒ t.getNotes(pt).size).sum > 1
            )

        if (nts.nonEmpty && !overlapped) {
            val toksCopy = ns.clone()
            ns.clear()

            val buf = mutable.ArrayBuffer.empty[Int]
            
            for (i ← toksCopy.indices)
                nts.find(_.tokenIndexes.contains(i)) match {
                    case Some(n) ⇒
                        if (!buf.contains(n.tokenFrom)) {
                            buf += n.tokenFrom
                            
                            ns += mkCompound(ns, toksCopy, n.tokenIndexes, stop = false, ns.size, Some(n), history)
                        }
                    case None ⇒ simpleCopy(ns, history, toksCopy, i)
                }

            fixIndexes(ns, userNotesTypes)
        }
    }

    /**
      * Copies token.
      *
      * @param ns Sentence.
      * @param history Indexes transformation history.
      * @param toksCopy Copied tokens.
      * @param i Index.
      */
    private def simpleCopy(ns: NCNlpSentence, history: mutable.ArrayBuffer[(Int, Int)], toksCopy: NCNlpSentence, i: Int) = {
        val tokCopy = toksCopy(i)

        history += tokCopy.index → ns.size

        ns += tokCopy.clone(ns.size)
    }

    /**
      * Glues stop words.
      *
      * @param ns Sentence.
      * @param userNoteTypes Notes types.
      * @param history Indexes transformation history.
      */
    private def unionStops(ns: NCNlpSentence, userNoteTypes: Seq[String], history: mutable.ArrayBuffer[(Int, Int)]): Unit = {
        // Java collection used because using scala collections (mutable.Buffer.empty[mutable.Buffer[Token]]) is reason
        // Of compilation errors which seems as scala compiler internal error.
        import java.util

        import scala.collection.JavaConverters._
        
        val bufs = new util.ArrayList[mutable.Buffer[NCNlpSentenceToken]]()
        
        def last[T](l: JList[T]): T = l.get(l.size() - 1)
    
        ns.filter(t ⇒ t.isStopword && !t.isBracketed).foreach(t ⇒
            if (!bufs.isEmpty && last(bufs).last.index + 1 == t.index)
                last(bufs) += t
            else
                bufs.add(mutable.Buffer.empty[NCNlpSentenceToken] :+ t)
        )
        
        val idxsSeq = bufs.asScala.filter(_.lengthCompare(1) > 0).map(_.map(_.index))
        
        if (idxsSeq.nonEmpty) {
            val toksCopy = ns.clone()
            ns.clear()
            
            val buf = mutable.Buffer.empty[Int]
            
            for (i ← toksCopy.indices)
                idxsSeq.find(_.contains(i)) match {
                    case Some(idxs) ⇒
                        if (!buf.contains(idxs.head)) {
                            buf += idxs.head
                            
                            ns += mkCompound(ns, toksCopy, idxs, stop = true, ns.size, None, history)
                        }
                    case None ⇒ simpleCopy(ns, history, toksCopy, i)
                }
            
            fixIndexes(ns, userNoteTypes)
        }
    }

    /**
      * Fixes indexes for all notes after recreating tokens.
      *
      * @param ns Sentence.
      * @param userNoteTypes Notes types.
      */
    private def fixIndexes(ns: NCNlpSentence, userNoteTypes: Seq[String]) {
        // Replaces other notes indexes.
        for (t ← userNoteTypes :+ "nlp:nlp"; note ← ns.getNotes(t)) {
            val id = note.id
            
            val toks = ns.filter(_.contains(id)).sortBy(_.index)

            val newNote = note.clone(toks.map(_.index), toks.flatMap(_.wordIndexes).sorted)
            
            toks.foreach(t ⇒ {
                t.remove(id)
                t.add(newNote)
            })
        }
        
        // Special case - field index of core NLP note.
        ns.zipWithIndex.foreach { case (tok, idx) ⇒ tok.getNlpNote += "index" → idx}
    }

    /**
      * Makes compound note.
      *
      * @param ns Sentence.
      * @param toks Tokens.
      * @param indexes Indexes.
      * @param stop Flag.
      * @param idx Index.
      * @param commonNote Common note.
      * @param history Indexes transformation history.
      */
    private def mkCompound(
        ns: NCNlpSentence,
        toks: Seq[NCNlpSentenceToken],
        indexes: Seq[Int],
        stop: Boolean,
        idx: Int,
        commonNote: Option[NCNlpSentenceNote],
        history: mutable.ArrayBuffer[(Int, Int)]
    ): NCNlpSentenceToken = {
        val t = NCNlpSentenceToken(idx)

        // Note, it adds stop-words too.
        val content = toks.zipWithIndex.filter(p ⇒ indexes.contains(p._2)).unzip._1

        content.foreach(t ⇒ history += t.index → idx)
        
        def mkValue(get: NCNlpSentenceToken ⇒ String): String = {
            val buf = mutable.Buffer.empty[String]
            
            val n = content.size - 1
            
            content.zipWithIndex.foreach(p ⇒ {
                val t = p._1
                val idx = p._2
                
                buf += get(t)
                
                if (idx < n && t.endCharIndex != content(idx + 1).startCharIndex)
                    buf += " "
            })
            
            buf.mkString
        }
        
        val origText = mkValue((t: NCNlpSentenceToken) ⇒ t.origText)

        val idxs = Seq(idx)
        val wordIdxs = content.flatMap(_.wordIndexes).sorted

        val direct =
            commonNote match {
                case Some(n) if n.isUser ⇒ n.isDirect
                case _ ⇒ content.forall(_.isDirect)
            }

        val nlpNote = NCNlpSentenceNote(
            idxs,
            wordIdxs,
            "nlp:nlp",
            "index" → idx,
            "pos" → NCPennTreebank.SYNTH_POS,
            "posDesc" → NCPennTreebank.SYNTH_POS_DESC,
            "lemma" → mkValue((t: NCNlpSentenceToken) ⇒ t.lemma),
            "origText" → origText,
            "normText" → mkValue((t: NCNlpSentenceToken) ⇒ t.normText),
            "stem" → mkValue((t: NCNlpSentenceToken) ⇒ t.stem),
            "start" → t.index,
            "end" → t.index,
            "charLength" → origText.length,
            "quoted" → false,
            "stopWord" → stop,
            "bracketed" → false,
            "direct" → direct
        )

        t.add(nlpNote)
        
        // Adds processed note with fixed indexes.
        commonNote match {
            case Some(n) ⇒
                ns.removeNote(n.id)
                t.add(n.clone(idxs, wordIdxs))
            case None ⇒ // No-op.
        }

        t
    }
}
