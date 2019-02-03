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

package org.nlpcraft.mdllib.tools.builder.impl;

import org.nlpcraft.mdllib.*;
import org.nlpcraft.mdllib.tools.*;
import org.nlpcraft.mdllib.tools.impl.*;
import java.io.*;
import java.util.*;

/**
 * Default model implementation.
 */
public class NCModelImpl implements NCModel {
    private NCModelDescriptor ds = null;
    private NCMetadata meta = new NCMetadataImpl();
    private Set<String> suspWords = new HashSet<>();
    private Set<String> examples = new HashSet<>();
    private Set<String> exclStopWords = new HashSet<>();
    private Set<NCElement> elms = new HashSet<>();
    private Map<String, String> macros = new HashMap<>();
    private Set<String> addStopWords = new HashSet<>();

    private NCSerializableFunction<NCQueryContext, NCQueryResult> qryFun;
    private NCSerializableConsumer<NCProbeContext> initFun;
    private NCSerializableRunnable discardFun;

    private String desc;
    private String docsUrl;
    private String vendorUrl;
    private String vendorEmail;
    private String vendorContact;
    private String vendorName;

    private int jiggleFactor = DFLT_JIGGLE_FACTOR;
    private int minDateTokens = DFLT_MIN_DATE_TOKENS;
    private int maxDateTokens = DFLT_MAX_DATE_TOKENS;
    private int minNumTokens = DFLT_MIN_NUM_TOKENS;
    private int maxNumTokens = DFLT_MAX_NUM_TOKENS;
    private int minGeoTokens = DFLT_MIN_GEO_TOKENS;
    private int maxGeoTokens = DFLT_MAX_GEO_TOKENS;
    private int minFunctionTokens = DFLT_MIN_FUNCTION_TOKENS;
    private int maxFunctionTokens = DFLT_MAX_FUNCTION_TOKENS;
    private int maxUnknownWords = DFLT_MAX_UNKNOWN_WORDS;
    private int maxFreeWords = DFLT_MAX_FREE_WORDS;
    private int maxSuspiciousWords = DFLT_MAX_SUSPICIOUS_WORDS;
    private int minWords = DFLT_MIN_WORDS;
    private int maxWords = DFLT_MAX_WORDS;
    private int minTokens = DFLT_MIN_TOKENS;
    private int maxTokens = DFLT_MAX_TOKENS;
    private int minNonStopwords = DFLT_MIN_NON_STOPWORDS;
    private boolean isNonEnglishAllowed = DFLT_IS_NON_ENGLISH_ALLOWED;
    private boolean isNotLatinCharsetAllowed = DFLT_IS_NOT_LATIN_CHARSET_ALLOWED;
    private boolean isSwearWordsAllowed = DFLT_IS_SWEAR_WORDS_ALLOWED;
    private boolean isNoNounsAllowed = DFLT_IS_NO_NOUNS_ALLOWED;
    private boolean isNoUserTokensAllowed = DFLT_IS_NO_USER_TOKENS_ALLOWED;
    private boolean isDupSynonymsAllowed = DFLT_IS_DUP_SYNONYMS_ALLOWED;
    private boolean isPermutateSynonyms = DFLT_IS_PERMUTATE_SYNONYMS;
    private int maxTotalSynonyms = DFLT_MAX_TOTAL_SYNONYMS;

    @Override
    public NCMetadata getMetadata() {
        return meta;
    }

    @Override
    public void discard() {
        if (discardFun != null)
            discardFun.run();
    }

    /**
     *
     * @param ds 
     */
    public void setDescriptor(NCModelDescriptor ds) {
        assert ds != null;

        this.ds = ds;
    }

    @Override
    public NCModelDescriptor getDescriptor() {
        return ds;
    }

    /**
     *
     * @param name
     * @param val
     */
    public void addMetadata(String name, Serializable val) {
        assert name != null;
        assert val != null;

        meta.put(name, val);
    }

    @Override
    public Set<String> getAdditionalStopWords() {
        return addStopWords;
    }

    @Override
    public Set<String> getExamples() {
        return examples;
    }

    @Override
    public Set<String> getSuspiciousWords() {
        return suspWords;
    }

    /**
     *
     * @param word
     */
    public void addSuspiciousWord(String word) {
        assert word != null;

        suspWords.add(word);
    }

    /**
     *
     * @param example
     */
    public void addExample(String example) {
        assert example != null;

        examples.add(example);
    }

    /**
     *
     * @param word
     */
    public void addAdditionalStopWord(String word) {
        assert word != null;

        addStopWords.add(word);
    }

    @Override
    public Set<String> getExcludedStopWords() {
        return exclStopWords;
    }

    /**
     *
     * @param word
     */
    public void addExcludedStopWord(String word) {
        assert word != null;

        exclStopWords.add(word);
    }

    @Override
    public Map<String, String> getMacros() {
        return macros;
    }

    /**
     * 
     * @param name
     * @param val
     */
    public void addMacro(String name, String val) {
        assert name != null;
        assert val != null;

        macros.put(name, val);
    }

    @Override
    public Set<NCElement> getElements() {
        return elms;
    }

    /**
     * 
     * @param elm
     */
    public void addElement(NCElement elm) {
        assert elm != null;

        elms.add(elm);
    }

    @Override
    public NCQueryResult query(NCQueryContext ctx) {
        return qryFun.apply(ctx);
    }

    @Override
    public void initialize(NCProbeContext probeCtx) {
        if (initFun != null)
            initFun.accept(probeCtx);
    }

    /**
     * 
     * @return
     */
    public NCSerializableFunction<NCQueryContext, NCQueryResult> getQueryFunction() {
        return qryFun;
    }

    /**
     *
     * @param qryFun
     */
    public void setQueryFunction(NCSerializableFunction<NCQueryContext, NCQueryResult> qryFun) {
        assert qryFun != null;

        this.qryFun = qryFun;
    }

    /**
     *
     * @return
     */
    public NCSerializableConsumer<NCProbeContext> getInitFunction() {
        return initFun;
    }

    /**
     *
     * @param initFun
     */
    public void setInitFunction(NCSerializableConsumer<NCProbeContext> initFun) {
        assert initFun != null;

        this.initFun = initFun;
    }

    /**
     *
     * @param discardFun
     */
    public void setDiscardFunction(NCSerializableRunnable discardFun) {
        assert discardFun != null;

        this.discardFun = discardFun;
    }
    
    public void setDocsUrl(String docsUrl) {
        this.docsUrl = docsUrl;
    }
    
    public void setVendorUrl(String vendorUrl) {
        this.vendorUrl = vendorUrl;
    }
    
    public void setVendorEmail(String vendorEmail) {
        this.vendorEmail = vendorEmail;
    }
    
    public void setVendorContact(String vendorContact) {
        this.vendorContact = vendorContact;
    }
    
    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }
    
    /**
     * 
     * @param desc
     */
    public void setDescription(String desc) {
        this.desc = desc;
    }

    @Override
    public int getJiggleFactor() {
        return jiggleFactor;
    }

    public void setJiggleFactor(int jiggleFactor) {
        this.jiggleFactor = jiggleFactor;
    }

    @Override
    public int getMinDateTokens() {
        return minDateTokens;
    }

    public void setMinDateTokens(int minDateTokens) {
        this.minDateTokens = minDateTokens;
    }

    @Override
    public int getMaxDateTokens() {
        return maxDateTokens;
    }

    public void setMaxDateTokens(int maxDateTokens) {
        this.maxDateTokens = maxDateTokens;
    }

    @Override
    public int getMinNumTokens() {
        return minNumTokens;
    }

    public void setMinNumTokens(int minNumTokens) {
        this.minNumTokens = minNumTokens;
    }

    @Override
    public int getMaxNumTokens() {
        return maxNumTokens;
    }

    public void setMaxNumTokens(int maxNumTokens) {
        this.maxNumTokens = maxNumTokens;
    }

    @Override
    public int getMinGeoTokens() {
        return minGeoTokens;
    }

    public void setMinGeoTokens(int minGeoTokens) {
        this.minGeoTokens = minGeoTokens;
    }

    @Override
    public int getMaxGeoTokens() {
        return maxGeoTokens;
    }

    public void setMaxGeoTokens(int maxGeoTokens) {
        this.maxGeoTokens = maxGeoTokens;
    }

    @Override
    public int getMinFunctionTokens() {
        return minFunctionTokens;
    }

    public void setMinFunctionTokens(int minFunctionTokens) {
        this.minFunctionTokens = minFunctionTokens;
    }

    @Override
    public int getMaxFunctionTokens() {
        return maxFunctionTokens;
    }

    public void setMaxFunctionTokens(int maxFunctionTokens) {
        this.maxFunctionTokens = maxFunctionTokens;
    }

    @Override
    public int getMaxUnknownWords() {
        return maxUnknownWords;
    }

    public void setMaxUnknownWords(int maxUnknownWords) {
        this.maxUnknownWords = maxUnknownWords;
    }

    @Override
    public int getMaxFreeWords() {
        return maxFreeWords;
    }

    public void setMaxFreeWords(int maxFreeWords) {
        this.maxFreeWords = maxFreeWords;
    }

    @Override
    public int getMaxSuspiciousWords() {
        return maxSuspiciousWords;
    }

    public void setMaxSuspiciousWords(int maxSuspiciousWords) {
        this.maxSuspiciousWords = maxSuspiciousWords;
    }

    @Override
    public int getMinWords() {
        return minWords;
    }

    public void setMinWords(int minWords) {
        this.minWords = minWords;
    }

    @Override
    public int getMaxWords() {
        return maxWords;
    }

    public void setMaxWords(int maxWords) {
        this.maxWords = maxWords;
    }

    @Override
    public int getMinTokens() {
        return minTokens;
    }

    public void setMinTokens(int minTokens) {
        this.minTokens = minTokens;
    }

    @Override
    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    @Override
    public int getMinNonStopwords() {
        return minNonStopwords;
    }

    public void setMinNonStopwords(int minNonStopwords) {
        this.minNonStopwords = minNonStopwords;
    }

    @Override
    public boolean isNonEnglishAllowed() {
        return isNonEnglishAllowed;
    }

    public void setNonEnglishAllowed(boolean nonEnglishAllowed) {
        isNonEnglishAllowed = nonEnglishAllowed;
    }

    @Override
    public boolean isNotLatinCharsetAllowed() {
        return isNotLatinCharsetAllowed;
    }

    public void setNotLatinCharsetAllowed(boolean notLatinCharsetAllowed) {
        isNotLatinCharsetAllowed = notLatinCharsetAllowed;
    }

    @Override
    public boolean isSwearWordsAllowed() {
        return isSwearWordsAllowed;
    }

    public void setSwearWordsAllowed(boolean swearWordsAllowed) {
        isSwearWordsAllowed = swearWordsAllowed;
    }

    @Override
    public boolean isNoNounsAllowed() {
        return isNoNounsAllowed;
    }

    public void setNoNounsAllowed(boolean noNounsAllowed) {
        isNoNounsAllowed = noNounsAllowed;
    }

    @Override
    public boolean isNoUserTokensAllowed() {
        return isNoUserTokensAllowed;
    }

    public void setNoUserTokensAllowed(boolean noUserTokensAllowed) {
        isNoUserTokensAllowed = noUserTokensAllowed;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public String getDocsUrl() {
        return docsUrl;
    }

    @Override
    public String getVendorUrl() {
        return vendorUrl;
    }

    @Override
    public String getVendorEmail() {
        return vendorEmail;
    }

    @Override
    public String getVendorContact() {
        return vendorContact;
    }

    @Override
    public String getVendorName() {
        return vendorName;
    }
    
    @Override
    public boolean isDupSynonymsAllowed() {
        return isDupSynonymsAllowed;
    }
    
    public void setDupSynonymsAllowed(boolean dupSynonymsAllowed) {
        isDupSynonymsAllowed = dupSynonymsAllowed;
    }
    
    @Override
    public boolean isPermutateSynonyms() {
        return isPermutateSynonyms;
    }
    
    public void setPermutateSynonyms(boolean isPermutateSynonyms) {
        this.isPermutateSynonyms = isPermutateSynonyms;
    }
    
    @Override
    public int getMaxTotalSynonyms() {
        return maxTotalSynonyms;
    }
    
    public void setMaxTotalSynonyms(int maxTotalSynonyms) {
        this.maxTotalSynonyms = maxTotalSynonyms;
    }
}
