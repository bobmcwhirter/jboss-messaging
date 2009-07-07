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
package org.jboss.messaging.tests.integration.client;

import java.util.concurrent.CountDownLatch;

import org.jboss.messaging.core.client.ClientConsumer;
import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.ClientProducer;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.MessageHandler;
import org.jboss.messaging.core.exception.MessagingException;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.tests.util.ServiceTestBase;
import org.jboss.messaging.utils.SimpleString;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 */
public class AutogroupIdTest extends ServiceTestBase
{
   public final SimpleString addressA = new SimpleString("addressA");

   public final SimpleString queueA = new SimpleString("queueA");

   public final SimpleString queueB = new SimpleString("queueB");

   public final SimpleString queueC = new SimpleString("queueC");

   private final SimpleString groupTestQ = new SimpleString("testGroupQueue");

   /* auto group id tests*/

   /*
  * tests when the autogroupid is set only 1 consumer (out of 2) gets all the messages from a single producer
  * */

   public void testGroupIdAutomaticallySet() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();

         ClientSessionFactory sf = createInVMFactory();
         sf.setAutoGroup(true);
         ClientSession session = sf.createSession(false, true, true);

         session.createQueue(groupTestQ, groupTestQ, null, false);

         ClientProducer producer = session.createProducer(groupTestQ);

         final CountDownLatch latch = new CountDownLatch(100);

         MyMessageHandler myMessageHandler = new MyMessageHandler(latch);
         MyMessageHandler myMessageHandler2 = new MyMessageHandler(latch);

         ClientConsumer consumer = session.createConsumer(groupTestQ);
         consumer.setMessageHandler(myMessageHandler);
         ClientConsumer consumer2 = session.createConsumer(groupTestQ);
         consumer2.setMessageHandler(myMessageHandler2);

         session.start();

         final int numMessages = 100;

         for (int i = 0; i < numMessages; i++)
         {
            producer.send(session.createClientMessage(false));
         }
         latch.await();

         session.close();

         assertEquals(myMessageHandler.messagesReceived, 100);
         assertEquals(myMessageHandler2.messagesReceived, 0);
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }

   }

   /*
  * tests when the autogroupid is set only 2 consumers (out of 3) gets all the messages from 2 producers
  * */
   public void testGroupIdAutomaticallySetMultipleProducers() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();

         ClientSessionFactory sf = createInVMFactory();
         sf.setAutoGroup(true);
         ClientSession session = sf.createSession(false, true, true);

         session.createQueue(groupTestQ, groupTestQ, null, false);

         ClientProducer producer = session.createProducer(groupTestQ);
         ClientProducer producer2 = session.createProducer(groupTestQ);

         final CountDownLatch latch = new CountDownLatch(200);

         MyMessageHandler myMessageHandler = new MyMessageHandler(latch);
         MyMessageHandler myMessageHandler2 = new MyMessageHandler(latch);
         MyMessageHandler myMessageHandler3 = new MyMessageHandler(latch);

         ClientConsumer consumer = session.createConsumer(groupTestQ);
         consumer.setMessageHandler(myMessageHandler);
         ClientConsumer consumer2 = session.createConsumer(groupTestQ);
         consumer2.setMessageHandler(myMessageHandler2);
         ClientConsumer consumer3 = session.createConsumer(groupTestQ);
         consumer3.setMessageHandler(myMessageHandler3);

         session.start();

         final int numMessages = 100;

         for (int i = 0; i < numMessages; i++)
         {
            producer.send(session.createClientMessage(false));
         }
         for (int i = 0; i < numMessages; i++)
         {
            producer2.send(session.createClientMessage(false));
         }
         latch.await();

         session.close();

         assertEquals(myMessageHandler.messagesReceived, 100);
         assertEquals(myMessageHandler2.messagesReceived, 100);
         assertEquals(myMessageHandler3.messagesReceived, 0);
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }

   }

   /*
  * tests that even tho we have an grouping round robin distributor we don't pin the consumer as autogroup is false
  * */
   public void testGroupIdAutomaticallyNotSet() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();

         ClientSessionFactory sf = createInVMFactory();

         ClientSession session = sf.createSession(false, true, true);

         session.createQueue(groupTestQ, groupTestQ, null, false);

         ClientProducer producer = session.createProducer(groupTestQ);

         final CountDownLatch latch = new CountDownLatch(100);

         MyMessageHandler myMessageHandler = new MyMessageHandler(latch);
         MyMessageHandler myMessageHandler2 = new MyMessageHandler(latch);

         ClientConsumer consumer = session.createConsumer(groupTestQ);
         consumer.setMessageHandler(myMessageHandler);
         ClientConsumer consumer2 = session.createConsumer(groupTestQ);
         consumer2.setMessageHandler(myMessageHandler2);

         session.start();

         final int numMessages = 100;

         for (int i = 0; i < numMessages; i++)
         {
            producer.send(session.createClientMessage(false));
         }
         latch.await();

         session.close();

         assertEquals(myMessageHandler.messagesReceived, 50);
         assertEquals(myMessageHandler2.messagesReceived, 50);
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }

   }

   private static class MyMessageHandler implements MessageHandler
   {
      volatile int messagesReceived = 0;

      private final CountDownLatch latch;

      public MyMessageHandler(CountDownLatch latch)
      {
         this.latch = latch;
      }

      public void onMessage(ClientMessage message)
      {
         messagesReceived++;
         try
         {
            message.acknowledge();
         }
         catch (MessagingException e)
         {
            e.printStackTrace();
         }
         latch.countDown();
      }
   }
}
