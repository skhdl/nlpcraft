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

package org.nlpcraft.server

import com.typesafe.config.{Config, ConfigFactory}
import org.nlpcraft.common._

/**
  * Mixin for configuration factory defined by default in `nlpcraft.conf` file. Use `NLPCRAFT_CONFIG_FILE`
  * system property or environment variable to override the default.
  */
trait NCConfigurable {
    import NCConfigurable._
    
    // Accessor to the loaded config.
    protected val hocon: Config = cfg

    /**
     * Calling this function will touch the object and trigger
     * the eager evaluation.
     */
    def check(): Unit = ()
}

object NCConfigurable {
    private final val cfgFile = U.sysEnv("NLPCRAFT_CONFIG_FILE").getOrElse("nlpcraft.conf")
    
    // Singleton to load full NLPCraft configuration (only once).
    protected lazy val cfg: Config = {
        val x = ConfigFactory.parseFile(new java.io.File(cfgFile)).
            withFallback(ConfigFactory.load(cfgFile))
        
        if (!x.hasPath("probe"))
            throw new IllegalStateException(
                "No configuration found. " +
                "Place 'nlpcraft.conf' config file in the same folder or use '-config=path' to set alternative path to config file."
            )
        x
    }
}
