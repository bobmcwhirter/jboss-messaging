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

package org.jboss.messaging.tests.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import javax.transaction.xa.Xid;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.jboss.messaging.core.exception.MessagingException;
import org.jboss.messaging.core.journal.EncodingSupport;
import org.jboss.messaging.core.remoting.impl.ByteBufferWrapper;
import org.jboss.messaging.core.remoting.spi.MessagingBuffer;
import org.jboss.messaging.core.server.MessageReference;
import org.jboss.messaging.core.server.Queue;
import org.jboss.messaging.core.server.ServerMessage;
import org.jboss.messaging.core.server.impl.ServerMessageImpl;

/**
 * 
 * Helper base class for our unit tests
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class UnitTestCase extends TestCase
{
   // Constants -----------------------------------------------------
   
   // Attributes ----------------------------------------------------
   
   // Static --------------------------------------------------------
   
   public static String dumpBytes(byte[] bytes)
   {
      StringBuffer buff = new StringBuffer();
      
      buff.append(System.identityHashCode(bytes) + ", size: " + bytes.length + " [");
      
      for (int i = 0; i < bytes.length; i++)
      {
         buff.append(bytes[i]);
         
         if (i != bytes.length - 1)
         {
            buff.append(", ");
         }
      }
      
      buff.append("]");
      
      return buff.toString();      
   }
   
   public static void assertEqualsByteArrays(byte[] expected, byte[] actual)
   {
      assertEquals(expected.length, actual.length);
      for (int i = 0; i < expected.length; i++)
      {
         assertEquals("byte at index " + i, expected[i], actual[i]);
      }
   }

   public static void assertEqualsByteArrays(int length, byte[] expected, byte[] actual)
   {
      // we check only for the given length (the arrays might be
      // larger)
      assertTrue(expected.length >= length);
      assertTrue(actual.length >= length);
      for (int i = 0; i < length; i++)
      {
         assertEquals("byte at index " + i, expected[i], actual[i]);
      }
   }

   public static void assertSameXids(List<Xid> expected, List<Xid> actual)
   {
      assertNotNull(expected);
      assertNotNull(actual);
      assertEquals(expected.size(), actual.size());
   
      for (int i = 0; i < expected.size(); i++)
      {
         Xid expectedXid = expected.get(i);
         Xid actualXid = actual.get(i);
         UnitTestCase.assertEqualsByteArrays(expectedXid.getBranchQualifier(), actualXid
               .getBranchQualifier());
         assertEquals(expectedXid.getFormatId(), actualXid.getFormatId());
         UnitTestCase.assertEqualsByteArrays(expectedXid.getGlobalTransactionId(), actualXid
               .getGlobalTransactionId());
      }
   }

   
   public static MessagingException messagingExceptionMatch(final int errorID)
   {
      EasyMock.reportMatcher(new IArgumentMatcher()
      {

         public void appendTo(StringBuffer buffer)
         {
            buffer.append(errorID);
         }

         public boolean matches(Object argument)
         {
            MessagingException ex = (MessagingException) argument;
            
            return ex.getCode() == errorID;
         }
         
      });
      
      return null;
   }
   
   // Constructors --------------------------------------------------
   
   // Public --------------------------------------------------------
   
   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   protected byte[] autoEncode(Object... args)
   {
      
      int size = 0;
      
      for (Object arg : args)
      {
         if (arg instanceof Byte)
         {
            size++;
         }
         else if (arg instanceof Boolean)
         {
            size++;
         }
         else if (arg instanceof Integer)
         {
            size += 4;
         }
         else if (arg instanceof Long)
         {
            size += 8;
         }
         else if (arg instanceof Float)
         {
            size += 4;
         }
         else if (arg instanceof Double)
         {
            size += 8;
         }
         else
         {
            throw new IllegalArgumentException(
                  "method autoEncode doesn't know how to convert "
                        + arg.getClass() + " yet");
         }
      }
      
      ByteBuffer buffer = ByteBuffer.allocate(size);
      
      for (Object arg : args)
      {
         if (arg instanceof Byte)
         {
            buffer.put(((Byte) arg).byteValue());
         }
         else if (arg instanceof Boolean)
         {
            Boolean b = (Boolean) arg;
            buffer.put((byte) (b.booleanValue() ? 1 : 0));
         }
         else if (arg instanceof Integer)
         {
            buffer.putInt(((Integer) arg).intValue());
         }
         else if (arg instanceof Long)
         {
            buffer.putLong(((Long) arg).longValue());
         }
         else if (arg instanceof Float)
         {
            buffer.putFloat(((Float) arg).floatValue());
         }
         else if (arg instanceof Double)
         {
            buffer.putDouble(((Double) arg).doubleValue());
         }
         else
         {
            throw new IllegalArgumentException(
                  "method autoEncode doesn't know how to convert "
                        + arg.getClass() + " yet");
         }
      }
      
      return buffer.array();
   }
   
   
   protected ByteBuffer compareByteBuffer(final byte expectedArray[])
   {
      
      EasyMock.reportMatcher(new IArgumentMatcher()
      {

         public void appendTo(StringBuffer buffer)
         {
            buffer.append("ByteArray");
         }

         public boolean matches(Object argument)
         {
            ByteBuffer buffer = (ByteBuffer) argument;
            
            buffer.rewind();
            byte[] compareArray = new byte[buffer.limit()];
            buffer.get(compareArray);
            
            if (compareArray.length != expectedArray.length)
            {
               return false;
            }
            
            for (int i = 0; i < expectedArray.length; i++)
            {
               if (expectedArray[i] != compareArray[i])
               {
                  return false;
               }
            }
            
            return true;
         }
         
      });
      
      return null;
   }

   protected EncodingSupport compareEncodingSupport(final byte expectedArray[])
   {
      
      EasyMock.reportMatcher(new IArgumentMatcher()
      {

         public void appendTo(StringBuffer buffer)
         {
            buffer.append("EncodingSupport buffer didn't match");
         }

         public boolean matches(Object argument)
         {
            EncodingSupport encoding = (EncodingSupport) argument;

            final int size = encoding.getEncodeSize();
            
            if (size != expectedArray.length)
            {
               System.out.println(size + " != " + expectedArray.length);
               return false;
            }
            
            byte[] compareArray = new byte[size];
            
            MessagingBuffer buffer = new ByteBufferWrapper(ByteBuffer.wrap(compareArray));
            encoding.encode(buffer);
            
            for (int i = 0; i < expectedArray.length; i++)
            {
               if (expectedArray[i] != compareArray[i])
               {
                  return false;
               }
            }
            
            return true;
         }
         
      });
      
      return null;
   }

   

   protected boolean deleteDirectory(File directory)
   {
      if (directory.isDirectory())
      {
         String[] files = directory.list();

         for (int j = 0; j < files.length; j++)
         {
            if (!deleteDirectory(new File(directory, files[j])))
            {
               return false;
            }
         }
      }

      return directory.delete();
   }
   
   protected void copyRecursive(File from , File to) throws Exception
   {     
       if (from.isDirectory())
       {
           if (!to.exists())
           {
               to.mkdir();
           }
           
           String[] subs = from.list();
           
           for (int i = 0; i < subs.length; i++)
           {
               copyRecursive(new File(from, subs[i]),
                             new File(to, subs[i]));
           }
       }
       else
       {           
           InputStream in = null;
           
           OutputStream out = null;
                      
           try
           {           
              in = new BufferedInputStream(new FileInputStream(from));              
              
              out = new BufferedOutputStream(new FileOutputStream(to));
              
              int b;
              
              while ((b = in.read()) != -1)
              {
                  out.write(b);
              }
           }
           finally
           {   
              if (in != null)
              {
                 in.close();
              }
              
              if (out != null)
              {
                 out.close();
              }
           }
       }
   }
   
   protected void assertRefListsIdenticalRefs(List<MessageReference> l1, List<MessageReference> l2)
   {
      if (l1.size() != l2.size())
      {
         fail("Lists different sizes: " + l1.size() + ", " + l2.size());
      }
      
      Iterator<MessageReference> iter1 = l1.iterator();
      Iterator<MessageReference> iter2 = l2.iterator();
      
      while (iter1.hasNext())
      {
         MessageReference o1 = iter1.next();
         MessageReference o2 = iter2.next();
                  
         assertTrue(o1 == o2);
      }                   
   }
           
   protected ServerMessage generateMessage(long id)
   {
      ServerMessage message = new ServerMessageImpl((byte)0, true, 0, System.currentTimeMillis(), (byte)4, new ByteBufferWrapper(ByteBuffer.allocateDirect(1024)));
      
      message.setMessageID(id);
      
      byte[] bytes = new byte[1024];
      
      for (int i = 0; i < 1024; i++)
      {
         bytes[i] = (byte)i;
      }
      
      //message.setPayload(bytes);
      
      message.getBody().putString(UUID.randomUUID().toString());
      
      return message;
   }
   
   protected MessageReference generateReference(Queue queue, long id)
   {
      ServerMessage message = generateMessage(id);
      
      return message.createReference(queue);
   }
   
	protected int calculateRecordSize(int size, int alignment)
   {
      return ((size / alignment) + (size % alignment != 0 ? 1 : 0)) * alignment;
   }

   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
   
   public static class DirectExecutorService extends AbstractExecutorService
   {
      public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException
      {
         return false;
      }

      public boolean isShutdown()
      {
         return false;
      }

      public void shutdown()
      { 
      }

      public boolean isTerminated()
      {
         return false;
      }

      public List<Runnable> shutdownNow()
      {
         return null;
      }

      public void execute(Runnable command)
      {
         command.run();
      }
   }

   
}
