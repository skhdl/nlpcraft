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

package org.nlpcraft.examples.misc.geo.keycdn;

import com.google.gson.Gson;
import org.nlpcraft.examples.misc.geo.keycdn.beans.GeoDataBean;
import org.nlpcraft.examples.misc.geo.keycdn.beans.ResponseBean;
import org.nlpcraft.model.NCSentence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Geo data finder.
 *
 * There are following restrictions to simplify example:
 *
 * 1. Finder's cache is never cleared.
 * 2. Implementation is not thread safe.
 * 3. Errors just forwarded to error console.
 * 4. Cache, which used to avoid rate-limiting requests (3 requests per second, see https://tools.keycdn.com/geo),
 *    applied only to successfully received GEO data.
 */
public class GeoManager {
    private static final String URL = "https://tools.keycdn.com/geo.json?host=";
    private static final Gson GSON = new Gson();
    
    private final Map<String, GeoDataBean> cache = new HashMap<>();
    private String externalIp = null;
    
    /**
     * Gets optional geo data by given sentence.
     *
     * @param sen Sentence.
     * @return Geo data. Optional.
     */
    public Optional<GeoDataBean> get(NCSentence sen) {
        if (!sen.getRemoteAddress().isPresent()) {
            System.err.println("Geo data can't be found because remote address is not available in the sentence.");

            return Optional.empty();
        }
    
        String host = sen.getRemoteAddress().get();
    
        if (host.equalsIgnoreCase("localhost") || host.equalsIgnoreCase("127.0.0.1")) {
            if (externalIp == null) {
                try {
                    externalIp = getExternalIp();
                }
                catch (IOException e) {
                    System.err.println("External IP cannot be detected for localhost.");
        
                    return Optional.empty();
                }
            }
    
            host = externalIp;
        }
    
        try {
            GeoDataBean geo = cache.get(host);
    
            if (geo != null)
                return Optional.of(geo);
            
            HttpURLConnection conn = (HttpURLConnection)(new URL(URL + host).openConnection());
    
            // This service requires "User-Agent" property for some reasons.
            conn.setRequestProperty("User-Agent", "rest");
    
            try (InputStream in = conn.getInputStream()) {
                String enc = conn.getContentEncoding();
    
                InputStream stream = enc != null && enc.equals("gzip") ? new GZIPInputStream(in) : in;
                
                ResponseBean resp =
                    GSON.fromJson(new BufferedReader(new InputStreamReader(stream)), ResponseBean.class);
        
                if (!resp.getStatus().equals("success"))
                    throw new IOException(
                        MessageFormat.format(
                            "Unexpected response [status={0}, description={1}]",
                            resp.getStatus(),
                            resp.getDescription())
                    );
        
                geo = resp.getData().getGeo();
                
                cache.put(host, geo);
        
                return Optional.of(geo);
            }
        }
        catch (Exception e) {
            System.err.println(
                MessageFormat.format(
                    "Unable to answer due to IP Location Finder (keycdn) error for host: {0}",
                    host
                )
            );
    
            e.printStackTrace(System.err);
    
            return Optional.empty();
        }
    }
    
    /**
     * Gets external IP.
     *
     * @return External IP.
     * @throws IOException If any errors occur.
     */
    private static String getExternalIp() throws IOException {
        try (BufferedReader in =
            new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()))) {
            return in.readLine();
        }
    }
    
    /**
     * Gets Silicon Valley location. Used as default value for each example service.
     * This default location definition added here just for accumulating all GEO manipulation logic in one class.
     *
     * @return Silicon Valley location.
     */
    public GeoDataBean getSiliconValley() {
        GeoDataBean geo = new GeoDataBean();
        
        geo.setCityName("");
        geo.setCountryName("United States");
        geo.setTimezoneName("America/Los_Angeles");
        geo.setTimezoneName("America/Los_Angeles");
        geo.setLatitude(37.7749);
        geo.setLongitude(122.4194);
        
        return geo;
    }
}
