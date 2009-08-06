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

package org.jboss.messaging.tests.integration.paging;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.messaging.core.client.ClientConsumer;
import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.ClientProducer;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.paging.Page;
import org.jboss.messaging.core.paging.PagedMessage;
import org.jboss.messaging.core.paging.PagingManager;
import org.jboss.messaging.core.paging.PagingStore;
import org.jboss.messaging.core.paging.impl.PagingManagerImpl;
import org.jboss.messaging.core.paging.impl.PagingStoreFactoryNIO;
import org.jboss.messaging.core.paging.impl.PagingStoreImpl;
import org.jboss.messaging.core.security.JBMSecurityManager;
import org.jboss.messaging.core.security.impl.JBMSecurityManagerImpl;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.core.server.impl.MessagingServerImpl;
import org.jboss.messaging.core.settings.impl.AddressSettings;
import org.jboss.messaging.tests.util.ServiceTestBase;
import org.jboss.messaging.utils.OrderedExecutorFactory;
import org.jboss.messaging.utils.SimpleString;

/**
 * This test will make sure that a failing depage won't cause duplicated messages
 *
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * 
 * Created Jan 7, 2009 6:19:43 PM
 *
 *
 */
public class PageCrashTest extends ServiceTestBase
{

   // Constants -----------------------------------------------------

   public static final SimpleString ADDRESS = new SimpleString("SimpleAddress");

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testCrashDuringDeleteFile() throws Exception
   {
      pageAndFail();

      File pageDir = new File(getPageDir());

      File directories[] = pageDir.listFiles();

      assertEquals(1, directories.length);

      // When depage happened, a new empty page was supposed to be opened, what will create 3 files
      assertEquals("Missing a file, supposed to have address.txt, 1st page and 2nd page",
                   3,
                   directories[0].list().length);

      Configuration config = createDefaultConfig();

      MessagingServer messagingService = createServer(true, config, 10 * 1024, 100 * 1024, new HashMap<String, AddressSettings>());

      messagingService.start();

      try
      {
         ClientSessionFactory sf = createInVMFactory();

         ClientSession session = sf.createSession(null, null, false, true, true, false, 0);

         session.start();

         ClientConsumer consumer = session.createConsumer(ADDRESS);

         assertNull(consumer.receive(200));

         session.close();
      }
      finally
      {
         messagingService.stop();
      }

   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   /** This method will leave garbage on paging. 
    *  It will not delete page files as if the server crashed right after commit, 
    *  and before removing the file*/
   private void pageAndFail() throws Exception
   {
      clearData();
      Configuration config = createDefaultConfig();

      MessagingServer server = newMessagingServer(config);

      server.start();

      try
      {
         ClientSessionFactory sf = createInVMFactory();

         // Making it synchronous, just because we want to stop sending messages as soon as the page-store becomes in
         // page mode
         // and we could only guarantee that by setting it to synchronous
         sf.setBlockOnNonPersistentSend(true);
         sf.setBlockOnPersistentSend(true);
         sf.setBlockOnAcknowledge(true);

         ClientSession session = sf.createSession(null, null, false, true, true, false, 0);

         session.createQueue(ADDRESS, ADDRESS, null, true);

         ClientProducer producer = session.createProducer(ADDRESS);

         ClientMessage message = null;

         message = session.createClientMessage(true);
         message.getBody().writeBytes(new byte[1024]);

         PagingStore store = server.getPostOffice().getPagingManager().getPageStore(ADDRESS);

         int messages = 0;
         while (!store.isPaging())
         {
            producer.send(message);
            messages++;
         }

         for (int i = 0; i < 2; i++)
         {
            messages++;
            producer.send(message);
         }

         session.close();

         assertTrue(server.getPostOffice().getPagingManager().getTotalMemory() > 0);

         session = sf.createSession(null, null, false, true, true, false, 0);

         ClientConsumer consumer = session.createConsumer(ADDRESS);

         session.start();

         for (int i = 0; i < messages; i++)
         {
            ClientMessage message2 = consumer.receive(10000);

            assertNotNull(message2);

            message2.acknowledge();
         }

         consumer.close();

         session.close();

         assertEquals(0, server.getPostOffice().getPagingManager().getTotalMemory());

      }
      finally
      {
         try
         {
            server.stop();
         }
         catch (Throwable ignored)
         {
         }
      }
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   private MessagingServer newMessagingServer(final Configuration configuration)
   {
      JBMSecurityManager securityManager = new JBMSecurityManagerImpl();

      MessagingServer server = new FailingMessagingServerImpl(configuration, securityManager);

      AddressSettings defaultSetting = new AddressSettings();
      defaultSetting.setPageSizeBytes(10 * 1024);
      defaultSetting.setMaxSizeBytes(100 * 1024);

      server.getAddressSettingsRepository().addMatch("#", defaultSetting);

      return server;
   }

   // Inner classes -------------------------------------------------

   /** This is hacking MessagingServerImpl, 
    *  to make sure the server will fail right 
    *  before the page-file was removed */
   class FailingMessagingServerImpl extends MessagingServerImpl
   {
      FailingMessagingServerImpl(final Configuration config, final JBMSecurityManager securityManager)
      {
         super(config, ManagementFactory.getPlatformMBeanServer(), securityManager);
      }

      @Override
      protected PagingManager createPagingManager()
      {
         return new PagingManagerImpl(new FailurePagingStoreFactoryNIO(super.getConfiguration().getPagingDirectory()),
                                      super.getStorageManager(),
                                      super.getAddressSettingsRepository(),
                                       super.getConfiguration().isJournalSyncNonTransactional(),
                                      false);
      }

      class FailurePagingStoreFactoryNIO extends PagingStoreFactoryNIO

      {
         /**
          * @param directory
          * @param maxThreads
          */
         public FailurePagingStoreFactoryNIO(final String directory)
         {
            super(directory, new OrderedExecutorFactory(Executors.newCachedThreadPool()));
         }

         // Constants -----------------------------------------------------

         // Attributes ----------------------------------------------------

         // Static --------------------------------------------------------

         // Constructors --------------------------------------------------

         // Public --------------------------------------------------------

         @Override
         public synchronized PagingStore newStore(final SimpleString destinationName, final AddressSettings settings) throws Exception
         {
            Field factoryField = PagingStoreFactoryNIO.class.getDeclaredField("executorFactory");
            factoryField.setAccessible(true);

            OrderedExecutorFactory factory = (org.jboss.messaging.utils.OrderedExecutorFactory)factoryField.get(this);
            return new FailingPagingStore(destinationName, settings, factory.getExecutor());
         }

         // Package protected ---------------------------------------------

         // Protected -----------------------------------------------------

         // Private -------------------------------------------------------

         // Inner classes -------------------------------------------------
         class FailingPagingStore extends PagingStoreImpl
         {

            /**
             * @param storeName
             * @param addressSettings
             * @param executor
             */
            public FailingPagingStore(final SimpleString storeName,
                                      final AddressSettings addressSettings,
                                      final Executor executor)
            {
               super(getPostOffice().getPagingManager(),
                     getStorageManager(),
                     getPostOffice(),
                     null,
                     FailurePagingStoreFactoryNIO.this,
                     storeName,
                     addressSettings,
                     executor);
            }

            @Override
            protected Page createPage(final int page) throws Exception
            {

               Page originalPage = super.createPage(page);

               return new FailingPage(originalPage);
            }

         }

      }

      class FailingPage implements Page
      {
         Page delegatedPage;

         /**
          * @throws Exception
          * @see org.jboss.messaging.core.paging.Page#close()
          */
         public void close() throws Exception
         {
            delegatedPage.close();
         }

         /**
          * @throws Exception
          * @see org.jboss.messaging.core.paging.Page#delete()
          */
         public void delete() throws Exception
         {
            // This will let the file stay, simulating a system failure
         }

         /**
          * @return
          * @see org.jboss.messaging.core.paging.Page#getNumberOfMessages()
          */
         public int getNumberOfMessages()
         {
            return delegatedPage.getNumberOfMessages();
         }

         /**
          * @return
          * @see org.jboss.messaging.core.paging.Page#getPageId()
          */
         public int getPageId()
         {
            return delegatedPage.getPageId();
         }

         /**
          * @return
          * @see org.jboss.messaging.core.paging.Page#getSize()
          */
         public int getSize()
         {
            return delegatedPage.getSize();
         }

         /**
          * @throws Exception
          * @see org.jboss.messaging.core.paging.Page#open()
          */
         public void open() throws Exception
         {
            delegatedPage.open();
         }

         /**
          * @return
          * @throws Exception
          * @see org.jboss.messaging.core.paging.Page#read()
          */
         public List<PagedMessage> read() throws Exception
         {
            return delegatedPage.read();
         }

         /**
          * @throws Exception
          * @see org.jboss.messaging.core.paging.Page#sync()
          */
         public void sync() throws Exception
         {
            delegatedPage.sync();
         }

         /**
          * @param message
          * @throws Exception
          * @see org.jboss.messaging.core.paging.Page#write(org.jboss.messaging.core.paging.PagedMessage)
          */
         public void write(final PagedMessage message) throws Exception
         {
            delegatedPage.write(message);
         }

         public FailingPage(final Page delegatePage)
         {
            delegatedPage = delegatePage;
         }
      }

   }

   // Inner classes -------------------------------------------------

}
