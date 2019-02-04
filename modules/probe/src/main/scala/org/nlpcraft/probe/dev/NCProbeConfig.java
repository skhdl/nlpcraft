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

package org.nlpcraft.probe.dev;

import org.nlpcraft.mdllib.*;
import java.io.*;

/**
 * Probe configuration bean. In most cases you should use {@link NCProbeConfigBuilder} factory to create
 * an instance of this class as it would properly handle various default values and validation.
 */
public class NCProbeConfig implements Serializable {
    private String id;
    private String token;
    private String upLink;
    private String downLink;
    private String jarsFolder;
    private NCModelProvider provider;
    private boolean versionAskEnabled;

    /**
     * Gets probe unique ID.
     *
     * @return Probe ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets probe unique ID.
     *
     * @param id Probe ID to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets probe token. Probe and server should be configured with the same probe token.
     * If you change the probe token here make sure to change the probe token on the server
     * to the same value.
     * <br><br>
     * Probe tokens should be kept secure. Note that both probe and the server come with
     * the same default token so everything works out-of-the-box, however any default
     * probe can technically connect to any default server.
     *
     * @return Probe token.
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets probe token. Probe and server should be configured with the same probe token.
     * If you change the probe token here make sure to change the probe token on the server
     * to the same value.
     * <br><br>
     * Probe tokens should be kept secure. Note that both probe and the server come with
     * the same default token so everything works out-of-the-box, however any default
     * probe can technically connect to any default server.
     *
     * @param token Probe token to set.
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Gets probe uplink <code>host:port</code> endpoint.
     *
     * @return Probe uplink <code>host:port</code> endpoint.
     */
    public String getUpLink() {
        return upLink;
    }

    /**
     * Sets uplink <code>host:port</code> endpoint for this probe.
     *
     * @param upLink Uplink <code>host:port</code> endpoint to set.
     */
    public void setUpLink(String upLink) {
        this.upLink = upLink;
    }

    /**
     * Gets probe downlink <code>host:port</code> endpoint.
     *
     * @return Probe downlink <code>host:port</code> endpoint.
     */
    public String getDownLink() {
        return downLink;
    }

    /**
     * Sets downlink <code>host:port</code> endpoint for this probe.
     *
     * @param downLink Downlink <code>host:port</code> endpoint to set.
     */
    public void setDownLink(String downLink) {
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
     * Sets optional folder to scan for model JARs. Note that either {@link #setProvider(NCModelProvider) provider}
     * or JAR folder must be set.
     *
     * @param jarsFolder Folder to scan for model JARs.
     */
    public void setJarsFolder(String jarsFolder) {
        this.jarsFolder = jarsFolder;
    }

    /**
     * Gets optional model provider for the probe
     * 
     * @return Optional model provider for the probe.
     */
    public NCModelProvider getProvider() {
        return provider;
    }

    /**
     * Sets optional model provider for the probe. Note that either model provider
     * or {@link #setJarsFolder(String) JAR folder} must be set.
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
            "upLink=%s, " +
            "downLink=%s, " +
            "jarsFolder=%s, " +
            "provider=%s" +
            "]",
            id, token, upLink, downLink, jarsFolder, provider);
    }
}
