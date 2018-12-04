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
    private String type;
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

    @Override
    public String getType() {
        return type == null ? "STRING" : type;
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
     * @param type
     */
    public void setType(String type) {
        this.type = type;
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

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return String.format("Model element [id=%s, group=%s, type=%s, parentId=%s]", id, group, type, parentId);
    }
}
