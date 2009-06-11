/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.messaging.tests.integration.jms.server.management;

import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.config.impl.ConfigurationImpl;
import org.jboss.messaging.core.remoting.impl.invm.InVMAcceptorFactory;
import org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory;
import org.jboss.messaging.core.server.Messaging;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.core.settings.impl.AddressSettings;
import org.jboss.messaging.jms.JBossQueue;
import org.jboss.messaging.jms.client.JBossConnectionFactory;
import org.jboss.messaging.jms.server.impl.JMSServerManagerImpl;
import org.jboss.messaging.jms.server.management.JMSQueueControl;
import org.jboss.messaging.tests.integration.management.ManagementControlHelper;
import static org.jboss.messaging.tests.integration.management.ManagementControlHelper.createJMSQueueControl;
import org.jboss.messaging.tests.integration.management.ManagementTestBase;
import org.jboss.messaging.tests.unit.util.InVMContext;
import static org.jboss.messaging.tests.util.RandomUtil.randomLong;
import static org.jboss.messaging.tests.util.RandomUtil.randomSimpleString;
import static org.jboss.messaging.tests.util.RandomUtil.randomString;
import org.jboss.messaging.utils.SimpleString;
import org.jboss.messaging.utils.json.JSONArray;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A QueueControlTest
 *
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 * Created 14 nov. 2008 13:35:10
 *
 *
 */
public class JMSQueueControlTest extends ManagementTestBase
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private MessagingServer server;

   private JMSServerManagerImpl serverManager;

   protected JBossQueue queue;

   protected Context context;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testGetAttributes() throws Exception
   {
      JMSQueueControl queueControl = createManagementControl();

      assertEquals(queue.getName(), queueControl.getName());
      assertEquals(queue.getAddress(), queueControl.getAddress());
      assertEquals(queue.isTemporary(), queueControl.isTemporary());
   }

   public void testGetXXXCount() throws Exception
   {
      JMSQueueControl queueControl = createManagementControl();

      assertEquals(0, queueControl.getMessageCount());
      assertEquals(0, queueControl.getConsumerCount());

      Connection connection = JMSUtil.createConnection(InVMConnectorFactory.class.getName());
      MessageConsumer consumer = JMSUtil.createConsumer(connection, queue);

      assertEquals(1, queueControl.getConsumerCount());

      JMSUtil.sendMessages(queue, 2);

      assertEquals(2, queueControl.getMessageCount());
      assertEquals(2, queueControl.getMessagesAdded());

      connection.start();

      assertNotNull(consumer.receive(500));
      assertNotNull(consumer.receive(500));

      assertEquals(0, queueControl.getMessageCount());
      assertEquals(2, queueControl.getMessagesAdded());

      consumer.close();

      assertEquals(0, queueControl.getConsumerCount());

      connection.close();
   }

   public void testListAllMessages() throws Exception
   {
      JMSQueueControl queueControl = createManagementControl();

      assertEquals(0, queueControl.getMessageCount());

      String[] ids = JMSUtil.sendMessages(queue, 2);

      assertEquals(2, queueControl.getMessageCount());

      Map<String, Object>[] data = queueControl.listAllMessages();
      assertEquals(2, data.length);
      System.out.println(data[0].keySet());
      assertEquals(ids[0], data[0].get("JMSMessageID").toString());
      assertEquals(ids[1], data[1].get("JMSMessageID").toString());

      JMSUtil.consumeMessages(2, queue);

      data = queueControl.listAllMessages();
      assertEquals(0, data.length);
   }

   public void testListAllMessagesAsJSON() throws Exception
   {
      JMSQueueControl queueControl = createManagementControl();

      assertEquals(0, queueControl.getMessageCount());

      String[] ids = JMSUtil.sendMessages(queue, 2);

      assertEquals(2, queueControl.getMessageCount());

      String jsonString = queueControl.listAllMessagesAsJSON();
      assertNotNull(jsonString);
      JSONArray array = new JSONArray(jsonString);
      assertEquals(2, array.length());
      assertEquals(ids[0], array.getJSONObject(0).get("JMSMessageID"));
      assertEquals(ids[1], array.getJSONObject(1).get("JMSMessageID"));

      JMSUtil.consumeMessages(2, queue);

      jsonString = queueControl.listAllMessagesAsJSON();
      assertNotNull(jsonString);
      array = new JSONArray(jsonString);
      assertEquals(0, array.length());
   }

   public void testRemoveMessage() throws Exception
   {
      JMSQueueControl queueControl = createManagementControl();

      assertEquals(0, queueControl.getMessageCount());

      JMSUtil.sendMessages(queue, 2);

      assertEquals(2, queueControl.getMessageCount());

      Map<String, Object>[] data = queueControl.listAllMessages();
      assertEquals(2, data.length);

      System.out.println(data[0]);
      // retrieve the first message info
      Set<String> keySet = data[0].keySet();
      Iterator<String> it = keySet.iterator();
      while (it.hasNext())
      {
         System.out.println(it.next());
      }
      String messageID = (String)data[0].get("JMSMessageID");

      queueControl.removeMessage(messageID.toString());

      assertEquals(1, queueControl.getMessageCount());
   }

   public void testRemoveMessageWithUnknownMessage() throws Exception
   {
      String unknownMessageID = randomString();

      JMSQueueControl queueControl = createManagementControl();

      assertEquals(0, queueControl.getMessageCount());

      try
      {
         queueControl.removeMessage(unknownMessageID);
         fail("should throw an exception is the message ID is unknown");
      }
      catch (Exception e)
      {
      }
   }

   public void testRemoveAllMessages() throws Exception
   {
      JMSQueueControl queueControl = createManagementControl();

      assertEquals(0, queueControl.getMessageCount());

      JMSUtil.sendMessages(queue, 2);

      assertEquals(2, queueControl.getMessageCount());

      queueControl.removeAllMessages();

      assertEquals(0, queueControl.getMessageCount());

      Connection connection = JMSUtil.createConnection(InVMConnectorFactory.class.getName());
      connection.start();

      MessageConsumer consumer = JMSUtil.createConsumer(connection, queue);
      assertNull(consumer.receive(500));

      connection.close();
   }

   public void testRemoveMatchingMessages() throws Exception
   {
      JMSQueueControl queueControl = createManagementControl();

      assertEquals(0, queueControl.getMessageCount());

      Connection conn = createConnection();

      Session s = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageProducer producer = s.createProducer(queue);

      Message message = s.createMessage();
      message.setStringProperty("foo", "bar");
      producer.send(message);

      message = s.createMessage();
      message.setStringProperty("foo", "baz");
      producer.send(message);

      assertEquals(2, queueControl.getMessageCount());

      int removedMatchingMessagesCount = queueControl.removeMatchingMessages("foo = 'bar'");
      assertEquals(1, removedMatchingMessagesCount);

      assertEquals(1, queueControl.getMessageCount());

      conn.start();
      MessageConsumer consumer = JMSUtil.createConsumer(conn, queue);
      Message msg = consumer.receive(500);
      assertNotNull(msg);
      assertEquals("baz", msg.getStringProperty("foo"));

      conn.close();
   }

   public void testChangeMessagePriority() throws Exception
   {
      JMSQueueControl queueControl = createManagementControl();

      JMSUtil.sendMessages(queue, 1);

      assertEquals(1, queueControl.getMessageCount());

      Map<String, Object>[] data = queueControl.listAllMessages();
      // retrieve the first message info
      String messageID = (String)data[0].get("JMSMessageID");
      int currentPriority = ((Number)data[0].get("JMSPriority")).intValue();
      int newPriority = 9;

      assertTrue(newPriority != currentPriority);

      queueControl.changeMessagePriority(messageID.toString(), newPriority);

      Connection connection = JMSUtil.createConnection(InVMConnectorFactory.class.getName());
      connection.start();
      MessageConsumer consumer = JMSUtil.createConsumer(connection, queue);
      Message message = consumer.receive(500);
      assertNotNull(message);
      assertEquals(newPriority, message.getJMSPriority());

      connection.close();
   }

   public void testChangeMessagePriorityWithInvalidPriority() throws Exception
   {
      byte invalidPriority = (byte)23;

      JMSQueueControl queueControl = createManagementControl();

      String[] messageIDs = JMSUtil.sendMessages(queue, 1);

      assertEquals(1, queueControl.getMessageCount());

      try
      {
         queueControl.changeMessagePriority(messageIDs[0], invalidPriority);
         fail("must throw an exception if the new priority is not a valid value");
      }
      catch (Exception e)
      {
      }

      Connection connection = JMSUtil.createConnection(InVMConnectorFactory.class.getName());
      connection.start();
      MessageConsumer consumer = JMSUtil.createConsumer(connection, queue);
      Message message = consumer.receive(500);
      assertNotNull(message);
      assertTrue(message.getJMSPriority() != invalidPriority);

      connection.close();
   }

   public void testChangeMessagePriorityWithUnknownMessageID() throws Exception
   {
      String unkownMessageID = randomString();

      JMSQueueControl queueControl = createManagementControl();

      try
      {
         queueControl.changeMessagePriority(unkownMessageID, 7);
         fail();
      }
      catch (Exception e)
      {
      }
   }

   public void testGetExpiryAddress() throws Exception
   {
      final SimpleString expiryAddress = randomSimpleString();

      JMSQueueControl queueControl = createManagementControl();

      assertNull(queueControl.getExpiryAddress());

      server.getAddressSettingsRepository().addMatch(queue.getAddress(), new AddressSettings()
      {
         @Override
         public SimpleString getExpiryAddress()
         {
            return expiryAddress;
         }
      });

      assertEquals(expiryAddress.toString(), queueControl.getExpiryAddress());
   }

   public void testSetExpiryAddress() throws Exception
   {
      final String expiryAddress = randomString();

      JMSQueueControl queueControl = createManagementControl();

      assertNull(queueControl.getExpiryAddress());

      queueControl.setExpiryAddress(expiryAddress);
      assertEquals(expiryAddress, queueControl.getExpiryAddress());
   }

   public void testExpireMessage() throws Exception
   {
      JMSQueueControl queueControl = createManagementControl();
      String expiryQueueName = randomString();
      JBossQueue expiryQueue = new JBossQueue(expiryQueueName);
      serverManager.createQueue(expiryQueueName, expiryQueueName, null, true);
      queueControl.setExpiryAddress(expiryQueue.getAddress());

      JMSQueueControl expiryQueueControl = createJMSQueueControl(expiryQueue, mbeanServer);

      String[] messageIDs = JMSUtil.sendMessages(queue, 1);

      assertEquals(1, queueControl.getMessageCount());
      assertEquals(0, expiryQueueControl.getMessageCount());

      assertTrue(queueControl.expireMessage(messageIDs[0]));

      assertEquals(0, queueControl.getMessageCount());
      assertEquals(1, expiryQueueControl.getMessageCount());

      Connection connection = JMSUtil.createConnection(InVMConnectorFactory.class.getName());
      connection.start();

      MessageConsumer consumer = JMSUtil.createConsumer(connection, expiryQueue);
      Message message = consumer.receive(500);
      assertNotNull(message);
      assertEquals(messageIDs[0], message.getJMSMessageID());

      connection.close();
   }

   public void testExpireMessageWithUnknownMessageID() throws Exception
   {
      String unknownMessageID = randomString();

      JMSQueueControl queueControl = createManagementControl();

      try
      {
         queueControl.expireMessage(unknownMessageID);
         fail();
      }
      catch (Exception e)
      {
      }
   }

   public void testExpireMessagesWithFilter() throws Exception
   {
      String key = new String("key");
      long matchingValue = randomLong();
      long unmatchingValue = matchingValue + 1;
      String filter = key + " =" + matchingValue;

      Connection connection = JMSUtil.createConnection(InVMConnectorFactory.class.getName());
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      // send on queue
      JMSUtil.sendMessageWithProperty(session, queue, key, matchingValue);
      JMSUtil.sendMessageWithProperty(session, queue, key, unmatchingValue);

      connection.close();

      JMSQueueControl queueControl = createManagementControl();
      assertEquals(2, queueControl.getMessageCount());

      int expiredMessagesCount = queueControl.expireMessages(filter);
      assertEquals(1, expiredMessagesCount);
      assertEquals(1, queueControl.getMessageCount());

      // consume the unmatched message from queue
      JMSUtil.consumeMessages(1, queue);
   }

   public void testCountMessagesWithFilter() throws Exception
   {
      String key = "key";
      long matchingValue = randomLong();
      long unmatchingValue = matchingValue + 1;

      JMSQueueControl queueControl = createManagementControl();

      Connection connection = JMSUtil.createConnection(InVMConnectorFactory.class.getName());
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      JMSUtil.sendMessageWithProperty(session, queue, key, matchingValue);
      JMSUtil.sendMessageWithProperty(session, queue, key, unmatchingValue);
      JMSUtil.sendMessageWithProperty(session, queue, key, matchingValue);

      assertEquals(3, queueControl.getMessageCount());

      assertEquals(2, queueControl.countMessages(key + " =" + matchingValue));
      assertEquals(1, queueControl.countMessages(key + " =" + unmatchingValue));

      session.close();
   }

   public void testGetDeadLetterAddress() throws Exception
   {
      final SimpleString deadLetterAddress = randomSimpleString();

      JMSQueueControl queueControl = createManagementControl();

      assertNull(queueControl.getDeadLetterAddress());

      server.getAddressSettingsRepository().addMatch(queue.getAddress(), new AddressSettings()
      {
         @Override
         public SimpleString getDeadLetterAddress()
         {
            return deadLetterAddress;
         }
      });

      assertEquals(deadLetterAddress.toString(), queueControl.getDeadLetterAddress());
   }

   public void testSetDeadLetterAddress() throws Exception
   {
      final String deadLetterAddress = randomString();

      JMSQueueControl queueControl = createManagementControl();

      assertNull(queueControl.getDeadLetterAddress());

      queueControl.setDeadLetterAddress(deadLetterAddress);
      assertEquals(deadLetterAddress, queueControl.getDeadLetterAddress());
   }

   public void testSendMessageToDeadLetterAddress() throws Exception
   {
      String deadLetterQueue = randomString();
      serverManager.createQueue(deadLetterQueue, deadLetterQueue, null, true);
      JBossQueue dlq = new JBossQueue(deadLetterQueue);

      Connection conn = createConnection();
      Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageProducer producer = sess.createProducer(queue);

      // send 2 messages on queue
      Message message = sess.createMessage();
      producer.send(message);
      producer.send(sess.createMessage());

      conn.close();

      JMSQueueControl queueControl = createManagementControl();
      JMSQueueControl dlqControl = ManagementControlHelper.createJMSQueueControl(dlq, mbeanServer);

      assertEquals(2, queueControl.getMessageCount());
      assertEquals(0, dlqControl.getMessageCount());

      queueControl.setDeadLetterAddress(dlq.getAddress());

      boolean movedToDeadLetterAddress = queueControl.sendMessageToDeadLetterAddress(message.getJMSMessageID());
      assertTrue(movedToDeadLetterAddress);
      assertEquals(1, queueControl.getMessageCount());
      assertEquals(1, dlqControl.getMessageCount());

      // check there is a single message to consume from queue
      JMSUtil.consumeMessages(1, queue);

      // check there is a single message to consume from deadletter queue
      JMSUtil.consumeMessages(1, dlq);

      serverManager.destroyQueue(deadLetterQueue);
   }

   public void testSendMessageToDeadLetterAddressWithUnknownMessageID() throws Exception
   {
      String unknownMessageID = randomString();

      JMSQueueControl queueControl = createManagementControl();

      try
      {
         queueControl.sendMessageToDeadLetterAddress(unknownMessageID);
         fail();
      }
      catch (Exception e)
      {
      }

   }

   public void testMoveAllMessages() throws Exception
   {
      String otherQueueName = randomString();

      serverManager.createQueue(otherQueueName, otherQueueName, null, true);
      JBossQueue otherQueue = new JBossQueue(otherQueueName);

      // send on queue
      JMSUtil.sendMessages(queue, 2);

      JMSQueueControl queueControl = createManagementControl();
      assertEquals(2, queueControl.getMessageCount());

      // moved all messages to otherQueue
      int movedMessagesCount = queueControl.moveAllMessages(otherQueueName);
      assertEquals(2, movedMessagesCount);
      assertEquals(0, queueControl.getMessageCount());

      // check there is no message to consume from queue
      JMSUtil.consumeMessages(0, queue);

      // consume the message from otherQueue
      JMSUtil.consumeMessages(2, otherQueue);

      serverManager.destroyQueue(otherQueueName);
   }

   public void testMoveAllMessagesToUknownQueue() throws Exception
   {
      String unknownQueue = randomString();

      JMSQueueControl queueControl = createManagementControl();

      try
      {
         queueControl.moveAllMessages(unknownQueue);
         fail();
      }
      catch (Exception e)
      {
      }
   }

   public void testMoveMatchingMessages() throws Exception
   {
      String key = "key";
      long matchingValue = randomLong();
      long unmatchingValue = matchingValue + 1;
      String filter = "key = " + matchingValue;
      String otherQueueName = randomString();

      serverManager.createQueue(otherQueueName, otherQueueName, null, true);
      JBossQueue otherQueue = new JBossQueue(otherQueueName);

      Connection connection = JMSUtil.createConnection(InVMConnectorFactory.class.getName());
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      // send on queue
      JMSUtil.sendMessageWithProperty(session, queue, key, matchingValue);
      JMSUtil.sendMessageWithProperty(session, queue, key, unmatchingValue);

      JMSQueueControl queueControl = createManagementControl();
      assertEquals(2, queueControl.getMessageCount());

      // moved matching messages to otherQueue
      int movedMessagesCount = queueControl.moveMatchingMessages(filter, otherQueueName);
      assertEquals(1, movedMessagesCount);
      assertEquals(1, queueControl.getMessageCount());

      connection.start();
      MessageConsumer consumer = session.createConsumer(queue);
      Message message = consumer.receive(500);
      assertNotNull(message);
      assertEquals(unmatchingValue, message.getLongProperty(key));
      assertNull(consumer.receive(500));

      JMSUtil.consumeMessages(1, otherQueue);

      serverManager.destroyQueue(otherQueueName);

      connection.close();
   }

   public void testMoveMessage() throws Exception
   {
      String otherQueueName = randomString();

      serverManager.createQueue(otherQueueName, otherQueueName, null, true);
      JBossQueue otherQueue = new JBossQueue(otherQueueName);

      String[] messageIDs = JMSUtil.sendMessages(queue, 1);

      JMSQueueControl queueControl = createManagementControl();
      assertEquals(1, queueControl.getMessageCount());

      boolean moved = queueControl.moveMessage(messageIDs[0], otherQueueName);
      assertTrue(moved);
      assertEquals(0, queueControl.getMessageCount());

      JMSUtil.consumeMessages(0, queue);
      JMSUtil.consumeMessages(1, otherQueue);

      serverManager.destroyQueue(otherQueueName);
   }

   public void testMoveMessageWithUnknownMessageID() throws Exception
   {
      String unknownMessageID = randomString();
      String otherQueueName = randomString();

      serverManager.createQueue(otherQueueName, otherQueueName, null, true);

      JMSQueueControl queueControl = createManagementControl();
      assertEquals(0, queueControl.getMessageCount());

      try
      {
         queueControl.moveMessage(unknownMessageID, otherQueueName);
         fail();
      }
      catch (Exception e)
      {
      }

      serverManager.destroyQueue(otherQueueName);
   }

   public void testMoveMessageToUnknownQueue() throws Exception
   {
      String unknwonQueue = randomString();

      String[] messageIDs = JMSUtil.sendMessages(queue, 1);

      JMSQueueControl queueControl = createManagementControl();
      assertEquals(1, queueControl.getMessageCount());

      try
      {
         queueControl.moveMessage(messageIDs[0], unknwonQueue);
         fail();
      }
      catch (Exception e)
      {
      }

      JMSUtil.consumeMessages(1, queue);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      Configuration conf = new ConfigurationImpl();
      conf.setSecurityEnabled(false);
      conf.setJMXManagementEnabled(true);
      conf.getAcceptorConfigurations().add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
      server = Messaging.newMessagingServer(conf, mbeanServer, false);
      server.start();

      serverManager = new JMSServerManagerImpl(server);
      context = new InVMContext();
      serverManager.setContext(context);
      serverManager.start();
      serverManager.activated();

      String queueName = randomString();
      serverManager.createQueue(queueName, queueName, null, true);
      queue = new JBossQueue(queueName);
   }

   @Override
   protected void tearDown() throws Exception
   {
      server.stop();

      super.tearDown();
   }

   protected JMSQueueControl createManagementControl() throws Exception
   {
      return createJMSQueueControl(queue, mbeanServer);
   }

   // Private -------------------------------------------------------

   private Connection createConnection() throws JMSException
   {
      JBossConnectionFactory cf = new JBossConnectionFactory(new TransportConfiguration(InVMConnectorFactory.class.getName()));

      cf.setBlockOnPersistentSend(true);

      return cf.createConnection();
   }

   // Inner classes -------------------------------------------------

}