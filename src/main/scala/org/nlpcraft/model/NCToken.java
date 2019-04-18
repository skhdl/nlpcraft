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
import org.nlpcraft.model.utils.NCTokenUtils;

import java.io.Serializable;

/**
 * A token is a part of a fully parsed user input. A token corresponds to a one or more
 * words, sequential or not, in the user sentence and can be of two types:
 * <ul>
 *     <li>
 *         <b>System defined</b> token that represents a built-in element whose ID starts with {@code nlp:},
 *         e.g. {@code nlp:num, nlp:geo}.
 *     </li>
 *     <li>
 *         <b>User defined</b> token that represent a detected user defined element in the model
 *         (see {@link NCModel#getElements()}).
 *     </li>
 * </ul>
 * Most of the token's information is stored in a map-like metadata accessible via {@link #getMetadata()} method.
 * Depending on the {@link #getId() ID} of the token metadata will contain different properties. Some properties
 * are common across all tokens. Consult {@link #getMetadata()} method for detailed explanation. Note that
 * class {@link NCTokenUtils} provides utility accessors to all metadata properties returned by
 * {@link #getMetadata()} method in a more convenient way.
 *
 * @see NCTokenUtils
 */
public interface NCToken extends Serializable {
    /**
     * Gets token metadata.
     * <br><br>
     * When a token is detected in the user input it has number of properties describing that token. These
     * properties are stored in a map-like {@link NCMetadata metadata} interface for better future
     * extensibility and API compatibility. Note that certain metadata is common across all
     * types of tokens, while other parameters are specific to the particular types of built-in token.
     * Note also that class {@link NCTokenUtils} provide convenient typed accessors to these properties as well
     * as many utility methods.
     * <br><br>
     * <b>Intent DSL</b>
     * <br>
     * All token properties documented in this method can be used in {@link NCIntentSolver.RULE} DSL.
     * Using token properties in intent definition may allow for better accuracy and deeper matching
     * semantics for intents. For example:
     * <pre class="brush: java">
     *      new RULE("~GEO_KIND == CITY"); // Match only cities.
     *      new RULE("~NUM_INDEX != null"); // Avoid "dangling" numeric conditions.
     *      new RULE("~NUM_TO &gt;= 100"); // Match any numeric values &gt;= 100.
     * </pre>
     * Consult {@link NCIntentSolver.RULE} class for details.
     * <br><br>
     * <b>Common Properties</b>
     * <br>
     * The following metadata properties are provided for every token regardless of the type:
     * <table class="dl-table" summary="">
     *     <tr>
     *         <th>Name <sub>(Map Key)</sub></th>
     *         <th>Description</th>
     *         <th>Value Type</th>
     *     </tr>
     *     <tr>
     *         <td>NLP_UNID</td>
     *         <td>Internal globally unique system ID of the token.</td>
     *         <td>{@link String}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_BRACKETED</td>
     *         <td>Whether or not this token is surrounded by any of {@code '[', ']', '{', '}', '(', ')'} brackets.</td>
     *         <td>{@link Boolean}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_FREEWORD</td>
     *         <td>
     *              Whether or not this token represents a free word.
     *              A free word is a token that was detected
     *              neither as a part of user defined or system tokens, i.e. it has
     *              token ID {@code nlp:nlp}.
     *         </td>
     *         <td>{@link Boolean}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_DIRECT</td>
     *         <td>
     *              Whether or not this token was matched on direct (nor permutated) synonym.
     *         </td>
     *         <td>{@link Boolean}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_ENGLISH</td>
     *         <td>
     *              Whether this token represents an English word. Note that this only checks that token's text
     *              consists of characters of English alphabet, i.e. the text doesn't have to be necessary
     *              a known valid English word. See {@link NCModel#isNonEnglishAllowed()} for corresponding
     *              model configuration.
     *         </td>
     *         <td>{@link Boolean}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_LEMMA</td>
     *         <td>
     *             Lemma of this token, i.e. a canonical form of this word. Note that stemming and lemmatization
     *             allow to reduce inflectional forms and sometimes derivationally related forms of a word to a
     *             common base form. Lemmatization refers to the use of a vocabulary and morphological analysis
     *             of words, normally aiming to remove inflectional endings only and to return the base or dictionary
     *             form of a word, which is known as the lemma. Learn
     *             more at <a href="https://nlp.stanford.edu/IR-book/html/htmledition/stemming-and-lemmatization-1.html">https://nlp.stanford.edu/IR-book/html/htmledition/stemming-and-lemmatization-1.html</a>
     *         </td>
     *         <td>{@link String}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_STEM</td>
     *         <td>
     *             Stem of this token. Note that stemming and lemmatization allow to reduce inflectional forms
     *             and sometimes derivationally related forms of a word to a common base form. Unlike lemma,
     *             stemming is a basic heuristic process that chops off the ends of words in the hope of achieving
     *             this goal correctly most of the time, and often includes the removal of derivational affixes.
     *             Learn more at <a href="https://nlp.stanford.edu/IR-book/html/htmledition/stemming-and-lemmatization-1.html">https://nlp.stanford.edu/IR-book/html/htmledition/stemming-and-lemmatization-1.html</a>
     *         </td>
     *         <td>{@link String}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_POS</td>
     *         <td>
     *             Penn Treebank POS tag for this token. Note that additionally to standard Penn Treebank POS
     *             tags NLPCraft introduced {@code '---'} synthetic tag to indicate a POS tag for multiword tokens.
     *             Learn more at <a href="http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html</a>
     *         </td>
     *         <td>{@link String}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_POSDESC</td>
     *         <td>
     *             Description of Penn Treebank POS tag. Learn more at <a href="http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html</a>
     *         </td>
     *         <td>{@link String}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_SWEAR</td>
     *         <td>
     *             Whether or not this token is a swear word. NLPCraft has built-in list of common English swear words.
     *             See {@link NCModel#isSwearWordsAllowed()} for corresponding model configuration.
     *         </td>
     *         <td>{@link Boolean}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_ORIGTEXT</td>
     *         <td>
     *              Original user input text for this token.
     *         </td>
     *         <td>{@link String}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_NORMTEXT</td>
     *         <td>
     *             Normalized user input text for this token.
     *         </td>
     *         <td>{@link String}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_NE</td>
     *         <td>
     *             Optional (nullable) named entity.
     *         </td>
     *         <td>{@link String}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_NNE</td>
     *         <td>
     *             Optional (nullable) normalized named entity. Currently provided only by 'stanford' NLP engine.
     *         </td>
     *         <td>{@link String}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_SPARSITY</td>
     *         <td>
     *              Numeric value of how sparse the token is. Sparsity zero means that all individual words in the token
     *              follow each other.
     *         </td>
     *         <td>{@link Integer}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_MININDEX</td>
     *         <td>
     *             Index of the first word in this token. Note that token may not be contiguous.
     *         </td>
     *         <td>{@link Integer}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_MAXINDEX</td>
     *         <td>
     *             Index of the last word in this token. Note that token may not be contiguous.
     *         </td>
     *         <td>{@link Integer}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_WORDINDEXES</td>
     *         <td>
     *             List of original word indexes in this token. Note that a token can have words that are not
     *             contiguous in the original sentence. Always has at least one element in it.
     *         </td>
     *         <td>{@link java.util.List}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_WORDLENGTH</td>
     *         <td>
     *             Number of individual words in this token. Equal to the size of <code>NLP_WORDINDEXES</code> list.
     *         </td>
     *         <td>{@link Integer}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_CONTIGUOUS</td>
     *         <td>
     *             Whether or not this token has zero sparsity, i.e. consists of contiguous words.
     *         </td>
     *         <td>{@link Boolean}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_START</td>
     *         <td>
     *              Start character index of this token.
     *         </td>
     *         <td>{@link Integer}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_END</td>
     *         <td>
     *             End character index of this token.
     *         </td>
     *         <td>{@link Integer}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_INDEX</td>
     *         <td>
     *             Index of this token in the sentence.
     *         </td>
     *         <td>{@link Integer}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_CHARLENGTH</td>
     *         <td>
     *             Character length of this token.
     *         </td>
     *         <td>{@link Integer}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_QUOTED</td>
     *         <td>
     *             Whether or not this token is surrounded by single or double quotes.
     *         </td>
     *         <td>{@link Boolean}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_STOPWORD</td>
     *         <td>
     *             Whether or not this token is a stopword. Stopwords are some extremely common words which
     *             add little value in helping understanding user input and are excluded from the processing
     *             entirely. For example, words like {@code a, the, can, of, about, over}, etc. are typical
     *             stopwords in English. NLPCraft has built-in set of stopwords.
     *             <br><br>
     *             See {@link NCModel#getAdditionalStopWords()} and {@link NCModel#getExcludedStopWords()} methods
     *             for corresponding model configuration.
     *         </td>
     *         <td>{@link Boolean}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_NOTETYPE</td>
     *         <td>
     *             System type name of the token.
     *         </td>
     *         <td>{@link String}</td>
     *     </tr>
     *     <tr>
     *         <td>NLP_DICT</td>
     *         <td>
     *             Whether or not this token is found in Princeton WordNet database.
     *         </td>
     *         <td>{@link Boolean}</td>
     *     </tr>
     * </table>
     * <br><br>
     * <b>Properties For <tt>nlp:geo</tt> Tokens</b><br>
     * <tt>nlp:geo</tt> system tokens represent geographical locations such as continents, countries,
     * regions, cities, and metro areas. The following metadata properties are specifically provided
     * for <tt>nlp:geo</tt> tokens:
     * <table class="dl-table" summary="">
     *     <tr>
     *         <th>Name <sub>(Map Key)</sub></th>
     *         <th>Description</th>
     *         <th>Value Type</th>
     *         <th>Optional</th>
     *     </tr>
     *     <tr>
     *         <td>GEO_KIND</td>
     *         <td>
     *              Kind of geographical location:
     *              <ul>
     *                  <li><tt>CONTINENT</tt></li>
     *                  <li><tt>SUBCONTINENT</tt></li>
     *                  <li><tt>COUNTRY</tt></li>
     *                  <li><tt>METRO</tt></li>
     *                  <li><tt>REGION</tt></li>
     *                  <li><tt>CITY</tt></li>
     *              </ul>
     *         </td>
     *         <td>{@link String}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>GEO_CONTINENT</td>
     *         <td>
     *              Name of the continent.
     *         </td>
     *         <td>{@link String}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>GEO_SUBCONTINENT</td>
     *         <td>
     *              Name of the subcontinent.
     *         </td>
     *         <td>{@link String}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>GEO_COUNTRY</td>
     *         <td>
     *              Name of the continent.
     *         </td>
     *         <td>{@link String}</td>
     *         <td>Yes</td>
     *     </tr>
     *     <tr>
     *         <td>GEO_REGION</td>
     *         <td>
     *              Name of the region.
     *         </td>
     *         <td>{@link String}</td>
     *         <td>Yes</td>
     *     </tr>
     *     <tr>
     *         <td>GEO_CITY</td>
     *         <td>
     *              Name of the city.
     *         </td>
     *         <td>{@link String}</td>
     *         <td>Yes</td>
     *     </tr>
     *     <tr>
     *         <td>GEO_METRO</td>
     *         <td>
     *              Name of the metro area.
     *         </td>
     *         <td>{@link String}</td>
     *         <td>Yes</td>
     *     </tr>
     *     <tr>
     *         <td>GEO_LATITUDE</td>
     *         <td>
     *              Optional latitude of the location.
     *         </td>
     *         <td>{@link Double}</td>
     *         <td>Yes</td>
     *     </tr>
     *     <tr>
     *         <td>GEO_LONGITUDE</td>
     *         <td>
     *              Optional longitude of the location.
     *         </td>
     *         <td>{@link Double}</td>
     *         <td>Yes</td>
     *     </tr>
     * </table>
     * <br><br>
     * <b>Properties For <tt>nlp:date</tt> Tokens</b><br>
     * <tt>nlp:date</tt> tokens represent a datetime range, i.e. two datetime points. The following
     * metadata properties are specifically provided for <tt>nlp:date</tt> tokens:
     * <table class="dl-table" summary="">
     *     <tr>
     *         <th>Name <sub>(Map Key)</sub></th>
     *         <th>Description</th>
     *         <th>Value Type</th>
     *         <th>Optional</th>
     *     </tr>
     *     <tr>
     *         <td>DATE_FROM</td>
     *         <td>
     *              Start timestamp of the datetime range.
     *         </td>
     *         <td>{@link Long}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>DATE_TO</td>
     *         <td>
     *              End timestamp of the datetime range.
     *         </td>
     *         <td>{@link Long}</td>
     *         <td>No</td>
     *     </tr>
     * </table>
     * <br><br>
     * <b>Properties For <tt>nlp:num</tt> Tokens</b><br>
     * <tt>nlp:num</tt> tokens represent a single numeric value, a numeric range, or a numeric condition.
     * All numeric values can have optional units. The following metadata properties are specifically
     * provided for <tt>nlp:num</tt> tokens:
     * <table class="dl-table" summary="">
     *     <tr>
     *         <th>Name <sub>(Map Key)</sub></th>
     *         <th>Description</th>
     *         <th>Value Type</th>
     *         <th>Optional</th>
     *     </tr>
     *     <tr>
     *         <td>NUM_FROM</td>
     *         <td>
     *             Start of numeric range that satisfies the condition (exclusive).
     *             Note that if <code>NUM_FROM</code> and <code>NUM_TO</code> are the same this
     *             token represent a single value (whole or fractional) in which case
     *             <code>NUM_ISEQUALCONDITION</code> will be <code>true</code>.
     *         </td>
     *         <td>{@link Double}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_FROMINCL</td>
     *         <td>
     *             Whether or not start of the numeric range is inclusive.
     *         </td>
     *         <td>{@link Boolean}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_TO</td>
     *         <td>
     *             End of numeric range that satisfies the condition (exclusive).
     *             Note that if <code>NUM_FROM</code> and <code>NUM_TO</code> are the same this
     *             token represent a single value (whole or fractional) in which case
     *             <code>NUM_ISEQUALCONDITION</code> will be <code>true</code>.
     *         </td>
     *         <td>{@link Double}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_TOINCL</td>
     *         <td>
     *             Whether or not end of the numeric range is inclusive.
     *         </td>
     *         <td>{@link Boolean}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_ISEQUALCONDITION</td>
     *         <td>
     *             Whether this is an equality condition. Note that <b>single numeric values</b> (when
     *             <code>NUM_FROM</code> and <code>NUM_TO</code> are the same) also default
     *             to equality condition and this property will be {@code true}.
     *         </td>
     *         <td>{@link Boolean}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_ISNOTEQUALCONDITION</td>
     *         <td>
     *             Whether this is a not-equality condition.
     *         </td>
     *         <td>{@link Boolean}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_ISFROMNEGATIVEINFINITY</td>
     *         <td>
     *             Whether this range is from negative infinity.
     *         </td>
     *         <td>{@link Boolean}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_ISRANGECONDITION</td>
     *         <td>
     *             Whether this is a range condition.
     *         </td>
     *         <td>{@link Boolean}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_ISTOPOSITIVEINFINITY</td>
     *         <td>
     *             Whether this range is to positive infinity.
     *         </td>
     *         <td>{@link Boolean}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_ISFRACTIONAL</td>
     *         <td>
     *             Whether this token's value (single numeric value of a range) is a
     *             whole or a fractional number.
     *         </td>
     *         <td>{@link Boolean}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_INDEX</td>
     *         <td>
     *             Index of another token in the sentence that this numeric condition is referring to.
     *             If index could not be determined this token refers to a free word or a stopword.
     *         </td>
     *         <td>{@link Integer}</td>
     *         <td>Yes</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_UNIT</td>
     *         <td>
     *             Optional numeric value unit (e.g. "mm", "cm", "ft"). See <code>NUM_UNITTYPE</code>
     *             for specific values of units and unit types.
     *         </td>
     *         <td>{@link String}</td>
     *         <td>Yes</td>
     *     </tr>
     *     <tr>
     *         <td>NUM_UNITTYPE</td>
     *         <td>
     *             Optional type of the numeric value unit (e.g. "length", "force", "mass"):
     *             <table class="dl-table" summary="">
     *                  <tr>
     *                      <th><code>NUM_UNITTYPE</code></th>
     *                      <th><code>NUM_UNIT</code></th>
     *                  </tr>
     *                  <tr>
     *                      <td>mass</td>
     *                      <td>feet per second, grams, kilogram, grain, dram, ounce, pound, hundredweight, ton, tonne, slug</td>
     *                  </tr>
     *                  <tr>
     *                      <td>torque</td>
     *                      <td>newton meter</td>
     *                  </tr>
     *                  <tr>
     *                      <td>area</td>
     *                      <td>square meter, acre, are, hectare, square inches, square feet, square yards, square miles</td>
     *                  </tr>
     *                  <tr>
     *                      <td>paper quantity</td>
     *                      <td>paper bale</td>
     *                  </tr>
     *                  <tr>
     *                      <td>force</td>
     *                      <td>kilopond, pond</td>
     *                  </tr>
     *                  <tr>
     *                      <td>pressure</td>
     *                      <td>pounds per square inch</td>
     *                  </tr>
     *                  <tr>
     *                      <td>solid angle</td>
     *                      <td>steradian</td>
     *                  </tr>
     *                  <tr>
     *                      <td>pressure, stress</td>
     *                      <td>pascal</td>
     *                  </tr>
     *                  <tr>
     *                      <td>luminous flux</td>
     *                      <td>lumen</td>
     *                  </tr>
     *                  <tr>
     *                      <td>amount of substance</td>
     *                      <td>mole</td>
     *                  </tr>
     *                  <tr>
     *                      <td>luminance</td>
     *                      <td>candela per square metre</td>
     *                  </tr>
     *                  <tr>
     *                      <td>angle</td>
     *                      <td>radian, degree</td>
     *                  </tr>
     *                  <tr>
     *                      <td>magnetic flux density, magnetic field</td>
     *                      <td>tesla</td>
     *                  </tr>
     *                  <tr>
     *                      <td>power, radiant flux</td>
     *                      <td>watt</td>
     *                  </tr>
     *                  <tr>
     *                      <td>datetime</td>
     *                      <td>second, minute, hour, day, week, month, year</td>
     *                  </tr>
     *                  <tr>
     *                      <td>electrical inductance</td>
     *                      <td>henry</td>
     *                  </tr>
     *                  <tr>
     *                      <td>electric charge</td>
     *                      <td>coulomb</td>
     *                  </tr>
     *                  <tr>
     *                      <td>temperature</td>
     *                      <td>kelvin, centigrade, fahrenheit</td>
     *                  </tr>
     *                  <tr>
     *                      <td>voltage, electrical</td>
     *                      <td>volt</td>
     *                  </tr>
     *                  <tr>
     *                      <td>momentum</td>
     *                      <td>kilogram meters per second</td>
     *                  </tr>
     *                  <tr>
     *                      <td>amount of heat</td>
     *                      <td>calorie</td>
     *                  </tr>
     *                  <tr>
     *                      <td>electrical capacitance</td>
     *                      <td>farad</td>
     *                  </tr>
     *                  <tr>
     *                      <td>radioactive decay</td>
     *                      <td>becquerel</td>
     *                  </tr>
     *                  <tr>
     *                      <td>electrical conductance</td>
     *                      <td>siemens</td>
     *                  </tr>
     *                  <tr>
     *                      <td>luminous intensity</td>
     *                      <td>candela</td>
     *                  </tr>
     *                  <tr>
     *                      <td>work, energy</td>
     *                      <td>joule</td>
     *                  </tr>
     *                  <tr>
     *                      <td>quantities</td>
     *                      <td>dozen</td>
     *                  </tr>
     *                  <tr>
     *                      <td>density</td>
     *                      <td>density</td>
     *                  </tr>
     *                  <tr>
     *                      <td>sound</td>
     *                      <td>decibel</td>
     *                  </tr>
     *                  <tr>
     *                      <td>electrical resistance, impedence</td>
     *                      <td>ohm</td>
     *                  </tr>
     *                  <tr>
     *                      <td>force, weight</td>
     *                      <td>newton</td>
     *                  </tr>
     *                  <tr>
     *                      <td>light quantity</td>
     *                      <td>lumen seconds</td>
     *                  </tr>
     *                  <tr>
     *                      <td>length</td>
     *                      <td>meter, millimeter, centimeter, decimeter, kilometer, astronomical unit, light year, parsec, inch, foot, yard, mile, nautical mile</td>
     *                  </tr>
     *                  <tr>
     *                      <td>refractive index</td>
     *                      <td>diopter</td>
     *                  </tr>
     *                  <tr>
     *                      <td>frequency</td>
     *                      <td>hertz, angular frequency</td>
     *                  </tr>
     *                  <tr>
     *                      <td>power</td>
     *                      <td>kilowatt, horsepower, bar</td>
     *                  </tr>
     *                  <tr>
     *                      <td>magnetic flux</td>
     *                      <td>weber</td>
     *                  </tr>
     *                  <tr>
     *                      <td>current</td>
     *                      <td>ampere</td>
     *                  </tr>
     *                  <tr>
     *                      <td>acceleration of gravity</td>
     *                      <td>gravity imperial, gravity metric</td>
     *                  </tr>
     *                  <tr>
     *                      <td>volume</td>
     *                      <td>cubic meter, liter, milliliter, centiliter, deciliter, hectoliter, cubic inch, cubic foot, cubic yard, acre-foot, teaspoon, tablespoon, fluid ounce, cup, gill, pint, quart, gallon</td>
     *                  </tr>
     *                  <tr>
     *                      <td>speed</td>
     *                      <td>miles per hour, meters per second</td>
     *                  </tr>
     *                  <tr>
     *                      <td>illuminance</td>
     *                      <td>lux</td>
     *                  </tr>
     *             </table>
     *         </td>
     *         <td>{@link String}</td>
     *         <td>Yes</td>
     *     </tr>
     * </table>
     * <br><br>
     * <b>Properties For <tt>nlp:function</tt> Tokens</b><br>
     * <tt>nlp:function</tt> tokens represent a function in relation to another token or tokens.
     * The following metadata properties are specifically provided for <tt>nlp:function</tt> tokens:
     * <table class="dl-table" summary="">
     *     <tr>
     *         <th>Name <sub>(Map Key)</sub></th>
     *         <th>Description</th>
     *         <th>Value Type</th>
     *         <th>Optional</th>
     *     </tr>
     *     <tr>
     *         <td>FUNCTION_TYPE</td>
     *         <td>
     *             Type of the function detected. One of the following:
     *             <ul>
     *                 <li><tt>SUM</tt> - sum of some elements.</li>
     *                 <li><tt>MAX</tt> - maximum of some elements.</li>
     *                 <li><tt>MIN</tt> - minimum of some elements.</li>
     *                 <li><tt>AVG</tt> - average of some elements.</li>
     *                 <li><tt>SORT</tt> - sorting or ordering instruction over some elements.</li>
     *                 <li><tt>LIMIT</tt> - range limit over some elements.</li>
     *                 <li><tt>GROUP</tt> - grouping over some elements.</li>
     *                 <li><tt>CORRELATION</tt> - correlation between two elements.</li>
     *                 <li><tt>COMPARE</tt> - comparison between some elements.</li>
     *             </ul>
     *         </td>
     *         <td>{@link String}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>FUNCTION_INDEXES</td>
     *         <td>
     *             Indexes of the element(s) this token is referencing. Can be empty.
     *         </td>
     *         <td>{@link java.util.List}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>FUNCTION_LIMIT</td>
     *         <td>
     *             <b>Applicable only for <tt>LIMIT</tt> type.</b>
     *             Limit value.
     *         </td>
     *         <td>{@link Integer}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>FUNCTION_ASC</td>
     *         <td>
     *             <b>Applicable only for <tt>LIMIT</tt> and <tt>SORT</tt> types.</b>
     *             Whether limit or sort direction is ascending (bottom to top) or descending (top to bottom).
     *         </td>
     *         <td>{@link Boolean}</td>
     *         <td>No</td>
     *     </tr>
     * </table>
     * <br><br>
     * <b>Properties For <tt>nlp:coordinate</tt> Tokens</b><br>
     * <tt>nlp:coordinate</tt> tokens geographical latitude and longitude coordinates.
     * The following metadata properties are specifically provided for <tt>nlp:coordinate</tt> tokens:
     * <table class="dl-table" summary="">
     *     <tr>
     *         <th>Name <sub>(Map Key)</sub></th>
     *         <th>Description</th>
     *         <th>Value Type</th>
     *         <th>Optional</th>
     *     </tr>
     *     <tr>
     *         <td>COORDINATE_LATITUDE</td>
     *         <td>
     *             Coordinate latitude.
     *         </td>
     *         <td>{@link Double}</td>
     *         <td>No</td>
     *     </tr>
     *     <tr>
     *         <td>COORDINATE_LONGITUDE</td>
     *         <td>
     *             Coordinate longitude.
     *         </td>
     *         <td>{@link Double}</td>
     *         <td>No</td>
     *     </tr>
     * </table>
     *
     * @return Token metadata.
     * @see NCTokenUtils
     * @see #getElementMetadata()
     */
    NCMetadata getMetadata();

    /**
     * Gets model element metadata when this token corresponds to the user element. <b>Don't confuse</b> this method
     * with {@link #getMetadata()} that returns properties for this token vs. the metadata for the model
     * element this token is referring to. If this token is a {@link #isSystemDefined() system-defined one} this
     * method will return {@code null}.
     * <br><br>
     * <b>Intent DSL</b>
     * <br>
     * All properties returned by this method can be used in {@link NCIntentSolver.RULE} DSL.
     * Using model element properties in intent definition may allow for better accuracy and deeper matching
     * semantics for intents. For example:
     * <pre class="brush: java">
     *      new RULE("~MY_PROP == VALUE");
     *      new RULE("~INT_PROP != 100"); 
     * </pre>
     *
     * @return Model element metadata when this token corresponds to the user element or {@code null} otherwise.
     * @see NCElement#getMetadata()
     * @see #getMetadata()
     */
    NCMetadata getElementMetadata();

    /**
     * Checks whether or not this token represents a user defined model element (vs. a built-in system token).
     *
     * @return {@code True} if this token represents user-defined model element.
     */
    boolean isUserDefined();

    /**
     * Checks whether or not this token represents a built-in system token.
     *
     * @return {@code True} if this token represents built-in system token.
     */
    boolean isSystemDefined();

    /**
     * Gets ID of the server request this token is part of.
     *
     * @return ID of the server request this token is part of.
     */
    String getServerRequestId();

    /**
     * If this token represents user defined model element this method returns
     * the ID of that element. Otherwise, it returns ID of the built-in system token starting with {@code nlp:}.
     * Note that a sentence can have multiple tokens with the same element ID. Don't confuse this ID
     * with the internal system ID of the token itself accessible through {@link #getMetadata() NLP_ID}
     * metadata property.
     *
     * @return ID of the element (system or user defined).
     * @see NCElement#getId()
     * @see #getMetadata()
     */
    String getId();

    /**
     * Gets the optional parent ID of the model element this token represents. This only available
     * for user-defined model elements (system built-in tokens do not have parents).
     *
     * @return ID of the token's element immediate parent or {@code null} if not available.
     * @see NCElement#getParentId()
     */
    String getParentId();

    /**
     * Gets the value if this token was detected via element's value (or its synonyms). Otherwise
     * returns {@code null}. Only applicable for user-defined model elements (system built-in tokens
     * do not have values).
     * 
     * @return Value for the user-defined model element or {@code null}, if not available.
     * @see NCElement#getValues()
     */
    String getValue();

    /**
     * Gets optional group this token's element belongs to. Only applicable for user-defined model elements
     * (system built-in tokens will return {@code null}).
     *
     * @return Element group or {@code null} if group is not available.
     * @see NCElement#getGroup()
     */
    String getGroup();
}
