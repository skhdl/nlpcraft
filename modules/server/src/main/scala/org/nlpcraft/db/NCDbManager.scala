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
import org.nlpcraft.{NCE, NCLifecycle, _}
import org.nlpcraft.mdo._

/**
  * Provides basic CRUD and often used operations on PostgreSQL RDBMS.
  * Note that all functions in this class expect outside 'NCPsql.sql()' block.
  */
object NCDbManager extends NCLifecycle("SERVER DB manager") with NCIgniteNlpCraft {
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
      * Gets user for given email.
      *
      * @param email User's email.
      * @return User MDO.
      */
    @throws[NCE]
    def getUserByEmail(email: String): Option[NCUserMdo] = {
        ensureStarted()

        NCPsql.selectSingle[NCUserMdo](
            """
              |SELECT *
              |FROM company_user
              |WHERE
              |     email = ? AND
              |     deleted = FALSE""".stripMargin,
            G.normalizeEmail(email)
        )
    }

    /**
      * Adds login history record.
      *
      * @param usrId User ID.
      * @param userEmail User email.
      * @param act Specific login or logout action.
      * @param userAgent User agent descriptor.
      * @param rmtAddr Remote address.
      */
    @throws[NCE]
    def addLoginHistory(
        usrId: Long,
        userEmail: String,
        act: String,
        userAgent: String,
        rmtAddr: String
    ): Unit = {
        ensureStarted()

        NCPsql.insertSingle(
            """
              |INSERT INTO login_history (
              |    user_id,
              |    user_email,
              |    act,
              |    user_agent,
              |    rmt_addr)
              |VALUES (?, ?, ?, ?, ?)""".stripMargin,
            usrId,
            G.normalizeEmail(userEmail),
            act.toUpperCase,
            userAgent,
            rmtAddr
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
    def checkProbeTokenAndAdminEmail(probeTkn: String, email: String): Boolean = {
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
              |    u.deleted = FALSE AND
              |    u.is_active = TRUE AND
              |    u.is_admin = TRUE
            """.stripMargin,
            probeTkn,
            email
        )
    }
}

