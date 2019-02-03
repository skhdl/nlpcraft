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

package org.nlpcraft.probe.mgrs.nlp

import java.io.Serializable
import java.util.concurrent.Executors
import java.util.function.Predicate

import org.nlpcraft.mdllib._
import org.nlpcraft.mdllib.tools.impl.NCMetadataImpl
import org.nlpcraft.nlp.NCNlpSentence
import org.nlpcraft.nlp.log.NCNlpAsciiLogger
import org.nlpcraft.probe.mgrs.conn.NCProbeConnectionManager
import org.nlpcraft.probe.mgrs.nlp.conversation.NCConversationManager
import org.nlpcraft.probe.mgrs.nlp.enrichers.coordinates.NCCoordinatesEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.context.NCContextEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.dictionary.NCDictionaryEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.function.NCFunctionEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.model.NCModelEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.stopword.NCStopWordEnricher
import org.nlpcraft.probe.mgrs.nlp.enrichers.suspicious.NCSuspiciousNounsEnricher
import org.nlpcraft.probe.mgrs.nlp.impl._
import org.nlpcraft.probe.mgrs.nlp.post._
import org.nlpcraft.probe.mgrs.nlp.pre._
import org.nlpcraft.probe._
import org.nlpcraft.probe.mgrs.model.NCModelManager
import org.nlpcraft._

import scala.collection._
import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

/**
  * Probe NLP manager.
  */
object NCProbeNlpManager extends NCProbeManager("NLP manager") with NCDebug {
    private final val EC = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(8 * Runtime.getRuntime.availableProcessors())
    )

    // Maximum size of the result body.
    private final val MAX_RES_BODY_LENGTH = 1024 * 1024 // 1MB.

    /**
      * Processes 'ask' request from probe server.
      *
      * @param srvReqId Server request ID.
      * @param txt Text.
      * @param nlpSen NLP sentence.
      * @param usrId User ID.
      * @param senMeta Sentence meta data.
      * @param dsId Datasource ID.
      * @param dsModelId Model ID.
      * @param dsName Datasource name.
      * @param dsDesc Datasource description.
      * @param dsModelCfg Datasource model config.
      * @param test Test flag.
      */
    @throws[NCE]
    def ask(
        srvReqId: String,
        txt: String,
        nlpSen: NCNlpSentence,
        usrId: Long,
        senMeta: Map[String, Serializable],
        dsId: Long,
        dsModelId: String,
        dsName: String,
        dsDesc: String,
        dsModelCfg: String,
        test: Boolean
    ): Unit = {
        ensureStarted()

        try
            ask0(
                srvReqId,
                txt,
                nlpSen,
                usrId,
                senMeta,
                dsId,
                dsModelId,
                dsName,
                dsDesc,
                dsModelCfg,
                test
            )
        catch {
            case e: Throwable ⇒
                logger.error("Failed to process request.", e)
                
                val msg = NCProbeMessage("P2S_ASK_RESULT",
                    "srvReqId" → srvReqId,
                    "error" → "Processing failed due to a system error.",
                    "dsId" → dsId,
                    "dsModelId" → dsModelId,
                    "txt" → txt
                )
                
                NCProbeConnectionManager.send(msg)
        }
    }

    /**
      * Adds optional parameter to the message.
      *
      * @param msg Message.
      * @param name Parameter name.
      * @param vOpt Optional value.
      */
    private def addOptional(msg: NCProbeMessage, name: String, vOpt: Option[Serializable]): Unit =
        if (vOpt.isDefined)
            msg += name → vOpt.get

    /**
      * Processes 'ask' request from probe server.
      *
      * @param srvReqId Server request ID.
      * @param txt Text.
      * @param nlpSen NLP sentence.
      * @param usrId User ID.
      * @param senMeta Sentence meta data.
      * @param dsId Data source ID.
      * @param dsModelId Model ID.
      * @param dsName Data source name.
      * @param dsDesc Data source description.
      * @param dsModelCfg Data source model config.
      * @param test Test flag.
      */
    @throws[NCE]
    private def ask0(
        srvReqId: String,
        txt: String,
        nlpSen: NCNlpSentence,
        usrId: Long,
        senMeta: Map[String, Serializable],
        dsId: Long,
        dsModelId: String,
        dsName: String,
        dsDesc: String,
        dsModelCfg: String,
        test: Boolean
    ): Unit = {
        if (!IS_PROBE_SILENT)
            logger.info(s"New sentence received: ${nlpSen.text}")
    
        /**
          *
          * @param code Pre or post checker error code.
          */
        def errorMsg(code: String): String =
            code match {
                case "MAX_UNKNOWN_WORDS" ⇒ "Too many unknown words."
                case "MAX_FREE_WORDS" ⇒ "Sentence is too complex."
                case "MAX_SUSPICIOUS_WORDS" ⇒ "Too many suspicious or unrelated words."
                case "ALLOW_SWEAR_WORDS" ⇒ "Swear words are not allowed."
                case "ALLOW_NO_NOUNS" ⇒ "Sentence contains no nouns."
                case "ALLOW_NON_LATIN_CHARSET" ⇒ "Only latin charset is supported."
                case "ALLOW_NON_ENGLISH" ⇒ "Only english language is supported."
                case "ALLOW_NO_USER_TOKENS" ⇒ "Sentence seems unrelated to data source."
                case "MIN_WORDS" ⇒ "Sentence is too short."
                case "MIN_NON_STOPWORDS" ⇒ "Sentence is ambiguous."
                case "MIN_TOKENS" ⇒ "Sentence is too short."
                case "MAX_TOKENS" ⇒ "Sentence is too long."
                case "MAX_GEO_TOKENS" ⇒ "Too many geographical locations detected."
                case "MIN_GEO_TOKENS" ⇒ "Too few geographical locations detected."
                case "MAX_DATE_TOKENS" ⇒ "Too many dates detected."
                case "MIN_DATE_TOKENS" ⇒ "Too few dates detected."
                case "MAX_NUM_TOKENS" ⇒ "Too many numbers detected."
                case "MIN_NUM_TOKENS" ⇒ "Too few numbers detected."
                case "MAX_FUNCTION_TOKENS" ⇒ "Too many functions detected."
                case "MIN_FUNCTION_TOKENS" ⇒ "Too few functions detected."
                case _ ⇒ s"System error: $code."
            }

        /**
          * Makes response.
          *
          * @param resType Result type.
          * @param resBody Result body.
          * @param errMsg Error message.
          * @param msgName Message name.
          */
        def respond(
            resType: Option[String],
            resBody: Option[String],
            errMsg: Option[String],
            msgName: String
        ): Unit = {
            require(errMsg.isDefined || (resType.isDefined && resBody.isDefined))

            val msg = NCProbeMessage(msgName)

            msg += "srvReqId" → srvReqId
            msg += "dsId" → dsId
            msg += "dsModelId" → dsModelId
            msg += "txt" → txt
            msg += "test" → test
            
            if (resBody.isDefined && resBody.get.length > MAX_RES_BODY_LENGTH)
                addOptional(msg, "error", Some("Result is too big. Model results must to be corrected."))
            else {
                addOptional(msg, "error", errMsg)
                addOptional(msg, "resType", resType)
                addOptional(msg, "resBody", resBody)
            }

            NCProbeConnectionManager.send(msg)
            
            if (errMsg.isEmpty)
                logger.trace(s"OK response $msgName [srvReqId=$srvReqId, type=${resType.getOrElse("")}]")
            else
                logger.trace(s"REJECT response $msgName [srvReqId=$srvReqId, response=${errMsg.get}]")
        }

        val mdl = NCModelManager.getModel(dsModelId).getOrElse(throw new NCE(s"Model not found: $dsModelId"))
        
        try
            NCNlpPreChecker.validate(mdl, nlpSen)
        catch {
            case e: NCNlpPreException ⇒
                val errMsg = errorMsg(e.status)

                logger.error(s"Pre-enrichment validation: $errMsg ")

                respond(
                    None,
                    None,
                    Some(errMsg),
                    "P2S_ASK_RESULT"
                )

                return
        }

        // Order is important!
        NCStopWordEnricher.enrich(mdl, nlpSen)
        NCModelEnricher.enrich(mdl, nlpSen)
        NCFunctionEnricher.enrich(mdl, nlpSen)
        NCCoordinatesEnricher.enrich(mdl, nlpSen)
        NCSuspiciousNounsEnricher.enrich(mdl, nlpSen)

        var senSeq =
            NCPostEnrichCollapser.collapse(mdl, nlpSen)
            .flatMap(sen ⇒ {
                NCPostEnricher.postEnrich(mdl, sen)
                NCPostEnrichCollapser.collapse(mdl, sen)
            })

        senSeq.foreach(sen ⇒ {
            NCContextEnricher.enrich(mdl, sen)
            NCDictionaryEnricher.enrich(mdl, sen)
        })

        // Collapse again.
        senSeq = senSeq.flatMap(p ⇒ NCPostEnrichCollapser.collapse(mdl, p))

        if (!IS_PROBE_SILENT) {
            val sz = senSeq.size

            // Print here because validation can change sentence.
            senSeq.zipWithIndex.foreach(p ⇒
                NCNlpAsciiLogger.prepareTable(p._1).info(logger,
                    Some(s"Sentence variant (#${p._2 + 1} of $sz) for: ${p._1.text}")))
        }

        // Final validation before execution.
        try
            senSeq.foreach(sen ⇒ NCPostChecker.validate(mdl, sen))
        catch {
            case e: NCPostException ⇒
                val errMsg = errorMsg(e.code)

                logger.error(s"Post-enrichment validation: $errMsg ")

                respond(
                    None,
                    None,
                    Some(errorMsg(e.code)),
                    "P2S_ASK_RESULT"
                )

                return
        }

        val conv = NCConversationManager.get(usrId, dsId)

        // Update STM and recalculate context.
        conv.update()

        if (!IS_PROBE_SILENT)
            conv.ack()

        val unitedSen =
            new NCSentenceImpl(mdl, new NCMetadataImpl(senMeta.asJava), srvReqId, senSeq)

        // Create model query context.
        val qryCtx: NCQueryContext = new NCQueryContext {
            override val getDataSource: NCDataSource = new NCDataSource {
                override lazy val getDescription: String = dsDesc
                override lazy val getName: String = dsName
                override lazy val getConfig: String = dsModelCfg
            }

            override lazy val getSentence: NCSentence = unitedSen
            override lazy val getModel: NCModel = mdl.model
            override lazy val getServerRequestId: String = srvReqId

            override lazy val getConversationContext: NCConversationContext = new NCConversationContext {
                override def getTokens: java.util.HashSet[NCToken] = conv.tokens
                override def clear(filter: Predicate[NCToken]): Unit = conv.clear(filter)
            }
        }

        // Execute model query asynchronously.
        G.asFuture(
            _ ⇒ {
                val res = mdl.model.query(qryCtx)

                if (res == null)
                    throw new IllegalStateException("Result cannot be null.")
                if (res.getBody == null)
                    throw new IllegalStateException("Result body cannot be null.")
                if (res.getType == null)
                    throw new IllegalStateException("Result type cannot be null.")

                res
            },
            {
                case e: NCRejection ⇒
                    logger.info(s"Rejection [srvReqId=$srvReqId, msg=${e.getMessage}]")

                    if (e.getCause != null)
                        logger.info(s"Rejection cause:", e.getCause)

                    respond(
                        None,
                        None,
                        Some(e.getMessage), // User provided rejection message.
                        "P2S_ASK_RESULT"
                    )

                case e: Throwable ⇒
                    logger.error(s"Unexpected error for: $srvReqId", e)

                    respond(
                        None,
                        None,
                        Some("Processing failed with unexpected error."), // System error message.
                        "P2S_ASK_RESULT"
                    )
            },
            (res: NCQueryResult) ⇒ {
                respond(
                    Some(res.getType),
                    Some(res.getBody),
                    None,
                    "P2S_ASK_RESULT"
                )
            }
        )(EC)
    }
}
