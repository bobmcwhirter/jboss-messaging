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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jboss.messaging.core.logging.Logger;

/**
 * The RA default properties - these are set in the ra.xml file
 *
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 * @version $Revision: $
 */
public class JBMRAProperties implements Serializable
{
   /** Serial version UID */
   static final long serialVersionUID = -2772367477755473248L;

   /** The logger */
   private static final Logger log = Logger.getLogger(JBMRAProperties.class);

   /** Trace enabled */
   private static boolean trace = log.isTraceEnabled();

   /** The discovery group name */
   private String discoveryGroupAddress;

   /** The discovery group port */
   private Integer discoveryGroupPort;

   /** */
   private Long discoveryRefreshTimeout;

   /** */
   private Long discoveryInitialWaitTimeout;

   /** */
   private String connectionLoadBalancingPolicyClassName;

   /** */
   private Long clientFailureCheckPeriod;

   /** */
   private Long connectionTTL;

   /** */
   private Long callTimeout;

   /** */
   private Integer dupsOKBatchSize;

   /** */
   private Integer transactionBatchSize;

   /** */
   private Integer consumerWindowSize;

   /** */
   private Integer consumerMaxRate;

   /** */
   private Integer producerWindowSize;

   /** */
   private Integer producerMaxRate;

   /** */
   private Integer minLargeMessageSize;

   /** */
   private Boolean blockOnAcknowledge;

   /** */
   private Boolean blockOnNonPersistentSend;

   /** */
   private Boolean blockOnPersistentSend;

   /** */
   private Boolean autoGroup;

   /** */
   private Integer maxConnections;

   /** */
   private Boolean preAcknowledge;

   /** */
   private Long retryInterval;

   /** */
   private Double retryIntervalMultiplier;

   /** */
   private Integer reconnectAttempts;

   /** */
   private Boolean failoverOnServerShutdown;

   /** The user name */
   private String userName;

   /** The password */
   private String password;

   /** The client ID */
   private String clientID;

   /** Use XA */
   private Boolean useXA;

   /* the transport type*/
   private String connectorClassName;

   private Map<String, Object> connectionParameters = new HashMap<String, Object>();

   private Map<String, Object> backupConnectionParameters = new HashMap<String, Object>();

   private String backupConnectorClassName;

   /**
    * Constructor
    */
   public JBMRAProperties()
   {
      if (trace)
      {
         log.trace("constructor()");
      }
   }

   /**
    * Get the discovery group name
    * @return The value
    */
   public String getDiscoveryGroupAddress()
   {
      if (trace)
      {
         log.trace("getDiscoveryGroupAddress()");
      }

      return discoveryGroupAddress;
   }

   /**
    * Set the discovery group name
    * @param dgn The value
    */
   public void setDiscoveryGroupAddress(final String dgn)
   {
      if (trace)
      {
         log.trace("setDiscoveryGroupAddress(" + dgn + ")");
      }

      discoveryGroupAddress = dgn;
   }

   /**
    * Get the discovery group port
    * @return The value
    */
   public Integer getDiscoveryGroupPort()
   {
      if (trace)
      {
         log.trace("getDiscoveryGroupPort()");
      }

      return discoveryGroupPort;
   }

   /**
    * Set the discovery group port
    * @param dgp The value
    */
   public void setDiscoveryGroupPort(final Integer dgp)
   {
      if (trace)
      {
         log.trace("setDiscoveryGroupPort(" + dgp + ")");
      }

      discoveryGroupPort = dgp;
   }

   /**
    * Get discovery refresh timeout
    * @return The value
    */
   public Long getDiscoveryRefreshTimeout()
   {
      if (trace)
      {
         log.trace("getDiscoveryRefreshTimeout()");
      }

      return discoveryRefreshTimeout;
   }

   /**
    * Set discovery refresh timeout
    * @param discoveryRefreshTimeout The value
    */
   public void setDiscoveryRefreshTimeout(final Long discoveryRefreshTimeout)
   {
      if (trace)
      {
         log.trace("setDiscoveryRefreshTimeout(" + discoveryRefreshTimeout + ")");
      }

      this.discoveryRefreshTimeout = discoveryRefreshTimeout;
   }

   /**
    * Get discovery initial wait timeout
    * @return The value
    */
   public Long getDiscoveryInitialWaitTimeout()
   {
      if (trace)
      {
         log.trace("getDiscoveryInitialWaitTimeout()");
      }

      return discoveryInitialWaitTimeout;
   }

   /**
    * Set discovery initial wait timeout
    * @param discoveryInitialWaitTimeout The value
    */
   public void setDiscoveryInitialWaitTimeout(final Long discoveryInitialWaitTimeout)
   {
      if (trace)
      {
         log.trace("setDiscoveryInitialWaitTimeout(" + discoveryInitialWaitTimeout + ")");
      }

      this.discoveryInitialWaitTimeout = discoveryInitialWaitTimeout;
   }

   /**
    * Get load balancing policy class name
    * @return The value
    */
   public String getConnectionLoadBalancingPolicyClassName()
   {
      if (trace)
      {
         log.trace("getLoadBalancingPolicyClassName()");
      }

      return connectionLoadBalancingPolicyClassName;
   }

   /**
    * Set load balancing policy class name
    * @param connectionLoadBalancingPolicyClassName The value
    */
   public void setConnectionLoadBalancingPolicyClassName(final String loadBalancingPolicyClassName)
   {
      if (trace)
      {
         log.trace("setLoadBalancingPolicyClassName(" + loadBalancingPolicyClassName + ")");
      }

      this.connectionLoadBalancingPolicyClassName = loadBalancingPolicyClassName;
   }

   /**
    * Get client failure check period
    * @return The value
    */
   public Long getClientFailureCheckPeriod()
   {
      if (trace)
      {
         log.trace("getClientFailureCheckPeriod()");
      }

      return clientFailureCheckPeriod;
   }

   /**
    * Set client failure check period
    * @param clientFailureCheckPeriod The value
    */
   public void setClientFailureCheckPeriod(final Long clientFailureCheckPeriod)
   {
      if (trace)
      {
         log.trace("setClientFailureCheckPeriod(" + clientFailureCheckPeriod + ")");
      }

      this.clientFailureCheckPeriod = clientFailureCheckPeriod;
   }

   /**
    * Get connection TTL
    * @return The value
    */
   public Long getConnectionTTL()
   {
      if (trace)
      {
         log.trace("getConnectionTTL()");
      }

      return connectionTTL;
   }

   /**
    * Set connection TTL
    * @param connectionTTL The value
    */
   public void setConnectionTTL(final Long connectionTTL)
   {
      if (trace)
      {
         log.trace("setConnectionTTL(" + connectionTTL + ")");
      }

      this.connectionTTL = connectionTTL;
   }

   /**
    * Get call timeout
    * @return The value
    */
   public Long getCallTimeout()
   {
      if (trace)
      {
         log.trace("getCallTimeout()");
      }

      return callTimeout;
   }

   /**
    * Set call timeout
    * @param callTimeout The value
    */
   public void setCallTimeout(final Long callTimeout)
   {
      if (trace)
      {
         log.trace("setCallTimeout(" + callTimeout + ")");
      }

      this.callTimeout = callTimeout;
   }

   /**
    * Get dups ok batch size
    * @return The value
    */
   public Integer getDupsOKBatchSize()
   {
      if (trace)
      {
         log.trace("getDupsOKBatchSize()");
      }

      return dupsOKBatchSize;
   }

   /**
    * Set dups ok batch size
    * @param dupsOKBatchSize The value
    */
   public void setDupsOKBatchSize(final Integer dupsOKBatchSize)
   {
      if (trace)
      {
         log.trace("setDupsOKBatchSize(" + dupsOKBatchSize + ")");
      }

      this.dupsOKBatchSize = dupsOKBatchSize;
   }

   /**
    * Get transaction batch size
    * @return The value
    */
   public Integer getTransactionBatchSize()
   {
      if (trace)
      {
         log.trace("getTransactionBatchSize()");
      }

      return transactionBatchSize;
   }

   /**
    * Set transaction batch size
    * @param transactionBatchSize The value
    */
   public void setTransactionBatchSize(final Integer transactionBatchSize)
   {
      if (trace)
      {
         log.trace("setTransactionBatchSize(" + transactionBatchSize + ")");
      }

      this.transactionBatchSize = transactionBatchSize;
   }

   /**
    * Get consumer window size
    * @return The value
    */
   public Integer getConsumerWindowSize()
   {
      if (trace)
      {
         log.trace("getConsumerWindowSize()");
      }

      return consumerWindowSize;
   }

   /**
    * Set consumer window size
    * @param consumerWindowSize The value
    */
   public void setConsumerWindowSize(final Integer consumerWindowSize)
   {
      if (trace)
      {
         log.trace("setConsumerWindowSize(" + consumerWindowSize + ")");
      }

      this.consumerWindowSize = consumerWindowSize;
   }

   /**
    * Get consumer max rate
    * @return The value
    */
   public Integer getConsumerMaxRate()
   {
      if (trace)
      {
         log.trace("getConsumerMaxRate()");
      }

      return consumerMaxRate;
   }

   /**
    * Set consumer max rate
    * @param consumerMaxRate The value
    */
   public void setConsumerMaxRate(final Integer consumerMaxRate)
   {
      if (trace)
      {
         log.trace("setConsumerMaxRate(" + consumerMaxRate + ")");
      }

      this.consumerMaxRate = consumerMaxRate;
   }

   /**
    * Get producer window size
    * @return The value
    */
   public Integer getProducerWindowSize()
   {
      if (trace)
      {
         log.trace("getProducerWindowSize()");
      }

      return producerWindowSize;
   }

   /**
    * Set producer window size
    * @param producerWindowSize The value
    */
   public void setProducerWindowSize(final Integer producerWindowSize)
   {
      if (trace)
      {
         log.trace("setProducerWindowSize(" + producerWindowSize + ")");
      }

      this.producerWindowSize = producerWindowSize;
   }

   /**
    * Get producer max rate
    * @return The value
    */
   public Integer getProducerMaxRate()
   {
      if (trace)
      {
         log.trace("getProducerMaxRate()");
      }

      return producerMaxRate;
   }

   /**
    * Set producer max rate
    * @param producerMaxRate The value
    */
   public void setProducerMaxRate(final Integer producerMaxRate)
   {
      if (trace)
      {
         log.trace("setProducerMaxRate(" + producerMaxRate + ")");
      }

      this.producerMaxRate = producerMaxRate;
   }

   /**
    * Get min large message size
    * @return The value
    */
   public Integer getMinLargeMessageSize()
   {
      if (trace)
      {
         log.trace("getMinLargeMessageSize()");
      }

      return minLargeMessageSize;
   }

   /**
    * Set min large message size
    * @param minLargeMessageSize The value
    */
   public void setMinLargeMessageSize(final Integer minLargeMessageSize)
   {
      if (trace)
      {
         log.trace("setMinLargeMessageSize(" + minLargeMessageSize + ")");
      }

      this.minLargeMessageSize = minLargeMessageSize;
   }

   /**
    * Get block on acknowledge
    * @return The value
    */
   public Boolean getBlockOnAcknowledge()
   {
      if (trace)
      {
         log.trace("getBlockOnAcknowledge()");
      }

      return blockOnAcknowledge;
   }

   /**
    * Set block on acknowledge
    * @param blockOnAcknowledge The value
    */
   public void setBlockOnAcknowledge(final Boolean blockOnAcknowledge)
   {
      if (trace)
      {
         log.trace("setBlockOnAcknowledge(" + blockOnAcknowledge + ")");
      }

      this.blockOnAcknowledge = blockOnAcknowledge;
   }

   /**
    * Get block on non persistent send
    * @return The value
    */
   public Boolean getBlockOnNonPersistentSend()
   {
      if (trace)
      {
         log.trace("getBlockOnNonPersistentSend()");
      }

      return blockOnNonPersistentSend;
   }

   /**
    * Set block on non persistent send
    * @param blockOnNonPersistentSend The value
    */
   public void setBlockOnNonPersistentSend(final Boolean blockOnNonPersistentSend)
   {
      if (trace)
      {
         log.trace("setBlockOnNonPersistentSend(" + blockOnNonPersistentSend + ")");
      }

      this.blockOnNonPersistentSend = blockOnNonPersistentSend;
   }

   /**
    * Get block on persistent send
    * @return The value
    */
   public Boolean getBlockOnPersistentSend()
   {
      if (trace)
      {
         log.trace("getBlockOnPersistentSend()");
      }

      return blockOnPersistentSend;
   }

   /**
    * Set block on persistent send
    * @param blockOnPersistentSend The value
    */
   public void setBlockOnPersistentSend(final Boolean blockOnPersistentSend)
   {
      if (trace)
      {
         log.trace("setBlockOnPersistentSend(" + blockOnPersistentSend + ")");
      }

      this.blockOnPersistentSend = blockOnPersistentSend;
   }

   /**
    * Get auto group
    * @return The value
    */
   public Boolean getAutoGroup()
   {
      if (trace)
      {
         log.trace("getAutoGroup()");
      }

      return autoGroup;
   }

   /**
    * Set auto group
    * @param autoGroup The value
    */
   public void setAutoGroup(final Boolean autoGroup)
   {
      if (trace)
      {
         log.trace("setAutoGroup(" + autoGroup + ")");
      }

      this.autoGroup = autoGroup;
   }

   /**
    * Get max connections
    * @return The value
    */
   public Integer getMaxConnections()
   {
      if (trace)
      {
         log.trace("getMaxConnections()");
      }

      return maxConnections;
   }

   /**
    * Set max connections
    * @param maxConnections The value
    */
   public void setMaxConnections(final Integer maxConnections)
   {
      if (trace)
      {
         log.trace("setMaxConnections(" + maxConnections + ")");
      }

      this.maxConnections = maxConnections;
   }

   /**
    * Get pre acknowledge
    * @return The value
    */
   public Boolean getPreAcknowledge()
   {
      if (trace)
      {
         log.trace("getPreAcknowledge()");
      }

      return preAcknowledge;
   }

   /**
    * Set pre acknowledge
    * @param preAcknowledge The value
    */
   public void setPreAcknowledge(final Boolean preAcknowledge)
   {
      if (trace)
      {
         log.trace("setPreAcknowledge(" + preAcknowledge + ")");
      }

      this.preAcknowledge = preAcknowledge;
   }

   /**
    * Get retry interval
    * @return The value
    */
   public Long getRetryInterval()
   {
      if (trace)
      {
         log.trace("getRetryInterval()");
      }

      return retryInterval;
   }

   /**
    * Set retry interval
    * @param retryInterval The value
    */
   public void setRetryInterval(final Long retryInterval)
   {
      if (trace)
      {
         log.trace("setRetryInterval(" + retryInterval + ")");
      }

      this.retryInterval = retryInterval;
   }

   /**
    * Get retry interval multiplier
    * @return The value
    */
   public Double getRetryIntervalMultiplier()
   {
      if (trace)
      {
         log.trace("getRetryIntervalMultiplier()");
      }

      return retryIntervalMultiplier;
   }

   /**
    * Set retry interval multiplier
    * @param retryIntervalMultiplier The value
    */
   public void setRetryIntervalMultiplier(final Double retryIntervalMultiplier)
   {
      if (trace)
      {
         log.trace("setRetryIntervalMultiplier(" + retryIntervalMultiplier + ")");
      }

      this.retryIntervalMultiplier = retryIntervalMultiplier;
   }

   /**
    * Get reconnect attempts
    * @return The value
    */
   public Integer getReconnectAttempts()
   {
      if (trace)
      {
         log.trace("getReconnectAttempts()");
      }

      return reconnectAttempts;
   }

   /**
    * Set reconnect attempts
    * @param reconnectAttempts The value
    */
   public void setReconnectAttempts(final Integer reconnectAttempts)
   {
      if (trace)
      {
         log.trace("setReconnectAttempts(" + reconnectAttempts + ")");
      }

      this.reconnectAttempts = reconnectAttempts;
   }

   /**
    * Get failover on server shutdowns
    * @return The value
    */
   public Boolean isFailoverOnServerShutdown()
   {
      if (trace)
      {
         log.trace("isFailoverOnServerShutdown()");
      }

      return failoverOnServerShutdown;
   }

   /**
    * Set failover on server shutdown
    * @param failoverOnServerShutdown The value
    */
   public void setFailoverOnServerShutdown(final Boolean failoverOnServerShutdown)
   {
      if (trace)
      {
         log.trace("setFailoverOnServerShutdown(" + failoverOnServerShutdown + ")");
      }

      this.failoverOnServerShutdown = failoverOnServerShutdown;
   }

   /**
    * Get the user name
    * @return The value
    */
   public String getUserName()
   {
      if (trace)
      {
         log.trace("getUserName()");
      }

      return userName;
   }

   /**
    * Set the user name
    * @param userName The value
    */
   public void setUserName(final String userName)
   {
      if (trace)
      {
         log.trace("setUserName(" + userName + ")");
      }

      this.userName = userName;
   }

   /**
    * Get the password
    * @return The value
    */
   public String getPassword()
   {
      if (trace)
      {
         log.trace("getPassword()");
      }

      return password;
   }

   /**
    * Set the password
    * @param password The value
    */
   public void setPassword(final String password)
   {
      if (trace)
      {
         log.trace("setPassword(****)");
      }

      this.password = password;
   }

   /**
    * Get the client id
    * @return The value
    */
   public String getClientID()
   {
      if (trace)
      {
         log.trace("getClientID()");
      }

      return clientID;
   }

   /**
    * Set the client id
    * @param clientID The value
    */
   public void setClientID(final String clientID)
   {
      if (trace)
      {
         log.trace("setClientID(" + clientID + ")");
      }

      this.clientID = clientID;
   }

   /**
    * Get the use XA flag
    * @return The value
    */
   public Boolean getUseXA()
   {
      if (trace)
      {
         log.trace("getUseXA()");
      }

      return useXA;
   }

   /**
    * Set the use XA flag
    * @param xa The value
    */
   public void setUseXA(final Boolean xa)
   {
      if (trace)
      {
         log.trace("setUseXA(" + xa + ")");
      }

      useXA = xa;
   }

   /**
    * Use XA for communication
    * @return The value
    */
   public boolean isUseXA()
   {
      if (trace)
      {
         log.trace("isUseXA()");
      }

      if (useXA == null)
      {
         return false;
      }

      return useXA.booleanValue();
   }

   /**
    * Indicates whether some other object is "equal to" this one.
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

      if (obj instanceof JBMRAProperties)
      {
         JBMRAProperties you = (JBMRAProperties)obj;
         return Util.compare(discoveryGroupAddress, you.getDiscoveryGroupAddress()) && Util.compare(discoveryGroupPort,
                                                                                                    you.getDiscoveryGroupPort()) &&
                Util.compare(discoveryRefreshTimeout, you.getDiscoveryRefreshTimeout()) &&
                Util.compare(discoveryInitialWaitTimeout, you.getDiscoveryInitialWaitTimeout()) &&
                Util.compare(connectionLoadBalancingPolicyClassName, you.getConnectionLoadBalancingPolicyClassName()) &&
                Util.compare(clientFailureCheckPeriod, you.getClientFailureCheckPeriod()) &&
                Util.compare(connectionTTL, you.getConnectionTTL()) &&
                Util.compare(callTimeout, you.getCallTimeout()) &&
                Util.compare(dupsOKBatchSize, you.getDupsOKBatchSize()) &&
                Util.compare(transactionBatchSize, you.getTransactionBatchSize()) &&
                Util.compare(consumerWindowSize, you.getConsumerWindowSize()) &&
                Util.compare(consumerMaxRate, you.getConsumerMaxRate()) &&
                Util.compare(producerWindowSize, you.getProducerWindowSize()) &&
                Util.compare(producerMaxRate, you.getProducerMaxRate()) &&
                Util.compare(minLargeMessageSize, you.getMinLargeMessageSize()) &&
                Util.compare(blockOnAcknowledge, you.getBlockOnAcknowledge()) &&
                Util.compare(blockOnNonPersistentSend, you.getBlockOnNonPersistentSend()) &&
                Util.compare(blockOnPersistentSend, you.getBlockOnPersistentSend()) &&
                Util.compare(autoGroup, you.getAutoGroup()) &&
                Util.compare(maxConnections, you.getMaxConnections()) &&
                Util.compare(preAcknowledge, you.getPreAcknowledge()) &&
                Util.compare(retryInterval, you.getRetryInterval()) &&
                Util.compare(retryIntervalMultiplier, you.getRetryIntervalMultiplier()) &&
                Util.compare(reconnectAttempts, you.getReconnectAttempts()) &&
                Util.compare(failoverOnServerShutdown, you.isFailoverOnServerShutdown()) &&
                Util.compare(userName, you.getUserName()) &&
                Util.compare(password, you.getPassword()) &&
                Util.compare(clientID, you.getClientID()) &&
                Util.compare(useXA, you.getUseXA());
      }

      return false;
   }

   /**
    * Return the hash code for the object
    * @return The hash code
    */
   @Override
   public int hashCode()
   {
      if (trace)
      {
         log.trace("hashCode()");
      }

      int hash = 7;

      hash += 31 * hash + (discoveryGroupAddress != null ? discoveryGroupAddress.hashCode() : 0);
      hash += 31 * hash + (discoveryGroupPort != null ? discoveryGroupPort.hashCode() : 0);
      hash += 31 * hash + (discoveryRefreshTimeout != null ? discoveryRefreshTimeout.hashCode() : 0);
      hash += 31 * hash + (discoveryInitialWaitTimeout != null ? discoveryInitialWaitTimeout.hashCode() : 0);
      hash += 31 * hash + (connectionLoadBalancingPolicyClassName != null ? connectionLoadBalancingPolicyClassName.hashCode() : 0);
      hash += 31 * hash + (clientFailureCheckPeriod != null ? clientFailureCheckPeriod.hashCode() : 0);
      hash += 31 * hash + (connectionTTL != null ? connectionTTL.hashCode() : 0);
      hash += 31 * hash + (callTimeout != null ? callTimeout.hashCode() : 0);
      hash += 31 * hash + (dupsOKBatchSize != null ? dupsOKBatchSize.hashCode() : 0);
      hash += 31 * hash + (transactionBatchSize != null ? transactionBatchSize.hashCode() : 0);
      hash += 31 * hash + (consumerWindowSize != null ? consumerWindowSize.hashCode() : 0);
      hash += 31 * hash + (consumerMaxRate != null ? consumerMaxRate.hashCode() : 0);
      hash += 31 * hash + (producerWindowSize != null ? producerWindowSize.hashCode() : 0);
      hash += 31 * hash + (producerMaxRate != null ? producerMaxRate.hashCode() : 0);
      hash += 31 * hash + (minLargeMessageSize != null ? minLargeMessageSize.hashCode() : 0);
      hash += 31 * hash + (blockOnAcknowledge != null ? blockOnAcknowledge.hashCode() : 0);
      hash += 31 * hash + (blockOnNonPersistentSend != null ? blockOnNonPersistentSend.hashCode() : 0);
      hash += 31 * hash + (blockOnPersistentSend != null ? blockOnPersistentSend.hashCode() : 0);
      hash += 31 * hash + (autoGroup != null ? autoGroup.hashCode() : 0);
      hash += 31 * hash + (maxConnections != null ? maxConnections.hashCode() : 0);
      hash += 31 * hash + (preAcknowledge != null ? preAcknowledge.hashCode() : 0);
      hash += 31 * hash + (retryInterval != null ? retryInterval.hashCode() : 0);
      hash += 31 * hash + (retryIntervalMultiplier != null ? retryIntervalMultiplier.hashCode() : 0);
      hash += 31 * hash + (reconnectAttempts != null ? reconnectAttempts.hashCode() : 0);
      hash += 31 * hash + (failoverOnServerShutdown != null ? failoverOnServerShutdown.hashCode() : 0);
      hash += 31 * hash + (userName != null ? userName.hashCode() : 0);
      hash += 31 * hash + (password != null ? password.hashCode() : 0);
      hash += 31 * hash + (clientID != null ? clientID.hashCode() : 0);
      hash += 31 * hash + (useXA != null ? useXA.hashCode() : 0);
      hash += 31 * hash + (connectorClassName != null ? connectorClassName.hashCode() : 0);
      return hash;
   }

   public void setConnectorClassName(final String connectorClassName)
   {
      this.connectorClassName = connectorClassName;
   }

   public String getConnectorClassName()
   {
      return connectorClassName;
   }

   public void setConnectionParameters(final Map<String, Object> config)
   {
      connectionParameters = config;
   }

   public Map<String, Object> getConnectionParameters()
   {
      return connectionParameters;
   }

   public Map<String, Object> getBackupConnectionParameters()
   {
      return backupConnectionParameters;
   }

   public void setBackupConnectionParameters(final Map<String, Object> config)
   {
      backupConnectionParameters = config;
   }

   public String getBackupConnectorClassName()
   {
      return backupConnectorClassName;
   }

   public void setBackupConnectorClassName(final String connectorClassName)
   {
      backupConnectorClassName = connectorClassName;
   }
}
