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

import com.datalingvo.DLE
import com.typesafe.scalalogging.LazyLogging

import scala.compat.Platform._

/**
 * Basic abstract class defining internal service/manager/component lifecycle.
 */
abstract class NCLifecycle(name: String) extends LazyLogging with NCDebug {
    private val startMsec = currentTime

    @volatile private var started = false

    /**
      * Call this method ONLY in `stop()` overrides.
      */
    protected final def checkStopping(): Unit =
        // Softer warning instead of an assertion to allow stopping components
        // that failed at starting.
        if (!started)
            logger.warn(s"Stopping component that wasn't properly started (ignoring): $name")
    
    /**
      * Call this method ONLY in user methods (other than `start()` and `stop()`).
      */
    protected final def ensureStarted(): Unit = require(started)
    
    /**
      * Call this method ONLY in `start()` overrides.
      */
    protected final def ensureStopped(): Unit = require(!started)
    
    /**
      * Checks if this component is started.
      */
    def isStarted: Boolean = started

    /**
     * Starts this component.
     */
    @throws[DLE]
    def start(): NCLifecycle = {
        ensureStopped()
        
        started = true

        val dur = s"[${currentTime - startMsec} ms]"

        logger.trace(s"$name started $dur")
        
        this
    }

    /**
     * Stops this component.
     */
    @throws[DLE]
    def stop() {
        checkStopping()
        
        started = false

        logger.trace(s"$name stopped.")
    }
}
