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
package org.jboss.test.messaging.core.distributed.replicator.base;

import org.jboss.messaging.core.distributed.replicator.Replicator;
import org.jboss.messaging.core.distributed.replicator.ReplicatorOutput;
import org.jboss.messaging.core.distributed.replicator.ReplicatorOutputDelivery;
import org.jboss.messaging.core.message.MessageFactory;
import org.jboss.messaging.core.message.PersistentMessageStore;
import org.jboss.messaging.core.Message;
import org.jboss.messaging.core.Delivery;
import org.jboss.messaging.core.MessageStore;
import org.jboss.messaging.core.PersistenceManager;
import org.jboss.messaging.core.persistence.JDBCPersistenceManager;
import org.jboss.test.messaging.core.distributed.base.PeerTestBase;
import org.jboss.test.messaging.core.SimpleDeliveryObserver;
import org.jboss.test.messaging.core.SimpleReceiver;
import org.jboss.test.messaging.tools.jmx.ServiceContainer;

import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/**
 * The test strategy is to try as many combination as it makes sense of the following
 * variables:
 *
 * 1. The replicator may cancel an active delivery when receiving an asyncrhonous message rejection
 *    or not.
 * 2. The replicator may have zero, one or more outputs.
 * 3. There may be one or more outputs per dispatcher.
 * 4. An output may reject messages or may acknowledge them immediately, or it may acknowledge them
 *    after a while.
 * 5. Messages may be reliable or unreliable.
 * 6. One or more messages may be sent.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public abstract class ReplicatorTestBase extends PeerTestBase
{
   // Constants -----------------------------------------------------

   public static final int NUMBER_OF_MESSAGES = 10;

   // Static --------------------------------------------------------

   public static void assertValidMessage(Message original, Message m)
   {
      assertEquals(original.getMessageID(), m.getMessageID());
      assertEquals(original.getPayload(), m.getPayload());

      // TODO review these tests. The received message must behave constantly relative to these tests
//      assertNotNull(m.getHeader(Routable.REMOTE_ROUTABLE));
//      assertNull(m.getHeader(Routable.REPLICATOR_ID));
//      assertNull(m.getHeader(Routable.COLLECTOR_ID));
//      assertNull(m.getHeader(ReplicatorOutput.REPLICATOR_OUTPUT_COLLECTOR_ADDRESS));
   }


   // Attributes ----------------------------------------------------

   protected ServiceContainer sc;

   protected PersistenceManager pm, pm2;
   protected MessageStore ms2;

   protected Replicator replicator, replicator2, replicator3;

   private MessageStore outputms, outputms2, outputms3;

   // Constructors --------------------------------------------------

   public ReplicatorTestBase(String name)
   {
      super(name);
   }

   // Public --------------------------------------------------------

   public void setUp() throws Exception
   {
      sc = new ServiceContainer("all,-aop,-remoting,-security");
      sc.start();

      super.setUp();

      pm = new JDBCPersistenceManager();
      pm.start();
      pm2 = new JDBCPersistenceManager();
      pm2.start();

      ms = new PersistentMessageStore("persistent-ms", pm);
      ms2 = new PersistentMessageStore("persistent-ms2", pm2);

      // override previous definitions of distributed and distributed2
      distributed = createDistributed("test", ms, dispatcher);
      distributed2 = createDistributed("test", ms2, dispatcher2);

      peer = distributed.getPeer();
      peer2 = distributed2.getPeer();

      replicator = (Replicator)distributed;
      replicator2 = (Replicator)distributed2;
      replicator3 = (Replicator)distributed3;

      outputms = new PersistentMessageStore("persistent-outputms", pm);
      outputms2 = new PersistentMessageStore("persistent-outputms2", pm);
      outputms3 = new PersistentMessageStore("persistent-outputms3", pm);
   }

   public void tearDown() throws Exception
   {
      replicator = null;
      replicator2 = null;
      replicator3 = null;
      ms = null;
      outputms = null;
      outputms2 = null;
      outputms3 = null;
      pm.stop();
      pm = null;
      pm2.stop();
      pm2 = null;
      sc.stop();
      sc = null;
      super.tearDown();
   }

   public void testHandleReplicatorDidNotJoin() throws Exception
   {
      assertFalse(peer.hasJoined());
      assertNull(replicator.handle(null, MessageFactory.createMessage("message0"), null));
   }

   //
   // Replicator DOESN'T cancel active delivery on message rejection
   //

   ////
   //// No output
   ////

   //////
   ////// One message
   //////

   public void testReplicator_1() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      replicator.join();

      log.debug("replicator has joined");

      assertTrue(replicator.hasJoined());
      assertTrue(replicator.getOutputs().isEmpty());

      SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
      Message m = MessageFactory.createMessage("message0", true, "payload");

      Set deliveries = replicator.handle(observer, m, null);

      assertTrue(deliveries.isEmpty());

      log.info("ok");
   }

   ////
   //// One dispatcher, one output per dispatcher
   ////

   //////
   ////// No receiver
   //////

   ////////
   //////// Non-reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_2() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         log.debug("sending the message");
         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_3() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(1, deliveries.size());
            Delivery delivery = (Delivery)deliveries.iterator().next();

            assertTrue(observer.waitForAcknowledgment(delivery, 3000));

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   ////////
   //////// Reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_4() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", true, "payload");

         log.debug("sending the message");
         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_5() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, true, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(1, deliveries.size());
            Delivery delivery = (Delivery)deliveries.iterator().next();

            assertTrue(observer.waitForAcknowledgment(delivery, 3000));

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   //////
   ////// ACKING receiver
   //////

   ////////
   //////// Non-reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_6() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.ACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_7() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.ACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(1, deliveries.size());
            Delivery delivery = (Delivery)deliveries.iterator().next();

            assertTrue(observer.waitForAcknowledgment(delivery, 3000));

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   ////////
   //////// Reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_8() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.ACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", true, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_9() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.ACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, true, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(1, deliveries.size());
            Delivery delivery = (Delivery)deliveries.iterator().next();

            assertTrue(observer.waitForAcknowledgment(delivery, 3000));

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   //////
   ////// NACKING receiver
   //////

   ////////
   //////// Non-reliable message
   ////////

   //////////
   ////////// Receiver acknowledges
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_10() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.NACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertFalse(observer.waitForAcknowledgment(delivery, 3000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         receiver.acknowledge((Message)receiver.getMessages().get(0), null);

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   public void testReplicator_11() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.NACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         Delivery[] deliveries = new Delivery[NUMBER_OF_MESSAGES];
         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);

            Set dels = replicator.handle(observer, m, null);

            assertEquals(1, dels.size());
            deliveries[i] = (Delivery)dels.iterator().next();

            assertFalse(observer.waitForAcknowledgment(deliveries[i], 1000));

            assertFalse(deliveries[i].isDone());
            assertFalse(deliveries[i].isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));
         }

         // acknowledge all messages at the same time
         for(Iterator i = receiver.getMessages().iterator(); i.hasNext();)
         {
            Message rm = (Message)i.next();
            receiver.acknowledge(rm, null);
         }

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            assertTrue(observer.waitForAcknowledgment(deliveries[i], 1000));
            assertTrue(deliveries[i].isDone());
            assertFalse(deliveries[i].isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   //////////
   ////////// Receiver cancels delivery
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_12() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.NACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertFalse(observer.waitForAcknowledgment(delivery, 3000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         receiver.cancel((Message)receiver.getMessages().get(0));

         assertTrue(observer.waitForCancellation(delivery, 3000));

         assertFalse(delivery.isDone());
         assertTrue(delivery.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   public void testReplicator_13() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.NACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         Delivery[] deliveries = new Delivery[NUMBER_OF_MESSAGES];
         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);
            Set dels = replicator.handle(observer, m, null);

            assertEquals(1, dels.size());
            deliveries[i] = (Delivery)dels.iterator().next();

            assertFalse(observer.waitForAcknowledgment(deliveries[i], 1000));

            assertFalse(deliveries[i].isDone());
            assertFalse(deliveries[i].isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));
         }

         // cancel all messages
         List messages = new ArrayList();
         for(Iterator i = receiver.getMessages().iterator(); i.hasNext();)
         {
            messages.add(i.next());
         }
         for(Iterator i = messages.iterator(); i.hasNext();)
         {
            Message rm = (Message)i.next();
            receiver.cancel(rm);
         }

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            assertTrue(observer.waitForCancellation(deliveries[i], 1000));
            assertFalse(deliveries[i].isDone());
            assertTrue(deliveries[i].isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   ////////
   //////// Reliable message
   ////////

   //////////
   ////////// Receiver acknowledges
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_14() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.NACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", true, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertFalse(observer.waitForAcknowledgment(delivery, 3000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         receiver.acknowledge((Message)receiver.getMessages().get(0), null);

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   public void testReplicator_15() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.NACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         Delivery[] deliveries = new Delivery[NUMBER_OF_MESSAGES];
         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, true, "payload" + i);

            Set dels = replicator.handle(observer, m, null);

            assertEquals(1, dels.size());
            deliveries[i] = (Delivery)dels.iterator().next();

            assertFalse(observer.waitForAcknowledgment(deliveries[i], 1000));

            assertFalse(deliveries[i].isDone());
            assertFalse(deliveries[i].isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));
         }

         // acknowledge all messages at the same time
         for(Iterator i = receiver.getMessages().iterator(); i.hasNext();)
         {
            Message rm = (Message)i.next();
            receiver.acknowledge(rm, null);
         }

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            assertTrue(observer.waitForAcknowledgment(deliveries[i], 1000));
            assertTrue(deliveries[i].isDone());
            assertFalse(deliveries[i].isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   //////////
   ////////// Receiver cancels delivery
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_16() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.NACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", true, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertFalse(observer.waitForAcknowledgment(delivery, 3000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         receiver.cancel((Message)receiver.getMessages().get(0));

         assertTrue(observer.waitForCancellation(delivery, 3000));

         assertFalse(delivery.isDone());
         assertTrue(delivery.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   public void testReplicator_17() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.NACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         Delivery[] deliveries = new Delivery[NUMBER_OF_MESSAGES];
         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, true, "payload" + i);
            Set dels = replicator.handle(observer, m, null);

            assertEquals(1, dels.size());
            deliveries[i] = (Delivery)dels.iterator().next();

            assertFalse(observer.waitForAcknowledgment(deliveries[i], 1000));

            assertFalse(deliveries[i].isDone());
            assertFalse(deliveries[i].isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));
         }

         // cancel all messages
         List messages = new ArrayList();
         for(Iterator i = receiver.getMessages().iterator(); i.hasNext();)
         {
            messages.add(i.next());
         }
         for(Iterator i = messages.iterator(); i.hasNext();)
         {
            Message rm = (Message)i.next();
            receiver.cancel(rm);
         }

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            assertTrue(observer.waitForCancellation(deliveries[i], 1000));
            assertFalse(deliveries[i].isDone());
            assertTrue(deliveries[i].isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
      }
   }

   ////
   //// One dispatcher, two outputs per dispatcher
   ////

   //////
   ////// No receiver
   //////

   ////////
   //////// Non-reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_18() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, null);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(2, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         delivery = (Delivery)deliveries.iterator().next();
         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_19() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, null);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(2, deliveries.size());
            Delivery delivery = (Delivery)deliveries.iterator().next();

            observer.waitForAcknowledgment(delivery, 3000);

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            delivery = (Delivery)deliveries.iterator().next();

            observer.waitForAcknowledgment(delivery, 3000);

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////
   //////// Reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_20() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, null);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", true, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(2, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         delivery = (Delivery)deliveries.iterator().next();
         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_21() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, null);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, true, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(2, deliveries.size());
            Delivery delivery = (Delivery)deliveries.iterator().next();

            observer.waitForAcknowledgment(delivery, 3000);

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            delivery = (Delivery)deliveries.iterator().next();

            observer.waitForAcknowledgment(delivery, 3000);

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////
   ////// ACKING receiver
   //////

   ////////
   //////// Non-reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_22() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.ACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.ACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(2, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         delivery = (Delivery)deliveries.iterator().next();

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_23() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.ACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.ACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(2, deliveries.size());
            Delivery delivery = (Delivery)deliveries.iterator().next();

            assertTrue(observer.waitForAcknowledgment(delivery, 3000));

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            delivery = (Delivery)deliveries.iterator().next();

            assertTrue(observer.waitForAcknowledgment(delivery, 3000));

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));

            assertEquals(i + 1, receiver2.getMessages().size());
            assertValidMessage(m, (Message)receiver2.getMessages().get(i));
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////
   //////// Reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_24() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.ACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.ACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", true, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(2, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         delivery = (Delivery)deliveries.iterator().next();

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_25() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.ACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.ACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, true, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(2, deliveries.size());
            Delivery delivery = (Delivery)deliveries.iterator().next();

            assertTrue(observer.waitForAcknowledgment(delivery, 3000));

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            delivery = (Delivery)deliveries.iterator().next();

            assertTrue(observer.waitForAcknowledgment(delivery, 3000));

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));

            assertEquals(i + 1, receiver2.getMessages().size());
            assertValidMessage(m, (Message)receiver2.getMessages().get(i));
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////
   ////// NACKING receiver
   //////

   ////////
   //////// Non-reliable message
   ////////

   //////////
   ////////// Receiver acknowledges
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_26() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         assertFalse(observer.waitForAcknowledgment(delivery, 1000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertFalse(observer.waitForAcknowledgment(delivery2, 1000));

         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         log.debug("receiver acknowledging");
         receiver.acknowledge((Message)receiver.getMessages().get(0), null);
         assertTrue(observer.waitForAcknowledgment(delivery, 2000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         log.debug("receiver2 acknowledging");
         receiver2.acknowledge((Message)receiver2.getMessages().get(0), null);
         assertTrue(observer.waitForAcknowledgment(delivery2, 3000));

         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   public void testReplicator_27() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         final SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output =
            new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         final SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 =
            new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         Delivery[] deliveries = new Delivery[2 * NUMBER_OF_MESSAGES];
         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);

            Set dels = replicator.handle(observer, m, null);

            assertEquals(2, dels.size());
            Iterator j = dels.iterator();

            Delivery delivery = (Delivery)j.next();
            Delivery delivery2;

            if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
            {
               delivery2 = (Delivery)j.next();
            }
            else
            {
               delivery2 = delivery;
               delivery = (Delivery)j.next();
            }

            deliveries[2 * i] = delivery;
            deliveries[2 * i + 1] = delivery2;

            assertFalse(observer.waitForAcknowledgment(deliveries[2 * i], 300));

            assertFalse(deliveries[2 * i].isDone());
            assertFalse(deliveries[2 * i].isCancelled());

            assertFalse(observer.waitForAcknowledgment(deliveries[2 * i + 1], 300));

            assertFalse(deliveries[2 * i + 1].isDone());
            assertFalse(deliveries[2 * i + 1].isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));

            assertEquals(i + 1, receiver2.getMessages().size());
            assertValidMessage(m, (Message)receiver2.getMessages().get(i));
         }

         // acknowledge all messages at the same time, on two different threads

         new Thread(new Runnable()
         {
            public void run()
            {
               for(Iterator i = receiver.getMessages().iterator(); i.hasNext();)
               {
                  Message rm = (Message)i.next();
                  try
                  {
                     receiver.acknowledge(rm, null);
                  }
                  catch(Throwable t)
                  {
                     log.error("failed to acknowledge", t);
                  }
               }
            }
         }, "Acknowledging Thread 1").start();

         new Thread(new Runnable()
         {
            public void run()
            {
               for(Iterator i = receiver2.getMessages().iterator(); i.hasNext();)
               {
                  Message rm = (Message)i.next();
                  try
                  {
                     receiver2.acknowledge(rm, null);
                  }
                  catch(Throwable t)
                  {
                     log.error("failed to acknowledge", t);
                  }
               }
            }
         }, "Acknowledging Thread 2").start();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            assertTrue(observer.waitForAcknowledgment(deliveries[2 * i], 1000));
            assertTrue(deliveries[2 * i].isDone());
            assertFalse(deliveries[2 * i].isCancelled());

            assertTrue(observer.waitForAcknowledgment(deliveries[2 * i + 1], 1000));
            assertTrue(deliveries[2 * i + 1].isDone());
            assertFalse(deliveries[2 * i + 1].isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Receiver cancels delivery
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_28() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);
         log.debug("message submitted to replicator");

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         assertFalse(observer.waitForCancellation(delivery, 1000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertFalse(observer.waitForCancellation(delivery2, 1000));

         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());


         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         receiver.cancel((Message)receiver.getMessages().get(0));

         assertTrue(observer.waitForCancellation(delivery, 2000));

         assertFalse(delivery.isDone());
         assertTrue(delivery.isCancelled());

         receiver2.acknowledge((Message)receiver2.getMessages().get(0), null);

         assertTrue(observer.waitForAcknowledgment(delivery2, 2000));

         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   // TODO

   ////////
   //////// Reliable message
   ////////

   //////////
   ////////// Receiver acknowledges
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_30() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", true, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         assertFalse(observer.waitForAcknowledgment(delivery, 1000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertFalse(observer.waitForAcknowledgment(delivery2, 1000));

         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         log.debug("receiver acknowledging");
         receiver.acknowledge((Message)receiver.getMessages().get(0), null);
         assertTrue(observer.waitForAcknowledgment(delivery, 2000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         log.debug("receiver2 acknowledging");
         receiver2.acknowledge((Message)receiver2.getMessages().get(0), null);
         assertTrue(observer.waitForAcknowledgment(delivery2, 3000));

         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   public void testReplicator_31() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         final SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output =
            new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         final SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 =
            new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         Delivery[] deliveries = new Delivery[2 * NUMBER_OF_MESSAGES];
         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, true, "payload" + i);

            Set dels = replicator.handle(observer, m, null);

            assertEquals(2, dels.size());
            Iterator j = dels.iterator();

            Delivery delivery = (Delivery)j.next();
            Delivery delivery2;

            if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
            {
               delivery2 = (Delivery)j.next();
            }
            else
            {
               delivery2 = delivery;
               delivery = (Delivery)j.next();
            }

            deliveries[2 * i] = delivery;
            deliveries[2 * i + 1] = delivery2;

            assertFalse(observer.waitForAcknowledgment(deliveries[2 * i], 300));

            assertFalse(deliveries[2 * i].isDone());
            assertFalse(deliveries[2 * i].isCancelled());

            assertFalse(observer.waitForAcknowledgment(deliveries[2 * i + 1], 300));

            assertFalse(deliveries[2 * i + 1].isDone());
            assertFalse(deliveries[2 * i + 1].isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));

            assertEquals(i + 1, receiver2.getMessages().size());
            assertValidMessage(m, (Message)receiver2.getMessages().get(i));
         }

         // acknowledge all messages at the same time, on two different threads

         new Thread(new Runnable()
         {
            public void run()
            {
               for(Iterator i = receiver.getMessages().iterator(); i.hasNext();)
               {
                  Message rm = (Message)i.next();
                  try
                  {
                     receiver.acknowledge(rm, null);
                  }
                  catch(Throwable t)
                  {
                     log.error("failed to acknowledge", t);
                  }
               }
            }
         }, "Acknowledging Thread 1").start();

         new Thread(new Runnable()
         {
            public void run()
            {
               for(Iterator i = receiver2.getMessages().iterator(); i.hasNext();)
               {
                  Message rm = (Message)i.next();
                  try
                  {
                     receiver2.acknowledge(rm, null);
                  }
                  catch(Throwable t)
                  {
                     log.error("failed to acknowledge", t);
                  }
               }
            }
         }, "Acknowledging Thread 2").start();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            assertTrue(observer.waitForAcknowledgment(deliveries[2 * i], 1000));
            assertTrue(deliveries[2 * i].isDone());
            assertFalse(deliveries[2 * i].isCancelled());

            assertTrue(observer.waitForAcknowledgment(deliveries[2 * i + 1], 1000));
            assertTrue(deliveries[2 * i + 1].isDone());
            assertFalse(deliveries[2 * i + 1].isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Receiver cancels delivery
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_32() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", true, "payload");

         Set deliveries = replicator.handle(observer, m, null);
         log.debug("message submitted to replicator");

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         assertFalse(observer.waitForCancellation(delivery, 1000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertFalse(observer.waitForCancellation(delivery2, 1000));

         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());


         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         receiver.cancel((Message)receiver.getMessages().get(0));

         assertTrue(observer.waitForCancellation(delivery, 2000));

         assertFalse(delivery.isDone());
         assertTrue(delivery.isCancelled());

         receiver2.acknowledge((Message)receiver2.getMessages().get(0), null);

         assertTrue(observer.waitForAcknowledgment(delivery2, 2000));

         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   // TODO

   ////
   //// Two dispatchers, one output per dispatcher
   ////

   //////
   ////// No receiver
   //////

   ////////
   //////// Non-reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_34() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, null);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertTrue(observer.waitForAcknowledgment(delivery2, 3000));

         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_35() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, null);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(2, deliveries.size());
            Iterator j = deliveries.iterator();

            Delivery delivery = (Delivery)j.next();
            Delivery delivery2;

            if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
            {
               delivery2 = (Delivery)j.next();
            }
            else
            {
               delivery2 = delivery;
               delivery = (Delivery)j.next();
            }

            observer.waitForAcknowledgment(delivery, 3000);

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            observer.waitForAcknowledgment(delivery2, 3000);

            assertTrue(delivery2.isDone());
            assertFalse(delivery2.isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////
   //////// Reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_36() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, null);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         assertTrue(observer.waitForAcknowledgment(delivery, 3000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertTrue(observer.waitForAcknowledgment(delivery2, 3000));

         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_37() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, null);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(2, deliveries.size());
            Iterator j = deliveries.iterator();

            Delivery delivery = (Delivery)j.next();
            Delivery delivery2;

            if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
            {
               delivery2 = (Delivery)j.next();
            }
            else
            {
               delivery2 = delivery;
               delivery = (Delivery)j.next();
            }

            observer.waitForAcknowledgment(delivery, 3000);

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            observer.waitForAcknowledgment(delivery2, 3000);

            assertTrue(delivery2.isDone());
            assertFalse(delivery2.isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////
   ////// ACKING receiver
   //////

   ////////
   //////// Non-reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_38() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());
      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.ACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.ACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         observer.waitForAcknowledgment(delivery2, 3000);

         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_39() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());
      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.ACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.ACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(2, deliveries.size());
            Iterator j = deliveries.iterator();

            Delivery delivery = (Delivery)j.next();
            Delivery delivery2;

            if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
            {
               delivery2 = (Delivery)j.next();
            }
            else
            {
               delivery2 = delivery;
               delivery = (Delivery)j.next();
            }

            observer.waitForAcknowledgment(delivery, 3000);

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            observer.waitForAcknowledgment(delivery2, 3000);

            assertTrue(delivery2.isDone());
            assertFalse(delivery2.isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));

            assertEquals(i + 1, receiver2.getMessages().size());
            assertValidMessage(m, (Message)receiver2.getMessages().get(i));
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////
   //////// Reliable message
   ////////

   //////////
   ////////// One message
   //////////

   public void testReplicator_40() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());
      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.ACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.ACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         observer.waitForAcknowledgment(delivery2, 3000);

         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Multiple messages
   //////////

   public void testReplicator_41() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());
      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.ACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.ACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);

            Set deliveries = replicator.handle(observer, m, null);

            assertEquals(2, deliveries.size());
            Iterator j = deliveries.iterator();

            Delivery delivery = (Delivery)j.next();
            Delivery delivery2;

            if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
            {
               delivery2 = (Delivery)j.next();
            }
            else
            {
               delivery2 = delivery;
               delivery = (Delivery)j.next();
            }

            observer.waitForAcknowledgment(delivery, 3000);

            assertTrue(delivery.isDone());
            assertFalse(delivery.isCancelled());

            observer.waitForAcknowledgment(delivery2, 3000);

            assertTrue(delivery2.isDone());
            assertFalse(delivery2.isCancelled());

            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));

            assertEquals(i + 1, receiver2.getMessages().size());
            assertValidMessage(m, (Message)receiver2.getMessages().get(i));
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////
   ////// NACKING receiver
   //////

   ////////
   //////// Non-reliable message
   ////////

   //////////
   ////////// Receiver acknowledges
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_42() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);
         log.debug("message submitted to replicator");

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         assertFalse(observer.waitForAcknowledgment(delivery, 2000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertFalse(observer.waitForAcknowledgment(delivery2, 2000));

         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         receiver.acknowledge((Message)receiver.getMessages().get(0), null);

         assertTrue(observer.waitForAcknowledgment(delivery, 2000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());
         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         receiver2.acknowledge((Message)receiver2.getMessages().get(0), null);

         assertTrue(observer.waitForAcknowledgment(delivery2, 3000));
         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   public void testReplicator_43() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         final SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         final SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         Delivery[] deliveries = new Delivery[2 * NUMBER_OF_MESSAGES];
         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, false, "payload" + i);

            Set dels = replicator.handle(observer, m, null);

            assertEquals(2, dels.size());
            Iterator j = dels.iterator();

            Delivery delivery = (Delivery)j.next();
            Delivery delivery2;

            if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
            {
               delivery2 = (Delivery)j.next();
            }
            else
            {
               delivery2 = delivery;
               delivery = (Delivery)j.next();
            }


            deliveries[2 * i] = delivery;
            deliveries[2 * i + 1] = delivery2;

            assertFalse(observer.waitForAcknowledgment(deliveries[2 * i], 300));

            assertFalse(deliveries[2 * i].isDone());
            assertFalse(deliveries[2 * i].isCancelled());

            assertFalse(observer.waitForAcknowledgment(deliveries[2 * i + 1], 300));

            assertFalse(deliveries[2 * i + 1].isDone());
            assertFalse(deliveries[2 * i + 1].isCancelled());


            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));

            assertEquals(i + 1, receiver2.getMessages().size());
            assertValidMessage(m, (Message)receiver2.getMessages().get(i));
         }

         // acknowledge all messages at the same time, on two different threads

         new Thread(new Runnable()
         {
            public void run()
            {
               for(Iterator i = receiver.getMessages().iterator(); i.hasNext();)
               {
                  Message rm = (Message)i.next();
                  try
                  {
                     receiver.acknowledge(rm, null);
                  }
                  catch(Throwable t)
                  {
                     log.error("failed to acknowledge", t);
                  }
               }
            }
         }, "Acknowledging Thread 1").start();

         new Thread(new Runnable()
         {
            public void run()
            {
               for(Iterator i = receiver2.getMessages().iterator(); i.hasNext();)
               {
                  Message rm = (Message)i.next();
                  try
                  {
                     receiver2.acknowledge(rm, null);
                  }
                  catch(Throwable t)
                  {
                     log.error("failed to acknowledge", t);
                  }
               }
            }
         }, "Acknowledging Thread 2").start();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            assertTrue(observer.waitForAcknowledgment(deliveries[2 * i], 1000));
            assertTrue(deliveries[2 * i].isDone());
            assertFalse(deliveries[2 * i].isCancelled());
            assertTrue(observer.waitForAcknowledgment(deliveries[2 * i  + 1], 1000));
            assertTrue(deliveries[2 * i + 1].isDone());
            assertFalse(deliveries[2 * i + 1].isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Receiver cancels delivery
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_44() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);
         log.debug("message submitted to replicator");

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         assertFalse(observer.waitForCancellation(delivery, 2000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertFalse(observer.waitForCancellation(delivery2, 2000));

         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         receiver.cancel((Message)receiver.getMessages().get(0));

         assertTrue(observer.waitForCancellation(delivery, 2000));

         assertFalse(delivery.isDone());
         assertTrue(delivery.isCancelled());
         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         receiver2.acknowledge((Message)receiver2.getMessages().get(0), null);

         assertTrue(observer.waitForAcknowledgment(delivery2, 2000));

         assertFalse(delivery.isDone());
         assertTrue(delivery.isCancelled());
         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   // TODO

   ////////
   //////// Reliable message
   ////////

   //////////
   ////////// Receiver acknowledges
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_45() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", true, "payload");

         Set deliveries = replicator.handle(observer, m, null);
         log.debug("message submitted to replicator");

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         assertFalse(observer.waitForAcknowledgment(delivery, 2000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertFalse(observer.waitForAcknowledgment(delivery2, 2000));

         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         receiver.acknowledge((Message)receiver.getMessages().get(0), null);

         assertTrue(observer.waitForAcknowledgment(delivery, 2000));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());
         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         receiver2.acknowledge((Message)receiver2.getMessages().get(0), null);

         assertTrue(observer.waitForAcknowledgment(delivery2, 3000));
         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   public void testReplicator_46() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         final SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         final SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         Delivery[] deliveries = new Delivery[2 * NUMBER_OF_MESSAGES];
         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            Message m = MessageFactory.createMessage("message" + i, true, "payload" + i);

            Set dels = replicator.handle(observer, m, null);

            assertEquals(2, dels.size());
            Iterator j = dels.iterator();

            Delivery delivery = (Delivery)j.next();
            Delivery delivery2;

            if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
            {
               delivery2 = (Delivery)j.next();
            }
            else
            {
               delivery2 = delivery;
               delivery = (Delivery)j.next();
            }


            deliveries[2 * i] = delivery;
            deliveries[2 * i + 1] = delivery2;

            assertFalse(observer.waitForAcknowledgment(deliveries[2 * i], 300));

            assertFalse(deliveries[2 * i].isDone());
            assertFalse(deliveries[2 * i].isCancelled());

            assertFalse(observer.waitForAcknowledgment(deliveries[2 * i + 1], 300));

            assertFalse(deliveries[2 * i + 1].isDone());
            assertFalse(deliveries[2 * i + 1].isCancelled());


            assertEquals(i + 1, receiver.getMessages().size());
            assertValidMessage(m, (Message)receiver.getMessages().get(i));

            assertEquals(i + 1, receiver2.getMessages().size());
            assertValidMessage(m, (Message)receiver2.getMessages().get(i));
         }

         // acknowledge all messages at the same time, on two different threads

         new Thread(new Runnable()
         {
            public void run()
            {
               for(Iterator i = receiver.getMessages().iterator(); i.hasNext();)
               {
                  Message rm = (Message)i.next();
                  try
                  {
                     receiver.acknowledge(rm, null);
                  }
                  catch(Throwable t)
                  {
                     log.error("failed to acknowledge", t);
                  }
               }
            }
         }, "Acknowledging Thread 1").start();

         new Thread(new Runnable()
         {
            public void run()
            {
               for(Iterator i = receiver2.getMessages().iterator(); i.hasNext();)
               {
                  Message rm = (Message)i.next();
                  try
                  {
                     receiver2.acknowledge(rm, null);
                  }
                  catch(Throwable t)
                  {
                     log.error("failed to acknowledge", t);
                  }
               }
            }
         }, "Acknowledging Thread 2").start();

         for(int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            assertTrue(observer.waitForAcknowledgment(deliveries[2 * i], 1000));
            assertTrue(deliveries[2 * i].isDone());
            assertFalse(deliveries[2 * i].isCancelled());
            assertTrue(observer.waitForAcknowledgment(deliveries[2 * i  + 1], 1000));
            assertTrue(deliveries[2 * i + 1].isDone());
            assertFalse(deliveries[2 * i + 1].isCancelled());
         }

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   //////////
   ////////// Receiver cancels delivery
   //////////

   ////////////
   //////////// One message
   ////////////

   public void testReplicator_47() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver1", SimpleReceiver.NACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.NACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();
         Message m = MessageFactory.createMessage("message0", true, "payload");

         Set deliveries = replicator.handle(observer, m, null);
         log.debug("message submitted to replicator");

         assertEquals(2, deliveries.size());
         Iterator i = deliveries.iterator();

         Delivery delivery = (Delivery)i.next();
         Delivery delivery2;

         if (((ReplicatorOutputDelivery)delivery).getOutputID().equals(output.getID()))
         {
            delivery2 = (Delivery)i.next();
         }
         else
         {
            delivery2 = delivery;
            delivery = (Delivery)i.next();
         }

         assertFalse(observer.waitForCancellation(delivery, 2000));

         assertFalse(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertFalse(observer.waitForCancellation(delivery2, 2000));

         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m, (Message)receiver.getMessages().get(0));

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         receiver.cancel((Message)receiver.getMessages().get(0));

         assertTrue(observer.waitForCancellation(delivery, 2000));

         assertFalse(delivery.isDone());
         assertTrue(delivery.isCancelled());
         assertFalse(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         receiver2.acknowledge((Message)receiver2.getMessages().get(0), null);

         assertTrue(observer.waitForAcknowledgment(delivery2, 2000));

         assertFalse(delivery.isDone());
         assertTrue(delivery.isCancelled());
         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
      }
   }

   ////////////
   //////////// Multiple messages
   ////////////

   // TODO


   /**
    * Three outputs that return ACCEPTED, REJECTED and CANCELLED.
    *
    * Tests a non-reliable message.
    * TODO: add test for reliable message.
    *
    * @see MultipleReceiversDeliveryTestBase#testMixedAcknowledgments
    */
   public void testMixedAcknowledgments() throws Throwable
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;
      ReplicatorOutput output3 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         // rejecting output
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, null);
         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         SimpleReceiver receiver2 = new SimpleReceiver("ACKING_receiver", SimpleReceiver.ACKING);
         output2 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms2, receiver2);
         output2.join();
         log.debug("output2 has joined");

         assertTrue(output2.hasJoined());

         SimpleReceiver receiver3 = new SimpleReceiver("CANCELLING_receiver", SimpleReceiver.NACKING);
         output3 = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms3, receiver3);
         output3.join();
         log.debug("output3 has joined");

         assertTrue(output3.hasJoined());


         Set identities = replicator.getOutputs();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(output3.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(4, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(output3.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         Message m = MessageFactory.createMessage("message0", false, "payload");

         Set deliveries = replicator.handle(observer, m, null);
         log.debug("message submitted to replicator");

         assertEquals(3, deliveries.size());

         Delivery delivery = null, delivery2 = null, delivery3 = null;

         for(Iterator i = deliveries.iterator(); i.hasNext(); )
         {
            ReplicatorOutputDelivery d = (ReplicatorOutputDelivery)i.next();
            if (d.getOutputID().equals(output.getID()))
            {
               delivery = d;
            }
            else if (d.getOutputID().equals(output2.getID()))
            {
               delivery2 = d;
            }
            else if (d.getOutputID().equals(output3.getID()))
            {
               delivery3 = d;
            }
            else
            {
               fail();
            }
         }

         assertTrue(observer.waitForAcknowledgment(delivery, 500));
         assertTrue(observer.waitForAcknowledgment(delivery2, 500));
         assertFalse(observer.waitForCancellation(delivery3, 500));

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());
         assertTrue(delivery2.isDone());
         assertFalse(delivery2.isCancelled());
         assertFalse(delivery3.isDone());
         assertFalse(delivery3.isCancelled());

         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m, (Message)receiver2.getMessages().get(0));

         assertEquals(1, receiver3.getMessages().size());
         assertValidMessage(m, (Message)receiver3.getMessages().get(0));

         receiver3.cancel((Message)receiver3.getMessages().get(0));

         assertTrue(observer.waitForCancellation(delivery3, 2000));

         assertFalse(delivery3.isDone());
         assertTrue(delivery3.isCancelled());

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         output.leave();
         output2.leave();
         output3.leave();
      }
   }

   //
   // Replicator DOES cancel active delivery on message rejection
   //

   //
   // I don't use yet such a replicator, but if I need one, add corresponding tests
   //

   public void testReplicatorThatDOESCancelDeliveriesOnRejection()
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         fail("Missing tests: convert the above tests and append them to this file");
      }
   }

   //
   // Two replicator inputs
   //

   ////
   //// Two replicator inputs on the same channel, output on the same channel
   ////

   public void testTwoReplicatorInputs_1() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      ReplicatorOutput output = null;

      // create another replicator input that shares the dispatcher with the first replicator
      replicator2 = (Replicator)createDistributed((String)replicator.getReplicatorID(), ms2,
                                                  replicator.getDispatcher());

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         replicator2.join();
         log.debug("replicator2 has joined");

         assertTrue(replicator2.hasJoined());
         assertTrue(replicator2.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.ACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));
         assertTrue(identities.contains(replicator2.getPeerIdentity()));

         identities = replicator2.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator2.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));
         assertTrue(identities.contains(replicator2.getPeerIdentity()));


         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         // unreliable message; TODO add test for reliable message
         Message m1 = MessageFactory.createMessage("message1", false, "payload1");

         Set deliveries = replicator.handle(observer, m1, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m1, (Message)receiver.getMessages().get(0));

         receiver.clear();

         // unreliable message; TODO add test for reliable message
         Message m2 = MessageFactory.createMessage("message2", false, "payload2");

         deliveries = replicator2.handle(observer, m2, null);

         assertEquals(1, deliveries.size());
         delivery = (Delivery)deliveries.iterator().next();

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m2, (Message)receiver.getMessages().get(0));

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         replicator2.leave();
         output.leave();
      }
   }

   ////
   //// Two replicator inputs on the same channel, output on different channel
   ////

   public void testTwoReplicatorInputs_2() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;

      // create another replicator input that shares the dispatcher with the first replicator
      replicator2 = (Replicator)createDistributed((String)replicator.getReplicatorID(), ms2,
                                                  replicator.getDispatcher());

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         replicator2.join();
         log.debug("replicator2 has joined");

         assertTrue(replicator2.hasJoined());
         assertTrue(replicator2.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.ACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher2, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));
         assertTrue(identities.contains(replicator2.getPeerIdentity()));

         identities = replicator2.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator2.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));
         assertTrue(identities.contains(replicator2.getPeerIdentity()));


         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         // unreliable message; TODO add test for reliable message
         Message m1 = MessageFactory.createMessage("message1", false, "payload1");

         Set deliveries = replicator.handle(observer, m1, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m1, (Message)receiver.getMessages().get(0));

         receiver.clear();

         // unreliable message; TODO add test for reliable message
         Message m2 = MessageFactory.createMessage("message2", false, "payload2");

         deliveries = replicator2.handle(observer, m2, null);

         assertEquals(1, deliveries.size());
         delivery = (Delivery)deliveries.iterator().next();

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m2, (Message)receiver.getMessages().get(0));

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         replicator2.leave();
         output.leave();
      }
   }

   ////
   //// Two replicator inputs on two separated channels, output on one of the channels
   ////

   public void testTwoReplicatorInputs_3() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         replicator2.join();
         log.debug("replicator2 has joined");

         assertTrue(replicator2.hasJoined());
         assertTrue(replicator2.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.ACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));
         assertTrue(identities.contains(replicator2.getPeerIdentity()));

         identities = replicator2.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator2.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));
         assertTrue(identities.contains(replicator2.getPeerIdentity()));


         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         // unreliable message; TODO add test for reliable message
         Message m1 = MessageFactory.createMessage("message1", false, "payload1");

         Set deliveries = replicator.handle(observer, m1, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m1, (Message)receiver.getMessages().get(0));

         receiver.clear();

         // unreliable message; TODO add test for reliable message
         Message m2 = MessageFactory.createMessage("message2", false, "payload2");

         deliveries = replicator2.handle(observer, m2, null);

         assertEquals(1, deliveries.size());
         delivery = (Delivery)deliveries.iterator().next();

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m2, (Message)receiver.getMessages().get(0));

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         replicator2.leave();
         output.leave();
      }
   }

   ////
   //// Two replicator inputs on two separated channels, output on the third channel
   ////

   public void testTwoReplicatorInputs_4() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      jchannel2.connect("testGroup");
      jchannel3.connect("testGroup");

      // allow the group time to form
      Thread.sleep(2000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());
      assertTrue(jchannel3.isConnected());

      // make sure all three jchannels joined the group
      assertEquals(3, jchannel.getView().getMembers().size());
      assertEquals(3, jchannel2.getView().getMembers().size());
      assertEquals(3, jchannel3.getView().getMembers().size());

      ReplicatorOutput output = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         replicator2.join();
         log.debug("replicator2 has joined");

         assertTrue(replicator2.hasJoined());
         assertTrue(replicator2.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.ACKING);

         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher3, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));
         assertTrue(identities.contains(replicator2.getPeerIdentity()));

         identities = replicator2.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator2.getView();
         assertEquals(3, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));
         assertTrue(identities.contains(replicator2.getPeerIdentity()));


         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         // unreliable message; TODO add test for reliable message
         Message m1 = MessageFactory.createMessage("message1", false, "payload1");

         Set deliveries = replicator.handle(observer, m1, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m1, (Message)receiver.getMessages().get(0));

         receiver.clear();

         // unreliable message; TODO add test for reliable message
         Message m2 = MessageFactory.createMessage("message2", false, "payload2");

         deliveries = replicator2.handle(observer, m2, null);

         assertEquals(1, deliveries.size());
         delivery = (Delivery)deliveries.iterator().next();

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m2, (Message)receiver.getMessages().get(0));

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         replicator2.leave();
         output.leave();
      }
   }

   //
   // Two different replicators on the same channel, outputs on the same channel
   //

   public void testTwoReplicators_1() throws Exception
   {
      if (replicator.doesCancelOnMessageRejection())
      {
         // we only test replicators that do not cancel delivery on message rejection
         return;
      }

      assertTrue(jchannel.isConnected());

      // create another replicator that shares the dispatcher with the first replicator
      String replicator2ID = (String)(replicator.getReplicatorID()) + "2";
      replicator2 = (Replicator)createDistributed(replicator2ID, ms2, replicator.getDispatcher());

      ReplicatorOutput output = null;
      ReplicatorOutput output2 = null;

      try
      {
         replicator.join();
         log.debug("replicator has joined");

         assertTrue(replicator.hasJoined());
         assertTrue(replicator.getOutputs().isEmpty());

         replicator2.join();
         log.debug("replicator2 has joined");

         assertTrue(replicator2.hasJoined());
         assertTrue(replicator2.getOutputs().isEmpty());

         SimpleReceiver receiver = new SimpleReceiver("receiver0", SimpleReceiver.ACKING);
         output = new ReplicatorOutput(replicator.getReplicatorID(), dispatcher, outputms, receiver);

         output.join();
         log.debug("output has joined");

         assertTrue(output.hasJoined());

         Set identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         identities = replicator2.getOutputs();
         assertTrue(identities.isEmpty());

         identities = replicator2.getView();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(replicator2.getPeerIdentity()));

         SimpleReceiver receiver2 = new SimpleReceiver("receiver2", SimpleReceiver.ACKING);
         output2 = new ReplicatorOutput(replicator2ID, dispatcher, outputms, receiver2);

         output2.join();
         log.debug("output2 has joined");

         assertTrue(output.hasJoined());

         identities = replicator.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));

         identities = replicator.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output.getPeerIdentity()));
         assertTrue(identities.contains(replicator.getPeerIdentity()));

         identities = replicator2.getOutputs();
         assertEquals(1, identities.size());
         assertTrue(identities.contains(output2.getPeerIdentity()));

         identities = replicator2.getView();
         assertEquals(2, identities.size());
         assertTrue(identities.contains(output2.getPeerIdentity()));
         assertTrue(identities.contains(replicator2.getPeerIdentity()));


         SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

         // unreliable message; TODO add test for reliable message
         Message m1 = MessageFactory.createMessage("message1", false, "payload1");

         Set deliveries = replicator.handle(observer, m1, null);

         assertEquals(1, deliveries.size());
         Delivery delivery = (Delivery)deliveries.iterator().next();

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertEquals(1, receiver.getMessages().size());
         assertValidMessage(m1, (Message)receiver.getMessages().get(0));
         assertTrue(receiver2.getMessages().isEmpty());

         receiver.clear();

         // unreliable message; TODO add test for reliable message
         Message m2 = MessageFactory.createMessage("message2", false, "payload2");

         deliveries = replicator2.handle(observer, m2, null);

         assertEquals(1, deliveries.size());
         delivery = (Delivery)deliveries.iterator().next();

         observer.waitForAcknowledgment(delivery, 3000);

         assertTrue(delivery.isDone());
         assertFalse(delivery.isCancelled());

         assertTrue(receiver.getMessages().isEmpty());
         assertEquals(1, receiver2.getMessages().size());
         assertValidMessage(m2, (Message)receiver2.getMessages().get(0));

         log.info("ok");
      }
      finally
      {
         replicator.leave();
         replicator2.leave();
         output.leave();
         output2.leave();
      }
   }

   //
   // Replicator DOES cancel active delivery on message rejection
   //

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------   
}
