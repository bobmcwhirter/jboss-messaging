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
package org.jboss.test.messaging.core.plugin.postoffice.cluster;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.messaging.core.Channel;
import org.jboss.messaging.core.Delivery;
import org.jboss.messaging.core.DeliveryObserver;
import org.jboss.messaging.core.Receiver;
import org.jboss.messaging.core.SimpleDelivery;
import org.jboss.messaging.core.message.Message;
import org.jboss.messaging.core.message.MessageReference;
import org.jboss.messaging.core.plugin.postoffice.cluster.DefaultClusteredPostOffice;
import org.jboss.messaging.core.plugin.postoffice.cluster.DefaultMessagePullPolicy;
import org.jboss.messaging.core.plugin.postoffice.cluster.LocalClusteredQueue;
import org.jboss.messaging.core.tx.Transaction;
import org.jboss.test.messaging.core.SimpleCondition;
import org.jboss.test.messaging.core.SimpleReceiver;
import org.jboss.test.messaging.core.plugin.base.PostOfficeTestBase;
import org.jboss.test.messaging.util.CoreMessageFactory;

import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.QueuedExecutor;

/**
 * A RedistributionWithDefaultMessagePullPolicyTest
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:ovidiu@jboss.org">Oviidu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class RedistributionWithDefaultMessagePullPolicyTest extends PostOfficeTestBase
{
   // Constants ------------------------------------------------------------------------------------

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   // Constructors ---------------------------------------------------------------------------------

   public RedistributionWithDefaultMessagePullPolicyTest(String name)
   {
      super(name);
   }

   // Public ---------------------------------------------------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();
   }

   public void tearDown() throws Exception
   {
      super.tearDown();
   }

   public void testConsumeAllNonPersistentNonRecoverable() throws Throwable
   {
      consumeAll(false, false);
   }

   public void testConsumeAllPersistentNonRecoverable() throws Throwable
   {
      consumeAll(true, false);
   }

   public void testConsumeAllNonPersistentRecoverable() throws Throwable
   {
      consumeAll(false, true);
   }

   public void testConsumeAllPersistentRecoverable() throws Throwable
   {
      consumeAll(true, true);
   }

   public void testConsumeBitByBitNonPersistentNonRecoverable() throws Throwable
   {
      consumeBitByBit(false, false);
   }

   public void testConsumeBitByBitPersistentNonRecoverable() throws Throwable
   {
      consumeBitByBit(true, false);
   }

   public void testConsumeBitByBitNonPersistentRecoverable() throws Throwable
   {
      consumeBitByBit(false, true);
   }

   public void testConsumeBitByBitPersistentRecoverable() throws Throwable
   {
      consumeBitByBit(true, true);
   }

   public void testSimpleMessagePull() throws Throwable
   {
      DefaultClusteredPostOffice office1 = null;
      DefaultClusteredPostOffice office2 = null;

      try
      {
         office1 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(1, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         office2 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(2, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         LocalClusteredQueue queue1 =
            new LocalClusteredQueue(office1, 1, "queue1", channelIDManager.getID(), ms, pm,
                                    true, true, -1, null, tr);

         office1.bindClusteredQueue(new SimpleCondition("queue1"), queue1);

         LocalClusteredQueue queue2 =
            new LocalClusteredQueue(office2, 2, "queue1", channelIDManager.getID(), ms, pm,
                                    true, true, -1, null, tr);
         office2.bindClusteredQueue(new SimpleCondition("queue1"), queue2);

         Message msg = CoreMessageFactory.createCoreMessage(1, true, null);

         MessageReference ref = ms.reference(msg);

         office1.route(ref, new SimpleCondition("queue1"), null);

         Thread.sleep(2000);

         // Messages should all be in queue1

         List msgs = queue1.browse();
         assertEquals(1, msgs.size());

         msgs = queue2.browse();
         assertTrue(msgs.isEmpty());

         SimpleReceiver receiver1 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
         receiver1.setMaxRefs(0);
         queue1.add(receiver1);
         SimpleReceiver receiver2 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
         receiver2.setMaxRefs(0);
         queue2.add(receiver2);

         // Prompt delivery so the channels know if the receivers are ready
         queue1.deliver();
         Thread.sleep(2000);

         // Pull from 1 to 2

         receiver2.setMaxRefs(1);

         log.trace("delivering");
         queue2.deliver();

         Thread.sleep(3000);

         assertTrue(office1.getHoldingTransactions().isEmpty());
         assertTrue(office2.getHoldingTransactions().isEmpty());

         log.trace("r2 " + receiver2.getMessages().size());

         log.trace("queue1 refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
         log.trace("queue2 refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());

         assertEquals(0, queue1.memoryRefCount());
         assertEquals(0, queue1.getDeliveringCount());

         assertEquals(0, queue2.memoryRefCount());
         assertEquals(1, queue2.getDeliveringCount());

         this.acknowledgeAll(receiver2);

         assertEquals(0, queue2.memoryRefCount());
         assertEquals(0, queue2.getDeliveringCount());

         assertTrue(office1.getHoldingTransactions().isEmpty());
         assertTrue(office2.getHoldingTransactions().isEmpty());

      }
      finally
      {
         if (office1 != null)
         {
            office1.stop();
         }

         if (office2 != null)
         {
            office2.stop();
         }
      }
   }

// Commented because of http://jira.jboss.com/jira/browse/JBMESSAGING-972
//   public void testSimpleMessagePullCrashBeforeCommit() throws Throwable
//   {
//      DefaultClusteredPostOffice office1 = null;
//      DefaultClusteredPostOffice office2 = null;
//
//      try
//      {
//         office1 = (DefaultClusteredPostOffice)
//            createClusteredPostOffice(1, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
//                                      sc, ms, pm, tr);
//
//         office2 = (DefaultClusteredPostOffice)
//            createClusteredPostOffice(2, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
//                                      sc, ms, pm, tr);
//
//         LocalClusteredQueue queue1 =
//            new LocalClusteredQueue(office1, 1, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, true, -1, null, tr);
//         office1.bindClusteredQueue(new SimpleCondition("queue1"), queue1);
//
//         LocalClusteredQueue queue2 =
//            new LocalClusteredQueue(office2, 2, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, true, -1, null, tr);
//         office2.bindClusteredQueue(new SimpleCondition("queue1"), queue2);
//
//         Message msg = CoreMessageFactory.createCoreMessage(1, true, null);
//
//         MessageReference ref = ms.reference(msg);
//
//         office1.route(ref, new SimpleCondition("queue1"), null);
//
//         Thread.sleep(2000);
//
//         //Messages should all be in queue1
//
//         List msgs = queue1.browse();
//         assertEquals(1, msgs.size());
//
//         msgs = queue2.browse();
//         assertTrue(msgs.isEmpty());
//
//         SimpleReceiver receiver1 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
//         receiver1.setMaxRefs(0);
//         queue1.add(receiver1);
//         SimpleReceiver receiver2 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
//         receiver2.setMaxRefs(0);
//         queue2.add(receiver2);
//
//         //Prompt delivery so the channels know if the receivers are ready
//         queue1.deliver();
//         Thread.sleep(2000);
//
//         //Pull from 1 to 2
//
//         receiver2.setMaxRefs(1);
//
//         //Force a failure before commit
//         office2.setFail(true, false, false);
//
//         log.trace("delivering");
//         queue2.deliver();
//
//         Thread.sleep(3000);
//
//         assertEquals(1, office1.getHoldingTransactions().size());
//         assertTrue(office2.getHoldingTransactions().isEmpty());
//
//         log.trace("queue1 refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
//         log.trace("queue2 refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());
//
//         assertEquals(0, queue1.memoryRefCount());
//         assertEquals(1, queue1.getDeliveringCount());
//
//         assertEquals(0, queue2.memoryRefCount());
//         assertEquals(0, queue2.getDeliveringCount());
//
//         //Now kill office 2 - this should cause office1 to remove the dead held transaction
//
//         office2.stop();
//         Thread.sleep(2000);
//
//         assertTrue(office1.getHoldingTransactions().isEmpty());
//
//         //The delivery should be cancelled back to the queue too
//
//         assertEquals(1, queue1.memoryRefCount());
//         assertEquals(0, queue1.getDeliveringCount());
//
//
//      }
//      finally
//      {
//         if (office1 != null)
//         {
//            office1.stop();
//         }
//
//         if (office2 != null)
//         {
//            office2.stop();
//         }
//      }
//   }
//
   
// Commented because of http://jira.jboss.com/jira/browse/JBMESSAGING-972   
//   public void testSimpleMessagePullCrashAfterCommit() throws Throwable
//   {
//      DefaultClusteredPostOffice office1 = null;
//      DefaultClusteredPostOffice office2 = null;
//
//      try
//      {
//         office1 = (DefaultClusteredPostOffice)
//            createClusteredPostOffice(1, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
//                                      sc, ms, pm, tr);
//
//         office2 = (DefaultClusteredPostOffice)
//            createClusteredPostOffice(2, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
//                                      sc, ms, pm, tr);
//
//         LocalClusteredQueue queue1 =
//            new LocalClusteredQueue(office1, 1, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, true, -1, null, tr);
//         office1.bindClusteredQueue(new SimpleCondition("queue1"), queue1);
//
//         LocalClusteredQueue queue2 =
//            new LocalClusteredQueue(office2, 2, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, true, -1, null, tr);
//         office2.bindClusteredQueue(new SimpleCondition("queue1"), queue2);
//
//         Message msg = CoreMessageFactory.createCoreMessage(1, true, null);
//
//         MessageReference ref = ms.reference(msg);
//
//         office1.route(ref, new SimpleCondition("queue1"), null);
//
//         Thread.sleep(2000);
//
//         //Messages should all be in queue1
//
//         List msgs = queue1.browse();
//         assertEquals(1, msgs.size());
//
//         msgs = queue2.browse();
//         assertTrue(msgs.isEmpty());
//
//         SimpleReceiver receiver1 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
//         receiver1.setMaxRefs(0);
//         queue1.add(receiver1);
//         SimpleReceiver receiver2 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
//         receiver2.setMaxRefs(0);
//         queue2.add(receiver2);
//
//         //Prompt delivery so the channels know if the receivers are ready
//         queue1.deliver();
//         Thread.sleep(2000);
//
//         //Pull from 1 to 2
//
//         receiver2.setMaxRefs(1);
//
//         //Force a failure after commit the ack to storage
//         office2.setFail(false, true, false);
//
//         log.trace("delivering");
//         queue2.deliver();
//
//         Thread.sleep(3000);
//
//         assertEquals(1, office1.getHoldingTransactions().size());
//         assertTrue(office2.getHoldingTransactions().isEmpty());
//
//         log.trace("queue1 refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
//         log.trace("queue2 refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());
//
//         assertEquals(0, queue1.memoryRefCount());
//         assertEquals(1, queue1.getDeliveringCount());
//
//         //Now kill office 2 - this should cause office1 to remove the dead held transaction
//
//         office2.stop();
//         Thread.sleep(2000);
//
//         assertTrue(office1.getHoldingTransactions().isEmpty());
//
//         //The delivery should be committed
//
//         assertEquals(0, queue1.memoryRefCount());
//         assertEquals(0, queue1.getDeliveringCount());
//
//      }
//      finally
//      {
//         if (office1 != null)
//         {
//            office1.stop();
//         }
//
//         if (office2 != null)
//         {
//            office2.stop();
//         }
//      }
//   }

// Commented because of http://jira.jboss.com/jira/browse/JBMESSAGING-972    
//   public void testFailHandleMessagePullResult() throws Throwable
//   {
//      DefaultClusteredPostOffice office1 = null;
//      DefaultClusteredPostOffice office2 = null;
//
//      try
//      {
//         office1 = (DefaultClusteredPostOffice)
//            createClusteredPostOffice(1, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
//                                      sc, ms, pm, tr);
//
//         office2 = (DefaultClusteredPostOffice)
//            createClusteredPostOffice(2, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
//                                      sc, ms, pm, tr);
//
//         LocalClusteredQueue queue1 =
//            new LocalClusteredQueue(office1, 1, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, true, -1, null, tr);
//         office1.bindClusteredQueue(new SimpleCondition("queue1"), queue1);
//
//         LocalClusteredQueue queue2 =
//            new LocalClusteredQueue(office2, 2, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, true, -1, null, tr);
//         office2.bindClusteredQueue(new SimpleCondition("queue1"), queue2);
//
//         Message msg = CoreMessageFactory.createCoreMessage(1, true, null);
//
//         MessageReference ref = ms.reference(msg);
//
//         office1.route(ref, new SimpleCondition("queue1"), null);
//
//         Thread.sleep(2000);
//
//         //Messages should all be in queue1
//
//         List msgs = queue1.browse();
//         assertEquals(1, msgs.size());
//
//         msgs = queue2.browse();
//         assertTrue(msgs.isEmpty());
//
//         SimpleReceiver receiver1 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
//         receiver1.setMaxRefs(0);
//         queue1.add(receiver1);
//         SimpleReceiver receiver2 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
//         receiver2.setMaxRefs(0);
//         queue2.add(receiver2);
//
//         //Prompt delivery so the channels know if the receivers are ready
//         queue1.deliver();
//         Thread.sleep(2000);
//
//         //Pull from 1 to 2
//
//         receiver2.setMaxRefs(1);
//
//         office2.setFail(false, false, true);
//
//         log.trace("delivering");
//         queue2.deliver();
//
//         Thread.sleep(3000);
//
//         //The delivery should be rolled back
//
//         assertTrue(office2.getHoldingTransactions().isEmpty());
//         assertTrue(office2.getHoldingTransactions().isEmpty());
//
//         log.trace("queue1 refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
//         log.trace("queue2 refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());
//
//         assertEquals(1, queue1.memoryRefCount());
//         assertEquals(0, queue1.getDeliveringCount());
//
//         assertEquals(0, queue2.memoryRefCount());
//         assertEquals(0, queue2.getDeliveringCount());
//      }
//      finally
//      {
//         if (office1 != null)
//         {
//            office1.stop();
//         }
//
//         if (office2 != null)
//         {
//            office2.stop();
//         }
//      }
//   }

   protected void consumeAll(boolean persistent, boolean recoverable) throws Throwable
   {
      DefaultClusteredPostOffice office1 = null;
      DefaultClusteredPostOffice office2 = null;
      DefaultClusteredPostOffice office3 = null;
      DefaultClusteredPostOffice office4 = null;
      DefaultClusteredPostOffice office5 = null;

      boolean readOK;

      try
      {
         log.trace("Creating post offices");

         office1 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(1, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         office2 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(2, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         office3 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(3, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         office4 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(4, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         office5 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(5, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         log.trace("Created postoffices");

         LocalClusteredQueue queue1 =
            new LocalClusteredQueue(office1, 1, "queue1", channelIDManager.getID(), ms, pm,
                                    true, recoverable, -1, null, tr);
         office1.bindClusteredQueue(new SimpleCondition("queue1"), queue1);

         LocalClusteredQueue queue2 =
            new LocalClusteredQueue(office2, 2, "queue1", channelIDManager.getID(), ms, pm,
                                    true, recoverable, -1, null, tr);
         office2.bindClusteredQueue(new SimpleCondition("queue1"), queue2);

         LocalClusteredQueue queue3 =
            new LocalClusteredQueue(office3, 3, "queue1", channelIDManager.getID(), ms, pm,
                                    true, recoverable, -1, null, tr);
         office3.bindClusteredQueue(new SimpleCondition("queue1"), queue3);

         LocalClusteredQueue queue4 =
            new LocalClusteredQueue(office4, 4, "queue1", channelIDManager.getID(), ms, pm,
                                    true, recoverable, -1, null, tr);
         office4.bindClusteredQueue(new SimpleCondition("queue1"), queue4);

         LocalClusteredQueue queue5 =
            new LocalClusteredQueue(office5, 5, "queue1", channelIDManager.getID(), ms, pm,
                                    true, recoverable, -1, null, tr);
         office5.bindClusteredQueue(new SimpleCondition("queue1"), queue5);

         log.trace("Created and bound queues");

         final int NUM_MESSAGES = 100;

         log.trace("sending messages");
         this.sendMessages("queue1", persistent, office1, NUM_MESSAGES, null);
         this.sendMessages("queue1", persistent, office2, NUM_MESSAGES, null);
         this.sendMessages("queue1", persistent, office3, NUM_MESSAGES, null);
         this.sendMessages("queue1", persistent, office4, NUM_MESSAGES, null);
         this.sendMessages("queue1", persistent, office5, NUM_MESSAGES, null);

         log.trace("sent messages");

         Thread.sleep(2000);


         log.trace("Finished small sleep");

         //Check the sizes

         log.trace("Here are the sizes:");
         log.trace("queue1, refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
         log.trace("queue2, refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());
         log.trace("queue3, refs:" + queue3.memoryRefCount() + " dels:" + queue3.getDeliveringCount());
         log.trace("queue4, refs:" + queue4.memoryRefCount() + " dels:" + queue4.getDeliveringCount());
         log.trace("queue5, refs:" + queue5.memoryRefCount() + " dels:" + queue5.getDeliveringCount());

         assertEquals(NUM_MESSAGES, queue1.memoryRefCount());
         assertEquals(0, queue1.getDeliveringCount());

         assertEquals(NUM_MESSAGES, queue2.memoryRefCount());
         assertEquals(0, queue2.getDeliveringCount());

         assertEquals(NUM_MESSAGES, queue3.memoryRefCount());
         assertEquals(0, queue3.getDeliveringCount());

         assertEquals(NUM_MESSAGES, queue4.memoryRefCount());
         assertEquals(0, queue4.getDeliveringCount());

         assertEquals(NUM_MESSAGES, queue5.memoryRefCount());
         assertEquals(0, queue5.getDeliveringCount());

         log.trace("Creating receiver");

         SimpleReceiver receiver = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING);

         log.trace("Created receiver");

         queue1.add(receiver);

         log.trace("Added receiver");

         queue1.deliver();

         log.trace("Called deliver");

         log.trace("Waiting for handleInvocations");
         long start = System.currentTimeMillis();
         readOK = receiver.waitForHandleInvocations(NUM_MESSAGES * 5, 60000);
         long end = System.currentTimeMillis();
         log.trace("I waited for " + (end - start) + " ms");

         assertTrue(readOK);

         Thread.sleep(2000);


         log.trace("Here are the sizes:");
         log.trace("queue1, refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
         log.trace("queue2, refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());
         log.trace("queue3, refs:" + queue3.memoryRefCount() + " dels:" + queue3.getDeliveringCount());
         log.trace("queue4, refs:" + queue4.memoryRefCount() + " dels:" + queue4.getDeliveringCount());
         log.trace("queue5, refs:" + queue5.memoryRefCount() + " dels:" + queue5.getDeliveringCount());

         assertEquals(0, queue1.memoryRefCount());
         assertEquals(NUM_MESSAGES * 5, queue1.getDeliveringCount());

         assertEquals(0, queue2.memoryRefCount());
         assertEquals(0, queue2.getDeliveringCount());

         assertEquals(0, queue3.memoryRefCount());
         assertEquals(0, queue3.getDeliveringCount());

         assertEquals(0, queue4.memoryRefCount());
         assertEquals(0, queue4.getDeliveringCount());

         assertEquals(0, queue5.memoryRefCount());
         assertEquals(0, queue5.getDeliveringCount());

         List messages = receiver.getMessages();

         assertNotNull(messages);

         assertEquals(NUM_MESSAGES * 5, messages.size());

         Iterator iter = messages.iterator();

         log.trace("Acknowledging messages");

         while (iter.hasNext())
         {
            Message msg = (Message) iter.next();

            receiver.acknowledge(msg, null);
         }

         log.trace("Acknowledged messages");

         receiver.clear();

         assertEquals(0, queue1.memoryRefCount());
         assertEquals(0, queue1.getDeliveringCount());

         assertTrue(office1.getHoldingTransactions().isEmpty());
         assertTrue(office2.getHoldingTransactions().isEmpty());
         assertTrue(office3.getHoldingTransactions().isEmpty());
         assertTrue(office4.getHoldingTransactions().isEmpty());
         assertTrue(office5.getHoldingTransactions().isEmpty());

         if (checkNoMessageData())
         {
            fail("Message data still in database");
         }
      }
      finally
      {
         if (office1 != null)
         {
            office1.stop();
         }

         if (office2 != null)
         {
            office2.stop();
         }

         if (office3 != null)
         {
            office3.stop();
         }

         if (office4 != null)
         {
            office4.stop();
         }

         if (office5 != null)
         {
            office5.stop();
         }
      }
   }

   protected void consumeBitByBit(boolean persistent, boolean recoverable) throws Throwable
   {
      DefaultClusteredPostOffice office1 = null;
      DefaultClusteredPostOffice office2 = null;
      DefaultClusteredPostOffice office3 = null;
      DefaultClusteredPostOffice office4 = null;
      DefaultClusteredPostOffice office5 = null;

      boolean readOK;

      try
      {
         office1 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(1, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         office2 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(2, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         office3 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(3, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         office4 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(4, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         office5 = (DefaultClusteredPostOffice)
            createClusteredPostOffice(5, "testgroup", 10000, 10000, new DefaultMessagePullPolicy(),
                                      sc, ms, pm, tr);

         LocalClusteredQueue queue1 =
            new LocalClusteredQueue(office1, 1, "queue1", channelIDManager.getID(), ms, pm,
                                    true, recoverable, -1, null, tr);
         office1.bindClusteredQueue(new SimpleCondition("queue1"), queue1);

         LocalClusteredQueue queue2 =
            new LocalClusteredQueue(office2, 2, "queue1", channelIDManager.getID(), ms, pm,
                                    true, recoverable, -1, null, tr);
         office2.bindClusteredQueue(new SimpleCondition("queue1"), queue2);

         LocalClusteredQueue queue3 =
            new LocalClusteredQueue(office3, 3, "queue1", channelIDManager.getID(), ms, pm,
                                    true, recoverable, -1, null, tr);
         office3.bindClusteredQueue(new SimpleCondition("queue1"), queue3);

         LocalClusteredQueue queue4 =
            new LocalClusteredQueue(office4, 4, "queue1", channelIDManager.getID(), ms, pm,
                                    true, recoverable, -1, null, tr);
         office4.bindClusteredQueue(new SimpleCondition("queue1"), queue4);

         LocalClusteredQueue queue5 =
            new LocalClusteredQueue(office5, 5, "queue1", channelIDManager.getID(), ms, pm,
                                    true, recoverable, -1, null, tr);
         office5.bindClusteredQueue(new SimpleCondition("queue1"), queue5);

         final int NUM_MESSAGES = 100;

         this.sendMessages("queue1", persistent, office1, NUM_MESSAGES, null);
         this.sendMessages("queue1", persistent, office2, NUM_MESSAGES, null);
         this.sendMessages("queue1", persistent, office3, NUM_MESSAGES, null);
         this.sendMessages("queue1", persistent, office4, NUM_MESSAGES, null);
         this.sendMessages("queue1", persistent, office5, NUM_MESSAGES, null);

         Thread.sleep(2000);

         // Check the sizes

         log.trace("Here are the sizes 1:");
         log.trace("queue1, refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
         log.trace("queue2, refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());
         log.trace("queue3, refs:" + queue3.memoryRefCount() + " dels:" + queue3.getDeliveringCount());
         log.trace("queue4, refs:" + queue4.memoryRefCount() + " dels:" + queue4.getDeliveringCount());
         log.trace("queue5, refs:" + queue5.memoryRefCount() + " dels:" + queue5.getDeliveringCount());

         assertEquals(NUM_MESSAGES, queue1.memoryRefCount());
         assertEquals(0, queue1.getDeliveringCount());

         assertEquals(NUM_MESSAGES, queue2.memoryRefCount());
         assertEquals(0, queue2.getDeliveringCount());

         assertEquals(NUM_MESSAGES, queue3.memoryRefCount());
         assertEquals(0, queue3.getDeliveringCount());

         assertEquals(NUM_MESSAGES, queue4.memoryRefCount());
         assertEquals(0, queue4.getDeliveringCount());

         assertEquals(NUM_MESSAGES, queue5.memoryRefCount());
         assertEquals(0, queue5.getDeliveringCount());

         assertTrue(office1.getHoldingTransactions().isEmpty());
         assertTrue(office2.getHoldingTransactions().isEmpty());
         assertTrue(office3.getHoldingTransactions().isEmpty());
         assertTrue(office4.getHoldingTransactions().isEmpty());
         assertTrue(office5.getHoldingTransactions().isEmpty());

         SimpleReceiver receiver1 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
         queue1.add(receiver1);
         SimpleReceiver receiver2 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
         queue2.add(receiver2);
         SimpleReceiver receiver3 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
         queue3.add(receiver3);
         SimpleReceiver receiver4 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
         queue4.add(receiver4);
         SimpleReceiver receiver5 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING_TO_MAX);
         queue5.add(receiver5);

         receiver1.setMaxRefs(5);
         queue1.deliver();
         readOK = receiver1.waitForHandleInvocations(5, 20000);
         assertTrue(readOK);
         Thread.sleep(1000);
         assertEquals(NUM_MESSAGES - 5, queue1.memoryRefCount());
         assertEquals(5, queue1.getDeliveringCount());

         acknowledgeAll(receiver1);
         assertEquals(0, queue1.getDeliveringCount());
         receiver1.setMaxRefs(0);

         receiver2.setMaxRefs(10);
         queue2.deliver();
         readOK = receiver2.waitForHandleInvocations(10, 20000);
         assertTrue(readOK);
         Thread.sleep(1000);
         assertEquals(NUM_MESSAGES - 10, queue2.memoryRefCount());
         assertEquals(10, queue2.getDeliveringCount());
         acknowledgeAll(receiver2);
         receiver2.setMaxRefs(0);

         receiver3.setMaxRefs(15);
         queue3.deliver();
         readOK = receiver3.waitForHandleInvocations(15, 20000);
         Thread.sleep(1000);
         assertEquals(NUM_MESSAGES - 15, queue3.memoryRefCount());
         assertEquals(15, queue3.getDeliveringCount());
         acknowledgeAll(receiver3);
         receiver3.setMaxRefs(0);

         receiver4.setMaxRefs(20);
         queue4.deliver();
         readOK = receiver4.waitForHandleInvocations(20, 20000);
         assertTrue(readOK);
         Thread.sleep(1000);
         assertEquals(NUM_MESSAGES - 20, queue4.memoryRefCount());
         assertEquals(20, queue4.getDeliveringCount());
         acknowledgeAll(receiver4);
         receiver4.setMaxRefs(0);

         receiver5.setMaxRefs(25);
         queue5.deliver();
         readOK = receiver5.waitForHandleInvocations(25, 20000);
         assertTrue(readOK);
         Thread.sleep(1000);
         assertEquals(NUM_MESSAGES - 25, queue5.memoryRefCount());
         assertEquals(25, queue5.getDeliveringCount());
         acknowledgeAll(receiver5);
         receiver5.setMaxRefs(0);

         assertTrue(office1.getHoldingTransactions().isEmpty());
         assertTrue(office2.getHoldingTransactions().isEmpty());
         assertTrue(office3.getHoldingTransactions().isEmpty());
         assertTrue(office4.getHoldingTransactions().isEmpty());
         assertTrue(office5.getHoldingTransactions().isEmpty());

         log.trace("Here are the sizes 2:");
         log.trace("queue1, refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
         log.trace("queue2, refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());
         log.trace("queue3, refs:" + queue3.memoryRefCount() + " dels:" + queue3.getDeliveringCount());
         log.trace("queue4, refs:" + queue4.memoryRefCount() + " dels:" + queue4.getDeliveringCount());
         log.trace("queue5, refs:" + queue5.memoryRefCount() + " dels:" + queue5.getDeliveringCount());

         // Consume the rest from queue 5
         receiver5.setMaxRefs(NUM_MESSAGES - 25);
         queue5.deliver();
         readOK = receiver5.waitForHandleInvocations(NUM_MESSAGES - 25, 20000);
         assertTrue(readOK);

         Thread.sleep(2000);

         log.trace("receiver5 msgs:" + receiver5.getMessages().size());

         log.trace("Here are the sizes 3:");
         log.trace("queue1, refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
         log.trace("queue2, refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());
         log.trace("queue3, refs:" + queue3.memoryRefCount() + " dels:" + queue3.getDeliveringCount());
         log.trace("queue4, refs:" + queue4.memoryRefCount() + " dels:" + queue4.getDeliveringCount());
         log.trace("queue5, refs:" + queue5.memoryRefCount() + " dels:" + queue5.getDeliveringCount());

         // This will result in an extra one being pulled from queue1 - we cannot avoid this. This
         // is because the channel does not know that the receiver is full unless it tries with a
         // ref so it needs to retrieve one

                                                                   // http://jira.jboss.org/jira/browse/JBMESSAGING-901                                                                          
         assertEquals(NUM_MESSAGES - 6, queue1.memoryRefCount());  // <-  expected:<94> but was:<93>
         assertEquals(0, queue1.getDeliveringCount());

         assertEquals(NUM_MESSAGES - 10, queue2.memoryRefCount());
         assertEquals(0, queue2.getDeliveringCount());

         assertEquals(NUM_MESSAGES - 15, queue3.memoryRefCount());
         assertEquals(0, queue3.getDeliveringCount());

         assertEquals(NUM_MESSAGES - 20, queue4.memoryRefCount());
         assertEquals(0, queue4.getDeliveringCount());

         assertEquals(1, queue5.memoryRefCount());
         assertEquals(NUM_MESSAGES - 25, queue5.getDeliveringCount());

         acknowledgeAll(receiver5);

         assertEquals(0, queue5.getDeliveringCount());

         receiver5.setMaxRefs(0);

         assertTrue(office1.getHoldingTransactions().isEmpty());
         assertTrue(office2.getHoldingTransactions().isEmpty());
         assertTrue(office3.getHoldingTransactions().isEmpty());
         assertTrue(office4.getHoldingTransactions().isEmpty());
         assertTrue(office5.getHoldingTransactions().isEmpty());

         // Now consume 5 more from queue5, they should come from queue1 which has the most messages

         log.trace("Consume 5 more from queue 5");

         receiver5.setMaxRefs(5);
         queue5.deliver();
         readOK = receiver5.waitForHandleInvocations(5, 20000);
         assertTrue(readOK);

         Thread.sleep(4000);

         log.trace("Here are the sizes 4:");
         log.trace("queue1, refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
         log.trace("queue2, refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());
         log.trace("queue3, refs:" + queue3.memoryRefCount() + " dels:" + queue3.getDeliveringCount());
         log.trace("queue4, refs:" + queue4.memoryRefCount() + " dels:" + queue4.getDeliveringCount());
         log.trace("queue5, refs:" + queue5.memoryRefCount() + " dels:" + queue5.getDeliveringCount());

         assertEquals(NUM_MESSAGES - 11, queue1.memoryRefCount()); // <--  expected:<89> but was:<88>

         assertEquals(0, queue1.getDeliveringCount());

         assertEquals(NUM_MESSAGES - 10, queue2.memoryRefCount());
         assertEquals(0, queue2.getDeliveringCount());

         assertEquals(NUM_MESSAGES - 15, queue3.memoryRefCount());
         assertEquals(0, queue3.getDeliveringCount());

         assertEquals(NUM_MESSAGES - 20, queue4.memoryRefCount());
         assertEquals(0, queue4.getDeliveringCount());

         assertEquals(1, queue5.memoryRefCount());
         assertEquals(5, queue5.getDeliveringCount());

         acknowledgeAll(receiver5);

         assertEquals(0, queue5.getDeliveringCount());

         receiver1.setMaxRefs(0);

         assertTrue(office1.getHoldingTransactions().isEmpty());
         assertTrue(office2.getHoldingTransactions().isEmpty());
         assertTrue(office3.getHoldingTransactions().isEmpty());
         assertTrue(office4.getHoldingTransactions().isEmpty());
         assertTrue(office5.getHoldingTransactions().isEmpty());

         // Consume 1 more - should pull one from queue2

         receiver5.setMaxRefs(1);
         queue5.deliver();
         readOK = receiver5.waitForHandleInvocations(1, 20000);
         assertTrue(readOK);

         Thread.sleep(2000);

         log.trace("Here are the sizes 5:");
         log.trace("queue1, refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
         log.trace("queue2, refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());
         log.trace("queue3, refs:" + queue3.memoryRefCount() + " dels:" + queue3.getDeliveringCount());
         log.trace("queue4, refs:" + queue4.memoryRefCount() + " dels:" + queue4.getDeliveringCount());
         log.trace("queue5, refs:" + queue5.memoryRefCount() + " dels:" + queue5.getDeliveringCount());

         assertEquals(NUM_MESSAGES - 11, queue1.memoryRefCount());
         assertEquals(0, queue1.getDeliveringCount());

         assertEquals(NUM_MESSAGES - 11, queue2.memoryRefCount());
         assertEquals(0, queue2.getDeliveringCount());

         assertEquals(NUM_MESSAGES - 15, queue3.memoryRefCount());
         assertEquals(0, queue3.getDeliveringCount());

         assertEquals(NUM_MESSAGES - 20, queue4.memoryRefCount());
         assertEquals(0, queue4.getDeliveringCount());

         assertEquals(1, queue5.memoryRefCount());
         assertEquals(1, queue5.getDeliveringCount());

         acknowledgeAll(receiver5);

         assertEquals(0, queue5.getDeliveringCount());

         receiver5.setMaxRefs(0);

         assertTrue(office1.getHoldingTransactions().isEmpty());
         assertTrue(office2.getHoldingTransactions().isEmpty());
         assertTrue(office3.getHoldingTransactions().isEmpty());
         assertTrue(office4.getHoldingTransactions().isEmpty());
         assertTrue(office5.getHoldingTransactions().isEmpty());

         // From queue 4 consume everything else

         int num = NUM_MESSAGES - 15 + NUM_MESSAGES - 20 + NUM_MESSAGES - 11 + NUM_MESSAGES - 11 + 1;
         receiver4.setMaxRefs(num);
         queue4.deliver();
         readOK = receiver4.waitForHandleInvocations(num, 20000);
         assertTrue(readOK);

         Thread.sleep(2000);

         log.trace("Here are the sizes 6:");
         log.trace("queue1, refs:" + queue1.memoryRefCount() + " dels:" + queue1.getDeliveringCount());
         log.trace("queue2, refs:" + queue2.memoryRefCount() + " dels:" + queue2.getDeliveringCount());
         log.trace("queue3, refs:" + queue3.memoryRefCount() + " dels:" + queue3.getDeliveringCount());
         log.trace("queue4, refs:" + queue4.memoryRefCount() + " dels:" + queue4.getDeliveringCount());
         log.trace("queue5, refs:" + queue5.memoryRefCount() + " dels:" + queue5.getDeliveringCount());

         assertEquals(0, queue1.memoryRefCount());
         assertEquals(0, queue1.getDeliveringCount());

         assertEquals(0, queue2.memoryRefCount());
         assertEquals(0, queue2.getDeliveringCount());

         assertEquals(0, queue3.memoryRefCount()); // <- sometimes, there is still 1 message left in this queue
         assertEquals(0, queue3.getDeliveringCount());

         assertEquals(0, queue4.memoryRefCount());
         assertEquals(NUM_MESSAGES - 15 + NUM_MESSAGES - 20 + NUM_MESSAGES - 11 + NUM_MESSAGES - 11 + 1, queue4.getDeliveringCount());

         assertEquals(0, queue5.memoryRefCount());
         assertEquals(0, queue5.getDeliveringCount());

         acknowledgeAll(receiver4);

         assertEquals(0, queue4.getDeliveringCount());

         assertTrue(office1.getHoldingTransactions().isEmpty());
         assertTrue(office2.getHoldingTransactions().isEmpty());
         assertTrue(office3.getHoldingTransactions().isEmpty());
         assertTrue(office4.getHoldingTransactions().isEmpty());
         assertTrue(office5.getHoldingTransactions().isEmpty());

         if (checkNoMessageData())
         {
            fail("Message data still in database");
         }
      }
      finally
      {
         if (office1 != null)
         {
            office1.stop();
         }

         if (office2 != null)
         {
            office2.stop();
         }

         if (office3 != null)
         {
            office3.stop();
         }

         if (office4 != null)
         {
            office4.stop();
         }

         if (office5 != null)
         {
            office5.stop();
         }
      }
   }

   class ThrottleReceiver implements Receiver, Runnable
   {
      long pause;

      volatile int totalCount;

      int count;

      int maxSize;

      volatile boolean full;

      Executor executor;

      List dels;

      Channel queue;

      int getTotalCount()
      {
         return totalCount;
      }

      ThrottleReceiver(Channel queue, long pause, int maxSize)
      {
         this.queue = queue;

         this.pause = pause;

         this.maxSize = maxSize;

         this.executor = new QueuedExecutor();

         this.dels = new ArrayList();
      }

      public Delivery handle(DeliveryObserver observer, MessageReference reference, Transaction tx)
      {
         if (full)
         {
            return null;
         }

         //log.trace(this + " got ref");

         //log.trace("cnt:" + totalCount);

         SimpleDelivery del = new SimpleDelivery(observer, reference);

         dels.add(del);

         count++;

         totalCount++;

         if (count == maxSize)
         {
            full = true;

            count = 0;

            try
            {
               executor.execute(this);
            }
            catch (InterruptedException e)
            {
               //Ignore
            }
         }

         return del;

      }

      public void run()
      {
         //Simulate processing of messages

         try
         {
            Thread.sleep(pause);
         }
         catch (InterruptedException e)
         {
            //Ignore
         }

         Iterator iter = dels.iterator();

         while (iter.hasNext())
         {
            Delivery del = (Delivery) iter.next();

            try
            {
               del.acknowledge(null);
            }
            catch (Throwable t)
            {
               //Ignore
            }
         }

         dels.clear();

         full = false;

         queue.deliver();
      }

   }

   private void acknowledgeAll(SimpleReceiver receiver) throws Throwable
   {
      List messages = receiver.getMessages();

      Iterator iter = messages.iterator();

      while (iter.hasNext())
      {
         Message msg = (Message) iter.next();

         receiver.acknowledge(msg, null);
      }

      receiver.clear();
   }


   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------

}




