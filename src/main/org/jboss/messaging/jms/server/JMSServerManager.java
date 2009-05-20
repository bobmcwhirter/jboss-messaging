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

package org.jboss.messaging.jms.server;

import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.server.MessagingComponent;
import org.jboss.messaging.utils.Pair;

import javax.naming.Context;
import java.util.List;

/**
 * The JMS Management interface.
 * 
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 */
public interface JMSServerManager extends MessagingComponent
{
   String getVersion();

   /**
    * Has the Server been started.
    * 
    * @return true if the server us running
    */
   boolean isStarted();

   /**
    * Creates a JMS Queue.
    * 
    * @param queueName
    *           The name of the queue to create
    * @param jndiBinding
    *           the name of the binding for JNDI
    * @param selectorString
    * @param durable
    * @return true if the queue is created or if it existed and was added to
    *         JNDI
    * @throws Exception
    *            if problems were encountered creating the queue.
    */
   boolean createQueue(String queueName, String jndiBinding, String selectorString, boolean durable) throws Exception;

   /**
    * Creates a JMS Topic
    * 
    * @param topicName
    *           the name of the topic
    * @param jndiBinding
    *           the name of the binding for JNDI
    * @return true if the topic was created or if it existed and was added to
    *         JNDI
    * @throws Exception
    *            if a problem occurred creating the topic
    */
   boolean createTopic(String topicName, String jndiBinding) throws Exception;

   /**
    * Remove the destination from JNDI.
    * Calling this method does <em>not</em> destroy the destination.
    * 
    * @param name
    *           the name of the destination to remove from JNDI
    * @return true if removed
    * @throws Exception
    *            if a problem occurred removing the destination
    */
   boolean undeployDestination(String name) throws Exception;

   /**
    * destroys a queue and removes it from JNDI
    * 
    * @param name
    *           the name of the queue to destroy
    * @return true if destroyed
    * @throws Exception
    *            if a problem occurred destroying the queue
    */
   boolean destroyQueue(String name) throws Exception;

   /**
    * destroys a topic and removes it from JNDI
    * 
    * @param name
    *           the name of the topic to destroy
    * @return true if the topic was destroyed
    * @throws Exception
    *            if a problem occurred destroying the topic
    */
   boolean destroyTopic(String name) throws Exception;

   void createConnectionFactory(String name, String discoveryAddress, int discoveryPort, List<String> jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                List<Pair<TransportConfiguration, TransportConfiguration>> connectorConfigs,
                                List<String> jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                TransportConfiguration liveTC,
                                TransportConfiguration backupTC,
                                List<String> jndiBindings) throws Exception;

   void createConnectionFactory(String name, TransportConfiguration liveTC, List<String> jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                String discoveryAddress,
                                int discoveryPort,
                                String clientID,
                                List<String> jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                List<Pair<TransportConfiguration, TransportConfiguration>> connectorConfigs,
                                String clientID,
                                List<String> jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                TransportConfiguration liveTC,
                                TransportConfiguration backupTC,
                                String clientID,
                                List<String> jndiBindings) throws Exception;

   void createConnectionFactory(String name, TransportConfiguration liveTC, String clientID, List<String> jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                List<Pair<TransportConfiguration, TransportConfiguration>> connectorConfigs,
                                String clientID,
                                long pingPeriod,
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
                                List<String> jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                String discoveryAddress,
                                int discoveryPort,
                                String clientID,
                                long discoveryRefreshTimeout,
                                long pingPeriod,
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
                                List<String> jndiBindings) throws Exception;

   /**
    * destroys a connection factory.
    * 
    * @param name
    *           the name of the connection factory to destroy
    * @return true if the connection factory was destroyed
    * @throws Exception
    *            if a problem occurred destroying the connection factory
    */
   boolean destroyConnectionFactory(String name) throws Exception;

   String[] listRemoteAddresses() throws Exception;

   String[] listRemoteAddresses(String ipAddress) throws Exception;

   boolean closeConnectionsForAddress(String ipAddress) throws Exception;

   String[] listConnectionIDs() throws Exception;

   String[] listSessions(String connectionID) throws Exception;

   void setContext(final Context context);
}
