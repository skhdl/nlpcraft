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
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.mdllib;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import static org.nlpcraft.mdllib.utils.NCTokenUtils.*;

/**
 * Sentence variant is a list of {@link NCToken tokens}. This class is used throughout the
 * API as a container of tokens coupled with their utility methods. Note that a given user input
 * can have one or more possible different parsing variants (i.e. set of tokens representing that parsed input).
 * Note also that variants support natural ordering from best to worst probable fitness.
 *
 * @see NCSentence#variants()
 */
public class NCVariant implements Serializable, Comparable<NCVariant> {
    private List<NCToken> toks;

    // Weight components.
    private int userToks = 0; // More is better.
    private int wordCnt = 0; // More is better.
    private float avgWordsPerTok = 0f; // More is better.
    private int totalSparsity = 0; // Less is better.
    private int totalUserDirect = 0; // More is better.

    /**
     * Calculates weight components.
     */
    private void calcWeight() {
        assert toks != null;

        wordCnt = 0;
        userToks = 0;
        totalSparsity = 0;
        totalUserDirect = 0;

        int tokCnt = 0;

        for (NCToken tok : toks)
            if (!isFreeWord(tok) && !isStopWord(tok)) {
                wordCnt += getWordLength(tok);
                totalSparsity += getSparsity(tok);

                if (tok.isUserDefined()) {
                    userToks++;

                    if (isDirectSynonym(tok))
                        totalUserDirect++;
                }

                tokCnt++;
            }

        avgWordsPerTok = wordCnt > 0 ? tokCnt / wordCnt : 0;
    }

    @Override
    public String toString() {
        return String.format("Variant [" +
                "userToks=%d, " +
                "wordCnt=%d, " +
                "totalUserDirect=%d, " +
                "avgWordsPerTok=%f, " +
                "sparsity=%d, " +
                "toks=%s" +
            "]",
            userToks,
            wordCnt,
            totalUserDirect,
            avgWordsPerTok,
            totalSparsity,
            toks
        );
    }

    /**
     * Creates new empty variant.
     */
    public NCVariant() {
        // No-op.
    }

    /**
     * Creates new variant with given tokens.
     *
     * @param toks Tokens for the variant.
     */
    public NCVariant(NCToken... toks) {
        this.toks = new ArrayList<>();

        this.toks.addAll(Arrays.asList(toks));

        calcWeight();
    }

    /**
     * Creates new variant with given tokens.
     *
     * @param toks Tokens for the variant.
     */
    public NCVariant(Iterable<NCToken> toks) {
        this.toks = new ArrayList<>();

        toks.forEach(this.toks::add);

        calcWeight();
    }

    @Override
    public int compareTo(NCVariant v) {
        if (userToks > v.userToks)
            return 1;
        else if (userToks < v.userToks)
            return -1;
        else if (wordCnt > v.wordCnt)
            return 1;
        else if (wordCnt < v.wordCnt)
            return -1;
        else if (totalUserDirect > v.totalUserDirect)
            return 1;
        else if (totalUserDirect < v.totalUserDirect)
            return -1;
        else if (avgWordsPerTok > v.avgWordsPerTok)
            return 1;
        else if (avgWordsPerTok < v.avgWordsPerTok)
            return -1;
        else
            return Integer.compare(v.totalSparsity, totalSparsity);
    }

    /**
     * Gets this variant tokens.
     *
     * @return List of tokens.
     */
    public List<NCToken> getTokens() {
        return toks;
    }

    /**
     * Sets (overrides) tokens in this variant.
     *
     * @param toks Tokens to set. Cannot be {@code null}.
     */
    public void setTokens(List<NCToken> toks) {
        assert toks != null;

        this.toks = toks;

        calcWeight();
    }

    /**
     * Clones this variants filtering out tokens. This is equivalent to:
     * <pre class="brush: java">
     *     return new NCVariant(filter(pred));
     * </pre>
     *
     * @param pred Fitter predicate. Only tokens satisfying this predicate will be copied to a new variant.
     * @return New variant.
     */
    public NCVariant filterClone(Predicate<NCToken> pred) {
        return new NCVariant(filter(pred));
    }

    /**
     * Gets filtered list of tokens for this variant. This is equivalent to:
     * <pre class="brush: java">
     *      return stream(pred).collect(Collectors.toList());
     * </pre>
     *
     * @param pred Filter predicate.
     * @return Filtered list of tokens
     */
    public List<NCToken> filter(Predicate<NCToken> pred) {
        return stream(pred).collect(Collectors.toList());
    }

    /**
     * Gets filtered stream of tokens for this variant. This is equivalent to:
     * <pre class="brush: java">
     *      return getTokens().stream().filter(pred);
     * </pre>
     *
     * @param pred Filter predicate.
     * @return Filtered stream.
     */
    public Stream<NCToken> stream(Predicate<NCToken> pred) {
        return getTokens().stream().filter(pred);
    }

    /**
     * Gets all neighbouring tokens to the left or right of the given pivot token (excluded) that
     * pass given predicate. Neighbouring tokens are returned in the same order they appear
     * in the sentence.
     *
     * @param idx Index of the pivot token (excluding).
     * @param left Whether to get left or right neighbours.
     * @param pred Predicate to pass.
     * @return Left or right neighbouring tokens list for a given token's index. List can be empty.
     */
    public List<NCToken> getNeighbours(int idx, boolean left, Predicate<NCToken> pred) {
        List<NCToken> sublist = left ? toks.subList(0, idx) : toks.subList(idx + 1, toks.size());

        return sublist.stream().filter(pred).collect(Collectors.toList());
    }

    /**
     * Gets token from this variant with given {@link NCToken#getId() ID}.
     *
     * @param id Token {@link NCToken#getId() ID}.
     * @return Token with given {@link NCToken#getId() ID}.
     */
    public Optional<NCToken> getById(String id) {
        return toks.stream().filter(t -> t.getId().equals(id)).findFirst();
    }
}
