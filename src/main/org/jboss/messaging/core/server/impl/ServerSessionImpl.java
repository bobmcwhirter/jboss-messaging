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

package org.jboss.messaging.core.server.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.messaging.core.exception.MessagingException;
import org.jboss.messaging.core.filter.Filter;
import org.jboss.messaging.core.filter.impl.FilterImpl;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.paging.PagingManager;
import org.jboss.messaging.core.persistence.StorageManager;
import org.jboss.messaging.core.postoffice.Binding;
import org.jboss.messaging.core.postoffice.FlowController;
import org.jboss.messaging.core.postoffice.PostOffice;
import org.jboss.messaging.core.remoting.Channel;
import org.jboss.messaging.core.remoting.FailureListener;
import org.jboss.messaging.core.remoting.Packet;
import org.jboss.messaging.core.remoting.RemotingConnection;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionBindingQueryResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionCreateConsumerResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionCreateProducerResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionQueueQueryResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionReplicateDeliveryMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionReplicateDeliveryResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXAResponseMessage;
import org.jboss.messaging.core.security.CheckType;
import org.jboss.messaging.core.security.SecurityStore;
import org.jboss.messaging.core.server.Delivery;
import org.jboss.messaging.core.server.MessageReference;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.core.server.Queue;
import org.jboss.messaging.core.server.ServerConsumer;
import org.jboss.messaging.core.server.ServerMessage;
import org.jboss.messaging.core.server.ServerProducer;
import org.jboss.messaging.core.server.ServerSession;
import org.jboss.messaging.core.settings.HierarchicalRepository;
import org.jboss.messaging.core.settings.impl.QueueSettings;
import org.jboss.messaging.core.transaction.ResourceManager;
import org.jboss.messaging.core.transaction.Transaction;
import org.jboss.messaging.core.transaction.impl.TransactionImpl;
import org.jboss.messaging.util.IDGenerator;
import org.jboss.messaging.util.SimpleString;

/**
 * Session implementation
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 */

public class ServerSessionImpl implements ServerSession, FailureListener
{
   // Constants
   // ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(ServerSessionImpl.class);

   // Static
   // ---------------------------------------------------------------------------------------

   // Attributes
   // -----------------------------------------------------------------------------------

   private final boolean trace = log.isTraceEnabled();

   private final String name;
   
   private final long id;

   private final String username;

   private final String password;

   private final boolean autoCommitSends;

   private final boolean autoCommitAcks;

   private volatile RemotingConnection remotingConnection;

   private final Map<Long, ServerConsumer> consumers = new ConcurrentHashMap<Long, ServerConsumer>();

   private final Map<Long, ServerBrowserImpl> browsers = new ConcurrentHashMap<Long, ServerBrowserImpl>();

   private final Map<Long, ServerProducer> producers = new ConcurrentHashMap<Long, ServerProducer>();

   private final java.util.Queue<Delivery> deliveries = new ConcurrentLinkedQueue<Delivery>();

   private final AtomicLong deliveryIDSequence = new AtomicLong(0);

   private final Executor executor;

   private Transaction tx;

   private final StorageManager storageManager;

   private final HierarchicalRepository<QueueSettings> queueSettingsRepository;

   private final ResourceManager resourceManager;

   private final PostOffice postOffice;

   private final PagingManager pager;

   private final SecurityStore securityStore;

   private final Channel channel;
   
   private final Channel replicatingChannel;
   
   private final java.util.Queue<Delivery> deferredDeliveries = new ConcurrentLinkedQueue<Delivery>();
   
   private final MessagingServer server;
   
   private volatile boolean started = false;

   private final List<Runnable> failureRunners = new ArrayList<Runnable>();
   
   private final IDGenerator idGenerator = new IDGenerator(0);
   
   // Constructors
   // ---------------------------------------------------------------------------------

   public ServerSessionImpl(
            final String name,
            final long id,
            final String username,
            final String password,
            final boolean autoCommitSends,
            final boolean autoCommitAcks,
            final boolean xa,
            final RemotingConnection remotingConnection,
            final StorageManager storageManager,
            final PostOffice postOffice,
            final HierarchicalRepository<QueueSettings> queueSettingsRepository,
            final ResourceManager resourceManager,
            final SecurityStore securityStore,
            final Executor executor,
            final Channel channel,
            final MessagingServer server) throws Exception
   {
      this.name = name;
      
      this.id = id;

      this.username = username;

      this.password = password;

      this.autoCommitSends = autoCommitSends;

      this.autoCommitAcks = autoCommitAcks;

      this.remotingConnection = remotingConnection;

      this.storageManager = storageManager;

      this.postOffice = postOffice;

      this.pager = postOffice.getPagingManager();

      this.queueSettingsRepository = queueSettingsRepository;

      this.resourceManager = resourceManager;

      this.securityStore = securityStore;

      this.executor = executor;

      if (!xa)
      {
         tx = new TransactionImpl(storageManager, postOffice);
      }

      this.channel = channel;
      
      this.replicatingChannel = channel.getReplicatingChannel();
      
      this.server = server;
   }

   // ServerSession implementation
   // ---------------------------------------------------------------------------------------

   public String getUsername()
   {
      return username;
   }

   public String getPassword()
   {
      return password;
   }

   public long getID()
   {
      return id;
   }

   public void removeBrowser(final ServerBrowserImpl browser) throws Exception
   {
      if (browsers.remove(browser.getID()) == null)
      {
         throw new IllegalStateException("Cannot find browser with id " + browser.getID() + " to remove");
      }
   }

   public void removeConsumer(final ServerConsumer consumer) throws Exception
   {
      if (consumers.remove(consumer.getID()) == null)
      {
         throw new IllegalStateException("Cannot find consumer with id " + consumer.getID()
                        + " to remove");
      }
   }

   public void removeProducer(final ServerProducer producer) throws Exception
   {
      if (producers.remove(producer.getID()) == null)
      {
         throw new IllegalStateException("Cannot find producer with id " + producer.getID()
                        + " to remove");
      }
   }

   public void handleDelivery(final MessageReference ref, final ServerConsumer consumer)
   {
      Delivery delivery;

      long nextID = deliveryIDSequence.getAndIncrement();

      delivery = new DeliveryImpl(ref, consumer.getID(), nextID, channel);

      deliveries.add(delivery);
      
      if (replicatingChannel != null)
      {
         deferredDeliveries.add(delivery);
         
         Packet msg = new SessionReplicateDeliveryMessage(consumer.getID(), ref.getMessage().getMessageID());
       
         replicatingChannel.send(msg);
      }
      else
      {      
         delivery.deliver();
      }
   }
   
   public void deliverDeferredDelivery(final long messageID)
   {
      for (Delivery del : deliveries)
      {
         long id = del.getReference().getMessage().getMessageID();

         if (id == messageID)
         {
            del.deliver();

            break;
         }
      }
   }

   public void setStarted(final boolean s) throws Exception
   {
      Set<ServerConsumer> consumersClone = new HashSet<ServerConsumer>(consumers.values());

      for (ServerConsumer consumer : consumersClone)
      {
         consumer.setStarted(s);
      }

      started = s;
   }

   public void close() throws Exception
   {
      Set<ServerConsumer> consumersClone = new HashSet<ServerConsumer>(consumers.values());

      for (ServerConsumer consumer : consumersClone)
      {
         consumer.close();
      }

      consumers.clear();

      Set<ServerBrowserImpl> browsersClone = new HashSet<ServerBrowserImpl>(browsers.values());

      for (ServerBrowserImpl browser : browsersClone)
      {
         browser.close();
      }

      browsers.clear();

      Set<ServerProducer> producersClone = new HashSet<ServerProducer>(producers.values());

      for (ServerProducer producer : producersClone)
      {
         producer.close();
      }

      producers.clear();

      rollback();

      deliveries.clear();

      channel.close();
      
      server.removeSession(name);
   }

   public void promptDelivery(final Queue queue)
   {
      queue.deliverAsync(executor);
   }

   public void send(final ServerMessage msg) throws Exception
   {
      // check the user has write access to this address.
      try
      {
         securityStore.check(msg.getDestination(), CheckType.WRITE, this);
      }
      catch (MessagingException e)
      {
         if (!autoCommitSends)
         {
            tx.markAsRollbackOnly(e);
         }
         throw e;
      }

      if (autoCommitSends)
      {
         if (!pager.page(msg))
         {
            // We only set the messageID after we are sure the message is not being paged
            // Paged messages won't have an ID until they are depaged
            msg.setMessageID(storageManager.generateID());

            List<MessageReference> refs = postOffice.route(msg);

            if (msg.getDurableRefCount() != 0)
            {
               storageManager.storeMessage(msg);
            }

            for (MessageReference ref : refs)
            {
               ref.getQueue().addLast(ref);
            }
         }
      }
      else
      {
         tx.addMessage(msg);
      }
   }

   public void acknowledge(final long deliveryID, final boolean allUpTo)
            throws Exception
   {
      /*
      Note that we do not consider it an error if the deliveries cannot be found to be acked.
      This can legitimately occur if a connection/session/consumer is closed
      from inside a MessageHandlers onMessage method. In this situation the close will cancel any unacked
      deliveries, but the subsequent call to delivered() will try and ack again and not find the last
      delivery on the server.
      */
      if (allUpTo)
      {
         // Ack all deliveries up to and including the specified id

         for (Iterator<Delivery> iter = deliveries.iterator(); iter.hasNext();)
         {
            Delivery rec = iter.next();

            if (rec.getDeliveryID() <= deliveryID)
            {
               iter.remove();

               MessageReference ref = rec.getReference();

               if (rec.getDeliveryID() > deliveryID)
               {
                  // This catches the case where the delivery has been cancelled
                  // since it's expired
                  // And we don't want to end up acking all deliveries!
                  break;
               }

               if (autoCommitAcks)
               {
                  doAck(ref);
               }
               else
               {
                  tx.addAcknowledgement(ref);

                  // Del count is not actually updated in storage unless it's
                  // cancelled
                  ref.incrementDeliveryCount();
               }

               if (rec.getDeliveryID() == deliveryID)
               {
                  break;
               }
            }
            else
            {
               // Sanity check
               throw new IllegalStateException("Failed to ack contiguently");
            }
         }
      }
      else
      {
         // Ack a specific delivery

         for (Iterator<Delivery> iter = deliveries.iterator(); iter.hasNext();)
         {
            Delivery rec = iter.next();

            if (rec.getDeliveryID() == deliveryID)
            {
               iter.remove();

               MessageReference ref = rec.getReference();

               if (autoCommitAcks)
               {
                  doAck(ref);
               }
               else
               {
                  tx.addAcknowledgement(ref);

                  // Del count is not actually updated in storage unless it's
                  // cancelled
                  ref.incrementDeliveryCount();
               }

               break;
            }
         }
      }
   }

   public void rollback() throws Exception
   {
      if (tx == null)
      {
         // Might be null if XA

         tx = new TransactionImpl(storageManager, postOffice);
      }

      // We need to lock all the queues while we're rolling back, to prevent any
      // deliveries occurring during this
      // period

      List<Queue> locked = new ArrayList<Queue>();

      for (ServerConsumer consumer : consumers.values())
      {
         consumer.getQueue().lock();

         locked.add(consumer.getQueue());
      }

      try
      {

         // Add any unacked deliveries into the tx. Doing this ensures all
         // references are rolled back in the correct
         // order in a single contiguous block

         for (Delivery del : deliveries)
         {
            tx.addAcknowledgement(del.getReference());
         }

         deliveries.clear();

         deliveryIDSequence.addAndGet(-tx.getAcknowledgementsCount());

         tx.rollback(queueSettingsRepository);
      }
      finally
      {
         // Now unlock

         for (Queue queue : locked)
         {
            queue.unlock();
         }
      }

      tx = new TransactionImpl(storageManager, postOffice);
   }

   public void cancel(final long deliveryID, final boolean expired)
            throws Exception
   {
      if (deliveryID == -1)
      {
         // Cancel all

         // We need to lock all the queues while we're rolling back, to prevent
         // any deliveries occurring during this
         // period

         List<Queue> locked = new ArrayList<Queue>();

         for (ServerConsumer consumer : consumers.values())
         {
            consumer.getQueue().lock();

            locked.add(consumer.getQueue());
         }

         try
         {
            Transaction cancelTx = new TransactionImpl(storageManager,
                     postOffice);

            for (Delivery del : deliveries)
            {
               cancelTx.addAcknowledgement(del.getReference());
            }

            deliveries.clear();

            cancelTx.rollback(queueSettingsRepository);
         }
         finally
         {
         }
         //finally (TODO: enable this back)
         {
            //Now unlock

            for (Queue queue: locked)
            {
               queue.unlock();
            }
         }
      }
      else if (expired)
      {
         if (deliveryID == -1) { throw new IllegalArgumentException(
                  "Invalid delivery id"); }

         // Expire a single reference

         for (Iterator<Delivery> iter = deliveries.iterator(); iter.hasNext();)
         {
            Delivery delivery = iter.next();

            if (delivery.getDeliveryID() == deliveryID)
            {
               delivery.getReference().expire(storageManager, postOffice,
                        queueSettingsRepository);

               iter.remove();

               break;
            }
         }
      }
      else
      {
         throw new IllegalArgumentException("Invalid delivery id " + deliveryID);
      }
   }

   public void commit() throws Exception
   {
      try
      {
         tx.commit();
      }
      finally
      {
         tx = new TransactionImpl(storageManager, postOffice);
      }
   }

   public SessionXAResponseMessage XACommit(final boolean onePhase,
            final Xid xid) throws Exception
   {
      if (tx != null)
      {
         final String msg = "Cannot commit, session is currently doing work in transaction "
                  + tx.getXid();

         return new SessionXAResponseMessage(true, XAException.XAER_PROTO, msg);
      }

      Transaction theTx = resourceManager.getTransaction(xid);

      if (theTx == null)
      {
         final String msg = "Cannot find xid in resource manager: " + xid;

         return new SessionXAResponseMessage(true, XAException.XAER_NOTA, msg);
      }

      if (theTx.getState() == Transaction.State.SUSPENDED) { return new SessionXAResponseMessage(
               true, XAException.XAER_PROTO,
               "Cannot commit transaction, it is suspended " + xid); }

      theTx.commit();

      boolean removed = resourceManager.removeTransaction(xid);

      if (!removed)
      {
         final String msg = "Failed to remove transaction: " + xid;

         return new SessionXAResponseMessage(true, XAException.XAER_PROTO, msg);
      }

      return new SessionXAResponseMessage(false, XAResource.XA_OK, null);
   }

   public SessionXAResponseMessage XAEnd(final Xid xid, final boolean failed)
            throws Exception
   {
      if (tx != null && tx.getXid().equals(xid))
      {
         if (tx.getState() == Transaction.State.SUSPENDED)
         {
            final String msg = "Cannot end, transaction is suspended";

            return new SessionXAResponseMessage(true, XAException.XAER_PROTO,
                     msg);
         }

         tx = null;
      }
      else
      {
         // It's also legal for the TM to call end for a Xid in the suspended
         // state
         // See JTA 1.1 spec 3.4.4 - state diagram
         // Although in practice TMs rarely do this.
         Transaction theTx = resourceManager.getTransaction(xid);

         if (theTx == null)
         {
            final String msg = "Cannot find suspended transaction to end "
                     + xid;

            return new SessionXAResponseMessage(true, XAException.XAER_NOTA,
                     msg);
         }

         if (theTx.getState() != Transaction.State.SUSPENDED)
         {
            final String msg = "Transaction is not suspended " + xid;

            return new SessionXAResponseMessage(true, XAException.XAER_PROTO,
                     msg);
         }

         theTx.resume();
      }

      return new SessionXAResponseMessage(false, XAResource.XA_OK, null);
   }

   public SessionXAResponseMessage XAForget(final Xid xid)
   {
      // Do nothing since we don't support heuristic commits / rollback from the
      // resource manager

      return new SessionXAResponseMessage(false, XAResource.XA_OK, null);
   }

   public SessionXAResponseMessage XAJoin(final Xid xid) throws Exception
   {
      Transaction theTx = resourceManager.getTransaction(xid);

      if (theTx == null)
      {
         final String msg = "Cannot find xid in resource manager: " + xid;

         return new SessionXAResponseMessage(true, XAException.XAER_NOTA, msg);
      }

      if (theTx.getState() == Transaction.State.SUSPENDED) { return new SessionXAResponseMessage(
               true, XAException.XAER_PROTO, "Cannot join tx, it is suspended "
                        + xid); }

      tx = theTx;

      return new SessionXAResponseMessage(false, XAResource.XA_OK, null);
   }

   public SessionXAResponseMessage XAPrepare(final Xid xid) throws Exception
   {
      if (tx != null)
      {
         final String msg = "Cannot commit, session is currently doing work in a transaction "
                  + tx.getXid();

         return new SessionXAResponseMessage(true, XAException.XAER_PROTO, msg);
      }

      Transaction theTx = resourceManager.getTransaction(xid);

      if (theTx == null)
      {
         final String msg = "Cannot find xid in resource manager: " + xid;

         return new SessionXAResponseMessage(true, XAException.XAER_NOTA, msg);
      }

      if (theTx.getState() == Transaction.State.SUSPENDED) { return new SessionXAResponseMessage(
               true, XAException.XAER_PROTO,
               "Cannot prepare transaction, it is suspended " + xid); }

      if (theTx.isEmpty())
      {
         // Nothing to do - remove it

         boolean removed = resourceManager.removeTransaction(xid);

         if (!removed)
         {
            final String msg = "Failed to remove transaction: " + xid;

            return new SessionXAResponseMessage(true, XAException.XAER_PROTO,
                     msg);
         }

         return new SessionXAResponseMessage(false, XAResource.XA_RDONLY, null);
      }
      else
      {
         theTx.prepare();

         return new SessionXAResponseMessage(false, XAResource.XA_OK, null);
      }
   }

   public SessionXAResponseMessage XAResume(final Xid xid) throws Exception
   {
      if (tx != null)
      {
         final String msg = "Cannot resume, session is currently doing work in a transaction "
                  + tx.getXid();

         return new SessionXAResponseMessage(true, XAException.XAER_PROTO, msg);
      }

      Transaction theTx = resourceManager.getTransaction(xid);

      if (theTx == null)
      {
         final String msg = "Cannot find xid in resource manager: " + xid;

         return new SessionXAResponseMessage(true, XAException.XAER_NOTA, msg);
      }

      if (theTx.getState() != Transaction.State.SUSPENDED) { return new SessionXAResponseMessage(
               true, XAException.XAER_PROTO,
               "Cannot resume transaction, it is not suspended " + xid); }

      tx = theTx;

      tx.resume();

      return new SessionXAResponseMessage(false, XAResource.XA_OK, null);
   }

   public SessionXAResponseMessage XARollback(final Xid xid) throws Exception
   {
      if (tx != null)
      {
         final String msg = "Cannot roll back, session is currently doing work in a transaction "
                  + tx.getXid();

         return new SessionXAResponseMessage(true, XAException.XAER_PROTO, msg);
      }

      Transaction theTx = resourceManager.getTransaction(xid);

      if (theTx == null)
      {
         final String msg = "Cannot find xid in resource manager: " + xid;

         return new SessionXAResponseMessage(true, XAException.XAER_NOTA, msg);
      }

      if (theTx.getState() == Transaction.State.SUSPENDED) { return new SessionXAResponseMessage(
               true, XAException.XAER_PROTO,
               "Cannot rollback transaction, it is suspended " + xid); }

      theTx.rollback(queueSettingsRepository);

      boolean removed = resourceManager.removeTransaction(xid);

      if (!removed)
      {
         final String msg = "Failed to remove transaction: " + xid;

         return new SessionXAResponseMessage(true, XAException.XAER_PROTO, msg);
      }

      return new SessionXAResponseMessage(false, XAResource.XA_OK, null);
   }

   public SessionXAResponseMessage XAStart(final Xid xid)
   {
      if (tx != null)
      {
         final String msg = "Cannot start, session is already doing work in a transaction "
                  + tx.getXid();

         return new SessionXAResponseMessage(true, XAException.XAER_PROTO, msg);
      }

      tx = new TransactionImpl(xid, storageManager, postOffice);

      boolean added = resourceManager.putTransaction(xid, tx);

      if (!added)
      {
         final String msg = "Cannot start, there is already a xid "
                  + tx.getXid();

         return new SessionXAResponseMessage(true, XAException.XAER_DUPID, msg);
      }

      return new SessionXAResponseMessage(false, XAResource.XA_OK, null);
   }

   public SessionXAResponseMessage XASuspend() throws Exception
   {
      if (tx == null)
      {
         final String msg = "Cannot suspend, session is not doing work in a transaction ";

         return new SessionXAResponseMessage(true, XAException.XAER_PROTO, msg);
      }

      if (tx.getState() == Transaction.State.SUSPENDED)
      {
         final String msg = "Cannot suspend, transaction is already suspended "
                  + tx.getXid();

         return new SessionXAResponseMessage(true, XAException.XAER_PROTO, msg);
      }

      tx.suspend();

      tx = null;

      return new SessionXAResponseMessage(false, XAResource.XA_OK, null);
   }

   public List<Xid> getInDoubtXids() throws Exception
   {
      return resourceManager.getPreparedTransactions();
   }

   public int getXATimeout()
   {
      return resourceManager.getTimeoutSeconds();
   }

   public boolean setXATimeout(int timeoutSeconds)
   {
      return resourceManager.setTimeoutSeconds(timeoutSeconds);
   }

   public void addDestination(final SimpleString address,
            final boolean durable, final boolean temporary) throws Exception
   {
      securityStore.check(address, CheckType.CREATE, this);

      if (!postOffice.addDestination(address, durable)) { throw new MessagingException(
               MessagingException.ADDRESS_EXISTS, "Address already exists: "
                        + address); }

      if (temporary)
      {
         // Temporary address in core simply means the address will be deleted
         // if the remoting connection
         // dies. It does not mean it will get deleted automatically when the
         // session is closed.
         // It is up to the user to delete the address when finished with it

        failureRunners.add(
              new Runnable()
              {
                 public void run()
                 {
                    try
                    {
                       postOffice.removeDestination(address, durable);
                    }
                    catch (Exception e)
                    {
                       log.error("Failed to remove temporary address " + address);
                    }
                 }
              }
         );
      }
   }
   
   public void removeDestination(final SimpleString address,
            final boolean durable) throws Exception
   {
      securityStore.check(address, CheckType.CREATE, this);

      if (!postOffice.removeDestination(address, durable))
      {
         throw new MessagingException(MessagingException.ADDRESS_DOES_NOT_EXIST,
               "Address does not exist: " + address);
      }
   }

   public void createQueue(final SimpleString address,
            final SimpleString queueName, final SimpleString filterString,
            final boolean durable, final boolean temporary) throws Exception
   {
      // make sure the user has privileges to create this queue
      if (!postOffice.containsDestination(address))
      {
         securityStore.check(address, CheckType.CREATE, this);
      }

      Binding binding = postOffice.getBinding(queueName);

      if (binding != null) { throw new MessagingException(
               MessagingException.QUEUE_EXISTS); }

      Filter filter = null;

      if (filterString != null)
      {
         filter = new FilterImpl(filterString);
      }

      binding = postOffice.addBinding(address, queueName, filter, durable);

      if (temporary)
      {
         // Temporary queue in core simply means the queue will be deleted if
         // the remoting connection
         // dies. It does not mean it will get deleted automatically when the
         // session is closed.
         // It is up to the user to delete the queue when finished with it

         final Queue queue = binding.getQueue();
         
         failureRunners.add(
                  new Runnable()
                  {
                     public void run()
                     {
                        try
                        {
                           postOffice.removeBinding(queue.getName());
                        }
                        catch (Exception e)
                        {
                           log.error("Failed to remove temporary queue "
                                    + queue.getName());
                        }
                     }
                  }
             );        
      }
   }

   public void deleteQueue(final SimpleString queueName) throws Exception
   {
      Binding binding = postOffice.removeBinding(queueName);

      if (binding == null)
      {
         throw new MessagingException(MessagingException.QUEUE_DOES_NOT_EXIST);
      }

      Queue queue = binding.getQueue();

      if (queue.getConsumerCount() != 0) { throw new MessagingException(
               MessagingException.ILLEGAL_STATE,
               "Cannot delete queue - it has consumers"); }

      if (queue.isDurable())
      {
         binding.getQueue().deleteAllReferences(storageManager);
      }
   }

   public SessionCreateConsumerResponseMessage createConsumer(
            final SimpleString queueName,
            final SimpleString filterString, int windowSize, int maxRate)
            throws Exception
   {
      Binding binding = postOffice.getBinding(queueName);

      if (binding == null)
      {
         throw new MessagingException(MessagingException.QUEUE_DOES_NOT_EXIST);
      }

      securityStore.check(binding.getAddress(), CheckType.READ, this);

      Filter filter = null;

      if (filterString != null)
      {
         filter = new FilterImpl(filterString);
      }

      // Flow control values if specified on queue override those passed in from
      // client

      QueueSettings qs = queueSettingsRepository.getMatch(queueName.toString());

      Integer queueWindowSize = (Integer) qs.getConsumerWindowSize();

      windowSize = queueWindowSize != null ? queueWindowSize : windowSize;

      Integer queueMaxRate = queueSettingsRepository.getMatch(
               queueName.toString()).getConsumerMaxRate();

      maxRate = queueMaxRate != null ? queueMaxRate : maxRate;
      
      ServerConsumer consumer = new ServerConsumerImpl(idGenerator.generateID(), this,
               binding.getQueue(), filter, windowSize != -1, maxRate, started,
               storageManager, queueSettingsRepository, postOffice);

      SessionCreateConsumerResponseMessage response = new SessionCreateConsumerResponseMessage(windowSize);

      consumers.put(consumer.getID(), consumer);

      return response;
   }

   public SessionQueueQueryResponseMessage executeQueueQuery(final SimpleString queueName) throws Exception
   {
      if (queueName == null)
      {
         throw new IllegalArgumentException("Queue name is null");
      }

      Binding binding = postOffice.getBinding(queueName);

      SessionQueueQueryResponseMessage response;

      if (binding != null)
      {
         Queue queue = binding.getQueue();

         Filter filter = queue.getFilter();

         SimpleString filterString = filter == null ? null : filter
                  .getFilterString();
         // TODO: Remove MAX-SIZE-BYTES from SessionQueueQueryResponse.
         response = new SessionQueueQueryResponseMessage(queue.isDurable(), 
                 queue.getConsumerCount(), queue.getMessageCount(),
                 filterString, binding.getAddress());
      }
      else
      {
         response = new SessionQueueQueryResponseMessage();
      }

      return response;
   }

   public SessionBindingQueryResponseMessage executeBindingQuery(
            final SimpleString address) throws Exception
   {
      if (address == null) { throw new IllegalArgumentException(
               "Address is null"); }

      boolean exists = postOffice.containsDestination(address);

      List<SimpleString> queueNames = new ArrayList<SimpleString>();

      if (exists)
      {
         List<Binding> bindings = postOffice.getBindingsForAddress(address);

         for (Binding binding : bindings)
         {
            queueNames.add(binding.getQueue().getName());
         }
      }

      return new SessionBindingQueryResponseMessage(exists, queueNames);
   }

   public void createBrowser(final SimpleString queueName, final SimpleString filterString)
            throws Exception
   {
      Binding binding = postOffice.getBinding(queueName);

      if (binding == null)
      {
         throw new MessagingException(MessagingException.QUEUE_DOES_NOT_EXIST);
      }

      securityStore.check(binding.getAddress(), CheckType.READ, this);

      ServerBrowserImpl browser = new ServerBrowserImpl(idGenerator.generateID(), this, binding
               .getQueue(), filterString == null ? null : filterString
               .toString());

      browsers.put(browser.getID(), browser);
   }

   /**
    * Create a producer for the specified address
    *
    * @param address    The address to produce too
    * @param windowSize - the producer window size to use for flow control.
    *                   Specify -1 to disable flow control completely
    *                   The actual window size used may be less than the specified window size if
    *                   it is overridden by any producer-window-size specified on the queue
    */
   public SessionCreateProducerResponseMessage createProducer(
            final SimpleString address,
            final int windowSize, final int maxRate) throws Exception
   {
      FlowController flowController = null;

      final int maxRateToUse = maxRate;

      if (address != null)
      {
         flowController = windowSize == -1 ? null : postOffice
                  .getFlowController(address);
      }

      final int windowToUse = flowController == null ? -1 : windowSize;

      // Server window size is 0.75 client window size for producer flow control
      // (other way round to consumer flow control)

      final int serverWindowSize = windowToUse == -1 ? -1
               : (int) (windowToUse * 0.75);

      ServerProducerImpl producer = new ServerProducerImpl(idGenerator.generateID(), this,
               address, flowController, serverWindowSize,
               channel);

      producers.put(producer.getID(), producer);

      // Get some initial credits to send to the producer - we try for
      // windowToUse

      int initialCredits = flowController == null ? -1 : flowController
               .getInitialCredits(windowToUse, producer);

      return new SessionCreateProducerResponseMessage(initialCredits, maxRateToUse);
   }

   public boolean browserHasNextMessage(final long browserID) throws Exception
   {
      return browsers.get(browserID).hasNextMessage();
   }

   public ServerMessage browserNextMessage(final long browserID) throws Exception
   {
      return browsers.get(browserID).nextMessage();
   }

   public void browserReset(final long browserID) throws Exception
   {
      browsers.get(browserID).reset();
   }

   public void closeBrowser(final long browserID) throws Exception
   {
      browsers.get(browserID).close();
   }

   public void closeConsumer(final long consumerID) throws Exception
   {
      consumers.get(consumerID).close();
   }

   public void closeProducer(final long producerID) throws Exception
   {
      producers.get(producerID).close();
   }

   public void receiveConsumerCredits(final long consumerID, final int credits) throws Exception
   {
      consumers.get(consumerID).receiveCredits(credits);
   }

   public void sendProducerMessage(final long producerID, final ServerMessage message) throws Exception
   {
      producers.get(producerID).send(message);
   }
   
   public void handleReplicateDelivery(final long consumerID, final long messageID) throws Exception
   {
      consumers.get(consumerID).deliverMessage(messageID);
      
      Packet response = new SessionReplicateDeliveryResponseMessage();
      
      channel.send(response);
   }
      
   public void handleDeferredDelivery()
   {  
      Delivery delivery = deferredDeliveries.poll();
      
      if (delivery == null)
      {
         throw new IllegalStateException("Cannot find deferred delivery to deliver");
      }
      
      delivery.deliver();
   }
   
   public void transferConnection(final RemotingConnection newConnection)
   {           
      remotingConnection.removeFailureListener(this);
      
      channel.transferConnection(newConnection);            
                        
      //Destroy the old connection
      remotingConnection.destroy();
      
      remotingConnection = newConnection;
      
      remotingConnection.addFailureListener(this);      
   }
   
   public int replayCommands(final int lastReceivedCommandID)
   {    
      return channel.replayCommands(lastReceivedCommandID);
   }
   
   // FailureListener implementation
   // --------------------------------------------------------------------

   public void connectionFailed(MessagingException me)
   {
      try
      {
         for (Runnable runner: failureRunners)
         {
            try
            {
               runner.run();
            }
            catch (Throwable t)
            {
               log.error("Failed to execute failure runner", t);
            }
         }
         
         close();
      }
      catch (Throwable t)
      {
         log.error("Failed to close connection " + this);
      }
   }

   // Public
   // ---------------------------------------------------------------------------------------------

   public Transaction getTransaction()
   {
      return tx;
   }

   public java.util.Queue<Delivery> getDeliveries()
   {
      return deliveries;
   }

   // Private
   // --------------------------------------------------------------------------------------------

   private void doAck(final MessageReference ref) throws Exception
   {
      ServerMessage message = ref.getMessage();

      Queue queue = ref.getQueue();

      if (message.decrementRefCount() == 0)
      {
         pager.messageDone(message);
      }

      if (message.isDurable() && queue.isDurable())
      {
         int count = message.decrementDurableRefCount();

         if (count == 0)
         {
            storageManager.storeDelete(message.getMessageID());
         }
         else
         {
            storageManager.storeAcknowledge(queue.getPersistenceID(), message
                     .getMessageID());
         }
      }

      queue.referenceAcknowledged(ref);
   }

}
