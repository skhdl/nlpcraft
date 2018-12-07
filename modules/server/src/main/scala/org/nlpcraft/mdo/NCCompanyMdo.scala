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

import java.sql.Timestamp

import org.nlpcraft.db.postgres.NCPsql.Implicits.RsParser
import org.nlpcraft.mdo.impl.{NCAnnotatedMdo, NCMdoEntity, NCMdoField}

/**
  * Customer company.
  */
@NCMdoEntity(table = "company")
case class NCCompanyMdo (
    @NCMdoField(column = "id", pk = true) id: Long,
    @NCMdoField(column = "origin") origin: String,
    @NCMdoField(column = "name") name: String,
    @NCMdoField(column = "sign_up_domain") signUpDomain: String,
    @NCMdoField(column = "website") website: String,
    @NCMdoField(column = "address") address: String,
    @NCMdoField(column = "city") city: String,
    @NCMdoField(column = "region") region: String,
    @NCMdoField(column = "postal_code") postalCode: String,
    @NCMdoField(column = "country") country: String,
    @NCMdoField(column = "probe_token") probeToken: String,
    @NCMdoField(column = "probe_token_hash") probeTokenHash: String,
    
    // Base MDO.
    @NCMdoField(column = "created_on", json = false) createdOn: Timestamp,
    @NCMdoField(column = "last_modified_on", json = false) lastModifiedOn: Timestamp
) extends NCEntityMdo with NCAnnotatedMdo[NCCompanyMdo]

object NCCompanyMdo {
    implicit val x: RsParser[NCCompanyMdo] = NCAnnotatedMdo.mkRsParser(classOf[NCCompanyMdo])
}
