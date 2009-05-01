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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;
import javax.jms.TopicSession;
import javax.jms.XAQueueSession;
import javax.jms.XASession;
import javax.jms.XATopicSession;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.spi.ConnectionManager;

import org.jboss.messaging.core.logging.Logger;

/**
 * Implements the JMS Connection API and produces {@link JBMSession} objects.
 *
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @version $Revision:  $
 */
public class JBMSessionFactoryImpl implements JBMSessionFactory, Referenceable
{
   /** The logger */
   private static final Logger log = Logger.getLogger(JBMSessionFactoryImpl.class);

   /** Trace enabled */
   private static boolean trace = log.isTraceEnabled();

   /** Are we closed? */
   private boolean closed = false;

   /** The naming reference */
   private Reference reference;

   /** The user name */
   private String userName;

   /** The password */
   private String password;

   /** The client ID */
   private String clientID;

   /** The connection type */
   private final int type;

   /** Whether we are started */
   private boolean started = false;

   /** The managed connection factory */
   private final JBMManagedConnectionFactory mcf;

   /** The connection manager */
   private ConnectionManager cm;

   /** The sessions */
   private final Set sessions = new HashSet();

   /** The temporary queues */
   private final Set tempQueues = new HashSet();

   /** The temporary topics */
   private final Set tempTopics = new HashSet();

   /**
    * Constructor
    * @param mcf The managed connection factory
    * @param cm The connection manager
    * @param type The connection type
    */
   public JBMSessionFactoryImpl(final JBMManagedConnectionFactory mcf, final ConnectionManager cm, final int type)
   {
      this.mcf = mcf;

      if (cm == null)
      {
         this.cm = new JBMConnectionManager();
      }
      else
      {
         this.cm = cm;
      }

      this.type = type;

      if (trace)
      {
         log.trace("constructor(" + mcf + ", " + cm + ", " + type);
      }
   }

   /**
    * Set the naming reference
    * @param reference The reference
    */
   public void setReference(final Reference reference)
   {
      if (trace)
      {
         log.trace("setReference(" + reference + ")");
      }

      this.reference = reference;
   }

   /**
    * Get the naming reference
    * @return The reference
    */
   public Reference getReference()
   {
      if (trace)
      {
         log.trace("getReference()");
      }

      return reference;
   }

   /**
    * Set the user name
    * @param name The user name
    */
   public void setUserName(final String name)
   {
      if (trace)
      {
         log.trace("setUserName(" + name + ")");
      }

      userName = name;
   }

   /**
    * Set the password
    * @param password The password
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
    * Get the client ID
    * @return The client ID
    * @exception JMSException Thrown if an error occurs
    */
   public String getClientID() throws JMSException
   {
      if (trace)
      {
         log.trace("getClientID()");
      }

      checkClosed();

      if (clientID == null)
      {
         return ((JBMResourceAdapter)mcf.getResourceAdapter()).getProperties().getClientID();
      }

      return clientID;
   }

   /**
    * Set the client ID -- throws IllegalStateException
    * @param cID The client ID
    * @exception JMSException Thrown if an error occurs
    */
   public void setClientID(final String cID) throws JMSException
   {
      if (trace)
      {
         log.trace("setClientID(" + cID + ")");
      }

      throw new IllegalStateException(ISE);
   }

   /**
    * Create a queue session
    * @param transacted Use transactions
    * @param acknowledgeMode The acknowledge mode
    * @return The queue session
    * @exception JMSException Thrown if an error occurs
    */
   public QueueSession createQueueSession(final boolean transacted, final int acknowledgeMode) throws JMSException
   {
      if (trace)
      {
         log.trace("createQueueSession(" + transacted + ", " + acknowledgeMode + ")");
      }

      checkClosed();

      if (type == JBMConnectionFactory.TOPIC_CONNECTION || type == JBMConnectionFactory.XA_TOPIC_CONNECTION)
      {
         throw new IllegalStateException("Can not get a queue session from a topic connection");
      }

      return allocateConnection(transacted, acknowledgeMode, type);
   }

   /**
    * Create a XA queue session
    * @return The XA queue session
    * @exception JMSException Thrown if an error occurs
    */
   public XAQueueSession createXAQueueSession() throws JMSException
   {
      if (trace)
      {
         log.trace("createXAQueueSession()");
      }

      checkClosed();

      if (type == JBMConnectionFactory.CONNECTION || type == JBMConnectionFactory.TOPIC_CONNECTION ||
          type == JBMConnectionFactory.XA_TOPIC_CONNECTION)
      {
         throw new IllegalStateException("Can not get a topic session from a queue connection");
      }

      return allocateConnection(type);
   }

   /**
    * Create a connection consumer -- throws IllegalStateException
    * @param queue The queue
    * @param messageSelector The message selector
    * @param sessionPool The session pool
    * @param maxMessages The number of max messages
    * @return The connection consumer
    * @exception JMSException Thrown if an error occurs
    */
   public ConnectionConsumer createConnectionConsumer(final Queue queue,
                                                      final String messageSelector,
                                                      final ServerSessionPool sessionPool,
                                                      final int maxMessages) throws JMSException
   {
      if (trace)
      {
         log.trace("createConnectionConsumer(" + queue +
                   ", " +
                   messageSelector +
                   ", " +
                   sessionPool +
                   ", " +
                   maxMessages +
                   ")");
      }

      throw new IllegalStateException(ISE);
   }

   /**
    * Create a topic session
    * @param transacted Use transactions
    * @param acknowledgeMode The acknowledge mode
    * @return The topic session
    * @exception JMSException Thrown if an error occurs
    */
   public TopicSession createTopicSession(final boolean transacted, final int acknowledgeMode) throws JMSException
   {
      if (trace)
      {
         log.trace("createTopicSession(" + transacted + ", " + acknowledgeMode + ")");
      }

      checkClosed();

      if (type == JBMConnectionFactory.QUEUE_CONNECTION || type == JBMConnectionFactory.XA_QUEUE_CONNECTION)
      {
         throw new IllegalStateException("Can not get a topic session from a queue connection");
      }

      return allocateConnection(transacted, acknowledgeMode, type);
   }

   /**
    * Create a XA topic session
    * @return The XA topic session
    * @exception JMSException Thrown if an error occurs
    */
   public XATopicSession createXATopicSession() throws JMSException
   {
      if (trace)
      {
         log.trace("createXATopicSession()");
      }

      checkClosed();

      if (type == JBMConnectionFactory.CONNECTION || type == JBMConnectionFactory.QUEUE_CONNECTION ||
          type == JBMConnectionFactory.XA_QUEUE_CONNECTION)
      {
         throw new IllegalStateException("Can not get a topic session from a queue connection");
      }

      return allocateConnection(type);
   }

   /**
    * Create a connection consumer -- throws IllegalStateException
    * @param topic The topic
    * @param messageSelector The message selector
    * @param sessionPool The session pool
    * @param maxMessages The number of max messages
    * @return The connection consumer
    * @exception JMSException Thrown if an error occurs
    */
   public ConnectionConsumer createConnectionConsumer(final Topic topic,
                                                      final String messageSelector,
                                                      final ServerSessionPool sessionPool,
                                                      final int maxMessages) throws JMSException
   {
      if (trace)
      {
         log.trace("createConnectionConsumer(" + topic +
                   ", " +
                   messageSelector +
                   ", " +
                   sessionPool +
                   ", " +
                   maxMessages +
                   ")");
      }

      throw new IllegalStateException(ISE);
   }

   /**
    * Create a durable connection consumer -- throws IllegalStateException
    * @param topic The topic
    * @param subscriptionName The subscription name
    * @param messageSelector The message selector
    * @param sessionPool The session pool
    * @param maxMessages The number of max messages
    * @return The connection consumer
    * @exception JMSException Thrown if an error occurs
    */
   public ConnectionConsumer createDurableConnectionConsumer(final Topic topic,
                                                             final String subscriptionName,
                                                             final String messageSelector,
                                                             final ServerSessionPool sessionPool,
                                                             final int maxMessages) throws JMSException
   {
      if (trace)
      {
         log.trace("createConnectionConsumer(" + topic +
                   ", " +
                   subscriptionName +
                   ", " +
                   messageSelector +
                   ", " +
                   sessionPool +
                   ", " +
                   maxMessages +
                   ")");
      }

      throw new IllegalStateException(ISE);
   }

   /**
    * Create a connection consumer -- throws IllegalStateException
    * @param destination The destination
    * @param pool The session pool
    * @param maxMessages The number of max messages
    * @return The connection consumer
    * @exception JMSException Thrown if an error occurs
    */
   public ConnectionConsumer createConnectionConsumer(final Destination destination,
                                                      final ServerSessionPool pool,
                                                      final int maxMessages) throws JMSException
   {
      if (trace)
      {
         log.trace("createConnectionConsumer(" + destination + ", " + pool + ", " + maxMessages + ")");
      }

      throw new IllegalStateException(ISE);
   }

   /**
    * Create a connection consumer -- throws IllegalStateException
    * @param destination The destination
    * @param name The name
    * @param pool The session pool
    * @param maxMessages The number of max messages
    * @return The connection consumer
    * @exception JMSException Thrown if an error occurs
    */
   public ConnectionConsumer createConnectionConsumer(final Destination destination,
                                                      final String name,
                                                      final ServerSessionPool pool,
                                                      final int maxMessages) throws JMSException
   {
      if (trace)
      {
         log.trace("createConnectionConsumer(" + destination + ", " + name + ", " + pool + ", " + maxMessages + ")");
      }

      throw new IllegalStateException(ISE);
   }

   /**
    * Create a session
    * @param transacted Use transactions
    * @param acknowledgeMode The acknowledge mode
    * @return The session
    * @exception JMSException Thrown if an error occurs
    */
   public Session createSession(final boolean transacted, final int acknowledgeMode) throws JMSException
   {
      if (trace)
      {
         log.trace("createSession(" + transacted + ", " + acknowledgeMode + ")");
      }

      checkClosed();
      return allocateConnection(transacted, acknowledgeMode, type);
   }

   /**
    * Create a XA session
    * @return The XA session
    * @exception JMSException Thrown if an error occurs
    */
   public XASession createXASession() throws JMSException
   {
      if (trace)
      {
         log.trace("createXASession()");
      }

      checkClosed();
      return allocateConnection(type);
   }

   /**
    * Get the connection metadata
    * @return The connection metadata
    * @exception JMSException Thrown if an error occurs
    */
   public ConnectionMetaData getMetaData() throws JMSException
   {
      if (trace)
      {
         log.trace("getMetaData()");
      }

      checkClosed();
      return mcf.getMetaData();
   }

   /**
    * Get the exception listener -- throws IllegalStateException
    * @return The exception listener
    * @exception JMSException Thrown if an error occurs
    */
   public ExceptionListener getExceptionListener() throws JMSException
   {
      if (trace)
      {
         log.trace("getExceptionListener()");
      }

      throw new IllegalStateException(ISE);
   }

   /**
    * Set the exception listener -- throws IllegalStateException
    * @param listener The exception listener
    * @exception JMSException Thrown if an error occurs
    */
   public void setExceptionListener(final ExceptionListener listener) throws JMSException
   {
      if (trace)
      {
         log.trace("setExceptionListener(" + listener + ")");
      }

      throw new IllegalStateException(ISE);
   }

   /**
    * Start
    * @exception JMSException Thrown if an error occurs
    */
   public void start() throws JMSException
   {
      checkClosed();

      if (trace)
      {
         log.trace("start() " + this);
      }

      synchronized (sessions)
      {
         if (started)
         {
            return;
         }
         started = true;
         for (Iterator i = sessions.iterator(); i.hasNext();)
         {
            JBMSession session = (JBMSession)i.next();
            session.start();
         }
      }
   }

   /**
    * Stop -- throws IllegalStateException
    * @exception JMSException Thrown if an error occurs
    */
   public void stop() throws JMSException
   {
      if (trace)
      {
         log.trace("stop() " + this);
      }

      throw new IllegalStateException(ISE);
   }

   /**
    * Close
    * @exception JMSException Thrown if an error occurs
    */
   public void close() throws JMSException
   {
      if (trace)
      {
         log.trace("close() " + this);
      }

      if (closed)
      {
         return;
      }

      closed = true;

      synchronized (sessions)
      {
         for (Iterator i = sessions.iterator(); i.hasNext();)
         {
            JBMSession session = (JBMSession)i.next();
            try
            {
               session.closeSession();
            }
            catch (Throwable t)
            {
               log.trace("Error closing session", t);
            }
            i.remove();
         }
      }

      synchronized (tempQueues)
      {
         for (Iterator i = tempQueues.iterator(); i.hasNext();)
         {
            TemporaryQueue temp = (TemporaryQueue)i.next();
            try
            {
               if (trace)
               {
                  log.trace("Closing temporary queue " + temp + " for " + this);
               }
               temp.delete();
            }
            catch (Throwable t)
            {
               log.trace("Error deleting temporary queue", t);
            }
            i.remove();
         }
      }

      synchronized (tempTopics)
      {
         for (Iterator i = tempTopics.iterator(); i.hasNext();)
         {
            TemporaryTopic temp = (TemporaryTopic)i.next();
            try
            {
               if (trace)
               {
                  log.trace("Closing temporary topic " + temp + " for " + this);
               }
               temp.delete();
            }
            catch (Throwable t)
            {
               log.trace("Error deleting temporary queue", t);
            }
            i.remove();
         }
      }
   }

   /**
    * Close session
    * @param session The session
    * @exception JMSException Thrown if an error occurs
    */
   public void closeSession(final JBMSession session) throws JMSException
   {
      if (trace)
      {
         log.trace("closeSession(" + session + ")");
      }

      synchronized (sessions)
      {
         sessions.remove(session);
      }
   }

   /**
    * Add temporary queue
    * @param temp The temporary queue
    */
   public void addTemporaryQueue(final TemporaryQueue temp)
   {
      if (trace)
      {
         log.trace("addTemporaryQueue(" + temp + ")");
      }

      synchronized (tempQueues)
      {
         tempQueues.add(temp);
      }
   }

   /**
    * Add temporary topic
    * @param temp The temporary topic
    */
   public void addTemporaryTopic(final TemporaryTopic temp)
   {
      if (trace)
      {
         log.trace("addTemporaryTopic(" + temp + ")");
      }

      synchronized (tempTopics)
      {
         tempTopics.add(temp);
      }
   }

   /**
    * Allocation a connection
    * @param sessionType The session type
    * @return The session 
    * @exception JMSException Thrown if an error occurs
    */
   protected JBMSession allocateConnection(final int sessionType) throws JMSException
   {
      if (trace)
      {
         log.trace("allocateConnection(" + sessionType + ")");
      }

      try
      {
         synchronized (sessions)
         {
            if (sessions.isEmpty() == false)
            {
               throw new IllegalStateException("Only allowed one session per connection. See the J2EE spec, e.g. J2EE1.4 Section 6.6");
            }

            JBMConnectionRequestInfo info = new JBMConnectionRequestInfo(sessionType);
            info.setUserName(userName);
            info.setPassword(password);
            info.setClientID(clientID);
            info.setDefaults(((JBMResourceAdapter)mcf.getResourceAdapter()).getProperties());

            if (trace)
            {
               log.trace("Allocating session for " + this + " with request info=" + info);
            }

            JBMSession session = (JBMSession)cm.allocateConnection(mcf, info);

            try
            {
               if (trace)
               {
                  log.trace("Allocated  " + this + " session=" + session);
               }

               session.setJBMSessionFactory(this);

               if (started)
               {
                  session.start();
               }

               sessions.add(session);

               return session;
            }
            catch (Throwable t)
            {
               try
               {
                  session.close();
               }
               catch (Throwable ignored)
               {
               }
               if (t instanceof Exception)
               {
                  throw (Exception)t;
               }
               else
               {
                  throw new RuntimeException("Unexpected error: ", t);
               }
            }
         }
      }
      catch (Exception e)
      {
         log.error("Could not create session", e);

         JMSException je = new JMSException("Could not create a session: " + e.getMessage());
         je.setLinkedException(e);
         throw je;
      }
   }

   /**
    * Allocation a connection
    * @param transacted Use transactions
    * @param acknowledgeMode The acknowledge mode
    * @param sessionType The session type
    * @return The session 
    * @exception JMSException Thrown if an error occurs
    */
   protected JBMSession allocateConnection(final boolean transacted, int acknowledgeMode, final int sessionType) throws JMSException
   {
      if (trace)
      {
         log.trace("allocateConnection(" + transacted + ", " + acknowledgeMode + ", " + sessionType + ")");
      }

      try
      {
         synchronized (sessions)
         {
            if (sessions.isEmpty() == false)
            {
               throw new IllegalStateException("Only allowed one session per connection. See the J2EE spec, e.g. J2EE1.4 Section 6.6");
            }

            if (transacted)
            {
               acknowledgeMode = Session.SESSION_TRANSACTED;
            }

            JBMConnectionRequestInfo info = new JBMConnectionRequestInfo(transacted, acknowledgeMode, sessionType);
            info.setUserName(userName);
            info.setPassword(password);
            info.setClientID(clientID);

            if (trace)
            {
               log.trace("Allocating session for " + this + " with request info=" + info);
            }

            JBMSession session = (JBMSession)cm.allocateConnection(mcf, info);

            try
            {
               if (trace)
               {
                  log.trace("Allocated  " + this + " session=" + session);
               }

               session.setJBMSessionFactory(this);

               if (started)
               {
                  session.start();
               }

               sessions.add(session);

               return session;
            }
            catch (Throwable t)
            {
               try
               {
                  session.close();
               }
               catch (Throwable ignored)
               {
               }
               if (t instanceof Exception)
               {
                  throw (Exception)t;
               }
               else
               {
                  throw new RuntimeException("Unexpected error: ", t);
               }
            }
         }
      }
      catch (Exception e)
      {
         log.error("Could not create session", e);

         JMSException je = new JMSException("Could not create a session: " + e.getMessage());
         je.setLinkedException(e);
         throw je;
      }
   }

   /**
    * Check if we are closed
    * @exception IllegalStateException Thrown if closed
    */
   protected void checkClosed() throws IllegalStateException
   {
      if (trace)
      {
         log.trace("checkClosed()" + this);
      }

      if (closed)
      {
         throw new IllegalStateException("The connection is closed");
      }
   }
}
