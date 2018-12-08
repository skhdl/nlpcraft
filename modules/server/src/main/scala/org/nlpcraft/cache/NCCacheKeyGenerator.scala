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

package org.nlpcraft.cache

import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonDSL._
import org.nlpcraft.ascii.NCAsciiTable
import org.nlpcraft.cache.NCCacheKeyType._
import org.nlpcraft.json.NCJson
//import org.nlpcraft.mdllib.NCToken
import org.nlpcraft.nlp.wordnet.NCWordNet
import org.nlpcraft.{NCE, NCLifecycle}

import scala.collection._

/**
 * Cache generator.
 */
object NCCacheKeyGenerator extends NCLifecycle("SERVER cache store key generator") {
    implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats

    case class KeyDescriptor(keyType: NCCacheKeyType, values: Seq[String], sort: Boolean, name: String)
    case class Word(
        lemma: Option[String],
        stem: Option[String],
        pos: Option[String],
        tokenKey: Option[String],
        isQuoted: Boolean
    ) {
        def isArtificial: Boolean = lemma.isEmpty
    }

    object Word {
        def apply(lemma: String, stem: String, pos: String, tokenKey: Option[String], isQuoted: Boolean): Word =
            new Word(Some(lemma), Some(stem), Some(pos), tokenKey, isQuoted)

        // Artificial word. It's used for added tokens which are not related with real sentence's words.
        def apply(tokenKey: String): Word =
            new Word(None, None, None, Some(tokenKey), isQuoted = false)
    }

    /**
     * Gets key token.
     *
     * @param t Token.
     */
    @throws[NCE]
    private def getKeyToken(t: NCToken): String = {
        val id = t.getId

        require(id != null)

        def x(s: String): String = s"${id}_$s"

        id match {
            case "nlp:geo" ⇒ x(t.getMetadata.getString("GEO_KIND"))
            case "nlp:function" ⇒ x(t.getMetadata.getString("FUNCTION_TYPE"))
            case _ ⇒ id
        }
    }

    /**
     * Makes key value.
     *
     * @param t Key type.
     * @param sort Flag which indicates is key sorted or not.
     * @param v String value.
     */
    private[cache] def mkKeyValue(t: NCCacheKeyType, sort: Boolean, v: String): String = s"$t $v"

    /**
     * Generates compound key.
     *
     * @param hs Holders.
     * @param kd Direct keys.
     * @param ks Synonym keys.
     */
    private def generate(hs: Seq[Word], kd: Seq[KeyDescriptor], ks: Seq[KeyDescriptor]): NCCacheKey = {
        val tbl = NCAsciiTable()

        val jssDirect = mutable.ArrayBuffer.empty[NCJson]
        val keysDirect = mutable.ArrayBuffer.empty[NCCacheSingleKey]

        val jssSyns = mutable.ArrayBuffer.empty[NCJson]
        val keysSyns = mutable.ArrayBuffer.empty[NCCacheSingleKey]

        case class KeyImpl(kd: KeyDescriptor) extends NCCacheSingleKey {
            override def keyType: NCCacheKeyType = kd.keyType
            override def keyValue: String =
                mkKeyValue(kd.keyType, sorted, (if (kd.sort) kd.values.sorted else kd.values).mkString("|")).
                    replaceAll("nlp:", "nlp_").
                    toUpperCase
            override def sorted: Boolean = kd.sort
            override def toJson: NCJson = ("keyType" → kd.keyType.toString) ~ ("keyValue" → keyValue) ~ ("sorted" → kd.sort)
            override def toString: String = keyValue
            override def hashCode(): Int = keyValue.hashCode
            override def equals(obj: scala.Any): Boolean = obj match {
                case c: KeyImpl ⇒ c.keyValue == keyValue
                case _ ⇒ false
            }
        }

        def add(seq: Seq[KeyDescriptor], buf: mutable.ArrayBuffer[NCCacheSingleKey], jss: mutable.ArrayBuffer[NCJson]): Unit =
            seq.filter(_.values.nonEmpty).foreach(k ⇒ {
                val key = KeyImpl(k)

                // To escape coincided sorted.
                if (!buf.contains(key)) {
                    buf += key

                    tbl +=(k.name, key.toString)

                    jss += key.toJson
                }
            })

        add(kd, keysDirect, jssDirect)
        add(ks, keysSyns, jssSyns)

        val fWords = hs.flatMap(w ⇒
            w.tokenKey match {
                case Some(_) ⇒ None
                case None ⇒ mkFreeWord(w)
            }
        )

        val fWordsPoses = fWords.map(_.pos)
        val fWordsBases = fWords.map(_.lemma)

        tbl +=("Free words", fWordsPoses.zip(fWordsBases).map(p ⇒ s"${p._2}(${p._1})").mkString("|"))

        new NCCacheKey() {
            override def directKeys: Seq[NCCacheSingleKey] = keysDirect.distinct
            override def synonymKeys: Seq[NCCacheSingleKey] = keysSyns.distinct
            override def freeWords: Seq[NCCacheFreeWord] = fWords
            override def toAscii: String = tbl.toString
            override def toJson: NCJson =
                ("directKeys" → jssDirect) ~
                ("synonymKeys" → jssSyns) ~
                ("freeWordsPoses" → fWordsPoses) ~
                ("freeWordsBases" → fWordsBases)
        }
    }

    /**
     * Makes 'free' word.
     *
     * @param h Holder.
     */
    private def mkFreeWord(h: Word): Option[NCCacheFreeWord] =
        if (h.isArtificial)
            None
        else {
            require(h.pos.isDefined)
            require(h.lemma.isDefined)

            val p = h.pos.get
            val l = h.lemma.get

            Some(
                new NCCacheFreeWord {
                    override def pos: String = p
                    override def lemma: String = NCWordNet.getBaseForm(l, p)
                    override def quoted: Boolean = h.isQuoted
                    override def toJson: NCJson = ("lemma" → l) ~ ("pos" → p) ~ ("quoted" → quoted)
                }
            )
        }

    /**
     * Extracts tokens.
     *
     * @param toks Sentence tokens.
     */
    private def mkHolders(toks: Seq[NCToken]): Seq[Word] =
        try {
            val hs = toks.filter(!_.getMetadata.getBoolean("NLP_STOPWORD")).
                map(t ⇒ {
                    val meta = t.getMetadata

                    val lemma = meta.getString("NLP_LEMMA")
                    val stem = meta.getString("NLP_STEM")
                    val pos = meta.getString("NLP_POS")
                    val isQuoted = meta.getBoolean("NLP_QUOTED")

                    Word(lemma, stem, pos, if (t.getId == "nlp:nlp") None else Some(getKeyToken(t)), isQuoted)
                })

            hs
        }
        catch {
            case _: NCE ⇒ Seq.empty
        }
    /**
     *
     * Makes single token elements.
     *
     * @param hs Holders
     * @param getDefault Default function.
     */
    private def mkTokens(hs: Seq[Word], getDefault: Word ⇒ Option[String]): Seq[String] =
        hs.flatMap(h ⇒ h.tokenKey match {
            case Some(tk) ⇒ Some(tk)
            case None ⇒ getDefault(h)
        })

    /**
     * Gets base token value.
     */
    private def getBase(tok: String): String = {
        require(tok.nonEmpty)

        tok.substring(0, 1)
    }

    /**
     *
     * Makes single base token elements.
     *
     * @param hs Holders
     * @param getDefault Default function.
     */
    private def mkTokensBase(hs: Seq[Word], getDefault: Word ⇒ Option[String]): Seq[String] =
        hs.flatMap(h ⇒ h.tokenKey match {
            case Some(tk) ⇒ Some(tk)
            case None ⇒
                getDefault(h) match {
                    case Some(dflt) ⇒ Some(getBase(dflt))
                    case None ⇒ None
                }
        })


    /**
     * Makes cache key for NLP sentence.
     *
     * @param sentToks Fully enriched NLP sentence.
     */
    def genKey(sentToks: Seq[NCToken]): NCCacheKey = {
        val words = mkHolders(sentToks)

        val stems = words.flatMap(_.stem)
        val lems = words.flatMap(_.lemma)
        val toks = mkTokens(words, (h: Word) ⇒ h.stem)
        val synsToks = mkTokens(words, (h: Word) ⇒ h.pos)
        val synsBaseToks = mkTokensBase(words, (h: Word) ⇒ h.pos)

        generate(
            words,
            Seq(
                KeyDescriptor(KT_STEM, stems, sort = false, "Stem key"),
                KeyDescriptor(KT_STEM, stems, sort = true, "Stem sorted key"),
                KeyDescriptor(KT_LEMMA, lems, sort = false, "Lemma key"),
                KeyDescriptor(KT_LEMMA, lems, sort = true, "Lemma sorted key"),
                KeyDescriptor(KT_TOKEN, toks, sort = false, "Token key"),
                KeyDescriptor(KT_TOKEN, toks, sort = true, "Token sorted key")
            ),
            Seq(
                KeyDescriptor(KT_SYN_TOKEN, synsToks, sort = false, "Synonym tkn key"),
                KeyDescriptor(KT_SYN_TOKEN, synsToks, sort = true, "Synonym tkn sorted key"),
                KeyDescriptor(KT_SYN_BASE_TOKEN, synsBaseToks, sort = false, "Synonym base tkn key"),
                KeyDescriptor(KT_SYN_BASE_TOKEN, synsBaseToks, sort = true, "Synonym base tkn sorted key")
            )
        )
    }
}