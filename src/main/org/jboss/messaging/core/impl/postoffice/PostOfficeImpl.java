/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.messaging.core.impl.postoffice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.messaging.core.Binding;
import org.jboss.messaging.core.Filter;
import org.jboss.messaging.core.Message;
import org.jboss.messaging.core.MessageReference;
import org.jboss.messaging.core.MessagingException;
import org.jboss.messaging.core.PersistenceManager;
import org.jboss.messaging.core.PostOffice;
import org.jboss.messaging.core.Queue;
import org.jboss.messaging.core.QueueFactory;
import org.jboss.messaging.core.impl.BindingImpl;
import org.jboss.messaging.util.ConcurrentHashSet;
import org.jboss.messaging.util.Logger;

/**
 * 
 * A PostOfficeImpl
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class PostOfficeImpl implements PostOffice
{  
   private static final Logger log = Logger.getLogger(PostOfficeImpl.class);
   
   private int nodeID;
   
   private ConcurrentMap<String, List<Binding>> mappings = new ConcurrentHashMap<String, List<Binding>>();
   
   private Set<String> allowableAddresses = new ConcurrentHashSet<String>();
   
   private ConcurrentMap<String, Binding> nameMap = new ConcurrentHashMap<String, Binding>();
   
   private PersistenceManager persistenceManager;
   
   private QueueFactory queueFactory;
   
   private boolean checkAllowable;
    
   public PostOfficeImpl(int nodeID, PersistenceManager persistenceManager, QueueFactory queueFactory,
                         boolean checkAllowable)
   {
      this.nodeID = nodeID;
      
      this.persistenceManager = persistenceManager;
      
      this.queueFactory = queueFactory;
      
      this.checkAllowable = checkAllowable;
   }
      
   // MessagingComponent implementation ---------------------------------------
   
   public void start() throws Exception
   {
      loadBindings();
   }

   public void stop() throws Exception
   {
      mappings.clear();
      
      allowableAddresses.clear();
   }
   
   // PostOffice implementation -----------------------------------------------

   public void addAllowableAddress(String address)
   {      
      allowableAddresses.add(address);
   }
   
   public boolean removeAllowableAddress(String address)
   {      
      return allowableAddresses.remove(address);
   }
   
   public boolean containsAllowableAddress(String address)
   {
      return allowableAddresses.contains(address);
   }


   public Set<String> listAvailableAddresses()
   {
      return allowableAddresses;
   }

   public Binding addBinding(String address, String queueName, Filter filter, 
                             boolean durable, boolean temporary) throws Exception
   {
      Binding binding = createBinding(address, queueName, filter, durable, temporary);

      addBindingInMemory(binding);
      
      if (durable)
      {
         persistenceManager.addBinding(binding);
      }
      
      return binding;      
   }
         
   public Binding removeBinding(String queueName) throws Exception
   {
      Binding binding = removeQueueInMemory(queueName);
      
      if (binding.getQueue().isDurable())
      {
         persistenceManager.deleteBinding(binding);
      }
      
      return binding;
   }
   
   public List<Binding> getBindingsForAddress(String address)
   {
      List<Binding> list = new ArrayList<Binding>();
      
      List<Binding> bindings = mappings.get(address);
      
      if (bindings != null)
      {
         for (Binding binding: bindings)
         {
            if (binding.getNodeID() == nodeID)
            {
               list.add(binding);
            }
         }
      }         
         
      return list;
   }
   
   public Binding getBinding(String queueName)
   {
      return nameMap.get(queueName);
   }
         
   public void route(String address, Message message) throws Exception
   {
     // boolean routeRemote = false;
      
      if (checkAllowable)
      {
         if (!allowableAddresses.contains(address))
         {
            throw new MessagingException(MessagingException.ADDRESS_DOES_NOT_EXIST,
                                         "Cannot route to address " + address);
         }
      }
           
      List<Binding> bindings = mappings.get(address);
      
      if (bindings != null)
      {
         for (Binding binding: bindings)
         {
            Queue queue = binding.getQueue();
            
            if (queue.getFilter() == null || queue.getFilter().match(message))
            {         
               if (binding.getNodeID() == nodeID)
               {
                  //Local queue
                                 
                  message.createReference(queue);              
               }
               else
               {
//                  if (!queue.isDurable())
//                  {
//                     //Remote queue - we never route to remote durable queues since we will lose atomicity in event
//                     //of crash - for moving between durable queues we use message redistribution
//                     
//                     routeRemote = true;                  
//                  }               
               }
            }
         }
      }

      
//      if (routeRemote)
//      {
//         tx.addSynchronization(new CastMessageCallback(new MessageRequest(address, message)));
//      }
   }
   
   public void routeFromCluster(String address, Message message) throws Exception
   {     
      List<Binding> bindings = mappings.get(address);
      
      for (Binding binding: bindings)
      {
         Queue queue = binding.getQueue();
         
         if (binding.getNodeID() == nodeID)
         {         
            if (queue.getFilter() == null || queue.getFilter().match(message))
            {         
               MessageReference ref = message.createReference(queue);

               //We never route durably from other nodes - so no need to persist

               queue.addLast(ref);             
            }
         }
      }
   }

   public Map<String, List<Binding>> getMappings()
   {
      return mappings;
   }



   // Private -----------------------------------------------------------------
   
   private Binding createBinding(String address, String name, Filter filter,
                                 boolean durable, boolean temporary)
   {
      Queue queue = queueFactory.createQueue(-1, name, filter, durable, temporary);
      
      Binding binding = new BindingImpl(this.nodeID, address, queue);
      
      return binding;
   }
   
   private void addBindingInMemory(Binding binding)
   {              
      List<Binding> bindings = new CopyOnWriteArrayList<Binding>();
      
      List<Binding> prevBindings = mappings.putIfAbsent(binding.getAddress(), bindings);
      
      if (prevBindings != null)
      {
         bindings = prevBindings;
      }
                     
      bindings.add(binding);  

      if (nameMap.putIfAbsent(binding.getQueue().getName(), binding) != null)
      {
         throw new IllegalStateException("Binding already exists " + binding);
      }     
   }
   
   private Binding removeQueueInMemory(String queueName) throws Exception
   {
      Binding binding = nameMap.remove(queueName);
      
      if (binding == null)
      {
         throw new IllegalStateException("Queue is not bound " + queueName);
      }
                  
      List<Binding> bindings = mappings.get(binding.getAddress());
                  
      for (Iterator<Binding> iter = bindings.iterator(); iter.hasNext();)
      {
         Binding b = iter.next();
         
         if (b.getQueue().getName().equals(queueName))
         {
            binding = b;
                                          
            break;
         }
      }
      
      if (binding == null)
      {
         throw new IllegalStateException("Cannot find binding " + queueName);
      }
      
      bindings.remove(binding);      
      
      if (bindings.isEmpty())
      {
         mappings.remove(binding.getAddress());
      }
               
      return binding;
   }
   
   private void loadBindings() throws Exception
   {
      List<Binding> bindings = persistenceManager.loadBindings(queueFactory);
      
      for (Binding binding: bindings)
      {
         addBindingInMemory(binding);                    
      }
   }

}
