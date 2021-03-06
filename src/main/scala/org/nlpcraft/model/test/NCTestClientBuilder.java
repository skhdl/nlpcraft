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

package org.nlpcraft.model.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    static class NCRequestStateJson {
        @SerializedName("srvReqId") private String srvReqId;
        @SerializedName("txt") private String text;
        @SerializedName("usrId") private long userId;
        @SerializedName("resType") private String resType;
        @SerializedName("resBody") private Object resBody;
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
    
        public String getText() {
            return text;
        }
    
        public void setText(String text) {
            this.text = text;
        }
    
        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
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
        
        public Object getResultBody() {
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

        private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
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
        private String mdlId;
        private HttpServer srv;

        private volatile boolean opened = false;
        private volatile boolean closed = false;
        
        private final Set<String> srvReqIds = ConcurrentHashMap.newKeySet();

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
                    mdlId,
                    null,
                    null,
                    e.getLocalizedMessage(),
                    0
                );
            }
    
            srvReqIds.add(srvReqId);

            long maxTime = System.currentTimeMillis() + MAX_WAIT_TIME;

            while (true) {
                NCRequestStateJson js = this.res.remove(srvReqId);

                if (js != null) {
                    assert js.status.equals("QRY_READY");
                    
                    String body = null;
                    
                    if (js.getResultBody() != null) {
                        body =
                            js.getResultType() != null && js.getResultType().equals("json") ?
                                gson.toJson(js.getResultBody()) :
                                (String)js.getResultBody();
                    }
    
                    NCTestResult res =
                        mkResult(
                            js.getText(),
                            mdlId,
                            js.getResultType(),
                            body,
                            js.getError(),
                            js.getUpdateTstamp() - js.getCreateTstamp()
                        );

                    if (res.isSuccessful()) {
                        assert res.getResultType().isPresent() && res.getResult().isPresent();
                        
                        log.info(
                            "'ask' request '{}' answered successfully with '{}' result:\n{}",
                            txt,
                            res.getResultType().get(),
                            mkPrettyString(res.getResultType().get(), res.getResult().get())
                        );
                    }
                    else {
                        assert res.getResultError().isPresent();
                        
                        log.info("'ask' request '{}' answered unsuccessfully with result:\n{}",
                            txt,
                            res.getResultError().get()
                        );
                    }
                    return res;
                }

                waitUntil(maxTime);
            } 
        }
    
        /**
         *
         * @param type
         * @param body
         * @return
         */
        private String mkPrettyString(String type, String body) {
            try {
                switch (type) {
                    case "html": return Jsoup.parseBodyFragment(body).outerHtml();
                    case "text":
                    case "yaml":
                    case "json": // JSON already configured for pretty printing.
                        return body;
    
                    default: return body;
                }
            }
            catch (Exception e) {
                log.error(
                    "Error during result decoding [type={}, body={}, error={}]", type, body, e.getLocalizedMessage()
                );
                
                return body;
            }
        }

        @Override
        public void open(String mdlId) throws NCTestClientException, IOException {
            assert mdlId != null;
    
            if (opened) throw new IllegalStateException("Client already opened.");
            if (closed) throw new IllegalStateException("Client already closed.");
    
            acsTok = restSignin();
    
    
            this.mdlId = mdlId;
    
            URL url = new URL(endpoint);
    
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
        public void close() throws NCTestClientException, IOException {
            if (!opened) throw new IllegalStateException("Client is not opened.");
            if (closed) throw new IllegalStateException("Client is already closed.");
            
            if (srv != null) srv.stop(0);
            
            res.clear();
            
            if (!srvReqIds.isEmpty()) {
                restCancel();
                
                srvReqIds.clear();
            }
            
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
            if (closed) throw new IllegalStateException("Client is already closed.");
            
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
            log.info("'clear/conversation' request sent for data model: {}", mdlId);
            
            checkStatus(gson.fromJson(
                post(
                    "clear/conversation",
                    Pair.of("acsTok", acsTok),
                    Pair.of("mdlId", mdlId)
                ),
                TYPE_RESP)
            );
        }
        
        /**
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private void restCancel() throws IOException, NCTestClientException {
            log.info("'cancel' request sent for server request IDs: {}", srvReqIds);
        
            checkStatus(
                gson.fromJson(
                    post(
                        "cancel",
                        Pair.of("acsTok", acsTok),
                        Pair.of("srvReqIds", srvReqIds)
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
            log.info("'endpoint/register' request sent to: {}", endpoint);
        
            checkStatus(
                gson.fromJson(
                    post(
                        "endpoint/register",
                        Pair.of("acsTok", acsTok),
                        Pair.of("endpoint", endpoint)
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
            log.info("'/signin' request sent for: {}", email);
            
            return checkAndExtract(
                post(
                    "/signin",
                    Pair.of("email", email),
                    Pair.of("passwd", pswd)
                ),
                "acsTok",
                String.class
            );
        }
        
        /**
         * @param txt
         * @return
         * @throws IOException Thrown in case of IO errors.
         * @throws NCTestClientException Thrown in case of test client errors.
         */
        private String restAsk(String txt) throws IOException, NCTestClientException {
            log.info("'ask' request '{}' sent for data model ID: {}", txt, mdlId);
            
            return checkAndExtract(
                post(
                    "ask",
                    Pair.of("acsTok", acsTok),
                    Pair.of("txt", txt),
                    Pair.of("mdlId", mdlId)
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
            log.info("'/signout' request sent for: {}", email);
            
            checkStatus(gson.fromJson(
                post(
                    "signout",
                    Pair.of("acsTok", acsTok)
                ),
                TYPE_RESP)
            );
        }
    
        /**
         *
         * @param txt
         * @param mdlId
         * @param resType
         * @param resBody
         * @param errMsg
         * @param time
         * @return
         */
        private NCTestResult mkResult(
            String txt,
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
                private<T> Optional<String> convert(String s) {
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
