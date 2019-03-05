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

package org.nlpcraft.server.sql

import java.sql.Timestamp

import org.nlpcraft.common.{NCLifecycle, _}
import org.nlpcraft.server.apicodes.NCApiStatusCode._
import org.nlpcraft.server.ignite.NCIgniteInstance
import org.nlpcraft.server.mdo._
import org.nlpcraft.server.sql.NCSql.Implicits._

import scala.collection.JavaConverters._
import scala.util.control.Exception.catching

/**
  * Provides basic CRUD and often used operations on RDBMS.
  * Note that all functions in this class expect outside `NCSql.sql()` block.
  */
object NCSqlManager extends NCLifecycle("Database manager") with NCIgniteInstance {
    private final val DB_TABLES = Seq("nc_user", "passwd_pool", "ds_instance", "proc_log")

    /**
      * Starts manager.
      */
    @throws[NCE]
    override def start(): NCLifecycle = {
        ensureStopped()

        prepareSchema()

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
      * @param id Id.
      * @param hash Password hash to insert into anonymous password pool.
      */
    @throws[NCE]
    def addPasswordHash(id: Long, hash: String): Unit = {
        ensureStarted()

        NCSql.insert("INSERT INTO passwd_pool (id, passwd_hash) VALUES (?, ?)", id, hash)
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
              |WHERE email = ?
              """.stripMargin,
            email
        )
    }

    /**
      * Deletes user with given ID.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def deleteUser(usrId: Long): Unit = {
        ensureStarted()

        NCSql.delete("DELETE FROM nc_user WHERE id = ?", usrId)
    }

    /**
      * Deletes data source with given ID.
      *
      * @param dsId Data source ID.
      */
    @throws[NCE]
    def deleteDataSource(dsId: Long): Unit = {
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
               |WHERE id = ?
                """.stripMargin,
            firstName,
            lastName,
            avatarUrl.orNull,
            isAdmin,
            U.nowUtcTs(),
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
               |WHERE id = ?
                """.stripMargin,
            name,
            shortDesc,
            U.nowUtcTs(),
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
               |WHERE id = ?
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
            |WHERE id = ?
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

        NCSql.select[NCUserMdo]("SELECT * FROM nc_user")
    }

    /**
      * Gets all data sources.
      *
      * @return Data source MDOs.
      */
    @throws[NCE]
    def getAllDataSources: List[NCDataSourceMdo] = {
        ensureStarted()

        NCSql.select[NCDataSourceMdo]("SELECT *FROM ds_instance")
    }

    /**
      * Adds new user with given parameters.
      *
      * @param id User's ID.
      * @param email User's normalized email.
      * @param firstName User's first name.
      * @param lastName User's last name.
      * @param avatarUrl User's avatar URL.
      * @param passwdSalt Optional salt for password Blowfish hashing.
      * @param isAdmin Whether or not the user is admin.
      *
      * @return Newly added user ID.
      */
    @throws[NCE]
    def addUser(
        id: Long,
        email: String,
        firstName: String,
        lastName: String,
        avatarUrl: Option[String],
        passwdSalt: String,
        isAdmin: Boolean
    ): Long = {
        ensureStarted()

        val now = U.nowUtcTs()

        // Insert user.
        NCSql.insert(
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
      */
    @throws[NCE]
    def addDataSource(
        id: Long,
        name: String,
        desc: String,
        mdlId: String,
        mdlName: String,
        mdlVer: String,
        mdlCfg: Option[String]
    ): Long = {
        ensureStarted()

        val now = U.nowUtcTs()

        NCSql.insert(
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
              |     last_modified_on
              |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
              """.stripMargin,
            id,
            name,
            desc,
            mdlId,
            mdlName,
            mdlVer,
            mdlCfg.orNull,
            now,
            now
        )
    }

    /**
      * Adds processing log.
      *
      * @param id Id.
      * @param usrId User Id.
      * @param srvReqId Server request ID.
      * @param txt Original text.
      * @param dsId Data source ID.
      * @param mdlId Data source model ID.
      * @param usrAgent User agent string.
      * @param rmtAddr Remote user address.
      * @param rcvTstamp Receive timestamp.
      */
    @throws[NCE]
    def newProcessingLog(
        id: Long,
        usrId: Long,
        srvReqId: String,
        txt: String,
        dsId: Long,
        mdlId: String,
        status: NCApiStatusCode,
        usrAgent: String,
        rmtAddr: String,
        rcvTstamp: Timestamp
    ): Unit = {
        ensureStarted()

        NCSql.insertSingle(
            """
              |INSERT INTO proc_log (
              |     id,
              |     user_id,
              |     srv_req_id,
              |     txt,
              |     ds_id,
              |     model_id,
              |     status,
              |     user_agent,
              |     rmt_address,
              |     recv_tstamp
              | )
              | VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              """.stripMargin,
            id,
            usrId,
            srvReqId,
            txt,
            dsId,
            mdlId,
            status.toString,
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
            if (resBody == null) null else U.compress(resBody),
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
        probeApiDate: java.sql.Date,
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

    /**
      *
      * @param sqlPath
      */
    @throws[NCE]
    private def executeScript(sqlPath: String): Unit =
        U.readResource(sqlPath, "UTF-8").
            mkString("\n").
            split(";").
            map(_.trim).
            filter(!_.isEmpty).
            foreach(p ⇒ NCSql.ddl(p))

    /**
      *
      */
    @throws[NCE]
    def prepareSchema(): Unit = {
        def safeClear(): Unit =
            try
                executeScript("sql/drop_schema.sql")
            catch {
                case _: NCE ⇒ // No-op.
            }

        val sqlTabs =
            catching(wrapIE) {
                ignite.cacheNames().asScala.
                    map(_.toLowerCase).
                    flatMap(p ⇒ if (p.startsWith("sql_")) Some(p.drop(4)) else None)
            }.toSet

        val dbInitParam = "NLPCRAFT_DB_INITIALIZE"

        var initFlag = U.isSysEnvTrue(dbInitParam)

        if (initFlag)
            logger.info(s"Database schema initialization flag found: -D$dbInitParam=true")
        else {
            // Ignite cache names can be `sql_nc_user` or `sql_nlpcraft_nc_user` if schema used.
            initFlag = DB_TABLES.exists(t ⇒ !sqlTabs.exists(st ⇒ st == t || st.endsWith(s"_$t")))
        }

        NCSql.sql {
            if (initFlag)
                try {
                    safeClear()

                    executeScript("sql/create_schema.sql")

                    logger.info("Database schema initialized.")
                }
                catch {
                    case e: NCE ⇒
                        safeClear()

                        throw e
                }
        }
    }
}

