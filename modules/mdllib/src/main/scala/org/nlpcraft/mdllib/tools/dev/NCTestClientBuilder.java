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
import org.nlpcraft.mdllib.NCQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Test client builder for {@link NCTestClient} instances.
 */
public class NCTestClientBuilder {
    /** Default public REST API URL (endpoint). */
    public static final String DFLT_BASEURL = "http://localhost:8081/api/v1/";
    
    public static final String DFLT_EMAIL = "admin@admin.com";
    public static final String DFLT_PASSWORD = "admin";
    
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
    
    private String baseUrl = DFLT_BASEURL;
    private String email = DFLT_EMAIL;
    private String pswd = DFLT_PASSWORD;
    private Supplier<CloseableHttpClient> cliSup;
    
    /**
     * JSON helper class.
     */
    static class NCRequestStateJson {
        @SerializedName("srvReqId") private String serverRequestId;
        @SerializedName("usrId") private long userId;
        @SerializedName("dsId") private long dsId;
        @SerializedName("resType") private String resultType;
        @SerializedName("resBody") private String resultBody;
        @SerializedName("status") private String status;
        @SerializedName("error") private String error;
        @SerializedName("createTstamp") private long createTstamp;
        @SerializedName("updateTstamp") private long updateTstamp;
    
        public String getServerRequestId() {
            return serverRequestId;
        }
    
        public void setServerRequestId(String serverRequestId) {
            this.serverRequestId = serverRequestId;
        }
    
        public long getUserId() {
            return userId;
        }
    
        public void setUserId(long userId) {
            this.userId = userId;
        }
    
        public long getDsId() {
            return dsId;
        }
    
        public void setDsId(long dsId) {
            this.dsId = dsId;
        }
    
        public String getStatus() {
            return status;
        }
    
        public void setStatus(String status) {
            this.status = status;
        }
    
        public String getResultType() {
            return resultType;
        }
    
        public void setResultType(String resultType) {
            this.resultType = resultType;
        }
    
        public String getResultBody() {
            return resultBody;
        }
    
        public void setResultBody(String resultBody) {
            this.resultBody = resultBody;
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
    
    /**
     * Client implementation.
     */
    private class NCTestClientImpl implements NCTestClient {
        private static final String STATUS_API_OK = "API_OK";
        private final Type TYPE_RESP = new TypeToken<HashMap<String, Object>>() {}.getType();
        private final Type TYPE_REQS = new TypeToken<ArrayList<NCRequestStateJson>>() {}.getType();
    
        private final Gson gson = new Gson();
        
        private final CloseableHttpClient client;
    
        NCTestClientImpl() {
            this.client = mkClient();
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
    
        private <T> T extract(String js, String name, Type type) throws NCTestClientException {
            Map<String, Object> m = gson.fromJson(js, TYPE_RESP);
        
            checkStatus(m);
        
            return extract(gson.toJsonTree(getField(m, name)), type);
        }
        
        @SafeVarargs
        private final String post(String url, Pair<String, Object>... ps) throws NCTestClientException, IOException {
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
                        case 400: throw new NCTestClientException(String.format("Server error: %s", js));
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
            notNull("tests", tests);
            
            if (tests.isEmpty())
                throw new IllegalArgumentException("Tests cannot be empty");
    
            checkDups(tests, NCTestSentence::getDatasourceId, "datasource");
            checkDups(tests, NCTestSentence::getModelId, "model");
    
            Set<String> mdlIds =
                tests.stream().
                    filter(p -> p.getModelId() != null).
                    map(NCTestSentence::getModelId).
                    collect(Collectors.toSet());
            
            if (delayMs != 0) {
                int minApiCalls =
                    clearConv ?
                        // - for each test: 'conv/clear', 'ask', 'check'.
                        3 * tests.size() :
                        // - 'conv/clear' - calls for each data source before test.
                        // - 'ask' for each test
                        // - at least one 'check'
                        (int)tests.stream().map(NCTestSentence::getDatasourceId).distinct().count() + tests.size() + 1;
    
                
                // - 3 - 'signin', 'ds/all', 'signout',
                minApiCalls += 3;
                // Create and delete test datasources based on model ID.
                minApiCalls += mdlIds.size() * 2;
                
                if (minApiCalls * delayMs > HOUR)
                    throw new NCTestClientException("Test too long, decrease delay or sentences count");
            }
            
            String auth = signin();
    
            sleep(delayMs);
    
            Map<String, Long> newDssIds = new HashMap<>();
            
            int num = 0;
            
            for (String mdlId : mdlIds) {
                newDssIds.put(mdlId, createTestDs(auth, mdlId, num++));
    
                sleep(delayMs);
            }
            
            Function<NCTestSentence, Long> getDsId = (s) -> {
                if (s.getDatasourceId() != null)
                    return s.getDatasourceId();
    
                Long dsId = newDssIds.get(s.getModelId());
                
                assert dsId != null;
                
                return dsId;
            };
    
            List<NCTestResult> res = new ArrayList<>();
            
            try {
                if (clearConv) {
                    for (NCTestSentence test : tests) {
                        clearConversation(auth, getDsId.apply(test));
        
                        sleep(delayMs);
        
                        res.addAll(executeAsync(auth, Collections.singletonList(test), getDsId));
                    }
                }
                else {
                    Set<Long> dsIds = tests.stream().map(getDsId).collect(Collectors.toSet());
                    
                    if (asyncMode) {
                        clearConversationAllDss(auth, dsIds);
    
                        res.addAll(executeAsync(auth, tests, getDsId));
                    }
                    else {
                        clearConversationAllDss(auth, dsIds);
    
                        for (NCTestSentence test : tests) {
                            res.addAll(executeAsync(auth, Collections.singletonList(test), getDsId));
        
                            sleep(delayMs);
                        }
                    }
                }
            }
            finally {
                // This potential error can be ignored. Also it shouldn't override main method errors.
                try {
                    sleep(delayMs);
    
                    for (Long id : newDssIds.values()) {
                        deleteTestDs(auth, id);
    
                        sleep(delayMs);
                    }
                    
                    signout(auth);
                }
                catch (Exception e) {
                    log.error("Signout error.", e);
                }
            }
    
            // TODO:
            // res.sort(Comparator.comparingInt(o -> testsPairs.indexOf(Pair.of(o.getText(), o.getDsId()))));
    
            printResult(tests, res);
    
            return res;
        }
        
    
        private void printResult(List<NCTestSentence> tests, List<NCTestResult> results) {
            assert tests != null && results != null;
            assert !tests.isEmpty();
            assert tests.size() == results.size();
            
            int n = tests.size();
            
            AsciiTable resTab = new AsciiTable(
                "Sentence",
                "Expected Result",
                "Has checked function",
                "Result",
                "Error",
                "Processing Time (ms)"
            );
    
            for (int i = 0; i < n; i++) {
                NCTestSentence test = tests.get(i);
                NCTestResult res = results.get(i);
    
                List<Object> row = new ArrayList<>();
    
                row.add(res.getText());
                row.add(test.isSuccessful());
                row.add(test.isSuccessful() ? test.getCheckResult() != null  : test.getCheckError() != null);
                row.add(res.getResult());
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
    
        private void clearConversation(String auth, long dsId) throws IOException {
            log.info("`clear/conversation` request sent for datasource: {}", dsId);
            
            checkStatus(
                gson.fromJson(
                    post(baseUrl + "clear/conversation",
                        Pair.of("accessToken", auth),
                        Pair.of("dsId", dsId)
                    ),
                    TYPE_RESP
                )
            );
        }
    
        private void cancel(String auth, Set<String> ids) throws IOException {
            log.info("`cancel` request sent for requests: {}", ids);
            
            checkStatus(
                gson.fromJson(
                    post(baseUrl + "cancel",
                        Pair.of("accessToken", auth),
                        Pair.of("srvReqIds", ids)
                    ),
                    TYPE_RESP
                )
            );
        }
    
        private long createTestDs(String auth, String mdlId, long num) throws IOException {
            log.info("`ds/add` request sent for model: {}", mdlId);
            
            long id =
                extract(
                    post(baseUrl + "ds/add",
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
        
        private void deleteTestDs(String auth, long id) throws IOException {
            log.info("`ds/delete` request sent for model: {}", id);
            
            checkStatus(
                gson.fromJson(
                    post(baseUrl + "ds/delete",
                        Pair.of("accessToken", auth),
                        Pair.of("id", id)
                    ),
                    TYPE_RESP
                )
            );
        }
    
        private String signin() throws IOException {
            log.info("`user/signin` request sent for: {}", email);
            
            // TODO: check status
            return extract(
                post(
                    baseUrl + "user/signin",
                    Pair.of("email", email),
                    Pair.of("passwd", pswd)
                    
                ),
                "accessToken",
                String.class
            );
        }
    
        private void signout(String auth) throws IOException {
            log.info("`user/signout` request sent for: {}", email);
            
            checkStatus(
                gson.fromJson(
                    post(baseUrl + "user/signout",
                        Pair.of("accessToken", auth)
                    ),
                    TYPE_RESP
                )
            );
        }
    
        private List<NCTestResult> executeAsync(
            String auth,
            List<NCTestSentence> batch,
            Function<NCTestSentence, Long> getDsId
        ) throws IOException {
            int n = batch.size();
    
            Map<String, NCTestSentence> testsMap = new HashMap<>(n);
            Map<String, NCRequestStateJson> testsResMap = new HashMap<>();
            
            try {
                for (int i = 0; i < n; i++) {
                    NCTestSentence test = batch.get(i);
    
                    String srvReqId = ask(auth, test.getText(), getDsId.apply(test));
        
                    log.debug("Sentence sent: {}", srvReqId);
        
                    testsMap.put(srvReqId, test);
    
                    if (delayMs > 0 && i != n - 1)
                        sleep(delayMs);
                }
    
                log.debug("Sentences sent: {}", testsMap.size());
    
                long startTime = System.currentTimeMillis();
    
                while (testsResMap.size() != testsMap.size()) {
                    sleep(checkIntervalMs);
                    
                    if (System.currentTimeMillis() - startTime > MINS_5)
                        throw new NCTestClientException("Timed out (5 minutes) waiting for response.");
        
                    List<NCRequestStateJson> states = check(auth);
    
                    Map<String, NCRequestStateJson> res =
                        states.stream().
                        filter(p -> p.getStatus().equals("QRY_READY")).
                        collect(Collectors.toMap(NCRequestStateJson::getServerRequestId, p -> p));
    
                    long newResps = res.keySet().stream().filter(p -> !testsResMap.containsKey(p)).count();
    
                    testsResMap.putAll(res);
                    
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
            
            return testsResMap.entrySet().stream().map(p -> {
                NCTestSentence test = testsMap.get(p.getKey());
                NCRequestStateJson testRes = p.getValue();
                
                return new NCTestResult() {
                    private String res;
                    private String err = null;
                    
                    {
                        if (test.isSuccessful()) {
                            res = testRes.getResultBody();
                            
                            if (test.getCheckResult() != null) {
                                NCQueryResult res = new NCQueryResult();
    
                                res.setType(testRes.getResultType());
                                res.setBody(testRes.getResultBody());
    
                                if (!test.getCheckResult().test(res))
                                    err = "Check result function invocation was not successful";
                            }
                        }
                        else {
                            res = testRes.getError();
                            
                            if (test.getCheckError() != null && !test.getCheckError().test(testRes.getError()))
                                err = "Check error function invocation was not successful";
                        }
                    }
    
                    @Override
                    public String getResult() {
                        return res;
                    }
    
                    @Override
                    public String getError() {
                        return err;
                    }
    
                    @Override
                    public String getText() {
                        return test.getText();
                    }
    
                    @Override
                    public long getProcessingTime() {
                        return testRes.getUpdateTstamp() - testRes.getCreateTstamp();
                    }
                };
        
            }).collect(Collectors.toList());
        }
    
        private List<NCRequestStateJson> check(String auth) throws IOException {
            log.info("`check` request sent for: {}", email);
            
            Map<String, Object> m = gson.fromJson(
                post(baseUrl + "check",
                    Pair.of("accessToken", auth)
                ),
                TYPE_RESP
            );
    
            checkStatus(m);
    
            return extract(gson.toJsonTree(getField(m, "states")), TYPE_REQS);
        }
    
        private String ask(String auth, String txt, long dsId) throws IOException {
            log.info("`ask` request sent: {} to datasource: {}", txt, dsId);
            
            return extract(
                post(baseUrl + "ask",
                    Pair.of("accessToken", auth),
                    Pair.of("txt", txt),
                    Pair.of("dsId", dsId),
                    Pair.of("isTest", true)
                ),
                "srvReqId",
                String.class
            );
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
    
    public NCTestClientBuilder withHttpClientSupplier(Supplier<CloseableHttpClient> cliSup) {
        this.cliSup = cliSup;
    
        return this;
    }
    
    public NCTestClientBuilder withBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    
        return this;
    }
    
    public NCTestClientBuilder withEmail(String email) {
        this.email = email;
    
        return this;
    }
    
    public NCTestClientBuilder withPassword(String pswd) {
        this.pswd = pswd;
        
        return this;
    }
    
    
    /**
     * Build new configured test client instance.
     *
     * @return Newly built test client instance.
     */
    public NCTestClient build() {
        return new NCTestClientImpl();
    }
}
