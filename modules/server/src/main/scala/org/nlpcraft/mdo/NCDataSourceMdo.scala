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

package org.nlpcraft.mdo

import java.sql.Timestamp

import org.nlpcraft.db.postgres.NCPsql.Implicits.RsParser
import org.nlpcraft.mdo.impl._

/**
  * Data source MDO.
  */
@NCMdoEntity(table = "ds_instance")
case class NCDataSourceMdo(
    @NCMdoField(column = "id", pk = true)  id: Long,
    @NCMdoField(column = "name") name: String,
    @NCMdoField(column = "short_desc") shortDesc: String,
    @NCMdoField(column = "model_id") modelId: String,
    @NCMdoField(column = "model_name") modelName: String,
    @NCMdoField(column = "model_ver") modelVersion: String,
    @NCMdoField(column = "model_cfg") modelConfig: String,
    
    // Base MDO.
    @NCMdoField(json = false, column = "created_on") createdOn: Timestamp,
    @NCMdoField(json = false, column = "last_modified_on") lastModifiedOn: Timestamp
) extends NCEntityMdo with NCAnnotatedMdo[NCDataSourceMdo]

object DLDataSourceInstanceMdo {
    implicit val x: RsParser[NCDataSourceMdo] =
        NCAnnotatedMdo.mkRsParser(classOf[NCDataSourceMdo])
}
