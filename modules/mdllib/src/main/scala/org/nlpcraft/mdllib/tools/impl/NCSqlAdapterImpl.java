/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
