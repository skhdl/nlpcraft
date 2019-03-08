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

package org.nlpcraft.common

import com.typesafe.scalalogging.LazyLogging
import scala.compat.Platform._

/**
  * Basic abstract class defining internal service/manager/component lifecycle. Components that
  * extend this class are typically called 'managers'.
  */
abstract class NCLifecycle(name: String) extends LazyLogging {
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
    @throws[NCE]
    def start(): NCLifecycle = {
        ensureStopped()
        
        started = true

        val dur = s"[${currentTime - startMsec}ms]"

        logger.info(s"$name started $dur")
        
        this
    }

    /**
     * Stops this component.
     */
    @throws[NCE]
    def stop() {
        checkStopping()
        
        started = false

        logger.info(s"$name stopped.")
    }
}
