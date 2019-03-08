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

package org.nlpcraft.server.mdo.impl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks constructor field for automated support for (a) JSON conversion, and (b) SQL CRUD operations.
 * Annotations 'NCMdoEntity' and 'NCMdoField' should be used together.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface NCMdoField {
    /**
     * Whether or not to include into SQL CRUD operations.
     */                                                              
    boolean sql() default true;

    /**
     * Whether or not to include into JSON export.
     */
    boolean json() default true;

    /**
     * Optional function name to generate JSON value for the field.
     *
     * By default the actual field value will be used for JSON export.
     * This converter function can be used to modify this default behavior.
     *
     * Converter function can have zero or one parameter only. If it has one parameter
     * the actual field value will be passed in to convert. Function should return a
     * new value to be used in JSON export.
     */
    String jsonConverter() default "";

    /**
     * SQL column name. This is mandatory if 'table' is specified in 'NCMdoEntity' annotation.
     */
    String column() default "";

    /**
     * Custom JSON field name to use instead of source code parameter name.
     */
    String jsonName() default "";

    /**
     * Custom JDBC type to use instead of default JDBC type mapping.
     */
    int jdbcType() default Integer.MIN_VALUE;

    /**
     * Wether or not this field is a primary key.
     */
    boolean pk() default false;
}
