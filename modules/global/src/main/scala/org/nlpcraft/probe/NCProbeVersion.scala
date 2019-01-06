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
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
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
  * Probe version manager.
  */
object NCProbeVersion extends LazyLogging {
    object Status extends Enumeration {
        type Status = Value
        
        // General API codes.
        val NO_UPDATE: Value = Value
        val OPTIONAL_UPDATE: Value = Value
        val MANDATORY_UPDATE: Value = Value
    }
    
    import Status._
    
    /**
      *
      * @param version Sequentially increasing internal version of the probe.
      * @param date Date of this version release.
      * @param notes Release notes.
      * @param status Update status relative to the previous version.
      */
    case class Version(
        version: Int,
        date: LocalDate,
        notes: String,
        status: Status
    ) {
        override def toString = s"Probe version [version=$version, notes=$notes, date=$date, status=$status]"
    }
    
    // +=================================================+
    // | UPDATE THIS SEQUENCE FOR EACH RELEASE MANUALLY. |
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private final val VERSIONS = Seq(
        Version(1, LocalDate.of(2018, 12, 5), "Initial.", NO_UPDATE)
    ).sortBy(_.version)
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // | UPDATE THIS SEQUENCE FOR EACH RELEASE MANUALLY. |
    // +=================================================+
    
    if (G.distinct(VERSIONS.map(_.version).toList).lengthCompare(VERSIONS.size) != 0)
        throw new AssertionError(s"Prove versions are NOT unique.")
    
    /**
      * Gets current version.
      */
    def getCurrent: Version = VERSIONS.last
    
    /**
      * Gets updates status for given version.
      *
      * @param ver Version to get status update for.
      */
    def calculate(ver: Int): Status =
        VERSIONS.indexWhere(_.version == ver) match {
            case -1 ⇒
                logger.error(s"Unknown probe version (returning MANDATORY_UPDATE): $ver")
                
                MANDATORY_UPDATE
            
            case 0 ⇒ NO_UPDATE
            case i  ⇒
                val changes = VERSIONS.drop(i + 1)

                if (changes.nonEmpty) {
                    def check(s: Status): Boolean = changes.exists(_.status == s)

                    if (check(MANDATORY_UPDATE))
                        MANDATORY_UPDATE
                    else if (check(OPTIONAL_UPDATE))
                        OPTIONAL_UPDATE
                    else
                        NO_UPDATE
                }
                else
                    NO_UPDATE
        }
}

