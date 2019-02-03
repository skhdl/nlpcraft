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

package org.nlpcraft.mdllib.tools.builder;

import org.nlpcraft.mdllib.*;
import org.nlpcraft.mdllib.tools.*;
import org.nlpcraft.mdllib.tools.builder.impl.*;
import org.nlpcraft.mdllib.tools.builder.json.*;
import org.nlpcraft.mdllib.tools.impl.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

/**
 * Convenient model builder.
 * <br><br>
 * To use this builder start by invoking one of the following static method to create new builder instance:
 * <ul>
 *     <li>{@link #newJsonModel(InputStream)}</li>
 *     <li>{@link #newJsonModel(String)}</li>
 *     <li>{@link #newModel(String, String, String)}</li>
 *     <li>{@link #newModel()}</li>
 *     <li>{@link #newJsonStringModel(String)}</li>
 * </ul>
 * Once you have the builder instance you can set all necessary properties and finally call {@link #build()}
 * method to get properly constructed {@link NCModel} instance. Note that at the minimum the
 * {@link #setDescriptor(NCModelDescriptor) descriptor} and
 * the {@link #setQueryFunction(NCSerializableFunction) query function}
 * must be set.
 */
public class NCModelBuilder extends NCJsonBuilder {
    /** */
    private NCModelImpl impl;
    
    /**
     *
     */
    private NCModelBuilder() {
        impl = new NCModelImpl();
    }

    /**
     * Utility method for locating resource on a classpath.
     *
     * @param fileName Resource's file name on the classpath.
     * @return Full path of the resource.
     */
    public static String classPathFile(String fileName) {
        URL res = NCModelBuilder.class.getClassLoader().getResource(fileName);

        if (res == null)
            throw new IllegalArgumentException("Classpath resource not found: " + fileName);

        return res.getFile();
    }

    /**
     * Creates new model builder.
     * 
     * @return New model builder.
     */
    public static NCModelBuilder newModel() {
        return new NCModelBuilder();
    }

    /**
     * Creates new model builder with given parameters.
     * 
     * @param id Unique, <i>immutable</i> ID of the model.
     * @param name Descriptive name of this model.
     * @param ver Version of this model using semantic versioning compatible
     *      with (<a href="http://www.semver.org">www.semver.org</a>) specification.
     * @return New model builder.
     */
    public static NCModelBuilder newModel(String id, String name, String ver) {
        NCModelBuilder bldr = new NCModelBuilder();

        bldr.setDescriptor(
            NCModelDescriptorBuilder.newDescriptor(id, name, ver).build()
        );

        return bldr;
    }

    /**
     * Creates new model builder and loads model definition from JSON file.
     * Look at {@code model_template.json} file in the examples {@code resources} for
     * the JSON model template.
     * 
     * @param filePath JSON file path to load from.
     * @return New model builder.
     */
    public static NCModelBuilder newJsonModel(String filePath) {
        NCModelBuilder bldr = new NCModelBuilder();

        bldr.ingestJsonModel(readFile(filePath, NCModelJson.class));

        return bldr;
    }
    
    /**
     * Creates new model builder and loads JSON model definition from input stream.
     * Look at {@code model_template.json} file in the examples {@code resources} for
     * the JSON model template.
     *
     * @param in Input stream to load JSON model from.
     * @return New model builder.
     */
    public static NCModelBuilder newJsonModel(InputStream in) {
        NCModelBuilder bldr = new NCModelBuilder();
        
        bldr.ingestJsonModel(readFile(in, NCModelJson.class));
        
        return bldr;
    }

    /**
     * Creates new model builder and loads JSON model definition from given JSON string.
     * Look at {@code model_template.json} file in the examples {@code resources} for
     * the JSON model template.
     * 
     * @param jsonStr JSON string to load model from.
     * @return New model builder.
     */
    public static NCModelBuilder newJsonStringModel(String jsonStr) {
        NCModelBuilder bldr = new NCModelBuilder();

        bldr.ingestJsonModel(readString(jsonStr, NCModelJson.class));

        return bldr;
    }

    /**
     * Returns newly built model. Note that at the minimum the
     * {@link #setDescriptor(NCModelDescriptor) descriptor} and
     * the {@link #setQueryFunction(NCSerializableFunction) query function}
     * must be set.
     *
     * @return New built model.
     * @throws NCBuilderException Thrown in case of any errors building the model.
     */
    public NCModel build() throws NCBuilderException {
        if (impl.getQueryFunction() == null)
            throw new NCBuilderException("Query function is not.");

        if (impl.getDescriptor() == null)
            throw new NCBuilderException("Model descriptor is not set.");

        return impl;
    }

    /**
     * Sets model descriptor.
     *
     * @param ds Model descriptor to set.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setDescriptor(NCModelDescriptor ds) {
        impl.setDescriptor(ds);

        return this;
    }

    /**
     * Sets model descriptor.
     *
     * @param desc Model description to set.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setDescription(String desc) {
        impl.setDescription(desc);

        return this;
    }

    /**
     * Adds model metadata. See {@link NCModel#getMetadata()} for more information.
     *
     * @param name Metadata property name.
     * @param val Metadata property value.
     * @return This builder for chaining operations.
     * @see NCModel#getMetadata()
     */
    public NCModelBuilder addUserMetadata(String name, Serializable val) {
        impl.addMetadata(name, val);

        return this;
    }

    /**
     * Adds model element. See {@link NCElement} for more information.
     *
     * @param elm Model element to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addElement(NCElement elm) {
        impl.addElement(elm);

        return this;
    }

    /**
     * Adds macro. See {@link NCElement} for more macro information.
     *
     * @param name Macro name.
     * @param val Macro value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addMacro(String name, String val) {
        impl.addMacro(name, val);

        return this;
    }

    /**
     *
     * @param js Model JSON.
     *
     * @throws NCBuilderException Thrown in case of any errors.
     */
    private void ingestJsonModel(NCModelJson js) throws NCBuilderException {
        impl.setDescriptor(
            NCModelDescriptorBuilder.newDescriptor(
                js.getId(),
                js.getName(),
                js.getVersion()
            ).build()
        );
    
        impl.setDescription(js.getDescription());
        impl.setDocsUrl(js.getDocsUrl());
        impl.setVendorUrl(js.getVendorUrl());
        impl.setVendorEmail(js.getVendorEmail());
        impl.setVendorContact(js.getVendorContact());
        impl.setVendorName(js.getVendorName());
        impl.setMaxUnknownWords(js.getMaxUnknownWords());
        impl.setMaxFreeWords(js.getMaxFreeWords());
        impl.setMaxSuspiciousWords(js.getMaxSuspiciousWords());
        impl.setMinWords(js.getMinWords());
        impl.setMaxWords(js.getMaxWords());
        impl.setMinTokens(js.getMinTokens());
        impl.setMaxTokens(js.getMaxTokens());
        impl.setMinNonStopwords(js.getMinNonStopwords());
        impl.setNonEnglishAllowed(js.isNonEnglishAllowed());
        impl.setNotLatinCharsetAllowed(js.isNotLatinCharsetAllowed());
        impl.setSwearWordsAllowed(js.isSwearWordsAllowed());
        impl.setNoNounsAllowed(js.isNoNounsAllowed());
        impl.setNoUserTokensAllowed(js.isNoUserTokensAllowed());
        impl.setJiggleFactor(js.getJiggleFactor());
        impl.setMinDateTokens(js.getMinDateTokens());
        impl.setMaxDateTokens(js.getMaxDateTokens());
        impl.setMinNumTokens(js.getMinNumTokens());
        impl.setMaxNumTokens(js.getMaxNumTokens());
        impl.setMinGeoTokens(js.getMinGeoTokens());
        impl.setMaxGeoTokens(js.getMaxGeoTokens());
        impl.setMinFunctionTokens(js.getMinFunctionTokens());
        impl.setMaxFunctionTokens(js.getMaxFunctionTokens());
        impl.setDupSynonymsAllowed(js.isDupSynonymsAllowed());
        impl.setMaxTotalSynonyms(js.getMaxTotalSynonyms());
        impl.setPermutateSynonyms(js.isPermutateSynonyms());
    
        if (js.getAdditionalStopwords() != null)
            addAdditionalStopWords(js.getAdditionalStopwords());

        if (js.getExcludedStopwords() != null)
            addExcludedStopWords(js.getExcludedStopwords());

        if (js.getSuspiciousWords() != null)
            addSuspiciousWords(js.getSuspiciousWords());

        if (js.getExamples() != null)
            addExamples(js.getExamples());

        if (js.getMacros() != null)
            for (NCMacroJson m : js.getMacros())
                addMacro(m.getName(), m.getMacro());

        if (js.getUserMetadata() != null)
            for (Map.Entry<String, Object> entry : js.getUserMetadata().entrySet())
                addUserMetadata(entry.getKey(), (Serializable)entry.getValue());

        for (NCElementJson e : js.getElements()) {
            NCMetadata elmMeta = new NCMetadataImpl();

            if (e.getMetadata() != null)
                for (Map.Entry<String, Object> entry : e.getMetadata().entrySet())
                    elmMeta.put(entry.getKey(), (Serializable)entry.getValue());

            addElement(new NCElement() {
                private final List<String> syns =
                    e.getSynonyms() == null ? Collections.emptyList() : Arrays.asList(e.getSynonyms());
                private final List<String> exclSyns =
                    e.getExcludedSynonyms() == null ? Collections.emptyList() : Arrays.asList(e.getExcludedSynonyms());
                private final List<NCValue> values =
                    e.getValues() == null ?
                        Collections.emptyList() :
                        Arrays.stream(e.getValues()).map(
                            p -> new NCValueImpl(
                                p.getName(),
                                p.getSynonyms() == null ? Collections.emptyList() : Arrays.asList(p.getSynonyms())
                            )
                        ).collect(Collectors.toList());
                
                @Override
                public List<NCValue> getValues() {
                    return values;
                }

                @Override
                public String getParentId() {
                    return e.getParentId();
                }

                @Override
                public String getDescription() {
                    return e.getDescription();
                }

                @Override
                public String getId() {
                    return e.getId();
                }

                @Override
                public String getGroup() {
                    return e.getGroup();
                }

                @Override
                public NCMetadata getMetadata() {
                    return elmMeta;
                }

                @Override
                public List<String> getSynonyms() {
                    return syns;
                }

                @Override
                public List<String> getExcludedSynonyms() {
                    return exclSyns;
                }
            });
        }
    }

    /**
     * Adds additional stopword. See {@link NCModel#getAdditionalStopWords()} for more information.
     *
     * @param words Additional stopwords to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addAdditionalStopWords(String... words) {
        return addAdditionalStopWords(Arrays.asList(words));
    }

    /**
     * Adds additional stopword. See {@link NCModel#getAdditionalStopWords()} for more information.
     *
     * @param words Additional stopwords to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addAdditionalStopWords(Collection<String> words) {
        assert words != null;

        for (String word : words)
            impl.addAdditionalStopWord(word);

        return this;
    }

    /**
     * Adds suspicious stopword. See {@link NCModel#getSuspiciousWords()} for more information.
     *
     * @param words Suspicious words to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addSuspiciousWords(String... words) {
        return addSuspiciousWords(Arrays.asList(words));
    }

    /**
     * Adds suspicious stopword. See {@link NCModel#getSuspiciousWords()} for more information.
     *
     * @param words Suspicious words to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addSuspiciousWords(Collection<String> words) {
        assert words != null;

        for (String word : words)
            impl.addSuspiciousWord(word);

        return this;
    }

    /**
     * Adds examples to the model. See {@link NCModel#getExamples()} for more information.
     *
     * @param examples Examples to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addExamples(String... examples) {
        return addExamples(Arrays.asList(examples));
    }

    /**
     * Adds examples to the model. See {@link NCModel#getExamples()} for more information.
     *
     * @param examples Examples to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addExamples(Collection<String> examples) {
        assert examples != null;

        for (String example : examples)
            impl.addExample (example);

        return this;
    }

    /**
     * Adds excluding stopwords. See {@link NCModel#getExcludedStopWords()} for more information.
     *
     * @param words Excluding stopwords to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addExcludedStopWords(String... words) {
        return addExcludedStopWords(Arrays.asList(words));
    }

    /**
     * Adds excluding stopwords. See {@link NCModel#getExcludedStopWords()} for more information.
     *
     * @param words Excluding stopwords to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addExcludedStopWords(Collection<String> words) {
        assert words != null;

        for (String word : words)
            impl.addExcludedStopWord(word);

        return this;
    }

    /**
     * Sets query function. See {@link NCModel#query(NCQueryContext)} for more information.
     *
     * @param qryFun Query function to set.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setQueryFunction(NCSerializableFunction<NCQueryContext, NCQueryResult> qryFun) {
        impl.setQueryFunction(qryFun);

        return this;
    }

    /**
     * Sets model's discard function. See {@link NCModel#discard()} for more information.
     * 
     * @param discardFun Model's discard function to set.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setDiscardFunction(NCSerializableRunnable discardFun) {
        impl.setDiscardFunction(discardFun);

        return this;
    }

    /**
     * Sets model's initialize function. See {@link NCModel#initialize(NCProbeContext)} ()} for more information.
     *
     * @param initFun Model's initialize function to set.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setInitFunction(NCSerializableConsumer<NCProbeContext> initFun) {
        impl.setInitFunction(initFun);

        return this;
    }

    /**
     * Sets {@link NCModel#getDocsUrl()} configuration value.
     *
     * @param docsUrl {@link NCModel#getDocsUrl()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setDocsUrl(String docsUrl) {
        impl.setDocsUrl(docsUrl);
        
        return this;
    }

    /**
     * Sets {@link NCModel#getVendorUrl()} configuration value.
     *
     * @param vendorUrl {@link NCModel#getVendorUrl()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setVendorUrl(String vendorUrl) {
        impl.setVendorUrl(vendorUrl);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getVendorEmail()} configuration value.
     *
     * @param vendorEmail {@link NCModel#getVendorEmail()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setVendorEmail(String vendorEmail) {
        impl.setVendorEmail(vendorEmail);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getVendorContact()} configuration value.
     *
     * @param vendorContact {@link NCModel#getVendorContact()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setVendorContact(String vendorContact) {
        impl.setVendorContact(vendorContact);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getVendorName()} configuration value.
     *
     * @param vendorName {@link NCModel#getVendorName()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setVendorName(String vendorName) {
        impl.setVendorName(vendorName);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getJiggleFactor()} configuration value.
     *
     * @param jiggleFactor {@link NCModel#getJiggleFactor()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setJiggleFactor(int jiggleFactor) {
        impl.setJiggleFactor(jiggleFactor);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinDateTokens()} configuration value.
     *
     * @param minDateTokens {@link NCModel#getMinDateTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinDateTokens(int minDateTokens) {
        impl.setMinDateTokens(minDateTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxDateTokens()} configuration value.
     *
     * @param maxDateTokens {@link NCModel#getMaxDateTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxDateTokens(int maxDateTokens) {
        impl.setMaxDateTokens(maxDateTokens);
    
        return this;
    }
    
    /**
     * Sets {@link NCModel#getMinNumTokens()} configuration value.
     *
     * @param minNumTokens {@link NCModel#getMinNumTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinNumTokens(int minNumTokens) {
        impl.setMinNumTokens(minNumTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxNumTokens()} configuration value.
     *
     * @param maxNumTokens {@link NCModel#getMaxNumTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxNumTokens(int maxNumTokens) {
        impl.setMinNumTokens(maxNumTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinGeoTokens()} configuration value.
     *
     * @param minGeoTokens {@link NCModel#getMinGeoTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinGeoTokens(int minGeoTokens) {
        impl.setMinGeoTokens(minGeoTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxGeoTokens()} configuration value.
     *
     * @param maxGeoTokens {@link NCModel#getMaxGeoTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxGeoTokens(int maxGeoTokens) {
        impl.setMaxGeoTokens(maxGeoTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinFunctionTokens()} configuration value.
     *
     * @param minFunctionTokens {@link NCModel#getMinFunctionTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinFunctionTokens(int minFunctionTokens) {
        impl.setMinFunctionTokens(minFunctionTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxFunctionTokens()} configuration value.
     *
     * @param maxFunctionTokens {@link NCModel#getMaxFunctionTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxFunctionTokens(int maxFunctionTokens) {
        impl.setMaxFunctionTokens(maxFunctionTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxUnknownWords()} configuration value.
     *
     * @param maxUnknownWords {@link NCModel#getMaxUnknownWords()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxUnknownWords(int maxUnknownWords) {
        impl.setMaxUnknownWords(maxUnknownWords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxFreeWords()} configuration value.
     *
     * @param maxFreeWords {@link NCModel#getMaxFreeWords()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxFreeWords(int maxFreeWords) {
        impl.setMaxFreeWords(maxFreeWords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxSuspiciousWords()} configuration value.
     *
     * @param maxSuspiciousWords {@link NCModel#getMaxSuspiciousWords()}  configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxSuspiciousWords(int maxSuspiciousWords) {
        impl.setMaxSuspiciousWords(maxSuspiciousWords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinWords()} configuration value.
     *
     * @param minWords {@link NCModel#getMinWords()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinWords(int minWords) {
        impl.setMinWords(minWords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxWords()} configuration value.
     *
     * @param maxWords {@link NCModel#getMaxWords()}  configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxWords(int maxWords) {
        impl.setMaxWords(maxWords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinTokens()} configuration value.
     *
     * @param minTokens {@link NCModel#getMinTokens()}  configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinTokens(int minTokens) {
        impl.setMinTokens(minTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxTokens()} configuration value.
     *
     * @param maxTokens {@link NCModel#getMaxTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxTokens(int maxTokens) {
        impl.setMaxTokens(maxTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinNonStopwords()} configuration value.
     *
     * @param minNonStopwords {@link NCModel#getMinNonStopwords()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinNonStopwords(int minNonStopwords) {
        impl.setMinNonStopwords(minNonStopwords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#isNonEnglishAllowed()} configuration value.
     *
     * @param nonEnglishAllowed {@link NCModel#isNonEnglishAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setNonEnglishAllowed(boolean nonEnglishAllowed) {
        impl.setNonEnglishAllowed(nonEnglishAllowed);
    
        return this;
    }

    /**
     * Sets {@link NCModel#isNotLatinCharsetAllowed()} configuration value.
     *
     * @param notLatinCharsetAllowed {@link NCModel#isNotLatinCharsetAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setNotLatinCharsetAllowed(boolean notLatinCharsetAllowed) {
        impl.setNotLatinCharsetAllowed(notLatinCharsetAllowed);
    
        return this;
    }

    /**
     * Sets {@link NCModel#isSwearWordsAllowed()} configuration value.
     *
     * @param swearWordsAllowed {@link NCModel#isSwearWordsAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setSwearWordsAllowed(boolean swearWordsAllowed) {
        impl.setSwearWordsAllowed(swearWordsAllowed);
    
        return this;
    }

    /**
     * Sets {@link NCModel#isNoNounsAllowed()} configuration value.
     *
     * @param noNounsAllowed {@link NCModel#isNoNounsAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setNoNounsAllowed(boolean noNounsAllowed) {
        impl.setNoNounsAllowed(noNounsAllowed);
    
        return this;
    }

    /**
     * Sets {@link NCModel#isNoUserTokensAllowed()} configuration value.
     *
     * @param noUserTokensAllowed {@link NCModel#isNoUserTokensAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setNoUserTokensAllowed(boolean noUserTokensAllowed) {
        impl.setNoUserTokensAllowed(noUserTokensAllowed);
        
        return this;
    }
    
    /**
     * Sets {@link NCModel#isDupSynonymsAllowed()} configuration value.
     *
     * @param isDupSynonymsAllowed {@link NCModel#isDupSynonymsAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setDupSynonymsAllowed(boolean isDupSynonymsAllowed) {
        impl.setDupSynonymsAllowed(isDupSynonymsAllowed);
        
        return this;
    }
    
    /**
     * Sets {@link NCModel#getMaxTotalSynonyms()} configuration value.
     *
     * @param maxTotalSynonyms {@link NCModel#getMaxTotalSynonyms()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxTotalSynonyms(int maxTotalSynonyms) {
        impl.setMaxTotalSynonyms(maxTotalSynonyms);
        
        return this;
    }
    
    /**
     * Sets {@link NCModel#isPermutateSynonyms()} configuration value.
     *
     * @param permutateSynonyms {@link NCModel#isPermutateSynonyms()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setPermutateSynonyms(boolean permutateSynonyms) {
        impl.setPermutateSynonyms(permutateSynonyms);
        
        return this;
    }
}
