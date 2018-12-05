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

package org.nlpcraft.probe.mgrs.nlp.conversation

import org.nlpcraft.NCDebug
import org.nlpcraft.probe.NCProbeManager

import scala.collection._

/**
  * Conversation manager.
  */
object NCConversationManager extends NCProbeManager("PROBE conversation manager") with NCDebug {
    case class Key(userId: Long, dsId: Long)
    
    // TODO: add periodic garbage collector for stale conversations.
    private val convs = mutable.HashMap.empty[Key, NCConversation]

    /**
      * Gets conversation for given key.
      *
      * @param usrId User ID.
      * @param dsId Data source ID.
      * @return New or existing conversation.
      */
    def get(usrId: Long, dsId: Long): NCConversation = {
        convs.synchronized {
            convs.getOrElseUpdate(Key(usrId, dsId), NCConversation(usrId, dsId))
        }
    }
}
