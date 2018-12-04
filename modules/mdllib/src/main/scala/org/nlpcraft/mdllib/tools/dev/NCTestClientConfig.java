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

package org.nlpcraft.mdllib.tools.dev;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.function.Supplier;

/**
 * Configuration container used by {@link NCTestClientBuilder}. Note that this class automatically reads the
 * following system properties and environment variables:
 * <ul>
 *     <li><code>NLPCRAFT_PROBE_TOKEN</code> for {@link #setProbeToken(String) probe token}</li>
 *     <li><code>NLPCRAFT_PROBE_EMAIL</code> for {@link #setEmail(String) user email}</li>
 *     <li><code>NLPCRAFT_TEST_BASEURL</code> for {@link #setBaseUrl(String) base REST API URL}</li>
 * </ul>
 * Note that {@link #setProbeToken(String) probe token} and {@link #setEmail(String) user email} are
 * the only two mandatory configuration properties.
 */
public class NCTestClientConfig {
    /** Default public REST API URL (endpoint). */
    public static final String DFLT_BASEURL = "https://localhost:8081/";
    
    private Supplier<CloseableHttpClient> cliSup;
    private String probeTkn;
    private String email;
    private String baseUrl;
    
    /**
     * Creates new configuration container with given parameters. Note that this constructor will use
     * default base URL for REST APIs.
     *
     * @param cliSup Custom HTTP client supplier.
     * @param probeTkn Probe token. Note that probe token is a secure per-company token available only to
     *      registered user with administrative privileges. It must be kept in a secure fashion. It can also
     *      be set via {@code NLPCRAFT_PROBE_TOKEN} system property or environment variable which is a
     *      preferred way for security reasons.
     * @param email User email under which account the test will be performed. The account should be active
     *      and have access to the data source(s) used for testing.
     */
    public NCTestClientConfig(Supplier<CloseableHttpClient> cliSup, String probeTkn, String email) {
        checkNotNull(cliSup, "cliSup");
        checkNotNull(probeTkn, "probeTkn");
        checkNotNull(email, "email");
        
        this.cliSup = cliSup;
        this.probeTkn = probeTkn;
        this.email = email;
    
        String baseUrl = read("NLPCRAFT_TEST_BASEURL", false);
        this.baseUrl = baseUrl != null ? baseUrl : DFLT_BASEURL;
    }
    
    /**
     * Creates new configuration container with given parameters. Note that this constructor will use
     * default base URL for REST APIs and default HTTP client.
     *
     * @param probeTkn Probe token. Note that probe token is a secure per-company token available only to
     *      registered user with administrative privileges. It must be kept in a secure fashion. It can also
     *      be set via {@code NLPCRAFT_PROBE_TOKEN} system property or environment variable which is a
     *      preferred way for security reasons.
     * @param email User email under which account the test will be performed. The account should be active
     *      and have access to the data source(s) used for testing.
     */
    public NCTestClientConfig(String probeTkn, String email) {
        this(HttpClients::createDefault, probeTkn, email);
    }
    
    /**
     * Creates new configuration container with probe token and user email read from {@code NLPCRAFT_PROBE_TOKEN}
     * and {@code NLPCRAFT_PROBE_EMAIL} system properties or environment variables. Note that this constructor
     * will use default base URL for REST APIs and default HTTP client.
     */
    public NCTestClientConfig() {
        this(
            HttpClients::createDefault,
            read("NLPCRAFT_PROBE_TOKEN", true),
            read("NLPCRAFT_PROBE_EMAIL", true)
        );
    }
    
    /**
     * Creates new configuration container with probe token and user email read from {@code NLPCRAFT_PROBE_TOKEN}
     * and {@code NLPCRAFT_PROBE_EMAIL} system properties or environment variables. Note that this constructor
     * will use default base URL for REST APIs.
     *
     * @param cliSup Custom HTTP client supplier.
     */
    public NCTestClientConfig(Supplier<CloseableHttpClient> cliSup) {
        this(
            cliSup,
            read("NLPCRAFT_PROBE_TOKEN", true),
            read("NLPCRAFT_PROBE_EMAIL", true)
        );
    }
    
    /**
     * Checks parameter value.
     *
     * @param o Parameter value.
     * @param name Parameter name.
     */
    private static void checkNotNull(Object o, String name) {
        if (o == null)
            throw new IllegalArgumentException(String.format("Parameter cannot be null: %s", name));
    }
    
    /**
     * Reads property.
     *
     * @param name Property name.
     * @param mandatory Property value `mandatory` flag.
     * @return Property value.
     */
    private static String read(String name, boolean mandatory) {
        String v = (String)System.getProperties().get(name);
        
        if (v == null)
            v = System.getenv().get(name);
        
        if (v == null && mandatory)
            throw new IllegalStateException(String.format("System property %s is not defined.", name));
        
        return v;
    }

    /**
     * Gets previously set probe token. Note that probe token is a secure per-company token available only to
     * registered user with administrative privileges. It must be kept in a secure fashion. It can also
     * be set via {@code NLPCRAFT_PROBE_TOKEN} system property or environment variable which is a
     * preferred way for security reasons.
     *
     * @return Previously set probe token.
     */
    public String getProbeToken() {
        return probeTkn;
    }

    /**
     * Gets previously set user email for the account the test will be performed with.
     * The account should be active and have access to the data source(s) used for testing.
     *
     * @return Previously set user email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Gets the previously set base URL for NLPCraft REST APIs.
     *
     * @return Previously set base URL for NLPCraft REST APIs.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Sets optional base URL for NLPCraft REST APIs. Note that
     * this property can be set via {@code NLPCRAFT_TEST_BASEURL} system property or environment variable.
     *
     * @param baseUrl Base URL for NLPCraft REST APIs.
     * @see #DFLT_BASEURL
     */
    public void setBaseUrl(String baseUrl) {
        checkNotNull(baseUrl, "baseUrl");
        
        this.baseUrl = baseUrl;

        if (!this.baseUrl.endsWith("/"))
            this.baseUrl += "/";
    }

    /**
     * Gets previously set custom HTTP client supplier.
     *
     * @return Previously set custom HTTP client supplier.
     */
    public Supplier<CloseableHttpClient> getClientSupplier() {
        return cliSup;
    }

    /**
     * Sets optional custom HTTP client supplier. In most cases you don't need to set this property unless
     * you want a low-level control of HTTP traffic.
     * 
     * @param cliSup Custom HTTP client supplier.
     */
    public void setClientSupplier(Supplier<CloseableHttpClient> cliSup) {
        this.cliSup = cliSup;
    }

    /**
     * Sets a mandatory probe token. Note that probe token is a secure per-company token available only to
     * registered user with administrative privileges. It must be kept in a secure fashion. It can also
     * be set via {@code NLPCRAFT_PROBE_TOKEN} system property or environment variable which is a
     * preferred way for security reasons.
     *
     * @param probeTkn Probe token to set.
     */
    public void setProbeToken(String probeTkn) {
        this.probeTkn = probeTkn;
    }

    /**
     * Sets mandatory user email. Note that the account denoted by this email should be active
     * and have access to the data source(s) used for testing.
     *
     * @param email User email to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }
}
