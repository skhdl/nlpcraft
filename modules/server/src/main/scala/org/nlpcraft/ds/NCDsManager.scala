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

package org.nlpcraft.ds

import org.nlpcraft.db.NCDbManager
import org.nlpcraft.{NCE, NCLifecycle}
import org.nlpcraft.mdo.NCProbeMdo

/**
  * Datasources manager.
  */
object NCDsManager extends NCLifecycle("Datasources manager") {
    /**
      * Adds new data source.
      *
      * @param probes Company probes.
      * @param compId Company ID.
      * @param usrId User ID.
      * @param name Data source name.
      * @param desc Data source description.
      * @param mdlId Model ID.
      * @param mdlName Model name.
      * @param mdlVer Model version.
      * @param mdlCfg Model configuration.
      * @return `None` if data source with given name already exists for this user, Option of datasource ID
      * when new data source has been successfully added.
      */
    @throws[NCE]
    def addDataSource(
        probes: Seq[NCProbeMdo],
        compId: Long,
        usrId: Long,
        name: String,
        desc: String,
        mdlId: String,
        mdlName: String,
        mdlVer: String,
        mdlCfg: String): Option[Long] = {
        ensureStarted()

        // TODO: userId?
        // TODO: enabled?

        val dsId = NCDbManager.addDataSource(name, desc, usrId, true, mdlId, mdlName, mdlVer, mdlCfg)

        Some(dsId)
    }
}
