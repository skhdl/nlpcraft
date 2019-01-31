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

package org.nlpcraft.probe

import java.time.LocalDate

import org.nlpcraft._
import com.typesafe.scalalogging.LazyLogging

/**
  * Version manager.
  */
object NCVersion extends LazyLogging {
    /**
      *
      * @param version Sequentially increasing internal version of the probe.
      * @param date Date of this version release.
      * @param notes Release notes.
      */
    case class Version(
        version: String, // Semver.
        date: LocalDate,
        notes: String
    ) {
        override def toString = s"Probe version [version=$version, notes=$notes, date=$date]"
    }
    
    // +=========================================================================+
    // | UPDATE THIS SEQUENCE FOR EACH NON-BACKWARD-COMPATIBLE RELEASE MANUALLY. |
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private final val VERSIONS = Seq(
        Version("1.0.0", LocalDate.of(2018, 12, 5), "Initial release.")
    ).sortBy(_.version)
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // | UPDATE THIS SEQUENCE FOR EACH NON-BACKWARD-COMPATIBLE RELEASE MANUALLY. |
    // +=========================================================================+
    
    if (G.distinct(VERSIONS.map(_.version).toList).lengthCompare(VERSIONS.size) != 0)
        throw new AssertionError(s"Probe versions are NOT unique.")
    
    /**
      * Gets current version.
      */
    def getCurrent: Version = VERSIONS.last
}

