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
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.proclog

import java.sql.Timestamp

import org.nlpcraft._
import org.nlpcraft.apicodes.NCApiStatusCode.NCApiStatusCode
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.db.postgres.NCPsql

/**
  * Process log manager.
  */
object NCProcessLogManager extends NCLifecycle("Process log manager") {
    /**
      * 
      * @param srvReqId
      * @param tstamp
      */
    @throws[NCE]
    def updateCancel(
        srvReqId: String,
        tstamp: Long
    )
    : Unit = {
        ensureStarted()
    
        NCPsql.sql {
            NCDbManager.updateCancelProcessingLog(
                srvReqId,
                tstamp
            )
        }
    }
    
    /**
      * Updates log entry with given result parameters.
      * 
      * @param srvReqId ID of the server request to update.
      * @param tstamp
      * @param errMsg
      * @param resType
      * @param resBody
      */
    @throws[NCE]
    def updateReady(
        srvReqId: String,
        tstamp: Timestamp,
        errMsg: Option[String] = None,
        resType: Option[String] = None,
        resBody: Option[String] = None
    ): Unit = {
        ensureStarted()
    
        NCPsql.sql {
            NCDbManager.updateReadyProcessingLog(
                srvReqId,
                errMsg.orNull,
                resType.orNull,
                resBody.orNull,
                tstamp
            )
        }
    }
    
    /**
      * Adds new processing log entry.
      * 
      * @param usrId
      * @param srvReqId
      * @param txt
      * @param dsId
      * @param mdlId
      * @param status
      * @param test
      * @param usrAgent
      * @param rmtAddr
      * @param rcvTstamp
      */
    @throws[NCE]
    def newEntry(
        usrId: Long,
        srvReqId: String,
        txt: String,
        dsId: Long,
        mdlId: String,
        status: NCApiStatusCode,
        test: Boolean,
        usrAgent: String,
        rmtAddr: String,
        rcvTstamp: Timestamp
    ): Unit = {
        ensureStarted()
        
        NCPsql.sql {
            NCDbManager.newProcessingLog(
                usrId,
                srvReqId,
                txt,
                dsId,
                mdlId,
                status,
                test,
                usrAgent,
                rmtAddr,
                rcvTstamp
            )
        }
    }
}
