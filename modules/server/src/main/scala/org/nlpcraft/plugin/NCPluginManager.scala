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

package org.nlpcraft.plugin

import org.nlpcraft.plugin.apis.{NCNotificationPlugin, NCProbeAuthenticationPlugin}
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
        notifyPlugin = createPlugin("notification", Config.notifyPluginClass)
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
