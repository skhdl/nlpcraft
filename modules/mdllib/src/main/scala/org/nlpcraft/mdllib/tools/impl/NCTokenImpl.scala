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

package org.nlpcraft.mdllib.tools.impl

import org.nlpcraft.mdllib.{NCMetadata, NCToken}

/**
  *
  * @param srvReqId
  * @param elmId
  * @param elmGrp
  * @param parentId
  * @param value
  * @param tokMeta
  * @param elmMeta
  */
private[nlpcraft] class NCTokenImpl(
    srvReqId: String,
    elmId: String,
    elmGrp: String,
    parentId: String,
    value: String,
    tokMeta: NCMetadata,
    elmMeta: NCMetadata
) extends NCToken with Serializable {
    override def getMetadata: NCMetadata = tokMeta
    override def getElementMetadata: NCMetadata = elmMeta
    override def getServerRequestId: String = srvReqId
    override def getId: String = elmId
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
