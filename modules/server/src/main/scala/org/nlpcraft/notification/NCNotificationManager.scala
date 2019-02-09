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

package org.nlpcraft.notification

import org.nlpcraft.plugin.NCPluginManager
import org.nlpcraft._
import org.nlpcraft.plugin.apis.NCNotificationPlugin

/**
  * Push-based notification manager.
  */
object NCNotificationManager extends NCLifecycle("Notification manager") {
    private var plugin: NCNotificationPlugin = _
    
    /**
      * Passes over to the configured notification plugin.
      *
      * @param evtName Event name.
      * @param params Optional set of named event parameters. Note that parameter values should JSON compatible.
      */
    def addEvent(evtName: String, params: (String, Any)*): Unit = {
        ensureStarted()
    
        plugin.onEvent(evtName, params: _*)
    }
    
    /**
      * 
      * @return
      */
    override def start(): NCLifecycle = {
        plugin = NCPluginManager.getNotificationPlugin

        plugin.start()
        
        super.start()
    }

    /**
      * Stops this component.
      */
    override def stop(): Unit = {
        super.stop()

        if (plugin != null)
            plugin.stop()
    }
}
