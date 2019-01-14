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

package org.nlpcraft.mdllib.tools.builder;

import org.nlpcraft.mdllib.*;
import org.nlpcraft.mdllib.tools.builder.impl.*;

/**
 * Convenient model descriptor builder.
 * <br><br>
 * To use this builder start by invoking one of the following static method to create new builder instance:
 * <ul>
 *     <li>{@link #newDescriptor(String, String, String)}</li>
 *     <li>{@link #newDescriptor()}</li>
 * </ul>
 * Once you have the builder instance you can set all necessary properties and finally call {@link #build()}
 * method to get properly constructed {@link NCModelDescriptor} instance.
 */
public class NCModelDescriptorBuilder {
    private NCModelDescriptorImpl impl;

    /**
     * Builds and returns model descriptor instance.
     * 
     * @return Built model descriptor instance.
     */
    public NCModelDescriptor build() {
        return impl;
    }

    /**
     *
     */
    private NCModelDescriptorBuilder() {
        impl = new NCModelDescriptorImpl();
    }

    /**
     * Creates new model descriptor builder.
     *
     * @return New builder.
     */
    public static NCModelDescriptorBuilder newDescriptor() {
        return new NCModelDescriptorBuilder();
    }

    /**
     * Creates new model descriptor builder with given parameters.
     *
     * @param id Unique, <i>immutable</i> ID of the model.
     * @param name Descriptive name of this model.
     * @param ver Version of this model using semantic versioning compatible
     *      with (<a href="http://www.semver.org">www.semver.org</a>) specification.
     * @return New builder.
     */
    public static NCModelDescriptorBuilder newDescriptor(String id, String name, String ver) {
        NCModelDescriptorBuilder bldr = new NCModelDescriptorBuilder();

        bldr.setId(id);
        bldr.setName(name);
        bldr.setVersion(ver);

        return bldr;
    }

    /**
     * Sets unique, <i>immutable</i> ID of the model.
     * 
     * @param id Unique, <i>immutable</i> ID of the model.
     * @return This builder for chaining operations.
     */
    public NCModelDescriptorBuilder setId(String id) {
        impl.setId(id);

        return this;
    }

    /**
     * Sets descriptive name of this model.
     *
     * @param name Descriptive name of this model.
     * @return This builder for chaining operations.
     */
    public NCModelDescriptorBuilder setName(String name) {
        impl.setName(name);

        return this;
    }

    /**
     * Sets version of this model using semantic versioning compatible
     * with (<a href="http://www.semver.org">www.semver.org</a>) specification.
     * 
     * @param ver Version of this model using semantic versioning compatible
     *      with (<a href="http://www.semver.org">www.semver.org</a>) specification.
     * @return This builder for chaining operations.
     */
    public NCModelDescriptorBuilder setVersion(String ver) {
        impl.setVersion(ver);

        return this;
    }
}
