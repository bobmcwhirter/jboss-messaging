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

package org.jboss.jms.client.container;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSException;

import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.jms.client.delegate.ClientBrowserDelegate;
import org.jboss.jms.client.delegate.ClientConnectionDelegate;
import org.jboss.jms.client.delegate.ClientConnectionFactoryDelegate;
import org.jboss.jms.client.delegate.ClientConsumerDelegate;
import org.jboss.jms.client.delegate.ClientProducerDelegate;
import org.jboss.jms.client.delegate.ClientSessionDelegate;
import org.jboss.jms.client.delegate.ClusteredClientConnectionFactoryDelegate;
import org.jboss.jms.client.delegate.DelegateSupport;
import org.jboss.jms.client.remoting.CallbackManager;
import org.jboss.jms.client.remoting.MessageCallbackHandler;
import org.jboss.jms.client.state.BrowserState;
import org.jboss.jms.client.state.ConnectionState;
import org.jboss.jms.client.state.ConsumerState;
import org.jboss.jms.client.state.HierarchicalStateSupport;
import org.jboss.jms.client.state.ProducerState;
import org.jboss.jms.client.state.SessionState;
import org.jboss.jms.destination.JBossDestination;
import org.jboss.jms.server.endpoint.CreateConnectionResult;
import org.jboss.jms.tx.AckInfo;
import org.jboss.jms.tx.ResourceManager;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;

/**
 * 
 * A HAAspect
 * 
 * There is one of these PER_INSTANCE of connection factory
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * 
 * @version <tt>$Revision: 1.1 $</tt>
 *
 * $Id$
 */
public class HAAspect
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(HAAspect.class);

   public static final int MAX_RECONNECT_HOP_COUNT = 10;

   // Static --------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   // Attributes ----------------------------------------------------

   private ClientConnectionFactoryDelegate[] delegates;

   private Map failoverMap;

   private int currentRobinIndex;

   // The identity of the delegate this interceptor is associated with
   private DelegateIdentity id;

   // Constructors --------------------------------------------------

   public HAAspect()
   {
      id = null;
   }

   // Public --------------------------------------------------------

   public Object handleCreateConnectionDelegate(Invocation invocation) throws Throwable
   {
      // maintain the identity of the delegate that sends invocation through this aspect, for
      // logging purposes. It makes sense, since it's an PER_INSTANCE aspect.
      if (id == null)
      {
         id = DelegateIdentity.getIdentity(invocation);
      }

      cacheLocalDelegates(invocation);

      if (delegates == null)
      {
         // not clustered, pass the invocation through
         if(trace) { log.trace(this + " detecting non-clustered connection creation request, letting it pass through"); }

         return invocation.invokeNext();
      }

      // clustered, stopping the invocation here and re-send it as non-clustered down the stack

      // TODO: this should be in loop while we get exceptions creating connections, always trying
      //       the next Delegate when we get an exception.

      // In a clustered configuration we create connections in a round-robin fashion, contacting
      // successively all available servers.

      ClientConnectionFactoryDelegate cfDelegate = getDelegateRoundRobin();

      if(trace) { log.trace(this + " detecting clustered connection creation request, choosing " + cfDelegate + " as target"); }

      // Now create a connection delegate for this

      MethodInvocation mi = (MethodInvocation)invocation;
      String username = (String)mi.getArguments()[0];
      String password = (String)mi.getArguments()[1];

      CreateConnectionResult res = (CreateConnectionResult)cfDelegate.
         createConnectionDelegate(username, password, -1);

      ClientConnectionDelegate cd = (ClientConnectionDelegate)res.getDelegate();

      if(trace) { log.trace(this + " got local connection delegate " + cd); }

      // Add a connection listener to detect failure; the consolidated remoting connection listener
      // must be already in place and configured

      ConnectionListener listener = new ConnectionFailureListener(cd);

      ((ConnectionState)((DelegateSupport)cd).getState()).
         getRemotingConnectionListener().addDelegateListener(listener);

      return new CreateConnectionResult(cd);
   }

   public String toString()
   {
      StringBuffer sb = new StringBuffer("HAAspect.");
      if (id == null)
      {
         sb.append("UNINITIALIZED");
      }
      else
      {
         sb.append(id.getType()).append("[").append(id.getID()).append("]");
      }
      return sb.toString();
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   private synchronized void cacheLocalDelegates(Invocation invocation)
   {
      if (delegates != null)
      {
         // the first set of delegates is already cached, return
         return;
      }

      MethodInvocation mi = (MethodInvocation)invocation;
      Object target = mi.getTargetObject();

      if (target instanceof ClusteredClientConnectionFactoryDelegate)
      {
         ClusteredClientConnectionFactoryDelegate cccfd =
            (ClusteredClientConnectionFactoryDelegate)target;

         delegates = cccfd.getDelegates();

         if (delegates != null)
         {
            failoverMap = cccfd.getFailoverMap();

            if (failoverMap == null)
            {
               throw new IllegalStateException("HAAspect cannot find the failover map!");
            }
         }
      }
   }

   //TODO this is currently hardcoded as round-robin, this should be made pluggable
   private synchronized ClientConnectionFactoryDelegate getDelegateRoundRobin()
   {
      ClientConnectionFactoryDelegate currentDelegate = delegates[currentRobinIndex++];

      if (currentRobinIndex >= delegates.length)
      {
         currentRobinIndex = 0;
      }

      return currentDelegate;
   }

   /**
    * @return a failover ClientConnectionFactoryDelegate or null if a suitable delegate cannot be
    *         found.
    */
   private ClientConnectionFactoryDelegate getFailoverDelegate(int failedServerID)
   {
      // Look up the server to failover onto in the failover map

      Integer failoverServerID = (Integer)failoverMap.get(new Integer(failedServerID));

      for (int i = 0; i < delegates.length; i++)
      {
         if (delegates[i].getServerID() == failoverServerID.intValue())
         {
            return delegates[i];
         }
      }

      return null;
   }

   private void handleConnectionFailure(ClientConnectionDelegate failedConnDelegate)
      throws Exception
   {
      log.debug(this + " handling failed connection " + failedConnDelegate);

      ConnectionState failedConnState =
         (ConnectionState)((DelegateSupport)failedConnDelegate).getState();

      int failedServerID = failedConnState.getServerID();

      // Get the default connection factory delegate we are going to failover onto

      ClientConnectionFactoryDelegate failoverDelegate = getFailoverDelegate(failedServerID);

      if (failoverDelegate == null)
      {
         throw new IllegalStateException("Cannot find default failover node for server " +
                                         failedServerID);
      }

      // We attempt to connect to the failover node in a loop, since we might need to go through
      // multiple hops

      int attemptCount = 0;

      outer: while (attemptCount < MAX_RECONNECT_HOP_COUNT)
      {
         // Create a connection using that connection factory
         CreateConnectionResult r = failoverDelegate.
            createConnectionDelegate(failedConnState.getUser(),
                                     failedConnState.getPassword(),
                                     failedConnState.getServerID());

         if (r.getDelegate() != null)
         {
            // We got the right server and created a new connection
            performClientSideFailover(failedConnDelegate, (ClientConnectionDelegate)r.getDelegate());
            return;
         }

         // Did not get a valid connection to the node we've just tried

         int actualServerID = r.getActualFailoverNodeID();

         if (actualServerID == -1)
         {
            // No failover attempt was detected on the server side; this might happen if the client
            // side network fails temporarily so the client connection breaks but the server cluster
            // is still up and running - in this case we don't perform failover.

            //TODO Is this the right thing to do?

            log.warn("Client attempted failover, but no failover attempt " +
                     "has been detected on the server side.");

            return;
         }

         // Server side failover has occurred / is occurring but trying to go to the 'default'
         // failover node did not succeed. Retry with the node suggested by the cluster.

         attemptCount++;

         for (int i = 0; i < delegates.length; i++)
         {
            if (delegates[i].getServerID() == actualServerID)
            {
               failoverDelegate = delegates[i];
               continue outer;
            }
         }

         // the delegate corresponding to the actualServerID not found among the cached delegates
         //TODO Could this ever happen? Should we send back the cf, or update it instead of just the id??
         throw new JMSException("Cannot find a cached connection factory delegate for " +
                                "node " + actualServerID);
      }

      throw new JMSException("Maximum number of failover attempts exceeded. " +
                             "Cannot find a server to failover onto.");
   }

   private void performClientSideFailover(ClientConnectionDelegate failedConnDelegate,
                                          ClientConnectionDelegate newConnDelegate) throws Exception
   {
      log.debug(this + " performing client side failover");

      ConnectionState failedState = (ConnectionState)failedConnDelegate.getState();
      ConnectionState newState = (ConnectionState)newConnDelegate.getState();

      if (failedState.getClientID() != null)
      {
         newConnDelegate.setClientID(failedState.getClientID());
      }

      // Transfer attributes from newDelegate to failedDelegate
      failedConnDelegate.copyAttributes(newConnDelegate);

      int oldServerId = failedState.getServerID();

      CallbackManager oldCallbackManager = failedState.getRemotingConnection().getCallbackManager();

      //We need to update some of the attributes on the state
      failedState.copyState(newState);

      for(Iterator i = failedState.getChildren().iterator(); i.hasNext(); )
      {
         SessionState failedSessionState = (SessionState)i.next();

         ClientSessionDelegate failedSessionDelegate =
            (ClientSessionDelegate)failedSessionState.getDelegate();

         ClientSessionDelegate newSessionDelegate = (ClientSessionDelegate)newConnDelegate.
            createSessionDelegate(failedSessionState.isTransacted(),
                                  failedSessionState.getAcknowledgeMode(),
                                  failedSessionState.isXA());

         SessionState newSessionState = (SessionState)newSessionDelegate.getState();

         failedSessionDelegate.copyAttributes(newSessionDelegate);

         //We need to update some of the attributes on the state
         newSessionState.copyState(newSessionState);

         if (trace) { log.trace("replacing session (" + failedSessionDelegate + ") with a new failover session " + newSessionDelegate); }

         List children = new ArrayList();

         // TODO Why is this clone necessary?
         children.addAll(failedSessionState.getChildren());

         Set consumerIds = new HashSet();

         for (Iterator j = children.iterator(); j.hasNext(); )
         {
            HierarchicalStateSupport sessionChild = (HierarchicalStateSupport)j.next();

            if (sessionChild instanceof ProducerState)
            {
               handleFailoverOnProducer((ProducerState)sessionChild, newSessionDelegate);
            }
            else if (sessionChild instanceof ConsumerState)
            {
               handleFailoverOnConsumer(failedConnDelegate,
                                        failedState,
                                        failedSessionState,
                                        (ConsumerState)sessionChild,
                                        failedSessionDelegate,
                                        oldServerId,
                                        oldCallbackManager);

               // We add the new consumer id to the list of old ids
               consumerIds.add(new Integer(((ConsumerState)sessionChild).getConsumerID()));
            }
            else if (sessionChild instanceof BrowserState)
            {
                handleFailoverOnBrowser((BrowserState)sessionChild, newSessionDelegate);
            }
         }

         /* Now we must sent the list of unacked AckInfos to the server - so the consumers
          * delivery lists can be repopulated
          */
         List ackInfos = null;

         if (!failedSessionState.isTransacted() && !failedSessionState.isXA())
         {
            /*
            Now we remove any unacked np messages - this is because we don't want to ack them
            since the server won't know about them and will barf
            */

            Iterator iter = newSessionState.getToAck().iterator();

            while (iter.hasNext())
            {
               AckInfo info = (AckInfo)iter.next();

               if (!info.getMessage().getMessage().isReliable())
               {
                  iter.remove();
               }
            }

            //Get the ack infos from the list in the session state
            ackInfos = failedSessionState.getToAck();
         }
         else
         {
            //Transacted session - we need to get the acks from the resource manager
            //btw we have kept the old resource manager
            ResourceManager rm = failedState.getResourceManager();

            // Remove any non persistent acks - so server doesn't barf on commit

            rm.removeNonPersistentAcks(consumerIds);

            ackInfos = rm.getAckInfosForConsumerIds(consumerIds);
         }

         if (!ackInfos.isEmpty())
         {
            log.info("Sending " + ackInfos.size() + " unacked");
            newSessionDelegate.sendUnackedAckInfos(ackInfos);
         }
      }

      //TODO
      //If the session had consumers which are now closed then there is no way to recreate them on the server
      //we need to store with session id

      // We must not start the connection until the end
      if (failedState.isStarted())
      {
         failedConnDelegate.start();
      }

      log.info(this + " completed client-side failover");
   }

   private void handleFailoverOnConsumer(ClientConnectionDelegate failedConnectionDelegate,
                                         ConnectionState failedConnectionState,
                                         SessionState failedSessionState,
                                         ConsumerState failedConsumerState,
                                         ClientSessionDelegate failedSessionDelegate,
                                         int oldServerID,
                                         CallbackManager oldCallbackManager)
      throws JMSException
   {
      log.debug(this + " failing over consumer " + failedConsumerState);

      ClientConsumerDelegate failedConsumerDelegate =
         (ClientConsumerDelegate)failedConsumerState.getDelegate();

      if (trace) { log.trace(this + " creating alternate consumer"); }

      ClientConsumerDelegate newConsumerDelegate = (ClientConsumerDelegate)failedSessionDelegate.
         createConsumerDelegate((JBossDestination)failedConsumerState.getDestination(),
                                 failedConsumerState.getSelector(),
                                 failedConsumerState.isNoLocal(),
                                 failedConsumerState.getSubscriptionName(),
                                 failedConsumerState.isConnectionConsumer(),
                                 failedConsumerState.getChannelId());

      if (trace) { log.trace(this + " alternate consumer created"); }

      // Copy the attributes from the new consumer to the old consumer
      failedConsumerDelegate.copyAttributes(newConsumerDelegate);

      ConsumerState newState = (ConsumerState)newConsumerDelegate.getState();

      int oldConsumerID = failedConsumerState.getConsumerID();

      // Update attributes on the old state
      failedConsumerState.copyState(newState);

      if (failedSessionState.isTransacted() || failedSessionState.isXA())
      {
         // Replace the old consumer id with the new consumer id

         ResourceManager rm = failedConnectionState.getResourceManager();

         rm.handleFailover(oldConsumerID, failedConsumerState.getConsumerID());
      }

      // We need to re-use the existing message callback handler

      MessageCallbackHandler oldHandler =
         oldCallbackManager.unregisterHandler(oldServerID, oldConsumerID);

      ConnectionState newConnectionState = (ConnectionState)failedConnectionDelegate.getState();

      CallbackManager newCallbackManager =
         newConnectionState.getRemotingConnection().getCallbackManager();

      // Remove the new handler
      MessageCallbackHandler newHandler = newCallbackManager.
         unregisterHandler(newConnectionState.getServerID(), newState.getConsumerID());

      log.debug("New handler is " + System.identityHashCode(newHandler));

      //But we need to update some fields from the new one
      oldHandler.copyState(newHandler);

      //Now we re-register the old handler with the new callback manager

      newCallbackManager.registerHandler(newConnectionState.getServerID(),
                                         newState.getConsumerID(),
                                         oldHandler);

      // We don't need to add the handler to the session state since it is already there - we
      // are re-using the old handler

      log.debug(this + " failed over consumer");
   }


   private void handleFailoverOnProducer(ProducerState failedProducerState,
                                         ClientSessionDelegate failedSessionDelegate)
      throws JMSException
   {
      ClientProducerDelegate newProducerDelegate = (ClientProducerDelegate)failedSessionDelegate.
         createProducerDelegate((JBossDestination)failedProducerState.getDestination());

      ClientProducerDelegate failedProducerDelegate =
         (ClientProducerDelegate)failedProducerState.getDelegate();

      failedProducerDelegate.copyAttributes(newProducerDelegate);
      failedProducerState.copyState((ProducerState)newProducerDelegate.getState());

      if (trace) { log.trace("handling fail over on producerDelegate " + failedProducerDelegate + " destination=" + failedProducerState.getDestination()); }
   }

   private void handleFailoverOnBrowser(BrowserState failedBrowserState,
                                         ClientSessionDelegate failedSessionDelegate)
      throws JMSException
   {
      ClientBrowserDelegate newBrowserDelegate = (ClientBrowserDelegate)failedSessionDelegate.
         createBrowserDelegate(failedBrowserState.getJmsDestination(),
                               failedBrowserState.getMessageSelector());

      ClientBrowserDelegate failedBrowserDelegate =
         (ClientBrowserDelegate)failedBrowserState.getDelegate();

      failedBrowserDelegate.copyAttributes(newBrowserDelegate);
      failedBrowserState.copyState((BrowserState)newBrowserDelegate.getState());

      if (trace) { log.trace("handling fail over on browserDelegate " + failedBrowserDelegate + " destination=" + failedBrowserState.getJmsDestination() + " selector=" + failedBrowserState.getMessageSelector()); }

   }

   // Inner classes -------------------------------------------------

   private class ConnectionFailureListener implements ConnectionListener
   {
      private ClientConnectionDelegate cd;

      ConnectionFailureListener(ClientConnectionDelegate cd)
      {
         this.cd = cd;
      }

      // ConnectionListener implementation ---------------------------

      public void handleConnectionException(Throwable throwable, Client client)
      {
         try
         {
            log.debug(this + " is being notified of connection failure: " + throwable);
            handleConnectionFailure(cd);
         }
         catch (Throwable e)
         {
            log.error("Caught exception in handling failure", e);
         }
      }

      public String toString()
      {
         return "ConnectionFailureListener[" + cd + "]";
      }
   }
}


