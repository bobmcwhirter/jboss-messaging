/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.messaging.core.asyncio.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.messaging.core.asyncio.AIOCallback;
import org.jboss.messaging.core.buffers.ChannelBuffers;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.spi.MessagingBuffer;
import org.jboss.messaging.utils.VariableLatch;

/**
 * A TimedBuffer
 *
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class TimedBuffer
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(TimedBuffer.class);

   // Attributes ----------------------------------------------------

   private TimedBufferObserver bufferObserver;
   
   // Some kernels don't have good resolutions on timers.. I've set this to disabled.. we may decide later
   private static final boolean USE_NATIVE_TIMERS = true;

   // This is used to pause and resume the timer
   // This is a reusable Latch, that uses java.util.concurrent base classes
   private final VariableLatch latchTimer = new VariableLatch();

   private CheckTimer timerRunnable = new CheckTimer();

   private final int bufferSize;

   private final MessagingBuffer buffer;

   private int bufferLimit = 0;

   private List<AIOCallback> callbacks;

   private final Lock lock = new ReentrantReadWriteLock().writeLock();

   // used to measure inactivity. This buffer will be automatically flushed when more than timeout inactive
   private volatile boolean active = false;

   private final long timeout;

   // used to measure sync requests. When a sync is requested, it shouldn't take more than timeout to happen
   private volatile boolean pendingSync = false;

   private Thread timerThread;

   private volatile boolean started;

   private final boolean flushOnSync;

   // for logging write rates

   private final boolean logRates;

   private volatile long bytesFlushed;

   private Timer logRatesTimer;

   private TimerTask logRatesTimerTask;

   private long lastExecution;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public TimedBuffer(final int size, final long timeout, final boolean flushOnSync, final boolean logRates)
   {
      bufferSize = size;
      this.logRates = logRates;
      if (logRates)
      {
         this.logRatesTimer = new Timer(true);
      }
      // Setting the interval for nano-sleeps

      // We are keeping this disabled for now until we figure out what to do.
      // I've found a few problems with nano-sleep depending on the version of the kernel:
      // http://fixunix.com/unix/552033-problem-nanosleep.html
      if (USE_NATIVE_TIMERS)
      {
         AsynchronousFileImpl.setNanoSleepInterval((int)timeout);
      }

      buffer = ChannelBuffers.buffer(bufferSize);
      buffer.clear();
      bufferLimit = 0;

      callbacks = new ArrayList<AIOCallback>();
      this.flushOnSync = flushOnSync;
      latchTimer.up();
      this.timeout = timeout;
   }

   public synchronized void start()
   {
      if (started)
      {
         return;
      }

      timerRunnable = new CheckTimer();

      timerThread = new Thread(timerRunnable, "jbm-aio-timer");

      timerThread.start();

      if (logRates)
      {
         logRatesTimerTask = new LogRatesTimerTask();

         logRatesTimer.scheduleAtFixedRate(logRatesTimerTask, 2000, 2000);
      }

      started = true;
   }

   public void stop()
   {
      if (!started)
      {
         return;
      }

      this.flush();

      this.bufferObserver = null;

      latchTimer.down();

      timerRunnable.close();

      if (logRates)
      {
         logRatesTimerTask.cancel();
      }

      while (timerThread.isAlive())
      {
         try
         {
            timerThread.join();
         }
         catch (InterruptedException e)
         {
         }
      }

      started = false;
   }

   public synchronized void setObserver(TimedBufferObserver observer)
   {
      if (this.bufferObserver != null)
      {
         flush();
      }

      this.bufferObserver = observer;
   }

   public void lock()
   {
      lock.lock();
   }

   public void unlock()
   {
      lock.unlock();
   }

   /**
    * Verify if the size fits the buffer
    * @param sizeChecked
    * @return
    */
   public synchronized boolean checkSize(final int sizeChecked)
   {
      if (sizeChecked > bufferSize)
      {
         throw new IllegalStateException("Can't write records bigger than the bufferSize(" + bufferSize +
                                         ") on the journal");
      }

      if (bufferLimit == 0 || buffer.writerIndex() + sizeChecked > bufferLimit)
      {
         flush();

         final int remaining = bufferObserver.getRemainingBytes();

         if (sizeChecked > remaining)
         {
            return false;
         }
         else
         {
            buffer.clear();
            bufferLimit = Math.min(remaining, bufferSize);
            return true;
         }
      }
      else
      {
         return true;
      }
   }

   public synchronized void addBytes(final byte[] bytes, final boolean sync, final AIOCallback callback)
   {
      if (buffer.writerIndex() == 0)
      {
         // Resume latch
         latchTimer.down();
      }

      buffer.writeBytes(bytes);

      callbacks.add(callback);

      active = true;

      if (sync)
      {
         if (flushOnSync)
         {
            flush();
         }
         else
         {
            // We should flush on the next timeout, no matter what other activity happens on the buffer
            if (!pendingSync)
            {
               pendingSync = true;
            }
         }
      }

      if (buffer.writerIndex() == bufferLimit)
      {
         flush();
      }
   }

   public synchronized void flush()
   {
      if (buffer.writerIndex() > 0)
      {
         latchTimer.up();

         int pos = buffer.writerIndex();

         if (logRates)
         {
            bytesFlushed += pos;
         }

         ByteBuffer directBuffer = bufferObserver.newBuffer(bufferSize, pos);

         // Putting a byteArray on a native buffer is much faster, since it will do in a single native call.
         // Using directBuffer.put(buffer) would make several append calls for each byte

         directBuffer.put(buffer.array(), 0, pos);

         bufferObserver.flushBuffer(directBuffer, callbacks);

         callbacks = new ArrayList<AIOCallback>();

         active = false;
         pendingSync = false;

         buffer.clear();
         bufferLimit = 0;
      }
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   private void checkTimer()
   {
      // if inactive for more than the timeout
      // of if a sync happened at more than the the timeout ago
      if (!active || pendingSync)
      {
         lock.lock();
         try
         {
            if (bufferObserver != null)
            {
               flush();
            }
         }
         finally
         {
            lock.unlock();
         }
      }

      // Set the buffer as inactive.. we will flush the buffer next tick if nothing change this
      active = false;
   }

   // Inner classes -------------------------------------------------

   private class LogRatesTimerTask extends TimerTask
   {
      private boolean closed;

      @Override
      public synchronized void run()
      {
         if (!closed)
         {
            long now = System.currentTimeMillis();

            if (lastExecution != 0)
            {
               double rate = 1000 * ((double)bytesFlushed) / (now - lastExecution);
               log.info("Write rate = " + rate + " bytes / sec or " + (long)(rate / (1024 * 1024)) + " MiB / sec");
            }

            lastExecution = now;

            bytesFlushed = 0;
         }
      }

      public synchronized boolean cancel()
      {
         closed = true;

         return super.cancel();
      }
   }

   private class CheckTimer implements Runnable
   {
      private volatile boolean closed = false;

      public void run()
      {
         if (USE_NATIVE_TIMERS)
         {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
         }

         while (!closed)
         {
            try
            {
               latchTimer.waitCompletion();
            }
            catch (InterruptedException ignored)
            {
            }

            sleep();

            checkTimer();

         }
      }

      /**
       * 
       */
      private void sleep()
      {
         if (USE_NATIVE_TIMERS)
         {
            // The time is passed on the constructor.
            // I'm avoiding the long on the calling stack, to avoid performance hits here
            AsynchronousFileImpl.nanoSleep();
         }
         else
         {
            long time = System.nanoTime() + timeout;
            while (time > System.nanoTime())
            {
               Thread.yield();
            }
         }
      }

      public void close()
      {
         closed = true;
      }
   }

}
