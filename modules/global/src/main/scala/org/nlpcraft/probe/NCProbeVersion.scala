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

