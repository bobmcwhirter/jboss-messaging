/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.messaging.ra;

import javax.jms.JMSException;
import javax.jms.StreamMessage;

import org.jboss.messaging.core.logging.Logger;

/**
 * A wrapper for a message
 *
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @version $Revision: $
 */
public class JBMStreamMessage extends JBMMessage implements StreamMessage
{
   /** The logger */
   private static final Logger log = Logger.getLogger(JBMStreamMessage.class);

   /** Whether trace is enabled */
   private static boolean trace = log.isTraceEnabled();

   /**
    * Create a new wrapper
    * @param message the message
    * @param session the session
    */
   public JBMStreamMessage(final StreamMessage message, final JBMSession session)
   {
      super(message, session);

      if (trace)
      {
         log.trace("constructor(" + message + ", " + session + ")");
      }
   }

   /**
    * Read 
    * @return The value
    * @exception JMSException Thrown if an error occurs
    */
   public boolean readBoolean() throws JMSException
   {
      if (trace)
      {
         log.trace("readBoolean()");
      }

      return ((StreamMessage)message).readBoolean();
   }

   /**
    * Read 
    * @return The value
    * @exception JMSException Thrown if an error occurs
    */
   public byte readByte() throws JMSException
   {
      if (trace)
      {
         log.trace("readByte()");
      }

      return ((StreamMessage)message).readByte();
   }

   /**
    * Read 
    * @param value The value
    * @return The value
    * @exception JMSException Thrown if an error occurs
    */
   public int readBytes(final byte[] value) throws JMSException
   {
      if (trace)
      {
         log.trace("readBytes(" + value + ")");
      }

      return ((StreamMessage)message).readBytes(value);
   }

   /**
    * Read 
    * @return The value
    * @exception JMSException Thrown if an error occurs
    */
   public char readChar() throws JMSException
   {
      if (trace)
      {
         log.trace("readChar()");
      }

      return ((StreamMessage)message).readChar();
   }

   /**
    * Read 
    * @return The value
    * @exception JMSException Thrown if an error occurs
    */
   public double readDouble() throws JMSException
   {
      if (trace)
      {
         log.trace("readDouble()");
      }

      return ((StreamMessage)message).readDouble();
   }

   /**
    * Read 
    * @return The value
    * @exception JMSException Thrown if an error occurs
    */
   public float readFloat() throws JMSException
   {
      if (trace)
      {
         log.trace("readFloat()");
      }

      return ((StreamMessage)message).readFloat();
   }

   /**
    * Read 
    * @return The value
    * @exception JMSException Thrown if an error occurs
    */
   public int readInt() throws JMSException
   {
      if (trace)
      {
         log.trace("readInt()");
      }

      return ((StreamMessage)message).readInt();
   }

   /**
    * Read 
    * @return The value
    * @exception JMSException Thrown if an error occurs
    */
   public long readLong() throws JMSException
   {
      if (trace)
      {
         log.trace("readLong()");
      }

      return ((StreamMessage)message).readLong();
   }

   /**
    * Read 
    * @return The value
    * @exception JMSException Thrown if an error occurs
    */
   public Object readObject() throws JMSException
   {
      if (trace)
      {
         log.trace("readObject()");
      }

      return ((StreamMessage)message).readObject();
   }

   /**
    * Read 
    * @return The value
    * @exception JMSException Thrown if an error occurs
    */
   public short readShort() throws JMSException
   {
      if (trace)
      {
         log.trace("readShort()");
      }

      return ((StreamMessage)message).readShort();
   }

   /**
    * Read 
    * @return The value
    * @exception JMSException Thrown if an error occurs
    */
   public String readString() throws JMSException
   {
      if (trace)
      {
         log.trace("readString()");
      }

      return ((StreamMessage)message).readString();
   }

   /**
    * Reset 
    * @exception JMSException Thrown if an error occurs
    */
   public void reset() throws JMSException
   {
      if (trace)
      {
         log.trace("reset()");
      }

      ((StreamMessage)message).reset();
   }

   /**
    * Write
    * @param value The value 
    * @exception JMSException Thrown if an error occurs
    */
   public void writeBoolean(final boolean value) throws JMSException
   {
      if (trace)
      {
         log.trace("writeBoolean(" + value + ")");
      }

      ((StreamMessage)message).writeBoolean(value);
   }

   /**
    * Write
    * @param value The value 
    * @exception JMSException Thrown if an error occurs
    */
   public void writeByte(final byte value) throws JMSException
   {
      if (trace)
      {
         log.trace("writeByte(" + value + ")");
      }

      ((StreamMessage)message).writeByte(value);
   }

   /**
    * Write
    * @param value The value 
    * @param offset The offset
    * @param length The length
    * @exception JMSException Thrown if an error occurs
    */
   public void writeBytes(final byte[] value, final int offset, final int length) throws JMSException
   {
      if (trace)
      {
         log.trace("writeBytes(" + value + ", " + offset + ", " + length + ")");
      }

      ((StreamMessage)message).writeBytes(value, offset, length);
   }

   /**
    * Write
    * @param value The value 
    * @exception JMSException Thrown if an error occurs
    */
   public void writeBytes(final byte[] value) throws JMSException
   {
      if (trace)
      {
         log.trace("writeBytes(" + value + ")");
      }

      ((StreamMessage)message).writeBytes(value);
   }

   /**
    * Write
    * @param value The value 
    * @exception JMSException Thrown if an error occurs
    */
   public void writeChar(final char value) throws JMSException
   {
      if (trace)
      {
         log.trace("writeChar(" + value + ")");
      }

      ((StreamMessage)message).writeChar(value);
   }

   /**
    * Write
    * @param value The value 
    * @exception JMSException Thrown if an error occurs
    */
   public void writeDouble(final double value) throws JMSException
   {
      if (trace)
      {
         log.trace("writeDouble(" + value + ")");
      }

      ((StreamMessage)message).writeDouble(value);
   }

   /**
    * Write
    * @param value The value 
    * @exception JMSException Thrown if an error occurs
    */
   public void writeFloat(final float value) throws JMSException
   {
      if (trace)
      {
         log.trace("writeFloat(" + value + ")");
      }

      ((StreamMessage)message).writeFloat(value);
   }

   /**
    * Write
    * @param value The value 
    * @exception JMSException Thrown if an error occurs
    */
   public void writeInt(final int value) throws JMSException
   {
      if (trace)
      {
         log.trace("writeInt(" + value + ")");
      }

      ((StreamMessage)message).writeInt(value);
   }

   /**
    * Write
    * @param value The value 
    * @exception JMSException Thrown if an error occurs
    */
   public void writeLong(final long value) throws JMSException
   {
      if (trace)
      {
         log.trace("writeLong(" + value + ")");
      }

      ((StreamMessage)message).writeLong(value);
   }

   /**
    * Write
    * @param value The value 
    * @exception JMSException Thrown if an error occurs
    */
   public void writeObject(final Object value) throws JMSException
   {
      if (trace)
      {
         log.trace("writeObject(" + value + ")");
      }

      ((StreamMessage)message).writeObject(value);
   }

   /**
    * Write
    * @param value The value 
    * @exception JMSException Thrown if an error occurs
    */
   public void writeShort(final short value) throws JMSException
   {
      if (trace)
      {
         log.trace("writeShort(" + value + ")");
      }

      ((StreamMessage)message).writeShort(value);
   }

   /**
    * Write
    * @param value The value 
    * @exception JMSException Thrown if an error occurs
    */
   public void writeString(final String value) throws JMSException
   {
      if (trace)
      {
         log.trace("writeString(" + value + ")");
      }

      ((StreamMessage)message).writeString(value);
   }
}
