/*
 * “Commons Clause” License, https://commonsclause.com/
 *
 * The Software is provided to you by the Licensor under the License,
 * as defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software:    NLPCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.server.db

import java.sql.Timestamp
import java.time.LocalDate

import org.nlpcraft.common.util.NCUtils
import org.nlpcraft.common.{NCLifecycle, _}
import org.nlpcraft.server.apicodes.NCApiStatusCode._
import org.nlpcraft.server.db.utils.NCSql
import org.nlpcraft.server.db.utils.NCSql.Implicits._
import org.nlpcraft.server.mdo._

/**
  * Provides basic CRUD and often used operations on RDBMS.
  * Note that all functions in this class expect outside `NCSql.sql()` block.
  */
object NCDbManager extends NCLifecycle("Database manager") {
    /**
      * Starts manager.
      */
    @throws[NCE]
    override def start(): NCLifecycle = {
        ensureStopped()

        super.start()
    }

    /**
      * Stop manager.
      */
    override def stop(): Unit = {
        checkStopping()

        super.stop()
    }

    /**
      * Checks if given hash exists in the password pool.
      *
      * @param hash Hash to check.
      */
    @throws[NCE]
    def isKnownPasswordHash(hash: String): Boolean = {
        ensureStarted()
    
        NCSql.exists("passwd_pool WHERE passwd_hash = ?", hash)
    }
    
    /**
      * Inserts password hash into anonymous password pool.
      *
      * @param hash Password hash to insert into anonymous password pool.
      */
    @throws[NCE]
    def addPasswordHash(hash: String): Unit = {
        ensureStarted()
    
        NCSql.insert("INSERT INTO passwd_pool (passwd_hash) VALUES (?)", hash)
    }
    
    /**
      * Removes password hash from anonymous password pool.
      *
      * @param hash Password hash to remove.
      */
    @throws[NCE]
    def erasePasswordHash(hash: String): Unit = {
        ensureStarted()
    
        NCSql.delete("DELETE FROM passwd_pool WHERE passwd_hash = ?", hash)
    }

    /**
      * Gets user for given email.
      *
      * @param email User's normalized email.
      * @return User MDO.
      */
    @throws[NCE]
    def getUserByEmail(email: String): Option[NCUserMdo] = {
        ensureStarted()

        NCSql.selectSingle[NCUserMdo](
            """
              |SELECT *
              |FROM nc_user
              |WHERE
              |    email = ? AND
              |    deleted = FALSE
              """.stripMargin,
            email
        )
    }
    
    /**
      * Marks user as `deleted` with given ID.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def deleteUser(usrId: Long): Unit = {
        ensureStarted()

        NCSql.markAsDeleted("nc_user", "id", usrId)
    }

    /**
      * Marks data source as `deleted` with given ID.
      *
      * @param dsId Data source ID.
      */
    @throws[NCE]
    def deleteDataSource(dsId: Long): Unit = {
        ensureStarted()
        
        NCSql.markAsDeleted("ds_instance", "id", dsId)
    }

    /**
      * Deletes data source with given ID.
      *
      * @param dsId Data source ID.
      */
    @throws[NCE]
    def eraseDataSource(dsId: Long): Unit = {
        ensureStarted()

        NCSql.delete("DELETE FROM ds_instance WHERE id = ?", dsId)
    }

    /**
      * Updates user.
      *
      * @param usrId ID of the user to update.
      * @param firstName First name.
      * @param lastName Last name.
      * @param avatarUrl Avatar URL.
      * @param isAdmin Admin flag.
      */
    @throws[NCE]
    def updateUser(
        usrId: Long,
        firstName: String,
        lastName: String,
        avatarUrl: Option[String],
        isAdmin: Boolean
    ): Int = {
        ensureStarted()

        NCSql.update(
            s"""
               |UPDATE nc_user
               |SET
               |    first_name = ?,
               |    last_name = ?,
               |    avatar_url = ?,
               |    is_admin = ?,
               |    last_modified_on = ?
               |WHERE
               |    id = ? AND
               |    deleted = FALSE
                """.stripMargin,
            firstName,
            lastName,
            avatarUrl.orNull,
            isAdmin,
            NCUtils.nowUtcTs(),
            usrId
        )
    }

    /**
      * Updates data source.
      *
      * @param dsId ID of the data source to update.
      * @param name Data source name.
      * @param shortDesc Short data source description.
      */
    @throws[NCE]
    def updateDataSource(
        dsId: Long,
        name: String,
        shortDesc: String
    ): Int = {
        ensureStarted()
        
        NCSql.update(
            s"""
               |UPDATE ds_instance
               |SET
               |    name = ?,
               |    short_desc = ?,
               |    last_modified_on = ?
               |WHERE
               |    id = ? AND
               |    deleted = FALSE
                """.stripMargin,
            name,
            shortDesc,
            NCUtils.nowUtcTs(),
            dsId
        )
    }

    /**
      * Gets user for given ID.
      *
      * @param usrId User ID.
      * @return User MDO.
      */
    @throws[NCE]
    def getUser(usrId: Long): Option[NCUserMdo] = {
        ensureStarted()
    
        NCSql.selectSingle[NCUserMdo](
            s"""
               |SELECT *
               |FROM nc_user
               |WHERE
               |    id = ? AND
               |    deleted = FALSE
            """.stripMargin,
            usrId)
    }
    
    /**
      * Gets data source for given ID.
      *
      * @param dsId Data source ID.
      * @return Data source MDO.
      */
    @throws[NCE]
    def getDataSource(dsId: Long): Option[NCDataSourceMdo] = {
        ensureStarted()
        
        NCSql.selectSingle[NCDataSourceMdo](
            s"""
            |SELECT *
            |FROM ds_instance
            |WHERE
            |    id = ? AND
            |    deleted = FALSE
            """.stripMargin,
            dsId
        )
    }

    /**
      * Gets all users.
      *
      * @return User MDOs.
      */
    @throws[NCE]
    def getAllUsers: List[NCUserMdo] = {
        ensureStarted()
        
        NCSql.select[NCUserMdo](
            s"""
            |SELECT *
            |FROM nc_user
            |WHERE deleted = FALSE
            """.stripMargin
        )
    }
    
    /**
      * Gets all data sources.
      *
      * @return Data source MDOs.
      */
    @throws[NCE]
    def getAllDataSources: List[NCDataSourceMdo] = {
        ensureStarted()
        
        NCSql.select[NCDataSourceMdo](
            s"""
            |SELECT *
            |FROM ds_instance
            |WHERE deleted = FALSE
            """.stripMargin
        )
    }

    /**
      * Adds new user with given parameters.
      *
      * @param firstName User's first name.
      * @param lastName User's last name.
      * @param email User's normalized email.
      * @param passwdSalt Optional salt for password Blowfish hashing.
      * @param avatarUrl User's avatar URL.
      * @param isAdmin Whether or not the user is admin.
      * @return Newly added user ID.
      */
    @throws[NCE]
    def addUser(
        id: Long,
        firstName: String,
        lastName: String,
        email: String,
        passwdSalt: String,
        avatarUrl: Option[String],
        isAdmin: Boolean
    ): Long = {
        ensureStarted()

        val now = NCUtils.nowUtcTs()
        
        // Insert user.
        NCSql.insertGetKey[Long](
            """
              | INSERT INTO nc_user(
              |    id,
              |    first_name,
              |    last_name,
              |    email,
              |    passwd_salt,
              |    avatar_url,
              |    last_ds_id,
              |    is_admin,
              |    created_on,
              |    last_modified_on
              | )
              | VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              """.stripMargin,
            id,
            firstName,
            lastName,
            email,
            passwdSalt,
            avatarUrl.orNull,
            -1, // No data source yet.
            isAdmin,
            now,
            now
        )
    }

    /**
      * Adds new data source instance.
      *
      * @param id ID.
      * @param name Name.
      * @param desc Description.
      * @param mdlId Model ID.
      * @param mdlName Model name.
      * @param mdlVer Model version.
      * @param mdlCfg Model config.
      * @param isTemp Temporary flag.
      */
    @throws[NCE]
    def addDataSource(
        id: Long,
        name: String,
        desc: String,
        mdlId: String,
        mdlName: String,
        mdlVer: String,
        mdlCfg: Option[String],
        isTemp: Boolean
    ): Long = {
        ensureStarted()

        val now = NCUtils.nowUtcTs()

        NCSql.insertGetKey[Long](
            """
              |INSERT INTO ds_instance(
              |     id,
              |     name,
              |     short_desc,
              |     model_id,
              |     model_name,
              |     model_ver,
              |     model_cfg,
              |     created_on,
              |     last_modified_on,
              |     is_temporary
              |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              """.stripMargin,
            id,
            name,
            desc,
            mdlId,
            mdlName,
            mdlVer,
            mdlCfg.orNull,
            now,
            now,
            isTemp
        )
    }
    
    /**
      * Adds processing log.
      *
      * @param usrId User Id.
      * @param srvReqId Server request ID.
      * @param txt Original text.
      * @param dsId Data source ID.
      * @param mdlId Data source model ID.
      * @param test Test flag.
      * @param usrAgent User agent string.
      * @param rmtAddr Remote user address.
      * @param rcvTstamp Receive timestamp.
      */
    @throws[NCE]
    def newProcessingLog(
        usrId: Long,
        srvReqId: String,
        txt: String,
        dsId: Long,
        mdlId: String,
        status: NCApiStatusCode,
        test: Boolean,
        usrAgent: String,
        rmtAddr: String,
        rcvTstamp: Timestamp
    ): Unit = {
        ensureStarted()
        
        NCSql.insertSingle(
            """
              |INSERT INTO proc_log (
              |     user_id,
              |     srv_req_id,
              |     txt,
              |     ds_id,
              |     model_id,
              |     status,
              |     is_test,
              |     user_agent,
              |     rmt_address,
              |     recv_tstamp
              | )
              | VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              """.stripMargin,
            usrId,
            srvReqId,
            txt,
            dsId,
            mdlId,
            status.toString,
            test,
            usrAgent,
            rmtAddr,
            rcvTstamp
        )
    }
    
    /**
      * 
      * @param srvReqId
      * @param tstamp
      */
    @throws[NCE]
    def updateCancelProcessingLog(
        srvReqId: String,
        tstamp: Timestamp
    ): Unit = {
        ensureStarted()
        NCSql.insertSingle(
            """
            |UPDATE proc_log
            |SET
            |    status = ?,
            |    cancel_tstamp = ?
            |WHERE srv_req_id = ?
            """.stripMargin,
            "QRY_CANCELLED",
            tstamp,
            srvReqId
        )
    }
    
    /**
      * Updates processing log.
      *
      * @param srvReqId
      * @param errMsg
      * @param resType
      * @param resBody
      * @param tstamp
      */
    @throws[NCE]
    def updateReadyProcessingLog(
        srvReqId: String,
        errMsg: String,
        resType: String,
        resBody: String,
        tstamp: Timestamp
    ): Unit = {
        ensureStarted()
        
        NCSql.insertSingle(
            """
              |UPDATE proc_log
              |SET
              |    status = ?,
              |    error = ?,
              |    res_type = ?,
              |    res_body_gzip = ?,
              |    resp_tstamp = ?
              |WHERE srv_req_id = ?
              """.stripMargin,
            QRY_READY.toString,
            errMsg,
            resType,
            resBody,
            tstamp,
            srvReqId
        )
    }
    
    /**
      * Updates processing log.
      *
      * @param srvReqId
      * @param probeToken
      * @param probeId
      * @param probeGuid
      * @param probeApiVersion
      * @param probeApiDate
      * @param osVersion
      * @param osName
      * @param osArch
      * @param startTstamp
      * @param tmzId
      * @param tmzAbbr
      * @param tmzName
      * @param userName
      * @param javaVersion
      * @param javaVendor
      * @param hostName
      * @param hostAddr
      * @param macAddr
      */
    @throws[NCE]
    def updateProbeProcessingLog(
        srvReqId: String,
        probeToken: String,
        probeId: String,
        probeGuid: String,
        probeApiVersion: String,
        probeApiDate: LocalDate,
        osVersion: String,
        osName: String,
        osArch: String,
        startTstamp: Timestamp,
        tmzId: String,
        tmzAbbr: String,
        tmzName: String,
        userName: String,
        javaVersion: String,
        javaVendor: String,
        hostName: String,
        hostAddr: String,
        macAddr: String
    ): Unit = {
        ensureStarted()
        
        NCSql.insertSingle(
            """
              |UPDATE proc_log
              |SET
              |    probe_token = ?,
              |    probe_id = ?,
              |    probe_guid = ?,
              |    probe_api_version = ?,
              |    probe_api_date = ?,
              |    probe_os_version = ?,
              |    probe_os_name = ?,
              |    probe_os_arch = ?,
              |    probe_start_tstamp = ?,
              |    probe_tmz_id = ?,
              |    probe_tmz_abbr = ?,
              |    probe_tmz_name = ?,
              |    probe_user_name = ?,
              |    probe_java_version = ?,
              |    probe_java_vendor = ?,
              |    probe_host_name = ?,
              |    probe_host_addr = ?,
              |    probe_mac_addr = ?
              |WHERE srv_req_id = ?
              """.stripMargin,
            probeToken,
            probeId,
            probeGuid,
            probeApiVersion,
            probeApiDate,
            osVersion,
            osName,
            osArch,
            startTstamp,
            tmzId,
            tmzAbbr,
            tmzName,
            userName,
            javaVersion,
            javaVendor,
            hostName,
            hostAddr,
            macAddr,
            srvReqId
        )
    }

    /**
      * Gets maximum long column value.
      *
      * @param table Table name.
      * @param col Column name.
      */
    @throws[NCE]
    def getMaxColumnValue(table: String, col: String): Option[Long] = {
        ensureStarted()

        NCSql.selectSingle[Long](s"SELECT max($col) FROM $table")
    }
}

