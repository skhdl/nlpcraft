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

package org.nlpcraft.ascii

import org.scalatest.FlatSpec

/**
 * Test for ASCII text table.
 */
class NCAsciiTableSpec extends FlatSpec {
    behavior of "ASCII table"

    it should "render" in {
        val t = NCAsciiTable()
        
        t.headerStyle = "leftPad: 10, rightPad: 5"

        t.margin(5, 5, 5, 5)

        t.maxCellWidth = 10

        t #= ("Header 1", Seq("Header 2.1", "Header 2.2"), "Header 3")
        t += ("Row 1", Seq("Row 2"), Seq("Row 3.1", "Row 3.2"))
        t += ("1234567890zxcvbnmasdASDFGHJKLQ", Seq("Row 2"), Seq("Row 3.1", "Row 3.2"))
        t += (Seq("Row 31.1", "Row 31.2"), "Row 11", "Row 21")

        t.render()
    }

    it should "render with sequence header" in {
        val t = NCAsciiTable()
        
        t.headerStyle = "leftPad: 10, rightPad: 5"

        t.margin(5, 5, 5, 5)

        t.maxCellWidth = 10

        t #= (Seq("Header 1", "Header 2", "Header 3", "Header 4"): _*)
        t += ("Column 1", "Column 2", "Column 3", "Column 4")

        t.render()
    }

    it should "render a very big table" in {
        val NUM = 1000

        val start = System.currentTimeMillis()

        val t = NCAsciiTable()
        
        t.headerStyle = "leftPad: 10, rightPad: 5"

        t #= (Seq("Header 1", "Header 2", "Header 3"): _*)

        for (i ← 0 to NUM)
            t += (s"Value 1:$i", s"Value 2:$i", s"Value 3:$i")

        t.render()

        val dur = System.currentTimeMillis() - start

        println(s"Rendered in ${dur}msec.")
    }
}
