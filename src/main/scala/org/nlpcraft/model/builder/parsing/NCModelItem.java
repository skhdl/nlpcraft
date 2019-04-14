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

package org.nlpcraft.model.builder.parsing;

import java.util.Map;

import static org.nlpcraft.model.NCModel.*;

/**
 * Parsing bean.
 */
public class NCModelItem {
    private String id;
    private String name;
    private String version;
    private String description;
    private String docsUrl;
    private String vendorUrl;
    private String vendorEmail;
    private String vendorContact;
    private String vendorName;
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
    private int jiggleFactor = DFLT_JIGGLE_FACTOR;
    private int minDateTokens = DFLT_MIN_DATE_TOKENS;
    private int maxDateTokens = DFLT_MAX_DATE_TOKENS;
    private int minNumTokens = DFLT_MIN_NUM_TOKENS;
    private int maxNumTokens = DFLT_MAX_NUM_TOKENS;
    private int minGeoTokens = DFLT_MIN_GEO_TOKENS;
    private int maxGeoTokens = DFLT_MAX_GEO_TOKENS;
    private int minFunctionTokens = DFLT_MIN_FUNCTION_TOKENS;
    private int maxFunctionTokens = DFLT_MAX_FUNCTION_TOKENS;
    private boolean isDupSynonymsAllowed = DFLT_IS_DUP_SYNONYMS_ALLOWED;
    private int maxTotalSynonyms = DFLT_MAX_TOTAL_SYNONYMS;
    private boolean isPermutateSynonyms = DFLT_IS_PERMUTATE_SYNONYMS;
    private Map<String, Object> usrMetadata;
    private NCMacroItem[] macros = null;
    private NCElementItem[] elements = null;
    private String[] additionalStopwords = null;
    private String[] excludedStopwords = null;
    private String[] suspiciousWords = null;
    private String[] examples = null;

    /**
     *
     * @return
     */
    public String[] getExamples() {
        return examples;
    }

    /**
     * 
     * @param examples
     */
    public void setExamples(String[] examples) {
        this.examples = examples;
    }

    /**
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     */
    public String getVersion() {
        return version;
    }

    /**
     *
     * @param version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getUserMetadata() {
        return usrMetadata;
    }

    /**
     *
     * @param usrMetadata
     */
    public void setUserMetadata(Map<String, Object> usrMetadata) {
        this.usrMetadata = usrMetadata;
    }

    /**
     *
     * @return
     */
    public NCMacroItem[] getMacros() {
        return macros;
    }

    /**
     *
     * @param macros
     */
    public void setMacros(NCMacroItem[] macros) {
        this.macros = macros;
    }

    /**
     *
     * @return
     */
    public NCElementItem[] getElements() {
        return elements;
    }

    /**
     *
     * @param elements
     */
    public void setElements(NCElementItem[] elements) {
        this.elements = elements;
    }

    /**
     *
     * @return
     */
    public String[] getAdditionalStopwords() {
        return additionalStopwords;
    }

    /**
     *
     * @param additionalStopwords
     */
    public void setAdditionalStopwords(String[] additionalStopwords) {
        this.additionalStopwords = additionalStopwords;
    }

    /**
     *
     * @return
     */
    public String[] getExcludedStopwords() {
        return excludedStopwords;
    }

    /**
     *
     * @param excludedStopwords
     */
    public void setExcludedStopwords(String[] excludedStopwords) {
        this.excludedStopwords = excludedStopwords;
    }

    /**
     *
     * @return
     */
    public String[] getSuspiciousWords() {
        return suspiciousWords;
    }

    /**
     * 
     * @param suspiciousWords
     */
    public void setSuspiciousWords(String[] suspiciousWords) {
        this.suspiciousWords = suspiciousWords;
    }

    /**
     *
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return
     */
    public String getDocsUrl() {
        return docsUrl;
    }

    /**
     *
     * @param docsUrl
     */
    public void setDocsUrl(String docsUrl) {
        this.docsUrl = docsUrl;
    }

    /**
     *
     * @return
     */
    public String getVendorUrl() {
        return vendorUrl;
    }

    /**
     *
     * @param vendorUrl
     */
    public void setVendorUrl(String vendorUrl) {
        this.vendorUrl = vendorUrl;
    }

    /**
     *
     * @return
     */
    public String getVendorEmail() {
        return vendorEmail;
    }

    /**
     *
     * @param vendorEmail
     */
    public void setVendorEmail(String vendorEmail) {
        this.vendorEmail = vendorEmail;
    }

    /**
     *
     * @return
     */
    public String getVendorContact() {
        return vendorContact;
    }

    /**
     *
     * @param vendorContact
     */
    public void setVendorContact(String vendorContact) {
        this.vendorContact = vendorContact;
    }

    /**
     *
     * @return
     */
    public String getVendorName() {
        return vendorName;
    }

    /**
     *
     * @param vendorName
     */
    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    /**
     *
     * @return
     */
    public int getMaxUnknownWords() {
        return maxUnknownWords;
    }

    /**
     *
     * @param maxUnknownWords
     */
    public void setMaxUnknownWords(int maxUnknownWords) {
        this.maxUnknownWords = maxUnknownWords;
    }

    /**
     *
     * @return
     */
    public int getMaxFreeWords() {
        return maxFreeWords;
    }

    /**
     *
     * @param maxFreeWords
     */
    public void setMaxFreeWords(int maxFreeWords) {
        this.maxFreeWords = maxFreeWords;
    }

    /**
     *
     * @return
     */
    public int getMaxSuspiciousWords() {
        return maxSuspiciousWords;
    }

    /**
     *
     * @param maxSuspiciousWords
     */
    public void setMaxSuspiciousWords(int maxSuspiciousWords) {
        this.maxSuspiciousWords = maxSuspiciousWords;
    }

    /**
     *
     * @return
     */
    public int getMinWords() {
        return minWords;
    }

    /**
     *
     * @param minWords
     */
    public void setMinWords(int minWords) {
        this.minWords = minWords;
    }

    /**
     *
     * @return
     */
    public int getMaxWords() {
        return maxWords;
    }

    /**
     *
     * @param maxWords
     */
    public void setMaxWords(int maxWords) {
        this.maxWords = maxWords;
    }

    /**
     *
     * @return
     */
    public int getMinTokens() {
        return minTokens;
    }

    /**
     *
     * @param minTokens
     */
    public void setMinTokens(int minTokens) {
        this.minTokens = minTokens;
    }

    /**
     *
     * @return
     */
    public int getMaxTokens() {
        return maxTokens;
    }

    /**
     *
     * @param maxTokens
     */
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    /**
     *
     * @return
     */
    public int getMinNonStopwords() {
        return minNonStopwords;
    }

    /**
     *
     * @param minNonStopwords
     */
    public void setMinNonStopwords(int minNonStopwords) {
        this.minNonStopwords = minNonStopwords;
    }

    /**
     *
     * @return
     */
    public boolean isNonEnglishAllowed() {
        return isNonEnglishAllowed;
    }

    /**
     *
     * @param nonEnglishAllowed
     */
    public void setNonEnglishAllowed(boolean nonEnglishAllowed) {
        isNonEnglishAllowed = nonEnglishAllowed;
    }

    /**
     *
     * @return
     */
    public boolean isNotLatinCharsetAllowed() {
        return isNotLatinCharsetAllowed;
    }

    /**
     *
     * @param notLatinCharsetAllowed
     */
    public void setNotLatinCharsetAllowed(boolean notLatinCharsetAllowed) {
        isNotLatinCharsetAllowed = notLatinCharsetAllowed;
    }

    /**
     *
     * @return
     */
    public boolean isSwearWordsAllowed() {
        return isSwearWordsAllowed;
    }

    /**
     *
     * @param swearWordsAllowed
     */
    public void setSwearWordsAllowed(boolean swearWordsAllowed) {
        isSwearWordsAllowed = swearWordsAllowed;
    }

    /**
     *
     * @return
     */
    public boolean isNoNounsAllowed() {
        return isNoNounsAllowed;
    }

    /**
     *
     * @param noNounsAllowed
     */
    public void setNoNounsAllowed(boolean noNounsAllowed) {
        isNoNounsAllowed = noNounsAllowed;
    }

    /**
     *
     * @return
     */
    public boolean isNoUserTokensAllowed() {
        return isNoUserTokensAllowed;
    }

    /**
     *
     * @param noUserTokensAllowed
     */
    public void setNoUserTokensAllowed(boolean noUserTokensAllowed) {
        isNoUserTokensAllowed = noUserTokensAllowed;
    }

    /**
     *
     * @return
     */
    public int getJiggleFactor() {
        return jiggleFactor;
    }

    /**
     *
     * @param jiggleFactor
     */
    public void setJiggleFactor(int jiggleFactor) {
        this.jiggleFactor = jiggleFactor;
    }

    /**
     *
     * @return
     */
    public int getMinDateTokens() {
        return minDateTokens;
    }

    /**
     *
     * @param minDateTokens
     */
    public void setMinDateTokens(int minDateTokens) {
        this.minDateTokens = minDateTokens;
    }

    /**
     *
     * @return
     */
    public int getMaxDateTokens() {
        return maxDateTokens;
    }

    /**
     *
     * @param maxDateTokens
     */
    public void setMaxDateTokens(int maxDateTokens) {
        this.maxDateTokens = maxDateTokens;
    }

    /**
     *
     * @return
     */
    public int getMinNumTokens() {
        return minNumTokens;
    }

    /**
     *
     * @param minNumTokens
     */
    public void setMinNumTokens(int minNumTokens) {
        this.minNumTokens = minNumTokens;
    }

    /**
     *
     * @return
     */
    public int getMaxNumTokens() {
        return maxNumTokens;
    }

    /**
     *
     * @param maxNumTokens
     */
    public void setMaxNumTokens(int maxNumTokens) {
        this.maxNumTokens = maxNumTokens;
    }

    /**
     *
     * @return
     */
    public int getMinGeoTokens() {
        return minGeoTokens;
    }

    /**
     *
     * @param minGeoTokens
     */
    public void setMinGeoTokens(int minGeoTokens) {
        this.minGeoTokens = minGeoTokens;
    }

    /**
     *
     * @return
     */
    public int getMaxGeoTokens() {
        return maxGeoTokens;
    }

    /**
     *
     * @param maxGeoTokens
     */
    public void setMaxGeoTokens(int maxGeoTokens) {
        this.maxGeoTokens = maxGeoTokens;
    }

    /**
     *
     * @return
     */
    public int getMinFunctionTokens() {
        return minFunctionTokens;
    }

    /**
     *
     * @param minFunctionTokens
     */
    public void setMinFunctionTokens(int minFunctionTokens) {
        this.minFunctionTokens = minFunctionTokens;
    }

    /**
     *
     * @return
     */
    public int getMaxFunctionTokens() {
        return maxFunctionTokens;
    }

    /**
     *
     * @param maxFunctionTokens
     */
    public void setMaxFunctionTokens(int maxFunctionTokens) {
        this.maxFunctionTokens = maxFunctionTokens;
    }
    
    /**
     *
     * @return
     */
    public boolean isDupSynonymsAllowed() {
        return isDupSynonymsAllowed;
    }
    
    /**
     *
     * @param dupSynonymsAllowed
     */
    public void setDupSynonymsAllowed(boolean dupSynonymsAllowed) {
        isDupSynonymsAllowed = dupSynonymsAllowed;
    }
    
    /**
     *
     * @return
     */
    public int getMaxTotalSynonyms() {
        return maxTotalSynonyms;
    }
    
    /**
     *
     * @param maxTotalSynonyms
     */
    public void setMaxTotalSynonyms(int maxTotalSynonyms) {
        this.maxTotalSynonyms = maxTotalSynonyms;
    }

    /**
     * 
     * @param isPermutateSynonyms
     */
    public void setPermutateSynonyms(boolean isPermutateSynonyms) {
        this.isPermutateSynonyms = isPermutateSynonyms;
    }

    /**
     *
     * @return
     */
    public boolean isPermutateSynonyms() {
        return isPermutateSynonyms;
    }
}
