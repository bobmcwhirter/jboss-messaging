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
package org.jboss.jms.client;

import java.util.LinkedList;

import javax.jms.ConnectionConsumer;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ServerSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;

import org.jboss.jms.client.delegate.DelegateSupport;
import org.jboss.jms.client.state.ConsumerState;
import org.jboss.jms.delegate.ConnectionDelegate;
import org.jboss.jms.delegate.ConsumerDelegate;
import org.jboss.jms.delegate.SessionDelegate;
import org.jboss.jms.destination.JBossDestination;
import org.jboss.jms.message.MessageProxy;
import org.jboss.jms.util.ThreadContextClassLoaderChanger;
import org.jboss.logging.Logger;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

/**
 * This class implements javax.jms.ConnectionConsumer
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * 
 * Partially based on JBossMQ version by:
 * 
 * @author Hiram Chirino (Cojonudo14@hotmail.com)
 * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
 * 
 * @version $Revision$
 *
 * $Id$
 */
public class JBossConnectionConsumer implements ConnectionConsumer, Runnable
{
   // Constants -----------------------------------------------------

   private static Logger log = Logger.getLogger(JBossConnectionConsumer.class);

   private static boolean trace = log.isTraceEnabled();
   
   // Attributes ----------------------------------------------------
   
   protected ConsumerDelegate cons;
   
   protected SessionDelegate sess;
   
   protected int consumerID;
   
   /** The destination this consumer will receive messages from */
   protected Destination destination;
   
   /** The ServerSessionPool that is implemented by the AS */
   protected ServerSessionPool serverSessionPool;
   
   /** The maximum number of messages that a single session will be loaded with. */
   protected int maxMessages;
   
   /** Is the ConnectionConsumer closed? */
   protected volatile boolean closed;
     
   /** The "listening" thread that gets messages from destination and queues
   them for delivery to sessions */
   protected Thread internalThread;
   
   /** The thread id */
   protected int id;
   
   /** The thread id generator */
   protected static SynchronizedInt threadId = new SynchronizedInt(0);
   
   protected Object closeLock = new Object();
   
   // Static --------------------------------------------------------
   
   // Constructors --------------------------------------------------

   /**
    * JBossConnectionConsumer constructor
    * 
    * @param conn the connection
    * @param dest destination
    * @param messageSelector the message selector
    * @param sessPool the server session pool
    * @param maxMessages the maxmimum messages
    * @exception JMSException for any error
    */
   public JBossConnectionConsumer(ConnectionDelegate conn, JBossDestination dest, 
                                  String subName, String messageSelector,
                                  ServerSessionPool sessPool, int maxMessages) throws JMSException
   {
      trace = log.isTraceEnabled();

      this.destination = dest;
      this.serverSessionPool = sessPool;
      this.maxMessages = maxMessages;
      if (this.maxMessages < 1)
      {
         this.maxMessages = 1;
      }

      ThreadContextClassLoaderChanger tccc = new ThreadContextClassLoaderChanger();

      try
      {
         tccc.set(getClass().getClassLoader());

         // Create a consumer. We must create with CLIENT_ACKNOWLEDGE, they get must not get acked
         // via this session!
         sess = conn.createSessionDelegate(false, Session.CLIENT_ACKNOWLEDGE, false);

         cons = sess.createConsumerDelegate(dest, messageSelector, false, subName, true);
      }
      finally
      {
         tccc.restore();
      }

      ConsumerState state = (ConsumerState)((DelegateSupport)cons).getState();

      this.consumerID = state.getConsumerID();

      id = threadId.increment();
      internalThread = new Thread(this, "Connection Consumer for dest " + destination + " id=" + id);
      internalThread.start();

      if (trace) { log.trace(this + " created"); }
   }
   
   // ConnectionConsumer implementation -----------------------------

   public ServerSessionPool getServerSessionPool() throws JMSException
   {
      return serverSessionPool;
   }

   public void close() throws javax.jms.JMSException
   {
      if (trace) { log.trace("close " + this); }
      
      closed = true;
      
      if (trace) { log.trace("Closed message handler"); }
      
      internalThread.interrupt();
          
      try
      {
         internalThread.join(3000);
         if (internalThread.isAlive())
         {
            internalThread.interrupt();
            internalThread.join();
         }         
      }
      catch (InterruptedException e)
      {
         if (trace)
            log.trace("Ignoring interrupting while joining thread " + this);
      }
      
      sess.close();
      
      if (trace) { log.trace("Closed: " + this); }
      
   }
   
   // Runnable implementation ---------------------------------------
     
   public void run()
   {
      if (trace) { log.trace("running connection consumer"); }
      try
      {
         LinkedList queue = new LinkedList();
         while (true)
         {
            
            if (closed)
            {
               if (trace) { log.trace("Connection consumer is closed, breaking"); }
               break;
            }
            
            if (queue.isEmpty())
            {
               // Remove up to maxMessages messages from the consumer
               for (int i = 0; i < maxMessages; i++)
               {               
                  // receiveNoWait

                  if (trace) { log.trace(this + " attempting to get message with receiveNoWait"); }

                  Message m = null;                  

                  if (!closed)
                  {
                     m = cons.receive(-1);
                  }
                  
                  if (m == null)
                  {
                     if (trace) { log.trace("receiveNoWait did not retrieve any message"); }
                     break;
                  }

                  if (trace) { log.trace("receiveNoWait got message " + m + " adding to queue"); }
                  queue.addLast(m);
               }

               if (queue.isEmpty())
               {
                  // We didn't get any messages doing receiveNoWait, so let's wait. This returns if
                  // a message is received or by the consumer closing.

                  if (trace) { log.trace(this + " attempting to get message with blocking receive (no timeout)"); }

                  Message m = null;
                  
                  if (!closed)
                  {
                     m = cons.receive(0);
                  }
                  
                  if (m != null)
                  {
                     if (trace) { log.trace("receive (no timeout) got message " + m + " adding to queue"); }
                     queue.addLast(m);
                  }
                  else
                  {
                     // The consumer must have closed
                     if (trace) { log.trace("blocking receive returned null, consumer must have closed"); }
                     break;
                  }
               }
            }
            
            if (!queue.isEmpty())
            {
               if (trace) { log.trace("there are " + queue.size() + " messages to send to session"); }

               ServerSession serverSession = serverSessionPool.getServerSession();
               JBossSession session = (JBossSession)serverSession.getSession();

               MessageListener listener = session.getMessageListener();

               if (listener == null)
               {
                  // Sanity check
                  if (trace) { log.trace(this + ": session " + session + " did not have a set MessageListener"); }
               }

               for (int i = 0; i < queue.size(); i++)
               {
                  MessageProxy m = (MessageProxy)queue.get(i);
                  session.addAsfMessage(m, consumerID, cons);
                  if (trace) { log.trace("added " + m + " to session"); }
               }

               if (trace) { log.trace(this + " starting serverSession " + serverSession); }

               serverSession.start();

               if (trace) { log.trace(this + "'s serverSession processed messages"); }

               queue.clear();
            }
            
         }
         if (trace) { log.trace("ConnectionConsumer run() exiting"); }
      }
      catch (JMSException e)
      {
         //Receive interrupted - ignore
      }
      catch (Throwable t)
      {
         log.error("Caught Throwable in processing run()", t);         
      }      
   }

   // Public --------------------------------------------------------

   public String toString()
   {
      return "JBossConnectionConsumer[" + consumerID + ", " + id + "]";
   }

   // Object overrides ----------------------------------------------

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
  
}
