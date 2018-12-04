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

package org.nlpcraft.mdllib.tools.impl

import org.nlpcraft.mdllib._
import org.nlpcraft.mdllib.{NCMetadata, NCToken}

/**
  *
  * @param srvReqId
  * @param elmId
  * @param elmGrp
  * @param elmTyp
  * @param parentId
  * @param value
  * @param tokMeta
  * @param elmMeta
  */
private[nlpcraft] class NCTokenImpl(
    srvReqId: String,
    elmId: String,
    elmGrp: String,
    elmTyp: String,
    parentId: String,
    value: String,
    tokMeta: NCMetadata,
    elmMeta: NCMetadata
) extends NCToken with Serializable {
    elmTyp match {
        case "STRING" | "LONG" | "DOUBLE" | "DATE" | "TIME" | "DATETIME" | "BOOLEAN" ⇒ ()
        case _ ⇒ throw new IllegalArgumentException(s"Invalid token type: $elmTyp")
    }
    
    override def getMetadata: NCMetadata = tokMeta
    override def getElementMetadata: NCMetadata = elmMeta
    override def getServerRequestId: String = srvReqId
    override def getId: String = elmId
    override def getType: String = elmTyp
    override def getGroup: String = elmGrp
    override def getParentId: String = parentId
    override def isUserDefined: Boolean = !elmId.startsWith("nlp:")
    override def isSystemDefined: Boolean = elmId.startsWith("nlp:")
    override def getValue: String = value

    override def toString: String =
        // NOTE: we don't print type and free words status on purpose.
        s"Token [" +
            s"id=$elmId, " +
            s"text=${tokMeta.getString("NLP_NORMTEXT")}, " +
            s"group=$elmGrp, " +
            s"value=$value" +
        s"]"
}
