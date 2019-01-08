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

package org.nlpcraft.mdo

import org.nlpcraft.db.postgres.NCPsql.Implicits.RsParser
import org.nlpcraft.mdllib.NCToken
import org.nlpcraft.mdo.impl._

/**
  * Query state MDO.
  */
@NCMdoEntity(sql = false)
case class NCQueryStateMdo(
    @NCMdoField srvReqId: String,
    @NCMdoField test: Boolean,
    @NCMdoField dsId: Long,
    @NCMdoField modelId: String,
    @NCMdoField var probeId: Option[String] = None, // Optional probe ID.
    @NCMdoField userId: Long,
    @NCMdoField email: String,
    @NCMdoField origText: String, // Text of the initial question.
    @NCMdoField createTstamp: Long, // Creation timestamp.
    @NCMdoField var updateTstamp: Long, // Last update timestamp.
    @NCMdoField var status: String,
    @NCMdoField var curateText: Option[String] = None, // Optional text after human curation.
    @NCMdoField var curateJson: Option[String] = None, // Optional request JSON explanation for curation.
    @NCMdoField var curateHint: Option[String] = None, // Optional hint after human curation.
    @NCMdoField(jsonConverter = "toJsonList") var tokens: Option[Seq[NCToken]] = None, // Optional tokens.
    @NCMdoField(jsonConverter = "toJsonList") var origTokens: Option[Seq[NCToken]] = None, // Optional tokens.
    @NCMdoField var cacheId: Option[Long] = None, // Optional cache ID.
    // Query OK (result, trivia, or talkback).
    @NCMdoField var resultType: Option[String] = None,
    @NCMdoField var resultBody: Option[String] = None,
    @NCMdoField var resultMetadata: Option[Map[String, Object]] = None,
    // Query ERROR.
    @NCMdoField var error: Option[String] = None
) extends NCAnnotatedMdo[NCQueryStateMdo]

object NCQueryStateMdo {
    implicit val x: RsParser[NCQueryStateMdo] =
        NCAnnotatedMdo.mkRsParser(classOf[NCQueryStateMdo])
}
