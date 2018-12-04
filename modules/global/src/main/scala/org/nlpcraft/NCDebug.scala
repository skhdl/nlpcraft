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

package org.nlpcraft

/**
  * Runtime flags.
  */
trait NCDebug {
    private def getProperty(name: String): Option[String] = sys.props.get(name)
    private def isSet(name: String): Boolean = getProperty(name).isDefined
    
    /** Development vs. production mode. */
    protected lazy final val IS_DEBUG: Boolean = isSet("NLPCRAFT_DEBUG")
    /** Verbose output or not. */
    protected lazy final val IS_PROBE_SILENT: Boolean = isSet("NLPCRAFT_PROBE_SILENT")
    
    /** Running under production mode? */
    protected lazy final val IS_PROD: Boolean =
        getProperty("NLPCRAFT_PRODUCTION") match {
            case Some(v) ⇒ java.lang.Boolean.parseBoolean(v)
            case None ⇒ false
        }
}