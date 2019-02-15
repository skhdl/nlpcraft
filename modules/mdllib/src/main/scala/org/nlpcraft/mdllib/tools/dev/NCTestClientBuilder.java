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

package org.nlpcraft.mdllib.tools.dev;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Test client builder for {@link NCTestClient} instances. Note that all configuration values
 * have sensible defaults. Most of the time only user {@link #setUser(String, String) credentials}
 * will have to be changed if not testing with default account.
 */
public class NCTestClientBuilder {
    /** Maximum time to wait for the individual result. */
    private static final long MAX_WAIT_TIME = 30 * 1000; // 30s.

    /** Default public REST API URL (endpoint). */
    public static final String DFLT_BASEURL = "http://localhost:8081/api/v1/";
    /** Default client email. */
    public static final String DFLT_EMAIL = "admin@admin.com";
    /** Default client password. */
    public static final String DFLT_PASSWORD = "admin";
    /** Default local endpoint to get result notifications. */
    public static final String DFLT_ENDPOINT = "http://localhost:8463/test";
    
    private static final Logger log = LoggerFactory.getLogger(NCTestClientBuilder.class);
    
    private static final UrlValidator urlValidator =
        new UrlValidator(new String[]{"http"}, UrlValidator.ALLOW_LOCAL_URLS);
    
    private NCTestClientImpl impl;
    
    /**
     * Creates new builder instance with all defaults set.
     *
     * @return Builder instance.
     */
    public NCTestClientBuilder newBuilder() {
        impl = new NCTestClientImpl();
        
        return this;
    }
    
    /**
     * Sets optional HTTP REST client configuration parameters.
     *
     * @param reqCfg HTTP REST client configuration parameters.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder setRequestConfig(RequestConfig reqCfg) {
        impl.setRequestConfig(reqCfg);
        
        return this;
    }
    
    /**
     * Sets non-default {@link CloseableHttpClient} custom supplier.
     * By default {@link CloseableHttpClient} created with {@link HttpClients#createDefault()}.
     *
     * @param cliSup {@link CloseableHttpClient} custom supplier.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder setHttpClientSupplier(Supplier<CloseableHttpClient> cliSup) {
        impl.setClientSupplier(cliSup);
        
        return this;
    }
    
    /**
     * Sets non-default API base URL. Only change it if your server is not running on localhost.
     * By default {@link NCTestClientBuilder#DFLT_BASEURL} is used.
     *
     * @param baseUrl API base URL.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder setBaseUrl(String baseUrl) {
        String s = baseUrl;
        
        if (!s.endsWith("/")) s += '/';
        
        impl.setBaseUrl(s);
        
        return this;
    }
    
    /**
     * Sets non-default user credentials.
     * By default {@link NCTestClientBuilder#DFLT_EMAIL} and {@link NCTestClientBuilder#DFLT_PASSWORD} are used
     * and they match the default NLPCraft server user.
     *
     * @param email User email.
     * @param pswd  User password.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder setUser(String email, String pswd) {
        impl.setEmail(email);
        impl.setPassword(pswd);
        
        return this;
    }
    
    /**
     * Sets non-default query result endpoint. In most cases the default value should be used unless
     * there's a problem with network exchange on the default endpoint.
     * By default {@link NCTestClientBuilder#DFLT_ENDPOINT} is used.
     *
     * @param endpoint Endpoint URL to set, e.g. "http://localhost:8463/test".
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder setEndpoint(String endpoint) {
        if (endpoint != null) {
            if (!urlValidator.isValid(endpoint))
                throw new IllegalArgumentException(String.format("Invalid endpoint: %s", endpoint));
        }
        
        impl.setEndpoint(endpoint);
        
        return this;
    }
    
    /**
     * Build new configured test client instance.
     *
     * @return Newly built test client instance.
     */
    public NCTestClient build() {
        checkNotNull("email", impl.getEmail());
        checkNotNull("password", impl.getPassword());
        checkNotNull("baseUrl", impl.getBaseUrl());
        checkNotNull("endpoint", impl.getEndpoint());

        impl.prepareClient();
        
        return impl;
    }
    
    /**
     * JSON helper class.
     */
    static class NCDsJson {
        @SerializedName("id") private long dsId;
        @SerializedName("mdlId") private String mdlId;
        
        public long getDataSourceId() {
            return dsId;
        }

        public void setDataSourceId(long dsId) {
            this.dsId = dsId;
        }

        public String getModelId() {
            return mdlId;
        }

        public void setModelId(String mdlId) {
            this.mdlId = mdlId;
        }
    }
    
    /**
     * JSON helper class.
     */
    static class NCRequestStateJson {
        @SerializedName("srvReqId") private String srvReqId;
        @SerializedName("usrId") private long userId;
        @SerializedName("dsId") private long dsId;
        @SerializedName("resType") private String resType;
        @SerializedName("resBody") private String resBody;
        @SerializedName("status") private String status;
        @SerializedName("error") private String error;
        @SerializedName("createTstamp") private long createTstamp;
        @SerializedName("updateTstamp") private long updateTstamp;
        
        public String getServerRequestId() {
            return srvReqId;
        }

        public void setServerRequestId(String srvReqId) {
            this.srvReqId = srvReqId;
        }

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public long getDataSourceId() {
            return dsId;
        }

        public void setDataSourceId(long dsId) {
            this.dsId = dsId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getResultType() {
            return resType;
        }

        public void setResultType(String resType) {
            this.resType = resType;
        }
        public String getResultBody() {
            return resBody;
        }

        public void setResultBody(String resBody) {
            this.resBody = resBody;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public long getCreateTstamp() {
            return createTstamp;
        }

        public void setCreateTstamp(long createTstamp) {
            this.createTstamp = createTstamp;
        }

        public long getUpdateTstamp() {
            return updateTstamp;
        }
        
        public void setUpdateTstamp(long updateTstamp) {
            this.updateTstamp = updateTstamp;
        }
    }
    
    /**
     * Client implementation.
     */
    private class NCTestClientImpl implements NCTestClient {
        private static final String STATUS_API_OK = "API_OK";

        private final Type TYPE_RESP = new TypeToken<HashMap<String, Object>>() {}.getType();
        private final Type TYPE_STATES = new TypeToken<ArrayList<NCRequestStateJson>>() {}.getType();
        private final Type TYPE_DSS = new TypeToken<ArrayList<NCDsJson>>() {}.getType();

        private final Gson gson = new Gson();
        private final Object mux = new Object();
        private final ConcurrentHashMap<String, NCRequestStateJson> res = new ConcurrentHashMap<>();
        
        private String baseUrl = DFLT_BASEURL;
        private String email = DFLT_EMAIL;
        private String pswd = DFLT_PASSWORD;
        private String endpoint = DFLT_ENDPOINT;

        private CloseableHttpClient httpCli;
        private RequestConfig reqCfg;
        private Supplier<CloseableHttpClient> cliSup;
        private String acsTok;
        private long dsId;
        private String mdlId;
        private boolean isTestDs = false;
        private HttpServer srv;

        private volatile boolean opened = false;
        private volatile boolean closed = false;

        RequestConfig getRequestConfig() {
            return reqCfg;
        }
        
        void setRequestConfig(RequestConfig reqCfg) {
            this.reqCfg = reqCfg;
        }
        
        String getBaseUrl() {
            return baseUrl;
        }
        
        void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        String getEmail() {
            return email;
        }
        
        void setEmail(String email) {
            this.email = email;
        }
        
        String getPassword() {
            return pswd;
        }
        
        void setPassword(String pswd) {
            this.pswd = pswd;
        }
        
        Supplier<CloseableHttpClient> getClientSupplier() {
            return cliSup;
        }
        
        void setClientSupplier(Supplier<CloseableHttpClient> cliSup) {
            this.cliSup = cliSup;
        }
        
        void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    
        String getEndpoint() {
            return endpoint;
        }
    
        void prepareClient() {
            httpCli = cliSup != null ? cliSup.get() : HttpClients.createDefault();
        }
        
        @Override
        public NCTestResult ask(String txt) throws NCTestClientException, IOException {
            if (txt == null)
                throw new IllegalArgumentException("Test sentence text cannot be 'null'.");

            if (!opened) throw new IllegalStateException("Client is not opened.");
            if (closed) throw new IllegalStateException("Client already closed.");

            String srvReqId;
            
            try {
                srvReqId = restAsk(txt);
            }
            catch (NCTestClientException e) {
                return mkResult(
                    txt,
                    dsId,
                    mdlId,
                    null,
                    null,
                    e.getLocalizedMessage(),
                    0
                );
            }

            long maxTime = System.currentTimeMillis() + MAX_WAIT_TIME;

            while (true) {
                NCRequestStateJson js = this.res.remove(srvReqId);

                if (js != null) {
                    assert js.status.equals("QRY_READY");

                    return mkResult(txt, dsId, mdlId, js);
                }

                waitUntil(maxTime);
            } 
        }

        /**
         *
         * @param dsId
         * @param mdlId
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private void open0(Long dsId, String mdlId) throws NCTestClientException, IOException {
            assert dsId != null ^ mdlId != null;
            
            if (opened) throw new IllegalStateException("Client already opened.");
            if (closed) throw new IllegalStateException("Client already closed.");
            
            acsTok = restSignin();
            
            if (dsId != null) {
                this.dsId = dsId;
                isTestDs = false;
            }
            else {
                this.dsId = restCreateTestDs(mdlId);
                isTestDs = true;
            }
    
            Optional<NCDsJson> dsOpt = restGetDataSources().stream().filter(p -> p.getDataSourceId() == this.dsId).findAny();
            
            if (!dsOpt.isPresent())
                throw new NCTestClientException(String.format("Data source ID not found: %d", dsId));
            
            this.mdlId = dsOpt.get().getModelId();
            
            URL url = new URL(endpoint);

            String host = url.getHost();

            if (host.equals("127.0.0.1") || host.equalsIgnoreCase("localhost")) {
                endpoint = endpoint.replaceAll(host, InetAddress.getLocalHost().getHostAddress());

                url = new URL(endpoint);
            }

            restRegisterEndpoint();

            srv = HttpServer.create(new InetSocketAddress(url.getHost(), url.getPort()), 0);

            srv.createContext(url.getPath(), http -> {
                try (BufferedInputStream is = new BufferedInputStream(http.getRequestBody())) {
                    byte[] arr = new byte[Integer.parseInt(http.getRequestHeaders().getFirst("Content-length"))];

                    int n = 0;

                    while (n != arr.length) {
                        int k = is.read(arr, n, arr.length - n);

                        if (k == -1)
                            throw new EOFException();

                        n = n + k;
                    }

                    List<NCRequestStateJson> list =
                        gson.fromJson(new String(arr, StandardCharsets.UTF_8), TYPE_STATES);

                    for (NCRequestStateJson p : list)
                        res.put(p.getServerRequestId(), p);

                    synchronized (mux) {
                        mux.notifyAll();
                    }

                    String resp = "OK";

                    http.sendResponseHeaders(200, resp.length());

                    try (BufferedOutputStream out = new BufferedOutputStream(http.getResponseBody())) {
                        out.write(resp.getBytes());
                    }
                }
                catch (Exception e) {
                    log.error("Error processing endpoint message.", e);
                }
            });

            srv.start();

            log.info("Endpoint listener started: {}", endpoint);

            this.opened = true;
        }
        
        @Override
        public void open(long dsId) throws NCTestClientException, IOException {
            open0(dsId, null);
        }
        
        @Override
        public void open(String mdlId) throws NCTestClientException, IOException {
            open0(null, mdlId);
        }
        
        @Override
        public void close() throws NCTestClientException, IOException {
            if (!opened) throw new IllegalStateException("Client is not opened.");
            if (closed) throw new IllegalStateException("Client already closed.");
            
            if (srv != null) srv.stop(0);
            
            res.clear();
            
            if (isTestDs) restDeleteTestDs();

            restRemoveEndpoint();
            
            restSignout();
            
            closed = true;
        }
    
        /**
         * Clears conversation for this test client. This method will clear conversation for
         * its configured user.
         *
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        @Override
        public void clearConversation() throws NCTestClientException, IOException {
            if (!opened) throw new IllegalStateException("Client is not opened.");
            if (closed) throw new IllegalStateException("Client already closed.");
            
            restClearConversation();
        }

        /**
         * 
         * @param wakeupTime
         * @throws NCTestClientException
         */
        private void waitUntil(long wakeupTime) throws NCTestClientException {
            long sleepTime = wakeupTime - System.currentTimeMillis();
        
            if (sleepTime <= 0)
                throw new NCTestClientException("Max wait time elapsed.");
        
            synchronized (mux) {
                try {
                    mux.wait(sleepTime);
                }
                catch (InterruptedException e) {
                    throw new NCTestClientException("Result wait thread interrupted.", e);
                }
            }
        }
    
        @SuppressWarnings("unchecked")
        private <T> T getField(Map<String, Object> m, String fn) throws NCTestClientException {
            Object o = m.get(fn);
            
            if (o == null)
                throw new NCTestClientException(String.format("Missed expected field [fields=%s, field=%s]",
                    m.keySet(), fn));
            
            try {
                return (T) o;
            }
            catch (ClassCastException e) {
                throw new NCTestClientException(String.format("Invalid field type: %s", o), e);
            }
        }
        
        private void checkStatus(Map<String, Object> m) throws NCTestClientException {
            String status = getField(m, "status");
            
            if (!status.equals(STATUS_API_OK))
                throw new NCTestClientException(String.format("Unexpected message status: %s", status));
        }
        
        private <T> T extract(JsonElement js, Type t) throws NCTestClientException {
            try {
                return gson.fromJson(js, t);
            }
            catch (JsonSyntaxException e) {
                throw new NCTestClientException(String.format("Invalid field type [json=%s, type=%s]", js, t), e);
            }
        }
        
        private <T> T checkAndExtract(String js, String name, Type type) throws NCTestClientException {
            Map<String, Object> m = gson.fromJson(js, TYPE_RESP);
            
            checkStatus(m);
            
            return extract(gson.toJsonTree(getField(m, name)), type);
        }
        
        @SafeVarargs
        private final String post(String url, Pair<String, Object>... ps) throws NCTestClientException, IOException {
            HttpPost post = new HttpPost(baseUrl + url);
            
            try {
                if (reqCfg != null)
                    post.setConfig(reqCfg);
                
                StringEntity entity = new StringEntity(
                    gson.toJson(Arrays.stream(ps).
                    filter(p -> p.getValue() != null).
                    collect(Collectors.toMap(Pair::getKey, Pair::getValue))),
                    "UTF-8"
                );
                
                post.setHeader("Content-Type", "application/json");
                post.setEntity(entity);
                
                ResponseHandler<String> h = resp -> {
                    int code = resp.getStatusLine().getStatusCode();
                    
                    HttpEntity e = resp.getEntity();
                    
                    String js = e != null ? EntityUtils.toString(e) : null;
                    
                    if (js == null)
                        throw new NCTestClientException(String.format("Unexpected empty response [code=%d]", code));
                    
                    switch (code) {
                        case 200: return js;
                        case 400: throw new NCTestClientException(js);
                        default:
                            throw new NCTestClientException(
                                String.format("Unexpected response [code=%d, text=%s]", code, js)
                            );
                    }
                };
                
                return httpCli.execute(post, h);
            }
            finally {
                post.releaseConnection();
            }
        }

        /**
         *
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private void restClearConversation() throws IOException, NCTestClientException {
            log.info("`clear/conversation` request sent for data source: `{}`", dsId);
            
            checkStatus(gson.fromJson(
                post(
                    "clear/conversation",
                    Pair.of("accessToken", acsTok),
                    Pair.of("dsId", dsId)
                ),
                TYPE_RESP)
            );
        }
        
        /**
         * @param mdlId Model ID.
         * @return ID of newly created data source.
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private long restCreateTestDs(String mdlId) throws IOException, NCTestClientException {
            log.info("`ds/add` request sent for model: `{}`", mdlId);
            
            long id =
                checkAndExtract(
                    post(
                        "ds/add",
                        Pair.of("accessToken", acsTok),
                        Pair.of("name", "test"),
                        Pair.of("shortDesc", "Test data source"),
                        Pair.of("mdlId", mdlId),
                        Pair.of("mdlName", "Test model"),
                        Pair.of("mdlVer", "Test version")
                    ),
                    "id",
                    Long.class
                );
            
            log.info("Temporary test data source created: {}", id);
            
            return id;
        }
        
        /**
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private void restDeleteTestDs() throws IOException, NCTestClientException {
            log.info("`ds/delete` request sent for temporary data source: `{}`", dsId);
            
            checkStatus(
                gson.fromJson(
                    post(
                        "ds/delete",
                        Pair.of("accessToken", acsTok),
                        Pair.of("id", dsId)
                    ),
                    TYPE_RESP
                )
            );
        }
    
        /**
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private void restRegisterEndpoint() throws IOException, NCTestClientException {
            log.info("`user/endpoint/register` request sent `{}`", endpoint);
        
            checkStatus(
                gson.fromJson(
                    post(
                        "user/endpoint/register",
                        Pair.of("accessToken", acsTok),
                        Pair.of("endpoint", endpoint)
                    ),
                    TYPE_RESP
                )
            );
        }

        /**
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private void restRemoveEndpoint() throws IOException, NCTestClientException {
            log.info("`user/endpoint/remove` request sent `{}`", endpoint);

            checkStatus(
                gson.fromJson(
                    post(
                        "user/endpoint/remove",
                        Pair.of("accessToken", acsTok)
                    ),
                    TYPE_RESP
                )
            );
        }

        /**
         * @return Access token.
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private String restSignin() throws IOException, NCTestClientException {
            log.info("`user/signin` request sent for: `{}`", email);
            
            return checkAndExtract(
                post(
                    "user/signin",
                    Pair.of("email", email),
                    Pair.of("passwd", pswd)
                ),
                "accessToken",
                String.class
            );
        }
        
        /**
         * @return List of data sources for configured user.
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private List<NCDsJson> restGetDataSources() throws IOException, NCTestClientException {
            log.info("`ds/all` request sent for: `{}`", email);
            
            Map<String, Object> m = gson.fromJson(
                post(
                    "ds/all",
                    Pair.of("accessToken", acsTok)
                ),
                TYPE_RESP
            );
            
            checkStatus(m);
            
            return extract(gson.toJsonTree(getField(m, "dataSources")), TYPE_DSS);
        }
        
        /**
         * @param txt
         * @return
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private String restAsk(String txt) throws IOException, NCTestClientException {
            log.info("`ask` request sent: `{}` to data source: `{}`", txt, dsId);
            
            return checkAndExtract(
                post(
                    "ask",
                    Pair.of("accessToken", acsTok),
                    Pair.of("txt", txt),
                    Pair.of("dsId", dsId),
                    Pair.of("isTest", true)
                ),
                "srvReqId",
                String.class
            );
        }
        
        /**
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private void restSignout() throws IOException, NCTestClientException {
            log.info("`user/signout` request sent for: `{}`", email);
            
            checkStatus(gson.fromJson(
                post(
                    "user/signout",
                    Pair.of("accessToken", acsTok)
                ),
                TYPE_RESP)
            );
        }
    
        /**
         *
         * @param txt
         * @param dsId
         * @param mdlId
         * @param resType
         * @param resBody
         * @param errMsg
         * @param time
         * @return
         */
        private NCTestResult mkResult(
            String txt,
            long dsId,
            String mdlId,
            String resType,
            String resBody,
            String errMsg,
            long time
        ) {
            assert txt != null;
            assert mdlId != null;
            assert (resType != null && resBody != null) ^ errMsg != null;
            
            return new NCTestResult() {
                private Optional<String> convert(String s) {
                    return s == null ? Optional.empty() : Optional.of(s);
                }
                
                @Override
                public String getText() {
                    return txt;
                }
                
                @Override
                public long getProcessingTime() {
                    return time;
                }
                
                @Override
                public long getDataSourceId() {
                    return dsId;
                }
                
                @Override
                public String getModelId() {
                    return mdlId;
                }
                
                @Override
                public Optional<String> getResult() {
                    return convert(resBody);
                }
                
                @Override
                public Optional<String> getResultType() {
                    return convert(resType);
                }
                
                @Override
                public Optional<String> getResultError() {
                    return convert(errMsg);
                }
            };
        }
    
        /**
         *
         * @param txt
         * @param dsId
         * @param mdlId
         * @param js
         * @return
         */
        private NCTestResult mkResult(String txt, long dsId, String mdlId, NCRequestStateJson js) {
            return mkResult(
                txt,
                dsId,
                mdlId,
                js.getResultType(),
                js.getResultBody(),
                js.getError(),
                js.getUpdateTstamp() - js.getCreateTstamp()
            );
        }
    }
    
    /**
     * @param name
     * @param v
     * @throws IllegalArgumentException
     */
    private void checkNotNull(String name, Object v) throws IllegalArgumentException {
        if (v == null) throw new IllegalArgumentException(String.format("Test client property cannot be null: '%s'", name));
    }
}
