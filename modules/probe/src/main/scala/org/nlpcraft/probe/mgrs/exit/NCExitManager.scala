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

package org.nlpcraft.probe.mgrs.exit

import java.util.concurrent.CountDownLatch
import org.nlpcraft.probe.NCProbeManager
import org.nlpcraft.{NCLifecycle, _}

/**
  * Probe process exit manager.
  */
object NCExitManager extends NCProbeManager("PROBE exit manager") with NCDebug {
    // JVM exit codes.
    final val EXIT_FAIL = 0
    final val EXIT_OK = 1
    final val RESTART = 2
    
    private var code = EXIT_FAIL
    private var latch: CountDownLatch = _
    
    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        latch = new CountDownLatch(1)
        
        super.start()
    }
    
    /**
      * Blocks and awaits until `exit()` method is called.
      *
      * @return Exit code.
      */
    def awaitExit(): Int = {
        while (latch.getCount > 0)
            G.ignoreInterrupt {
                latch.await()
            }
        
        code
    }
    
    /**
      * Exit probe with `RESTART` code.
      */
    def restart(): Unit = {
        this.code = RESTART
        
        latch.countDown()
    }

    /**
      * Exit probe with `EXIT_OK` code.
      */
    def exit(): Unit = {
        this.code = EXIT_OK
        
        latch.countDown()
    }
    
    /**
      * Exit probe with `EXIT_FAIL` code.
      */
    def fail(): Unit = {
        this.code = EXIT_FAIL
        
        latch.countDown()
    }
}
