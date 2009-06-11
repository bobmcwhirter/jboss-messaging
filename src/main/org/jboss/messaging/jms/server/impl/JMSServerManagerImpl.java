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

package org.jboss.messaging.jms.server.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.deployers.DeploymentManager;
import org.jboss.messaging.core.deployers.impl.FileDeploymentManager;
import org.jboss.messaging.core.deployers.impl.XmlDeployer;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.server.ActivateCallback;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.jms.JBossQueue;
import org.jboss.messaging.jms.JBossTopic;
import org.jboss.messaging.jms.client.JBossConnectionFactory;
import org.jboss.messaging.jms.client.SelectorTranslator;
import org.jboss.messaging.jms.server.JMSServerManager;
import org.jboss.messaging.jms.server.management.JMSManagementService;
import org.jboss.messaging.jms.server.management.impl.JMSManagementServiceImpl;
import org.jboss.messaging.utils.Pair;

/**
 * A Deployer used to create and add to JNDI queues, topics and connection
 * factories. Typically this would only be used in an app server env.
 *
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 * @author <a href="tim.fox@jboss.com">Tim Fox</a>
 */
public class JMSServerManagerImpl implements JMSServerManager, ActivateCallback
{
   private static final Logger log = Logger.getLogger(JMSServerManagerImpl.class);

   private static final String REJECT_FILTER = "__JBMX=-1";

   /**
    * the context to bind to
    */
   private Context context;

   private final Map<String, List<String>> destinations = new HashMap<String, List<String>>();

   private final Map<String, JBossConnectionFactory> connectionFactories = new HashMap<String, JBossConnectionFactory>();

   private final Map<String, List<String>> connectionFactoryBindings = new HashMap<String, List<String>>();

   private final MessagingServer server;

   private JMSManagementService jmsManagementService;

   private XmlDeployer jmsDeployer;

   private boolean started;

   private boolean active;

   private DeploymentManager deploymentManager;

   private final String configFileName;
   
   private boolean contextSet;
   
   public JMSServerManagerImpl(final MessagingServer server) throws Exception
   {
      this.server = server;
      
      this.configFileName = null;
   }

   public JMSServerManagerImpl(final MessagingServer server, final String configFileName) throws Exception
   {
      this.server = server;

      this.configFileName = configFileName;
   }

   // ActivateCallback implementation -------------------------------------

   public synchronized void activated()
   {
      active = true;

      jmsManagementService = new JMSManagementServiceImpl(server.getManagementService());

      try
      {
         jmsManagementService.registerJMSServer(this);

         jmsDeployer = new JMSServerDeployer(this, deploymentManager, server.getConfiguration());

         if (configFileName != null)
         {
            jmsDeployer.setConfigFileNames(new String[] { configFileName });
         }

         jmsDeployer.start();

         deploymentManager.start();
      }
      catch (Exception e)
      {
         log.error("Failed to start jms deployer");
      }
   }

   // MessagingComponent implementation -----------------------------------

   public synchronized void start() throws Exception
   {
      if (started)
      {
         return;
      }

      if (!contextSet)
      {
         context = new InitialContext();
      }

      deploymentManager = new FileDeploymentManager(server.getConfiguration().getFileDeployerScanPeriod());

      server.registerActivateCallback(this);

      server.start();

      started = true;
   }

   public synchronized void stop() throws Exception
   {
      if (!started)
      {
         return;
      }

      if (jmsDeployer != null)
      {
         jmsDeployer.stop();
      }

      deploymentManager.stop();

      for (String destination : destinations.keySet())
      {
         undeployDestination(destination);
      }

      for (String connectionFactory : new HashSet<String>(connectionFactories.keySet()))
      {
         destroyConnectionFactory(connectionFactory);
      }

      destinations.clear();
      connectionFactories.clear();
      connectionFactoryBindings.clear();

      if (context != null)
      {
         context.close();
      }

      server.stop();

      started = false;
   }

   public boolean isStarted()
   {
      return server.getMessagingServerControl().isStarted();
   }

   // JMSServerManager implementation -------------------------------

   public synchronized void setContext(final Context context)
   {
      this.context = context;
      
      this.contextSet = true;
   }

   public synchronized String getVersion()
   {
      checkInitialised();

      return server.getMessagingServerControl().getVersion();
   }

   public synchronized boolean createQueue(final String queueName,
                                           final String jndiBinding,
                                           final String selectorString,
                                           boolean durable) throws Exception
   {
      checkInitialised();
      JBossQueue jBossQueue = new JBossQueue(queueName);

      // Convert from JMS selector to core filter
      String coreFilterString = null;

      if (selectorString != null)
      {
         coreFilterString = SelectorTranslator.convertToJBMFilterString(selectorString);
      }

      server.getMessagingServerControl().deployQueue(jBossQueue.getAddress(),
                                                     jBossQueue.getAddress(),
                                                     coreFilterString,
                                                     durable);

      boolean added = bindToJndi(jndiBinding, jBossQueue);

      if (added)
      {
         addToDestinationBindings(queueName, jndiBinding);
      }

      jmsManagementService.registerQueue(jBossQueue, jndiBinding);
      return added;
   }

   public synchronized boolean createTopic(final String topicName, final String jndiBinding) throws Exception
   {
      checkInitialised();
      JBossTopic jBossTopic = new JBossTopic(topicName);
      // We create a dummy subscription on the topic, that never receives messages - this is so we can perform JMS
      // checks when routing messages to a topic that
      // does not exist - otherwise we would not be able to distinguish from a non existent topic and one with no
      // subscriptions - core has no notion of a topic
      server.getMessagingServerControl().deployQueue(jBossTopic.getAddress(),
                                                     jBossTopic.getAddress(),
                                                     REJECT_FILTER,
                                                     true);
      boolean added = bindToJndi(jndiBinding, jBossTopic);
      if (added)
      {
         addToDestinationBindings(topicName, jndiBinding);
      }
      jmsManagementService.registerTopic(jBossTopic, jndiBinding);
      return added;
   }

   public synchronized boolean undeployDestination(final String name) throws Exception
   {
      checkInitialised();
      List<String> jndiBindings = destinations.get(name);
      if (jndiBindings == null || jndiBindings.size() == 0)
      {
         return false;
      }
      if (context != null)
      {
         Iterator<String> iter = jndiBindings.iterator();      
         while (iter.hasNext())
         {
            String jndiBinding = (String)iter.next();
            context.unbind(jndiBinding);
            iter.remove();
         }
      }
      return true;
   }

   public synchronized boolean destroyQueue(final String name) throws Exception
   {
      checkInitialised();
      undeployDestination(name);

      destinations.remove(name);
      jmsManagementService.unregisterQueue(name);
      server.getMessagingServerControl().destroyQueue(JBossQueue.createAddressFromName(name).toString());

      return true;
   }

   public synchronized boolean destroyTopic(final String name) throws Exception
   {
      checkInitialised();
      undeployDestination(name);

      destinations.remove(name);
      jmsManagementService.unregisterTopic(name);
      server.getMessagingServerControl().destroyQueue(JBossTopic.createAddressFromName(name).toString());

      return true;
   }

   public synchronized void createConnectionFactory(String name,
                                                    List<Pair<TransportConfiguration, TransportConfiguration>> connectorConfigs,
                                                    List<String> jndiBindings) throws Exception
   {
      checkInitialised();
      JBossConnectionFactory cf = connectionFactories.get(name);
      if (cf == null)
      {
         cf = new JBossConnectionFactory(connectorConfigs);
      }

      bindConnectionFactory(cf, name, jndiBindings);
   }

   public synchronized void createConnectionFactory(String name,
                                                    List<Pair<TransportConfiguration, TransportConfiguration>> connectorConfigs,
                                                    String clientID,
                                                    List<String> jndiBindings) throws Exception
   {
      checkInitialised();
      JBossConnectionFactory cf = connectionFactories.get(name);
      if (cf == null)
      {
         cf = new JBossConnectionFactory(connectorConfigs);
         cf.setClientID(clientID);
      }

      bindConnectionFactory(cf, name, jndiBindings);
   }

   public synchronized void createConnectionFactory(String name,
                                                    List<Pair<TransportConfiguration, TransportConfiguration>> connectorConfigs,
                                                    String clientID,
                                                    long clientFailureCheckPeriod,
                                                    long connectionTTL,
                                                    long callTimeout,
                                                    int maxConnections,
                                                    int minLargeMessageSize,
                                                    int consumerWindowSize,
                                                    int consumerMaxRate,
                                                    int producerWindowSize,
                                                    int producerMaxRate,
                                                    boolean blockOnAcknowledge,
                                                    boolean blockOnPersistentSend,
                                                    boolean blockOnNonPersistentSend,
                                                    boolean autoGroup,
                                                    boolean preAcknowledge,
                                                    String loadBalancingPolicyClassName,
                                                    int transactionBatchSize,
                                                    int dupsOKBatchSize,
                                                    boolean useGlobalPools,
                                                    int scheduledThreadPoolMaxSize,
                                                    int threadPoolMaxSize,
                                                    long retryInterval,
                                                    double retryIntervalMultiplier,
                                                    int reconnectAttempts,
                                                    boolean failoverOnServerShutdown,
                                                    List<String> jndiBindings) throws Exception
   {
      checkInitialised();
      JBossConnectionFactory cf = connectionFactories.get(name);
      if (cf == null)
      {
         cf = new JBossConnectionFactory(connectorConfigs);
         cf.setClientID(clientID);
         cf.setClientFailureCheckPeriod(clientFailureCheckPeriod);
         cf.setConnectionTTL(connectionTTL);
         cf.setCallTimeout(callTimeout);
         cf.setMaxConnections(maxConnections);
         cf.setMinLargeMessageSize(minLargeMessageSize);
         cf.setConsumerWindowSize(consumerWindowSize);
         cf.setConsumerMaxRate(consumerMaxRate);
         cf.setProducerWindowSize(producerWindowSize);
         cf.setProducerMaxRate(producerMaxRate);
         cf.setBlockOnAcknowledge(blockOnAcknowledge);
         cf.setBlockOnPersistentSend(blockOnPersistentSend);
         cf.setBlockOnNonPersistentSend(blockOnNonPersistentSend);
         cf.setAutoGroup(autoGroup);
         cf.setPreAcknowledge(preAcknowledge);
         cf.setConnectionLoadBalancingPolicyClassName(loadBalancingPolicyClassName);
         cf.setTransactionBatchSize(transactionBatchSize);
         cf.setDupsOKBatchSize(dupsOKBatchSize);
         cf.setUseGlobalPools(useGlobalPools);
         cf.setScheduledThreadPoolMaxSize(scheduledThreadPoolMaxSize);
         cf.setThreadPoolMaxSize(threadPoolMaxSize);
         cf.setRetryInterval(retryInterval);
         cf.setRetryIntervalMultiplier(retryIntervalMultiplier);
         cf.setReconnectAttempts(reconnectAttempts);
         cf.setFailoverOnServerShutdown(failoverOnServerShutdown);
      }

      bindConnectionFactory(cf, name, jndiBindings);
   }

   public synchronized void createConnectionFactory(String name,
                                                    String discoveryAddress,
                                                    int discoveryPort,
                                                    String clientID,
                                                    long discoveryRefreshTimeout,
                                                    long clientFailureCheckPeriod,
                                                    long connectionTTL,
                                                    long callTimeout,
                                                    int maxConnections,
                                                    int minLargeMessageSize,
                                                    int consumerWindowSize,
                                                    int consumerMaxRate,
                                                    int producerWindowSize,
                                                    int producerMaxRate,
                                                    boolean blockOnAcknowledge,
                                                    boolean blockOnPersistentSend,
                                                    boolean blockOnNonPersistentSend,
                                                    boolean autoGroup,
                                                    boolean preAcknowledge,
                                                    String loadBalancingPolicyClassName,
                                                    int transactionBatchSize,
                                                    int dupsOKBatchSize,
                                                    long initialWaitTimeout,
                                                    boolean useGlobalPools,
                                                    int scheduledThreadPoolMaxSize,
                                                    int threadPoolMaxSize,
                                                    long retryInterval,
                                                    double retryIntervalMultiplier,
                                                    int reconnectAttempts,
                                                    boolean failoverOnServerShutdown,
                                                    List<String> jndiBindings) throws Exception
   {
      checkInitialised();
      JBossConnectionFactory cf = connectionFactories.get(name);
      if (cf == null)
      {
         cf = new JBossConnectionFactory(discoveryAddress, discoveryPort);
         cf.setClientID(clientID);
         cf.setDiscoveryRefreshTimeout(discoveryRefreshTimeout);
         cf.setClientFailureCheckPeriod(clientFailureCheckPeriod);
         cf.setConnectionTTL(connectionTTL);
         cf.setCallTimeout(callTimeout);
         cf.setMaxConnections(maxConnections);
         cf.setMinLargeMessageSize(minLargeMessageSize);
         cf.setConsumerWindowSize(consumerWindowSize);
         cf.setConsumerMaxRate(consumerMaxRate);
         cf.setProducerWindowSize(producerWindowSize);
         cf.setProducerMaxRate(producerMaxRate);
         cf.setBlockOnAcknowledge(blockOnAcknowledge);
         cf.setBlockOnPersistentSend(blockOnPersistentSend);
         cf.setBlockOnNonPersistentSend(blockOnNonPersistentSend);
         cf.setAutoGroup(autoGroup);
         cf.setPreAcknowledge(preAcknowledge);
         cf.setConnectionLoadBalancingPolicyClassName(loadBalancingPolicyClassName);
         cf.setTransactionBatchSize(transactionBatchSize);
         cf.setDupsOKBatchSize(dupsOKBatchSize);
         cf.setDiscoveryInitialWaitTimeout(initialWaitTimeout);
         cf.setUseGlobalPools(useGlobalPools);
         cf.setScheduledThreadPoolMaxSize(scheduledThreadPoolMaxSize);
         cf.setThreadPoolMaxSize(threadPoolMaxSize);
         cf.setRetryInterval(retryInterval);
         cf.setRetryIntervalMultiplier(retryIntervalMultiplier);
         cf.setReconnectAttempts(reconnectAttempts);
         cf.setFailoverOnServerShutdown(failoverOnServerShutdown);
      }

      bindConnectionFactory(cf, name, jndiBindings);
   }

   public synchronized void createConnectionFactory(String name,
                                                    String discoveryAddress,
                                                    int discoveryPort,
                                                    List<String> jndiBindings) throws Exception
   {
      checkInitialised();
      JBossConnectionFactory cf = connectionFactories.get(name);
      if (cf == null)
      {
         cf = new JBossConnectionFactory(discoveryAddress, discoveryPort);
      }

      bindConnectionFactory(cf, name, jndiBindings);
   }

   public synchronized void createConnectionFactory(String name,
                                                    String discoveryAddress,
                                                    int discoveryPort,
                                                    String clientID,
                                                    List<String> jndiBindings) throws Exception
   {
      checkInitialised();
      JBossConnectionFactory cf = connectionFactories.get(name);
      if (cf == null)
      {
         cf = new JBossConnectionFactory(discoveryAddress, discoveryPort);
         cf.setClientID(clientID);
      }

      bindConnectionFactory(cf, name, jndiBindings);
   }

   public synchronized void createConnectionFactory(String name,
                                                    TransportConfiguration liveTC,
                                                    List<String> jndiBindings) throws Exception
   {
      checkInitialised();
      JBossConnectionFactory cf = connectionFactories.get(name);
      if (cf == null)
      {
         cf = new JBossConnectionFactory(liveTC);
      }

      bindConnectionFactory(cf, name, jndiBindings);
   }

   public synchronized void createConnectionFactory(String name,
                                                    TransportConfiguration liveTC,
                                                    String clientID,
                                                    List<String> jndiBindings) throws Exception
   {
      checkInitialised();
      JBossConnectionFactory cf = connectionFactories.get(name);
      if (cf == null)
      {
         cf = new JBossConnectionFactory(liveTC);
         cf.setClientID(clientID);
      }

      bindConnectionFactory(cf, name, jndiBindings);
   }

   public synchronized void createConnectionFactory(String name,
                                                    TransportConfiguration liveTC,
                                                    TransportConfiguration backupTC,
                                                    List<String> jndiBindings) throws Exception
   {
      checkInitialised();
      JBossConnectionFactory cf = connectionFactories.get(name);
      if (cf == null)
      {
         cf = new JBossConnectionFactory(liveTC, backupTC);
      }

      bindConnectionFactory(cf, name, jndiBindings);
   }

   public synchronized void createConnectionFactory(String name,
                                                    TransportConfiguration liveTC,
                                                    TransportConfiguration backupTC,
                                                    String clientID,
                                                    List<String> jndiBindings) throws Exception
   {
      checkInitialised();
      JBossConnectionFactory cf = connectionFactories.get(name);
      if (cf == null)
      {
         cf = new JBossConnectionFactory(liveTC, backupTC);
         cf.setClientID(clientID);
      }

      bindConnectionFactory(cf, name, jndiBindings);
   }

   public synchronized boolean destroyConnectionFactory(final String name) throws Exception
   {
      checkInitialised();
      List<String> jndiBindings = connectionFactoryBindings.get(name);
      if (jndiBindings == null || jndiBindings.size() == 0)
      {
         return false;
      }
      if (context != null)
      {
         for (String jndiBinding : jndiBindings)
         {
            try
            {
               context.unbind(jndiBinding);
            }
            catch (NameNotFoundException e)
            {
               // this is ok.
            }
         }
      }
      connectionFactoryBindings.remove(name);
      connectionFactories.remove(name);

      jmsManagementService.unregisterConnectionFactory(name);

      return true;
   }

   public String[] listRemoteAddresses() throws Exception
   {
      checkInitialised();
      return server.getMessagingServerControl().listRemoteAddresses();
   }

   public String[] listRemoteAddresses(final String ipAddress) throws Exception
   {
      checkInitialised();
      return server.getMessagingServerControl().listRemoteAddresses(ipAddress);
   }

   public boolean closeConnectionsForAddress(final String ipAddress) throws Exception
   {
      checkInitialised();
      return server.getMessagingServerControl().closeConnectionsForAddress(ipAddress);
   }

   public String[] listConnectionIDs() throws Exception
   {
      return server.getMessagingServerControl().listConnectionIDs();
   }

   public String[] listSessions(final String connectionID) throws Exception
   {
      checkInitialised();
      return server.getMessagingServerControl().listSessions(connectionID);
   }

   // Public --------------------------------------------------------

   // Private -------------------------------------------------------

   private synchronized void checkInitialised()
   {
      if (!active)
      {
         throw new IllegalStateException("Cannot access JMS Server, core server is not yet active");
      }
   }

   private void bindConnectionFactory(final JBossConnectionFactory cf,
                                      final String name,
                                      final List<String> jndiBindings) throws Exception
   {
      for (String jndiBinding : jndiBindings)
      {
         bindToJndi(jndiBinding, cf);

         if (connectionFactoryBindings.get(name) == null)
         {
            connectionFactoryBindings.put(name, new ArrayList<String>());
         }
         connectionFactoryBindings.get(name).add(jndiBinding);
      }

      jmsManagementService.registerConnectionFactory(name, cf, jndiBindings);
   }

   private boolean bindToJndi(final String jndiName, final Object objectToBind) throws NamingException
   {
      if (context != null)
      {
         String parentContext;
         String jndiNameInContext;
         int sepIndex = jndiName.lastIndexOf('/');
         if (sepIndex == -1)
         {
            parentContext = "";
         }
         else
         {
            parentContext = jndiName.substring(0, sepIndex);
         }
         jndiNameInContext = jndiName.substring(sepIndex + 1);
         try
         {
            context.lookup(jndiName);
   
            log.warn("Binding for " + jndiName + " already exists");
            return false;
         }
         catch (Throwable e)
         {
            // OK
         }
   
         Context c = org.jboss.messaging.utils.JNDIUtil.createContext(context, parentContext);
   
         c.rebind(jndiNameInContext, objectToBind);
      }   
      return true;
   }

   private void addToDestinationBindings(final String destination, final String jndiBinding)
   {
      if (destinations.get(destination) == null)
      {
         destinations.put(destination, new ArrayList<String>());
      }
      destinations.get(destination).add(jndiBinding);
   }
}
