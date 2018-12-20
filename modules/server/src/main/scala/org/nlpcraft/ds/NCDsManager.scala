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
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft._
import org.nlpcraft.mdo._
import org.nlpcraft.notification.NCNotificationManager

/**
  * Datasources manager.
  */
object NCDsManager extends NCLifecycle("Data source manager") {
    /**
      * Updates given data source.
      *
      * @param dsId ID of the data source to update.
      * @param name Data source name.
      * @param shortDesc Data source description.
      */
    @throws[NCE]
    def updateDataSource(
        dsId: Long,
        name: String,
        shortDesc: String
    ): Unit = {
        ensureStarted()
    
        NCPsql.sql {
            NCDbManager.getDataSource(dsId) match {
                case None ⇒ throw new NCE(s"Unknown data source ID: $dsId")
                case Some(ds) ⇒
                    NCDbManager.updateDataSource(dsId, name, shortDesc)
                
                    // Notification.
                    NCNotificationManager.addEvent("NC_DS_UPDATE",
                        "dsId" → dsId,
                        "name" → ds.name,
                        "desc" → ds.shortDesc
                    )
            }
        }
    }
    
    /**
      * Adds new data source.
      *
      * @param name Data source name.
      * @param desc Data source description.
      * @param mdlId Model ID.
      * @param mdlName Model name.
      * @param mdlVer Model version.
      * @param mdlCfg Model configuration.
      * @return
      */
    @throws[NCE]
    def addDataSource(
        name: String,
        desc: String,
        mdlId: String,
        mdlName: String,
        mdlVer: String,
        mdlCfg: String): Long = {
        ensureStarted()

        val dsId = NCPsql.sql {
            NCDbManager.addDataSource(name, desc, mdlId, mdlName, mdlVer, mdlCfg)
        }
    
        // Notification.
        NCNotificationManager.addEvent("NC_DS_ADD",
            "dsId" → dsId,
            "name" → name,
            "desc" → desc,
            "modelId" → mdlId,
            "modelName" → mdlName,
            "mdlVer" → mdlVer,
            "mdlCfg" → mdlCfg
        )

        dsId
    }
    
    /**
      * Deletes data source with given DI.
      *
      * @param dsId ID of the data source to delete.
      */
    @throws[NCE]
    def deleteDataSource(dsId: Long): Unit = {
        ensureStarted()
    
        NCPsql.sql {
            NCDbManager.getDataSource(dsId) match {
                case None ⇒ throw new NCE(s"Unknown data source ID: $dsId")
                case Some(ds) ⇒
                    NCDbManager.deleteDataSource(dsId)
    
                    // Notification.
                    NCNotificationManager.addEvent("NC_DS_DELETE",
                        "dsId" → dsId,
                        "name" → ds.name,
                        "desc" → ds.shortDesc,
                        "modelId" → ds.modelId,
                        "modelName" → ds.modelName,
                        "mdlVer" → ds.modelVersion,
                        "mdlCfg" → ds.modelConfig
                    )
            }
        }
    }
    
    /**
      * Gets the list of all data sources.
      */
    @throws[NCE]
    def getAllDataSources: List[NCDataSourceMdo] = {
        ensureStarted()
        
        NCPsql.sql {
            NCDbManager.getAllDataSources
        }
    }
}
