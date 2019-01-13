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

package org.nlpcraft.mdllib.tools.builder.impl;

import org.nlpcraft.mdllib.*;
import org.nlpcraft.mdllib.tools.impl.*;
import java.io.*;
import java.util.*;

/**
 * Default model element implementation.
 */
public class NCElementImpl implements NCElement {
    private String id;
    private String group;
    private String desc;
    private String parentId;
    private List<String> syns = new ArrayList<>();
    private List<String> exclSyns = new ArrayList<>();
    private List<NCValue> values = new ArrayList<>();
    private NCMetadata meta = new NCMetadataImpl();

    @Override
    public List<NCValue> getValues() {
        return values;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    /**
     *
     * @param desc
     */
    public void setDescription(String desc) {
        this.desc = desc;
    }

    /**
     *
     * @return
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * 
     * @param name
     * @param val
     */
    public void addMetadata(String name, Serializable val) {
        assert name != null;
        assert val != null;

        meta.put(name, val);
    }

    /**
     *
     * @return
     */
    @Override
    public NCMetadata getMetadata() {
        return meta;
    }

    /**
     *
     * @param values
     */
    public void setValues(List<NCValue> values) {
        this.values = values;
    }

    /**
     * 
     * @param name
     * @param syns
     */
    public void addValue(String name, Collection<String> syns) {
        assert name != null;
        assert syns != null;

        values.add(new NCValueImpl(name, new ArrayList<>(syns)));
    }

    /**
     *
     * @return
     */
    @Override
    public String getGroup() {
        return group;
    }

    /**
     *
     * @param group
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     *
     * @param id
     */
    public void setId(String id) {
        assert id != null;

        this.id = id;
    }

    /**
     *
     * @param parentId
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public List<String> getSynonyms() {
        return syns;
    }

    /**
     *
     * @param syn
     */
    public void addSynonym(String syn) {
        assert syn != null;

        syns.add(syn);
    }
    
    /**
     *
     * @return
     */
    @Override
    public List<String> getExcludedSynonyms() {
        return exclSyns;
    }

    /**
     *
     * @param syn
     */
    public void addExcludedSynonym(String syn) {
        assert syn != null;

        exclSyns.add(syn);
    }
}
