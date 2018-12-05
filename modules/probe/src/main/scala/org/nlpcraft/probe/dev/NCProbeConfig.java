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

package org.nlpcraft.probe.dev;

import org.nlpcraft.mdllib.*;
import java.io.*;
import java.util.regex.*;

/**
 * Probe configuration container. It is used by {@link NCProbeDevApp#start(NCProbeConfig)} and
 * {@link NCProbeDevApp#start(NCProbeConfig)} methods. Note that most of the probe configuration parameters
 * can be set up via system properties or environment variables:
 * <table class="dl-table" summary="">
 *     <tr>
 *         <th>System Property</th>
 *         <th>Description</th>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_ID}</td>
 *         <td>
 *             ID of the probe.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_TOKEN}</td>
 *         <td>
 *             Company specific probe token. All probes belonging to one company should have
 *             the same token. This token should be kept secure. See account page on the website
 *             to see your company's token.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_EMAIL}</td>
 *         <td>
 *             Optional user email. If provided - only user signed in with that email
 *             will see this probe and thus its models. It should be used during development
 *             and debugging of the model to ensure that unfinished model isn't exposed to other users.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_DOWNLINK}</td>
 *         <td>
 *             Optional custom downlink endpoint in {@code host:port} format. It defaults to
 *             {@code localhost:8081}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_UPLINK}</td>
 *         <td>
 *             Optional custom uplink endpoint in {@code host:port} format. It defaults to
 *  *             {@code localhost:8082}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_SILENT}</td>
 *         <td>
 *              Set to {@code true} to disable verbose probe logging mode.
 *         </td>
 *     </tr>
 * </table>
 *
 * @see NCProbeDevApp
 */
public class NCProbeConfig implements Serializable {
    // Default up-link endpoint.
    private final static String DFLT_UP_LINK = "localhost:8082";

    // Default down-link endpoint.
    private final static String DFLT_DOWN_LINK = "localhost:8081";

    // Email verification pattern.
    private final static Pattern emailPtrn =
        Pattern.compile("(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*:(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*))*)?;\\s*)");

    private String id;
    private String token;
    private String upLink;
    private String downLink;
    private String email;
    private String jarsFolder;
    private NCModelProvider provider;

    /**
     *
     * @param key
     * @return
     */
    private String propOrEnv(String key) {
        String v = System.getProperty(key);

        if (v == null)
            v = System.getenv(key);

        return v;
    }

    /**
     *
     * @param s
     * @return
     */
    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Checks endpoint validity.
     *
     * @param ep endpoint to check.
     * @throws IllegalArgumentException
     */
    private void checkEndpoint(String ep) throws IllegalArgumentException {
        if (isEmpty(ep))
            throw new IllegalArgumentException("Endpoint cannot be null or empty.");

        int idx = ep.indexOf(':');

        String help = "Endpoint must be in 'host:port' or 'ip-addr:port' format.";

        if (idx == -1)
            throw new IllegalArgumentException(String.format("Invalid uplink endpoint: %s. %s", ep, help));
        else
            try {
                int port = Integer.parseInt(ep.substring(idx + 1));

                // 0 to 65536
                if (port < 0 || port > 65536)
                    throw new IllegalArgumentException(String.format("Endpoint port is invalid in: %d. %s", port, help));
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("Endpoint port is invalid in: %s. %s", ep, help));
            }
    }

    /**
     *
     * @param id
     * @return
     */
    private String mkId(String id) {
        String x = isEmpty(id) ? propOrEnv("NLPCRAFT_PROBE_ID") : id;

        if (isEmpty(x))
            throw new IllegalArgumentException("Probe ID cannot be null or empty.");

        return x;
    }

    /**
     *
     * @param tok
     * @return
     */
    private String mkToken(String tok) {
        String x = isEmpty(tok) ? propOrEnv("NLPCRAFT_PROBE_TOKEN") : tok;

        if (isEmpty(x))
            throw new IllegalArgumentException("Probe token cannot be null or empty.");

        return x;
    }

    /**
     *
     * @param email
     * @return
     */
    private String mkEmail(String email) {
        String x = isEmpty(email) ? propOrEnv("NLPCRAFT_PROBE_EMAIL") : email;

        if (!isEmpty(x) && !emailPtrn.matcher(x).matches())
            throw new IllegalArgumentException(String.format("Probe email is not valid: %s", x));

        return x;
    }

    /**
     *
     * @param upLink
     * @return
     */
    private String mkUpLink(String upLink) {
        String x = isEmpty(upLink) ? propOrEnv("NLPCRAFT_PROBE_UPLINK") : upLink;

        if (!isEmpty(x))
            checkEndpoint(x);
        else
            x = DFLT_UP_LINK;

        return x;
    }

    /**
     *
     * @param downLink
     * @return
     */
    private String mkDownLink(String downLink) {
        String x = isEmpty(downLink) ? propOrEnv("NLPCRAFT_PROBE_DOWNLINK") : downLink;

        if (!isEmpty(x))
            checkEndpoint(x);
        else
            x = DFLT_DOWN_LINK;

        return x;
    }

    /**
     * Creates probe configuration.
     *
     * @param id ID of the probe. If {@code null} environment variable or system
     *      property {@code NLPCRAFT_PROBE_ID} will be checked.
     * @param token Company specific probe token. All probes belonging to one company should have the same token.
     *      See admin account page on the website to see your company's token. If {@code null} environment
     *      variable or system property {@code NLPCRAFT_PROBE_TOKEN} will be checked.
     * @param email Optional admin user email. If not {@code null} - only user signed in with that email
     *      will see this probe and thus its models. It should be used during development and debugging of the model
     *      to ensure that unfinished model isn't exposed to other users. If {@code null} environment
     *      variable or system property {@code NLPCRAFT_PROBE_EMAIL} will be checked.
     * @param upLink Optional custom uplink endpoint in {@code host:port} format.
     *      If {@code null} environment variable or system property {@code NLPCRAFT_PROBE_UPLINK} will be checked.
     *      If still {@code null} - default uplink will be used. It defaults to {@code localhost:8082}.
     * @param downLink Optional custom downlink endpoint in {@code host:port} format.
     *      If {@code null} environment variable or system property {@code NLPCRAFT_PROBE_DOWNLINK} will be checked.
     *      If still {@code null} - default downlink will be used. It defaults to {@code localhost:8081}.
     * @param jarsFolder Optional folder to scan for model JARs.
     *      Note that either {@code jarsFolder} or {@code provider} should be specified.
     * @param provider Optional model provider for the probe. If specified, it will be used additionally to
     *      scanning JARs in {@code jarsFolder} folder, if provided. Note that either {@code jarsFolder}
     *      or {@code provider} should be specified.
     */
    public NCProbeConfig(
        String id,
        String token,
        String email,
        String upLink,
        String downLink,
        String jarsFolder,
        NCModelProvider provider) {
        this.id = mkId(id);
        this.token = mkToken(token);
        this.email = mkEmail(email);
        this.upLink = mkUpLink(upLink);
        this.downLink = mkDownLink(downLink);
        this.jarsFolder = jarsFolder;
        this.provider = provider;
    }

    /**
     * Creates probe configuration. This is equivalent to:
     * <pre class="brush: java">
     *     this(id, token, email, null, null, null, provider);
     * </pre>
     *
     * @param id ID of the probe. If {@code null} environment variable or system
     *      property {@code NLPCRAFT_PROBE_ID} will be checked.
     * @param token Company specific probe token. All probes belonging to one company should have the same token.
     *      See admin account page on the website to see your company's token. If {@code null} environment
     *      variable or system property {@code NLPCRAFT_PROBE_TOKEN} will be checked.
     * @param email Optional admin user email. If not {@code null} - only user signed in with that email
     *      will see this probe and thus its models. It should be used during development and debugging of the model
     *      to ensure that unfinished model isn't exposed to other users. If {@code null} environment
     *      variable or system property {@code NLPCRAFT_PROBE_EMAIL} will be checked.
     * @param provider Optional model provider for the probe. If specified, it will be used additionally to
     *      scanning JARs in {@code jarsFolder} folder, if provided. Note that either {@code jarsFolder}
     *      or {@code provider} should be specified.
     */
    public NCProbeConfig(
        String id,
        String token,
        String email,
        NCModelProvider provider) {
        this(id, token, email, null, null, null, provider);
    }

    /**
     * Creates probe configuration. This is equivalent to:
     * <pre class="brush: java">
     *     this(null, null, null, null, null, null, provider);
     * </pre>
     *
     * @param provider Mandatory model provider for the probe.
     */
    public NCProbeConfig(NCModelProvider provider) {
        this(null, null, null, null, null, null, provider);
    }

    /**
     * Creates probe configuration. This is equivalent to:
     * <pre class="brush: java">
     *     this(id, token, email, null, null, jarsFolder, null);
     * </pre>
     *
     * @param id ID of the probe. If {@code null} environment variable or system
     *      property {@code NLPCRAFT_PROBE_ID} will be checked.
     * @param token Company specific probe token. All probes belonging to one company should have the same token.
     *      See admin account page on the website to see your company's token. If {@code null} environment
     *      variable or system property {@code NLPCRAFT_PROBE_TOKEN} will be checked.
     * @param email Optional admin user email. If not {@code null} - only user signed in with that email
     *      will see this probe and thus its models. It should be used during development and debugging of the model
     *      to ensure that unfinished model isn't exposed to other users. If {@code null} environment
     *      variable or system property {@code NLPCRAFT_PROBE_EMAIL} will be checked.
     * @param jarsFolder Optional folder to scan for model JARs.
     *      Note that either {@code jarsFolder} or {@code provider} should be specified.
     */
    public NCProbeConfig(
        String id,
        String token,
        String email,
        String jarsFolder
    ) {
        this(id, token, email, null, null, jarsFolder, null);
    }

    /**
     * Creates probe configuration. This is equivalent to:
     * <pre class="brush: java">
     *     this(null, null, null, null, null, jarsFolder, null);
     * </pre>
     *
     * @param jarsFolder Mandatory folder to scan for model JARs.
     */
    public NCProbeConfig(String jarsFolder) {
        this(null, null, null, null, null, jarsFolder, null);
    }

    /**
     * Gets ID of the probe.
     *
     * @return ID of the probe.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets company specific probe token. All probes belonging to one company should have
     * the same token. This token should be kept secure. See account page on the website
     * to see your company's token.
     *
     * @return Company specific probe token.
     */
    public String getToken() {
        return token;
    }

    /**
     * Gets optional model provider for the probe.
     *
     * @return Optional model provider for the probe.
     */
    public NCModelProvider getProvider() {
        return provider;
    }

    /**
     * Gets uplink endpoint for this probe.
     *
     * @return Uplink endpoint for this probe.
     */
    public String getUpLink() {
        return upLink;
    }

    /**
     * Gets downlink endpoint for this probe.
     *
     * @return Downlink endpoint for this probe.
     */
    public String getDownLink() {
        return downLink;
    }

    /**
     * Get optional user email. It should be used during development and debugging of the model
     * to ensure that unfinished model isn't exposed to other users. 
     *
     * @return Optional user email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets probe ID.
     *
     * @param id Probe ID to set.
     */
    public void setId(String id) {
        assert id != null;

        this.id = id;
    }

    /**
     * Sets company specific probe token. All probes belonging to one company should have
     * the same token. This token should be kept secure. See account page on the website
     * to see your company's token.
     *
     * @param token Company specific probe token to set.
     */
    public void setToken(String token) {
        assert token != null;

        this.token = token;
    }

    /**
     * Sets uplink endpoint for this probe.
     *
     * @param upLink Uplink endpoint to set.
     */
    public void setUpLink(String upLink) {
        assert upLink != null;

        this.upLink = upLink;
    }

    /**
     * Sets downlink endpoint for this probe. 
     *
     * @param downLink Downlink endpoint to set.
     */
    public void setDownLink(String downLink) {
        assert downLink != null;

        this.downLink = downLink;
    }

    /**
     * Gets optional folder to scan for model JARs.
     *
     * @return Optional folder to scan for model JARs.
     */
    public String getJarsFolder() {
        return jarsFolder;
    }

    /**
     * Sets optional folder to scan for model JARs.
     *
     * @param jarsFolder Folder to scan for model JARs.
     */
    public void setJarsFolder(String jarsFolder) {
        this.jarsFolder = jarsFolder;
    }

    /**
     * Set optional admin user email. It should be used during development and debugging of the model
     * to ensure that unfinished model isn't exposed to other users.
     *
     * @param email Admin user email.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Sets optional model provider for the probe.
     *
     * @param provider Optional model provider for the probe.
     */
    public void setProvider(NCModelProvider provider) {
        this.provider = provider;
    }

    @Override
    public String toString() {
        return String.format("Probe configuration [" +
            "id=%s, " +
            "token=%s, " +
            "email=%s, " +
            "upLink=%s, " +
            "downLink=%s, " +
            "jarsFolder=%s, " +
            "provider=%s" +
        "]",
        id, token, email, upLink, downLink, jarsFolder, provider);
    }
}
