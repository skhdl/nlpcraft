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

import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.ling.{CoreAnnotation, CoreLabel}
import edu.stanford.nlp.pipeline.{Annotation, Annotator}
import edu.stanford.nlp.process.AbstractTokenizer
import edu.stanford.nlp.util.TypesafeMap

import scala.collection.JavaConversions._

/**
  * Custom annotator.
  * Implementation is based on edu.stanford.nlp.pipeline.TokenizerAnnotator
  */
class NCNlpAnnotator extends Annotator {
    override def annotate(annotation: Annotation): Unit =
        if (annotation.containsKey(classOf[TextAnnotation])) {
            val text = annotation.get(classOf[TextAnnotation])
            // don't wrap in BufferedReader.  It gives you nothing for in-memory String unless you need the readLine() method!
            val tokens = NCNlpTokenizer(text).tokenize

            println("!!!tokens=" + tokens)

            // cdm 2010-05-15: This is now unnecessary, as it is done in CoreLabelTokenFactory
            // for (CoreLabel token: tokens) {
            // token.set(CoreAnnotations.TextAnnotation.class, token.get(CoreAnnotations.TextAnnotation.class));
            // }
            // label newlines
            setNewlineStatus(tokens)
            // set indexes into document wide token list
            setTokenBeginTokenEnd(tokens)
            // add tokens list to annotation
            annotation.set(classOf[TokensAnnotation], tokens)
        }
        else
            throw new RuntimeException("Tokenizer unable to find text in annotation: " + annotation)


    override def requires = new util.HashSet[Class[_ <: CoreAnnotation[_]]]()

    override def requirementsSatisfied =
        new util.HashSet[Class[_ <: CoreAnnotation[_]]](
            util.Arrays.asList(classOf[TextAnnotation],
                classOf[TokensAnnotation],
                classOf[CharacterOffsetBeginAnnotation],
                classOf[CharacterOffsetEndAnnotation],
                classOf[BeforeAnnotation],
                classOf[AfterAnnotation],
                classOf[TokenBeginAnnotation],
                classOf[TokenEndAnnotation],
                classOf[PositionAnnotation],
                classOf[IndexAnnotation],
                classOf[OriginalTextAnnotation],
                classOf[ValueAnnotation],
                classOf[IsNewlineAnnotation])
        )

    /**
      *
      * @param toks
      */
    private def setNewlineStatus(toks: util.List[CoreLabel]): Unit = // label newlines
        for (tok ← toks)
            tok.set(
                classOf[IsNewlineAnnotation],
                tok.word == AbstractTokenizer.NEWLINE_TOKEN && (tok.endPosition - tok.beginPosition == 1)
            )

    /**
      *
      * @param toks
      */
    private def setTokenBeginTokenEnd(toks: util.List[CoreLabel]): Unit =
        toks.zipWithIndex.foreach { case (tok, idx) ⇒
            tok.set(classOf[TokenBeginAnnotation], idx)
            tok.set(classOf[TokenEndAnnotation], idx + 1)
        }

    /**
      *
      * @param claxx
      * @return
      */
    private implicit def convert(claxx: Class[_]): Class[_ <: TypesafeMap.Key[Any]] =
        claxx.asInstanceOf[Class[_ <: TypesafeMap.Key[Any]]]
}
