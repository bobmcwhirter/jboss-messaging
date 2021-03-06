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
package org.jboss.test.messaging.jms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.jms.client.JBossConnectionFactory;
import org.jboss.profiler.jvmti.InventoryDataPoint;
import org.jboss.profiler.jvmti.JVMTIInterface;
import org.jboss.test.messaging.JBMServerTestCase;

/**
 * 
 * These tests should be run manually with a profiler running.
 * After running the heap should be inspected to ensure there are no unexpected objects.
 * Ideally we would automate this.
 * 
 * @author <a href="tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class MemLeakTest extends JBMServerTestCase
{
   // Constants -----------------------------------------------------


   private static final Logger log = Logger.getLogger(MemLeakTest.class);


   // Static --------------------------------------------------------

   /*public static void main(String[] args)
   {
      try
      {
         MemLeakTest tests = new MemLeakTest("MemLeakTest");
         tests.setUp();
         tests.testRemotingMemLeaks();
         tests.tearDown();
      }
      catch (Throwable e)
      {
         e.printStackTrace();
      }
   } */

   // Attributes ----------------------------------------------------

   // Constructors --------------------------------------------------

   public MemLeakTest(String name)
   {
      super(name);
   }

   // TestCase overrides -------------------------------------------

   
   /** @todo I can't execute this test if executed with testExpressionParginMessages. That's why I renamed it. */
   public void renamedtestNonTxSendReceiveNP() throws Exception
   {
      log.info("testNonTxSendReceiveNP");
      //Thread.sleep(10000);
      
      InitialContext initialContext = getInitialContext();
      ConnectionFactory cf = (JBossConnectionFactory)initialContext.lookup("/ConnectionFactory");

      createQueue("Queue");
      
      Queue queue = (Queue)initialContext.lookup("/queue/Queue");
      
      Connection conn = cf.createConnection();

      Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageProducer prod = sess.createProducer(queue);

      prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

      final int NUM_MESSAGES = 1000;
      
      //send some messages
      
      conn.start();
      MessageConsumer cons = sess.createConsumer(queue);

      produceMessages(sess, prod, 100, cons);
      
      JVMTIInterface jvmti = new JVMTIInterface();
      Map inventory1=jvmti.produceInventory();
      log.info("Producing first snapshot");
      produceMessages(sess, prod, NUM_MESSAGES, cons);
      log.info("Producing second snapshot");
      jvmti.forceReleaseOnSoftReferences();
      jvmti.forceGC();
      Map inventory2 = jvmti.produceInventory();
      
      log.info("inventory1.size=" + inventory1.size());
      log.info("inventory2.size=" + inventory2.size());
      
      assertTrue("Test produced unexpected objects",jvmti.compareInventories(System.out, inventory1,inventory2,null, null, new InventoryDataPoint[] {new InventoryDataPoint(Object.class,10)}));
            
      conn.close();
      
      conn = null;
      
      sess = null;
      
      prod = null;
      
      cons = null;
      
      queue = null;
      
      cf = null;
   }

   private void produceMessages(Session sess, MessageProducer prod, final int NUM_MESSAGES, MessageConsumer cons) throws JMSException
   {
      for (int i = 0; i < NUM_MESSAGES; i++)
      {

         TextMessage m = sess.createTextMessage("hello" + i);
         
         prod.send(m);
         
         log.info("Sent " + i);

      }
      //receive
      
      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         cons.receive();
         log.info("Received " + i);
      }
   }
   
   public void testExpressionParsingMessages() throws Exception
   {
      log.info("testExpressionParsingMessages");
      //Thread.sleep(10000);
      
      InitialContext initialContext = getInitialContext();
      ConnectionFactory cf = (JBossConnectionFactory)initialContext.lookup("/ConnectionFactory");

      createQueue("Queue");
      
      Queue queue = (Queue)initialContext.lookup("/queue/Queue");
      
      Connection conn = cf.createConnection();

      Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageProducer prod = sess.createProducer(queue);

      prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

      //send some messages
      ArrayList payLoad = new ArrayList();
      for (int i=0;i<100;i++)
      {
         payLoad.add("" + i);
      }
      conn.start();
      MessageConsumer cons1 = sess.createConsumer(queue,"target='1'");
      MessageConsumer cons2 = sess.createConsumer(queue,"target='2'");

      produceMessages(sess, prod, 30, cons1,cons2,payLoad);
      
      JVMTIInterface jvmti = new JVMTIInterface();
      Map inventory1=jvmti.produceInventory();
      log.info("Producing first snapshot");
      produceMessages(sess, prod, 10, cons1,cons2,payLoad);
      produceMessages(sess, prod, 10, cons1,cons2,payLoad);
      produceMessages(sess, prod, 10, cons1,cons2,payLoad);
      produceMessages(sess, prod, 10, cons1,cons2,payLoad);
      produceMessages(sess, prod, 10, cons1,cons2,payLoad);
      produceMessages(sess, prod, 10, cons1,cons2,payLoad);
      log.info("Producing second snapshot");
      Map inventory2 = jvmti.produceInventory();
      
      log.info("inventory1.size=" + inventory1.size());
      log.info("inventory2.size=" + inventory2.size());
      
      assertTrue("Test produced unexpected objects",jvmti.compareInventories(System.out, inventory1,inventory2,null, null, new InventoryDataPoint[] {new InventoryDataPoint(Object.class,10)}));
      
      conn.close();
      
      conn = null;
      
      sess = null;
      
      prod = null;
      
      cons1 = null;
      cons2 = null;
      
      queue = null;
      
      cf = null;
   }

   
   private void produceMessages(Session sess, MessageProducer prod, final int NUM_MESSAGES, MessageConsumer cons1, MessageConsumer cons2, Object payload) throws Exception
   {
      for (int i = 0; i < NUM_MESSAGES; i++)
      {

         ObjectMessage  m = sess.createObjectMessage();
         m.setObject((Serializable)payload);
         if (i%2==0)
         {
            m.setStringProperty("target","1");
         }
         else
         {
            m.setStringProperty("target","2");
         }
         
         
         prod.send(m);
         
         log.info("Sent " + i);

      }
      //receive
      
      for (int i = 0; i < NUM_MESSAGES/2; i++)
      {
         cons1.receive();
         log.info("Received " + i);
      }
      log.info("Starting second queue");
      for (int i = 0; i < NUM_MESSAGES/2; i++)
      {
         cons2.receive();
         log.info("Received " + i);
      }
   }
   


   //   public void testManyConns() throws Exception
//   {
//      log.info("Pausing");
//      Thread.sleep(10000);
//      
//      InitialContext initialContext = new InitialContext(ServerManagement.getJNDIEnvironment());
//      ConnectionFactory cf = (JBossConnectionFactory)initialContext.lookup("/ConnectionFactory");
//
//      ServerManagement.deployQueue("Queue", 10000, 1000, 1000);
//      
//      Queue queue = (Queue)initialContext.lookup("/queue/Queue");
//      
//      
//      for (int i = 0; i < 200; i++)
//      {
//      
//         
//         Connection conn = cf.createConnection();
//   
//         Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
//   
//         MessageProducer prod = sess.createProducer(queue);
//   
//         prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//   
//         //send a message
//         
//         Message m = sess.createTextMessage("hello" + i);
//   
//         prod.send(m);
//         
//         log.info("Sent " + i);
//   
//         conn.start();
//         
//         MessageConsumer cons = sess.createConsumer(queue);
//         
//         //receive
//         
//         m = cons.receive();
//                              
//         conn.close();
//         
//         log.info("i:" + i);
//      }
//
//      queue = null;
//      
//      cf = null;
//      
//      Thread.sleep(20 * 60 * 1000);
//   }
//   
//   public void testNonTxSendReceiveP() throws Exception
//   {
//      log.info("Pausing");
//      Thread.sleep(10000);
//      
//      InitialContext initialContext = new InitialContext(ServerManagement.getJNDIEnvironment());
//      ConnectionFactory cf = (JBossConnectionFactory)initialContext.lookup("/ConnectionFactory");
//
//      ServerManagement.deployQueue("Queue", 10000, 1000, 1000);
//      
//      Queue queue = (Queue)initialContext.lookup("/queue/Queue");
//      
//      Connection conn = cf.createConnection();
//
//      Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
//
//      MessageProducer prod = sess.createProducer(queue);
//
//      prod.setDeliveryMode(DeliveryMode.PERSISTENT);
//
//      final int NUM_MESSAGES = 1000;
//      
//      //send some messages
//      
//      String s = new GUID().toString();
//      
//      for (int i = 0; i < NUM_MESSAGES; i++)
//      {
//
//         TextMessage m = sess.createTextMessage(s);
//
//         prod.send(m);
//         
//         log.info("Sent " + i);
//
//      }
//      
//      conn.start();
//      
//      MessageConsumer cons = sess.createConsumer(queue);
//      
//      //receive
//      
//      for (int i = 0; i < NUM_MESSAGES; i++)
//      {
//
//         Message m = cons.receive();
//         
//         log.info("Received " + i);
//
//      }
//            
//      conn.close();
//      
//      conn = null;
//      
//      sess = null;
//      
//      prod = null;
//      
//      cons = null;
//      
//      queue = null;
//      
//      cf = null;
//      
//      Thread.sleep(20 * 60 * 1000);
//   }
//   
//   
//   public void testTxSendReceiveNP() throws Exception
//   {
//      log.info("Pausing");
//      Thread.sleep(10000);
//      
//      InitialContext initialContext = new InitialContext(ServerManagement.getJNDIEnvironment());
//      ConnectionFactory cf = (JBossConnectionFactory)initialContext.lookup("/ConnectionFactory");
//
//      ServerManagement.deployQueue("Queue", 10000, 1000, 1000);
//      
//      Queue queue = (Queue)initialContext.lookup("/queue/Queue");
//      
//      Connection conn = cf.createConnection();
//
//      Session sess = conn.createSession(true, Session.SESSION_TRANSACTED);
//
//      MessageProducer prod = sess.createProducer(queue);
//
//      prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//
//      final int NUM_MESSAGES = 100;
//      
//      //send some messages
//      
//      String s = new GUID().toString();
//      
//      for (int i = 0; i < NUM_MESSAGES; i++)
//      {
//
//         TextMessage m = sess.createTextMessage(s);
//
//         prod.send(m);
//         
//         log.info("Sent " + i);
//
//      }
//      
//      sess.rollback();
//      
//      for (int i = 0; i < NUM_MESSAGES; i++)
//      {
//
//         TextMessage m = sess.createTextMessage(s);
//
//         prod.send(m);
//         
//         log.info("Sent " + i);
//
//      }
//      
//      sess.commit();
//      
//      conn.start();
//      
//      MessageConsumer cons = sess.createConsumer(queue);
//      
//      //receive
//      
//      for (int i = 0; i < NUM_MESSAGES; i++)
//      {
//
//         Message m = cons.receive();
//         
//         log.info("Received " + i);
//
//      }
//      
//      sess.rollback();
//      
//      for (int i = 0; i < NUM_MESSAGES; i++)
//      {
//
//         Message m = cons.receive();
//         
//         log.info("Received " + i);
//
//      }
//      
//      sess.commit();
//                        
//      conn.close();
//      
//      conn = null;
//      
//      sess = null;
//      
//      prod = null;
//      
//      cons = null;
//      
//      queue = null;
//      
//      cf = null;
//      
//      Thread.sleep(20 * 60 * 1000);
//   }
//   
//   public void testTxSendReceiveP() throws Exception
//   {
//      log.info("Pausing");
//      Thread.sleep(10000);
//      
//      InitialContext initialContext = new InitialContext(ServerManagement.getJNDIEnvironment());
//      ConnectionFactory cf = (JBossConnectionFactory)initialContext.lookup("/ConnectionFactory");
//
//      ServerManagement.deployQueue("Queue", 10000, 1000, 1000);
//      
//      Queue queue = (Queue)initialContext.lookup("/queue/Queue");
//      
//      Connection conn = cf.createConnection();
//
//      Session sess = conn.createSession(true, Session.SESSION_TRANSACTED);
//
//      MessageProducer prod = sess.createProducer(queue);
//
//      prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//
//      final int NUM_MESSAGES = 100;
//      
//      //send some messages
//      
//      String s = new GUID().toString();
//      
//      for (int i = 0; i < NUM_MESSAGES; i++)
//      {
//
//         TextMessage m = sess.createTextMessage(s);
//
//         prod.send(m);
//         
//         log.info("Sent " + i);
//
//      }
//      
//      sess.rollback();
//      
//      for (int i = 0; i < NUM_MESSAGES; i++)
//      {
//
//         TextMessage m = sess.createTextMessage(s);
//
//         prod.send(m);
//         
//         log.info("Sent " + i);
//
//      }
//      
//      sess.commit();
//      
//      conn.start();
//      
//      MessageConsumer cons = sess.createConsumer(queue);
//      
//      //receive
//      
//      for (int i = 0; i < NUM_MESSAGES; i++)
//      {
//
//         Message m = cons.receive();
//         
//         log.info("Received " + i);
//
//      }
//      
//      sess.rollback();
//      
//      for (int i = 0; i < NUM_MESSAGES; i++)
//      {
//
//         Message m = cons.receive();
//         
//         log.info("Received " + i);
//
//      }
//      
//      sess.commit();
//                        
//      conn.close();
//      
//      conn = null;
//      
//      sess = null;
//      
//      prod = null;
//      
//      cons = null;
//      
//      queue = null;
//      
//      cf = null;
//      
//      Thread.sleep(20 * 60 * 1000);
//   }
//   
//   
//   public void testSendReceiveClientAckNP() throws Exception
//   {
//      log.info("Pausing");
//      Thread.sleep(10000);
//      
//      InitialContext initialContext = new InitialContext(ServerManagement.getJNDIEnvironment());
//      ConnectionFactory cf = (JBossConnectionFactory)initialContext.lookup("/ConnectionFactory");
//
//      ServerManagement.deployQueue("Queue", 10000, 1000, 1000);
//      
//      Queue queue = (Queue)initialContext.lookup("/queue/Queue");
//      
//      Connection conn = cf.createConnection();
//
//      Session sess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
//
//      MessageProducer prod = sess.createProducer(queue);
//
//      prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//
//      final int NUM_MESSAGES = 100;
//      
//      //send some messages
//      
//      for (int i = 0; i < NUM_MESSAGES; i++)
//      {
//
//         TextMessage m = sess.createTextMessage("hello" + i);
//
//         prod.send(m);
//         
//         log.info("Sent " + i);
//
//      }
//      
//      conn.start();
//      
//      MessageConsumer cons = sess.createConsumer(queue);
//      
//      //receive
//      
//      Message m = null;
//      
//      for (int i = 0; i < NUM_MESSAGES / 2; i++)
//      {
//
//         m = cons.receive();
//         
//         log.info("Received " + i);
//
//      }
//      
//      m.acknowledge();
//      
//      for (int i = 0; i < NUM_MESSAGES / 2; i++)
//      {
//
//         m = cons.receive();
//         
//         log.info("Received " + i);
//
//      }
//      
//      //don't ack
//      
//      sess.close();
//      
//      sess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
//      
//      cons = sess.createConsumer(queue);
//      
//      //ack the rest
//      
//      for (int i = 0; i < NUM_MESSAGES / 2; i++)
//      {
//
//         m = cons.receive();
//         
//         log.info("Received " + i);
//
//      }
//      
//      m.acknowledge();
//                 
//      conn.close();
//      
//      conn = null;
//      
//      sess = null;
//      
//      prod = null;
//      
//      cons = null;
//      
//      queue = null;
//      
//      cf = null;
//      
//      Thread.sleep(20 * 60 * 1000);
//   }



}
