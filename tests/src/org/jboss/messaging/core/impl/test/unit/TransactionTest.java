package org.jboss.messaging.core.impl.test.unit;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.xa.Xid;

import org.easymock.EasyMock;
import org.jboss.messaging.core.Message;
import org.jboss.messaging.core.MessageReference;
import org.jboss.messaging.core.PersistenceManager;
import org.jboss.messaging.core.Queue;
import org.jboss.messaging.core.Transaction;
import org.jboss.messaging.core.TransactionSynchronization;
import org.jboss.messaging.core.impl.QueueImpl;
import org.jboss.messaging.core.impl.TransactionImpl;
import org.jboss.messaging.test.unit.UnitTestCase;

/**
 * 
 * A TransactionTest
 * 
 * TODO test with persistent and non persistent
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class TransactionTest extends UnitTestCase
{
   
   public void test1PCCommit() throws Exception
   {
      List<Message> msgsToAdd = new ArrayList<Message>();
      
      List<MessageReference> refsToRemove = new ArrayList<MessageReference>();
      
      Queue queue = new QueueImpl(1, "queue1", null, false, true, false, -1);
      
      MessageReference ref1 = this.generateReference(queue, 1);
      msgsToAdd.add(ref1.getMessage());
      
      MessageReference ref2 = this.generateReference(queue, 2);
      refsToRemove.add(ref2);
                  
      Transaction tx = new TransactionImpl();
      
      tx.addMessage(ref1.getMessage());
      tx.addAcknowledgement(ref2);
      
      PersistenceManager pm = EasyMock.createStrictMock(PersistenceManager.class);
      
      pm.commitTransaction(msgsToAdd, refsToRemove);
      
      EasyMock.replay(pm);
      
      tx.commit(pm);
      
      EasyMock.verify(pm);
      
      assertEquals(ref1, queue.list(null).get(0));
   }
   
   public void test1PCRollback() throws Exception
   {
      List<Message> msgsToAdd = new ArrayList<Message>();
      
      List<MessageReference> refsToRemove = new ArrayList<MessageReference>();
      
      Queue queue = new QueueImpl(1, "queue1", null, false, true, false, -1);
      
      MessageReference ref1 = this.generateReference(queue, 1);
      msgsToAdd.add(ref1.getMessage());
      
      MessageReference ref2 = this.generateReference(queue, 2);
      refsToRemove.add(ref2);
                  
      Transaction tx = new TransactionImpl();
      tx.addMessage(ref1.getMessage());
      tx.addAcknowledgement(ref2);
      
      PersistenceManager pm = EasyMock.createStrictMock(PersistenceManager.class);
      
      pm.updateDeliveryCount(queue, ref2);
      
      EasyMock.replay(pm);
      
      tx.rollback(pm);
      
      EasyMock.verify(pm);
 
      assertEquals(ref2, queue.list(null).get(0));
   }
   
   public void test1PCPrepare() throws Exception
   {
      List<Message> msgsToAdd = new ArrayList<Message>();
      
      List<MessageReference> refsToRemove = new ArrayList<MessageReference>();
      
      Queue queue = new QueueImpl(1, "queue1", null, false, true, false, -1);
      
      MessageReference ref1 = this.generateReference(queue, 1);
      msgsToAdd.add(ref1.getMessage());
      
      MessageReference ref2 = this.generateReference(queue, 2);
      refsToRemove.add(ref2);
                  
      Transaction tx = new TransactionImpl();
      tx.addMessage(ref1.getMessage());
      tx.addAcknowledgement(ref2);
      
      PersistenceManager pm = EasyMock.createStrictMock(PersistenceManager.class);
      
      try
      {
         tx.prepare(pm);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         //OK
      }   
      
      assertTrue(queue.list(null).isEmpty());
   }
   
   public void test2PCPrepareCommit() throws Exception
   {
      List<Message> msgsToAdd = new ArrayList<Message>();
      
      List<MessageReference> refsToRemove = new ArrayList<MessageReference>();
      
      Queue queue = new QueueImpl(1, "queue1", null, false, true, false, -1);
      
      MessageReference ref1 = this.generateReference(queue, 1);
      msgsToAdd.add(ref1.getMessage());
      
      MessageReference ref2 = this.generateReference(queue, 2);
      refsToRemove.add(ref2);
      
      Xid xid = generateXid();
                  
      Transaction tx = new TransactionImpl(xid);
      tx.addMessage(ref1.getMessage());
      tx.addAcknowledgement(ref2);
      
      PersistenceManager pm = EasyMock.createStrictMock(PersistenceManager.class);
      
      pm.prepareTransaction(xid, msgsToAdd, refsToRemove);
      
      EasyMock.replay(pm);
      
      tx.prepare(pm);
      
      EasyMock.verify(pm);
      
      EasyMock.reset(pm);
      
      pm.commitPreparedTransaction(xid);
      
      EasyMock.replay(pm);
      
      tx.commit(pm);
      
      EasyMock.verify(pm);
   }
   
   public void test2PCCommitBeforePrepare() throws Exception
   {
      List<Message> msgsToAdd = new ArrayList<Message>();
      
      List<MessageReference> refsToRemove = new ArrayList<MessageReference>();
      
      Queue queue = new QueueImpl(1, "queue1", null, false, true, false, -1);
      
      MessageReference ref1 = this.generateReference(queue, 1);
      msgsToAdd.add(ref1.getMessage());
      
      MessageReference ref2 = this.generateReference(queue, 2);
      refsToRemove.add(ref2);
          
      Xid xid = generateXid();
      
      Transaction tx = new TransactionImpl(xid);
      tx.addMessage(ref1.getMessage());
      tx.addAcknowledgement(ref2);
      
      PersistenceManager pm = EasyMock.createStrictMock(PersistenceManager.class);
      
      try
      {    
         tx.commit(pm);
         
         fail ("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         //Ok
      }      
   }
   
   public void test2PCPrepareRollback() throws Exception
   {
      List<Message> msgsToAdd = new ArrayList<Message>();
      
      List<MessageReference> refsToRemove = new ArrayList<MessageReference>();
      
      Queue queue = new QueueImpl(1, "queue1", null, false, true, false, -1);
      
      MessageReference ref1 = this.generateReference(queue, 1);
      msgsToAdd.add(ref1.getMessage());
      
      MessageReference ref2 = this.generateReference(queue, 2);
      refsToRemove.add(ref2);
      
      Xid xid = generateXid();
                  
      Transaction tx = new TransactionImpl(xid);
      tx.addMessage(ref1.getMessage());
      tx.addAcknowledgement(ref2);
      
      PersistenceManager pm = EasyMock.createStrictMock(PersistenceManager.class);
      
      pm.prepareTransaction(xid, msgsToAdd, refsToRemove);
      
      EasyMock.replay(pm);
      
      tx.prepare(pm);
      
      EasyMock.verify(pm);
      
      EasyMock.reset(pm);
      
      pm.unprepareTransaction(xid, msgsToAdd, refsToRemove);
      
      pm.updateDeliveryCount(queue, ref2);
      
      EasyMock.replay(pm);
      
      tx.rollback(pm);
      
      EasyMock.verify(pm);
   }
   
   public void testSynchronizations() throws Exception
   {
      List<Message> msgsToAdd = new ArrayList<Message>();
      
      List<MessageReference> refsToRemove = new ArrayList<MessageReference>();
      
      Queue queue = new QueueImpl(1, "queue1", null, false, true, false, -1);
      
      MessageReference ref1 = this.generateReference(queue, 1);
      msgsToAdd.add(ref1.getMessage());
      
      MessageReference ref2 = this.generateReference(queue, 2);
      refsToRemove.add(ref2);
                  
      Transaction tx = new TransactionImpl();
      tx.addMessage(ref1.getMessage());
      tx.addAcknowledgement(ref2);
      
      TransactionSynchronization sync = EasyMock.createStrictMock(TransactionSynchronization.class);
      
      PersistenceManager pm = EasyMock.createStrictMock(PersistenceManager.class);
      
      tx.addSynchronization(sync);
      
      sync.beforeCommit();
      sync.afterCommit();
      
      EasyMock.replay(sync);
      
      tx.commit(pm);
      
      EasyMock.verify(sync);
      
      EasyMock.reset(sync);
      
      tx = new TransactionImpl();
      tx.addMessage(ref1.getMessage());
      tx.addAcknowledgement(ref2);
      
      tx.addSynchronization(sync);
      
      sync.beforeRollback();
      sync.afterRollback();
      
      EasyMock.replay(sync);
      
      tx.rollback(pm);
      
      EasyMock.verify(sync);            
   }
   
   public void testSynchronizations2PC() throws Exception
   {
      List<Message> msgsToAdd = new ArrayList<Message>();
      
      List<MessageReference> refsToRemove = new ArrayList<MessageReference>();
      
      Queue queue = new QueueImpl(1, "queue1", null, false, true, false, -1);
      
      MessageReference ref1 = this.generateReference(queue, 1);
      msgsToAdd.add(ref1.getMessage());
      
      MessageReference ref2 = this.generateReference(queue, 2);
      refsToRemove.add(ref2);
      
      Xid xid = generateXid();
                  
      Transaction tx = new TransactionImpl(xid);
      tx.addMessage(ref1.getMessage());
      tx.addAcknowledgement(ref2);
      
      TransactionSynchronization sync = EasyMock.createStrictMock(TransactionSynchronization.class);
      
      PersistenceManager pm = EasyMock.createStrictMock(PersistenceManager.class);
      
      tx.addSynchronization(sync);
      
      sync.beforeCommit();
      sync.afterCommit();
      
      EasyMock.replay(sync);
      
      tx.prepare(pm);
      tx.commit(pm);
      
      EasyMock.verify(sync);
      
      EasyMock.reset(sync);
      
      xid = generateXid();
      
      tx = new TransactionImpl(xid);
      tx.addMessage(ref1.getMessage());
      tx.addAcknowledgement(ref2);
      
      tx.addSynchronization(sync);
      
      sync.beforeRollback();
      sync.afterRollback();
      
      EasyMock.replay(sync);
      
      tx.prepare(pm);
      tx.rollback(pm);
      
      EasyMock.verify(sync);            
   }
   
   // Inner classes -----------------------------------------------------------------------
   
}
