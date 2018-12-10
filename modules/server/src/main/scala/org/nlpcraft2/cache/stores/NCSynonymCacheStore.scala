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

package org.nlpcraft2.cache.stores

import org.apache.ignite.IgniteException
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.ignite.NCIgniteCacheStore
import org.nlpcraft2.mdo.{NCSynonymCacheKeyMdo, NCSynonymCacheMdo}

import scala.util.control.Exception._

/**
  * Submit cache storage.
  */
class NCSynonymCacheStore extends NCIgniteCacheStore[NCSynonymCacheKeyMdo, NCSynonymCacheMdo] {
    /**
      * Deletes main cache.
      */
    private def clearMainCache() =
        NCPsql.delete(
            """
            | DELETE FROM main_cache m
            | WHERE
            |     NOT EXISTS (
            |         SELECT NULL
            |         FROM synonyms_cache sk
            |         WHERE sk.main_cache_id = m.id
            |     ) AND
            |     NOT EXISTS (
            |         SELECT NULL
            |         FROM submit_cache sk
            |         WHERE sk.main_cache_id = m.id
            |     )
            """.stripMargin.trim
        )

    @throws[IgniteException]
    override def get(key: NCSynonymCacheKeyMdo): NCSynonymCacheMdo =
        catching(wrapNCE) {
            NCPsql.sql {
                NCPsql.selectSingle[NCSynonymCacheMdo](
                    """
                      | SELECT
                      |     id,
                      |     main_cache_id,
                      |     sorted
                      | FROM synonyms_cache s
                      | WHERE
                      |     model_id = ? AND
                      |     cache_key = ? AND
                      |     base_words = ?
                    """.stripMargin.trim,
                    key.modelId,
                    key.cacheKey,
                    key.baseWords
                ).orNull
            }
        }

    @throws[IgniteException]
    override def put(key: NCSynonymCacheKeyMdo, m: NCSynonymCacheMdo): Unit =
        catching(wrapNCE) {
            NCPsql.sql {
                NCPsql.insert(
                    """
                      |INSERT INTO synonyms_cache (
                      |  id,
                      |  main_cache_id,
                      |  cache_key,
                      |  base_words,
                      |  sorted,
                      |  model_id
                      |)
                      |VALUES(?, ?, ?, ?, ?, ?)
                    """.stripMargin.trim,
                    m.id,
                    m.mainId,
                    key.cacheKey,
                    key.baseWords,
                    m.sorted,
                    key.modelId
                )
            }
        }

    @throws[IgniteException]
    override def remove(key: NCSynonymCacheKeyMdo): Unit =
        catching(wrapNCE) {
            NCPsql.sql {
                NCPsql.delete(
                    "DELETE FROM synonyms_cache WHERE cache_key = ? AND base_words = ? AND model_id = ?",
                    key.cacheKey, key.baseWords, key.modelId
                )

                clearMainCache()
            }
        }

    // Note that keys is unused parameter.
    @throws[IgniteException]
    override def deleteAll(keys: java.util.Collection[_]): Unit =
    catching(wrapNCE) {
        NCPsql.sql {
            NCPsql.delete("DELETE FROM synonyms_cache")

            clearMainCache()
        }
    }
}