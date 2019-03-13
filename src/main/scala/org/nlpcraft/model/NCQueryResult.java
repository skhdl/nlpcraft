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

import org.nlpcraft.common.NCException;
import org.nlpcraft.common.util.NCUtils;
import org.nlpcraft.model.intent.NCIntentSolver;

import java.io.Serializable;

/**
 * Model query result returned from {@link NCModel#query(NCQueryContext)} method. Query result consists of the
 * text body and the type. The type is similar in notion to MIME types. The following is the list of supported
 * result types:
 * <table summary="" class="dl-table">
 *     <tr>
 *         <th>Result Type</th>
 *         <th>Factory Method</th>
 *     </tr>
 *     <tr>
 *         <td><code>text</code></td>
 *         <td>{@link #text(String)}</td>
 *     </tr>
 *     <tr>
 *         <td><code>html</code></td>
 *         <td>{@link #html(String)}</td>
 *     </tr>
 *     <tr>
 *         <td><code>json</code></td>
 *         <td>{@link #json(String)}</td>
 *     </tr>
 *     <tr>
 *         <td><code>yaml</code></td>
 *         <td>{@link #yaml(String)}</td>
 *     </tr>
 * </table>
 * Note that all of these types have specific meaning <b>only</b> for REST applications that interpret them
 * accordingly. For example, the REST client interfacing between NLPCraft and Amazon Alexa or Apple HomeKit can only
 * accept {@code text} result type and ignore everything else.
 */
public class NCQueryResult implements Serializable {
    private String body;
    private String type;
    private NCVariant var;
    
    /**
     * Creates {@code text} result.
     *
     * @param txt Textual result. Text interpretation will be defined by the client receiving this result.
     * @return Newly created query result.
     */
    public static NCQueryResult text(String txt) {
        checkNull("txt", txt);
        
        return new NCQueryResult(txt, "text");
    }
    
    /**
     * Creates {@code html} result.
     *
     * @param html HTML markup.
     * @return Newly created query result.
     */
    public static NCQueryResult html(String html) {
        checkNull("html", html);
        
        return new NCQueryResult(html, "html");
    }
    
    /**
     * Creates {@code json} result.
     *
     * @param json Any JSON string to be rendered on the client.
     * @return Newly created query result.
     */
    public static NCQueryResult json(String json) {
        checkNull("json", json);
        
        // Validation.
        try {
            NCUtils.js2Map(json);
        }
        catch (NCException e) {
            throw new IllegalArgumentException("Invalid json value: " + json);
        }
        
        return new NCQueryResult(json, "json");
    }
    
    /**
     * Creates {@code yaml} result.
     *
     * @param yaml Any YAML string to be rendered on the client.
     * @return Newly created query result.
     */
    public static NCQueryResult yaml(String yaml) {
        checkNull("yaml", yaml);
        
        return new NCQueryResult(yaml, "yaml");
    }
    
    /**
     * Creates new result with given body and type.
     *
     * @param body Result body.
     * @param type Result type.
     * @throws IllegalArgumentException Thrown if type of invalid.
     */
    private NCQueryResult(String body, String type) {
        assert body != null;
        assert type != null;
        
        this.body = body;
        this.type = checkType(type);
    }
    
    /**
     *
     * @param type Type to check.
     * @throws IllegalArgumentException Thrown if type of invalid.
     */
    private static String checkType(String type) {
        String typeLc = type.toLowerCase();
        
        if (!typeLc.equals("html") &&
            !typeLc.equals("json") &&
            !typeLc.equals("yaml") &&
            !typeLc.equals("text")
        )
            throw new IllegalArgumentException("Invalid result type: " + type);
        else
            return typeLc;
    }
    
    /**
     *
     * @param name
     * @param val
     * @return
     */
    private static void checkNull(String name, String val) {
        if (val == null)
            throw new IllegalArgumentException("Invalid null value: " + name);
    }
    
    /**
     * No-arg constructor.
     */
    public NCQueryResult() {
        // No-op.
    }

    /**
     * Gets optional sentence variant associated with this result.
     * <br><br>
     * Note that in general a user input can have one or more possible
     * parsing {@link NCSentence#getVariants() variants}. Setting the specific variant that was the origin of
     * this result is required for proper conversation context maintenance. Note also that
     * sub-systems like {@link NCIntentSolver intent-based solver} will set the proper variant automatically.
     *
     * @return Sentence variant associated with this result or {@code null}.
     * @see NCSentence#getVariants()
     */
    public NCVariant getVariant() {
        return var;
    }
    
    /**
     * Sets optional sentence variant this result originated from.
     * <br><br>
     * Note that in general a user input can have one or more possible
     * parsing {@link NCSentence#getVariants() variants}. Setting the specific variant that was the origin of
     * this result is required for proper conversation context maintenance. Note also that
     * sub-systems like {@link NCIntentSolver intent-based solver} will set the proper variant automatically.
     *
     * @param var Sentence variant to set.
     * @return This instance of chaining calls.
     * @see NCSentence#getVariants()
     */
    public NCQueryResult setVariant(NCVariant var) {
        this.var = var;
        
        return this;
    }
    
    /**
     * Gets result type.
     *
     * @return Result type.
     */
    public String getType() {
        return type;
    }
    
    /**
     * Gets result body.
     *
     * @return Result body.
     */
    public String getBody() {
        return body;
    }
}
