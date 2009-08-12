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
package org.jboss.jms.soak.example.reconnect;

import java.util.Hashtable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class SoakReconnectableReceiver
{
   private static final Logger log = Logger.getLogger(SoakReconnectableReceiver.class.getName());

   private static final String EOF = UUID.randomUUID().toString();

   public static void main(String[] args)
   {
      for (int i = 0; i < args.length; i++)
      {
         System.out.println(i + ":" + args[i]);
      }
      String jndiURL = "jndi://localhost:1099";
      if (args.length > 0)
      {
         jndiURL = args[0];
      }
      
      System.out.println("Connecting to JNDI at " + jndiURL);
      
      try
      {
         String fileName = SoakBase.getPerfFileName(args);

         SoakParams params = SoakBase.getParams(fileName);

         Hashtable<String, String> jndiProps = new Hashtable<String, String>();
         jndiProps.put("java.naming.provider.url", jndiURL);
         jndiProps.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
         jndiProps.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");

         final SoakReconnectableReceiver receiver = new SoakReconnectableReceiver(jndiProps, params);

         Runtime.getRuntime().addShutdownHook(new Thread()
         {
            @Override
            public void run()
            {
               receiver.disconnect();
            }
         });

         receiver.run();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   private final Hashtable<String, String> jndiProps;

   private final SoakParams perfParams;

   private final ExceptionListener exceptionListener = new ExceptionListener()
   {
      public void onException(JMSException e)
      {
         disconnect();
         connect();
      }
   };

   private final MessageListener listener = new MessageListener()
   {
      int modulo = 10000;

      private final AtomicLong count = new AtomicLong(0);

      private long start = System.currentTimeMillis();
      long moduloStart = start;

      public void onMessage(Message msg)
      {
         long totalDuration = System.currentTimeMillis() - start;

         try
         {
            if (EOF.equals(msg.getStringProperty("eof")))
            {
               log.info(String.format("Received %s messages in %.2f minutes", count, (1.0 * totalDuration) / SoakBase.TO_MILLIS));
               log.info("END OF RUN");

               return;
            }
         }
         catch (JMSException e1)
         {
            e1.printStackTrace();
         }
         if (count.incrementAndGet() % modulo == 0)
         {
            double duration = (1.0 * System.currentTimeMillis() - moduloStart) / 1000;
            moduloStart = System.currentTimeMillis();
            log.info(String.format("received %s messages in %2.2fs (total: %.0fs)", modulo, duration, totalDuration / 1000.0));
         }
      }
   };

   private Session session;

   private Connection connection;

   private SoakReconnectableReceiver(final Hashtable<String, String> jndiProps, final SoakParams perfParams)
   {
      this.jndiProps = jndiProps;
      this.perfParams = perfParams;
   }

   public void run() throws Exception
   {
      connect();

      boolean runInfinitely = (perfParams.getDurationInMinutes() == -1);

      if (!runInfinitely)
      {
         Thread.sleep(perfParams.getDurationInMinutes() * SoakBase.TO_MILLIS);

         // send EOF message
         Message eof = session.createMessage();
         eof.setStringProperty("eof", EOF);
         listener.onMessage(eof);
         
         if (connection != null)
         {
            connection.close();
            connection = null;
         }
      } else
      {
         while (true)
         {
            Thread.sleep(500);
         }
      }
   }

   private void disconnect()
   {
      if (connection != null)
      {
         try
         {
            connection.setExceptionListener(null);
            connection.close();
         }
         catch (JMSException e)
         {
            e.printStackTrace();
         }
         finally
         {
            connection = null;
         }
      }
   }

   private void connect()
   {
      InitialContext ic = null;
      try
      {
         ic = new InitialContext(jndiProps);

         ConnectionFactory factory = (ConnectionFactory)ic.lookup(perfParams.getConnectionFactoryLookup());

         Destination destination = (Destination)ic.lookup(perfParams.getDestinationLookup());

         connection = factory.createConnection();
         connection.setExceptionListener(exceptionListener);

         session = connection.createSession(perfParams.isSessionTransacted(),
                                                    perfParams.isDupsOK() ? Session.DUPS_OK_ACKNOWLEDGE
                                                                         : Session.AUTO_ACKNOWLEDGE);

         MessageConsumer messageConsumer = session.createConsumer(destination);
         messageConsumer.setMessageListener(listener);

         connection.start();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      finally
      {
         try
         {
            ic.close();
         }
         catch (NamingException e)
         {
            e.printStackTrace();
         }
      }
   }
}