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

package org.nlpcraft.examples.echo

import java.util.Optional

import org.nlpcraft.model._
import org.nlpcraft.common._
import org.nlpcraft.model.builder.NCModelBuilder

import scala.collection.JavaConverters._
import scala.collection.Seq

/**
  * Echo example model provider.
  * <p>
  * This example for any user input returns JSON representation of the query context
  * corresponding to that input. This is a simple demonstration of the JSON output
  * and of most of the NLPCraft-provided data that a user defined model can operate on.
  */
class EchoModel extends NCModelProviderAdapter {
    // Any immutable user defined ID.
    private final val MODEL_ID = "nlpcraft.echo.ex"
    
    /**
      * Escapes given string for JSON according to RFC 4627 http://www.ietf.org/rfc/rfc4627.txt.
      *
      * @param s String to escape.
      * @return Escaped string.
      */
    def escapeJson(s: String): String = {
        val len = s.length
        
        if (len == 0)
            ""
        else {
            val sb = new StringBuilder
            
            for (ch ← s.toCharArray)
                ch match {
                    case '\\' | '"' ⇒ sb += '\\' += ch
                    case '/' ⇒ sb += '\\' += ch
                    case '\b' ⇒ sb ++= "\\b"
                    case '\t' ⇒ sb ++= "\\t"
                    case '\n' ⇒ sb ++= "\\n"
                    case '\f' ⇒ sb ++= "\\f"
                    case '\r' ⇒ sb ++= "\\r"
                    case _ ⇒
                        if (ch < ' ') {
                            val t = "000" + Integer.toHexString(ch)
                            
                            sb ++= "\\u" ++= t.substring(t.length - 4)
                        }
                        else
                            sb += ch
                }
            
            sb.toString()
        }
    }
    
    /**
      * Converts Java serializable into JSON value.
      *
      * @param v Value to convert.
      * @return Converted value.
      */
    private def mkJsonVal(v: Any): String = {
        val x = v match {
            case opt: Optional[_] ⇒ opt.get
            case _ ⇒ v
        }
        
        if (x == null)
            "null"
        else if (x.isInstanceOf[java.lang.Number] || x.isInstanceOf[java.lang.Boolean])
            x.toString
        else
            s""""${escapeJson(x.toString)}""""
    }

    /**
      * Represents JSON values as JSON array.
      *
      * @param jss JSON values.
      * @return JSON array.
      */
    private def mkJsonVals(jss: Seq[String]): String = s"[${jss.mkString(",")}]"

    /**
      * Converts Java map into JSON value.
      *
      * @param map Map to convert.
      * @return Converted value.
      */
    private def mkMapJson(map: Map[String, java.io.Serializable]): String =
        // Skip very long line keys for prettier display...
        // Is this necessary?
        map.toList.filter(p ⇒ p._1 != "AVATAR_URL" && p._1 != "USER_AGENT").
            sortBy(_._1).map(t ⇒ s""""${t._1}": ${mkJsonVal(t._2)}""").mkString("{", ",", "}")
    
    /**
      * Makes JSON presentation of data source from given query context.
      *
      * @param ctx Query context.
      * @return JSON presentation of data source.
      */
    private def mkDataSourceJson(ctx: NCQueryContext): String = {
        val ds = ctx.getDataSource
    
        // Hand-rolled JSON for simplicity...
        s"""
           | {
           |    "name": ${mkJsonVal(ds.getName)},
           |    "description": ${mkJsonVal(ds.getDescription)},
           |    "config": ${mkJsonVal(ds.getConfig)}
           | }
         """.stripMargin
    }
    
    /**
      * Makes JSON presentation of the given token.
      *
      * @param tok A token.
      * @return JSON presentation of data source.
      */
    private def mkTokenJson(tok: NCToken): String =
        // Hand-rolled JSON for simplicity...
        s"""
           | {
           |    "id": ${mkJsonVal(tok.getId)},
           |    "group": ${mkJsonVal(tok.getGroup)},
           |    "parentId": ${mkJsonVal(tok.getParentId)},
           |    "value": ${mkJsonVal(tok.getValue)},
           |    "group": ${mkJsonVal(tok.getGroup)},
           |    "isUserDefined": ${mkJsonVal(tok.isUserDefined)},
           |    "metadata": ${mkMapJson(tok.getMetadata.asScala.toMap)}
           | }
         """.stripMargin

    /**
      * Makes JSON presentation of the NLP sentence from given query context.
      *
      * @param ctx Query context.
      * @return JSON presentation of NLP sentence.
      */
    private def mkSentenceJson(ctx: NCQueryContext): String = {
        val sen = ctx.getSentence
    
        // Hand-rolled JSON for simplicity...
        s"""
           | {
           |    "normalizedText": ${mkJsonVal(sen.getNormalizedText)},
           |    "srvReqId": ${mkJsonVal(sen.getServerRequestId)},
           |    "receiveTimestamp": ${mkJsonVal(sen.getReceiveTimestamp)},
           |    "userFirstName": ${mkJsonVal(sen.getUserFirstName)},
           |    "userLastName": ${mkJsonVal(sen.getUserLastName)},
           |    "userEmail": ${mkJsonVal(sen.getUserEmail)},
           |    "isUserAdmin": ${mkJsonVal(sen.isUserAdmin)},
           |    "userSignupDate": ${mkJsonVal(sen.getUserSignupDate)},
           |    "variants":
           |        ${mkJsonVals(sen.getVariants.asScala.map(p ⇒ mkJsonVals(p.getTokens.asScala.map(mkTokenJson))))}
           | }
         """.stripMargin
    }

    setup(
        // Using inline JSON model.
        NCModelBuilder.newJsonStringModel(
            s"""
              | {
              |    "id": "$MODEL_ID",
              |    "name": "Echo Example Model",
              |    "version": "1.0",
              |    "description": "Echo example model.",
              |    "vendorName": "NLPCraft",
              |    "vendorUrl": "https://www.nlpcraft.org",
              |    "vendorContact": "Support",
              |    "vendorEmail": "info@nlpcraft.org",
              |    "docsUrl": "https://www.nlpcraft.org",
              |    "allowNoUserTokens": true
              | }
            """.stripMargin)
            .setQueryFunction((ctx: NCQueryContext) ⇒ {
                // Hand-rolled JSON for simplicity...
                NCQueryResult.json(
                    s"""
                       |{
                       |    "srvReqId": ${mkJsonVal(ctx.getServerRequestId)},
                       |    "dataSource": ${mkDataSourceJson(ctx)},
                       |    "sentence": ${mkSentenceJson(ctx)}
                       |}
                     """.stripMargin
                )
            })
            .build()
    )
}
