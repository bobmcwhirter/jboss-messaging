/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.messaging.ra.inflow;

import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.jms.JBossDestination;
import org.jboss.messaging.jms.JBossQueue;
import org.jboss.messaging.jms.JBossTopic;
import org.jboss.messaging.jms.client.JBossConnectionFactory;
import org.jboss.messaging.ra.JBMResourceAdapter;
import org.jboss.messaging.ra.Util;
import org.jboss.messaging.utils.SimpleString;
import org.jboss.tm.TransactionManagerLocator;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;
import javax.transaction.TransactionManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The activation.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 * @version $Revision: $
 */
public class JBMActivation
{
   /**
    * The logger
    */
   private static final Logger log = Logger.getLogger(JBMActivation.class);

   /**
    * Trace enabled
    */
   private static boolean trace = log.isTraceEnabled();

   /**
    * The onMessage method
    */
   public static final Method ONMESSAGE;

   /**
    * The resource adapter
    */
   private final JBMResourceAdapter ra;

   /**
    * The activation spec
    */
   private final JBMActivationSpec spec;

   /**
    * The message endpoint factory
    */
   private final MessageEndpointFactory endpointFactory;

   /**
    * Whether delivery is active
    */
   private final AtomicBoolean deliveryActive = new AtomicBoolean(false);

   /**
    * The destination type
    */
   private boolean isTopic = false;

   /**
    * Is the delivery transacted
    */
   private boolean isDeliveryTransacted;

   private JBossDestination destination;

   /**
    * The TransactionManager
    */
   private TransactionManager tm;

   private final List<JBMMessageHandler> handlers = new ArrayList<JBMMessageHandler>();

   private JBossConnectionFactory factory;

   static
   {
      try
      {
         ONMESSAGE = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Constructor
    *
    * @param ra              The resource adapter
    * @param endpointFactory The endpoint factory
    * @param spec            The activation spec
    * @throws ResourceException Thrown if an error occurs
    */
   public JBMActivation(final JBMResourceAdapter ra,
                        final MessageEndpointFactory endpointFactory,
                        final JBMActivationSpec spec) throws ResourceException
   {
      if (trace)
      {
         log.trace("constructor(" + ra + ", " + endpointFactory + ", " + spec + ")");
      }

      this.ra = ra;
      this.endpointFactory = endpointFactory;
      this.spec = spec;
      try
      {
         isDeliveryTransacted = endpointFactory.isDeliveryTransacted(ONMESSAGE);
      }
      catch (Exception e)
      {
         throw new ResourceException(e);
      }
   }

   /**
    * Get the activation spec
    *
    * @return The value
    */
   public JBMActivationSpec getActivationSpec()
   {
      if (trace)
      {
         log.trace("getActivationSpec()");
      }

      return spec;
   }

   /**
    * Get the message endpoint factory
    *
    * @return The value
    */
   public MessageEndpointFactory getMessageEndpointFactory()
   {
      if (trace)
      {
         log.trace("getMessageEndpointFactory()");
      }

      return endpointFactory;
   }

   /**
    * Get whether delivery is transacted
    *
    * @return The value
    */
   public boolean isDeliveryTransacted()
   {
      if (trace)
      {
         log.trace("isDeliveryTransacted()");
      }

      return isDeliveryTransacted;
   }

   /**
    * Get the work manager
    *
    * @return The value
    */
   public WorkManager getWorkManager()
   {
      if (trace)
      {
         log.trace("getWorkManager()");
      }

      return ra.getWorkManager();
   }

   /**
    * Get the transaction manager
    *
    * @return The value
    */
   public TransactionManager getTransactionManager()
   {
      if (trace)
      {
         log.trace("getTransactionManager()");
      }

      if (tm == null)
      {
         tm = TransactionManagerLocator.locateTransactionManager();
      }

      return tm;
   }

   /**
    * Is the destination a topic
    *
    * @return The value
    */
   public boolean isTopic()
   {
      if (trace)
      {
         log.trace("isTopic()");
      }

      return isTopic;
   }

   /**
    * Start the activation
    *
    * @throws ResourceException Thrown if an error occurs
    */
   public void start() throws ResourceException
   {
      if (trace)
      {
         log.trace("start()");
      }
      deliveryActive.set(true);
      ra.getWorkManager().scheduleWork(new SetupActivation());
   }

   /**
    * Stop the activation
    */
   public void stop()
   {
      if (trace)
      {
         log.trace("stop()");
      }

      deliveryActive.set(false);
      teardown();
   }

   /**
    * Setup the activation
    *
    * @throws Exception Thrown if an error occurs
    */
   protected void setup() throws Exception
   {
      log.debug("Setting up " + spec);

      setupCF();

      setupDestination();
      for (int i = 0; i < spec.getMaxSessionInt(); i++)
      {
         ClientSession session = setupSession();

         JBMMessageHandler handler = new JBMMessageHandler(this, session);
         handler.setup();
         session.start();
         handlers.add(handler);
      }

      log.debug("Setup complete " + this);
   }

   /**
    * Teardown the activation
    */
   protected void teardown()
   {
      log.debug("Tearing down " + spec);

      for (JBMMessageHandler handler : handlers)
      {
         handler.teardown();
      }
      if(spec.isHasBeenUpdated())
      {
         factory.close();
         factory = null;
      }
      log.debug("Tearing down complete " + this);
   }

   protected void setupCF() throws Exception
   {
      if(spec.isHasBeenUpdated())
      {
         factory = ra.createJBossConnectionFactory(spec);
      }
      else
      {
         factory = ra.getDefaultJBossConnectionFactory();
      }
   }

   /**
    * Setup a session
    * @return The connection
    * @throws Exception Thrown if an error occurs
    */
   protected ClientSession setupSession() throws Exception
   {
      ClientSession result = null;

      try
      {
         result = ra.createSession(factory.getCoreFactory(),
                                   spec.getAcknowledgeModeInt(),
                                   spec.getUser(),
                                   spec.getPassword(),
                                   ra.getPreAcknowledge(),
                                   ra.getDupsOKBatchSize(),
                                   ra.getTransactionBatchSize(),
                                   isDeliveryTransacted,
                                   spec.isUseLocalTx());

         log.debug("Using queue connection " + result);

         return result;
      }
      catch (Throwable t)
      {
         try
         {
            if (result != null)
            {
               result.close();
            }
         }
         catch (Exception e)
         {
            log.trace("Ignored error closing connection", e);
         }
         if (t instanceof Exception)
         {
            throw (Exception)t;
         }
         throw new RuntimeException("Error configuring connection", t);
      }
   }

   public SimpleString getAddress()
   {
      return destination.getSimpleAddress();
   }

   protected void setupDestination() throws Exception
   {

      String destinationName = spec.getDestination();

      if (spec.isUseJNDI())
      {
         Context ctx = new InitialContext();
         log.debug("Using context " + ctx.getEnvironment() + " for " + spec);
         if (trace)
         {
            log.trace("setupDestination(" + ctx + ")");
         }

         String destinationTypeString = spec.getDestinationType();
         if (destinationTypeString != null && !destinationTypeString.trim().equals(""))
         {
            log.debug("Destination type defined as " + destinationTypeString);

            Class<?> destinationType;
            if (Topic.class.getName().equals(destinationTypeString))
            {
               destinationType = Topic.class;
               isTopic = true;
            }
            else
            {
               destinationType = Queue.class;
            }

            log.debug("Retrieving destination " + destinationName + " of type " + destinationType.getName());
            try
            {
               destination = (JBossDestination)Util.lookup(ctx, destinationName, destinationType);
            }
            catch (Exception e)
            {
               if (destinationName == null)
               {                 
                  throw e;
               }
               // If there is no binding on naming, we will just create a new instance
               if (isTopic)
               {
                  destination = new JBossTopic(destinationName.substring(destinationName.lastIndexOf('/') + 1));
               }
               else
               {
                  destination = new JBossQueue(destinationName.substring(destinationName.lastIndexOf('/') + 1));
               }
            }
         }
         else
         {
            log.debug("Destination type not defined");
            log.debug("Retrieving destination " + destinationName + " of type " + Destination.class.getName());

            destination = (JBossDestination)Util.lookup(ctx, destinationName, Destination.class);
            if (destination instanceof Topic)
            {
               isTopic = true;
            }
         }
      }
      else
      {
         if (Topic.class.getName().equals(spec.getDestinationType()))
         {
            destination = new JBossTopic(spec.getDestination());
         }
         else
         {
            destination = new JBossQueue(spec.getDestination());
         }
      }

      log.debug("Got destination " + destination + " from " + destinationName);
   }

   /**
    * Get a string representation
    *
    * @return The value
    */
   @Override
   public String toString()
   {
      StringBuffer buffer = new StringBuffer();
      buffer.append(JBMActivation.class.getName()).append('(');
      buffer.append("spec=").append(spec.getClass().getName());
      buffer.append(" mepf=").append(endpointFactory.getClass().getName());
      buffer.append(" active=").append(deliveryActive.get());
      if (spec.getDestination() != null)
      {
         buffer.append(" destination=").append(spec.getDestination());
      }
      /*if (session != null)
      {
         buffer.append(" connection=").append(session);
      }*/
      // if (pool != null)
      // buffer.append(" pool=").append(pool.getClass().getName());
      buffer.append(" transacted=").append(isDeliveryTransacted);
      buffer.append(')');
      return buffer.toString();
   }

   /**
    * Handles the setup
    */
   private class SetupActivation implements Work
   {
      public void run()
      {
         try
         {
            setup();
         }
         catch (Throwable t)
         {
            log.error("Unabler to start activation ", t);
         }
      }

      public void release()
      {
      }
   }
}
