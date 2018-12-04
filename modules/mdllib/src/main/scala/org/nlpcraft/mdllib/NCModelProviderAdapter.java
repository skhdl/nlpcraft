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

package org.nlpcraft.mdllib;

import java.util.*;

/**
 * Convenient model provider adapter. This provider takes one or more pre-created models and will
 * return them from {@link #makeModel(String)} method.
 */
public class NCModelProviderAdapter implements NCModelProvider {
    private List<String> ids = new ArrayList<>();
    private List<NCModel> mdls = new ArrayList<>();
    private List<NCModelDescriptor> dss = new ArrayList<>();

    /**
     * Sets up this provider with given model instances.
     *
     * @param models Model instances.
     */
    public void setup(NCModel... models) {
        assert models != null;

        setup(Arrays.asList(models));
    }

    /**
     * Sets up this provider with given model instances. This method can be called
     * multiple times to initialize the provider with different models.
     *
     * @param models Model instances.
     */
    public void setup(Collection<NCModel> models) {
        assert models != null;

        ids.clear();
        mdls.clear();
        dss.clear();

        for (NCModel mdl : models) {
            NCModelDescriptor ds = mdl.getDescriptor();

            mdls.add(mdl);
            dss.add(ds);
            ids.add(ds.getId());
        }
    }

    @Override
    public NCModel makeModel(String id) {
        int idx = ids.indexOf(id);

        return idx == -1 ? null : mdls.get(idx);
    }

    @Override
    public List<NCModelDescriptor> getDescriptors() {
        return dss;
    }
}
