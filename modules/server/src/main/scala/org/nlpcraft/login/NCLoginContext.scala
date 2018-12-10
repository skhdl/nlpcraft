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

import org.nlpcraft2.json.NCJson
import org.nlpcraft2.mdo.impl.{NCAnnotatedMdo, NCMdoEntity, NCMdoField}

/**
 * Successful login context.
 */
@NCMdoEntity(sql = false)
case class NCLoginContext(
    @NCMdoField userId: Long,
    @NCMdoField title: String,
    @NCMdoField department: String,
    @NCMdoField avatarUrl: String,
    @NCMdoField loginToken: String,
    @NCMdoField firstName: String,
    @NCMdoField lastName: String,
    @NCMdoField email: String,
    @NCMdoField companyId: Long,
    @NCMdoField signUpDomain: String,
    @NCMdoField signInOrigin: String,
    @NCMdoField signUpOn: Long,
    @NCMdoField isAdmin: Boolean,
    @NCMdoField isRoot: Boolean,
    @NCMdoField isFirstLogin: Boolean,
    @NCMdoField activeDsId: Long,
    @NCMdoField preferences: NCJson,
    @NCMdoField referralCode: String,
    @NCMdoField tmzName: String,
    @NCMdoField tmzAbbr: String,
    @NCMdoField latitude: Double,
    @NCMdoField longitude: Double,
    @NCMdoField countryCode: String,
    @NCMdoField regionName: String,
    @NCMdoField regionCode: String,
    @NCMdoField city: String,
    @NCMdoField zipCode: String,
    @NCMdoField metroCode: Long
) extends NCAnnotatedMdo[NCLoginContext]