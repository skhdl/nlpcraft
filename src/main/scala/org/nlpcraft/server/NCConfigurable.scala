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
import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.common._

/**
  * Mixin for configuration factory defined by default in `application.conf` file. Use `NLPCRAFT_CONFIG_FILE`
  * system property or environment variable to override the default.
  */
trait NCConfigurable extends LazyLogging {
    import NCConfigurable._
    
    // Accessor to the loaded config.
    private val hocon: Config = cfg
    
    /**
      * Gets mandatory configuration property.
      *
      * @param name Full configuration property path (name).
      */
    protected def getInt(name: String): Int = {
        if (!hocon.hasPath(name))
            abortError(s"Configuration property '$name' not found.")
        
        hocon.getInt(name)
    }
    
    /**
      * Gets mandatory configuration property.
      *
      * @param name Full configuration property path (name).
      */
    protected def getLong(name: String): Long = {
        if (!hocon.hasPath(name))
            abortError(s"Configuration property '$name' not found.")
        
        hocon.getLong(name)
    }
    
    /**
      * Gets mandatory configuration property.
      *
      * @param name Full configuration property path (name).
      */
    protected def getLongList(name: String): java.util.List[java.lang.Long] = {
        if (!hocon.hasPath(name))
            abortError(s"Configuration property '$name' not found.")
        
        hocon.getLongList(name)
    }

    /**
      * Gets mandatory configuration property.
      *
      * @param name Full configuration property path (name).
      */
    protected def getString(name: String): String = {
        if (!hocon.hasPath(name))
            abortError(s"Configuration property '$name' not found.")
        
        hocon.getString(name)
    }
    
    /**
      * Gets mandatory configuration property.
      *
      * @param name Full configuration property path (name).
      */
    protected def getStringList(name: String): java.util.List[String] = {
        if (!hocon.hasPath(name))
            abortError(s"Configuration property '$name' not found.")
        
        hocon.getStringList(name)
    }

    /**
      *
      * @param errMsgs
      */
    protected def abortError(errMsgs: String*): Unit = {
        errMsgs.foreach(s ⇒ logger.error(s"ERROR: $s"))
        
        // Abort immediately.
        System.exit(1)
    }

    /**
     * Calling this function will touch the object and trigger
     * the eager evaluation.
     */
    def check(): Unit = ()
}

object NCConfigurable {
    private final val cfgFile = U.sysEnv("NLPCRAFT_CONFIG_FILE").getOrElse("application.conf")
    
    // Singleton to load full NLPCraft configuration (only once).
    protected lazy val cfg: Config = {
        val x = ConfigFactory.parseFile(new java.io.File(cfgFile)).
            withFallback(ConfigFactory.load(cfgFile))
        
        if (!x.hasPath("server"))
            throw new IllegalStateException(
                "No configuration found. " +
                "Place 'application.conf' config file in the same folder or use '-config=path' to set alternative path to config file."
            )
        x
    }
}
