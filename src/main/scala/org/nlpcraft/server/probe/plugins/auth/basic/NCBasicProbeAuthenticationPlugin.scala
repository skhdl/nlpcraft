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

package org.nlpcraft.server.probe.plugins.auth.basic

import java.security.Key

import org.nlpcraft.common._
import org.nlpcraft.common.crypto.NCCipher
import org.nlpcraft.server.NCConfigurable
import org.nlpcraft.server.plugin.apis.NCProbeAuthenticationPlugin

/**
  * Basic probe authentication plugin.
  */
object NCBasicProbeAuthenticationPlugin extends NCProbeAuthenticationPlugin {
    // Configuration prefix.
    private final val CFG = "server.org.nlpcraft.server.probe.plugins.auth.basic.NCBasicProbeAuthenticationPlugin"
    
    private object Config extends NCConfigurable {
        val probeToken: String = getString(s"$CFG.probe.token")
    
        override def check(): Unit = {
            if (probeToken == null)
                abortError(s"Configuration property '$CFG.probe.token' must be specified.")
        }
    }
    
    Config.check()
    
    private val srvHash = U.makeSha256Hash(Config.probeToken)
    private val cryptoKey = NCCipher.makeTokenKey(Config.probeToken)
    
    /**
      * 
      * @param probeTokenHash Probe token hash.
      * @return An encryption key for a given probe token hash, or `None` if given hash is unknown or invalid.
      */
    override def acquireKey(probeTokenHash: String): Option[Key] = {
        if (probeTokenHash != srvHash)
            None
        else
            Some(cryptoKey)
    }
}
