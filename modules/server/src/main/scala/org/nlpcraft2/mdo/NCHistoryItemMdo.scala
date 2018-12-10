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
import org.nlpcraft2.mdo.impl.{NCAnnotatedMdo, NCMdoEntity, NCMdoField}

/**
  * History item.
  */
@NCMdoEntity(table = "history")
case class NCHistoryItemMdo(
    @NCMdoField(column = "id", pk = true) id: Long,
    @NCMdoField(column = "srv_req_id") srvReqId: String,
    @NCMdoField(column = "orig_txt") origText: String,
    @NCMdoField(column = "curate_hint") curateHint: String,
    @NCMdoField(column = "curate_txt") curateText: String,
    @NCMdoField(column = "user_id") userId: Long,
    @NCMdoField(column = "ds_id") dsId: Long,
    @NCMdoField(column = "user_agent") userAgent: String,
    @NCMdoField(column = "origin") origin: String,
    @NCMdoField(column = "rmt_addr") rmtAddr: String,
    @NCMdoField(column = "status") status: String,
    @NCMdoField(column = "recv_tstamp") recvTstamp: Timestamp,
    @NCMdoField(column = "resp_tstamp") respTstamp: Timestamp,
    @NCMdoField(column = "feedback_msg") feedbackMsg: String,
    @NCMdoField(column = "error") error: String,
    @NCMdoField(column = "cache_id") cacheId: Long,
    @NCMdoField(column = "last_linguist_user_id") lastLinguistUserId: Long,
    @NCMdoField(column = "last_linguist_op") lastLinguistOp: String,
    // Optional IP-based GEO location information.
    @NCMdoField(column = "tmz_name") tmzName: String,
    @NCMdoField(column = "tmz_abbr") tmzAbbr: String,
    @NCMdoField(column = "latitude") latitude: Double,
    @NCMdoField(column = "longitude") longitude: Double,
    @NCMdoField(column = "country_name") countryName: String,
    @NCMdoField(column = "country_code") countryCode: String,
    @NCMdoField(column = "region_name") regionName: String,
    @NCMdoField(column = "region_code") regionCode: String,
    @NCMdoField(column = "city") city: String,
    @NCMdoField(column = "zip_code") zipCode: String,
    @NCMdoField(column = "metro_code") metroCode: Long,
    // Optional probe information.
    @NCMdoField(column = "probe_token", json = false) probeToken: String,
    @NCMdoField(column = "probe_id") probeId: String,
    @NCMdoField(column = "probe_guid") probeGuid: String,
    @NCMdoField(column = "probe_api_version") probeApiVersion: Int,
    @NCMdoField(column = "probe_api_date") probeApiDate: Timestamp,
    @NCMdoField(column = "probe_os_version") probeOsVersion: String,
    @NCMdoField(column = "probe_os_name") probeOsName: String,
    @NCMdoField(column = "probe_os_arch") probeOsArch: String,
    @NCMdoField(column = "probe_start_tstamp") probeStartTstamp: Timestamp,
    @NCMdoField(column = "probe_tmz_id") probeTmzId: String,
    @NCMdoField(column = "probe_tmz_abbr") probeTmzAbbr: String,
    @NCMdoField(column = "probe_tmz_name") probeTmzName: String,
    @NCMdoField(column = "probe_user_name") probeUserName: String,
    @NCMdoField(column = "probe_java_version") probeJavaVersion: String,
    @NCMdoField(column = "probe_java_vendor") probeJavaVendor: String,
    @NCMdoField(column = "probe_host_name") probeHostName: String,
    @NCMdoField(column = "probe_host_addr") probeHostAddr: String,
    @NCMdoField(column = "probe_mac_addr") probeMacAddr: String,
    @NCMdoField(column = "probe_email") probeEmail: String,
    // Don't include result by default to minimize the size of JSON history record.
    @NCMdoField(column = "res_type") resultType: String,
    @NCMdoField(column = "res_body_gzip") resultBodyGZip: String
) extends NCAnnotatedMdo[NCHistoryItemMdo]

object NCHistoryItemMdo {
    implicit val x: RsParser[NCHistoryItemMdo] =
        NCAnnotatedMdo.mkRsParser(classOf[NCHistoryItemMdo])
}
