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

package org.nlpcraft.notification

import org.nlpcraft.{NCConfigurable, NCLifecycle}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
  * Push-based notification manager.
  */
object NCNotificationManager extends NCLifecycle("Notification manager") {
    case class Event(
        name: String,
        params: Seq[(String, Any)]
    )
    
    private object Config extends NCConfigurable {
        private val CFG_PATH = "notification.endpoints"
    
        private val epList: List[String] = if (hocon.hasPath(CFG_PATH)) hocon.getStringList(CFG_PATH).asScala.toList else Nil
        
        val flushMsec = hocon.getLong("notification.flushSecs") * 1000
        val maxBufferSize = hocon.getInt("notification.maxBufferSize")
    
        override def check(): Unit = {
            require(flushMsec > 0 , s"flush interval ($flushMsec) must be > 0")
            require(maxBufferSize > 0 , s"maximum buffer size ($maxBufferSize) must be > 0")
        }
    }
    
    Config.check()
    
    // Bounded buffer of events to be flushed.
    private val evts: ArrayBuffer[Event] = new ArrayBuffer[Event](Config.maxBufferSize)
    
    /**
      * Adds event with given name and optional parameters to the buffer. Buffer will be pushed to configured
      * endpoints periodically.
      *
      * @param evtName Event name.
      * @param params Optional set of named event parameters. Note that parameter values should JSON compatible.
      */
    def addEvent(evtName: String, params: (String, Any)*): Unit = {
        ensureStarted()
    
        evts.synchronized {
            evts += Event(evtName, params)
            
            if (evts.size > Config.maxBufferSize)
                flush()
        }
    }
    
    /**
      * Flushes accumulated events, if any, to the registered URL endpoints.
      */
    private def flush(): Unit = {
        evts.synchronized {
        
        }
    }
    
    override def start(): NCLifecycle = {
        super.start()
    }
    
    override def stop(): Unit = {
        super.stop()
    }
}
