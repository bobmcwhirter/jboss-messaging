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
package org.jboss.jms.example;

import org.jboss.messaging.core.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 */
public class ScheduledExample
{
   final static Logger log = Logger.getLogger(ScheduledExample.class);

   public static void main(final String[] args)
   {
      DateFormat df = new SimpleDateFormat("hh:mm:ss");
      Connection connection = null;
      try
      {
         //create an initial context, env will be picked up from jndi.properties
         InitialContext initialContext = new InitialContext();
         Queue queue = (Queue) initialContext.lookup("/queue/testQueue");
         ConnectionFactory cf = (ConnectionFactory) initialContext.lookup("/ConnectionFactory");

         connection = cf.createConnection();
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer producer = session.createProducer(queue);
         Message message = session.createTextMessage("This is a text message!");
         Calendar cal = Calendar.getInstance();
         log.info("current time " + df.format(cal.getTime()));
         cal.add(Calendar.SECOND, 5);
         log.info("message scheduled for " + df.format(cal.getTime()));
         message.setLongProperty("JMS_JBOSS_SCHEDULED_DELIVERY_PROP_NAME", cal.getTimeInMillis());
         log.info("sending message to queue");
         producer.send(message);

         MessageConsumer messageConsumer = session.createConsumer(queue);
         connection.start();
         TextMessage message2 = (TextMessage) messageConsumer.receive(7000);
         log.info("message received at " + df.format(Calendar.getInstance().getTime()));
         log.info("message = " + message2.getText());
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      finally
      {
         if(connection != null)
         {
            try
            {
               connection.close();
            }
            catch (JMSException e)
            {
               e.printStackTrace();
            }
         }
      }
   }
}
