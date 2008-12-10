/*
 * JBoss, Home of Professional Open Source Copyright 2005-2008, Red Hat
 * Middleware LLC, and individual contributors by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of individual
 * contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.jboss.messaging.tests.integration.cluster.distribution;

import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_MAX_RETRIES_AFTER_FAILOVER;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_MAX_RETRIES_BEFORE_FAILOVER;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL_MULTIPLIER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.messaging.core.client.ClientConsumer;
import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.ClientProducer;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.config.cluster.MessageFlowConfiguration;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.impl.invm.InVMRegistry;
import org.jboss.messaging.core.server.MessagingService;
import org.jboss.messaging.util.Pair;
import org.jboss.messaging.util.SimpleString;

/**
 * 
 * A ActivationTimeoutTest
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Created 4 Nov 2008 16:54:50
 *
 *
 */
public class BasicMessageFlowTest extends MessageFlowTestBase
{
   private static final Logger log = Logger.getLogger(BasicMessageFlowTest.class);

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testMessageFlowsSameName() throws Exception
   {
      Map<String, Object> service0Params = new HashMap<String, Object>();
      MessagingService service0 = createMessagingService(0, service0Params);

      Map<String, Object> service1Params = new HashMap<String, Object>();
      MessagingService service1 = createMessagingService(1, service1Params);
      service1.start();

      TransportConfiguration server0tc = new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory",
                                                                    service0Params);

      Map<String, TransportConfiguration> connectors = new HashMap<String, TransportConfiguration>();

      TransportConfiguration server1tc = new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory",
                                                                    service1Params);
      connectors.put(server1tc.getName(), server1tc);
      service0.getServer().getConfiguration().setConnectorConfigurations(connectors);

      List<Pair<String, String>> connectorNames = new ArrayList<Pair<String, String>>();
      connectorNames.add(new Pair<String, String>(server1tc.getName(), null));

      final SimpleString address1 = new SimpleString("testaddress");

      MessageFlowConfiguration ofconfig1 = new MessageFlowConfiguration("flow1",
                                                                        address1.toString(),
                                                                        "car='saab'",
                                                                        true,
                                                                        1,
                                                                        -1,
                                                                        null,
                                                                        DEFAULT_RETRY_INTERVAL,
                                                                        DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                                                                        DEFAULT_MAX_RETRIES_BEFORE_FAILOVER,
                                                                        DEFAULT_MAX_RETRIES_AFTER_FAILOVER,
                                                                        connectorNames);
      MessageFlowConfiguration ofconfig2 = new MessageFlowConfiguration("flow1",
                                                                        address1.toString(),
                                                                        "car='bmw'",
                                                                        true,
                                                                        1,
                                                                        -1,
                                                                        null,
                                                                        DEFAULT_RETRY_INTERVAL,
                                                                        DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                                                                        DEFAULT_MAX_RETRIES_BEFORE_FAILOVER,
                                                                        DEFAULT_MAX_RETRIES_AFTER_FAILOVER,
                                                                        connectorNames);

      Set<MessageFlowConfiguration> ofconfigs = new HashSet<MessageFlowConfiguration>();
      ofconfigs.add(ofconfig1);
      ofconfigs.add(ofconfig2);

      service0.getServer().getConfiguration().setMessageFlowConfigurations(ofconfigs);

      // Only one of the flows should be deployed
      service0.start();

      ClientSessionFactory csf0 = new ClientSessionFactoryImpl(server0tc);
      ClientSession session0 = csf0.createSession(false, true, true);

      ClientSessionFactory csf1 = new ClientSessionFactoryImpl(server1tc);
      ClientSession session1 = csf1.createSession(false, true, true);

      session0.createQueue(address1, address1, null, false, false, false);
      session1.createQueue(address1, address1, null, false, false, false);
      ClientProducer prod0 = session0.createProducer(address1);

      ClientConsumer cons1 = session1.createConsumer(address1);

      session1.start();

      SimpleString propKey = new SimpleString("car");

      ClientMessage messageSaab = session0.createClientMessage(false);
      messageSaab.putStringProperty(propKey, new SimpleString("saab"));
      messageSaab.getBody().flip();

      ClientMessage messageBMW = session0.createClientMessage(false);
      messageBMW.putStringProperty(propKey, new SimpleString("bmw"));
      messageBMW.getBody().flip();

      prod0.send(messageSaab);
      prod0.send(messageBMW);

      ClientMessage r1 = cons1.receive(1000);
      assertNotNull(r1);

      SimpleString val = (SimpleString)r1.getProperty(propKey);
      assertTrue(val.equals(new SimpleString("saab")) || val.equals(new SimpleString("bmw")));
      r1 = cons1.receiveImmediate();
      assertNull(r1);

      session0.close();
      session1.close();

      service0.stop();
      service1.stop();

      assertEquals(0, service0.getServer().getRemotingService().getConnections().size());
      assertEquals(0, service1.getServer().getRemotingService().getConnections().size());
   }

   public void testMessageNullName() throws Exception
   {
      Map<String, Object> service0Params = new HashMap<String, Object>();
      MessagingService service0 = createMessagingService(0, service0Params);

      Map<String, Object> service1Params = new HashMap<String, Object>();
      MessagingService service1 = createMessagingService(1, service1Params);
      service1.start();

      TransportConfiguration server0tc = new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory",
                                                                    service0Params);

      Map<String, TransportConfiguration> connectors = new HashMap<String, TransportConfiguration>();

      TransportConfiguration server1tc = new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory",
                                                                    service1Params);
      connectors.put(server1tc.getName(), server1tc);
      service0.getServer().getConfiguration().setConnectorConfigurations(connectors);

      List<Pair<String, String>> connectorNames = new ArrayList<Pair<String, String>>();
      connectorNames.add(new Pair<String, String>(server1tc.getName(), null));

      final SimpleString address1 = new SimpleString("testaddress");

      MessageFlowConfiguration ofconfig1 = new MessageFlowConfiguration(null,
                                                                        address1.toString(),
                                                                        null,
                                                                        true,
                                                                        1,
                                                                        -1,
                                                                        null,
                                                                        DEFAULT_RETRY_INTERVAL,
                                                                        DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                                                                        DEFAULT_MAX_RETRIES_BEFORE_FAILOVER,
                                                                        DEFAULT_MAX_RETRIES_AFTER_FAILOVER,
                                                                        connectorNames);

      Set<MessageFlowConfiguration> ofconfigs = new HashSet<MessageFlowConfiguration>();
      ofconfigs.add(ofconfig1);

      service0.getServer().getConfiguration().setMessageFlowConfigurations(ofconfigs);

      service0.start();

      ClientSessionFactory csf0 = new ClientSessionFactoryImpl(server0tc);
      ClientSession session0 = csf0.createSession(false, true, true);

      ClientSessionFactory csf1 = new ClientSessionFactoryImpl(server1tc);
      ClientSession session1 = csf1.createSession(false, true, true);

      session0.createQueue(address1, address1, null, false, false, false);
      session1.createQueue(address1, address1, null, false, false, false);
      ClientProducer prod0 = session0.createProducer(address1);

      ClientConsumer cons1 = session1.createConsumer(address1);

      session1.start();

      SimpleString propKey = new SimpleString("car");

      ClientMessage message = session0.createClientMessage(false);
      message.getBody().flip();

      prod0.send(message);

      ClientMessage r1 = cons1.receive(1000);
      assertNull(r1);

      session0.close();
      session1.close();

      service0.stop();
      service1.stop();

      assertEquals(0, service0.getServer().getRemotingService().getConnections().size());
      assertEquals(0, service1.getServer().getRemotingService().getConnections().size());
   }

   public void testMessageNullAdress() throws Exception
   {
      Map<String, Object> service0Params = new HashMap<String, Object>();
      MessagingService service0 = createMessagingService(0, service0Params);

      Map<String, Object> service1Params = new HashMap<String, Object>();
      MessagingService service1 = createMessagingService(1, service1Params);
      service1.start();

      TransportConfiguration server0tc = new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory",
                                                                    service0Params);

      Map<String, TransportConfiguration> connectors = new HashMap<String, TransportConfiguration>();

      TransportConfiguration server1tc = new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory",
                                                                    service1Params);
      connectors.put(server1tc.getName(), server1tc);
      service0.getServer().getConfiguration().setConnectorConfigurations(connectors);

      List<Pair<String, String>> connectorNames = new ArrayList<Pair<String, String>>();
      connectorNames.add(new Pair<String, String>(server1tc.getName(), null));

      final SimpleString address1 = new SimpleString("testaddress");

      MessageFlowConfiguration ofconfig1 = new MessageFlowConfiguration("blah",
                                                                        null,
                                                                        null,
                                                                        true,
                                                                        1,
                                                                        -1,
                                                                        null,
                                                                        DEFAULT_RETRY_INTERVAL,
                                                                        DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                                                                        DEFAULT_MAX_RETRIES_BEFORE_FAILOVER,
                                                                        DEFAULT_MAX_RETRIES_AFTER_FAILOVER,
                                                                        connectorNames);

      Set<MessageFlowConfiguration> ofconfigs = new HashSet<MessageFlowConfiguration>();
      ofconfigs.add(ofconfig1);

      service0.getServer().getConfiguration().setMessageFlowConfigurations(ofconfigs);

      service0.start();

      ClientSessionFactory csf0 = new ClientSessionFactoryImpl(server0tc);
      ClientSession session0 = csf0.createSession(false, true, true);

      ClientSessionFactory csf1 = new ClientSessionFactoryImpl(server1tc);
      ClientSession session1 = csf1.createSession(false, true, true);

      session0.createQueue(address1, address1, null, false, false, false);
      session1.createQueue(address1, address1, null, false, false, false);
      ClientProducer prod0 = session0.createProducer(address1);

      ClientConsumer cons1 = session1.createConsumer(address1);

      session1.start();

      SimpleString propKey = new SimpleString("car");

      ClientMessage message = session0.createClientMessage(false);
      message.getBody().flip();

      prod0.send(message);

      ClientMessage r1 = cons1.receive(1000);
      assertNull(r1);

      session0.close();
      session1.close();

      service0.stop();
      service1.stop();

      assertEquals(0, service0.getServer().getRemotingService().getConnections().size());
      assertEquals(0, service1.getServer().getRemotingService().getConnections().size());
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   @Override
   protected void setUp() throws Exception
   {
   }

   @Override
   protected void tearDown() throws Exception
   {
      assertEquals(0, InVMRegistry.instance.size());
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}