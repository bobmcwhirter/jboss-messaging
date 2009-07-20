/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.messaging.jms.server.management;

import static javax.management.MBeanOperationInfo.ACTION;
import static javax.management.MBeanOperationInfo.INFO;

import java.util.Map;

import org.jboss.messaging.core.management.Operation;
import org.jboss.messaging.core.management.Parameter;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * @version <tt>$Revision$</tt>
 * 
 */
public interface JMSServerControl
{
   // Attributes ----------------------------------------------------

   boolean isStarted();

   String getVersion();

   // Operations ----------------------------------------------------

   @Operation(desc = "Create a JMS Queue", impact = ACTION)
   boolean createQueue(@Parameter(name = "name", desc = "Name of the queue to create") String name,
                       @Parameter(name = "jndiBinding", desc = "the name of the binding for JNDI") String jndiBinding) throws Exception;

   @Operation(desc = "Destroy a JMS Queue", impact = ACTION)
   boolean destroyQueue(@Parameter(name = "name", desc = "Name of the queue to destroy") String name) throws Exception;

   @Operation(desc = "Create a JMS Topic", impact = ACTION)
   boolean createTopic(@Parameter(name = "name", desc = "Name of the topic to create") String name,
                       @Parameter(name = "jndiBinding", desc = "the name of the binding for JNDI") String jndiBinding) throws Exception;

   @Operation(desc = "Destroy a JMS Topic", impact = ACTION)
   boolean destroyTopic(@Parameter(name = "name", desc = "Name of the topic to destroy") String name) throws Exception;

   void createConnectionFactory(String name,
                                Object[] liveConnectorsTransportClassNames,
                                Object[] liveConnectorTransportParams,
                                Object[] backupConnectorsTransportClassNames,
                                Object[] backupConnectorTransportParams,
                                Object[] bindings) throws Exception;

   @Operation(desc = "Create a JMS ConnectionFactory", impact = ACTION)
   void createConnectionFactory(@Parameter(name = "name") String name,
                                @Parameter(name = "liveTransportClassNames", desc = "comma-separated list of class names for transport to live servers") String liveTransportClassNames,
                                @Parameter(name = "liveTransportParams", desc = "comma-separated list of key=value parameters for the live transports (enclosed between { } for each transport)") String liveTransportParams,
                                @Parameter(name = "backupTransportClassNames", desc = "comma-separated list of class names for transport to backup servers") String backupTransportClassNames,
                                @Parameter(name = "backupTransportParams", desc = "comma-separated list of key=value parameters for the backup transports (enclosed between { } for each transport)") String backupTransportParams,
                                @Parameter(name = "jndiBindings", desc = "comma-separated list of JNDI bindings") String jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                Object[] liveConnectorsTransportClassNames,
                                Object[] liveConnectorTransportParams,
                                Object[] backupConnectorsTransportClassNames,
                                Object[] backupConnectorTransportParams,
                                String clientID,
                                Object[] jndiBindings) throws Exception;

   @Operation(desc = "Create a JMS ConnectionFactory", impact = ACTION)
   void createConnectionFactory(@Parameter(name = "name") String name,
                                @Parameter(name = "liveTransportClassNames", desc = "comma-separated list of class names for transport to live servers") String liveTransportClassNames,
                                @Parameter(name = "liveTransportParams", desc = "comma-separated list of key=value parameters for the live transports (enclosed between { } for each transport)") String liveTransportParams,
                                @Parameter(name = "backupTransportClassNames", desc = "comma-separated list of class names for transport to backup servers") String backupTransportClassNames,
                                @Parameter(name = "backupTransportParams", desc = "comma-separated list of key=value parameters for the backup transports (enclosed between { } for each transport)") String backupTransportParams,
                                @Parameter(name = "clientID") String clientID,
                                @Parameter(name = "jndiBindings", desc = "comma-separated list of JNDI bindings") String jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                Object[] liveConnectorsTransportClassNames,
                                Object[] liveConnectorTransportParams,
                                Object[] backupConnectorsTransportClassNames,
                                Object[] backupConnectorTransportParams,
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
                                Object[] jndiBindings) throws Exception;

   void createConnectionFactory(@Parameter(name = "name") String name,
                                @Parameter(name = "liveTransportClassNames", desc = "comma-separated list of class names for transport to live servers") String liveTransportClassNames,
                                @Parameter(name = "liveTransportParams", desc = "comma-separated list of key=value parameters for the live transports (enclosed between { } for each transport)") String liveTransportParams,
                                @Parameter(name = "backupTransportClassNames", desc = "comma-separated list of class names for transport to backup servers") String backupTransportClassNames,
                                @Parameter(name = "backupTransportParams", desc = "comma-separated list of key=value parameters for the backup transports (enclosed between { } for each transport)") String backupTransportParams,
                                @Parameter(name = "clientID") String clientID,
                                @Parameter(name = "clientFailureCheckPeriod") long clientFailureCheckPeriod,
                                @Parameter(name = "connectionTTL") long connectionTTL,
                                @Parameter(name = "callTimeout") long callTimeout,
                                @Parameter(name = "maxConnections") int maxConnections,
                                @Parameter(name = "minLargeMessageSize") int minLargeMessageSize,
                                @Parameter(name = "consumerWindowSize") int consumerWindowSize,
                                @Parameter(name = "consumerMaxRate") int consumerMaxRate,
                                @Parameter(name = "producerWindowSize") int producerWindowSize,
                                @Parameter(name = "producerMaxRate") int producerMaxRate,
                                @Parameter(name = "blockOnAcknowledge") boolean blockOnAcknowledge,
                                @Parameter(name = "blockOnPersistentSend") boolean blockOnPersistentSend,
                                @Parameter(name = "blockOnNonPersistentSend") boolean blockOnNonPersistentSend,
                                @Parameter(name = "autoGroup") boolean autoGroup,
                                @Parameter(name = "preAcknowledge") boolean preAcknowledge,
                                @Parameter(name = "loadBalancingPolicyClassName") String loadBalancingPolicyClassName,
                                @Parameter(name = "transactionBatchSize") int transactionBatchSize,
                                @Parameter(name = "dupsOKBatchSize") int dupsOKBatchSize,
                                @Parameter(name = "useGlobalPools") boolean useGlobalPools,
                                @Parameter(name = "scheduledThreadPoolMaxSize") int scheduledThreadPoolMaxSize,
                                @Parameter(name = "threadPoolMaxSize") int threadPoolMaxSize,
                                @Parameter(name = "retryInterval") long retryInterval,
                                @Parameter(name = "retryIntervalMultiplier") double retryIntervalMultiplier,
                                @Parameter(name = "reconnectAttempts") int reconnectAttempts,
                                @Parameter(name = "failoverOnServerShutdown") boolean failoverOnServerShutdown,
                                @Parameter(name = "jndiBindings", desc = "comma-separated list of JNDI bindings") String jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                String discoveryAddress,
                                int discoveryPort,
                                String clientID,
                                Object[] bindings) throws Exception;

   @Operation(desc = "Create a JMS ConnectionFactory", impact = ACTION)
   void createConnectionFactory(@Parameter(name = "name") String name,
                                @Parameter(name = "discoveryAddress") String discoveryAddress,
                                @Parameter(name = "discoveryPort") int discoveryPort,
                                @Parameter(name = "clientID") String clientID,
                                @Parameter(name = "jndiBindings") String jndiBindings) throws Exception;

   void createConnectionFactory(String name,
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
                                Object[] jndiBindings) throws Exception;

   @Operation(desc = "Create a JMS ConnectionFactory", impact = ACTION)
   void createConnectionFactory(@Parameter(name = "name") String name,
                                @Parameter(name = "discoveryAddress") String discoveryAddress,
                                @Parameter(name = "discoveryPort") int discoveryPort,
                                @Parameter(name = "clientID") String clientID,
                                @Parameter(name = "discoveryRefreshTimeout") long discoveryRefreshTimeout,
                                @Parameter(name = "clientFailureCheckPeriod") long clientFailureCheckPeriod,
                                @Parameter(name = "connectionTTL") long connectionTTL,
                                @Parameter(name = "callTimeout") long callTimeout,
                                @Parameter(name = "maxConnections") int maxConnections,
                                @Parameter(name = "minLargeMessageSize") int minLargeMessageSize,
                                @Parameter(name = "consumerWindowSize") int consumerWindowSize,
                                @Parameter(name = "consumerMaxRate") int consumerMaxRate,
                                @Parameter(name = "producerWindowSize") int producerWindowSize,
                                @Parameter(name = "producerMaxRate") int producerMaxRate,
                                @Parameter(name = "blockOnAcknowledge") boolean blockOnAcknowledge,
                                @Parameter(name = "blockOnPersistentSend") boolean blockOnPersistentSend,
                                @Parameter(name = "blockOnNonPersistentSend") boolean blockOnNonPersistentSend,
                                @Parameter(name = "autoGroup") boolean autoGroup,
                                @Parameter(name = "preAcknowledge") boolean preAcknowledge,
                                @Parameter(name = "loadBalancingPolicyClassName") String loadBalancingPolicyClassName,
                                @Parameter(name = "transactionBatchSize") int transactionBatchSize,
                                @Parameter(name = "dupsOKBatchSize") int dupsOKBatchSize,
                                @Parameter(name = "initialWaitTimeout") long initialWaitTimeout,
                                @Parameter(name = "useGlobalPools") boolean useGlobalPools,
                                @Parameter(name = "scheduledThreadPoolMaxSize") int scheduledThreadPoolMaxSize,
                                @Parameter(name = "threadPoolMaxSize") int threadPoolMaxSize,
                                @Parameter(name = "retryInterval") long retryInterval,
                                @Parameter(name = "retryIntervalMultiplier") double retryIntervalMultiplier,
                                @Parameter(name = "reconnectAttempts") int reconnectAttempts,
                                @Parameter(name = "failoverOnServerShutdown") boolean failoverOnServerShutdown,
                                @Parameter(name = "jndiBindings", desc = "comma-separated list of JNDI bindings") String jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                String liveTransportClassName,
                                Map<String, Object> liveTransportParams,
                                Object[] jndiBindings) throws Exception;

   @Operation(desc = "Create a JMS ConnectionFactory", impact = ACTION)
   void createConnectionFactory(@Parameter(name = "name") String name,
                                @Parameter(name = "liveTransportClassName") String liveTransportClassName,
                                @Parameter(name = "liveTransportParams", desc = "comma-separated list of key=value for the transport parameters") String liveTransportParams,
                                @Parameter(name = "jndiBindings", desc = "comma-separated list of JNDI bindings") String jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                String liveTransportClassName,
                                Map<String, Object> liveTransportParams,
                                String clientID,
                                Object[] jndiBindings) throws Exception;

   @Operation(desc = "Create a JMS ConnectionFactory", impact = ACTION)
   void createConnectionFactory(@Parameter(name = "name") String name,
                                @Parameter(name = "liveTransportClassName") String liveTransportClassName,
                                @Parameter(name = "liveTransportParams", desc = "comma-separated list of key=value for the transport parameters") String liveTransportParams,
                                @Parameter(name = "clientID") String clientID,
                                @Parameter(name = "jndiBindings", desc = "comma-separated list of JNDI bindings") String jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                String liveTransportClassName,
                                Map<String, Object> liveTransportParams,
                                String backupTransportClassName,
                                Map<String, Object> backupTransportParams,
                                Object[] jndiBindings) throws Exception;

   void createConnectionFactory(String name,
                                String liveTransportClassName,
                                Map<String, Object> liveTransportParams,
                                String backupTransportClassName,
                                Map<String, Object> backupTransportParams,
                                String clientID,
                                Object[] jndiBindings) throws Exception;

   @Operation(desc = "Destroy a JMS ConnectionFactory", impact = ACTION)
   void destroyConnectionFactory(@Parameter(name = "name", desc = "Name of the ConnectionFactory to destroy") String name) throws Exception;

   @Operation(desc = "List the client addresses", impact = INFO)
   String[] listRemoteAddresses() throws Exception;

   @Operation(desc = "List the client addresses which match the given IP Address", impact = INFO)
   String[] listRemoteAddresses(@Parameter(desc = "an IP address", name = "ipAddress") String ipAddress) throws Exception;

   @Operation(desc = "Closes all the connections for the given IP Address", impact = INFO)
   boolean closeConnectionsForAddress(@Parameter(desc = "an IP address", name = "ipAddress") String ipAddress) throws Exception;

   @Operation(desc = "List all the connection IDs", impact = INFO)
   String[] listConnectionIDs() throws Exception;

   @Operation(desc = "List the sessions for the given connectionID", impact = INFO)
   String[] listSessions(@Parameter(desc = "a connection ID", name = "connectionID") String connectionID) throws Exception;
}
