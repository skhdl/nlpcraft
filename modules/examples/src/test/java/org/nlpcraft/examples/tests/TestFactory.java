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

package org.nlpcraft.examples.tests;

import org.nlpcraft.mdllib.tools.dev.NCTestSentence;

import java.util.Optional;
import java.util.function.Function;

/**
 * Test exception instances factory. See {@link TestExpectation}.
 */
public class TestFactory {
    private static class ExpectationImpl implements TestExpectation {
        private NCTestSentence test;
        private boolean shouldPassed;
        private Function<String, Optional<String>> resultChecker;
        private Function<String, Optional<String>>errorChecker;
    
        ExpectationImpl(
            String txt,
            String mdlId,
            boolean shouldPassed,
            Function<String, Optional<String>> resultChecker,
            Function<String, Optional<String>> errorChecker
        ) {
            assert txt != null;
            assert mdlId != null;
            assert shouldPassed || resultChecker == null;
            assert !shouldPassed || errorChecker == null;
            
            this.test = new NCTestSentence(txt, mdlId);
            this.shouldPassed = shouldPassed;
            this.resultChecker = resultChecker;
            this.errorChecker = errorChecker;
        }
    
        @Override
        public NCTestSentence getTest() {
            return test;
        }
    
        @Override
        public boolean shouldPassed() {
            return shouldPassed;
        }
    
        @Override
        public Optional<Function<String, Optional<String>>> getResultChecker() {
            return resultChecker == null ? Optional.empty() : Optional.of(resultChecker);
        }
    
        @Override
        public Optional<Function<String, Optional<String>>> getErrorChecker() {
            return errorChecker == null ? Optional.empty() : Optional.of(errorChecker);
        }
    }
    
    /**
     * Initializes {@link TestExpectation} instance, which execution should be passed, without additional result checkers.
     *
     * @param txt Sentence text.
     * @param mdlId Model ID.
     * @return {@link TestExpectation} instance
     */
    public TestExpectation mkPassed(String txt, String mdlId) {
        return new ExpectationImpl(txt, mdlId, true, null, null);
    }
    
    /**
     * Initializes {@link TestExpectation} instance, which execution should be passed, with additional result checkers.
     *
     * @param txt Sentence text.
     * @param mdlId Model ID.
     * @param resultChecker Result checker. See {@link TestExpectation#getResultChecker()}
     * @return {@link TestExpectation} instance
     */
    public TestExpectation mkPassed(String txt, String mdlId, Function<String, Optional<String>> resultChecker) {
        return new ExpectationImpl(txt, mdlId, true, resultChecker, null);
    }
    
    /**
     * Initializes {@link TestExpectation} instance, which execution should be failed,
     * without additional error messages checkers.
     *
     * @param txt Sentence text.
     * @param mdlId Model ID.
     * @return {@link TestExpectation} instance
     */
    public TestExpectation mkFailed(String txt, String mdlId) {
        return new ExpectationImpl(txt, mdlId, false, null, null);
    }
    
    /**
     * Initializes {@link TestExpectation} instance, which execution should be failed,
     * with additional error message checkers.
     *
     * @param txt Sentence text.
     * @param mdlId Model ID.
     * @param errorChecker Error checker. See {@link TestExpectation#getErrorChecker()}
     * @return {@link TestExpectation} instance
     */
    public TestExpectation mkFailed(String txt, String mdlId, Function<String, Optional<String>> errorChecker) {
        return new ExpectationImpl(txt, mdlId, false, null, errorChecker);
    }
}
