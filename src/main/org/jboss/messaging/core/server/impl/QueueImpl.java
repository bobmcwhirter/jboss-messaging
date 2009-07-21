/*
 * JBoss, Home of Professional Open Source Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors by
 * the @authors tag. See the copyright.txt in the distribution for a full listing of individual contributors. This is
 * free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. You should have received a copy of the GNU Lesser General Public License along with this software; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.jboss.messaging.core.server.impl;

import static org.jboss.messaging.core.message.impl.MessageImpl.HDR_ACTUAL_EXPIRY_TIME;
import static org.jboss.messaging.core.message.impl.MessageImpl.HDR_ORIGINAL_DESTINATION;
import static org.jboss.messaging.core.message.impl.MessageImpl.HDR_ORIG_MESSAGE_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.messaging.core.filter.Filter;
import org.jboss.messaging.core.list.PriorityLinkedList;
import org.jboss.messaging.core.list.impl.PriorityLinkedListImpl;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.message.impl.MessageImpl;
import org.jboss.messaging.core.paging.PagingManager;
import org.jboss.messaging.core.paging.PagingStore;
import org.jboss.messaging.core.persistence.StorageManager;
import org.jboss.messaging.core.postoffice.Bindings;
import org.jboss.messaging.core.postoffice.PostOffice;
import org.jboss.messaging.core.remoting.Channel;
import org.jboss.messaging.core.server.Consumer;
import org.jboss.messaging.core.server.Distributor;
import org.jboss.messaging.core.server.HandleStatus;
import org.jboss.messaging.core.server.MessageReference;
import org.jboss.messaging.core.server.Queue;
import org.jboss.messaging.core.server.ScheduledDeliveryHandler;
import org.jboss.messaging.core.server.ServerMessage;
import org.jboss.messaging.core.server.cluster.impl.Redistributor;
import org.jboss.messaging.core.settings.HierarchicalRepository;
import org.jboss.messaging.core.settings.impl.AddressSettings;
import org.jboss.messaging.core.transaction.Transaction;
import org.jboss.messaging.core.transaction.TransactionOperation;
import org.jboss.messaging.core.transaction.TransactionPropertyIndexes;
import org.jboss.messaging.core.transaction.impl.TransactionImpl;
import org.jboss.messaging.utils.ConcurrentHashSet;
import org.jboss.messaging.utils.ConcurrentSet;
import org.jboss.messaging.utils.SimpleString;

/**
 * Implementation of a Queue
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 * @author <a href="clebert.suconic@jboss.com">Clebert Suconic</a>
 */
public class QueueImpl implements Queue
{
   private static final Logger log = Logger.getLogger(QueueImpl.class);

   public static final int REDISTRIBUTOR_BATCH_SIZE = 100;

   private static final boolean trace = log.isTraceEnabled();

   public static final int NUM_PRIORITIES = 10;

   private volatile long persistenceID = -1;

   private final SimpleString name;

   private volatile Filter filter;

   private final boolean durable;

   private final boolean temporary;

   private final PostOffice postOffice;

   private final PriorityLinkedList<MessageReference> messageReferences = new PriorityLinkedListImpl<MessageReference>(NUM_PRIORITIES);

   private final ConcurrentSet<MessageReference> expiringMessageReferences = new ConcurrentHashSet<MessageReference>();

   private final ScheduledDeliveryHandler scheduledDeliveryHandler;

   private volatile Distributor distributionPolicy = new RoundRobinDistributor();

   private boolean direct;

   private boolean promptDelivery;

   private final AtomicInteger messagesAdded = new AtomicInteger(0);

   protected final AtomicInteger deliveringCount = new AtomicInteger(0);

   private final AtomicBoolean waitingToDeliver = new AtomicBoolean(false);

   private final Runnable deliverRunner = new DeliverRunner();

   private final PagingManager pagingManager;

   private final Semaphore lock = new Semaphore(1);

   private volatile PagingStore pagingStore;

   private final StorageManager storageManager;

   private final HierarchicalRepository<AddressSettings> addressSettingsRepository;

   private final ScheduledExecutorService scheduledExecutor;

   private volatile boolean backup;

   private int consumersToFailover = -1;

   private SimpleString address;

   private Redistributor redistributor;

   private final Set<ScheduledFuture<?>> futures = new ConcurrentHashSet<ScheduledFuture<?>>();

   private ScheduledFuture<?> future;

   // We cache the consumers here since we don't want to include the redistributor

   private final Set<Consumer> consumers = new HashSet<Consumer>();

   private final Map<Consumer, Iterator<MessageReference>> iterators = new HashMap<Consumer, Iterator<MessageReference>>();

   private ConcurrentMap<SimpleString, Consumer> groups = new ConcurrentHashMap<SimpleString, Consumer>();

   public QueueImpl(final long persistenceID,
                    final SimpleString address,
                    final SimpleString name,
                    final Filter filter,
                    final boolean durable,
                    final boolean temporary,
                    final ScheduledExecutorService scheduledExecutor,
                    final PostOffice postOffice,
                    final StorageManager storageManager,
                    final HierarchicalRepository<AddressSettings> addressSettingsRepository)
   {
      this.persistenceID = persistenceID;

      this.address = address;

      this.name = name;

      this.filter = filter;

      this.durable = durable;

      this.temporary = temporary;

      this.postOffice = postOffice;

      this.storageManager = storageManager;

      this.addressSettingsRepository = addressSettingsRepository;

      this.scheduledExecutor = scheduledExecutor;

      if (postOffice == null)
      {
         pagingManager = null;
      }
      else
      {
         pagingManager = postOffice.getPagingManager();
      }

      direct = true;

      scheduledDeliveryHandler = new ScheduledDeliveryHandlerImpl(scheduledExecutor);
   }

   // Bindable implementation -------------------------------------------------------------------------------------

   public SimpleString getRoutingName()
   {
      return name;
   }

   public SimpleString getUniqueName()
   {
      return name;
   }

   public boolean isExclusive()
   {
      return false;
   }

   public void preroute(final ServerMessage message, final Transaction tx) throws Exception
   {
      int count = message.incrementRefCount();

      if (count == 1)
      {
         PagingStore store = pagingManager.getPageStore(message.getDestination());

         store.addSize(message.getMemoryEstimate());
      }

      boolean durableRef = message.isDurable() && durable;

      if (durableRef)
      {
         message.incrementDurableRefCount();
      }
   }

   public void route(final ServerMessage message, final Transaction tx) throws Exception
   {
      boolean durableRef = message.isDurable() && durable;

      // If durable, must be persisted before anything is routed
      MessageReference ref = message.createReference(this);

      PagingStore store = pagingManager.getPageStore(message.getDestination());

      store.addSize(ref.getMemoryEstimate());

      Long scheduledDeliveryTime = (Long)message.getProperty(MessageImpl.HDR_SCHEDULED_DELIVERY_TIME);

      if (scheduledDeliveryTime != null)
      {
         ref.setScheduledDeliveryTime(scheduledDeliveryTime);
      }

      if (tx == null)
      {
         if (durableRef)
         {
            if (!message.isStored())
            {
               storageManager.storeMessage(message);

               message.setStored();
            }

            storageManager.storeReference(ref.getQueue().getPersistenceID(), message.getMessageID());
         }

         if (scheduledDeliveryTime != null && durableRef)
         {
            storageManager.updateScheduledDeliveryTime(ref);
         }

         addLast(ref);
      }
      else
      {
         if (durableRef)
         {
            if (!message.isStored())
            {
               storageManager.storeMessageTransactional(tx.getID(), message);

               message.setStored();
            }

            tx.putProperty(TransactionPropertyIndexes.CONTAINS_PERSISTENT, true);

            storageManager.storeReferenceTransactional(tx.getID(),
                                                       ref.getQueue().getPersistenceID(),
                                                       message.getMessageID());
         }

         if (scheduledDeliveryTime != null && durableRef)
         {
            storageManager.updateScheduledDeliveryTimeTransactional(tx.getID(), ref);
         }

         getRefsOperation(tx).addRef(ref);
      }
   }

   public MessageReference reroute(final ServerMessage message, final Transaction tx) throws Exception
   {
      MessageReference ref = message.createReference(this);

      int count = message.incrementRefCount();

      PagingStore store = pagingManager.getPageStore(message.getDestination());

      if (count == 1)
      {
         store.addSize(message.getMemoryEstimate());
      }

      store.addSize(ref.getMemoryEstimate());

      boolean durableRef = message.isDurable() && durable;

      if (durableRef)
      {
         message.incrementDurableRefCount();
      }

      Long scheduledDeliveryTime = (Long)message.getProperty(MessageImpl.HDR_SCHEDULED_DELIVERY_TIME);

      if (scheduledDeliveryTime != null)
      {
         ref.setScheduledDeliveryTime(scheduledDeliveryTime);
      }

      if (tx == null)
      {
         addLast(ref);
      }
      else
      {
         getRefsOperation(tx).addRef(ref);
      }

      message.setStored();

      return ref;
   }

   // Queue implementation ----------------------------------------------------------------------------------------

   public void lockDelivery()
   {
      if (backup)
      {
         return;
      }

      try
      {
         lock.acquire();
      }
      catch (InterruptedException e)
      {
         log.warn(e.getMessage(), e);
      }
   }

   public void unlockDelivery()
   {
      if (backup)
      {
         return;
      }

      lock.release();
   }

   public boolean isDurable()
   {
      return durable;
   }

   public boolean isTemporary()
   {
      return temporary;
   }

   public SimpleString getName()
   {
      return name;
   }

   public long getPersistenceID()
   {
      return persistenceID;
   }

   public void setPersistenceID(final long id)
   {
      persistenceID = id;
   }

   public Filter getFilter()
   {
      return filter;
   }

   public void addLast(final MessageReference ref)
   {
      add(ref, false);
   }

   public void addFirst(final MessageReference ref)
   {
      add(ref, true);
   }

   public void deliverAsync(final Executor executor)
   {
      // Prevent too many executors running at once

      if (waitingToDeliver.compareAndSet(false, true))
      {
         executor.execute(deliverRunner);
      }
   }

   // Only used in testing - do not call directly!
   public synchronized void deliverNow()
   {
      deliver();
   }

   public synchronized void addConsumer(final Consumer consumer) throws Exception
   {
      cancelRedistributor();

      distributionPolicy.addConsumer(consumer);
      consumers.add(consumer);
      if (consumer.getFilter() != null)
      {
         iterators.put(consumer, messageReferences.iterator());
      }
   }

   public synchronized boolean removeConsumer(final Consumer consumer) throws Exception
   {
      boolean removed = distributionPolicy.removeConsumer(consumer);

      if (distributionPolicy.getConsumerCount() == 0)
      {
         promptDelivery = false;
      }

      consumers.remove(consumer);
      iterators.remove(consumer);

      if (removed)
      {
         for (SimpleString groupID : groups.keySet())
         {
            if (consumer == groups.get(groupID))
            {
               groups.remove(groupID);
            }
         }
      }

      return removed;
   }

   public synchronized void addRedistributor(final long delay, final Executor executor, final Channel replicatingChannel)
   {
      if (future != null)
      {
         future.cancel(false);

         futures.remove(future);
      }

      if (redistributor != null)
      {
         // Just prompt delivery
         deliverAsync(executor);
      }

      if (delay > 0)
      {
         if (consumers.size() == 0)
         {
            DelayedAddRedistributor dar = new DelayedAddRedistributor(executor, replicatingChannel);

            future = scheduledExecutor.schedule(dar, delay, TimeUnit.MILLISECONDS);

            futures.add(future);
         }
      }
      else
      {
         internalAddRedistributor(executor, replicatingChannel);
      }
   }

   public synchronized void cancelRedistributor() throws Exception
   {
      if (redistributor != null)
      {
         redistributor.stop();

         redistributor = null;
      }

      if (future != null)
      {
         future.cancel(false);

         future = null;
      }
   }

   public synchronized int getConsumerCount()
   {
      return consumers.size();
   }

   public synchronized Set<Consumer> getConsumers()
   {
      return consumers;
   }

   public Iterator<MessageReference> iterator()
   {
      return new Iterator<MessageReference>()
      {
         private final Iterator<MessageReference> iterator = messageReferences.iterator();

         public boolean hasNext()
         {
            return iterator.hasNext();
         }

         public MessageReference next()
         {
            return iterator.next();
         }

         public void remove()
         {
            throw new UnsupportedOperationException("iterator is immutable");
         }
      };
   }

   public synchronized List<MessageReference> list(final Filter filter)
   {
      if (filter == null)
      {
         return new ArrayList<MessageReference>(messageReferences.getAll());
      }
      else
      {
         ArrayList<MessageReference> list = new ArrayList<MessageReference>();

         for (MessageReference ref : messageReferences.getAll())
         {
            if (filter.match(ref.getMessage()))
            {
               list.add(ref);
            }
         }

         return list;
      }
   }

   public synchronized MessageReference removeReferenceWithID(final long id) throws Exception
   {
      Iterator<MessageReference> iterator = messageReferences.iterator();

      MessageReference removed = null;

      while (iterator.hasNext())
      {
         MessageReference ref = iterator.next();

         if (ref.getMessage().getMessageID() == id)
         {
            iterator.remove();

            removed = ref;

            removeExpiringReference(removed);

            break;
         }
      }

      if (removed == null)
      {
         // Look in scheduled deliveries
         removed = scheduledDeliveryHandler.removeReferenceWithID(id);
      }

      return removed;
   }

   public synchronized MessageReference removeFirstReference(final long id) throws Exception
   {
      MessageReference ref = messageReferences.peekFirst();

      if (ref != null && ref.getMessage().getMessageID() == id)
      {
         messageReferences.removeFirst();

         return ref;
      }
      else
      {
         ref = scheduledDeliveryHandler.removeReferenceWithID(id);
      }

      return ref;
   }

   public synchronized MessageReference getReference(final long id)
   {
      Iterator<MessageReference> iterator = messageReferences.iterator();

      while (iterator.hasNext())
      {
         MessageReference ref = iterator.next();

         if (ref.getMessage().getMessageID() == id)
         {
            return ref;
         }
      }

      return null;
   }

   public synchronized int getMessageCount()
   {
      int count = messageReferences.size() + getScheduledCount() + getDeliveringCount();

      // log.info(System.identityHashCode(this) + " message count is " +
      // count +
      // " ( mr:" +
      // messageReferences.size() +
      // " sc:" +
      // getScheduledCount() +
      // " dc:" +
      // getDeliveringCount() +
      // ")");

      return count;
   }

   public synchronized int getScheduledCount()
   {
      return scheduledDeliveryHandler.getScheduledCount();
   }

   public synchronized List<MessageReference> getScheduledMessages()
   {
      return scheduledDeliveryHandler.getScheduledReferences();
   }

   public int getDeliveringCount()
   {
      return deliveringCount.get();
   }

   public void acknowledge(final MessageReference ref) throws Exception
   {
      ServerMessage message = ref.getMessage();

      boolean durableRef = message.isDurable() && durable;

      if (durableRef)
      {
         storageManager.storeAcknowledge(persistenceID, message.getMessageID());
      }

      postAcknowledge(ref);
   }

   public void acknowledge(final Transaction tx, final MessageReference ref) throws Exception
   {
      ServerMessage message = ref.getMessage();

      boolean durableRef = message.isDurable() && durable;

      if (durableRef)
      {
         storageManager.storeAcknowledgeTransactional(tx.getID(), persistenceID, message.getMessageID());

         tx.putProperty(TransactionPropertyIndexes.CONTAINS_PERSISTENT, true);
      }

      getRefsOperation(tx).addAck(ref);
   }

   public void reacknowledge(final Transaction tx, final MessageReference ref) throws Exception
   {
      ServerMessage message = ref.getMessage();

      if (message.isDurable() && durable)
      {
         tx.putProperty(TransactionPropertyIndexes.CONTAINS_PERSISTENT, true);
      }

      getRefsOperation(tx).addAck(ref);
   }

   final RefsOperation getRefsOperation(final Transaction tx)
   {
      synchronized (tx)
      {
         RefsOperation oper = (RefsOperation)tx.getProperty(TransactionPropertyIndexes.REFS_OPERATION);

         if (oper == null)
         {
            oper = new RefsOperation();

            tx.putProperty(TransactionPropertyIndexes.REFS_OPERATION, oper);

            tx.addOperation(oper);
         }

         return oper;
      }
   }

   public void cancel(final Transaction tx, final MessageReference reference) throws Exception
   {
      getRefsOperation(tx).addAck(reference);
   }

   public synchronized void cancel(final MessageReference reference) throws Exception
   {
      if (checkDLQ(reference))
      {
         if (!scheduledDeliveryHandler.checkAndSchedule(reference, backup))
         {
            messageReferences.addFirst(reference, reference.getMessage().getPriority());
         }
      }
   }

   public void expire(final MessageReference ref) throws Exception
   {
      SimpleString expiryAddress = addressSettingsRepository.getMatch(address.toString()).getExpiryAddress();

      if (expiryAddress != null)
      {
         Bindings bindingList = postOffice.getBindingsForAddress(expiryAddress);

         if (bindingList.getBindings().isEmpty())
         {
            log.warn("Message has expired. No bindings for Expiry Address " + expiryAddress + " so dropping it");
         }
         else
         {
            move(expiryAddress, ref, true);
         }
      }
      else
      {
         log.warn("Message has expired. No expiry queue configured for queue " + name + " so dropping it");

         acknowledge(ref);
      }
   }

   public void referenceHandled()
   {
      deliveringCount.incrementAndGet();
   }

   public Distributor getDistributionPolicy()
   {
      return distributionPolicy;
   }

   public void setDistributionPolicy(final Distributor distributionPolicy)
   {
      this.distributionPolicy = distributionPolicy;
   }

   public int getMessagesAdded()
   {
      return messagesAdded.get();
   }

   public int deleteAllReferences() throws Exception
   {
      return deleteMatchingReferences(null);
   }

   public synchronized int deleteMatchingReferences(final Filter filter) throws Exception
   {
      int count = 0;

      Transaction tx = new TransactionImpl(storageManager);

      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();

         if (filter == null || filter.match(ref.getMessage()))
         {
            deliveringCount.incrementAndGet();
            acknowledge(tx, ref);
            iter.remove();
            count++;
         }
      }

      List<MessageReference> cancelled = scheduledDeliveryHandler.cancel();
      for (MessageReference messageReference : cancelled)
      {
         if (filter == null || filter.match(messageReference.getMessage()))
         {
            deliveringCount.incrementAndGet();
            acknowledge(tx, messageReference);
            count++;
         }
      }

      tx.commit();

      return count;
   }

   public synchronized boolean deleteReference(final long messageID) throws Exception
   {
      boolean deleted = false;

      Transaction tx = new TransactionImpl(storageManager);

      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();
         if (ref.getMessage().getMessageID() == messageID)
         {
            deliveringCount.incrementAndGet();
            acknowledge(tx, ref);
            iter.remove();
            deleted = true;
            break;
         }
      }

      tx.commit();

      return deleted;
   }

   public synchronized boolean expireReference(final long messageID) throws Exception
   {
      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();
         if (ref.getMessage().getMessageID() == messageID)
         {
            deliveringCount.incrementAndGet();
            expire(ref);
            iter.remove();
            return true;
         }
      }
      return false;
   }

   public synchronized int expireReferences(final Filter filter) throws Exception
   {
      Transaction tx = new TransactionImpl(storageManager);

      int count = 0;
      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();
         if (filter == null || filter.match(ref.getMessage()))
         {
            deliveringCount.incrementAndGet();
            expire(tx, ref);
            iter.remove();
            count++;
         }
      }

      tx.commit();

      return count;
   }

   public synchronized void expireReferences() throws Exception
   {
      for (MessageReference expiringMessageReference : expiringMessageReferences)
      {
         if (expiringMessageReference.getMessage().isExpired())
         {
            expireReference(expiringMessageReference.getMessage().getMessageID());
         }
      }
   }

   public synchronized boolean sendMessageToDeadLetterAddress(final long messageID) throws Exception
   {
      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();
         if (ref.getMessage().getMessageID() == messageID)
         {
            deliveringCount.incrementAndGet();
            sendToDeadLetterAddress(ref);
            iter.remove();
            return true;
         }
      }
      return false;
   }

   public synchronized boolean moveReference(final long messageID, final SimpleString toAddress) throws Exception
   {
      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();
         if (ref.getMessage().getMessageID() == messageID)
         {
            iter.remove();
            deliveringCount.incrementAndGet();
            move(toAddress, ref);
            return true;
         }
      }
      return false;
   }

   public synchronized int moveReferences(final Filter filter, final SimpleString toAddress) throws Exception
   {
      Transaction tx = new TransactionImpl(storageManager);

      int count = 0;
      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();
         if (filter == null || filter.match(ref.getMessage()))
         {
            deliveringCount.incrementAndGet();
            move(toAddress, tx, ref, false);
            iter.remove();
            count++;
         }
      }

      List<MessageReference> cancelled = scheduledDeliveryHandler.cancel();
      for (MessageReference ref : cancelled)
      {
         if (filter == null || filter.match(ref.getMessage()))
         {
            deliveringCount.incrementAndGet();
            move(toAddress, tx, ref, false);
            acknowledge(tx, ref);
            count++;
         }
      }

      tx.commit();

      return count;
   }

   public synchronized boolean changeReferencePriority(final long messageID, final byte newPriority) throws Exception
   {
      List<MessageReference> refs = list(null);
      for (MessageReference ref : refs)
      {
         ServerMessage message = ref.getMessage();
         if (message.getMessageID() == messageID)
         {
            message.setPriority(newPriority);
            // delete and add the reference so that it
            // goes to the right queues for the new priority

            // FIXME - why deleting the reference?? This will delete it from storage!!

            deleteReference(messageID);
            addLast(ref);
            return true;
         }
      }
      return false;
   }

   public boolean isBackup()
   {
      return backup;
   }

   public synchronized void setBackup()
   {
      backup = true;

      direct = false;
   }

   public synchronized boolean activate()
   {
      consumersToFailover = consumers.size();

      if (consumersToFailover == 0)
      {
         backup = false;

         return true;
      }
      else
      {
         return false;
      }
   }

   public synchronized void activateNow(final Executor executor)
   {
      if (backup)
      {
         log.info("Timed out waiting for all consumers to reconnect to queue " + name +
                  " so queue will be activated now");

         backup = false;

         scheduledDeliveryHandler.reSchedule();

         deliverAsync(executor);
      }
   }

   public synchronized boolean consumerFailedOver()
   {
      consumersToFailover--;

      if (consumersToFailover == 0)
      {
         // All consumers for the queue have failed over, can re-activate it now

         backup = false;

         scheduledDeliveryHandler.reSchedule();

         return true;
      }
      else
      {
         return false;
      }
   }

   // Public
   // -----------------------------------------------------------------------------

   @Override
   public boolean equals(final Object other)
   {
      if (this == other)
      {
         return true;
      }

      QueueImpl qother = (QueueImpl)other;

      return name.equals(qother.name);
   }

   @Override
   public int hashCode()
   {
      return name.hashCode();
   }

   @Override
   public String toString()
   {
      return "QueueImpl(name=" + this.name.toString() + ")";
   }

   // Private
   // ------------------------------------------------------------------------------

   private void internalAddRedistributor(final Executor executor, final Channel replicatingChannel)
   {
      // create the redistributor only once if there are no local consumers
      if (consumers.size() == 0 && redistributor == null)
      {
         redistributor = new Redistributor(this,
                                           storageManager,
                                           postOffice,
                                           executor,
                                           REDISTRIBUTOR_BATCH_SIZE,
                                           replicatingChannel);

         distributionPolicy.addConsumer(redistributor);

         redistributor.start();

         deliverAsync(executor);
      }
   }

   public boolean checkDLQ(final MessageReference reference) throws Exception
   {
      ServerMessage message = reference.getMessage();

      if (message.isDurable() && durable)
      {
         storageManager.updateDeliveryCount(reference);
      }

      AddressSettings addressSettings = addressSettingsRepository.getMatch(address.toString());

      int maxDeliveries = addressSettings.getMaxDeliveryAttempts();

      if (maxDeliveries > 0 && reference.getDeliveryCount() >= maxDeliveries)
      {
         sendToDeadLetterAddress(reference);

         return false;
      }
      else
      {
         long redeliveryDelay = addressSettings.getRedeliveryDelay();

         if (redeliveryDelay > 0)
         {
            reference.setScheduledDeliveryTime(System.currentTimeMillis() + redeliveryDelay);

            storageManager.updateScheduledDeliveryTime(reference);
         }

         deliveringCount.decrementAndGet();

         return true;
      }
   }

   private void move(final SimpleString toAddress, final MessageReference ref) throws Exception
   {
      move(toAddress, ref, false);
   }

   private void move(final SimpleString toAddress,
                     final Transaction tx,
                     final MessageReference ref,
                     final boolean expiry) throws Exception
   {
      ServerMessage copyMessage = makeCopy(ref, expiry);

      copyMessage.setDestination(toAddress);

      postOffice.route(copyMessage, tx);

      acknowledge(tx, ref);
   }

   private ServerMessage makeCopy(final MessageReference ref, final boolean expiry) throws Exception
   {
      ServerMessage message = ref.getMessage();
      /*
       We copy the message and send that to the dla/expiry queue - this is
       because otherwise we may end up with a ref with the same message id in the
       queue more than once which would barf - this might happen if the same message had been
       expire from multiple subscriptions of a topic for example
       We set headers that hold the original message destination, expiry time
       and original message id
      */

      long newMessageId = storageManager.generateUniqueID();

      ServerMessage copy = message.copy(newMessageId);

      if (ref.getMessage().getProperty(HDR_ORIG_MESSAGE_ID) != null)
      {
         copy.putStringProperty(HDR_ORIGINAL_DESTINATION, (SimpleString)ref.getMessage()
                                                                           .getProperty(HDR_ORIGINAL_DESTINATION));
         copy.putLongProperty(HDR_ORIG_MESSAGE_ID, (Long)ref.getMessage().getProperty(HDR_ORIG_MESSAGE_ID));
      }
      else
      {
         SimpleString originalQueue = copy.getDestination();
         copy.putStringProperty(HDR_ORIGINAL_DESTINATION, originalQueue);
         copy.putLongProperty(HDR_ORIG_MESSAGE_ID, message.getMessageID());
      }
      // reset expiry
      copy.setExpiration(0);
      if (expiry)
      {
         long actualExpiryTime = System.currentTimeMillis();

         copy.putLongProperty(HDR_ACTUAL_EXPIRY_TIME, actualExpiryTime);
      }

      return copy;
   }

   private void expire(final Transaction tx, final MessageReference ref) throws Exception
   {
      SimpleString expiryAddress = addressSettingsRepository.getMatch(address.toString()).getExpiryAddress();

      if (expiryAddress != null)
      {
         Bindings bindingList = postOffice.getBindingsForAddress(expiryAddress);

         if (bindingList.getBindings().isEmpty())
         {
            log.warn("Message has expired. No bindings for Expiry Address " + expiryAddress + " so dropping it");
         }
         else
         {
            move(expiryAddress, tx, ref, true);
         }
      }
      else
      {
         log.warn("Message has expired. No expiry queue configured for queue " + name + " so dropping it");

         acknowledge(tx, ref);
      }
   }

   private void sendToDeadLetterAddress(final MessageReference ref) throws Exception
   {
      SimpleString deadLetterAddress = addressSettingsRepository.getMatch(address.toString()).getDeadLetterAddress();
      if (deadLetterAddress != null)
      {
         Bindings bindingList = postOffice.getBindingsForAddress(deadLetterAddress);

         if (bindingList.getBindings().isEmpty())
         {
            log.warn("Message has exceeded max delivery attempts. No bindings for Dead Letter Address " + deadLetterAddress +
                     " so dropping it");
         }
         else
         {

            log.warn("Message has reached maximum delivery attempts, sending it to Dead Letter Address " + deadLetterAddress +
                     " from " +
                     name);
            move(deadLetterAddress, ref, false);
         }
      }
      else
      {
         log.warn("Message has exceeded max delivery attempts. No Dead Letter Address configured for queue " + name +
                  " so dropping it");

         acknowledge(ref);
      }
   }

   private void move(final SimpleString address, final MessageReference ref, final boolean expiry) throws Exception
   {
      Transaction tx = new TransactionImpl(storageManager);

      ServerMessage copyMessage = makeCopy(ref, expiry);

      copyMessage.setDestination(address);

      postOffice.route(copyMessage, tx);

      acknowledge(tx, ref);

      tx.commit();
   }

   /*
    * Attempt to deliver all the messages in the queue
    */
   private synchronized void deliver()
   {
      // We don't do actual delivery if the queue is on a backup node - this is
      // because it's async and could get out of step
      // with the live node. Instead, when we replicate the delivery we remove
      // the ref from the queue

      if (backup)
      {
         return;
      }

      direct = false;

      if (distributionPolicy.getConsumerCount() == 0)
      {
         return;
      }

      Consumer consumer;

      MessageReference reference;

      Iterator<MessageReference> iterator = null;

      int totalConsumers = distributionPolicy.getConsumerCount();
      Set<Consumer> busyConsumers = new HashSet<Consumer>();
      Set<Consumer> nullReferences = new HashSet<Consumer>();

      while (true)
      {
         consumer = distributionPolicy.getNextConsumer();

         iterator = iterators.get(consumer);

         if (iterator == null)
         {
            reference = messageReferences.peekFirst();
         }
         else
         {
            if (iterator.hasNext())
            {
               reference = iterator.next();
            }
            else
            {
               reference = null;
               if (consumer.getFilter() != null)
               {
                  // we have iterated on the whole queue for
                  // messages which matches the consumer filter.
                  // we reset its iterator in case new messages are added to the queue
                  iterators.put(consumer, messageReferences.iterator());
               }
            }
         }

         if (reference == null)
         {
            nullReferences.add(consumer);
            if (nullReferences.size() + busyConsumers.size() == totalConsumers)
            {
               startDepaging();
               // We delivered all the messages - go into direct delivery
               direct = true;
               promptDelivery = false;
               return;
            }
            continue;
         }
         else
         {
            nullReferences.remove(consumer);
         }

         initPagingStore(reference.getMessage().getDestination());

         final SimpleString groupID = (SimpleString)reference.getMessage().getProperty(MessageImpl.HDR_GROUP_ID);

         if (groupID != null)
         {
            Consumer groupConsumer = groups.putIfAbsent(groupID, consumer);
            if (groupConsumer != null && groupConsumer != consumer)
            {
               continue;
            }
         }

         HandleStatus status = handle(reference, consumer);

         if (status == HandleStatus.HANDLED)
         {
            if (iterator == null)
            {
               messageReferences.removeFirst();
            }
            else
            {
               iterator.remove();
            }
         }
         else if (status == HandleStatus.BUSY)
         {
            busyConsumers.add(consumer);
            if (groupID != null || busyConsumers.size() == totalConsumers)
            {
               // when all consumers are busy, we stop
               break;
            }
         }
         else if (status == HandleStatus.NO_MATCH)
         {
            // if consumer filter reject the message make sure it won't be assigned the message group
            if (groupID != null)
            {
               groups.remove(consumer);
            }
            continue;
         }
      }
   }

   private synchronized void add(final MessageReference ref, final boolean first)
   {
      if (!first)
      {
         messagesAdded.incrementAndGet();
      }

      if (scheduledDeliveryHandler.checkAndSchedule(ref, backup))
      {
         return;
      }

      boolean add = false;

      if (direct && !backup)
      {
         // Deliver directly

         HandleStatus status = directDeliver(ref);

         if (status == HandleStatus.HANDLED)
         {
            // Ok
         }
         else if (status == HandleStatus.BUSY)
         {
            add = true;
         }
         else if (status == HandleStatus.NO_MATCH)
         {
            add = true;
         }

         if (add)
         {
            direct = false;
         }
      }
      else
      {
         add = true;
      }

      if (add)
      {
         if (ref.getMessage().getExpiration() != 0)
         {
            expiringMessageReferences.addIfAbsent(ref);
         }

         if (first)
         {
            messageReferences.addFirst(ref, ref.getMessage().getPriority());
         }
         else
         {
            messageReferences.addLast(ref, ref.getMessage().getPriority());
         }

         if (!direct && promptDelivery)
         {
            // We have consumers with filters which don't match, so we need
            // to prompt delivery every time
            // a new message arrives - this is why you really shouldn't use
            // filters with queues - in most cases
            // it's an ant-pattern since it would cause a queue scan on each
            // message
            deliver();
         }
      }
   }

   private synchronized HandleStatus directDeliver(final MessageReference reference)
   {
      if (distributionPolicy.getConsumerCount() == 0)
      {
         return HandleStatus.BUSY;
      }

      HandleStatus status;

      boolean filterRejected = false;

      int consumerCount = 0;

      while (true)
      {
         Consumer consumer = distributionPolicy.getNextConsumer();
         consumerCount++;

         final SimpleString groupId = (SimpleString)reference.getMessage().getProperty(MessageImpl.HDR_GROUP_ID);

         if (groupId != null)
         {
            Consumer groupConsumer = groups.putIfAbsent(groupId, consumer);
            if (groupConsumer != null && groupConsumer != consumer)
            {
               continue;
            }
         }

         status = handle(reference, consumer);

         if (status == HandleStatus.HANDLED)
         {
            break;
         }
         else if (status == HandleStatus.NO_MATCH)
         {
            filterRejected = true;
            if (groupId != null)
            {
               groups.remove(consumer);
            }
         }
         else if (status == HandleStatus.BUSY)
         {
            if (groupId != null)
            {
               break;
            }
         }
         // if we've tried all of them
         if (consumerCount == distributionPolicy.getConsumerCount())
         {
            if (filterRejected)
            {
               status = HandleStatus.NO_MATCH;
               break;
            }
            else
            {
               // Give up - all consumers busy
               status = HandleStatus.BUSY;
               break;
            }
         }
      }

      if (status == HandleStatus.NO_MATCH)
      {
         promptDelivery = true;
      }

      return status;
   }

   private synchronized HandleStatus handle(final MessageReference reference, final Consumer consumer)
   {

      HandleStatus status;
      try
      {
         status = consumer.handle(reference);
      }
      catch (Throwable t)
      {
         log.warn("removing consumer which did not handle a message, consumer=" + consumer + ", message=" + reference,
                  t);

         // If the consumer throws an exception we remove the consumer
         try
         {
            removeConsumer(consumer);
         }
         catch (Exception e)
         {
            log.error("Failed to remove consumer", e);
         }
         return HandleStatus.BUSY;
      }

      if (status == null)
      {
         throw new IllegalStateException("ClientConsumer.handle() should never return null");
      }

      return status;
   }

   private void removeExpiringReference(final MessageReference ref) throws Exception
   {
      if (ref.getMessage().getExpiration() > 0)
      {
         expiringMessageReferences.remove(ref);
      }
   }

   private void postAcknowledge(final MessageReference ref) throws Exception
   {
      ServerMessage message = ref.getMessage();

      QueueImpl queue = (QueueImpl)ref.getQueue();

      boolean durableRef = message.isDurable() && queue.durable;

      if (durableRef)
      {
         int count = message.decrementDurableRefCount();

         if (count == 0)
         {
            // Note - we MUST store the delete after the preceeding ack has been committed to storage, we cannot combine
            // the last ack and delete into a single delete.
            // This is because otherwise we could have a situation where the same message is being acked concurrently
            // from two different queues on different sessions.
            // One decrements the ref count, then the other stores a delete, the delete gets committed, but the first
            // ack isn't committed, then the server crashes and on
            // recovery the message is deleted even though the other ack never committed
            storageManager.deleteMessage(message.getMessageID());
         }
      }

      queue.removeExpiringReference(ref);

      queue.deliveringCount.decrementAndGet();

      // TODO: We could optimize this by storing the paging-store for the address on the Queue. We would need to know
      // the Address for the Queue
      PagingStore store = null;

      if (pagingManager != null)
      {
         store = pagingManager.getPageStore(ref.getMessage().getDestination());

         store.addSize(-ref.getMemoryEstimate());
      }

      if (message.decrementRefCount() == 0)
      {
         if (store != null)
         {
            store.addSize(-ref.getMessage().getMemoryEstimate());
         }
      }
   }

   void postRollback(LinkedList<MessageReference> refs) throws Exception
   {
      synchronized (this)
      {
         for (MessageReference ref : refs)
         {
            ServerMessage msg = ref.getMessage();

            if (!scheduledDeliveryHandler.checkAndSchedule(ref, backup))
            {
               messageReferences.addFirst(ref, msg.getPriority());
            }
         }

         deliver();
      }
   }

   private synchronized void initPagingStore(SimpleString destination)
   {
      // PagingManager would be null only on testcases
      if (pagingStore == null && pagingManager != null)
      {
         // TODO: It would be better if we could initialize the pagingStore during the construction
         try
         {
            pagingStore = pagingManager.getPageStore(destination);
         }
         catch (Exception e)
         {
            // This shouldn't happen, and if it happens, this shouldn't abort the route
         }
      }
   }

   private synchronized void startDepaging()
   {
      if (pagingStore != null)
      {
         // If the queue is empty, we need to check if there are pending messages, and throw a warning
         if (pagingStore.isPaging() && !pagingStore.isDropWhenMaxSize())
         {
            // This is just a *request* to depage. Depage will only happens if there is space on the Address
            // and GlobalSize
            pagingStore.startDepaging();

            log.warn("The Queue " + name +
                     " is empty, however there are pending messages on Paging for the address " +
                     pagingStore.getStoreName() +
                     " waiting message ACK before they could be routed");
         }
      }
   }

   // Inner classes
   // --------------------------------------------------------------------------

   private class DeliverRunner implements Runnable
   {
      public void run()
      {
         // Must be set to false *before* executing to avoid race
         waitingToDeliver.set(false);

         QueueImpl.this.lockDelivery();
         try
         {
            deliver();
         }
         finally
         {
            QueueImpl.this.unlockDelivery();
         }
      }
   }

   final class RefsOperation implements TransactionOperation
   {
      List<MessageReference> refsToAdd = new ArrayList<MessageReference>();

      List<MessageReference> refsToAck = new ArrayList<MessageReference>();

      synchronized void addRef(final MessageReference ref)
      {
         refsToAdd.add(ref);
      }

      synchronized void addAck(final MessageReference ref)
      {
         refsToAck.add(ref);
      }

      public void beforeCommit(final Transaction tx) throws Exception
      {
      }

      public void afterPrepare(final Transaction tx) throws Exception
      {
      }

      public void afterRollback(final Transaction tx) throws Exception
      {
         Map<QueueImpl, LinkedList<MessageReference>> queueMap = new HashMap<QueueImpl, LinkedList<MessageReference>>();

         for (MessageReference ref : refsToAck)
         {
            if (ref.getQueue().checkDLQ(ref))
            {
               LinkedList<MessageReference> toCancel = queueMap.get(ref.getQueue());

               if (toCancel == null)
               {
                  toCancel = new LinkedList<MessageReference>();

                  queueMap.put((QueueImpl)ref.getQueue(), toCancel);
               }

               toCancel.addFirst(ref);
            }
         }

         for (Map.Entry<QueueImpl, LinkedList<MessageReference>> entry : queueMap.entrySet())
         {
            LinkedList<MessageReference> refs = entry.getValue();

            QueueImpl queue = entry.getKey();

            synchronized (queue)
            {
               queue.postRollback(refs);
            }
         }
      }

      /* (non-Javadoc)
       * @see org.jboss.messaging.core.transaction.TransactionOperation#getDistinctQueues()
       */
      public synchronized Collection<Queue> getDistinctQueues()
      {
         HashSet<Queue> queues = new HashSet<Queue>();

         for (MessageReference ref : refsToAck)
         {
            queues.add(ref.getQueue());
         }

         return queues;
      }

      public void afterCommit(final Transaction tx) throws Exception
      {
         for (MessageReference ref : refsToAdd)
         {
            ref.getQueue().addLast(ref);
         }

         for (MessageReference ref : refsToAck)
         {
            synchronized (ref.getQueue())
            {
               postAcknowledge(ref);
            }
         }
      }

      public void beforePrepare(final Transaction tx) throws Exception
      {
      }

      public void beforeRollback(final Transaction tx) throws Exception
      {
         Set<ServerMessage> msgs = new HashSet<ServerMessage>();

         for (MessageReference ref : refsToAdd)
         {
            ServerMessage msg = ref.getMessage();

            // Optimise this
            PagingStore store = pagingManager.getPageStore(msg.getDestination());

            store.addSize(-ref.getMemoryEstimate());

            if (!msgs.contains(msg))
            {
               store.addSize(-msg.getMemoryEstimate());
               msg.decrementRefCount();
            }

            msgs.add(msg);
         }
      }
   }

   private class DelayedAddRedistributor implements Runnable
   {
      private final Executor executor;

      private final Channel replicatingChannel;

      DelayedAddRedistributor(final Executor executor, final Channel replicatingChannel)
      {
         this.executor = executor;

         this.replicatingChannel = replicatingChannel;
      }

      public void run()
      {
         synchronized (QueueImpl.this)
         {
            internalAddRedistributor(executor, replicatingChannel);

            futures.remove(this);
         }
      }
   }
}
