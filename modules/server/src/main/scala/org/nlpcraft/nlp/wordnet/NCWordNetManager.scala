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

package org.nlpcraft.nlp.wordnet

import org.nlpcraft._
import net.sf.extjwnl.data.POS._
import net.sf.extjwnl.data.{IndexWord, POS, PointerType}
import net.sf.extjwnl.dictionary.MorphologicalProcessor
import net.sf.extjwnl.dictionary.Dictionary
import scala.collection.JavaConverters._

/**
  * WordNet manager.
  */
object NCWordNetManager extends NCLifecycle("WordNet manager") {
    @volatile private var dic: Dictionary = _
    @volatile private var morph: MorphologicalProcessor = _
    
    private def pennPos2WordNet(pennPos: String): Option[POS] =
        pennPos.head match {
            case 'N' ⇒ Some(NOUN)
            case 'V' ⇒ Some(VERB)
            case 'J' ⇒ Some(ADJECTIVE)
            case 'R' ⇒ Some(ADVERB)
            
            case _ ⇒ None
        }
    
    // Process WordNet formatted multi-word entries (they are split with '_').
    private def normalize(str: String) = str.replaceAll("_", " ")
    
    // Converts words.
    private def convert(str: String, initPos: POS, targetPos: POS): Seq[String] = {
        val word = dic.getIndexWord(initPos, str)
        
        if (word != null)
            word.getSenses.asScala.flatMap(synset ⇒
                synset.getPointers(PointerType.DERIVATION).asScala.flatMap(p ⇒ {
                    val trg = p.getTargetSynset

                    if (trg.getPOS == targetPos)
                        trg.getWords.asScala.map(p ⇒ normalize(p.getLemma))
                    else
                        Seq.empty
                })
            ).distinct
        else
            Seq.empty[String]
    }

    /**
      * Starts manager.
      */
    @throws[NCE]
    override def start(): NCLifecycle = {
        ensureStopped()
        
        dic =  Dictionary.getDefaultResourceInstance
        morph = dic.getMorphologicalProcessor

        super.start()
    }
    
    /**
      * Gets a sequence of possible nouns relatives for the given adjective.
      *
      * @param adj An adjective to match.
      * @return A number of possible noun relatives.
      */
    def getNNsForJJ(adj: String): Seq[String] = {
        ensureStarted()
        
        convert(adj, ADJECTIVE, NOUN)
    }
    
    /**
      * Gets a sequence of possible adjective relatives for the given noun.
      *
      * @param noun A noun to match.
      * @return A number of possible adjective relatives.
      */
    def getJJsForNN(noun: String): Seq[String] = {
        ensureStarted()
        
        convert(noun, NOUN, ADJECTIVE)
    }
    
    /**
      * Gets base form using more precision method.
      *
      * It drops base form like 'Alice'→'louse', 'God'→'od' and 'better'→'well'
      * which produced by WordNet if the exact base form not found.
      *
      * @param lemma Lemma to get a WordNet base form.
      * @param pennPos Lemma's Penn Treebank POS tag.
      */
    def getBaseForm(lemma: String, pennPos: String, syns: Set[String] = null): String = {
        ensureStarted()
        
        pennPos2WordNet(pennPos) match {
            case Some(wnPos) ⇒
                morph.lookupBaseForm(wnPos, lemma) match {
                    case wnWord: IndexWord ⇒
                        val wnLemma = wnWord.getLemma
                        val synonyms = if (syns == null) getSynonyms(lemma, pennPos).flatten.toSet else syns
                        
                        if (synonyms.contains(wnLemma))
                            wnLemma
                        else
                            lemma
                    case null ⇒ lemma
                }
                
            // For unsupported POS tags - return the input lemma.
            case None ⇒ lemma
        }
    }
    
    /**
      * Gets synonyms for given lemma and its POS tag.
      *
      * @param lemma Lemma to find synonyms for.
      * @param pennPos Lemma's Penn Treebank POS tag.
      */
    def getSynonyms(
        lemma: String,
        pennPos: String): Seq[Seq[String]] = {
        ensureStarted()
        
        val res: Seq[Seq[String]] = pennPos2WordNet(pennPos) match {
            case Some(wnPos) ⇒
                val wnWord = dic.lookupIndexWord(wnPos, lemma)
                
                if (wnWord == null)
                    Seq.empty
                else
                    wnWord.getSynsetOffsets match {
                        case synsOffs: Array[Long] ⇒
                            synsOffs.
                                map(dic.getSynsetAt(wnPos, _)).
                                filter(_.getPOS == wnPos).
                                map(
                                    _.getWords.asScala.
                                        map(_.getLemma.toLowerCase).
                                        filter(_ != lemma).
                                        map(normalize).toSeq
                                )
                        
                        case null ⇒ Seq.empty
                    }
                
            // Invalid POS.
            case None ⇒ Seq.empty
        }
        
        res.filter(_.nonEmpty)
    }
}
