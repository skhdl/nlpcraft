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

package org.nlpcraft.tx

import java.sql.Connection

import org.apache.ignite.IgniteTransactions
import org.apache.ignite.lang.IgniteUuid
import org.apache.ignite.transactions.{Transaction, TransactionConcurrency, TransactionIsolation}
import org.nlpcraft.ignite.NCIgniteNLPCraft
import org.nlpcraft.{NCE, NCLifecycle, _}

import scala.collection.mutable
import scala.util.control.Exception.catching

/**
  * Transaction manager based on Ignite transaction management. It manages both Ignite cache
  * and JDBC operations, and allows for multi-threaded transactions.
  */
object NCTxManager extends NCLifecycle("Transaction manager") with NCIgniteNLPCraft {
    // Internal log switch.
    private final val LOG_TX = false
    
    private val cons = mutable.HashMap.empty[IgniteUuid, Connection]
    
    /**
      * 
      * @return
      */
    private var itx: IgniteTransactions = _
    
    /**
      * Stops this component.
      */
    override def stop(): Unit = {
        // Close all still attached JDBC connections on stop.
        cons.values.foreach(G.close)
        
        super.stop()
    }


    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        itx = ignite.transactions()

        super.start()
    }

    /**
      * Gets Ignite transaction associated with the current thread, or `null` otherwise.
      */
    def tx(): Transaction = {
        ensureStarted()

        catching(wrapIE) {
            itx.tx()
        }
    }
    
    /**
      * 
      * @return
      */
    def inTx(): Boolean = {
        ensureStarted()

        tx() != null
    }
    
    /**
      * Attaches JDBC connection to the given transaction. No-op if transaction already
      * has attached connection.
      * 
      * @param tx Transaction to attach to.
      * @param con JDBC connection to attach.
      */
    def attach(tx: Transaction, con: Connection): Unit = {
        ensureStarted()

        val xid = tx.xid()
        
        cons.synchronized {
            if (!cons.contains(xid))
                cons += xid → con
        }
    }
    
    /**
      * Attaches JDBC connection to the current transaction. No-op if there is not transaction or transaction already
      * has attached connection.
      *
      * @param con JDBC connection to attach.
      */
    def attach(con: Connection): Unit = {
        ensureStarted()

        val x = tx()
        
        if (x != null) {
            val xid = x.xid()
    
            cons.synchronized {
                if (!cons.contains(xid))
                    cons += xid → con
            }
        }
    }

    /**
      * Gets connection attached to the given transaction of the current thread, or `null`
      * if JDBC connection wasn't attached to that thread.
      */
    def connection(tx: Transaction): Connection = {
        ensureStarted()

        cons.synchronized {
            cons.get(tx.xid).orNull
        }
    }
    
    /**
      * Gets connection attached to the transaction of the current thread, or `null`
      * if (a) current thread has no ongoing transaction, or (b) JDBC connection wasn't
      * attached.
      */
    def connection(): Connection = {
        ensureStarted()

        val x = tx()
        
        if (x != null)
            cons.synchronized {
                cons.get(x.xid).orNull
            }
        else
            null
    }
    
    /**
      *
      * @param tx Transaction to finalize.
      */
    private def finalizeTx(tx: Transaction): Unit = {
        // Close tx (rollback if it wasn't explicitly committed).
        try {
            if (tx.isRollbackOnly)
                tx.rollback()

            tx.close()

            if (LOG_TX)
                logger.trace(s"Finalized tx: ${tx.xid}")
        }
        finally
            // Detach the connection on tx finalization.
            cons.synchronized {
                cons.remove(tx.xid) match {
                    // If there was a JDBC connection - explicitly close it on tx close.
                    case Some(con) ⇒ G.close(con)
                    case None ⇒ ()
                }
            }
    }
    
    /**
      *
      * @param tx
      * @param f
      * @tparam R
      * @return
      */
    private def startTx[R](tx: Transaction)(f: Transaction ⇒ R): R = {
        try {
            val res = f(tx)
            
            tx.commit()
            
            res
        }
        catch {
            case e: Throwable ⇒ tx.setRollbackOnly(); throw e
        }
        finally
            finalizeTx(tx)
    }
    
    /**
      *
      * @param tx
      * @param f
      * @tparam R
      * @return
      */
    private def startTx0[R](tx: Transaction)(f: ⇒ R): R = {
        try {
            val res = f
            
            tx.commit()
            
            res
        }
        catch {
            case e: Throwable ⇒ tx.setRollbackOnly(); throw e
        }
        finally
            finalizeTx(tx)
    }
    
    private def ack(tx: Transaction): Unit =
        if (LOG_TX)
            logger.trace(s"New tx started: [xid=${tx.xid()}, iso=${tx.isolation()}, concurrency=${tx.concurrency()}]")

    /**
      * Executes given closure in the transaction context. It will start a new transaction if the current
      * thread does not have one already. 
      *
      * @param cry Transaction concurrency.
      * @param iso Transaction isolation.
      * @param timeout Transaction timeout.
      * @param size Approximate number of cache entries participating in the transaction.
      * @param f Closure to execute.
      * @tparam R Result type.
      */
    @throws[NCE]
    def startTx[R](cry: TransactionConcurrency, iso: TransactionIsolation, timeout: Long, size: Int)
        (f: Transaction ⇒ R): R = {
        ensureStarted()
    
        catching(wrapIE) {
            if (inTx()) {
                val x = tx()
                
                if (x.concurrency() != cry || x.isolation() != iso || x.timeout() != timeout)
                    throw new NCE(s"Requested transaction does not match the existing one: $x")
                
                f(x)
            }
            else {
                val tx = itx.txStart(cry, iso, timeout, size)
                
                ack(tx)
                
                startTx[R](tx)(f)
            }
        }
    }
    
    /**
      * Executes given closure in the transaction context. It will start a new transaction if the current
      * thread does not have one already.
      *
      * @param cry Transaction concurrency.
      * @param iso Transaction isolation.
      * @param f Closure to execute.
      * @tparam R Result type.
      */
    @throws[NCE]
    def startTx[R](cry: TransactionConcurrency, iso: TransactionIsolation)(f: Transaction ⇒ R): R = {
        ensureStarted()
    
        catching(wrapIE) {
            if (inTx()) {
                val x = tx()
            
                if (x.concurrency() != cry || x.isolation() != iso)
                    throw new NCE(s"Requested transaction does not match the existing one: $x")
            
                f(x)
            }
            else {
                val tx = itx.txStart(cry, iso)
                
                ack(tx)
                
                startTx[R](tx)(f)
            }
        }
    }

    /**
      * Executes given closure in the transaction context. It will start a new transaction if the current
      * thread does not have one already.
      *
      * @param f Closure to execute.
      * @tparam R Result type.
      */
    @throws[NCE]
    def startTx[R](f: Transaction ⇒ R): R = {
        ensureStarted()
    
        catching(wrapIE) {
            if (inTx())
                f(tx())
            else {
                val tx = itx.txStart()
                
                ack(tx)
                
                startTx[R](tx)(f)
            }
        }
    }
    
    /**
      * Executes given closure in the transaction context. It will start a new transaction if the current
      * thread does not have one already.
      *
      * @param f Closure to execute.
      * @tparam R Result type.
      */
    @throws[NCE]
    def startTx[R](f: ⇒ R): R = {
        ensureStarted()
    
        catching(wrapIE) {
            if (inTx())
                f
            else {
                val tx = itx.txStart()
                
                ack(tx)
                
                startTx0[R](tx)(f)
            }
        }
    }
}
