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
 * TODO:
 */
public class NCTestSentenceBuilder {
    private Long dsId;
    private String modelId;
    private Boolean successful;
    private Predicate<NCQueryResult> checkResult;
    private Predicate<String> checkError;
    
    /**
     *
     * @return
     */
    public static NCTestSentenceBuilder newBuilder() {
        return new NCTestSentenceBuilder();
    }
    
    /**
     *
     * @return
     */
    public NCTestSentenceBuilder withDsId(long dsId) {
        this.dsId = dsId;
        
        return this;
    }
    
    /**
     *
     * @return
     */
    public NCTestSentenceBuilder withModelId(String modelId) {
        this.modelId = modelId;
        
        return this;
    }
    
    /**
     *
     * @return
     */
    public NCTestSentenceBuilder withSuccessfulFlag(boolean successful) {
        this.successful = successful;
        
        return this;
    }
    
    /**
     *
     * @return
     */
    public NCTestSentenceBuilder withCheckResult(Predicate<NCQueryResult> checkResult) {
        this.checkResult = checkResult;
        
        return this;
    }
    
    /**
     *
     * @return
     */
    public NCTestSentenceBuilder withCheckError(Predicate<String> checkError) {
        this.checkError = checkError;
        
        return this;
    }
    
    /**
     *
     * @return
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
        
        if (checkResult != null && successful != null && !successful)
            throw new IllegalStateException("Check result function can be defined only for successful results.");
        
        if (checkError != null && successful != null && successful)
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
            public boolean isExpectedPassed() {
                // True by default;
                return successful != null ? successful : checkError == null;
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