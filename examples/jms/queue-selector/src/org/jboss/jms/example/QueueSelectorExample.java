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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

/**
 * A simple JMS example that uses selectors with queue consumers.
 *
 * @author <a href="hgao@redhat.com">Howard Gao</a>
 */
public class QueueSelectorExample extends JMSExample
{
   private volatile boolean result = true;

   public static void main(String[] args)
   {
      new QueueSelectorExample().run(args);
   }

   public boolean runExample() throws Exception
   {
      Connection connection = null;
      InitialContext initialContext = null;
      try
      {
         //Step 1. Create an initial context to perform the JNDI lookup.
         initialContext = getContext(0);

         //Step 2. look-up the JMS queue object from JNDI
         Queue queue = (Queue) initialContext.lookup("/queue/exampleQueue");

         //Step 3. look-up the JMS connection factory object from JNDI
         ConnectionFactory cf = (ConnectionFactory) initialContext.lookup("/ConnectionFactory");

         //Step 4. Create a JMS Connection
         connection = cf.createConnection();
         
         //Step 5. Start the connection
         connection.start();

         //Step 5. Create a JMS Session
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

         //Step 6. Create a JMS Message Producer
         MessageProducer producer = session.createProducer(queue);
         
         //Step 8. Prepare two selectors
         String redSelector = "color='red'";
         String greenSelector = "color='green'";

         //Step 9. Create a JMS Message Consumer that receives 'red' messages
         MessageConsumer redConsumer = session.createConsumer(queue, redSelector);
         redConsumer.setMessageListener(new SimpleMessageListener("red"));

         //Step 10. Create a second JMS message consumer that receives 'green' messages
         MessageConsumer greenConsumer = session.createConsumer(queue, greenSelector);
         greenConsumer.setMessageListener(new SimpleMessageListener("green"));
         
         //Step 11. Create another JMS message consumer that receives any messages.
         MessageConsumer anyConsumer = session.createConsumer(queue);
         anyConsumer.setMessageListener(new SimpleMessageListener("any"));
         
         //Step 12. Create three messages, each has a color property
         TextMessage redMessage = session.createTextMessage("Red");
         redMessage.setStringProperty("color", "red");
         TextMessage greenMessage = session.createTextMessage("Green");
         greenMessage.setStringProperty("color", "green");
         TextMessage blueMessage = session.createTextMessage("Blue");
         blueMessage.setStringProperty("color", "blue");

         //Step 13. Send the Messages
         producer.send(redMessage);
         System.out.println("Message sent: " + redMessage.getText());
         producer.send(greenMessage);
         System.out.println("Message sent: " + greenMessage.getText());
         producer.send(blueMessage);
         System.out.println("Message sent: " + blueMessage.getText());
         
         Thread.sleep(5000);
         
         return result;
      }
      finally
      {
         //Step 12. Be sure to close our JMS resources!
         if (initialContext != null)
         {
            initialContext.close();
         }
         if(connection != null)
         {
            connection.close();
         }
      }
   }
   
   public class SimpleMessageListener implements MessageListener
   {

      private String name;
      
      public SimpleMessageListener(String listener)
      {
         name = listener;
      }

      public void onMessage(Message msg)
      {
         TextMessage textMessage = (TextMessage)msg;
         try
         {
            String colorProp = msg.getStringProperty("color");
            System.out.println("Receiver " + name + " receives message [" + textMessage.getText() + "] with color property: " + colorProp);
            if ((!colorProp.equals(name)) && (!name.equals("any")))
            {
               result = false;
            }
         }
         catch (JMSException e)
         {
            e.printStackTrace();
            result = false;
         }
      }
      
   }

}
