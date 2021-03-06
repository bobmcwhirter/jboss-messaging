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

package org.jboss.messaging.tests.unit.core.management.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.jboss.messaging.tests.util.RandomUtil.randomSimpleString;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.management.AddressControlMBean;
import org.jboss.messaging.core.management.ManagementService;
import org.jboss.messaging.core.management.MessagingServerControlMBean;
import org.jboss.messaging.core.management.QueueControlMBean;
import org.jboss.messaging.core.management.impl.AddressControl;
import org.jboss.messaging.core.management.impl.ManagementServiceImpl;
import org.jboss.messaging.core.management.impl.MessagingServerControl;
import org.jboss.messaging.core.management.impl.QueueControl;
import org.jboss.messaging.core.persistence.StorageManager;
import org.jboss.messaging.core.postoffice.PostOffice;
import org.jboss.messaging.core.security.Role;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.core.server.Queue;
import org.jboss.messaging.core.settings.HierarchicalRepository;
import org.jboss.messaging.core.settings.impl.QueueSettings;
import org.jboss.messaging.util.SimpleString;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 * @version <tt>$Revision$</tt>
 * 
 */
public class ManagementServiceImplTest extends TestCase
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testRegisterMessagingServer() throws Exception
   {
      ObjectName objectName = ManagementServiceImpl
            .getMessagingServerObjectName();
      ObjectInstance objectInstance = new ObjectInstance(objectName,
            MessagingServerControl.class.getName());

      PostOffice postOffice = createMock(PostOffice.class);
      StorageManager storageManager = createMock(StorageManager.class);
      Configuration configuration = createMock(Configuration.class);
      HierarchicalRepository<Set<Role>> securityRepository = createMock(HierarchicalRepository.class);
      HierarchicalRepository<QueueSettings> queueSettingsRepository = createMock(HierarchicalRepository.class);
      MessagingServer messagingServer = createMock(MessagingServer.class);
      MBeanServer mbeanServer = createMock(MBeanServer.class);
      expect(mbeanServer.isRegistered(objectName)).andReturn(false);
      expect(
            mbeanServer.registerMBean(isA(MessagingServerControl.class),
                  eq(objectName))).andReturn(objectInstance);

      replay(mbeanServer, postOffice, storageManager, configuration, securityRepository, queueSettingsRepository, messagingServer);

      ManagementService service = new ManagementServiceImpl(mbeanServer, true);
      service.registerServer(postOffice, storageManager, configuration, securityRepository, queueSettingsRepository, messagingServer);

      verify(mbeanServer, postOffice, storageManager, configuration, securityRepository, queueSettingsRepository, messagingServer);
   }

   public void testRegisterAlreadyRegisteredMessagingServer() throws Exception
   {
      ObjectName objectName = ManagementServiceImpl
            .getMessagingServerObjectName();
      ObjectInstance objectInstance = new ObjectInstance(objectName,
            MessagingServerControl.class.getName());

      PostOffice postOffice = createMock(PostOffice.class);
      StorageManager storageManager = createMock(StorageManager.class);
      Configuration configuration = createMock(Configuration.class);
      HierarchicalRepository<Set<Role>> securityRepository = createMock(HierarchicalRepository.class);
      HierarchicalRepository<QueueSettings> queueSettingsRepository = createMock(HierarchicalRepository.class);
      MessagingServer messagingServer = createMock(MessagingServer.class);
      MBeanServer mbeanServer = createMock(MBeanServer.class);
      expect(mbeanServer.isRegistered(objectName)).andReturn(true);
      mbeanServer.unregisterMBean(objectName);
      expect(
            mbeanServer.registerMBean(isA(MessagingServerControlMBean.class),
                  eq(objectName))).andReturn(objectInstance);

      replay(mbeanServer, postOffice, storageManager, configuration, securityRepository, queueSettingsRepository, messagingServer);

      ManagementService service = new ManagementServiceImpl(mbeanServer, true);
      service.registerServer(postOffice, storageManager, configuration, securityRepository, queueSettingsRepository, messagingServer);

      verify(mbeanServer, postOffice, storageManager, configuration, securityRepository, queueSettingsRepository, messagingServer);
   }

   public void testUnregisterMessagingServer() throws Exception
   {
      ObjectName objectName = ManagementServiceImpl
            .getMessagingServerObjectName();

      MBeanServer mbeanServer = createMock(MBeanServer.class);
      expect(mbeanServer.isRegistered(objectName)).andReturn(true);
      mbeanServer.unregisterMBean(objectName);

      replay(mbeanServer);

      ManagementService service = new ManagementServiceImpl(mbeanServer, true);
      service.unregisterServer();

      verify(mbeanServer);
   }

   public void testRegisterAddress() throws Exception
   {
      SimpleString address = randomSimpleString();
      ObjectName objectName = ManagementServiceImpl
            .getAddressObjectName(address);
      ObjectInstance objectInstance = new ObjectInstance(objectName,
            AddressControl.class.getName());

      MBeanServer mbeanServer = createMock(MBeanServer.class);
      expect(mbeanServer.isRegistered(objectName)).andReturn(false);
      expect(
            mbeanServer.registerMBean(isA(AddressControlMBean.class),
                  eq(objectName))).andReturn(objectInstance);

      replay(mbeanServer);

      ManagementService service = new ManagementServiceImpl(mbeanServer, true);
      service.registerAddress(address);

      verify(mbeanServer);
   }

   public void testRegisterAlreadyRegisteredAddress() throws Exception
   {
      SimpleString address = randomSimpleString();
      ObjectName objectName = ManagementServiceImpl
            .getAddressObjectName(address);
      ObjectInstance objectInstance = new ObjectInstance(objectName,
            AddressControl.class.getName());

      MBeanServer mbeanServer = createMock(MBeanServer.class);
      expect(mbeanServer.isRegistered(objectName)).andReturn(true);
      mbeanServer.unregisterMBean(objectName);
      expect(
            mbeanServer.registerMBean(isA(AddressControlMBean.class),
                  eq(objectName))).andReturn(objectInstance);

      replay(mbeanServer);

      ManagementService service = new ManagementServiceImpl(mbeanServer, true);
      service.registerAddress(address);

      verify(mbeanServer);
   }

   public void testUnregisterAddress() throws Exception
   {
      SimpleString address = randomSimpleString();
      ObjectName objectName = ManagementServiceImpl
            .getAddressObjectName(address);

      MBeanServer mbeanServer = createMock(MBeanServer.class);
      expect(mbeanServer.isRegistered(objectName)).andReturn(true);
      mbeanServer.unregisterMBean(objectName);

      replay(mbeanServer);

      ManagementService service = new ManagementServiceImpl(mbeanServer, true);
      service.unregisterAddress(address);

      verify(mbeanServer);
   }

   public void testRegisterQueue() throws Exception
   {
      SimpleString address = randomSimpleString();
      SimpleString name = randomSimpleString();
      ObjectName objectName = ManagementServiceImpl.getQueueObjectName(address,
            name);
      ObjectInstance objectInstance = new ObjectInstance(objectName,
            QueueControl.class.getName());

      MBeanServer mbeanServer = createMock(MBeanServer.class);
      Queue queue = createMock(Queue.class);
      expect(queue.getName()).andStubReturn(name);
      expect(queue.isDurable()).andReturn(true);
      StorageManager storageManager = createMock(StorageManager.class);
      expect(mbeanServer.isRegistered(objectName)).andReturn(false);
      expect(
            mbeanServer.registerMBean(isA(QueueControlMBean.class),
                  eq(objectName))).andReturn(objectInstance);

      replay(mbeanServer, queue, storageManager);

      ManagementService service = new ManagementServiceImpl(mbeanServer, true);
      service.registerQueue(queue, address, storageManager);

      verify(mbeanServer, queue, storageManager);
   }

   public void testRegisterAlreadyRegisteredQueue() throws Exception
   {
      SimpleString address = randomSimpleString();
      SimpleString name = randomSimpleString();
      ObjectName objectName = ManagementServiceImpl.getQueueObjectName(address,
            name);
      ObjectInstance objectInstance = new ObjectInstance(objectName,
            QueueControl.class.getName());

      MBeanServer mbeanServer = createMock(MBeanServer.class);
      Queue queue = createMock(Queue.class);
      expect(queue.getName()).andStubReturn(name);
      expect(queue.isDurable()).andReturn(true);
      StorageManager storageManager = createMock(StorageManager.class);
      expect(mbeanServer.isRegistered(objectName)).andReturn(true);
      mbeanServer.unregisterMBean(objectName);
      expect(
            mbeanServer.registerMBean(isA(QueueControlMBean.class),
                  eq(objectName))).andReturn(objectInstance);

      replay(mbeanServer, queue, storageManager);

      ManagementService service = new ManagementServiceImpl(mbeanServer, true);
      service.registerQueue(queue, address, storageManager);

      verify(mbeanServer, queue, storageManager);
   }

   public void testUnregisterQueue() throws Exception
   {
      SimpleString address = randomSimpleString();
      SimpleString name = randomSimpleString();
      ObjectName objectName = ManagementServiceImpl.getQueueObjectName(address,
            name);

      MBeanServer mbeanServer = createMock(MBeanServer.class);
      expect(mbeanServer.isRegistered(objectName)).andReturn(true);
      mbeanServer.unregisterMBean(objectName);

      replay(mbeanServer);

      ManagementService service = new ManagementServiceImpl(mbeanServer, true);
      service.unregisterQueue(name, address);

      verify(mbeanServer);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
