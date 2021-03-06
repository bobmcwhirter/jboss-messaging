/*
 * JBoss, Home of Professional Open Source Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors by
 * the @authors tag. See the copyright.txt in the distribution for a full listing of individual contributors. This is
 * free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. You should have received a copy of the GNU Lesser General Public License along with this software; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.jboss.test.messaging.jms;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

/**
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a> <p/> $Id: AcknowledgementTest.java 3173 2007-10-05 12:48:16Z
 *         timfox $
 */
public class AcknowledgementTest extends JMSTestCase
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   // Constructors --------------------------------------------------

   public AcknowledgementTest(final String name)
   {
      super(name);
   }

   // TestCase overrides -------------------------------------------

   // Public --------------------------------------------------------

   /* Topics shouldn't hold on to messages if there are no subscribers */

   public void testPersistentMessagesForTopicDropped() throws Exception
   {
      TopicConnection conn = null;

      try
      {
         conn = cf.createTopicConnection();
         TopicSession sess = conn.createTopicSession(true, 0);
         TopicPublisher pub = sess.createPublisher(topic1);
         pub.setDeliveryMode(DeliveryMode.PERSISTENT);

         Message m = sess.createTextMessage("testing123");
         pub.publish(m);
         sess.commit();

         conn.close();

         checkEmpty(topic1);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }
   }

   /* Topics shouldn't hold on to messages when the non-durable subscribers close */
   public void testPersistentMessagesForTopicDropped2() throws Exception
   {
      TopicConnection conn = null;

      try
      {
         conn = cf.createTopicConnection();
         conn.start();
         TopicSession sess = conn.createTopicSession(true, 0);
         TopicPublisher pub = sess.createPublisher(topic1);
         TopicSubscriber sub = sess.createSubscriber(topic1);
         pub.setDeliveryMode(DeliveryMode.PERSISTENT);

         Message m = sess.createTextMessage("testing123");
         pub.publish(m);
         sess.commit();

         // receive but rollback
         TextMessage m2 = (TextMessage)sub.receive(3000);

         assertNotNull(m2);
         assertEquals("testing123", m2.getText());

         sess.rollback();

         conn.close();

         checkEmpty(topic1);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }
   }

   public void testRollbackRecover() throws Exception
   {
      TopicConnection conn = null;

      try
      {
         conn = cf.createTopicConnection();
         TopicSession sess = conn.createTopicSession(true, 0);
         TopicPublisher pub = sess.createPublisher(topic1);
         TopicSubscriber cons = sess.createSubscriber(topic1);
         conn.start();

         Message m = sess.createTextMessage("testing123");
         pub.publish(m);
         sess.commit();

         TextMessage m2 = (TextMessage)cons.receive(3000);
         assertNotNull(m2);
         assertEquals("testing123", m2.getText());

         sess.rollback();

         m2 = (TextMessage)cons.receive(3000);
         assertNotNull(m2);
         assertEquals("testing123", m2.getText());

         conn.close();

         conn = cf.createTopicConnection();
         conn.start();

         // test 2

         TopicSession newsess = conn.createTopicSession(true, 0);
         TopicPublisher newpub = newsess.createPublisher(topic1);
         TopicSubscriber newcons = newsess.createSubscriber(topic1);

         Message m3 = newsess.createTextMessage("testing456");
         newpub.publish(m3);
         newsess.commit();

         TextMessage m4 = (TextMessage)newcons.receive(3000);
         assertNotNull(m4);
         assertEquals("testing456", m4.getText());

         newsess.commit();

         newpub.publish(m3);
         newsess.commit();

         TextMessage m5 = (TextMessage)newcons.receive(3000);
         assertNotNull(m5);
         assertEquals("testing456", m5.getText());

         newsess.rollback();

         TextMessage m6 = (TextMessage)newcons.receive(3000);
         assertNotNull(m6);
         assertEquals("testing456", m6.getText());

         newsess.commit();
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }
   }

   public void testTransactionalAcknowledgement() throws Exception
   {
      Connection conn = null;

      try
      {

         conn = cf.createConnection();

         Session producerSess = conn.createSession(true, Session.SESSION_TRANSACTED);
         MessageProducer producer = producerSess.createProducer(queue1);

         Session consumerSess = conn.createSession(true, Session.SESSION_TRANSACTED);
         MessageConsumer consumer = consumerSess.createConsumer(queue1);
         conn.start();

         final int NUM_MESSAGES = 20;

         // Send some messages
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message m = producerSess.createMessage();
            producer.send(m);
         }

         assertRemainingMessages(0);

         producerSess.rollback();

         // Send some messages
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message m = producerSess.createMessage();
            producer.send(m);
         }
         assertRemainingMessages(0);

         producerSess.commit();

         assertRemainingMessages(NUM_MESSAGES);

         log.trace("Sent messages");

         int count = 0;
         while (true)
         {
            Message m = consumer.receive(200);
            if (m == null)
            {
               break;
            }
            count++;
         }

         assertRemainingMessages(NUM_MESSAGES);

         log.trace("Received " + count + " messages");

         assertEquals(count, NUM_MESSAGES);

         consumerSess.rollback();

         assertRemainingMessages(NUM_MESSAGES);

         log.trace("Session rollback called");

         int i = 0;
         for (; i < NUM_MESSAGES; i++)
         {
            consumer.receive();
            log.trace("Received message " + i);
         }

         assertRemainingMessages(NUM_MESSAGES);

         // if I don't receive enough messages, the test will timeout

         log.trace("Received " + i + " messages after recover");

         consumerSess.commit();

         assertRemainingMessages(0);

         checkEmpty(queue1);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }
   }

   /**
    * Send some messages, don't acknowledge them and verify that they are re-sent on recovery.
    */
   public void testClientAcknowledgeNoAcknowledgement() throws Exception
   {
      Connection conn = null;

      try
      {
         conn = cf.createConnection();

         Session producerSess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
         MessageProducer producer = producerSess.createProducer(queue1);

         Session consumerSess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
         MessageConsumer consumer = consumerSess.createConsumer(queue1);
         conn.start();

         final int NUM_MESSAGES = 20;

         // Send some messages
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message m = producerSess.createMessage();
            producer.send(m);
         }

         assertRemainingMessages(NUM_MESSAGES);

         log.trace("Sent messages");

         int count = 0;
         while (true)
         {
            Message m = consumer.receive(200);
            if (m == null)
            {
               break;
            }
            count++;
         }

         assertRemainingMessages(NUM_MESSAGES);

         log.trace("Received " + count + " messages");

         assertEquals(count, NUM_MESSAGES);

         consumerSess.recover();

         assertRemainingMessages(NUM_MESSAGES);

         log.trace("Session recover called");

         Message m = null;

         int i = 0;
         for (; i < NUM_MESSAGES; i++)
         {
            m = consumer.receive();
            log.trace("Received message " + i);

         }

         assertRemainingMessages(NUM_MESSAGES);

         // if I don't receive enough messages, the test will timeout

         log.trace("Received " + i + " messages after recover");

         m.acknowledge();

         assertRemainingMessages(0);

         // make sure I don't receive anything else

         checkEmpty(queue1);

         conn.close();
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }
   }

   /**
    * Send some messages, acknowledge them individually and verify they are not resent after recovery.
    */
   public void testIndividualClientAcknowledge() throws Exception
   {
      Connection conn = null;

      try
      {
         conn = cf.createConnection();

         Session producerSess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
         MessageProducer producer = producerSess.createProducer(queue1);

         Session consumerSess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
         MessageConsumer consumer = consumerSess.createConsumer(queue1);
         conn.start();

         final int NUM_MESSAGES = 20;

         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message m = producerSess.createMessage();
            producer.send(m);
         }

         assertRemainingMessages(NUM_MESSAGES);

         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message m = consumer.receive(200);

            assertNotNull(m);

            assertRemainingMessages(NUM_MESSAGES - i);

            m.acknowledge();

            assertRemainingMessages(NUM_MESSAGES - (i + 1));
         }

         assertRemainingMessages(0);

         consumerSess.recover();

         Message m = consumer.receive(200);
         assertNull(m);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }

   }

   /**
    * Send some messages, acknowledge them once after all have been received verify they are not resent after recovery
    */
   public void testBulkClientAcknowledge() throws Exception
   {
      Connection conn = null;

      try
      {
         conn = cf.createConnection();

         Session producerSess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
         MessageProducer producer = producerSess.createProducer(queue1);

         Session consumerSess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
         MessageConsumer consumer = consumerSess.createConsumer(queue1);
         conn.start();

         final int NUM_MESSAGES = 20;

         // Send some messages
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message m = producerSess.createMessage();
            producer.send(m);
         }

         assertRemainingMessages(NUM_MESSAGES);

         log.trace("Sent messages");

         Message m = null;
         int count = 0;
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            m = consumer.receive(200);
            if (m == null)
            {
               break;
            }
            count++;
         }

         assertRemainingMessages(NUM_MESSAGES);

         assertNotNull(m);

         m.acknowledge();

         assertRemainingMessages(0);

         log.trace("Received " + count + " messages");

         assertEquals(count, NUM_MESSAGES);

         consumerSess.recover();

         log.trace("Session recover called");

         m = consumer.receive(200);

         log.trace("Message is:" + m);

         assertNull(m);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }
   }

   /**
    * Send some messages, acknowledge some of them, and verify that the others are resent after delivery
    */
   public void testPartialClientAcknowledge() throws Exception
   {
      Connection conn = null;

      try
      {

         conn = cf.createConnection();

         Session producerSess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
         MessageProducer producer = producerSess.createProducer(queue1);

         Session consumerSess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
         MessageConsumer consumer = consumerSess.createConsumer(queue1);
         conn.start();

         final int NUM_MESSAGES = 20;
         final int ACKED_MESSAGES = 11;

         // Send some messages
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message m = producerSess.createMessage();
            producer.send(m);
         }

         assertRemainingMessages(NUM_MESSAGES);

         log.trace("Sent messages");

         int count = 0;

         Message m = null;
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            m = consumer.receive(200);
            if (m == null)
            {
               break;
            }
            if (count == ACKED_MESSAGES - 1)
            {
               m.acknowledge();
            }
            count++;
         }

         assertRemainingMessages(NUM_MESSAGES - ACKED_MESSAGES);

         assertNotNull(m);

         log.trace("Received " + count + " messages");

         assertEquals(count, NUM_MESSAGES);

         consumerSess.recover();

         log.trace("Session recover called");

         count = 0;
         while (true)
         {
            m = consumer.receive(200);
            if (m == null)
            {
               break;
            }
            count++;
         }

         assertEquals(NUM_MESSAGES - ACKED_MESSAGES, count);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }

         removeAllMessages(queue1.getQueueName(), true, 0);
      }
   }

   /*
    * Send some messages, consume them and verify the messages are not sent upon recovery
    */
   public void testAutoAcknowledge() throws Exception
   {
      Connection conn = null;

      try
      {
         conn = cf.createConnection();

         Session producerSess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

         MessageProducer producer = producerSess.createProducer(queue1);

         Session consumerSess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

         MessageConsumer consumer = consumerSess.createConsumer(queue1);
         conn.start();

         final int NUM_MESSAGES = 20;

         // Send some messages
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message m = producerSess.createMessage();
            producer.send(m);
         }

         assertRemainingMessages(NUM_MESSAGES);

         int count = 0;

         Message m = null;
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            assertRemainingMessages(NUM_MESSAGES - i);

            m = consumer.receive(200);

            assertRemainingMessages(NUM_MESSAGES - (i + 1));

            if (m == null)
            {
               break;
            }
            count++;
         }

         assertRemainingMessages(0);

         assertNotNull(m);

         log.trace("Received " + count + " messages");

         assertEquals(count, NUM_MESSAGES);

         consumerSess.recover();

         log.trace("Session recover called");

         m = consumer.receive(200);

         log.trace("Message is:" + m);

         assertNull(m);

         // Thread.sleep(3000000);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }

   }

   public void testDupsOKAcknowledgeQueue() throws Exception
   {
      Connection conn = null;

      try
      {
         conn = cf.createConnection();

         Session producerSess = conn.createSession(false, Session.DUPS_OK_ACKNOWLEDGE);

         MessageProducer producer = producerSess.createProducer(queue1);

         Session consumerSess = conn.createSession(false, Session.DUPS_OK_ACKNOWLEDGE);

         MessageConsumer consumer = consumerSess.createConsumer(queue1);
         conn.start();

         final int NUM_MESSAGES = 20;

         // Send some messages
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message m = producerSess.createMessage();
            producer.send(m);
         }

         assertRemainingMessages(NUM_MESSAGES);

         int count = 0;

         Message m = null;
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            assertRemainingMessages(NUM_MESSAGES - i);

            m = consumer.receive(200);

            assertRemainingMessages(NUM_MESSAGES - (i + 1));

            if (m == null)
            {
               break;
            }
            count++;
         }

         assertRemainingMessages(0);

         assertNotNull(m);

         log.trace("Received " + count + " messages");

         assertEquals(count, NUM_MESSAGES);

         consumerSess.recover();

         log.trace("Session recover called");

         m = consumer.receive(200);

         log.trace("Message is:" + m);

         assertNull(m);

         // Thread.sleep(3000000);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }

   }

   public void testDupsOKAcknowledgeTopic() throws Exception
   {
      final int BATCH_SIZE = 10;

      ArrayList<String> bindings = new ArrayList<String>();
      bindings.add("mycf");
      deployConnectionFactory(null, "MyConnectionFactory2", bindings, -1, -1, -1, -1, false, false, BATCH_SIZE, true);
      Connection conn = null;

      try
      {

         ConnectionFactory myCF = (ConnectionFactory)ic.lookup("/mycf");

         conn = myCF.createConnection();

         Session producerSess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer producer = producerSess.createProducer(topic1);

         Session consumerSess = conn.createSession(false, Session.DUPS_OK_ACKNOWLEDGE);
         MessageConsumer consumer = consumerSess.createConsumer(topic1);
         conn.start();

         // Send some messages
         for (int i = 0; i < 19; i++)
         {
            Message m = producerSess.createMessage();
            producer.send(m);
         }

         log.trace("Sent messages");

         Message m = null;
         for (int i = 0; i < 19; i++)
         {
            m = consumer.receive(200);

            assertNotNull(m);
         }

         consumerSess.close();
      }
      finally
      {

         if (conn != null)
         {
            conn.close();
         }

         undeployConnectionFactory("MyConnectionFactory2");
      }

   }

   /*
    * Send some messages, consume them and verify the messages are not sent upon recovery
    */
   public void testLazyAcknowledge() throws Exception
   {
      Connection conn = null;

      try
      {
         conn = cf.createConnection();

         Session producerSess = conn.createSession(false, Session.DUPS_OK_ACKNOWLEDGE);
         MessageProducer producer = producerSess.createProducer(queue1);

         Session consumerSess = conn.createSession(false, Session.DUPS_OK_ACKNOWLEDGE);
         MessageConsumer consumer = consumerSess.createConsumer(queue1);
         conn.start();

         final int NUM_MESSAGES = 20;

         // Send some messages
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message m = producerSess.createMessage();
            producer.send(m);
         }

         assertRemainingMessages(NUM_MESSAGES);

         log.trace("Sent messages");

         int count = 0;

         Message m = null;
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            m = consumer.receive(200);
            if (m == null)
            {
               break;
            }
            count++;
         }

         assertNotNull(m);

         assertRemainingMessages(0);

         log.trace("Received " + count + " messages");

         assertEquals(count, NUM_MESSAGES);

         consumerSess.recover();

         log.trace("Session recover called");

         m = consumer.receive(200);

         log.trace("Message is:" + m);

         assertNull(m);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }

   }

   public void testMessageListenerAutoAck() throws Exception
   {
      Connection conn = null;

      try
      {
         conn = cf.createConnection();
         Session sessSend = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod = sessSend.createProducer(queue1);

         log.trace("Sending messages");

         TextMessage tm1 = sessSend.createTextMessage("a");
         TextMessage tm2 = sessSend.createTextMessage("b");
         TextMessage tm3 = sessSend.createTextMessage("c");
         prod.send(tm1);
         prod.send(tm2);
         prod.send(tm3);

         log.trace("Sent messages");

         sessSend.close();

         assertRemainingMessages(3);

         conn.start();

         Session sessReceive = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

         log.trace("Creating consumer");

         MessageConsumer cons = sessReceive.createConsumer(queue1);

         log.trace("Created consumer");

         MessageListenerAutoAck listener = new MessageListenerAutoAck(sessReceive);

         log.trace("Setting message listener");

         cons.setMessageListener(listener);

         log.trace("Set message listener");

         listener.waitForMessages();

         Thread.sleep(500);

         assertRemainingMessages(0);

         assertFalse(listener.failed);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }
   }

   /*
    * This test will: - Send two messages over a producer - Receive one message over a consumer - Call Recover - Receive
    * the second message - The queue should be empty after that Note: testMessageListenerAutoAck will test a similar
    * case using MessageListeners
    */
   public void testRecoverAutoACK() throws Exception
   {
      Connection conn = null;

      try
      {
         conn = cf.createConnection();
         Session s = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer p = s.createProducer(queue1);
         p.setDeliveryMode(DeliveryMode.PERSISTENT);
         Message m = s.createTextMessage("one");
         p.send(m);
         m = s.createTextMessage("two");
         p.send(m);
         conn.close();

         conn = null;

         assertRemainingMessages(2);

         conn = cf.createConnection();

         conn.start();

         Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

         MessageConsumer consumer = session.createConsumer(queue1);

         TextMessage messageReceived = (TextMessage)consumer.receive(1000);

         assertNotNull(messageReceived);

         assertEquals("one", messageReceived.getText());

         session.recover();

         messageReceived = (TextMessage)consumer.receive(1000);

         assertEquals("two", messageReceived.getText());

         consumer.close();

         // I can't call xasession.close for this test as JCA layer would cache the session
         // So.. keep this close commented!
         // xasession.close();

         assertRemainingMessages(0);

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }

         destroyQueue("MyQueue2");
      }
   }

   public void testMessageListenerDupsOK() throws Exception
   {
      Connection conn = null;

      try
      {

         conn = cf.createConnection();
         Session sessSend = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod = sessSend.createProducer(queue1);

         log.trace("Sending messages");

         TextMessage tm1 = sessSend.createTextMessage("a");
         TextMessage tm2 = sessSend.createTextMessage("b");
         TextMessage tm3 = sessSend.createTextMessage("c");
         prod.send(tm1);
         prod.send(tm2);
         prod.send(tm3);

         log.trace("Sent messages");

         sessSend.close();

         assertRemainingMessages(3);

         conn.start();

         Session sessReceive = conn.createSession(false, Session.DUPS_OK_ACKNOWLEDGE);

         log.trace("Creating consumer");

         MessageConsumer cons = sessReceive.createConsumer(queue1);

         log.trace("Created consumer");

         MessageListenerDupsOK listener = new MessageListenerDupsOK(sessReceive);

         log.trace("Setting message listener");

         cons.setMessageListener(listener);

         log.trace("Set message listener");

         listener.waitForMessages();

         log.info("Waited for messages");

         // Recover forces an ack so there will be one
         assertRemainingMessages(1);

         log.info("closing connection");
         conn.close();

         Thread.sleep(500);

         assertRemainingMessages(0);
         assertFalse(listener.failed);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }
   }

   public void testMessageListenerClientAck() throws Exception
   {
      Connection conn = null;

      try
      {
         conn = cf.createConnection();
         Session sessSend = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod = sessSend.createProducer(queue1);

         TextMessage tm1 = sessSend.createTextMessage("a");
         TextMessage tm2 = sessSend.createTextMessage("b");
         TextMessage tm3 = sessSend.createTextMessage("c");
         prod.send(tm1);
         prod.send(tm2);
         prod.send(tm3);
         sessSend.close();

         assertRemainingMessages(3);

         conn.start();
         Session sessReceive = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
         MessageConsumer cons = sessReceive.createConsumer(queue1);
         MessageListenerClientAck listener = new MessageListenerClientAck(sessReceive);
         cons.setMessageListener(listener);

         listener.waitForMessages();

         Thread.sleep(500);

         assertRemainingMessages(0);

         conn.close();

         assertFalse(listener.failed);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }
   }

   public void testMessageListenerTransactionalAck() throws Exception
   {
      Connection conn = null;

      try
      {
         conn = cf.createConnection();
         Session sessSend = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod = sessSend.createProducer(queue1);

         TextMessage tm1 = sessSend.createTextMessage("a");
         TextMessage tm2 = sessSend.createTextMessage("b");
         TextMessage tm3 = sessSend.createTextMessage("c");
         prod.send(tm1);
         prod.send(tm2);
         prod.send(tm3);
         sessSend.close();

         assertRemainingMessages(3);

         conn.start();
         Session sessReceive = conn.createSession(true, Session.SESSION_TRANSACTED);
         MessageConsumer cons = sessReceive.createConsumer(queue1);
         MessageListenerTransactionalAck listener = new MessageListenerTransactionalAck(sessReceive);
         cons.setMessageListener(listener);
         listener.waitForMessages();

         Thread.sleep(500);

         assertRemainingMessages(0);

         conn.close();

         assertFalse(listener.failed);
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
      }
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

   private abstract class LatchListener implements MessageListener
   {
      protected CountDownLatch latch = new CountDownLatch(1);

      protected Session sess;

      protected int count = 0;

      boolean failed;

      LatchListener(final Session sess)
      {
         this.sess = sess;
      }

      public void waitForMessages() throws InterruptedException
      {
         assertTrue("failed to receive all messages", latch.await(2000, MILLISECONDS));
      }

      public abstract void onMessage(Message m);

   }

   private class MessageListenerAutoAck extends LatchListener
   {

      MessageListenerAutoAck(final Session sess)
      {
         super(sess);
      }

      @Override
      public void onMessage(final Message m)
      {
         try
         {
            count++;

            TextMessage tm = (TextMessage)m;

            log.info("got message " + tm.getText());

            // Receive first three messages then recover() session
            // Only last message should be redelivered
            if (count == 1)
            {
               assertRemainingMessages(3);

               if (!"a".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
            }
            if (count == 2)
            {
               assertRemainingMessages(2);

               if (!"b".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
            }
            if (count == 3)
            {
               assertRemainingMessages(1);

               if (!"c".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
               sess.recover();
            }
            if (count == 4)
            {
               assertRemainingMessages(1);

               if (!"c".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
               latch.countDown();
            }

         }
         catch (Exception e)
         {
            failed = true;
            latch.countDown();
         }
      }

   }

   private class MessageListenerDupsOK extends LatchListener
   {

      MessageListenerDupsOK(final Session sess)
      {
         super(sess);
      }

      @Override
      public void onMessage(final Message m)
      {
         try
         {
            count++;

            TextMessage tm = (TextMessage)m;

            log.info("Got message " + tm.getText());

            // Receive first three messages then recover() session
            // Only last message should be redelivered
            if (count == 1)
            {
               assertRemainingMessages(3);

               if (!"a".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
            }
            if (count == 2)
            {
               assertRemainingMessages(2);

               if (!"b".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
            }
            if (count == 3)
            {
               assertRemainingMessages(1);

               if (!"c".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
               sess.recover();
            }
            if (count == 4)
            {
               // Recover forces an ack, so there will be only one left
               assertRemainingMessages(1);

               if (!"c".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
               latch.countDown();
            }

         }
         catch (Exception e)
         {
            failed = true;
            latch.countDown();
         }
      }

   }

   private class MessageListenerClientAck extends LatchListener
   {
      MessageListenerClientAck(final Session sess)
      {
         super(sess);
      }

      @Override
      public void onMessage(final Message m)
      {
         try
         {
            count++;

            TextMessage tm = (TextMessage)m;

            log.info("Got message " + tm.getText());

            if (count == 1)
            {
               assertRemainingMessages(3);
               if (!"a".equals(tm.getText()))
               {
                  log.info("Expected a but got " + tm.getText());
                  failed = true;
                  latch.countDown();
               }
            }
            if (count == 2)
            {
               assertRemainingMessages(3);
               if (!"b".equals(tm.getText()))
               {
                  log.info("Expected b but got " + tm.getText());
                  failed = true;
                  latch.countDown();
               }
            }
            if (count == 3)
            {
               assertRemainingMessages(3);
               if (!"c".equals(tm.getText()))
               {
                  log.info("Expected c but got " + tm.getText());
                  failed = true;
                  latch.countDown();
               }
               log.info("calling recover");
               sess.recover();
            }
            if (count == 4)
            {
               assertRemainingMessages(3);
               if (!"a".equals(tm.getText()))
               {
                  log.info("Expected a but got " + tm.getText());
                  failed = true;
                  latch.countDown();
               }
               log.info("*** calling acknowledge");
               tm.acknowledge();
               assertRemainingMessages(2);
               log.info("calling recover");
               sess.recover();
            }
            if (count == 5)
            {
               assertRemainingMessages(2);
               if (!"b".equals(tm.getText()))
               {
                  log.info("Expected b but got " + tm.getText());
                  failed = true;
                  latch.countDown();
               }
               log.info("calling recover");
               sess.recover();
            }
            if (count == 6)
            {
               assertRemainingMessages(2);
               if (!"b".equals(tm.getText()))
               {
                  log.info("Expected b but got " + tm.getText());
                  failed = true;
                  latch.countDown();
               }
            }
            if (count == 7)
            {
               assertRemainingMessages(2);
               if (!"c".equals(tm.getText()))
               {
                  log.info("Expected c but got " + tm.getText());
                  failed = true;
                  latch.countDown();
               }
               tm.acknowledge();
               assertRemainingMessages(0);
               latch.countDown();
            }

         }
         catch (Exception e)
         {
            log.error("Caught exception", e);
            failed = true;
            latch.countDown();
         }
      }

   }

   private class MessageListenerTransactionalAck extends LatchListener
   {

      MessageListenerTransactionalAck(final Session sess)
      {
         super(sess);
      }

      public void onMessage(final Message m)
      {
         try
         {
            count++;

            TextMessage tm = (TextMessage)m;

            if (count == 1)
            {
               assertRemainingMessages(3);
               if (!"a".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
            }
            if (count == 2)
            {
               assertRemainingMessages(3);
               if (!"b".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
            }
            if (count == 3)
            {
               assertRemainingMessages(3);
               if (!"c".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
               log.trace("Rollback");
               sess.rollback();
            }
            if (count == 4)
            {
               assertRemainingMessages(3);
               if (!"a".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
            }
            if (count == 5)
            {
               assertRemainingMessages(3);
               if (!"b".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
               log.trace("commit");
               sess.commit();
               assertRemainingMessages(1);
            }
            if (count == 6)
            {
               assertRemainingMessages(1);
               if (!"c".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
               log.trace("recover");
               sess.rollback();
            }
            if (count == 7)
            {
               assertRemainingMessages(1);
               if (!"c".equals(tm.getText()))
               {
                  failed = true;
                  latch.countDown();
               }
               log.trace("Commit");
               sess.commit();
               assertRemainingMessages(0);
               latch.countDown();
            }
         }
         catch (Exception e)
         {
            // log.error(e);
            failed = true;
            latch.countDown();
         }
      }

   }
}
