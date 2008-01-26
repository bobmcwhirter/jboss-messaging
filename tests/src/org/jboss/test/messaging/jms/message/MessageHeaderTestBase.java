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
package org.jboss.test.messaging.jms.message;

import java.util.Arrays;
import java.util.Enumeration;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageEOFException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import org.jboss.jms.destination.JBossQueue;
import org.jboss.jms.message.JBossBytesMessage;
import org.jboss.jms.message.JBossMapMessage;
import org.jboss.jms.message.JBossMessage;
import org.jboss.jms.message.JBossObjectMessage;
import org.jboss.jms.message.JBossStreamMessage;
import org.jboss.jms.message.JBossTextMessage;
import org.jboss.test.messaging.JBMServerTestCase;

/**
 *
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision: 2883 $</tt>
 *
 * $Id: MessageTest.java 2883 2007-07-12 23:36:16Z timfox $
 */
public class MessageHeaderTestBase extends JBMServerTestCase
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------

   /**
    * Loads the message header fields with significant values.
    */
   public static void configureMessage(JBossMessage m) throws JMSException
   {
      m.setJMSMessageID("ID:messageID777");
      m.setJMSTimestamp(123456789l);
      m.setJMSCorrelationID("correlationID777");
      m.setJMSReplyTo(new JBossQueue("ReplyToQueue"));
      m.setJMSDestination(new JBossQueue("DestinationQueue"));
      m.setJMSDeliveryMode(DeliveryMode.PERSISTENT);      
      m.setJMSExpiration(987654321l);
      m.setJMSPriority(9);
      m.setBooleanProperty("booleanProperty", true);
      m.setByteProperty("byteProperty", (byte)2);
      m.setShortProperty("shortProperty", (short)3);
      m.setIntProperty("intProperty", 4);
      m.setLongProperty("longProperty", 5l);
      m.setFloatProperty("floatProperty", 6);
      m.setDoubleProperty("doubleProperty", 7);
      m.setStringProperty("stringPoperty", "someString");
   }

   /**
    * Makes sure two physically different message are equivalent: they have identical JMS fields and
    * body.
    */
   public static void ensureEquivalent(Message m1, JBossMessage m2) throws JMSException
   {
      assertTrue(m1 != m2);
      
      //Can't compare message id since not set until send

      assertEquals(m1.getJMSTimestamp(), m2.getJMSTimestamp());

      byte[] corrIDBytes = null;
      String corrIDString = null;

      try
      {
         corrIDBytes = m1.getJMSCorrelationIDAsBytes();
      }
      catch(JMSException e)
      {
         // correlation ID specified as String
         corrIDString = m1.getJMSCorrelationID();
      }

      if (corrIDBytes != null)
      {
         assertTrue(Arrays.equals(corrIDBytes, m2.getJMSCorrelationIDAsBytes()));
      }
      else if (corrIDString != null)
      {
         assertEquals(corrIDString, m2.getJMSCorrelationID());
      }
      else
      {
         // no correlation id

         try
         {
            byte[] corrID2 = m2.getJMSCorrelationIDAsBytes();
            assertNull(corrID2);
         }
         catch(JMSException e)
         {
            // correlatin ID specified as String
            String corrID2 = m2.getJMSCorrelationID();
            assertNull(corrID2);
         }
      }
      assertEquals(m1.getJMSReplyTo(), m2.getJMSReplyTo());
      assertEquals(m1.getJMSDestination(), m2.getJMSDestination());
      assertEquals(m1.getJMSDeliveryMode(), m2.getJMSDeliveryMode());
      //We don't check redelivered since this is always dealt with on the proxy
      assertEquals(m1.getJMSType(), m2.getJMSType());
      assertEquals(m1.getJMSExpiration(), m2.getJMSExpiration());
      assertEquals(m1.getJMSPriority(), m2.getJMSPriority());

      int m1PropertyCount = 0, m2PropertyCount = 0;
      for(Enumeration p = m1.getPropertyNames(); p.hasMoreElements(); )
      {
         String name = (String)p.nextElement();
         
         if (!name.startsWith("JMSX"))
         {
            m1PropertyCount++;
         }
      }
      for(Enumeration p = m2.getPropertyNames(); p.hasMoreElements();)
      {
         String name = (String)p.nextElement();
         
         if (!name.startsWith("JMSX"))
         {
            m2PropertyCount++;
         }
      }

      assertEquals(m1PropertyCount, m2PropertyCount);

      for(Enumeration props = m1.getPropertyNames(); props.hasMoreElements(); )
      {
         boolean found = false;

         String name = (String)props.nextElement();
         
         if (name.startsWith("JMSX"))
         {
            //ignore
            continue;
         }

         boolean booleanProperty = false;
         try
         {
            booleanProperty = m1.getBooleanProperty(name);
            found = true;
         }
         catch(JMSException e)
         {
            // not a boolean
         }

         if (found)
         {
            assertEquals(booleanProperty, m2.getBooleanProperty(name));
            continue;
         }

         byte byteProperty = 0;
         try
         {
            byteProperty = m1.getByteProperty(name);
            found = true;
         }
         catch(JMSException e)
         {
            // not a byte
         }

         if (found)
         {
            assertEquals(byteProperty, m2.getByteProperty(name));
            continue;
         }

         short shortProperty = 0;
         try
         {
            shortProperty = m1.getShortProperty(name);
            found = true;
         }
         catch(JMSException e)
         {
            // not a short
         }

         if (found)
         {
            assertEquals(shortProperty, m2.getShortProperty(name));
            continue;
         }


         int intProperty = 0;
         try
         {
            intProperty = m1.getIntProperty(name);
            found = true;
         }
         catch(JMSException e)
         {
            // not a int
         }

         if (found)
         {
            assertEquals(intProperty, m2.getIntProperty(name));
            continue;
         }


         long longProperty = 0;
         try
         {
            longProperty = m1.getLongProperty(name);
            found = true;
         }
         catch(JMSException e)
         {
            // not a long
         }

         if (found)
         {
            assertEquals(longProperty, m2.getLongProperty(name));
            continue;
         }


         float floatProperty = 0;
         try
         {
            floatProperty = m1.getFloatProperty(name);
            found = true;
         }
         catch(JMSException e)
         {
            // not a float
         }

         if (found)
         {
            assertTrue(floatProperty == m2.getFloatProperty(name));
            continue;
         }

         double doubleProperty = 0;
         try
         {
            doubleProperty = m1.getDoubleProperty(name);
            found = true;
         }
         catch(JMSException e)
         {
            // not a double
         }

         if (found)
         {
            assertTrue(doubleProperty == m2.getDoubleProperty(name));
            continue;
         }

         String stringProperty = null;
         try
         {
            stringProperty = m1.getStringProperty(name);
            found = true;
         }
         catch(JMSException e)
         {
            // not a String
         }

         if (found)
         {
            assertEquals(stringProperty, m2.getStringProperty(name));
            continue;
         }


         fail("Cannot identify property " + name);
      }
   }

   public static void ensureEquivalent(BytesMessage m1, JBossBytesMessage m2) throws JMSException
   {
      ensureEquivalent((Message)m1, m2);

      long len = m1.getBodyLength();
      for(int i = 0; i < len; i++)
      {
         assertEquals(m1.readByte(), m2.readByte());
      }

      try
      {
         m1.readByte();
         fail("should throw MessageEOFException");
      }
      catch(MessageEOFException e)
      {
         // OK
      }

      try
      {
         m2.readByte();
         fail("should throw MessageEOFException");
      }
      catch(MessageEOFException e)
      {
         // OK
      }
   }

   public static void ensureEquivalent(MapMessage m1, JBossMapMessage m2) throws JMSException
   {
      ensureEquivalent((Message)m1, m2);

      for(Enumeration e = m1.getMapNames(); e.hasMoreElements(); )
      {
         String name = (String)e.nextElement();
         assertEquals(m1.getObject(name), m2.getObject(name));
      }

      for(Enumeration e = m2.getMapNames(); e.hasMoreElements(); )
      {
         String name = (String)e.nextElement();
         assertEquals(m2.getObject(name), m1.getObject(name));
      }
   }

   public static void ensureEquivalent(ObjectMessage m1, JBossObjectMessage m2) throws JMSException
   {
      ensureEquivalent((Message)m1, m2);
      assertEquals(m1.getObject(), m2.getObject());
   }

   public static void ensureEquivalent(StreamMessage m1, JBossStreamMessage m2) throws JMSException
   {
      ensureEquivalent((Message)m1, m2);

      m1.reset();
      m2.reset();
      boolean m1eof = false, m2eof = false;
      while(true)
      {
         byte b1, b2;
         try
         {
            b1 = m1.readByte();
         }
         catch(MessageEOFException e)
         {
            m1eof = true;
            break;
         }

         try
         {
            b2 = m2.readByte();
         }
         catch(MessageEOFException e)
         {
            m2eof = true;
            break;
         }

         assertEquals(b1, b2);
      }


      if (m1eof)
      {
         try
         {
            m2.readByte();
            fail("should throw MessageEOFException");
         }
         catch(MessageEOFException e)
         {
            // OK
         }
      }

      if (m2eof)
      {
         try
         {
            m1.readByte();
            fail("should throw MessageEOFException");
         }
         catch(MessageEOFException e)
         {
            // OK
         }
      }
   }
   
   public static void ensureEquivalent(TextMessage m1, JBossTextMessage m2) throws JMSException
   {
      ensureEquivalent((Message)m1, m2);
      assertEquals(m1.getText(), m2.getText());
   }

   // Attributes ----------------------------------------------------

   protected Connection producerConnection, consumerConnection;
   protected Session queueProducerSession, queueConsumerSession;
   protected MessageProducer queueProducer;
   protected MessageConsumer queueConsumer;
   protected Session topicProducerSession, topicConsumerSession;
   protected MessageProducer topicProducer;
   protected MessageConsumer topicConsumer;
   
   
   // Constructors --------------------------------------------------

   public MessageHeaderTestBase(String name)
   {
      super(name);
   }

   // Public --------------------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();

      producerConnection = getConnectionFactory().createConnection();
      consumerConnection = getConnectionFactory().createConnection();

      queueProducerSession = producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      queueConsumerSession = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      queueProducer = queueProducerSession.createProducer(queue1);
      queueConsumer = queueConsumerSession.createConsumer(queue1);
      
      topicProducerSession = producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      topicConsumerSession = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      topicProducer = topicProducerSession.createProducer(topic1);
      topicConsumer = topicConsumerSession.createConsumer(topic1);

      consumerConnection.start();
   }

   public void tearDown() throws Exception
   {
      producerConnection.close();
      consumerConnection.close();
      
      super.tearDown();
      
   }
   
   

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
   
}
