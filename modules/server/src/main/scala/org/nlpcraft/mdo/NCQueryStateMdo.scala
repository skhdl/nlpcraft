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

import org.nlpcraft.db.postgres.NCPsql.Implicits.RsParser
import org.nlpcraft.mdllib.NCToken
import org.nlpcraft.mdo.impl._

/**
  * Query state MDO.
  */
@NCMdoEntity(sql = false)
case class NCQueryStateMdo(
    @NCMdoField srvReqId: String,
    @NCMdoField test: Boolean,
    @NCMdoField dsId: Long,
    @NCMdoField modelId: String,
    @NCMdoField var probeId: Option[String] = None, // Optional probe ID.
    @NCMdoField userId: Long,
    @NCMdoField email: String,
    @NCMdoField origText: String, // Text of the initial question.
    @NCMdoField createTstamp: Long, // Creation timestamp.
    @NCMdoField var updateTstamp: Long, // Last update timestamp.
    @NCMdoField var status: String,
    @NCMdoField var curateText: Option[String] = None, // Optional text after human curation.
    @NCMdoField var curateJson: Option[String] = None, // Optional request JSON explanation for curation.
    @NCMdoField var curateHint: Option[String] = None, // Optional hint after human curation.
    @NCMdoField(jsonConverter = "toJsonList") var tokens: Option[Seq[NCToken]] = None, // Optional tokens.
    @NCMdoField(jsonConverter = "toJsonList") var origTokens: Option[Seq[NCToken]] = None, // Optional tokens.
    @NCMdoField var cacheId: Option[Long] = None, // Optional cache ID.
    // Query OK (result, trivia, or talkback).
    @NCMdoField var resultType: Option[String] = None,
    @NCMdoField var resultBody: Option[String] = None,
    @NCMdoField var resultMetadata: Option[Map[String, Object]] = None,
    // Query ERROR (HTML message).
    @NCMdoField var error: Option[String] = None
) extends NCAnnotatedMdo[NCQueryStateMdo]

object NCQueryStateMdo {
    implicit val x: RsParser[NCQueryStateMdo] =
        NCAnnotatedMdo.mkRsParser(classOf[NCQueryStateMdo])
}
