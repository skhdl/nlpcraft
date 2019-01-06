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

package org.nlpcraft.crypto

import java.util.Base64
import org.nlpcraft._
import org.scalatest.{BeforeAndAfter, FlatSpec}

/**
 * Tests for crypto and PKI support.
 */
class NCCipherSpec extends FlatSpec with BeforeAndAfter {
    behavior of "Cipher"

    private final val IN = "abcdefghijklmnopqrstuvwxyz0123456789"

    it should "properly encrypt and decrypt" in {
        val s1 = NCCipher.encrypt(IN)
        val s2 = NCCipher.decrypt(s1)

        assertResult(IN)(s2)
    }

    it should "produce different encrypted string for the same input" in {
        val s1 = NCCipher.encrypt(IN)
        val s2 = NCCipher.encrypt(IN)
        val s3 = NCCipher.encrypt(IN)

        assert(s1 != s2)
        assert(s2 != s3)

        val r1 = NCCipher.decrypt(s1)
        val r2 = NCCipher.decrypt(s2)
        val r3 = NCCipher.decrypt(s3)

        assertResult(r1)(IN)
        assertResult(r2)(IN)
        assertResult(r3)(IN)
    }
    
    it should "properly encrypt" in {
        val buf = new StringBuilder
        
        // Max long string.
        for (i ← 0 to 1275535) buf.append(i.toString)
        
        val str = buf.toString
        
        val bytes = G.serialize(str)
        
        val key = NCCipher.makeTokenKey(G.genGuid())
        
        val now = System.currentTimeMillis()
        
        val sec = NCCipher.encrypt(Base64.getEncoder.encodeToString(bytes), key)
        
        val dur = System.currentTimeMillis() - now
        
        println(s"Input length: ${str.length}")
        println(s"Output length: ${sec.length}")
        println(s"Total: $dur ms.")
    }
}
