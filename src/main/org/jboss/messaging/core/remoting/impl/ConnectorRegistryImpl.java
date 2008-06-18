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

package org.jboss.messaging.core.remoting.impl;

import org.jboss.messaging.core.client.ConnectionParams;
import org.jboss.messaging.core.client.Location;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.ConnectorRegistry;
import org.jboss.messaging.core.remoting.PacketDispatcher;
import org.jboss.messaging.core.remoting.RemotingConnector;
import org.jboss.messaging.core.remoting.TransportType;
import static org.jboss.messaging.core.remoting.TransportType.INVM;
import static org.jboss.messaging.core.remoting.TransportType.TCP;
import org.jboss.messaging.core.remoting.impl.invm.INVMConnector;
import org.jboss.messaging.core.remoting.impl.mina.MinaConnector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * @version <tt>$Revision$</tt>
 */
public class ConnectorRegistryImpl implements ConnectorRegistry
{
   // Constants -----------------------------------------------------

   public static final Logger log = Logger.getLogger(ConnectorRegistryImpl.class);

   // Attributes ----------------------------------------------------

   // the String key corresponds to Configuration.getLocation()
   private Map<String, PacketDispatcher> localDispatchers = new HashMap<String, PacketDispatcher>();

   private Map<String, NIOConnectorHolder> connectors = new HashMap<String, NIOConnectorHolder>();

   private final AtomicLong idCounter = new AtomicLong(0);

   // Static --------------------------------------------------------

   /**
    * @return <code>true</code> if this Configuration has not already been registered,
    *         <code>false</code> else
    */
   public boolean register(Location location, PacketDispatcher serverDispatcher)
   {
      assert location != null;
      assert serverDispatcher != null;
      String key = location.getLocation();

      PacketDispatcher previousDispatcher = localDispatchers.get(key);

      localDispatchers.put(key, serverDispatcher);
      if (log.isDebugEnabled())
      {
         log.debug("registered " + key + " for " + serverDispatcher);
      }

      return (previousDispatcher == null);
   }

   /**
    * @return <code>true</code> if this Configuration was registered,
    *         <code>false</code> else
    */
   public boolean unregister(Location location)
   {
      PacketDispatcher dispatcher = localDispatchers.remove(location.getLocation());

      if (log.isDebugEnabled())
      {
         log.debug("unregistered " + dispatcher);
      }

      return (dispatcher != null);
   }

   public synchronized RemotingConnector getConnector(Location location, ConnectionParams connectionParams)
   {
      assert location != null;
      String key = location.getLocation();

      if (connectors.containsKey(key))
      {
         NIOConnectorHolder holder = connectors.get(key);
         holder.increment();
         RemotingConnector connector = holder.getConnector();

         if (log.isDebugEnabled())
            log.debug("Reuse " + connector + " to connect to "
                    + key + " [count=" + holder.getCount() + "]");

         return connector;
      }

      //TODO INVM optimisation is disabled for now

      // check if the server is in the same vm than the client
//      if (localDispatchers.containsKey(key))
//      {
//         PacketDispatcher localDispatcher = localDispatchers.get(key);
//         NIOConnector connector = new INVMConnector(idCounter.getAndIncrement(), dispatcher, localDispatcher);
//
//         if (log.isDebugEnabled())
//            log.debug("Created " + connector + " to connect to "
//                  + key);
//
//         NIOConnectorHolder holder = new NIOConnectorHolder(connector);
//         connectors.put(key, holder);
//         return connector;
//      }

      RemotingConnector connector = null;

      TransportType transport = location.getTransport();

      if (transport == TCP)
      {
         connector = new MinaConnector(location, connectionParams, new PacketDispatcherImpl(null));
      }
      else if (transport == INVM)
      {
         PacketDispatcher localDispatcher = localDispatchers.get(key);
         connector = new INVMConnector(location, connectionParams, idCounter.getAndIncrement(), new PacketDispatcherImpl(null), localDispatcher);
      }

      if (connector == null)
      {
         throw new IllegalArgumentException(
                 "no connector defined for transport " + transport);
      }

      if (log.isDebugEnabled())
         log.debug("Created " + connector + " to connect to "
                 + location);

      NIOConnectorHolder holder = new NIOConnectorHolder(connector);
      connectors.put(key, holder);
      return connector;
   }

   /**
    * Decrement the number of references on the NIOConnector corresponding to
    * the Configuration.
    * <p/>
    * If there is only one reference, remove it from the connectors Map and
    * returns it. Otherwise return null.
    *
    * @param location a Location
    * @return the NIOConnector if there is no longer any references to it or
    *         <code>null</code>
    * @throws IllegalStateException if no NIOConnector were created for the given Configuration
    */
   public synchronized RemotingConnector removeConnector(Location location)
   {
      assert location != null;
      String key = location.getLocation();

      NIOConnectorHolder holder = connectors.get(key);
      if (holder == null)
      {
         throw new IllegalStateException("No Connector were created for "
                 + key);
      }

      if (holder.getCount() == 1)
      {
         if (log.isDebugEnabled())
            log.debug("Removed connector for " + key);
         connectors.remove(key);
         return holder.getConnector();
      }
      else
      {
         holder.decrement();
         if (log.isDebugEnabled())
            log.debug(holder.getCount() + " remaining references to "
                    + key);
         return null;
      }
   }

   public int getRegisteredConfigurationSize()
   {
      Collection<String> registeredConfigs = connectors.keySet();
      return registeredConfigs.size();
   }

   public int getConnectorCount(Location location)
   {
      String key = location.getLocation();
      NIOConnectorHolder holder = connectors.get(key);
      if (holder == null)
      {
         return 0;
      }
      return holder.getCount();
   }

   public void clear()
   {
      connectors.clear();
   }

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

   static class NIOConnectorHolder
   {
      private final RemotingConnector connector;
      private int count;

      public NIOConnectorHolder(RemotingConnector connector)
      {
         assert connector != null;

         this.connector = connector;
         this.count = 1;
      }

      public void increment()
      {
         assert count > 0;

         count++;
      }

      public void decrement()
      {
         count--;

         assert count > 0;
      }

      public int getCount()
      {
         return count;
      }

      public RemotingConnector getConnector()
      {
         return connector;
      }
   }
}
