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

package org.nlpcraft.model.builder;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.*;
import com.google.gson.*;
import com.google.gson.reflect.*;
import org.apache.commons.lang3.tuple.*;
import org.nlpcraft.model.*;
import org.nlpcraft.model.builder.impl.*;
import org.nlpcraft.model.builder.parsing.*;
import org.nlpcraft.model.impl.*;
import org.nlpcraft.model.intent.*;
import org.nlpcraft.model.tools.dump.*;
import org.nlpcraft.model.tools.dump.scala.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static org.nlpcraft.model.NCElement.*;
import static org.nlpcraft.model.intent.NCIntentSolver.*;

/**
 * Model builder for {@link NCModel} instances.
 * <br><br>
 * To use this builder start by either invoking:
 * <ul>
 *     <li>{@link #newModel()}</li>
 * </ul>
 * or by loading static model definition in JSON or YAML:
 * <ul>
 *     <li>{@link #newJsonModel(InputStream)}</li>
 *     <li>{@link #newJsonModel(String)}</li>
 *     <li>{@link #newYamlModel(InputStream)}</li>
 *     <li>{@link #newYamlModel(String)}</li>
 *     <li>{@link #newJsonStringModel(String)}</li>
 *     <li>{@link #newYamlStringModel(String)}</li>
 * </ul>
 * Once you have the builder instance you can set all necessary properties and finally call {@link #build()}
 * method to get properly constructed {@link NCModel} instance. Note that at the minimum the
 * {@link #setDescriptor(NCModelDescriptor) descriptor} and the {@link #setQueryFunction(Function) query function}
 * must be set.
 * <br><br>
 * Here's an example of the typical model builder usage (from <a target=_ href="https://github.com/vic64/nlpcraft/tree/master/src/main/scala/org/nlpcraft/examples/time">Time Example</a>):
 * <pre class="brush: java">
 * NCModelBuilder.
 *      newYamlModel(TimeModel.class.
 *          getClassLoader().
 *          getResourceAsStream("org/nlpcraft/examples/time/time_model.yaml")
 *      )
 *      .setSolver(solver)
 *      .build()
 * </pre>
 */
public class NCModelBuilder {
    /** */
    private final NCModelImpl impl;
    
    /** */
    private NCIntentSolver solver;
    
    /** */
    private static final Gson GSON = new Gson();
    
    /** */
    private static final java.lang.reflect.Type TYPE_LIST_IDXS = new TypeToken<ArrayList<Integer>>() {}.getType();
    
    /** */
    private enum Type { STRING, NUM, BOOL, LIST_INDEXES, VALUE }
    
    /** */
    private static final Map<Type, Set<String>> TYPES_OPS =
        Stream.of(
            // Sublists of OPS.
            Pair.of(Type.STRING, Arrays.asList("==", "!=", "%%", "!%")),
            Pair.of(Type.NUM, Arrays.asList("==", "!=", ">=", "<=", ">", "<")),
            Pair.of(Type.BOOL, Arrays.asList("==", "!=")),
            Pair.of(Type.LIST_INDEXES, Arrays.asList("==", "!=")),
            Pair.of(Type.VALUE, Arrays.asList("==", "!="))
        ).collect(Collectors.toMap(Pair::getLeft, p -> new HashSet<>(p.getRight())));
    
    /** */
    private static final Set<String> NC_IDS =
        new HashSet<>(
            Arrays.asList(
                "nlp:date",
                "nlp:function",
                "nlp:coordinate",
                "nlp:geo",
                "nlp:num"
            )
        );
    
    /** */
    private static final Map<String, Type> NC_META =
        Stream.of(
            Pair.of("~DATE_FROM", Type.NUM),
            Pair.of("~DATE_TO", Type.NUM),
            
            Pair.of("~FUNCTION_TYPE", Type.VALUE),
            Pair.of("~FUNCTION_ASC", Type.BOOL),
            Pair.of("~FUNCTION_INDEXES", Type.LIST_INDEXES),
            
            Pair.of("~COORDINATE_LATITUDE", Type.NUM),
            Pair.of("~COORDINATE_LONGITUDE", Type.NUM),
            
            Pair.of("~GEO_KIND", Type.VALUE),
            Pair.of("~GEO_CONTINENT", Type.STRING),
            Pair.of("~GEO_SUBCONTINENT", Type.STRING),
            Pair.of("~GEO_COUNTRY", Type.STRING),
            Pair.of("~GEO_REGION", Type.STRING),
            Pair.of("~GEO_CITY", Type.STRING),
            Pair.of("~GEO_METRO", Type.STRING),
            
            Pair.of("~NUM_FROM", Type.NUM),
            Pair.of("~NUM_FROMINCL", Type.NUM),
            Pair.of("~NUM_TO", Type.NUM),
            Pair.of("~NUM_TOINCL", Type.NUM),
            Pair.of("~NUM_ISFRACTIONAL", Type.BOOL),
            Pair.of("~NUM_ISRANGECONDITION", Type.BOOL),
            Pair.of("~NUM_ISEQUALCONDITION", Type.BOOL),
            Pair.of("~NUM_ISNOTEQUALCONDITION", Type.BOOL),
            Pair.of("~NUM_ISFROMNEGATIVEINFINITY", Type.BOOL),
            Pair.of("~NUM_ISTOPOSITIVEINFINITY", Type.BOOL),
            Pair.of("~NUM_UNIT", Type.STRING),
            Pair.of("~NUM_UNITTYPE", Type.STRING)
        ).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    
    /** */
    private static final Map<String, Set<String>> NC_VALUES =
        Stream.of(
            Pair.of("~FUNCTION_TYPE",
                new HashSet<>(
                    Arrays.asList(
                        "SUM", "MAX", "MIN", "AVG", "SORT", "LIMIT", "GROUP", "CORRELATION", "COMPARE")
                )
            ),
            Pair.of(
                "~GEO_KIND",
                new HashSet<>(Arrays.asList("CONTINENT", "SUBCONTINENT", "COUNTRY", "REGION", "CITY", "METRO"))
            )
        ).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    
    /**
     *
     * @param op
     */
    private static void validateStrOperation(String op) {
        if (!TYPES_OPS.get(Type.STRING).contains(op))
            throw new IllegalArgumentException(String.format("Invalid intent DSL rule operation: %s", op));
    }
    
    /**
     *
     * @param param
     * @param op
     * @param v
     * @param nullable
     * @param allVals
     */
    private static void validateRef(String param, String op, Object v, boolean nullable, Set<String> allVals) {
        assert param != null;
        assert op != null;
        assert allVals != null;
        
        if (v == null && !nullable)
            throw new IllegalArgumentException(String.format("Invalid intent DSL rule null value: %s", param));
        
        validateStrOperation(op);
        
        if (v != null) {
            String vs = v.toString();
            
            if (!allVals.contains(vs))
                throw new IllegalArgumentException(
                    String.format("Unknown intent DSL rule value in: %s%s%s", param, op, vs)
                );
        }
    }
    
    /**
     *
     * @param param Metadata name to validate.
     * @param allParams All possible metadata names.
     */
    private static void validateMetaUser(String param, Set<String> allParams) {
        assert param != null;
        assert allParams != null;
    
        if (!allParams.contains(param))
            throw new IllegalArgumentException(String.format("Invalid intent DSL metadata name: %s", param));
    }
    
    /**
     *
     * @param param
     * @param op
     * @param v
     */
    private static void validateMetaSystem(String param, String op, Type type, Object v) {
        assert param != null;
        assert op != null;
        assert type != null;
    
        Set<String> typeOps = TYPES_OPS.get(type);
        
        assert typeOps != null;
        
        if (!typeOps.contains(op))
            throw new IllegalArgumentException(
                String.format("Invalid intent DSL rule operation in: %s%s%s", param, op, v)
            );
        
        if (v != null) {
            String vs = v.toString();
            
            switch (type) {
                case STRING: break;
    
                case NUM: {
                    try {
                        Double.parseDouble(vs);
                    }
                    catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                            String.format(
                                "Invalid intent DSL rule numeric value in: %s%s%s",
                                param, op, vs
                            ),
                            e
                        );
                    }
                    
                    break;
                }
                case BOOL: {
                    if (!vs.equalsIgnoreCase("true") && !vs.equalsIgnoreCase("false"))
                        throw new IllegalArgumentException(
                            String.format(
                                "Invalid intent DSL rule boolean value in: %s%s%s",
                                param, op, vs
                            )
                        );
                        
                    break;
                }
                
                case VALUE: {
                    Set<String> enums = NC_VALUES.get(param);
    
                    assert enums != null;
    
                    if (!enums.contains(vs))
                        throw new IllegalArgumentException(
                            String.format(
                                "Invalid intent DSL rule operation value in: %s%s%s",
                                param, op, vs
                            )
                        );
                    
                    break;
                }
                
                case LIST_INDEXES: {
                    try {
                        GSON.fromJson(vs, TYPE_LIST_IDXS);
                    }
                    catch (Exception e) {
                        throw new IllegalArgumentException(
                            String.format(
                                "Invalid intent DSL rule indexes list value in: %s%s%s",
                                param, op, vs
                            ),
                            e
                        );
                    }
                    
                    break;
                }
                
                default:
                    assert false;
            }
        }
    }
    
    /**
     * Reads JSON file and creates JSON representation object of given type.
     *
     * @param filePath File path.
     * @param claxx JSON representation class.
     * @param <T> Object type.
     * @return Initialized object.
     * @throws NCBuilderException In case of any errors loading JSON.
     */
    static private <T> T readFileJson(String filePath, Class<T> claxx) throws NCBuilderException {
        try (Reader reader = new BufferedReader(new FileReader(filePath))) {
            return GSON.fromJson(reader, claxx);
        }
        catch (Exception e) {
            throw new NCBuilderException("Failed to load JSON from: " + filePath, e);
        }
    }
    
    /**
     * Reads YAML file and creates YAML representation object of given type.
     *
     * @param filePath File path.`
     * @param claxx YAML representation class.
     * @param <T> Object type.
     * @return Initialized object.
     * @throws NCBuilderException In case of any errors loading YAML.
     */
    static private <T> T readFileYaml(String filePath, Class<T> claxx) throws NCBuilderException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    
        try {
            return mapper.readValue(new File(filePath), claxx);
        }
        catch (Exception e) {
            throw new NCBuilderException("Failed to load YAML from: " + filePath, e);
        }
    }
    
    /**
     * Reads JSON file and creates JSON representation object of given type.
     *
     * @param in Input stream.
     * @param claxx JSON representation class.
     * @param <T> Object type.
     * @return Initialized object.
     * @throws NCBuilderException In case of any errors loading JSON.
     */
    static private <T> T readFileJson(InputStream in, Class<T> claxx) throws NCBuilderException {
        try (Reader reader = new BufferedReader(new InputStreamReader(in))) {
            return GSON.fromJson(reader, claxx);
        }
        catch (Exception e) {
            throw new NCBuilderException("Failed to load JSON from stream.", e);
        }
    }
    
    /**
     * Reads YAML file and creates YAML representation object of given type.
     *
     * @param in Input stream.
     * @param claxx YAML representation class.
     * @param <T> Object type.
     * @return Initialized object.
     * @throws NCBuilderException In case of any errors loading YAML.
     */
    static private <T> T readFileYaml(InputStream in, Class<T> claxx) throws NCBuilderException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    
        try {
            return mapper.readValue(in, claxx);
        }
        catch (Exception e) {
            throw new NCBuilderException("Failed to load YAML from stream.", e);
        }
    }

    /**
     * Reads JSON string and creates JSON representation object of given type.
     *
     * @param jsonStr JSON string to read from.
     * @param claxx JSON representation class.
     * @param <T> Object type.
     * @return Initialized object.
     * @throws NCBuilderException In case of any errors loading JSON.
     */
    static private <T> T readStringJson(String jsonStr, Class<T> claxx) throws NCBuilderException {
        try (Reader reader = new BufferedReader(new StringReader(jsonStr))) {
            return GSON.fromJson(reader, claxx);
        }
        catch (Exception e) {
            throw new NCBuilderException("Failed to load JSON from string.", e);
        }
    }
    
    /**
     * Reads YAML string and creates YAML representation object of given type.
     *
     * @param yamlStr YAML string to read from.
     * @param claxx YAML representation class.
     * @param <T> Object type.
     * @return Initialized object.
     * @throws NCBuilderException In case of any errors loading YAML.
     */
    static private <T> T readStringYaml(String yamlStr, Class<T> claxx) throws NCBuilderException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    
        try {
            return mapper.readValue(yamlStr, claxx);
        }
        catch (Exception e) {
            throw new NCBuilderException("Failed to load YAML from string.", e);
        }
    }

    /**
     *
     */
    private NCModelBuilder() {
        impl = new NCModelImpl();
    }

    /**
     * Utility method for locating resource on a classpath.
     *
     * @param fileName Resource's file name on the classpath.
     * @return Full path of the resource.
     */
    public static String classPathFile(String fileName) {
        URL res = NCModelBuilder.class.getClassLoader().getResource(fileName);

        if (res == null)
            throw new IllegalArgumentException("Classpath resource not found: " + fileName);

        return res.getFile();
    }

    /**
     * Creates new model builder.
     * 
     * @return New model builder.
     */
    public static NCModelBuilder newModel() {
        return new NCModelBuilder();
    }

    /**
     * Creates new model builder with given parameters.
     * 
     * @param mdlId Unique, <i>immutable</i> ID of the model.
     * @param mdlName Descriptive name of this model.
     * @param mdlVer Version of this model using semantic versioning compatible
     *      with (<a href="http://www.semver.org">www.semver.org</a>) specification.
     * @return New model builder.
     */
    public static NCModelBuilder newModel(String mdlId, String mdlName, String mdlVer) {
        NCModelBuilder bldr = new NCModelBuilder();

        bldr.setDescriptor(
            NCModelDescriptorBuilder.newDescriptor(mdlId, mdlName, mdlVer).build()
        );

        return bldr;
    }

    /**
     * Creates new model builder and loads model definition from JSON file.
     * See {@link NCModel} for JSON example.
     * 
     * @param filePath JSON file path to load from.
     * @return New model builder.
     * @throws NCBuilderException Thrown in case of any errors building the model.
     * @see NCModel
     */
    public static NCModelBuilder newJsonModel(String filePath) throws NCBuilderException {
        if (filePath == null)
            throw new IllegalArgumentException("JSON file path cannot be null.");

        NCModelBuilder bldr = new NCModelBuilder();

        bldr.ingestJsonModel(readFileJson(filePath, NCModelItem.class));

        return bldr;
    }
    
    /**
     * Creates new model builder and loads model definition from YAML file.
     * See {@link NCModel} for JSON example.
     *
     * @param filePath YAML file path to load from.
     * @return New model builder.
     * @throws NCBuilderException Thrown in case of any errors building the model.
     * @see NCModel
     */
    public static NCModelBuilder newYamlModel(String filePath) throws NCBuilderException {
        if (filePath == null)
            throw new IllegalArgumentException("YAML file path cannot be null.");
        
        NCModelBuilder bldr = new NCModelBuilder();
        
        bldr.ingestJsonModel(readFileYaml(filePath, NCModelItem.class));
        
        return bldr;
    }
    
    /**
     * Creates new model builder and loads JSON model definition from input stream.
     * See {@link NCModel} for JSON example.
     *
     * @param in Input stream to load JSON model from.
     * @return New model builder.
     * @throws NCBuilderException Thrown in case of any errors building the model.
     * @see NCModel
     */
    public static NCModelBuilder newJsonModel(InputStream in) throws NCBuilderException {
        if (in == null)
            throw new IllegalArgumentException("JSON input stream cannot be null (wrong JSON file path?).");

        NCModelBuilder bldr = new NCModelBuilder();
        
        bldr.ingestJsonModel(readFileJson(in, NCModelItem.class));
        
        return bldr;
    }
    
    /**
     * Creates new model builder and loads YAML model definition from input stream.
     * See {@link NCModel} for JSON example.
     *
     * @param in Input stream to load YAML model from.
     * @return New model builder.
     * @throws NCBuilderException Thrown in case of any errors building the model.
     * @see NCModel
     */
    public static NCModelBuilder newYamlModel(InputStream in) throws NCBuilderException {
        if (in == null)
            throw new IllegalArgumentException("YAML input stream cannot be null (wrong YAML file path?).");
        
        NCModelBuilder bldr = new NCModelBuilder();
        
        bldr.ingestJsonModel(readFileYaml(in, NCModelItem.class));
        
        return bldr;
    }

    /**
     * Creates new model builder and loads JSON model definition from given JSON string.
     * See {@link NCModel} for JSON example.
     * 
     * @param jsonStr JSON string to load model from.
     * @return New model builder.
     * @throws NCBuilderException Thrown in case of any errors building the model.
     * @see NCModel
     */
    public static NCModelBuilder newJsonStringModel(String jsonStr) throws NCBuilderException {
        if (jsonStr == null)
            throw new IllegalArgumentException("JSON input string cannot be null.");

        NCModelBuilder bldr = new NCModelBuilder();

        bldr.ingestJsonModel(readStringJson(jsonStr, NCModelItem.class));

        return bldr;
    }
    
    /**
     * Creates new model builder and loads YAML model definition from given YAML string.
     * See {@link NCModel} for JSON example.
     *
     * @param yamlStr YAML string to load model from.
     * @return New model builder.
     * @throws NCBuilderException Thrown in case of any errors building the model.
     * @see NCModel
     */
    public static NCModelBuilder newYamlStringModel(String yamlStr) throws NCBuilderException {
        if (yamlStr == null)
            throw new IllegalArgumentException("YAML input string cannot be null.");
        
        NCModelBuilder bldr = new NCModelBuilder();
        
        bldr.ingestJsonModel(readStringYaml(yamlStr, NCModelItem.class));
        
        return bldr;
    }
    
    /**
     *
     * @param rules
     * @param p
     */
    private static void fillRules(List<RULE> rules, PREDICATE p) {
        // Complex rule.
        if (p instanceof List)
            ((List<PREDICATE>)p).forEach(x -> fillRules(rules, x) );
        // Plain rule.
        else
            rules.add((RULE)p);
    }
    
    /**
     * Sets intent solver to be used for this model's {@link NCModel#query(NCQueryContext)} method
     * implementation. Note that this method ensures that model builder will check and validate the
     * intents against this model when {@link #build()} method is called.
     * <br><br>
     * Note also that you can technically use {@link #setQueryFunction(Function)} method to set intent solver
     * for this model. However, since model builder wouldn't have access to the actual intent solver
     * it won't be able to check and validate the intents. It is highly recommended to use this
     * method when using {@link NCIntentSolver}.
     *
     * @param solver Intent solver to set.
     */
    public NCModelBuilder setSolver(NCIntentSolver solver) {
        this.solver = solver;
        
        return this;
    }
    
    /**
     * Returns newly built model. Note that at the minimum the
     * {@link #setDescriptor(NCModelDescriptor) descriptor} and
     * the {@link #setQueryFunction(Function) query function}
     * must be set.
     *
     * @return New built model.
     * @throws NCBuilderException Thrown in case of any errors building the model.
     */
    public NCModel build() throws NCBuilderException {
        if (impl.getDescriptor() == null)
            throw new NCBuilderException("Model descriptor is not set.");
    
        if (solver != null) {
            if (impl.getQueryFunction() != null)
                throw new NCBuilderException("Query function and solver cannot be set together.");
    
            Set<String> allIds = new HashSet<>(NC_IDS);
            Set<String> userVals = new HashSet<>();
            Set<String> userMetaNames = new HashSet<>();
            Set<String> userGroups = new HashSet<>();
            
            for (NCElement e : impl.getElements()) {
                String g = e.getGroup();
        
                if (g != null)
                    userGroups.add(g);
        
                String id = e.getId();
        
                if (id != null)
                    allIds.add(id);
        
                NCMetadata md = e.getMetadata();
        
                if (md != null)
                    userMetaNames.addAll(md.keySet().stream().map(p -> '~' + p).collect(Collectors.toSet()));
        
                List<NCValue> vals = e.getValues();
        
                if (vals != null)
                    userVals.addAll(vals.stream().map(NCValue::getName).collect(Collectors.toSet()));
            }
    
            Consumer<RULE> validate = (rule) -> {
                String param = rule.getParameter();
                String op = rule.getOp();
                Object v = rule.getValue();
                
                if (param.charAt(0) == '~') {
                    Type type = NC_META.get(param);
    
                    if (type != null)
                        validateMetaSystem(param, op, type, v);
                    else
                        validateMetaUser(param, userMetaNames);
                }
                else if (param.equals("id"))  // Cannot be null.
                    validateRef(param, op, v, false, allIds);
                else if (param.equals("parent"))  // Can be null.
                    validateRef(param, op, v, true, allIds);
                else if (param.equals("group"))  // Can be null.
                    validateRef(param, op, v, true, userGroups);
                else if (param.equals("value")) // Can be null.
                    validateRef(param, op, v, true, userVals);
                else
                    assert false;
            };
    
            List<RULE> rules = new ArrayList<>();
    
            solver.
                getIntents().
                stream().
                flatMap(p -> Arrays.stream(p.getTerms())).
                flatMap(x -> Arrays.stream(x.getItems())).
                map(ITEM::getPattern).
                collect(Collectors.toList()).
                forEach(p -> fillRules(rules, p));
    
            rules.forEach(validate);
    
            impl.setQueryFunction(solver::solve);
        }
        else if (impl.getQueryFunction() == null)
            throw new NCBuilderException("Query function is not set.");

        return impl;
    }
    
    /**
     * Loads and creates data model proxy from given data model dump.
     *
     * @param dumpFilePath Dump file path.
     * @return Newly built model proxy. Proxy model will have a no-op callback implementations for intent and will return the
     *      following JSON response from its {@link NCModel#query(NCQueryContext)} method:
     * <pre class="brush: js">
     * {
     *     "modelId": "model-id",
     *     "intentId": "intent-id",
     *     "modelFile": "model-id-01:01:01:123.gz"
     *  }
     * </pre>
     * @throws NCBuilderException Thrown in case of any errors building the model.
     *
     * @see NCDumpReader
     * @see NCDumpWriter
     */
    public NCModel loadFromDump(String dumpFilePath) throws NCBuilderException {
        return NCDumpReaderScala.read(dumpFilePath);
    }
    
    /**
     * Sets model descriptor.
     *
     * @param ds Model descriptor to set.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setDescriptor(NCModelDescriptor ds) {
        impl.setDescriptor(ds);

        return this;
    }

    /**
     * Sets model descriptor.
     *
     * @param desc Model description to set.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setDescription(String desc) {
        impl.setDescription(desc);

        return this;
    }

    /**
     * Adds model metadata. See {@link NCModel#getMetadata()} for more information.
     *
     * @param name Metadata property name.
     * @param val Metadata property value.
     * @return This builder for chaining operations.
     * @see NCModel#getMetadata()
     */
    public NCModelBuilder addUserMetadata(String name, Serializable val) {
        impl.addMetadata(name, val);

        return this;
    }

    /**
     * Adds model element. See {@link NCElement} for more information.
     *
     * @param elm Model element to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addElement(NCElement elm) {
        impl.addElement(elm);

        return this;
    }

    /**
     * Adds macro. See {@link NCElement} for more macro information.
     *
     * @param name Macro name.
     * @param val Macro value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addMacro(String name, String val) {
        impl.addMacro(name, val);

        return this;
    }

    /**
     *
     * @param js Model JSON.
     *
     * @throws NCBuilderException Thrown in case of any errors.
     */
    private void ingestJsonModel(NCModelItem js) throws NCBuilderException {
        impl.setDescriptor(
            NCModelDescriptorBuilder.newDescriptor(
                js.getId(),
                js.getName(),
                js.getVersion()
            ).build()
        );
    
        impl.setDescription(js.getDescription());
        impl.setDocsUrl(js.getDocsUrl());
        impl.setVendorUrl(js.getVendorUrl());
        impl.setVendorEmail(js.getVendorEmail());
        impl.setVendorContact(js.getVendorContact());
        impl.setVendorName(js.getVendorName());
        impl.setMaxUnknownWords(js.getMaxUnknownWords());
        impl.setMaxFreeWords(js.getMaxFreeWords());
        impl.setMaxSuspiciousWords(js.getMaxSuspiciousWords());
        impl.setMinWords(js.getMinWords());
        impl.setMaxWords(js.getMaxWords());
        impl.setMinTokens(js.getMinTokens());
        impl.setMaxTokens(js.getMaxTokens());
        impl.setMinNonStopwords(js.getMinNonStopwords());
        impl.setNonEnglishAllowed(js.isNonEnglishAllowed());
        impl.setNotLatinCharsetAllowed(js.isNotLatinCharsetAllowed());
        impl.setSwearWordsAllowed(js.isSwearWordsAllowed());
        impl.setNoNounsAllowed(js.isNoNounsAllowed());
        impl.setNoUserTokensAllowed(js.isNoUserTokensAllowed());
        impl.setJiggleFactor(js.getJiggleFactor());
        impl.setMinDateTokens(js.getMinDateTokens());
        impl.setMaxDateTokens(js.getMaxDateTokens());
        impl.setMinNumTokens(js.getMinNumTokens());
        impl.setMaxNumTokens(js.getMaxNumTokens());
        impl.setMinGeoTokens(js.getMinGeoTokens());
        impl.setMaxGeoTokens(js.getMaxGeoTokens());
        impl.setMinFunctionTokens(js.getMinFunctionTokens());
        impl.setMaxFunctionTokens(js.getMaxFunctionTokens());
        impl.setDupSynonymsAllowed(js.isDupSynonymsAllowed());
        impl.setMaxTotalSynonyms(js.getMaxTotalSynonyms());
        impl.setPermutateSynonyms(js.isPermutateSynonyms());
    
        if (js.getAdditionalStopwords() != null)
            addAdditionalStopWords(js.getAdditionalStopwords());

        if (js.getExcludedStopwords() != null)
            addExcludedStopWords(js.getExcludedStopwords());

        if (js.getSuspiciousWords() != null)
            addSuspiciousWords(js.getSuspiciousWords());

        if (js.getExamples() != null)
            addExamples(js.getExamples());

        if (js.getMacros() != null)
            for (NCMacroItem m : js.getMacros())
                addMacro(m.getName(), m.getMacro());

        if (js.getUserMetadata() != null)
            for (Map.Entry<String, Object> entry : js.getUserMetadata().entrySet())
                addUserMetadata(entry.getKey(), (Serializable)entry.getValue());

        if (js.getElements() != null)
            for (NCElementItem e : js.getElements()) {
                NCMetadata elmMeta = new NCMetadataImpl();
        
                if (e.getMetadata() != null)
                    for (Map.Entry<String, Object> entry : e.getMetadata().entrySet())
                        elmMeta.put(entry.getKey(), (Serializable)entry.getValue());
    
                NCElementImpl elmImpl = new NCElementImpl();
    
                elmImpl.setSynonyms(e.getSynonyms() == null ? Collections.emptyList() : Arrays.asList(e.getSynonyms()));
                elmImpl.setExcludedSynonyms(e.getExcludedSynonyms() == null ? Collections.emptyList() : Arrays.asList(e.getExcludedSynonyms()));
                elmImpl.setValues(e.getValues() == null ?
                    Collections.emptyList() :
                    Arrays.stream(e.getValues()).map(
                        p -> new NCValueImpl(
                            p.getName(),
                            p.getSynonyms() == null ? Collections.emptyList() : Arrays.asList(p.getSynonyms())
                        )
                    ).collect(Collectors.toList()));
    
                elmImpl.setParentId(e.getParentId());
                elmImpl.setDescription(e.getDescription());
                elmImpl.setId(e.getId());
                elmImpl.setGroup(e.getGroup());
                elmImpl.setMeta(elmMeta);
    
                addElement(elmImpl);
    
        }
    }

    /**
     * Adds additional stopword. See {@link NCModel#getAdditionalStopWords()} for more information.
     *
     * @param words Additional stopwords to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addAdditionalStopWords(String... words) {
        return addAdditionalStopWords(Arrays.asList(words));
    }

    /**
     * Adds additional stopword. See {@link NCModel#getAdditionalStopWords()} for more information.
     *
     * @param words Additional stopwords to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addAdditionalStopWords(Collection<String> words) {
        assert words != null;

        for (String word : words)
            impl.addAdditionalStopWord(word);

        return this;
    }

    /**
     * Adds suspicious stopword. See {@link NCModel#getSuspiciousWords()} for more information.
     *
     * @param words Suspicious words to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addSuspiciousWords(String... words) {
        return addSuspiciousWords(Arrays.asList(words));
    }

    /**
     * Adds suspicious stopword. See {@link NCModel#getSuspiciousWords()} for more information.
     *
     * @param words Suspicious words to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addSuspiciousWords(Collection<String> words) {
        assert words != null;

        for (String word : words)
            impl.addSuspiciousWord(word);

        return this;
    }

    /**
     * Adds examples to the model. See {@link NCModel#getExamples()} for more information.
     *
     * @param examples Examples to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addExamples(String... examples) {
        return addExamples(Arrays.asList(examples));
    }

    /**
     * Adds examples to the model. See {@link NCModel#getExamples()} for more information.
     *
     * @param examples Examples to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addExamples(Collection<String> examples) {
        assert examples != null;

        for (String example : examples)
            impl.addExample (example);

        return this;
    }

    /**
     * Adds excluding stopwords. See {@link NCModel#getExcludedStopWords()} for more information.
     *
     * @param words Excluding stopwords to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addExcludedStopWords(String... words) {
        return addExcludedStopWords(Arrays.asList(words));
    }

    /**
     * Adds excluding stopwords. See {@link NCModel#getExcludedStopWords()} for more information.
     *
     * @param words Excluding stopwords to add.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder addExcludedStopWords(Collection<String> words) {
        assert words != null;

        for (String word : words)
            impl.addExcludedStopWord(word);

        return this;
    }

    /**
     * Sets query function. See {@link NCModel#query(NCQueryContext)} for more information.
     *
     * @param qryFun Query function to set.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setQueryFunction(Function<NCQueryContext, NCQueryResult> qryFun) {
        impl.setQueryFunction(qryFun);

        return this;
    }

    /**
     * Sets model's discard function. See {@link NCModel#discard()} for more information.
     * 
     * @param discardFun Model's discard function to set.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setDiscardFunction(Runnable discardFun) {
        impl.setDiscardFunction(discardFun);

        return this;
    }

    /**
     * Sets model's initialize function. See {@link NCModel#initialize(NCProbeContext)} ()} for more information.
     *
     * @param initFun Model's initialize function to set.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setInitFunction(Consumer<NCProbeContext> initFun) {
        impl.setInitFunction(initFun);

        return this;
    }

    /**
     * Sets {@link NCModel#getDocsUrl()} configuration value.
     *
     * @param docsUrl {@link NCModel#getDocsUrl()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setDocsUrl(String docsUrl) {
        impl.setDocsUrl(docsUrl);
        
        return this;
    }

    /**
     * Sets {@link NCModel#getVendorUrl()} configuration value.
     *
     * @param vendorUrl {@link NCModel#getVendorUrl()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setVendorUrl(String vendorUrl) {
        impl.setVendorUrl(vendorUrl);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getVendorEmail()} configuration value.
     *
     * @param vendorEmail {@link NCModel#getVendorEmail()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setVendorEmail(String vendorEmail) {
        impl.setVendorEmail(vendorEmail);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getVendorContact()} configuration value.
     *
     * @param vendorContact {@link NCModel#getVendorContact()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setVendorContact(String vendorContact) {
        impl.setVendorContact(vendorContact);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getVendorName()} configuration value.
     *
     * @param vendorName {@link NCModel#getVendorName()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setVendorName(String vendorName) {
        impl.setVendorName(vendorName);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getJiggleFactor()} configuration value.
     *
     * @param jiggleFactor {@link NCModel#getJiggleFactor()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setJiggleFactor(int jiggleFactor) {
        impl.setJiggleFactor(jiggleFactor);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinDateTokens()} configuration value.
     *
     * @param minDateTokens {@link NCModel#getMinDateTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinDateTokens(int minDateTokens) {
        impl.setMinDateTokens(minDateTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxDateTokens()} configuration value.
     *
     * @param maxDateTokens {@link NCModel#getMaxDateTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxDateTokens(int maxDateTokens) {
        impl.setMaxDateTokens(maxDateTokens);
    
        return this;
    }
    
    /**
     * Sets {@link NCModel#getMinNumTokens()} configuration value.
     *
     * @param minNumTokens {@link NCModel#getMinNumTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinNumTokens(int minNumTokens) {
        impl.setMinNumTokens(minNumTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxNumTokens()} configuration value.
     *
     * @param maxNumTokens {@link NCModel#getMaxNumTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxNumTokens(int maxNumTokens) {
        impl.setMinNumTokens(maxNumTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinGeoTokens()} configuration value.
     *
     * @param minGeoTokens {@link NCModel#getMinGeoTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinGeoTokens(int minGeoTokens) {
        impl.setMinGeoTokens(minGeoTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxGeoTokens()} configuration value.
     *
     * @param maxGeoTokens {@link NCModel#getMaxGeoTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxGeoTokens(int maxGeoTokens) {
        impl.setMaxGeoTokens(maxGeoTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinFunctionTokens()} configuration value.
     *
     * @param minFunctionTokens {@link NCModel#getMinFunctionTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinFunctionTokens(int minFunctionTokens) {
        impl.setMinFunctionTokens(minFunctionTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxFunctionTokens()} configuration value.
     *
     * @param maxFunctionTokens {@link NCModel#getMaxFunctionTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxFunctionTokens(int maxFunctionTokens) {
        impl.setMaxFunctionTokens(maxFunctionTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxUnknownWords()} configuration value.
     *
     * @param maxUnknownWords {@link NCModel#getMaxUnknownWords()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxUnknownWords(int maxUnknownWords) {
        impl.setMaxUnknownWords(maxUnknownWords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxFreeWords()} configuration value.
     *
     * @param maxFreeWords {@link NCModel#getMaxFreeWords()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxFreeWords(int maxFreeWords) {
        impl.setMaxFreeWords(maxFreeWords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxSuspiciousWords()} configuration value.
     *
     * @param maxSuspiciousWords {@link NCModel#getMaxSuspiciousWords()}  configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxSuspiciousWords(int maxSuspiciousWords) {
        impl.setMaxSuspiciousWords(maxSuspiciousWords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinWords()} configuration value.
     *
     * @param minWords {@link NCModel#getMinWords()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinWords(int minWords) {
        impl.setMinWords(minWords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxWords()} configuration value.
     *
     * @param maxWords {@link NCModel#getMaxWords()}  configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxWords(int maxWords) {
        impl.setMaxWords(maxWords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinTokens()} configuration value.
     *
     * @param minTokens {@link NCModel#getMinTokens()}  configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinTokens(int minTokens) {
        impl.setMinTokens(minTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMaxTokens()} configuration value.
     *
     * @param maxTokens {@link NCModel#getMaxTokens()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxTokens(int maxTokens) {
        impl.setMaxTokens(maxTokens);
    
        return this;
    }

    /**
     * Sets {@link NCModel#getMinNonStopwords()} configuration value.
     *
     * @param minNonStopwords {@link NCModel#getMinNonStopwords()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMinNonStopwords(int minNonStopwords) {
        impl.setMinNonStopwords(minNonStopwords);
    
        return this;
    }

    /**
     * Sets {@link NCModel#isNonEnglishAllowed()} configuration value.
     *
     * @param nonEnglishAllowed {@link NCModel#isNonEnglishAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setNonEnglishAllowed(boolean nonEnglishAllowed) {
        impl.setNonEnglishAllowed(nonEnglishAllowed);
    
        return this;
    }

    /**
     * Sets {@link NCModel#isNotLatinCharsetAllowed()} configuration value.
     *
     * @param notLatinCharsetAllowed {@link NCModel#isNotLatinCharsetAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setNotLatinCharsetAllowed(boolean notLatinCharsetAllowed) {
        impl.setNotLatinCharsetAllowed(notLatinCharsetAllowed);
    
        return this;
    }

    /**
     * Sets {@link NCModel#isSwearWordsAllowed()} configuration value.
     *
     * @param swearWordsAllowed {@link NCModel#isSwearWordsAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setSwearWordsAllowed(boolean swearWordsAllowed) {
        impl.setSwearWordsAllowed(swearWordsAllowed);
    
        return this;
    }

    /**
     * Sets {@link NCModel#isNoNounsAllowed()} configuration value.
     *
     * @param noNounsAllowed {@link NCModel#isNoNounsAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setNoNounsAllowed(boolean noNounsAllowed) {
        impl.setNoNounsAllowed(noNounsAllowed);
    
        return this;
    }

    /**
     * Sets {@link NCModel#isNoUserTokensAllowed()} configuration value.
     *
     * @param noUserTokensAllowed {@link NCModel#isNoUserTokensAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setNoUserTokensAllowed(boolean noUserTokensAllowed) {
        impl.setNoUserTokensAllowed(noUserTokensAllowed);
        
        return this;
    }
    
    /**
     * Sets {@link NCModel#isDupSynonymsAllowed()} configuration value.
     *
     * @param isDupSynonymsAllowed {@link NCModel#isDupSynonymsAllowed()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setDupSynonymsAllowed(boolean isDupSynonymsAllowed) {
        impl.setDupSynonymsAllowed(isDupSynonymsAllowed);
        
        return this;
    }
    
    /**
     * Sets {@link NCModel#getMaxTotalSynonyms()} configuration value.
     *
     * @param maxTotalSynonyms {@link NCModel#getMaxTotalSynonyms()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setMaxTotalSynonyms(int maxTotalSynonyms) {
        impl.setMaxTotalSynonyms(maxTotalSynonyms);
        
        return this;
    }
    
    /**
     * Sets {@link NCModel#isPermutateSynonyms()} configuration value.
     *
     * @param permutateSynonyms {@link NCModel#isPermutateSynonyms()} configuration value.
     * @return This builder for chaining operations.
     */
    public NCModelBuilder setPermutateSynonyms(boolean permutateSynonyms) {
        impl.setPermutateSynonyms(permutateSynonyms);
        
        return this;
    }
}
