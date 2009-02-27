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

package org.jboss.messaging.jms.client;

import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.StreamMessage;

import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.utils.DataConstants;

/**
 * This class implements javax.jms.StreamMessage.
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Some parts based on JBM 1.x class by:
 * 
 * @author Norbert Lataille (Norbert.Lataille@m4x.org)
 * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 * @author <a href="mailto:ataylor@redhat.com">Andy Taylor</a>
 * 
 * @version $Revision: 3412 $
 *
 * $Id: JBossStreamMessage.java 3412 2007-12-05 19:41:47Z timfox $
 */
public class JBossStreamMessage extends JBossMessage implements StreamMessage
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(JBossStreamMessage.class);
   
   
   public static final byte TYPE = 6;

   // Attributes ----------------------------------------------------
 
   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   /*
    * This constructor is used to construct messages prior to sending
    */
   public JBossStreamMessage()
   {   
      super(JBossStreamMessage.TYPE);
   }

   public JBossStreamMessage(final ClientSession session)
   {   
      super(JBossStreamMessage.TYPE, session);
   }
   
   public JBossStreamMessage(final ClientMessage message, final ClientSession session)
   {
      super(message, session);
   }
   
   public JBossStreamMessage(final StreamMessage foreign, final ClientSession session) throws JMSException
   {
      super(foreign, JBossStreamMessage.TYPE, session);
      
      foreign.reset();
      
      try
      {
         while (true)
         {
            Object obj = foreign.readObject();
            this.writeObject(obj);
         }
      }
      catch (MessageEOFException e)
      {
         //Ignore
      }
   }

   // Public --------------------------------------------------------

   public byte getType()
   {
      return JBossStreamMessage.TYPE;
   }
   
   // StreamMessage implementation ----------------------------------

   public boolean readBoolean() throws JMSException
   {
      checkRead();
      try
      {
         byte type = getBody().readByte();
         
         switch (type)
         {
            case DataConstants.BOOLEAN:
               return getBody().readBoolean();
            case DataConstants.STRING:
               String s = getBody().readNullableString();
               return Boolean.valueOf(s);
            default:
               throw new MessageFormatException("Invalid conversion");           
         }
      }
      catch (IndexOutOfBoundsException e)
      {
         throw new MessageEOFException("");
      }
   }

   public byte readByte() throws JMSException
   {
      checkRead();
      try
      {
         byte type = getBody().readByte();
         switch (type)
         {
            case DataConstants.BYTE:
               return getBody().readByte();
            case DataConstants.STRING:
               String s = getBody().readNullableString();
               return Byte.parseByte(s);
            default:
               throw new MessageFormatException("Invalid conversion");           
         }
      }
      catch (IndexOutOfBoundsException e)
      {
         throw new MessageEOFException("");
      }
   }

   public short readShort() throws JMSException
   {
      checkRead();
      try
      {
         byte type = getBody().readByte();
         switch (type)
         {
            case DataConstants.BYTE:
               return getBody().readByte();
            case DataConstants.SHORT:
               return getBody().readShort();
            case DataConstants.STRING:
               String s = getBody().readNullableString();
               return Short.parseShort(s);
            default:
               throw new MessageFormatException("Invalid conversion");           
         }
      }
      catch (IndexOutOfBoundsException e)
      {
         throw new MessageEOFException("");
      }
   }

   public char readChar() throws JMSException
   {
      checkRead();
      try
      {
         byte type = getBody().readByte();
         switch (type)
         {
            case DataConstants.CHAR:
               return getBody().readChar();
            default:
               throw new MessageFormatException("Invalid conversion");           
         }
      }
      catch (IndexOutOfBoundsException e)
      {
         throw new MessageEOFException("");
      }
   }

   public int readInt() throws JMSException
   {
      checkRead();
      try
      {
         byte type = getBody().readByte();
         switch (type)
         {
            case DataConstants.BYTE:
               return getBody().readByte();
            case DataConstants.SHORT:
               return getBody().readShort();
            case DataConstants.INT:
               return getBody().readInt();
            case DataConstants.STRING:
               String s = getBody().readNullableString();
               return Integer.parseInt(s);
            default:
               throw new MessageFormatException("Invalid conversion");           
         }
      }
      catch (IndexOutOfBoundsException e)
      {
         throw new MessageEOFException("");
      }
   }

   public long readLong() throws JMSException
   {
      checkRead();
      try
      {
         byte type = getBody().readByte();
         switch (type)
         {
            case DataConstants.BYTE:
               return getBody().readByte();
            case DataConstants.SHORT:
               return getBody().readShort();
            case DataConstants.INT:
               return getBody().readInt();
            case DataConstants.LONG:
               return getBody().readLong();
            case DataConstants.STRING:
               String s = getBody().readNullableString();
               return Long.parseLong(s);
            default:
               throw new MessageFormatException("Invalid conversion");           
         }
      }
      catch (IndexOutOfBoundsException e)
      {
         throw new MessageEOFException("");
      }
   }

   public float readFloat() throws JMSException
   {
      checkRead();
      try
      {
         byte type = getBody().readByte();
         switch (type)
         {
            case DataConstants.FLOAT:
               return getBody().readFloat();
            case DataConstants.STRING:
               String s = getBody().readNullableString();
               return Float.parseFloat(s);
            default:
               throw new MessageFormatException("Invalid conversion");           
         }
      }
      catch (IndexOutOfBoundsException e)
      {
         throw new MessageEOFException("");
      }
   }

   public double readDouble() throws JMSException
   {
      checkRead();
      try
      {
         byte type = getBody().readByte();
         switch (type)
         {
            case DataConstants.FLOAT:
               return getBody().readFloat();
            case DataConstants.DOUBLE:
               return getBody().readDouble();
            case DataConstants.STRING:
               String s = getBody().readNullableString();
               return Double.parseDouble(s);
            default:
               throw new MessageFormatException("Invalid conversion: " + type);           
         }
      }
      catch (IndexOutOfBoundsException e)
      {
         throw new MessageEOFException("");
      }
   }
   
   public String readString() throws JMSException
   {
      checkRead();
      try
      {
         byte type = getBody().readByte();
         switch (type)
         {
            case DataConstants.BOOLEAN:
               return String.valueOf(getBody().readBoolean());
            case DataConstants.BYTE:
               return String.valueOf(getBody().readByte());
            case DataConstants.SHORT:
               return String.valueOf(getBody().readShort());
            case DataConstants.CHAR:
               return String.valueOf(getBody().readChar());
            case DataConstants.INT:
               return String.valueOf(getBody().readInt());
            case DataConstants.LONG:
               return String.valueOf(getBody().readLong());
            case DataConstants.FLOAT:
               return String.valueOf(getBody().readFloat());
            case DataConstants.DOUBLE:
               return String.valueOf(getBody().readDouble());
            case DataConstants.STRING:
               return getBody().readNullableString();
            default:
               throw new MessageFormatException("Invalid conversion");           
         }
      }
      catch (IndexOutOfBoundsException e)
      {
         throw new MessageEOFException("");
      }
   }

   private int len;
   
   public int readBytes(final byte[] value) throws JMSException
   {
      checkRead();
      try
      {
         if (len == -1)
         {
            len = 0;
            return -1;
         }
         else if (len == 0)
         {
            byte type = getBody().readByte();
            if (type != DataConstants.BYTES)
            {
               throw new MessageFormatException("Invalid conversion"); 
            }
            len = getBody().readInt();       
         }     
         int read = Math.min(value.length, len);
         getBody().readBytes(value, 0, read);
         len -= read;
         if (len == 0)
         {
            len = -1;
         }
         return read;      
      }
      catch (IndexOutOfBoundsException e)
      {
         throw new MessageEOFException("");
      }
   }
   
   public Object readObject() throws JMSException
   {
      checkRead();
      byte type = getBody().readByte();
      switch (type)
      {
         case DataConstants.BOOLEAN:
            return getBody().readBoolean();
         case DataConstants.BYTE:
            return getBody().readByte();
         case DataConstants.SHORT:
            return getBody().readShort();
         case DataConstants.CHAR:
            return getBody().readChar();
         case DataConstants.INT:
            return getBody().readInt();
         case DataConstants.LONG:
            return getBody().readLong();
         case DataConstants.FLOAT:
            return getBody().readFloat();
         case DataConstants.DOUBLE:
            return getBody().readDouble();
         case DataConstants.STRING:
            return getBody().readNullableString();         
         case DataConstants.BYTES:
            int len = getBody().readInt();
            byte[] bytes = new byte[len];
            getBody().readBytes(bytes);
            return bytes;
         default:
            throw new MessageFormatException("Invalid conversion");           
      }
   }

   public void writeBoolean(final boolean value) throws JMSException
   {
      checkWrite();
      getBody().writeByte(DataConstants.BOOLEAN);
      getBody().writeBoolean(value);
   }

   public void writeByte(final byte value) throws JMSException
   {
      checkWrite();
      getBody().writeByte(DataConstants.BYTE);
      getBody().writeByte(value);
   }

   public void writeShort(final short value) throws JMSException
   {
      checkWrite();
      getBody().writeByte(DataConstants.SHORT);
      getBody().writeShort(value);
   }

   public void writeChar(final char value) throws JMSException
   {
      checkWrite();
      getBody().writeByte(DataConstants.CHAR);
      getBody().writeChar(value);
   }

   public void writeInt(final int value) throws JMSException
   {
      checkWrite();
      getBody().writeByte(DataConstants.INT);
      getBody().writeInt(value);
   }

   public void writeLong(final long value) throws JMSException
   {
      checkWrite();
      getBody().writeByte(DataConstants.LONG);
      getBody().writeLong(value);
   }

   public void writeFloat(final float value) throws JMSException
   {
      checkWrite();
      getBody().writeByte(DataConstants.FLOAT);
      getBody().writeFloat(value);
   }

   public void writeDouble(final double value) throws JMSException
   {
      checkWrite();
      getBody().writeByte(DataConstants.DOUBLE);
      getBody().writeDouble(value);
   }
   
   public void writeString(final String value) throws JMSException
   {
      checkWrite();
      getBody().writeByte(DataConstants.STRING);
      getBody().writeNullableString(value);
   }

   public void writeBytes(final byte[] value) throws JMSException
   {
      checkWrite();
      getBody().writeByte(DataConstants.BYTES);
      getBody().writeInt(value.length);
      getBody().writeBytes(value);
   }

   public void writeBytes(final byte[] value, final int offset, final int length)
         throws JMSException
   {
      checkWrite();
      getBody().writeByte(DataConstants.BYTES);
      getBody().writeInt(length);
      getBody().writeBytes(value, offset, length);
   }

   public void writeObject(final Object value) throws JMSException
   {
      if (value == null) 
      {
         throw new NullPointerException("Attempt to write a null value");
      }
      if (value instanceof String)
      {
         writeString((String)value);
      }
      else if (value instanceof Boolean)
      {
         writeBoolean((Boolean)value);
      }
      else if (value instanceof Byte)
      {
         writeByte((Byte)value);
      }
      else if (value instanceof Short)
      {
         writeShort((Short)value);
      }
      else if (value instanceof Integer)
      {
         writeInt((Integer)value);
      }
      else if (value instanceof Long)
      {
         writeLong((Long)value);
      }
      else if (value instanceof Float)
      {
         writeFloat((Float)value);
      }
      else if (value instanceof Double)
      {
         writeDouble((Double)value);
      }
      else if (value instanceof byte[])
      {
         writeBytes((byte[])value);
      }
      else if (value instanceof Character)
      {
         this.writeChar((Character)value);
      }
      else
      {
         throw new MessageFormatException("Invalid object type: " + value.getClass());
      }
   }

   public void reset() throws JMSException
   {
      if (!readOnly)
      {
         readOnly = true;
      }
      getBody().resetReaderIndex();
   }

   // JBossMessage overrides ----------------------------------------
  
   public void clearBody() throws JMSException
   {
      super.clearBody();
      message.getBody().clear();
   }
   
   public void doBeforeSend() throws Exception
   {
      reset();
   }
   
   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
}
