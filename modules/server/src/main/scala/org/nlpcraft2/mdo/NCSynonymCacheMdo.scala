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

package org.nlpcraft2.mdo

import org.apache.ignite.cache.query.annotations.QuerySqlField
import org.nlpcraft.db.postgres.NCPsql.Implicits.RsParser
import org.nlpcraft.mdo.impl.NCAnnotatedMdo

import scala.annotation.meta.field

/**
  * Cache key.
  *
  * @param modelId Model ID.
  * @param cacheKey Key.
  */
@impl.NCMdoEntity
case class NCSynonymCacheKeyMdo(
    @(QuerySqlField @field)(index = true) @impl.NCMdoField(column = "model_id") modelId: String,
    @(QuerySqlField @field)(index = true) @impl.NCMdoField(column = "cache_key") cacheKey: String,
    @(QuerySqlField @field) @impl.NCMdoField(column = "base_words") baseWords: String
) extends NCAnnotatedMdo[NCSynonymCacheKeyMdo]

object NCSynonymCacheKeyMdo {
    implicit val x: RsParser[NCSynonymCacheKeyMdo] = NCAnnotatedMdo.mkRsParser(classOf[NCSynonymCacheKeyMdo])
}

/**
  * Synonym cache object.
  */
@impl.NCMdoEntity
case class NCSynonymCacheMdo(
    @(QuerySqlField @field) @impl.NCMdoField(column = "id") id: Long,
    @(QuerySqlField @field) @impl.NCMdoField(column = "main_cache_id") mainId: Long,
    @(QuerySqlField @field) @impl.NCMdoField(column = "sorted") sorted: Boolean
) extends NCAnnotatedMdo[NCSynonymCacheMdo]

object NCSynonymCacheMdo {
    implicit val x: RsParser[NCSynonymCacheMdo] = NCAnnotatedMdo.mkRsParser(classOf[NCSynonymCacheMdo])
}
