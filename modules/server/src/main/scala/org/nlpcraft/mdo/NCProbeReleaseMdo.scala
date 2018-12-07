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
import org.nlpcraft.mdo.impl.{NCAnnotatedMdo, NCMdoEntity, NCMdoField}

/**
  * Probe release record.
  */
@NCMdoEntity(table = "probe_release")
case class NCProbeReleaseMdo (
    @NCMdoField(column = "version") version: String,
    @NCMdoField(column = "date") date: Timestamp,
    @NCMdoField(column = "size_bytes") size: Int,
    @NCMdoField(column = "filename") fileName: String,
    @NCMdoField(column = "md5_sig_filename") md5SigFileName: String,
    @NCMdoField(column = "sha1_sig_filename") sha1SigFileName: String,
    @NCMdoField(column = "sha256_sig_filename") sha256SigFileName: String,
    @NCMdoField(column = "pgp_sig_filename") pgpSigFileName: String
) extends NCAnnotatedMdo[NCProbeReleaseMdo]

object NCProbeReleaseMdo {
    implicit val x: RsParser[NCProbeReleaseMdo] = NCAnnotatedMdo.mkRsParser(classOf[NCProbeReleaseMdo])
}
