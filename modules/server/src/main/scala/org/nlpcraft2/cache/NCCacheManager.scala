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

package org.nlpcraft2.cache

import net.liftweb.json.JsonDSL._
import org.apache.ignite.{IgniteAtomicSequence, IgniteCache}
import org.nlpcraft.ascii.NCAsciiTable
import org.nlpcraft2.cache.NCCacheKeyType._
import org.nlpcraft2.db.NCDbManager
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.db.postgres.NCPsql.NCPsqlConstraintViolation
import org.nlpcraft.ignite.NCIgniteHelpers._
import org.nlpcraft.ignite.NCIgniteNlpCraft
import org.nlpcraft2.json.NCJson
import org.nlpcraft2.mdo._
import org.nlpcraft2.nlp.synonym.NCSynonymManager
import org.nlpcraft2.nlp.synonym.NCSynonymType._
import org.nlpcraft.tx.NCTxManager
import org.nlpcraft.{NCDebug, NCE, NCLifecycle}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.Seq
import scala.util.control.Exception._
/**
 * Main NLP cache manager.
 * Main cache stores linguist-prepared json objects keyed by NLP cache keys.
 */
object NCCacheManager extends NCLifecycle("CORE NLP cache manager") with NCIgniteNlpCraft with NCDebug {
    private final val MAX_SYNS_COMBS = 40320 // = 8!
    private final val MAX_FREE_WORDS = 17 // pow(2, 17) = 131072
    /** Maximum synsets count for each word, it's used for restriction synsets count.*/
    private final val MAX_SYNSETS = 4

    /** Maximum synonyms count for each word, it's used for restriction synonyms count.*/
    private final val MAX_SYNONYMS_PER_SYNSET = 4

    private final val SORTED_DIRECT_KEYS = Seq(KT_LEMMA, KT_STEM, KT_TOKEN)
    private final val SORTED_SYN_KEYS = Seq(KT_SYN_TOKEN, KT_SYN_BASE_TOKEN)

    private final val SORT_ORDER = Seq(false, true)

    // Ignite caches (actual storage).
    @volatile private var mainCache: IgniteCache[Long, NCMainCacheMdo] = _
    @volatile private var submitCache: IgniteCache[NCSubmitDsCacheKeyMdo, NCSubmitCacheMdo] = _
    @volatile private var synonymCache: IgniteCache[NCSynonymCacheKeyMdo, NCSynonymCacheMdo] = _

    @volatile private var seqMain: IgniteAtomicSequence = _
    @volatile private var seqSubmit: IgniteAtomicSequence = _
    @volatile private var seqSyns: IgniteAtomicSequence = _

    /**
      * Starts manager.
      */
    override def start(): NCLifecycle = {
        ensureStopped()

        def make(seqName: String, tab: String, id: String): IgniteAtomicSequence =
            NCPsql.sqlNoTx {
                nlpcraft.atomicSequence(
                    "seqSubmit",
                    NCDbManager.getMaxColumnValue("submit_cache", "id").getOrElse(0),
                    true
                )
            }

        seqMain = make("seqMain", "main_cache", "id")
        seqSubmit = make("seqSubmit", "submit_cache", "id")
        seqSyns = make("seqSyns", "synonyms_cache", "id")

        mainCache = nlpcraft.cache[Long, NCMainCacheMdo]("core-json-main-cache")
        submitCache = nlpcraft.cache[NCSubmitDsCacheKeyMdo, NCSubmitCacheMdo]("core-json-submit-cache")
        synonymCache = nlpcraft.cache[NCSynonymCacheKeyMdo, NCSynonymCacheMdo]("core-json-synonym-cache")

        super.start()
    }

    /**
      * Stop manager.
      */
    override def stop(): Unit = {
        checkStopping()

        mainCache = null
        submitCache = null
        synonymCache = null

        super.stop()
    }

    /**
     * Makes cache searching result.
      *
     * @param modelId Model ID.
     * @param kt Key type.
     * @param mainId Main cache ID.
     * @param sort Flag which indicates is key sorted or not.
     * @param cacheId Cache ID.
     */
    @throws[NCE]
    private def mkResult(modelId: String, kt: NCCacheKeyType, mainId: Long, sort: Boolean, cacheId: Long, keyVal: String): NCCacheHolder =
        mainCache(mainId) match {
            case None ⇒ throw new NCE(s"Main cache not found for id: $mainId")
            case Some(v) ⇒
                val js = NCJson.unzip2Json(v.json)

                val res = new NCCacheResult() {
                    override def keyType: NCCacheKeyType = kt
                    override def mainCacheId: Long = mainId
                    override def json: NCJson = js
                    override def sorted: Boolean = sort
                    override def toJson: NCJson =
                        ("mainCacheId" → mainId) ~
                        ("json" → js) ~
                        ("keyType" → kt.toString) ~
                        ("sorted" → sort)

                    override def toAscii: String = {
                        val tbl = NCAsciiTable()

                        tbl +=("KeyType", kt)
                        tbl +=("Sorted", sort)
                        tbl +=("MainId", mainId)
                        tbl +=("Json", js.compact)

                        tbl.toString
                    }
                }

                NCCacheHolder(res, cacheId, keyVal)
        }

    /**
     * Sort keys function.
     *
     * @param k Key.
     * @param ordered Ordered key types.
     */
    private def sortKey(k: NCCacheSingleKey, ordered: Seq[NCCacheKeyType]) = {
        val (sort1, sort2) = (SORT_ORDER.indexOf(k.sorted), ordered.indexOf(k.keyType))

        require(sort1 >= 0)
        require(sort2 >= 0)

        (sort1, sort2)
    }

    /**
     * Packs key.
     *
     * @param seq Elements.
     */
    private def pack(seq: Seq[String]): String = seq.mkString("|")

    /**
     * Extracts key elements.
     *
     * @param k Key.
     */
    private def unpack(k: String): Seq[String] = if (k.isEmpty) Seq.empty else k.split("\\|")

    /**
     * Sorts 'free' words.
     *
     * @param seq 'Free' words.
     */
    private def sort(seq: Seq[NCCacheFreeWord]): Seq[NCCacheFreeWord] = seq.sortBy(_.pos)

    /**
     * Calculates factorial math function.
     *
     * @param n Value to calculate.
     */
    private def factorial(n: Long): Long = {
        @tailrec def accumulator(acc: Long, n: Long): Long =  if (n <= 1) acc else accumulator(n * acc, n - 1)

        accumulator(1, n)
    }

    /**
     * Prepares list of combinations.
     *
     * @param seq List of 'free' words.
     */
    private def combine(seq: List[List[NCCacheFreeWord]]): List[List[NCCacheFreeWord]] =
        seq match {
            case Nil ⇒ List(Nil)
            case xs :: rss ⇒ for (x ← xs; cs ← combine(rss) if !cs.contains(x)) yield x +: cs
        }

    /**
     * Prepares all combinations of sorted lists with non-unique sorted elements.
     *
     * @param seq Elements ('Free' words)
     */
    private def sortCombinations(seq: Seq[NCCacheFreeWord]): Seq[Seq[NCCacheFreeWord]] =
        combine(seq.map(h ⇒ seq.filter(_.pos == h.pos).toList).toList).distinct

    // Synonyms key consists of POSes.
    // Not all of them are unique, so there are possible some number of synonyms permutations.
    // We don't save synonyms cache with too many possible permutations to avoid
    // performance decreasing during cache scanning.
    // Anyway precision of these answers is doubtful.
    private def isSuitable(seq: Seq[NCCacheFreeWord], k: NCCacheKey): Boolean =
        if (seq.lengthCompare(MAX_FREE_WORDS) > 0) {
            logger.trace(s"Too many free words: ${seq.size} to processing synonyms for key:")

            k.ascii()

            false
        }
        else {
            val n = seq.groupBy(_.pos).map(p ⇒ factorial(p._2.map(_.lemma).distinct.size)).product

            if (n > MAX_SYNS_COMBS) {
                logger.trace(s"Too many combinations: $n to processing synonyms for key:")

                k.ascii()

                false
            }
            else
                true
        }

    /**
     * Deletes cache data.
     *
     * @param mainCacheId Main cache ID.
     */
    @throws[NCE]
    def delete(mainCacheId: Long): Unit = {
        ensureStarted()

        delete0(mainCacheId)
    }

    /**
     * Deletes cache data.
     *
     * @param mainId Main cache ID.
     */
    @throws[NCE]
    private def delete0(mainId: Long) {
        catching(wrapIE) {
            mainCache -= mainId
        }
    }

    /**
     * Finds all matches for given cache key and returns them in result object.
     *
     * Searching in direct way and sorted way.
      *
     * @param modelId Model ID.
     * @param ks Cache key.
     * @param onlyDirect Flag, should synonyms be used on not.
     * @return Cache result object containing all found matches for a given key and cache key.
     */
    @throws[NCE]
    def find(modelId: String, ks: NCCacheKey, onlyDirect: Boolean): Option[NCCacheHolder] = {
        ensureStarted()

        (if (onlyDirect) ks.directKeys.filter(!_.sorted) else ks.directKeys).
            sortBy(k ⇒ sortKey(k, SORTED_DIRECT_KEYS)).
            toStream.
            flatMap(k ⇒
            submitCache(NCSubmitDsCacheKeyMdo(modelId, k.keyValue)) match {
                case None ⇒ None
                case Some(m) ⇒ Some(mkResult(modelId, k.keyType, m.mainId, k.sorted, m.id, k.keyValue))
            }
        ).headOption
    }

    /**
      * Finds all matches for given cache key using synonyms and returns them in result object.
      * Searching in direct way and sorted way.
      *
      * @param modelId Model ID.
      * @param ks Cache key to find all matches for.
      * @param onlyDirect Flag, should synonyms be used on not.
      * @return Cache result object containing all found matches for a given key and cache key.
      */
    @throws[NCE]
    def findBySynonyms(modelId: String, ks: NCCacheKey, onlyDirect: Boolean): Option[NCCacheHolder] = {
        ensureStarted()

        def findBySynonyms0(k: NCCacheSingleKey): Option[NCCacheHolder] = {
            val cacheRes = synonymCache.select(s"cacheKey = ?", k.keyValue).getAll.asScala

            if (cacheRes.isEmpty || !isSuitable(ks.freeWords, ks))
                None
            else {
                // Note that ordering by POSEs is not unique solution.
                // Too many combinations shouldn't be stored.
                val synsComb =
                    (if (k.sorted) sortCombinations(sort(ks.freeWords)) else Seq(ks.freeWords)).map(genSynonyms)

                cacheRes.toStream.flatMap(p ⇒ {
                    // Already sorted for sorted key.
                    val baseWords = unpack(p.getKey.baseWords)

                    var i = 0

                    synsComb.toStream.flatMap(syns ⇒ {
                        // TODO: Temporary workaround.
                        // This assertion occurred,
                        // because it is difficult to reproduce, log error message temporary added
                        // here instead of assertion to catch error and fix it.

                        // require(baseWords.length == syns.size)

                        i = i + 1

                        if (baseWords.lengthCompare(syns.size) == 0) {
                            if (baseWords.zip(syns).forall(x ⇒ x._2.contains(x._1))) Some(p.getValue) else None
                        }
                        else {
                            logger.error("Internal error [" +
                                s"keys=$ks, " +
                                s"onlyDirect=$onlyDirect, " +
                                s"key=$k, " +
                                s"cacheRes=${cacheRes.mkString(", ")}}, " +
                                s"baseWords=${baseWords.mkString(", ")}" +
                                s"syns=${syns.mkString(", ")}" +
                                "]"
                            )

                            None
                        }
                    }).headOption
                }).headOption match {
                    case Some(m) ⇒
                        Some(
                            mkResult(
                                modelId, k.keyType, m.mainId, sort = k.sorted, m.id, k.keyValue
                            )
                        )
                    case None ⇒ None
                }
            }
        }

        (if (onlyDirect) ks.synonymKeys.filter(!_.sorted) else ks.synonymKeys).
            sortBy(k ⇒ sortKey(k, SORTED_SYN_KEYS)).
            toStream.
            flatMap(findBySynonyms0).
            headOption
    }

    /**
     * Gets Linguist json with given ID.
     *
     * @param id main cache ID.
     */
    @throws[NCE]
    def getJson(id: Long): NCJson = {
        ensureStarted()

        catching(wrapIE) {
            mainCache(id) match {
                case None ⇒ throw new NCE(s"JSON not found for id: $id")
                case Some(v) ⇒ NCJson.unzip2Json(v.json)
            }
        }
    }

    /**
     * Stores given JSON object with provided cache key.
     *
     * @param modelId Model ID.
     * @param ks Cache key to store with.
     * @param json JSON object received from Linguist.
     * @param fullKeys Full keys flag.
     */
    @throws[NCE]
    def store(modelId: String, ks: NCCacheKey, json: NCJson, fullKeys: Boolean = true): Option[NCCacheStoreResult] = {
        ensureStarted()

        def mkBaseKey: NCCacheKey =
            new NCCacheKey() {
                private lazy val fWordsPoses = ks.freeWords.map(_.pos)
                private lazy val fWordsBases = ks.freeWords.map(_.lemma)

                private def filter(seq: Seq[NCCacheSingleKey]): Seq[NCCacheSingleKey] =
                    seq.filter(p ⇒ p.keyType == KT_STEM || p.keyType == KT_LEMMA)

                override def directKeys: Seq[NCCacheSingleKey] = filter(ks.directKeys)
                override def synonymKeys: Seq[NCCacheSingleKey] = filter(ks.synonymKeys)
                override def freeWords: Seq[NCCacheFreeWord] = ks.freeWords

                override def toAscii: String = {
                    val tbl = NCAsciiTable()

                    (directKeys ++ synonymKeys).foreach(p ⇒ tbl += (p.keyType.toString, p.keyValue))

                    tbl += (
                        "Free words",
                        fWordsPoses.zip(fWordsBases).map(p ⇒ s"${p._2}(${p._1})").mkString("|")
                    )

                    tbl.toString
                }

                override def toJson: NCJson =
                    ("directKeys" → directKeys.map(_.toJson)) ~
                    ("synonymKeys" → synonymKeys.map(_.toJson)) ~
                    ("freeWordsPoses" → fWordsPoses) ~
                    ("freeWordsBases" → fWordsBases)
            }

        val mainId = seqMain.incrementAndGet()

        def storeKey0(cacheKey: NCCacheKey, fullKeys: Boolean): Option[NCCacheStoreResult] = {
            store0(mainId, modelId, cacheKey, json)

            Some(NCCacheStoreResult(mainId, fullKeys))
        }

        def storeKey(cacheKey: NCCacheKey, fullKeys: Boolean): Option[NCCacheStoreResult] =
            try
                storeKey0(cacheKey, fullKeys)
            catch {
                case e: NCE ⇒
                    if (isUniqueConstraintViolation(e)) {
                        mainCache -= mainId

                        None
                    }
                    else
                        throw e
            }

        if (fullKeys)
            try
                storeKey0(ks, fullKeys)
            catch {
                case e: NCE ⇒ if (isUniqueConstraintViolation(e)) storeKey(mkBaseKey, fullKeys = false) else throw e
            }
        else
            storeKey(mkBaseKey, fullKeys)
    }

    /**
     * Stores given JSON object with provided cache key.
     *
     * @param mainId Main cache ID.
     * @param modelId Model ID.
     * @param ks Cache key to store with.
     * @param json JSON object received from Linguist.
     * @return ID of the stored Linguist json object.
     */
    @throws[NCE]
    private def store0(mainId: Long, modelId: String, ks: NCCacheKey, json: NCJson): Unit =
        catching(wrapIE) {
            NCTxManager.startTx {
                mainCache += mainId → NCMainCacheMdo(json.gzip(), modelId)

                def processDirect(dirKeys: Seq[NCCacheSingleKey]): Unit =
                    dirKeys.foreach(key ⇒
                        submitCache += NCSubmitDsCacheKeyMdo(modelId, key.keyValue) →
                            NCSubmitCacheMdo(seqSubmit.incrementAndGet(), mainId, key.sorted)
                    )

                processDirect(ks.directKeys)

                if (isSuitable(ks.freeWords, ks)) {
                    val unsorted = ks.synonymKeys.filter(!_.sorted).map(_.keyValue)

                    ks.synonymKeys.foreach(k ⇒
                        if (!k.sorted)
                            synonymCache +=
                                NCSynonymCacheKeyMdo(modelId, k.keyValue, pack(ks.freeWords.map(_.lemma))) →
                                    NCSynonymCacheMdo(seqSyns.incrementAndGet(), mainId, sorted = false)
                        // To avoid duplicated records.
                        else if (!unsorted.contains(k.keyValue))
                            synonymCache +=
                                NCSynonymCacheKeyMdo(modelId, k.keyValue, pack(sort(ks.freeWords).map(_.lemma))) →
                                    NCSynonymCacheMdo(seqSyns.incrementAndGet(), mainId, sorted = true)
                    )
                }
            }
        }

    /**
     * Removes cache.
     */
    @throws[NCE]
    private def clear[K, T](cache: IgniteCache[K, T]): Unit = {
        ensureStarted()

        if (!IS_PROD) {
            catching(wrapIE) {
                cache.removeAll()
            }

            logger.debug(s"Cache cleared: $cache")
        }
        else
            throw new NCE("Unsupported in production environment.")
    }

    /**
     * Clear all necessary data (cache and DB) for given server request ID.
      *
     * @param srvReqId Request's server ID to clear.
     */
    // TODO: do we need it?
    @throws[NCE]
    def clearForRequest(srvReqId: String): Unit = {
        ensureStarted()

        NCPsql.sql {
            if (!NCDbManager.isHistoryExist(srvReqId))
                throw new NCE(s"Attempted to drop unknown server request ID: $srvReqId")

            NCDbManager.getCacheId(srvReqId) match {
                case Some(cacheId) ⇒ delete0(cacheId)
                case None ⇒ logger.info(s"Missed cache data for request ID: $srvReqId")
            }

            NCDbManager.deleteHistory(srvReqId)
        }
    }

    /**
     * Clears entire cache.
     */
    @throws[NCE]
    def clearAll(): Unit = {
        clear(submitCache)
        clear(synonymCache)
        clear(mainCache)
    }

    /**
     * Checks if given exception is unique constraint cache error or not.
     *
     * @param e Exception.
     */
    @tailrec
    def isUniqueConstraintViolation(e: Throwable): Boolean =
        e match {
            case null ⇒ false
            case _: NCPsqlConstraintViolation ⇒ true
            case _ ⇒ isUniqueConstraintViolation(e.getCause)
        }

    /**
      * Makes synonyms.
      *
      * @param freeWords Free words.
      */
    private def genSynonyms(freeWords: Seq[NCCacheFreeWord]): Seq[Seq[String]] =
        freeWords.map(w ⇒
            if (w.quoted)
                Seq(w.lemma)
            else {
                NCSynonymManager.
                    get(w.lemma, w.pos).
                    toSeq.
                    // NLPCRAFT synonyms with higher priority.
                    sortBy(p ⇒ p._1 match {
                    case NLPCRAFT ⇒ 0
                    case WORDNET ⇒ 1
                    case _ ⇒ throw new AssertionError(s"Unexpected synonyms type: ${p._1}")
                }).
                    flatMap(_._2).
                    flatMap(
                        _.take(MAX_SYNONYMS_PER_SYNSET).
                            // Only chars and '-' are possible. Multi-words synonyms skipped.
                            filter(_.forall(ch ⇒ ch.isLetter || ch == '-'))
                    ).
                    filter(_ != w.lemma).
                    distinct.
                    take(MAX_SYNSETS) :+ w.lemma
            }
        )

}