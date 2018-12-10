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
  * Extended data source instance data.
  */
@impl.NCMdoEntity
case class NCDataSourceDataMdo (
    // Data source instance data.
    @impl.NCMdoField(column = "id")  id: Long,
    @impl.NCMdoField(column = "name") name: String,
    @impl.NCMdoField(column = "short_desc") shortDesc: String,
    @impl.NCMdoField(column = "user_id") userId: Long,
    @impl.NCMdoField(column = "enabled") enabled: Boolean,
    @impl.NCMdoField(column = "model_id") modelId: String,
    @impl.NCMdoField(column = "model_name") modelName: String,
    @impl.NCMdoField(column = "model_ver") modelVersion: String,
    @impl.NCMdoField(column = "model_cfg") modelConfig: String,
    
    // User data.
    @impl.NCMdoField(column = "prefs_json", jsonConverter = "asJson") userPrefs: String,
    @impl.NCMdoField(column = "avatar_url") userAvatarUrl: String,
    @impl.NCMdoField(column = "user_origin") userOrigin: String,
    @impl.NCMdoField(column = "company_origin") userCompanyOrigin: String,
    @impl.NCMdoField(column = "first_name") userFirstName: String,
    @impl.NCMdoField(column = "last_name") userLastName: String,
    @impl.NCMdoField(column = "email") userEmail: String,
    @impl.NCMdoField(column = "company") userCompany: String,
    @impl.NCMdoField(column = "probe_token") probeToken: String,
    @impl.NCMdoField(column = "sign_up_domain") userSignUpDomain: String,
    @impl.NCMdoField(column = "registration_date", jsonConverter = "sqlTstampConverter") userRegistrationDate: Timestamp,
    @impl.NCMdoField(column = "is_active") userActive: Boolean,
    @impl.NCMdoField(column = "last_login_time", jsonConverter = "sqlTstampConverter") userLastLoginTime: Timestamp,
    @impl.NCMdoField(column = "last_question_time", jsonConverter = "sqlTstampConverter") userLastQuestionTime: Timestamp,
    @impl.NCMdoField(column = "total_question_count") userTotalQuestionCount: Long,
    @impl.NCMdoField(column = "is_admin") userIsAdmin: Boolean,
    @impl.NCMdoField(column = "is_root") userIsRoot: Boolean
) extends NCAnnotatedMdo[NCDataSourceDataMdo] {
    // No-op.
}

object NCDataSourceDataMdo {
    implicit val x: RsParser[NCDataSourceDataMdo] =
        NCAnnotatedMdo.mkRsParser(classOf[NCDataSourceDataMdo])
}
