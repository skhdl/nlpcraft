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

import scala.collection.JavaConverters._
import scala.collection._
import NCTestClient._

/**
  * Test client usage test.
  */
class NCTestClientSpec extends FlatSpec with NCModelSpecBase {
    private val CFG = new NCTestClientConfig()

    // Override default to use localhost - change if required.
    CFG.setBaseUrl("http://localhost:8081/pub/v1/")

    case class TestHolder(test: NCTestSentence, shouldTestPassed: Boolean)

    private def test(client: NCTestClient, tests: Seq[TestHolder]): Unit = {
        val results = client.test(tests.map(_.test).asJava).asScala

        results.foreach(println)

        require(tests.size == results.size)

        val errs = mutable.ArrayBuffer.empty[String]

        results.zip(tests).foreach { case (res, sen) ⇒
            val ok = sen.shouldTestPassed && !res.hasError || !sen.shouldTestPassed && res.hasError

            if (!ok) {
                val resErr = if (res.hasError) res.getError else "No errors expected"

                errs +=
                    s"Unexpected test result " +
                    s"[" +
                        s"test=${toString(sen.test)}, " +
                        s"shouldTestPassed=${sen.shouldTestPassed}, " +
                        s"resultError=$resErr" +
                    s"]"
            }
        }

        errs.foreach(System.err.println)

        require(errs.isEmpty, s"See errors logs above")
    }

    private def toString(sen: NCTestSentence): String =
        "[" +
            "dsName=" + sen.getDsName +
            ", text=" + sen.getText +
            ", intentId=" + sen.getExpectedIntentId +
            ", status=" + convertStatus(sen.getExpectedStatus) +
        "]"

    private def convertStatus(code: Int): String =
        code match {
            case RESP_OK ⇒ "RESP_OK"
            case RESP_VALIDATION ⇒ "RESP_VALIDATION"
            case RESP_ERROR ⇒ "RESP_ERROR"

            case _ ⇒ throw new AssertionError(s"Unexpected state: $code")
        }

    it should "properly work" in {
        val client = NCTestClientBuilder.newBuilder().withClearConversation(false).build(CFG)

        // It should be started outside conversation context.
        test(
            client,
            Seq(
                // Reject because missed main ('weather') element.
                TestHolder(
                    new NCTestSentence(
                        "weather",
                        "test",
                        // Without intent (notFound solver method called)
                        NCTestClient.RESP_REJECT
                    ),
                    shouldTestPassed = true
                )
            )
        )

        test(
            client,
            Seq(
                // Waits for OK, result is not OK (invalid intent), so test shouldn't be OK.
                TestHolder(
                    new NCTestSentence(
                        "weather",
                        "LA weather",
                        "invalid.id",
                        NCTestClient.RESP_OK
                    ),
                    shouldTestPassed = false
                ),
                // Waits for REJECT, result is not OK (invalid intent), so test shouldn't be OK.
                TestHolder(
                    new NCTestSentence(
                        "weather",
                        "LA weather today today",
                        "invalid.id",
                        NCTestClient.RESP_REJECT
                    ),
                    shouldTestPassed = false
                ),
                // Validation because model configured to maximum one date token.
                TestHolder(
                    new NCTestSentence(
                        "weather",
                        "LA weather today second date today test",
                        NCTestClient.RESP_VALIDATION
                    ),
                    shouldTestPassed = true
                )
            )
        )
    }

    it should "take into account conversation" in {
        // Big delay to provide right order of executions.
        val builder = NCTestClientBuilder.newBuilder().withClearConversation(false).withDelay(2000)

        // Because batch mode turned off we can be sure that sentences processed one by one in correct order.
        // First OK, `weather` element added into conversation, short second is OK too,
        // because `weather` element received from conversation.
        test(
            builder.withClearConversation(false).withAsyncMode(false).build(CFG),
            Seq(
                TestHolder(
                    new NCTestSentence(
                        "weather",
                        "LA weather .",
                        "curr|date?|city?",
                        NCTestClient.RESP_OK
                    ),
                    shouldTestPassed = true
                ),
                TestHolder(
                    new NCTestSentence(
                        "weather",
                        "SF .",
                        "curr|date?|city?",
                        NCTestClient.RESP_OK
                    ),
                    shouldTestPassed = true
                )
            )
        )

        // Because batch mode turned off we can be sure that sentences processed one by one in correct order.
        // First OK, `weather` element added into conversation, but conversation cleared,
        // so second element rejected because missed main ('weather') element.
        test(
            builder.withClearConversation(true).withAsyncMode(false).build(CFG),
            Seq(
                TestHolder(
                    new NCTestSentence(
                        "weather",
                        "LA weather ..",
                        "curr|date?|city?",
                        NCTestClient.RESP_OK
                    ),
                    shouldTestPassed = true
                ),
                TestHolder(
                    new NCTestSentence(
                        "weather",
                        "SF ..",
                        // Without intent (notFound solver method called)
                        NCTestClient.RESP_REJECT
                    ),
                    shouldTestPassed = true
                )
            )
        )
    }
}
