/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.messaging.tests.timing.jms.bridge.impl;

import static org.jboss.messaging.tests.util.RandomUtil.randomString;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.config.impl.ConfigurationImpl;
import org.jboss.messaging.core.remoting.impl.invm.InVMAcceptorFactory;
import org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory;
import org.jboss.messaging.core.server.Messaging;
import org.jboss.messaging.jms.JBossQueue;
import org.jboss.messaging.jms.bridge.ConnectionFactoryFactory;
import org.jboss.messaging.jms.bridge.DestinationFactory;
import org.jboss.messaging.jms.bridge.QualityOfServiceMode;
import org.jboss.messaging.jms.bridge.impl.JMSBridgeImpl;
import org.jboss.messaging.jms.client.JBossConnectionFactory;
import org.jboss.messaging.jms.server.JMSServerManager;
import org.jboss.messaging.jms.server.impl.JMSServerManagerImpl;
import org.jboss.messaging.tests.unit.util.InVMContext;
import org.jboss.messaging.tests.util.UnitTestCase;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 * @version <tt>$Revision$</tt>
 * 
 */
public class JMSBridgeImplTest extends UnitTestCase
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private static final String SOURCE = randomString();

   private static final String TARGET = randomString();

   private JMSServerManager jmsServer;

   // Static --------------------------------------------------------

   protected static TransactionManager newTransactionManager()
   {
      return new TransactionManager()
      {
         public Transaction suspend() throws SystemException
         {
            return null;
         }

         public void setTransactionTimeout(int arg0) throws SystemException
         {
         }

         public void setRollbackOnly() throws IllegalStateException, SystemException
         {
         }

         public void rollback() throws IllegalStateException, SecurityException, SystemException
         {
         }

         public void resume(Transaction arg0) throws InvalidTransactionException,
                                             IllegalStateException,
                                             SystemException
         {
         }

         public Transaction getTransaction() throws SystemException
         {
            return null;
         }

         public int getStatus() throws SystemException
         {
            return 0;
         }

         public void commit() throws RollbackException,
                             HeuristicMixedException,
                             HeuristicRollbackException,
                             SecurityException,
                             IllegalStateException,
                             SystemException
         {
         }

         public void begin() throws NotSupportedException, SystemException
         {
         }
      };
   }

   private static DestinationFactory newDestinationFactory(final Destination dest)
   {
      return new DestinationFactory()
      {
         public Destination createDestination() throws Exception
         {
            return dest;
         }
      };
   };

   private static ConnectionFactoryFactory newConnectionFactoryFactory(final ConnectionFactory cf)
   {
      return new ConnectionFactoryFactory()
      {
         public ConnectionFactory createConnectionFactory() throws Exception
         {
            return cf;
         }
      };
   }

   private static ConnectionFactory createConnectionFactory()
   {
      JBossConnectionFactory cf = new JBossConnectionFactory(new TransportConfiguration(InVMConnectorFactory.class.getName()));
      // Note! We disable automatic reconnection on the session factory. The bridge needs to do the reconnection
      cf.setReconnectAttempts(0);
      cf.setBlockOnNonPersistentSend(true);
      cf.setBlockOnPersistentSend(true);
      return cf;
   }

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testStartWithRepeatedFailure() throws Exception
   {
      JBossConnectionFactory failingSourceCF = new JBossConnectionFactory(new TransportConfiguration(InVMConnectorFactory.class.getName()))
      {
         @Override
         public Connection createConnection() throws JMSException
         {
            throw new JMSException("unable to create a conn");
         }
      };

      ConnectionFactoryFactory sourceCFF = newConnectionFactoryFactory(failingSourceCF);
      ConnectionFactoryFactory targetCFF = newConnectionFactoryFactory(createConnectionFactory());
      DestinationFactory sourceDF = newDestinationFactory(new JBossQueue(SOURCE));
      DestinationFactory targetDF = newDestinationFactory(new JBossQueue(TARGET));
      TransactionManager tm = newTransactionManager();

      JMSBridgeImpl bridge = new JMSBridgeImpl();

      bridge.setSourceConnectionFactoryFactory(sourceCFF);
      bridge.setSourceDestinationFactory(sourceDF);
      bridge.setTargetConnectionFactoryFactory(targetCFF);
      bridge.setTargetDestinationFactory(targetDF);
      // retry after 10 ms
      bridge.setFailureRetryInterval(10);
      // retry only once
      bridge.setMaxRetries(1);
      bridge.setMaxBatchSize(1);
      bridge.setMaxBatchTime(-1);
      bridge.setTransactionManager(tm);
      bridge.setQualityOfServiceMode(QualityOfServiceMode.AT_MOST_ONCE);

      assertFalse(bridge.isStarted());
      bridge.start();

      Thread.sleep(50);
      assertFalse(bridge.isStarted());
      assertTrue(bridge.isFailed());

   }

   public void testStartWithFailureThenSuccess() throws Exception
   {
      JBossConnectionFactory failingSourceCF = new JBossConnectionFactory(new TransportConfiguration(InVMConnectorFactory.class.getName()))
      {
         boolean firstTime = true;

         @Override
         public Connection createConnection() throws JMSException
         {
            if (firstTime)
            {
               firstTime = false;
               throw new JMSException("unable to create a conn");
            }
            else
            {
               return super.createConnection();
            }
         }
      };
      // Note! We disable automatic reconnection on the session factory. The bridge needs to do the reconnection
      failingSourceCF.setReconnectAttempts(0);
      failingSourceCF.setBlockOnNonPersistentSend(true);
      failingSourceCF.setBlockOnPersistentSend(true);

      ConnectionFactoryFactory sourceCFF = newConnectionFactoryFactory(failingSourceCF);
      ConnectionFactoryFactory targetCFF = newConnectionFactoryFactory(createConnectionFactory());
      DestinationFactory sourceDF = newDestinationFactory(new JBossQueue(SOURCE));
      DestinationFactory targetDF = newDestinationFactory(new JBossQueue(TARGET));
      TransactionManager tm = newTransactionManager();

      JMSBridgeImpl bridge = new JMSBridgeImpl();

      bridge.setSourceConnectionFactoryFactory(sourceCFF);
      bridge.setSourceDestinationFactory(sourceDF);
      bridge.setTargetConnectionFactoryFactory(targetCFF);
      bridge.setTargetDestinationFactory(targetDF);
      // retry after 10 ms
      bridge.setFailureRetryInterval(10);
      // retry only once
      bridge.setMaxRetries(1);
      bridge.setMaxBatchSize(1);
      bridge.setMaxBatchTime(-1);
      bridge.setTransactionManager(tm);
      bridge.setQualityOfServiceMode(QualityOfServiceMode.AT_MOST_ONCE);

      assertFalse(bridge.isStarted());
      bridge.start();

      Thread.sleep(500);
      assertTrue(bridge.isStarted());
      assertFalse(bridge.isFailed());

      bridge.stop();
   }

   /*
   * we receive only 1 message. The message is sent when the maxBatchTime
   * expires even if the maxBatchSize is not reached
   */
   public void testSendMessagesWhenMaxBatchTimeExpires() throws Exception
   {
      int maxBatchSize = 2;
      long maxBatchTime = 500;

      ConnectionFactoryFactory sourceCFF = newConnectionFactoryFactory(createConnectionFactory());
      ConnectionFactoryFactory targetCFF = newConnectionFactoryFactory(createConnectionFactory());
      DestinationFactory sourceDF = newDestinationFactory(new JBossQueue(SOURCE));
      DestinationFactory targetDF = newDestinationFactory(new JBossQueue(TARGET));
      TransactionManager tm = newTransactionManager();

      JMSBridgeImpl bridge = new JMSBridgeImpl();
      assertNotNull(bridge);

      bridge.setSourceConnectionFactoryFactory(sourceCFF);
      bridge.setSourceDestinationFactory(sourceDF);
      bridge.setTargetConnectionFactoryFactory(targetCFF);
      bridge.setTargetDestinationFactory(targetDF);
      bridge.setFailureRetryInterval(-1);
      bridge.setMaxRetries(-1);
      bridge.setMaxBatchSize(maxBatchSize);
      bridge.setMaxBatchTime(maxBatchTime);
      bridge.setTransactionManager(tm);
      bridge.setQualityOfServiceMode(QualityOfServiceMode.AT_MOST_ONCE);

      assertFalse(bridge.isStarted());
      bridge.start();
      assertTrue(bridge.isStarted());

      Connection targetConn = createConnectionFactory().createConnection();
      Session targetSess = targetConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageConsumer consumer = targetSess.createConsumer(targetDF.createDestination());
      final List<Message> messages = new LinkedList<Message>();
      MessageListener listener = new MessageListener()
      {

         public void onMessage(Message message)
         {
            messages.add(message);
         }
      };
      consumer.setMessageListener(listener);
      targetConn.start();

      Connection sourceConn = createConnectionFactory().createConnection();
      Session sourceSess = sourceConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageProducer producer = sourceSess.createProducer(sourceDF.createDestination());
      producer.send(sourceSess.createTextMessage());
      sourceConn.close();

      assertEquals(0, messages.size());
      Thread.sleep(3 * maxBatchTime);

      assertEquals(1, messages.size());

      bridge.stop();
      assertFalse(bridge.isStarted());

      targetConn.close();
   }

   public void testExceptionOnSourceAndRetrySucceeds() throws Exception
   {
      final AtomicReference<Connection> sourceConn = new AtomicReference<Connection>();
      JBossConnectionFactory failingSourceCF = new JBossConnectionFactory(new TransportConfiguration(InVMConnectorFactory.class.getName()))
      {
         @Override
         public Connection createConnection() throws JMSException
         {
            sourceConn.set(super.createConnection());
            return sourceConn.get();
         }
      };
      // Note! We disable automatic reconnection on the session factory. The bridge needs to do the reconnection
      failingSourceCF.setReconnectAttempts(0);
      failingSourceCF.setBlockOnNonPersistentSend(true);
      failingSourceCF.setBlockOnPersistentSend(true);

      ConnectionFactoryFactory sourceCFF = newConnectionFactoryFactory(failingSourceCF);
      ConnectionFactoryFactory targetCFF = newConnectionFactoryFactory(createConnectionFactory());
      DestinationFactory sourceDF = newDestinationFactory(new JBossQueue(SOURCE));
      DestinationFactory targetDF = newDestinationFactory(new JBossQueue(TARGET));
      TransactionManager tm = newTransactionManager();

      JMSBridgeImpl bridge = new JMSBridgeImpl();
      assertNotNull(bridge);

      bridge.setSourceConnectionFactoryFactory(sourceCFF);
      bridge.setSourceDestinationFactory(sourceDF);
      bridge.setTargetConnectionFactoryFactory(targetCFF);
      bridge.setTargetDestinationFactory(targetDF);
      bridge.setFailureRetryInterval(10);
      bridge.setMaxRetries(2);
      bridge.setMaxBatchSize(1);
      bridge.setMaxBatchTime(-1);
      bridge.setTransactionManager(tm);
      bridge.setQualityOfServiceMode(QualityOfServiceMode.AT_MOST_ONCE);

      assertFalse(bridge.isStarted());
      bridge.start();
      assertTrue(bridge.isStarted());

      sourceConn.get().getExceptionListener().onException(new JMSException("exception on the source"));
      Thread.sleep(4 * bridge.getFailureRetryInterval());
      // reconnection must have succeeded
      assertTrue(bridge.isStarted());

      bridge.stop();
      assertFalse(bridge.isStarted());
   }

   public void testExceptionOnSourceAndRetryFails() throws Exception
   {
      final AtomicReference<Connection> sourceConn = new AtomicReference<Connection>();
      JBossConnectionFactory failingSourceCF = new JBossConnectionFactory(new TransportConfiguration(InVMConnectorFactory.class.getName()))
      {
         boolean firstTime = true;

         @Override
         public Connection createConnection() throws JMSException
         {
            if (firstTime)
            {
               firstTime = false;
               sourceConn.set(super.createConnection());
               return sourceConn.get();
            }
            else
            {
               throw new JMSException("exception while retrying to connect");
            }
         }
      };
      // Note! We disable automatic reconnection on the session factory. The bridge needs to do the reconnection
      failingSourceCF.setReconnectAttempts(0);
      failingSourceCF.setBlockOnNonPersistentSend(true);
      failingSourceCF.setBlockOnPersistentSend(true);

      ConnectionFactoryFactory sourceCFF = newConnectionFactoryFactory(failingSourceCF);
      ConnectionFactoryFactory targetCFF = newConnectionFactoryFactory(createConnectionFactory());
      DestinationFactory sourceDF = newDestinationFactory(new JBossQueue(SOURCE));
      DestinationFactory targetDF = newDestinationFactory(new JBossQueue(TARGET));
      TransactionManager tm = newTransactionManager();

      JMSBridgeImpl bridge = new JMSBridgeImpl();
      assertNotNull(bridge);

      bridge.setSourceConnectionFactoryFactory(sourceCFF);
      bridge.setSourceDestinationFactory(sourceDF);
      bridge.setTargetConnectionFactoryFactory(targetCFF);
      bridge.setTargetDestinationFactory(targetDF);
      bridge.setFailureRetryInterval(10);
      bridge.setMaxRetries(1);
      bridge.setMaxBatchSize(1);
      bridge.setMaxBatchTime(-1);
      bridge.setTransactionManager(tm);
      bridge.setQualityOfServiceMode(QualityOfServiceMode.AT_MOST_ONCE);

      assertFalse(bridge.isStarted());
      bridge.start();
      assertTrue(bridge.isStarted());

      sourceConn.get().getExceptionListener().onException(new JMSException("exception on the source"));
      Thread.sleep(4 * bridge.getFailureRetryInterval());
      // reconnection must have failed
      assertFalse(bridge.isStarted());

   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      Configuration config = new ConfigurationImpl();
      config.setFileDeploymentEnabled(false);
      config.setSecurityEnabled(false);
      config.getAcceptorConfigurations().add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
      InVMContext context = new InVMContext();
      jmsServer = new JMSServerManagerImpl(Messaging.newMessagingServer(config, false));
      jmsServer.setContext(context);
      jmsServer.start();

      jmsServer.createQueue(SOURCE, "/queue/" + SOURCE, null, true);
      jmsServer.createQueue(TARGET, "/queue/" + TARGET, null, true);

   }

   @Override
   protected void tearDown() throws Exception
   {
      jmsServer.stop();

      super.tearDown();
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
