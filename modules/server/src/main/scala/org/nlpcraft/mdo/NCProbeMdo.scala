/*
 * “Commons Clause” License, https://commonsclause.com/
 *
 * The Software is provided to you by the Licensor under the License,
 * as defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software:    NlpCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
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
import java.time.LocalDate

import org.nlpcraft.mdo.impl._

/**
  * Probe MDO.
  */
@NCMdoEntity(sql = false)
case class NCProbeMdo(
    @NCMdoField probeToken: String,
    @NCMdoField probeId: String,
    @NCMdoField probeGuid: String,
    @NCMdoField probeApiVersion: String,
    @NCMdoField probeApiDate: LocalDate,
    @NCMdoField osVersion: String,
    @NCMdoField osName: String,
    @NCMdoField osArch: String,
    @NCMdoField startTstamp: Timestamp,
    @NCMdoField tmzId: String,
    @NCMdoField tmzAbbr: String,
    @NCMdoField tmzName: String,
    @NCMdoField userName: String,
    @NCMdoField javaVersion: String,
    @NCMdoField javaVendor: String,
    @NCMdoField hostName: String,
    @NCMdoField hostAddr: String,
    @NCMdoField macAddr: String,
    @NCMdoField models: Set[NCProbeModelMdo]
) extends NCAnnotatedMdo[NCProbeMdo] {
    override def hashCode(): Int = probeToken.hashCode() * 37 + probeId.hashCode
    override def equals(obj: scala.Any): Boolean =
        obj match {
            case x: NCProbeMdo ⇒ x.probeId == probeId && x.probeToken == probeToken
            case _ ⇒ false
        }
}
