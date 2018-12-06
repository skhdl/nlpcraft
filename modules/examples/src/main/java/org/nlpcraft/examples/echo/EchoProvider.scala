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

package org.nlpcraft.examples.echo

import java.util.Optional

import org.nlpcraft.mdllib._
import org.nlpcraft.mdllib.tools.builder.NCModelBuilder
import org.nlpcraft.mdllib.tools.scala.NCScalaSupport._

import scala.collection.JavaConverters._
import scala.collection.Seq

/**
  * Echo example model provider.
  * <p>
  * This example for any user input replies with JSON representation query context
  * corresponding to that input. This is a simple demonstration of the JSON output
  * and all NlpCraft-provided information that a user defined model can operate on.
  */
@NCActiveModelProvider
class EchoProvider extends NCModelProviderAdapter {
    // Any immutable user defined ID.
    private final val MODEL_ID = "nlpcraft.echo.ex"
    
    /**
      * Escapes given string for JSON according to RFC 4627 http://www.ietf.org/rfc/rfc4627.txt.
      *
      * @param s String to escape.
      * @return Escaped string.
      */
    // TODO: instead of coding it here - you can borrow it from some well known library...
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
           |    "type": ${mkJsonVal(tok.getType)},
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
           |    "userCompany": ${mkJsonVal(sen.getUserCompany)},
           |    "userSignupDate": ${mkJsonVal(sen.getUserSignupDate)},
           |    "userTotalQs": ${mkJsonVal(sen.getUserTotalQs)},
           |    "userLastQTstamp": ${mkJsonVal(sen.getUserLastQTimestamp)},
           |    "countryName": ${mkJsonVal(sen.getCountryName)},
           |    "countryCode": ${mkJsonVal(sen.getCountryCode)},
           |    "regionName": ${mkJsonVal(sen.getRegionName)},
           |    "cityName": ${mkJsonVal(sen.getCityName)},
           |    "metroCode": ${mkJsonVal(sen.getMetroCode)},
           |    "origin": ${mkJsonVal(sen.getOrigin)},
           |    "remoteAddress": ${mkJsonVal(sen.getRemoteAddress)},
           |    "timezoneName": ${mkJsonVal(sen.getTimezoneName)},
           |    "timezoneAbbr": ${mkJsonVal(sen.getTimezoneAbbreviation)},
           |    "latitude": ${mkJsonVal(sen.getLatitude)},
           |    "longitude": ${mkJsonVal(sen.getLongitude)},
           |    "variants":
           |        ${mkJsonVals(sen.variants().asScala.map(p ⇒ mkJsonVals(p.getTokens.asScala.map(mkTokenJson))))}
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
              |    "vendorName": "NlpCraft",
              |    "vendorUrl": "https://www.nlpcraft.org",
              |    "vendorContact": "Support",
              |    "vendorEmail": "info@nlpcraft.org",
              |    "docsUrl": "https://www.nlpcraft.org",
              |    "allowNoUserTokens": true,
              |    "defaultTrivia": false
              | }
            """.stripMargin)
            .setQueryFunction((ctx: NCQueryContext) ⇒ {
                // Hand-rolled JSON for simplicity...
                NCQueryResult.json(
                    s"""
                       |{
                       |    "srvReqId": ${mkJsonVal(ctx.getServerRequestId)},
                       |    "dataSource": ${mkDataSourceJson(ctx)},
                       |    "sentence": ${mkSentenceJson(ctx)},
                       |    "hint": ${mkJsonVal(ctx.getHint)}
                       |}
                     """.stripMargin
                )
            })
            .build()
    )
}
