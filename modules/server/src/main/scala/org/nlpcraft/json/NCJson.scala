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

package org.nlpcraft.json

import java.io.{IOException, _}
import java.util.zip._

import com.typesafe.scalalogging.LazyLogging
import net.liftweb.json.{compact => liftCompact, pretty => liftPretty, render => liftRender, _}
import org.nlpcraft._
import org.nlpcraft.apicodes.NCApiStatusCode
import org.nlpcraft.apicodes.NCApiStatusCode.DLApiStatusCode

import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.util.matching.Regex

/**
 * Project-wide, Lift-based general JSON wrapper.
 *
 */
class NCJson(val json: JValue) {
    import NCJson._

    require(json != null)

    // Delegate to underlying JValue.
    override def hashCode(): Int = json.hashCode()
    override def equals(obj: scala.Any): Boolean = json.equals(obj)

    // Convenient lazy extractors.
    @throws[DLJ] lazy val dbType: String = value2[String]("dbType", _.trim)
    @throws[DLJ] lazy val dbName: String = value2[String]("dbName", _.trim)
    @throws[DLJ] lazy val dbHost: String = value2[String]("dbHost", _.trim)
    @throws[DLJ] lazy val dbPort: Int = value[Int, BigInt]("dbPort",_.intValue())
    @throws[DLJ] lazy val dbUser: String = value2[String]("dbUser", _.trim)
    @throws[DLJ] lazy val dbPasswd: String = value2[String]("dbPasswd", _.trim)
    @throws[DLJ] lazy val strSeqVal: Seq[String] = value2[Seq[String]]("strSeqVal", a ⇒ a.map(_.trim))
    @throws[DLJ] lazy val base64Obj: String = value2[String]("base64Obj", _.trim)
    @throws[DLJ] lazy val teamId: String = value2[String]("teamId", _.trim)
    @throws[DLJ] lazy val teamName: String = value2[String]("teamName", _.trim)
    @throws[DLJ] lazy val respTopic: String = value2[String]("respTopic", _.trim)
    @throws[DLJ] lazy val tokType: String = value2[String]("tokType", _.trim)
    @throws[DLJ] lazy val strVal: String = value2[String]("strVal", _.trim)
    @throws[DLJ] lazy val apiVer: Int = value[Int, BigInt]("apiVer", _.intValue())
    @throws[DLJ] lazy val userId: Long = valueX[Long, BigInt](Seq("userId", "usrId"), _.longValue())
    @throws[DLJ] lazy val id: Int = value[Int, BigInt]("id", _.intValue())
    @throws[DLJ] lazy val hash: Long = value[Long, BigInt]("hash", _.longValue())
    @throws[DLJ] lazy val userRank: Int = value[Int, BigInt]("userRank", _.intValue())
    @throws[DLJ] lazy val rcvTstamp: Long = value[Long, BigInt]("rcvTstamp", _.longValue())
    @throws[DLJ] lazy val txt: String = value2[String]("txt", _.trim)
    @throws[DLJ] lazy val text: String = value2[String]("text", _.trim)
    @throws[DLJ] lazy val accessToken: String = value2[String]("accessToken", _.trim)
    @throws[DLJ] lazy val probeGuid: String = value2[String]("probeGuid", _.trim)
    @throws[DLJ] lazy val comment: String = value2[String]("comment", _.trim)
    @throws[DLJ] lazy val tmzName: String = value2[String]("tmzName", _.trim)
    @throws[DLJ] lazy val tmzAbbr: String = value2[String]("tmzAbbr", _.trim)
    @throws[DLJ] lazy val city: String = value2[String]("city", _.trim)
    @throws[DLJ] lazy val countryName: String = value2[String]("countryName", _.trim)
    @throws[DLJ] lazy val countryCode: String = value2[String]("countryCode", _.trim)
    @throws[DLJ] lazy val regionName: String = value2[String]("regionName", _.trim)
    @throws[DLJ] lazy val regionCode: String = value2[String]("regionCode", _.trim)
    @throws[DLJ] lazy val zipCode: String = value2[String]("zipCode", _.trim)
    @throws[DLJ] lazy val metroCode: Int = value[Int, BigInt]("metroCode", _.intValue)
    @throws[DLJ] lazy val userAgent: String = value2[String]("userAgent", _.trim)
    @throws[DLJ] lazy val origin: String = value2[String]("origin", _.trim)
    @throws[DLJ] lazy val debug: Boolean = value2[Boolean]("debug")
    @throws[DLJ] lazy val newDomain: Boolean = value2[Boolean]("newDomain")
    @throws[DLJ] lazy val prefs: String = value2[String]("prefs", _.trim)
    @throws[DLJ] lazy val rmtAddr: String = value2[String]("rmtAddr", _.trim)
    @throws[DLJ] lazy val cliReqId: String = value2[String]("cliReqId", _.trim)
    @throws[DLJ] lazy val curateTxt: String = value2[String]("curateTxt", _.trim)
    @throws[DLJ] lazy val curateHint: String = value2[String]("curateHint", _.trim)
    @throws[DLJ] lazy val cliDateTime: String = value2[String]("cliDateTime", _.trim)
    @throws[DLJ] lazy val restNodeId: String = value2[String]("restNodeId", _.trim)
    @throws[DLJ] lazy val restNodeIps: List[String] = value2[List[String]]("restNodeIps", a ⇒ a.map(_.trim))
    @throws[DLJ] lazy val srvReqId: String = value2[String]("srvReqId", _.trim)
    @throws[DLJ] lazy val srvReqIds: List[String] = value2[List[String]]("srvReqIds")
    @throws[DLJ] lazy val firstName: String = value2[String]("firstName", _.trim)
    @throws[DLJ] lazy val lastName: String = value2[String]("lastName", _.trim)
    @throws[DLJ] lazy val phone: String = value2[String]("phone", _.trim)
    @throws[DLJ] lazy val department: String = value2[String]("department", _.trim)
    @throws[DLJ] lazy val title: String = value2[String]("title", _.trim)
    @throws[DLJ] lazy val avatarUrl: String = value2[String]("avatarUrl", _.trim)
    @throws[DLJ] lazy val syncReqRes: Boolean = value2[Boolean]("syncReqRes")
    @throws[DLJ] lazy val syncReqCmd: String = value2[String]("syncReqCmd", _.trim)
    @throws[DLJ] lazy val password: String = value2[String]("passwd", _.trim)
    @throws[DLJ] lazy val idTkn: String = value2[String]("idTkn", _.trim)
    @throws[DLJ] lazy val authCode: String = value2[String]("authCode", _.trim)
    @throws[DLJ] lazy val loginTkn: String = value2[String]("loginTkn", _.trim)
    @throws[DLJ] lazy val tkn: String = value2[String]("tkn", _.trim)
    @throws[DLJ] lazy val domain: String = value2[String]("domain", _.trim)
    @throws[DLJ] lazy val email: String = value2[String]("email", G.normalizeEmail)
    @throws[DLJ] lazy val code: String = value2[String]("code", _.trim)
    @throws[DLJ] lazy val newPassword: String = value2[String]("newPassword", _.trim)
    @throws[DLJ] lazy val oldPassword: String = value2[String]("oldPassword", _.trim)
    @throws[DLJ] lazy val probeToken: String = value2[String]("probeToken", _.trim)
    @throws[DLJ] lazy val name: String = value2[String]("name", _.trim)
    @throws[DLJ] lazy val website: String = value2[String]("website", _.trim)
    @throws[DLJ] lazy val region: String = value2[String]("region", _.trim)
    @throws[DLJ] lazy val address: String = value2[String]("address", _.trim)
    @throws[DLJ] lazy val country: String = value2[String]("country", _.trim)
    @throws[DLJ] lazy val postalCode: String = value2[String]("postalCode", _.trim)
    @throws[DLJ] lazy val synonym: String = value2[String]("synonym", _.trim)
    @throws[DLJ] lazy val companyName: String = value2[String]("companyName", _.trim)
    @throws[DLJ] lazy val dsId: Long = value[Long, BigInt]("dsId", _.longValue())
    @throws[DLJ] lazy val dsName: String = value2[String]("dsName", _.trim)
    @throws[DLJ] lazy val dsDesc: String = value2[String]("dsDesc", _.trim)
    @throws[DLJ] lazy val mdlName: String = value2[String]("mdlName", _.trim)
    @throws[DLJ] lazy val mdlVer: String = value2[String]("mdlVer", _.trim)
    @throws[DLJ] lazy val mdlId: String = value2[String]("mdlId", _.trim)
    @throws[DLJ] lazy val modelName: String = value2[String]("modelName", _.trim)
    @throws[DLJ] lazy val modelId: String = value2[String]("modelId", _.trim)
    @throws[DLJ] lazy val modelVendor: String = value2[String]("modelVendor", _.trim)
    @throws[DLJ] lazy val modelVersion: String = value2[String]("modelVersion", _.trim)
    @throws[DLJ] lazy val status: DLApiStatusCode = value[NCApiStatusCode.DLApiStatusCode, String]("status", NCApiStatusCode.byName)
    @throws[DLJ] lazy val message: String = value2[String]("message", _.trim)
    @throws[DLJ] lazy val jsExpanded: NCJson = valueJs("expanded")
    @throws[DLJ] lazy val jsNlpReq: NCJson = valueJs("nlpReq")
    @throws[DLJ] lazy val jsNlpSen: NCJson = valueJs("nlpSen")
    @throws[DLJ] lazy val jsAuto: NCJson = valueJs("auto")
    @throws[DLJ] lazy val jsJsonData: NCJson = valueJs("jsonData")
    @throws[DLJ] lazy val jsCache: NCJson = valueJs("cache")
    @throws[DLJ] lazy val editText: String = value2[String]("editText", _.trim)
    @throws[DLJ] lazy val result: Boolean = value2[Boolean]("result")
    @throws[DLJ] lazy val cacheKeyType: String = value2[String]("cacheKeyType", _.trim)
    @throws[DLJ] lazy val talkback: String = value2[String]("talkback", _.trim)
    @throws[DLJ] lazy val chartX: String = value2[String]("chartX", _.trim)
    @throws[DLJ] lazy val chartY: List[String] = value2[List[String]]("chartY", _.map(_.trim))
    @throws[DLJ] lazy val date: Long = value[Long, BigInt]("date", _.longValue())
    @throws[DLJ] lazy val metrics: String = value2[String]("metrics", _.trim)
    @throws[DLJ] lazy val maxRows: Int = value[Int, BigInt]("maxRows", _.intValue())
    @throws[DLJ] lazy val limit: Int = value[Int, BigInt]("limit", _.intValue())
    @throws[DLJ] lazy val lastMins: Int = value[Int, BigInt]("lastMins", _.intValue())
    @throws[DLJ] lazy val noteType: String = value2[String]("noteType", _.trim)
    @throws[DLJ] lazy val jsNotes: NCJson = valueJs("notes")
    @throws[DLJ] lazy val jsValues: NCJson = valueJs("values")
    @throws[DLJ] lazy val longitude: Double = value2[Double]("longitude")
    @throws[DLJ] lazy val latitude: Double = value2[Double]("latitude")
    @throws[DLJ] lazy val kind: String = value2[String]("kind", _.trim)
    @throws[DLJ] lazy val from: Long = value[Long, BigInt]("from", _.longValue())
    @throws[DLJ] lazy val to: Long = value[Long, BigInt]("to", _.longValue())
    @throws[DLJ] lazy val numFrom: Double = value2[Double]("from")
    @throws[DLJ] lazy val numFromIncl: Boolean = value2[Boolean]("fromIncl")
    @throws[DLJ] lazy val numTo: Double = value2[Double]("to")
    @throws[DLJ] lazy val numToIncl: Boolean = value2[Boolean]("toIncl")
    @throws[DLJ] lazy val numIsRangeCondition: Boolean = value2[Boolean]("isRangeCondition")
    @throws[DLJ] lazy val numIsEqualCondition: Boolean = value2[Boolean]("isEqualCondition")
    @throws[DLJ] lazy val numIsNotEqualCondition: Boolean = value2[Boolean]("isNotEqualCondition")
    @throws[DLJ] lazy val numIsFromNegativeInfinity: Boolean = value2[Boolean]("isFromNegativeInfinity")
    @throws[DLJ] lazy val numIsToPositiveInfinity: Boolean = value2[Boolean]("isToPositiveInfinity")
    @throws[DLJ] lazy val chartEnabled: Boolean = value2[Boolean]("chartEnabled")
    @throws[DLJ] lazy val chartType: String = value2[String]("chartType", _.trim)
    @throws[DLJ] lazy val chartXAxis: String = value2[String]("chartXAxis", _.trim)
    @throws[DLJ] lazy val chartDataSeries1: String = value2[String]("chartDataSeries1", _.trim)
    @throws[DLJ] lazy val chartDataSeries2: String = value2[String]("chartDataSeries2", _.trim)
    @throws[DLJ] lazy val chartDataSeries3: String = value2[String]("chartDataSeries3", _.trim)
    @throws[DLJ] lazy val all: Boolean = value2[Boolean]("all")
    @throws[DLJ] lazy val okToSuggest: Boolean = value2[Boolean]("okToSuggest")
    @throws[DLJ] lazy val suggest: String = value2[String]("suggest")
    @throws[DLJ] lazy val url: String = value2[String]("url")
    @throws[DLJ] lazy val probeId: String = value2[String]("probeId")
    @throws[DLJ] lazy val suggestLimit: Int = value[Int, BigInt]("suggestLimit", _.intValue())
    @throws[DLJ] lazy val enable: Boolean = value2[Boolean]("enable")
    @throws[DLJ] lazy val companyId: Long = value[Long, BigInt]("companyId", _.longValue())
    @throws[DLJ] lazy val notifyFlag: Boolean = value2[Boolean]("notify")
    @throws[DLJ] lazy val pendingFlag: Boolean = value2[Boolean]("pending")
    @throws[DLJ] lazy val flag: Boolean = value2[Boolean]("flag")
    @throws[DLJ] lazy val auto: Boolean = value2[Boolean]("auto")
    @throws[DLJ] lazy val dashboardItemId: Long = value[Long, BigInt]("dashboardItemId", _.longValue())

    // Optional lazy extractors.
    @throws[DLJ] lazy val phoneOpt: Option[String] = value2Opt[String]("phone", _.trim)
    @throws[DLJ] lazy val departmentOpt: Option[String] = value2Opt[String]("department", _.trim)
    @throws[DLJ] lazy val titleOpt: Option[String] = value2Opt[String]("title", _.trim)
    @throws[DLJ] lazy val modelConfigOpt: Option[String] = value2Opt[String]("modelConfig", _.trim)
    @throws[DLJ] lazy val mdlCfgOpt: Option[String] = value2Opt[String]("mdlCfg", _.trim)
    @throws[DLJ] lazy val apiVerOpt: Option[Int] = valueOpt[Int, BigInt]("apiVer", _.intValue())
    @throws[DLJ] lazy val userIdOpt: Option[Long] = valueOpt[Long, BigInt]("userId", _.longValue())
    @throws[DLJ] lazy val companyIdOpt: Option[Long] = valueOpt[Long, BigInt]("companyId", _.longValue())
    @throws[DLJ] lazy val idOpt: Option[Int] = valueOpt[Int, BigInt]("id", _.intValue())
    @throws[DLJ] lazy val hashOpt: Option[Long] = valueOpt[Long, BigInt]("hash", _.longValue())
    @throws[DLJ] lazy val userRankOpt: Option[Int] = valueOpt[Int, BigInt]("userRank", _.intValue())
    @throws[DLJ] lazy val rcvTstampOpt: Option[Long] = valueOpt[Long, BigInt]("rcvTstamp", _.longValue())
    @throws[DLJ] lazy val txtOpt: Option[String] = value2Opt[String]("txt", _.trim)
    @throws[DLJ] lazy val userAgentOpt: Option[String] = value2Opt[String]("userAgent", _.trim)
    @throws[DLJ] lazy val debugOpt: Option[Boolean] = value2Opt[Boolean]("debug")
    @throws[DLJ] lazy val oldPasswordOpt: Option[String] = value2Opt[ String]("oldPassword", _.trim)
    @throws[DLJ] lazy val rmtAddrOpt: Option[String] = value2Opt[ String]("rmtAddr", _.trim)
    @throws[DLJ] lazy val cliReqIdOpt: Option[String] = value2Opt[String]("cliReqId", _.trim)
    @throws[DLJ] lazy val cliDateTimeOpt: Option[String] = value2Opt[String]("cliDateTime", _.trim)
    @throws[DLJ] lazy val restNodeIdOpt: Option[String] = value2Opt[String]("restNodeId", _.trim)
    @throws[DLJ] lazy val restNodeIpsOpt: Option[List[String]] = value2Opt[List[String]]("restNodeIps", a ⇒ a.map(_.trim))
    @throws[DLJ] lazy val srvReqIdOpt: Option[String] = value2Opt[String]("srvReqId", _.trim)
    @throws[DLJ] lazy val srvReqIdsOpt: Option[List[String]] = value2Opt[List[String]]("srvReqIds")
    @throws[DLJ] lazy val firstNameOpt: Option[String] = value2Opt[String]("firstName", _.trim)
    @throws[DLJ] lazy val lastNameOpt: Option[String] = value2Opt[String]("lastName", _.trim)
    @throws[DLJ] lazy val avatarUrlOpt: Option[String] = value2Opt[String]("avatarUrl", _.trim)
    @throws[DLJ] lazy val syncReqResOpt: Option[Boolean] = value2Opt[Boolean]("syncReqRes")
    @throws[DLJ] lazy val syncReqCmdOpt: Option[String] = value2Opt[String]("syncReqCmd", _.trim)
    @throws[DLJ] lazy val passwdOpt: Option[String] = value2Opt[String]("passwd", _.trim)
    @throws[DLJ] lazy val idTknOpt: Option[String] = value2Opt[String]("idTkn", _.trim)
    @throws[DLJ] lazy val authCodeOpt: Option[String] = value2Opt[String]("authCode", _.trim)
    @throws[DLJ] lazy val loginTknOpt: Option[String] = value2Opt[String]("loginTkn", _.trim)
    @throws[DLJ] lazy val tknOpt: Option[String] = value2Opt[String]("tkn", _.trim)
    @throws[DLJ] lazy val domainOpt: Option[String] = value2Opt[String]("domain", _.trim)
    @throws[DLJ] lazy val emailOpt: Option[String] = value2Opt[String]("email", G.normalizeEmail)
    @throws[DLJ] lazy val nameOpt: Option[String] = value2Opt[String]("name", _.trim)
    @throws[DLJ] lazy val guidOpt: Option[String] = value2Opt[String]("guid", _.trim)
    @throws[DLJ] lazy val companyNameOpt: Option[String] = value2Opt[String]("companyName", _.trim)
    @throws[DLJ] lazy val dsIdOpt: Option[Long] = valueOpt[Long, BigInt]("dsId", _.longValue())
    @throws[DLJ] lazy val dsTypeIdOpt: Option[String] = value2Opt[String]("dsTypeId", _.trim)
    @throws[DLJ] lazy val dsNameOpt: Option[String] = value2Opt[String]("dsName", _.trim)
    @throws[DLJ] lazy val dashboardItemIdOpt: Option[Long] = valueOpt[Long, BigInt]("dashboardItemId", _.longValue())
    @throws[DLJ] lazy val statusOpt: Option[DLApiStatusCode] = valueOpt[DLApiStatusCode, String]("status", NCApiStatusCode.byName)
    @throws[DLJ] lazy val messageOpt: Option[String] = value2Opt[String]("message", _.trim)
    @throws[DLJ] lazy val editTextOpt: Option[String] = value2Opt[String]("editText", _.trim)
    @throws[DLJ] lazy val saveOpt: Option[Boolean] = value2Opt[Boolean]("save")
    @throws[DLJ] lazy val sqlOpt: Option[String] = value2Opt[String]("sql", _.trim)
    @throws[DLJ] lazy val dimensionsOpt: Option[String] = value2Opt[String]("dimensions", _.trim)
    @throws[DLJ] lazy val filtersOpt: Option[String] = value2Opt[String]("filters", _.trim)
    @throws[DLJ] lazy val segmentOpt: Option[String] = value2Opt[String]("segment", _.trim)
    @throws[DLJ] lazy val sortsOpt: Option[String] = value2Opt[String]("sorts", _.trim)
    @throws[DLJ] lazy val startIndexOpt: Option[Int] = valueOpt[Int, BigInt]("startIndex", _.intValue())
    @throws[DLJ] lazy val fromOpt: Option[Long] = valueOpt[Long, BigInt]("from", _.longValue())
    @throws[DLJ] lazy val toOpt: Option[Long] = valueOpt[Long, BigInt]("to", _.longValue())
    @throws[DLJ] lazy val valueListOpt: Option[List[String]] = value2Opt[List[String]]("value", _.map(_.trim))
    @throws[DLJ] lazy val sortAscOpt: Option[Boolean] = value2Opt[Boolean]("sortAsc")
    @throws[DLJ] lazy val sortByOpt: Option[String] = value2Opt[String]("sortBy", _.trim)
    @throws[DLJ] lazy val stopWordOpt: Option[Boolean] = value2Opt[Boolean]("stopWord")
    @throws[DLJ] lazy val stemOpt: Option[String] = value2Opt[String]("stem", _.trim)
    @throws[DLJ] lazy val notifyFlagOpt: Option[Boolean] = value2Opt[Boolean]("notify")
    @throws[DLJ] lazy val refIdOpt: Option[String] = value2Opt[String]("refId", _.trim)
    @throws[DLJ] lazy val tmzNameOpt: Option[String] = value2Opt[String]("tmzName", _.trim)
    @throws[DLJ] lazy val tmzAbbrOpt: Option[String] = value2Opt[String]("tmzAbbr", _.trim)
    @throws[DLJ] lazy val cityOpt: Option[String] = value2Opt[String]("city", _.trim)
    @throws[DLJ] lazy val countryNameOpt: Option[String] = value2Opt[String]("countryName", _.trim)
    @throws[DLJ] lazy val countryCodeOpt: Option[String] = value2Opt[String]("countryCode", _.trim)
    @throws[DLJ] lazy val regionNameOpt: Option[String] = value2Opt[String]("regionName", _.trim)
    @throws[DLJ] lazy val regionCodeOpt: Option[String] = value2Opt[String]("regionCode", _.trim)
    @throws[DLJ] lazy val zipCodeOpt: Option[String] = value2Opt[String]("zipCode", _.trim)
    @throws[DLJ] lazy val metroCodeOpt: Option[Int] = valueOpt[Int, BigInt]("metroCode", _.intValue)
    @throws[DLJ] lazy val longitudeOpt: Option[Double] = value2Opt[Double]("longitude")
    @throws[DLJ] lazy val latitudeOpt: Option[Double] = value2Opt[Double]("latitude")

    /**
     * Gets JSON value with given types and name.
     *
     * @param fn Field name.
     * @param f Function to convert from `T2` to `T1`.
     * @tparam T1 Final type of the value.
     * @tparam T2 Intermediate (Lift JSON) type of value.
     */
    @throws[DLJ]
    private def value[T1, T2](fn: String, f: T2 ⇒ T1): T1 =
        try
            f(json \ fn match {
                case JNothing | null ⇒ throw MissingJsonField(fn)
                case v: JValue ⇒ v.values.asInstanceOf[T2]
            })
        catch {
            case e: MissingJsonField ⇒ throw e // Rethrow.
            case e: Throwable ⇒ throw InvalidJsonField(fn, e)
        }

    /**
      *
      * @param fns Multiple possible field names.
      * @param f Function to convert from `T2` to `T1`.
      * @tparam T1 Final type of the value.
      * @tparam T2 Intermediate (Lift JSON) type of value.
      */
    @throws[DLJ]
    private def valueX[T1, T2](fns: Seq[String], f: T2 ⇒ T1): T1 = {
        var retVal: JValue = null

        for (fn ← fns if retVal == null)
            try
                json \ fn match {
                    case JNothing | null ⇒ () // No-op.
                    case v: JValue ⇒ retVal =  v
                }
            catch {
                case e: Throwable ⇒ throw InvalidJsonField(fn, e)
            }

        if (retVal != null)
            f(retVal.values.asInstanceOf[T2])
        else
            throw MissingJsonField(fns.mkString("|"))
    }

    /**
     * Convenient method to get JSON unboxed value with given type and name.
     *
     * @param fn Field name.
     * @tparam T Type of the value.
     */
    @throws[DLJ]
    def field[T](fn: String): T =
        try
            json \ fn match {
                case JNothing | null ⇒ throw MissingJsonField(fn)
                case v: JValue ⇒ v.values.asInstanceOf[T]
            }
        catch {
            case e: MissingJsonField ⇒ throw e // Rethrow.
            case e: Throwable ⇒ throw InvalidJsonField(fn, e)
        }

    /**
      * Tests whether given JSON field present or not.
      *
      * @param fn JSON field name.
      */
    def hasField(fn: String): Boolean =
        json \ fn match {
            case JNothing | null ⇒ false
            case _: JValue ⇒ true
        }

    /**
     * Convenient method to get JSON unboxed value with given type and name.
     *
     * @param fn Field name.
     * @tparam T Type of the value.
     */
    def fieldOpt[T](fn: String): Option[T] =
        try
            json \ fn match {
                case JNothing ⇒ None
                case v: JValue ⇒ Some(v.values.asInstanceOf[T])
            }
        catch {
            case _: Throwable ⇒ None
        }

    /**
     * Gets non-null JSON value with given types and name.
     *
     * @param fn Field name.
     * @param f Function to convert from `T2` to `T1`.
     * @tparam T1 Final type of the value.
     * @tparam T2 Intermediate (Lift JSON) type of value.
     */
    @throws[DLJ]
    private def valueOpt[T1, T2](fn: String, f: T2 ⇒ T1): Option[T1] =
        try
            json \ fn match {
                case JNothing ⇒ None
                case v: JValue ⇒ Some(f(v.values.asInstanceOf[T2]))
            }
        catch {
            case _: Throwable ⇒ None
        }

    // Shortcuts.
    @throws[DLJ]
    private def value2[T](fn: String, f: T ⇒ T = (t: T) ⇒ t): T = value[T, T](fn, f)
    private def value2Opt[T](fn: String, f: T ⇒ T = (t: T) ⇒ t): Option[T] = valueOpt[T, T](fn, f)

    /**
     * Extracts mandatory JSON element.
     *
     * @param fn Name.
     */
    def valueJs(fn: String): NCJson =
        try
            json \ fn match {
                case JNothing ⇒ throw MissingJsonField(fn)
                case v: JValue ⇒ v
            }
        catch {
            case e: MissingJsonField ⇒ throw e // Rethrow.
            case e: Throwable ⇒ throw InvalidJsonField(fn, e)
        }

    /**
     * Extracts optional JSON element.
     *
     * @param fn Name.
     */
    def valueJsOpt(fn: String): Option[NCJson] =
        try
            json \ fn match {
                case JNothing ⇒ None
                case v: JValue ⇒ Some(v)
            }
        catch {
            case e: MissingJsonField ⇒ throw e // Rethrow.
            case e: Throwable ⇒ throw InvalidJsonField(fn, e)
        }

    /**
     * Renders this JSON with proper new-lines and indentation (suitable for human readability).
     *
     * @return String presentation of this JSON object.
     */
    def pretty: String = liftPretty(liftRender(json))

    /**
     * Renders this JSON in a compact form (suitable for exchange).
     *
     * @return String presentation of this JSON object.
     */
    def compact: String = liftCompact(liftRender(json))

    /**
     * Zips this JSON object into array of bytes using GZIP.
     */
    def gzip(): Array[Byte] = {
        val out = new ByteArrayOutputStream(1024)

        try {
            val gzip = new GZIPOutputStream(out)

            gzip.write(compact.getBytes)

            gzip.close()

            out.toByteArray
        }
        // Let IOException to propagate unchecked (since it shouldn't appear here by the spec).
        finally {
            out.close()
        }
    }

    override def toString: String = compact
}

/**
 * Static scope for JSON wrapper.
 */
object NCJson {
    private type DLJ = NCJsonException

    // Specific control flow exceptions.
    case class InvalidJson(js: String) extends DLJ(s"Malformed JSON syntax in: $js") with LazyLogging {
        // Log right away.
        logger.error(s"Malformed JSON syntax in: $js")
    }

    case class InvalidJsonField(fn: String, cause: Throwable) extends DLJ(s"Invalid '$fn' JSON field <" +
        cause.getMessage + ">", cause) with LazyLogging {
        require(cause != null)

        // Log right away.
        logger.error(s"Invalid '$fn' JSON field <${cause.getMessage}>")
    }

    case class MissingJsonField(fn: String) extends DLJ(s"Missing mandatory '$fn' JSON field.") with LazyLogging {
        // Log right away.
        logger.error(s"Missing mandatory '$fn' JSON field.")
    }

    implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats

    // Regex for numbers with positive exponent part with explicit + in notation. Example 2E+5.
    // Also, these numbers should be pre-fixed and post-fixed by restricted JSON symbols set.
    private val EXP_REGEX = {
        val mask = "[-+]?([0-9]+\\.?[0-9]*|\\.[0-9]+)([eE]\\+[0-9]+)"
        val pre = Seq(' ', '"', '[', ',', ':')
        val post = Seq(' ', '"', ']', ',', '}')

        def makeMask(chars: Seq[Char]): String = s"[${chars.map(ch ⇒ s"\\$ch").mkString}]"

        new Regex(s"${makeMask(pre)}$mask${makeMask(post)}")
    }

    /**
     * Creates JSON wrapper from given string.
     *
     * @param js JSON string presentation.
     * @return JSON wrapper.
     */
    @throws[DLJ]
    def apply(js: String): NCJson = {
        require(js != null)

        JsonParser.parseOpt(processExpNumbers(js)) match {
            case Some(a) ⇒ new NCJson(a)
            case _ ⇒ throw InvalidJson(js)
        }
    }

    /**
     * Creates JSON wrapper from given Lift `JValue` object.
     *
     * @param json Lift `JValue` AST object.
     * @return JSON wrapper.
     */
    @throws[DLJ]
    def apply(json: JValue): NCJson = {
        require(json != null)

        new NCJson(json)
    }

// TODO: skh
//    /**
//     * Creates JSON wrapper from the POST payload of given 'spray-route' request context.
//     *
//     * @param x 'spray-route' request context.
//     * @return JSON wrapper.
//     */
//    @throws[DLJ]
//    def apply(x: RequestContext): DLJson = {
//        var js = x.request.entity.asString
//
//        if (js != null && js.isEmpty)
//            js = "{}"
//
//        js match {
//            case s: String if s != null ⇒ DLJson(s)
//            case null ⇒ throw new NCE(s"Missing HTTP request JSON POST payload in: $x")
//            case _ ⇒ throw InvalidJson(js)
//        }
//    }

    /**
     * Unzips array of bytes into string.
     *
     * @param arr Array of bytes produced by 'gzip' method.
     */
    def unzip2String(arr: Array[Byte]): String = {
        val in = new ByteArrayInputStream(arr)
        val out = new ByteArrayOutputStream(1024)

        val tmpArr = new Array[Byte](512)

        try {
            val gzip = new GZIPInputStream(in)

            var n = gzip.read(tmpArr, 0, tmpArr.length)

            while (n > 0) {
                out.write(tmpArr, 0, n)

                n = gzip.read(tmpArr, 0, tmpArr.length)
            }

            gzip.close()

            // Trim method added to delete last symbol of ZLIB compression
            // protocol (NULL - 'no error' flag) http://www.zlib.net/manual.html
            out.toString("UTF-8").trim
        }
        // Let IOException to propagate unchecked (since it shouldn't appear here by the spec).
        finally {
            out.close()
            in.close()
        }
    }

    /**
     * Unzips array of bytes into JSON object.
     *
     * @param arr Array of bytes produced by 'gzip' method.
     */
    def unzip2Json(arr: Array[Byte]): NCJson = NCJson(unzip2String(arr))

    /**
     * Reads file.
     *
     * @param f File to extract from.
     */
    private def readFile(f: File): String = removeComments(G.readFile(f, "UTF8").mkString)

    /**
      * Reads stream.
      *
      * @param in Stream to extract from.
      */
    private def readStream(in: InputStream): String = removeComments(G.readStream(in, "UTF8").mkString)

    /**
     * Extracts type `T` from given JSON `file`.
     *
     * @param f File to extract from.
     * @param ignoreCase Whether or not to ignore case.
     * @tparam T Type of the object to extract.
     */
    @throws[NCE]
    def extractFile[T: Manifest](f: java.io.File, ignoreCase: Boolean): T =
        try
            if (ignoreCase) NCJson(readFile(f).toLowerCase).json.extract[T] else NCJson(readFile(f)).json.extract[T]
        catch {
            case e: IOException ⇒ throw new NCE(s"Failed to read: ${f.getAbsolutePath}", e)
            case e: Throwable ⇒ throw new NCE(s"Failed to parse: ${f.getAbsolutePath}", e)
        }

    /**
     * Removes C-style /* */ multi-line comments from JSON.
     *
     * @param json JSON text.
     */
    private def removeComments(json: String): String = json.replaceAll("""\/\*(\*(?!\/)|[^*])*\*\/""", "")

    /**
     * Extracts type `T` from given JSON `file`.
     *
     * @param path File path to extract from.
     * @param ignoreCase Whether or not to ignore case.
     * @tparam T Type of the object to extract.
     */
    @throws[NCE]
    def extractPath[T: Manifest](path: String, ignoreCase: Boolean): T = extractFile(new java.io.File(path), ignoreCase)

    /**
      * Extracts type `T` from given JSON `file`.
      *
      * @param res Resource to extract from.
      * @param ignoreCase Whether or not to ignore case.
      * @tparam T Type of the object to extract.
      */
    @throws[NCE]
    def extractResource[T: Manifest](res: String, ignoreCase: Boolean): T =
        try {
            val in = G.getStream(res)

            if (ignoreCase) NCJson(readStream(in).toLowerCase).json.extract[T] else NCJson(readStream(in)).json.extract[T]
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Failed to read: $res", e)
            case e: Throwable ⇒ throw new NCE(s"Failed to parse: $res", e)
        }

    // Gets string with removed symbol + from exponent part of numbers.
    // It is developed to avoid Lift parsing errors during processing numbers like '2E+2'.
    @tailrec
    def processExpNumbers(s: String): String =
        EXP_REGEX.findFirstMatchIn(s) match {
            case Some(m) ⇒ processExpNumbers(m.before + m.group(0).replaceAll("\\+", "") + m.after)
            case None ⇒ s
        }

    // Implicit conversions.
    implicit def x(jv: JValue): NCJson = new NCJson(jv)
    implicit def x1(js: NCJson): JValue = js.json
    // TODO: skh
//    implicit def x2(likeJs: DLJsonLike): JValue = likeJs.toJson.json
//    implicit def x3(likeJs: DLJsonLike): DLJson = likeJs.toJson
//    implicit def x4(js: DLJson): String = js.compact
//    implicit def x4(js: DLJsonLike): String = js.toJson.compact
}
