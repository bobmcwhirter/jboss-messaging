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
package org.jboss.test.messaging.tools.container;

import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_ACK_BATCH_SIZE;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_AUTO_GROUP;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_CALL_TIMEOUT;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_CONNECTION_TTL;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_CONSUMER_MAX_RATE;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_MAX_CONNECTIONS;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_MIN_LARGE_MESSAGE_SIZE;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_PRE_ACKNOWLEDGE;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_PRODUCER_MAX_RATE;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_PRODUCER_WINDOW_SIZE;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_RECONNECT_ATTEMPTS;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_THREAD_POOL_MAX_SIZE;
import static org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl.DEFAULT_USE_GLOBAL_POOLS;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.naming.InitialContext;

import org.jboss.kernel.plugins.config.property.PropertyKernelConfig;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.management.ObjectNames;
import org.jboss.messaging.core.management.ResourceNames;
import org.jboss.messaging.core.postoffice.Binding;
import org.jboss.messaging.core.postoffice.BindingType;
import org.jboss.messaging.core.security.Role;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.core.server.Queue;
import org.jboss.messaging.integration.bootstrap.JBMBootstrapServer;
import org.jboss.messaging.jms.JBossQueue;
import org.jboss.messaging.jms.JBossTopic;
import org.jboss.messaging.jms.server.JMSServerManager;
import org.jboss.messaging.jms.server.management.JMSQueueControlMBean;
import org.jboss.messaging.jms.server.management.TopicControlMBean;
import org.jboss.messaging.utils.Pair;
import org.jboss.messaging.utils.SimpleString;

/**
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>1.1</tt>
 *          <p/>
 *          LocalTestServer.java,v 1.1 2006/02/21 08:25:32 timfox Exp
 */
public class LocalTestServer implements Server, Runnable
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(LocalTestServer.class);

   private boolean started = false;

   private HashMap<String, List<String>> allBindings = new HashMap<String, List<String>>();

   // Static ---------------------------------------------------------------------------------------

   public static void setEnvironmentServerIndex(int serverIndex)
   {
      System.setProperty(Constants.SERVER_INDEX_PROPERTY_NAME, Integer.toString(serverIndex));
   }

   public static void clearEnvironmentServerIndex()
   {
      System.getProperty(Constants.SERVER_INDEX_PROPERTY_NAME, null);
   }

   // Attributes -----------------------------------------------------------------------------------

   private int serverIndex;

   JBMBootstrapServer bootstrap;

   // Constructors ---------------------------------------------------------------------------------

   public LocalTestServer()
   {
      super();

      this.serverIndex = 0;
   }

   // Server implementation ------------------------------------------------------------------------

   public int getServerID()
   {
      return serverIndex;
   }

   public synchronized void start(String[] containerConfig, HashMap<String, Object> configuration, boolean clearJournal) throws Exception
   {
      if (isStarted())
      {
         return;
      }

      if (clearJournal)
      {
         // Delete the Journal environment

         File dir = new File("data");

         boolean deleted = deleteDirectory(dir);

         log.info("Deleted dir: " + dir.getAbsolutePath() + " deleted: " + deleted);
      }

      PropertyKernelConfig propertyKernelConfig = new PropertyKernelConfig(System.getProperties());
      bootstrap = new JBMBootstrapServer(propertyKernelConfig, containerConfig);
      System.setProperty(Constants.SERVER_INDEX_PROPERTY_NAME, "" + getServerID());
      bootstrap.run();
      started = true;

   }

   private static boolean deleteDirectory(File directory)
   {
      if (directory.isDirectory())
      {
         String[] files = directory.list();

         for (int j = 0; j < files.length; j++)
         {
            if (!deleteDirectory(new File(directory, files[j])))
            {
               return false;
            }
         }
      }

      return directory.delete();
   }

   public synchronized boolean stop() throws Exception
   {
      bootstrap.shutDown();
      started = false;
      unbindAll();
      return true;
   }

   public void ping() throws Exception
   {
      if (!isStarted())
      {
         throw new RuntimeException("ok");
      }
   }

   public synchronized void kill() throws Exception
   {
      stop();
   }

   public synchronized boolean isStarted() throws Exception
   {
      return started;
   }

   public synchronized void startServerPeer() throws Exception
   {
      System.setProperty(Constants.SERVER_INDEX_PROPERTY_NAME, "" + getServerID());
      getMessagingServer().start();
   }

   public synchronized void stopServerPeer() throws Exception
   {
      System.setProperty(Constants.SERVER_INDEX_PROPERTY_NAME, "" + getServerID());
      getMessagingServer().stop();
      // also unbind everything
      unbindAll();
   }

   private void unbindAll() throws Exception
   {
      Collection<List<String>> bindings = allBindings.values();
      for (List<String> binding : bindings)
      {
         for (String s : binding)
         {
            getInitialContext().unbind(s);
         }
      }
   }

   /**
    * Only for in-VM use!
    */
   public MessagingServer getServerPeer()
   {
      return getMessagingServer();
   }

   public void destroyQueue(String name, String jndiName) throws Exception
   {
      this.getJMSServerManager().destroyQueue(name);
   }

   public void destroyTopic(String name, String jndiName) throws Exception
   {
      this.getJMSServerManager().destroyTopic(name);
   }

   public void createQueue(String name, String jndiName) throws Exception
   {
      this.getJMSServerManager().createQueue(name, "/queue/" + (jndiName != null ? jndiName : name), null, true);
   }

   public void createTopic(String name, String jndiName) throws Exception
   {
      this.getJMSServerManager().createTopic(name, "/topic/" + (jndiName != null ? jndiName : name));
   }

   public void deployConnectionFactory(String clientId, String objectName, List<String> jndiBindings) throws Exception
   {
      deployConnectionFactory(clientId, objectName, jndiBindings, -1, -1, -1, -1, false, false, -1, false);
   }

   public void deployConnectionFactory(String objectName, List<String> jndiBindings, int consumerWindowSize) throws Exception
   {
      deployConnectionFactory(null, objectName, jndiBindings, consumerWindowSize, -1, -1, -1, false, false, -1, false);
   }

   public void deployConnectionFactory(String objectName, List<String> jndiBindings) throws Exception
   {
      deployConnectionFactory(null, objectName, jndiBindings, -1, -1, -1, -1, false, false, -1, false);
   }

   public void deployConnectionFactory(String objectName,
                                       List<String> jndiBindings,
                                       int prefetchSize,
                                       int defaultTempQueueFullSize,
                                       int defaultTempQueuePageSize,
                                       int defaultTempQueueDownCacheSize) throws Exception
   {
      this.deployConnectionFactory(null,
                                   objectName,
                                   jndiBindings,
                                   prefetchSize,
                                   defaultTempQueueFullSize,
                                   defaultTempQueuePageSize,
                                   defaultTempQueueDownCacheSize,
                                   false,
                                   false,
                                   -1,
                                   false);
   }

   public void deployConnectionFactory(String objectName,
                                       List<String> jndiBindings,
                                       boolean supportsFailover,
                                       boolean supportsLoadBalancing) throws Exception
   {
      this.deployConnectionFactory(null,
                                   objectName,
                                   jndiBindings,
                                   -1,
                                   -1,
                                   -1,
                                   -1,
                                   supportsFailover,
                                   supportsLoadBalancing,
                                   -1,
                                   false);
   }

   public void deployConnectionFactory(String clientId,
                                       String objectName,
                                       List<String> jndiBindings,
                                       int prefetchSize,
                                       int defaultTempQueueFullSize,
                                       int defaultTempQueuePageSize,
                                       int defaultTempQueueDownCacheSize,
                                       boolean supportsFailover,
                                       boolean supportsLoadBalancing,
                                       int dupsOkBatchSize,
                                       boolean blockOnAcknowledge) throws Exception
   {
      List<Pair<TransportConfiguration, TransportConfiguration>> connectorConfigs = new ArrayList<Pair<TransportConfiguration, TransportConfiguration>>();

      connectorConfigs.add(new Pair<TransportConfiguration, TransportConfiguration>(new TransportConfiguration("org.jboss.messaging.integration.transports.netty.NettyConnectorFactory"),
                                                                                    null));

      getJMSServerManager().createConnectionFactory(objectName,
                                                    connectorConfigs,
                                                    clientId,
                                                    DEFAULT_CLIENT_FAILURE_CHECK_PERIOD,
                                                    DEFAULT_CONNECTION_TTL,
                                                    DEFAULT_CALL_TIMEOUT,
                                                    DEFAULT_MAX_CONNECTIONS,
                                                    DEFAULT_MIN_LARGE_MESSAGE_SIZE,
                                                    prefetchSize,
                                                    DEFAULT_CONSUMER_MAX_RATE,
                                                    DEFAULT_PRODUCER_WINDOW_SIZE,
                                                    DEFAULT_PRODUCER_MAX_RATE,
                                                    blockOnAcknowledge,
                                                    true,
                                                    true,
                                                    DEFAULT_AUTO_GROUP,
                                                    DEFAULT_PRE_ACKNOWLEDGE,
                                                    DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME,
                                                    DEFAULT_ACK_BATCH_SIZE,
                                                    dupsOkBatchSize,
                                                    DEFAULT_USE_GLOBAL_POOLS,
                                                    DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE,
                                                    DEFAULT_THREAD_POOL_MAX_SIZE,                                                     
                                                    DEFAULT_RETRY_INTERVAL,
                                                    DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                                                    DEFAULT_RECONNECT_ATTEMPTS,
                                                    DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN,
                                                    jndiBindings);
   }

   public void undeployConnectionFactory(String objectName) throws Exception
   {
      getJMSServerManager().destroyConnectionFactory(objectName);
   }

   public void configureSecurityForDestination(String destName, boolean isQueue, Set<Role> roles) throws Exception
   {
      String destination = (isQueue ? "jms.queue." : "jms.topic.") + destName;
      if (roles != null)
      {
         getMessagingServer().getSecurityRepository().addMatch(destination, roles);
      }
      else
      {
         getMessagingServer().getSecurityRepository().removeMatch(destination);
      }
   }

   // Public ---------------------------------------------------------------------------------------

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   public MessagingServer getMessagingServer()
   {
      return (MessagingServer)bootstrap.getKernel().getRegistry().getEntry("MessagingServer").getTarget();
   }

   public JMSServerManager getJMSServerManager()
   {
      return (JMSServerManager)bootstrap.getKernel().getRegistry().getEntry("JMSServerManager").getTarget();
   }

   public InitialContext getInitialContext() throws Exception
   {
      Properties props = new Properties();
      props.setProperty("java.naming.factory.initial",
                        "org.jboss.test.messaging.tools.container.InVMInitialContextFactory");
      props.setProperty(Constants.SERVER_INDEX_PROPERTY_NAME, "" + getServerID());
      // props.setProperty("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
      return new InitialContext(props);
   }

   public void run()
   {
      bootstrap.run();

      started = true;

      synchronized (this)
      {
         notify();
         try
         {
            wait();
         }
         catch (InterruptedException e)
         {
            // e.printStackTrace();
         }
      }

   }

   public Integer getMessageCountForQueue(String queueName) throws Exception
   {
      JMSQueueControlMBean queue = (JMSQueueControlMBean)getMessagingServer().getManagementService()
                                                                             .getResource(ResourceNames.JMS_QUEUE + queueName);
      if (queue != null)
      {
         return queue.getMessageCount();
      }
      else
      {
         return -1;
      }
   }

   public void removeAllMessages(String destination, boolean isQueue) throws Exception
   {
      SimpleString address = JBossQueue.createAddressFromName(destination);
      if (!isQueue)
      {
         address = JBossTopic.createAddressFromName(destination);
      }
      Binding binding = getMessagingServer().getPostOffice().getBinding(address);
      if (binding != null && binding.getType() == BindingType.LOCAL_QUEUE)
      {
         ((Queue)binding.getBindable()).deleteAllReferences();
      }
   }

   public List<String> listAllSubscribersForTopic(String s) throws Exception
   {
      ObjectName objectName = ObjectNames.getJMSTopicObjectName(s);
      TopicControlMBean topic = (TopicControlMBean)MBeanServerInvocationHandler.newProxyInstance(ManagementFactory.getPlatformMBeanServer(),
                                                                                                 objectName,
                                                                                                 TopicControlMBean.class,
                                                                                                 false);
      Object[] subInfos = topic.listAllSubscriptions();
      List<String> subs = new ArrayList<String>();
      for (Object o : subInfos)
      {
         Object[] data = (Object[])o;
         subs.add((String)data[2]);
      }
      return subs;
   }

   public Set<Role> getSecurityConfig() throws Exception
   {
      return getMessagingServer().getSecurityRepository().getMatch("*");
   }

   public void setSecurityConfig(Set<Role> defConfig) throws Exception
   {
      getMessagingServer().getSecurityRepository().removeMatch("#");
      getMessagingServer().getSecurityRepository().addMatch("#", defConfig);
   }

   // Inner classes --------------------------------------------------------------------------------

}
