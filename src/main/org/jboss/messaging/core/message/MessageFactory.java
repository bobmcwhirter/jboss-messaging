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
package org.jboss.messaging.core.message;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jms.destination.JBossDestination;
import org.jboss.jms.message.JBossBytesMessage;
import org.jboss.jms.message.JBossMapMessage;
import org.jboss.jms.message.JBossMessage;
import org.jboss.jms.message.JBossObjectMessage;
import org.jboss.jms.message.JBossStreamMessage;
import org.jboss.jms.message.JBossTextMessage;
import org.jboss.messaging.core.Message;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>  
 * @version <tt>$Revision$</tt>
 * 
 * $Id$
 */
public class MessageFactory
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------

   public static Message createMessage(byte type)
   {
      Message m = null;
      
      if (type == JBossMessage.TYPE)
      {
         m = new JBossMessage();
      }
      else if (type == JBossObjectMessage.TYPE)
      {
         m = new JBossObjectMessage();
      }
      else if (type == JBossTextMessage.TYPE)
      {
         m = new JBossTextMessage();
      }
      else if (type == JBossBytesMessage.TYPE)
      {
         m = new JBossBytesMessage();
      }
      else if (type == JBossMapMessage.TYPE)
      {
         m = new JBossMapMessage();
      }
      else if (type == JBossStreamMessage.TYPE)
      {
         m = new JBossStreamMessage();
      }
      else if (type == CoreMessage.TYPE)
      {
         m = new CoreMessage();
      }
     
      return m;
   }
   
   /*
    * Create a message from persistent storage
    */
   public static Message createMessage(long messageID,
                                       boolean reliable, 
                                       long expiration, 
                                       long timestamp,
                                       byte priority,
                                       Map coreHeaders,
                                       byte[] payloadAsByteArray,                                                                                    
                                       byte type,
                                       String jmsType,                                       
                                       String correlationID,
                                       byte[] correlationIDBytes,
                                       JBossDestination destination,
                                       JBossDestination replyTo, 
                                       long scheduledDeliveryTime,
                                       HashMap jmsProperties)

   {
      Message m = null;
      
      switch (type)
      {
         case JBossMessage.TYPE:
         {
            m = new JBossMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
                     payloadAsByteArray, jmsType, correlationID, correlationIDBytes,
                     destination, replyTo, jmsProperties);
            break;
         }
         case JBossObjectMessage.TYPE:
         {
            m = new JBossObjectMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
                     payloadAsByteArray, jmsType, correlationID, correlationIDBytes,
                     destination, replyTo, jmsProperties);
            break;
         }
         case JBossTextMessage.TYPE:
         {
            m = new JBossTextMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
                     payloadAsByteArray, jmsType, correlationID, correlationIDBytes,
                     destination, replyTo, jmsProperties);
            break;
         }
         case JBossBytesMessage.TYPE:
         {
            m = new JBossBytesMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
                     payloadAsByteArray, jmsType, correlationID, correlationIDBytes,
                     destination, replyTo, jmsProperties);
            break;
         }
         case JBossMapMessage.TYPE:
         {
            m = new JBossMapMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
                     payloadAsByteArray, jmsType, correlationID, correlationIDBytes,
                     destination, replyTo, jmsProperties);
            break;
         }
         case JBossStreamMessage.TYPE:
         {
            m = new JBossStreamMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
                     payloadAsByteArray, jmsType, correlationID, correlationIDBytes,
                     destination, replyTo, jmsProperties);
            break;
         }
         case CoreMessage.TYPE:
         {
            m = new CoreMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders, payloadAsByteArray);
            break;
         }
         default:
         {
            throw new IllegalArgumentException("Unknown type " + type);       
         }
      }
      
      m.setPersisted(true);
      
      return m;
   }

   // Attributes ----------------------------------------------------
   
   // Constructors --------------------------------------------------
   
   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------   
}

