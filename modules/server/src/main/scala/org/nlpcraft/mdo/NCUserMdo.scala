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

import org.nlpcraft._
import org.nlpcraft.db.postgres.NCPsql.Implicits.RsParser
import org.nlpcraft.json.NCJson
import org.nlpcraft.mdo.impl.NCAnnotatedMdo

/**
  * Company user.
  */
@impl.NCMdoEntity(table = "company_user")
case class NCUserMdo(
    @impl.NCMdoField(column = "id", pk = true) id: Long,

    // Personal contact info.
    @impl.NCMdoField(column = "first_name") firstName: String,
    @impl.NCMdoField(column = "last_name") lastName: String,
    @impl.NCMdoField(column = "email") email: String,
    @impl.NCMdoField(column = "title") title: String,
    @impl.NCMdoField(column = "department") department: String,
    @impl.NCMdoField(column = "phone") phone: String,

    // Other info.
    @impl.NCMdoField(column = "origin") origin: String,
    @impl.NCMdoField(column = "avatar_url") avatarUrl: String,
    @impl.NCMdoField(column = "passwd_salt") passwordSalt: String,
    @impl.NCMdoField(column = "company_id") companyId: Long,
    @impl.NCMdoField(column = "is_active") isActive: Boolean,
    @impl.NCMdoField(column = "is_first_login") isFirstLogin: Boolean,
    @impl.NCMdoField(column = "is_admin") isAdmin: Boolean,
    @impl.NCMdoField(column = "is_root") isRoot: Boolean,
    @impl.NCMdoField(column = "active_ds_id") activeDsId: Long,
    @impl.NCMdoField(column = "prefs_json") prefsJson: String,
    @impl.NCMdoField(column = "referral_code") referralCode: String,

    // Optional IP-based GEO location information.
    @impl.NCMdoField(column = "tmz_name") tmzName: String,
    @impl.NCMdoField(column = "tmz_abbr") tmzAbbr: String,
    @impl.NCMdoField(column = "latitude") latitude: Double,
    @impl.NCMdoField(column = "longitude") longitude: Double,
    @impl.NCMdoField(column = "country_name") countryName: String,
    @impl.NCMdoField(column = "country_code") countryCode: String,
    @impl.NCMdoField(column = "region_name") regionName: String,
    @impl.NCMdoField(column = "region_code") regionCode: String,
    @impl.NCMdoField(column = "city") city: String,
    @impl.NCMdoField(column = "zip_code") zipCode: String,
    @impl.NCMdoField(column = "metro_code") metroCode: Long,

    // Base MDO.
    @impl.NCMdoField(column = "created_on") createdOn: Timestamp,
    @impl.NCMdoField(column = "last_modified_on") lastModifiedOn: Timestamp
) extends NCEntityMdo with NCAnnotatedMdo[NCUserMdo] {
    /**
      * Preferences for this user in JSON format.
      */
    lazy val preferences: NCJson = NCJson(prefsJson)
}

object NCUserMdo {
    implicit val x: RsParser[NCUserMdo] =
        NCAnnotatedMdo.mkRsParser(classOf[NCUserMdo])
}
