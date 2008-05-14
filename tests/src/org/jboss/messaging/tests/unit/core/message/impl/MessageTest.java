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
package org.jboss.messaging.tests.unit.core.message.impl;

import java.nio.ByteBuffer;

import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.impl.ClientMessageImpl;
import org.jboss.messaging.core.journal.EncodingSupport;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.impl.mina.BufferWrapper;
import org.jboss.messaging.core.server.MessageReference;
import org.jboss.messaging.core.server.Queue;
import org.jboss.messaging.core.server.QueueFactory;
import org.jboss.messaging.core.server.ServerMessage;
import org.jboss.messaging.core.server.impl.ServerMessageImpl;
import org.jboss.messaging.tests.unit.core.server.impl.fakes.FakeQueueFactory;
import org.jboss.messaging.tests.util.UnitTestCase;
import org.jboss.messaging.util.ByteBufferWrapper;
import org.jboss.messaging.util.SimpleString;
import org.jboss.messaging.util.TypedProperties;

/**
 * 
 * Tests for Message and MessageReference
 * 
 * TODO - Test streaming and destreaming
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class MessageTest extends UnitTestCase
{
   private static final Logger log = Logger.getLogger(MessageTest.class);

   private QueueFactory queueFactory = new FakeQueueFactory();
   
   public void testCreateMessageBeforeSending()
   {
      long id = 56465;
      int type = 655;
      boolean reliable = true;
      long expiration = 6712671;
      long timestamp = 82798172;
      byte priority = 32;
      
      ClientMessage message = new ClientMessageImpl(type, reliable, expiration, timestamp, priority);
  
      assertEquals(type, message.getType());
      assertEquals(reliable, message.isDurable());
      assertEquals(timestamp, message.getTimestamp());
      assertEquals(priority, message.getPriority());
      
      reliable = false;
      
      message = new ClientMessageImpl(type, reliable, expiration, timestamp, priority);

      assertEquals(type, message.getType());
      assertEquals(reliable, message.isDurable());
      assertEquals(timestamp, message.getTimestamp());
      assertEquals(priority, message.getPriority());
   }
   
   public void testCreateMessageFromStorage() throws Exception
   {
      long id = 56465;

      ServerMessage message = new ServerMessageImpl(id);
      
      assertEquals(id, message.getMessageID());
   }
   
   public void testSetAndGetMessageID()
   {
      ServerMessage message = new ServerMessageImpl();
      
      assertEquals(0, message.getMessageID());
      
      message = new ServerMessageImpl(23);
      
      assertEquals(23, message.getMessageID());
      
      long id = 765432;
      message.setMessageID(id);
      assertEquals(id, message.getMessageID());
   }
   
   public void testSetAndGetReliable()
   {
      ServerMessage message = new ServerMessageImpl();
      
      boolean reliable = true;
      message.setDurable(reliable);
      assertEquals(reliable, message.isDurable());
      
      reliable = false;
      message.setDurable(reliable);
      assertEquals(reliable, message.isDurable());
   }
    
   public void testSetAndGetExpiration()
   {
      ServerMessage message = new ServerMessageImpl();
      
      long expiration = System.currentTimeMillis() + 10000;
      message.setExpiration(expiration);
      assertEquals(expiration, message.getExpiration());
      assertFalse(message.isExpired());
      message.setExpiration(System.currentTimeMillis() - 1);
      assertTrue(message.isExpired());
      
      expiration = 0; //O means never expire
      message.setExpiration(expiration);
      assertEquals(expiration, message.getExpiration());
      assertFalse(message.isExpired());
   }
      
   public void testSetAndGetTimestamp()
   {
      ServerMessage message = new ServerMessageImpl();
      
      long timestamp = System.currentTimeMillis();
      message.setTimestamp(timestamp);
      assertEquals(timestamp, message.getTimestamp());
   }
   
   public void testSetAndGetPriority()
   {
      ServerMessage message = new ServerMessageImpl();
      byte priority = 7;
      message.setPriority(priority);
      assertEquals(priority, message.getPriority());
   }
   
   public void testSetAndGetConnectionID()
   {
      ServerMessage message = new ServerMessageImpl();
      
      assertEquals(0, message.getConnectionID());
      long connectionID = 781628;
      message.setConnectionID(connectionID);
      assertEquals(connectionID, message.getConnectionID());      
   }

   public void testMessageReference()
   {
      ServerMessage message = new ServerMessageImpl();
      
      SimpleString squeue1 = new SimpleString("queue1");
      SimpleString squeue2 = new SimpleString("queue2");
      SimpleString squeue3 = new SimpleString("queue3");
      
      Queue queue1 = queueFactory.createQueue(1, squeue1, null, false, true);
      Queue queue2 = queueFactory.createQueue(2, squeue2, null, false, true);
   
      MessageReference ref1 = message.createReference(queue1);
      MessageReference ref2 = message.createReference(queue2);
      MessageReference ref3 = message.createReference(queue1);
      MessageReference ref4 = message.createReference(queue2);
      
      assertEquals(queue1, ref1.getQueue());
      assertEquals(queue2, ref2.getQueue());
      assertEquals(queue1, ref3.getQueue());
      assertEquals(queue2, ref4.getQueue());
      
      int deliveryCount = 65235;
      ref1.setDeliveryCount(deliveryCount);
      assertEquals(deliveryCount, ref1.getDeliveryCount());
      
      long scheduledDeliveryTime = 908298123;
      ref1.setScheduledDeliveryTime(scheduledDeliveryTime);
      assertEquals(scheduledDeliveryTime, ref1.getScheduledDeliveryTime());
      
      Queue queue3 = queueFactory.createQueue(3, squeue3, null, false, true);
      MessageReference ref5 = ref1.copy(queue3);
      
      assertEquals(deliveryCount, ref5.getDeliveryCount());
      assertEquals(scheduledDeliveryTime, ref5.getScheduledDeliveryTime());
      assertEquals(queue3, ref5.getQueue());
   }
   

   public void testDurableReferences()
   {
      ServerMessage messageDurable = new ServerMessageImpl();
      messageDurable.setDurable(true);
      
      ServerMessage messageNonDurable = new ServerMessageImpl();
      messageNonDurable.setDurable(false);
      
      SimpleString squeue1 = new SimpleString("queue1");
      SimpleString squeue2 = new SimpleString("queue2");
        
      //Durable queue
      Queue queue1 = queueFactory.createQueue(1, squeue1, null, true, false);
      
      //Non durable queue
      Queue queue2 = queueFactory.createQueue(2, squeue2, null, false, false);
      
      assertEquals(0, messageDurable.getDurableRefCount());
      
      MessageReference ref1 = messageDurable.createReference(queue1);
      
      assertEquals(1, messageDurable.getDurableRefCount());
      
      MessageReference ref2 = messageDurable.createReference(queue2);
      
      assertEquals(1, messageDurable.getDurableRefCount());
      
      assertEquals(0, messageNonDurable.getDurableRefCount());
      
      MessageReference ref3 = messageNonDurable.createReference(queue1);
      
      assertEquals(0, messageNonDurable.getDurableRefCount());
      
      MessageReference ref4 = messageNonDurable.createReference(queue2);
      
      assertEquals(0, messageNonDurable.getDurableRefCount());
               
      MessageReference ref5 = messageDurable.createReference(queue1);
      
      assertEquals(2, messageDurable.getDurableRefCount());
      
      messageDurable.decrementDurableRefCount();
      
      assertEquals(1, messageDurable.getDurableRefCount());
      
      messageDurable.decrementDurableRefCount();
      
      assertEquals(0, messageDurable.getDurableRefCount());
      
      messageDurable.incrementDurableRefCount();
      
      assertEquals(1, messageDurable.getDurableRefCount());                 
   }

   public void testEncodingMessageProperties()
   {

      TypedProperties properties = new TypedProperties();
      properties.putStringProperty(new SimpleString("str"), new SimpleString("Str2"));
      properties.putStringProperty(new SimpleString("str2"), new SimpleString("Str2"));
      properties.putBooleanProperty(new SimpleString("str3"), true );
      properties.putByteProperty(new SimpleString("str4"), (byte)1);
      properties.putBytesProperty(new SimpleString("str5"), new byte[]{1,2,3,4,5});
      properties.putShortProperty(new SimpleString("str6"),(short)1);
      properties.putIntProperty(new SimpleString("str7"), (int)1);
      properties.putLongProperty(new SimpleString("str8"), (long)1);
      properties.putFloatProperty(new SimpleString("str9"),(float) 1);
      properties.putDoubleProperty(new SimpleString("str10"), (double) 1);
      properties.putCharProperty(new SimpleString("str11"), 'a');
      
      checkSizes(properties, new TypedProperties());
      
      properties.removeProperty(new SimpleString("str"));
      checkSizes(properties, new TypedProperties());
      
   }

   public void testEncodingMessage() throws Exception
   {
      byte[] bytes = new byte[]{(byte)1, (byte)2, (byte)3};
      final BufferWrapper bufferBody = new BufferWrapper(bytes.length);
      bufferBody.putBytes(bytes);
      
      
      SimpleString address = new SimpleString("Simple Destination ");
      
      ServerMessageImpl implMsg = new ServerMessageImpl(/* type */ 1, /* durable */ true, /* expiration */ 0,
            /* timestamp */ 0, /* priority */(byte)0);
      
      implMsg.setDestination(address);
      implMsg.setBody(bufferBody);
      implMsg.putStringProperty(new SimpleString("Key"), new SimpleString("This String is worthless!"));
      implMsg.putStringProperty(new SimpleString("Key"), new SimpleString("This String is worthless and bigger!"));
      implMsg.putStringProperty(new SimpleString("Key2"), new SimpleString("This String is worthless and bigger and bigger!"));
      implMsg.removeProperty(new SimpleString("Key2"));

      checkSizes(implMsg, new ServerMessageImpl());

      implMsg.removeProperty(new SimpleString("Key"));
      
      checkSizes(implMsg, new ServerMessageImpl());

   }
   
   private void checkSizes(EncodingSupport obj, EncodingSupport newObject)
   {
      ByteBuffer bf = ByteBuffer.allocateDirect(1024);
      ByteBufferWrapper buffer = new ByteBufferWrapper(bf);
      obj.encode(buffer);
      assertEquals (buffer.position(), obj.encodeSize());
      int originalSize = buffer.position();
      
      bf.rewind();
      newObject.decode(buffer);
      
      log.info("Obj.size = " + obj.encodeSize() + " newObject.size = " + newObject.encodeSize());
      
      bf = ByteBuffer.allocateDirect(1024 * 10);
      buffer = new ByteBufferWrapper(bf);
      
      newObject.encode(buffer);
      
      assertEquals(newObject.encodeSize(), bf.position());
      assertEquals(originalSize, bf.position());
      
      
      
      
   }
   

   
}
