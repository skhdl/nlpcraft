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

import java.sql.Timestamp

import org.nlpcraft.db.postgres.NCPsql.Implicits.RsParser
import org.nlpcraft.mdo.impl.NCAnnotatedMdo

/**
  * Extended company data.
  */
@impl.NCMdoEntity
case class NCCompanyDataMdo(
    @impl.NCMdoField(column = "id") id: Long,
    @impl.NCMdoField(column = "origin") origin: String,
    @impl.NCMdoField(column = "name") name: String,
    @impl.NCMdoField(column = "sign_up_domain") signUpDomain: String,
    @impl.NCMdoField(column = "website") website: String,
    @impl.NCMdoField(column = "address") address: String,
    @impl.NCMdoField(column = "city") city: String,
    @impl.NCMdoField(column = "region") region: String,
    @impl.NCMdoField(column = "postal_code") postalCode: String,
    @impl.NCMdoField(column = "country") country: String,
    @impl.NCMdoField(column = "probe_token") probeToken: String,
    @impl.NCMdoField(column = "probe_token_hash") probeTokenHash: String,

    // Totals for the company.
    @impl.NCMdoField(column = "total_enabled_users") totalEnabledUsers: Int,
    @impl.NCMdoField(column = "total_questions") totalQuestions: Int,
    @impl.NCMdoField(column = "total_rejections") totalRejections: Int,
    @impl.NCMdoField(column = "total_answers") totalAnswers: Int,
    
    @impl.NCMdoField(column = "probe_token") firstLoginTime: Timestamp, // First login of the 1st user in this company.
    @impl.NCMdoField(column = "probe_token") lastLoginTime: Timestamp, // Latest login of any user in this company.
    
    // Number of requests for given time period.
    @impl.NCMdoField(column = "l30m") l30m: Int,
    @impl.NCMdoField(column = "l24h") l24h: Int,
    @impl.NCMdoField(column = "l7d") l7d: Int,
    @impl.NCMdoField(column = "l30d") l30d: Int
) extends NCAnnotatedMdo[NCCompanyDataMdo]

object NCCompanyDataMdo {
    implicit val x: RsParser[NCCompanyDataMdo] =
        NCAnnotatedMdo.mkRsParser(classOf[NCCompanyDataMdo])
}

