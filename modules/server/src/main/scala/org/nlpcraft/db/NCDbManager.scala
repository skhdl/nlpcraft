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

package org.nlpcraft.db

import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.ignite.NCIgniteNlpCraft
import org.nlpcraft.db.postgres.NCPsql.Implicits._
import org.nlpcraft._
import org.nlpcraft.mdo._
import scala.util.control.Exception._

/**
  * Provides basic CRUD and often used operations on PostgreSQL RDBMS.
  * Note that all functions in this class expect outside `NCPsql.sql()` block.
  */
object NCDbManager extends NCLifecycle("DB manager") with NCIgniteNlpCraft {
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
      * Updates user.
      *
      * @param usrId ID of the user to update.
      * @param avatarUrl Avatar URL.
      * @param firstName First name.
      * @param lastName Last name.
      * @param isAdmin Admin flag.
      */
    @throws[NCE]
    def updateUser(
        usrId: Long,
        avatarUrl: String,
        firstName: String,
        lastName: String,
        isAdmin: Boolean
    ): Unit = {
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
            avatarUrl,
            isAdmin,
            usrId
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
      * Gets all users.
      *
      * @return User MDO.
      */
    @throws[NCE]
    def getAllUsers: List[NCUserMdo] = {
        ensureStarted()
        
        NCPsql.select[NCUserMdo](
            s"""
               |SELECT *
               |FROM nc_user
               |WHERE
               |    deleted = FALSE
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
        firstName: String,
        lastName: String,
        email: String,
        passwdSalt: String,
        avatarUrl: String,
        isAdmin: Boolean
    ): Long = {
        ensureStarted()
        
        // Insert user.
        NCPsql.insertGetKey[Long](
            """
              | INSERT INTO nc_user(
              |    first_name,
              |    last_name,
              |    email,
              |    passwd_salt,
              |    avatar_url,
              |    last_ds_id,
              |    is_admin
              | )
              | VALUES (?, ?, ?, ?, ?, ?, ?)""".stripMargin,
            firstName,
            lastName,
            email,
            passwdSalt,
            avatarUrl,
            -1, // No data source yet.
            isAdmin
        )
    }

    /**
      * Adds new data source instance.
      *
      * @param name Name.
      * @param desc Description.
      * @param mdlId Model ID.
      * @param mdlName Model name.
      * @param mdlVer Model version.
      * @param mdlCfg Model config.
      */
    @throws[NCE]
    def addDataSource(
        name: String,
        desc: String,
        mdlId: String,
        mdlName: String,
        mdlVer: String,
        mdlCfg: String
    ): Long = {
        ensureStarted()

        NCPsql.insertGetKey[Long](
            """
              |INSERT INTO ds_instance(
              |     name,
              |     short_desc,
              |     model_id,
              |     model_name,
              |     model_ver,
              |     model_cfg
              |) VALUES (?, ?, ?, ?, ?, ?)""".stripMargin,
            name,
            desc,
            mdlId,
            mdlName,
            mdlVer,
            mdlCfg
        )
    }
}

