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

package org.nlpcraft.nlp.enrichers

import org.apache.ignite.IgniteCache
import org.nlpcraft.ascii.NCAsciiTable
import org.nlpcraft.ignite.NCIgniteNlpCraft
import org.nlpcraft.{NCE, NCLifecycle}
import org.nlpcraft.nlp.{NCNlpSentence, NCNlpSentenceNote, NCNlpSentenceToken}
import org.nlpcraft.nlp.enrichers.basenlp.NCBaseNlpEnricher
import org.nlpcraft.nlp.enrichers.date.NCDateEnricher
import org.nlpcraft.nlp.enrichers.geo.NCGeoEnricher
import org.nlpcraft.nlp.enrichers.numeric.NCNumericEnricher
import org.nlpcraft.nlp.enrichers.quote.NCQuoteEnricher
import org.nlpcraft.nlp.enrichers.stopword.NCStopWordEnricher
import org.nlpcraft.nlp.preproc.NCPreProcessManager
import org.nlpcraft.ignite.NCIgniteHelpers._

/**
  * Enrichment pipeline manager.
  */
object NCNlpEnricherManager extends NCLifecycle("Enrichment manager") with NCIgniteNlpCraft {
    // NOTE: this cache is independent from datasource.
    @volatile private var cache: IgniteCache[String, NCNlpSentence] = _
    
    private val HEADERS = Map(
        "nlp:nlp" → (
            1,
            Seq("origText", "index", "pos", "lemma", "stem", "stopWord", "ne", "nne")
        ),
        "nlp:geo" → (
            2,
            Seq("city", "latitude", "longitude", "continent", "country", "kind", "region")
        ),
        "nlp:date" → (
            3,
            Seq("from", "to", "periods")
        ),
        "nlp:num" → (
            4,
            Seq("from", "to", "unit", "unitType")
        )
    )
    
    /**
      *
      * @param txt Text to enrich into NLP sentence.
      * @return
      */
    @throws[NCE]
    def enrich(txt: String): NCNlpSentence = {
        ensureStarted()
    
        val normTxt = NCPreProcessManager.normalize(txt)
    
        cache(normTxt) match {
            case None ⇒
                val s = new NCNlpSentence(normTxt)
            
                // Server-side enrichment pipeline.
                // NOTE: order of enrichers is IMPORTANT.
                NCBaseNlpEnricher.enrich(s)
                NCQuoteEnricher.enrich(s)
                NCStopWordEnricher.enrich(s)
                NCDateEnricher.enrich(s)
                NCNumericEnricher.enrich(s)
                NCGeoEnricher.enrich(s)
            
                prepareAsciiTable(s).info(logger, Some(s"Sentence enriched: $normTxt"))
            
                cache += normTxt → s
            
                s
        
            case Some(s) ⇒ s
        }
    }
    
    /**
      *
      * @param s NLP sentence to ASCII print.
      * @return
      */
    private def prepareAsciiTable(s: NCNlpSentence): NCAsciiTable = {
        case class Header(
            header: String,
            noteType: String,
            noteName: String
        )
        
        /**
          *
          * @param n Note.
          * @return
          */
        def mkNoteHeaders(n: NCNlpSentenceNote): scala.collection.Set[Header] = {
            val typ = n.noteType
            val prefix = typ.substring(typ.indexOf(':') + 1) // Remove 'nlp:' prefix.
            
            n.keySet
                .filter(name ⇒ HEADERS.get(typ) match {
                    case None ⇒ false
                    case Some(x) ⇒ x._2.contains(name)
                })
                .map(name ⇒ Header(prefix + ':' + name, typ, name))
        }
        
        val headers = s.flatten.flatMap(mkNoteHeaders).distinct.sortBy(hdr ⇒ {
            val x = HEADERS(hdr.noteType)
            
            (x._1 * 100) + x._2.indexOf(hdr.noteName)
        })
        
        val tbl = NCAsciiTable(headers.map(_.header): _*)
        
        def mkNoteValue(tok: NCNlpSentenceToken, hdr: Header): Seq[String] =
            tok.getNotes(hdr.noteType).filter(_.contains(hdr.noteName)).map(_(hdr.noteName).toString()).toSeq
        
        for (tok ← s) tbl += (headers.map(mkNoteValue(tok, _)): _*)
        
        tbl
    }
    
    /**
      * Starts this manager.
      */
    override def start(): NCLifecycle = {
        cache = ignite.cache[String, NCNlpSentence]("sentence-cache")
        
        NCBaseNlpEnricher.start()
        NCDateEnricher.start()
        NCStopWordEnricher.start()
        NCQuoteEnricher.start()
        NCNumericEnricher.start()
        NCGeoEnricher.start()
        
        super.start()
    }
    
    /**
      * Stops this manager.
      */
    override def stop(): Unit = {
        NCGeoEnricher.stop()
        NCNumericEnricher.stop()
        NCQuoteEnricher.stop()
        NCStopWordEnricher.stop()
        NCDateEnricher.stop()
        NCBaseNlpEnricher.stop()
        
        cache = null
        
        super.stop()
    }
}
