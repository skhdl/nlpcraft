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

package org.nlpcraft.mdllib.tools.impl;

import org.nlpcraft.mdllib.utils.NCTokenSqlAdapter;

import java.util.*;

/**
 *
 * @param <T>
 */
public class NCSqlAdapterImpl<T> implements NCTokenSqlAdapter<T> {
    private final String clause;
    private final List<T> params;

    /**
     *
     * @param clause
     * @param param
     */
    public NCSqlAdapterImpl(String clause, T param) {
        this.clause = clause;
        this.params = Collections.singletonList(param);
    }

    /**
     *
     * @param clause
     * @param param1
     * @param param2
     */
    public NCSqlAdapterImpl(String clause, T param1, T param2) {
        this.clause = clause;
        this.params = Arrays.asList(param1, param2);
    }

    @Override public String getClause() {
        return clause;
    }
    
    @Override public List<T> getClauseParameters() {
        return params;
    }
}
