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

import static org.jboss.messaging.tests.util.RandomUtil.randomBoolean;
import static org.jboss.messaging.tests.util.RandomUtil.randomSimpleString;
import static org.jboss.messaging.tests.util.RandomUtil.randomString;

import java.util.HashSet;
import java.util.Set;

import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.config.impl.ConfigurationImpl;
import org.jboss.messaging.core.management.AddressControl;
import org.jboss.messaging.core.management.ResourceNames;
import org.jboss.messaging.core.remoting.impl.invm.InVMAcceptorFactory;
import org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory;
import org.jboss.messaging.core.security.CheckType;
import org.jboss.messaging.core.security.Role;
import org.jboss.messaging.core.server.Messaging;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.utils.SimpleString;

/**
 * 
 * A AddressControlUsingCoreTest
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 *
 */
public class AddressControlUsingCoreTest extends ManagementTestBase
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private MessagingServer server;

   protected ClientSession session;
   
   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testGetAddress() throws Exception
   {
      SimpleString address = randomSimpleString();
      SimpleString queue = randomSimpleString();

      session.createQueue(address, queue, false);

      CoreMessagingProxy proxy = createProxy(address);

      assertEquals(address.toString(), proxy.retrieveAttributeValue("Address"));

      session.deleteQueue(queue);
   }

   public void testGetQueueNames() throws Exception
   {
      SimpleString address = randomSimpleString();
      SimpleString queue = randomSimpleString();
      SimpleString anotherQueue = randomSimpleString();

      session.createQueue(address, queue, true);

      CoreMessagingProxy proxy = createProxy(address);
      Object[] queueNames = (Object[])proxy.retrieveAttributeValue("QueueNames");
      assertEquals(1, queueNames.length);
      assertEquals(queue.toString(), queueNames[0]);

      session.createQueue(address, anotherQueue, false);
      queueNames = (Object[])proxy.retrieveAttributeValue("QueueNames");
      assertEquals(2, queueNames.length);

      session.deleteQueue(queue);

      queueNames = (Object[])proxy.retrieveAttributeValue("QueueNames");
      assertEquals(1, queueNames.length);
      assertEquals(anotherQueue.toString(), queueNames[0]);

      session.deleteQueue(anotherQueue);
   }

   public void testGetRoles() throws Exception
   {
      SimpleString address = randomSimpleString();
      SimpleString queue = randomSimpleString();
      Role role = new Role(randomString(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean());

      session.createQueue(address, queue, true);

      CoreMessagingProxy proxy = createProxy(address);
      Object[] roles = (Object[])proxy.retrieveAttributeValue("Roles");
      for (int i = 0; i < roles.length; i++)
      {
         System.out.println(((Object[])roles[i])[0]);
      }
      assertEquals(0, roles.length);

      Set<Role> newRoles = new HashSet<Role>();
      newRoles.add(role);
      server.getSecurityRepository().addMatch(address.toString(), newRoles);

      roles = (Object[])proxy.retrieveAttributeValue("Roles");
      assertEquals(1, roles.length);
      Object[] r = (Object[])roles[0];
      assertEquals(role.getName(), r[0]);
      assertEquals(CheckType.SEND.hasRole(role), r[1]);
      assertEquals(CheckType.CONSUME.hasRole(role), r[2]);
      assertEquals(CheckType.CREATE_DURABLE_QUEUE.hasRole(role), r[3]);
      assertEquals(CheckType.DELETE_DURABLE_QUEUE.hasRole(role), r[4]);
      assertEquals(CheckType.CREATE_NON_DURABLE_QUEUE.hasRole(role), r[5]);
      assertEquals(CheckType.DELETE_NON_DURABLE_QUEUE.hasRole(role), r[6]);
      assertEquals(CheckType.MANAGE.hasRole(role), r[7]);
      
      session.deleteQueue(queue);
   }

   public void testAddRole() throws Exception
   {
      SimpleString address = randomSimpleString();
      SimpleString queue = randomSimpleString();
      Role role = new Role(randomString(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean());

      session.createQueue(address, queue, true);

      CoreMessagingProxy proxy = createProxy(address);
      Object[] roles = (Object[])proxy.retrieveAttributeValue("Roles");
      assertEquals(0, roles.length);

      proxy.invokeOperation("addRole", role.getName(),
                             CheckType.SEND.hasRole(role),
                             CheckType.CONSUME.hasRole(role),
                             CheckType.CREATE_DURABLE_QUEUE.hasRole(role),
                             CheckType.DELETE_DURABLE_QUEUE.hasRole(role),
                             CheckType.CREATE_NON_DURABLE_QUEUE.hasRole(role),
                             CheckType.DELETE_NON_DURABLE_QUEUE.hasRole(role),
                             CheckType.MANAGE.hasRole(role));

      roles = (Object[])proxy.retrieveAttributeValue("Roles");
      assertEquals(1, roles.length);
      Object[] r = (Object[])roles[0];
      assertEquals(role.getName(), r[0]);
      assertEquals(CheckType.SEND.hasRole(role), r[1]);
      assertEquals(CheckType.CONSUME.hasRole(role), r[2]);
      assertEquals(CheckType.CREATE_DURABLE_QUEUE.hasRole(role), r[3]);
      assertEquals(CheckType.DELETE_DURABLE_QUEUE.hasRole(role), r[4]);
      assertEquals(CheckType.CREATE_NON_DURABLE_QUEUE.hasRole(role), r[5]);
      assertEquals(CheckType.DELETE_NON_DURABLE_QUEUE.hasRole(role), r[6]);
      assertEquals(CheckType.MANAGE.hasRole(role), r[7]);



      session.deleteQueue(queue);
   }

   public void testAddRoleWhichAlreadyExists() throws Exception
   {
      SimpleString address = randomSimpleString();
      SimpleString queue = randomSimpleString();
      Role role = new Role(randomString(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean());

      session.createQueue(address, queue, true);

      CoreMessagingProxy proxy = createProxy(address);
      Object[] data = (Object[])proxy.retrieveAttributeValue("Roles");
      assertEquals(0, data.length);

      proxy.invokeOperation("addRole", role.getName(),
                             CheckType.SEND.hasRole(role),
                             CheckType.CONSUME.hasRole(role),
                             CheckType.CREATE_DURABLE_QUEUE.hasRole(role),
                             CheckType.DELETE_DURABLE_QUEUE.hasRole(role),
                             CheckType.CREATE_NON_DURABLE_QUEUE.hasRole(role),
                             CheckType.DELETE_NON_DURABLE_QUEUE.hasRole(role),
                             CheckType.MANAGE.hasRole(role));

      data = (Object[])proxy.retrieveAttributeValue("Roles");
      assertEquals(1, data.length);

      try
      {
         proxy.invokeOperation("addRole", role.getName(),
                             CheckType.SEND.hasRole(role),
                             CheckType.CONSUME.hasRole(role),
                             CheckType.CREATE_DURABLE_QUEUE.hasRole(role),
                             CheckType.DELETE_DURABLE_QUEUE.hasRole(role),
                             CheckType.CREATE_NON_DURABLE_QUEUE.hasRole(role),
                             CheckType.DELETE_NON_DURABLE_QUEUE.hasRole(role),
                             CheckType.MANAGE.hasRole(role));
         fail("can not add a role which already exists");
      }
      catch (Exception e)
      {
      }

      data = (Object[])proxy.retrieveAttributeValue("Roles");
      assertEquals(1, data.length);

      session.deleteQueue(queue);
   }

   public void testRemoveRole() throws Exception
   {
      SimpleString address = randomSimpleString();
      SimpleString queue = randomSimpleString();
      String roleName = randomString();

      session.createQueue(address, queue, true);

      CoreMessagingProxy proxy = createProxy(address);
      Object[] data = (Object[])proxy.retrieveAttributeValue("Roles");
      assertEquals(0, data.length);

      proxy.invokeOperation("addRole", roleName, randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean());

      data = (Object[])proxy.retrieveAttributeValue("Roles");
      assertEquals(1, data.length);

      proxy.invokeOperation("removeRole", roleName);  

      data = (Object[])proxy.retrieveAttributeValue("Roles");
      assertEquals(0, data.length);

      session.deleteQueue(queue);
   }

   public void testRemoveRoleWhichDoesNotExist() throws Exception
   {
      SimpleString address = randomSimpleString();
      SimpleString queue = randomSimpleString();
      String roleName = randomString();

      session.createQueue(address, queue, true);

      CoreMessagingProxy proxy = createProxy(address);
      Object[] data = (Object[])proxy.retrieveAttributeValue("Roles");
      assertEquals(0, data.length);

      try
      {
         proxy.invokeOperation("removeRole", roleName);  
         fail("can not remove a role which does not exist");
      }
      catch (Exception e)
      {
      }

      session.deleteQueue(queue);
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

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration(InVMConnectorFactory.class.getName()));
      sf.setBlockOnNonPersistentSend(true);
      sf.setBlockOnNonPersistentSend(true);
      session = sf.createSession(false, true, false);
      session.start();
   }
      
   
   protected CoreMessagingProxy createProxy(final SimpleString address) throws Exception
   {
      CoreMessagingProxy proxy = new CoreMessagingProxy(session, ResourceNames.CORE_ADDRESS + address);
      
      return proxy;
   }

   @Override
   protected void tearDown() throws Exception
   {
      session.close();

      server.stop();

      super.tearDown();
   }

   protected AddressControl createManagementControl(SimpleString address) throws Exception
   {
      return ManagementControlHelper.createAddressControl(address, mbeanServer);
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
