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
package org.jboss.messaging.core.plugin;

import java.io.Serializable;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.logging.Logger;
import org.jboss.messaging.core.Message;
import org.jboss.messaging.core.MessageReference;
import org.jboss.messaging.core.message.SimpleMessageReference;
import org.jboss.messaging.core.plugin.contract.PersistenceManager;
import org.jboss.messaging.util.Util;

/**
 * An abstract class that interfaces the access to a persistent store.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class PersistentMessageStore extends InMemoryMessageStore
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(PersistentMessageStore.class);

   // Static --------------------------------------------------------
   
   // Attributes ----------------------------------------------------
   
   private boolean trace = log.isTraceEnabled();
   
   protected PersistenceManager pm;
   
   protected ObjectName pmObjectName;

   // Constructors --------------------------------------------------

   public PersistentMessageStore(String storeID)
   {
      super(storeID, true);
   }
   
   /**
    * Only used for testing. In a real deployment, the persistence manager is
    * injected as a dependency.
    */
   public PersistentMessageStore(Serializable storeID, PersistenceManager pm)
   {
      super(storeID, true);
      
      this.pm = pm;
   }
   
   // JMX operations / attributes ------------------------------------
   
   /**
    * Managed attribute.
    */
   public void setPersistenceManager(ObjectName pmObjectName) throws Exception
   {
      this.pmObjectName = pmObjectName;
   }

   /**
    * Managed attribute.
    */
   public ObjectName getPersistenceManager()
   {
      return pmObjectName;
   }
   
   public synchronized void startService() throws Exception
   {
      super.startService();
      
      MBeanServer server = getServer();
   
      pm = (PersistenceManager)server.getAttribute(pmObjectName, "Instance");   
   }
   
   // MessageStore overrides ----------------------------------------

   public boolean isRecoverable()
   {
      return true;
   }
     
   //Will disappear once lazy loading is implemented
   public MessageReference reference(String messageID) throws Exception
   {
      if (trace) { log.trace("getting reference for message ID: " + messageID);}
      
      //Try and get the reference from the in memory cache first
      MessageReference ref = super.reference(messageID);
      
      if (ref != null)
      {        
         if (trace) { log.trace("Retrieved it from memory cache"); }
         return ref;
      }

      // Try and retrieve it from persistent storage
      // TODO We make a database trip even if the message is non-reliable, but I see no way to avoid
      // TODO this by only knowing the messageID ...
      
      //TODO - We would avoid this by storing the message header fields in the message reference table - Tim
            
      Message m = retrieveMessage(messageID);

      if (m != null)
      {
         //Put it in the memory cache
         MessageHolder holder = super.addMessage(m);
         
         holder.setMessagePersisted(true);
         
         ref = new SimpleMessageReference(holder, this);
      }
      
      return ref;      
   }
   


   // Public --------------------------------------------------------

   public String toString()
   {
      return "PersistentStore[" + Util.guidToString(getStoreID()) + "]";
   }

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   protected Message retrieveMessage(String messageId) throws Exception
   {
      Message m = super.retrieveMessage(messageId);
      
      if (m == null)
      {
         m = pm.getMessage((Serializable)messageId);
         
         if (m != null)
         {
            super.addMessage(m);
            
            if (trace) { log.trace("Retreived it from persistent storage:" + m); }    
         }
      }
      
      return m;      
   }
    
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------   
}
