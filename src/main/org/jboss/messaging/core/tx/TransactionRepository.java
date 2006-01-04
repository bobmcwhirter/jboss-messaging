/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
package org.jboss.messaging.core.tx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.transaction.xa.Xid;

import org.jboss.messaging.core.PersistenceManager;
import org.jboss.logging.Logger;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;


/**
 * 
 * This class maintains JMS Server local transactions
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version $Revision 1.1 $
 *
 * $Id$
 */
public class TransactionRepository
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(TransactionRepository.class);

   // Attributes ----------------------------------------------------
   
   protected Map globalToLocalMap;     
   
   protected PersistenceManager pm;

   // Static --------------------------------------------------------
   
   // Constructors --------------------------------------------------
   
   public TransactionRepository(PersistenceManager pm)
   {
      globalToLocalMap = new ConcurrentReaderHashMap();
      this.pm = pm;  
   }
   
   // Public --------------------------------------------------------
   
   public List getPreparedTransactions()
   {
      ArrayList prepared = new ArrayList();
      Iterator iter = globalToLocalMap.values().iterator();
      while (iter.hasNext())
      {
         Transaction tx = (Transaction)iter.next();
         if (tx.xid != null && tx.getState() == Transaction.STATE_PREPARED)
         {
            prepared.add(tx.getXid());
         }
      }
      return prepared;
   }
   
   /*
    * Load any prepared transactions into the repository so they can be recovered
    */
   public void loadPreparedTransactions() throws TransactionException
   {
      List prepared = null;
      
      try
      {
         prepared = pm.retrievePreparedTransactions();
      }
      catch (Exception e)
      {
         throw new TransactionException("Failed to retrieve prepared transactions", e);
      }
      
      if (prepared != null)
      {         
         Iterator iter = prepared.iterator();
         
         while (iter.hasNext())
         {
            Xid xid = (Xid)iter.next();
            Transaction tx = createTransaction(xid);
            tx.insertedTXRecord();
            tx.state = Transaction.STATE_PREPARED;
            
            //Load the references for this transaction
         }
      }
   }
         
   public Transaction getPreparedTx(Xid xid) throws TransactionException
   {
      Transaction tx = (Transaction)globalToLocalMap.get(xid);
      if (tx == null)
      {
         throw new TransactionException("Cannot find local tx for xid:" + xid);
      }
      if (tx.getState() != Transaction.STATE_PREPARED)
      {
         throw new TransactionException("Transaction with xid " + xid + " is not in prepared state");
      }
      return tx;
   }
   
   public Transaction createTransaction(Xid xid) throws TransactionException
   {
      if (globalToLocalMap.containsKey(xid))
      {
         throw new TransactionException("There is already a local tx for global tx " + xid);
      }
      Transaction tx = new Transaction(xid, pm);
      
      if (log.isTraceEnabled()) { log.trace("created transaction " + tx); }
      
      globalToLocalMap.put(xid, tx);
      return tx;
   }
   
   public Transaction createTransaction() throws TransactionException
   {
      Transaction tx = new Transaction(null, pm);

      if (log.isTraceEnabled()) { log.trace("created transaction " + tx); }

      return tx;
   }
   
   public void setPersistenceManager(PersistenceManager pm)
   {
      this.pm = pm;
   }
   
   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------         
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
   
}