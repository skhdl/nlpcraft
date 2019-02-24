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

import org.nlpcraft.common.util.NCUtils;
import org.nlpcraft.model.intent.NCIntentSolver;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

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
 *         <td><code>html/raw</code></td>
 *         <td>{@link #htmlRaw(String)}</td>
 *     </tr>
 *     <tr>
 *         <td><code>json</code></td>
 *         <td>{@link #json(String)}</td>
 *     </tr>
 *     <tr>
 *         <td><code>json/multipart</code></td>
 *         <td>{@link #jsonMultipart(NCQueryResult...)}</td>
 *     </tr>
 *     <tr>
 *         <td><code>json/table</code></td>
 *         <td>{@link #jsonTable(String)}</td>
 *     </tr>
 *     <tr>
 *         <td><code>json/google/map</code></td>
 *         <td>{@link #jsonGmap(String)}</td>
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
        return new NCQueryResult(txt, "text");
    }
    
    /**
     * Creates {@code html} result.
     *
     * @param html Minimal markup HTML. Unlike {@link #htmlRaw(String)} this assumes only minimal
     *      HTML text formatting markup: {@code <i> <b> <u> <a> <br> <strong> <em> <mark> <small> <del> <ins> <sub> <sup>}.
     * @return Newly created query result.
     */
    public static NCQueryResult html(String html) {
        return new NCQueryResult(html, "html");
    }
    
    /**
     * Creates {@code html/raw} result. Result with any arbitrary HTML code snippet. For obvious security reasons
     * models that return this type of result should be verified and trusted.
     *
     * @param html Any raw HTML code snippet.
     * @return Newly created query result.
     */
    public static NCQueryResult htmlRaw(String html) {
        return new NCQueryResult(html, "html/raw");
    }
    
    /**
     * Creates {@code json} result.
     *
     * @param json Any JSON string to be rendered on the client.
     * @return Newly created query result.
     */
    public static NCQueryResult json(String json) {
        return new NCQueryResult(json, "json");
    }
    
    /**
     * Creates {@code json/table} result. Here's the format of the result JavaScript snippet:
     * <pre class="brush: js">
     * {
     *      "border": true, // Whether or not table has border.
     *      "title": "Title:", // Optional title for the table.
     *      "background": "#2f4963", // Background color.
     *      "borderColor": "#607d8b", // Border color if border is enabled.
     *      "columns": [ // Array of columns.
     *          {
     *              "header": "Header", // Any arbitrary HTML snippet (including inline CSS styling).
     *              "type": "String" // Type of the column (current ignored).
     *          }
     *      ],
     *      "rows": [ // Array of rows.
     *          ["one"], // Any arbitrary HTML snippet (including inline CSS styling).
     *          ["two"] // Any arbitrary HTML snippet (including inline CSS styling).
     *      ]
     * }
     * </pre>
     *
     * @param json JSON table specification.
     * @return Newly created query result.
     */
    public static NCQueryResult jsonTable(String json) {
        return new NCQueryResult(json, "json/table");
    }
    
    /**
     * Creates {@code json/google/map} result. This allows for simplified usage of static
     * <a href="https://developers.google.com/maps/documentation/static-maps/intro">Google Map API</a>:
     * <pre class="brush: js">
     * {
     *      "cssStyle": { // CSS style for enclosing image for static Google Map.
     *          "width": "600px",
     *          "height": "300px"
     *      },
     *      "gmap": { // URL parameters according to https://developers.google.com/maps/documentation/static-maps/intro
     *          "center": "20.000,30.000",
     *          "zoom": 4,
     *          "scale": 2,
     *          "size": "600x300",
     *          "maptype": "terrain",
     *          "markers": "color:red|20.000,30.000"
     *      }
     * }
     * </pre>
     *
     * @param gmapJsonCfg JSON string representing CSS styling and URL parameters and their values according to
     *      <a href="https://developers.google.com/maps/documentation/static-maps/intro">https://developers.google.com/maps/documentation/static-maps/intro</a>.
     * @return Newly created query result.
     */
    public static NCQueryResult jsonGmap(String gmapJsonCfg) {
        return new NCQueryResult(gmapJsonCfg, "json/google/map");
    }
    
    /**
     * Creates {@code json/multipart} result. Multipart result is a list (container) of other results. Note
     * that nesting or recursion of results are not supported.
     *
     * @param parts Other results making up this multipart result.
     * @return Newly created query result.
     */
    public static NCQueryResult jsonMultipart(NCQueryResult... parts) {
        return new NCQueryResult(
            "[" +
                Arrays.stream(parts).map(part ->
                    "{" +
                        "\"resType\": \"" + part.getType() + "\", " +
                        "\"resBody\": \"" + NCUtils.escapeJson(part.getBody()) + "\"" +
                        "}")
                    .collect(Collectors.joining(",")) +
            "]",
            "json/multipart"
        );
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
    private String checkType(String type) {
        String typeLc = type.toLowerCase();
        
        if (!typeLc.equals("html") &&
            !typeLc.equals("json") &&
            !typeLc.equals("text") &&
            !typeLc.equals("json/google/map") &&
            !typeLc.equals("json/multipart") &&
            !typeLc.equals("json/table") &&
            !typeLc.equals("html/raw"))
            throw new IllegalArgumentException("Invalid result type: " + type);
        else
            return typeLc;
    }
    
    /**
     * No-arg constructor.
     */
    public NCQueryResult() {
        // No-op.
    }

    /**
     * Sets result body.
     *
     * @param body Result body.
     * @return This instance of chaining calls.
     */
    public NCQueryResult setBody(String body) {
        this.body = body;

        return this;
    }

    /**
     * Set result type.
     *
     * @param type Result type.
     * @throws IllegalArgumentException Thrown if type of invalid.
     * @return This instance of chaining calls.
     */
    public NCQueryResult setType(String type) {
        this.type = checkType(type);

        return this;
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
