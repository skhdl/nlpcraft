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
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.mdllib;

import java.util.*;
import java.util.regex.*;

/**
 * Semantic model element.
 * <br><br>
 * An element is the main building block of the semantic model. A semantic element defines an entity
 * that will be automatically recognized in the user input either by one of its synonyms or values, or directly by
 * its ID.
 *
 * <h2>Synonyms</h2>
 * Synonyms are the key building blocks of the semantic element and used in the following methods:
 * <ul>
 *     <li>{@link #getSynonyms()}</li>
 *     <li>{@link #getExcludedSynonyms()}</li>
 *     <li>{@link #getValues()}</li>
 * </ul>
 * Each model element has one or more synonyms. Note that element ID is its implicit synonym so that even if no
 * additional synonyms are defined at least one synonym always exists. Each individual synonym is a whitespace
 * separated combination of:
 * <ul>
 *     <li>simple word,</li>
 *     <li>regular expression, or</li>
 *     <li>PoS tag</li>
 * </ul>
 * Note that synonym matching for simple words is case insensitive and automatically
 * performed on <i>normalized and stemmatized forms</i> of such word and therefore the model
 * provider doesn't have to account for this in the synonyms themselves.
 *
 * <h3>Macro Expansions</h3>
 * Listing all possible multi-word synonyms for a given element can be a time consuming tasks. Macros together with
 * option groups allow for dramatic simplification of this process. Model provides a list of macros via
 * {@link NCModel#getMacros()} method. Each macro has a name in a form of {@code <X>} where {@code X} is
 * just any string, and a string value. Note that macros can be nested, i.e. macro value can include references
 * to another macros. When macro name {@code <X>} is encountered in the synonym it gets repeatedly replaced with
 * its value.
 *
 * <h3>Option Groups</h3>
 * Option groups are a simplified form of regular expressions that operates on a single word base. The
 * following examples demonstrate how to use option groups. Consider that the following macros are defined:
 * <table summary="" class="dl-table">
 *     <tr>
 *         <th>Macro Name</th>
 *         <th>Macro Value</th>
 *     </tr>
 *     <tr>
 *         <td>{@code <A>}</td>
 *         <td>{@code aaa}</td>
 *     </tr>
 *     <tr>
 *         <td>{@code <B>}</td>
 *         <td>{@code <A> bbb}</td>
 *     </tr>
 *     <tr>
 *         <td>{@code <C>}</td>
 *         <td>{@code <A> bbb {z|w}}</td>
 *     </tr>
 * </table>
 * Note that macros {@code <B>} and {@code <C>} are nested. Then the following option group expansions
 * will occur in these examples:
 * <table summary="" class="dl-table">
 *     <tr>
 *         <th>Synonym</th>
 *         <th>Expanded Synonyms</th>
 *     </tr>
 *     <tr>
 *         <td>{@code <A> {b|*} c}</td>
 *         <td>
 *             {@code "aaa b c"}<br>
 *             {@code "aaa c"}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code <B> {b|*} c}</td>
 *         <td>
 *             {@code "aaa bbb b c"}<br>
*              {@code "aaa bbb c"}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code {b|```NN```} c}</td>
 *         <td>
 *             {@code "b c"}<br>
 *             {@code "```NN``` c"}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code {b|\{\*\}}}</td>
 *         <td>
 *             {@code "b"}<br>
 *             {@code "b {*}"}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code a {b|*}. c}</td>
 *         <td>
 *             {@code "a b. c"}<br>
 *             {@code "a . c"}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code a .{b,  |*}. c}</td>
 *         <td>
 *             {@code "a .b, . c"}<br>
 *             {@code "a .. c"}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code a {{b|c}|*}.}</td>
 *         <td>
 *             {@code "a ."}<br>
 *             {@code "a b."}<br>
 *             {@code "a c."}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code a {{{<C>}}|{*}} c}</td>
 *         <td>
 *             {@code "a aaa bbb z c"}<br>
 *             {@code "a aaa bbb w c"}<br>
 *             {@code "a c"}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code {{{a}}} {b||*|{{*}}||*}}</td>
 *         <td>
 *             {@code "a b"}<br>
 *             {@code "a"}
 *         </td>
 *     </tr>
 * </table>
 * Specifically:
 * <ul>
 *     <li>{@code {A|B}}  denotes either {@code A} or {@code B}.</li>
 *     <li>{@code {A|B|*}}  denotes either {@code A} or {@code B} or nothing.</li>
 *     <li>Excessive curly brackets are ignored, when safe to do so.</li>
 *     <li>Macros cannot be recursive but can be nested.</li>
 *     <li>Option groups can be nested.</li>
 *     <li>
 *         {@code '\'} (backslash) can be used to escape <code>'{'</code>, <code>'}'</code>, {@code '|'} and
 *         {@code '*'} special symbols used by the option groups.
 *     </li>
 *     <li>Excessive whitespaces are trimmed when expanding option groups.</li>
 * </ul>
 *
 * <h3>Regular Expressions</h3>
 * Any individual synonym word that starts and ends with {@code "///"} (three forward slashes) is considered to be Java
 * regular expression as defined in {@link Pattern}. Note that regular expression can only span a single word, i.e.
 * only individual words from the user input will be matched against given regular expression and no whitespaces are
 * allowed within regular expression. Note also that option group special symbols <code>'{'</code>, <code>'}'</code>,
 * {@code '|'} and {@code '*'} have to be escaped in the regular expression using {@code '\'} (backslash).
 * <br><br>
 * For example, the following synonym {@code {foo|///[bar].+///}} will match word {@code foo} or any other strings
 * that start with {@code bar} as long as this string doesn't contain whitespaces.
 *
 * <h3>PoS Tags</h3>
 * Any individual synonym word that that starts and ends with {@code "```"} (three backticks) in a form <code>```XXX```</code> is
 * considered to be a PoS (Part-of-Speech) tag that will be matched against PoS tag of the individual word in the
 * user input, where {@code XXX} is one of the
 * <a target=_ href="https://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">Penn Treebank PoS</a> tags.
 * <br><br>
 * For example, the following synonym <code>{foo|{```NN```|```NNS```|```NNP```|```NNPS```}}</code> will match word {@code foo} or any
 * form of a noun.
 */
public interface NCElement {
    /**
     * Element's value.
     *
     * @see NCElement#getValues()
     */
    interface NCValue {
        /**
         * Gets value name.
         *
         * @return Value name.
         */
        String getName();

        /**
         * Gets optional list of value's synonyms.
         *
         * @return Potentially empty list of value's synonyms.
         */
        List<String> getSynonyms();
    }

    /**
     * Gets unique ID of this element.
     * <br><br>
     * This unique ID should be human readable for simpler debugging and testing of the model.
     * Although element ID could be any arbitrary string it is highly recommended to have
     * element ID as a lower case string starting with some model prefix, followed by colon and
     * then the element's name. This naming consistency will greatly help during curation process
     * where a human operator can specify model element directly by its ID. For example, some
     * built-in NlpCraft IDs are: <code>nlp:date</code>, <code>nlp:geo</code>
     * <br><br>
     * Few important notes:
     * <ul>
     *      <li>Element IDs starting with <code>nlp:</code> are reserved for built-in system IDs.</li>
     *      <li>
     *          Element ID can be, and often is, used by the human operator during curation to clearly
     *          disambiguate the element in the input sentence instead of relying on synonyms or other
     *          ways of detection.
     *      </li>
     * </ul>
     *
     * @see NCToken#getId()
     * @return Unique ID of this element.
     */
    String getId();

    /**
     * Gets optional group name this element belongs to.
     * <br><br>
     * Elements groups is an important mechanism in implementing {@link NCModel#query(NCQueryContext)} method.
     * Defining proper group for an element is important for proper operation of Short-Term-Memory (STM) in
     * {@link NCConversationContext conversation context}. Specifically, a user token (i.e. found model element)
     * with a given group name will be overridden in the conversation by the more recent token with the same group.
     *
     * @return Optional group name, or {@code null} if not specified. Note that {@code null} group logically
     *      defines a default group.
     * @see NCConversationContext
     */
    String getGroup();

    /**
     * Gets optional user-defined element's metadata. When a {@link NCToken token} for this element
     * is detected in the input this metadata can be accessed via {@link NCToken#getElementMetadata()} method.
     *
     * @return Element's metadata.
     */
    NCMetadata getMetadata();

    /**
     * Gets optional element description.
     *
     * @return Optional element description.
     */
    String getDescription();

    /**
     * Gets optional map of {@link NCValue values} for this element.
     * <br><br>
     * Each element can generally be recognized either by one of its synonyms or values. Elements and their values
     * are analogous to types and instances of that type in programming languages. Each value
     * has a name and optional set of its own synonyms by which that value, and ultimately its element, can be
     * recognized by. Note that value name itself acts as an implicit synonym even when no additional synonyms added
     * for that value.
     * <br><br>
     * Consider this example. A model element {@code x:car} can have:
     * <ul>
     *      <li>
     *          Set of general synonyms:
     *          <code>{transportation|transport|*} {vehicle|car|sedan|auto|automobile|suv|crossover|coupe|truck}</code>
     *      </li>
     *      <li>Set of values:
     *          <ul>
     *              <li>{@code mercedes} with synonyms {@code (mercedes, mercedes-benz, mb, benz)}</li>
     *              <li>{@code bmw} with synonyms {@code (bmw, bimmer)}</li>
     *              <li>{@code chevrolet} with synonyms {@code (chevy, chevrolet)}</li>
     *          </ul>
     *      </li>
     * </ul>
     * With that setup {@code x:car} element will be recognized by any of the following input sub-string:
     * <ul>
     *      <li>{@code transport car}</li>
     *      <li>{@code benz}</li>
     *      <li>{@code automobile}</li>
     *      <li>{@code transport vehicle}</li>
     *      <li>{@code sedan}</li>
     *      <li>{@code chevy}</li>
     *      <li>{@code bimmer}</li>
     *      <li>{@code x:car}</li>
     * </ul>
     *
     * @return Map of value's name and its synonyms or {@code null} if not defined.
     */
    // TODO: javadoc
    List<NCValue> getValues();

    /**
     * Gets optional ID of the immediate parent element. Parent ID allows elements to form into hierarchy
     * and can be used by the user logic in {@link NCModel#query(NCQueryContext)} method.
     * 
     * @return Optional parent element ID, or {@code null} if not specified.
     */
    String getParentId();

    /**
     * Gets the list of synonyms by which this semantic element will be recognized by.
     *
     * @return List of synonyms for this element.
     * @see #getExcludedSynonyms()
     */
    List<String> getSynonyms();

    /**
     * Gets the optional list of synonyms to exclude from the list returned by {@link #getSynonyms()}.
     * Can return empty list or {@code null} to indicate that there are no synonyms to exclude.
     *
     * @return Optional list of synonyms to exclude.
     * @see #getSynonyms() 
     */
    List<String> getExcludedSynonyms();
}
