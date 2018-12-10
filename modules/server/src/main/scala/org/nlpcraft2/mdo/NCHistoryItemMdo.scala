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
  * History item.
  */
@impl.NCMdoEntity(table = "history")
case class NCHistoryItemMdo(
    @impl.NCMdoField(column = "id", pk = true) id: Long,
    @impl.NCMdoField(column = "srv_req_id") srvReqId: String,
    @impl.NCMdoField(column = "orig_txt") origText: String,
    @impl.NCMdoField(column = "curate_hint") curateHint: String,
    @impl.NCMdoField(column = "curate_txt") curateText: String,
    @impl.NCMdoField(column = "user_id") userId: Long,
    @impl.NCMdoField(column = "ds_id") dsId: Long,
    @impl.NCMdoField(column = "user_agent") userAgent: String,
    @impl.NCMdoField(column = "origin") origin: String,
    @impl.NCMdoField(column = "rmt_addr") rmtAddr: String,
    @impl.NCMdoField(column = "status") status: String,
    @impl.NCMdoField(column = "recv_tstamp") recvTstamp: Timestamp,
    @impl.NCMdoField(column = "resp_tstamp") respTstamp: Timestamp,
    @impl.NCMdoField(column = "feedback_msg") feedbackMsg: String,
    @impl.NCMdoField(column = "error") error: String,
    @impl.NCMdoField(column = "cache_id") cacheId: Long,
    @impl.NCMdoField(column = "last_linguist_user_id") lastLinguistUserId: Long,
    @impl.NCMdoField(column = "last_linguist_op") lastLinguistOp: String,
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
    // Optional probe information.
    @impl.NCMdoField(column = "probe_token", json = false) probeToken: String,
    @impl.NCMdoField(column = "probe_id") probeId: String,
    @impl.NCMdoField(column = "probe_guid") probeGuid: String,
    @impl.NCMdoField(column = "probe_api_version") probeApiVersion: Int,
    @impl.NCMdoField(column = "probe_api_date") probeApiDate: Timestamp,
    @impl.NCMdoField(column = "probe_os_version") probeOsVersion: String,
    @impl.NCMdoField(column = "probe_os_name") probeOsName: String,
    @impl.NCMdoField(column = "probe_os_arch") probeOsArch: String,
    @impl.NCMdoField(column = "probe_start_tstamp") probeStartTstamp: Timestamp,
    @impl.NCMdoField(column = "probe_tmz_id") probeTmzId: String,
    @impl.NCMdoField(column = "probe_tmz_abbr") probeTmzAbbr: String,
    @impl.NCMdoField(column = "probe_tmz_name") probeTmzName: String,
    @impl.NCMdoField(column = "probe_user_name") probeUserName: String,
    @impl.NCMdoField(column = "probe_java_version") probeJavaVersion: String,
    @impl.NCMdoField(column = "probe_java_vendor") probeJavaVendor: String,
    @impl.NCMdoField(column = "probe_host_name") probeHostName: String,
    @impl.NCMdoField(column = "probe_host_addr") probeHostAddr: String,
    @impl.NCMdoField(column = "probe_mac_addr") probeMacAddr: String,
    @impl.NCMdoField(column = "probe_email") probeEmail: String,
    // Don't include result by default to minimize the size of JSON history record.
    @impl.NCMdoField(column = "res_type") resultType: String,
    @impl.NCMdoField(column = "res_body_gzip") resultBodyGZip: String
) extends NCAnnotatedMdo[NCHistoryItemMdo]

object NCHistoryItemMdo {
    implicit val x: RsParser[NCHistoryItemMdo] =
        NCAnnotatedMdo.mkRsParser(classOf[NCHistoryItemMdo])
}
