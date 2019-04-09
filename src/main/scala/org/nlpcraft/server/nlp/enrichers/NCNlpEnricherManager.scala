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

package org.nlpcraft.server.nlp.enrichers

import org.apache.ignite.IgniteCache
import org.nlpcraft.common.ascii.NCAsciiTable
import org.nlpcraft.common.nlp.{NCNlpSentence, NCNlpSentenceNote, NCNlpSentenceToken}
import org.nlpcraft.server.ignite.NCIgniteHelpers._
import org.nlpcraft.server.ignite.NCIgniteInstance
import org.nlpcraft.server.nlp.enrichers.basenlp.NCBaseNlpEnricher
import org.nlpcraft.server.nlp.enrichers.date.NCDateEnricher
import org.nlpcraft.server.nlp.enrichers.geo.NCGeoEnricher
import org.nlpcraft.server.nlp.enrichers.numeric.NCNumericEnricher
import org.nlpcraft.server.nlp.enrichers.quote.NCQuoteEnricher
import org.nlpcraft.server.nlp.enrichers.stopword.NCStopWordEnricher
import org.nlpcraft.server.nlp.preproc.NCPreProcessManager
import org.nlpcraft.common._
import org.nlpcraft.common.NCLifecycle

import scala.util.control.Exception.catching

/**
  * Enrichment pipeline manager.
  */
object NCNlpEnricherManager extends NCLifecycle("Enrichment manager") with NCIgniteInstance {
    // NOTE: this cache is independent from datasource.
    @volatile private var cache: IgniteCache[String, NCNlpSentence] = _
    
    private val HEADERS = Map(
        "nlp:nlp" → (
            1,
            Seq("origText", "index", "pos", "lemma", "stem", "bracketed", "quoted", "stopWord", "ne", "nne")
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
        
        if (normTxt != txt)
            logger.info(s"Sentence normalized to: $normTxt")
        
        catching(wrapIE) {
            cache(normTxt) match {
                case Some(s) ⇒
                    prepareAsciiTable(s).info(logger, Some(s"Sentence enriched: $normTxt"))
                    
                    s
                    
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
            }
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
        catching(wrapIE) {
            cache = ignite.cache[String, NCNlpSentence]("sentence-cache")
        }
        
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
