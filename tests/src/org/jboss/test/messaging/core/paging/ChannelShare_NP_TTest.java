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
package org.jboss.test.messaging.core.paging;

import java.util.List;

import org.jboss.messaging.core.ChannelSupport;
import org.jboss.messaging.core.Message;
import org.jboss.messaging.core.MessageReference;
import org.jboss.messaging.core.local.Queue;
import org.jboss.messaging.core.message.MessageFactory;
import org.jboss.messaging.core.plugin.LockMap;
import org.jboss.messaging.core.tx.Transaction;

import EDU.oswego.cs.dl.util.concurrent.QueuedExecutor;


/**
 * 
 * A ChannelShare_NP_T_Recoverable.
 * 
 * @author <a href="tim.fox@jboss.com">Tim Fox</a>
 * @version 1.1
 *
 * ChannelShare_NP_T.java,v 1.1 2006/03/22 10:23:35 timfox Exp
 */
public class ChannelShare_NP_TTest extends PagingStateTestBase
{
   public ChannelShare_NP_TTest(String name)
   {
      super(name);
   }

   // Public --------------------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp(); 
   }
   
   
   public void tearDown() throws Exception
   {
      super.tearDown();
   }
   
   public void testChannelShareNP_Transactional() throws Throwable
   {
      ChannelSupport queue1 = new Queue(1, ms, pm, null, true, 100, 20, 10, new QueuedExecutor());
      
      ChannelSupport queue2 = new Queue(2, ms, pm, null, true, 50, 10, 5, new QueuedExecutor());
                     
      Message[] msgs = new Message[150];
      
      MessageReference[] refs1 = new MessageReference[150];
      
      MessageReference[] refs2 = new MessageReference[150];
      
      //Send 50 refs to both channels
  
      Transaction tx = tr.createTransaction();
      for (int i = 0; i < 50; i++)
      {
         msgs[i] = MessageFactory.createCoreMessage(i, false, null);
         
         refs1[i] = ms.reference(msgs[i]);
                
         queue1.handle(null, refs1[i], tx); 
         
         refs1[i].releaseMemoryReference();
         
         refs2[i] = ms.reference(msgs[i]);
         
         queue2.handle(null, refs2[i], tx); 
         
         refs2[i].releaseMemoryReference();
      }
      tx.commit();
      
      //Queue1
      List refIds = getReferenceIds(queue1.getChannelID());
      assertEquals(0, refIds.size());
                                    
      assertEquals(50, queue1.memoryRefCount());
      
      assertEquals(0, queue1.downCacheCount());
      
      assertFalse(queue1.isPaging());      
      
      assertEquals(0, queue1.memoryDeliveryCount());
      
      //Queue2
      
      refIds = getReferenceIds(queue2.getChannelID());
      assertEquals(0, refIds.size());
                              
      assertEquals(50, queue2.memoryRefCount());
      
      assertEquals(0, queue2.downCacheCount());
      
      assertTrue(queue2.isPaging());      
      
      assertEquals(0, queue2.memoryDeliveryCount());
            
      //Msgs
      
      assertEquals(50, ms.size());
      
      List msgIds = getMessageIds();
      assertEquals(0, msgIds.size()); 
      
      //Add 25 more
      
      tx = tr.createTransaction();
      for (int i = 50; i < 75; i++)
      {
         msgs[i] = MessageFactory.createCoreMessage(i, false, null);
         
         refs1[i] = ms.reference(msgs[i]);
                
         queue1.handle(null, refs1[i], tx); 
         
         refs1[i].releaseMemoryReference();
         
         refs2[i] = ms.reference(msgs[i]);
         
         queue2.handle(null, refs2[i], tx); 
         
         refs2[i].releaseMemoryReference();
      }
      tx.commit();
      
      //Queue1
      refIds = getReferenceIds(queue1.getChannelID());
              
      assertEquals(0, refIds.size());
                                    
      assertEquals(75, queue1.memoryRefCount());
      
      assertEquals(0, queue1.downCacheCount());
      
      assertFalse(queue1.isPaging());      
      
      assertEquals(0, queue1.memoryDeliveryCount());
      
      //Queue2
      
      refIds = getReferenceIds(queue2.getChannelID());
      assertEquals(25, refIds.size());
                              
      assertEquals(50, queue2.memoryRefCount());
      
      assertEquals(0, queue2.downCacheCount());
      
      assertTrue(queue2.isPaging());      
      
      assertEquals(0, queue2.memoryDeliveryCount());
            
      //Msgs
      
      assertEquals(75, ms.size());
      
      msgIds = getMessageIds();
      assertEquals(25, msgIds.size());
      
      
      
      // Add 25 more
      tx = tr.createTransaction();
      for (int i = 75; i < 100; i++)
      {
         msgs[i] = MessageFactory.createCoreMessage(i, false, null);
         
         refs1[i] = ms.reference(msgs[i]);
                
         queue1.handle(null, refs1[i], tx); 
         
         refs1[i].releaseMemoryReference();
         
         refs2[i] = ms.reference(msgs[i]);
         
         queue2.handle(null, refs2[i], tx); 
         
         refs2[i].releaseMemoryReference();
      }
      tx.commit();
      
      //Queue1
      refIds = getReferenceIds(queue1.getChannelID());
                
      assertEquals(0, refIds.size());
                                    
      assertEquals(100, queue1.memoryRefCount());
      
      assertEquals(0, queue1.downCacheCount());
      
      assertTrue(queue1.isPaging());      
      
      assertEquals(0, queue1.memoryDeliveryCount());
      
      //Queue2
      
      refIds = getReferenceIds(queue2.getChannelID());
      assertEquals(50, refIds.size());
                              
      assertEquals(50, queue2.memoryRefCount());
      
      assertEquals(0, queue2.downCacheCount());
      
      assertTrue(queue2.isPaging());      
      
      assertEquals(0, queue2.memoryDeliveryCount());
            
      //Msgs
      
      assertEquals(100, ms.size());
      
      msgIds = getMessageIds();
      assertEquals(50, msgIds.size());
      
      
      // Add 50 more
      tx = tr.createTransaction();
      for (int i = 100; i < 150; i++)
      {
         msgs[i] = MessageFactory.createCoreMessage(i, false, null);
         
         refs1[i] = ms.reference(msgs[i]);
                
         queue1.handle(null, refs1[i], tx); 
         
         refs1[i].releaseMemoryReference();
         
         refs2[i] = ms.reference(msgs[i]);
         
         queue2.handle(null, refs2[i], tx); 
         
         refs2[i].releaseMemoryReference();
      }
      tx.commit();
      
      //Queue1
      refIds = getReferenceIds(queue1.getChannelID());
                
      assertEquals(50, refIds.size());
                                    
      assertEquals(100, queue1.memoryRefCount());
      
      assertEquals(0, queue1.downCacheCount());
      
      assertTrue(queue1.isPaging());      
      
      assertEquals(0, queue1.memoryDeliveryCount());
      
      //Queue2
      
      refIds = getReferenceIds(queue2.getChannelID());
      assertEquals(100, refIds.size());
                              
      assertEquals(50, queue2.memoryRefCount());
      
      assertEquals(0, queue2.downCacheCount());
      
      assertTrue(queue2.isPaging());      
      
      assertEquals(0, queue2.memoryDeliveryCount());
            
      //Msgs
      
      assertEquals(100, ms.size());
      
      msgIds = getMessageIds();
      assertEquals(100, msgIds.size());
      
      //    Remove 100 then cancel
      this.cancelDeliveries(queue1, 100);
      
      this.cancelDeliveries(queue2, 100);
      
      //Now consume them all
      
      this.consumeInTx(queue1, 0, refs1, 150);
       
      this.consumeInTx(queue2, 0, refs2, 150);
      
      //    Queue1
      refIds = getReferenceIds(queue1.getChannelID());
                
      assertEquals(0, refIds.size());
                                    
      assertEquals(0, queue1.memoryRefCount());
      
      assertEquals(0, queue1.downCacheCount());
      
      assertFalse(queue1.isPaging());      
      
      assertEquals(0, queue1.memoryDeliveryCount());
      
      //Queue2
      
      refIds = getReferenceIds(queue2.getChannelID());
      assertEquals(0, refIds.size());
                              
      assertEquals(0, queue2.memoryRefCount());
      
      assertEquals(0, queue2.downCacheCount());
      
      assertFalse(queue2.isPaging());      
      
      assertEquals(0, queue2.memoryDeliveryCount());
            
      //Msgs
      
      assertEquals(0, ms.size());
      
      msgIds = getMessageIds();
      assertEquals(0, msgIds.size());
      
      //Should be none left
      
      assertEquals(0, queue1.messageCount());
      
      assertEquals(0, queue2.messageCount());
      
      assertEquals(0, LockMap.instance.getSize());
      
   }

}

