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

package org.nlpcraft.model.intent;

import org.apache.commons.lang3.tuple.Pair;
import org.nlpcraft.common.NCException;
import org.nlpcraft.model.NCMetadata;
import org.nlpcraft.model.NCModel;
import org.nlpcraft.model.NCModelProvider;
import org.nlpcraft.model.NCQueryContext;
import org.nlpcraft.model.NCQueryResult;
import org.nlpcraft.model.NCRejection;
import org.nlpcraft.model.NCSentence;
import org.nlpcraft.model.NCToken;
import org.nlpcraft.model.NCVariant;
import org.nlpcraft.model.intent.impl.NCIntentSolverEngine;
import org.nlpcraft.model.intent.impl.NCIntentSolverResult;
import org.nlpcraft.model.builder.NCModelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <b>Main entry point</b> for intent-based user input matcher.
 * <br><br>
 * Intent solver is intended for simpler and more formalized implementation of {@link NCModel#query(NCQueryContext)}
 * method. This is the non-declarative part of the model and cannot be defined outside of JVM code as this
 * method is responsible for user-defined converting of parsed user input into specific action.
 * Although this method can be implemented "manually" by manually parsing query context and its list of
 * parsing variants - it is often more complex and error-prone than necessary.
 * <br><br>
 * Intent solver is based on a well known idea of registering one or more formal user input template, also
 * known as an {@link INTENT intent}, and let this solver select the best matching intent for a given the user input.
 * Each intent defines a pattern of the user input and associated action to take when that pattern is detected.
 * While selecting the best matching intent the solver uses sophisticated NLP analysis, resolves anaphores via
 * conversational context and can ask user for missing terms. If more than one intent is registered the solver
 * tries to find the most specific intent that matches user input.
 * <br><br>
 * The basic usage of token solver consists of three steps:
 * <ul>
 *     <li>Create new token solver instance.</li>
 *     <li>Add one or more intents to the solver.</li>
 *     <li>
 *         Use {@link NCIntentSolver#solve(NCQueryContext)} method as implementation
 *         for {@link NCModel#query(NCQueryContext)} method on your model. For example, if you use
 *         {@link NCModelBuilder} class you can use its {@link NCModelBuilder#setQueryFunction(Function)}
 *         method to set this up.
 *     </li>
 * </ul>
 * This solver setup is typically done in {@link NCModelProvider} interface implementation. Once configured
 * the model is deployed and it will use the set token solver for resolving user input and converting it into actions.
 * <p>
 * To add an intent you will need to use simple DSL based on the following classes and interfaces:
 * <pre>
 * +---------------+    +-------------------+
 * |  {@link CONV_INTENT CONV_INTENT}  |    |  {@link NON_CONV_INTENT NON_CONV_INTENT}  |
 * +-------+-------+    +----------+--------+
 *         |                       |
 *         |                       |
 *         |    +----------+       |
 *         +---^+  {@link INTENT INTENT}  +^------+
 *              +----+-----+
 *                   |
 *                   |
 *               +---o----+
 *               |  {@link TERM TERM} |
 *               +---+----+
 *                   |
 *                   |
 *               +---0----+
 *               |  {@link ITEM ITEM}  |
 *               +---+----+
 *                   |
 *                   |
 *             +-----0-------+
 *       +----^+ /predicate/ |^---+-----------+------------+------------+-----------+----------+
 *       |     +-----^-------+    |           |            |            |           |          |
 *       |                        |           |            |            |           |          |
 *       |                        |           |            |            |           |          |
 *  +----+---+                +---+---+   +---+---+   +--------+   +--------+   +-------+  +------+
 *  |  {@link RULE RULE}  |                |  {@link AND AND}  |   |  {@link NOR NOR}  |   |  {@link XNOR XNOR}  |   |  {@link NAND NAND}  |   |  {@link XOR XOR}  |  |  {@link OR OR}  |
 *  +--------+                +-------+   +-------+   +--------+   +--------+   +-------+  +------+
 * </pre>
 * An intent is a collection of {@link TERM TERMs} with a callback for when that intent is
 * selected. Intents can be ordered or unordered, conversational and non-conversational. Additionally to
 * {@link INTENT INTENT} class there are two special subclasses that provide
 * convenient shortcuts for creating specific intents: {@link CONV_INTENT CONV_INTENT} and
 * {@link NON_CONV_INTENT NON_CONV_INTENT}. Term represents a selector on semantic token either from the model or
 * the built-in one. Technically the term consists of one or more {@link ITEM items} where each item is either
 * a {@link RULE rule} or one of the logical combinators. Few important notes:
 * <ul>
 *     <li>
 *         Classes like {@link TERM TERM}, {@link ITEM ITEM} and {@link RULE RULE} have multiple convenient
 *         shortcut constructors to shorten the DSL notation.
 *     </li>
 *     <li>
 *         {@link TERM TERM} can represent a complex match on a token beyond just matching on token ID. Employing
 *         multiple {@link ITEM ITEMs} with combinators the {@link TERM TERM} can encode a sophisticated
 *         matching logic.
 *     </li>
 *     <li>
 *         The order of {@link ITEM ITEMs} in the {@link TERM TERM} is not taking into consideration when
 *         matching, i.e. a term (i.e. its items) is matched when it's found anywhere in the input string.
 *     </li>
 *     <li>
 *         All {@link TERM TERMs} should be found in the input string for the intent to match.
 *     </li>
 *     <li>
 *         If multiple intents match - the system will pick the one with the most specific match.
 *     </li>
 * </ul>
 * Here's an example of using token solver taken from Time Example:
 * <pre class="brush: java">
 * NCTokenSolver solver = new NCTokenSolver(
 *      "time-solver",
 *      false,
 *      __ -&gt; { throw new NCRejection("Seems unrelated (check city name)."); }
 * );
 *
 * // Check for exactly one 'x:time' token without looking into conversation.
 * // That's an indication of asking for local time. Note that non-conversational intents are
 * // always more specific that conversational intents since they deal only with the most
 * // recent user input (i.e. without looking into conversation history).
 * solver.addIntent(
 *      new NON_CONV_INTENT("id == x:time", 1, 1), // Term index 0.
 *      this::onLocalMatch
 * );
 *
 * // Check for exactly one 'x:time' and one 'nlp:geo' CITY token including conversation
 * // context. That can be either local or remote time.
 * solver.addIntent(
 *      new CONV_INTENT(
 *          new TERM("id == x:time", 1, 1), // Term index 0.
 *          new TERM(new AND(               // Term index 1.
 *              "id == nlp:geo",
 *              "~GEO_KIND == CITY"
 *          ), 1, 1)
 *      ),
 *      this::onRemoteMatch
 * );
 * </pre>
 *
 * @see INTENT INTENT
 * @see CONV_INTENT CONV_INTENT
 * @see NON_CONV_INTENT NON_CONV_INTENT
 * @see ITEM ITEM
 * @see RULE RULE
 * @see OR OR
 * @see AND AND
 * @see NAND NAND
 * @see XOR XOR
 * @see XNOR XNOR
 * @see NOR NOR
 * @see TERM TERM
 */
public class NCIntentSolver {
    private static final Logger log = LoggerFactory.getLogger(NCIntentSolver.class);
    private static final int[] EMPTY_WEIGHT = {0, 0, 0};
    private static final Pair<Boolean, int[]> EMPTY_PAIR = Pair.of(false, EMPTY_WEIGHT);
    private static final Supplier<NCQueryResult> DFLT_NOT_FOUND = () -> {
        throw new NCRejection("Request seems unrelated to the data source.");
    };
    private static final String DFLT_NAME = "default";

    /**
     * Marker type of pattern items. Only built-in {@link OR}, {@link AND}, {@link NAND}, {@link NOR}, {@link XOR},
     * {@link XNOR} or {@link RULE} implementations are supported, i.e. no user-defined implementation are
     * allowed.
     *
     * @see RULE RULE
     * @see OR OR
     * @see AND AND
     * @see NAND NAND
     * @see XOR XOR
     * @see XNOR XNOR
     * @see NOR NOR
     */
    public interface Predicate extends Function<NCToken, Pair<Boolean/*Pass or no-pass*/, int[]/*Weight*/>> {}

    /**
     * Callback provided by the user for an intent when it is matched. It takes solver
     * context as a parameter and should return query result. It can also throw either
     * {@link NCRejection} or {@link NCIntentSkip} exceptions.
     *
     * @see NCIntentSolver#addIntent(INTENT, IntentCallback)
     */
    public interface IntentCallback extends Function<NCIntentSolverContext, NCQueryResult> {}

    /**
     * Checks that combinator is one of the built-in implementations.
     * 
     * @param obj Combinator to check.
     */
    private static void checkCombinator(Predicate obj) {
        if (!(obj instanceof AND) &&
            !(obj instanceof OR) &&
            !(obj instanceof XOR) &&
            !(obj instanceof XNOR) &&
            !(obj instanceof NOR) &&
            !(obj instanceof NAND) &&
            !(obj instanceof RULE))
            throw new IllegalArgumentException("Combinators can only combine AND, OR, NAND, XOR, XNOR, NOR or RULE.");
    }

    /**
     * OR-combinator for token predicates. This combinator produces {@code true} output only when at least
     * one of its constituent predicates produce {@code true} result.
     *
     * @see OR OR
     * @see AND AND
     * @see NAND NAND
     * @see XOR XOR
     * @see XNOR XNOR
     * @see NOR NOR
     * @see RULE RULE
     */
    public static final class OR extends ArrayList<Predicate> implements Predicate {
        /**
         * Creates new OR-combinator with given items.
         * Note that only built-in {@link OR OR}, {@link AND AND}, {@link NAND NAND},
         * {@link NOR NOR}, {@link XOR XOR}, {@link XNOR XNOR} or {@link RULE RULE} item are allowed.
         *
         * @param items Items to OR combine.
         */
        public OR(Predicate... items) {
            if (items == null || items.length < 2)
                throw new IllegalArgumentException("OR combinator should have at least two elements.");

            for (Predicate item : items)
                checkCombinator(item);

            addAll(Arrays.asList(items));
        }

        /**
         * Creates new OR-combinator with given string-based {@link RULE}s:
         * <pre class="brush: java">
         *      new OR("id == x:time", "~GEO_KIND == CITY", "value %% ^[Ff]oo[Bb]ar$");
         * </pre>
         *
         * @param exprs List of whitespace separated string of parameter, its operation and value
         *      for the {@link RULE}.
         */
        public OR(String... exprs) {
            if (exprs == null || exprs.length < 2)
                throw new IllegalArgumentException("OR combinator should have at least two elements.");

            addAll(Arrays.stream(exprs).map(RULE::new).collect(Collectors.toList()));
        }

        @Override
        public Pair<Boolean, int[]> apply(NCToken tok) {
            for (Predicate tp : this) {
                Pair<Boolean, int[]> pair = tp.apply(tok);

                boolean pass = pair.getLeft();
                int[] weight = pair.getRight();

                if (pass)
                    return Pair.of(true, weight);
            }

            return EMPTY_PAIR;
        }

        @Override
        public String toString() {
            return stream().map(Object::toString).collect(Collectors.joining(" OR ", "(", ")"));
        }
    }

    /**
     * XOR (exclusive OR)-combinator for token predicates. This combinator produces {@code true} output only
     * when only one of its constituent predicates produce {@code true} result.
     *
     * @see OR OR
     * @see AND AND
     * @see NAND NAND
     * @see XOR XOR
     * @see XNOR XNOR
     * @see NOR NOR
     * @see RULE RULE
     */
    public static final class XOR extends ArrayList<Predicate> implements Predicate {
        /**
         * Creates new XOR-combinator with given items.
         * Note that only built-in {@link OR OR}, {@link AND AND}, {@link NAND NAND},
         * {@link NOR NOR}, {@link XOR XOR}, {@link XNOR XNOR} or {@link RULE RULE} item are allowed.
         *
         * @param items Items to XOR combine.
         */
        public XOR(Predicate... items) {
            if (items == null || items.length < 2)
                throw new IllegalArgumentException("XOR combinator should have at least two elements.");

            for (Predicate item : items)
                checkCombinator(item);

            addAll(Arrays.asList(items));
        }

        /**
         * Creates new XOR-combinator with given string-based {@link RULE}s:
         * <pre class="brush: java">
         *      new XOR("id == x:time", "~GEO_KIND == CITY", "value %% ^[Ff]oo[Bb]ar$");
         * </pre>
         *
         * @param exprs List of whitespace separated string of parameter, its operation and value
         *      for the {@link RULE}.
         */
        public XOR(String... exprs) {
            if (exprs == null || exprs.length < 2)
                throw new IllegalArgumentException("XOR combinator should have at least two elements.");

            addAll(Arrays.stream(exprs).map(RULE::new).collect(Collectors.toList()));
        }

        @Override
        public Pair<Boolean, int[]> apply(NCToken tok) {
            Pair<Boolean, int[]> xorPair = null;

            for (Predicate tp : this) {
                Pair<Boolean, int[]> pair = tp.apply(tok);

                boolean pass = pair.getLeft();

                if (pass) {
                    if (xorPair == null)
                        xorPair = pair;
                    else
                        return EMPTY_PAIR;
                }
            }

            return xorPair == null ? EMPTY_PAIR : xorPair;
        }

        @Override
        public String toString() {
            return stream().map(Object::toString).collect(Collectors.joining(" XOR ", "(", ")"));
        }
    }

    /**
     * NAND-combinator for token predicates. This combinator produces {@code false} output when all of its
     * constituent predicates produce {@code true} results. In all other cases it produces {@code true} output.
     *
     * @see OR OR
     * @see AND AND
     * @see NAND NAND
     * @see XOR XOR
     * @see XNOR XNOR
     * @see NOR NOR
     * @see RULE RULE
     */
    public static final class NAND extends ArrayList<Predicate> implements Predicate {
        /**
         * Creates new NAND-combinator with given items.
         * Note that only built-in {@link OR OR}, {@link AND AND}, {@link NAND NAND},
         * {@link NOR NOR}, {@link XOR XOR}, {@link XNOR XNOR} or {@link RULE RULE} item are allowed.
         *
         * @param items Items to NAND combine.
         */
        public NAND(Predicate... items) {
            if (items == null || items.length < 2)
                throw new IllegalArgumentException("NAND combinator should have at least two elements.");

            for (Predicate item : items)
                checkCombinator(item);

            addAll(Arrays.asList(items));
        }

        /**
         * Creates new NAND-combinator with given string-based {@link RULE RULEs}:
         * <pre class="brush: java">
         *      new NAND("id == x:time", "~GEO_KIND == CITY", "value %% ^[Ff]oo[Bb]ar$");
         * </pre>
         *
         * @param exprs List of whitespace separated string of parameter, its operation and value
         *      for the {@link RULE RULE}.
         */
        public NAND(String... exprs) {
            if (exprs == null || exprs.length < 2)
                throw new IllegalArgumentException("NAND combinator should have at least two elements.");

            addAll(Arrays.stream(exprs).map(RULE::new).collect(Collectors.toList()));
        }

        @Override
        public Pair<Boolean, int[]> apply(NCToken tok) {
            int[] w = {0, 0, 0};

            boolean foundFalse = false;

            for (Predicate tp : this) {
                Pair<Boolean, int[]> pair = tp.apply(tok);

                boolean pass = pair.getLeft();
                int[] weight = pair.getRight();

                if (!pass)
                    foundFalse = true;

                w[0] += weight[0];
                w[1] += weight[1];
                w[2] += weight[2];
            }

            return foundFalse ? Pair.of(true, w) : EMPTY_PAIR;
        }

        @Override
        public String toString() {
            return stream().map(Object::toString).collect(Collectors.joining(" NAND ", "(", ")"));
        }
    }

    /**
     * XNOR (exclusive NOR)-combinator for token predicates. This combinator produces {@code true} output only
     * when all of its constituent predicates produce the same (either {@code false} or {@code true}) results.
     *
     * @see OR OR
     * @see AND AND
     * @see NAND NAND
     * @see XOR XOR
     * @see XNOR XNOR
     * @see NOR NOR
     * @see RULE RULE
     */
    public static final class XNOR extends ArrayList<Predicate> implements Predicate {
        /**
         * Creates new XNOR-combinator with given items.
         * Note that only built-in {@link OR OR}, {@link AND AND}, {@link NAND NAND},
         * {@link NOR NOR}, {@link XOR XOR}, {@link XNOR XNOR} or {@link RULE RULE} item are allowed.
         *
         * @param items Items to XNOR combine.
         */
        public XNOR(Predicate... items) {
            if (items == null || items.length < 2)
                throw new IllegalArgumentException("XNOR combinator should have at least two elements.");

            for (Predicate item : items)
                checkCombinator(item);

            addAll(Arrays.asList(items));
        }

        /**
         * Creates new XNOR-combinator with given string-based {@link RULE}s:
         * <pre class="brush: java">
         *      new XNOR("id == x:time", "~GEO_KIND == CITY", "value %% ^[Ff]oo[Bb]ar$");
         * </pre>
         *
         * @param exprs List of whitespace separated string of parameter, its operation and value
         *      for the {@link RULE}.
         */
        public XNOR(String... exprs) {
            if (exprs == null || exprs.length < 2)
                throw new IllegalArgumentException("XNOR combinator should have at least two elements.");

            addAll(Arrays.stream(exprs).map(RULE::new).collect(Collectors.toList()));
        }

        @Override
        public Pair<Boolean, int[]> apply(NCToken tok) {
            boolean first = get(0).apply(tok).getLeft();

            int[] w = {0, 0, 0};

            for (Predicate tp : this) {
                Pair<Boolean, int[]> pair = tp.apply(tok);

                boolean pass = pair.getLeft();
                int[] weight = pair.getRight();

                if (pass != first)
                    return EMPTY_PAIR;
                else {
                    w[0] += weight[0];
                    w[1] += weight[1];
                    w[2] += weight[2];
                }
            }

            return Pair.of(true, w);
        }

        @Override
        public String toString() {
            return stream().map(Object::toString).collect(Collectors.joining(" XNOR ", "(", ")"));
        }
    }

    /**
     * NOR-combinator for token predicates. This combinator produces {@code true} output only when all of
     * its constituent predicates produce {@code false} result.
     *
     * @see OR OR
     * @see AND AND
     * @see NAND NAND
     * @see XOR XOR
     * @see XNOR XNOR
     * @see NOR NOR
     * @see RULE RULE
     */
    public static final class NOR extends ArrayList<Predicate> implements Predicate {
        /**
         * Creates new NOR-combinator with given items.
         * Note that only built-in {@link OR OR}, {@link AND AND}, {@link NAND NAND},
         * {@link NOR NOR}, {@link XOR XOR}, {@link XNOR XNOR} or {@link RULE RULE} item are allowed.
         *
         * @param items Items to NOR combine.
         */
        public NOR(Predicate... items) {
            if (items == null || items.length < 2)
                throw new IllegalArgumentException("NOR combinator should have at least two elements.");

            for (Predicate item : items)
                checkCombinator(item);

            addAll(Arrays.asList(items));
        }

        /**
         * Creates new NOR-combinator with given string-based {@link RULE}s:
         * <pre class="brush: java">
         *      new NOR("id == x:time", "~GEO_KIND == CITY", "value %% ^[Ff]oo[Bb]ar$");
         * </pre>
         *
         * @param exprs List of whitespace separated string of parameter, its operation and value
         *      for the {@link RULE}.
         */
        public NOR(String... exprs) {
            if (exprs == null || exprs.length < 2)
                throw new IllegalArgumentException("NOR combinator should have at least two elements.");

            addAll(Arrays.stream(exprs).map(RULE::new).collect(Collectors.toList()));
        }

        @Override
        public Pair<Boolean, int[]> apply(NCToken tok) {
            int[] w = {0, 0, 0};

            for (Predicate tp : this) {
                Pair<Boolean, int[]> pair = tp.apply(tok);

                boolean pass = pair.getLeft();
                int[] weight = pair.getRight();

                if (pass)
                    return EMPTY_PAIR;
                else {
                    w[0] += weight[0];
                    w[1] += weight[1];
                    w[2] += weight[2];
                }
            }

            return Pair.of(true, w);
        }

        @Override
        public String toString() {
            return stream().map(Object::toString).collect(Collectors.joining(" NOR ", "(", ")"));
        }
    }

    /**
     * AND-combinator for token predicates. This combinator produces {@code true} output only when all of its
     * constituent predicates produce {@code true} results.
     *
     * @see OR OR
     * @see AND AND
     * @see NAND NAND
     * @see XOR XOR
     * @see XNOR XNOR
     * @see NOR NOR
     * @see RULE RULE
     */
    public static final class AND extends ArrayList<Predicate> implements Predicate {
        /**
         * Creates new AND-combinator with given items.
         * Note that only built-in {@link OR OR}, {@link AND AND}, {@link NAND NAND},
         * {@link NOR NOR}, {@link XOR XOR}, {@link XNOR XNOR} or {@link RULE RULE} item are allowed.
         *
         * @param items Items to AND combine.
         */
        public AND(Predicate... items) {
            if (items == null || items.length < 2)
                throw new IllegalArgumentException("AND combinator should have at least two elements.");

            for (Predicate item : items)
                checkCombinator(item);

            addAll(Arrays.asList(items));
        }

        /**
         * Creates new AND-combinator with given string-based {@link RULE RULEs}:
         * <pre class="brush: java">
         *      new AND("id == x:time", "~GEO_KIND == CITY", "value %% ^[Ff]oo[Bb]ar$");
         * </pre>
         *
         * @param exprs List of whitespace separated string of parameter, its operation and value
         *      for the {@link RULE RULE}.
         */
        public AND(String... exprs) {
            if (exprs == null || exprs.length < 2)
                throw new IllegalArgumentException("AND combinator should have at least two elements.");

            addAll(Arrays.stream(exprs).map(RULE::new).collect(Collectors.toList()));
        }

        @Override
        public Pair<Boolean, int[]> apply(NCToken tok) {
            int[] w = {0, 0, 0};

            for (Predicate tp : this) {
                Pair<Boolean, int[]> pair = tp.apply(tok);

                boolean pass = pair.getLeft();
                int[] weight = pair.getRight();

                if (!pass)
                    return EMPTY_PAIR;
                else {
                    w[0] += weight[0];
                    w[1] += weight[1];
                    w[2] += weight[2];
                }
            }

            return Pair.of(true, w);
        }

        @Override
        public String toString() {
            return stream().map(Object::toString).collect(Collectors.joining(" AND ", "(", ")"));
        }
    }

    /**
     * Binary operator based on declarative condition over {@link NCToken token}.
     *
     * @see OR OR
     * @see AND AND
     * @see NAND NAND
     * @see XOR XOR
     * @see XNOR XNOR
     * @see NOR NOR
     */
    public static final class RULE implements Predicate {
        private static final List<String> OPS = Arrays.asList(
            // Order is important!
            "==",
            "!=",
            ">=",
            "<=",
            "%%",
            "!%",
            ">",
            "<"
        );

        private static final List<String> PARAMS = Arrays.asList(
            "id",
            "parent",
            "group",
            "type",
            "value"
        );

        private String param, op;
        private Object value;
        private int[] weight;

        /**
         * Creates new binary operator with given parameters. Here's few examples of the rules:
         * <pre class="brush: java">
         *      new RULE("id", "==", "x:time");
         *      new RULE("~GEO_KIND", "==", "CITY");
         *      new RULE("value", "%%", "^[Ff]oo[Bb]ar$");
         * </pre>
         *
         * @param param Rule's left-side parameter. Parameter can be one of the following:
         * <table summary="" class="dl-table">
         *     <tr>
         *         <th>Parameter</th>
         *         <th>Description</th>
         *     </tr>
         *     <tr>
         *         <td><code>id</code></td>
         *         <td>
         *             Token {@link NCToken#getId() ID}
         *         </td>
         *     </tr>
         *     <tr>
         *         <td><code>group</code></td>
         *         <td>
         *             Token {@link NCToken#getGroup() group}
         *         </td>
         *     </tr>
         *     <tr>
         *         <td><code>parent</code></td>
         *         <td>
         *             Token {@link NCToken#getParentId() parent} ID
         *         </td>
         *     </tr>
         *     <tr>
         *         <td><code>value</code></td>
         *         <td>
         *             Token {@link NCToken#getValue() value}
         *         </td>
         *     </tr>
         *     <tr>
         *         <td><code>~META</code></td>
         *         <td>
         *             Token {@link NCToken#getMetadata() metadata} or
         *             {@link NCToken#getElementMetadata() element metadata} property {@code META}.
         *         </td>
         *     </tr>
         * </table>
         * @param op Rule's operation. Operation can be one of the following:
         * <table summary="" class="dl-table">
         *     <tr>
         *         <th>Operation</th>
         *         <th>Description</th>
         *     </tr>
         *     <tr>
         *         <td><code>==</code></td>
         *         <td>
         *             <code>param</code> is equal to <code>value</code>.
         *             Java {@link Object#equals(Object)} method is used for equality check.
         *         </td>
         *     </tr>
         *     <tr>
         *         <td><code>!=</code></td>
         *         <td>
         *             <code>param</code> is not equal to <code>value</code>.
         *             Java {@link Object#equals(Object)} method is used for equality check.
         *         </td>
         *     </tr>
         *     <tr>
         *         <td><code>&gt;=</code></td>
         *         <td>
         *             <code>param</code> is greater or equal than <code>value</code>.
         *             Applicable to {@link Number} and {@link ZonedDateTime} values only.
         *         </td>
         *     </tr>
         *     <tr>
         *         <td><code>&lt;=</code></td>
         *         <td>
         *             <code>param</code> is less or equal than <code>value</code>.
         *             Applicable to {@link Number} and {@link ZonedDateTime} values only.
         *         </td>
         *     </tr>
         *     <tr>
         *         <td><code>&gt;</code></td>
         *         <td>
         *             <code>param</code> is greater than <code>value</code>.
         *             Applicable to {@link Number} and {@link ZonedDateTime} values only.
         *         </td>
         *     </tr>
         *     <tr>
         *         <td><code>&lt;</code></td>
         *         <td>
         *             <code>param</code> is less than <code>value</code>.
         *             Applicable to {@link Number} and {@link ZonedDateTime} values only.
         *         </td>
         *     </tr>
         *     <tr>
         *         <td><code>%%</code></td>
         *         <td>
         *             <code>param</code> matches the regular expression in <code>value</code>.
         *             Applicable to {@link String} and {@link Pattern} values only.
         *         </td>
         *     </tr>
         *     <tr>
         *         <td><code>!%</code></td>
         *         <td>
         *             <code>param</code> does not match the regular expression in <code>value</code>.
         *             Applicable to {@link String} and {@link Pattern} values only.
         *         </td>
         *     </tr>
         * </table>
         * @param value Rule's right-side string value. It's processing depends on {@code param} and {@code op}.
         */
        public RULE(String param, String op, Object value) {
            init(param, op, value);
        }

        /**
         * Shortcut constructor for use cases where rule expression can be expressed as a
         * whitespace separated string of parameter, its operation and value. Parameter and operation cannot
         * have whitespaces in them. Value will auto detected as {@code null}, {@code boolean}, {@code Integer} or
         * a {@code String} otherwise.
         * <br><br>
         * Here's few examples of the rules:
         * <pre class="brush: java">
         *      new RULE("id == x:time"); // Value is converted to string.
         *      new RULE("~GEO_KIND == CITY");  // Value is converted to string.
         *      new RULE("value %% ^[Ff]oo[Bb]ar$"); // Value is converted to string.
         *      new RULE("~NUM_INDEX != null"); // Value is converted to 'null'.
         *      new RULE("~NUM_VALUE &gt;= 100"); // Value is converted to integer.
         * </pre>
         *
         * @param expr Whitespace separated string of parameter, its operation and value for this rule.
         */
        public RULE(String expr) {
             String[] parts = expr.trim().split("\\s+");

             if (parts.length < 3)
                 throw new IllegalArgumentException("Invalid rule expression: " + expr);

             String param = parts[0];
             String op = parts[1];
             String value = Arrays.stream(parts).skip(2).collect(Collectors.joining(" "));

             if (value.equals("null"))
                 init(param, op, null);
             else if (value.equalsIgnoreCase("true"))
                 init(param, op, true);
             else if (value.equalsIgnoreCase("false"))
                 init(param, op, false);
             else
                 try {
                     init(param, op, Integer.parseInt(value));
                 }
                 catch (NumberFormatException e) {
                     init(param, op, value);
                 }
        }

        /**
         *
         * @param param Parameter.
         * @param op Operation.
         * @param value Value.
         */
        private void init(String param, String op, Object value) {
            String param0 = param == null ? null : param.trim();
            String op0 = op == null ? null : op.trim();

            if (param0 == null || (param0.charAt(0) != '~' && !PARAMS.contains(param0)))
                throw new IllegalArgumentException("Invalid rule's parameter: " + param0);

            if (!OPS.contains(op0))
                throw new IllegalArgumentException("Invalid rule's operation: " + op0);

            this.param = param0;
            this.op = op0;
            this.value = value;

            weight = calcWeight();
        }

        /**
         * 
         * @return Rule's weight.
         */
        private int[] calcWeight() {
            int[] w = new int[3];

            // Calculation weights.
            switch (param){
                case "id": w[0] = 6; break;
                case "value": w[0] = 5; break;
                case "group": w[0] = 4; break;
                case "type": w[0] = 2; break;
                case "parent": w[0] = 1; break;

                default:
                    if (param.charAt(0) == '~')
                        w[0] = 3;
                    else
                        throw new AssertionError("Unexpected parameter: " + param);
            }

            switch (op) {
                case "==": w[1] = 4; break;
                case ">":
                case ">=":
                case "<":
                case "<=": w[1] = 3; break;
                case "%%":
                case "!%": w[1] = 2; break;
                case "!=": w[1] = 1; break;

                default:
                    throw new AssertionError("Unexpected operation: " + op);
            }

            w[2] = value == null? 2 : 1;

            return w;
        }

        /**
         * Gets logical weight of this rule. Used internally only.
         *
         * @return Logical weight of this rule.
         */
        int[] getWeight() {
            return weight;
        }

        /**
         * Gets left-side parameter of this rule.
         *
         * @return Left-side parameter of this rule.
         */
        public String getParameter() {
            return param;
        }

        /**
         * Gets binary operation of this rule.
         *
         * @return Binary operation of this rule.
         */
        public String getOp() {
            return op;
        }

        /**
         * Gets right-side value of this rule.
         *
         * @return Right-side value of this rule.
         */
        public Object getValue() {
            return value;
        }

        /**
         *
         */
        private NCException mkOperatorError(Object v1, Object v2) {
            return new NCException(String.format("Unexpected operator: %s for elements: %s, %s",
                op, v1.toString(), v2.toString()));
        }

        @Override
        public Pair<Boolean, int[]> apply(NCToken tok) {
            return apply0(tok) ? Pair.of(true, weight) : EMPTY_PAIR;
        }

        private boolean apply0(NCToken tok) {
            Object v1 = null;

            if (param.charAt(0) == '~') {
                String name = param.substring(1);

                if (name.isEmpty())
                    throw new NCException("Empty meta parameter name.");

                NCMetadata tokMeta = tok.getMetadata();
                NCMetadata elmMeta = tok.getElementMetadata();

                if (tokMeta.containsKey(name))
                    v1 = tokMeta.get(name);
                else if (elmMeta.containsKey(name))
                    v1 = elmMeta.get(name);
            }
            else
                switch (param) {
                    case "id": v1 = tok.getId().trim(); break;
                    case "group": v1 = tok.getGroup().trim(); break;
                    case "value":  v1 = tok.getValue().trim(); break;
                    case "parent": v1 = tok.getParentId().trim(); break;

                    default: throw new NCException("Unexpected parameter: " + param);
            }

            Object v2 = value;

            switch (op) {
                case "==": return Objects.equals(v1, v2);

                case "!=": return !(Objects.equals(v1, v2));

                case "!%":
                    if (v2 == null || !(v1 instanceof String))
                        return true;
                    else if (v2 instanceof String)
                        return !Pattern.matches((String)v2, (String)v1);
                    else if (v2 instanceof Pattern)
                        return !((Pattern)v2).matcher((String)v1).find();
                    else
                        throw mkOperatorError(v1, v2);

                case "%%":
                    if (v2 == null || !(v1 instanceof String))
                        return false;
                    else if (v2 instanceof String)
                        return Pattern.matches((String)v2, (String)v1);
                    else if (v2 instanceof Pattern)
                        return ((Pattern)v2).matcher((String)v1).find();
                    else
                        throw mkOperatorError(v1, v2);

                case ">":
                    if (v1 == v2 || v1 == null || v2 == null)
                        return false;
                    else if (v1 instanceof Number && v2 instanceof Number)
                        return ((Number)v1).doubleValue() > ((Number)v2).doubleValue();
                    else if (v1 instanceof ZonedDateTime && v2 instanceof ZonedDateTime)
                        return ((ZonedDateTime)v1).isBefore((ZonedDateTime)v2);
                    else
                        throw mkOperatorError(v1, v2);

                case ">=":
                    if (v1 == v2)
                        return true;
                    else if (v1 == null || v2 == null)
                        return false;
                    else if (v1 instanceof Number && v2 instanceof Number)
                        return ((Number)v1).doubleValue() >= ((Number)v2).doubleValue();
                    else if (v1 instanceof ZonedDateTime && v2 instanceof ZonedDateTime) {
                        ZonedDateTime z1 = (ZonedDateTime)v1;
                        ZonedDateTime z2 = (ZonedDateTime)v2;

                        return z1.equals(z2) || z1.isBefore(z2);
                    }
                    else
                        throw mkOperatorError(v1, v2);
                case "<":
                    if (v1 == v2 || v1 == null || v2 == null)
                        return false;
                    else if (v1 instanceof Number && v2 instanceof Number)
                        return ((Number)v1).doubleValue() < ((Number)v2).doubleValue();
                    else if (v1 instanceof ZonedDateTime && v2 instanceof ZonedDateTime)
                        return ((ZonedDateTime)v1).isAfter((ZonedDateTime)v2);
                    else
                        throw mkOperatorError(v1, v2);

                case "<=":
                    if (v1 == v2)
                        return true;
                    else if (v1 == null || v2 == null)
                        return false;
                    else if (v1 instanceof Number && v2 instanceof Number)
                        return ((Number)v1).doubleValue() <= ((Number)v2).doubleValue();
                    else if (v1 instanceof ZonedDateTime && v2 instanceof ZonedDateTime) {
                        ZonedDateTime z1 = (ZonedDateTime)v1;
                        ZonedDateTime z2 = (ZonedDateTime)v2;

                        return z1.equals(z2) || z1.isAfter(z2);
                    }
                    else
                        throw mkOperatorError(v1, v2);

                default:
                    throw new AssertionError("Unexpected operation: " + op);
            }
        }

        @Override
        public String toString() {
            return String.format("%s%s%s", param, op, value);
        }
    }

    /**
     * Item is a token predicate plus quantifiers. It is a building block for {@link TERM TERM}.
     */
    public static final class ITEM {
        final private Predicate ptrn;
        final private int min, max;

        /**
         * Creates new item with given parameters.
         *
         * @param pred Built-in token predicate: either one of the built-in logical combinators or {@link RULE RULE}.
         * @param min Minimum (inclusive) number of times the token predicate {@code pred} should find a match in
         *      the user input for this item to match.
         * @param max Maximum (inclusive) number of times the token predicate {@code pred} should find a match in
         *      the user input for this item to match.
         */
        public ITEM(Predicate pred, int min, int max) {
            if (pred == null)
                throw new IllegalArgumentException("Item pattern cannot be null.");
            if (min < 0 || min > max || max < 1)
                throw new IllegalArgumentException("Invalid item quantifiers (must be min >= 0, min <= max, max > 0).");

            this.ptrn = pred;
            this.min = min;
            this.max = max;
        }

        /**
         * Gets token predicate for this item.
         *
         * @return Token predicate for this item.
         */
        public Predicate getPattern() {
            return ptrn;
        }

        /**
         * Gets minimum inclusive quantifier.
         *
         * @return Minimum inclusive quantifier.
         */
        public int getMin() {
            return min;
        }

        /**
         * Gets maximum inclusive quantifier.
         *
         * @return Maximum inclusive quantifier.
         */
        public int getMax() {
            return max;
        }

        @Override
        public String toString() {
            return String.format("ITEM{%s [%d,%d]}", ptrn, min, max);
        }
    }

    /**
     * Term is a building block of the {@link INTENT INTENT}. Term is a collection of one or more {@link ITEM ITEMs}.
     * Intent has a list of terms that all have to be found in the user input for the intent to match. Note that
     * order of items is not important for matching the term.
     */
    public static final class TERM {
        final private ITEM[] items;

        /**
         * Creates new term with given parameters.
         *
         * @param items List of items that define conditions for that term to match.
         */
        public TERM(ITEM... items) {
            if (items.length == 0)
                throw new IllegalArgumentException("Term should have at least one item.");

            this.items = items;
        }

        /**
         * Shortcut constructor for term with a single item. It is equivalent to:
         * <pre class="brush: java">
         *      this(new ITEM(ptrn, min, max));
         * </pre>
         *
         * @param pred Built-in token predicate: either one of the logical combinators or {@link RULE RULE}.
         * @param min Minimum quantifier for this predicate.
         * @param max Maximum quantifier for this predicate.
         */
        public TERM(Predicate pred, int min, int max) {
            this(new ITEM(pred, min, max));
        }

        /**
         * Shortcut constructor for term with a single string-based rule. It is equivalent to:
         * <pre class="brush: java">
         *      this((String)null, new ITEM(new RULE(expr), min, max));
         * </pre>
         *
         * @param expr Whitespace separated string of parameter, its operation and value for the {@link RULE RULE}.
         * @param min Minimum quantifier for this predicate.
         * @param max Maximum quantifier for this predicate.
         */
        public TERM(String expr, int min, int max) {
            this(new ITEM(new RULE(expr), min, max));
        }

        /**
         * Gets the collection of term items.
         *
         * @return Collection of term items.
         */
        public ITEM[] getItems() {
            return items;
        }

        @Override
        public String toString() {
            return String.format("TERM(%s)", Arrays.stream(items).map(Object::toString).collect(Collectors.joining(", ")));
        }
    }

    /**
     * Token solver intent.
     * <br><br>
     * Intent is similar in idea to a regular expressions. Just like regular expression defines a search
     * pattern for text, the intent defines a search pattern for the list of tokens. Intent has hierarchical
     * structure: it consists of {@link TERM TERMs}, where each term consists of {@link ITEM ITEMs} where each
     * item is a predicate ({@link RULE RULE} or one of the logical combinators) with quantifiers.
     *
     * @see CONV_INTENT CONV_INTENT
     * @see NON_CONV_INTENT NON_CONV_INTENT
     */
    public static class INTENT {
        final private String id;
        final private TERM[] terms;
        final private boolean inclConv;
        final private boolean ordered;

        /**
         * Creates new intent with given parameters.
         *
         * @param id Intent ID. It can be any arbitrary string meaningful for the developer.
         *      Look at the example for one approach to provide descriptive intent IDs.
         * @param inclConv Whether or not to include conversation into the search for this intent. If conversation
         *      is not included than only the tokens present in the user input will be considered. 
         * @param ordered Whether or not the specified order of {@link TERM TERMs} is important for matching
         *      this intent. If intent is unordered its {@link TERM TERMs} can be found anywhere in the string
         *      and in any order.
         * @param terms List of {@link TERM TERMs} defining this intent.
         */
        public INTENT(String id, boolean inclConv, boolean ordered, TERM... terms) {
            if (terms.length == 0)
                throw new IllegalArgumentException("Intent should have at least one term.");

            this.id = id;
            this.inclConv = inclConv;
            this.ordered = ordered;
            this.terms = terms;
        }

        /**
         * Whether or not this intent is ordered, i.e. specified order of {@link TERM TERMs} is important
         * for matching this intent.
         * 
         * @return {@code True} if this intent is ordered, {@code false} otherwise.
         */
        public boolean isOrdered() {
            return ordered;
        }

        /**
         * Gets conversation policy flag.
         *
         * @return Conversation policy flag.
         */
        public boolean isIncludeConversation() {
            return inclConv;
        }

        /**
         * Gets list of terms defining this intent.
         *
         * @return List of terms defining this intent.
         */
        public TERM[] getTerms() {
            return terms;
        }
    
        /**
         * Gets ID of this intent.
         *
         * @return Intent ID.
         */
        public String getId() {
            return id;
        }
    
        @Override
        public String toString() {
            return String.format("INTENT(id='%s', inclConv=%b, ordered=%b, %s)",
                id,
                inclConv,
                ordered,
                Arrays.stream(terms).map(Object::toString).collect(Collectors.joining(", "))
            );
        }
    }

    /**
     * Convenient adapter for conversational intent.
     */
    public static class CONV_INTENT extends INTENT {
        /**
         * Creates new exact match, unordered intent <b>with</b> conversation support and
         * number of free tokens equal to 3 (which is a good general default value).
         * <br><br>
         * This is equivalent to:
         * <pre class="brush: java">
         *     new INTENT(id, true, false, terms);
         * </pre>
         *
         * @param id Intent ID. It can be any arbitrary string meaningful for the developer.
         *      Look at the example for one approach to provide descriptive intent IDs.
         * @param terms List of terms defining this intent.
         */
        public CONV_INTENT(String id, TERM... terms) {
            super(id, true, false, terms);
        }

        /**
         * Shortcut constructor for an exact match, unordered, conversational intent with just one {@link TERM TERM}.
         * It is equivalent to:
         * <pre class="brush: java">
         *      this(id, new TERM(pred, min, max));
         * </pre>
         *
         * @param id Intent ID. It can be any arbitrary string meaningful for the developer.
         *      Look at the example for one approach to provide descriptive intent IDs.
         * @param pred Built-in token predicate: either one of the logical combinators or {@link RULE RULE}.
         * @param min Minimum quantifier for this predicate.
         * @param max Maximum quantifier for this predicate.
         */
        public CONV_INTENT(String id, Predicate pred, int min, int max) {
            this(id, new TERM(pred, min, max));
        }

        /**
         * Shortcut constructor for an exact match, unordered, conversational intent with just one {@link TERM TERM}.
         * It is equivalent to:
         * <pre class="brush: java">
         *      this(id, new TERM(expr, min, max));
         * </pre>
         *
         * @param id Intent ID. It can be any arbitrary string meaningful for the developer.
         *      Look at the example for one approach to provide descriptive intent IDs.
         * @param expr Whitespace separated string of parameter, its operation and value for the {@link RULE RULE}.
         * @param min Minimum quantifier for this predicate.
         * @param max Maximum quantifier for this predicate.
         */
        public CONV_INTENT(String id, String expr, int min, int max) {
            this(id, new TERM(expr, min, max));
        }
    }

    /**
     * Convenient adapter for non-conversational intent.
     */
    public static class NON_CONV_INTENT extends INTENT {
        /**
         * Creates new exact match, unordered, intent <b>without</b> conversation support and
         * number of free tokens equal to 3 (which is a good general default value).
         * <br><br>
         * This is equivalent to:
         * <pre class="brush: java">
         *     new INTENT(id, false, false, terms);
         * </pre>
         *
         * @param id Intent ID. It can be any arbitrary string meaningful for the developer.
         *      Look at the example for one approach to provide descriptive intent IDs.
         * @param terms List of terms defining this intent.
         */
        public NON_CONV_INTENT(String id, TERM... terms) {
            super(id, false, false, terms);
        }

        /**
         * Shortcut constructor for an exact match, unordered, non-conversational intent with just one {@link TERM TERM}.
         * It is equivalent to:
         * <pre class="brush: java">
         *      this(id, new TERM(pred, min, max));
         * </pre>
         *
         * @param id Intent ID. It can be any arbitrary string meaningful for the developer.
         *      Look at the example for one approach to provide descriptive intent IDs.
         * @param pred Built-in token predicate: either one of the logical combinators or {@link RULE RULE}.
         * @param min Minimum quantifier for this predicate.
         * @param max Maximum quantifier for this predicate.
         */
        public NON_CONV_INTENT(String id, Predicate pred, int min, int max) {
            this(id, new TERM(pred, min, max));
        }

        /**
         * Shortcut constructor for an exact match, unordered, non-conversational intent with just one {@link TERM TERM}.
         * It is equivalent to:
         * <pre class="brush: java">
         *      this(id, new TERM(expr, min, max));
         * </pre>
         *
         * @param id Intent ID. It can be any arbitrary string meaningful for the developer.
         *      Look at the example for one approach to provide descriptive intent IDs.
         * @param expr Whitespace separated string of parameter, its operation and value for the {@link RULE RULE}.
         * @param min Minimum quantifier for this predicate.
         * @param max Maximum quantifier for this predicate.
         */
        public NON_CONV_INTENT(String id, String expr, int min, int max) {
            this(id, new TERM(expr, min, max));
        }
    }

    // Default not-found callback.
    private final Supplier<NCQueryResult> notFound;

    // Added intents.
    private final List<Pair<INTENT, IntentCallback>> intents = new ArrayList<>();

    /**
     * Creates new default token solver. Default solver has default {@code null} name, no multi-match and
     * default not-found function that throws {@link NCRejection} exception. This is equivalent to:
     * <pre class="brush: java">
     *      this(null, false, null);
     * </pre>
     */
    public NCIntentSolver() {
        this(null, null);
    }

    /**
     * Creates new named token solver with given parameters and default not-found function. Default not-found
     * function throws {@link NCRejection} exception with generic error message.
     *
     * @param name Name of the solver (for informational purpose only).
     */
    public NCIntentSolver(String name) {
        this(name, null);
    }

    /**
     * Creates new named token solver with given parameters.
     *
     * @param name Name of the solver (for informational purpose only).
     * @param notFound Custom not-found function. This function will be called when no matching intent can
     *      be found for given sentence. If {@code null}, a default implementation will be used
     *      that throws {@link NCRejection} exception with generic error message.
     */
    public NCIntentSolver(String name, Supplier<NCQueryResult> notFound) {
        this.notFound = notFound;
    }

    /**
     * Adds given intent, its ID and its callback function to this solver.
     *
     * @param intent Intent to add.
     * @param fun A callback function that will be called when given intent is found to be the best match.
     * @return Returns this solver for call chaining.
     */
    public NCIntentSolver addIntent(INTENT intent, IntentCallback fun) {
        if (intent == null)
            throw new IllegalArgumentException("Intent cannot be null.");
        if (fun == null)
            throw new IllegalArgumentException("Intent callback cannot be null.");

        if (intents.stream().anyMatch(p -> p.getLeft().id.equals(intent.id)))
            throw new IllegalArgumentException("Intent with given ID has already been added.");

        intents.add(Pair.of(intent, fun));

        NCIntentSolverEngine.ackNewIntent(DFLT_NAME, intent);

        return this;
    }

    /**
     *
     * @param words
     * @return
     */
    private String mkHumanList(List<String> words) {
        String list = String.join(", ", words);

        int lastIdx = list.lastIndexOf(',');
        
        return list.indexOf(',') == lastIdx ?
            list.replace(", ", " and ") :  // "A, B" ⇒ "A and B"
            list.substring(0, lastIdx) + ", and" + list.substring(lastIdx); // "A, B, C" ⇒ "A, B, and C"
    }
    
    /**
     *
     * @param existedMeta
     * @param res
     * @return
     */
    private static Map<String, Object> cloneMetadata(Map<String, Object> existedMeta, NCIntentSolverResult res) {
        Map<String, Object> meta = new HashMap<>();
        
        meta.put("intentId", res.intentId());
        
        if (existedMeta != null)
            meta.putAll(existedMeta);
        
        return meta;
    }
    
    /**
     * Finds the best matching intent for given sentence and calls its callback function to get the final query result.
     * If no matching intent is found a not-found function will be called, if provided. If not-found function
     * wasn't provided, the {@link NCRejection} exception with default rejection message will be thrown.
     * <p>
     * The specific algorithm of selecting the best matching intent is relatively complex and can be challenging to
     * trace and understand. During development and testing phase it is highly recommended <i>to
     * use {@code NLPCRAFT_PROBE_VERBOSE} system property</i> to make sure that solver logs out all debugging
     * information.
     * 
     * @param ctx Sentence's context to find the best match for.
     * @return Query result.
     */
    public NCQueryResult solve(NCQueryContext ctx) throws NCRejection {
        if (intents.isEmpty())
            log.warn("Intent solver has no registered intents (ignoring).");

        List<NCIntentSolverResult> results;
        NCSentence sen = ctx.getSentence();

        try {
            results = NCIntentSolverEngine.solve(sen, ctx.getConversationContext().getTokens(), intents);
        }
        catch (Exception e) {
            // It should be shown to the model developer.
            log.error("Processing failed due to unexpected error.", e);
            
            throw new NCRejection("Processing failed due to unexpected error.", e);
        }

        if (results.isEmpty())
            return (notFound != null ? notFound : DFLT_NOT_FOUND).get();
    
        NCRejection errRej = null;
    
        for (NCIntentSolverResult res : results)
            try {
                NCQueryResult qryRes =
                    res.fn().apply(new NCIntentSolverContext() {
                        @Override public NCQueryContext getQueryContext() { return ctx; }
                        @Override public List<List<NCToken>> getIntentTokens() { return res.toks(); }
                        @Override public NCVariant getVariant() { return res.variant(); }
                        @Override public boolean isExactMatch() { return res.isExactMatch(); }
                        @Override public String getIntentId() { return res.intentId(); }
                    });
    
                // Don't override if user already set it.
                if (qryRes.getVariant() == null) {
                    if (res.variant() != null)
                        qryRes.setVariant(res.variant());
                    else if (sen.getVariants().size() == 1)
                        // If there's only one variant - use it implicitly.
                        qryRes.setVariant(sen.getVariants().get(0));
                }
                
                return qryRes;
            }
            catch (NCIntentSkip e) {
                // No-op - just skipping this result.
                String msg = e.getLocalizedMessage();

                if (msg != null)
                    log.trace("Selected intent skipped due to: " + msg);
            }
            catch (NCRejection e) {
                errRej = e;

                break;
            }
    
        if (errRej != null)
            throw errRej;
    
        log.trace("Not matching intents found.");

        return (notFound != null ? notFound : DFLT_NOT_FOUND).get();
    }
}
