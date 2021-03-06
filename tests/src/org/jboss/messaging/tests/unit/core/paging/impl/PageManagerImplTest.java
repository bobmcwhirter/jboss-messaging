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

package org.jboss.messaging.tests.unit.core.paging.impl;

import org.easymock.EasyMock;
import org.jboss.messaging.core.journal.SequentialFileFactory;
import org.jboss.messaging.core.paging.LastPageRecord;
import org.jboss.messaging.core.paging.PageMessage;
import org.jboss.messaging.core.paging.PagingStore;
import org.jboss.messaging.core.paging.PagingStoreFactory;
import org.jboss.messaging.core.paging.impl.PageMessageImpl;
import org.jboss.messaging.core.paging.impl.PagingManagerImpl;
import org.jboss.messaging.core.paging.impl.PagingStoreImpl;
import org.jboss.messaging.core.persistence.StorageManager;
import org.jboss.messaging.core.postoffice.PostOffice;
import org.jboss.messaging.core.server.HandleStatus;
import org.jboss.messaging.core.server.MessageReference;
import org.jboss.messaging.core.server.Queue;
import org.jboss.messaging.core.server.ServerMessage;
import org.jboss.messaging.core.settings.HierarchicalRepository;
import org.jboss.messaging.core.settings.impl.HierarchicalObjectRepository;
import org.jboss.messaging.core.settings.impl.QueueSettings;
import org.jboss.messaging.tests.util.UnitTestCase;
import org.jboss.messaging.util.SimpleString;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class PageManagerImplTest extends UnitTestCase
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private static HierarchicalRepository<QueueSettings> repoSettings = new HierarchicalObjectRepository<QueueSettings>();
   static
   {
      repoSettings.setDefault(new QueueSettings());
   }

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testGetStore() throws Exception
   {
      HierarchicalRepository<QueueSettings> queueSettings = new HierarchicalObjectRepository<QueueSettings>();
      queueSettings.setDefault(new QueueSettings());

      PagingStoreFactory spi = EasyMock.createMock(PagingStoreFactory.class);
      PagingManagerImpl manager = new PagingManagerImpl(spi, null, queueSettings, -1);

      SimpleString destination = new SimpleString("some-destination");

      try
      {
         manager.getPageStore(destination);
         fail("supposed to throw an exception");
      }
      catch (Exception ignored)
      {
      }

      manager.start();

      PagingStore store = EasyMock.createNiceMock(PagingStore.class);

      EasyMock.expect(spi.newStore(EasyMock.eq(destination), EasyMock.isA(QueueSettings.class))).andReturn(store);

      store.start();

      EasyMock.replay(spi, store);

      assertEquals(store, manager.getPageStore(destination));

      EasyMock.verify(spi, store);

      EasyMock.reset(spi, store);

      EasyMock.replay(spi, store);

      // it should use the cached store, so nothing else should be called on any
      // SPI
      assertEquals(store, manager.getPageStore(destination));

      EasyMock.verify(spi, store);

      EasyMock.reset(spi, store);

      store.stop();

      EasyMock.replay(spi, store);

      manager.stop();

      EasyMock.verify(spi, store);

   }

   public void testMultipleThreadsGetStore() throws Exception
   {
      PagingStoreFactory spi = EasyMock.createMock(PagingStoreFactory.class);
      final PagingManagerImpl manager = new PagingManagerImpl(spi, null, repoSettings, -1);

      final SimpleString destination = new SimpleString("some-destination");

      final SequentialFileFactory factory = EasyMock.createNiceMock(SequentialFileFactory.class);

      EasyMock.expect(factory.listFiles(EasyMock.isA(String.class))).andStubReturn(new ArrayList<String>());

      PagingStoreImpl storeImpl = new PagingStoreImpl(manager,
                                                      factory,
                                                      destination,
                                                      new QueueSettings(),
                                                      Executors.newSingleThreadExecutor());

      EasyMock.expect(spi.newStore(EasyMock.eq(destination), EasyMock.isA(QueueSettings.class)))
              .andStubReturn(storeImpl);

      EasyMock.replay(spi, factory);

      manager.start();

      int NUMBER_OF_THREADS = 100;

      final CountDownLatch latchPositioned = new CountDownLatch(NUMBER_OF_THREADS);
      final CountDownLatch latchReady = new CountDownLatch(1);

      class GetPageThread extends Thread
      {
         Exception e;

         @Override
         public void run()
         {
            try
            {
               latchPositioned.countDown();
               latchReady.await();
               manager.getPageStore(destination);

            }
            catch (Exception e)
            {
               e.printStackTrace();
               this.e = e;
            }

         }
      }

      GetPageThread threads[] = new GetPageThread[NUMBER_OF_THREADS];
      for (int i = 0; i < NUMBER_OF_THREADS; i++)
      {
         threads[i] = new GetPageThread();
         threads[i].start();
      }

      latchPositioned.await();
      latchReady.countDown();

      for (GetPageThread thread : threads)
      {
         thread.join();
         if (thread.e != null)
         {
            throw thread.e;
         }
      }

      EasyMock.verify(spi, factory);

   }

   public void testOnDepage() throws Exception
   {
      long time = System.currentTimeMillis() + 10000;
      List<MessageReference> refs = new ArrayList<MessageReference>();
      MessageReference ref = EasyMock.createStrictMock(MessageReference.class);
      refs.add(ref);
      Queue queue = EasyMock.createStrictMock(Queue.class);
      HierarchicalRepository<QueueSettings> queueSettings = new HierarchicalObjectRepository<QueueSettings>();
      queueSettings.setDefault(new QueueSettings());
      PostOffice po = EasyMock.createStrictMock(PostOffice.class);
      PagingStoreFactory spi = EasyMock.createMock(PagingStoreFactory.class);
      PagingStore store = EasyMock.createNiceMock(PagingStore.class);
      StorageManager storageManager = EasyMock.createStrictMock(StorageManager.class);
      PagingManagerImpl manager = new PagingManagerImpl(spi, storageManager, queueSettings, -1);
      manager.setPostOffice(po);
      ServerMessage message = EasyMock.createStrictMock(ServerMessage.class);

      EasyMock.expect(storageManager.generateUniqueID()).andReturn(1l);
      EasyMock.expect(po.route(message)).andReturn(refs);
      EasyMock.expect(message.getDurableRefCount()).andReturn(1);
      storageManager.storeLastPage(EasyMock.anyLong(), (LastPageRecord) EasyMock.anyObject());
      storageManager.storeMessageTransactional(EasyMock.anyLong(), (ServerMessage) EasyMock.anyObject());
      storageManager.commit(EasyMock.anyLong());
      EasyMock.expect(ref.getQueue()).andReturn(queue);
      EasyMock.expect(queue.addLast(ref)).andReturn(null);
      EasyMock.replay(spi, store, message, storageManager, po, ref, queue);
      SimpleString queueName = new SimpleString("aq");
      PageMessageImpl pageMessage = new PageMessageImpl(message);

      manager.onDepage(0, queueName, store, new PageMessage[] {pageMessage} );
      EasyMock.verify(spi, store, message, storageManager, po, ref, queue);
   }

   public void testOnDepageScheduledMessage() throws Exception
   {
      long time = System.currentTimeMillis() + 10000;
      List<MessageReference> refs = new ArrayList<MessageReference>();
      MessageReference ref = EasyMock.createStrictMock(MessageReference.class);
      refs.add(ref);
      Queue queue = EasyMock.createStrictMock(Queue.class);
      HierarchicalRepository<QueueSettings> queueSettings = new HierarchicalObjectRepository<QueueSettings>();
      queueSettings.setDefault(new QueueSettings());
      PostOffice po = EasyMock.createStrictMock(PostOffice.class);
      PagingStoreFactory spi = EasyMock.createMock(PagingStoreFactory.class);
      PagingStore store = EasyMock.createNiceMock(PagingStore.class);
      StorageManager storageManager = EasyMock.createStrictMock(StorageManager.class);
      PagingManagerImpl manager = new PagingManagerImpl(spi, storageManager, queueSettings, -1);
      manager.setPostOffice(po);
      ServerMessage message = EasyMock.createStrictMock(ServerMessage.class);

      EasyMock.expect(storageManager.generateUniqueID()).andReturn(1l);
      EasyMock.expect(po.route(message)).andReturn(refs);
      EasyMock.expect(message.getDurableRefCount()).andReturn(1);
      ref.setScheduledDeliveryTime(time);
      storageManager.storeLastPage(EasyMock.anyLong(), (LastPageRecord) EasyMock.anyObject());
      storageManager.storeMessageReferenceScheduledTransactional(EasyMock.anyLong(), EasyMock.anyLong(), EasyMock.anyLong(), EasyMock.eq(time));
      storageManager.storeMessageTransactional(EasyMock.anyLong(), (ServerMessage) EasyMock.anyObject());
      storageManager.commit(EasyMock.anyLong());
      EasyMock.expect(ref.getQueue()).andStubReturn(queue);
      EasyMock.expect(queue.isDurable()).andReturn(true);
      EasyMock.expect(queue.getPersistenceID()).andStubReturn(1);
      EasyMock.expect(message.getMessageID()).andStubReturn(2);
      //storageManager.storeMessageReferenceScheduledTransactional(1,1,2,time);
      EasyMock.expect(queue.addLast(ref)).andReturn(HandleStatus.HANDLED);
      EasyMock.replay(spi, store, message, storageManager, po, ref, queue);
      SimpleString queueName = new SimpleString("aq");
      PageMessageImpl pageMessage = new PageMessageImpl(message);

      pageMessage.getProperties().putLongProperty(new SimpleString("JBM_SCHEDULED_DELIVERY_PROP"), time);
      manager.onDepage(0, queueName, store, new PageMessage[] {pageMessage} );
      EasyMock.verify(spi, store, message, storageManager, po, ref, queue);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
