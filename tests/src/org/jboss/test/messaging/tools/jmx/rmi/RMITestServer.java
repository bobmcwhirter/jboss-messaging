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
package org.jboss.test.messaging.tools.jmx.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;

import javax.management.ObjectName;
import javax.transaction.UserTransaction;

import org.jboss.jms.server.DestinationManager;
import org.jboss.jms.server.ServerPeer;
import org.jboss.logging.Logger;
import org.jboss.messaging.core.plugin.contract.MessageStore;
import org.jboss.messaging.core.plugin.contract.PersistenceManager;
import org.jboss.messaging.core.plugin.contract.PostOffice;
import org.jboss.remoting.ServerInvocationHandler;

/**
 * An RMI wrapper to access the ServiceContainer from a different address space.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:tim.fox@jboss.com>Tim Fox</a>
 * @version <tt>1.1</tt>
 *
 * RMITestServer.java,v 1.1 2006/02/21 08:25:33 timfox Exp
 */
public class RMITestServer extends UnicastRemoteObject implements Server
{
   private static final long serialVersionUID = -368445344011004778L;

   private static final Logger log = Logger.getLogger(RMITestServer.class);

   protected RemoteTestServer server;

   private RMINamingDelegate namingDelegate;

   //public static final int RMI_REGISTRY_PORT = 25989;
   
   //We allow for up to 5 rmi test servers running simultaneously
   public static final int[] RMI_REGISTRY_PORTS = 
      new int[] {25989, 25990, 25991, 25992, 25993};
   
   public static final String RMI_SERVER_NAME = "messaging-rmi-server";
   public static final String NAMING_SERVER_NAME = "naming-rmi-server";

   private static Registry registry;
   
   public static void main(String[] args) throws Exception
   {
      log.debug("initializing RMI runtime");

      String host = System.getProperty("test.bind.address");
      if (host == null)
      {
         host = "localhost";
      }

      log.info("bind address: " + host);
      
      int index;
      String registryIndex = System.getProperty("test.registry.index");
      if (registryIndex == null)
      {
         //Use the 0th port
         index = 0;
      }
      else
      {
         index = Integer.parseInt(registryIndex);         
      }
      int port = RMI_REGISTRY_PORTS[index];

      // let RMI know the bind address
      System.setProperty("java.rmi.server.hostname", host);

      registry = LocateRegistry.createRegistry(port);
      log.debug("registry created at port: " + port);

      RMITestServer testServer = new RMITestServer(index);
      log.debug("RMI server created");

      registry.bind(RMI_SERVER_NAME, testServer);
      registry.bind(NAMING_SERVER_NAME, testServer.getNamingDelegate());

      log.info("RMI server bound");
   }

   public class VMKiller implements Runnable
   {
      public void run()
      {
         log.info("shutting down the VM");

         try
         {
            Thread.sleep(250);
         }
         catch(Exception e)
         {
            log.warn("interrupted while sleeping", e);
         }

         System.exit(0);
      }
   }

   public RMITestServer(int index) throws Exception
   {
      namingDelegate = new RMINamingDelegate();

      server = new RemoteTestServer(index);
   }

   public void configureSecurityForDestination(String destName, String config) throws Exception
   {
      server.configureSecurityForDestination(destName, config);
   }

   public ObjectName deploy(String mbeanConfiguration) throws Exception
   {
      return server.deploy(mbeanConfiguration);
   }

   public void deployQueue(String name, String jndiName, boolean clustered) throws Exception
   {
      server.deployQueue(name, jndiName, clustered);
   }

   public void deployTopic(String name, String jndiName, boolean clustered) throws Exception
   {
      server.deployTopic(name, jndiName, clustered);
   }
   
   public void deployQueue(String name,
                           String jndiName,
                           int fullSize,
                           int pageSize,
                           int downCacheSize,
                           boolean clustered) throws Exception
   {
      server.deployQueue(name, jndiName, fullSize, pageSize, downCacheSize, clustered);
   }

   public void createQueue(String name, String jndiName) throws Exception
   {
      server.createQueue(name, jndiName);
   }

   public void deployTopic(String name,
                           String jndiName,
                           int fullSize,
                           int pageSize,
                           int downCacheSize,
                           boolean clustered) throws Exception
   {
      server.deployTopic(name, jndiName, fullSize, pageSize, downCacheSize, clustered);
   }

   public void createTopic(String name, String jndiName) throws Exception
   {
      server.createTopic(name, jndiName);
   }

   public void deployConnectionFactory(String objectName, String[] jndiBindings)
      throws Exception
   {
      server.deployConnectionFactory(objectName, jndiBindings);
   }
   
   public void deployConnectionFactory(String objectName, String[] jndiBindings, int prefetchSize)
      throws Exception
   {
      server.deployConnectionFactory(objectName, jndiBindings, prefetchSize);
   }

   public void deployConnectionFactory(String objectName,
                                       String[] jndiBindings,
                                       int prefetchSize,
                                       int defaultTempQueueFullSize,
                                       int defaultTempQueuePageSize,
                                       int defaultTempQueueDownCacheSize) throws Exception
   {
      server.deployConnectionFactory(objectName, jndiBindings, prefetchSize,
               defaultTempQueueFullSize, defaultTempQueuePageSize, defaultTempQueueDownCacheSize);
   }

   public void undeployConnectionFactory(ObjectName objectName) throws Exception
   {
      server.undeployConnectionFactory(objectName);
   }

   public synchronized void destroy() throws Exception
   {
      server.destroy();

      registry.unbind(RMI_SERVER_NAME);
      registry.unbind(NAMING_SERVER_NAME);
      
      //Now shutdown the process
      
      //TODO - we should shutdown cleanly - let main() exit - not kill the process
      
      new Thread(new VMKiller(), "VM Killer").start();
   }

   public Object getAttribute(ObjectName on, String attribute) throws Exception
   {
      return server.getAttribute(on, attribute);
   }

   public Set getConnectorSubsystems() throws Exception
   {
      return server.getConnectorSubsystems();
   }

   public void addServerInvocationHandler(String subsystem, ServerInvocationHandler handler)
      throws Exception
   {
      server.addServerInvocationHandler(subsystem, handler);
   }

   public void removeServerInvocationHandler(String subsystem) throws Exception
   {
      server.removeServerInvocationHandler(subsystem);
   }

   public String getDefaultSecurityConfig() throws Exception
   {
      return server.getDefaultSecurityConfig();
   }

   public DestinationManager getDestinationManager() throws Exception
   {
      return server.getDestinationManager();
   }

   public MessageStore getMessageStore() throws Exception
   {
      return server.getMessageStore();
   }

   public PersistenceManager getPersistenceManager() throws Exception
   {
      return server.getPersistenceManager();
   }
   
   public PostOffice getQueuePostOffice() throws Exception
   {
      return server.getQueuePostOffice();
   }
   
   public PostOffice getTopicPostOffice() throws Exception
   {
      return server.getTopicPostOffice();
   }

   public ObjectName getServerPeerObjectName() throws Exception
   {
      return server.getServerPeerObjectName();
   }

   public Object invoke(ObjectName on, String operationName, Object[] params, String[] signature) throws Exception
   {
      return server.invoke(on, operationName, params, signature);
   }

   public boolean isServerPeerStarted() throws Exception
   {
      return server.isServerPeerStarted();
   }

   public boolean isStarted() throws Exception
   {
      return server.isStarted();
   }

   public void log(int level, String text) throws Exception
   {
      server.log(level, text);
   }

   public Set query(ObjectName pattern) throws Exception
   {
      return server.query(pattern);
   }

   public String getDatabaseType()
   {
      return server.getDatabaseType();
   }

   public void setAttribute(ObjectName on, String name, String valueAsString) throws Exception
   {
      server.setAttribute(on, name, valueAsString);
   }

   public void setDefaultSecurityConfig(String config) throws Exception
   {
      server.setDefaultSecurityConfig(config);
   }

   public void start(String containerConfig, boolean clustered) throws Exception
   {
      server.start(containerConfig, clustered);
   }

   public void startServerPeer(int serverPeerID, String defaultQueueJNDIContext,
                               String defaultTopicJNDIContext, boolean clustered) throws Exception
   {
      server.startServerPeer(serverPeerID, defaultQueueJNDIContext, defaultTopicJNDIContext, clustered);
   }

   public void stop() throws Exception
   {
      server.stop();

      namingDelegate.reset();
   }

   public void stopServerPeer() throws Exception
   {
      server.stopServerPeer();
   }

   public void undeploy(ObjectName on) throws Exception
   {
      server.undeploy(on);
   }

   public void undeployDestination(boolean isQueue, String name) throws Exception
   {
      server.undeployDestination(isQueue, name);
   }

   public boolean destroyDestination(boolean isQueue, String name) throws Exception
   {
      return server.destroyDestination(isQueue, name);
   }

   public Object executeCommand(Command command) throws Exception
   {
      return server.executeCommand(command);
   }

   public ServerPeer getServerPeer() throws Exception
   {
      return server.getServerPeer();
   }

   public UserTransaction getUserTransaction() throws Exception
   {
      return server.getUserTransaction();
   }

   private RMINamingDelegate getNamingDelegate()
   {
      return namingDelegate;
   }
}
