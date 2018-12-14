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

package org.nlpcraft.login

import org.nlpcraft.db.NCDbManager
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.ignite.NCIgniteNlpCraft
import org.nlpcraft.{NCDebug, NCLifecycle, _}

import scala.collection.mutable

/**
 * Login/logout manager.
 */
object NCLoginManager extends NCLifecycle("Login manager") with NCIgniteNlpCraft with NCDebug {
    // API access tokens.
    // Note: not using cache here...
    // TODO: how is this cleaned up? expiration?
    private val accessTkns = mutable.HashMap.empty[String/*Access token*/, AccessToken]

    // Access token.
    private case class AccessToken(
        accessToken: String,
        probeToken: String,
        email: String,
        userId: Long,
        companyId: Long,
        var lastAccessMs: Long
    )

    /**
      * Generates new token or returns existing one for given probe token and user email.
      *
      * @param probeTkn Probe token.
      * @param email User email.
      * @return New or existing access token for this user or `None` in case of authentication problem.
      */
    @throws[NCE]
    def getAccessToken(probeTkn: String, email: String): Option[String] = {
        ensureStarted()

        accessTkns.synchronized {
            accessTkns.values.find(x ⇒ x.probeToken == probeTkn && x.email == email) match {
                // TODO: check access token expiration?
                case Some(x) ⇒ Some(x.accessToken)
                case None ⇒
                    NCPsql.sql {
                        if (NCDbManager.checkProbeTokenAndEmail(probeTkn, email)) {
                            val accessTkn = G.genGuid()

                            NCDbManager.getUserByEmail(email) match {
                                case Some(usr) ⇒
                                    accessTkns +=
                                        accessTkn → AccessToken(
                                            accessTkn,
                                            probeTkn,
                                            email,
                                            usr.id,
                                            usr.companyId,
                                            System.currentTimeMillis()
                                        )

                                    Some(accessTkn)

                                case None ⇒ None
                            }
                        }
                        else
                            None
                    }
            }
        }
    }
}
