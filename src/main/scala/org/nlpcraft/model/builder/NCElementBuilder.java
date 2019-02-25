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

package org.nlpcraft.model.builder;

import org.nlpcraft.model.NCElement;
import org.nlpcraft.model.builder.impl.NCElementImpl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

/**
 * Model element builder for {@link NCElement} instances.
 * <br><br>
 * To use this builder start by invoking one of the following static method to create new builder instance:
 * <ul>
 *     <li>{@link #newElement(String)}</li>
 *     <li>{@link #newElement()}</li>
 *     <li>{@link #newElement(String, String)}</li>
 * </ul>
 * Once you have the builder instance you can set all necessary properties and finally call {@link #build()}
 * method to get properly constructed {@link NCElement} instance. Note that at the minimum the element
 * {@link #setId(String) ID} must be set.
 */
public class NCElementBuilder {
    private final NCElementImpl impl;

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
     * Builds and returns mode element. Note that at the minimum the element
     * {@link #setId(String) ID} must be set.
     *
     * @return Model element.
     * @throws NCBuilderException Thrown in case of any errors building the element.
     */
    public NCElement build() throws NCBuilderException {
        if (impl.getId() == null)
            throw new NCBuilderException("Element ID is not set.");
        
        return impl;
    }
}
