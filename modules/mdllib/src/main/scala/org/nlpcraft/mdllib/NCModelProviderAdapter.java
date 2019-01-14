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
