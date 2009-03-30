/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.messaging.tests.integration.cluster.failover;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.messaging.core.client.ClientConsumer;
import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.ClientProducer;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.MessageHandler;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryInternal;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.exception.MessagingException;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.impl.invm.InVMRegistry;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.jms.client.JBossBytesMessage;
import org.jboss.messaging.jms.client.JBossTextMessage;
import org.jboss.messaging.utils.SimpleString;

/**
 * A MultiThreadRandomFailoverTestBase
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * 
 *
 */
public abstract class MultiThreadRandomFailoverTestBase extends MultiThreadFailoverSupport
{

   private final Logger log = Logger.getLogger(getClass());

   // Constants -----------------------------------------------------

   private static final int RECEIVE_TIMEOUT = 30000;

   private final int LATCH_WAIT = getLatchWait();

   private int NUM_THREADS = getNumThreads();

   // Attributes ----------------------------------------------------
   protected static final SimpleString ADDRESS = new SimpleString("FailoverTestAddress");

   protected MessagingServer liveServer;

   protected MessagingServer backupServer;

   protected final Map<String, Object> backupParams = new HashMap<String, Object>();

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testA() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestA(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   public void testB() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestB(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   public void testC() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestC(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   public void testD() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestD(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   public void testE() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestE(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   public void testF() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestF(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   public void testG() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestG(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   public void testH() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestH(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   public void testI() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestI(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   public void testJ() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestJ(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   public void testK() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestK(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   public void testL() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestL(sf);
         }
      }, NUM_THREADS, true, 10);
   }

   // public void testM() throws Exception
   // {
   // runTestMultipleThreads(new RunnableT()
   // {
   // public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
   // {
   // doTestM(sf, threadNum);
   // }
   // }, NUM_THREADS);
   // }

   public void testN() throws Exception
   {
      runTestMultipleThreads(new RunnableT()
      {
         @Override
         public void run(final ClientSessionFactory sf, final int threadNum) throws Exception
         {
            doTestN(sf, threadNum);
         }
      }, NUM_THREADS, false);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   protected abstract void start() throws Exception;

   protected abstract void setBody(ClientMessage message) throws Exception;

   protected abstract boolean checkSize(ClientMessage message);

   protected int getNumThreads()
   {
      return 10;
   }

   protected ClientSession createAutoCommitSession(ClientSessionFactory sf) throws Exception
   {
      return sf.createSession(false, true, true);
   }

   protected ClientSession createTransactionalSession(ClientSessionFactory sf) throws Exception
   {
      return sf.createSession(false, false, false);
   }

   protected void doTestA(final ClientSessionFactory sf, final int threadNum, final ClientSession session2) throws Exception
   {
      SimpleString subName = new SimpleString("sub" + threadNum);

      ClientSession session = sf.createSession(false, true, true);

      session.createQueue(ADDRESS, subName, null, false);

      ClientProducer producer = session.createProducer(ADDRESS);

      ClientConsumer consumer = session.createConsumer(subName);

      final int numMessages = 100;

      sendMessages(session, producer, numMessages, threadNum);

      session.start();

      MyHandler handler = new MyHandler(threadNum, numMessages);

      consumer.setMessageHandler(handler);

      boolean ok = handler.latch.await(LATCH_WAIT, TimeUnit.MILLISECONDS);

      if (!ok)
      {
         throw new Exception("Timed out waiting for messages on handler " + System.identityHashCode(handler) +
                             " threadnum " +
                             threadNum);
      }

      if (handler.failure != null)
      {
         throw new Exception("Handler failed: " + handler.failure);
      }

      producer.close();

      consumer.close();

      session.deleteQueue(subName);

      session.close();
   }

   protected void doTestA(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      long start = System.currentTimeMillis();

      ClientSession s = sf.createSession(false, false, false);

      final int numMessages = 100;

      final int numSessions = 10;

      Set<ClientConsumer> consumers = new HashSet<ClientConsumer>();
      Set<ClientSession> sessions = new HashSet<ClientSession>();

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         ClientSession sessConsume = createAutoCommitSession(sf);

         sessConsume.start();

         sessConsume.createQueue(ADDRESS, subName, null, false);

         ClientConsumer consumer = sessConsume.createConsumer(subName);

         consumers.add(consumer);

         sessions.add(sessConsume);

      }

      ClientSession sessSend = sf.createSession(false, true, true);

      ClientProducer producer = sessSend.createProducer(ADDRESS);

      sendMessages(sessSend, producer, numMessages, threadNum);

      Set<MyHandler> handlers = new HashSet<MyHandler>();

      for (ClientConsumer consumer : consumers)
      {
         MyHandler handler = new MyHandler(threadNum, numMessages);

         consumer.setMessageHandler(handler);

         handlers.add(handler);
      }

      for (MyHandler handler : handlers)
      {
         boolean ok = handler.latch.await(LATCH_WAIT, TimeUnit.MILLISECONDS);

         if (!ok)
         {
            throw new Exception("Timed out waiting for messages on handler " + System.identityHashCode(handler) +
                                " threadnum " +
                                threadNum);
         }

         if (handler.failure != null)
         {
            throw new Exception("Handler failed: " + handler.failure);
         }
      }

      sessSend.close();

      for (ClientSession session : sessions)
      {
         session.close();
      }

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         s.deleteQueue(subName);
      }

      s.close();

      long end = System.currentTimeMillis();

      log.info("duration " + (end - start));
   }

   protected void doTestB(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      long start = System.currentTimeMillis();

      ClientSession s = sf.createSession(false, false, false);

      final int numMessages = 100;

      final int numSessions = 10;

      Set<ClientConsumer> consumers = new HashSet<ClientConsumer>();
      Set<ClientSession> sessions = new HashSet<ClientSession>();

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         ClientSession sessConsume = createAutoCommitSession(sf);

         sessConsume.createQueue(ADDRESS, subName, null, false);

         ClientConsumer consumer = sessConsume.createConsumer(subName);

         consumers.add(consumer);

         sessions.add(sessConsume);
      }

      ClientSession sessSend = sf.createSession(false, true, true);

      ClientProducer producer = sessSend.createProducer(ADDRESS);

      sendMessages(sessSend, producer, numMessages, threadNum);

      for (ClientSession session : sessions)
      {
         session.start();
      }

      Set<MyHandler> handlers = new HashSet<MyHandler>();

      for (ClientConsumer consumer : consumers)
      {
         MyHandler handler = new MyHandler(threadNum, numMessages);

         consumer.setMessageHandler(handler);

         handlers.add(handler);
      }

      for (MyHandler handler : handlers)
      {
         boolean ok = handler.latch.await(LATCH_WAIT, TimeUnit.MILLISECONDS);

         if (!ok)
         {
            throw new Exception("Timed out waiting for messages on handler " + System.identityHashCode(handler) +
                                " threadnum " +
                                threadNum);
         }

         if (handler.failure != null)
         {
            throw new Exception("Handler failed: " + handler.failure);
         }
      }

      sessSend.close();

      for (ClientSession session : sessions)
      {
         session.close();
      }

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         s.deleteQueue(subName);
      }

      s.close();

      long end = System.currentTimeMillis();

      log.info("duration " + (end - start));

   }

   protected void doTestC(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      long start = System.currentTimeMillis();

      ClientSession s = sf.createSession(false, false, false);

      final int numMessages = 100;

      final int numSessions = 10;

      Set<ClientConsumer> consumers = new HashSet<ClientConsumer>();
      Set<ClientSession> sessions = new HashSet<ClientSession>();

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         ClientSession sessConsume = createTransactionalSession(sf);

         sessConsume.start();

         sessConsume.createQueue(ADDRESS, subName, null, false);

         ClientConsumer consumer = sessConsume.createConsumer(subName);

         consumers.add(consumer);

         sessions.add(sessConsume);
      }

      ClientSession sessSend = sf.createSession(false, false, false);

      ClientProducer producer = sessSend.createProducer(ADDRESS);

      sendMessages(sessSend, producer, numMessages, threadNum);

      sessSend.rollback();

      sendMessages(sessSend, producer, numMessages, threadNum);

      sessSend.commit();

      Set<MyHandler> handlers = new HashSet<MyHandler>();

      for (ClientConsumer consumer : consumers)
      {
         MyHandler handler = new MyHandler(threadNum, numMessages);

         consumer.setMessageHandler(handler);

         handlers.add(handler);
      }

      for (MyHandler handler : handlers)
      {
         boolean ok = handler.latch.await(LATCH_WAIT, TimeUnit.MILLISECONDS);

         if (!ok)
         {
            throw new Exception("Timed out waiting for messages on handler " + System.identityHashCode(handler) +
                                " threadnum " +
                                threadNum);
         }

         if (handler.failure != null)
         {
            throw new Exception("Handler failed: " + handler.failure);
         }

         handler.reset();
      }

      for (ClientSession session : sessions)
      {
         session.rollback();
      }

      for (MyHandler handler : handlers)
      {
         boolean ok = handler.latch.await(LATCH_WAIT, TimeUnit.MILLISECONDS);

         assertTrue(ok);
      }

      for (ClientSession session : sessions)
      {
         session.commit();
      }

      sessSend.close();
      for (ClientSession session : sessions)
      {
         session.close();
      }

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         s.deleteQueue(subName);
      }

      s.close();

      long end = System.currentTimeMillis();

      log.info("duration " + (end - start));
   }

   protected void doTestD(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      long start = System.currentTimeMillis();

      ClientSession s = sf.createSession(false, false, false);

      final int numMessages = 100;

      final int numSessions = 10;

      Set<ClientConsumer> consumers = new HashSet<ClientConsumer>();
      Set<ClientSession> sessions = new HashSet<ClientSession>();

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + " sub" + i);

         ClientSession sessConsume = sf.createSession(false, false, false);

         sessConsume.createQueue(ADDRESS, subName, null, false);

         ClientConsumer consumer = sessConsume.createConsumer(subName);

         consumers.add(consumer);

         sessions.add(sessConsume);
      }

      ClientSession sessSend = sf.createSession(false, false, false);

      ClientProducer producer = sessSend.createProducer(ADDRESS);

      sendMessages(sessSend, producer, numMessages, threadNum);

      sessSend.rollback();

      sendMessages(sessSend, producer, numMessages, threadNum);

      sessSend.commit();

      for (ClientSession session : sessions)
      {
         session.start();
      }

      Set<MyHandler> handlers = new HashSet<MyHandler>();

      for (ClientConsumer consumer : consumers)
      {
         MyHandler handler = new MyHandler(threadNum, numMessages);

         consumer.setMessageHandler(handler);

         handlers.add(handler);
      }

      for (MyHandler handler : handlers)
      {
         boolean ok = handler.latch.await(LATCH_WAIT, TimeUnit.MILLISECONDS);

         if (!ok)
         {
            throw new Exception("Timed out waiting for messages on handler " + System.identityHashCode(handler) +
                                " threadnum " +
                                threadNum);
         }

         if (handler.failure != null)
         {
            throw new Exception("Handler failed: " + handler.failure);
         }
      }

      handlers.clear();

      // Set handlers to null
      for (ClientConsumer consumer : consumers)
      {
         consumer.setMessageHandler(null);
      }

      for (ClientSession session : sessions)
      {
         session.rollback();
      }

      // New handlers
      for (ClientConsumer consumer : consumers)
      {
         MyHandler handler = new MyHandler(threadNum, numMessages);

         consumer.setMessageHandler(handler);

         handlers.add(handler);
      }

      for (MyHandler handler : handlers)
      {
         boolean ok = handler.latch.await(LATCH_WAIT, TimeUnit.MILLISECONDS);

         if (!ok)
         {
            throw new Exception("Timed out waiting for messages on handler " + System.identityHashCode(handler) +
                                " threadnum " +
                                threadNum);
         }

         if (handler.failure != null)
         {
            throw new Exception("Handler failed on rollback: " + handler.failure);
         }
      }

      for (ClientSession session : sessions)
      {
         session.commit();
      }

      sessSend.close();
      for (ClientSession session : sessions)
      {
         session.close();
      }

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + " sub" + i);

         s.deleteQueue(subName);
      }

      s.close();

      long end = System.currentTimeMillis();

      log.info("duration " + (end - start));
   }

   // Now with synchronous receive()

   protected void doTestE(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      long start = System.currentTimeMillis();

      ClientSession s = sf.createSession(false, false, false);

      final int numMessages = 100;

      final int numSessions = 10;

      Set<ClientConsumer> consumers = new HashSet<ClientConsumer>();
      Set<ClientSession> sessions = new HashSet<ClientSession>();

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         ClientSession sessConsume = sf.createSession(false, true, true);

         sessConsume.start();

         sessConsume.createQueue(ADDRESS, subName, null, false);

         ClientConsumer consumer = sessConsume.createConsumer(subName);

         consumers.add(consumer);

         sessions.add(sessConsume);
      }

      ClientSession sessSend = sf.createSession(false, true, true);

      ClientProducer producer = sessSend.createProducer(ADDRESS);

      sendMessages(sessSend, producer, numMessages, threadNum);

      consumeMessages(consumers, numMessages, threadNum);

      sessSend.close();
      for (ClientSession session : sessions)
      {
         session.close();
      }

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         s.deleteQueue(subName);
      }

      s.close();

      long end = System.currentTimeMillis();

      log.info("duration " + (end - start));
   }

   protected void doTestF(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      long start = System.currentTimeMillis();

      ClientSession s = sf.createSession(false, false, false);

      final int numMessages = 100;

      final int numSessions = 10;

      Set<ClientConsumer> consumers = new HashSet<ClientConsumer>();
      Set<ClientSession> sessions = new HashSet<ClientSession>();

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         ClientSession sessConsume = sf.createSession(false, true, true);

         sessConsume.createQueue(ADDRESS, subName, null, false);

         ClientConsumer consumer = sessConsume.createConsumer(subName);

         consumers.add(consumer);

         sessions.add(sessConsume);
      }

      ClientSession sessSend = sf.createSession(false, true, true);

      ClientProducer producer = sessSend.createProducer(ADDRESS);

      sendMessages(sessSend, producer, numMessages, threadNum);

      for (ClientSession session : sessions)
      {
         session.start();
      }

      consumeMessages(consumers, numMessages, threadNum);

      sessSend.close();
      for (ClientSession session : sessions)
      {
         session.close();
      }

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         s.deleteQueue(subName);
      }

      s.close();

      long end = System.currentTimeMillis();

      log.info("duration " + (end - start));
   }

   protected void doTestG(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      long start = System.currentTimeMillis();

      ClientSession s = sf.createSession(false, false, false);

      final int numMessages = 100;

      final int numSessions = 10;

      Set<ClientConsumer> consumers = new HashSet<ClientConsumer>();
      Set<ClientSession> sessions = new HashSet<ClientSession>();

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         ClientSession sessConsume = sf.createSession(false, false, false);

         sessConsume.start();

         sessConsume.createQueue(ADDRESS, subName, null, false);

         ClientConsumer consumer = sessConsume.createConsumer(subName);

         consumers.add(consumer);

         sessions.add(sessConsume);
      }

      ClientSession sessSend = sf.createSession(false, false, false);

      ClientProducer producer = sessSend.createProducer(ADDRESS);

      sendMessages(sessSend, producer, numMessages, threadNum);

      sessSend.rollback();

      sendMessages(sessSend, producer, numMessages, threadNum);

      sessSend.commit();

      consumeMessages(consumers, numMessages, threadNum);

      for (ClientSession session : sessions)
      {
         session.rollback();
      }

      consumeMessages(consumers, numMessages, threadNum);

      for (ClientSession session : sessions)
      {
         session.commit();
      }

      sessSend.close();
      for (ClientSession session : sessions)
      {
         session.close();
      }

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         s.deleteQueue(subName);
      }

      s.close();

      long end = System.currentTimeMillis();

      log.info("duration " + (end - start));
   }

   protected void doTestH(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      long start = System.currentTimeMillis();

      ClientSession s = sf.createSession(false, false, false);

      final int numMessages = 100;

      final int numSessions = 10;

      Set<ClientConsumer> consumers = new HashSet<ClientConsumer>();
      Set<ClientSession> sessions = new HashSet<ClientSession>();

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         ClientSession sessConsume = sf.createSession(false, false, false);

         sessConsume.createQueue(ADDRESS, subName, null, false);

         ClientConsumer consumer = sessConsume.createConsumer(subName);

         consumers.add(consumer);

         sessions.add(sessConsume);
      }

      ClientSession sessSend = sf.createSession(false, false, false);

      ClientProducer producer = sessSend.createProducer(ADDRESS);

      sendMessages(sessSend, producer, numMessages, threadNum);

      sessSend.rollback();

      sendMessages(sessSend, producer, numMessages, threadNum);

      sessSend.commit();

      for (ClientSession session : sessions)
      {
         session.start();
      }

      consumeMessages(consumers, numMessages, threadNum);

      for (ClientSession session : sessions)
      {
         session.rollback();
      }

      consumeMessages(consumers, numMessages, threadNum);

      for (ClientSession session : sessions)
      {
         session.commit();
      }

      sessSend.close();
      for (ClientSession session : sessions)
      {
         session.close();
      }

      for (int i = 0; i < numSessions; i++)
      {
         SimpleString subName = new SimpleString(threadNum + "sub" + i);

         s.deleteQueue(subName);
      }

      s.close();

      long end = System.currentTimeMillis();

      log.info("duration " + (end - start));
   }

   protected void doTestI(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      ClientSession sessCreate = sf.createSession(false, true, true);

      sessCreate.createQueue(ADDRESS, new SimpleString(threadNum + ADDRESS.toString()), null, false);

      ClientSession sess = sf.createSession(false, true, true);

      sess.start();

      ClientConsumer consumer = sess.createConsumer(new SimpleString(threadNum + ADDRESS.toString()));

      ClientProducer producer = sess.createProducer(ADDRESS);

      ClientMessage message = sess.createClientMessage(JBossTextMessage.TYPE,
                                                       false,
                                                       0,
                                                       System.currentTimeMillis(),
                                                       (byte)1);
      producer.send(message);

      ClientMessage message2 = consumer.receive(RECEIVE_TIMEOUT);

      assertNotNull(message2);

      message2.acknowledge();

      sess.close();

      sessCreate.deleteQueue(new SimpleString(threadNum + ADDRESS.toString()));

      sessCreate.close();
   }

   protected void doTestJ(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      ClientSession sessCreate = sf.createSession(false, true, true);

      sessCreate.createQueue(ADDRESS, new SimpleString(threadNum + ADDRESS.toString()), null, false);

      ClientSession sess = sf.createSession(false, true, true);

      sess.start();

      ClientConsumer consumer = sess.createConsumer(new SimpleString(threadNum + ADDRESS.toString()));

      ClientProducer producer = sess.createProducer(ADDRESS);

      ClientMessage message = sess.createClientMessage(JBossTextMessage.TYPE,
                                                       false,
                                                       0,
                                                       System.currentTimeMillis(),
                                                       (byte)1);
      producer.send(message);

      ClientMessage message2 = consumer.receive(RECEIVE_TIMEOUT);

      assertNotNull(message2);

      message2.acknowledge();

      sess.close();

      sessCreate.deleteQueue(new SimpleString(threadNum + ADDRESS.toString()));

      sessCreate.close();
   }

   protected void doTestK(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      ClientSession s = sf.createSession(false, false, false);

      s.createQueue(ADDRESS, new SimpleString(threadNum + ADDRESS.toString()), null, false);

      final int numConsumers = 100;

      for (int i = 0; i < numConsumers; i++)
      {
         ClientConsumer consumer = s.createConsumer(new SimpleString(threadNum + ADDRESS.toString()));

         consumer.close();
      }

      s.deleteQueue(new SimpleString(threadNum + ADDRESS.toString()));

      s.close();
   }

   /*
    * This test tests failure during create connection
    */
   protected void doTestL(final ClientSessionFactory sf) throws Exception
   {
      final int numSessions = 10;

      for (int i = 0; i < numSessions; i++)
      {
         ClientSession session = sf.createSession(false, false, false);

         session.close();
      }
   }

   // Browsers
   // FIXME - this test won't work until we use a proper iterator for browsing a queue.
   // Making a copy of the queue for a browser consumer doesn't work well with replication since
   // When replicating the create consumer (browser) to the backup, when executed on the backup the
   // backup may have different messages in its queue since been added on different threads.
   // So when replicating deliveries they may not be found.
   // https://jira.jboss.org/jira/browse/JBMESSAGING-1433
   // protected void doTestM(final ClientSessionFactory sf, final int threadNum) throws Exception
   // {
   // long start = System.currentTimeMillis();
   //
   // ClientSession sessSend = sf.createSession(false, true, true, false);
   //      
   // ClientSession sessConsume = sf.createSession(false, true, true, false);
   //      
   // sessConsume.createQueue(ADDRESS, new SimpleString(threadNum + "sub"), null, false, false);
   //
   // final int numMessages = 100;
   //
   // ClientProducer producer = sessSend.createProducer(ADDRESS);
   //
   // sendMessages(sessSend, producer, numMessages, threadNum);
   //      
   // ClientConsumer browser = sessConsume.createConsumer(new SimpleString(threadNum + "sub"),
   // null, false, true);
   //      
   // Map<Integer, Integer> consumerCounts = new HashMap<Integer, Integer>();
   //      
   // for (int i = 0; i < numMessages; i++)
   // {
   // ClientMessage msg = browser.receive(RECEIVE_TIMEOUT);
   //
   // assertNotNull(msg);
   //
   // int tn = (Integer)msg.getProperty(new SimpleString("threadnum"));
   // int cnt = (Integer)msg.getProperty(new SimpleString("count"));
   //
   // Integer c = consumerCounts.get(tn);
   // if (c == null)
   // {
   // c = new Integer(cnt);
   // }
   //
   // if (cnt != c.intValue())
   // {
   // throw new Exception("Invalid count, expected " + c + " got " + cnt);
   // }
   //         
   // c++;
   //         
   // //Wrap
   // if (c == numMessages)
   // {
   // c = 0;
   // }
   //         
   // consumerCounts.put(tn, c);
   //
   // msg.acknowledge();
   // }
   //
   // sessConsume.close();
   //      
   // sessConsume = sf.createSession(false, true, true, false);
   //      
   // browser = sessConsume.createConsumer(new SimpleString(threadNum + "sub"),
   // null, false, true);
   //      
   // //Messages should still be there
   //      
   // consumerCounts.clear();
   //      
   // for (int i = 0; i < numMessages; i++)
   // {
   // ClientMessage msg = browser.receive(RECEIVE_TIMEOUT);
   //
   // assertNotNull(msg);
   //
   // int tn = (Integer)msg.getProperty(new SimpleString("threadnum"));
   // int cnt = (Integer)msg.getProperty(new SimpleString("count"));
   //
   // Integer c = consumerCounts.get(tn);
   // if (c == null)
   // {
   // c = new Integer(cnt);
   // }
   //
   // if (cnt != c.intValue())
   // {
   // throw new Exception("Invalid count, expected " + c + " got " + cnt);
   // }
   //         
   // c++;
   //         
   // //Wrap
   // if (c == numMessages)
   // {
   // c = 0;
   // }
   //         
   // consumerCounts.put(tn, c);
   //
   // msg.acknowledge();
   // }
   //      
   // sessConsume.close();
   //      
   // sessSend.deleteQueue(new SimpleString(threadNum + "sub"));
   //      
   // sessSend.close();
   //
   // long end = System.currentTimeMillis();
   //
   // log.info("duration " + (end - start));
   // }

   protected void doTestN(final ClientSessionFactory sf, final int threadNum) throws Exception
   {
      ClientSession sessCreate = sf.createSession(false, true, true);

      sessCreate.createQueue(ADDRESS, new SimpleString(threadNum + ADDRESS.toString()), null, false);

      ClientSession sess = sf.createSession(false, true, true);

      sess.stop();

      sess.start();

      sess.stop();

      ClientConsumer consumer = sess.createConsumer(new SimpleString(threadNum + ADDRESS.toString()));

      ClientProducer producer = sess.createProducer(ADDRESS);

      ClientMessage message = sess.createClientMessage(JBossTextMessage.TYPE,
                                                       false,
                                                       0,
                                                       System.currentTimeMillis(),
                                                       (byte)1);
      producer.send(message);

      sess.start();

      ClientMessage message2 = consumer.receive(RECEIVE_TIMEOUT);

      assertNotNull(message2);

      message2.acknowledge();

      sess.stop();

      sess.start();

      sess.close();

      sessCreate.deleteQueue(new SimpleString(threadNum + ADDRESS.toString()));

      sessCreate.close();
   }

   protected int getLatchWait()
   {
      return 20000;
   }

   protected int getNumIterations()
   {
      return 2;
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      log.info("************ Starting test " + getName());
   }

   @Override
   protected void tearDown() throws Exception
   {
      log.info("************* Ending test " + getName());

      if (liveServer != null && liveServer.isStarted())
      {
         liveServer.stop();
      }
      if (backupServer != null && backupServer.isStarted())
      {
         backupServer.stop();
      }

      super.tearDown();
   }

   // Private -------------------------------------------------------

   private void runTestMultipleThreads(final RunnableT runnable,
                                       final int numThreads,
                                       final boolean failOnCreateConnection) throws Exception
   {
      this.runTestMultipleThreads(runnable, numThreads, failOnCreateConnection, 1000);
   }

   private void runTestMultipleThreads(final RunnableT runnable,
                                       final int numThreads,
                                       final boolean failOnCreateConnection,
                                       final long failDelay) throws Exception
   {

      runMultipleThreadsFailoverTest(runnable, numThreads, getNumIterations(), failOnCreateConnection, failDelay);
   }

   /**
    * @return
    */
   protected ClientSessionFactoryInternal createSessionFactory()
   {
      final ClientSessionFactoryInternal sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory"),
                                                                           new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory",
                                                                                                      backupParams),
                                                                           ClientSessionFactoryImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN,
                                                                           ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL,
                                                                           ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                                                                           -1);

      sf.setSendWindowSize(32 * 1024);
      return sf;
   }

   protected void stop() throws Exception
   {
      backupServer.stop();

      liveServer.stop();

      assertEquals(0, InVMRegistry.instance.size());
   }

   private void sendMessages(final ClientSession sessSend,
                             final ClientProducer producer,
                             final int numMessages,
                             final int threadNum) throws Exception
   {
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = sessSend.createClientMessage(JBossBytesMessage.TYPE,
                                                              false,
                                                              0,
                                                              System.currentTimeMillis(),
                                                              (byte)1);
         message.putIntProperty(new SimpleString("threadnum"), threadNum);
         message.putIntProperty(new SimpleString("count"), i);
         setBody(message);
         producer.send(message);
      }
   }

   private void consumeMessages(final Set<ClientConsumer> consumers, final int numMessages, final int threadNum) throws Exception
   {
      // We make sure the messages arrive in the order they were sent from a particular producer
      Map<ClientConsumer, Map<Integer, Integer>> counts = new HashMap<ClientConsumer, Map<Integer, Integer>>();

      for (int i = 0; i < numMessages; i++)
      {
         for (ClientConsumer consumer : consumers)
         {
            Map<Integer, Integer> consumerCounts = counts.get(consumer);

            if (consumerCounts == null)
            {
               consumerCounts = new HashMap<Integer, Integer>();
               counts.put(consumer, consumerCounts);
            }

            ClientMessage msg = consumer.receive(RECEIVE_TIMEOUT);

            assertNotNull(msg);

            int tn = (Integer)msg.getProperty(new SimpleString("threadnum"));
            int cnt = (Integer)msg.getProperty(new SimpleString("count"));

            Integer c = consumerCounts.get(tn);
            if (c == null)
            {
               c = new Integer(cnt);
            }

            if (tn == threadNum && cnt != c.intValue())
            {
               throw new Exception("Invalid count, expected " + tn + ": " + c + " got " + cnt);
            }

            c++;

            // Wrap
            if (c == numMessages)
            {
               c = 0;
            }

            consumerCounts.put(tn, c);

            msg.acknowledge();
         }
      }
   }

   // Inner classes -------------------------------------------------

   private class MyHandler implements MessageHandler
   {
      CountDownLatch latch = new CountDownLatch(1);

      private final Map<Integer, Integer> counts = new HashMap<Integer, Integer>();

      volatile String failure;

      final int tn;

      final int numMessages;

      volatile boolean done;

      synchronized void reset()
      {
         counts.clear();

         done = false;

         failure = null;

         latch = new CountDownLatch(1);
      }

      MyHandler(final int threadNum, final int numMessages)
      {
         tn = threadNum;

         this.numMessages = numMessages;
      }

      public synchronized void onMessage(final ClientMessage message)
      {
         try
         {
            message.acknowledge();
         }
         catch (MessagingException me)
         {
            log.error("Failed to process", me);
         }

         if (done)
         {
            return;
         }

         int threadNum = (Integer)message.getProperty(new SimpleString("threadnum"));
         int cnt = (Integer)message.getProperty(new SimpleString("count"));

         Integer c = counts.get(threadNum);
         if (c == null)
         {
            c = new Integer(cnt);
         }

         if (tn == threadNum && cnt != c.intValue())
         {
            failure = "Invalid count, expected " + threadNum + ":" + c + " got " + cnt;
            log.error(failure);

            latch.countDown();
         }

         if (!checkSize(message))
         {
            failure = "Invalid size on message";
            log.error(failure);
            latch.countDown();
         }

         if (tn == threadNum && c == numMessages - 1)
         {
            done = true;
            latch.countDown();
         }

         c++;
         // Wrap around at numMessages
         if (c == numMessages)
         {
            c = 0;
         }

         counts.put(threadNum, c);

      }
   }
}