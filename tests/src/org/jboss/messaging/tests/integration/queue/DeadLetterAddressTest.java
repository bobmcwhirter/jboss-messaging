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
package org.jboss.messaging.tests.integration.queue;

import org.jboss.messaging.tests.util.UnitTestCase;
import org.jboss.messaging.core.server.MessagingService;
import org.jboss.messaging.core.server.impl.MessagingServiceImpl;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.ClientProducer;
import org.jboss.messaging.core.client.ClientConsumer;
import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.config.impl.ConfigurationImpl;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.exception.MessagingException;
import org.jboss.messaging.core.settings.impl.QueueSettings;
import org.jboss.messaging.core.transaction.impl.XidImpl;
import org.jboss.messaging.core.message.impl.MessageImpl;
import org.jboss.messaging.util.SimpleString;
import org.jboss.messaging.jms.client.JBossMessage;

import javax.transaction.xa.Xid;
import javax.transaction.xa.XAResource;
import java.util.Map;
import java.util.HashMap;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 */
public class DeadLetterAddressTest extends UnitTestCase
{
   private MessagingService messagingService;

   private ClientSession clientSession;

   public void testBasicSend() throws Exception
   {
      Xid xid = new XidImpl("bq".getBytes(), 0, "gt".getBytes());
      SimpleString dla = new SimpleString("DLA");
      SimpleString qName = new SimpleString("q1");
      QueueSettings queueSettings = new QueueSettings();
      queueSettings.setMaxDeliveryAttempts(1);
      queueSettings.setDeadLetterAddress(dla);
      messagingService.getServer().getQueueSettingsRepository().addMatch(qName.toString(), queueSettings);
      SimpleString dlq = new SimpleString("DLQ1");
      clientSession.createQueue(dla, dlq, null, false, false, false);
      clientSession.createQueue(qName, qName, null, false, false, false);
      ClientProducer producer = clientSession.createProducer(qName);
      producer.send(createTextMessage("heyho!", clientSession));
      clientSession.start();
      clientSession.start(xid, XAResource.TMNOFLAGS);
      ClientConsumer clientConsumer = clientSession.createConsumer(qName);
      ClientMessage m = clientConsumer.receive(500);
      m.acknowledge();
      assertNotNull(m);
      assertEquals(m.getBody().getString(), "heyho!");
      //force a cancel
      clientSession.end(xid, XAResource.TMSUCCESS);
      clientSession.rollback(xid);
      m = clientConsumer.receive(500);
      assertNull(m);
      clientConsumer.close();
      clientConsumer = clientSession.createConsumer(dlq);
      m = clientConsumer.receive(500);
      assertNotNull(m);
      assertEquals(m.getBody().getString(), "heyho!");
   }

   public void testBasicSendToMultipleQueues() throws Exception
   {
      Xid xid = new XidImpl("bq".getBytes(), 0, "gt".getBytes());
      SimpleString dla = new SimpleString("DLA");
      SimpleString qName = new SimpleString("q1");
      QueueSettings queueSettings = new QueueSettings();
      queueSettings.setMaxDeliveryAttempts(1);
      queueSettings.setDeadLetterAddress(dla);
      messagingService.getServer().getQueueSettingsRepository().addMatch(qName.toString(), queueSettings);
      SimpleString dlq = new SimpleString("DLQ1");
      SimpleString dlq2 = new SimpleString("DLQ2");
      clientSession.createQueue(dla, dlq, null, false, false, true);
      clientSession.createQueue(dla, dlq2, null, false, false, true);
      clientSession.createQueue(qName, qName, null, false, false, true);
      ClientProducer producer = clientSession.createProducer(qName);
      producer.send(createTextMessage("heyho!", clientSession));
      clientSession.start();
      clientSession.start(xid, XAResource.TMNOFLAGS);
      ClientConsumer clientConsumer = clientSession.createConsumer(qName);
      ClientMessage m = clientConsumer.receive(500);
      m.acknowledge();
      assertNotNull(m);
      assertEquals(m.getBody().getString(), "heyho!");
      //force a cancel
      clientSession.end(xid, XAResource.TMSUCCESS);
      clientSession.rollback(xid);
      clientSession.start(xid, XAResource.TMNOFLAGS);
      m = clientConsumer.receive(500);
      assertNull(m);
      clientConsumer.close();
      clientConsumer = clientSession.createConsumer(dlq);
      m = clientConsumer.receive(500);
      assertNotNull(m);
      m.acknowledge();
      assertEquals(m.getBody().getString(), "heyho!");
      clientConsumer.close();
      clientConsumer = clientSession.createConsumer(dlq2);
      m = clientConsumer.receive(500);
      assertNotNull(m);
      m.acknowledge();
      assertEquals(m.getBody().getString(), "heyho!");
      clientConsumer.close();
   }

   public void testBasicSendToNoQueue() throws Exception
   {
      Xid xid = new XidImpl("bq".getBytes(), 0, "gt".getBytes());
      SimpleString qName = new SimpleString("q1");
      QueueSettings queueSettings = new QueueSettings();
      queueSettings.setMaxDeliveryAttempts(1);
      messagingService.getServer().getQueueSettingsRepository().addMatch(qName.toString(), queueSettings);
      clientSession.createQueue(qName, qName, null, false, false, false);
      ClientProducer producer = clientSession.createProducer(qName);
      producer.send(createTextMessage("heyho!", clientSession));
      clientSession.start();
      clientSession.start(xid, XAResource.TMNOFLAGS);
      ClientConsumer clientConsumer = clientSession.createConsumer(qName);
      ClientMessage m = clientConsumer.receive(500);
      m.acknowledge();
      assertNotNull(m);
      assertEquals(m.getBody().getString(), "heyho!");
      //force a cancel
      clientSession.end(xid, XAResource.TMSUCCESS);
      clientSession.rollback(xid);
      m = clientConsumer.receive(500);
      assertNull(m);
      clientConsumer.close();
   }

   public void testHeadersSet() throws Exception
   {
      final int MAX_DELIVERIES = 16;
      final int NUM_MESSAGES = 5;
      Xid xid = new XidImpl("bq".getBytes(), 0, "gt".getBytes());
      SimpleString dla = new SimpleString("DLA");
      SimpleString qName = new SimpleString("q1");
      QueueSettings queueSettings = new QueueSettings();
      queueSettings.setMaxDeliveryAttempts(MAX_DELIVERIES);
      queueSettings.setDeadLetterAddress(dla);
      messagingService.getServer().getQueueSettingsRepository().addMatch(qName.toString(), queueSettings);
      SimpleString dlq = new SimpleString("DLQ1");
      clientSession.createQueue(dla, dlq, null, false, false, false);
      clientSession.createQueue(qName, qName, null, false, false, false);
      ClientSessionFactory sessionFactory = new ClientSessionFactoryImpl(new TransportConfiguration(INVM_CONNECTOR_FACTORY));
      ClientSession sendSession = sessionFactory.createSession(false, true, true);
      ClientProducer producer = sendSession.createProducer(qName);
      Map<String, Long> origIds = new HashMap<String, Long>();

      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         ClientMessage tm = createTextMessage("Message:" + i, clientSession);
         producer.send(tm);
      }

      ClientConsumer clientConsumer = clientSession.createConsumer(qName);
      clientSession.start();

      for (int i = 0; i < MAX_DELIVERIES; i++)
      {
         clientSession.start(xid, XAResource.TMNOFLAGS);
         for (int j = 0; j < NUM_MESSAGES; j++)
         {
            ClientMessage tm = clientConsumer.receive(1000);

            assertNotNull(tm);
            tm.acknowledge();
            if(i == 0)
            {
               origIds.put("Message:" + j, tm.getMessageID());
            }
            assertEquals("Message:" + j, tm.getBody().getString());
         }
         clientSession.end(xid, XAResource.TMSUCCESS);
         clientSession.rollback(xid);
      }

      assertEquals(messagingService.getServer().getPostOffice().getBinding(qName).getQueue().getMessageCount(), 0);
      ClientMessage m = clientConsumer.receive(1000);
      assertNull(m);
      //All the messages should now be in the DLQ

      ClientConsumer cc3 = clientSession.createConsumer(dlq);

      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         ClientMessage tm = cc3.receive(1000);

         assertNotNull(tm);

         String text = tm.getBody().getString();
         assertEquals("Message:" + i, text);

         // Check the headers
         SimpleString origDest =
               (SimpleString) tm.getProperty(MessageImpl.HDR_ORIGIN_QUEUE);

         Long origMessageId =
               (Long) tm.getProperty(MessageImpl.HDR_ORIG_MESSAGE_ID);

         assertEquals(qName, origDest);

         Long origId = origIds.get(text);

         assertEquals(origId, origMessageId);
      }

   }

   @Override
   protected void setUp() throws Exception
   {
      ConfigurationImpl configuration = new ConfigurationImpl();
      configuration.setSecurityEnabled(false);
      TransportConfiguration transportConfig = new TransportConfiguration(INVM_ACCEPTOR_FACTORY);
      configuration.getAcceptorConfigurations().add(transportConfig);
      messagingService = MessagingServiceImpl.newNullStorageMessagingServer(configuration);
      //start the server
      messagingService.start();
      //then we create a client as normal
      ClientSessionFactory sessionFactory = new ClientSessionFactoryImpl(new TransportConfiguration(INVM_CONNECTOR_FACTORY));
      clientSession = sessionFactory.createSession(true, true, false);
   }

   @Override
   protected void tearDown() throws Exception
   {
      if (clientSession != null)
      {
         try
         {
            clientSession.close();
         }
         catch (MessagingException e1)
         {
            //
         }
      }
      if (messagingService != null && messagingService.isStarted())
      {
         try
         {
            messagingService.stop();
         }
         catch (Exception e1)
         {
            //
         }
      }
      messagingService = null;
      clientSession = null;
   }

}