/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.mdo

import net.liftweb.json.JsonAST.{JObject, _}
import org.nlpcraft._
import org.nlpcraft.cache.NCCacheKey
import org.nlpcraft.json.NCJson
import org.nlpcraft.mdllib.NCToken
import org.nlpcraft.mdo.NCQueryStateMdo._
import org.nlpcraft.mdo.impl.{NCAnnotatedMdo, NCMdoEntity, NCMdoField}

import scala.collection.JavaConverters._
import scala.collection.Map

/**
  * Query state MDO.
  */
@NCMdoEntity(sql = false)
case class NCQueryStateMdo(
    @NCMdoField srvReqId: String,
    @NCMdoField test: Boolean,
    @NCMdoField userAgent: String,
    @NCMdoField companyId: Long,
    @NCMdoField dsId: Long,
    @NCMdoField modelId: String,
    @NCMdoField userId: Long,
    @NCMdoField email: String,
    @NCMdoField origText: String, // Text of the initial question.
    @NCMdoField origin: String,
    @NCMdoField createTstamp: Long, // Creation timestamp.
    @NCMdoField var updateTstamp: Long, // Last update timestamp.
    @NCMdoField var status: String,
    @NCMdoField var curateText: Option[String] = None, // Optional text after human curation.
    @NCMdoField var curateJson: Option[String] = None, // Optional request JSON explanation for curation.
    @NCMdoField var curateHint: Option[String] = None, // Optional hint after human curation.
    @NCMdoField var cacheKey: Option[NCCacheKey] = None, // Optional cache key.
    @NCMdoField(jsonConverter = "toJsonList") var tokens: Option[Seq[NCToken]] = None, // Optional tokens.
    @NCMdoField(jsonConverter = "toJsonList") var origTokens: Option[Seq[NCToken]] = None, // Optional tokens.
    @NCMdoField var cacheId: Option[Long] = None, // Optional cache ID.
    @NCMdoField var probeId: Option[String] = None, // Optional probe ID.
    @NCMdoField var lingUserId: Option[Long] = None, // Optional ID of the last linguist.
    @NCMdoField var lingOp: Option[String] = None, // Optional last linguist operation.
    @NCMdoField var resNotifyDue: Boolean = false, // Result email notification is due.
    @NCMdoField var curNotifyDue: Boolean = false, // Curation email notification is due.
    // Query OK (result, trivia, or talkback).
    @NCMdoField var resultType: Option[String] = None,
    @NCMdoField var resultBody: Option[String] = None,
    @NCMdoField var resultMetadata: Option[Map[String, Object]] = None,
    // Query ERROR (HTML message).
    @NCMdoField var error: Option[String] = None,
    // 'initial', 'trivia', 'talkback', 'reject', 'error', 'validation, 'curator', 'ok'.
    @NCMdoField var responseType: String = "RESP_INITIAL"
) extends NCAnnotatedMdo[NCQueryStateMdo] {
    /**
      * Abbreviated dataset for public API.
      *
      * @return
      */
    def pubApiJson(): NCJson = {
        import net.liftweb.json.JsonDSL._

        var js: JObject =
            ("srvReqId" → srvReqId) ~
            ("userAgent" → userAgent) ~
            ("userId" → userId) ~
            ("dsId" → dsId) ~
            ("probeId" → probeId) ~
            ("mdlId" → modelId) ~
            ("text" → origText) ~
            ("origin" → origin) ~
            ("status" → status) ~
            ("resType" → resultType) ~
            ("resBody" → resultBody) ~
            ("error" → error) ~
            ("responseType" → responseType) ~
            ("createTstamp" → createTstamp) ~
            ("updateTstamp" → updateTstamp)

        resultMetadata match {
            case Some(md) ⇒
                if (md.nonEmpty)
                    // Important to convert as Map, otherwise it is serialized as List.
                    js ~= (
                        "resMetadata" →
                            md.toMap.map { case (key, value) ⇒
                                value match {
                                    case v: java.lang.Long ⇒ key → JInt(BigInt.apply(v))
                                    case v: java.lang.Integer ⇒ key → JInt(BigInt.apply(v))
                                    case v: java.lang.Double ⇒ key → JDouble(v)
                                    case v: java.lang.Float ⇒ key → JDouble(v.doubleValue())
                                    case v: String ⇒ key → JString(v)
                                    case v: java.lang.Boolean ⇒ key → JBool(v)
                                    case _ ⇒ key → (if (value == null) JNull else JString(value.toString))
                                }
                            }
                        )
            case None ⇒ // No-op.
        }

        js
    }

    /**
      *
      * @param toksOpt
      * @return
      */
    def toJsonList(toksOpt: Option[Seq[NCToken]]): String =
        toksOpt match {
            case None ⇒ "[]"
            case Some(toks) ⇒ s"[${toks.map(mkTokenJson).mkString(",")}]"
        }
}

object NCQueryStateMdo {
    /**
      *
      * @param obj Object which should be copied.
      */
    def apply(obj: NCQueryStateMdo): NCQueryStateMdo = {
        NCQueryStateMdo(
            srvReqId = obj.srvReqId,
            test = obj.test,
            userAgent = obj.userAgent,
            companyId = obj.companyId,
            dsId = obj.dsId,
            modelId = obj.modelId,
            userId = obj.userId,
            email = obj.email,
            status = obj.status,
            origText = obj.origText,
            curateText = obj.curateText,
            curateHint = obj.curateHint,
            curateJson = obj.curateJson,
            cacheId = obj.cacheId,
            probeId = obj.probeId,
            origin = obj.origin,
            createTstamp = obj.createTstamp,
            updateTstamp = obj.updateTstamp,
            lingUserId = obj.lingUserId,
            lingOp = obj.lingOp,
            resultType = obj.resultType,
            resultBody = obj.resultBody,
            resultMetadata = obj.resultMetadata,
            error = obj.error,
            resNotifyDue = obj.resNotifyDue,
            curNotifyDue = obj.curNotifyDue,
            cacheKey = obj.cacheKey,
            tokens = obj.tokens,
            origTokens = obj.origTokens,
            responseType =  obj.responseType
        )
    }
    
    /**
      *
      * @param tok
      * @return
      */
    private def mkTokenJson(tok: NCToken): String =
        s"""
           | {
           |    "id": ${mkJsonVal(tok.getId)},
           |    "group": ${mkJsonVal(tok.getGroup)},
           |    "type": ${mkJsonVal(tok.getType)},
           |    "parentId": ${mkJsonVal(tok.getParentId)},
           |    "value": ${mkJsonVal(tok.getValue)},
           |    "group": ${mkJsonVal(tok.getGroup)},
           |    "isUserDefined": ${mkJsonVal(tok.isUserDefined)},
           |    "metadata": ${mkMapJson(tok.getMetadata.asScala.toMap)}
           | }
         """.stripMargin

    /**
      *
      * @param v
      * @return
      */
    private def mkJsonVal(v: Any): String =
        if (v == null)
            "null"
        else if (v.isInstanceOf[java.lang.Number] || v.isInstanceOf[java.lang.Boolean])
            v.toString
        else
            s""""${G.escapeJson(v.toString)}""""

    /**
      *
      * @param map
      * @return
      */
    private def mkMapJson(map: Map[String, java.io.Serializable]): String =
        // Skip very long line keys for prettier display...
        // Is this necessary?
        map.toList.filter(p ⇒ p._1 != "AVATAR_URL" && p._1 != "USER_AGENT").
            sortBy(_._1).map(t ⇒ s""""${t._1}": ${mkJsonVal(t._2)}""").mkString("{", ",", "}")
}
