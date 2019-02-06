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

package org.nlpcraft.mdllib.tools.dev

import java.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.nlpcraft.mdllib.NCModelSpecBase
import org.scalatest.FlatSpec

import scala.collection.JavaConverters._

/**
  * Spec for Weather2 example.
  */
class NCTestClientWeather2Spec extends FlatSpec with NCModelSpecBase {
    private final val GSON = new Gson
    private final val MAP_RESP = new TypeToken[util.HashMap[String, AnyRef]]() {}.getType

    case class Expectation(test: NCTestSentence, shouldPassed: Boolean, intentId: Option[String])

    private def mkPassed(txt: String, modelId: String, intentId: String): Expectation =
        Expectation(new NCTestSentence(txt, modelId), true, Some(intentId))

    private def mkFailed(txt: String, modelId: String): Expectation =
        Expectation(new NCTestSentence(txt, modelId), false, None)

    private def process(exps: Seq[Expectation]): Unit = {
        val results = new NCTestClientBuilder().newBuilder().build().test(exps.map(_.test).asJava).asScala

        require(exps.length == results.length)

        val errs =
            exps.zip(results).flatMap { case (exp, result) ⇒
                var err: Option[String] = None

                if (exp.shouldPassed) {
                    if (result.getResultError.isPresent) {
                        err = Some(
                            s"Test should be passed but failed " +
                                s"[text=${exp.test.getText}, " +
                                s"modelId=${exp.test.getModelId}, " +
                                s"error=${result.getResultError.get()}" +
                                ']'
                        )
                    }
                    else {
                        require(exp.intentId.isDefined)

                        val expIntentId = exp.intentId.get

                        val resMap: util.Map[String, Any] = GSON.fromJson(result.getResult.get(), MAP_RESP)

                        // See Weather2Provider result data format.
                        val resIntentId = resMap.asScala("intentId")

                        if (expIntentId != resIntentId)
                            err = Some(
                                s"Test passed with unexpected intent ID " +
                                    s"[text=${exp.test.getText}, " +
                                    s"modelId=${exp.test.getModelId}, " +
                                    s"expectedIntentId=$expIntentId, " +
                                    s"expectedIntentId=$resIntentId" +
                                    ']'
                            )

                    }
                }
                else {
                    if (!result.getResultError.isPresent) {
                        err = Some(
                            s"Test should be failed but passed " +
                                s"[text=${exp.test.getText}, " +
                                s"modelId=${exp.test.getModelId}" +
                                ']'
                        )
                    }
                }

                err
            }

        if (errs.isEmpty)
            println("All sentences processed as expected.")
        else {
            System.err.println("Errors list:")

            errs.foreach(System.err.println)
        }
    }

    it should "properly work" in {
        val exps =
            Seq(
                mkFailed("", "nlpcraft.weather2.ex"),
                mkPassed("LA weather", "nlpcraft.weather2.ex", "curr|date?|city?")
            )

        process(exps)
    }
}
