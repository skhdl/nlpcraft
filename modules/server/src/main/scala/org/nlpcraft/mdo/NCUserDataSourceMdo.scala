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

import org.nlpcraft.db.postgres.NCPsql.Implicits.RsParser
import org.nlpcraft.mdo.impl.{NCAnnotatedMdo, NCMdoEntity, NCMdoField}

/**
  * User - data source relationship holder.
  */
@NCMdoEntity(table = "user_ds")
case class NCUserDataSourceMdo (
    @NCMdoField(column = "ds_id") dsId: Long,
    @NCMdoField(column = "user_id") userId: Long
) extends NCAnnotatedMdo[NCUserDataSourceMdo]

object NCUserDataSourceMdo {
    implicit val x: RsParser[NCUserDataSourceMdo] = NCAnnotatedMdo.mkRsParser(classOf[NCUserDataSourceMdo])
}