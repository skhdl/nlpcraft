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

/**
  * Provides basic CRUD and often used operations on PostgreSQL RDBMS.
  * Note that all functions in this class expect outside `NCPsql.sql()` block.
  */
object NCDbManager extends NCLifecycle("DB manager") with NCIgniteNlpCraft {
    /**
      * Starts manager.
      */
    @throws[NCE]
    override def start(): NCLifecycle = {
        ensureStopped()

        // Warm up SQL connection pool.
        NCPsql.sql {
            if (!NCPsql.isValid)
                throw new NCE("No valid JDBC connection found.")
        }

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
              |     email = ? AND
              |     deleted = FALSE""".stripMargin,
            email
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
              | INSERT INTO nc_user
              | (
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
      * Checks probe token and admin user email for REST API authentication.
      *
      * @param probeTkn Probe token.
      * @param email Admin user email.
      * @return
      */
    @throws[NCE]
    def checkProbeTokenAndEmail(probeTkn: String, email: String): Boolean = {
        ensureStarted()

        NCPsql.exists(
            """
              |SELECT 1
              |FROM
              |    company c,
              |    company_user u
              |WHERE
              |    c.probe_token = ? AND
              |    u.email = ? AND
              |    u.company_id = c.id AND
              |    c.deleted = FALSE AND
              |    u.deleted = FALSE
            """.stripMargin,
            probeTkn,
            email
        )
    }
}

