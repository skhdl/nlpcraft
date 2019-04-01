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

package org.nlpcraft.server.nlp.core.stanford

import java.util
import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.{NormalizedNamedEntityTagAnnotation, SentencesAnnotation, TokensAnnotation}
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.util.{ArrayCoreMap, CoreMap}
import org.nlpcraft.common.nlp.core.NCNlpCoreManager
import org.nlpcraft.common.{NCE, NCLifecycle}
import org.nlpcraft.server.nlp.core.{NCNlpParser, NCNlpWord}

import scala.collection.JavaConverters._

/**
  * Stanford NLP parser implementation.
  */
object NCStanfordParser extends NCLifecycle("Stanford NLP parser") with NCNlpParser {
    private var stanford: StanfordCoreNLP = _

    /**
      *
      * @param txt
      * @return
      */
    private def coreLabels(txt: String): Seq[CoreLabel] = {
        val ann = new Annotation(txt)

        stanford.annotate(ann)

        val a: util.List[CoreMap] = ann.get(classOf[SentencesAnnotation])

        if (a == null)
            throw new NCE("Sentence annotation not found.")

        a.asScala.flatMap(p ⇒ {
            val value: util.List[CoreLabel] = p.asInstanceOf[ArrayCoreMap].get(classOf[TokensAnnotation])

            value.asScala
        })
    }

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        val p = new Properties()

        p.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner")

        // Created with hardcoded properties just for minimize configuration issues.
        stanford = new StanfordCoreNLP(p)

        super.start()
    }

    override def parse(sen: String): Seq[NCNlpWord] = {
        ensureStarted()

        coreLabels(sen).map(t ⇒ {
            val nne = t.get(classOf[NormalizedNamedEntityTagAnnotation])
            val normalWord = t.originalText().toLowerCase

            NCNlpWord(
                word = t.originalText(),
                normalWord = normalWord,
                lemma = t.lemma().toLowerCase,
                stem = NCNlpCoreManager.stemWord(normalWord).toString,
                pos = t.tag(),
                start = t.beginPosition,
                end = t.endPosition(),
                length = t.endPosition() - t.beginPosition(),
                ne = Option(t.ner),
                nne = Option(nne)
            )
        })
    }
}
