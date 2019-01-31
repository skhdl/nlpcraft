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

package org.nlpcraft.mdllib.tools.dev

import org.nlpcraft.mdllib.NCModelSpecBase
import org.scalatest.FlatSpec

/**
  * TODO: should be removed.
  */
class NCTestClientSpec2 extends FlatSpec with NCModelSpecBase {
    case class TestHolder(test: NCTestSentence, shouldTestPassed: Boolean)

//    it should "properly work" in {
//        val client = new NCTestClientBuilder().newBuilder().build()
//
//        client.test(new NCTestSentence("LA weather", "nlpcraft.weather.ex"))
//    }

    it should "properly work with errors" in {
        val client = new NCTestClientBuilder().newBuilder().build()

        client.test(
            new NCTestSentence(
                "The US has warned Venezuela that any threats against American diplomats or opposition leader Juan Guaidó will be met with \"a significant response\".\n\nNational Security Adviser John Bolton said any such \"intimidation\" would be \"a grave assault on the rule of law\".\n\nHis warning comes days after the US and more than 20 other countries recognised Mr Guaidó as interim president.\n\nMeanwhile, Mr Guaidó has called for anti-government protests on Wednesday and Saturday.\n\nMr Guaidó, the elected leader of the opposition-held National Assembly, declared himself the interim president on 23 January.\n\nThe political crisis in Venezuela now appears to be reaching boiling point amid growing efforts by the opposition to unseat President Nicolás Maduro.\n\nHe was sworn in for a second term earlier this month after an election marred by an opposition boycott and allegations of vote-rigging, triggering large protests.\n\n    What's behind Venezuela's political crisis?\n    Venezuela crisis - in seven charts\n    Will the US target Venezuelan oil?\n\nOn Sunday, Venezuela's top military representative to the US, Col José Luis Silva, defected from Mr Maduro's government, saying he recognised Mr Guaidó as president instead.\n\nLater, Mr Bolton took to Twitter to reiterate Washington's position, warning others against any form of \"violence and intimidation\".\nSkip Twitter post by @AmbJohnBolton\n\nEnd of Twitter post by @AmbJohnBolton\nSkip Twitter post 2 by @AmbJohnBolton\n\nEnd of Twitter post 2 by @AmbJohnBolton\n\nAlso on Twitter, Mr Guaidó called for a \"peaceful\" two-hour strike on Wednesday and a \"big national and international rally\" on Saturday.\nWhat happens now?",
                "nlpcraft.weather.ex"
            )
        )
    }
}
