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

import java.util
import java.util.function.Consumer

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.model.intent.NCIntentSolver
import org.nlpcraft.model.{NCElement, NCMetadata, NCModel, NCModelDescriptor, NCProbeContext, NCQueryContext, NCQueryResult}

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
