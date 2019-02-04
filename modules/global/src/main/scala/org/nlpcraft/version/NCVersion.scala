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
 * Software:    NLPCraft
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

package org.nlpcraft.version

import java.time.LocalDate

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft._

/**
  * Release version holder. Note that this is manually changing property. For every official
  * release the new version will be added to this object manually.
  */
object NCVersion extends LazyLogging {
    /**
      *
      * @param version Semver-based release version of the NLPCraft.
      * @param date Date of this release.
      * @param notes Release notes.
      */
    case class Version(
        version: String, // Semver.
        date: LocalDate,
        notes: String
    ) {
        override def toString = s"Version [version=$version, notes=$notes, date=$date]"
    }
    
    // +=================================================+
    // | UPDATE THIS SEQUENCE FOR EACH RELEASE MANUALLY. |
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private final val VERSIONS = Seq(
        Version("1.0.0", LocalDate.of(2018, 12, 5), "Initial release.")
    ).sortBy(_.version)
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // | UPDATE THIS SEQUENCE FOR EACH RELEASE MANUALLY. |
    // +=================================================+
    
    if (G.distinct(VERSIONS.map(_.version).toList).lengthCompare(VERSIONS.size) != 0)
        throw new AssertionError(s"Versions are NOT unique.")
    
    /**
      * Gets current version.
      */
    lazy val getCurrent: Version = VERSIONS.last
}

