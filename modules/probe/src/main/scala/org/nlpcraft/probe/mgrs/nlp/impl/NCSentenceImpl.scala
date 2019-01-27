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
 * Software:    NlpCraft
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

package org.nlpcraft.probe.mgrs.nlp.impl

import java.util.Optional

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
    override lazy val variants: java.util.List[NCVariant] =
        combToks.map(toks ⇒ new NCVariantImpl(toks.asJava).asInstanceOf[NCVariant]).asJava
    override lazy val getNormalizedText: String = meta.getString("NORMTEXT")
    override lazy val getReceiveTimestamp: Long = meta.getLong("RECEIVE_TSTAMP") // UTC.
    override lazy val getUserFirstName: String = meta.getString("FIRST_NAME")
    override lazy val getUserLastName: String = meta.getString("LAST_NAME")
    override lazy val getUserEmail: String = meta.getString("EMAIL")
    override lazy val getUserAvatarUrl: String = meta.getString("AVATAR_URL")
    override lazy val isUserAdmin: Boolean = meta.getBoolean("IS_ADMIN")
    override lazy val getUserSignupDate: Long = meta.getLong("SIGNUP_DATE")
    override lazy val getUserClientAgent: Optional[String] = meta.getStringOpt("USER_AGENT")
    override lazy val getRemoteAddress: Optional[String] = meta.getStringOpt("REMOTE_ADDR")

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
                        null,
                        null,
                        nlpTokMeta,
                        new NCMetadataImpl()
                    )
            }
        })
}
