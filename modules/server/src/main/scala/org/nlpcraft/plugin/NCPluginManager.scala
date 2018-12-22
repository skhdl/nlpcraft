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

package org.nlpcraft.plugin

import org.nlpcraft.{NCConfigurable, NCE, NCLifecycle}
import scala.reflect.runtime.universe._

/**
  * Plugin manager.
  */
object NCPluginManager extends NCLifecycle("Plugin manager") {
    private var notifyPlugin: NCNotificationPlugin = _
    private var probeAuthPlugin: NCProbeAuthenticationPlugin = _
    
    private val mirror = runtimeMirror(getClass.getClassLoader)
    
    private object Config extends NCConfigurable {
        val notifyPluginClass: String = hocon.getString("plugins.notification")
        val probeAuthPluginClass: String = hocon.getString("plugins.probe.auth")
    }
    
    Config.check()
    
    /**
      * Starts plugin manager.
      */
    override def start(): NCLifecycle = {
        notifyPlugin = createPlugin("notify", Config.notifyPluginClass)
        probeAuthPlugin = createPlugin("probe authentication", Config.probeAuthPluginClass)

        super.start()
    }
    
    /**
      *
      * @param pluginName
      * @param clsName
      * @tparam T
      * @return
      */
    @throws[NCE]
    private def createPlugin[T](pluginName: String, clsName: String): T =
        try {
            val plugin = mirror.reflectModule(mirror.staticModule(clsName)).instance.asInstanceOf[T]
            
            logger.info(s"${pluginName.capitalize} plugin instantiated: $clsName")
            
            plugin
        }
        catch {
            case e: Exception ⇒ throw new NCE(s"Failed to instantiate $pluginName plugin: ${e.getLocalizedMessage}")
        }
    
    /**
      *
      * @return
      */
    def getNotificationPlugin: NCNotificationPlugin = {
        ensureStarted()
        
        notifyPlugin
    }
    
    /**
      * 
      * @return
      */
    def getProbeAuthenticationPlugin: NCProbeAuthenticationPlugin = {
        ensureStarted()
        
        probeAuthPlugin
    }
}
