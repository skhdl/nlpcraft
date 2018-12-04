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

/**
 * Default model descriptor implementation.
 */
public class NCModelDescriptorImpl implements NCModelDescriptor {
    private String id;
    private String name;
    private String ver;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return ver;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        assert name != null;

        this.name = name;
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
     * @param ver
     */
    public void setVersion(String ver) {
        assert ver != null;

        this.ver = ver;
    }

    @Override
    public String toString() {
        return String.format("Model descriptor [id=%s, name=%s, ver=%s]", id, name, ver);
    }
}
