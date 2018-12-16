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
    /**
      *
      * @param name Name of the boolean property.
      * @return
      */
    private def isSet(name: String): Boolean = G.sysEnv(name) match {
        case None ⇒ false
        case Some(s) ⇒ new java.lang.Boolean(s).booleanValue()
    }
    
    /** Development vs. production mode. */
    protected lazy final val IS_DEBUG: Boolean = isSet("NLPCRAFT_DEBUG")
    /** Verbose output or not. */
    protected lazy final val IS_PROBE_SILENT: Boolean = isSet("NLPCRAFT_PROBE_SILENT")
}