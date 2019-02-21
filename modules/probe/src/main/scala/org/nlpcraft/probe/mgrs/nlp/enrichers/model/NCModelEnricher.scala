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

package org.nlpcraft.probe.mgrs.nlp.enrichers.model

import org.nlpcraft._
import org.nlpcraft.mdllib.NCElement
import org.nlpcraft.nlp._
import org.nlpcraft.probe._
import org.nlpcraft.probe.mgrs.{NCModelDecorator, NCSynonym}
import org.nlpcraft.probe.mgrs.nlp.NCProbeEnricher

import scala.collection.convert.DecorateAsScala
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Model elements enricher.
  */
object NCModelEnricher extends NCProbeEnricher("Model enricher") with DecorateAsScala {
    // Found-by-synonym model element.
    case class ElementMatch(
        element: NCElement,
        tokens: Seq[NCNlpSentenceToken],
        synonym: NCSynonym
    ) extends Ordered[ElementMatch] {
        // Tokens sparsity.
        lazy val sparsity: Int = tokens.zipWithIndex.tail.map {
            case (tok, idx) ⇒ Math.abs(tok.index - tokens(idx - 1).index)
        }.sum - tokens.length + 1
        
        // Number of tokens.
        lazy val length: Int = tokens.size
    
        override def compare(that: ElementMatch): Int = {
            // Check synonym first, then length and then sparsity.
            // Note that less sparsity means more certainty in a match.
            
            if (that == null)
                1
            else if (synonym < that.synonym)
                -1
            else if (synonym > that.synonym)
                1
            else if (length < that.length)
                -1
            else if (length > that.length)
                1
            else if (sparsity < that.sparsity)
                1
            else if (sparsity > that.sparsity)
                -1
            else
                0
        }
    }
    
    /**
      * Returns an iterator of tokens arrays where each token is jiggled left and right by given factor.
      * Note that only one token is jiggled at a time.
      *
      * @param ns NLP sentence to jiggle.
      * @param factor Distance of left or right jiggle, i.e. how far can an individual token move
      *     left or right in the sentence.
      */
    private def jiggle(ns: NCNlpSentenceTokenBuffer, factor: Int): Iterator[NCNlpSentenceTokenBuffer] = {
        require(factor >= 0)
        
        if (ns.isEmpty)
            Iterator.empty
        else if (factor == 0)
            Iterator.apply(ns)
        else
            new Iterator[NCNlpSentenceTokenBuffer] {
                private val min = -factor
                private val max = factor
                private val sz = ns.size
    
                private var i = 0 // Token index.
                private var d = 0 // Jiggle amount [min, max].
    
                private var isNext = sz > 0
    
                private def calcNext(): Unit = {
                    isNext = false
                    
                    d += 1
                    
                    while (i < sz && !isNext) {
                        while (d <= max && !isNext) {
                            val p = i + d
                            
                            if (p >= 0 && p < sz) // Valid new position?
                                isNext = true
                            else
                                d += 1
                        }
                        
                        if (!isNext) {
                            d = min
                            i += 1
                        }
                    }
                }
    
                override def hasNext: Boolean = isNext
    
                override def next(): NCNlpSentenceTokenBuffer = {
                    require(isNext)
    
                    val buf = NCNlpSentenceTokenBuffer(ns)
    
                    if (d != 0)
                        buf.insert(i + d , buf.remove(i)) // Jiggle.
    
                    calcNext()
    
                    buf
                }
            }
    }

    /**
      * Gets all sequential permutations of given tokens.
      *
      * For example, if buffer contains "a b c d" tokens, then this function will return the
      * sequence of following token sequences in this order:
      * "a b c d"
      * "a b c"
      * "b c d"
      * "a b"
      * "b c"
      * "c d"
      * "a"
      * "b"
      * "c"
      * "d"
      *
      * @param toks
      * @return
      */
    private def combos(toks: NCNlpSentenceTokenBuffer): Seq[NCNlpSentenceTokenBuffer] =
        (for (n ← toks.size until 0 by -1) yield toks.sliding(n)).flatten.map(NCNlpSentenceTokenBuffer(_))
    
    /**
      * Processes this NLP sentence.
      *
      * @param mdl Model decorator.
      * @param ns  NLP sentence to enrich.
      */
    @throws[NCE]
    override def enrich(mdl: NCModelDecorator, ns: NCNlpSentence): Unit = {
        ensureStarted()

        val jiggleFactor = mdl.model.getJiggleFactor
        val cache = mutable.HashSet.empty[Seq[Int]]
        val matches = ArrayBuffer.empty[ElementMatch]

        /**
          * Gets sequence of synonyms sorted in descending order by their weight, i.e. first synonym in
          * the sequence is the most important one.
          * 
          * @param fastMap
          * @param elmId
          * @param len
          * @return
          */
        def fastAccess(
            fastMap: Map[String/*Element ID*/, Map[Int/*Synonym length*/, Seq[NCSynonym]]],
            elmId: String,
            len: Int): Seq[NCSynonym] =
            fastMap.get(elmId).flatMap(_.get(len)) match {
                case Some(seq) ⇒ seq
                case None ⇒ Seq.empty[NCSynonym]
            }
    
        /**
          *
          * @param toks
          * @return
          */
        def tokString(toks: Seq[NCNlpSentenceToken]): String =
            toks.map(t ⇒ (t.origText, t.index)).mkString(" ")
    
        /**
          *
          * @param perm Permutation to process.
          */
        def procPerm(perm: NCNlpSentenceTokenBuffer): Unit =
            for (toks ← combos(perm)) {
                val key = toks.map(_.index)

                if (!cache.contains(key)) {
                    val len = toks.size

                    // Attempt to match each element.
                    for (elm ← mdl.elements.values) {
                        val elmId = elm.getId
                        var excluded = false

                        // Check synonym exclusions.
                        for (syn ← fastAccess(mdl.excludedSynonyms, elmId, len) if !excluded)
                            excluded = syn.isMatch(toks)

                        if (!excluded) {
                            var found = false

                            // Check synonym matches.
                            for (syn ← fastAccess(mdl.synonyms, elmId, len) if !found)
                                if (syn.isMatch(toks)) {
                                    found = true
                                    
                                    matches += ElementMatch(elm, toks, syn)
                                }
                        }
                    }
                    
                    cache += key
                }
            }                                        
        
        // Iterate over depth-limited permutations of the original sentence with and without stopwords.
        jiggle(ns, jiggleFactor).foreach(procPerm)
        jiggle(NCNlpSentenceTokenBuffer(ns.filter(!_.isStopword)), jiggleFactor).foreach(procPerm)

        val matchCnt = matches.size
        
        // Add notes for all remaining (non-intersecting) matches.
        for ((m, idx) ← matches.zipWithIndex) {
            val elm = m.element
            val syn = m.synonym
    
            if (!IS_PROBE_SILENT)
                logger.debug(s"Model '${mdl.model.getDescriptor.getId}' element found (${idx + 1} of $matchCnt) [" +
                    s"elementId=${m.element.getId}, " +
                    s"synonym=${m.synonym}, " +
                    s"tokens=${tokString(m.tokens)}" +
                    s"]")

            val tokIdxs = m.tokens.map(_.index)

            val direct = syn.isDirect && (tokIdxs == tokIdxs.sorted)

            val params = ArrayBuffer(
                "group" → elm.getGroup,
                "length" → m.tokens.map(_.words).sum,
                "direct" → direct
            )

            if (syn.isValueSynonym)
                params.append("value" → syn.value)
            
            val idxs = m.tokens.map(_.index).sorted
            
            val note = NCNlpSentenceNote(idxs, elm.getId, params: _*)
            
            m.tokens.foreach(_.add(note))
        }
    }
}
