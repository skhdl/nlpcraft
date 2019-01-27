package org.nlpcraft.mdllib.tools.dev;

import org.nlpcraft.mdllib.NCQueryResult;

import java.util.function.Predicate;

public class NCTestSentenceBuilder {
    private Long dsId;
    private String modelId;
    private String text;
    private Boolean successful;
    private Predicate<NCQueryResult> checkResult;
    private Predicate<String> checkError;
    
    public static NCTestSentenceBuilder newBuilder() {
        return new NCTestSentenceBuilder();
    }
    
    public NCTestSentenceBuilder setDsId(long dsId) {
        this.dsId = dsId;
        
        return this;
    }
    
    public NCTestSentenceBuilder setModelId(String modelId) {
        this.modelId = modelId;
        
        return this;
    }
    
    public NCTestSentenceBuilder setText(String text) {
        this.text = text;
        
        return this;
    }
    
    public NCTestSentenceBuilder setSuccessful(boolean successful) {
        this.successful = successful;
        
        return this;
    }
    
    public NCTestSentenceBuilder setCheckResult(Predicate<NCQueryResult> checkResult) {
        this.checkResult = checkResult;
        
        return this;
    }
    
    public NCTestSentenceBuilder setCheckError(Predicate<String> checkError) {
        this.checkError = checkError;
        
        return this;
    }
    
    public NCTestSentence createNCTestSentence() {
        if (text == null)
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
            @Override
            public String getModelId() {
                return modelId;
            }
            
            @Override
            public Long getDsId() {
                return dsId;
            }
            
            @Override
            public String getText() {
                return text;
            }
            
            @Override
            public boolean isSuccessful() {
                return successful != null ? successful : checkError == null; // True by default.
            }
            
            @Override
            public Predicate<NCQueryResult> getCheckResult() {
                return checkResult;
            }
            
            @Override
            public Predicate<String> getCheckError() {
                return checkError;
            }
        };
    }
}