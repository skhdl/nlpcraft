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

package org.nlpcraft.ignite

import javax.cache.Cache.Entry
import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cache.store.{CacheStoreAdapter, CacheStoreSession}
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.resources.CacheStoreSessionResource
import org.apache.ignite.{Ignition, IgniteException => IE}
import org.nlpcraft.NCE
import org.nlpcraft.tx.NCTxManager

import scala.util.control.Exception._

/**
 * Base cache store.
 */
abstract class NCIgniteCacheStore[K, V] extends CacheStoreAdapter[K, V] with Serializable {
    @CacheStoreSessionResource
    protected var ses: CacheStoreSession = _
    
    /**
      * Partial function that catches database 'DLE' and wraps it into Ignite exception.
      *
      * @tparam R Type of the return value for the body.
      * @return Catcher.
      */
    protected def wrapDLE[R]: Catcher[R] = {
        // We assume here that DLE exception is database related.
        case e: NCE ⇒
            println(s"|> Wrapping DLE: ${e.toString}")

            if (e.getCause != null) {
                println(s"|>    |")
                println(s"|>    +--- Immediate cause: ${e.getCause.toString}")
            }

            throw new IE("Cache store error.", e)
    }
    
    /**
      * Ensures that current thread is associated with ongoing transaction.
      */
    @throws[NCE]
    protected def ensureInTx(): Unit = {
        if (!NCTxManager.inTx())
            throw new NCE(s"Thread NOT in transaction: ${Thread.currentThread()}")
    }
    
    /**
      *
      */
    @throws[NCE]
    protected def ensureTransactionAtomicity(): Unit = {
        if (ses != null) {
            val cfg = Ignition.localIgnite().getOrCreateCache[K, V](ses.cacheName).
                getConfiguration(classOf[CacheConfiguration[K, V]])
            
            if (cfg.getAtomicityMode != CacheAtomicityMode.TRANSACTIONAL)
                throw new NCE(s"Cache has non-transactional atomicity: ${ses.cacheName()}")
        }
    }

    /**
      * Adapter for writing key-value entry to the external storage.
      *
      * Must be implemented in the cache store.
      *
      * @param key Key to the cache entry.
      * @param value Value to the cache entry.
      */
    @throws[IE]
    protected def put(key: K, value: V): Unit

    /**
      * Adapter for loading value from the external storage.
      *
      * Must be implemented in the cache store.
      *
      * @param key Key to the cache entry.
      */
    @throws[IE]
    protected def get(key: K): V

    /**
      * Adapter for deleting cache entry.
      *
      * Must be implemented in the cache store.
      *
      * @param key The key that is used for the delete operation.
      */
    @throws[IE]
    protected def remove(key: K): Unit

    /**
     * Loads a value from external storage.
     *
     * @param key Key to the cache entry.
     */
   @throws[IE]
   override def load(key: K): V = {
       get(key)
   }

    /**
      * Write the specified value under the specified key to the external resource.
      *
      * @param e Key-value entry to write.
      */
    @throws[IE]
    override def write(e: Entry[_ <: K, _ <: V]): Unit = {
        put(e.getKey, e.getValue)
    }

    /**
      * Delete the cache entry from the external resource.
      *
      * @param key Key to the cache entry.
      */
    @throws[IE]
    override def delete(key: scala.Any): Unit = remove(key.asInstanceOf[K])
}
