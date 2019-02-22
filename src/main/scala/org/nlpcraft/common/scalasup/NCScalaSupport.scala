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

package org.nlpcraft.common.scalasup

import java.util.concurrent.Callable
import java.util.function.{BiPredicate, Supplier, Function ⇒ JFunction, Predicate ⇒ JPredicate}

import scala.language.implicitConversions

/**
  * Miscellaneous Scala support.
  */
object NCScalaSupport {
    /**
      *
      * @param f
      * @tparam A
      * @return
      */
    implicit def toJavaSupplier[A](f: () ⇒ A): Supplier[A] = new Supplier[A] {
        override def get(): A = f()
    }
    
    /**
      *
      * @param f
      * @tparam A
      * @tparam B
      * @return
      */
    implicit def toJavaFunction[A, B](f: (A) ⇒ B): JFunction[A, B] = new JFunction[A, B] {
        override def apply(a: A): B = f(a)
    }
    
    /**
      *
      * @param f
      * @tparam A
      * @return
      */
    implicit def toJavaPredicate[A](f: (A) ⇒ Boolean): JPredicate[A] = new JPredicate[A] {
        override def test(a: A): Boolean = f(a)
    }
    
    /**
      *
      * @param predicate
      * @tparam A
      * @tparam B
      * @return
      */
    implicit def toJavaBiPredicate[A, B](predicate: (A, B) ⇒ Boolean): BiPredicate[A, B] = new BiPredicate[A, B] {
        override def test(a: A, b: B) = predicate(a, b)
    }
    
    /**
      * @param f Lambda to convert.
      * @return Runnable object.
      */
    implicit def toRunnable(f: () ⇒ Unit): Runnable =
        new Runnable() {
            override def run(): Unit = f()
        }
    
    /**
      * @param f Lambda to convert.
      * @return Callable object.
      */
    implicit def toCallable[R](f: () ⇒ R): Callable[R] =
        new Callable[R] {
            override def call(): R = f()
        }
}

