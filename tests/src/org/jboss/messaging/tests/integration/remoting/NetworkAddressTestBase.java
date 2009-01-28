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

package org.jboss.messaging.tests.integration.remoting;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.server.MessagingService;
import org.jboss.messaging.tests.util.ServiceTestBase;

/**
 * A NetworkAddressTest
 *
 * @author jmesnil
 * 
 * Created 26 janv. 2009 15:06:58
 *
 *
 */
public abstract class NetworkAddressTestBase extends ServiceTestBase
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   static
   {
      try
      {
         Map<NetworkInterface, InetAddress> map = getAddressForEachNetworkInterface();
         StringBuilder s = new StringBuilder("using network settings:\n");
         Set<Entry<NetworkInterface, InetAddress>> set = map.entrySet();
         for (Entry<NetworkInterface, InetAddress> entry : set)
         {
            s.append(entry.getKey().getDisplayName() + ": " + entry.getValue().getHostAddress() + "\n");
         }
         System.out.println(s);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }

   public static Map<NetworkInterface, InetAddress> getAddressForEachNetworkInterface() throws Exception
   {
      Map<NetworkInterface, InetAddress> map = new HashMap<NetworkInterface, InetAddress>();
      Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      while (ifaces.hasMoreElements())
      {
         NetworkInterface iface = (NetworkInterface)ifaces.nextElement();
         Enumeration<InetAddress> enumeration = iface.getInetAddresses();
         while (enumeration.hasMoreElements())
         {
            InetAddress inetAddress = (InetAddress)enumeration.nextElement();
            if (inetAddress instanceof Inet4Address)
            {
               map.put(iface, inetAddress);
               break;
            }
         }
      }

      return map;
   }

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testConnectToServerWithSameHost() throws Exception
   {
      Map<NetworkInterface, InetAddress> map = getAddressForEachNetworkInterface();
      if (map.size() > 0)
      {
         Set<Entry<NetworkInterface, InetAddress>> set = map.entrySet();
         Iterator<Entry<NetworkInterface, InetAddress>> iterator = set.iterator();
         InetAddress address = iterator.next().getValue();
         String host = address.getHostAddress();
         testConnection(host, host, true);
      }
   }
   public void testConnectToServerAcceptingAllHosts() throws Exception
   {
      Map<NetworkInterface, InetAddress> map = getAddressForEachNetworkInterface();
      Set<Entry<NetworkInterface, InetAddress>> set = map.entrySet();
      for (Entry<NetworkInterface, InetAddress> entry : set)
      {
         String host = entry.getValue().getHostAddress();
         testConnection("0.0.0.0", host, true);
      }
   }

   public void testConnectToServerAcceptingOnlyAnotherHost() throws Exception
   {
      Map<NetworkInterface, InetAddress> map = getAddressForEachNetworkInterface();
      if (map.size() <= 1)
      {
         System.err.println("There must be at least 1 network interfaces: test will not be executed");
         return;
      }

      Set<Entry<NetworkInterface, InetAddress>> set = map.entrySet();
      Iterator<Entry<NetworkInterface, InetAddress>> iterator = set.iterator();
      Entry<NetworkInterface, InetAddress> acceptorEntry = iterator.next();
      Entry<NetworkInterface, InetAddress> connectorEntry = iterator.next();

      testConnection(acceptorEntry.getValue().getHostName(), connectorEntry.getValue().getHostAddress(), false);
   }

//   public void testConnectorToServerAcceptingAListOfHosts() throws Exception
//   {
//      Map<NetworkInterface, InetAddress> map = getAddressForEachNetworkInterface();
//      if (map.size() <= 1)
//      {
//         System.err.println("There must be at least 2 network interfaces: test will not be executed");
//         return;
//      }
//
//      Set<Entry<NetworkInterface, InetAddress>> set = map.entrySet();
//      Iterator<Entry<NetworkInterface, InetAddress>> iterator = set.iterator();
//      Entry<NetworkInterface, InetAddress> entry1 = iterator.next();
//      Entry<NetworkInterface, InetAddress> entry2 = iterator.next();
//
//      String listOfHosts = entry1.getValue().getHostAddress() + ", " + entry2.getValue().getHostAddress();
//
//      testConnection(listOfHosts, entry1.getValue().getHostAddress(), true);
//      testConnection(listOfHosts, entry2.getValue().getHostAddress(), true);
//   }

   public void testConnectorToServerAcceptingAListOfHosts_2() throws Exception
   {
      Map<NetworkInterface, InetAddress> map = getAddressForEachNetworkInterface();
      if (map.size() <= 2)
      {
         System.err.println("There must be at least 3 network interfaces: test will not be executed");
         return;
      }

      Set<Entry<NetworkInterface, InetAddress>> set = map.entrySet();
      Iterator<Entry<NetworkInterface, InetAddress>> iterator = set.iterator();
      Entry<NetworkInterface, InetAddress> entry1 = iterator.next();
      Entry<NetworkInterface, InetAddress> entry2 = iterator.next();
      Entry<NetworkInterface, InetAddress> entry3 = iterator.next();

      String listOfHosts = entry1.getValue().getHostAddress() + ", " + entry2.getValue().getHostAddress();

      testConnection(listOfHosts, entry1.getValue().getHostAddress(), true);
      testConnection(listOfHosts, entry2.getValue().getHostAddress(), true);
      testConnection(listOfHosts, entry3.getValue().getHostAddress(), false);
   }

   public void testConnection(String acceptorHost, String connectorHost, boolean mustConnect) throws Exception
   {
      System.out.println("acceptor=" + acceptorHost + ", connector=" + connectorHost + ", mustConnect=" + mustConnect);
      
      Map<String, Object> params = new HashMap<String, Object>();
      params.put(getHostPropertyKey(), acceptorHost);
      TransportConfiguration acceptorConfig = new TransportConfiguration(getAcceptorFactoryClassName(), params);
      Set<TransportConfiguration> transportConfigs = new HashSet<TransportConfiguration>();
      transportConfigs.add(acceptorConfig);

      Configuration config = createDefaultConfig(true);
      config.setAcceptorConfigurations(transportConfigs);
      MessagingService messagingService = createService(false, config);
      messagingService.start();

      params = new HashMap<String, Object>();
      params.put(getHostPropertyKey(), connectorHost);
      TransportConfiguration connectorConfig = new TransportConfiguration(getConnectorFactoryClassName(), params);

      try
      {
         ClientSessionFactory sf = new ClientSessionFactoryImpl(connectorConfig);

         if (mustConnect)
         {
            ClientSession session = sf.createSession(false, true, true);
            session.close();
            System.out.println("connection OK");
         }
         else
         {
            try
            {
               sf.createSession(false, true, true);
               fail("session creation must fail because connector must not be able to connect to the server bound to another network interface");
            }
            catch (Exception e)
            {
            }
         }
      }
      finally
      {
         if (messagingService != null)
         {
            messagingService.stop();
         }
      }
   }
   
   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   protected abstract String getAcceptorFactoryClassName();

   protected abstract String getConnectorFactoryClassName();

   protected abstract String getHostPropertyKey();

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
