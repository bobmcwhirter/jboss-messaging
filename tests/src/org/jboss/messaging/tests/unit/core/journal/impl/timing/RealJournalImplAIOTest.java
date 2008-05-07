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
package org.jboss.messaging.tests.unit.core.journal.impl.timing;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.messaging.core.asyncio.AIOCallback;
import org.jboss.messaging.core.journal.IOCallback;
import org.jboss.messaging.core.journal.Journal;
import org.jboss.messaging.core.journal.RecordInfo;
import org.jboss.messaging.core.journal.SequentialFileFactory;
import org.jboss.messaging.core.journal.impl.AIOSequentialFileFactory;
import org.jboss.messaging.core.journal.impl.JournalImpl;
import org.jboss.messaging.core.journal.impl.NIOSequentialFileFactory;
import org.jboss.messaging.core.logging.Logger;

/**
 * 
 * A RealJournalImplTest
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class RealJournalImplAIOTest extends JournalImplTestUnit
{
   private static final Logger log = Logger.getLogger(RealJournalImplAIOTest.class);
   
   protected String journalDir = System.getProperty("user.home") + "/journal-test";
      
   protected SequentialFileFactory getFileFactory() throws Exception
   {
      File file = new File(journalDir);
      
      log.info("deleting directory " + journalDir);
      
      deleteDirectory(file);
      
      file.mkdir();     
      
      return new NIOSequentialFileFactory(journalDir);
   }
   
   public void testSpeedNonTransactional() throws Exception
   {
      for (int i=0;i<1;i++)
      {
         this.setUp();
         System.gc(); Thread.sleep(500);
         internaltestSpeedNonTransactional();
         this.tearDown();
      }
   }
   
   public void internaltestSpeedNonTransactional() throws Exception
   {
      
      final long numMessages = 100000;
      
      int numFiles =  (int)(((numMessages * 1024 + 512) / (10 * 1024 * 1024)) * 1.3);
      
      if (numFiles<2) numFiles = 2;
      
      log.info("num Files=" + numFiles);

      Journal journal =
         new JournalImpl(10 * 1024 * 1024,  numFiles, true, new AIOSequentialFileFactory(journalDir),
               5000, "jbm-data", "jbm");
      
      journal.start();
      
      journal.load(new ArrayList<RecordInfo>(), null);
      

      final CountDownLatch latch = new CountDownLatch((int)numMessages);
      
      
      class LocalCallback implements IOCallback
      {

         int i=0;
         String message = null;
         boolean done = false;
         CountDownLatch latch;
         
         public LocalCallback(int i, CountDownLatch latch)
         {
            this.i = i;
            this.latch = latch;
         }
         public void done()
         {
            synchronized (this)
            {
               if (done)
               {
                  message = "done received in duplicate";
               }
               done = true;
               this.latch.countDown();
            }
         }

         public void onError(int errorCode, String errorMessage)
         {
            synchronized (this)
            {
               System.out.println("********************** Error = " + (i++));
               message = errorMessage;
               latch.countDown();
            }
         }
         
      }
      
      
      log.info("Adding data");
      byte[] data = new byte[700];
      
      long start = System.currentTimeMillis();
      
      LocalCallback callback = new LocalCallback(1, latch);
      for (int i = 0; i < numMessages; i++)
      {
         journal.appendAddRecord(i, data, callback);
      }
      
      latch.await(10, TimeUnit.SECONDS);
      
      // Validates if the test has completed 
      assertEquals(0, latch.getCount());
      
      long end = System.currentTimeMillis();
      
      double rate = 1000 * (double)numMessages / (end - start);
      
      boolean failed = false;
      
      // If this fails it is probably because JournalImpl it is closing the files without waiting all the completes to arrive first
      assertFalse(failed);
      
      
      log.info("Rate " + rate + " records/sec");

      journal.stop();
      
      journal =
         new JournalImpl(10 * 1024 * 1024,  numFiles, true, new AIOSequentialFileFactory(journalDir),
               5000, "jbm-data", "jbm");
      
      journal.start();
      journal.load(new ArrayList<RecordInfo>(), null);
      journal.stop();
      
   }
   
   public void testSpeedTransactional() throws Exception
   {
      Journal journal =
         new JournalImpl(10 * 1024 * 1024, 10, true, new AIOSequentialFileFactory(journalDir),
               5000, "jbm-data", "jbm");
      
      journal.start();
      
      journal.load(new ArrayList<RecordInfo>(), null);
      
      final int numMessages = 10000;
      
      byte[] data = new byte[1024];
      
      long start = System.currentTimeMillis();
      
      int count = 0;
      for (int i = 0; i < numMessages; i++)
      {
         journal.appendAddRecordTransactional(i, count++, data);
         
         journal.appendCommitRecord(i);
      }
      
      long end = System.currentTimeMillis();
      
      double rate = 1000 * (double)numMessages / (end - start);
      
      log.info("Rate " + rate + " records/sec");

   }
}
