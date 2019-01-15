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

package org.nlpcraft.nlp.synonym

import org.nlpcraft._
import org.nlpcraft.nlp.synonym.NCSynonymType._
import org.nlpcraft.json.NCJson
import org.nlpcraft.nlp.wordnet.NCWordNetManager

/**
 * Synonyms manager.
 */
object NCSynonymManager extends NCLifecycle("Synonyms manager") {
    @volatile private var m: Map[String, Seq[Seq[String]]] = _

    /**
     * Starts manager.
     */
    @throws[NCE]
    override def start(): NCLifecycle = {
        ensureStopped()
        
        m = NCJson.extractResource[Map[String, List[List[String]]]]("synonyms/synonyms.json", ignoreCase = true).
            map(p ⇒ p._1.toUpperCase → p._2)

        val sets = m.flatMap(_._2).toSeq
        val dups = sets.flatten.filter(w ⇒ sets.count(_.contains(w)) > 1).distinct

        if (dups.nonEmpty)
            throw new NCE(s"Duplicated synonyms: ${dups.mkString(", ")}")

        m.foreach(p ⇒
            if (p._2.exists(_.isEmpty))
                throw new NCE(s"Empty synonyms sets found for POS: ${p._1}")
        )
    
        super.start()
    }

    /**
     * Gets synonyms.
     *
     * @param lemma Lemma to get synonyms for.
     * @param pos Penn Treebank POS tag.
     */
    def get(lemma: String, pos: String): Map[NCSynonymType, Seq[Seq[String]]] = {
        ensureStarted()

        val dlSyns: Seq[String] =
            m.get(pos) match {
                case Some(seq) ⇒
                    seq.find(_.contains(lemma)) match {
                        case Some(s) ⇒ s
                        case None ⇒ Seq.empty
                    }
                case None ⇒ Seq.empty
            }

        val wnSyns: Seq[Seq[String]] = NCWordNetManager.getSynonyms(lemma, pos)

        Map(
            NLPCRAFT → (if (dlSyns.isEmpty)Seq.empty else Seq(dlSyns)),
            WORDNET → wnSyns
        ).filter(_._2.nonEmpty)
    }
}
