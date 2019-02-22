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

package org.nlpcraft.server.ignite

import java.sql.SQLException

import com.typesafe.scalalogging.Logger
import org.apache.ignite.IgniteException
import org.nlpcraft.NCE

import scala.util.control.Exception._

/**
  * Mixin for Ignite exception handling.
  */
trait NCIgniteExceptions {
    /**
      * Partial function that wraps Ignite exceptions and rethrows 'NCE'.
      *
      * @tparam R Type of the return value for the body.
      * @return Catcher.
      */
    @throws[NCE]
    protected def wrapIE[R]: Catcher[R] = {
        case e: IgniteException ⇒ throw new NCE(s"Ignite error: ${e.getMessage}", e)
    }

    /**
      * Partial function that wraps SQL exceptions and rethrows 'NCE'.
      *
      * @tparam R Type of the return value for the body.
      * @return Catcher.
      */
    @throws[NCE]
    protected def wrapSql[R]: Catcher[R] = {
        case e: SQLException ⇒ throw new NCE(s"SQL error.", e)
    }

    /**
      * Partial function that catches Ignite exceptions and logs them.
      *
      * @return Catcher.
      */
    protected def logIE(logger: Logger): Catcher[Unit] = {
        case e: IgniteException ⇒ logger.error(s"Ignite error.", e)
    }
}
