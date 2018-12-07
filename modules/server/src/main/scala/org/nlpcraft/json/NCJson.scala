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
import org.nlpcraft.apicodes.NCApiStatusCode.NCApiStatusCode

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
    @throws[NCJ] lazy val dbType: String = value2[String]("dbType", _.trim)
    @throws[NCJ] lazy val dbName: String = value2[String]("dbName", _.trim)
    @throws[NCJ] lazy val dbHost: String = value2[String]("dbHost", _.trim)
    @throws[NCJ] lazy val dbPort: Int = value[Int, BigInt]("dbPort",_.intValue())
    @throws[NCJ] lazy val dbUser: String = value2[String]("dbUser", _.trim)
    @throws[NCJ] lazy val dbPasswd: String = value2[String]("dbPasswd", _.trim)
    @throws[NCJ] lazy val strSeqVal: Seq[String] = value2[Seq[String]]("strSeqVal", a ⇒ a.map(_.trim))
    @throws[NCJ] lazy val base64Obj: String = value2[String]("base64Obj", _.trim)
    @throws[NCJ] lazy val teamId: String = value2[String]("teamId", _.trim)
    @throws[NCJ] lazy val teamName: String = value2[String]("teamName", _.trim)
    @throws[NCJ] lazy val respTopic: String = value2[String]("respTopic", _.trim)
    @throws[NCJ] lazy val tokType: String = value2[String]("tokType", _.trim)
    @throws[NCJ] lazy val strVal: String = value2[String]("strVal", _.trim)
    @throws[NCJ] lazy val apiVer: Int = value[Int, BigInt]("apiVer", _.intValue())
    @throws[NCJ] lazy val userId: Long = valueX[Long, BigInt](Seq("userId", "usrId"), _.longValue())
    @throws[NCJ] lazy val id: Int = value[Int, BigInt]("id", _.intValue())
    @throws[NCJ] lazy val hash: Long = value[Long, BigInt]("hash", _.longValue())
    @throws[NCJ] lazy val userRank: Int = value[Int, BigInt]("userRank", _.intValue())
    @throws[NCJ] lazy val rcvTstamp: Long = value[Long, BigInt]("rcvTstamp", _.longValue())
    @throws[NCJ] lazy val txt: String = value2[String]("txt", _.trim)
    @throws[NCJ] lazy val text: String = value2[String]("text", _.trim)
    @throws[NCJ] lazy val accessToken: String = value2[String]("accessToken", _.trim)
    @throws[NCJ] lazy val probeGuid: String = value2[String]("probeGuid", _.trim)
    @throws[NCJ] lazy val comment: String = value2[String]("comment", _.trim)
    @throws[NCJ] lazy val tmzName: String = value2[String]("tmzName", _.trim)
    @throws[NCJ] lazy val tmzAbbr: String = value2[String]("tmzAbbr", _.trim)
    @throws[NCJ] lazy val city: String = value2[String]("city", _.trim)
    @throws[NCJ] lazy val countryName: String = value2[String]("countryName", _.trim)
    @throws[NCJ] lazy val countryCode: String = value2[String]("countryCode", _.trim)
    @throws[NCJ] lazy val regionName: String = value2[String]("regionName", _.trim)
    @throws[NCJ] lazy val regionCode: String = value2[String]("regionCode", _.trim)
    @throws[NCJ] lazy val zipCode: String = value2[String]("zipCode", _.trim)
    @throws[NCJ] lazy val metroCode: Int = value[Int, BigInt]("metroCode", _.intValue)
    @throws[NCJ] lazy val userAgent: String = value2[String]("userAgent", _.trim)
    @throws[NCJ] lazy val origin: String = value2[String]("origin", _.trim)
    @throws[NCJ] lazy val debug: Boolean = value2[Boolean]("debug")
    @throws[NCJ] lazy val newDomain: Boolean = value2[Boolean]("newDomain")
    @throws[NCJ] lazy val prefs: String = value2[String]("prefs", _.trim)
    @throws[NCJ] lazy val rmtAddr: String = value2[String]("rmtAddr", _.trim)
    @throws[NCJ] lazy val cliReqId: String = value2[String]("cliReqId", _.trim)
    @throws[NCJ] lazy val curateTxt: String = value2[String]("curateTxt", _.trim)
    @throws[NCJ] lazy val curateHint: String = value2[String]("curateHint", _.trim)
    @throws[NCJ] lazy val cliDateTime: String = value2[String]("cliDateTime", _.trim)
    @throws[NCJ] lazy val restNodeId: String = value2[String]("restNodeId", _.trim)
    @throws[NCJ] lazy val restNodeIps: List[String] = value2[List[String]]("restNodeIps", a ⇒ a.map(_.trim))
    @throws[NCJ] lazy val srvReqId: String = value2[String]("srvReqId", _.trim)
    @throws[NCJ] lazy val srvReqIds: List[String] = value2[List[String]]("srvReqIds")
    @throws[NCJ] lazy val firstName: String = value2[String]("firstName", _.trim)
    @throws[NCJ] lazy val lastName: String = value2[String]("lastName", _.trim)
    @throws[NCJ] lazy val phone: String = value2[String]("phone", _.trim)
    @throws[NCJ] lazy val department: String = value2[String]("department", _.trim)
    @throws[NCJ] lazy val title: String = value2[String]("title", _.trim)
    @throws[NCJ] lazy val avatarUrl: String = value2[String]("avatarUrl", _.trim)
    @throws[NCJ] lazy val syncReqRes: Boolean = value2[Boolean]("syncReqRes")
    @throws[NCJ] lazy val syncReqCmd: String = value2[String]("syncReqCmd", _.trim)
    @throws[NCJ] lazy val password: String = value2[String]("passwd", _.trim)
    @throws[NCJ] lazy val idTkn: String = value2[String]("idTkn", _.trim)
    @throws[NCJ] lazy val authCode: String = value2[String]("authCode", _.trim)
    @throws[NCJ] lazy val loginTkn: String = value2[String]("loginTkn", _.trim)
    @throws[NCJ] lazy val tkn: String = value2[String]("tkn", _.trim)
    @throws[NCJ] lazy val domain: String = value2[String]("domain", _.trim)
    @throws[NCJ] lazy val email: String = value2[String]("email", G.normalizeEmail)
    @throws[NCJ] lazy val code: String = value2[String]("code", _.trim)
    @throws[NCJ] lazy val newPassword: String = value2[String]("newPassword", _.trim)
    @throws[NCJ] lazy val oldPassword: String = value2[String]("oldPassword", _.trim)
    @throws[NCJ] lazy val probeToken: String = value2[String]("probeToken", _.trim)
    @throws[NCJ] lazy val name: String = value2[String]("name", _.trim)
    @throws[NCJ] lazy val website: String = value2[String]("website", _.trim)
    @throws[NCJ] lazy val region: String = value2[String]("region", _.trim)
    @throws[NCJ] lazy val address: String = value2[String]("address", _.trim)
    @throws[NCJ] lazy val country: String = value2[String]("country", _.trim)
    @throws[NCJ] lazy val postalCode: String = value2[String]("postalCode", _.trim)
    @throws[NCJ] lazy val synonym: String = value2[String]("synonym", _.trim)
    @throws[NCJ] lazy val companyName: String = value2[String]("companyName", _.trim)
    @throws[NCJ] lazy val dsId: Long = value[Long, BigInt]("dsId", _.longValue())
    @throws[NCJ] lazy val dsName: String = value2[String]("dsName", _.trim)
    @throws[NCJ] lazy val dsDesc: String = value2[String]("dsDesc", _.trim)
    @throws[NCJ] lazy val mdlName: String = value2[String]("mdlName", _.trim)
    @throws[NCJ] lazy val mdlVer: String = value2[String]("mdlVer", _.trim)
    @throws[NCJ] lazy val mdlId: String = value2[String]("mdlId", _.trim)
    @throws[NCJ] lazy val modelName: String = value2[String]("modelName", _.trim)
    @throws[NCJ] lazy val modelId: String = value2[String]("modelId", _.trim)
    @throws[NCJ] lazy val modelVendor: String = value2[String]("modelVendor", _.trim)
    @throws[NCJ] lazy val modelVersion: String = value2[String]("modelVersion", _.trim)
    @throws[NCJ] lazy val status: NCApiStatusCode = value[NCApiStatusCode.NCApiStatusCode, String]("status", NCApiStatusCode.byName)
    @throws[NCJ] lazy val message: String = value2[String]("message", _.trim)
    @throws[NCJ] lazy val jsExpanded: NCJson = valueJs("expanded")
    @throws[NCJ] lazy val jsNlpReq: NCJson = valueJs("nlpReq")
    @throws[NCJ] lazy val jsNlpSen: NCJson = valueJs("nlpSen")
    @throws[NCJ] lazy val jsAuto: NCJson = valueJs("auto")
    @throws[NCJ] lazy val jsJsonData: NCJson = valueJs("jsonData")
    @throws[NCJ] lazy val jsCache: NCJson = valueJs("cache")
    @throws[NCJ] lazy val editText: String = value2[String]("editText", _.trim)
    @throws[NCJ] lazy val result: Boolean = value2[Boolean]("result")
    @throws[NCJ] lazy val cacheKeyType: String = value2[String]("cacheKeyType", _.trim)
    @throws[NCJ] lazy val talkback: String = value2[String]("talkback", _.trim)
    @throws[NCJ] lazy val chartX: String = value2[String]("chartX", _.trim)
    @throws[NCJ] lazy val chartY: List[String] = value2[List[String]]("chartY", _.map(_.trim))
    @throws[NCJ] lazy val date: Long = value[Long, BigInt]("date", _.longValue())
    @throws[NCJ] lazy val metrics: String = value2[String]("metrics", _.trim)
    @throws[NCJ] lazy val maxRows: Int = value[Int, BigInt]("maxRows", _.intValue())
    @throws[NCJ] lazy val limit: Int = value[Int, BigInt]("limit", _.intValue())
    @throws[NCJ] lazy val lastMins: Int = value[Int, BigInt]("lastMins", _.intValue())
    @throws[NCJ] lazy val noteType: String = value2[String]("noteType", _.trim)
    @throws[NCJ] lazy val jsNotes: NCJson = valueJs("notes")
    @throws[NCJ] lazy val jsValues: NCJson = valueJs("values")
    @throws[NCJ] lazy val longitude: Double = value2[Double]("longitude")
    @throws[NCJ] lazy val latitude: Double = value2[Double]("latitude")
    @throws[NCJ] lazy val kind: String = value2[String]("kind", _.trim)
    @throws[NCJ] lazy val from: Long = value[Long, BigInt]("from", _.longValue())
    @throws[NCJ] lazy val to: Long = value[Long, BigInt]("to", _.longValue())
    @throws[NCJ] lazy val numFrom: Double = value2[Double]("from")
    @throws[NCJ] lazy val numFromIncl: Boolean = value2[Boolean]("fromIncl")
    @throws[NCJ] lazy val numTo: Double = value2[Double]("to")
    @throws[NCJ] lazy val numToIncl: Boolean = value2[Boolean]("toIncl")
    @throws[NCJ] lazy val numIsRangeCondition: Boolean = value2[Boolean]("isRangeCondition")
    @throws[NCJ] lazy val numIsEqualCondition: Boolean = value2[Boolean]("isEqualCondition")
    @throws[NCJ] lazy val numIsNotEqualCondition: Boolean = value2[Boolean]("isNotEqualCondition")
    @throws[NCJ] lazy val numIsFromNegativeInfinity: Boolean = value2[Boolean]("isFromNegativeInfinity")
    @throws[NCJ] lazy val numIsToPositiveInfinity: Boolean = value2[Boolean]("isToPositiveInfinity")
    @throws[NCJ] lazy val chartEnabled: Boolean = value2[Boolean]("chartEnabled")
    @throws[NCJ] lazy val chartType: String = value2[String]("chartType", _.trim)
    @throws[NCJ] lazy val chartXAxis: String = value2[String]("chartXAxis", _.trim)
    @throws[NCJ] lazy val chartDataSeries1: String = value2[String]("chartDataSeries1", _.trim)
    @throws[NCJ] lazy val chartDataSeries2: String = value2[String]("chartDataSeries2", _.trim)
    @throws[NCJ] lazy val chartDataSeries3: String = value2[String]("chartDataSeries3", _.trim)
    @throws[NCJ] lazy val all: Boolean = value2[Boolean]("all")
    @throws[NCJ] lazy val okToSuggest: Boolean = value2[Boolean]("okToSuggest")
    @throws[NCJ] lazy val suggest: String = value2[String]("suggest")
    @throws[NCJ] lazy val url: String = value2[String]("url")
    @throws[NCJ] lazy val probeId: String = value2[String]("probeId")
    @throws[NCJ] lazy val suggestLimit: Int = value[Int, BigInt]("suggestLimit", _.intValue())
    @throws[NCJ] lazy val enable: Boolean = value2[Boolean]("enable")
    @throws[NCJ] lazy val companyId: Long = value[Long, BigInt]("companyId", _.longValue())
    @throws[NCJ] lazy val notifyFlag: Boolean = value2[Boolean]("notify")
    @throws[NCJ] lazy val pendingFlag: Boolean = value2[Boolean]("pending")
    @throws[NCJ] lazy val flag: Boolean = value2[Boolean]("flag")
    @throws[NCJ] lazy val auto: Boolean = value2[Boolean]("auto")
    @throws[NCJ] lazy val dashboardItemId: Long = value[Long, BigInt]("dashboardItemId", _.longValue())

    // Optional lazy extractors.
    @throws[NCJ] lazy val phoneOpt: Option[String] = value2Opt[String]("phone", _.trim)
    @throws[NCJ] lazy val departmentOpt: Option[String] = value2Opt[String]("department", _.trim)
    @throws[NCJ] lazy val titleOpt: Option[String] = value2Opt[String]("title", _.trim)
    @throws[NCJ] lazy val modelConfigOpt: Option[String] = value2Opt[String]("modelConfig", _.trim)
    @throws[NCJ] lazy val mdlCfgOpt: Option[String] = value2Opt[String]("mdlCfg", _.trim)
    @throws[NCJ] lazy val apiVerOpt: Option[Int] = valueOpt[Int, BigInt]("apiVer", _.intValue())
    @throws[NCJ] lazy val userIdOpt: Option[Long] = valueOpt[Long, BigInt]("userId", _.longValue())
    @throws[NCJ] lazy val companyIdOpt: Option[Long] = valueOpt[Long, BigInt]("companyId", _.longValue())
    @throws[NCJ] lazy val idOpt: Option[Int] = valueOpt[Int, BigInt]("id", _.intValue())
    @throws[NCJ] lazy val hashOpt: Option[Long] = valueOpt[Long, BigInt]("hash", _.longValue())
    @throws[NCJ] lazy val userRankOpt: Option[Int] = valueOpt[Int, BigInt]("userRank", _.intValue())
    @throws[NCJ] lazy val rcvTstampOpt: Option[Long] = valueOpt[Long, BigInt]("rcvTstamp", _.longValue())
    @throws[NCJ] lazy val txtOpt: Option[String] = value2Opt[String]("txt", _.trim)
    @throws[NCJ] lazy val userAgentOpt: Option[String] = value2Opt[String]("userAgent", _.trim)
    @throws[NCJ] lazy val debugOpt: Option[Boolean] = value2Opt[Boolean]("debug")
    @throws[NCJ] lazy val oldPasswordOpt: Option[String] = value2Opt[ String]("oldPassword", _.trim)
    @throws[NCJ] lazy val rmtAddrOpt: Option[String] = value2Opt[ String]("rmtAddr", _.trim)
    @throws[NCJ] lazy val cliReqIdOpt: Option[String] = value2Opt[String]("cliReqId", _.trim)
    @throws[NCJ] lazy val cliDateTimeOpt: Option[String] = value2Opt[String]("cliDateTime", _.trim)
    @throws[NCJ] lazy val restNodeIdOpt: Option[String] = value2Opt[String]("restNodeId", _.trim)
    @throws[NCJ] lazy val restNodeIpsOpt: Option[List[String]] = value2Opt[List[String]]("restNodeIps", a ⇒ a.map(_.trim))
    @throws[NCJ] lazy val srvReqIdOpt: Option[String] = value2Opt[String]("srvReqId", _.trim)
    @throws[NCJ] lazy val srvReqIdsOpt: Option[List[String]] = value2Opt[List[String]]("srvReqIds")
    @throws[NCJ] lazy val firstNameOpt: Option[String] = value2Opt[String]("firstName", _.trim)
    @throws[NCJ] lazy val lastNameOpt: Option[String] = value2Opt[String]("lastName", _.trim)
    @throws[NCJ] lazy val avatarUrlOpt: Option[String] = value2Opt[String]("avatarUrl", _.trim)
    @throws[NCJ] lazy val syncReqResOpt: Option[Boolean] = value2Opt[Boolean]("syncReqRes")
    @throws[NCJ] lazy val syncReqCmdOpt: Option[String] = value2Opt[String]("syncReqCmd", _.trim)
    @throws[NCJ] lazy val passwdOpt: Option[String] = value2Opt[String]("passwd", _.trim)
    @throws[NCJ] lazy val idTknOpt: Option[String] = value2Opt[String]("idTkn", _.trim)
    @throws[NCJ] lazy val authCodeOpt: Option[String] = value2Opt[String]("authCode", _.trim)
    @throws[NCJ] lazy val loginTknOpt: Option[String] = value2Opt[String]("loginTkn", _.trim)
    @throws[NCJ] lazy val tknOpt: Option[String] = value2Opt[String]("tkn", _.trim)
    @throws[NCJ] lazy val domainOpt: Option[String] = value2Opt[String]("domain", _.trim)
    @throws[NCJ] lazy val emailOpt: Option[String] = value2Opt[String]("email", G.normalizeEmail)
    @throws[NCJ] lazy val nameOpt: Option[String] = value2Opt[String]("name", _.trim)
    @throws[NCJ] lazy val guidOpt: Option[String] = value2Opt[String]("guid", _.trim)
    @throws[NCJ] lazy val companyNameOpt: Option[String] = value2Opt[String]("companyName", _.trim)
    @throws[NCJ] lazy val dsIdOpt: Option[Long] = valueOpt[Long, BigInt]("dsId", _.longValue())
    @throws[NCJ] lazy val dsTypeIdOpt: Option[String] = value2Opt[String]("dsTypeId", _.trim)
    @throws[NCJ] lazy val dsNameOpt: Option[String] = value2Opt[String]("dsName", _.trim)
    @throws[NCJ] lazy val dashboardItemIdOpt: Option[Long] = valueOpt[Long, BigInt]("dashboardItemId", _.longValue())
    @throws[NCJ] lazy val statusOpt: Option[NCApiStatusCode] = valueOpt[NCApiStatusCode, String]("status", NCApiStatusCode.byName)
    @throws[NCJ] lazy val messageOpt: Option[String] = value2Opt[String]("message", _.trim)
    @throws[NCJ] lazy val editTextOpt: Option[String] = value2Opt[String]("editText", _.trim)
    @throws[NCJ] lazy val saveOpt: Option[Boolean] = value2Opt[Boolean]("save")
    @throws[NCJ] lazy val sqlOpt: Option[String] = value2Opt[String]("sql", _.trim)
    @throws[NCJ] lazy val dimensionsOpt: Option[String] = value2Opt[String]("dimensions", _.trim)
    @throws[NCJ] lazy val filtersOpt: Option[String] = value2Opt[String]("filters", _.trim)
    @throws[NCJ] lazy val segmentOpt: Option[String] = value2Opt[String]("segment", _.trim)
    @throws[NCJ] lazy val sortsOpt: Option[String] = value2Opt[String]("sorts", _.trim)
    @throws[NCJ] lazy val startIndexOpt: Option[Int] = valueOpt[Int, BigInt]("startIndex", _.intValue())
    @throws[NCJ] lazy val fromOpt: Option[Long] = valueOpt[Long, BigInt]("from", _.longValue())
    @throws[NCJ] lazy val toOpt: Option[Long] = valueOpt[Long, BigInt]("to", _.longValue())
    @throws[NCJ] lazy val valueListOpt: Option[List[String]] = value2Opt[List[String]]("value", _.map(_.trim))
    @throws[NCJ] lazy val sortAscOpt: Option[Boolean] = value2Opt[Boolean]("sortAsc")
    @throws[NCJ] lazy val sortByOpt: Option[String] = value2Opt[String]("sortBy", _.trim)
    @throws[NCJ] lazy val stopWordOpt: Option[Boolean] = value2Opt[Boolean]("stopWord")
    @throws[NCJ] lazy val stemOpt: Option[String] = value2Opt[String]("stem", _.trim)
    @throws[NCJ] lazy val notifyFlagOpt: Option[Boolean] = value2Opt[Boolean]("notify")
    @throws[NCJ] lazy val refIdOpt: Option[String] = value2Opt[String]("refId", _.trim)
    @throws[NCJ] lazy val tmzNameOpt: Option[String] = value2Opt[String]("tmzName", _.trim)
    @throws[NCJ] lazy val tmzAbbrOpt: Option[String] = value2Opt[String]("tmzAbbr", _.trim)
    @throws[NCJ] lazy val cityOpt: Option[String] = value2Opt[String]("city", _.trim)
    @throws[NCJ] lazy val countryNameOpt: Option[String] = value2Opt[String]("countryName", _.trim)
    @throws[NCJ] lazy val countryCodeOpt: Option[String] = value2Opt[String]("countryCode", _.trim)
    @throws[NCJ] lazy val regionNameOpt: Option[String] = value2Opt[String]("regionName", _.trim)
    @throws[NCJ] lazy val regionCodeOpt: Option[String] = value2Opt[String]("regionCode", _.trim)
    @throws[NCJ] lazy val zipCodeOpt: Option[String] = value2Opt[String]("zipCode", _.trim)
    @throws[NCJ] lazy val metroCodeOpt: Option[Int] = valueOpt[Int, BigInt]("metroCode", _.intValue)
    @throws[NCJ] lazy val longitudeOpt: Option[Double] = value2Opt[Double]("longitude")
    @throws[NCJ] lazy val latitudeOpt: Option[Double] = value2Opt[Double]("latitude")

    /**
     * Gets JSON value with given types and name.
     *
     * @param fn Field name.
     * @param f Function to convert from `T2` to `T1`.
     * @tparam T1 Final type of the value.
     * @tparam T2 Intermediate (Lift JSON) type of value.
     */
    @throws[NCJ]
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
    @throws[NCJ]
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
    @throws[NCJ]
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
    @throws[NCJ]
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
    @throws[NCJ]
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
    private type NCJ = NCJsonException

    // Specific control flow exceptions.
    case class InvalidJson(js: String) extends NCJ(s"Malformed JSON syntax in: $js") with LazyLogging {
        // Log right away.
        logger.error(s"Malformed JSON syntax in: $js")
    }

    case class InvalidJsonField(fn: String, cause: Throwable) extends NCJ(s"Invalid '$fn' JSON field <" +
        cause.getMessage + ">", cause) with LazyLogging {
        require(cause != null)

        // Log right away.
        logger.error(s"Invalid '$fn' JSON field <${cause.getMessage}>")
    }

    case class MissingJsonField(fn: String) extends NCJ(s"Missing mandatory '$fn' JSON field.") with LazyLogging {
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
    @throws[NCJ]
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
    @throws[NCJ]
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
//    @throws[NCJ]
//    def apply(x: RequestContext): NCJson = {
//        var js = x.request.entity.asString
//
//        if (js != null && js.isEmpty)
//            js = "{}"
//
//        js match {
//            case s: String if s != null ⇒ NCJson(s)
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
    implicit def x2(likeJs: NCJsonLike): JValue = likeJs.toJson.json
    implicit def x3(likeJs: NCJsonLike): NCJson = likeJs.toJson
    implicit def x4(js: NCJson): String = js.compact
    implicit def x4(js: NCJsonLike): String = js.toJson.compact
}
