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

package org.jboss.messaging.tests.integration.management;

import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.management.DiscoveryGroupControl;
import org.jboss.messaging.core.management.ResourceNames;
import org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory;

/**
 * A DiscoveryGroupControlUsingCoreTest
 *
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 *
 *
 */
public class DiscoveryGroupControlUsingCoreTest extends DiscoveryGroupControlTest
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private ClientSession session;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // DiscoveryGroupControlTest overrides --------------------------------

   @Override
   protected DiscoveryGroupControl createManagementControl(final String name) throws Exception
   {
      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration(InVMConnectorFactory.class.getName()));
      session = sf.createSession(false, true, true);
      session.start();

      return new DiscoveryGroupControl()
      {
         private final CoreMessagingProxy proxy = new CoreMessagingProxy(session,
                                                                         ResourceNames.CORE_DISCOVERY_GROUP + name);
         
         public String getGroupAddress()
         {
            return (String)proxy.retrieveAttributeValue("GroupAddress");
         }

         public int getGroupPort()
         {
            return (Integer)proxy.retrieveAttributeValue("GroupPort");
         }

         public long getRefreshTimeout()
         {
            return (Long)proxy.retrieveAttributeValue("RefreshTimeout");
         }

         public String getName()
         {
            return (String)proxy.retrieveAttributeValue("Name");
         }

         public boolean isStarted()
         {
            return (Boolean)proxy.retrieveAttributeValue("Started");
         }

         public void start() throws Exception
         {
            proxy.invokeOperation("start");
         }

         public void stop() throws Exception
         {
            proxy.invokeOperation("stop");
         }
      };
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   @Override
   protected void tearDown() throws Exception
   {
      if (session != null)
      {
         session.close();
      }

      super.tearDown();
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
