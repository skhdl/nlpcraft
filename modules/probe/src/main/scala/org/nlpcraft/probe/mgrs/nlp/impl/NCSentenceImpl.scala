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

package org.nlpcraft.probe.mgrs.nlp.impl

import java.util.{Optional, List ⇒ JList}

import org.nlpcraft.mdllib.tools.impl._
import org.nlpcraft.mdllib._
import org.nlpcraft.nlp._
import org.nlpcraft.probe.NCModelDecorator

import scala.collection.JavaConverters._

/**
  *
  * @param mdl Model.
  * @param meta NLP server sentence metadata.
  * @param srvReqId Server request ID.
  * @param combs Variants.
  */
class NCSentenceImpl(
    mdl: NCModelDecorator,
    meta: NCMetadata,
    srvReqId: String,
    combs: Seq[Seq[NCNlpSentenceToken]]
) extends NCSentence {
    private val combToks = combs.map(toks ⇒ convert(toks, mdl, srvReqId))
    private val allToks = combToks.flatten.distinct

    override def isOwnerOf(tok: NCToken): Boolean = allToks.contains(tok)
    
    override lazy val getServerRequestId: String = srvReqId
    override lazy val variants: JList[NCVariant] = combToks.map(toks ⇒ new NCVariant(toks.asJava)).asJava
    override lazy val getNormalizedText: String = meta.getString("NORMTEXT")
    override lazy val getUserClientAgent: String = meta.getString("USER_AGENT")
    override lazy val getOrigin: String = meta.getString("ORIGIN")
    override lazy val getReceiveTimestamp: Long = meta.getLong("RECEIVE_TSTAMP")
    override lazy val getRemoteAddress: Optional[String] = meta.getStringOpt("REMOTE_ADDR")
    override lazy val getUserFirstName: String = meta.getString("FIRST_NAME")
    override lazy val getUserLastName: String = meta.getString("LAST_NAME")
    override lazy val getUserEmail: String = meta.getString("EMAIL")
    override lazy val getUserCompany: String = meta.getString("COMPANY_NAME")
    override lazy val getUserAvatarUrl: String = meta.getString("AVATAR_URL")
    override lazy val isUserAdmin: Boolean = meta.getBoolean("IS_ADMIN")
    override lazy val getUserSignupDate: Long = meta.getLong("SIGNUP_DATE")
    override lazy val getUserLastQTimestamp: Long = meta.getLong("LAST_Q_TSTAMP")
    override lazy val getUserTotalQs: Int = meta.getInteger("TOTAL_QS")
    override lazy val getTimezoneName: Optional[String] = meta.getStringOpt("TMZ_NAME")
    override lazy val getTimezoneAbbreviation: Optional[String] = meta.getStringOpt("TMZ_ABBR")
    override lazy val getLatitude: Optional[java.lang.Double] = meta.getDoubleOpt("LATITUDE")
    override lazy val getLongitude: Optional[java.lang.Double] = meta.getDoubleOpt("LONGITUDE")
    override lazy val getCountryCode: Optional[String] = meta.getStringOpt("COUNTRY_CODE")
    override lazy val getCountryName: Optional[String] = meta.getStringOpt("COUNTRY_NAME")
    override lazy val getRegionName: Optional[String] = meta.getStringOpt("REGION_NAME")
    override lazy val getCityName: Optional[String] = meta.getStringOpt("CITY")
    override lazy val getZipCode: Optional[String] = meta.getStringOpt("ZIP_CODE")
    override lazy val getMetroCode: Optional[java.lang.Long] = meta.getLongOpt("METRO_CODE")

    /**
      * Converts NLP sentence into sequence of model tokens.
      *
      * @param toks NLP sentences tokens.
      * @param mdl Model.
      * @param srvReqId Server request ID.
      */
    private def convert(
        toks: Seq[NCNlpSentenceToken],
        mdl: NCModelDecorator,
        srvReqId: String
    ): Seq[NCToken] =
        toks.map(nlpTok ⇒ {
            // nlp:nlp and some optional (after collapsing).
            require(nlpTok.size <= 2, s"Unexpected token [size=${nlpTok.size}, token=$nlpTok]")

            val nlpTokMeta =
                new NCMetadataImpl(nlpTok.flatMap(note ⇒
                    if (note.isUser)
                        Map.empty[String, Serializable]
                    else {
                        val typ = note.noteType

                        note.filter { case (key, _) ⇒
                            val lc = key.toLowerCase

                            // Skips internally used.
                            lc != "tokmaxindex" &&
                            lc != "tokminindex" &&
                            lc != "tokwordindexes" &&
                            lc != "tokwordlength"
                        }.map { case (name, value) ⇒
                            s"${typ.replaceAll("nlp:", "")}_$name".toUpperCase → value
                        }
                    }
                ).toMap.asJava)

            val usrNotes = nlpTok.filter(_.isUser)

            // No overlapping allowed at this point.
            require(usrNotes.size <= 1, s"Unexpected elements notes: $usrNotes")

            usrNotes.headOption match {
                case Some(usrNote) ⇒
                    require(mdl.elements.contains(usrNote.noteType), s"Element is not found: ${usrNote.noteType}")

                    val elm = mdl.elements(usrNote.noteType)

                    val tokMeta = new NCMetadataImpl

                    tokMeta.putAll(nlpTokMeta)

                    // Special synthetic meta data element.
                    tokMeta.put("NLP_FREEWORD", false)

                    new NCTokenImpl(
                        srvReqId,
                        elm.getId,
                        elm.getGroup,
                        elm.getType,
                        elm.getParentId,
                        usrNote.dataOpt("value").orNull,
                        tokMeta,
                        elm.getMetadata
                    )

                case None ⇒
                    require(nlpTok.size <= 2)

                    val note = nlpTok.toSeq.minBy(n ⇒ if (n.isNlp) 1 else 0)

                    // Special synthetic meta data element.
                    nlpTokMeta.put("NLP_FREEWORD", !nlpTokMeta.getBoolean("NLP_STOPWORD") && note.isNlp)

                    new NCTokenImpl(
                        srvReqId,
                        note.noteType, // Use NLP note type as synthetic element ID.
                        note.noteType, // Use NLP note type as synthetic element group.
                        "STRING",
                        null,
                        null,
                        nlpTokMeta,
                        new NCMetadataImpl()
                    )
            }
        })
}
