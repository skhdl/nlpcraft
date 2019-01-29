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

package org.nlpcraft.mdllib.tools.dev;

import org.nlpcraft.mdllib.NCQueryResult;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Sentences builder for {@link NCTestSentence} instances.
 */
public class NCTestSentenceBuilder {
    public static boolean DFLT_SHOULD_PASSED = true;
    
    private Long dsId;
    private String modelId;
    private boolean shouldPassed = DFLT_SHOULD_PASSED;
    private Predicate<NCQueryResult> checkResult;
    private Predicate<String> checkError;
    
    /**
     * Creates new default builder instance.
     *
     * @return Builder instance.
     */
    public static NCTestSentenceBuilder newBuilder() {
        return new NCTestSentenceBuilder();
    }
    
    /**
     * TODO:
     * Sets datasource ID.
     * Datasource with given ID should be already registered in the system.
     *
     * @param dsId Datasource ID.
     * @return Builder instance for chaining calls.
     */
    public NCTestSentenceBuilder withDsId(long dsId) {
        this.dsId = dsId;
        
        return this;
    }
    
    /**
     * TODO:
     * Sets model ID.
     * Temporary datasource for given model will be registered in the system before test and deleted after.
     * Note that these temporary tests datasources can be remained in the system if tests crushed.
     * You should delete it from database manually in such cases.
     *
     * @param modelId User Datasource ID.
     * @return Builder instance for chaining calls.
     */
    public NCTestSentenceBuilder withModelId(String modelId) {
        this.modelId = modelId;
        
        return this;
    }
    
    /**
     * TODO:
     * Sets test expected behaviour flag.
     *
     * Default value is {@link NCTestSentenceBuilder#DFLT_SHOULD_PASSED}.
     *
     * @param shouldPassed Flag.
     * @return Builder instance for chaining calls.
     */
    public NCTestSentenceBuilder withShouldPassed(boolean shouldPassed) {
        this.shouldPassed = shouldPassed;
        
        return this;
    }
    
    /**
     * Sets result validation predicate.
     *
     * @param checkResult Result validation predicate..
     * @return Builder instance for chaining calls.
     */
    public NCTestSentenceBuilder withCheckResult(Predicate<NCQueryResult> checkResult) {
        this.checkResult = checkResult;
        this.shouldPassed = true;
        
        return this;
    }
    
    /**
     * Sets error validation predicate.
     *
     * @param checkError Error validation predicate..
     * @return Builder instance for chaining calls.
     */
    public NCTestSentenceBuilder withCheckError(Predicate<String> checkError) {
        this.checkError = checkError;
        this.shouldPassed = false;
        
        return this;
    }
    
    /**
     * Build new configured test sentence instance.
     *
     * @return Newly built test sentence instance.
     */
    public NCTestSentence build(String txt) {
        if (txt == null)
            throw new IllegalStateException("Text must be defined.");
        
        if (modelId == null && dsId == null)
            throw new IllegalStateException("Model ID or Datasource ID must be defined.");
        
        if (modelId != null && dsId != null)
            throw new IllegalStateException("Model ID or Datasource ID must be defined, but not both of them.");
        
        if (checkResult != null && checkError != null)
            throw new IllegalStateException(
                "Check result function or check error function can be defined, but not both of them."
            );
        
        if (checkResult != null && !shouldPassed)
            throw new IllegalStateException("Check result function can be defined only for successful results.");
        
        if (checkError != null && shouldPassed)
            throw new IllegalStateException("Check error function can be defined only for unsuccessful results.");
    
        return new NCTestSentence() {
            private<T> Optional<T> convert(T t) {
                return t != null ? Optional.of(t) : Optional.empty();
            }
            
            @Override
            public String getText() {
                return txt;
            }
    
            @Override
            public Optional<Long> getDatasourceId() {
                return convert(dsId);
            }
    
            @Override
            public Optional<String> getModelId() {
                return convert(modelId);
            }
    
            @Override
            public boolean shouldPassed() {
                return shouldPassed;
            }
    
            @Override
            public Optional<Predicate<NCQueryResult>> getCheckResult() {
                return convert(checkResult);
            }
    
            @Override
            public Optional<Predicate<String>> getCheckError() {
                return convert(checkError);
            }
        };
    }
}