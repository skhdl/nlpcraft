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
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test client builder for {@link NCTestClient} instances. Note that all configuration values
 * have sensible defaults. Most of the time only user {@link #setUser(String, String) credentials}
 * will have to be changed if not testing with default account.
 */
public class NCTestClientBuilder {
    /** Default public REST API URL (endpoint). */
    public static final String DFLT_BASEURL = "http://localhost:8081/api/v1/";
    
    /** Default client email. */
    public static final String DFLT_EMAIL = "admin@admin.com";
    
    /** Default client password. */
    public static final String DFLT_PASSWORD = "admin";
    
    /** Default maximum statuses check time, millisecond. */
    public static final long DFLT_MAX_CHECK_TIME = 10 * 1000;
    
    /** Default millisecond delay between result checks. */
    public static final long DFLT_CHECK_INTERVAL_MS = 2000;
    
    /** Default clear conversation flag value. */
    public static final boolean DFLT_CLEAR_CONVERSATION = false;
    
    /** Default asynchronous mode flag value. */
    public static final boolean DFLT_ASYNC_MODE = true;
    
    private static final Logger log = LoggerFactory.getLogger(NCTestClientBuilder.class);
    
    private NCTestClientImpl impl;
    
    /**
     * JSON helper class.
     */
    static class NCDsJson {
        @SerializedName("id") private long dsId;
        @SerializedName("mdlId") private String mdlId;
    
        public long getDatasourceId() {
            return dsId;
        }
    
        public void setDatasourceId(long dsId) {
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
    
        public long getDatasourceId() {
            return dsId;
        }
    
        public void setDatasourceId(long dsId) {
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
    
    private static class AsciiTable {
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
    
    private static class IdHolder {
        private final long dsId;
        private final String mdlId;
    
        IdHolder(long dsId, String mdlId) {
            this.dsId = dsId;
            this.mdlId = mdlId;
        }
    
        long getDatasourceId() {
            return dsId;
        }
    
        String getModelId() {
            return mdlId;
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
        private final CloseableHttpClient client;
    
        private long checkIntervalMs = DFLT_CHECK_INTERVAL_MS;
        private boolean clearConv = DFLT_CLEAR_CONVERSATION;
        private boolean asyncMode = DFLT_ASYNC_MODE;
        private long maxCheckTimeMs = DFLT_MAX_CHECK_TIME;
        private RequestConfig reqCfg;
        private String baseUrl = DFLT_BASEURL;
        private String email = DFLT_EMAIL;
        private String pswd = DFLT_PASSWORD;
        private Supplier<CloseableHttpClient> cliSup;
    
    
        NCTestClientImpl() {
            this.client = mkClient();
        }
    
        long getCheckInterval() {
            return checkIntervalMs;
        }
    
        boolean isClearConversation() {
            return clearConv;
        }
    
        boolean isAsyncMode() {
            return asyncMode;
        }
    
        long getMaxCheckTime() {
            return maxCheckTimeMs;
        }
    
        RequestConfig getRequestConfig() {
            return reqCfg;
        }
    
        String getBaseUrl() {
            return baseUrl;
        }
    
        String getEmail() {
            return email;
        }
    
        String getPassword() {
            return pswd;
        }
    
        Supplier<CloseableHttpClient> getClientSupplier() {
            return cliSup;
        }
    
        void setCheckInterval(long checkIntervalMs) {
            this.checkIntervalMs = checkIntervalMs;
        }
    
        void setClearConversation(boolean clearConv) {
            this.clearConv = clearConv;
        }
    
        void setAsyncMode(boolean asyncMode) {
            this.asyncMode = asyncMode;
        }
    
        void setMaxCheckTime(long maxCheckTimeMs) {
            this.maxCheckTimeMs = maxCheckTimeMs;
        }
    
        void setRequestConfig(RequestConfig reqCfg) {
            this.reqCfg = reqCfg;
        }
    
        void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    
        void setEmail(String email) {
            this.email = email;
        }
    
        void setPasword(String pswd) {
            this.pswd = pswd;
        }
    
        void setClientSupplier(Supplier<CloseableHttpClient> cliSup) {
            this.cliSup = cliSup;
        }
    
        private CloseableHttpClient mkClient() {
            return cliSup != null ? cliSup.get() : HttpClients.createDefault();
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
                    gson.toJson(
                        Arrays.stream(ps).
                            filter(p -> p.getValue() != null).
                            collect(Collectors.toMap(Pair::getKey, Pair::getValue))
                    )
                );
    
                post.setHeader("Content-Type", "application/json");
                post.setEntity(entity);
            
                log.trace("Request prepared: {}", post);
            
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
            
                String s = client.execute(post, h);
            
                log.trace("Response received: {}", s);
            
                return s;
            }
            finally {
                post.releaseConnection();
            }
        }
    
        @Override
        public List<NCTestResult> test(NCTestSentence... tests) throws NCTestClientException, IOException {
            return test(Arrays.asList(tests));
        }
        
        private <T> void checkDups(
            List<NCTestSentence> tests,
            Function<NCTestSentence, T> extractField,
            String fieldName
        ) {
            List<Pair<String, T>> allTestPairs =
                tests.stream().
                    map(p -> Pair.of(p.getText(), extractField.apply(p))).
                    filter(p -> p.getRight() != null).
                    collect(Collectors.toList());
    
            List<Pair<String, T>> testsPairs = allTestPairs.stream().distinct().collect(Collectors.toList());
    
            if (testsPairs.size() != allTestPairs.size()) {
                allTestPairs.removeAll(testsPairs);
        
                String s =
                    allTestPairs.stream().
                        map(p -> "sentence=" + p.getLeft() + ", " + fieldName + "=" + p.getRight()).
                        collect(Collectors.joining(";", "[", "]"));
        
                throw new NCTestClientException("Sentences texts cannot be duplicated within same " + fieldName + ": " + s);
            }
        }
    
        @Override
        public synchronized List<NCTestResult> test(List<NCTestSentence> tests)
            throws NCTestClientException, IOException {
            checkNotNull("tests", tests);
            
            checkDups(tests, NCTestSentence::getDatasourceId, "datasource");
            checkDups(tests, NCTestSentence::getModelId, "model");
            
            Set<String> mdlIds =
                tests.stream().
                    filter(p -> p.getModelId().isPresent()).
                    map(p -> p.getModelId().get()).
                    collect(Collectors.toSet());
            
            String auth = signin();
    
            Map<String, Long> newDssIds = new HashMap<>();
            
            int num = 0;
            
            for (String mdlId : mdlIds) {
                newDssIds.put(mdlId, createTestDs(auth, mdlId, num++));
            }
    
            Map<NCTestSentence, NCTestResult> res = new HashMap<>();
            
            try {
                Map<Long, String> dssMdlIds =
                    getDss(auth).stream().collect(Collectors.toMap(NCDsJson::getDatasourceId, NCDsJson::getModelId));
    
                Map<NCTestSentence, IdHolder> testsExt =
                    tests.stream().collect(
                        Collectors.toMap(
                            p -> p,
                            p -> {
                                long dsId;
                                
                                if (p.getDatasourceId().isPresent())
                                    dsId = p.getDatasourceId().get();
                                else {
                                    assert p.getModelId().isPresent();
    
                                    dsId = newDssIds.get(p.getModelId().get());
                                }
                                
                                return new IdHolder(dsId, dssMdlIds.get(dsId));
                            }
                        )
                    );
                
                Function<NCTestSentence, Map<NCTestSentence, IdHolder>> mkSingleMap = (t) -> {
                    Map<NCTestSentence, IdHolder> m = new HashMap<>();
                    
                    m.put(t, testsExt.get(t));
                    
                    return m;
                };
    
                if (clearConv) {
                    for (NCTestSentence test : tests) {
                        clearConversation(auth, testsExt.get(test).getDatasourceId());
        
                        res.putAll(executeAsync(auth, mkSingleMap.apply(test)));
                    }
                }
                else {
                    Set<Long> dsIds =
                        tests.stream().map(t -> testsExt.get(t).getDatasourceId()).collect(Collectors.toSet());
                    
                    if (asyncMode) {
                        clearConversationAllDss(auth, dsIds);
    
                        res.putAll(executeAsync(auth, testsExt));
                    }
                    else {
                        clearConversationAllDss(auth, dsIds);
    
                        for (NCTestSentence test : tests) {
                            res.putAll(executeAsync(auth, mkSingleMap.apply(test)));
                        }
                    }
                }
            }
            catch (InterruptedException e) {
                throw new NCTestClientException("Test interrupted.", e);
            }
            finally {
                // This potential error can be ignored. Also it shouldn't override main method errors.
                try {
                    for (Long id : newDssIds.values()) {
                        deleteTestDs(auth, id);
                    }
                    
                    signout(auth);
                }
                catch (Exception e) {
                    log.error("Signout error.", e);
                }
            }
    
            List<NCTestResult> list =
                res.entrySet().
                    stream().
                    sorted(Comparator.comparingInt(o -> tests.indexOf(o.getKey()))).
                    map(Map.Entry::getValue).
                    collect(Collectors.toList());
            
            printResult(tests, list);
    
            return list;
        }
    
        private void printResult(List<NCTestSentence> tests, List<NCTestResult> results) {
            assert tests != null && results != null;
            assert !tests.isEmpty();
            assert tests.size() == results.size();
            
            int n = tests.size();
            
            AsciiTable resTab = new AsciiTable(
                "Sentence",
                "Datasource ID",
                "Model ID",
                "Result",
                "Error",
                "Processing Time (ms)"
            );
    
            for (NCTestResult res : results) {
                List<Object> row = new ArrayList<>();
    
                String ss = res.getText().substring(0, 100);
    
                row.add(ss.equals(res.getText()) ? ss : ss + " ...");
                row.add(res.getDatasourceId());
                row.add(res.getModelId());
                row.add(res.getResult().orElse(""));
                row.add(res.getResultError().orElse(""));
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
            
            long passed = results.stream().filter(p -> !p.getResultError().isPresent()).count();
            
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
    
        private void clearConversationAllDss(String auth, Set<Long> dssIds) throws IOException, NCTestClientException {
            for (Long dsId : dssIds) {
                clearConversation(auth, dsId);
            }
        }
    
        private void clearConversation(String auth, long dsId) throws IOException, NCTestClientException {
            log.info("`clear/conversation` request sent for datasource: {}", dsId);
            
            checkStatus(
                gson.fromJson(
                    post("clear/conversation",
                        Pair.of("accessToken", auth),
                        Pair.of("dsId", dsId)
                    ),
                    TYPE_RESP
                )
            );
        }
    
        private void cancel(String auth, Set<String> ids) throws IOException, NCTestClientException {
            log.info("`cancel` request sent for requests: {}", ids);
            
            checkStatus(
                gson.fromJson(
                    post("cancel",
                        Pair.of("accessToken", auth),
                        Pair.of("srvReqIds", ids)
                    ),
                    TYPE_RESP
                )
            );
        }
    
        private long createTestDs(String auth, String mdlId, long num) throws IOException, NCTestClientException {
            log.info("`ds/add` request sent for model: {}", mdlId);
            
            long id =
                checkAndExtract(
                    post("ds/add",
                        Pair.of("accessToken", auth),
                        Pair.of("name", "test-" + num),
                        Pair.of("shortDesc", "Test datasource"),
                        Pair.of("mdlId", mdlId),
                        Pair.of("mdlName", "Test model"),
                        Pair.of("mdlVer", "Test version")
            
                    ),
                    "id",
                    Long.class
               );
    
            log.info("Temporary test datasource created: {}", id);
            
            return id;
        }
        
        private void deleteTestDs(String auth, long id) throws IOException, NCTestClientException {
            log.info("`ds/delete` request sent for model: {}", id);
            
            checkStatus(
                gson.fromJson(
                    post("ds/delete",
                        Pair.of("accessToken", auth),
                        Pair.of("id", id)
                    ),
                    TYPE_RESP
                )
            );
        }
    
        private String signin() throws IOException, NCTestClientException {
            log.info("`user/signin` request sent for: {}", email);
            
            return checkAndExtract(
                post("user/signin",
                    Pair.of("email", email),
                    Pair.of("passwd", pswd)
                    
                ),
                "accessToken",
                String.class
            );
        }
    
        private List<NCDsJson> getDss(String auth) throws IOException, NCTestClientException {
            log.info("`ds/all` request sent for: {}", email);
    
            Map<String, Object> m = gson.fromJson(
                post("ds/all",
                    Pair.of("accessToken", auth)
                ),
                TYPE_RESP
            );
    
            checkStatus(m);
    
            return extract(gson.toJsonTree(getField(m, "dataSources")), TYPE_DSS);
        }
    
        private List<NCRequestStateJson> check(String auth) throws IOException, NCTestClientException {
            log.info("`check` request sent for: {}", email);
        
            Map<String, Object> m = gson.fromJson(
                post("check",
                    Pair.of("accessToken", auth)
                ),
                TYPE_RESP
            );
        
            checkStatus(m);
        
            return extract(gson.toJsonTree(getField(m, "states")), TYPE_STATES);
        }
    
        private String ask(String auth, String txt, long dsId) throws IOException, NCTestClientException {
            log.info("`ask` request sent: {} to datasource: {}", txt, dsId);
        
            return checkAndExtract(
                post("ask",
                    Pair.of("accessToken", auth),
                    Pair.of("txt", txt),
                    Pair.of("dsId", dsId),
                    Pair.of("isTest", true)
                ),
                "srvReqId",
                String.class
            );
        }
    
        private void signout(String auth) throws IOException, NCTestClientException {
            log.info("`user/signout` request sent for: {}", email);
            
            checkStatus(
                gson.fromJson(
                    post("user/signout",
                        Pair.of("accessToken", auth)
                    ),
                    TYPE_RESP
                )
            );
        }
    
        private Map<NCTestSentence, NCTestResult> executeAsync(
            String auth,
            Map<NCTestSentence, IdHolder> tests
        ) throws IOException, InterruptedException {
            int n = tests.size();
    
            Map<String, NCTestSentence> testsMap = new HashMap<>(n);
            Map<String, NCRequestStateJson> testsResMap = new HashMap<>();
            Map<NCTestSentence, Pair<String, Long>> askErrTests = new HashMap<>();
            
            try {
                for (Map.Entry<NCTestSentence, IdHolder> entry : tests.entrySet()) {
                    NCTestSentence test = entry.getKey();
                    IdHolder h = entry.getValue();
                    
                    long now = System.currentTimeMillis();
    
                    try {
                        String srvReqId = ask(auth, test.getText(), h.getDatasourceId());
        
                        log.debug("Sentence sent: {}", srvReqId);
        
                        testsMap.put(srvReqId, test);
                    }
                    catch (NCTestClientException e) {
                        askErrTests.put(test, Pair.of(e.getMessage(), System.currentTimeMillis() - now));
                    }
                }
    
                log.debug("Sentences sent: {}", testsMap.size());
    
                long startTime = System.currentTimeMillis();
    
                while (testsResMap.size() != testsMap.size()) {
                    if (System.currentTimeMillis() - startTime > maxCheckTimeMs)
                        throw new NCTestClientException(
                            String.format("Timed out waiting for response: %d", maxCheckTimeMs)
                        );
        
                    List<NCRequestStateJson> states = check(auth);
                    
                    Thread.sleep(checkIntervalMs);
    
                    Map<String, NCRequestStateJson> res =
                        states.stream().
                        filter(p -> p.getStatus().equals("QRY_READY")).
                        collect(Collectors.toMap(NCRequestStateJson::getServerRequestId, p -> p));
    
                    testsResMap.putAll(res);
    
                    long newResps = res.keySet().stream().filter(p -> !testsResMap.containsKey(p)).count();
    
                    log.debug("Request processed: {}", newResps);
                }
            }
            finally {
                if (!testsMap.isEmpty())
                    // This potential error can be ignored. Also it shouldn't override main method errors.
                    try {
                        cancel(auth, testsMap.keySet());
                    }
                    catch (Exception e) {
                        log.error("Tests request cancel error: " + testsMap.keySet(), e);
                    }
            }
    
            return Stream.concat(
                testsResMap.entrySet().stream().map(p -> {
                    NCTestSentence test = testsMap.get(p.getKey());
                    NCRequestStateJson testRes = p.getValue();
    
                    IdHolder h = tests.get(test);
                    
                    return
                        Pair.of(
                            test,
                            mkResult(
                                test,
                                testRes.getUpdateTstamp() - testRes.getCreateTstamp(),
                                h.getDatasourceId(),
                                h.getModelId(),
                                testRes.getResultBody(),
                                testRes.getResultType(),
                                testRes.getError()
                            )
                        );
                }),
                askErrTests.entrySet().stream().map(p -> {
                    NCTestSentence test = p.getKey();
                    String err = p.getValue().getLeft();
                    long time = p.getValue().getRight();
    
                    IdHolder h = tests.get(test);
    
                    return Pair.of(
                        test,
                        mkResult(
                            test, time, h.getDatasourceId(), h.getModelId(), null, null, err
                        )
                    );
                })
            ).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        }
    }
    
    private static NCTestResult mkResult(
        NCTestSentence test, long procTime, long dsId, String mdlId, String res, String resType, String err
    ) {
        assert test != null;
        assert mdlId != null;
        assert (res != null && resType != null) ^ err != null;
        
        return new NCTestResult() {
            private Optional<String>convert(String s) {
                return s == null ? Optional.empty() : Optional.of(s);
            }
            
            @Override
            public String getText() {
                return test.getText();
            }
    
            @Override
            public long getProcessingTime() {
                return procTime;
            }
    
            @Override
            public long getDatasourceId() {
                return dsId;
            }
    
            @Override
            public String getModelId() {
                return mdlId;
            }
    
            @Override
            public Optional<String> getResult() {
                return convert(res);
            }
    
            @Override
            public Optional<String> getResultType() {
                return convert(resType);
            }
            
            @Override
            public Optional<String> getResultError() {
                return convert(err);
            }
        };
    }
    
    private static void checkNotNull(String name, Object v) throws IllegalArgumentException {
        if (v == null)
            throw new IllegalArgumentException(String.format("Argument cannot be null: '%s'", name));
    }
    
    private static void checkPositive(String name, long v) throws IllegalArgumentException {
        if (v <= 0)
            throw new IllegalArgumentException(String.format("Argument '%s' must be positive: %d", name, v));
    }
    
    /**
     * Creates new default builder instance.
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
    public NCTestClientBuilder setConfig(RequestConfig reqCfg) {
        impl.setRequestConfig(reqCfg);
        
        return this;
    }
    
    /**
     * Sets check result delay value in milliseconds.
     * Default values is {@link NCTestClientBuilder#DFLT_CHECK_INTERVAL_MS}.
     *
     * @param checkIntervalMs Result check delay value in milliseconds.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder setCheckInterval(long checkIntervalMs) {
        impl.setCheckInterval(checkIntervalMs);
        
        return this;
    }
    
    /**
     * Sets whether or not process sentences in parallel (async mode) or one by one (sync mode).
     * Note that only synchronous mode make sense when testing with conversation support. Default values
     * is {@link NCTestClientBuilder#DFLT_CLEAR_CONVERSATION}.
     *
     * @param asyncMode {@code true} for asynchronous (parallel) mode, {@code false} for synchronous mode.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder setAsyncMode(boolean asyncMode) {
        impl.setAsyncMode(asyncMode);
        
        return this;
    }
    
    /**
     * Sets whether or not to clear conversation after each test request.
     * Note that if this flag set to {@code false}, requests always sent in synchronous (one-by-one) mode.
     * Default values is {@link NCTestClientBuilder#DFLT_CLEAR_CONVERSATION}.
     *
     * @param clearConv Whether or not to clear conversation after each test request.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder setClearConversation(boolean clearConv) {
        impl.setClearConversation(clearConv);
        
        return this;
    }
    
    /**
     * Sets {@link CloseableHttpClient} custom supplier.
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
     * Sets API base URL.
     * By default {@link NCTestClientBuilder#DFLT_BASEURL} is used.
     *
     * @param baseUrl API base URL.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder setBaseUrl(String baseUrl) {
        String s = baseUrl;
        
        if (!s.endsWith("/"))
            s += '/';
        
        impl.setBaseUrl(s);
    
        return this;
    }
    
    /**
     * Sets user credentials.
     * By default {@link NCTestClientBuilder#DFLT_EMAIL} and {@link NCTestClientBuilder#DFLT_PASSWORD} are used.
     *
     * @param email User email.
     * @param pswd User password.
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder setUser(String email, String pswd) {
        impl.setEmail(email);
        impl.setPasword(pswd);
     
        return this;
    }
    
    /**
     * Sets maximum check time. It is maximum time for waiting for the processing completion.
     * By default {@link NCTestClientBuilder#DFLT_MAX_CHECK_TIME} is used.
     *
     * @param maxCheckTimeMs Maximum check time (ms).
     * @return Builder instance for chaining calls.
     */
    public NCTestClientBuilder setMaxCheckTime(long maxCheckTimeMs) {
        impl.setMaxCheckTime(maxCheckTimeMs);
    
        return this;
    }
    
    /**
     * Build new configured test client instance.
     *
     * @return Newly built test client instance.
     */
    public NCTestClient build() {
        checkPositive("maxCheckTimeMs", impl.getMaxCheckTime());
        checkNotNull("email", impl.getEmail());
        checkNotNull("pswd", impl.getPassword());
        checkNotNull("baseUrl", impl.getBaseUrl());
        checkPositive("checkIntervalMs", impl.getCheckInterval());

        return impl;
    }
}
