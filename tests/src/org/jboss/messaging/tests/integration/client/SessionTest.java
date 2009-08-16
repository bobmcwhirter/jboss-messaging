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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.messaging.core.client.ClientConsumer;
import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.ClientProducer;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.exception.MessagingException;
import org.jboss.messaging.core.remoting.FailureListener;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionBindingQueryResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionQueueQueryResponseMessage;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.core.server.Queue;
import org.jboss.messaging.tests.util.ServiceTestBase;
import org.jboss.messaging.utils.SimpleString;

/**
 * This test covers the API for ClientSession altho XA tests are tested seperately.
 *
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 */
public class SessionTest extends ServiceTestBase
{
   private String queueName = "ClientSessionTestQ";

   public void testFailureListener() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         final CountDownLatch latch = new CountDownLatch(1);
         clientSession.addFailureListener(new FailureListener()
         {
            public void connectionFailed(MessagingException me)
            {
               latch.countDown();
            }
         });
         
         //Make sure failure listener is closed if server is stopped without session being closed first
         server.stop();
         assertTrue(latch.await(5, TimeUnit.SECONDS));
         
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testFailureListenerRemoved() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         class MyFailureListener implements FailureListener
         {
            boolean called = false;

            public void connectionFailed(MessagingException me)
            {
               called = true;
            }
         }

         MyFailureListener listener = new MyFailureListener();
         clientSession.addFailureListener(listener);

         assertTrue(clientSession.removeFailureListener(listener));
         clientSession.close();
         server.stop();
         assertFalse(listener.called);
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testBindingQuery() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         clientSession.createQueue("a1", "q1", false);
         clientSession.createQueue("a1", "q2", false);
         clientSession.createQueue("a2", "q3", false);
         clientSession.createQueue("a2", "q4", false);
         clientSession.createQueue("a2", "q5", false);
         SessionBindingQueryResponseMessage resp = clientSession.bindingQuery(new SimpleString("a"));
         List<SimpleString> queues = resp.getQueueNames();
         assertTrue(queues.isEmpty());
         resp = clientSession.bindingQuery(new SimpleString("a1"));
         queues = resp.getQueueNames();
         assertEquals(queues.size(), 2);
         assertTrue(queues.contains(new SimpleString("q1")));
         assertTrue(queues.contains(new SimpleString("q2")));
         resp = clientSession.bindingQuery(new SimpleString("a2"));
         queues = resp.getQueueNames();
         assertEquals(queues.size(), 3);
         assertTrue(queues.contains(new SimpleString("q3")));
         assertTrue(queues.contains(new SimpleString("q4")));
         assertTrue(queues.contains(new SimpleString("q5")));
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testQueueQuery() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         clientSession.createQueue("a1", queueName, false);
         clientSession.createConsumer(queueName);
         clientSession.createConsumer(queueName);
         ClientProducer cp = clientSession.createProducer("a1");
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         SessionQueueQueryResponseMessage resp = clientSession.queueQuery(new SimpleString(queueName));
         assertEquals(new SimpleString("a1"), resp.getAddress());
         assertEquals(2, resp.getConsumerCount());
         assertEquals(2, resp.getMessageCount());
         assertEquals(null, resp.getFilterString());
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testQueueQueryWithFilter() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         clientSession.createQueue("a1", queueName, "foo=bar", false);
         clientSession.createConsumer(queueName);
         clientSession.createConsumer(queueName);
         SessionQueueQueryResponseMessage resp = clientSession.queueQuery(new SimpleString(queueName));
         assertEquals(new SimpleString("a1"), resp.getAddress());
         assertEquals(2, resp.getConsumerCount());
         assertEquals(0, resp.getMessageCount());
         assertEquals(new SimpleString("foo=bar"), resp.getFilterString());
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

    public void testQueueQueryNoQ() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         SessionQueueQueryResponseMessage resp = clientSession.queueQuery(new SimpleString(queueName));
         assertFalse(resp.isExists());
         assertEquals(null, resp.getAddress());
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testClose() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         clientSession.createQueue(queueName, queueName, false);
         ClientProducer p = clientSession.createProducer();
         ClientProducer p1 = clientSession.createProducer(queueName);
         ClientConsumer c = clientSession.createConsumer(queueName);
         ClientConsumer c1 = clientSession.createConsumer(queueName);
         clientSession.close();
         assertTrue(clientSession.isClosed());
         assertTrue(p.isClosed());
         assertTrue(p1.isClosed());
         assertTrue(c.isClosed());
         assertTrue(c1.isClosed());
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testCreateClientMessageNonDurable() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         ClientMessage clientMessage = clientSession.createClientMessage(false);
         assertFalse(clientMessage.isDurable());
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testCreateClientMessageDurable() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         ClientMessage clientMessage = clientSession.createClientMessage(true);
         assertTrue(clientMessage.isDurable());
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testCreateClientMessageType() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         ClientMessage clientMessage = clientSession.createClientMessage((byte) 99, false);
         assertEquals((byte) 99, clientMessage.getType());
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testCreateClientMessageOverrides() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         ClientMessage clientMessage = clientSession.createClientMessage((byte) 88, false, 100l, 300l, (byte) 33);
         assertEquals((byte) 88, clientMessage.getType());
         assertEquals(100l, clientMessage.getExpiration());
         assertEquals(300l, clientMessage.getTimestamp());
         assertEquals((byte) 33, clientMessage.getPriority());
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testGetVersion() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         assertEquals(server.getVersion().getIncrementingVersion(), clientSession.getVersion());
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testStart() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         clientSession.createQueue(queueName, queueName, false);
         clientSession.start();
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testStop() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, true, true);
         clientSession.createQueue(queueName, queueName, false);
         clientSession.start();
         clientSession.stop();
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testCommitWithSend() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, false, true);
         clientSession.createQueue(queueName, queueName, false);
         ClientProducer cp = clientSession.createProducer(queueName);
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         Queue q = (Queue) server.getPostOffice().getBinding(new SimpleString(queueName)).getBindable();
         assertEquals(0, q.getMessageCount());
         clientSession.commit();
         assertEquals(10, q.getMessageCount());
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testRollbackWithSend() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         ClientSession clientSession = cf.createSession(false, false, true);
         clientSession.createQueue(queueName, queueName, false);
         ClientProducer cp = clientSession.createProducer(queueName);
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         Queue q = (Queue) server.getPostOffice().getBinding(new SimpleString(queueName)).getBindable();
         assertEquals(0, q.getMessageCount());
         clientSession.rollback();
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         clientSession.commit();
         assertEquals(2, q.getMessageCount());
         clientSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testCommitWithReceive() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         cf.setBlockOnNonPersistentSend(true);
         cf.setBlockOnPersistentSend(true);
         ClientSession sendSession = cf.createSession(false, true, true);
         ClientProducer cp = sendSession.createProducer(queueName);
         ClientSession clientSession = cf.createSession(false, true, false);
         clientSession.createQueue(queueName, queueName, false);
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         Queue q = (Queue) server.getPostOffice().getBinding(new SimpleString(queueName)).getBindable();
         assertEquals(10, q.getMessageCount());
         ClientConsumer cc = clientSession.createConsumer(queueName);
         clientSession.start();
         ClientMessage m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         clientSession.commit();
         assertEquals(0, q.getMessageCount());
         clientSession.close();
         sendSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }

   public void testRollbackWithReceive() throws Exception
   {
      MessagingServer server = createServer(false);
      try
      {
         server.start();
         ClientSessionFactory cf = createInVMFactory();
         cf.setBlockOnNonPersistentSend(true);
         cf.setBlockOnPersistentSend(true);
         ClientSession sendSession = cf.createSession(false, true, true);
         ClientProducer cp = sendSession.createProducer(queueName);
         ClientSession clientSession = cf.createSession(false, true, false);
         clientSession.createQueue(queueName, queueName, false);
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         cp.send(clientSession.createClientMessage(false));
         Queue q = (Queue) server.getPostOffice().getBinding(new SimpleString(queueName)).getBindable();
         assertEquals(10, q.getMessageCount());
         ClientConsumer cc = clientSession.createConsumer(queueName);
         clientSession.start();
         ClientMessage m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         m = cc.receive(5000);
         assertNotNull(m);
         m.acknowledge();
         clientSession.rollback();
         assertEquals(10, q.getMessageCount());
         clientSession.close();
         sendSession.close();
      }
      finally
      {
         if (server.isStarted())
         {
            server.stop();
         }
      }
   }
}
