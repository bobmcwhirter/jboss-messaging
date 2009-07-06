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

package org.jboss.messaging.tests.unit.core.journal.impl;

import java.util.List;

import org.jboss.messaging.core.journal.EncodingSupport;
import org.jboss.messaging.core.journal.RecordInfo;
import org.jboss.messaging.core.journal.impl.JournalImpl;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.tests.unit.core.journal.impl.fakes.SimpleEncoding;

/**
 * 
 * A JournalImplTestBase
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 *
 */
public abstract class JournalImplTestUnit extends JournalImplTestBase
{
   private static final Logger log = Logger.getLogger(JournalImplTestUnit.class);

   // General tests
   // =============

   public void testState() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      try
      {
         load();
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         stopJournal();
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      startJournal();
      try
      {
         startJournal();
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      stopJournal();
      startJournal();
      load();
      try
      {
         load();
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         startJournal();
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      stopJournal();
   }

   public void testRestartJournal() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      stopJournal();
      startJournal();
      load();
      byte[] record = new byte[1000];
      for (int i = 0; i < record.length; i++)
      {
         record[i] = (byte)'a';
      }
      // Appending records after restart should be valid (not throwing any
      // exceptions)
      for (int i = 0; i < 100; i++)
      {
         journal.appendAddRecord(1, (byte)1, new SimpleEncoding(2, (byte)'a'), false);
      }
      stopJournal();
   }

   public void testParams() throws Exception
   {
      try
      {
         new JournalImpl(JournalImpl.MIN_FILE_SIZE - 1, 10, 0, 0, fileFactory, filePrefix, fileExtension, 1);

         fail("Should throw exception");
      }
      catch (IllegalArgumentException e)
      {
         // Ok
      }

      try
      {
         new JournalImpl(10 * 1024, 1, 0, 0, fileFactory, filePrefix, fileExtension, 1);

         fail("Should throw exception");
      }
      catch (IllegalArgumentException e)
      {
         // Ok
      }

      try
      {
         new JournalImpl(10 * 1024, 10, 0, 0, null, filePrefix, fileExtension, 1);

         fail("Should throw exception");
      }
      catch (NullPointerException e)
      {
         // Ok
      }

      try
      {
         new JournalImpl(10 * 1024, 10, 0, 0, fileFactory, null, fileExtension, 1);

         fail("Should throw exception");
      }
      catch (NullPointerException e)
      {
         // Ok
      }

      try
      {
         new JournalImpl(10 * 1024, 10, 0, 0, fileFactory, filePrefix, null, 1);

         fail("Should throw exception");
      }
      catch (NullPointerException e)
      {
         // Ok
      }

      try
      {
         new JournalImpl(10 * 1024, 10, 0, 0, fileFactory, filePrefix, null, 0);

         fail("Should throw exception");
      }
      catch (NullPointerException e)
      {
         // Ok
      }

   }

   public void testFilesImmediatelyAfterload() throws Exception
   {
      try
      {
         setup(10, 10 * 1024, true);
         createJournal();
         startJournal();
         load();

         List<String> files = fileFactory.listFiles(fileExtension);

         assertEquals(10, files.size());

         for (String file : files)
         {
            assertTrue(file.startsWith(filePrefix));
         }

         stopJournal();

         resetFileFactory();

         setup(20, 10 * 1024, true);
         createJournal();
         startJournal();
         load();

         files = fileFactory.listFiles(fileExtension);

         assertEquals(20, files.size());

         for (String file : files)
         {
            assertTrue(file.startsWith(filePrefix));
         }

         stopJournal();

         fileExtension = "tim";

         resetFileFactory();

         setup(17, 10 * 1024, true);
         createJournal();
         startJournal();
         load();

         files = fileFactory.listFiles(fileExtension);

         assertEquals(17, files.size());

         for (String file : files)
         {
            assertTrue(file.startsWith(filePrefix));
         }

         stopJournal();

         filePrefix = "echidna";

         resetFileFactory();

         setup(11, 10 * 1024, true);
         createJournal();
         startJournal();
         load();

         files = fileFactory.listFiles(fileExtension);

         assertEquals(11, files.size());

         for (String file : files)
         {
            assertTrue(file.startsWith(filePrefix));
         }

         stopJournal();
      }
      finally
      {
         filePrefix = "jbm";

         fileExtension = "jbm";
      }
   }

   public void testEmptyReopen() throws Exception
   {
      setup(2, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(2, files1.size());

      stopJournal();

      setup(2, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files2 = fileFactory.listFiles(fileExtension);

      assertEquals(2, files2.size());

      for (String file : files1)
      {
         assertTrue(files2.contains(file));
      }

      stopJournal();
   }

   public void testCreateFilesOnLoad() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files1.size());

      stopJournal();

      // Now restart with different number of minFiles - should create 10 more

      setup(20, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files2 = fileFactory.listFiles(fileExtension);

      assertEquals(20, files2.size());

      for (String file : files1)
      {
         assertTrue(files2.contains(file));
      }

      stopJournal();
   }

   public void testReduceFreeFiles() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files1.size());

      stopJournal();

      setup(5, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files2 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files2.size());

      for (String file : files1)
      {
         assertTrue(files2.contains(file));
      }

      stopJournal();
   }

   private int calculateRecordsPerFile(final int fileSize, final int alignment, int recordSize)
   {
      recordSize = calculateRecordSize(recordSize, alignment);
      return fileSize / recordSize;
   }

   /** 
    * 
    * Use: calculateNumberOfFiles (fileSize, numberOfRecords, recordSize,  numberOfRecords2, recordSize2, , ...., numberOfRecordsN, recordSizeN);
    * */
   private int calculateNumberOfFiles(final int fileSize, final int alignment, final int... record) throws Exception
   {
      int headerSize = calculateRecordSize(JournalImpl.SIZE_HEADER, alignment);
      int currentPosition = headerSize;
      int totalFiles = 0;

      for (int i = 0; i < record.length; i += 2)
      {
         int numberOfRecords = record[i];
         int recordSize = calculateRecordSize(record[i + 1], alignment);

         while (numberOfRecords > 0)
         {
            int recordsFit = (fileSize - currentPosition) / recordSize;
            if (numberOfRecords < recordsFit)
            {
               currentPosition = currentPosition + numberOfRecords * recordSize;
               numberOfRecords = 0;
            }
            else if (recordsFit > 0)
            {
               currentPosition = currentPosition + recordsFit * recordSize;
               numberOfRecords -= recordsFit;
            }
            else
            {
               totalFiles++;
               currentPosition = headerSize;
            }
         }
      }

      return totalFiles;

   }

   public void testCheckCreateMoreFiles() throws Exception
   {
      setup(2, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(2, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      // Fill all the files

      for (int i = 0; i < 91; i++)
      {
         add(i);
      }

      int numberOfFiles = calculateNumberOfFiles(10 * 1024,
                                                 journal.getAlignment(),
                                                 91,
                                                 JournalImpl.SIZE_ADD_RECORD + recordLength);

      assertEquals(numberOfFiles, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(91, journal.getIDMapSize());

      List<String> files2 = fileFactory.listFiles(fileExtension);

      // The Journal will aways have a file ready to be opened
      assertEquals(numberOfFiles + 2, files2.size());

      assertEquals(1, journal.getOpenedFilesCount());

      for (String file : files1)
      {
         assertTrue(files2.contains(file));
      }

      // Now add some more

      for (int i = 90; i < 95; i++)
      {
         add(i);
      }

      numberOfFiles = calculateNumberOfFiles(10 * 1024,
                                             journal.getAlignment(),
                                             95,
                                             JournalImpl.SIZE_ADD_RECORD + recordLength);

      assertEquals(numberOfFiles, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(95, journal.getIDMapSize());

      List<String> files3 = fileFactory.listFiles(fileExtension);

      assertEquals(numberOfFiles + 2, files3.size());
      assertEquals(1, journal.getOpenedFilesCount());

      for (String file : files1)
      {
         assertTrue(files3.contains(file));
      }

      // And a load more

      for (int i = 95; i < 200; i++)
      {
         add(i);
      }

      numberOfFiles = calculateNumberOfFiles(10 * 1024,
                                             journal.getAlignment(),
                                             200,
                                             JournalImpl.SIZE_ADD_RECORD + recordLength);

      assertEquals(numberOfFiles, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(200, journal.getIDMapSize());

      List<String> files4 = fileFactory.listFiles(fileExtension);

      assertEquals(numberOfFiles + 2, files4.size());
      assertEquals(1, journal.getOpenedFilesCount());

      for (String file : files1)
      {
         assertTrue(files4.contains(file));
      }

      stopJournal();
   }

   // Validate the methods that are used on assertions
   public void testCalculations() throws Exception
   {

      assertEquals(0, calculateNumberOfFiles(10 * 1024, 1, 1, 10, 2, 20));
      assertEquals(0, calculateNumberOfFiles(10 * 1024, 512, 1, 1));
      assertEquals(0, calculateNumberOfFiles(10 * 1024, 512, 19, 10));
      assertEquals(1, calculateNumberOfFiles(10 * 1024, 512, 20, 10));
      assertEquals(0, calculateNumberOfFiles(3000, 500, 2, 1000, 1, 500));
      assertEquals(1, calculateNumberOfFiles(3000, 500, 2, 1000, 1, 1000));
      assertEquals(9, calculateNumberOfFiles(10240, 1, 90, 1038, 45, 10));
      assertEquals(11, calculateNumberOfFiles(10 * 1024, 512, 60, 14 + 1024, 30, 14));
   }

   public void testReclaim() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      int addRecordsPerFile = calculateRecordsPerFile(10 * 1024,
                                                      journal.getAlignment(),
                                                      JournalImpl.SIZE_ADD_RECORD + recordLength);

      // Fills exactly 10 files
      int initialNumberOfAddRecords = addRecordsPerFile * 10;
      for (int i = 0; i < initialNumberOfAddRecords; i++)
      {
         add(i);
      }

      // We have already 10 files, but since we have the last file on exact
      // size, the counter will be numberOfUsedFiles -1
      assertEquals(9, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(initialNumberOfAddRecords, journal.getIDMapSize());

      List<String> files4 = fileFactory.listFiles(fileExtension);

      assertEquals(11, files4.size());
      assertEquals(1, journal.getOpenedFilesCount());

      for (String file : files1)
      {
         assertTrue(files4.contains(file));
      }

      // Now delete half of them

      int deleteRecordsPerFile = calculateRecordsPerFile(10 * 1024,
                                                         journal.getAlignment(),
                                                         JournalImpl.SIZE_DELETE_RECORD);

      for (int i = 0; i < initialNumberOfAddRecords / 2; i++)
      {
         delete(i);
      }

      int numberOfFiles = calculateNumberOfFiles(10 * 1024,
                                                 journal.getAlignment(),
                                                 initialNumberOfAddRecords,
                                                 JournalImpl.SIZE_ADD_RECORD + recordLength,
                                                 initialNumberOfAddRecords / 2,
                                                 JournalImpl.SIZE_DELETE_RECORD);

      if (initialNumberOfAddRecords / 2 % deleteRecordsPerFile == 0)
      {
         // The file is already full, next add would fix it
         numberOfFiles--;
      }

      assertEquals(numberOfFiles, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(initialNumberOfAddRecords / 2, journal.getIDMapSize());

      // Make sure the deletes aren't in the current file

      for (int i = 0; i < 10; i++)
      {
         add(initialNumberOfAddRecords + i);
      }

      numberOfFiles = calculateNumberOfFiles(10 * 1024,
                                             journal.getAlignment(),
                                             initialNumberOfAddRecords,
                                             JournalImpl.SIZE_ADD_RECORD + recordLength,
                                             initialNumberOfAddRecords / 2,
                                             JournalImpl.SIZE_DELETE_RECORD,
                                             10,
                                             JournalImpl.SIZE_ADD_RECORD + recordLength);

      assertEquals(numberOfFiles, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(initialNumberOfAddRecords / 2 + 10, journal.getIDMapSize());

      checkAndReclaimFiles();

      // Several of them should be reclaimed - and others deleted - the total
      // number of files should not drop below
      // 10

      assertEquals(journal.getAlignment() == 1 ? 6 : 7, journal.getDataFilesCount());
      assertEquals(journal.getAlignment() == 1 ? 2 : 1, journal.getFreeFilesCount());
      assertEquals(initialNumberOfAddRecords / 2 + 10, journal.getIDMapSize());

      List<String> files5 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files5.size());
      assertEquals(1, journal.getOpenedFilesCount());

      // Now delete the rest

      for (int i = initialNumberOfAddRecords / 2; i < initialNumberOfAddRecords + 10; i++)
      {
         delete(i);
      }

      // And fill the current file

      for (int i = 110; i < 120; i++)
      {
         add(i);
         delete(i);
      }

      checkAndReclaimFiles();

      assertEquals(journal.getAlignment() == 1 ? 0 : 1, journal.getDataFilesCount());
      assertEquals(journal.getAlignment() == 1 ? 8 : 7, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      List<String> files6 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files6.size());
      assertEquals(1, journal.getOpenedFilesCount());

      stopJournal();
   }

   public void testReclaimAddUpdateDeleteDifferentFiles1() throws Exception
   {
      // Make sure there is one record per file
      setup(2, calculateRecordSize(8, getAlignment()) + calculateRecordSize(JournalImpl.SIZE_ADD_RECORD + recordLength,
                                                                            getAlignment()), true);
      createJournal();
      startJournal();
      load();

      add(1);
      update(1);
      delete(1);

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files1.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      checkAndReclaimFiles();

      List<String> files2 = fileFactory.listFiles(fileExtension);

      // 1 file for nextOpenedFile
      assertEquals(4, files2.size());
      assertEquals(1, journal.getOpenedFilesCount());

      // 1 gets deleted and 1 gets reclaimed

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      stopJournal();
   }

   public void testReclaimAddUpdateDeleteDifferentFiles2() throws Exception
   {
      // Make sure there is one record per file
      setup(2, calculateRecordSize(8, getAlignment()) + calculateRecordSize(JournalImpl.SIZE_ADD_RECORD + recordLength,
                                                                            getAlignment()), true);

      createJournal();
      startJournal();
      load();

      add(1);
      update(1);
      add(2);

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files1.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(2, journal.getIDMapSize());

      checkAndReclaimFiles();

      List<String> files2 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files2.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(2, journal.getIDMapSize());

      stopJournal();
   }

   public void testReclaimTransactionalAddCommit() throws Exception
   {
      testReclaimTransactionalAdd(true);
   }

   public void testReclaimTransactionalAddRollback() throws Exception
   {
      testReclaimTransactionalAdd(false);
   }

   // TODO commit and rollback, also transactional deletes

   private void testReclaimTransactionalAdd(final boolean commit) throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      for (int i = 0; i < 100; i++)
      {
         addTx(1, i);
      }

      assertEquals(calculateNumberOfFiles(fileSize, journal.getAlignment(), 100, recordLength),
                   journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      List<String> files2 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize, journal.getAlignment(), 100, recordLength) + 2, files2.size());
      assertEquals(1, journal.getOpenedFilesCount());

      for (String file : files1)
      {
         assertTrue(files2.contains(file));
      }

      checkAndReclaimFiles();

      // Make sure nothing reclaimed

      assertEquals(calculateNumberOfFiles(fileSize, journal.getAlignment(), 100, recordLength),
                   journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      List<String> files3 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize, journal.getAlignment(), 100, recordLength) + 2, files3.size());
      assertEquals(1, journal.getOpenedFilesCount());

      for (String file : files1)
      {
         assertTrue(files3.contains(file));
      }

      // Add a load more updates

      for (int i = 100; i < 200; i++)
      {
         updateTx(1, i);
      }

      assertEquals(calculateNumberOfFiles(fileSize, journal.getAlignment(), 200, recordLength),
                   journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      List<String> files4 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize, journal.getAlignment(), 200, recordLength) + 2, files4.size());
      assertEquals(1, journal.getOpenedFilesCount());

      for (String file : files1)
      {
         assertTrue(files4.contains(file));
      }

      checkAndReclaimFiles();

      // Make sure nothing reclaimed

      assertEquals(calculateNumberOfFiles(fileSize, journal.getAlignment(), 200, recordLength),
                   journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      List<String> files5 = fileFactory.listFiles(fileExtension);

      assertEquals(24, files5.size());
      assertEquals(1, journal.getOpenedFilesCount());

      for (String file : files1)
      {
         assertTrue(files5.contains(file));
      }

      // Now delete them

      for (int i = 0; i < 200; i++)
      {
         deleteTx(1, i);
      }

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          200,
                                          recordLength,
                                          200,
                                          JournalImpl.SIZE_DELETE_RECORD_TX), journal.getDataFilesCount());

      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      List<String> files7 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          200,
                                          recordLength,
                                          200,
                                          JournalImpl.SIZE_DELETE_RECORD_TX) + 2, files7.size());
      assertEquals(1, journal.getOpenedFilesCount());

      for (String file : files1)
      {
         assertTrue(files7.contains(file));
      }

      checkAndReclaimFiles();

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          200,
                                          recordLength,
                                          200,
                                          JournalImpl.SIZE_DELETE_RECORD_TX), journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      List<String> files8 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          200,
                                          recordLength,
                                          200,
                                          JournalImpl.SIZE_DELETE_RECORD_TX) + 2, files8.size());
      assertEquals(1, journal.getOpenedFilesCount());

      for (String file : files1)
      {
         assertTrue(files8.contains(file));
      }

      // Commit

      if (commit)
      {
         commit(1);
      }
      else
      {
         rollback(1);
      }

      // Add more records to make sure we get to the next file

      for (int i = 200; i < 210; i++)
      {
         add(i);
      }

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          200,
                                          recordLength,
                                          200,
                                          JournalImpl.SIZE_DELETE_RECORD_TX,
                                          1,
                                          JournalImpl.SIZE_COMMIT_RECORD,
                                          10,
                                          JournalImpl.SIZE_ADD_RECORD + recordLength), journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(10, journal.getIDMapSize());

      List<String> files9 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          200,
                                          recordLength,
                                          200,
                                          JournalImpl.SIZE_DELETE_RECORD_TX,
                                          1,
                                          JournalImpl.SIZE_COMMIT_RECORD,
                                          10,
                                          JournalImpl.SIZE_ADD_RECORD + recordLength) + 2, files9.size());
      assertEquals(1, journal.getOpenedFilesCount());

      for (String file : files1)
      {
         assertTrue(files9.contains(file));
      }

      checkAndReclaimFiles();

      // Most Should now be reclaimed - leaving 10 left in total

      assertEquals(journal.getAlignment() == 1 ? 1 : 2, journal.getDataFilesCount());
      assertEquals(journal.getAlignment() == 1 ? 7 : 6, journal.getFreeFilesCount());
      assertEquals(10, journal.getIDMapSize());

      List<String> files10 = fileFactory.listFiles(fileExtension);

      // The journal will aways keep one file opened (even if there are no more
      // files on freeFiles)
      assertEquals(10, files10.size());
      assertEquals(1, journal.getOpenedFilesCount());
   }

   public void testReclaimTransactionalSimple() throws Exception
   {
      setup(2, calculateRecordSize(JournalImpl.SIZE_HEADER, getAlignment()) + calculateRecordSize(recordLength,
                                                                                                  getAlignment()), true);
      createJournal();
      startJournal();
      load();
      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(2, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      addTx(1, 1); // in file 0

      deleteTx(1, 1); // in file 1

      journal.debugWait();

      System.out.println("journal tmp :" + journal.debug());

      List<String> files2 = fileFactory.listFiles(fileExtension);

      assertEquals(3, files2.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(1, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      // Make sure we move on to the next file

      addWithSize(recordLength - JournalImpl.SIZE_ADD_RECORD, 2); // in file 2

      journal.debugWait();

      System.out.println("journal tmp2 :" + journal.debug());

      List<String> files3 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files3.size());
      assertEquals(1, journal.getOpenedFilesCount());

      log.debug("data files count " + journal.getDataFilesCount());
      log.debug("free files count " + journal.getFreeFilesCount());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getIDMapSize());

      commit(1); // in file 3

      List<String> files4 = fileFactory.listFiles(fileExtension);

      assertEquals(5, files4.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(3, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getIDMapSize());

      // Make sure we move on to the next file

      addWithSize(recordLength - JournalImpl.SIZE_ADD_RECORD, 3); // in file 4

      List<String> files5 = fileFactory.listFiles(fileExtension);

      assertEquals(6, files5.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(4, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(2, journal.getIDMapSize());

      checkAndReclaimFiles();

      List<String> files6 = fileFactory.listFiles(fileExtension);

      // Three should get deleted (files 0, 1, 3)

      assertEquals(3, files6.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(1, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(2, journal.getIDMapSize());

      // Now restart

      journal.checkAndReclaimFiles();

      System.out.println("journal:" + journal.debug());

      stopJournal(false);
      createJournal();
      startJournal();
      loadAndCheck();

      assertEquals(3, files6.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(1, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(2, journal.getIDMapSize());
   }

   public void testAddDeleteCommitTXIDMap1() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      addTx(1, 1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      deleteTx(1, 1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      commit(1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());
   }

   public void testAddCommitTXIDMap1() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      addTx(1, 1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      commit(1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());
   }

   public void testAddDeleteCommitTXIDMap2() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      add(1, 1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      deleteTx(1, 1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      commit(1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());
   }

   public void testAddDeleteRollbackTXIDMap1() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      addTx(1, 1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      deleteTx(1, 1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      rollback(1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());
   }

   public void testAddRollbackTXIDMap1() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      addTx(1, 1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      rollback(1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());
   }

   public void testAddDeleteRollbackTXIDMap2() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      add(1, 1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      deleteTx(1, 1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      rollback(1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());
   }

   public void testAddDeleteIDMap() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(10, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      add(1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      delete(1);

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(8, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

   }

   public void testCommitRecordsInFileReclaim() throws Exception
   {
      setup(2, calculateRecordSize(JournalImpl.SIZE_HEADER, getAlignment()) + calculateRecordSize(recordLength,
                                                                                                  getAlignment()), true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(2, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      addTx(1, 1);

      List<String> files2 = fileFactory.listFiles(fileExtension);

      assertEquals(2, files2.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      // Make sure we move on to the next file

      commit(1);

      List<String> files3 = fileFactory.listFiles(fileExtension);

      assertEquals(3, files3.size());

      assertEquals(1, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      addWithSize(recordLength - JournalImpl.SIZE_ADD_RECORD, 2);

      // Move on to another file

      List<String> files4 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files4.size());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(2, journal.getIDMapSize());

      checkAndReclaimFiles();

      // Nothing should be reclaimed

      List<String> files5 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files5.size());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(2, journal.getIDMapSize());
   }

   // file 1: add 1 tx,
   // file 2: commit 1, add 2, delete 2
   // file 3: add 3

   public void testCommitRecordsInFileNoReclaim() throws Exception
   {
      setup(2, calculateRecordSize(JournalImpl.SIZE_HEADER, getAlignment()) + calculateRecordSize(recordLength,
                                                                                                  getAlignment()) +
               512, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(2, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      addTx(1, 1); // in file 0

      // Make sure we move on to the next file

      addWithSize(recordLength - JournalImpl.SIZE_ADD_RECORD, 2); // in file 1

      List<String> files2 = fileFactory.listFiles(fileExtension);

      assertEquals(3, files2.size());

      assertEquals(calculateNumberOfFiles(fileSize, journal.getAlignment(), 2, recordLength),
                   journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      commit(1); // in file 1

      List<String> files3 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_COMMIT_RECORD) + 2, files3.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_COMMIT_RECORD), journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(2, journal.getIDMapSize());

      delete(2); // in file 1

      List<String> files4 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_COMMIT_RECORD,
                                          1,
                                          JournalImpl.SIZE_DELETE_RECORD) + 2, files4.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_COMMIT_RECORD,
                                          1,
                                          JournalImpl.SIZE_DELETE_RECORD), journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getIDMapSize());

      // Move on to another file

      addWithSize(recordLength - JournalImpl.SIZE_ADD_RECORD, 3); // in file 2

      List<String> files5 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files5.size());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(2, journal.getIDMapSize());

      checkAndReclaimFiles();

      List<String> files6 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files6.size());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(2, journal.getIDMapSize());

      // Restart

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      List<String> files7 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files7.size());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(2, journal.getIDMapSize());
   }

   public void testRollbackRecordsInFileNoReclaim() throws Exception
   {
      setup(2, calculateRecordSize(JournalImpl.SIZE_HEADER, getAlignment()) + calculateRecordSize(recordLength,
                                                                                                  getAlignment()) +
               512, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(2, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      addTx(1, 1); // in file 0

      // Make sure we move on to the next file

      addWithSize(recordLength - JournalImpl.SIZE_ADD_RECORD, 2); // in file 1

      List<String> files2 = fileFactory.listFiles(fileExtension);

      assertEquals(3, files2.size());

      assertEquals(1, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      rollback(1); // in file 1

      List<String> files3 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_ROLLBACK_RECORD) + 2, files3.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_ROLLBACK_RECORD), journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getIDMapSize());

      delete(2); // in file 1

      List<String> files4 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_ROLLBACK_RECORD,
                                          1,
                                          JournalImpl.SIZE_DELETE_RECORD) + 2, files4.size());
      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_ROLLBACK_RECORD,
                                          1,
                                          JournalImpl.SIZE_DELETE_RECORD), journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      // Move on to another file

      addWithSize(recordLength - JournalImpl.SIZE_ADD_RECORD, 3); // in file 2
      // (current
      // file)

      List<String> files5 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_ROLLBACK_RECORD,
                                          1,
                                          JournalImpl.SIZE_DELETE_RECORD,
                                          1,
                                          recordLength) + 2, files5.size());

      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_ROLLBACK_RECORD,
                                          1,
                                          JournalImpl.SIZE_DELETE_RECORD,
                                          1,
                                          recordLength), journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getIDMapSize());

      checkAndReclaimFiles();

      List<String> files6 = fileFactory.listFiles(fileExtension);

      // files 0 and 1 should be deleted

      assertEquals(journal.getAlignment() == 1 ? 2 : 3, files6.size());

      assertEquals(journal.getAlignment() == 1 ? 0 : 1, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      // Restart

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      List<String> files7 = fileFactory.listFiles(fileExtension);

      assertEquals(journal.getAlignment() == 1 ? 2 : 3, files7.size());

      assertEquals(journal.getAlignment() == 1 ? 0 : 1, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());
   }

   public void testEmptyPrepare() throws Exception
   {
      setup(2, calculateRecordSize(JournalImpl.SIZE_HEADER, getAlignment()) + calculateRecordSize(recordLength,
                                                                                                  getAlignment()) +
               512, true);
      createJournal();
      startJournal();
      load();

      EncodingSupport xid = new SimpleEncoding(10, (byte)0);
      prepare(1, xid);

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      commit(1);

      xid = new SimpleEncoding(10, (byte)1);
      prepare(2, xid);

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      rollback(2);

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

   }

   public void testPrepareNoReclaim() throws Exception
   {
      setup(2, calculateRecordSize(JournalImpl.SIZE_HEADER, getAlignment()) + calculateRecordSize(recordLength,
                                                                                                  getAlignment()) +
               512, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(2, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      addTx(1, 1); // in file 0

      // Make sure we move on to the next file

      addWithSize(1024 - JournalImpl.SIZE_ADD_RECORD, 2); // in file 1

      List<String> files2 = fileFactory.listFiles(fileExtension);

      assertEquals(3, files2.size());

      assertEquals(calculateNumberOfFiles(fileSize, journal.getAlignment(), 2, recordLength),
                   journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      EncodingSupport xid = new SimpleEncoding(10, (byte)0);
      prepare(1, xid); // in file 1

      List<String> files3 = fileFactory.listFiles(fileExtension);

      assertEquals(3, files3.size());

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_PREPARE_RECORD), journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getIDMapSize());
      assertEquals(1, journal.getOpenedFilesCount());

      delete(2); // in file 1

      List<String> files4 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_PREPARE_RECORD,
                                          1,
                                          JournalImpl.SIZE_DELETE_RECORD) + 2, files4.size());

      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_PREPARE_RECORD,
                                          1,
                                          JournalImpl.SIZE_DELETE_RECORD), journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      // Move on to another file

      addWithSize(recordLength - JournalImpl.SIZE_ADD_RECORD, 3); // in file 2

      List<String> files5 = fileFactory.listFiles(fileExtension);

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_PREPARE_RECORD,
                                          1,
                                          JournalImpl.SIZE_DELETE_RECORD,
                                          1,
                                          recordLength) + 2, files5.size());

      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(calculateNumberOfFiles(fileSize,
                                          journal.getAlignment(),
                                          2,
                                          recordLength,
                                          1,
                                          JournalImpl.SIZE_PREPARE_RECORD,
                                          1,
                                          JournalImpl.SIZE_DELETE_RECORD,
                                          1,
                                          recordLength), journal.getDataFilesCount());

      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getIDMapSize());

      checkAndReclaimFiles();

      List<String> files6 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files6.size());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      addWithSize(recordLength - JournalImpl.SIZE_ADD_RECORD, 4); // in file 3

      List<String> files7 = fileFactory.listFiles(fileExtension);

      assertEquals(5, files7.size());

      assertEquals(3, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(2, journal.getIDMapSize());

      commit(1); // in file 4

      List<String> files8 = fileFactory.listFiles(fileExtension);

      assertEquals(5, files8.size());

      assertEquals(3, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(3, journal.getIDMapSize());

      // Restart

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

   }

   public void testPrepareReclaim() throws Exception
   {
      setup(2, 100 * 1024, true);
      createJournal();
      startJournal();
      load();

      List<String> files1 = fileFactory.listFiles(fileExtension);

      assertEquals(2, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      addTx(1, 1); // in file 0

      files1 = fileFactory.listFiles(fileExtension);

      assertEquals(2, files1.size());

      assertEquals(0, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getIDMapSize());

      // Make sure we move on to the next file

      journal.forceMoveNextFile();

      journal.debugWait();

      addWithSize(recordLength - JournalImpl.SIZE_ADD_RECORD, 2); // in file 1

      List<String> files2 = fileFactory.listFiles(fileExtension);

      assertEquals(3, files2.size());

      assertEquals(1, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getIDMapSize());

      EncodingSupport xid = new SimpleEncoding(10, (byte)0);
      prepare(1, xid); // in file 1

      List<String> files3 = fileFactory.listFiles(fileExtension);

      assertEquals(3, files3.size());

      assertEquals(1, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(1, journal.getIDMapSize());

      delete(2); // in file 1

      List<String> files4 = fileFactory.listFiles(fileExtension);

      assertEquals(3, files4.size());

      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(1, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(0, journal.getIDMapSize());

      // Move on to another file

      journal.forceMoveNextFile();

      addWithSize(1024 - JournalImpl.SIZE_ADD_RECORD, 3); // in file 2

      checkAndReclaimFiles();

      List<String> files5 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files5.size());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getIDMapSize());
      assertEquals(1, journal.getOpenedFilesCount());

      checkAndReclaimFiles();

      List<String> files6 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files6.size());

      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getIDMapSize());
      assertEquals(1, journal.getOpenedFilesCount());

      journal.forceMoveNextFile();

      addWithSize(1024 - JournalImpl.SIZE_ADD_RECORD, 4); // in file 3

      List<String> files7 = fileFactory.listFiles(fileExtension);

      assertEquals(5, files7.size());

      assertEquals(3, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(2, journal.getIDMapSize());

      commit(1); // in file 3

      List<String> files8 = fileFactory.listFiles(fileExtension);

      assertEquals(5, files8.size());

      assertEquals(3, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(3, journal.getIDMapSize());
      assertEquals(1, journal.getOpenedFilesCount());

      delete(1); // in file 3

      List<String> files9 = fileFactory.listFiles(fileExtension);

      assertEquals(5, files9.size());

      assertEquals(3, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(2, journal.getIDMapSize());

      checkAndReclaimFiles();

      List<String> files10 = fileFactory.listFiles(fileExtension);

      assertEquals(journal.getAlignment() == 1 ? 5 : 5, files10.size());

      assertEquals(journal.getAlignment() == 1 ? 3 : 3, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(2, journal.getIDMapSize());

      journal.forceMoveNextFile();

      addWithSize(1024 - JournalImpl.SIZE_ADD_RECORD, 5); // in file 4

      List<String> files11 = fileFactory.listFiles(fileExtension);

      assertEquals(6, files11.size());

      assertEquals(4, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(3, journal.getIDMapSize());

      checkAndReclaimFiles();

      List<String> files12 = fileFactory.listFiles(fileExtension);

      // File 0, and File 1 should be deleted

      assertEquals(4, files12.size());

      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(3, journal.getIDMapSize());

      // Restart

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      delete(4);

      assertEquals(1, journal.getOpenedFilesCount());

      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(2, journal.getIDMapSize());

      addWithSize(1024 - JournalImpl.SIZE_ADD_RECORD, 6);

      log.debug("Debug journal on testPrepareReclaim ->\n" + debugJournal());

      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(3, journal.getIDMapSize());

      checkAndReclaimFiles();

      // file 3 should now be deleted

      List<String> files15 = fileFactory.listFiles(fileExtension);

      assertEquals(4, files15.size());

      assertEquals(1, journal.getOpenedFilesCount());
      assertEquals(2, journal.getDataFilesCount());
      assertEquals(0, journal.getFreeFilesCount());
      assertEquals(3, journal.getIDMapSize());

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   // Non transactional tests
   // =======================

   public void testSimpleAdd() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAdd() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAddNonContiguous() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testSimpleAddUpdate() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1);
      update(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAddUpdate() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      update(1, 2, 4, 7, 9, 10);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAddUpdateAll() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      update(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAddUpdateNonContiguous() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      add(3, 7, 10, 13, 56, 100, 200, 202, 203);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAddUpdateAllNonContiguous() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      update(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testSimpleAddUpdateDelete() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1);
      update(1);
      delete(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testSimpleAddUpdateDeleteTransactional() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1);
      commit(1);
      updateTx(2, 1);
      commit(2);
      deleteTx(3, 1);
      commit(3);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAddUpdateDelete() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      update(1, 2, 4, 7, 9, 10);
      delete(1, 4, 7, 9, 10);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAddUpdateDeleteAll() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      update(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      update(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAddUpdateDeleteNonContiguous() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      add(3, 7, 10, 13, 56, 100, 200, 202, 203);
      delete(3, 10, 56, 100, 200, 203);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAddUpdateDeleteAllNonContiguous() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      update(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      delete(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAddUpdateDeleteDifferentOrder() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      update(203, 202, 201, 200, 102, 100, 1, 3, 5, 7, 10, 13, 56);
      delete(56, 13, 10, 7, 5, 3, 1, 203, 202, 201, 200, 102, 100);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleAddUpdateDeleteDifferentRecordLengths() throws Exception
   {
      setup(10, 2048, true);
      createJournal();
      startJournal();
      load();

      for (int i = 0; i < 100; i++)
      {
         byte[] record = generateRecord(10 + (int)(1500 * Math.random()));

         journal.appendAddRecord(i, (byte)0, record, false);

         records.add(new RecordInfo(i, (byte)0, record, false));
      }

      for (int i = 0; i < 100; i++)
      {
         byte[] record = generateRecord(10 + (int)(1024 * Math.random()));

         journal.appendUpdateRecord(i, (byte)0, record, false);

         records.add(new RecordInfo(i, (byte)0, record, true));
      }

      for (int i = 0; i < 100; i++)
      {
         journal.appendDeleteRecord(i, false);

         removeRecordsForID(i);
      }

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
      stopJournal();
   }

   public void testAddUpdateDeleteRestartAndContinue() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3);
      update(1, 2);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
      add(4, 5, 6);
      update(5);
      delete(3);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
      add(7, 8);
      delete(1, 2);
      delete(4, 5, 6);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testSimpleAddTXReload() throws Exception
   {
      setup(2, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1);
      commit(1);

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

   }

   public void testSimpleAddTXXAReload() throws Exception
   {
      setup(2, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1);

      EncodingSupport xid = new SimpleEncoding(10, (byte)'p');

      prepare(1, xid);

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

   }

   public void testAddUpdateDeleteTransactionalRestartAndContinue() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3);
      updateTx(1, 1, 2);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
      addTx(2, 4, 5, 6);
      update(2, 2);
      delete(2, 3);
      commit(2);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
      addTx(3, 7, 8);
      deleteTx(3, 1);
      deleteTx(3, 4, 5, 6);
      commit(3);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testFillFileExactly() throws Exception
   {
      recordLength = 500;

      int numRecords = 2;

      // The real appended record size in the journal file = SIZE_BYTE +
      // SIZE_LONG + SIZE_INT + recordLength + SIZE_BYTE

      int realLength = calculateRecordSize(JournalImpl.SIZE_ADD_RECORD + recordLength, getAlignment());

      int fileSize = numRecords * realLength + calculateRecordSize(8, getAlignment()); // 8
      // for
      // timestamp

      setup(10, fileSize, true);

      createJournal();
      startJournal();
      load();

      add(1, 2);

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      add(3, 4);

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      add(4, 5, 6, 7, 8, 9, 10);

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   // Transactional tests
   // ===================

   public void testSimpleTransaction() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1);
      updateTx(1, 1);
      deleteTx(1, 1);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testTransactionDontDeleteAll() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1, 2, 3);
      updateTx(1, 1, 2);
      deleteTx(1, 1);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testTransactionDeleteAll() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1, 2, 3);
      updateTx(1, 1, 2);
      deleteTx(1, 1, 2, 3);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testTransactionUpdateFromBeforeTx() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3);
      addTx(1, 4, 5, 6);
      updateTx(1, 1, 5);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testTransactionDeleteFromBeforeTx() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3);
      addTx(1, 4, 5, 6);
      deleteTx(1, 1, 2, 3, 4, 5, 6);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testTransactionChangesNotVisibleOutsideTX() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3);
      addTx(1, 4, 5, 6);
      updateTx(1, 1, 2, 4, 5);
      deleteTx(1, 1, 2, 3, 4, 5, 6);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleTransactionsDifferentIDs() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      addTx(1, 1, 2, 3, 4, 5, 6);
      updateTx(1, 1, 3, 5);
      deleteTx(1, 1, 2, 3, 4, 5, 6);
      commit(1);

      addTx(2, 11, 12, 13, 14, 15, 16);
      updateTx(2, 11, 13, 15);
      deleteTx(2, 11, 12, 13, 14, 15, 16);
      commit(2);

      addTx(3, 21, 22, 23, 24, 25, 26);
      updateTx(3, 21, 23, 25);
      deleteTx(3, 21, 22, 23, 24, 25, 26);
      commit(3);

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleInterleavedTransactionsDifferentIDs() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      addTx(1, 1, 2, 3, 4, 5, 6);
      addTx(3, 21, 22, 23, 24, 25, 26);
      updateTx(1, 1, 3, 5);
      addTx(2, 11, 12, 13, 14, 15, 16);
      deleteTx(1, 1, 2, 3, 4, 5, 6);
      updateTx(2, 11, 13, 15);
      updateTx(3, 21, 23, 25);
      deleteTx(2, 11, 12, 13, 14, 15, 16);
      deleteTx(3, 21, 22, 23, 24, 25, 26);

      commit(1);
      commit(2);
      commit(3);

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testMultipleInterleavedTransactionsSameIDs() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();

      add(1, 2, 3, 4, 5, 6, 7, 8);
      addTx(1, 9, 10, 11, 12);
      addTx(2, 13, 14, 15, 16, 17);
      addTx(3, 18, 19, 20, 21, 22);
      updateTx(1, 1, 2, 3);
      updateTx(2, 4, 5, 6);
      commit(2);
      updateTx(3, 7, 8);
      deleteTx(1, 1, 2);
      commit(1);
      deleteTx(3, 7, 8);
      commit(3);

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testTransactionMixed() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      addTx(1, 675, 676, 677, 700, 703);
      update(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      updateTx(1, 677, 700);
      delete(1, 3, 5, 7, 10, 13, 56, 100, 102, 200, 201, 202, 203);
      deleteTx(1, 703, 675);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testTransactionAddDeleteDifferentOrder() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      deleteTx(1, 9, 8, 5, 3, 7, 6, 2, 1, 4);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testAddOutsideTXThenUpdateInsideTX() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3);
      updateTx(1, 1, 2, 3);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testAddOutsideTXThenDeleteInsideTX() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3);
      deleteTx(1, 1, 2, 3);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testRollback() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3);
      deleteTx(1, 1, 2, 3);
      rollback(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testRollbackMultiple() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3);
      deleteTx(1, 1, 2, 3);
      addTx(2, 4, 5, 6);
      rollback(1);
      rollback(2);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testIsolation1() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1, 2, 3);
      deleteTx(1, 1, 2, 3);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testIsolation2() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1, 2, 3);
      try
      {
         update(1);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // Ok
      }

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testIsolation3() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1, 2, 3);
      try
      {
         delete(1);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // Ok
      }

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   // XA tests
   // ========

   public void testXASimpleNotPrepared() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      updateTx(1, 1, 2, 3, 4, 7, 8);
      deleteTx(1, 1, 2, 3, 4, 5);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testXASimplePrepared() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      updateTx(1, 1, 2, 3, 4, 7, 8);
      deleteTx(1, 1, 2, 3, 4, 5);

      EncodingSupport xid = new SimpleEncoding(10, (byte)0);

      prepare(1, xid);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testXASimpleCommit() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      updateTx(1, 1, 2, 3, 4, 7, 8);
      deleteTx(1, 1, 2, 3, 4, 5);

      EncodingSupport xid = new SimpleEncoding(10, (byte)0);

      prepare(1, xid);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testXASimpleRollback() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      addTx(1, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      updateTx(1, 1, 2, 3, 4, 7, 8);
      deleteTx(1, 1, 2, 3, 4, 5);

      EncodingSupport xid = new SimpleEncoding(10, (byte)0);

      prepare(1, xid);
      rollback(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testXAChangesNotVisibleNotPrepared() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3, 4, 5, 6);
      addTx(1, 7, 8, 9, 10);
      updateTx(1, 1, 2, 3, 7, 8, 9);
      deleteTx(1, 1, 2, 3, 4, 5);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testXAChangesNotVisiblePrepared() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3, 4, 5, 6);
      addTx(1, 7, 8, 9, 10);
      updateTx(1, 1, 2, 3, 7, 8, 9);
      deleteTx(1, 1, 2, 3, 4, 5);

      EncodingSupport xid = new SimpleEncoding(10, (byte)0);

      prepare(1, xid);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testXAChangesNotVisibleRollback() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3, 4, 5, 6);
      addTx(1, 7, 8, 9, 10);
      updateTx(1, 1, 2, 3, 7, 8, 9);
      deleteTx(1, 1, 2, 3, 4, 5);

      EncodingSupport xid = new SimpleEncoding(10, (byte)0);

      prepare(1, xid);
      rollback(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testXAChangesisibleCommit() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3, 4, 5, 6);
      addTx(1, 7, 8, 9, 10);
      updateTx(1, 1, 2, 3, 7, 8, 9);
      deleteTx(1, 1, 2, 3, 4, 5);

      EncodingSupport xid = new SimpleEncoding(10, (byte)0);

      prepare(1, xid);
      commit(1);
      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testXAMultiple() throws Exception
   {
      setup(10, 10 * 1024, true);
      createJournal();
      startJournal();
      load();
      add(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      addTx(1, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
      addTx(2, 21, 22, 23, 24, 25, 26, 27);
      updateTx(1, 1, 3, 6, 11, 14, 17);
      addTx(3, 28, 29, 30, 31, 32, 33, 34, 35);
      updateTx(3, 7, 8, 9, 10);
      deleteTx(2, 4, 5, 6, 23, 25, 27);

      EncodingSupport xid = new SimpleEncoding(10, (byte)0);

      prepare(2, xid);
      deleteTx(1, 1, 2, 11, 14, 15);
      prepare(1, xid);
      deleteTx(3, 28, 31, 32, 9);
      prepare(3, xid);

      commit(1);
      rollback(2);
      commit(3);
   }

   public void testTransactionOnDifferentFiles() throws Exception
   {
      setup(2, 512 + 2 * 1024, true);

      createJournal();
      startJournal();
      load();

      addTx(1, 1, 2, 3, 4, 5, 6);
      updateTx(1, 1, 3, 5);
      commit(1);
      deleteTx(2, 1, 2, 3, 4, 5, 6);
      commit(2);

      // Just to make sure the commit won't be released. The commit will be on
      // the same file as addTx(3);
      addTx(3, 11);
      addTx(4, 31);
      commit(3);

      log.debug("Debug on Journal before stopJournal - \n" + debugJournal());

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();
   }

   public void testReclaimAfterUpdate() throws Exception
   {
      setup(2, 60 * 1024, true);

      createJournal();
      startJournal();
      load();

      int transactionID = 0;

      for (int i = 0; i < 100; i++)
      {
         add(i);
         if (i % 10 == 0 && i > 0)
         {
            journal.forceMoveNextFile();
         }
         update(i);

      }

      for (int i = 100; i < 200; i++)
      {

         addTx(transactionID, i);
         if (i % 10 == 0 && i > 0)
         {
            journal.forceMoveNextFile();
         }
         commit(transactionID++);
         update(i);
      }

      System.out.println("Before stop ****************************");
      System.out.println(journal.debug());
      System.out.println("*****************************************");

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      System.out.println("After start ****************************");
      System.out.println(journal.debug());
      System.out.println("*****************************************");

      journal.forceMoveNextFile();

      for (int i = 0; i < 100; i++)
      {
         delete(i);
      }

      System.out.println("After delete ****************************");
      System.out.println(journal.debug());
      System.out.println("*****************************************");

      for (int i = 100; i < 200; i++)
      {
         updateTx(transactionID, i);
      }

      System.out.println("After updatetx ****************************");
      System.out.println(journal.debug());
      System.out.println("*****************************************");

      journal.forceMoveNextFile();

      commit(transactionID++);

      System.out.println("After commit ****************************");
      System.out.println(journal.debug());
      System.out.println("*****************************************");

      for (int i = 100; i < 200; i++)
      {
         updateTx(transactionID, i);
         deleteTx(transactionID, i);
      }

      System.out.println("After delete ****************************");
      System.out.println(journal.debug());
      System.out.println("*****************************************");

      commit(transactionID++);

      System.out.println("Before reclaim/after commit ****************************");
      System.out.println(journal.debug());
      System.out.println("*****************************************");

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      System.out.println("After reclaim ****************************");
      System.out.println(journal.debug());
      System.out.println("*****************************************");

      journal.forceMoveNextFile();
      journal.checkAndReclaimFiles();

      assertEquals(0, journal.getDataFilesCount());

      stopJournal();
      createJournal();
      startJournal();
      loadAndCheck();

      assertEquals(0, journal.getDataFilesCount());
   }

   protected abstract int getAlignment();

}
