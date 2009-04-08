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

package org.jboss.messaging.tests.integration.management;

import static org.jboss.messaging.tests.util.RandomUtil.randomString;

import java.util.HashMap;

import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.config.impl.ConfigurationImpl;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.management.AcceptorControlMBean;
import org.jboss.messaging.core.remoting.impl.invm.InVMAcceptorFactory;
import org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory;
import org.jboss.messaging.core.server.Messaging;
import org.jboss.messaging.core.server.MessagingServer;

/**
 * A AcceptorControlTest
 *
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 * Created 11 dec. 2008 17:38:58
 *
 *
 */
public class AcceptorControlTest extends ManagementTestBase
{

   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(AcceptorControlTest.class);

   
   // Attributes ----------------------------------------------------

   private MessagingServer service;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testAttributes() throws Exception
   {
      TransportConfiguration acceptorConfig = new TransportConfiguration(InVMAcceptorFactory.class.getName(),
                                                                         new HashMap<String, Object>(),
                                                                         randomString());

      Configuration conf = new ConfigurationImpl();
      conf.setSecurityEnabled(false);
      conf.setJMXManagementEnabled(true);
      conf.getAcceptorConfigurations().add(acceptorConfig);
      service = Messaging.newNullStorageMessagingServer(conf, mbeanServer);
      service.start();

      AcceptorControlMBean acceptorControl = createManagementControl(acceptorConfig.getName());

      assertEquals(acceptorConfig.getName(), acceptorControl.getName());
      assertEquals(acceptorConfig.getFactoryClassName(), acceptorControl.getFactoryClassName());
   }

   public void testStartStop() throws Exception
   {
      TransportConfiguration acceptorConfig = new TransportConfiguration(InVMAcceptorFactory.class.getName(),
                                                                         new HashMap<String, Object>(),
                                                                         randomString());
      Configuration conf = new ConfigurationImpl();
      conf.setSecurityEnabled(false);
      conf.setJMXManagementEnabled(true);
      conf.getAcceptorConfigurations().add(acceptorConfig);
      service = Messaging.newNullStorageMessagingServer(conf, mbeanServer);
      service.start();

      AcceptorControlMBean acceptorControl = createManagementControl(acceptorConfig.getName());

      // started by the server
      assertTrue(acceptorControl.isStarted());

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration(InVMConnectorFactory.class.getName()));
      ClientSession session = sf.createSession(false, true, true);
      assertNotNull(session);
      session.close();
      
      acceptorControl.pause();
      
      try
      {
         ClientSession session2 = sf.createSession(false, true, true);
         
         fail("acceptor must not accept connections when paused");
      }
      catch (Exception e)
      {
      }
      
      //Pausing a second time should do nothing
      
      acceptorControl.pause();
      assertTrue(acceptorControl.isStarted());
         
      try
      {
         ClientSession session2 = sf.createSession(false, true, true);
         
         fail("acceptor must not accept connections when paused");
      }
      catch (Exception e)
      {
      }
      
      acceptorControl.resume();
      assertTrue(acceptorControl.isStarted());
      
      ClientSession session2 = sf.createSession(false, true, true);
      assertNotNull(session2);
      session2.close();
      
      //resuming again should do nothing
      
      acceptorControl.resume();
      assertTrue(acceptorControl.isStarted());
      
      session2 = sf.createSession(false, true, true);
      assertNotNull(session2);
      session2.close();
      
      
      acceptorControl.stop();
       
      assertFalse(acceptorControl.isStarted());
      
      try
      {
         sf.createSession(false, true, true);
         fail("acceptor must not accept connections when stopped accepting");
      }
      catch (Exception e)
      {
      }
      
      acceptorControl.start();

      assertTrue(acceptorControl.isStarted());
      session = sf.createSession(false, true, true);
      assertNotNull(session);
      session.close();
      
      acceptorControl.stop();
      
      assertFalse(acceptorControl.isStarted());
      
      try
      {
         sf.createSession(false, true, true);
         fail("acceptor must not accept connections when stopped accepting");
      }
      catch (Exception e)
      {
      }
      
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   @Override
   protected void tearDown() throws Exception
   {
      if (service != null)
      {
         service.stop();
      }

      super.tearDown();
   }
   
   protected AcceptorControlMBean createManagementControl(String name) throws Exception
   {
      return ManagementControlHelper.createAcceptorControl(name, mbeanServer);
   }
   
   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
