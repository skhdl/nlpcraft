/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.mdllib;

import org.nlpcraft.mdllib.intent.*;
import org.nlpcraft.util.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

/**
 * Model query result returned from {@link NCModel#query(NCQueryContext)} method. Query result consists of the
 * text body and the type. The type is similar in notion to MIME types. Query result is what being sent back to
 * the user client like web browser or REST client. The following is the list of supported result types. These
 * types are directly accessible when used with REST API and automatically rendered by webapp:
 * <table summary="" class="dl-table">
 *     <tr>
 *         <th>Result Type</th>
 *         <th>Factory Method</th>
 *     </tr>
 *     <tr>
 *         <td><code>ask</code></td>
 *         <td>{@link #ask(String)}</td>
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
 *         <td><code>json/speech</code></td>
 *         <td>{@link #jsonSpeech(String)}</td>
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
 * Note that all of these types have specified meaning <b>only</b> for NLPCraft webapp where each of these result types
 * is rendered and processed in a special way. When used via REST API the responsibility to render and process
 * different result types will rest on REST client itself and may differ from the default processing of NLPCraft
 * webapp. For example, the REST client interfacing between NLPCraft and Amazon Alexa or Apple HomeKit can only
 * accept {@code text} result type and ignore everything else.
 */
public class NCQueryResult implements Serializable {
    private String body;
    private String type;
    private NCVariant var;
    private Map<String, Object> metadata;
    
    /**
     * Creates {@code text} result.
     *
     * @param txt Textual result. Text interpretation will be defined by the client receiving this result. In
     *      NLPCraft webapp, it will be rendered as {@link #html(String)} result.
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
     *      Webapp will render it in {@code <div></div>} element.
     * @return Newly created query result.
     */
    public static NCQueryResult html(String html) {
        return new NCQueryResult(html, "html");
    }
    
    /**
     * Creates {@code ask} result. This is a special result type that is used to ask
     * end user for additional or missing information. This is similar to {@link #html(String)} but
     * on webapp it is rendered differently.
     *
     * @param html Minimal markup HTML. This assumes only minimal
     *      HTML text formatting markup: {@code <i> <b> <u> <a> <br> <strong> <em> <mark> <small> <del> <ins> <sub> <sup>}.
     *      Webapp will render it in {@code <div></div>} element.
     * @return Newly created query result.
     */
    public static NCQueryResult ask(String html) {
        return new NCQueryResult(html, "ask");
    }
    
    /**
     * Creates {@code html/raw} result. Result with any arbitrary HTML code snippet. For obvious security reasons
     * models that return this type of result should be verified and trusted.
     *
     * @param html Any raw HTML code snippet that will be inserted into result HTML page on webapp.
     * @return Newly created query result.
     */
    public static NCQueryResult htmlRaw(String html) {
        return new NCQueryResult(html, "html/raw");
    }
    
    /**
     * Creates {@code json/speech} result. Accepts JSON configuration string that will be processed
     * by <a href="https://w3c.github.io/speech-api/webspeechapi.html">Web Speech</a> based synthesis:
     * <pre class="brush: js">
     * {
     *      "text": "Something to say",
     *      "lang": "en-US"
     * }
     * </pre>
     * Only two of <a href="https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesisUtterance">SpeechSynthesisUtterance</a>
     * parameters are accepted (others will be set to default values):
     * <ul>
     *     <li>{@code SpeechSynthesisUtterance.lang} - language of the utterance,</li>
     *     <li>{@code SpeechSynthesisUtterance.text} - text that will be synthesised when the utterance is spoken.</li>
     * </ul>
     *
     * @param json JSON configuration string.
     * @return Newly created query result.
     */
    public static NCQueryResult jsonSpeech(String json) {
        return new NCQueryResult(json, "json/speech");
    }
    
    /**
     * Shortcut method for {@link #jsonSpeech(String)} with {@code en-US} language.
     *
     * @param txt Text that will be synthesised when the <a href="https://w3c.github.io/speech-api/webspeechapi.html">Web Speech</a>
     *      utterance is spoken.
     * @return Newly created query result.
     */
    public static NCQueryResult enUsSpeak(String txt) {
        return new NCQueryResult(String.format(
            "{" +
                "\"text\": \"%s\", " +
                "\"lang\": \"en-US\"" +
                "}", txt), "json/speech");
    }
    
    /**
     * Creates {@code json} result. JSON will be properly rendered on webapp.
     *
     * @param json Any JSON string to be rendered on the client.
     * @return Newly created query result.
     */
    public static NCQueryResult json(String json) {
        return new NCQueryResult(json, "json");
    }
    
    /**
     * Creates {@code json/table} result. This allows for a simplified HTML table presentation rendered by webapp:
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
                        "\"resBody\": \"" + NCGlobals.escapeJson(part.getBody()) + "\"" +
                        "}").collect(Collectors.joining(",")) +
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
        
        // TODO: add future checks here.
        if (!typeLc.equals("html") &&
            !typeLc.equals("json") &&
            !typeLc.equals("text") &&
            !typeLc.equals("json/google/map") &&
            !typeLc.equals("json/multipart") &&
            !typeLc.equals("json/table") &&
            !typeLc.equals("json/speech") &&
            !typeLc.equals("ask") &&
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
     * Gets optional sentence variant associated with this result.
     *
     * @return Sentence variant associated with this result or {@code null}.
     */
    public NCVariant getVariant() {
        return var;
    }

    /**
     * Sets optional sentence variant this result refers to.
     * <br><br>
     * Note that in general a user input can have one or more possible
     * parsing {@link NCSentence#variants() variants}. Setting the specific variant that was the origin of this result
     * is optional but improves the self-learning capabilities of the system when provided. Note also that
     * sub-systems like {@link NCIntentSolver intent-based solver} will set the proper variant automatically.
     *
     * @param var Sentence variant to set.
     * @return This instance of chaining calls.
     */
    public NCQueryResult setVariant(NCVariant var) {
        this.var = var;

        return this;
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
    
    /**
     * Gets metadata.
     *
     * @return Metadata.
     */
    public Map<String, Object> getMetadata() {
        return metadata != null ? metadata : Collections.emptyMap();
    }
    
    /**
     * Sets metadata.
     *
     * @param metadata Metadata
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
