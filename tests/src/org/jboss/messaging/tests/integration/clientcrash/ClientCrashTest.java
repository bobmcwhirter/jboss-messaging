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

package org.jboss.messaging.tests.integration.clientcrash;

import junit.framework.TestCase;

import org.jboss.messaging.core.client.ClientConsumer;
import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.ClientProducer;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.config.impl.ConfigurationImpl;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.message.Message;
import org.jboss.messaging.core.remoting.impl.netty.NettyConnectorFactory;
import org.jboss.messaging.core.server.MessagingService;
import org.jboss.messaging.core.server.impl.MessagingServiceImpl;
import org.jboss.messaging.jms.client.JBossTextMessage;
import org.jboss.messaging.tests.util.SpawnedVMSupport;
import org.jboss.messaging.util.SimpleString;

/**
 * A test that makes sure that a Messaging server cleans up the associated
 * resources when one of its client crashes.
 *
 * @author <a href="tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * @version <tt>$Revision: 4032 $</tt>
 */
public class ClientCrashTest extends TestCase
{
   // Constants -----------------------------------------------------

   public static final SimpleString QUEUE = new SimpleString("ClientCrashTestQueue");
   public static final String MESSAGE_TEXT_FROM_SERVER = "ClientCrashTest from server";
   public static final String MESSAGE_TEXT_FROM_CLIENT = "ClientCrashTest from client";

   // Static --------------------------------------------------------

   private static final Logger log = Logger.getLogger(ClientCrashTest.class);

   // Attributes ----------------------------------------------------

   private MessagingService messagingService;
   private ClientSessionFactory sf;

   // Constructors --------------------------------------------------

   public ClientCrashTest(String name)
   {
      super(name);
   }

   // Public --------------------------------------------------------

   public void testCrashClientWithOneConnection() throws Exception
   {
      crashClient(1);
   }

   public void testCrashClientWithTwoConnections() throws Exception
   {
      crashClient(2);
   }

   public void crashClient(int numberOfConnectionsOnTheClient) throws Exception
   {
      assertActiveConnections(0);

      // spawn a JVM that creates a JMS client, which waits to receive a test
      // message
      Process p = SpawnedVMSupport.spawnVM(CrashClient.class
              .getName(), new String[]{Integer
              .toString(numberOfConnectionsOnTheClient)});

      ClientSession session = sf.createSession(false, true, true, false);
      session.createQueue(QUEUE, QUEUE, null, false, false);
      ClientConsumer consumer = session.createConsumer(QUEUE);
      ClientProducer producer = session.createProducer(QUEUE);

      session.start();

      // send the message to the queue
      Message messageFromClient = consumer.receive(5000);
      assertNotNull("no message received", messageFromClient);
      assertEquals(MESSAGE_TEXT_FROM_CLIENT, messageFromClient.getBody().getString());

      assertActiveConnections(1 + 1); //One local and one from the other vm

      ClientMessage message = session.createClientMessage(JBossTextMessage.TYPE, false, 0,
              System.currentTimeMillis(), (byte) 1);
      message.getBody().putString(ClientCrashTest.MESSAGE_TEXT_FROM_SERVER);
      producer.send(message);

      log.debug("waiting for the client VM to crash ...");
      p.waitFor();

      assertEquals(9, p.exitValue());

      Thread.sleep(4000);
      // the crash must have been detected and the client resources cleaned
      // up only the local connection remains
      assertActiveConnections(1);

      session.close();

      Thread.sleep(1000);

      assertActiveConnections(0);
   }

   // Package protected ---------------------------------------------

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      ConfigurationImpl config = new ConfigurationImpl();
      config.setSecurityEnabled(false);
      config.getAcceptorConfigurations().add(new TransportConfiguration("org.jboss.messaging.core.remoting.impl.netty.NettyAcceptorFactory"));
      messagingService = MessagingServiceImpl.newNullStorageMessagingServer(config);
      messagingService.start();

      sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.jboss.messaging.core.remoting.impl.netty.NettyConnectorFactory"));
      sf.setPingPeriod(2000);
      
   }

   @Override
   protected void tearDown() throws Exception
   {
      messagingService.stop();

      super.tearDown();
   }

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   private void assertActiveConnections(int expectedActiveConnections)
           throws Exception
   {
      assertEquals(expectedActiveConnections, messagingService.getServer().getServerManagement().getConnectionCount());
   }

   // Inner classes -------------------------------------------------

}
