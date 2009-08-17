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

package org.jboss.messaging.tests.util;

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.config.impl.ConfigurationImpl;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.impl.invm.InVMAcceptorFactory;
import org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory;
import org.jboss.messaging.core.security.JBMSecurityManager;
import org.jboss.messaging.core.server.JournalType;
import org.jboss.messaging.core.server.Messaging;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.core.settings.impl.AddressSettings;
import org.jboss.messaging.integration.transports.netty.NettyAcceptorFactory;
import org.jboss.messaging.integration.transports.netty.NettyConnectorFactory;
import org.jboss.messaging.jms.client.JBossBytesMessage;
import org.jboss.messaging.jms.client.JBossTextMessage;

/**
 * 
 * Base class with basic utilities on starting up a basic server
 * 
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 *
 */
public class ServiceTestBase extends UnitTestCase
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   protected static final String INVM_ACCEPTOR_FACTORY = InVMAcceptorFactory.class.getCanonicalName();

   protected static final String INVM_CONNECTOR_FACTORY = InVMConnectorFactory.class.getCanonicalName();

   protected static final String NETTY_ACCEPTOR_FACTORY = NettyAcceptorFactory.class.getCanonicalName();

   protected static final String NETTY_CONNECTOR_FACTORY = NettyConnectorFactory.class.getCanonicalName();

   // Static --------------------------------------------------------
   private final Logger log = Logger.getLogger(this.getClass());

   public static void forceGC()
   {
      WeakReference<Object> dumbReference = new WeakReference<Object>(new Object());
      // A loop that will wait GC, using the minimal time as possible
      while (dumbReference.get() != null)
      {
         System.gc();
         try
         {
            Thread.sleep(500);
         }
         catch (InterruptedException e)
         {
         }
      }
   }

   // verify if these weak references are released after a few GCs
   public static void checkWeakReferences(WeakReference<?>... references)
   {

      int i = 0;
      boolean hasValue = false;

      do
      {
         hasValue = false;

         if (i > 0)
         {
            forceGC();
         }

         for (WeakReference<?> ref : references)
         {
            if (ref.get() != null)
            {
               hasValue = true;
            }
         }
      }
      while (i++ <= 30 && hasValue);

      for (WeakReference<?> ref : references)
      {
         assertNull(ref.get());
      }
   }

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------
   protected Configuration createConfigForJournal()
   {
      Configuration config = new ConfigurationImpl();
      config.setJournalDirectory(getJournalDir());
      config.setBindingsDirectory(getBindingsDir());
      config.setJournalType(JournalType.ASYNCIO);
      config.setLargeMessagesDirectory(getLargeMessagesDir());
      config.setJournalCompactMinFiles(0);
      config.setJournalCompactPercentage(0);
      return config;
   }

   protected MessagingServer createServer(final boolean realFiles,
                                          final Configuration configuration,
                                          int pageSize,
                                          int maxAddressSize,
                                          final Map<String, AddressSettings> settings)
   {
      MessagingServer server;

      if (realFiles)
      {
         server = Messaging.newMessagingServer(configuration);
      }
      else
      {
         server = Messaging.newMessagingServer(configuration, false);
      }

      for (Map.Entry<String, AddressSettings> setting : settings.entrySet())
      {
         server.getAddressSettingsRepository().addMatch(setting.getKey(), setting.getValue());
      }

      AddressSettings defaultSetting = new AddressSettings();
      defaultSetting.setPageSizeBytes(pageSize);
      defaultSetting.setMaxSizeBytes(maxAddressSize);

      server.getAddressSettingsRepository().addMatch("#", defaultSetting);

      return server;
   }

   protected MessagingServer createServer(final boolean realFiles,
                                          final Configuration configuration,
                                          final MBeanServer mbeanServer,
                                          final Map<String, AddressSettings> settings)
   {
      MessagingServer server;

      if (realFiles)
      {
         server = Messaging.newMessagingServer(configuration, mbeanServer);
      }
      else
      {
         server = Messaging.newMessagingServer(configuration, mbeanServer, false);
      }

      for (Map.Entry<String, AddressSettings> setting : settings.entrySet())
      {
         server.getAddressSettingsRepository().addMatch(setting.getKey(), setting.getValue());
      }

      AddressSettings defaultSetting = new AddressSettings();
      server.getAddressSettingsRepository().addMatch("#", defaultSetting);

      return server;
   }

   protected MessagingServer createServer(final boolean realFiles)
   {
      return createServer(realFiles, createDefaultConfig(), -1, -1, new HashMap<String, AddressSettings>());
   }

   protected MessagingServer createServer(final boolean realFiles, final Configuration configuration)
   {
      return createServer(realFiles, configuration, -1, -1, new HashMap<String, AddressSettings>());
   }

   protected MessagingServer createServer(final boolean realFiles,
                                          final Configuration configuration,
                                          final JBMSecurityManager securityManager)
   {
      MessagingServer server;

      if (realFiles)
      {
         server = Messaging.newMessagingServer(configuration,
                                               ManagementFactory.getPlatformMBeanServer(),
                                               securityManager);
      }
      else
      {
         server = Messaging.newMessagingServer(configuration,
                                               ManagementFactory.getPlatformMBeanServer(),
                                               securityManager,
                                               false);
      }

      Map<String, AddressSettings> settings = new HashMap<String, AddressSettings>();

      for (Map.Entry<String, AddressSettings> setting : settings.entrySet())
      {
         server.getAddressSettingsRepository().addMatch(setting.getKey(), setting.getValue());
      }

      AddressSettings defaultSetting = new AddressSettings();

      server.getAddressSettingsRepository().addMatch("#", defaultSetting);

      return server;
   }

   protected MessagingServer createClusteredServerWithParams(final int index,
                                                             final boolean realFiles,
                                                             final Map<String, Object> params)
   {
      return createServer(realFiles,
                          createClusteredDefaultConfig(index, params, INVM_ACCEPTOR_FACTORY),
                          -1,
                          -1,
                          new HashMap<String, AddressSettings>());
   }

   protected Configuration createDefaultConfig()
   {
      return createDefaultConfig(false);
   }

   protected Configuration createDefaultConfig(final boolean netty)
   {
      if (netty)
      {
         return createDefaultConfig(new HashMap<String, Object>(), INVM_ACCEPTOR_FACTORY, NETTY_ACCEPTOR_FACTORY);
      }
      else
      {
         return createDefaultConfig(new HashMap<String, Object>(), INVM_ACCEPTOR_FACTORY);
      }
   }

   protected Configuration createClusteredDefaultConfig(final int index,
                                                        final Map<String, Object> params,
                                                        final String... acceptors)
   {
      Configuration config = createDefaultConfig(index, params, acceptors);

      config.setClustered(true);

      return config;
   }

   protected Configuration createDefaultConfig(int index, final Map<String, Object> params, final String... acceptors)
   {
      Configuration configuration = new ConfigurationImpl();
      configuration.setSecurityEnabled(false);
      configuration.setBindingsDirectory(getBindingsDir(index, false));
      configuration.setJournalMinFiles(2);
      configuration.setJournalDirectory(getJournalDir(index, false));
      configuration.setJournalFileSize(100 * 1024);
      configuration.setJournalType(JournalType.ASYNCIO);
      configuration.setPagingDirectory(getPageDir(index, false));
      configuration.setLargeMessagesDirectory(getLargeMessagesDir(index, false));
      configuration.setJournalCompactMinFiles(0);
      configuration.setJournalCompactPercentage(0);

      configuration.getAcceptorConfigurations().clear();

      for (String acceptor : acceptors)
      {
         TransportConfiguration transportConfig = new TransportConfiguration(acceptor, params);
         configuration.getAcceptorConfigurations().add(transportConfig);
      }

      return configuration;
   }

   protected Configuration createDefaultConfig(final Map<String, Object> params, final String... acceptors)
   {
      Configuration configuration = new ConfigurationImpl();
      configuration.setSecurityEnabled(false);
      configuration.setJMXManagementEnabled(false);
      configuration.setBindingsDirectory(getBindingsDir());
      configuration.setJournalMinFiles(2);
      configuration.setJournalDirectory(getJournalDir());
      configuration.setJournalFileSize(100 * 1024);
      configuration.setPagingDirectory(getPageDir());
      configuration.setLargeMessagesDirectory(getLargeMessagesDir());
      configuration.setJournalCompactMinFiles(0);
      configuration.setJournalCompactPercentage(0);

      configuration.setJournalType(JournalType.ASYNCIO);

      configuration.getAcceptorConfigurations().clear();

      for (String acceptor : acceptors)
      {
         TransportConfiguration transportConfig = new TransportConfiguration(acceptor, params);
         configuration.getAcceptorConfigurations().add(transportConfig);
      }

      return configuration;
   }

   protected ClientSessionFactory createInVMFactory()
   {
      return createFactory(INVM_CONNECTOR_FACTORY);
   }

   protected ClientSessionFactory createNettyFactory()
   {
      return createFactory(NETTY_CONNECTOR_FACTORY);
   }

   protected ClientSessionFactory createFactory(final String connectorClass)
   {
      return new ClientSessionFactoryImpl(new TransportConfiguration(connectorClass), null);

   }

   protected ClientMessage createTextMessage(final ClientSession session, final String s)
   {
      return createTextMessage(session, s, true);
   }

   public String getTextMessage(ClientMessage m)
   {
      m.getBody().resetReaderIndex();
      return m.getBody().readString();
   }

   protected ClientMessage createTextMessage(final ClientSession session, final String s, final boolean durable)
   {
      ClientMessage message = session.createClientMessage(JBossTextMessage.TYPE,
                                                          durable,
                                                          0,
                                                          System.currentTimeMillis(),
                                                          (byte)1);
      message.getBody().writeString(s);
      return message;
   }

   protected ClientMessage createBytesMessage(final ClientSession session, final byte[] b, final boolean durable)
   {
      ClientMessage message = session.createClientMessage(JBossBytesMessage.TYPE,
                                                          durable,
                                                          0,
                                                          System.currentTimeMillis(),
                                                          (byte)1);
      message.getBody().writeBytes(b);
      return message;
   }
   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
