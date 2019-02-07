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

package org.nlpcraft.examples.tests

import org.nlpcraft.mdllib.tools.dev.NCTestClient

import scala.collection.JavaConverters._

/**
  * Tests runner.
  */
object TestRunner {
    /**
      * Processes test expectations list with prepared test client.
      *
      * @param client Test client.
      * @param expsList Expectations list.
      * @return Flag all expectations are passed ot not.
      */
    def process(client: NCTestClient, expsList: java.util.List[TestExpectation]): Boolean = {
        val exps = expsList.asScala

        val results = client.test(exps.map(_.getTest).asJava).asScala

        require(exps.length == results.length)

        val errs =
            exps.zip(results).flatMap { case (exp, res) ⇒
                val test = exp.getTest

                var err: Option[String] = None

                if (exp.shouldPassed) {
                    if (res.getResultError.isPresent) {
                        err = Some(
                            s"Test should be passed but failed " +
                                s"[text=${test.getText}" +
                                s", modelId=${test.getModelId}" +
                                s", error=${res.getResultError.get()}" +
                                ']'
                        )
                    }
                    else if (exp.getResultChecker.isPresent) {
                        require(res.getResult.isPresent)

                        if (!exp.getResultChecker.get().test(res.getResult.get())) {
                            err = Some(
                                s"Test passed as expected but result validation failed " +
                                    s"[text=${test.getText}" +
                                    s", modelId=${test.getModelId}" +
                                    ']'
                            )
                        }
                    }
                }
                else {
                    if (!res.getResultError.isPresent) {
                        if (exp.getResultChecker.isPresent) {
                            require(res.getResult.isPresent)

                            if (exp.getResultChecker.get().test(res.getResult.get())) {
                                err = Some(
                                    s"Test passed and its result checked well but shouldn't be checked " +
                                        s"[text=${test.getText}" +
                                        s", modelId=${test.getModelId}" +
                                        ']'
                                )
                            }
                        }
                        else {
                            err = Some(
                                s"Test should be failed but passed " +
                                    s"[text=${test.getText}" +
                                    s", modelId=${test.getModelId}" +
                                    ']'
                            )
                        }
                    }
                    else if (exp.getErrorChecker.isPresent) {
                        require(res.getResultError.isPresent)

                        if (!exp.getErrorChecker.get().test(res.getResultError.get())) {
                            err = Some(
                                s"Test failed as expected but error message validation failed " +
                                    s"[text=${test.getText}" +
                                    s", modelId=${test.getModelId}" +
                                    ']'
                            )
                        }
                    }
                }

                err
            }

        if (errs.isEmpty) {
            println("All sentences processed as expected.")

            true
        }
        else {
            System.err.println("Errors list:")

            errs.foreach(System.err.println)

            false
        }
    }
}
