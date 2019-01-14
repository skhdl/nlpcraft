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

package org.nlpcraft.mdllib.utils;

import org.nlpcraft.mdllib.tools.impl.NCSqlAdapterImpl;
import org.nlpcraft.mdllib.*;

import java.sql.*;
import java.util.*;
import java.util.function.*;

/**
 * Convenient API for {@link NCToken} {@link NCToken#getMetadata() metadata}. Most of the methods in
 * this class provide static, conveniently named and typed accessors to {@link NCToken#getMetadata() metadata}
 * properties. Note that some methods are valid only for specific types of {@link NCToken} - consult
 * each method's documentation for details. Note also that the methods from this class can be statically
 * imported into the scope for easier usage. 
 * <br><br>
 * The following tables shows which methods correspond to what token {@link NCToken#getId() ID}:
 * <table class="dl-table" summary="">
 * <tr>
 *      <th>All tokens</th>
 *      <th>ID <code>nlp:date</code></th>
 *      <th>ID <code>nlp:geo</code></th>
 * </tr>
 * <tr>
 *      <td>
 *          {@link #isGeo(NCToken)}<br>
 *          {@link #isDate(NCToken)}<br>
 *          {@link #isNumeric(NCToken)}<br>
 *          {@link #isFunction(NCToken)}<br>
 *          {@link #isNlp(NCToken)}<br>
 *          {@link #isContiguous(NCToken)}<br>
 *          {@link #isAdjective(NCToken)}<br>
 *          {@link #isAdverb(NCToken)}<br>
 *          {@link #isBracketed(NCToken)}<br>
 *          {@link #isEnglish(NCToken)}<br>
 *          {@link #isFreeWord(NCToken)}<br>
 *          {@link #isSwearWord(NCToken)}<br>
 *          {@link #isSynthetic(NCToken)}<br>
 *          {@link #isKnownWord(NCToken)}<br>
 *          {@link #isStopWord(NCToken)}<br>
 *          {@link #isQuoted(NCToken)}<br>
 *          {@link #isPreposition(NCToken)}<br>
 *          {@link #isNoun(NCToken)}<br>
 *          {@link #isPronoun(NCToken)}<br>
 *          {@link #getPosTag(NCToken)}<br>
 *          {@link #getNormalizedText(NCToken)}<br>
 *          {@link #getOriginalText(NCToken)}<br>
 *          {@link #getTokenIndex(NCToken)}<br>
 *          {@link #getCharLength(NCToken)}<br>
 *          {@link #getLemma(NCToken)}<br>
 *          {@link #getStem(NCToken)}<br>
 *          {@link #getSparsity(NCToken)}<br>
 *          {@link #getPosDescription(NCToken)}<br>
 *          {@link #getUnid(NCToken)}<br>
 *          {@link #getWordIndexes(NCToken)}<br>
 *          {@link #getWordLength(NCToken)}<br>
 *          {@link #isDirectSynonym(NCToken)}
 *      </td>
 *      <td>
 *          {@link #isDateAfter(NCToken, long)}<br>
 *          {@link #isDateBefore(NCToken, long)}<br>
 *          {@link #isDateIntersect(NCToken, long, long)}<br>
 *          {@link #isDateWithin(NCToken, long)}<br>
 *          {@link #getDateFrom(NCToken)}<br>
 *          {@link #getDateTo(NCToken)}<br>
 *          {@link #prepareDateSql(NCToken, String)}
 *      </td>
 *      <td>
 *          {@link #isGeoCity(NCToken)}<br>
 *          {@link #isGeoCountry(NCToken)}<br>
 *          {@link #isGeoContinent(NCToken)}<br>
 *          {@link #isGeoMetro(NCToken)}<br>
 *          {@link #isGeoRegion(NCToken)}<br>
 *          {@link #isGeoSubcontinent(NCToken)}<br>
 *          {@link #getGeoCity(NCToken)}<br>
 *          {@link #getGeoCountry(NCToken)}<br>
 *          {@link #getGeoContinent(NCToken)}<br>
 *          {@link #getGeoMetro(NCToken)}<br>
 *          {@link #getGeoRegion(NCToken)}<br>
 *          {@link #getGeoLatitude(NCToken)}<br>
 *          {@link #getGeoLongitude(NCToken)}<br>
 *          {@link #getGeoSubcontinent(NCToken)}
 *      </td>
 * </tr>
 * </table>
 * <table class="dl-table" summary="">
 * <tr>
 *      <th>ID <code>nlp:num</code></th>
 *      <th>ID <code>nlp:function</code></th>
 *      <th>ID <code>nlp:coordinate</code></th>
 * </tr>
 * <tr>
 *      <td>
 *          {@link #isNumEqualCondition(NCToken)}<br>
 *          {@link #isNumFractional(NCToken)}<br>
 *          {@link #isNumFromInclusive(NCToken)}<br>
 *          {@link #isNumFromNegativeInfCondition(NCToken)}<br>
 *          {@link #isNumNotEqualCondition(NCToken)}<br>
 *          {@link #isNumRangeCondition(NCToken)}<br>
 *          {@link #isNumSingleValue(NCToken)}<br>
 *          {@link #isNumToPositiveInfCondition(NCToken)}<br>
 *          {@link #isNumToInclusive(NCToken)}<br>
 *          {@link #testNum(NCToken, int)}<br>
 *          {@link #testNum(NCToken, byte)}<br>
 *          {@link #testNum(NCToken, short)}<br>
 *          {@link #testNum(NCToken, long)}<br>
 *          {@link #testNum(NCToken, short)}<br>
 *          {@link #testNum(NCToken, float, float)}<br>
 *          {@link #testNum(NCToken, double, double)}<br>
 *          {@link #getNumFrom(NCToken)}<br>
 *          {@link #getNumTo(NCToken)}<br>
 *          {@link #getNumIndex(NCToken)}<br>
 *          {@link #getNumUnit(NCToken)}<br>
 *          {@link #getNumUnitType(NCToken)}<br>
 *          {@link #prepareNumSql(NCToken, String)}
 *      </td>
 *      <td>
 *          {@link #isLimitFun(NCToken)}<br>
 *          {@link #isSortFun(NCToken)}<br>
 *          {@link #isMaxFun(NCToken)}<br>
 *          {@link #isMinFun(NCToken)}<br>
 *          {@link #isAvgFun(NCToken)}<br>
 *          {@link #isGroupFun(NCToken)}<br>
 *          {@link #isCompareFun(NCToken)}<br>
 *          {@link #isCorrelationFun(NCToken)}<br>
 *          {@link #isAscendingFun(NCToken)}<br>
 *          {@link #getFunIndexes(NCToken)}<br>
 *          {@link #getFunLimit(NCToken)}
 *      </td>
 *      <td>
 *          {@link #getCoordinateLatitude(NCToken)}<br>
 *          {@link #getCoordinateLongitude(NCToken)}
 *      </td>
 * </tr>
 * </table>
 */
public class NCTokenUtils {
    /**
     *
     * @param tok
     * @param id
     */
    static private void checkId(NCToken tok, String id) {
        if (!tok.getId().equals(id))
            throw new IllegalArgumentException(
                String.format("Token of the wrong type [expected=%s, actual=%s]", id, tok.getId()));
    }

    /**
     * Whether or not this token has {@code nlp:geo} {@link NCToken#getId() ID}.
     *
     * @param tok A token.
     * @return Whether or not this token has {@code nlp:geo} {@link NCToken#getId() ID}.
     */
    static public boolean isGeo(NCToken tok) {
        assert tok != null;

        return tok.getId().equals("nlp:geo");
    }

    /**
     * Whether or not this token has {@code nlp:nlp} {@link NCToken#getId() ID}.
     *
     * @param tok A token.
     * @return Whether or not this token has {@code nlp:nlp} {@link NCToken#getId() ID}.
     */
    static public boolean isNlp(NCToken tok) {
        assert tok != null;

        return tok.getId().equals("nlp:nlp");
    }

    /**
     * Whether or not this token has {@code nlp:date} {@link NCToken#getId() ID}.
     *
     * @param tok A token.
     * @return Whether or not this token has {@code nlp:date} {@link NCToken#getId() ID}.
     */
    static public boolean isDate(NCToken tok) {
        assert tok != null;

        return tok.getId().equals("nlp:date");
    }

    /**
     * Whether or not this token has {@code nlp:num} {@link NCToken#getId() ID}.
     *
     * @param tok A token.
     * @return Whether or not this token has {@code nlp:num} {@link NCToken#getId() ID}.
     */
    static public boolean isNumeric(NCToken tok) {
        assert tok != null;

        return tok.getId().equals("nlp:num");
    }

    /**
     * Whether or not this token has {@code nlp:function} {@link NCToken#getId() ID}.
     *
     * @param tok A token.
     * @return Whether or not this token has {@code nlp:function} {@link NCToken#getId() ID}.
     */
    static public boolean isFunction(NCToken tok) {
        assert tok != null;

        return tok.getId().equals("nlp:function");
    }

    /**
     * Prepares SQL WHERE clause adapter for JDBC prepared statement based on condition in given {@code nlp:num} token.
     *
     * @param tok A token.
     * @param col SQL column name.
     * @param <T> Type of parameters.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return SQL adapter for JDBC prepared statement.
     */
    static public <T extends Number> NCTokenSqlAdapter<T> prepareNumSql(NCToken tok, String col) {
        assert tok != null;

        checkId(tok, "nlp:num");
    
        boolean isFractional = isNumFractional(tok);
        
        Function<Double, T> get = (d) -> {
            Number n;
    
            if (isFractional)
                n = d;
            else {
                Long l = d.longValue();
        
                if (Integer.MAX_VALUE >= l && Integer.MIN_VALUE <= l)
                    n = l.intValue();
                else if (Short.MAX_VALUE >= l && Short.MIN_VALUE <= l)
                    n = l.shortValue();
                else if (Byte.MAX_VALUE >= l && Byte.MIN_VALUE <= l)
                    n = l.byteValue();
                else
                    n = l;
            }
    
            return (T)n;
        };
        
        Supplier<String> moreThanFrom = () -> isNumFromInclusive(tok) ? col + " >= ?" : col + " > ?";
        Supplier<String> lessThanTo = () -> isNumToInclusive(tok) ? col + " <= ?" : col + " < ?";
    
        if (isNumEqualCondition(tok))
            return new NCSqlAdapterImpl<>(col + " = ?", get.apply(getNumFrom(tok)));
        else if (isNumNotEqualCondition(tok))
            return new NCSqlAdapterImpl<>(col + " <> ?", get.apply(getNumFrom(tok)));
        else if (isNumFromNegativeInfCondition(tok))
            return new NCSqlAdapterImpl<>(lessThanTo.get(), get.apply(getNumFrom(tok)));
        else if (isNumToPositiveInfCondition(tok))
            return new NCSqlAdapterImpl<>(moreThanFrom.get(), get.apply(getNumFrom(tok)));
        else
            return new NCSqlAdapterImpl<>(
                moreThanFrom.get() + " AND " + lessThanTo.get(),
                get.apply(getNumFrom(tok)),
                get.apply(getNumTo(tok))
            );
    }

    /**
     * Compares given value against the numeric condition in given {@code nlp:num} token.
     *
     * @param tok A token.
     * @param delta Precision delta for comparison.
     * @param v Value to compare with.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return {@code True} if the value satisfies condition in given {@code nlp:num} token.
     */
    static public boolean testNum(NCToken tok, double v, double delta) {
        return testCondition(tok, v, delta);
    }

    /**
     * Compares given value against the numeric condition in given {@code nlp:num} token.
     *
     * @param tok A token.
     * @param delta Precision delta for comparison.
     * @param v Value to compare with.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return {@code True} if the value satisfies condition in given {@code nlp:num} token.
     */
    static public boolean testNum(NCToken tok, float v, float delta) {
        return testCondition(tok, v, delta);
    }

    /**
     * Compares given value against the numeric condition in given {@code nlp:num} token.
     *
     * @param tok A token.
     * @param v Value to compare with.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return {@code True} if the value satisfies condition in given {@code nlp:num} token.
     */
    static public boolean testNum(NCToken tok, long v) {
        return testCondition(tok, v);
    }

    /**
     * Compares given value against the numeric condition in given {@code nlp:num} token.
     *
     * @param tok A token.
     * @param v Value to compare with.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return {@code True} if the value satisfies condition in given {@code nlp:num} token.
     */
    static public boolean testNum(NCToken tok, int v) {
        return testCondition(tok, v);
    }

    /**
     * Compares given value against the numeric condition in given {@code nlp:num} token.
     *
     * @param tok A token.
     * @param v Value to compare with.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return {@code True} if the value satisfies condition in given {@code nlp:num} token.
     */
    static public boolean testNum(NCToken tok, byte v) {
        return testCondition(tok, v);
    }

    /**
     * Compares given value against the numeric condition in given {@code nlp:num} token.
     *
     * @param tok A token.
     * @param v Value to compare with.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return {@code True} if the value satisfies condition in given {@code nlp:num} token.
     */
    static public boolean testNum(NCToken tok, short v) {
        return testCondition(tok, v);
    }

    /**
     *
     * @param tok
     * @param v
     * @return
     */
    static private boolean testCondition(NCToken tok, long v) {
        assert tok != null;

        checkId(tok, "nlp:num");

        if (isNumEqualCondition(tok))
            return (long)getNumFrom(tok) == v;
        else if (isNumNotEqualCondition(tok))
            return (long)getNumFrom(tok) != v;

        return handleRange(tok,
            () -> isNumFromInclusive(tok) ? v >= getNumFrom(tok) : v > getNumFrom(tok),
            () -> isNumToInclusive(tok) ? v <= getNumTo(tok) : v < getNumTo(tok)
        );
    }

    /**
     *
     * @param tok
     * @param v
     * @param delta
     * @return
     */
    private static boolean testCondition(NCToken tok, double v, double delta) {
        assert tok != null;

        checkId(tok, "nlp:num");

        if (isNumEqualCondition(tok))
            return Math.abs(getNumFrom(tok) - v) <= delta;
        else if (isNumNotEqualCondition(tok))
            return Math.abs(getNumFrom(tok) - v) > delta;

        return handleRange(tok,
            () -> isNumFromInclusive(tok) ? v > getNumFrom(tok) || Math.abs(getNumFrom(tok) - v) <= delta : v > getNumFrom(tok),
            () -> isNumToInclusive(tok) ? v < getNumTo(tok) || Math.abs(getNumTo(tok) - v) <= delta : v < getNumTo(tok)
        );
    }

    static private boolean handleRange(NCToken tok, Supplier<Boolean> moreThanFrom, Supplier<Boolean> lessThanTo) {
        if (isNumFromNegativeInfCondition(tok))
            return lessThanTo.get();
        else if (isNumToPositiveInfCondition(tok))
            return moreThanFrom.get();
        else
            return moreThanFrom.get() && lessThanTo.get();
    }

    /**
     * Whether given {@code nlp:num} token represents a single numeric value vs. a numeric range.
     * If this method return {@code true} you can use either {@link #getNumTo(NCToken)} or {@link #getNumFrom(NCToken)}
     * methods (since both will return the same value). Note also that in this case {@link #isNumEqualCondition(NCToken)}
     * will return {@code true}.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Whether given {@code nlp:num} token represents a single numeric value vs. a numeric range.
     */
    static public boolean isNumSingleValue(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        NCMetadata meta = tok.getMetadata();

        return meta.getDouble("NUM_TO") == meta.getDouble("NUM_FROM") && meta.getBoolean("NUM_ISEQUALCONDITION");
    }

    /**
     * Whether given {@code nlp:num} token represents a equality condition.
     * <br><br>
     * Corresponds to {@code NUM_ISEQUALCONDITION} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Whether given {@code nlp:num} token represents a equality condition.
     */
    static public boolean isNumEqualCondition(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getBoolean("NUM_ISEQUALCONDITION");
    }

    /**
     * Whether given {@code nlp:num} token represents a not-equality condition.
     * <br><br>
     * Corresponds to {@code NUM_ISNOTEQUALCONDITION} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Whether given {@code nlp:num} token represents a not-equality condition.
     */
    static public boolean isNumNotEqualCondition(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getBoolean("NUM_ISNOTEQUALCONDITION");
    }

    /**
     * Whether given {@code nlp:num} token represents a range to negative infinity.
     * <br><br>
     * Corresponds to {@code NUM_ISFROMNEGATIVEINFINITY} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Whether given {@code nlp:num} token represents a range to negative infinity.
     */
    static public boolean isNumFromNegativeInfCondition(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getBoolean("NUM_ISFROMNEGATIVEINFINITY");
    }

    /**
     * Whether given {@code nlp:num} token represents a range to positive infinity.
     * <br><br>
     * Corresponds to {@code NUM_ISTOPOSITIVEINFINITY} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Whether given {@code nlp:num} token represents a range to positive infinity.
     */
    static public boolean isNumToPositiveInfCondition(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getBoolean("NUM_ISTOPOSITIVEINFINITY");
    }

    /**
     * Whether given {@code nlp:num} token represents a range condition.
     * <br><br>
     * Corresponds to {@code NUM_ISRANGECONDITION} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Whether or not end of the numeric range is inclusive for given {@code nlp:num} token.
     */
    static public boolean isNumRangeCondition(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getBoolean("NUM_ISRANGECONDITION");
    }

    /**
     * Whether or not end of the numeric range is inclusive for given {@code nlp:num} token.
     * <br><br>
     * Corresponds to {@code NUM_TOINCL} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Whether or not end of the numeric range is inclusive for given {@code nlp:num} token.
     */
    static public boolean isNumToInclusive(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getBoolean("NUM_TOINCL");
    }
    
    /**
     * Whether or not start of the numeric range is inclusive for given {@code nlp:num} token.
     * <br><br>
     * Corresponds to {@code NUM_FROMINCL} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Whether or not start of the numeric range is inclusive for given {@code nlp:num} token.
     */
    static public boolean isNumFromInclusive(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getBoolean("NUM_FROMINCL");
    }

    /**
     * Gets the start of the numeric range that satisfies the condition of given {@code nlp:num} token.
     * Note that this method and {@link #getNumTo(NCToken)} can return the same value in
     * which case given {@code nlp:num} token represents a single value and {@link #isNumEqualCondition(NCToken)}
     * will return {@code true}.
     * <br><br>
     * Corresponds to {@code NUM_FROM} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Start of the numeric range that satisfies the condition of given {@code nlp:num} token.
     */
    static public double getNumFrom(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getDouble("NUM_FROM");
    }

    /**
     * Gets the end of numeric range that satisfies the condition of given {@code nlp:num} token.
     * Note that {@link #getNumFrom(NCToken)} and this method can return the same value in
     * which case given {@code nlp:num} token represents a single value and {@link #isNumEqualCondition(NCToken)}
     * will return {@code true}.
     * <br><br>
     * Corresponds to {@code NUM_TO} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return End of numeric range that satisfies the condition of given {@code nlp:num} token.
     */
    static public double getNumTo(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getDouble("NUM_TO");
    }

    /**
     * Whether this token's value (single numeric value of a range) is a whole or a fractional number
     * for given {@code nlp:num} token.
     * <br><br>
     * Corresponds to {@code NUM_ISFRACTIONAL} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Whether this {@code nlp:num} token's value is a whole or a fractional number.
     */
    static public boolean isNumFractional(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getBoolean("NUM_ISFRACTIONAL");
    }

    /**
     * Gets optional unit for this {@code nlp:num} token, e.g. "mm", "cm", "ft".
     * <br><br>
     * Corresponds to {@code NUM_UNIT} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Optional unit for this {@code nlp:num} token.
     */
    static public String getNumUnit(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getString("NUM_UNIT");
    }

    /**
     * Gets optional unit type for this {@code nlp:num} token, e.g. "length", "force", "mass".
     * <br><br>
     * Corresponds to {@code NUM_UNITTYPE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Optional unit type for this {@code nlp:num} token.
     */
    static public String getNumUnitType(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return tok.getMetadata().getString("NUM_UNITTYPE");
    }

    /**
     * Gets optional index of another token in the sentence that this {@code nlp:num} token is referring to.
     * If index could not be determined this token refers to a free word or a stopword.
     * <br><br>
     * Corresponds to {@code NUM_INDEX} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:num} ID.
     * @return Index of reference token for this {@code nlp:num} token.
     */
    static public Optional<Integer> getNumIndex(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:num");

        return Optional.ofNullable((Integer)tok.getMetadata().get("NUM_INDEX"));
    }

    /**
     * Gets start timestamp of the date range {@code nlp:date} token.
     * <br><br>
     * Corresponds to {@code DATE_FROM} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:date} ID.
     * @return Start timestamp of the date range {@code nlp:date} token.
     */
    static public long getDateFrom(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:date");

        return tok.getMetadata().getLong("DATE_FROM");
    }

    /**
     * Gets end timestamp of the date range {@code nlp:date} token.
     * <br><br>
     * Corresponds to {@code DATE_TO} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:date} ID.
     * @return End timestamp of the date range {@code nlp:date} token.
     */
    static public long getDateTo(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:date");

        return tok.getMetadata().getLong("DATE_TO");
    }

    /**
     * Tests if given timestamp is before the date range for given {@code nlp:date} token.
     *
     * @param tok A token.
     * @param tstamp Timestamp to test.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:date} ID.
     * @return {@code True} if given timestamp is before the date range for given token, {@code false} otherwise.
     */
    static public boolean isDateBefore(NCToken tok, long tstamp) {
        assert tok != null;

        checkId(tok, "nlp:date");

        return tstamp < getDateFrom(tok);
    }

    /**
     * Tests if given timestamp is after the date range for given {@code nlp:date} token.
     *
     * @param tok A token.
     * @param tstamp Timestamp to test.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:date} ID.
     * @return {@code True} if given timestamp is after the date range for given token, {@code false} otherwise.
     */
    static public boolean isDateAfter(NCToken tok, long tstamp) {
        assert tok != null;

        checkId(tok, "nlp:date");

        return tstamp > getDateTo(tok);
    }

    /**
     * Tests if given timestamp is within (inclusively) the date range for given {@code nlp:date} token.
     *
     * @param tok A token.
     * @param tstamp Timestamp to test.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:date} ID.
     * @return {@code True} if given timestamp is within the date range for given token, {@code false} otherwise.
     */
    static public boolean isDateWithin(NCToken tok, long tstamp) {
        assert tok != null;

        checkId(tok, "nlp:date");

        return tstamp >= getDateFrom(tok) && tstamp < getDateTo(tok);
    }

    /**
     * Tests if given from and to timestamps intersect (inclusively) with the date range for
     * given {@code nlp:date} token.
     *
     * @param tok A token.
     * @param from From timestamp to check.
     * @param to To timestamp to check.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:date} ID.
     * @return {@code True} if given timestamp intersects the date range for given token, {@code false} otherwise.
     */
    static public boolean isDateIntersect(NCToken tok, long from, long to) {
        assert tok != null;

        checkId(tok, "nlp:date");

        return from < getDateTo(tok) && to >= getDateFrom(tok);
    }

    /**
     * Creates SQL WHERE clause adapter for given token and SQL column name that can be conveniently
     * used in JDBC prepared statements.
     * 
     * @param tok A token.
     * @param col Column name.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:date} ID.
     * @return SQL adapter.
     */
    static public NCTokenSqlAdapter<Timestamp> prepareDateSql(NCToken tok, String col) {
        assert tok != null;

        checkId(tok, "nlp:date");

        String clause = String.format("%s BETWEEN ? AND ?", col);
        List<Timestamp> params = Arrays.asList(new Timestamp(getDateFrom(tok)), new Timestamp(getDateTo(tok)));

        return new NCTokenSqlAdapter<Timestamp>() {
            @Override public String getClause() { return clause; }
            @Override public List<Timestamp> getClauseParameters() { return params; }
        };
    }

    /**
     * Gets subcontinent for given {@code nlp:geo} token.
     * <br><br>
     * Corresponds to {@code GEO_SUBCONTINENT} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return Subcontinent.
     */
    static public String  getGeoSubcontinent(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_SUBCONTINENT");
    }

    /**
     * Gets continent for given {@code nlp:geo} token.
     * <br><br>
     * Corresponds to {@code GEO_CONTINENT} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return Continent.
     */
    static public String  getGeoContinent(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_CONTINENT");
    }

    /**
     * Gets country for given {@code nlp:geo} token.
     * <br><br>
     * Corresponds to {@code GEO_COUNTRY} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return Country.
     */
    static public String  getGeoCountry(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_COUNTRY");
    }

    /**
     * Gets metro area code for given {@code nlp:geo} token.
     * <br><br>
     * Corresponds to {@code GEO_METRO} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return Metro area code.
     */
    static public String  getGeoMetro(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_METRO");
    }

    /**
     * Gets optional latitude of the given {@code nlp:geo} token.
     * <br><br>
     * Corresponds to {@code GEO_LATITUDE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return Optional latitude of the given {@code nlp:geo} token.
     */
    static public Optional<Double> getGeoLatitude(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return Optional.ofNullable((Double)tok.getMetadata().get("GEO_LATITUDE"));
    }

    /**
     * Gets optional longitude of the given {@code nlp:geo} token.
     * <br><br>
     * Corresponds to {@code GEO_LONGITUDE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return Optional longitude of the given {@code nlp:geo} token.
     */
    static public Optional<Double> getGeoLongitude(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return Optional.ofNullable((Double)tok.getMetadata().get("GEO_LONGITUDE"));
    }

    /**
     * Gets region for given {@code nlp:geo} token.
     * <br><br>
     * Corresponds to {@code GEO_REGION} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return Region.
     */
    static public String  getGeoRegion(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_REGION");
    }

    /**
     * Gets city for given {@code nlp:geo} token.
     * <br><br>
     * Corresponds to {@code GEO_CITY} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return City name.
     */
    static public String  getGeoCity(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_CITY");
    }

    /**
     * Tests if given {@code nlp:geo} token represents a {@code subcontinent}.
     * <br><br>
     * Corresponds to {@code GEO_KIND} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return {@code True} if given token represents a {@code subcontinent}.
     *
     */
    static public boolean isGeoSubcontinent(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_KIND").equals("SUBCONTINENT");
    }

    /**
     * Tests if given {@code nlp:geo} token represents a {@code continent}.
     * <br><br>
     * Corresponds to {@code GEO_KIND} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return {@code True} if given token represents a {@code continent}.
     *
     */
    static public boolean isGeoContinent(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_KIND").equals("CONTINENT");
    }

    /**
     * Tests if given {@code nlp:geo} token represents a {@code country}.
     * <br><br>
     * Corresponds to {@code GEO_KIND} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return {@code True} if given token represents a {@code country}.
     *
     */
    static public boolean isGeoCountry(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_KIND").equals("COUNTRY");
    }

    /**
     * Tests if given {@code nlp:geo} token represents a {@code city}.
     * <br><br>
     * Corresponds to {@code GEO_KIND} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return {@code True} if given token represents a {@code city}.
     *
     */
    static public boolean isGeoCity(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_KIND").equals("CITY");
    }

    /**
     * Tests if given {@code nlp:geo} token represents a {@code metro}.
     * <br><br>
     * Corresponds to {@code GEO_KIND} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return {@code True} if given token represents a {@code metro}.
     *
     */
    static public boolean isGeoMetro(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_KIND").equals("METRO");
    }

    /**
     * Tests if given {@code nlp:geo} token represents a {@code region}.
     * <br><br>
     * Corresponds to {@code GEO_KIND} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:geo} ID.
     * @return {@code True} if given token represents a {@code region}.
     *
     */
    static public boolean isGeoRegion(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:geo");

        return tok.getMetadata().getString("GEO_KIND").equals("REGION");
    }

    /**
     * Tests if given {@code nlp:function} token represents {@code SUM} function of some elements.
     * <br><br>
     * Corresponds to {@code FUNCTION_TYPE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID.
     * @return {@code True} if given token represents {@code SUM} function of some elements.
     */
    static public boolean isSumFun(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        return tok.getMetadata().getString("FUNCTION_TYPE").equals("SUM");
    }

    /**
     * Tests if given {@code nlp:function} token represents {@code MAX} function of some elements.
     * <br><br>
     * Corresponds to {@code FUNCTION_TYPE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID.
     * @return {@code True} if given token represents {@code MAX} function of some elements.
     */
    static public boolean isMaxFun(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        return tok.getMetadata().getString("FUNCTION_TYPE").equals("MAX");
    }

    /**
     * Tests if given {@code nlp:function} token represents {@code MIN} function of some elements.
     * <br><br>
     * Corresponds to {@code FUNCTION_TYPE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID.
     * @return {@code True} if given token represents {@code MIN} function of some elements.
     */
    static public boolean isMinFun(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        return tok.getMetadata().getString("FUNCTION_TYPE").equals("MIN");
    }

    /**
     * Tests if given {@code nlp:function} token represents {@code AVG} function of some elements.
     * <br><br>
     * Corresponds to {@code FUNCTION_TYPE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID.
     * @return {@code True} if given token represents {@code AVG} function of some elements.
     */
    static public boolean isAvgFun(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        return tok.getMetadata().getString("FUNCTION_TYPE").equals("AVG");
    }

    /**
     * Tests if given {@code nlp:function} token represents {@code SORT} function of some elements.
     * <br><br>
     * Corresponds to {@code FUNCTION_TYPE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID.
     * @return {@code True} if given token represents {@code SORT} function of some elements.
     */
    static public boolean isSortFun(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        return tok.getMetadata().getString("FUNCTION_TYPE").equals("SORT");
    }

    /**
     * Tests if given {@code nlp:function} token represents {@code LIMIT} function of some elements.
     * <br><br>
     * Corresponds to {@code FUNCTION_TYPE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID.
     * @return {@code True} if given token represents {@code LIMIT} function of some elements.
     */
    static public boolean isLimitFun(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        return tok.getMetadata().getString("FUNCTION_TYPE").equals("LIMIT");
    }

    /**
     * Tests if given {@code nlp:function} token represents {@code GROUP} function of some elements.
     * <br><br>
     * Corresponds to {@code FUNCTION_TYPE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID.
     * @return {@code True} if given token represents {@code GROUP} function of some elements.
     */
    static public boolean isGroupFun(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        return tok.getMetadata().getString("FUNCTION_TYPE").equals("GROUP");
    }

    /**
     * Tests if given {@code nlp:function} token represents {@code CORRELATION} function of some elements.
     * <br><br>
     * Corresponds to {@code FUNCTION_TYPE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID.
     * @return {@code True} if given token represents {@code CORRELATION} function of some elements.
     */
    static public boolean isCorrelationFun(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        return tok.getMetadata().getString("FUNCTION_TYPE").equals("CORRELATION");
    }

    /**
     * Tests if given {@code nlp:function} token represents {@code COMPARE} function of some elements.
     * <br><br>
     * Corresponds to {@code FUNCTION_TYPE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID.
     * @return {@code True} if given token represents {@code COMPARE} function of some elements.
     */
    static public boolean isCompareFun(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        return tok.getMetadata().getString("FUNCTION_TYPE").equals("COMPARE");
    }

    /**
     * Gets limit value if given {@code nlp:function} token represents {@code LIMIT} function.
     * <br><br>
     * Corresponds to {@code FUNCTION_LIMIT} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID or
     *      it isn't of {@code LIMIT} type.
     * @return limit value if given {@code nlp:function} token represents {@code LIMIT} function.
     * @see #isLimitFun(NCToken)
     */
    static public double getFunLimit(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        if (!isLimitFun(tok))
            throw new IllegalArgumentException("'nlp:function' token is not of 'LIMIT' type: " + tok);

        return tok.getMetadata().getDouble("FUNCTION_LIMIT");
    }

    /**
     * Optional value of whether this limit or sort {@code nlp:function} token is ascending or descending.
     * <br><br>
     * Corresponds to {@code FUNCTION_ASC} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID or
     *      it isn't of {@code LIMIT} or {@code SORT} type.
     * @return Limit or sort function direction flag.
     */
    static public Optional<Boolean> isAscendingFun(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        if (!isLimitFun(tok) && !isSortFun(tok))
            throw new IllegalArgumentException("'nlp:function' token is not of 'LIMIT' or 'SORT' type: " + tok);

        return Optional.ofNullable((Boolean)tok.getMetadata().get("FUNCTION_ASC"));
    }

    /**
     * Gets indexes of the element(s) given @code nlp:function} token is referencing. Returns potentially empty list.
     * <br><br>
     * Corresponds to {@code FUNCTION_INDEXES} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:function} ID.
     * @return Indexes of the element(s) given token is referencing.
     */
    static public List<Integer> getFunIndexes(NCToken tok) {
        assert tok != null;

        checkId(tok, "nlp:function");

        return (List<Integer>)tok.getMetadata().get("FUNCTION_INDEXES");
    }
    
    /**
     * Tests whether or not given token represents a free word. A free word is a token that was detected
     * neither as a user defined token nor as one of the semantic system tokens, i.e. it has
     * token ID {@code nlp:nlp}.
     * <br><br>
     * Corresponds to {@code NLP_FREEWORD} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Free word flag.
     */
    static public boolean isFreeWord(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getBoolean("NLP_FREEWORD");
    }

    /**
     * Gets internal globally unique system ID of the given token.
     * <br><br>
     * Corresponds to {@code NLP_UNID} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Globally unique system ID of the token.
     */
    static public String getUnid(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getString("NLP_UNID");
    }

    /**
     * Tests whether or not the given token is a stopword. Stopwords are some extremely common words which
     * add little value in helping understanding user input and are excluded from the processing
     * entirely. For example, words like {@code a, the, can, of, about, over}, etc. are typical
     * stopwords in English. NlpCraft has built-in set of stopwords while user models can
     * specify additional stopwords.
     * <br><br>
     * Corresponds to {@code NLP_STOPWORD} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Stopword flag.
     */
    static public boolean isStopWord(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getBoolean("NLP_STOPWORD");
    }

    /**
     * Tests whether or not the given token is a swear word. NlpCraft has built-in list of common English swear words.
     * <br><br>
     * Corresponds to {@code NLP_SWEAR} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Swear word flag.
     */
    static public boolean isSwearWord(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getBoolean("NLP_SWEAR");
    }

    /**
     * Gets numeric value of how sparse the given token is. Sparsity zero means that all individual words in the token
     * follow each other.
     * <br><br>
     * Corresponds to {@code NLP_SPARSITY} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Sparsity value of this token.
     */
    static public int getSparsity(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getInteger("NLP_SPARSITY");
    }

    /**
     * Gets index of the first word in the given token. Note that token may not be contiguous.
     * <br><br>
     * Corresponds to {@code NLP_MININDEX} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Index of the first word in this token.
     */
    static public int getMinIndex(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getInteger("NLP_MININDEX");
    }

    /**
     * Whether or not this token was matched on direct (nor permutated) synonym.
     * <br><br>
     * Corresponds to {@code NLP_DIRECT} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Whether or not this token was matched on direct (nor permutated) synonym.
     */
    static public boolean isDirectSynonym(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getBoolean("NLP_DIRECT");
    }

    /**
     * Gets index of the last word in the given token. Note that token may not be contiguous.
     * <br><br>
     * Corresponds to {@code NLP_MAXINDEX} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Index of the last word in this token.
     */
    static public int getMaxIndex(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getInteger("NLP_MAXINDEX");
    }

    /**
     * Gets number of individual words in the given token.
     * <br><br>
     * Corresponds to {@code NLP_WORDLENGTH} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Number of individual words in this token.
     */
    static public int getWordLength(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getInteger("NLP_WORDLENGTH");
    }

    /**
     * Tests whether or not the given token has zero sparsity.
     * <br><br>
     * Corresponds to {@code NLP_CONTIGUOUS} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Contiguous flag.
     */
    static public boolean isContiguous(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getBoolean("NLP_CONTIGUOUS");
    }

    /**
     * Tests whether the given token represents an English word. Note that this only checks that token's text
     * consists of characters of English alphabet, i.e. the text doesn't have to be necessary
     * a known valid English word.
     * <br><br>
     * Corresponds to {@code NLP_ENGLISH} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Whether this token represents an English word.
     */
    static public boolean isEnglish(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getBoolean("NLP_ENGLISH");
    }

    /**
     * Gets list of word indexes in the given token. Always has at least one element in it.
     * <br><br>
     * Corresponds to {@code NLP_WORDINDEXES} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return List of word indexes in this token.
     */
    static public List<Integer> getWordIndexes(NCToken tok) {
        assert tok != null;

        return (List<Integer>)tok.getMetadata().get("NLP_WORDINDEXES");
    }

    /**
     * Tests whether given token's POS tag is a synthetic one (for multiword token).
     *
     * @param tok A token.
     * @return {@code True} if POS tag is a synthetic one.
     */
    static public boolean isSynthetic(NCToken tok) {
        return getPosTag(tok).equals("---");
    }

    /**
     * Tests whether POS tag for given token is one of {@code NN}, {@code NNS}, {@code NNP}, or {@code NNPS}.
     *
     * @param tok A token.
     * @return {@code True} if POS tag is one of {@code NN}, {@code NNS}, {@code NNP}, or {@code NNPS}.
     */
    static public boolean isNoun(NCToken tok) {
        String pos = getPosTag(tok);

        return
            pos.equals("NN") ||
            pos.equals("NNS") ||
            pos.equals("NNP") ||
            pos.equals("NNPS");
    }

    /**
     * Tests whether POS tag for given token is one of {@code PRP} or {@code PRP$}.
     *
     * @param tok A token.
     * @return {@code True} if POS tag is one of {@code PRP} or {@code PRP$}.
     */
    static public boolean isPronoun(NCToken tok) {
        String pos = getPosTag(tok);

        return
            pos.equals("PRP") ||
            pos.equals("PRP$");
    }

    /**
     * Tests whether POS tag for given token is one of {@code JJ}, {@code JJR}, or {@code JJS}.
     *
     * @param tok A token.
     * @return {@code True} if POS tag is one of {@code JJ}, {@code JJR}, or {@code JJS}.
     */
    static public boolean isAdjective(NCToken tok) {
        String pos = getPosTag(tok);

        return
            pos.equals("JJ") ||
            pos.equals("JJR") ||
            pos.equals("JJS");
    }

    /**
     * Tests whether POS tag for given token is one of {@code VB}, {@code VBD}, {@code VBG},
     * {@code VBN}, {@code VBP}, or {@code VBZ}.
     *
     * @param tok A token.
     * @return {@code True} if POS tag is one of {@code VB}, {@code VBD}, {@code VBG},
     *      {@code VBN}, {@code VBP}, or {@code VBZ}.
     */
    static public boolean isVerb(NCToken tok) {
        String pos = getPosTag(tok);

        return
            pos.equals("VB") ||
            pos.equals("VBD") ||
            pos.equals("VBG") ||
            pos.equals("VBN") ||
            pos.equals("VBP") ||
            pos.equals("VBZ");
    }

    /**
     * Tests whether POS tag for given token is one of {@code RB}, {@code RBR}, {@code RBS} or {@code WRB}.
     *
     * @param tok A token.
     * @return {@code True} if POS tag is one of {@code RB}, {@code RBR}, {@code RBS} or {@code WRB}.
     */
    static public boolean isAdverb(NCToken tok) {
        String pos = getPosTag(tok);

        return
            pos.equals("RB") ||
            pos.equals("RBR") ||
            pos.equals("RBS") ||
            pos.equals("WRB");
    }

    /**
     * Tests whether POS tag for given token is {@code IN}.
     *
     * @param tok A token.
     * @return {@code True} if POS tag is {@code IN}.
     */
    static public boolean isPreposition(NCToken tok) {
        String pos = getPosTag(tok);

        return
            pos.equals("IN");
    }

    /**
     * Tests whether POS tag for given token is one of {@code DT}, {@code PDT}, or {@code WDT}.
     *
     * @param tok A token.
     * @return {@code True} if POS tag is one of {@code DT}, {@code PDT}, or {@code WDT}.
     */
    static public boolean isDeterminer(NCToken tok) {
        String pos = getPosTag(tok);

        return
            pos.equals("DT") ||
            pos.equals("PDT") ||
            pos.equals("WDT");
    }

    /**
     * Tests whether or not this token is surrounded by any of {@code '[', ']', '{', '}', '(', ')'} brackets.
     * <br><br>
     * Corresponds to {@code NLP_BRACKETED} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Bracketing flag.
     */
    static public boolean isBracketed(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getBoolean("NLP_BRACKETED");
    }

    /**
     * Gets description of Penn Treebank POS tag.
     * Learn more at <a href="http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html</a>
     * <br><br>
     * Corresponds to {@code NLP_POSDESC} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Description of Penn Treebank POS tag.
     */
    static public String getPosDescription(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getString("NLP_POSDESC");
    }

    /**
     * Tests whether or not this token is found in Princeton WordNet database.
     * <br><br>
     * Corresponds to {@code NLP_DICT} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Princeton WordNet database inclusion flag.
     */
    static public boolean isKnownWord(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getBoolean("NLP_DICT");
    }

    /**
     * Gets index of the given token in the sentence.
     * <br><br>
     * Corresponds to {@code NLP_INDEX} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Index of the given token in the sentence.
     */
    static public int getTokenIndex(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getInteger("NLP_INDEX");
    }

    /**
     * Gets lemma of the given token. Lemma is a canonical form of this token. Note that stemming and lemmatization
     * allow to reduce inflectional forms and sometimes derivationally related forms of a word to a
     * common base form. Lemmatization refers to the use of a vocabulary and morphological analysis
     * of words, normally aiming to remove inflectional endings only and to return the base or dictionary
     * form of a word, which is known as the lemma. Learn
     * more at <a href="https://nlp.stanford.edu/IR-book/html/htmledition/stemming-and-lemmatization-1.html">https://nlp.stanford.edu/IR-book/html/htmledition/stemming-and-lemmatization-1.html</a>
     * <br><br>
     * Corresponds to {@code NLP_LEMMA} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Given token lemma.
     */
    static public String getLemma(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getString("NLP_LEMMA");
    }

    /**
     * Gets stem of the given token. Note that stemming and lemmatization allow to reduce inflectional forms
     * and sometimes derivationally related forms of a word to a common base form. Unlike lemma,
     * stemming is a basic heuristic process that chops off the ends of words in the hope of achieving
     * this goal correctly most of the time, and often includes the removal of derivational affixes.
     * Learn more at <a href="https://nlp.stanford.edu/IR-book/html/htmledition/stemming-and-lemmatization-1.html">https://nlp.stanford.edu/IR-book/html/htmledition/stemming-and-lemmatization-1.html</a>
     * <br><br>
     * Corresponds to {@code NLP_STEM} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Given token stem.
     */
    static public String getStem(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getString("NLP_STEM");
    }

    /**
     * Gets character length of the given token.
     * <br><br>
     * Corresponds to {@code NLP_CHARLENGTH} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Character length of the given token.
     */
    int getCharLength(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getInteger("NLP_CHARLENGTH");
    }

    /**
     * Gets original user input text for given token.
     * <br><br>
     * Corresponds to {@code NLP_ORIGTEXT} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Original token text.
     */
    static public String getOriginalText(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getString("NLP_ORIGTEXT");
    }

    /**
     * Gets normalized user input text for given token.
     * <br><br>
     * Corresponds to {@code NLP_NORMTEXT} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Normalized token text.
     */
    static public String getNormalizedText(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getString("NLP_NORMTEXT");
    }

    /**
     * Gets Penn Treebank POS tag for given token. Note that additionally to standard Penn Treebank POS
     * tags NlpCraft introduced {@code '---'} synthetic tag to indicate a POS tag for multiword tokens.
     * Learn more at <a href="http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html</a>
     * <br><br>
     * Corresponds to {@code NLP_POS} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Penn Treebank POS tag for given token.
     */
    static public String getPosTag(NCToken tok) {
        assert tok != null;
        
        return tok.getMetadata().getString("NLP_POS");
    }

    /**
     * Tests whether or not given token is surrounded by single or double quotes.
     * <br><br>
     * Corresponds to {@code NLP_QUOTED} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @return Quoted flag.
     */
    static public boolean isQuoted(NCToken tok) {
        assert tok != null;

        return tok.getMetadata().getBoolean("NLP_QUOTED");
    }

    /**
     * Gets coordinate latitude for this {@code nlp:coordinate} token.
     * <br><br>
     * Corresponds to {@code COORDINATE_LATITUDE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:coordinate} ID.
     * @return Coordinate latitude for this {@code nlp:coordinate} token.
     */
    static public Double getCoordinateLatitude(NCToken tok) {
        assert tok != null;
        
        checkId(tok, "nlp:coordinate");
        
        return tok.getMetadata().getDouble("COORDINATE_LATITUDE");
    }

    /**
     * Gets coordinate longitude for this {@code nlp:coordinate} token.
     * <br><br>
     * Corresponds to {@code COORDINATE_LONGITUDE} token {@link NCToken#getMetadata() metadata} property.
     *
     * @param tok A token.
     * @throws IllegalArgumentException Thrown if given token doesn't have {@code nlp:coordinate} ID.
     * @return Coordinate longitude for this {@code nlp:coordinate} token.
     */
    static public Double getCoordinateLongitude(NCToken tok) {
        assert tok != null;
        
        checkId(tok, "nlp:coordinate");
        
        return tok.getMetadata().getDouble("COORDINATE_LONGITUDE");
    }
}
