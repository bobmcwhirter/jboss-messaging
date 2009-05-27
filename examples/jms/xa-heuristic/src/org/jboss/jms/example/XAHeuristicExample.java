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

import org.jboss.common.example.JBMExample;
import org.jboss.messaging.core.management.ObjectNames;
import org.jboss.messaging.core.transaction.impl.XidImpl;
import org.jboss.messaging.utils.UUIDGenerator;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.InitialContext;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A simple JMS example showing how to administer un-finished transactions.
 *
 * @author <a href="hgao@redhat.com">Howard Gao</a>
 */
public class XAHeuristicExample extends JBMExample
{
   private volatile boolean result = true;
   private ArrayList<String> receiveHolder = new ArrayList<String>();
   
   private String JMX_URL = "service:jmx:rmi:///jndi/rmi://localhost:3001/jmxrmi";

   public static void main(String[] args)
   {
      new XAHeuristicExample().run(args);
   }

   public boolean runExample() throws Exception
   {
      XAConnection connection = null;
      InitialContext initialContext = null;
      try
      {
         //Step 1. Create an initial context to perform the JNDI lookup.
         initialContext = getContext(0);

         //Step 2. Lookup on the queue
         Queue queue = (Queue) initialContext.lookup("/queue/exampleQueue");

         //Step 3. Perform a lookup on the XA Connection Factory
         XAConnectionFactory cf = (XAConnectionFactory) initialContext.lookup("/XAConnectionFactory");

         //Step 4.Create a JMS XAConnection
         connection = cf.createXAConnection();
         
         //Step 5. Start the connection
         connection.start();

         //Step 6. Create a JMS XASession
         XASession xaSession = connection.createXASession();
         
         //Step 7. Create a normal session
         Session normalSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         
         //Step 8. Create a normal Message Consumer
         MessageConsumer normalConsumer = normalSession.createConsumer(queue);
         normalConsumer.setMessageListener(new SimpleMessageListener());

         //Step 9. Get the JMS Session
         Session session = xaSession.getSession();
         
         //Step 10. Create a message producer
         MessageProducer producer = session.createProducer(queue);
         
         //Step 11. Create two Text Messages
         TextMessage helloMessage = session.createTextMessage("hello");
         TextMessage worldMessage = session.createTextMessage("world");
         
         //Step 12. create a transaction
         Xid xid1 = new XidImpl("xa-example1".getBytes(), 1, UUIDGenerator.getInstance().generateStringUUID().getBytes());
         
         //Step 13. Get the JMS XAResource
         XAResource xaRes = xaSession.getXAResource();
         
         //Step 14. Begin the Transaction work
         xaRes.start(xid1, XAResource.TMNOFLAGS);
         
         //Step 15. do work, sending hello message.
         producer.send(helloMessage);
         
         System.out.println("Sent message " + helloMessage.getText());
         
         //Step 16. Stop the work for xid1
         xaRes.end(xid1, XAResource.TMSUCCESS);
         
         //Step 17. Prepare xid1
         xaRes.prepare(xid1);
         
         //Step 18. Check none should be received
         checkNoMessageReceived();
         
         //Step 19. Create another transaction.
         Xid xid2 = new XidImpl("xa-example2".getBytes(), 1, UUIDGenerator.getInstance().generateStringUUID().getBytes());
         
         //Step 20. Begin the transaction work
         xaRes.start(xid2, XAResource.TMNOFLAGS);

         //Step 21. Send the second message
         producer.send(worldMessage);
         
         System.out.println("Sent message " + worldMessage.getText());
         
         //Step 22. Stop the work for xid2
         xaRes.end(xid2, XAResource.TMSUCCESS);
         
         //Step 23. prepare xid2
         xaRes.prepare(xid2);
         
         //Step 24. Again, no messages should be received!
         checkNoMessageReceived();

         //Step 25. Create JMX Connector to connect to the server's MBeanServer
         JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(JMX_URL), new HashMap<String, String>());

         //Step 26. Retrieve the MBeanServerConnection
         MBeanServerConnection mbsc = connector.getMBeanServerConnection();
         
         //Step 27. List the prepared transactions
         ObjectName serverObject = ObjectNames.getMessagingServerObjectName();
         String[] infos = (String[])mbsc.invoke(serverObject, "listPreparedTransactions", null, null);
         
         System.out.println("Prepared transactions: ");
         for (String i : infos)
         {
            System.out.println(i);
         }

         //Step 28. Roll back the first transaction
         mbsc.invoke(serverObject, "rollbackPreparedTransaction", new String[] {XidImpl.toBase64String(xid1)}, new String[]{"java.lang.String"});
         
         //Step 29. Commit the second one
         mbsc.invoke(serverObject, "commitPreparedTransaction", new String[] {XidImpl.toBase64String(xid2)}, new String[]{"java.lang.String"});
         
         Thread.sleep(2000);
         
         //Step 30. Check the result, only the 'world' message received
         checkMessageReceived("world");

         //Step 31. Check the prepared transaction again, should have none.
         infos = (String[])mbsc.invoke(serverObject, "listPreparedTransactions", null, null);
         System.out.println("No. of prepared transactions now: " + infos.length);
         
         //Step 32. Close the JMX Connector
         connector.close();
         
         return result;
      }
      finally
      {
         //Step 32. Be sure to close our JMS resources!
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
   
   private void checkMessageReceived(String value)
   {
      if (receiveHolder.size() != 1)
      {
         System.out.println("Number of messages received not correct ! -- " + receiveHolder.size());
         result = false;
      }
      String msg = receiveHolder.get(0);
      if (!msg.equals(value))
      {
         System.out.println("Received message [" + msg + "], but we expect [" + value + "]");
         result = false;
      }
      receiveHolder.clear();
   }

   private void checkNoMessageReceived()
   {
      if (receiveHolder.size() > 0)
      {
         System.out.println("Message received, wrong!");
         result = false;
      }
      receiveHolder.clear();
   }

   
   public class SimpleMessageListener implements MessageListener
   {
      public void onMessage(Message message)
      {
         try
         {
            System.out.println("Message received: " + ((TextMessage)message).getText());
            receiveHolder.add(((TextMessage)message).getText());
         }
         catch (JMSException e)
         {
            result = false;
            e.printStackTrace();
         }
      }
      
   }

}
