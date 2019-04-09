package org.nlpcraft.model.tools.dump.scala

import java.io.{BufferedOutputStream, File, FileOutputStream, ObjectOutputStream}
import java.time.format.DateTimeFormatter
import java.util
import java.util.zip.GZIPOutputStream

import scala.collection.JavaConverters._
import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.common.version.{NCVersion, NCVersionManager}
import org.nlpcraft.common.{NCE, U}
import org.nlpcraft.model.intent.NCIntentSolver.IntentCallback
import org.nlpcraft.model.intent.{NCIntentSolver, NCIntentSolverContext}
import org.nlpcraft.model.{NCElement, NCMetadata, NCModel, NCModelDescriptor, NCProbeContext, NCQueryContext, NCQueryResult}
import resource.managed

/**
  * Data model dump writer.
  */
object NCDumpWriterScala extends LazyLogging {
    private final val FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH.mm.ss")

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
        solver: NCIntentSolver
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

        override def discard(): Unit = ()
        override def initialize(ctx: NCProbeContext): Unit = ()
    }

    /**
      *
      * @param mdl
      * @param solver
      * @param dirPath
      */
    @throws[NCE]
    def write(mdl: NCModel, solver: NCIntentSolver, dirPath: String): String = {
        val dirFile = new File(dirPath)

        if (!dirFile.exists() || !dirFile.isDirectory)
            throw new NCE(s"$dirPath path is not a directory.")

        val file = s"${mdl.getDescriptor.getId}-${U.nowUtc().format(FMT)}.gz"
        val filePath = s"$dirFile/$file"

        val ver = NCVersion.getCurrent.version

        val solverFix = new NCIntentSolver(
            s"Model dump intent solver [name=${solver.getName}, version=$ver]", null
        )

        val intents = solver.getIntents.asScala
        val mdlId = mdl.getDescriptor.getId

        intents.foreach(i ⇒
            solverFix.addIntent(
                i,
                new IntentCallback {
                    override def apply(t: NCIntentSolverContext): NCQueryResult =
                        NCQueryResult.json(
                            s"""
                            | {
                            |    "modelId": "$mdlId",
                            |    "intentId": "${i.getId}",
                            |    "modelFile": "$file"
                            | }
                            """.stripMargin
                        )
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

        val info = new util.HashMap[String, String]()

        NCVersionManager.getVersionInfo.foreach { case (k, v) ⇒ info.put(k, if (v != null) v.toString else null) }

        try {
            managed(
                new ObjectOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(filePath))))
            ) acquireAndGet {
                out ⇒
                    out.writeObject(ver)
                    out.writeObject(info)
                    out.writeObject(mdlFix)
            }
        }
        catch {
            case e: Exception ⇒ throw new NCE(s"Error writing file: $filePath", e)
        }

        logger.info(s"Data model dump exported " +
            s"[path=$filePath" +
            s", id=$mdlId" +
            s", name=${mdl.getDescriptor.getName}" +
            s", intentsCount=${intents.size}" +
            s", intents=${intents.map(_.getId).mkString(", ")}" +
            s"]"
        )

        file
    }
}
