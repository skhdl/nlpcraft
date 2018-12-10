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

import java.sql.ResultSet

import org.apache.ignite.IgniteException
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.db.postgres.NCPsql.Implicits.RsParser
import org.nlpcraft.ignite.NCIgniteCacheStore
import org.nlpcraft2.mdo.NCMainCacheMdo

import scala.util.control.Exception._

/**
 * Main cache storage.
 */
class NCMainCacheStore extends NCIgniteCacheStore[Long, NCMainCacheMdo] {
    // Implicit result set parser.
    private implicit val p: RsParser[NCMainCacheMdo] = {
        rs: ResultSet ⇒ NCMainCacheMdo(rs.getBytes("json"), rs.getString("model_id"))
    }

    @throws[IgniteException]
    override def get(id: Long): NCMainCacheMdo =
        catching(wrapNCE) {
            NCPsql.sql {
                NCPsql.selectSingle[NCMainCacheMdo]("SELECT * FROM main_cache WHERE id = ?", id).orNull
            }
        }

    @throws[IgniteException]
    override def put(id: Long, v: NCMainCacheMdo): Unit =
        catching(wrapNCE) {
            NCPsql.sql {
                NCPsql.insert("INSERT INTO main_cache(id, json, model_id) VALUES(?, ?, ?)", id, v.json, v.modelId)
            }
        }

    @throws[IgniteException]
    override def remove(id: Long): Unit =
        catching(wrapNCE) {
            NCPsql.sql {
                NCPsql.delete("DELETE FROM main_cache WHERE id = ? ", id)
            }
        }
}