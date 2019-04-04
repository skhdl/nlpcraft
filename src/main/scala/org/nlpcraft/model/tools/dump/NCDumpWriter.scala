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

package org.nlpcraft.model.tools.dump

import java.io.{BufferedOutputStream, FileOutputStream, ObjectOutputStream}

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.common._
import org.nlpcraft.common.version.NCVersion
import org.nlpcraft.model.intent.NCIntentSolver.IntentCallback
import org.nlpcraft.model.intent.{NCIntentSolver, NCIntentSolverContext}
import org.nlpcraft.model.{NCModel, NCQueryResult}
import resource.managed

import scala.collection.JavaConverters._

/**
  * Dump writer.
  */
object NCDumpWriter extends LazyLogging {
    /**
      *
      * @param mdl
      * @param solver
      * @param path
      */
    @throws[NCE]
    def write(mdl: NCModel, solver: NCIntentSolver, path: String): Unit = {
        val solverFix = new NCIntentSolver(
            s"Dump [name=${solver.getName}, version=${NCVersion.getCurrent}]", null
        )

        val intents = solver.getIntents.asScala

        intents.foreach(i ⇒
            solverFix.addIntent(
                i,
                new IntentCallback {
                    override def apply(t: NCIntentSolverContext): NCQueryResult =
                        NCQueryResult.text(s"OK: ${i.getId}")
                    }
            )
        )

        val mdlFix =
            NCDumpModel(
                description = mdl.getDescription,
                docsUrl = mdl.getDocsUrl,
                vendorUrl = mdl.getVendorUrl,
                vendorEmail = mdl.getVendorEmail,
                vendorContact = mdl.getVendorContact,
                vendorName = mdl.getVendorName,
                maxUnknownWords = mdl.getMaxUnknownWords,
                maxFreeWords = mdl.getMaxFreeWords,
                maxSuspiciousWords = mdl.getMaxSuspiciousWords,
                minWords = mdl.getMinWords,
                maxWords = mdl.getMaxWords,
                minTokens = mdl.getMinTokens,
                maxTokens = mdl.getMaxTokens,
                minNonStopwords = mdl.getMinNonStopwords,
                nonEnglishAllowed = mdl.isNonEnglishAllowed,
                notLatinCharsetAllowed = mdl.isNotLatinCharsetAllowed,
                swearWordsAllowed = mdl.isSwearWordsAllowed,
                noNounsAllowed = mdl.isNoNounsAllowed,
                permutateSynonyms = mdl.isPermutateSynonyms,
                dupSynonymsAllowed = mdl.isDupSynonymsAllowed,
                maxTotalSynonyms = mdl.getMaxTotalSynonyms,
                noUserTokensAllowed = mdl.isNoUserTokensAllowed,
                jiggleFactor = mdl.getJiggleFactor,
                minDateTokens = mdl.getMinDateTokens,
                maxDateTokens = mdl.getMaxDateTokens,
                minNumTokens = mdl.getMinNumTokens,
                maxNumTokens = mdl.getMaxNumTokens,
                minGeoTokens = mdl.getMinGeoTokens,
                maxGeoTokens = mdl.getMaxGeoTokens,
                minFunctionTokens = mdl.getMinFunctionTokens,
                maxFunctionTokens = mdl.getMaxFunctionTokens,
                metadata = mdl.getMetadata,
                additionalStopWords = mdl.getAdditionalStopWords,
                excludedStopWords = mdl.getExcludedStopWords,
                examples = mdl.getExamples,
                suspiciousWords = mdl.getSuspiciousWords,
                macros = mdl.getMacros,
                elements = mdl.getElements,
                descriptor = mdl.getDescriptor,
                solverFix
            )

        val isZip = path.endsWith(".gz")

        val filePath = if (isZip) path.dropRight(3) else path

        @throws[NCE]
        def serialize(objs: Object*): Unit = {
            try {
                managed(new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)))) acquireAndGet {
                    out ⇒ objs.foreach(out.writeObject)
                }
            }
            catch {
                case e: Exception ⇒ throw new NCE(s"Error writing file: $filePath", e)
            }
        }

        serialize(NCVersion.getCurrent, mdlFix)

        if (isZip)
            U.gzipPath(filePath, logger)

        logger.info(s"Model serialized " +
            s"[path=$path" +
            s", id=${mdl.getDescriptor.getId}" +
            s", name=${mdl.getDescriptor.getName}" +
            s", intentsCount=${intents.size}" +
            s", intents=${intents.map(p ⇒ s"*** ${p.getId} ****").mkString(" , ")}" +
            s"]"
        )
    }
}
