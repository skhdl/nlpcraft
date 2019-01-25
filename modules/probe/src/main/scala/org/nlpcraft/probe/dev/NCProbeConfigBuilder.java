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
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
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
import org.nlpcraft.util.*;

/**
 * Convenient builder for {@link NCProbeConfig} instances. Note that most of the configuration values
 * have defaults except for {@link #setProvider(NCModelProvider)} model provider} and
 * {@link #setJarsFolder(String) JARs folder}. Note also that the following system properties
 * or environment variables can be used to set up the configuration values from outside of the code:
 * <table class="dl-table" summary="">
 *     <tr>
 *         <th>System Property</th>
 *         <th>Description</th>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_ID}</td>
 *         <td>
 *             ID of the probe. See {@link #setId(String)} for more details.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_TOKEN}</td>
 *         <td>
 *             Probe token. See {@link #setToken(String)} for more details.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_DOWNLINK}</td>
 *         <td>
 *             Custom downlink endpoint. See {@link #setDownLink(String)}  for more details.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_UPLINK}</td>
 *         <td>
 *             Custom uplink endpoint. See {@link #setUpLink(String)} for more details.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_JARS}</td>
 *         <td>
 *             JARs folder. See {@link #setJarsFolder(String)} for more details.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code NLPCRAFT_PROBE_SILENT}</td>
 *         <td>
 *              Set to {@code true} to disable verbose probe logging mode.
 *         </td>
 *     </tr>
 * </table>
 */
public class NCProbeConfigBuilder {
    // Default up-link endpoint.
    private final static String DFLT_UP_LINK = "localhost:8082";

    // Default down-link endpoint.
    private final static String DFLT_DOWN_LINK = "localhost:8081";

    // Default probe token shared by the server as well.
    private final static String DFLT_PROBE_TOKEN = "3141592653589793";
    
    /** */
    private NCProbeConfig impl;

    /**
     * Null or empty check.
     *
     * @param s String to check.
     */
    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Checks endpoint validity.
     *
     * @param ep endpoint to check.
     */
    private static void checkEndpoint(String ep) throws IllegalArgumentException {
        if (isEmpty(ep))
            throw new IllegalArgumentException("Endpoint cannot be null or empty.");

        int idx = ep.indexOf(':');

        String help = "Endpoint must be in 'host:port' or 'ip-addr:port' format.";

        if (idx == -1)
            throw new IllegalArgumentException(String.format("Invalid uplink endpoint: %s. %s", ep, help));

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
     * @param key
     * @return
     */
    private static String propOrEnv(String key) {
        String v = System.getProperty(key);

        return v != null ? v : System.getenv(key);
    }

    /**
     *
     */
    private NCProbeConfigBuilder() {
        impl = new NCProbeConfig();

        // Set defaults.
        impl.setId(NCGlobals.genGuid());
        impl.setToken(DFLT_PROBE_TOKEN);
        impl.setUpLink(DFLT_UP_LINK);
        impl.setDownLink(DFLT_DOWN_LINK);

        // Check overrides from system and environmental variables.
        String x = propOrEnv("NLPCRAFT_PROBE_ID");

        if (!isEmpty(x))
            setId(x);

        x = propOrEnv("NLPCRAFT_PROBE_TOKEN");

        if (!isEmpty(x))
            setToken(x);

        x = propOrEnv("NLPCRAFT_PROBE_UPLINK");

        if (!isEmpty(x))
            setUpLink(x);

        x = propOrEnv("NLPCRAFT_PROBE_DOWNLINK");

        if (!isEmpty(x))
            setDownLink(x);

        x = propOrEnv("NLPCRAFT_PROBE_JARS");

        if (!isEmpty(x))
            setJarsFolder(x);
    }

    /**
     *
     * @return Newly created and validated probe configuration.
     * @throws IllegalArgumentException Thrown in case of any validation errors.
     */
    public NCProbeConfig build() throws IllegalArgumentException {
        if (isEmpty(impl.getId()))
            throw new IllegalArgumentException("Probe ID is not provided.");

        if (isEmpty(impl.getToken()))
            throw new IllegalArgumentException("Probe token is not provided.");

        if (isEmpty(impl.getUpLink()))
            throw new IllegalArgumentException("Probe uplink is not provided.");

        checkEndpoint(impl.getUpLink());

        if (isEmpty(impl.getDownLink()))
            throw new IllegalArgumentException("Probe downlink is not provided.");

        checkEndpoint(impl.getDownLink());

        if (impl.getProvider() == null && isEmpty(impl.getJarsFolder()))
            throw new IllegalArgumentException("Neither model provider nor JAR folder is provided.");

        return impl;
    }

    /**
     * Creates new probe configuration with all default values set.
     *
     * @return New probe configuration with all default values set.
     */
    public static NCProbeConfigBuilder newConfig() {
        return new NCProbeConfigBuilder();
    }

    /**
     * Creates new probe configuration with all default values set and given model provider.
     * This is a convenient shortcut factory constructor for usage in in-process probes.
     *
     * @param provider Model provider to set.
     * @return New probe configuration with all default values set.
     */
    public static NCProbeConfigBuilder newConfig(NCModelProvider provider) {
        return newConfig().setProvider(provider);
    }

    /**
     * Sets probe ID.
     *
     * @param id Probe ID to set.
     */
    public NCProbeConfigBuilder setId(String id) {
        impl.setId(id);

        return this;
    }

    /**
     * Sets probe token. Probe and server should be configured with the same probe token. If you change
     * the probe token here make sure to change the probe token on the server to the same value.
     * <br><br>
     * Probe tokens should be kept secure. Note that both probe and the server come with
     * the same default token so everything works out-of-the-box but not secure as any default
     * probe can technically connect to any default server.
     *
     * @param token Probe token to set.
     */
    public NCProbeConfigBuilder setToken(String token) {
        impl.setToken(token);

        return this;
    }

    /**
     * Sets uplink endpoint for this probe.
     *
     * @param upLink Uplink endpoint to set.
     */
    public NCProbeConfigBuilder setUpLink(String upLink) {
        impl.setUpLink(upLink);

        return this;
    }

    /**
     * Sets downlink endpoint for this probe.
     *
     * @param downLink Downlink endpoint to set.
     */
    public NCProbeConfigBuilder setDownLink(String downLink) {
        impl.setDownLink(downLink);

        return this;
    }

    /**
     * Sets optional folder to scan for model JARs.
     *
     * @param jarsFolder Folder to scan for model JARs.
     */
    public NCProbeConfigBuilder setJarsFolder(String jarsFolder) {
        impl.setJarsFolder(jarsFolder);

        return this;
    }

    /**
     * Sets optional model provider for the probe.
     *
     * @param provider Optional model provider for the probe.
     */
    public NCProbeConfigBuilder setProvider(NCModelProvider provider) {
        impl.setProvider(provider);

        return this;
    }
}
