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
import java.util
import java.util.function.Consumer

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.common._
import org.nlpcraft.common.version.{NCVersion, NCVersionManager}
import org.nlpcraft.model.intent.NCIntentSolver.IntentCallback
import org.nlpcraft.model.intent.{NCIntentSolver, NCIntentSolverContext}
import org.nlpcraft.model.{NCElement, NCMetadata, NCModel, NCModelDescriptor, NCProbeContext, NCQueryContext, NCQueryResult}
import resource.managed

import scala.collection.JavaConverters._

/**
  * Dump writer.
  */
object NCDumpWriter extends LazyLogging {
    /**
      * Dump model wrapper.
      */
    case class NCDumpModel(
        description: String,
        docsUrl: String,
        vendorUrl: String,
        vendorEmail: String,
        vendorContact: String,
        vendorName: String,
        maxUnknownWords: Int,
        maxFreeWords: Int,
        maxSuspiciousWords: Int,
        minWords: Int,
        maxWords: Int,
        minTokens: Int,
        maxTokens: Int,
        minNonStopwords: Int,
        nonEnglishAllowed: Boolean,
        notLatinCharsetAllowed: Boolean,
        swearWordsAllowed: Boolean,
        noNounsAllowed: Boolean,
        permutateSynonyms: Boolean,
        dupSynonymsAllowed: Boolean,
        maxTotalSynonyms: Int,
        noUserTokensAllowed: Boolean,
        jiggleFactor: Int,
        minDateTokens: Int,
        maxDateTokens: Int,
        minNumTokens: Int,
        maxNumTokens: Int,
        minGeoTokens: Int,
        maxGeoTokens: Int,
        minFunctionTokens: Int,
        maxFunctionTokens: Int,
        metadata: NCMetadata,
        additionalStopWords: util.Set[String],
        excludedStopWords: util.Set[String],
        examples: util.Set[String],
        suspiciousWords: util.Set[String],
        macros: util.Map[String, String],
        elements: util.Set[NCElement],
        descriptor: NCModelDescriptor,
        solver: NCIntentSolver,
        var initFun: Consumer[NCProbeContext] = null,
        var discardFun: Runnable = null
    ) extends NCModel with java.io.Serializable with LazyLogging {
        override def getDescription: String = description
        override def getDocsUrl: String = docsUrl
        override def getVendorUrl: String = vendorUrl
        override def getVendorEmail: String = vendorEmail
        override def getVendorContact: String = vendorContact
        override def getVendorName: String = vendorName
        override def getMaxUnknownWords: Int = maxUnknownWords
        override def getMaxFreeWords: Int = maxFreeWords
        override def getMaxSuspiciousWords: Int = maxSuspiciousWords
        override def getMinWords: Int = minWords
        override def getMaxWords: Int = maxWords
        override def getMinTokens: Int = minTokens
        override def getMaxTokens: Int = maxTokens
        override def getMinNonStopwords: Int = minNonStopwords
        override def isNonEnglishAllowed: Boolean = nonEnglishAllowed
        override def isNotLatinCharsetAllowed: Boolean = notLatinCharsetAllowed
        override def isSwearWordsAllowed: Boolean = swearWordsAllowed
        override def isNoNounsAllowed: Boolean = noNounsAllowed
        override def isPermutateSynonyms: Boolean = permutateSynonyms
        override def isDupSynonymsAllowed: Boolean = dupSynonymsAllowed
        override def getMaxTotalSynonyms: Int = maxTotalSynonyms
        override def isNoUserTokensAllowed: Boolean = noUserTokensAllowed
        override def getJiggleFactor: Int = jiggleFactor
        override def getMinDateTokens: Int = minDateTokens
        override def getMaxDateTokens: Int = maxDateTokens
        override def getMinNumTokens: Int = minNumTokens
        override def getMaxNumTokens: Int = maxNumTokens
        override def getMinGeoTokens: Int = minGeoTokens
        override def getMaxGeoTokens: Int = maxGeoTokens
        override def getMinFunctionTokens: Int = minFunctionTokens
        override def getMaxFunctionTokens: Int = maxFunctionTokens
        override def getMetadata: NCMetadata = metadata
        override def getAdditionalStopWords: util.Set[String] = additionalStopWords
        override def getExcludedStopWords: util.Set[String] = excludedStopWords
        override def getExamples: util.Set[String] = examples
        override def getSuspiciousWords: util.Set[String] = suspiciousWords
        override def getMacros: util.Map[String, String] = macros
        override def getElements: util.Set[NCElement] = elements
        override def getDescriptor: NCModelDescriptor = descriptor
        override def query(ctx: NCQueryContext): NCQueryResult = solver.solve(ctx)

        override def discard(): Unit =
            if (discardFun != null)
                discardFun.run()
            else
                logger.warn(s"'Discard' function is not defined for model: ${descriptor.getId}")

        override def initialize(ctx: NCProbeContext): Unit =
            if (initFun != null)
                initFun.accept(ctx)
            else
                logger.warn(s"'Initialize' function is not defined for model: ${descriptor.getId}")
    }

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

        val m = new util.HashMap[String, String]()

        NCVersionManager.getVersionInfo().foreach { case (k, v) ⇒ m.put(k, if (v != null) v.toString else null) }

        serialize(
            NCVersion.getCurrent.version,
            m,
            mdlFix
        )

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
