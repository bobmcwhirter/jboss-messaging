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
package org.jboss.test.messaging.core.distributed.topic.base;

import org.jboss.messaging.core.Delivery;
import org.jboss.messaging.core.Message;
import org.jboss.messaging.core.MessageReference;
import org.jboss.messaging.core.distributed.topic.DistributedTopic;
import org.jboss.messaging.core.distributed.util.RpcServer;
import org.jboss.messaging.core.message.MessageFactory;
import org.jboss.messaging.core.plugin.JDBCPersistenceManager;
import org.jboss.messaging.core.plugin.PagingMessageStore;
import org.jboss.messaging.core.plugin.contract.MessageStore;
import org.jboss.messaging.core.plugin.contract.PersistenceManager;
import org.jboss.test.messaging.core.SimpleDeliveryObserver;
import org.jboss.test.messaging.core.SimpleReceiver;
import org.jboss.test.messaging.core.distributed.JGroupsUtil;
import org.jboss.test.messaging.core.local.base.TopicTestBase;
import org.jgroups.JChannel;
import org.jgroups.blocks.RpcDispatcher;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public abstract class DistributedTopicTestBase extends TopicTestBase
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------
   
   // Attributes ----------------------------------------------------

   protected JChannel jchannel, jchannel2, jchannel3;
   protected RpcDispatcher dispatcher, dispatcher2, dispatcher3;

   protected PersistenceManager tl2, tl3;
   protected MessageStore ms2, ms3;

   protected DistributedTopic topic2, topic3;

   // Constructors --------------------------------------------------

   public DistributedTopicTestBase(String name)
   {
      super(name);
   }

   // Public --------------------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();

      //FIXME - Why are we starting more than one persistence manager using the same datasource??
      //They will pointing at the same set of db tables thus giving indeterministic results
      
      tl2 = new JDBCPersistenceManager(sc.getDataSource(), sc.getTransactionManager());
      tl2.start();

      ms2 = new PagingMessageStore("s30");

      tl3 = new JDBCPersistenceManager(sc.getDataSource(), sc.getTransactionManager());
      tl3.start();

      ms3 = new PagingMessageStore("s31");
  
      jchannel = new JChannel(JGroupsUtil.generateProperties(50, 1));
      jchannel2 = new JChannel(JGroupsUtil.generateProperties(900000, 1));
      jchannel3 = new JChannel(JGroupsUtil.generateProperties(900000, 2));

      dispatcher = new RpcDispatcher(jchannel, null, null, new RpcServer("1"));
      dispatcher2 = new RpcDispatcher(jchannel2, null, null, new RpcServer("2"));
      dispatcher3 = new RpcDispatcher(jchannel3, null, null, new RpcServer("3"));

      // connect only the first JChannel
      jchannel.connect("testGroup");

      assertEquals(1, jchannel.getView().getMembers().size());
   }

   public void tearDown() throws Exception
   {
      jchannel.close();
      jchannel2.close();
      jchannel3.close();

      tl2.stop();
      tl2 = null;
      ms2 = null;

      tl3.stop();
      tl3 = null;
      ms3 = null;

      super.tearDown();
   }

   //
   // One peer
   //

   // TODO

   //
   // Two peers
   //

   ////
   //// One physical JGroups channel
   ////

   // TODO

   ////
   //// Two physical JGroups channels
   ////

   //////
   ////// Two ACKING receivers (one local and one remote)
   //////

   ////////
   //////// Unreliable message
   ////////

   public void testDistributedTopic_1() throws Exception
   {
      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ((DistributedTopic)topic).join();
      log.debug("topic joined");

      topic2.join();
      log.debug("topic2 joined");

      SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

      SimpleReceiver r = new SimpleReceiver("ACKING", SimpleReceiver.ACKING);
      assertTrue(topic.add(r));

      SimpleReceiver r2 = new SimpleReceiver("ACKING2", SimpleReceiver.ACKING);
      assertTrue(topic2.add(r2));

      Message m = MessageFactory.createCoreMessage("message0", false, "payload0");

      log.debug("sending message");
      MessageReference ref = ms.reference(m);
      Delivery d = topic.handle(observer, ref, null);
      //ref.release();
      log.debug("message sent");

      assertTrue(d.isDone());

      assertEquals(1, r.getMessages().size());
      assertEquals("message0", ((Message)r.getMessages().get(0)).getMessageID());
      assertEquals("payload0", ((Message)r.getMessages().get(0)).getPayload());

      // wait so the receiver gets the message
      assertTrue(r2.waitForHandleInvocations(1, 3000));

      assertEquals(1, r2.getMessages().size());
      assertEquals("message0", ((Message)r2.getMessages().get(0)).getMessageID());
      assertEquals("payload0", ((Message)r2.getMessages().get(0)).getPayload());

      // allow time for acknowledgment to propagate back
      Thread.sleep(1000);

      assertTrue(((DistributedTopic)topic).browse().isEmpty());
      assertTrue(topic2.browse().isEmpty());
   }


   ////////
   //////// Reliable message
   ////////

   public void testDistributedTopic_2() throws Exception
   {
      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ((DistributedTopic)topic).join();
      log.debug("topic joined");

      topic2.join();
      log.debug("topic2 joined");

      SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

      SimpleReceiver r = new SimpleReceiver("ACKING", SimpleReceiver.ACKING);
      assertTrue(topic.add(r));

      SimpleReceiver r2 = new SimpleReceiver("ACKING2", SimpleReceiver.ACKING);
      assertTrue(topic2.add(r2));

      Message m = MessageFactory.createCoreMessage("message0", true, "payload0");

      log.debug("sending message");
      Delivery d = topic.handle(observer, ms.reference(m), null);
      log.debug("message sent");

      assertTrue(d.isDone());

      assertEquals(1, r.getMessages().size());
      assertEquals("message0", ((Message)r.getMessages().get(0)).getMessageID());
      assertEquals("payload0", ((Message)r.getMessages().get(0)).getPayload());

      // wait so the receiver gets the message
      assertTrue(r2.waitForHandleInvocations(1, 3000));

      assertEquals(1, r2.getMessages().size());
      assertEquals("message0", ((Message)r2.getMessages().get(0)).getMessageID());
      assertEquals("payload0", ((Message)r2.getMessages().get(0)).getPayload());

      // allow time for acknowledgment to propagate back
      Thread.sleep(1000);

      assertTrue(((DistributedTopic)topic).browse().isEmpty());
      assertTrue(topic2.browse().isEmpty());
   }

   // We cannot having NACKING receiver attached to a local topic, it will violate the topic
   // consistency constraint

   //////
   ////// One ACKING receiver, one REJECTING receiver
   //////

   ////////
   //////// Unreliable message
   ////////

   public void testDistributedTopic_3() throws Throwable
   {
      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ((DistributedTopic)topic).join();
      log.debug("topic joined");

      topic2.join();
      log.debug("topic2 joined");

      SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

      SimpleReceiver r = new SimpleReceiver("ACKING", SimpleReceiver.ACKING);
      assertTrue(topic.add(r));

      SimpleReceiver r2 = new SimpleReceiver("REJECTING", SimpleReceiver.REJECTING);
      assertTrue(topic2.add(r2));

      Message m = MessageFactory.createCoreMessage("message0", false, "payload0");

      log.debug("sending message");
      Delivery d = topic.handle(observer, ms.reference(m), null);
      log.debug("message sent");

      assertTrue(d.isDone());

      assertEquals(1, r.getMessages().size());
      assertEquals("message0", ((Message)r.getMessages().get(0)).getMessageID());
      assertEquals("payload0", ((Message)r.getMessages().get(0)).getPayload());

      // wait for a while so the acknowledgment has time to go back to the originating peer
      Thread.sleep(1000);

      assertEquals(0, r2.getMessages().size());

      assertTrue(((DistributedTopic)topic).browse().isEmpty());
      assertTrue(topic2.browse().isEmpty());
   }


   ////////
   //////// Reliable message
   ////////

   public void testDistributedTopic_4() throws Throwable
   {
      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannels joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      ((DistributedTopic)topic).join();
      log.debug("topic joined");

      topic2.join();
      log.debug("topic2 joined");

      SimpleDeliveryObserver observer = new SimpleDeliveryObserver();

      SimpleReceiver r = new SimpleReceiver("ACKING", SimpleReceiver.ACKING);
      assertTrue(topic.add(r));

      SimpleReceiver r2 = new SimpleReceiver("REJECTING", SimpleReceiver.REJECTING);
      assertTrue(topic2.add(r2));

      Message m = MessageFactory.createCoreMessage("message0", true, "payload0");

      log.debug("sending message");
      Delivery d = topic.handle(observer, ms.reference(m), null);
      log.debug("message sent");

      assertTrue(d.isDone());

      assertEquals(1, r.getMessages().size());
      assertEquals("message0", ((Message)r.getMessages().get(0)).getMessageID());
      assertEquals("payload0", ((Message)r.getMessages().get(0)).getPayload());

      // wait for a while so the acknowledgment has time to go back to the originating peer
      Thread.sleep(1000);

      assertEquals(0, r2.getMessages().size());

      assertTrue(((DistributedTopic)topic).browse().isEmpty());
      assertTrue(topic2.browse().isEmpty());
   }



   //
   // Three peers
   //

   ////
   //// One physical JGroups channel
   ////

   // TODO

   ////
   //// Two physical JGroups channels
   ////

   // TODO

   ////
   //// Three physical JGroups channels
   ////

   // TODO

   // TODO also have cases in which the Receiver cancels a delivery

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------   
}
