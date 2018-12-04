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

package org.nlpcraft.mdllib.tools.builder;

import org.nlpcraft.mdllib.*;
import org.nlpcraft.mdllib.tools.builder.impl.*;
import java.io.*;
import java.util.*;

/**
 * Convenient model element builder.
 * <br><br>
 * To use this builder start by invoking one of the following static method to create new builder instance:
 * <ul>
 *     <li>{@link #newElement(String)}</li>
 *     <li>{@link #newElement()}</li>
 *     <li>{@link #newElement(String, String)}</li>
 * </ul>
 * Once you have the builder instance you can set all necessary properties and finally call {@link #build()}
 * method to get properly constructed {@link NCElement} instance.
 */
public class NCElementBuilder extends NCJsonBuilder {
    private NCElementImpl impl;

    /**
     *
     */
    private NCElementBuilder() {
        impl = new NCElementImpl();
    }

    /**
     * Creates new element builder.
     *
     * @return New element builder.
     */
    public static NCElementBuilder newElement() {
        return new NCElementBuilder();
    }

    /**
     * Creates model element builder with given element ID. See {@link NCElement#getId()} for
     * more information.
     *
     * @param id Element ID.
     * @return Newly created model element builder.
     */
    public static NCElementBuilder newElement(String id) {
        NCElementBuilder bldr = new NCElementBuilder();

        bldr.setId(id);

        return bldr;
    }

    /**
     * Creates model element builder with given element ID and group. See
     * {@link NCElement#getId()} and {@link NCElement#getGroup()} for more information.
     *
     * @param id Element ID.
     * @param grp Element group.
     * @return Newly created model element builder.
     */
    public static NCElementBuilder newElement(String id, String grp) {
        NCElementBuilder bldr = new NCElementBuilder();

        bldr.setId(id);
        bldr.setGroup(grp);

        return bldr;
    }

    /**
     * Adds value. See {@link NCElement#getValues()} for more information.
     *
     * @param name Value name.
     * @param syns Optional value synonyms.
     * @return This element builder for chaining operations.
     */
    public NCElementBuilder addValue(String name, Collection<String> syns) {
        assert name != null;
        assert syns != null;

        impl.addValue(name, syns);

        return this;
    }

    /**
     * Adds value. See {@link NCElement#getValues()} for more information.
     *
     * @param name Value name.
     * @param syns Optional value synonyms.
     * @return This element builder for chaining operations.
     */
    public NCElementBuilder addValue(String name, String... syns) {
        return addValue(name, Arrays.asList(syns));
    }

    /**
     * Adds synonyms. See {@link NCElement#getSynonyms()} for more information.
     *
     * @param syns Synonyms to add.
     * @return This element builder for chaining operations.
     */
    public NCElementBuilder addSynonyms(String... syns) {
        return addSynonyms(Arrays.asList(syns));
    }

    /**
     * Adds synonyms. See {@link NCElement#getSynonyms()} for more information.
     *
     * @param syns Synonyms to add.
     * @return This element builder for chaining operations.
     */
    public NCElementBuilder addSynonyms(Collection<String> syns) {
        for (String syn : syns)
            impl.addSynonym(syn);

        return this;
    }
    
    /**
     * Adds excluding synonyms. See {@link NCElement#getExcludedSynonyms()} for more information.
     *
     * @param syns Excluding synonyms to add.
     * @return This element builder for chaining operations.
     */
    public NCElementBuilder addExcludedSynonyms(String... syns) {
        return addExcludedSynonyms(Arrays.asList(syns));
    }

    /**
     * Adds excluding synonyms. See {@link NCElement#getExcludedSynonyms()} for more information.
     *
     * @param syns Excluding synonyms to add.

     * @return This element builder for chaining operations.
     */
    public NCElementBuilder addExcludedSynonyms(Collection<String> syns) {
        for (String syn : syns)
            impl.addExcludedSynonym(syn);

        return this;
    }

    /**
     * Sets element ID. See {@link NCElement#getId()} for more information.
     *
     * @param id Element ID to set.
     * @return This element builder for chaining operations.
     */
    public NCElementBuilder setId(String id) {
        impl.setId(id);

        return this;
    }

    /**
     * Sets element description. See {@link NCElement#getDescription()} for more information.
     *
     * @param desc Element description to set.
     * @return This element builder for chaining operations.
     */
    public NCElementBuilder setDescription(String desc) {
        impl.setDescription(desc);

        return this;
    }

    /**
     * Sets parent ID. See {@link NCElement#getParentId()} for more information.
     *
     * @param parentId Parent ID to set.
     * @return This element builder for chaining operations.
     */
    public NCElementBuilder setParentId(String parentId) {
        impl.setParentId(parentId);

        return this;
    }

    /**
     * Sets group. See {@link NCElement#getGroup()} for more information.
     *
     * @param group Group to set.
     * @return This element builder for chaining operations.
     */
    public NCElementBuilder setGroup(String group) {
        impl.setGroup(group);

        return this;
    }

    /**
     * Sets type. See {@link NCElement#getType()} for more information.
     *
     * @param type Type to set.
     * @return This element builder for chaining operations.
     */
    public NCElementBuilder setType(String type) {
        impl.setType(type);

        return this;
    }

    /**
     * Adds user defined metadata. See {@link NCElement#getMetadata()} for more information.
     *
     * @param name Metadata name.
     * @param val Metadata value.
     * @return This element builder for chaining operations.
     */
    public NCElementBuilder addMetadata(String name, Serializable val) {
        impl.addMetadata(name, val);

        return this;
    }

    /**
     * Builds and returns mode element.
     *
     * @return Model element.
     */
    public NCElement build() {
        return impl;
    }
}
