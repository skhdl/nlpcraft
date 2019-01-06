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
