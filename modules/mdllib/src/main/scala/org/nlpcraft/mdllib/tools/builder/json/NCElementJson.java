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

package org.nlpcraft.mdllib.tools.builder.json;

import java.util.*;

/**
 * JSON parsing bean.
 */
public class NCElementJson {
    private String id;
    private String group;
    private String parentId;
    private String desc;
    private String[] excludedSynonyms = new String[0];
    private String[] synonyms = new String[0];
    @SuppressWarnings("unchecked") private Map<String, Object> metadata = Collections.EMPTY_MAP;
    private NCValueJson[] values = new NCValueJson[0];

    /**
     *
     * @return
     */
    public String getParentId() {
        return parentId;
    }

    /**
     *
     * @param parentId
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     *
     * @return
     */
    public NCValueJson[] getValues() {
        return values;
    }

    /**
     *
     * @param values
     */
    public void setValues(NCValueJson[] values) {
        this.values = values;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     *
     * @param metadata
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     * @return
     */
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
     * @return
     */
    public String getDescription() {
        return desc;
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
    public String[] getExcludedSynonyms() {
        return excludedSynonyms;
    }

    /**
     *
     * @param excludedSynonyms
     */
    public void setExcludedSynonyms(String[] excludedSynonyms) {
        this.excludedSynonyms = excludedSynonyms;
    }

    /**
     *
     * @return
     */
    public String[] getSynonyms() {
        return synonyms;
    }

    /**
     *
     * @param synonyms
     */
    public void setSynonyms(String[] synonyms) {
        this.synonyms = synonyms;
    }
}
