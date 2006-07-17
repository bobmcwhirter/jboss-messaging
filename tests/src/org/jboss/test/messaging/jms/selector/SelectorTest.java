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
package org.jboss.test.messaging.jms.selector;

import org.jboss.test.messaging.tools.ServerManagement;
import org.jboss.test.messaging.MessagingTestCase;

import javax.jms.Connection;
import javax.jms.Session;
import javax.jms.MessageProducer;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.Topic;
import javax.naming.InitialContext;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class SelectorTest extends MessagingTestCase
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   protected ConnectionFactory cf;
   protected Queue queue;
   protected Topic topic;

   // Constructors --------------------------------------------------

   public SelectorTest(String name)
   {
      super(name);
   }

   // Public --------------------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();

      ServerManagement.start("all");
      
      
      ServerManagement.undeployQueue("Queue");
      ServerManagement.deployQueue("Queue");
      ServerManagement.undeployTopic("Topic");
      ServerManagement.deployTopic("Topic");
      

      InitialContext ic = new InitialContext(ServerManagement.getJNDIEnvironment());
      cf = (ConnectionFactory)ic.lookup("/ConnectionFactory");
      queue = (Queue)ic.lookup("/queue/Queue");
      topic = (Topic)ic.lookup("/topic/Topic");     

      log.debug("setup done");
   }

   public void tearDown() throws Exception
   {
      ServerManagement.undeployQueue("Queue");
      
      super.tearDown();
   }


   // Public --------------------------------------------------------


   /**
    * Test case for http://jira.jboss.org/jira/browse/JBMESSAGING-105
    *
    * Two Messages are sent to a queue. There is one receiver on the queue. The receiver only
    * receives one of the messages due to a message selector only matching one of them. The receiver
    * is then closed. A new receiver is now attached to the queue. Redelivery of the remaining
    * message is now attempted. The message should be redelivered.
    */
   public void testSelectiveClosingConsumer() throws Exception
   {
      Connection conn = cf.createConnection();
      conn.start();
      
      Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageProducer prod = session.createProducer(queue);
      prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      
      String selector = "color = 'red'";
      MessageConsumer redConsumer = session.createConsumer(queue, selector);
      conn.start();

      Message redMessage = session.createMessage();
      redMessage.setStringProperty("color", "red");

      Message blueMessage = session.createMessage();
      blueMessage.setStringProperty("color", "blue");

      prod.send(redMessage);
      prod.send(blueMessage);
      
      Message rec = redConsumer.receive();
      assertEquals(redMessage.getJMSMessageID(), rec.getJMSMessageID());
      assertEquals("red", rec.getStringProperty("color"));
      
      assertNull(redConsumer.receive(3000));
      
      redConsumer.close();
      
      MessageConsumer universalConsumer = session.createConsumer(queue);
      
      rec = universalConsumer.receive();
      
      assertEquals(rec.getJMSMessageID(), blueMessage.getJMSMessageID());
      assertEquals("blue", rec.getStringProperty("color"));
      
      session.close();
   }
   
   public void testManyTopic() throws Exception
   {
      String selector1 = "beatle = 'john'";
 
      Connection conn = cf.createConnection();
      conn.start();
      
      Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      
      MessageConsumer cons1 = sess.createConsumer(topic, selector1);
      
      MessageProducer prod = sess.createProducer(topic);
               
      for (int j = 0; j < 100; j++)
      {
         Message m = sess.createMessage();        
         
         m.setStringProperty("beatle", "john");
         
         prod.send(m);
         
         m = sess.createMessage();        
         
         m.setStringProperty("beatle", "kermit the frog");
         
         prod.send(m);         
      }
      
      for (int j = 0; j < 100; j++)
      {
         Message m = cons1.receive(1000);
         
         assertNotNull(m);
      }
      
      Thread.sleep(500);
      
      Message m = cons1.receiveNoWait();
      
      assertNull(m);
      
      sess.close();                 
   }
   
   public void testManyQueue() throws Exception
   {
      String selector1 = "beatle = 'john'";
 
      Connection conn = cf.createConnection();
      conn.start();
      
      Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      
      MessageConsumer cons1 = sess.createConsumer(queue, selector1);
      
      MessageProducer prod = sess.createProducer(queue);
               
      for (int j = 0; j < 100; j++)
      {
         Message m = sess.createMessage();        
         
         m.setStringProperty("beatle", "john");
         
         prod.send(m);
         
         m = sess.createMessage();        
         
         m.setStringProperty("beatle", "kermit the frog");
         
         prod.send(m);         
      }
      
      for (int j = 0; j < 100; j++)
      {
         Message m = cons1.receive(1000);
              
         assertNotNull(m);
      }
      
      Message m = cons1.receiveNoWait();
      
      assertNull(m);
      
      sess.close();                 
   }
   
   public void testManyRedeliveriesTopic() throws Exception
   {
      String selector1 = "beatle = 'john'";
 
      Connection conn = cf.createConnection();
      conn.start();
      
      for (int i = 0; i < 30; i++)
      {      
         Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
                          
         MessageConsumer cons1 = sess.createConsumer(topic, selector1);
         
         MessageProducer prod = sess.createProducer(topic);
         
         for (int j = 0; j < 10; j++)
         {
            Message m = sess.createMessage();        
            
            m.setStringProperty("beatle", "john");
            
            prod.send(m);
            
            m = sess.createMessage();        
            
            m.setStringProperty("beatle", "kermit the frog");
            
            prod.send(m);         
         }
         
         for (int j = 0; j < 10; j++)
         {
            Message m = cons1.receive(1000);
            
            assertNotNull(m);
         }
         
         Message m = cons1.receiveNoWait();
         
         assertNull(m);
         
         sess.close();
         
      }           
   }
   
   public void testManyRedeliveriesQueue() throws Exception
   {
      String selector1 = "beatle = 'john'";
 
      Connection conn = cf.createConnection();
      conn.start();
      
      for (int i = 0; i < 30; i++)
      {      
         Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
                          
         MessageConsumer cons1 = sess.createConsumer(queue, selector1);
         
         MessageProducer prod = sess.createProducer(queue);
         
         for (int j = 0; j < 10; j++)
         {
            Message m = sess.createMessage();        
            
            m.setStringProperty("beatle", "john");
            
            prod.send(m);
            
            m = sess.createMessage();        
            
            m.setStringProperty("beatle", "kermit the frog");
            
            prod.send(m);         
         }
         
         for (int j = 0; j < 10; j++)
         {
            Message m = cons1.receive(1000);
            
            assertNotNull(m);
         }
         
         Message m = cons1.receiveNoWait();
         
         assertNull(m);
         
         sess.close();
         
      }           
      
      super.drainDestination(cf, queue);
   }
      
   public void testWithSelector() throws Exception
   {
      String selector1 = "beatle = 'john'";
      String selector2 = "beatle = 'paul'";
      String selector3 = "beatle = 'george'";
      String selector4 = "beatle = 'ringo'";
      String selector5 = "beatle = 'jesus'";
      
      Connection conn = cf.createConnection();
      conn.start();
      Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageConsumer cons1 = sess.createConsumer(topic, selector1);
      MessageConsumer cons2 = sess.createConsumer(topic, selector2);
      MessageConsumer cons3 = sess.createConsumer(topic, selector3);
      MessageConsumer cons4 = sess.createConsumer(topic, selector4);
      MessageConsumer cons5 = sess.createConsumer(topic, selector5);
      
      Message m1 = sess.createMessage();
      m1.setStringProperty("beatle", "john");
      
      Message m2 = sess.createMessage();
      m2.setStringProperty("beatle", "paul");
      
      Message m3 = sess.createMessage();
      m3.setStringProperty("beatle", "george");
      
      Message m4 = sess.createMessage();
      m4.setStringProperty("beatle", "ringo");
      
      Message m5 = sess.createMessage();
      m5.setStringProperty("beatle", "jesus");
      
      MessageProducer prod = sess.createProducer(topic);
      
      prod.send(m1);
      prod.send(m2);
      prod.send(m3);
      prod.send(m4);
      prod.send(m5);
      
      Message r1 = cons1.receive(500);
      assertNotNull(r1);
      Message n = cons1.receive(500);
      assertNull(n);
      
      Message r2 = cons2.receive(500);
      assertNotNull(r2);
      n = cons2.receive(500);
      assertNull(n);
      
      Message r3 = cons3.receive(500);
      assertNotNull(r3);
      n = cons3.receive(500);
      assertNull(n);
      
      Message r4 = cons4.receive(500);
      assertNotNull(r4);
      n = cons4.receive(500);
      assertNull(n);
      
      Message r5 = cons5.receive(500);
      assertNotNull(r5);
      n = cons5.receive(500);
      assertNull(n);
      
      assertEquals("john", r1.getStringProperty("beatle"));
      assertEquals("paul", r2.getStringProperty("beatle"));
      assertEquals("george", r3.getStringProperty("beatle"));
      assertEquals("ringo", r4.getStringProperty("beatle"));
      assertEquals("jesus", r5.getStringProperty("beatle"));
      
      conn.close();
            
   }

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------   
}
