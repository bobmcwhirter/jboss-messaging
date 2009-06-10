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

package org.jboss.messaging.core.management.impl;

import org.jboss.messaging.core.config.cluster.ClusterConnectionConfiguration;
import org.jboss.messaging.core.management.ClusterConnectionControl;
import org.jboss.messaging.core.server.cluster.ClusterConnection;
import org.jboss.messaging.utils.Pair;

/**
 * A ClusterConnectionControl
 *
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 */
public class ClusterConnectionControlImpl implements ClusterConnectionControl
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private final ClusterConnection clusterConnection;

   private final ClusterConnectionConfiguration configuration;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public ClusterConnectionControlImpl(final ClusterConnection clusterConnection,
                                   ClusterConnectionConfiguration configuration)
   {
      this.clusterConnection = clusterConnection;
      this.configuration = configuration;
   }

   // ClusterConnectionControlMBean implementation ---------------------------

   public String getAddress()
   {
      return configuration.getAddress();
   }

   public String getDiscoveryGroupName()
   {
      return configuration.getDiscoveryGroupName();
   }

   public int getMaxHops()
   {
      return configuration.getMaxHops();
   }
   
   public String getName()
   {
      return configuration.getName();
   }

   public long getRetryInterval()
   {
      return configuration.getRetryInterval();
   }

   public Object[] getStaticConnectorNamePairs()
   {
      Object[] ret = new Object[configuration.getStaticConnectorNamePairs().size()];
      
      int i = 0;
      for (Pair<String, String> pair: configuration.getStaticConnectorNamePairs())
      {
         String[] opair = new String[2];
         
         opair[0] = pair.a;
         opair[1] = pair.b != null ? pair.b : null;
         
         ret[i++] = opair;
      }
      
      return ret;            
   }

   public boolean isDuplicateDetection()
   {
      return configuration.isDuplicateDetection();
   }

   public boolean isForwardWhenNoConsumers()
   {
      return configuration.isForwardWhenNoConsumers();
   }

   public boolean isStarted()
   {
      return clusterConnection.isStarted();
   }

   public void start() throws Exception
   {
      clusterConnection.start();
   }

   public void stop() throws Exception
   {
      clusterConnection.stop();
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
