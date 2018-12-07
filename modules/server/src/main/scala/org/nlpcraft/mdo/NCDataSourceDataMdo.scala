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
  * Extended data source instance data.
  */
@NCMdoEntity
case class NCDataSourceDataMdo (
    // Data source instance data.
    @NCMdoField(column = "id")  id: Long,
    @NCMdoField(column = "name") name: String,
    @NCMdoField(column = "short_desc") shortDesc: String,
    @NCMdoField(column = "user_id") userId: Long,
    @NCMdoField(column = "enabled") enabled: Boolean,
    @NCMdoField(column = "model_id") modelId: String,
    @NCMdoField(column = "model_name") modelName: String,
    @NCMdoField(column = "model_ver") modelVersion: String,
    @NCMdoField(column = "model_cfg") modelConfig: String,
    
    // User data.
    @NCMdoField(column = "prefs_json", jsonConverter = "asJson") userPrefs: String,
    @NCMdoField(column = "avatar_url") userAvatarUrl: String,
    @NCMdoField(column = "user_origin") userOrigin: String,
    @NCMdoField(column = "company_origin") userCompanyOrigin: String,
    @NCMdoField(column = "first_name") userFirstName: String,
    @NCMdoField(column = "last_name") userLastName: String,
    @NCMdoField(column = "email") userEmail: String,
    @NCMdoField(column = "company") userCompany: String,
    @NCMdoField(column = "probe_token") probeToken: String,
    @NCMdoField(column = "sign_up_domain") userSignUpDomain: String,
    @NCMdoField(column = "registration_date", jsonConverter = "sqlTstampConverter") userRegistrationDate: Timestamp,
    @NCMdoField(column = "is_active") userActive: Boolean,
    @NCMdoField(column = "last_login_time", jsonConverter = "sqlTstampConverter") userLastLoginTime: Timestamp,
    @NCMdoField(column = "last_question_time", jsonConverter = "sqlTstampConverter") userLastQuestionTime: Timestamp,
    @NCMdoField(column = "total_question_count") userTotalQuestionCount: Long,
    @NCMdoField(column = "is_admin") userIsAdmin: Boolean,
    @NCMdoField(column = "is_root") userIsRoot: Boolean
) extends NCAnnotatedMdo[NCDataSourceDataMdo] {
    // No-op.
}

object NCDataSourceDataMdo {
    implicit val x: RsParser[NCDataSourceDataMdo] =
        NCAnnotatedMdo.mkRsParser(classOf[NCDataSourceDataMdo])
}
