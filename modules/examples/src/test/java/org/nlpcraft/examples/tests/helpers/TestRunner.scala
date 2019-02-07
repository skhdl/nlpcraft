package org.nlpcraft.examples.tests.helpers

import org.apache.http.util.Asserts
import org.junit.jupiter.api.Assertions
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
      */
    def test(client: NCTestClient, expsList: java.util.List[TestExpectation]): Unit = {
        val exps = expsList.asScala

        val results = client.test(exps.map(_.getTest).asJava).asScala

        require(exps.length == results.length)

        val errs =
            exps.zipWithIndex.zip(results).flatMap { case ((exp, idx), res) ⇒
                val test = exp.getTest
                val txt = test.getText

                require(test.getModelId.isPresent)

                val mdlId = test.getModelId.get()

                var errOpt: Option[String] = None

                if (exp.shouldPassed) {
                    if (res.getResultError.isPresent) {
                        errOpt = Some(
                            s"Test should be passed but failed " +
                                s"[text=$txt" +
                                s", modelId=$mdlId" +
                                s", error=${res.getResultError.get()}" +
                                ']'
                        )
                    }
                    else if (exp.getResultChecker.isPresent) {
                        require(res.getResult.isPresent)

                        if (!exp.getResultChecker.get().test(res.getResult.get())) {
                            errOpt = Some(
                                s"Test passed as expected but result validation failed " +
                                    s"[text=$txt" +
                                    s", modelId=$mdlId" +
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
                                errOpt = Some(
                                    s"Test passed and its result checked well but shouldn't be checked " +
                                        s"[text=$txt" +
                                        s", modelId=$mdlId" +
                                        ']'
                                )
                            }
                        }
                        else {
                            errOpt = Some(
                                s"Test should be failed but passed " +
                                    s"[text=$txt" +
                                    s", modelId=$mdlId" +
                                    ']'
                            )
                        }
                    }
                    else if (exp.getErrorChecker.isPresent) {
                        require(res.getResultError.isPresent)

                        if (!exp.getErrorChecker.get().test(res.getResultError.get())) {
                            errOpt = Some(
                                s"Test failed as expected but error message validation failed " +
                                    s"[text=$txt" +
                                    s", modelId=$mdlId" +
                                    ']'
                            )
                        }
                    }
                }

                errOpt match {
                    case Some(err) ⇒ Some(idx → err)
                    case None ⇒ None
                }
            }

        if (errs.isEmpty)
            println("All sentences processed as expected.")
        else {
            errs.foreach { case (idx, err) ⇒ System.err.println(s"${idx + 1}. $err") }

            Assertions.fail("See errors list.")
        }
    }
}
