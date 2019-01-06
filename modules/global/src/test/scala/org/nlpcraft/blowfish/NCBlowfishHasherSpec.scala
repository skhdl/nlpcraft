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

package org.nlpcraft.blowfish

import org.scalatest.FlatSpec

/**
  * Tests for Blowfish hasher.
  */
class NCBlowfishHasherSpec extends FlatSpec {
    behavior of "Blowfish hasher"
    
    it should "properly hash password" in {
        val email = "dev@nlpcraft.org"
        val passwd = "test"
        
        val salt1 = NCBlowfishHasher.hash(email)
        val salt2 = NCBlowfishHasher.hash(email)
    
        println(s"Salt1: $salt1")
        println(s"Salt2: $salt2")
        
        val hash1 = NCBlowfishHasher.hash(passwd, salt1)
        val hash2 = NCBlowfishHasher.hash(passwd, salt1)
    
        assert(hash1 == hash2)

        println(s"Salt: $salt1")
        println(s"Hash: $hash1")
    }
}
