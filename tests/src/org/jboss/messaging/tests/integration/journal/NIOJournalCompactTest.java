/*
 * JBoss, Home of Professional Open Source Copyright 2005-2008, Red Hat
 * Middleware LLC, and individual contributors by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of individual
 * contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.jboss.messaging.tests.integration.journal;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.jboss.messaging.core.journal.SequentialFile;
import org.jboss.messaging.core.journal.SequentialFileFactory;
import org.jboss.messaging.core.journal.impl.JournalCompactor;
import org.jboss.messaging.core.journal.impl.JournalFile;
import org.jboss.messaging.core.journal.impl.JournalFileImpl;
import org.jboss.messaging.core.journal.impl.JournalImpl;
import org.jboss.messaging.core.journal.impl.NIOSequentialFileFactory;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.tests.unit.core.journal.impl.JournalImplTestBase;
import org.jboss.messaging.tests.unit.core.journal.impl.fakes.SimpleEncoding;
import org.jboss.messaging.utils.IDGenerator;
import org.jboss.messaging.utils.Pair;
import org.jboss.messaging.utils.TimeAndCounterIDGenerator;

/**
 * 
 * A JournalImplTestBase
 * 
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 *
 */
public class NIOJournalCompactTest extends JournalImplTestBase
{
   private final Logger log = Logger.getLogger(this.getClass());

   private static final int NUMBER_OF_RECORDS = 1000;

   IDGenerator idGenerator = new TimeAndCounterIDGenerator();

   // General tests
   // =============

   public void testControlFile() throws Exception
   {
      ArrayList<JournalFile> dataFiles = new ArrayList<JournalFile>();

      for (int i = 0; i < 5; i++)
      {
         SequentialFile file = fileFactory.createSequentialFile("file-" + i + ".tst", 1);
         dataFiles.add(new JournalFileImpl(file, 0));
      }

      ArrayList<JournalFile> newFiles = new ArrayList<JournalFile>();

      for (int i = 0; i < 3; i++)
      {
         SequentialFile file = fileFactory.createSequentialFile("file-" + i + ".tst.new", 1);
         newFiles.add(new JournalFileImpl(file, 0));
      }

      JournalCompactor.writeControlFile(fileFactory, dataFiles, newFiles);

      ArrayList<String> strDataFiles = new ArrayList<String>();

      ArrayList<String> strNewFiles = new ArrayList<String>();

      assertNotNull(JournalCompactor.readControlFile(fileFactory, strDataFiles, strNewFiles));

      assertEquals(dataFiles.size(), strDataFiles.size());
      assertEquals(newFiles.size(), strNewFiles.size());

      Iterator<String> iterDataFiles = strDataFiles.iterator();
      for (JournalFile file : dataFiles)
      {
         assertEquals(file.getFile().getFileName(), iterDataFiles.next());
      }

      assertFalse(iterDataFiles.hasNext());

      Iterator<String> iterNewFiles = strNewFiles.iterator();
      for (JournalFile file : newFiles)
      {
         assertEquals(file.getFile().getFileName(), iterNewFiles.next());
      }
   }

   public void testCrashRenamingFiles() throws Exception
   {
      internalCompactTest(false, false, true, false, false, false, false, false, false, false, true, false, false);
   }

   public void testCrashDuringCompacting() throws Exception
   {
      internalCompactTest(false, false, true, false, false, false, false, false, false, false, false, false, false);
   }

   public void testCompactwithPendingXACommit() throws Exception
   {
      internalCompactTest(true, false, false, false, false, false, false, true, false, false, true, true, true);
   }

   public void testCompactwithPendingXAPrepareAndCommit() throws Exception
   {
      internalCompactTest(false, true, false, false, false, false, false, true, false, false, true, true, true);
   }

   public void testCompactwithPendingXAPrepareAndDelayedCommit() throws Exception
   {
      internalCompactTest(false, true, false, false, false, false, false, true, false, true, true, true, true);
   }

   public void testCompactwithPendingCommit() throws Exception
   {
      internalCompactTest(true, false, false, false, false, false, false, true, false, false, true, true, true);
   }

   public void testCompactwithDelayedCommit() throws Exception
   {
      internalCompactTest(false, true, false, false, false, false, false, true, false, true, true, true, true);
   }

   public void testCompactwithPendingCommitFollowedByDelete() throws Exception
   {
      internalCompactTest(false, false, false, false, false, false, false, true, true, false, true, true, true);
   }

   public void testCompactwithConcurrentUpdateAndDeletes() throws Exception
   {
      internalCompactTest(false, false, true, false, true, true, false, false, false, false, true, true, true);
      tearDown();
      setUp();
      internalCompactTest(false, false, true, false, true, false, true, false, false, false, true, true, true);
   }

   public void testCompactwithConcurrentDeletes() throws Exception
   {
      internalCompactTest(false, false, true, false, false, true, false, false, false, false, true, true, true);
      tearDown();
      setUp();
      internalCompactTest(false, false, true, false, false, false, true, false, false, false, true, true, true);
   }

   public void testCompactwithConcurrentUpdates() throws Exception
   {
      internalCompactTest(false, false, true, false, true, false, false, false, false, false, true, true, true);
   }

   public void testCompactWithConcurrentAppend() throws Exception
   {
      internalCompactTest(false, false, true, true, false, false, false, false, false, false, true, true, true);
   }

   private void internalCompactTest(final boolean preXA, // prepare before compact
                                    final boolean postXA, // prepare after compact
                                    final boolean regularAdd,
                                    final boolean performAppend,
                                    final boolean performUpdate,
                                    boolean performDelete,
                                    boolean performNonTransactionalDelete,
                                    final boolean pendingTransactions,
                                    final boolean deleteTransactRecords,
                                    final boolean delayCommit,
                                    final boolean createControlFile,
                                    final boolean deleteControlFile,
                                    final boolean renameFilesAfterCompacting) throws Exception
   {
      if (performNonTransactionalDelete)
      {
         performDelete = false;
      }
      if (performDelete)
      {
         performNonTransactionalDelete = false;
      }

      setup(50, 60 * 1024, true);

      ArrayList<Long> liveIDs = new ArrayList<Long>();

      ArrayList<Pair<Long, Long>> transactedRecords = new ArrayList<Pair<Long, Long>>();

      final CountDownLatch latchDone = new CountDownLatch(1);
      final CountDownLatch latchWait = new CountDownLatch(1);
      journal = new JournalImpl(fileSize, minFiles, 0, 0, fileFactory, filePrefix, fileExtension, maxAIO)
      {

         @Override
         protected SequentialFile createControlFile(List<JournalFile> files, List<JournalFile> newFiles) throws Exception
         {
            if (createControlFile)
            {
               return super.createControlFile(files, newFiles);
            }
            else
            {
               throw new IllegalStateException("Simulating a crash during compact creation");
            }
         }

         @Override
         protected void deleteControlFile(SequentialFile controlFile) throws Exception
         {
            if (deleteControlFile)
            {
               super.deleteControlFile(controlFile);
            }
         }

         @Override
         protected void renameFiles(List<JournalFile> oldFiles, List<JournalFile> newFiles) throws Exception
         {
            if (renameFilesAfterCompacting)
            {
               super.renameFiles(oldFiles, newFiles);
            }
         }

         @Override
         public void onCompactDone()
         {
            latchDone.countDown();
            System.out.println("Waiting on Compact");
            try
            {
               latchWait.await();
            }
            catch (InterruptedException e)
            {
               e.printStackTrace();
            }
            System.out.println("Done");
         }
      };
      
      journal.setAutoReclaim(false);
      
      startJournal();
      load();

      long transactionID = 0;

      if (regularAdd)
      {

         for (int i = 0; i < NUMBER_OF_RECORDS / 2; i++)
         {
            add(i);
            if (i % 10 == 0 && i > 0)
            {
               journal.forceMoveNextFile();
            }
            update(i);
         }

         for (int i = NUMBER_OF_RECORDS / 2; i < NUMBER_OF_RECORDS; i++)
         {

            addTx(transactionID, i);
            updateTx(transactionID, i);
            if (i % 10 == 0)
            {
               journal.forceMoveNextFile();
            }
            commit(transactionID++);
            update(i);
         }
      }

      if (pendingTransactions)
      {
         for (long i = 0; i < 100; i++)
         {
            long recordID = idGenerator.generateID();
            addTx(transactionID, recordID);
            updateTx(transactionID, recordID);
            if (preXA)
            {
               prepare(transactionID, new SimpleEncoding(10, (byte)0));
            }
            transactedRecords.add(new Pair<Long, Long>(transactionID++, recordID));
         }
      }

      System.out.println("Number of Files: " + journal.getDataFilesCount());

      if (regularAdd)
      {
         for (int i = 0; i < NUMBER_OF_RECORDS; i++)
         {
            if (!(i % 10 == 0))
            {
               delete(i);
            }
            else
            {
               liveIDs.add((long)i);
            }
         }
      }

      journal.forceMoveNextFile();

      Thread t = new Thread()
      {
         @Override
         public void run()
         {
            try
            {
               journal.compact();
            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
         }
      };

      t.start();

      latchDone.await();

      int nextID = NUMBER_OF_RECORDS;

      if (performAppend)
      {
         for (int i = 0; i < 50; i++)
         {
            add(nextID++);
            if (i % 10 == 0)
            {
               journal.forceMoveNextFile();
            }
         }

         for (int i = 0; i < 50; i++)
         {
            // A Total new transaction (that was created after the compact started) to add new record while compacting
            // is still working
            addTx(transactionID, nextID++);
            commit(transactionID++);
            if (i % 10 == 0)
            {
               journal.forceMoveNextFile();
            }
         }
      }

      if (performUpdate)
      {
         int count = 0;
         for (Long liveID : liveIDs)
         {
            if (count++ % 2 == 0)
            {
               update(liveID);
            }
            else
            {
               // A Total new transaction (that was created after the compact started) to update a record that is being
               // compacted
               updateTx(transactionID, liveID);
               commit(transactionID++);
            }
         }
      }

      if (performDelete)
      {
         int count = 0;
         for (long liveID : liveIDs)
         {
            if (count++ % 2 == 0)
            {
               System.out.println("Deleting no trans " + liveID);
               delete(liveID);
            }
            else
            {
               System.out.println("Deleting TX " + liveID);
               // A Total new transaction (that was created after the compact started) to delete a record that is being
               // compacted
               deleteTx(transactionID, liveID);
               commit(transactionID++);
            }

            System.out.println("Deletes are going into " + ((JournalImpl)journal).getCurrentFile());
         }
      }

      if (performNonTransactionalDelete)
      {
         for (long liveID : liveIDs)
         {
            delete(liveID);
         }
      }

      if (pendingTransactions && !delayCommit)
      {
         for (Pair<Long, Long> tx : transactedRecords)
         {
            if (postXA)
            {
               prepare(tx.a, new SimpleEncoding(10, (byte)0));
            }
            if (tx.a % 2 == 0)
            {
               commit(tx.a);

               if (deleteTransactRecords)
               {
                  delete(tx.b);
               }
            }
            else
            {
               rollback(tx.a);
            }
         }
      }

      /** Some independent adds and updates */
      for (int i = 0; i < 1000; i++)
      {
         long id = idGenerator.generateID();
         add(id);
         delete(id);

         if (i % 100 == 0)
         {
            journal.forceMoveNextFile();
         }
      }
      journal.forceMoveNextFile();

      latchWait.countDown();

      t.join();

      if (pendingTransactions && delayCommit)
      {
         for (Pair<Long, Long> tx : transactedRecords)
         {
            if (postXA)
            {
               prepare(tx.a, new SimpleEncoding(10, (byte)0));
            }
            if (tx.a % 2 == 0)
            {
               commit(tx.a);

               if (deleteTransactRecords)
               {
                  delete(tx.b);
               }
            }
            else
            {
               rollback(tx.a);
            }
         }
      }

      add(idGenerator.generateID());

      if (createControlFile && deleteControlFile && renameFilesAfterCompacting)
      {
         journal.compact();
      }

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

   }

   public void testSimpleCompacting() throws Exception
   {
      setup(2, 60 * 1024, true);

      createJournal();
      startJournal();
      load();

      int NUMBER_OF_RECORDS = 1000;

      // add and remove some data to force reclaiming
      {
         ArrayList<Long> ids = new ArrayList<Long>();
         for (int i = 0; i < NUMBER_OF_RECORDS; i++)
         {
            long id = idGenerator.generateID();
            ids.add(id);
            add(id);
            if (i > 0 && i % 100 == 0)
            {
               journal.forceMoveNextFile();
            }
         }

         for (Long id : ids)
         {
            delete(id);
         }

         journal.forceMoveNextFile();

         journal.checkAndReclaimFiles();
      }

      long transactionID = 0;

      for (int i = 0; i < NUMBER_OF_RECORDS / 2; i++)
      {
         add(i);
         if (i % 10 == 0 && i > 0)
         {
            journal.forceMoveNextFile();
         }
         update(i);
      }

      for (int i = NUMBER_OF_RECORDS / 2; i < NUMBER_OF_RECORDS; i++)
      {

         addTx(transactionID, i);
         updateTx(transactionID, i);
         if (i % 10 == 0)
         {
            journal.forceMoveNextFile();
         }
         commit(transactionID++);
         update(i);
      }

      System.out.println("Number of Files: " + journal.getDataFilesCount());

      for (int i = 0; i < NUMBER_OF_RECORDS; i++)
      {
         if (!(i % 10 == 0))
         {
            delete(i);
         }
      }

      journal.forceMoveNextFile();

      System.out.println("Number of Files: " + journal.getDataFilesCount());

      System.out.println("Before compact ****************************");
      System.out.println(journal.debug());
      System.out.println("*****************************************");

      journal.compact();

      add(idGenerator.generateID());

      journal.compact();

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

   }

   public void testLiveSize() throws Exception
   {
      setup(2, 60 * 1024, true);

      createJournal();
      startJournal();
      loadAndCheck();

      ArrayList<Long> listToDelete = new ArrayList<Long>();

      ArrayList<Integer> expectedSizes = new ArrayList<Integer>();

      for (int i = 0; i < 10; i++)
      {
         long id = idGenerator.generateID();
         listToDelete.add(id);

         expectedSizes.add(recordLength + JournalImpl.SIZE_ADD_RECORD);

         add(id);
         journal.forceMoveNextFile();
         update(id);

         expectedSizes.add(recordLength + JournalImpl.SIZE_UPDATE_RECORD);
         journal.forceMoveNextFile();
      }

      System.out.println("DataFiles = " + journal.getDataFilesCount());

      JournalFile files[] = journal.getDataFiles();

      for (JournalFile file : files)
      {
         System.out.println("Size: " + file.getLiveSize());
      }

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      journal.forceMoveNextFile();

      System.out.println("DataFiles = " + journal.getDataFilesCount());

      JournalFile files2[] = journal.getDataFiles();

      assertEquals(files.length, files2.length);

      for (JournalFile file : files2)
      {
         System.out.println("Size: " + file.getLiveSize());
      }

      for (int i = 0; i < files.length; i++)
      {
         assertEquals(expectedSizes.get(i).intValue(), files[i].getLiveSize());
         assertEquals(expectedSizes.get(i).intValue(), files2[i].getLiveSize());
      }

      for (long id : listToDelete)
      {
         delete(id);
      }

      journal.forceMoveNextFile();

      JournalFile files3[] = journal.getDataFiles();

      for (JournalFile file : files3)
      {
         System.out.println("Size: " + file.getLiveSize());
      }

      for (JournalFile file : files3)
      {
         assertEquals(0, file.getLiveSize());
      }

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      files3 = journal.getDataFiles();

      for (JournalFile file : files3)
      {
         assertEquals(0, file.getLiveSize());
      }

   }

   public void testLiveSizeTransactional() throws Exception
   {
      setup(2, 60 * 1024, true);

      createJournal();
      startJournal();
      loadAndCheck();

      ArrayList<Long> listToDelete = new ArrayList<Long>();

      ArrayList<Integer> expectedSizes = new ArrayList<Integer>();

      for (int i = 0; i < 10; i++)
      {
         long tx = idGenerator.generateID();
         long id = idGenerator.generateID();
         listToDelete.add(id);

         addTx(tx, id);

         // Append Record Transaction will make the recordSize as exactly recordLength (discounting SIZE_ADD_RECORD_TX)
         expectedSizes.add(recordLength);
         journal.forceMoveNextFile();

         updateTx(tx, id);
         // uPDATE Record Transaction will make the recordSize as exactly recordLength (discounting SIZE_ADD_RECORD_TX)
         expectedSizes.add(recordLength);

         journal.forceMoveNextFile();
         expectedSizes.add(0);

         commit(tx);

         journal.forceMoveNextFile();
      }

      System.out.println("DataFiles = " + journal.getDataFilesCount());

      JournalFile files[] = journal.getDataFiles();

      for (JournalFile file : files)
      {
         System.out.println("Size: " + file.getLiveSize());
      }

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      journal.forceMoveNextFile();

      System.out.println("DataFiles = " + journal.getDataFilesCount());

      JournalFile files2[] = journal.getDataFiles();

      assertEquals(files.length, files2.length);

      for (JournalFile file : files2)
      {
         System.out.println("Size: " + file.getLiveSize());
      }

      for (int i = 0; i < files.length; i++)
      {
         assertEquals(expectedSizes.get(i).intValue(), files[i].getLiveSize());
         assertEquals(expectedSizes.get(i).intValue(), files2[i].getLiveSize());
      }

      long tx = idGenerator.generateID();
      for (long id : listToDelete)
      {
         deleteTx(tx, id);
      }
      commit(tx);

      journal.forceMoveNextFile();

      JournalFile files3[] = journal.getDataFiles();

      for (JournalFile file : files3)
      {
         System.out.println("Size: " + file.getLiveSize());
      }

      for (JournalFile file : files3)
      {
         assertEquals(0, file.getLiveSize());
      }

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      files3 = journal.getDataFiles();

      for (JournalFile file : files3)
      {
         assertEquals(0, file.getLiveSize());
      }

   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      File file = new File(getTestDir());

      deleteDirectory(file);

      file.mkdir();
   }

   /* (non-Javadoc)
    * @see org.jboss.messaging.tests.unit.core.journal.impl.JournalImplTestBase#getFileFactory()
    */
   @Override
   protected SequentialFileFactory getFileFactory() throws Exception
   {
      return new NIOSequentialFileFactory(getTestDir());
   }

}
