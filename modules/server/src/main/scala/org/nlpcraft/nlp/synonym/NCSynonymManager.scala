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

package org.nlpcraft.nlp.synonym

import org.nlpcraft.json.NCJson
import org.nlpcraft.nlp.synonym.NCSynonymType._
import org.nlpcraft.nlp.wordnet.NCWordNet
import org.nlpcraft.{NCE, NCLifecycle}

/**
 * Synonyms manager.
 */
object NCSynonymManager extends NCLifecycle("CORE synonyms manager") {
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

        val wnSyns: Seq[Seq[String]] = NCWordNet.getSynonyms(lemma, pos)

        Map(
            NLPCRAFT → (if (dlSyns.isEmpty)Seq.empty else Seq(dlSyns)),
            WORDNET → wnSyns
        ).filter(_._2.nonEmpty)
    }
}
