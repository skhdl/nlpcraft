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

package org.nlpcraft.model;

import org.nlpcraft.model.intent.NCIntentSolver;
import org.nlpcraft.model.builder.NCElementBuilder;
import org.nlpcraft.model.builder.NCModelBuilder;
import org.nlpcraft.model.builder.NCModelDescriptorBuilder;
import org.nlpcraft.model.impl.NCMetadataImpl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * <b>Main interface</b> for user-defined semantic data model.
 * <br><br>
 * A model generally defines:
 * <ul>
 *     <li>How to interpret the input from the user.</li>
 *     <li>How to map it to the data source.</li>
 *     <li>How to format the data source result back to the user.</li>
 * </ul>
 * At a <b>minimum</b> only two methods need to be implemented on this interface:
 * <ul>
 *     <li>{@link #getDescriptor()} - model identification.</li>
 *     <li>{@link #query(NCQueryContext)} - the main method that user implements to provide result.</li>
 * </ul>
 * All other methods have reasonable defaults. In most cases, however, method {@link #getElements()}
 * should provide at least one user-defined element.
 * <br><br>
 * Note that while user can implement this interface directly it is highly recommended to use
 * convenient {@link NCModelBuilder} factory class. You can use {@link NCModelBuilder} programmatically
 * and set all properties manually or you can supply it a JSON presentation of the model to load.
 * <br><br>
 * If you use {@link NCModelBuilder} programmatically you should also use the following auxiliary builders:
 * <ul>
 *      <li>{@link NCElementBuilder} for model element building, and</li>
 *      <li>{@link NCModelDescriptorBuilder} for model descriptor building.</li>
 * </ul>
 * Here's Java example of the absolutely minimal model built with model builder (from Hello World example):
 * <pre class="brush: java">
 * NCModelBuilder.newModel("nlpcraft.helloworld.ex", "HelloWorld Example Model", "1.0")
 *    // Return HTML result.
 *    .setQueryFunction(ctx -&gt; NCQueryResult.html("Hello World!")
 * )
 * .build()
 * </pre>
 * For external JSON definition here's the JSON template. Note that only properties marked with
 * <code>--==MANDATORY==--</code> comment are mandatory and JSON supports C-style comments. All JSON properties
 * correspond to their counterparts in this interface:
 * <pre class="brush: js">
 * {
 *      "id": "user.defined.id", // --==MANDATORY==--
 *      "name": "User Defined Name", // --==MANDATORY==--
 *      "version": "1.0", // --==MANDATORY==--
 *      "description": "Short model description.", --==MANDATORY==--
 *      "vendorName": "ACME Inc.",
 *      "vendorUrl": "https://www.acme.com",
 *      "vendorContact": "Support",
 *      "vendorEmail": "info@acme.com",
 *      "docsUrl": "https://www.acme.com/docs",
 *      "maxGeoTokens": 1
 *      "examples": [],
 *      "macros": [],
 *      "metadata": {
 *          "myConfig": "myProperty"
 *      },
 *      "elements": [
 *          {
 *              "id": "x:id", // --==MANDATORY==--
 *              "group": "default",
 *              "parentId": null,
 *              "excludedSynonyms": [],
 *              "synonyms": [],
 *              "relations": {},
 *              "metadata": {},
 *              "values": []
 *          }
 *      ],
 *      "additionalStopwords": [],
 *      "excludedStopwords": [],
 *      "suspiciousWords": []
 *}
 * </pre>
 *
 * @see NCModelProvider
 */
public interface NCModel {
    /**
     * Default value returned from {@link #getJiggleFactor()} method.
     */
    int DFLT_JIGGLE_FACTOR = 2;

    /**
     * Default value returned from {@link #getMinDateTokens()} method.
     */
    int DFLT_MIN_DATE_TOKENS = 0;

    /**
     * Default value returned from {@link #getMaxDateTokens()} method.
     */
    int DFLT_MAX_DATE_TOKENS = Integer.MAX_VALUE;

    /**
     * Default value returned from {@link #getMinNumTokens()} method.
     */
    int DFLT_MIN_NUM_TOKENS = 0;

    /**
     * Default value returned from {@link #getMaxNumTokens()} method.
     */
    int DFLT_MAX_NUM_TOKENS = Integer.MAX_VALUE;

    /**
     * Default value returned from {@link #getMinGeoTokens()} method.
     */
    int DFLT_MIN_GEO_TOKENS = 0;

    /**
     * Default value returned from {@link #getMaxGeoTokens()} method.
     */
    int DFLT_MAX_GEO_TOKENS = Integer.MAX_VALUE;

    /**
     * Default value returned from {@link #getMinFunctionTokens()} method.
     */
    int DFLT_MIN_FUNCTION_TOKENS = 0;

    /**
     * Default value returned from {@link #getMaxFunctionTokens()} method.
     */
    int DFLT_MAX_FUNCTION_TOKENS = Integer.MAX_VALUE;

    /**
     * Default value returned from {@link #getMaxUnknownWords()} method.
     */
    int DFLT_MAX_UNKNOWN_WORDS = Integer.MAX_VALUE;

    /**
     * Default value returned from {@link #getMaxFreeWords()} method.
     */
    int DFLT_MAX_FREE_WORDS = Integer.MAX_VALUE;

    /**
     * Default value returned from {@link #getMaxSuspiciousWords()} method.
     */
    int DFLT_MAX_SUSPICIOUS_WORDS = 0;

    /**
     * Default value returned from {@link #getMinWords()} method.
     */
    int DFLT_MIN_WORDS = 1;

    /**
     * Default value returned from {@link #getMaxWords()} method.
     */
    int DFLT_MAX_WORDS = 50; 

    /**
     * Default value returned from {@link #getMinTokens()} method.
     */
    int DFLT_MIN_TOKENS = 0;

    /**
     * Default value returned from {@link #getMaxTokens()} method.
     */
    int DFLT_MAX_TOKENS = 50;

    /**
     * Default value returned from {@link #getMinNonStopwords()} method.
     */
    int DFLT_MIN_NON_STOPWORDS = 0;

    /**
     * Default value returned from {@link #isNonEnglishAllowed()}  method.
     */
    boolean DFLT_IS_NON_ENGLISH_ALLOWED = true;

    /**
     * Default value returned from {@link #isNotLatinCharsetAllowed()} method.
     */
    boolean DFLT_IS_NOT_LATIN_CHARSET_ALLOWED = false;

    /**
     * Default value returned from {@link #isSwearWordsAllowed()}  method.
     */
    boolean DFLT_IS_SWEAR_WORDS_ALLOWED = false;

    /**
     * Default value returned from {@link #isNoNounsAllowed()}  method.
     */
    boolean DFLT_IS_NO_NOUNS_ALLOWED = true;

    /**
     * Default value returned from {@link #isPermutateSynonyms()}  method.
     */
    boolean DFLT_IS_PERMUTATE_SYNONYMS = true;

    /**
     * Default value returned from {@link #isDupSynonymsAllowed()}  method.
     */
    boolean DFLT_IS_DUP_SYNONYMS_ALLOWED = true;

    /**
     * Default value returned from {@link #getMaxTotalSynonyms()}  method.
     */
    int DFLT_MAX_TOTAL_SYNONYMS = Integer.MAX_VALUE;

    /**
     * Default value returned from {@link #isNoUserTokensAllowed()}  method.
     */
    boolean DFLT_IS_NO_USER_TOKENS_ALLOWED = true;

    /**
     * Gets descriptor for this model.
     * <br><br>
     * Descriptor provides model identification including its unique, immutable ID,
     * user-friendly name and a version. Model ID is not exposed to the end user and
     * used only for internal purposes. Note that model ID <b>must</b> be immutable - changing
     * model ID is equal to creating a new model. Model name and version are both
     * exposed to the end user and can be changed. Note that model version should be
     * compatible with (<a href="http://www.semver.org">www.semver.org</a>) specification.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>id</code>, <code>name</code> and
     * <code>version</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "id": "user.defined.id",
     *      "name": "User Defined Name",
     *      "version": "1.0",
     * }
     * </pre>
     *
     * @return This model descriptor.
     */
    NCModelDescriptor getDescriptor();

    /**
     * Gets optional short model description. This can be displayed by the management tools.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>description</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "description": "Model description..."
     * }
     * </pre>
     *
     * @return Optional short model description.
     */
    default String getDescription() {
        return null;
    }

    /**
     * Gets optional URL to model documentation. This can be used by the management tools.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>docsUrl</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "docsUrl": "https://www.nlpcraft.org/docs"
     * }
     * </pre>
     *
     * @return Optional URL to model documentation.
     */
    default String getDocsUrl() {
        return null;
    }

    /**
     * Gets optional URL of model vendor. This can be used by the management tools.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>vendorUrl</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "vendorUrl": "https://www.nlpcraft.org"
     * }
     * </pre>
     *
     * @return Optional URL of model vendor.
     */
    default String getVendorUrl() {
        return null;
    }

    /**
     * Gets optional email of model vendor. This can be used by the management tools.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>vendorEmail</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "vendorEmail": "info@nlpcraft.org"
     * }
     * </pre>
     *
     * @return Optional email of model vendor.
     */
    default String getVendorEmail() {
        return null;
    }

    /**
     * Gets optional contact name of model vendor. This can be used by the management tools.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>vendorContact</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "vendorContact": "John Doer"
     * }
     * </pre>
     *
     * @return Optional contact name of model vendor.
     */
    default String getVendorContact() {
        return null;
    }

    /**
     * Gets optional name of model vendor. This can be used by the management tools.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>vendorName</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "vendorName": "NLPCraft"
     * }
     * </pre>
     *
     * @return Optional name of model vendor.
     */
    default String getVendorName() {
        return null;
    }

    /**
     * Gets maximum number of unknown words until automatic rejection. An unknown word is a word
     * that is not part of Princeton WordNet database. If you expect a very formalized and well
     * defined input without uncommon slang and abbreviations you can set this to a small number
     * like one or two. However, in most cases we recommend to leave it as default or set it to a larger
     * number like five or more.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MAX_UNKNOWN_WORDS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>maxUnknownWords</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "maxUnknownWords": 2
     * }
     * </pre>
     *
     * @return Maximum number of unknown words until automatic rejection.
     */
    default int getMaxUnknownWords() {
        return DFLT_MAX_UNKNOWN_WORDS;
    }

    /**
     * Gets maximum number of free words until automatic rejection. A free word is a known word that is
     * not part of any recognized token. In other words, a word that is present in the user input
     * but won't be used to understand its meaning. Setting it to a non-zero risks the misunderstanding
     * of the user input, while setting it to zero often makes understanding logic too rigid. In most
     * cases we recommend setting to between one and three. If you expect the user input to contain
     * many <i>noisy</i> idioms, slang or colloquials - you can set it to a larger number.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MAX_FREE_WORDS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>maxFreeWords</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "maxFreeWords": 2
     * }
     * </pre>
     *
     * @return Maximum number of free words until automatic rejection.
     */
    default int getMaxFreeWords() {
        return DFLT_MAX_FREE_WORDS;
    }

    /**
     * Gets maximum number of suspicious words until automatic rejection. A suspicious word is a word
     * that is defined by the model that should not appear in a valid user input under no circumstances.
     * A typical example of suspicious words would be words "sex" or "porn" when processing
     * queries about children books. In most cases this should be set to zero (default) to automatically
     * reject any such suspicious words in the user input.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MAX_SUSPICIOUS_WORDS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>maxSuspiciousWords</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "maxSuspiciousWords": 2
     * }
     * </pre>
     *
     * @return Maximum number of suspicious words until automatic rejection.
     */
    default int getMaxSuspiciousWords() {
        return DFLT_MAX_SUSPICIOUS_WORDS;
    }

    /**
     * Gets minimum word count (<i>including</i> stopwords) below which user input will be automatically
     * rejected as too short. In almost all cases this value should be greater than or equal to one.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MIN_WORDS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>minWords</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "minWords": 2
     * }
     * </pre>
     *
     * @return Minimum word count (<i>including</i> stopwords) below which user input will be automatically
     *      rejected as too short.
     */
    default int getMinWords() {
        return DFLT_MIN_WORDS;
    }

    /**
     * Gets maximum word count (<i>including</i> stopwords) above which user input will be automatically
     * rejected as too long. In almost all cases this value should be greater than or equal to one.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MAX_WORDS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>maxWords</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "maxWords": 50
     * }
     * </pre>
     *
     * @return Maximum word count (<i>including</i> stopwords) above which user input will be automatically
     *      rejected as too long.
     */
    default int getMaxWords() {
        return DFLT_MAX_WORDS;
    }

    /**
     * Gets minimum number of all tokens (system and user defined) below which user input will be
     * automatically rejected as too short. In almost all cases this value should be greater than or equal to one.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MIN_TOKENS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>minTokens</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "minTokens": 1
     * }
     * </pre>
     *
     * @return Minimum number of all tokens.
     */
    default int getMinTokens() {
        return DFLT_MIN_TOKENS;
    }

    /**
     * Gets maximum number of all tokens (system and user defined) above which user input will be
     * automatically rejected as too long. Note that sentences with large number of token can result
     * in significant processing delay and substantial memory consumption.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MAX_TOKENS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>maxTokens</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "maxTokens": 100
     * }
     * </pre>
     *
     * @return Maximum number of all tokens.
     */
    default int getMaxTokens() {
        return DFLT_MAX_TOKENS;
    }

    /**
     * Gets minimum word count (<i>excluding</i> stopwords) below which user input will be automatically rejected
     * as ambiguous sentence.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MIN_NON_STOPWORDS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>minNonStopwords</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "minNonStopwords": 2
     * }
     * </pre>
     *
     * @return Minimum word count (<i>excluding</i> stopwords) below which user input will be automatically
     *      rejected as too short.
     */
    default int getMinNonStopwords() {
        return DFLT_MIN_NON_STOPWORDS;
    }

    /**
     * Whether or not to allow non-English language in user input.
     * Currently, only English language is supported. However, model can choose whether or not
     * to automatically reject user input that is detected to be a non-English. Note that current
     * algorithm only works reliably on longer user input (10+ words). On short sentences it will
     * often produce an incorrect result.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_IS_NON_ENGLISH_ALLOWED} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>nonEnglishAllowed</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "nonEnglishAllowed": false
     * }
     * </pre>
     *
     * @return Whether or not to allow non-English language in user input.
     */
    default boolean isNonEnglishAllowed() {
        return DFLT_IS_NON_ENGLISH_ALLOWED;
    }

    /**
     * Whether or not to allow non-Latin charset in user input. Currently, only
     * Latin charset is supported. However, model can choose whether or not to automatically reject user
     * input with characters outside of Latin charset. If {@code false} such user input will be automatically
     * rejected.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_IS_NOT_LATIN_CHARSET_ALLOWED} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>nonLatinCharsetAllowed</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "nonLatinCharsetAllowed": false
     * }
     * </pre>
     *
     * @return Whether or not to allow non-Latin charset in user input.
     */
    default boolean isNotLatinCharsetAllowed() {
        return DFLT_IS_NOT_LATIN_CHARSET_ALLOWED;
    }

    /**
     * Whether or not to allow known English swear words in user input. If {@code false} - user input with
     * detected known English swear words will be automatically rejected.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_IS_SWEAR_WORDS_ALLOWED} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>swearWordsAllowed</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "swearWordsAllowed": false
     * }
     * </pre>
     *
     * @return Whether or not to allow known swear words in user input.
     */
    default boolean isSwearWordsAllowed() {
        return DFLT_IS_SWEAR_WORDS_ALLOWED;
    }

    /**
     * Whether or not to allow user input without a single noun. If {@code false} such user input
     * will be automatically rejected. Typically for command or query-oriented models this should be set to
     * {@code false} as any command or query should have at least one noun subject. However, for conversational
     * models this can be set to {@code false} to allow for a smalltalk and one-liners.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_IS_NO_NOUNS_ALLOWED} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>noNounsAllowed</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "noNounsAllowed": false
     * }
     * </pre>
     *
     * @return Whether or not to allow user input without a single noun.
     */
    default boolean isNoNounsAllowed() {
        return DFLT_IS_NO_NOUNS_ALLOWED;
    }

    /**
     * Whether or not to permutate multi-word synonyms. Automatic multi-word synonyms permutations greatly
     * increase the total number of synonyms in the system and allows for better multi-word synonym detection.
     * For example, if permutation is allowed the synonym "a b c" will be automatically converted into a
     * sequence of synonyms of "a b c", "b a c", "a c b".
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_IS_PERMUTATE_SYNONYMS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>permutateSynonyms</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "permutateSynonyms": true
     * }
     * </pre>
     *
     * @return Whether or not to permutate multi-word synonyms.
     */
    default boolean isPermutateSynonyms() {
        return DFLT_IS_PERMUTATE_SYNONYMS;
    }

    /**
     * Whether or not duplicate synonyms are allowed. If {@code true} - the model will pick the random
     * model element when multiple elements found due to duplicate synonyms. If {@code false} - model
     * will print error message and will not deploy.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_IS_DUP_SYNONYMS_ALLOWED} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>dupSynonymsAllowed</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "dupSynonymsAllowed": true
     * }
     * </pre>
     *
     * @return Whether or not to allow duplicate synonyms.
     */
    default boolean isDupSynonymsAllowed() {
        return DFLT_IS_DUP_SYNONYMS_ALLOWED;
    }

    /**
     * Total number of synonyms allowed per model. Model won't deploy if total number of synonyms exceeds this
     * number.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MAX_TOTAL_SYNONYMS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>maxTotalSynonyms</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "maxTotalSynonyms": true
     * }
     * </pre>
     *
     * @return Total number of synonyms allowed per model.
     */
    default int getMaxTotalSynonyms() {
        return DFLT_MAX_TOTAL_SYNONYMS;
    }

    /**
     * Whether or not to allow the user input with no user token detected. If {@code false} such user
     * input will be automatically rejected. Note that this property only applies to user-defined
     * token (i.e. model element). Even if there are no user defined tokens, the user input may still
     * contain system token like <code>nlp:geo</code> or <code>nlp:date</code>. In many cases models
     * should be build to allow user input without user tokens. However, set it to {@code false} if presence
     * of at least one user token is mandatory.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_IS_NO_USER_TOKENS_ALLOWED} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>noUserTokensAllowed</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "noUserTokensAllowed": false
     * }
     * </pre>
     *
     * @return Whether or not to allow the user input with no user token detected.
     */
    default boolean isNoUserTokensAllowed() {
        return DFLT_IS_NO_USER_TOKENS_ALLOWED;
    }

    /**
     * Measure of how much sparsity is allowed when user input words are reordered in attempt to
     * match the multi-word synonyms. Zero means no reordering is allowed. One means
     * that only one word in a synonym can move one position left or right, and so on. Empirically
     * the value of {@code 2} proved to be a good default value in most cases. Note that larger
     * values mean that synonym words can be almost in any random place in the user input which makes
     * synonym matching practically meaningless. Maximum value is <code>4</code>.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_JIGGLE_FACTOR} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>jiggleFactor</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "jiggleFactor": 2
     * }
     * </pre>
     *
     * @return Word jiggle factor (sparsity measure).
     */
    default int getJiggleFactor() {
        return DFLT_JIGGLE_FACTOR;
    }

    /**
     * Gets minimum number of {@code nlp:date} tokens below which user input will be automatically rejected.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MIN_DATE_TOKENS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>minDateTokens</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "minDateTokens": 1
     * }
     * </pre>
     *
     * @return Minimum number of {@code nlp:date} tokens below which user input will be automatically rejected.
     */
    default int getMinDateTokens() {
        return DFLT_MIN_DATE_TOKENS;
    }

    /**
     * Gets maximum number of {@code nlp:date} tokens above which user input will be automatically rejected.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MAX_FUNCTION_TOKENS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>maxDateTokens</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "maxDateTokens": 2
     * }
     * </pre>
     *
     * @return Maximum number of {@code nlp:date} tokens above which user input will be automatically rejected.
     */
    default int getMaxDateTokens() {
        return DFLT_MAX_DATE_TOKENS;
    }

    /**
     * Gets minimum number of {@code nlp:num} tokens below which user input will be automatically rejected.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MIN_NUM_TOKENS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>minNumTokens</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "minNumTokens": 1
     * }
     * </pre>
     *
     * @return Minimum number of {@code nlp:num} tokens below which user input will be automatically rejected.
     */
    default int getMinNumTokens() {
        return DFLT_MIN_NUM_TOKENS;
    }

    /**
     * Gets maximum number of {@code nlp:num} tokens above which user input will be automatically rejected.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MAX_NUM_TOKENS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>maxNumTokens</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "maxNumTokens": 2
     * }
     * </pre>
     *
     * @return Maximum number of {@code nlp:num} tokens above which user input will be automatically rejected.
     */
    default int getMaxNumTokens() {
        return DFLT_MAX_NUM_TOKENS;
    }

    /**
     * Gets minimum number of {@code nlp:geo} tokens below which user input will be automatically rejected.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MIN_GEO_TOKENS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>minGeoTokens</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "minGeoTokens": 1
     * }
     * </pre>
     *
     * @return Minimum number of {@code nlp:geo} tokens below which user input will be automatically rejected.
     */
    default int getMinGeoTokens() {
        return DFLT_MIN_GEO_TOKENS;
    }

    /**
     * Gets maximum number of {@code nlp:geo} tokens above which user input will be automatically rejected.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MAX_GEO_TOKENS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>maxGeoTokens</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "maxGeoTokens": 1
     * }
     * </pre>
     *
     * @return Maximum number of {@code nlp:geo} tokens above which user input will be automatically rejected.
     */
    default int getMaxGeoTokens() {
        return DFLT_MAX_GEO_TOKENS;
    }

    /**
     * Gets minimum number of {@code nlp:function} tokens below which user input will be automatically rejected.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MIN_GEO_TOKENS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>minFunctionTokens</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "minFunctionTokens": 1
     * }
     * </pre>
     *
     * @return Minimum number of {@code nlp:function} tokens below which user input will be automatically rejected.
     */
    default int getMinFunctionTokens() {
        return DFLT_MIN_FUNCTION_TOKENS;
    }

    /**
     * Gets maximum number of {@code nlp:function} tokens above which user input will be automatically rejected.
     * <br><br>
     * <b>Default</b>
     * <br>
     * If not provided by the model the default value {@link #DFLT_MAX_FUNCTION_TOKENS} will be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>maxFunctionTokens</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "maxFunctionTokens": 2
     * }
     * </pre>
     *
     * @return Maximum number of {@code nlp:function} tokens above which user input will be automatically rejected.
     */
    default int getMaxFunctionTokens() {
        return DFLT_MAX_FUNCTION_TOKENS;
    }

    /**
     * Gets optional user specific model metadata can be set by the developer and accessed later.
     *
     * @return Optional user defined model metadata.
     */
    default NCMetadata getMetadata() {
        return new NCMetadataImpl();
    }

    /**
     * Gets an optional list of stopwords to add to the built-in ones.
     * <br><br>
     * Stopword is an individual word (i.e. sequence of characters excluding whitespaces) that contribute no
     * semantic meaning to the sentence. For example, 'the', 'wow', or 'hm' provide no semantic meaning to the
     * sentence and can be safely excluded from semantic analysis.
     * <br><br>
     * NLPCraft comes with a carefully selected list of English stopwords which should be sufficient
     * for a majority of use cases. However, you can add additional stopwords to this list. The typical
     * use for user-defined stopwords are jargon parasite words that are specific to the model's domain.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>additionalStopwords</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "additionalStopwords": [
     *          "stopword1",
     *          "stopword2"
     *      ]
     * }
     * </pre>
     *
     * @return Potentially empty list of additional stopwords.
     */
    default Set<String> getAdditionalStopWords() {
        return Collections.emptySet();
    }

    /**
     * Gets an optional list of stopwords to exclude from the built-in list of stopwords.
     * <br><br>
     * Just like you can add additional stopwords via {@link #getAdditionalStopWords()} you can exclude
     * certain words from the list of stopwords. This can be useful in rare cases when default built-in
     * stopword has specific meaning of your model. In order to process them you need to exclude them
     * from the list of stopwords.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>excludedStopwords</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "excludedStopwords": [
     *          "excludedStopword1",
     *          "excludedStopword2"
     *      ]
     * }
     * </pre>
     *
     * @return Potentially empty list of excluded stopwords.
     */
    default Set<String> getExcludedStopWords() {
        return Collections.emptySet();
    }

    /**
     * Gets an optional list of example sentences demonstrating what can be asked with this model. These
     * examples may be displayed by the management tools. It is highly recommended to supply a good list of
     * examples for the model as this provides perhaps the best description to the end user on how a particular
     * model and its data sources can be used.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>examples</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "examples": [
     *          "Example questions one",
     *          "Another sample sentence"
     *      ]
     * }
     * </pre>
     *
     * @return Potentially empty list of model examples.
     */
    default Set<String> getExamples() {
        return Collections.emptySet();
    }

    /**
     * Gets an optional list of suspicious words. A suspicious word is a word that generally should not appear in user
     * sentence when used with this model. For example, if a particular model is for children oriented book search,
     * the words "sex" and "porn" should probably NOT appear in the user input and can be automatically rejected
     * when added here and model's metadata {@code MAX_SUSPICIOUS_WORDS} property set to zero.
     * <br><br>
     * Note that by setting model's metadata {@code MAX_SUSPICIOUS_WORDS} property to non-zero value you can
     * adjust the sensitivity of suspicious words auto-rejection logic.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>suspiciousWords</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "suspiciousWords": [
     *          "sex",
     *          "porn"
     *      ]
     * }
     * </pre>
     *
     * @return Potentially empty list of suspicious words in their lemma form.
     */
    default Set<String> getSuspiciousWords() {
        return Collections.emptySet();
    }

    /**
     * Gets an optional map of macros to be used in this model. Macros and option groups are instrumental
     * in defining model's elements. See {@link NCElement} for documentation on macros.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>macros</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "macros": [
     *          {
     *              "name": "&lt;OF&gt;",
     *              "macro": "{of|for|per}"
     *          },
     *          {
     *              "name": "&lt;CUR&gt;",
     *              "macro": "{current|present|moment|now}"
     *          }
     *      ]
     * }
     * </pre>
     *
     * @return Potentially empty map of macros.
     */
    default Map<String, String> getMacros() {
        return Collections.emptyMap();
    }

    /**
     * Gets a set of model elements.
     * <br><br>
     * An element is the main building block of the semantic model. Model element defines an entity
     * that will be automatically recognized in the user input either by one of its synonyms or values, or directly by
     * its ID. 
     * <br><br>
     * Model element is represented by {@link NCElement} interface. There are several way to create an
     * instance of this interface:
     * <ul>
     *     <li>
     *         Manually implement this interface.
     *     </li>
     *     <li>
     *         Use {@link NCElementBuilder} to conveniently build the instance in a programmatic way.
     *     </li>
     *     <li>
     *         Use JSON model representation. Unless your model has one or two elements we recommend
     *         defining model and its elements in an external JSON file.
     *     </li>
     * </ul>
     * See {@link NCElement} for documentation on macros and option groups.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this is set by <code>elements</code> JSON properties:
     * <pre class="brush: js">
     * {
     *      "elements": [
     *         {
     *             "id": "wt:hist",
     *             "synonyms": [
     *                 "{&lt;WEATHER&gt;|*} &lt;HISTORY&gt;",
     *                 "&lt;HISTORY&gt; {&lt;OF&gt;|*} &lt;WEATHER&gt;"
     *             ],
     *             "description": "Past weather conditions."
     *         }
     * }
     * </pre>
     *
     * @return Set of model elements (potentially empty).
     */
    default Set<NCElement> getElements() {
        return Collections.emptySet();
    }

    /**
     * A callback before this model instance gets discarded. It gives the model
     * a chance to do a state cleanup, if necessary. There's no guarantee that this method will ever
     * be invoked - it is only invoked if data probe process exits orderly. However, it is guaranteed
     * that this method will be called at most once. If model implementation has the state that must be
     * persisted it needs to persist that state through its own mechanisms without relying on this method.
     * <br><br>
     * Note that if model has an important state it is highly recommended that it would store it periodically
     * instead of relying on this method.
     * <br><br>
     * <b>Default</b>
     * <br>
     * Default implementation is a no-op.
     * <br><br>
     * <b>JSON</b>
     * <br>
     * If using JSON model presentation this method will have no-op implementation.
     *
     * @see NCProbeContext#reloadModel(String)
     */
    default void discard() {
        // No-op.
    }

    /**
     * Probe calls this method to initialize the model when it gets deployed in the probe. This method
     * is guaranteed to be called and it will be called only once.
     * <br><br>
     * <b>Default</b>
     * <br>
     * Default implementation is a no-op.
     *
     * @param probeCtx Probe context.
     */
    default void initialize(NCProbeContext probeCtx) {
        // No-op.
    }

    /**
     * Processes user input provided in the given query context and either returns the query result or throws
     * an exception. This is the <b>main method</b> that user
     * should implement when developing a semantic model. See {@link NCIntentSolver} for intent-based
     * user input processing for a simplified way to encode that processing logic.
     *
     * @param ctx Query context containing parsed user input and all associated data.
     * @return Query result. This result cannot be {@code null}. In case of any errors this method should
     *      throw {@link NCRejection} exception.
     * @throws NCRejection Thrown when user input cannot be processed as is and should be rejected.
     * @see NCIntentSolver
     */
    NCQueryResult query(NCQueryContext ctx) throws NCRejection;
}
