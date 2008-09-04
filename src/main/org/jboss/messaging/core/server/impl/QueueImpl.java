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

package org.jboss.messaging.core.server.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.messaging.core.filter.Filter;
import org.jboss.messaging.core.list.PriorityLinkedList;
import org.jboss.messaging.core.list.impl.PriorityLinkedListImpl;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.persistence.StorageManager;
import org.jboss.messaging.core.postoffice.Binding;
import org.jboss.messaging.core.postoffice.FlowController;
import org.jboss.messaging.core.postoffice.PostOffice;
import org.jboss.messaging.core.server.Consumer;
import org.jboss.messaging.core.server.DistributionPolicy;
import org.jboss.messaging.core.server.HandleStatus;
import org.jboss.messaging.core.server.MessageReference;
import org.jboss.messaging.core.server.Queue;
import org.jboss.messaging.core.server.ServerMessage;
import org.jboss.messaging.core.settings.HierarchicalRepository;
import org.jboss.messaging.core.settings.impl.QueueSettings;
import org.jboss.messaging.core.transaction.Transaction;
import org.jboss.messaging.core.transaction.impl.TransactionImpl;
import org.jboss.messaging.util.SimpleString;

/**
 * 
 * Implementation of a Queue
 * 
 * TODO use Java 5 concurrent queue
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="clebert.suconic@jboss.com">Clebert Suconic</a>
 * 
 */
public class QueueImpl implements Queue
{
   private static final Logger log = Logger.getLogger(QueueImpl.class);

   private static final boolean trace = log.isTraceEnabled();
   
   public static final int NUM_PRIORITIES = 10;
   
   private volatile long persistenceID = -1;

   private final SimpleString name;

   private volatile Filter filter;

   private final boolean clustered;

   private final boolean durable;

   private final ScheduledExecutorService scheduledExecutor;

   private final PriorityLinkedList<MessageReference> messageReferences =
      new PriorityLinkedListImpl<MessageReference>(NUM_PRIORITIES);

   private final List<Consumer> consumers = new ArrayList<Consumer>();

   private final Set<ScheduledDeliveryRunnable> scheduledRunnables = new LinkedHashSet<ScheduledDeliveryRunnable>();

   private volatile DistributionPolicy distributionPolicy = new RoundRobinDistributionPolicy();

   private boolean direct;

   private boolean promptDelivery;

   private int pos;
   
   private AtomicInteger sizeBytes = new AtomicInteger(0);

   private AtomicInteger messagesAdded = new AtomicInteger(0);

   private AtomicInteger deliveringCount = new AtomicInteger(0);

   private volatile FlowController flowController;
   
   private AtomicBoolean waitingToDeliver = new AtomicBoolean(false);  
   
   private final Runnable deliverRunner = new DeliverRunner();
   
   private final Lock lock = new ReentrantLock(false);
   
   private final QueueSettings settings;
   
   private volatile boolean backup;
         
   public QueueImpl(final long persistenceID, final SimpleString name,
         final Filter filter, final boolean clustered, final boolean durable,
         final QueueSettings settings,
         final ScheduledExecutorService scheduledExecutor)
   {
      this.persistenceID = persistenceID;

      this.name = name;

      this.filter = filter;

      this.clustered = clustered;

      this.durable = durable;

      this.scheduledExecutor = scheduledExecutor;
      
      this.settings = settings;

      direct = true;
   }

   // Queue implementation
   // -------------------------------------------------------------------


   public QueueSettings getSettings()
   {
      return this.settings;
   }

   public boolean isClustered()
   {
      return clustered;
   }

   public boolean isDurable()
   {
      return durable;
   }

   public SimpleString getName()
   {
      return name;
   }
   
   public HandleStatus addLast(final MessageReference ref)
   {     
      lock.lock();
      try
      {         
         return add(ref, false);
      }
      finally
      {
         lock.unlock();
      }
   }

   public HandleStatus addFirst(final MessageReference ref)
   {
      lock.lock();

      try
      {
         return add(ref, true);
      }
      finally
      {
         lock.unlock();       
      }
   }

   public void addListFirst(final LinkedList<MessageReference> list)
   {
      ListIterator<MessageReference> iter = list.listIterator(list.size());

      while (iter.hasPrevious())
      {
         MessageReference ref = iter.previous();

         messageReferences.addFirst(ref, ref.getMessage().getPriority());
      }

      deliver();
   }
 
   public void deliverAsync(final Executor executor)
   {
      //Prevent too many executors running at once
                  
      if (waitingToDeliver.compareAndSet(false, true))
      {
         executor.execute(deliverRunner);
      }
   }
      
   /*
    * Attempt to deliver all the messages in the queue
    */
   public void deliver()
   {
      //We don't do actual delivery if the queue is on a backup node - this is because it's async and could get out of step
      //with the live node. Instead, when we replicate the delivery we remove the ref from the queue
       
      if (backup)
      {
         return;
      }
      
      //TODO - we need to lock during delivery since otherwise delivery could occur while we're rolling back a transaction
      //which would mean messages got delivered in the wrong order
      //We need to revise this for better concurrency
      lock.lock();
      try
      {  
         MessageReference reference;
   
         Iterator<MessageReference> iterator = null;
   
         while (true)
         {
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
               }
            }
   
            if (reference == null)
            {
               if (iterator == null)
               {
                  // We delivered all the messages - go into direct delivery
                  direct = true;
                  
                  promptDelivery = false;
               }
               return;
            }
   
            HandleStatus status = deliver(reference);
   
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
               // All consumers busy - give up
               break;
            }
            else if (status == HandleStatus.NO_MATCH && iterator == null)
            {
               // Consumers not all busy - but filter not accepting - iterate back
               // through the queue
               iterator = messageReferences.iterator();
            }
         }
      }
      finally
      {
         lock.unlock();
      }
   }

   public synchronized void addConsumer(final Consumer consumer)
   {
      consumers.add(consumer);
   }

   public synchronized boolean removeConsumer(final Consumer consumer) throws Exception
   {
      boolean removed = consumers.remove(consumer);
      
      if (pos == consumers.size())
      {
         pos = 0;
      }
      
      if (consumers.isEmpty())
      {
         promptDelivery = false;
      }

      return removed;
   }

   public synchronized int getConsumerCount()
   {
      return consumers.size();
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

   public synchronized boolean removeReferenceWithID(final long id)
   {
      Iterator<MessageReference> iterator = messageReferences.iterator();

      boolean removed = false;

      while (iterator.hasNext())
      {
         MessageReference ref = iterator.next();

         if (ref.getMessage().getMessageID() == id)
         {
            iterator.remove();

            removed = true;

            break;
         }
      }

      return removed;
   }

   public synchronized MessageReference getReference(final long id)
   {
      Iterator<MessageReference> iterator = messageReferences.iterator();

      while (iterator.hasNext())
      {
         MessageReference ref = iterator.next();

         if (ref.getMessage().getMessageID() == id) { return ref; }
      }

      return null;
   }

   public long getPersistenceID()
   {
      return persistenceID;
   }

   public void setPersistenceID(final long id)
   {
      this.persistenceID = id;
   }

   public Filter getFilter()
   {
      return filter;
   }

   public synchronized int getMessageCount()
   {    
      return messageReferences.size() + getScheduledCount() + getDeliveringCount();
   }

   public synchronized int getScheduledCount()
   {
      return scheduledRunnables.size();
   }

   public int getDeliveringCount()
   {
      return deliveringCount.get();
   }

   public void referenceAcknowledged(MessageReference ref) throws Exception
   {
      deliveringCount.decrementAndGet();
      
      sizeBytes.addAndGet(-ref.getMessage().getEncodeSize());

//      if (flowController != null)
//      {
//         flowController.messageAcknowledged();
//      }
   }

   public void referenceCancelled()
   {
      deliveringCount.decrementAndGet();
   }

   public int getSizeBytes()
   {
      return sizeBytes.get();
   }

   public DistributionPolicy getDistributionPolicy()
   {
      return distributionPolicy;
   }

   public void setDistributionPolicy(final DistributionPolicy distributionPolicy)
   {
      this.distributionPolicy = distributionPolicy;
   }

   public int getMessagesAdded()
   {
      return messagesAdded.get();
   }

   public void setFlowController(final FlowController flowController)
   {
      this.flowController = flowController;
   }

   public FlowController getFlowController()
   {
      return flowController;
   }

   public synchronized void deleteAllReferences(final StorageManager storageManager) throws Exception
   {
      Transaction tx = new TransactionImpl(storageManager, null);

      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();

         deliveringCount.incrementAndGet();

         tx.addAcknowledgement(ref);

         iter.remove();
      }

      synchronized (scheduledRunnables)
      {
         for (ScheduledDeliveryRunnable runnable : scheduledRunnables)
         {
            runnable.cancel();

            deliveringCount.incrementAndGet();

            tx.addAcknowledgement(runnable.getReference());
         }

         scheduledRunnables.clear();
      }

      tx.commit();
   }
   
   public synchronized boolean deleteReference(final long messageID, final StorageManager storageManager) throws Exception
   {
      boolean deleted = false;
      
      Transaction tx = new TransactionImpl(storageManager, null);

      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();
         if (ref.getMessage().getMessageID() == messageID)
         {        
            deliveringCount.incrementAndGet();
            tx.addAcknowledgement(ref);
            iter.remove();
            deleted = true;
            break;
         }
      }

      tx.commit();
      
      return deleted;
   }
   
   public boolean expireMessage(final long messageID,
         final StorageManager storageManager, final PostOffice postOffice,
         final HierarchicalRepository<QueueSettings> queueSettingsRepository)
         throws Exception
   {
      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();
         if (ref.getMessage().getMessageID() == messageID)
         {
            deliveringCount.incrementAndGet();
            ref.expire(storageManager, postOffice, queueSettingsRepository);
            iter.remove();
            return true;
         }
      }
      return false;
   }

   public boolean sendMessageToDLQ(final long messageID,
         final StorageManager storageManager, final PostOffice postOffice,
         final HierarchicalRepository<QueueSettings> queueSettingsRepository)
         throws Exception
   {
      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();
         if (ref.getMessage().getMessageID() == messageID)
         {
            deliveringCount.incrementAndGet();
            ref.sendToDLQ(storageManager, postOffice, queueSettingsRepository);
            iter.remove();
            return true;
         }
      }
      return false;
   }

   public boolean moveMessage(final long messageID,
         final Binding toBinding, final StorageManager storageManager,
         final PostOffice postOffice) throws Exception
   {
      Iterator<MessageReference> iter = messageReferences.iterator();

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();
         if (ref.getMessage().getMessageID() == messageID)
         {
            deliveringCount.incrementAndGet();
            ref.move(toBinding, storageManager, postOffice);
            iter.remove();
            return true;
         }
      }
      return false;
   }
   
   public boolean changeMessagePriority(final long messageID, final byte newPriority,
         final StorageManager storageManager, final PostOffice postOffice,
         final HierarchicalRepository<QueueSettings> queueSettingsRepository)
         throws Exception
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
            deleteReference(messageID, storageManager);
            addLast(ref);
            return true;
         }
      }
      return false;
   }
   
   public void lock()
   {
      lock.lock();
   }
   
   public void unlock()
   {            
      lock.unlock();     
   }

   public boolean isBackup()
   {
      return backup;
   }
   
   public void setBackup(final boolean backup)
   {
      this.backup = backup;
      
      if (!backup)
      {
         for (ScheduledDeliveryRunnable runnable: scheduledRunnables)
         {
            scheduleDelivery(runnable, runnable.getReference().getScheduledDeliveryTime());
         }
      }
      
      //TODO - what about waiting for the deliverAsync executor to finish?
   }
   
   public MessageReference removeFirst()
   {
      return messageReferences.removeFirst();
   }
   
   // Public
   // -----------------------------------------------------------------------------

   public boolean equals(Object other)
   {
      if (this == other)
      {
         return true;
      }

      QueueImpl qother = (QueueImpl) other;

      return name.equals(qother.name);
   }

   public int hashCode()
   {
      return name.hashCode();
   }

   // Private
   // ------------------------------------------------------------------------------

   private synchronized HandleStatus add(final MessageReference ref, final boolean first)
   {
      if (!first)
      {
         messagesAdded.incrementAndGet();
         
         sizeBytes.addAndGet(ref.getMessage().getEncodeSize());
      }
      
      if (checkAndSchedule(ref))
      {
         return HandleStatus.HANDLED;         
      }

      boolean add = false;

      if (direct && !backup)
      {
         // Deliver directly

         HandleStatus status = deliver(ref);

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

      return HandleStatus.HANDLED;
   }

   private boolean checkAndSchedule(final MessageReference ref)
   {
      long deliveryTime = ref.getScheduledDeliveryTime();
      
      if (deliveryTime != 0 && scheduledExecutor != null)
      {                     
         if (trace)
         {
            log.trace("Scheduling delivery for " + ref + " to occur at "  + deliveryTime);
         }
         
         ScheduledDeliveryRunnable runnable = new ScheduledDeliveryRunnable(ref);

         scheduledRunnables.add(runnable);
         
         if (!backup)
         {            
            scheduleDelivery(runnable, deliveryTime);
         }

         return true;         
      }
      return false;      
   }
   
   private void scheduleDelivery(final ScheduledDeliveryRunnable runnable, final long deliveryTime)
   {
      long now = System.currentTimeMillis();
      
      long delay = deliveryTime - now;

      Future<?> future = scheduledExecutor.schedule(runnable, delay, TimeUnit.MILLISECONDS);

      runnable.setFuture(future);
   }

   private HandleStatus deliver(final MessageReference reference)
   {
      if (consumers.isEmpty())
      {
         return HandleStatus.BUSY;
      }

      int startPos = pos;

      boolean filterRejected = false;

      while (true)
      {
         Consumer consumer = consumers.get(pos);
         
         pos = distributionPolicy.select(consumers, pos);

         HandleStatus status;

         try
         {
            status = consumer.handle(reference);
         }
         catch (Throwable t)
         {
            log.warn("removing consumer which did not handle a message, " +
                  "consumer=" + consumer + ", message=" + reference, t);

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

         if (status == HandleStatus.HANDLED)
         {
            deliveringCount.incrementAndGet();

            return HandleStatus.HANDLED;
         }
         else if (status == HandleStatus.NO_MATCH)
         {
            promptDelivery = true;

            filterRejected = true;
         }

         if (pos == startPos)
         {
            // Tried all of them
            if (filterRejected)
            {
               return HandleStatus.NO_MATCH;
            }
            else
            {
               // Give up - all consumers busy
               return HandleStatus.BUSY;
            }
         }
      }
   }

   // Inner classes
   // --------------------------------------------------------------------------

   private class DeliverRunner implements Runnable
   {
      public void run()
      {
         //Must be set to false *before* executing to avoid race
         waitingToDeliver.set(false);
         
         deliver();
      }
   }   
   
   private class ScheduledDeliveryRunnable implements Runnable
   {
      private final MessageReference ref;

      private volatile Future<?> future;

      private boolean cancelled;

      public ScheduledDeliveryRunnable(final MessageReference ref)
      {
         this.ref = ref;
      }

      public synchronized void setFuture(final Future<?> future)
      {
         if (cancelled)
         {
            future.cancel(false);
         }
         else
         {
            this.future = future;
         }
      }

      public synchronized void cancel()
      {
         if (future != null)
         {
            future.cancel(false);
         }

         cancelled = true;
      }

      public MessageReference getReference()
      {
         return ref;
      }

      public void run()
      {
         if (trace)
         {
            log.trace("Scheduled delivery timeout " + ref);
         }

         synchronized (scheduledRunnables)
         {
            boolean removed = scheduledRunnables.remove(this);

            if (!removed)
            {
               log.warn("Failed to remove timeout " + this);

               return;
            }
         }

         ref.setScheduledDeliveryTime(0);

         HandleStatus status = deliver(ref);

         if (HandleStatus.HANDLED != status)
         {
            // Add back to the front of the queue

            //TODO - need to replicate this so backup node also adds back to front of queue
            
            addFirst(ref);
         }
      }
   }
}
