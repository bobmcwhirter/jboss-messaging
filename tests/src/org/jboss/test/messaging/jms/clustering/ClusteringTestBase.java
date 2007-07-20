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

package org.jboss.test.messaging.jms.clustering;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.jms.client.FailoverEvent;
import org.jboss.jms.client.FailoverListener;
import org.jboss.jms.client.JBossConnection;
import org.jboss.jms.client.JBossConnectionFactory;
import org.jboss.jms.client.delegate.ClientClusteredConnectionFactoryDelegate;
import org.jboss.jms.client.delegate.DelegateSupport;
import org.jboss.jms.client.state.ConnectionState;
import org.jboss.test.messaging.MessagingTestCase;
import org.jboss.test.messaging.tools.ServerManagement;
import org.jboss.test.messaging.tools.jmx.ServiceAttributeOverrides;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

/**
 * @author <a href="mailto:tim.fox@jboss.org">Tim Fox</a>
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 * $Id$
 */
public class ClusteringTestBase extends MessagingTestCase
{
   // Constants ------------------------------------------------------------------------------------

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   protected int nodeCount;
   protected String config = "all";

   protected Context[] ic;
   protected Queue queue[];
   protected Topic topic[];
   
   protected ServiceAttributeOverrides overrides;

   // No need to have multiple connection factories since a clustered connection factory will create
   // connections in a round robin fashion on different servers.

   protected ConnectionFactory cf;

   // Constructors ---------------------------------------------------------------------------------

   public ClusteringTestBase(String name)
   {
      super(name);
   }
   
   // Public ---------------------------------------------------------------------------------------

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected int getFailoverNodeForNode(JBossConnectionFactory factory, int nodeID)
   {
   	Integer l = (Integer)((ClientClusteredConnectionFactoryDelegate)(factory.getDelegate())).getFailoverMap().get(new Integer(nodeID));
   	
      return l.intValue();
   }
   
   protected void setUp() throws Exception
   {
      super.setUp();

      if (nodeCount < 1)
      {
         throw new Exception("Node count not defined! Initalize nodeCount in the test's setUp()");
      }

      for (int i = 0; i < nodeCount; i++)
      {
         // make sure all servers are created and started; make sure that database is zapped
         // ONLY for the first server, the others rely on values they expect to find in shared
         // tables; don't clear the database for those.
         ServerManagement.start(i, config, overrides, i == 0);

         ServerManagement.deployQueue("testDistributedQueue", i);
         ServerManagement.deployTopic("testDistributedTopic", i);
      }


      lookups();

      drainQueues();
   }

   // Perform Context creation and lookups on queues and factories
   // Case a server is restarted, a test may want to recreate contexts in certain scenarios
   protected void lookups() throws Exception
   {
      ic = new Context[nodeCount];
      queue = new Queue[nodeCount];
      topic = new Topic[nodeCount];

      for (int i = 0; i < nodeCount; i++)
      {
      	log.info("Getting lookups for " + i);
         ic[i] = new InitialContext(ServerManagement.getJNDIEnvironment(i));
         queue[i] = (Queue)ic[i].lookup("queue/testDistributedQueue");
         topic[i] = (Topic)ic[i].lookup("topic/testDistributedTopic");
      }

      // We only need to lookup one connection factory since it will be clustered so we will
      // actually create connections on different servers (round robin).
      cf = (ConnectionFactory)ic[0].lookup("/ClusteredConnectionFactory");

   }

   protected void tearDown() throws Exception
   {
   	log.info("tearing down");
      for(int i = 0; i < nodeCount; i++)
      {
         if (ServerManagement.isStarted(i))
         {
         	//log.info("stopping server " + i);
            //ServerManagement.log(ServerManagement.INFO, "Undeploying Server " + i, i);
            ServerManagement.undeployQueue("testDistributedQueue", i);
            ServerManagement.undeployTopic("testDistributedTopic", i);
            //ServerManagement.stop(i);
         }

         ic[i].close();
      }

      super.tearDown();
   }

   protected String getLocatorURL(Connection conn)
   {
      return getConnectionState(conn).getRemotingConnection().
         getRemotingClient().getInvoker().getLocator().getLocatorURI();
   }

   protected String getObjectId(Connection conn)
   {
      return ((DelegateSupport) ((JBossConnection) conn).
         getDelegate()).getID();
   }

   protected ConnectionState getConnectionState(Connection conn)
   {
      return (ConnectionState) (((DelegateSupport) ((JBossConnection) conn).
         getDelegate()).getState());
   }

   

   protected void waitForFailoverComplete(int serverID, Connection conn1)
      throws Exception
   {

      assertEquals(serverID, ((JBossConnection)conn1).getServerID());

      // register a failover listener
      SimpleFailoverListener failoverListener = new SimpleFailoverListener();
      ((JBossConnection)conn1).registerFailoverListener(failoverListener);

      log.debug("killing node " + serverID + " ....");

      ServerManagement.kill(serverID);

      log.info("########");
      log.info("######## KILLED NODE " + serverID);
      log.info("########");

      // wait for the client-side failover to complete

      while (true)
      {
      	FailoverEvent event = failoverListener.getEvent(30000);
      	if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
      	{
      		break;
      	}
      	if (event == null)
      	{
      		fail("Did not get expected FAILOVER_COMPLETED event");
      	}
      }

      // failover complete
      log.info("failover completed");
   }



   /**
    * Lookup for the connection with the right serverID. I'm using this method to find the proper
    * serverId so I won't relay on loadBalancing policies on testcases.
    */
   protected Connection getConnection(Connection[] conn, int serverId) throws Exception
   {
      for(int i = 0; i < conn.length; i++)
      {
         ConnectionState state = (ConnectionState)(((DelegateSupport)((JBossConnection)conn[i]).
            getDelegate()).getState());

         if (state.getServerID() == serverId)
         {
            return conn[i];
         }
      }

      return null;
   }

   protected void checkConnectionsDifferentServers(Connection[] conn) throws Exception
   {
      int[] serverID = new int[conn.length];
      for(int i = 0; i < conn.length; i++)
      {
         ConnectionState state = (ConnectionState)(((DelegateSupport)((JBossConnection)conn[i]).
            getDelegate()).getState());
         serverID[i] = state.getServerID();
      }

      for(int i = 0; i < nodeCount; i++)
      {
         for(int j = 0; j < nodeCount; j++)
         {
            if (i == j)
            {
               continue;
            }

            if (serverID[i] == serverID[j])
            {
               fail("Connections " + i + " and " + j +
                  " are pointing to the same physical node (" + serverID[i] + ")");
            }
         }
      }
   }

   // Private --------------------------------------------------------------------------------------

   private void drainQueues() throws Exception
   {
      Connection[] conn = new Connection[nodeCount];

      try
      {
         // TODO This is a dangerous hack, relying on an arbitrary distribution algorithm
         // (round-robin in this case). If we want a connection to a specific node, we should be
         // able to look up something like "/ClusteredConnectionFactory0"
         
         for(int i = 0; i < nodeCount; i++)
         {
            conn[i] = cf.createConnection();
         }

         // Safety check, making sure we get connections to distinct nodes

         checkConnectionsDifferentServers(conn);

         for(int i = 0; i < nodeCount; i++)
         {
            Session s = conn[i].createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer c = s.createConsumer(queue[i]);
            conn[i].start();

            Message msg = null;
            do
            {
               msg = c.receive(1000);
               if (msg != null)
               {
                  log.info("Drained message " + msg + " on node " + i);
               }
            }
            while (msg != null);
         }
      }
      finally
      {
         for(int i = 0; i < nodeCount; i++)
         {
            if (conn[i] != null)
            {
               conn[i].close();
            }
         }
      }
   }

   // Inner classes --------------------------------------------------------------------------------
   
   protected class SimpleFailoverListener implements FailoverListener
   {
      private LinkedQueue buffer;

      public SimpleFailoverListener()
      {
         buffer = new LinkedQueue();
      }

      public void failoverEventOccured(FailoverEvent event)
      {
         try
         {
            buffer.put(event);
         }
         catch(InterruptedException e)
         {
            throw new RuntimeException("Putting thread interrupted while trying to add event " +
               "to buffer", e);
         }
      }

      /**
       * Blocks until a FailoverEvent is available or timeout occurs, in which case returns null.
       */
      public FailoverEvent getEvent(long timeout) throws InterruptedException
      {
         return (FailoverEvent)buffer.poll(timeout);
      }
   }

}
