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

package org.nlpcraft2.mdo

import org.nlpcraft2.mdo.impl._

/**
  * Probe model MDO for rest probe manager.
  */
@NCMdoEntity(sql = false)
case class NCProbeModelMdo(
    @NCMdoField id: String,
    @NCMdoField name: String,
    @NCMdoField version: String,
    @NCMdoField companyId: Long
) extends NCAnnotatedMdo[NCProbeModelMdo] {
    override def hashCode(): Int = s"$id$companyId".hashCode()

    override def equals(obj: Any): Boolean = {
        obj match {
            case x: NCProbeModelMdo ⇒ x.id == id && x.companyId == companyId
            case _ ⇒ false
        }
    }
}
