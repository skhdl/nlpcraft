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

package org.nlpcraft

import java.io._
import java.util.concurrent.Callable

import org.apache.ignite.IgniteException
import org.nlpcraft.json.NCJsonException

import scala.language.implicitConversions

/**
 * Package scope.
 */
package object server {
    // Type aliases for `java.io`
    type OIS = ObjectInputStream
    type OOS = ObjectOutputStream
    type BOS = BufferedOutputStream
    type FOS = FileOutputStream
    type FIS = FileInputStream
    type BIS = BufferedInputStream
    type IOE = IOException

    type NCJ = NCJsonException

    // Type aliases for Ignite.
    type IE = IgniteException

    /**
     * Support for Ignite vis-a-vis Scala usage.
     *
     * @param f Clojure to convert.
     * @return Runnable object.
     */
    implicit def toRunnable(f: () ⇒ Unit): Runnable =
        new Runnable() {
            override def run(): Unit = f()
        }

    /**
     * Support for Ignite vis-a-vis Scala usage.
     *
     * @param f Clojure to convert.
     * @return Callable object.
     */
    implicit def toCallable[R](f: () ⇒ R): Callable[R] =
        new Callable[R] {
            override def call(): R = f()
        }
}
