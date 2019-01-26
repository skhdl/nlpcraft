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
 * Software:    NlpCraft
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

package org.nlpcraft.db

import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.db.postgres.NCPsql.Implicits._
import org.nlpcraft._
import org.nlpcraft.apicodes.NCApiStatusCode._
import org.nlpcraft.mdo._

import scala.util.control.Exception._

/**
  * Provides basic CRUD and often used operations on PostgreSQL RDBMS.
  * Note that all functions in this class expect outside `NCPsql.sql()` block.
  */
object NCDbManager extends NCLifecycle("Database manager") {
    // Relative database schema path.
    private final val SCHEMA_PATH = "sql/schema.sql"
    
    /**
      * Starts manager.
      */
    @throws[NCE]
    override def start(): NCLifecycle = {
        ensureStopped()

        checkSchema()

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
      * Checks and initializes, if necessary, the database schema.
      */
    private def checkSchema(): Unit = {
        val schemaExists =
            NCPsql.sql {
                try {
                    NCPsql.selectSingle[String]("SELECT NULL FROM base LIMIT 1")

                    true
                }
                catch {
                    case _: NCE ⇒ false
                }
            }

        if (!schemaExists)
            try {
                NCPsql.sql {
                    G.readResource(SCHEMA_PATH, "UTF-8").
                        map(_.trim).
                        filter(p ⇒ !p.startsWith("--")). // Skip full-line comments.
                        map(p ⇒ { // Remove partial comments.
                            val idx = p.indexOf("--")

                            if (idx < 0) p else p.take(idx)
                        }).
                        mkString(" ").
                        split(";"). // Split by SQL statements.
                        map(_.trim).
                        foreach(sql ⇒ NCPsql.ddl(sql))

                    // Mark the current schema as just created.
                    NCPsql.ddl("CREATE TABLE new_schema()")
                }
                
                logger.info("DB schema has been created.")
            }
            catch {
                case e: NCE ⇒
                    throw new NCE(s"Failed to auto-create database schema - " +
                        s"clear existing tables and create schema manually using '$SCHEMA_PATH' file.", e)
            }
        else
            // Clean up in case of previous failed start.
            ignoring(classOf[NCE]) {
                NCPsql.sql {
                    NCPsql.ddl("DROP TABLE new_schema")
                }
            }
    }
    
    /**
      * Whether or not DB schema was just created by the current JVM process.
      *
      * @return
      */
    @throws[NCE]
    def isNewSchema: Boolean = {
        ensureStarted()
    
        try {
            NCPsql.selectSingle[String]("SELECT NULL FROM new_schema LIMIT 1")
        
            true
        }
        catch {
            case _: NCE ⇒ false
        }
    }
    
    /**
      * Clears the "new DB schema" flag from database.
      */
    @throws[NCE]
    def clearNewSchemaFlag(): Unit = {
        ensureStarted()
    
        NCPsql.ddl("DROP TABLE new_schema")
    }

    /**
      * Checks if given hash exists in the password pool.
      *
      * @param hash Hash to check.
      */
    @throws[NCE]
    def isKnownPasswordHash(hash: String): Boolean = {
        ensureStarted()
    
        NCPsql.exists("passwd_pool WHERE passwd_hash = ?", hash)
    }
    
    /**
      * Inserts password hash into anonymous password pool.
      *
      * @param hash Password hash to insert into anonymous password pool.
      */
    @throws[NCE]
    def addPasswordHash(hash: String): Unit = {
        ensureStarted()
    
        NCPsql.insert("INSERT INTO passwd_pool (passwd_hash) VALUES (?)", hash)
    }
    
    /**
      * Removes password hash from anonymous password pool.
      *
      * @param hash Password hash to remove.
      */
    @throws[NCE]
    def removePasswordHash(hash: String): Unit = {
        ensureStarted()
    
        NCPsql.delete("DELETE FROM passwd_pool WHERE passwd_hash = ?", hash)
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

        NCPsql.selectSingle[NCUserMdo](
            """
              |SELECT *
              |FROM nc_user
              |WHERE
              |    email = ? AND
              |    deleted = FALSE""".stripMargin,
            email
        )
    }
    
    /**
      * Deletes user record with given ID.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def deleteUser(usrId: Long): Unit = {
        ensureStarted()

        NCPsql.markAsDeleted("nc_user", "id", usrId)
    }

    /**
      * Deletes user record with given email.
      *
      * @param email Email.
      */
    @throws[NCE]
    def deleteUser(email: String): Unit = {
        ensureStarted()

        NCPsql.markAsDeleted("nc_user", "email", email)
    }


    /**
      * Deletes data source with given ID.
      *
      * @param dsId Data source ID.
      */
    @throws[NCE]
    def deleteDataSource(dsId: Long): Unit = {
        ensureStarted()
        
        NCPsql.markAsDeleted("ds_instance", "id", dsId)
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

        NCPsql.update(
            s"""
               |UPDATE nc_user
               |SET
               |    first_name = ?,
               |    last_name = ?,
               |    avatar_url = ?,
               |    is_admin = ?,
               |    last_modified_on = current_timestamp
               |WHERE
               |    id = ? AND
               |    deleted = FALSE
                """.stripMargin,
            firstName,
            lastName,
            avatarUrl.orNull,
            isAdmin,
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
        
        NCPsql.update(
            s"""
               |UPDATE ds_instance
               |SET
               |    name = ?,
               |    short_desc = ?,
               |    last_modified_on = current_timestamp
               |WHERE
               |    id = ? AND
               |    deleted = FALSE
                """.stripMargin,
            name,
            shortDesc,
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
    
        NCPsql.selectSingle[NCUserMdo](
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
        
        NCPsql.selectSingle[NCDataSourceMdo](
            s"""
               |SELECT *
               |FROM ds_instance
               |WHERE
               |    id = ? AND
               |    deleted = FALSE
            """.stripMargin,
            dsId)
    }

    /**
      * Gets all users.
      *
      * @return User MDOs.
      */
    @throws[NCE]
    def getAllUsers: List[NCUserMdo] = {
        ensureStarted()
        
        NCPsql.select[NCUserMdo](
            s"""
               |SELECT *
               |FROM nc_user
               |WHERE deleted = FALSE
            """.stripMargin)
    }
    
    /**
      * Gets all data sources.
      *
      * @return Data source MDOs.
      */
    @throws[NCE]
    def getAllDataSources: List[NCDataSourceMdo] = {
        ensureStarted()
        
        NCPsql.select[NCDataSourceMdo](
            s"""
               |SELECT *
               |FROM ds_instance
               |WHERE deleted = FALSE
            """.stripMargin)
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
        
        // Insert user.
        NCPsql.insertGetKey[Long](
            """
              | INSERT INTO nc_user(
              |    id,
              |    first_name,
              |    last_name,
              |    email,
              |    passwd_salt,
              |    avatar_url,
              |    last_ds_id,
              |    is_admin
              | )
              | VALUES (?, ?, ?, ?, ?, ?, ?, ?)""".stripMargin,
            id,
            firstName,
            lastName,
            email,
            passwdSalt,
            avatarUrl.orNull,
            -1, // No data source yet.
            isAdmin
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

        NCPsql.insertGetKey[Long](
            """
              |INSERT INTO ds_instance(
              |     id,
              |     name,
              |     short_desc,
              |     model_id,
              |     model_name,
              |     model_ver,
              |     model_cfg
              |) VALUES (?, ?, ?, ?, ?, ?, ?)""".stripMargin,
            id,
            name,
            desc,
            mdlId,
            mdlName,
            mdlVer,
            mdlCfg.orNull
        )
    }
    
    /**
      * Adds processing log.
      *
      * @param usrId User Id.
      * @param srvReqId Server request ID.
      * @param origTxt Original text.
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
        origTxt: String,
        dsId: Long,
        mdlId: String,
        status: NCApiStatusCode,
        test: Boolean,
        usrAgent: String,
        rmtAddr: String,
        rcvTstamp: Long
    ): Unit = {
        ensureStarted()
        
        NCPsql.insertSingle(
            """
              |INSERT
              |  INTO proc_log (
              |     user_id,
              |     srv_req_id,
              |     orig_txt,
              |     ds_id,
              |     model_id,
              |     status,
              |     is_test,
              |     user_agent,
              |     rmt_addr,
              |     recv_tstamp
              | )
              | VALUES (
              |     ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
              | )""".stripMargin,
            usrId,
            srvReqId,
            origTxt,
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
        tstamp: Long
    ): Unit = {
        ensureStarted()
        NCPsql.insertSingle(
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
        tstamp: Long
    ): Unit = {
        ensureStarted()
        
        NCPsql.insertSingle(
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
      * Gets maximum long column value.
      *
      * @param table Table name.
      * @param col Column name.
      */
    @throws[NCE]
    def getMaxColumnValue(table: String, col: String): Option[Long] = {
        ensureStarted()

        NCPsql.selectSingle[Long](s"SELECT max($col) FROM $table")
    }
}

