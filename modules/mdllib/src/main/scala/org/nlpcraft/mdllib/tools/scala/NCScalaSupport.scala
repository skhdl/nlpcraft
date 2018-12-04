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

package org.nlpcraft.mdllib.tools.scala

import java.util.function.{BiPredicate, Supplier, Function ⇒ JFunction, Predicate ⇒ JPredicate}

import org.nlpcraft.mdllib._
import org.nlpcraft.mdllib.intent.NCIntentSolver._
import org.nlpcraft.mdllib.intent.NCIntentSolverContext
import org.nlpcraft.mdllib.tools.{NCSerializableFunction, NCSerializableRunnable}
import org.nlpcraft.mdllib.tools.{NCSerializableFunction, NCSerializableRunnable}

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
      *
      * @param f
      * @return
      */
    implicit def toIntentCallback(f: NCIntentSolverContext ⇒ NCQueryResult): IntentCallback =
        new IntentCallback() {
            override def apply(ctx: NCIntentSolverContext): NCQueryResult = f(ctx)
        }
    
    /**
      *                                           
      * @param f
      * @tparam T
      * @tparam R
      * @return
      */
    implicit def toSerializableFunction[T, R](f: T ⇒ R): NCSerializableFunction[T, R] =
        new NCSerializableFunction[T, R] {
            override def apply(t: T) = f(t)
        }
    
    /**
      *
      * @param f
      * @return
      */
    implicit def toSerializableRunnable(f: Unit ⇒ Unit): NCSerializableRunnable =
        new NCSerializableRunnable {
            override def run(): Unit = f(())
        }
}

