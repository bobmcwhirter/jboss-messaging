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

package org.jboss.messaging.core.journal.impl;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.messaging.core.asyncio.AIOCallback;
import org.jboss.messaging.core.asyncio.AsynchronousFile;
import org.jboss.messaging.core.asyncio.BufferCallback;
import org.jboss.messaging.core.asyncio.impl.AsynchronousFileImpl;
import org.jboss.messaging.core.asyncio.impl.TimedBuffer;
import org.jboss.messaging.core.asyncio.impl.TimedBufferObserver;
import org.jboss.messaging.core.journal.IOCallback;
import org.jboss.messaging.core.journal.SequentialFile;
import org.jboss.messaging.core.journal.SequentialFileFactory;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.spi.MessagingBuffer;

/**
 * 
 * A AIOSequentialFile
 * 
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 *
 */
public class AIOSequentialFile implements SequentialFile
{
   private static final Logger log = Logger.getLogger(AIOSequentialFile.class);

   private final String journalDir;

   private final String fileName;

   private boolean opened = false;

   private final int maxIO;

   private AsynchronousFile aioFile;

   private final SequentialFileFactory factory;

   private long fileSize = 0;

   private final AtomicLong position = new AtomicLong(0);

   private TimedBuffer timedBuffer;

   private final BufferCallback bufferCallback;

   /** Instead of having AIOSequentialFile implementing the Observer, I have done it on an inner class.
    *  This is the class returned to the factory when the file is being activated. */
   private final TimedBufferObserver timedBufferObserver = new LocalBufferObserver();

   /** A context switch on AIO would make it to synchronize the disk before
       switching to the new thread, what would cause
       serious performance problems. Because of that we make all the writes on
       AIO using a single thread. */
   private final Executor executor;

   /** The pool for Thread pollers */
   private final Executor pollerExecutor;

   public AIOSequentialFile(final SequentialFileFactory factory,
                            final int bufferSize,
                            final long bufferTimeoutMilliseconds,
                            final String journalDir,
                            final String fileName,
                            final int maxIO,
                            final BufferCallback bufferCallback,
                            final Executor executor,
                            final Executor pollerExecutor)
   {
      this.factory = factory;
      this.journalDir = journalDir;
      this.fileName = fileName;
      this.maxIO = maxIO;
      this.bufferCallback = bufferCallback;
      this.executor = executor;
      this.pollerExecutor = pollerExecutor;
   }

   public boolean isOpen()
   {
      return opened;
   }

   public int getAlignment() throws Exception
   {
      checkOpened();

      return aioFile.getBlockSize();
   }

   public int calculateBlockStart(final int position) throws Exception
   {
      int alignment = getAlignment();

      int pos = (position / alignment + (position % alignment != 0 ? 1 : 0)) * alignment;

      return pos;
   }

   public boolean fits(int size)
   {
      return timedBuffer.checkSize(size);
   }

   public void lockBuffer()
   {
      timedBuffer.lock();
   }

   public void unlockBuffer()
   {
      timedBuffer.unlock();
   }

   public synchronized void close() throws Exception
   {
      checkOpened();
      opened = false;

//      if (timedBuffer != null)
//      {
//         timedBuffer.flush();
//         timedBuffer.setObserver(null);
//      } -- remove this
//      
      timedBuffer = null;
      
      final CountDownLatch donelatch = new CountDownLatch(1);

      executor.execute(new Runnable()
      {
         public void run()
         {
            donelatch.countDown();
         }
      });

      while (!donelatch.await(60, TimeUnit.SECONDS))
      {
         log.warn("Executor on file " + fileName + " couldn't complete its tasks in 60 seconds.",
                  new Exception("Warning: Executor on file " + fileName + " couldn't complete its tasks in 60 seconds."));
      }

      aioFile.close();
      aioFile = null;
   }

   public void delete() throws Exception
   {
      if (aioFile != null)
      {
         aioFile.close();
         aioFile = null;
      }

      File file = new File(journalDir + "/" + fileName);
      file.delete();
   }

   public void fill(final int position, final int size, final byte fillCharacter) throws Exception
   {
      checkOpened();

      int fileblockSize = aioFile.getBlockSize();

      int blockSize = fileblockSize;

      if (size % (100 * 1024 * 1024) == 0)
      {
         blockSize = 100 * 1024 * 1024;
      }
      else if (size % (10 * 1024 * 1024) == 0)
      {
         blockSize = 10 * 1024 * 1024;
      }
      else if (size % (1024 * 1024) == 0)
      {
         blockSize = 1024 * 1024;
      }
      else if (size % (10 * 1024) == 0)
      {
         blockSize = 10 * 1024;
      }
      else
      {
         blockSize = fileblockSize;
      }

      int blocks = size / blockSize;

      if (size % blockSize != 0)
      {
         blocks++;
      }

      int filePosition = position;

      if (position % fileblockSize != 0)
      {
         filePosition = (position / fileblockSize + 1) * fileblockSize;
      }

      aioFile.fill(filePosition, blocks, blockSize, fillCharacter);

      this.fileSize = aioFile.size();
   }

   public String getFileName()
   {
      return fileName;
   }

   public void open() throws Exception
   {            
      open(maxIO);            
   }

   /* (non-Javadoc)
    * @see org.jboss.messaging.core.journal.SequentialFile#renameTo(org.jboss.messaging.core.journal.SequentialFile)
    */
   public void renameTo(String fileName) throws Exception
   {
      throw new IllegalStateException("method rename not supported on AIO");

   }

   public synchronized void open(final int currentMaxIO) throws Exception
   {
      opened = true;
      aioFile = newFile();
      aioFile.open(journalDir + "/" + fileName, currentMaxIO);
      position.set(0);
      aioFile.setBufferCallback(bufferCallback);
      this.fileSize = aioFile.size();
   }

   public void setBufferCallback(final BufferCallback callback)
   {
      aioFile.setBufferCallback(callback);
   }

   public void position(final long pos) throws Exception
   {
      position.set(pos);
   }

   public long position() throws Exception
   {
      return position.get();
   }

   public int read(final ByteBuffer bytes, final IOCallback callback) throws Exception
   {
      int bytesToRead = bytes.limit();

      long positionToRead = position.getAndAdd(bytesToRead);

      bytes.rewind();

      aioFile.read(positionToRead, bytesToRead, bytes, callback);

      return bytesToRead;
   }

   public int read(final ByteBuffer bytes) throws Exception
   {
      IOCallback waitCompletion = SimpleWaitIOCallback.getInstance();

      int bytesRead = read(bytes, waitCompletion);

      waitCompletion.waitCompletion();

      return bytesRead;
   }

   public void write(final MessagingBuffer bytes, final boolean sync, final IOCallback callback) throws Exception
   {
      if (timedBuffer != null)
      {
         timedBuffer.addBytes(bytes.array(), sync, callback);
      }
      else
      {
         ByteBuffer buffer = factory.newBuffer(bytes.capacity());
         buffer.put(bytes.array());
         doWrite(buffer, callback);
      }
   }

   public void write(final MessagingBuffer bytes, final boolean sync) throws Exception
   {
      if (sync)
      {
         IOCallback completion = SimpleWaitIOCallback.getInstance();

         write(bytes, true, completion);

         completion.waitCompletion();
      }
      else
      {
         write(bytes, false, DummyCallback.instance);
      }
   }
   
   
   public void write(final ByteBuffer bytes, final boolean sync, final IOCallback callback) throws Exception
   {
      if (timedBuffer != null)
      {
         // sanity check.. it shouldn't happen
         throw new IllegalStateException("Illegal buffered usage. Can't use ByteBuffer write while buffer SequentialFile");
      }
      else
      {
         doWrite(bytes, callback);
      }
   }

   public void write(final ByteBuffer bytes, final boolean sync) throws Exception
   {
      if (sync)
      {
         IOCallback completion = SimpleWaitIOCallback.getInstance();

         write(bytes, true, completion);

         completion.waitCompletion();
      }
      else
      {
         write(bytes, false, DummyCallback.instance);
      }
   }


   public void sync() throws Exception
   {
      throw new IllegalArgumentException("This method is not supported on AIO");
   }

   public long size() throws Exception
   {
      return aioFile.size();
   }

   @Override
   public String toString()
   {
      return "AIOSequentialFile:" + journalDir + "/" + fileName;
   }

   // Public methods
   // -----------------------------------------------------------------------------------------------------

   public void setTimedBuffer(TimedBuffer buffer)
   {
      if (timedBuffer != null)
      {
         timedBuffer.setObserver(null);
      }
      
      this.timedBuffer = buffer;

      if (buffer != null)
      {
         buffer.setObserver(this.timedBufferObserver);
      }

   }

   // Protected methods
   // -----------------------------------------------------------------------------------------------------

   /**
    * An extension point for tests
    */
   protected AsynchronousFile newFile()
   {
      return new AsynchronousFileImpl(executor, pollerExecutor);
   }

   // Private methods
   // -----------------------------------------------------------------------------------------------------

   private void doWrite(final ByteBuffer bytes, final IOCallback callback)
   {
      final int bytesToWrite = factory.calculateBlockSize(bytes.limit());

      final long positionToWrite = position.getAndAdd(bytesToWrite);

      aioFile.write(positionToWrite, bytesToWrite, bytes, callback);
   }

   private void checkOpened() throws Exception
   {
      if (aioFile == null || !opened)
      {
         throw new IllegalStateException("File not opened");
      }
   }

   private static class DelegateCallback implements IOCallback
   {
      final List<AIOCallback> delegates;

      DelegateCallback(List<AIOCallback> delegates)
      {
         this.delegates = delegates;
      }

      public void done()
      {
         for (AIOCallback callback : delegates)
         {
            try
            {
               callback.done();
            }
            catch (Throwable e)
            {
               log.warn(e.getMessage(), e);
            }
         }
      }

      public void onError(int errorCode, String errorMessage)
      {
         for (AIOCallback callback : delegates)
         {
            try
            {
               callback.onError(errorCode, errorMessage);
            }
            catch (Throwable e)
            {
               log.warn(e.getMessage(), e);
            }
         }
      }

      public void waitCompletion() throws Exception
      {
      }
   }

   class LocalBufferObserver implements TimedBufferObserver
   {

      public void flushBuffer(ByteBuffer buffer, List<AIOCallback> callbacks)
      {
         buffer.flip();

         if (buffer.limit() == 0)
         {
            factory.releaseBuffer(buffer);
         }
         else
         {
            doWrite(buffer, new DelegateCallback(callbacks));
         }
      }

      public ByteBuffer newBuffer(int size, int limit)
      {
         size = factory.calculateBlockSize(size);
         limit = factory.calculateBlockSize(limit);

         ByteBuffer buffer = factory.newBuffer(size);
         buffer.limit(limit);
         return buffer;
      }

      public int getRemainingBytes()
      {
         if (fileSize - position.get() > Integer.MAX_VALUE)
         {
            return Integer.MAX_VALUE;
         }
         else
         {
            return (int)(fileSize - position.get());
         }
      }
      
      public String toString()
      {
         return "TimedBufferObserver on file (" + AIOSequentialFile.this.fileName + ")";
      }

   }

}
