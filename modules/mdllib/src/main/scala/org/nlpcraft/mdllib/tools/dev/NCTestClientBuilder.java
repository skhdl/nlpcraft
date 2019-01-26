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

package org.nlpcraft.mdllib.tools.dev;

import org.nlpcraft.mdllib.NCQueryResult;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Test client builder for {@link NCTestClient} instances.
 */
public class NCTestClientBuilder {
    /** Default millisecond delay between result checks. */
    public static final long DFLT_CHECK_INTERVAL_MS = 2000;
    
    /** Default millisecond delay between each request call. */
    public static final long DFLT_DELAY_MS = 50;
    
    /** Default clear conversation flag value. */
    public static final boolean DFLT_CLEAR_CONVERSATION = false;
    
    /** Default asynchronous mode flag value. */
    public static final boolean DFLT_ASYNC_MODE = true;
    
    private static final long HOUR = 60 * 60 * 1000;
    private static final long MINS_5 = 5 * 60 * 1000;
    
    private static final Logger log = LoggerFactory.getLogger(NCTestClientBuilder.class);
    
    private long delayMs = DFLT_DELAY_MS;
    private long checkIntervalMs = DFLT_CHECK_INTERVAL_MS;
    private boolean clearConv = DFLT_CLEAR_CONVERSATION;
    private boolean asyncMode = DFLT_ASYNC_MODE;
    
    private RequestConfig reqCfg;
    
    /**
     * JSON helper class.
     */
    static class NCRequestStateJson {
        @SerializedName("srvReqId") private String serverRequestId;
        @SerializedName("userId") private long userId;
        @SerializedName("dsId") private long dsId;
        @SerializedName("text") private String text;
        @SerializedName("resType") private String resultType;
        @SerializedName("resBody") private String resultBody;
        @SerializedName("error") private String error;
        @SerializedName("createTstamp") private long createTstamp;
        @SerializedName("updateTstamp") private long updateTstamp;
        @SerializedName("resMetadata") private Map<String, Object> resultMetadata;
        @SerializedName("responseType") private String responseType;
    
        /**
         * Gets server request id.
         *
         * @return the server request id
         */
        public String getServerRequestId() {
            return serverRequestId;
        }
    
        /**
         * Sets server request id.
         *
         * @param serverRequestId the server request id
         */
        public void setServerRequestId(String serverRequestId) {
            this.serverRequestId = serverRequestId;
        }
    
        /**
         * Gets data source id.
         *
         * @return the data source id
         */
        public long getDsId() {
            return dsId;
        }
    
        /**
         * Sets data source id.
         *
         * @param dsId the data source id
         */
        public void setDsId(long dsId) {
            this.dsId = dsId;
        }
    
        /**
         * Gets user id.
         *
         * @return the user id
         */
        public long getUserId() {
            return userId;
        }
    
        /**
         * Sets user id.
         *
         * @param userId the user id
         */
        public void setUserId(long userId) {
            this.userId = userId;
        }
    
        /**
         * Gets text.
         *
         * @return the text
         */
        public String getText() {
            return text;
        }
    
        /**
         * Sets text.
         *
         * @param text the text
         */
        public void setText(String text) {
            this.text = text;
        }
    
        /**
         * Gets result type.
         *
         * @return the result type
         */
        public String getResultType() {
            return resultType;
        }
    
        /**
         * Sets result type.
         *
         * @param resultType the result type
         */
        public void setResultType(String resultType) {
            this.resultType = resultType;
        }
    
        /**
         * Gets result body.
         *
         * @return the result body
         */
        public String getResultBody() {
            return resultBody;
        }
    
        /**
         * Sets result body.
         *
         * @param resultBody the result body
         */
        public void setResultBody(String resultBody) {
            this.resultBody = resultBody;
        }
    
        /**
         * Gets error.
         *
         * @return the error
         */
        public String getError() {
            return error;
        }
    
        /**
         * Sets error.
         *
         * @param error the error
         */
        public void setError(String error) {
            this.error = error;
        }
    
        /**
         * Gets result metadata.
         *
         * @return the result metadata
         */
        public Map<String, Object> getResultMetadata() {
            return resultMetadata != null ? resultMetadata : Collections.emptyMap();
        }
    
        /**
         * Sets result metadata.
         *
         * @param resultMetadata the result metadata
         */
        public void setResultMetadata(Map<String, Object> resultMetadata) {
            this.resultMetadata = resultMetadata;
        }
    
        /**
         * Gets create tstamp.
         *
         * @return the create tstamp
         */
        public long getCreateTstamp() {
            return createTstamp;
        }
    
        /**
         * Sets create tstamp.
         *
         * @param createTstamp the create tstamp
         */
        public void setCreateTstamp(long createTstamp) {
            this.createTstamp = createTstamp;
        }
    
        /**
         * Gets update tstamp.
         *
         * @return the update tstamp
         */
        public long getUpdateTstamp() {
            return updateTstamp;
        }
    
        /**
         * Sets update tstamp.
         *
         * @param updateTstamp the update tstamp
         */
        public void setUpdateTstamp(long updateTstamp) {
            this.updateTstamp = updateTstamp;
        }
    
        /**
         * Gets state type.
         *
         * @return the state type
         */
        public String getResponseType() {
            return responseType;
        }
    
        /**
         * Sets state type.
         *
         * @param responseType the state type
         */
        public void setResponseType(String responseType) {
            this.responseType = responseType;
        }
    }
    
    /**
     * JSON helper class.
     */
    static class NCDataSourceJson {
        @SerializedName("dsId") private long id;
        @SerializedName("dsName") private String name;
        @SerializedName("isDeployed") private boolean isDeployed;
    
        /**
         * Gets id.
         *
         * @return the id
         */
        public long getId() {
            return id;
        }
    
        /**
         * Sets id.
         *
         * @param id the id
         */
        public void setId(long id) {
            this.id = id;
        }
    
        /**
         * Gets name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }
    
        /**
         * Sets name.
         *
         * @param name the name
         */
        public void setName(String name) {
            this.name = name;
        }
    
        /**
         * Is deployed boolean.
         *
         * @return the boolean
         */
        public boolean isDeployed() {
            return isDeployed;
        }
    
        /**
         * Sets deployed.
         *
         * @param isDeployed the is deployed
         */
        public void setDeployed(boolean isDeployed) {
            this.isDeployed = isDeployed;
        }
    }
    
    private class AsciiTable {
        private final List<Pair<String, Integer>> cols;
        private final List<List<String>> rows;
        
        AsciiTable(String... cols) {
            this.cols = Arrays.stream(cols).map(p -> MutablePair.of(p, 0)).collect(Collectors.toList());
            this.rows = new ArrayList<>();
        }
        
        private void writeColumnNames(List<Pair<String, Integer>> cols, StringBuilder buf) {
            buf.append('|');
            
            for (Pair<String, Integer> col : cols) {
                buf.append(String.format(" %-" + col.getValue() + 's', col.getKey()));
                
                buf.append('|');
            }
            
            buf.append('\n');
        }
        
        private void writeSeparator(List<Pair<String, Integer>> cols, StringBuilder buf) {
            buf.append('+');
            
            for (Pair<String, Integer> col : cols) {
                buf.append(String.format("%-" + (col.getValue() + 1) + 's', "").replace(' ', '-'));
                
                buf.append('+');
            }
            
            buf.append('\n');
        }
        
        private void writeValues(List<Pair<String, Integer>> cols, List<List<String>> rows, StringBuilder buf) {
            for (List<String> row : rows) {
                int idx = 0;
                
                buf.append('|');
                
                for (String cell : row) {
                    buf.append(String.format(" %-" + cols.get(idx).getValue() + 's', cell));
                    
                    buf.append('|');
                    
                    idx++;
                }
                
                buf.append('\n');
            }
        }
        
        void addRow(List<Object> row) {
            rows.add(row.stream().map(p -> p != null ? p.toString() : "").collect(Collectors.toList()));
        }
    
        String mkContent() {
            cols.forEach(col -> col.setValue(col.getKey().length() + 1));
        
            for (List<String> row : rows) {
                int i = 0;
            
                for (String cell : row) {
                    if (cell != null) {
                        Pair<String, Integer> col = cols.get(i);
                    
                        col.setValue(Math.max(col.getValue(), cell.length() + 1));
                    
                        i++;
                    }
                }
            }
        
            StringBuilder buf = new StringBuilder();
        
            writeSeparator(cols, buf);
            writeColumnNames(cols, buf);
            writeSeparator(cols, buf);
            writeValues(cols, rows, buf);
            writeSeparator(cols, buf);
        
            return buf.toString();
        }
    }
    
    /**
     * Client implementation.
     */
    private class NCTestClientImpl implements NCTestClient {
        private final String STATUS_API_OK = "PUB_API_OK";
        private final Type TYPE_RESP = new TypeToken<HashMap<String, Object>>() {}.getType();
        private final Type TYPE_REQS = new TypeToken<ArrayList<NCRequestStateJson>>() {}.getType();
        private final Type TYPE_DSS = new TypeToken<ArrayList<NCDataSourceJson>>() {}.getType();
    
        private final Gson gson = new Gson();
        
        private final NCTestClientConfig cfg;
        private final CloseableHttpClient client;
    
        /**
         * Instantiates a new Dl test client.
         *
         * @param cfg the cfg
         */
        NCTestClientImpl(NCTestClientConfig cfg) {
            this.cfg = cfg;
            this.client = cfg.getClientSupplier().get();
        }
    
        @SuppressWarnings("unchecked")
        private<T> T getField(Map<String, Object> m, String fn) throws NCTestClientException {
            Object o = m.get(fn);
        
            if (o == null)
                throw new NCTestClientException(
                    String.format("Missed expected field [fields=%s, field=%s]", m.keySet(), fn)
                );
        
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
                throw new NCTestClientException("Unexpected message status: " + status);
        }
    
        private <T> T extract(JsonElement js, Type t) throws NCTestClientException {
            try {
                return gson.fromJson(js, t);
            }
            catch (JsonSyntaxException e) {
                throw new NCTestClientException(String.format("Invalid field type [json=%s, type=%s]", js, t), e);
            }
        }
    
        private <T> T extract(String js, String name, Type type) throws NCTestClientException {
            Map<String, Object> m = gson.fromJson(js, TYPE_RESP);
        
            checkStatus(m);
        
            return extract(gson.toJsonTree(getField(m, name)), type);
        }
        
        private String post(String url, Pair<String, Object>... ps) throws NCTestClientException, IOException {
            HttpPost post = new HttpPost(url);
        
            try {
                if (reqCfg != null)
                    post.setConfig(reqCfg);
            
                StringEntity entity = new StringEntity(
                    gson.toJson(
                        Arrays.stream(ps).
                            filter(p -> p.getValue() != null).
                            collect(Collectors.toMap(Pair::getKey, Pair::getValue))
                    )
                );
            
                post.setEntity(entity);
            
                log.trace("Request prepared: {}", post);
            
                ResponseHandler<String> h = resp -> {
                    int code = resp.getStatusLine().getStatusCode();
                
                    HttpEntity e = resp.getEntity();
                
                    String js = e != null ? EntityUtils.toString(e) : null;
                
                    if (js == null)
                        throw new NCTestClientException(String.format("Unexpected empty response [code=%d]", code));
                
                    switch (code) {
                        case 200:
                            return js;
                        case 400:
                            Map<String, Object> m = gson.fromJson(js, TYPE_RESP);
                        
                            String status = getField(m, "status");
                            String reason = getField(m, "reason");
                        
                            throw new NCTestClientException(
                                String.format("Server error [status=%s, reason=%s]", status, reason)
                            );
                    
                        default:
                            throw new NCTestClientException(
                                String.format("Unexpected response [code=%d, text=%s]", code, js)
                            );
                    }
                };
            
                String s = client.execute(post, h);
            
                log.trace("Response received: {}", s);
            
                return s;
            }
            finally {
                post.releaseConnection();
            }
        }
    
        private int convertResponse(String respType) {
            switch (respType.toUpperCase()) {
                case "RESP_OK": return RESP_OK;
                case "RESP_VALIDATION": return RESP_VALIDATION;
                case "RESP_ERROR": return RESP_ERROR;
            
                default:
                    throw new IllegalArgumentException("Unexpected state:" + respType);
            }
        }
    
        private String convertCode(int code) {
            switch (code) {
                case RESP_OK: return "RESP_OK";
                case RESP_VALIDATION: return "RESP_VALIDATION";
                case RESP_ERROR: return "RESP_ERROR";
            
                default:
                    throw new IllegalArgumentException("Unexpected state:" + code);
            }
        }
    
        @Override
        public List<NCTestResult> test(NCTestSentence... tests) throws NCTestClientException, IOException {
            return test(Arrays.asList(tests));
        }
    
        @SuppressWarnings("unchecked")
        @Override
        public synchronized List<NCTestResult> test(List<NCTestSentence> tests)
            throws NCTestClientException, IOException {
            notNull("tests", tests);
            
            if (tests.isEmpty())
                throw new IllegalArgumentException("Tests cannot be empty");
    
            List<Pair<String, Long>> allTestPairs =
                tests.stream().map(p -> Pair.of(p.getText(), p.getDsId())).collect(Collectors.toList());
    
            List<Pair<String, Long>> testsPairs = allTestPairs.stream().distinct().collect(Collectors.toList());
    
            if (testsPairs.size() != tests.size()) {
                for (Pair<String, Long> testsPair : testsPairs) {
                    allTestPairs.remove(testsPair);
                }
    
                String s =
                    allTestPairs.stream().
                        map(p -> "sentence=" + p.getLeft() + ", datasource=" + p.getRight()).
                        collect(Collectors.joining(";", "[", "]"));
    
                throw new NCTestClientException("Sentences texts cannot be duplicated within same datasource: " + s);
            }

            if (delayMs != 0) {
                int minApiCalls =
                    clearConv ?
                        // - for each test: 'conv/clear', 'ask', 'check'.
                        3 * tests.size() :
                        // - 'conv/clear' - calls for each data source before test.
                        // - 'ask' for each test
                        // - at least one 'check'
                        (int)tests.stream().map(NCTestSentence::getDsId).distinct().count() + tests.size() + 1;
    
                
                // - 3 - 'signin', 'ds/all', 'signout',
                minApiCalls += 3;
                
                if (minApiCalls * delayMs > HOUR)
                    throw new NCTestClientException("Test too long, decrease delay or sentences count");
            }
            
            String auth =
                extract(
                    post(
                        cfg.getBaseUrl() + "signin",
                        Pair.of("probeToken", cfg.getProbeToken()),
                        Pair.of("email", cfg.getEmail())
                    ),
                    "accessToken",
                    String.class
                );
    
            sleep(delayMs);
    
            log.debug("Client logged in: {}", cfg.getBaseUrl());
    
            List<NCTestResult> res = new ArrayList<>();
            
            try {
                if (clearConv) {
                    for (NCTestSentence test : tests) {
                        clearConversation(auth, test.getDsId());
        
                        sleep(delayMs);
        
                        res.addAll(executeAsync(auth, Collections.singletonList(test)));
                    }
                }
                else {
                    Set<Long> dsIds = tests.stream().map(NCTestSentence::getDsId).collect(Collectors.toSet());
                    
                    if (asyncMode) {
                        clearConversationAllDss(auth, dsIds);
    
                        res.addAll(executeAsync(auth, tests));
                    }
                    else {
                        clearConversationAllDss(auth, dsIds);
    
                        for (NCTestSentence test : tests) {
                            res.addAll(executeAsync(auth, Collections.singletonList(test)));
        
                            sleep(delayMs);
                        }
                    }
                }
            }
            finally {
                // This potential error can be ignored. Also it shouldn't override main method errors.
                try {
                    sleep(delayMs);
                    
                    checkStatus(
                        gson.fromJson(
                            post(cfg.getBaseUrl() + "signout",
                                Pair.of("accessToken", auth)
                            ),
                            TYPE_RESP
                        )
                    );
                }
                catch (Exception e) {
                    log.error("Signout error.", e);
                }
            }
    
            res.sort(Comparator.comparingInt(o -> testsPairs.indexOf(Pair.of(o.getText(), o.getDsId()))));
    
            printResult(tests, res);
    
            return res;
        }
    
        private void printResult(List<NCTestSentence> tests, List<NCTestResult> results) {
            assert tests != null && results != null;
            assert !tests.isEmpty();
            assert tests.size() == results.size();
            
            int n = tests.size();
            
            AsciiTable resTab = new AsciiTable(
                "Data Source",
                "Sentence",
                "Expected Intent",
                "Intent",
                "Expected Result",
                "Result",
                "Error",
                "Processing Time (ms)"
            );
    
            for (int i = 0; i < n; i++) {
                NCTestSentence test = tests.get(i);
                NCTestResult res = results.get(i);
    
                List<Object> row = new ArrayList<>();
    
                row.add(res.getDsId());
                row.add(res.getText());
                row.add(test.getExpectedIntentId());
                row.add(res.getIntentId());
                row.add(convertCode(test.getExpectedStatus()));
                row.add(convertCode(res.getResultStatus()));
                row.add(res.getError());
                row.add(res.getProcessingTime());
    
                resTab.addRow(row);
            }
    
            log.info("Test result:\n" + resTab.mkContent());
    
            AsciiTable statTab = new AsciiTable(
                "Tests Count",
                "Passed",
                "Failed",
                "Min Processing Time (ms)",
                "Max Processing Time (ms)",
                "Avg Processing Time (ms)"
            );
    
            List<Object> row = new ArrayList<>();
            
            row.add(n);
            
            long passed = results.stream().filter(p -> !p.hasError()).count();
            
            row.add(passed);
            row.add(n - passed);
    
            OptionalLong min = results.stream().mapToLong(NCTestResult::getProcessingTime).min();
            OptionalLong max = results.stream().mapToLong(NCTestResult::getProcessingTime).max();
            
            assert min.isPresent() && max.isPresent();
            
            row.add(min.getAsLong());
            row.add(max.getAsLong());
            
            double avg = results.stream().mapToDouble(NCTestResult::getProcessingTime).sum() / n;
            
            row.add(Math.round(avg * 100.) / 100.);
    
            statTab.addRow(row);
    
            log.info("Tests statistic:\n" + statTab.mkContent());
        }
    
        private void clearConversationAllDss(String auth, Set<Long> dssIds) throws IOException {
            for (Long dsId : dssIds) {
                clearConversation(auth, dsId);
            
                sleep(delayMs);
            }
        }
    
        @SuppressWarnings("unchecked")
        private void clearConversation(String auth, long dsId) throws IOException {
            checkStatus(
                gson.fromJson(
                    post(cfg.getBaseUrl() + "clear/conv",
                        Pair.of("accessToken", auth),
                        Pair.of("dsId", dsId)
                    ),
                    TYPE_RESP
                )
            );
        }
    
        @SuppressWarnings("unchecked")
        private List<NCTestResult> executeAsync(
            String auth,
            List<NCTestSentence> batch
        ) throws IOException {
            int n = batch.size();
    
            Map<String, NCTestSentence> testsMap = new HashMap<>(n);
            Map<String, NCRequestStateJson> testsResMap = new HashMap<>();
            
            try {
                for (int i = 0; i < n; i++) {
                    NCTestSentence test = batch.get(i);
    
                    String srvReqId =
                        extract(
                            post(cfg.getBaseUrl() + "ask",
                                Pair.of("cliReqId", mkClientRequestId()),
                                Pair.of("accessToken", auth),
                                Pair.of("dsId", test.getDsId()),
                                Pair.of("text", test.getText()),
                                Pair.of("test", true)
                            ),
                            "srvReqId",
                            String.class
                        );
        
                    log.debug("Sentence sent: {}", srvReqId);
        
                    testsMap.put(srvReqId, test);
    
                    if (delayMs > 0 && i != n - 1)
                        sleep(delayMs);
                }
    
                log.debug("Sentences sent: {}", testsMap.size());
    
                Long cliHash = null;
                
                long startTime = System.currentTimeMillis();
    
                while (testsResMap.size() != testsMap.size()) {
                    sleep(checkIntervalMs);
                    
                    if (System.currentTimeMillis() - startTime > MINS_5)
                        throw new NCTestClientException("Timed out (5 minutes) waiting for response.");
        
                    Set<String> ids = new HashSet(testsMap.keySet());
        
                    ids.removeAll(testsResMap.keySet());
        
                    Map<String, Object> data =
                        gson.fromJson(
                            post(cfg.getBaseUrl() + "check",
                                Pair.of("cliReqId", mkClientRequestId()),
                                Pair.of("accessToken", auth),
                                Pair.of("hash", cliHash),
                                Pair.of("srvReqIds", ids)
                            ),
                            TYPE_RESP
                        );
        
                    log.debug("Check request sent");
        
                    checkStatus(data);
        
                    long srvHash = extract(gson.toJsonTree(getField(data, "hash")), Long.class);
        
                    if (cliHash == null || srvHash != cliHash) {
                        List<NCRequestStateJson> reqStates =
                            extract(gson.toJsonTree(getField(data, "states")), TYPE_REQS);
    
                        reqStates.forEach(p -> log.debug("Request state: " + p));
    
                        reqStates = reqStates.
                            stream().
                            filter(p -> !p.getResponseType().toUpperCase().equals("RESP_INITIAL")).
                            collect(Collectors.toList());
            
                        if (!reqStates.isEmpty()) testsResMap.putAll(reqStates.stream().
                            collect(Collectors.toMap(NCRequestStateJson::getServerRequestId, p -> p)));
    
                        log.debug("Request processed: {}", reqStates.size());
                    }
                    else
                        log.debug("Requests state is not changed.");
        
                    cliHash = srvHash;
                }
            }
            finally {
                if (!testsMap.isEmpty())
                    // This potential error can be ignored. Also it shouldn't override main method errors.
                    try {
                        checkStatus(
                            gson.fromJson(
                                post(cfg.getBaseUrl() + "cancelAll",
                                    Pair.of("cliReqId", mkClientRequestId()),
                                    Pair.of("accessToken", auth),
                                    Pair.of("srvReqIds", testsMap.keySet())
                                ),
                                TYPE_RESP
                            )
                        );
                    }
                    catch (Exception e) {
                        log.error("Tests request cancel error: " + testsMap.keySet(), e);
                    }
            }
            
            return testsResMap.entrySet().stream().map(p -> {
                NCTestSentence test = testsMap.get(p.getKey());
                NCRequestStateJson testRes = p.getValue();
        
                return new NCTestResult() {
                    private int respStatus = convertResponse(testRes.getResponseType());
                    private long dsId = testRes.getDsId();
                    private String text = test.getText();
                    private long procTime = testRes.getUpdateTstamp() - testRes.getCreateTstamp();
                    private String intentId = (String)testRes.getResultMetadata().get("intentId");
                    private String err = null;
            
                    {
                        if (test.getExpectedIntentId() != null && !test.getExpectedIntentId().equals(intentId))
                            err = "Invalid intent "
                                + "ID [expected=" + test.getExpectedIntentId() +
                                ", actual=" + intentId +
                                ']';
                        else if (test.getExpectedStatus() != respStatus)
                            err = "Invalid response status " +
                                "[expected=" + convertCode(test.getExpectedStatus())  +
                                ", actual=" + convertCode(respStatus) +
                                ']';
                        else if (test.getCheck() != null) {
                            String body = testRes.getResultBody();
                            String type = testRes.getResultType();
    
                            if (body == null || type == null)
                                err = "Result cannot be checked [body=" + body  + ", type=" + type + ']';
                            else {
                                NCQueryResult res = new NCQueryResult();
    
                                res.setBody(body);
                                res.setType(type);
    
                                if (!test.getCheck().test(res))
                                    err = "Result has not satisfied user validation";
                            }
                        }
                    }
            
                    @Override
                    public long getDsId() {
                        return dsId;
                    }
            
                    @Override
                    public String getText() {
                        return text;
                    }
            
                    @Override
                    public int getResultStatus() {
                        return respStatus;
                    }
            
                    @Override
                    public long getProcessingTime() {
                        return procTime;
                    }
            
                    @Override
                    public String getError() {
                        return err;
                    }
            
                    @Override
                    public String getIntentId() {
                        return intentId;
                    }
            
                    @Override
                    public String toString() {
                        return "NCTestResult " +
                            "[dsId=" + dsId +
                            ", text=" + text +
                            ", intentId=" + intentId +
                            ", respStatus=" + convertCode(respStatus) +
                            ", procTime=" + procTime +
                            ", err=" + err +
                            ']';
                    }
                };
            }).collect(Collectors.toList());
        }
    
        private String mkClientRequestId() {
            return UUID.randomUUID().toString();
        }
    
        private void sleep(long time) {
            if (time > 0) {
                log.debug("Sleep time: {}", time);
                
                try {
                    Thread.sleep(time);
                }
                catch (InterruptedException e) {
                    throw new NCTestClientException("Thread interrupted.", e);
                }
            }
        }
    }
    
    private static void notNull(String name, Object val) throws IllegalArgumentException {
        if (val == null)
            throw new IllegalArgumentException(String.format("Argument cannot be null: %s", name));
    }
    
    /**
     * Creates new default builder instance.
     *
     * @return Builder instance.
     */
    public static NCTestClientBuilder newBuilder() {
        return new NCTestClientBuilder();
    }
    
    /**
     * Sets HTTP REST client configuration parameters.
     *
     * @param reqCfg HTTP REST client configuration parameters.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder withConfig(RequestConfig reqCfg) {
        notNull("reqCfg", reqCfg);
        
        this.reqCfg = reqCfg;
        
        return this;
    }
    
    /**
     * Sets check result delay value in milliseconds.
     * Default values is {@link NCTestClientBuilder#DFLT_CHECK_INTERVAL_MS}. This value should be changed
     * only in cases when account's usage quota is exceeded.
     *
     * @param checkIntervalMs Delay value in milliseconds.
     * @return Builder instance for chaining calls.
     * @see #withDelay(long)
     */
    public NCTestClientBuilder withCheckInterval(long checkIntervalMs) {
        if (checkIntervalMs <= 0)
            throw new IllegalArgumentException(String.format("Parameter must be positive: %d", checkIntervalMs));
        
        this.checkIntervalMs = checkIntervalMs;
        
        return this;
    }
    
    /**
     * Sets whether or not test sentences will be processed in parallel (async mode) or one by one (sync mode).
     * Note that only synchronous mode make sense when testing with conversation support. Default values
     * is {@link NCTestClientBuilder#DFLT_CLEAR_CONVERSATION}.
     *
     * @param asyncMode {@code true} for asynchronous (parallel) mode, {@code false} for synchronous mode.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder withAsyncMode(boolean asyncMode) {
        this.asyncMode = asyncMode;
        
        return this;
    }
    
    /**
     * Sets request delay value. This value should be changed only in cases when account's usage quota is exceeded.
     * Default values is {@link NCTestClientBuilder#DFLT_DELAY_MS}.
     *
     * @param delayMs Request delay in milliseconds.
     * @return Builder instance for chaining calls.
     * @see #withCheckInterval(long)
     */
    public NCTestClientBuilder withDelay(long delayMs) {
        if (delayMs < 0)
            throw new IllegalArgumentException(String.format("Parameter must not be negative: %d", delayMs));
        
        this.delayMs = delayMs;
    
        return this;
    }
    
    /**
     * Sets whether or not to clear conversation after each test request.
     * Note, that if this flag set as {@code false}, requests always sent in synchronous (one-by-one) mode.
     * Default values is {@link NCTestClientBuilder#DFLT_CLEAR_CONVERSATION}.
     *
     * @param clearConv Whether or not to clear conversation after each test request.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder withClearConversation(boolean clearConv) {
        this.clearConv = clearConv;
        
        return this;
    }
    
    /**
     * Build new configured test client instance.
     *
     * @param cfg Mandatory test configuration.
     * @return Newly built test client instance.
     */
    public NCTestClient build(NCTestClientConfig cfg) {
        notNull("cfg", cfg);
        
        return new NCTestClientImpl(cfg);
    }
}
