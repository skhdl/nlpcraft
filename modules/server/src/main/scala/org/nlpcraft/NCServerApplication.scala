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

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.rest.NCRestManager
import org.nlpcraft.util.NCGlobals
import org.nlpcraft.ignite.NCIgniteServer
import org.nlpcraft.login.NCLoginManager
import org.nlpcraft.tx.NCTxManager

/**
 * Application.
 */
object NCServerApplication extends NCIgniteServer("server.xml") with LazyLogging {
    override def name() = "NLPCraft application"

    // Starts all managers.
    private def startComponents(): Unit = {
        NCTxManager.start()
        NCDbManager.start()
        NCLoginManager.start()
        NCRestManager.start()
    }

    /**
      * Initializes server without blocking thread.
      */
    private[nlpcraft] def initialize() {
        startComponents()

        // Ack server start.
        ackStart()
    }

    // Stops all managers.
    private def stopComponents(): Unit = {
        NCTxManager.stop()
        NCDbManager.stop()
        NCLoginManager.stop()
        NCRestManager.stop()
    }

    /**
     * Stops the server by counting down (i.e. releasing) the lifecycle latch.
     */
    override def stop(): Unit = {
        stopComponents()

        super.stop()
    }

    /**
     * Code to execute within Ignite node.
     */
    override def start() {
        super.start()

        initialize()

        try
            NCGlobals.ignoreInterrupt {
                lifecycle.await()
            }
        finally
            stop()
    }
}