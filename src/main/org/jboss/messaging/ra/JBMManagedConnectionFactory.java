/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.messaging.ra;

import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.jms.client.JBossConnectionFactory;

import javax.jms.ConnectionMetaData;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;

/**
 * JBM ManagedConectionFactory
 *
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>.
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 * @version $Revision: $
 */
public class JBMManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation
{
   /**
    * Serial version UID
    */
   static final long serialVersionUID = -1452379518562456741L;

   /**
    * The logger
    */
   private static final Logger log = Logger.getLogger(JBMManagedConnectionFactory.class);

   /**
    * Trace enabled
    */
   private static boolean trace = log.isTraceEnabled();

   /**
    * The resource adapter
    */
   private JBMResourceAdapter ra;

   /**
    * Connection manager
    */
   private ConnectionManager cm;

   /**
    * The managed connection factory properties
    */
   private final JBMMCFProperties mcfProperties;

   /**
    * Connection Factory used if properties are set
    */
   private JBossConnectionFactory connectionFactory;

   /**
    * Constructor
    */
   public JBMManagedConnectionFactory()
   {
      if (trace)
      {
         log.trace("constructor()");
      }

      ra = null;
      cm = null;
      mcfProperties = new JBMMCFProperties();
   }

   /**
    * Creates a Connection Factory instance
    *
    * @return javax.resource.cci.ConnectionFactory instance
    * @throws ResourceException Thrown if a connection factory cant be created
    */
   public Object createConnectionFactory() throws ResourceException
   {
      if (trace)
      {
         log.debug("createConnectionFactory()");
      }

      return createConnectionFactory(new JBMConnectionManager());
   }

   /**
    * Creates a Connection Factory instance
    *
    * @param cxManager The connection manager
    * @return javax.resource.cci.ConnectionFactory instance
    * @throws ResourceException Thrown if a connection factory cant be created
    */
   public Object createConnectionFactory(final ConnectionManager cxManager) throws ResourceException
   {
      if (trace)
      {
         log.trace("createConnectionFactory(" + cxManager + ")");
      }

      cm = cxManager;

      JBMConnectionFactory cf = new JBMConnectionFactoryImpl(this, cm);

      if (trace)
      {
         log.trace("Created connection factory: " + cf + ", using connection manager: " + cm);
      }

      return cf;
   }

   /**
    * Creates a new physical connection to the underlying EIS resource manager.
    *
    * @param subject       Caller's security information
    * @param cxRequestInfo Additional resource adapter specific connection request information
    * @return The managed connection
    * @throws ResourceException Thrown if a managed connection cant be created
    */
   public ManagedConnection createManagedConnection(final Subject subject, final ConnectionRequestInfo cxRequestInfo) throws ResourceException
   {
      if (trace)
      {
         log.trace("createManagedConnection(" + subject + ", " + cxRequestInfo + ")");
      }

      JBMConnectionRequestInfo cri = getCRI((JBMConnectionRequestInfo) cxRequestInfo);

      JBMCredential credential = JBMCredential.getCredential(this, subject, cri);

      if (trace)
      {
         log.trace("jms credential: " + credential);
      }

      JBMManagedConnection mc = new JBMManagedConnection(this, cri, credential.getUserName(), credential.getPassword());

      if (trace)
      {
         log.trace("created new managed connection: " + mc);
      }

      return mc;
   }

   /**
    * Returns a matched connection from the candidate set of connections.
    *
    * @param connectionSet The candidate connection set
    * @param subject       Caller's security information
    * @param cxRequestInfo Additional resource adapter specific connection request information
    * @return The managed connection
    * @throws ResourceException Thrown if no managed connection cant be found
    */
   public ManagedConnection matchManagedConnections(final Set connectionSet,
                                                    final Subject subject,
                                                    final ConnectionRequestInfo cxRequestInfo) throws ResourceException
   {
      if (trace)
      {
         log.trace("matchManagedConnections(" + connectionSet + ", " + subject + ", " + cxRequestInfo + ")");
      }

      JBMConnectionRequestInfo cri = getCRI((JBMConnectionRequestInfo) cxRequestInfo);
      JBMCredential credential = JBMCredential.getCredential(this, subject, cri);

      if (trace)
      {
         log.trace("Looking for connection matching credentials: " + credential);
      }

      Iterator connections = connectionSet.iterator();

      while (connections.hasNext())
      {
         Object obj = connections.next();

         if (obj instanceof JBMManagedConnection)
         {
            JBMManagedConnection mc = (JBMManagedConnection) obj;
            ManagedConnectionFactory mcf = mc.getManagedConnectionFactory();

            if ((mc.getUserName() == null || mc.getUserName() != null && mc.getUserName()
                  .equals(credential.getUserName())) && mcf.equals(this))
            {
               if (cri.equals(mc.getCRI()))
               {
                  if (trace)
                  {
                     log.trace("Found matching connection: " + mc);
                  }

                  return mc;
               }
            }
         }
      }

      if (trace)
      {
         log.trace("No matching connection was found");
      }

      return null;
   }

   /**
    * Set the log writer -- NOT SUPPORTED
    *
    * @param out The writer
    * @throws ResourceException Thrown if the writer cant be set
    */
   public void setLogWriter(final PrintWriter out) throws ResourceException
   {
      if (trace)
      {
         log.trace("setLogWriter(" + out + ")");
      }
   }

   /**
    * Get the log writer -- NOT SUPPORTED
    *
    * @return The writer
    * @throws ResourceException Thrown if the writer cant be retrieved
    */
   public PrintWriter getLogWriter() throws ResourceException
   {
      if (trace)
      {
         log.trace("getLogWriter()");
      }

      return null;
   }

   /**
    * Get the resource adapter
    *
    * @return The resource adapter
    */
   public ResourceAdapter getResourceAdapter()
   {
      if (trace)
      {
         log.trace("getResourceAdapter()");
      }

      return ra;
   }

   /**
    * Set the resource adapter
    *
    * @param ra The resource adapter
    * @throws ResourceException Thrown if incorrect resource adapter
    */
   public void setResourceAdapter(final ResourceAdapter ra) throws ResourceException
   {
      if (trace)
      {
         log.trace("setResourceAdapter(" + ra + ")");
      }

      if (ra == null || !(ra instanceof JBMResourceAdapter))
      {
         throw new ResourceException("Resource adapter is " + ra);
      }

      this.ra = (JBMResourceAdapter) ra;
   }

   /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @param obj Object with which to compare
    * @return True if this object is the same as the obj argument; false otherwise.
    */
   @Override
   public boolean equals(final Object obj)
   {
      if (trace)
      {
         log.trace("equals(" + obj + ")");
      }

      if (obj == null)
      {
         return false;
      }

      if (obj instanceof JBMManagedConnectionFactory)
      {
         JBMManagedConnectionFactory other = (JBMManagedConnectionFactory) obj;

         return mcfProperties.equals(other.getProperties()) && ra.equals(other.getResourceAdapter());
      }
      else
      {
         return false;
      }
   }

   /**
    * Return the hash code for the object
    *
    * @return The hash code
    */
   @Override
   public int hashCode()
   {
      if (trace)
      {
         log.trace("hashCode()");
      }

      int hash = mcfProperties.hashCode();
      hash += 31 * ra.hashCode();

      return hash;
   }

   /**
    * Get the default session type
    *
    * @return The value
    */
   public String getSessionDefaultType()
   {
      if (trace)
      {
         log.trace("getSessionDefaultType()");
      }

      return mcfProperties.getSessionDefaultType();
   }

   /**
    * Set the default session type
    *
    * @param type either javax.jms.Topic or javax.jms.Queue
    */
   public void setSessionDefaultType(final String type)
   {
      if (trace)
      {
         log.trace("setSessionDefaultType(" + type + ")");
      }

      mcfProperties.setSessionDefaultType(type);
   }

   /**
    * @return the connectionParameters
    */
   public String getConnectionParameters()
   {
      return mcfProperties.getStrConnectionParameters();
   }

   public void setConnectionParameters(final String configuration)
   {
      mcfProperties.setConnectionParameters(configuration);
   }

   public String getBackupConnectorClassName() {return mcfProperties.getBackupConnectorClassName();}

   public void setBackupConnectorClassName(String backupConnectorClassName)
   {
      mcfProperties.setBackupConnectorClassName(backupConnectorClassName);
   }

   public String getBackupConnectionParameters() {return mcfProperties.getBackupConnectionParameters();}

   public void setBackupConnectionParameters(String configuration)
   {
      mcfProperties.setBackupConnectionParameters(configuration);
   }

   /**
    * @return the transportType
    */
   public String getConnectorClassName()
   {
      return mcfProperties.getConnectorClassName();
   }

   public void setConnectorClassName(final String value)
   {
      mcfProperties.setConnectorClassName(value);
   }

   public String getConnectionLoadBalancingPolicyClassName()
   {
      return mcfProperties.getConnectionLoadBalancingPolicyClassName();
   }

   public void setConnectionLoadBalancingPolicyClassName(String connectionLoadBalancingPolicyClassName)
   {
      mcfProperties.setConnectionLoadBalancingPolicyClassName(connectionLoadBalancingPolicyClassName);
   }

   public String getDiscoveryAddress() {return mcfProperties.getDiscoveryAddress();}

   public void setDiscoveryAddress(String discoveryAddress)
   {
      mcfProperties.setDiscoveryAddress(discoveryAddress);
   }

   public Integer getDiscoveryPort()
   {
      return mcfProperties.getDiscoveryPort();
   }

   public void setDiscoveryPort(Integer discoveryPort)
   {
      mcfProperties.setDiscoveryPort(discoveryPort);
   }

   public Long getDiscoveryRefreshTimeout()
   {
      return mcfProperties.getDiscoveryRefreshTimeout();
   }

   public void setDiscoveryRefreshTimeout(
         Long discoveryRefreshTimeout)
   {
      mcfProperties.setDiscoveryRefreshTimeout(
            discoveryRefreshTimeout);
   }

   public Long getDiscoveryInitialWaitTimeout()
   {
      return mcfProperties.getDiscoveryInitialWaitTimeout();
   }

   public void setDiscoveryInitialWaitTimeout(Long discoveryInitialWaitTimeout)
   {
      mcfProperties.setDiscoveryInitialWaitTimeout(discoveryInitialWaitTimeout);
   }

   public String getClientID()
   {
      return mcfProperties.getClientID();
   }

   public void setClientID(String clientID)
   {
      mcfProperties.setClientID(clientID);
   }

   public Integer getDupsOKBatchSize()
   {
      return mcfProperties.getDupsOKBatchSize();
   }

   public void setDupsOKBatchSize(Integer dupsOKBatchSize)
   {
      mcfProperties.setDupsOKBatchSize(dupsOKBatchSize);
   }

   public Integer getTransactionBatchSize()
   {
      return mcfProperties.getTransactionBatchSize();
   }

   public void setTransactionBatchSize(Integer transactionBatchSize)
   {
      mcfProperties.setTransactionBatchSize(transactionBatchSize);
   }

   public Long getClientFailureCheckPeriod()
   {
      return mcfProperties.getClientFailureCheckPeriod();
   }

   public void setClientFailureCheckPeriod(Long clientFailureCheckPeriod)
   {
      mcfProperties.setClientFailureCheckPeriod(clientFailureCheckPeriod);
   }

   public Long getConnectionTTL()
   {
      return mcfProperties.getConnectionTTL();
   }

   public void setConnectionTTL(Long connectionTTL)
   {
      mcfProperties.setConnectionTTL(connectionTTL);
   }

   public Long getCallTimeout()
   {
      return mcfProperties.getCallTimeout();
   }

   public void setCallTimeout(Long callTimeout)
   {
      mcfProperties.setCallTimeout(callTimeout);
   }

   public Integer getConsumerWindowSize()
   {
      return mcfProperties.getConsumerWindowSize();
   }

   public void setConsumerWindowSize(Integer consumerWindowSize)
   {
      mcfProperties.setConsumerWindowSize(consumerWindowSize);
   }

   public Integer getConsumerMaxRate()
   {
      return mcfProperties.getConsumerMaxRate();
   }

   public void setConsumerMaxRate(Integer consumerMaxRate)
   {
      mcfProperties.setConsumerMaxRate(consumerMaxRate);
   }

   public Integer getProducerWindowSize()
   {
      return mcfProperties.getProducerWindowSize();
   }

   public void setProducerWindowSize(Integer producerWindowSize)
   {
      mcfProperties.setProducerWindowSize(producerWindowSize);
   }

   public Integer getProducerMaxRate()
   {
      return mcfProperties.getProducerMaxRate();
   }

   public void setProducerMaxRate(Integer producerMaxRate)
   {
      mcfProperties.setProducerMaxRate(producerMaxRate);
   }

   public Integer getMinLargeMessageSize()
   {
      return mcfProperties.getMinLargeMessageSize();
   }

   public void setMinLargeMessageSize(Integer minLargeMessageSize)
   {
      mcfProperties.setMinLargeMessageSize(minLargeMessageSize);
   }

   public Boolean isBlockOnAcknowledge()
   {
      return mcfProperties.isBlockOnAcknowledge();
   }

   public void setBlockOnAcknowledge(Boolean blockOnAcknowledge)
   {
      mcfProperties.setBlockOnAcknowledge(blockOnAcknowledge);
   }

   public Boolean isBlockOnNonPersistentSend() {return mcfProperties.isBlockOnNonPersistentSend();}

   public void setBlockOnNonPersistentSend(Boolean blockOnNonPersistentSend)
   {
      mcfProperties.setBlockOnNonPersistentSend(blockOnNonPersistentSend);
   }

   public Boolean isBlockOnPersistentSend() {return mcfProperties.isBlockOnPersistentSend();}

   public void setBlockOnPersistentSend(Boolean blockOnPersistentSend)
   {
      mcfProperties.setBlockOnPersistentSend(blockOnPersistentSend);
   }

   public Boolean isAutoGroup()
   {
      return mcfProperties.isAutoGroup();
   }

   public void setAutoGroup(Boolean autoGroup)
   {
      mcfProperties.setAutoGroup(autoGroup);
   }

   public Integer getMaxConnections()
   {
      return mcfProperties.getMaxConnections();
   }

   public void setMaxConnections(Integer maxConnections)
   {
      mcfProperties.setMaxConnections(maxConnections);
   }

   public Boolean isPreAcknowledge()
   {
      return mcfProperties.isPreAcknowledge();
   }

   public void setPreAcknowledge(Boolean preAcknowledge)
   {
      mcfProperties.setPreAcknowledge(preAcknowledge);
   }

   public Long getRetryInterval()
   {
      return mcfProperties.getRetryInterval();
   }

   public void setRetryInterval(Long retryInterval)
   {
      mcfProperties.setRetryInterval(retryInterval);
   }

   public Double getRetryIntervalMultiplier()
   {
      return mcfProperties.getRetryIntervalMultiplier();
   }

   public void setRetryIntervalMultiplier(Double retryIntervalMultiplier)
   {
      mcfProperties.setRetryIntervalMultiplier(retryIntervalMultiplier);
   }

   public Integer getReconnectAttempts()
   {
      return mcfProperties.getReconnectAttempts();
   }

   public void setReconnectAttempts(Integer reconnectAttempts)
   {
      mcfProperties.setReconnectAttempts(reconnectAttempts);
   }

   public Boolean isFailoverOnServerShutdown()
   {
      return mcfProperties.isFailoverOnServerShutdown();
   }

   public void setFailoverOnServerShutdown(Boolean failoverOnServerShutdown)
   {
      mcfProperties.setFailoverOnServerShutdown(failoverOnServerShutdown);
   }

   public Boolean isUseGlobalPools()
   {
      return mcfProperties.isUseGlobalPools();
   }

   public void setUseGlobalPools(Boolean useGlobalPools)
   {
      mcfProperties.setUseGlobalPools(useGlobalPools);
   }

   public Integer getScheduledThreadPoolMaxSize()
   {
      return mcfProperties.getScheduledThreadPoolMaxSize();
   }

   public void setScheduledThreadPoolMaxSize(Integer scheduledThreadPoolMaxSize)
   {
      mcfProperties.setScheduledThreadPoolMaxSize(scheduledThreadPoolMaxSize);
   }

   public Integer getThreadPoolMaxSize()
   {
      return mcfProperties.getThreadPoolMaxSize();
   }

   public void setThreadPoolMaxSize(Integer threadPoolMaxSize)
   {
      mcfProperties.setThreadPoolMaxSize(threadPoolMaxSize);
   }

   /**
    * Get the useTryLock.
    *
    * @return the useTryLock.
    */
   public Integer getUseTryLock()
   {
      if (trace)
      {
         log.trace("getUseTryLock()");
      }

      return mcfProperties.getUseTryLock();
   }

   /**
    * Set the useTryLock.
    *
    * @param useTryLock the useTryLock.
    */
   public void setUseTryLock(final Integer useTryLock)
   {
      if (trace)
      {
         log.trace("setUseTryLock(" + useTryLock + ")");
      }

      mcfProperties.setUseTryLock(useTryLock);
   }

   /**
    * Get the connection metadata
    *
    * @return The metadata
    */
   public ConnectionMetaData getMetaData()
   {
      if (trace)
      {
         log.trace("getMetadata()");
      }

      return new JBMConnectionMetaData();
   }

   /**
    * Get the JBoss connection factory
    *
    * @return The factory
    */
   protected synchronized JBossConnectionFactory getJBossConnectionFactory() throws ResourceException
   {
      /*if (mcfProperties.getConnectorClassName() != null)
      {
         if (connectionFactory == null)
         {
            connectionFactory = ra.createRemoteFactory(mcfProperties.getConnectorClassName(),
                                                       mcfProperties.getParsedConnectionParameters());
         }

         return connectionFactory;
      }
      else
      {
         return ra.getDefaultJBossConnectionFactory();
      }*/
      if(connectionFactory == null)
      {
         connectionFactory = ra.createJBossConnectionFactory(mcfProperties);
      }
      return connectionFactory;
   }

   /**
    * Get the managed connection factory properties
    *
    * @return The properties
    */
   protected JBMMCFProperties getProperties()
   {
      if (trace)
      {
         log.trace("getProperties()");
      }

      return mcfProperties;
   }

   /**
    * Get a connection request info instance
    *
    * @param info The instance that should be updated; may be <code>null</code>
    * @return The instance
    */
   private JBMConnectionRequestInfo getCRI(final JBMConnectionRequestInfo info)
   {
      if (trace)
      {
         log.trace("getCRI(" + info + ")");
      }

      if (info == null)
      {
         // Create a default one
         return new JBMConnectionRequestInfo(ra.getProperties(), mcfProperties.getType());
      }
      else
      {
         // Fill the one with any defaults
         info.setDefaults(ra.getProperties());
         return info;
      }
   }
}
