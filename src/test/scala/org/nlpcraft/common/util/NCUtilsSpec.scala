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

package org.nlpcraft.common.util

import org.nlpcraft.common._
import org.scalatest.FlatSpec

/**
 * Utilities tests.
 */
class NCUtilsSpec extends FlatSpec {
    "inflate() and deflate() methods" should "work" in {
        val rawStr = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. " +
            "Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when " +
            "an unknown printer took a galley of type and scrambled it to make a type specimen book. " +
            "It has survived not only five centuries, but also the leap into electronic typesetting, " +
            "remaining essentially unchanged. It was popularised in the 1960s with the release of " +
            "Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing " +
            "software like Aldus PageMaker including versions of Lorem Ipsum."
        
        println(s"Original length: " + rawStr.length())
        
        val zipStr = U.compress(rawStr)
        val rawStr2 = U.uncompress(zipStr)
        
        println(s"Compressed length: " + zipStr.length())
        
        assert(rawStr == rawStr2)
    }
    
    "toFirstLastName() method" should "properly work" in {
        assert(U.toFirstLastName("A BbbBB") == ("A", "Bbbbb"))
        assert(U.toFirstLastName("aBC BbbBB CCC") == ("Abc", "Bbbbb ccc"))
        assert(U.toFirstLastName("abc b C C C") == ("Abc", "B c c c"))
    }

    "sleep method" should "work without unnecessary logging" in {
        val t = new Thread() {
            override def run(): Unit = {
                while (!isInterrupted) {
                    println("before sleep")

                    U.sleep(100)

                    println("after sleep")
                }
            }
        }

        t.start()

        U.sleep(550)

        t.interrupt()

        t.join()

        println("OK")
    }
}