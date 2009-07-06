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

package org.jboss.messaging.core.journal.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.messaging.core.buffers.ChannelBuffer;
import org.jboss.messaging.core.buffers.ChannelBuffers;
import org.jboss.messaging.core.journal.EncodingSupport;
import org.jboss.messaging.core.journal.IOCallback;
import org.jboss.messaging.core.journal.LoaderCallback;
import org.jboss.messaging.core.journal.PreparedTransactionInfo;
import org.jboss.messaging.core.journal.RecordInfo;
import org.jboss.messaging.core.journal.SequentialFile;
import org.jboss.messaging.core.journal.SequentialFileFactory;
import org.jboss.messaging.core.journal.TestableJournal;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.spi.MessagingBuffer;
import org.jboss.messaging.utils.DataConstants;
import org.jboss.messaging.utils.Pair;
import org.jboss.messaging.utils.VariableLatch;
import org.jboss.messaging.utils.concurrent.LinkedBlockingDeque;

/**
 * 
 * <p>A JournalImpl</p
 * 
 * <p>WIKI Page: <a href="http://wiki.jboss.org/wiki/JBossMessaging2Journal"> http://wiki.jboss.org/wiki/JBossMessaging2Journal</a></p>
 * 
 * 
 * <p>Look at {@link JournalImpl#load(LoaderCallback)} for the file layout
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 *
 */
public class JournalImpl implements TestableJournal
{

   // Constants -----------------------------------------------------
   private static final int STATE_STOPPED = 0;

   private static final int STATE_STARTED = 1;

   private static final int STATE_LOADED = 2;

   // Static --------------------------------------------------------

   private static final Logger log = Logger.getLogger(JournalImpl.class);

   private static final boolean trace = log.isTraceEnabled();

   /** This is to be set to true at DEBUG & development only */
   private static final boolean LOAD_TRACE = false;

   // This method exists just to make debug easier.
   // I could replace log.trace by log.info temporarily while I was debugging
   // Journal
   private static final void trace(final String message)
   {
      log.trace(message);
   }

   // The sizes of primitive types

   public static final int MIN_FILE_SIZE = 1024;

   public static final int SIZE_HEADER = DataConstants.SIZE_INT;

   public static final int BASIC_SIZE = DataConstants.SIZE_BYTE + DataConstants.SIZE_INT + DataConstants.SIZE_INT;

   public static final int SIZE_ADD_RECORD = BASIC_SIZE + DataConstants.SIZE_LONG +
                                             DataConstants.SIZE_BYTE +
                                             DataConstants.SIZE_INT /* + record.length */;

   // Record markers - they must be all unique

   public static final byte ADD_RECORD = 11;

   public static final byte SIZE_UPDATE_RECORD = BASIC_SIZE + DataConstants.SIZE_LONG +
                                                 DataConstants.SIZE_BYTE +
                                                 DataConstants.SIZE_INT /* + record.length */;

   public static final byte UPDATE_RECORD = 12;

   public static final int SIZE_ADD_RECORD_TX = BASIC_SIZE + DataConstants.SIZE_LONG +
                                                DataConstants.SIZE_BYTE +
                                                DataConstants.SIZE_LONG +
                                                DataConstants.SIZE_INT /* + record.length */;

   public static final byte ADD_RECORD_TX = 13;

   public static final int SIZE_UPDATE_RECORD_TX = BASIC_SIZE + DataConstants.SIZE_LONG +
                                                   DataConstants.SIZE_BYTE +
                                                   DataConstants.SIZE_LONG +
                                                   DataConstants.SIZE_INT /* + record.length */;

   public static final byte UPDATE_RECORD_TX = 14;

   public static final int SIZE_DELETE_RECORD_TX = BASIC_SIZE + DataConstants.SIZE_LONG +
                                                   DataConstants.SIZE_LONG +
                                                   DataConstants.SIZE_INT /* + record.length */;

   public static final byte DELETE_RECORD_TX = 15;

   public static final int SIZE_DELETE_RECORD = BASIC_SIZE + DataConstants.SIZE_LONG;

   public static final byte DELETE_RECORD = 16;

   public static final int SIZE_COMPLETE_TRANSACTION_RECORD = BASIC_SIZE + DataConstants.SIZE_LONG +
                                                              DataConstants.SIZE_INT;

   public static final int SIZE_PREPARE_RECORD = SIZE_COMPLETE_TRANSACTION_RECORD + DataConstants.SIZE_INT;

   public static final byte PREPARE_RECORD = 17;

   public static final int SIZE_COMMIT_RECORD = SIZE_COMPLETE_TRANSACTION_RECORD;

   public static final byte COMMIT_RECORD = 18;

   public static final int SIZE_ROLLBACK_RECORD = BASIC_SIZE + DataConstants.SIZE_LONG;

   public static final byte ROLLBACK_RECORD = 19;

   public static final byte FILL_CHARACTER = (byte)'J';

   // Attributes ----------------------------------------------------

   private volatile boolean autoReclaim = true;

   private final AtomicInteger nextFileID = new AtomicInteger(0);

   // used for Asynchronous IO only (ignored on NIO).
   private final int maxAIO;

   private final int fileSize;

   private final int minFiles;

   private final float compactPercentage;

   private final int compactMinFiles;

   private final SequentialFileFactory fileFactory;

   public final String filePrefix;

   public final String fileExtension;

   private final LinkedBlockingDeque<JournalFile> dataFiles = new LinkedBlockingDeque<JournalFile>();

   private final LinkedBlockingDeque<JournalFile> pendingCloseFiles = new LinkedBlockingDeque<JournalFile>();

   private final Queue<JournalFile> freeFiles = new ConcurrentLinkedQueue<JournalFile>();

   private final BlockingQueue<JournalFile> openedFiles = new LinkedBlockingQueue<JournalFile>();

   // Compacting may replace this structure
   private final ConcurrentMap<Long, JournalRecord> records = new ConcurrentHashMap<Long, JournalRecord>();

   // Compacting may replace this structure
   private final ConcurrentMap<Long, JournalTransaction> transactions = new ConcurrentHashMap<Long, JournalTransaction>();

   // This will be set only while the JournalCompactor is being executed
   private volatile JournalCompactor compactor;

   // Latch used to wait compactor finish, to make sure we won't stop the journal with the compactor running
   private final VariableLatch compactorWait = new VariableLatch();

   private ExecutorService filesExecutor = null;

   private final Semaphore lock = new Semaphore(1);

   /** We don't lock the journal while compacting, however we need to lock it while taking and updating snapshots */
   private final ReadWriteLock compactingLock = new ReentrantReadWriteLock();

   private volatile JournalFile currentFile;

   private volatile int state;

   private final Reclaimer reclaimer = new Reclaimer();

   // Constructors --------------------------------------------------

   public JournalImpl(final int fileSize,
                      final int minFiles,
                      final int compactMinFiles,
                      final int compactPercentage,
                      final SequentialFileFactory fileFactory,
                      final String filePrefix,
                      final String fileExtension,
                      final int maxAIO)
   {
      if (fileFactory == null)
      {
         throw new NullPointerException("fileFactory is null");
      }
      if (fileSize < MIN_FILE_SIZE)
      {
         throw new IllegalArgumentException("File size cannot be less than " + MIN_FILE_SIZE + " bytes");
      }
      if (fileSize % fileFactory.getAlignment() != 0)
      {
         throw new IllegalArgumentException("Invalid journal-file-size " + fileSize +
                                            ", It should be multiple of " +
                                            fileFactory.getAlignment());
      }
      if (minFiles < 2)
      {
         throw new IllegalArgumentException("minFiles cannot be less than 2");
      }
      if (filePrefix == null)
      {
         throw new NullPointerException("filePrefix is null");
      }
      if (fileExtension == null)
      {
         throw new NullPointerException("fileExtension is null");
      }
      if (maxAIO <= 0)
      {
         throw new IllegalStateException("maxAIO should aways be a positive number");
      }

      if (compactPercentage < 0 || compactPercentage > 100)
      {
         throw new IllegalArgumentException("Compact Percentage out of range");
      }

      if (compactPercentage == 0)
      {
         this.compactPercentage = 0;
      }
      else
      {
         this.compactPercentage = (float)compactPercentage / 100f;
      }

      this.compactMinFiles = compactMinFiles;

      this.fileSize = fileSize;

      this.minFiles = minFiles;

      this.fileFactory = fileFactory;

      this.filePrefix = filePrefix;

      this.fileExtension = fileExtension;

      this.maxAIO = maxAIO;
   }

   // Public methods (used by package members such as JournalCompactor) (these methods are not part of the JournalImpl
   // interface)

   /**
    * <p>A transaction record (Commit or Prepare), will hold the number of elements the transaction has on each file.</p>
    * <p>For example, a transaction was spread along 3 journal files with 10 pendingTransactions on each file. 
    *    (What could happen if there are too many pendingTransactions, or if an user event delayed pendingTransactions to come in time to a single file).</p>
    * <p>The element-summary will then have</p>
    * <p>FileID1, 10</p>
    * <p>FileID2, 10</p>
    * <p>FileID3, 10</p>
    * 
    * <br>
    * <p> During the load, the transaction needs to have 30 pendingTransactions spread across the files as originally written.</p>
    * <p> If for any reason there are missing pendingTransactions, that means the transaction was not completed and we should ignore the whole transaction </p>
    * <p> We can't just use a global counter as reclaiming could delete files after the transaction was successfully committed. 
    *     That also means not having a whole file on journal-reload doesn't mean we have to invalidate the transaction </p>
    * 
    * @param recordType
    * @param txID
    * @param tx
    * @param transactionData
    * @return
    * @throws Exception
    */
   public static void writeTransaction(final int fileID,
                                       final byte recordType,
                                       final long txID,
                                       final JournalTransaction tx,
                                       final EncodingSupport transactionData,
                                       final int size,
                                       final int numberOfRecords,
                                       final ChannelBuffer bb) throws Exception
   {
      bb.writeByte(recordType);
      bb.writeInt(fileID); // skip ID part
      bb.writeLong(txID);
      bb.writeInt(numberOfRecords); // skip number of pendingTransactions part

      if (transactionData != null)
      {
         bb.writeInt(transactionData.getEncodeSize());
      }

      if (transactionData != null)
      {
         transactionData.encode(bb);
      }

      bb.writeInt(size);
   }

   /**
    * @param txID
    * @param id
    * @param recordType
    * @param record
    * @param size
    * @param bb
    */
   public static void writeUpdateRecordTX(final int fileID,
                                          final long txID,
                                          final long id,
                                          final byte recordType,
                                          final EncodingSupport record,
                                          final int size,
                                          final ChannelBuffer bb)
   {
      bb.writeByte(UPDATE_RECORD_TX);
      bb.writeInt(fileID);
      bb.writeLong(txID);
      bb.writeLong(id);
      bb.writeInt(record.getEncodeSize());
      bb.writeByte(recordType);
      record.encode(bb);
      bb.writeInt(size);
   }

   /**
    * @param id
    * @param recordType
    * @param record
    * @param size
    * @param bb
    */
   public static void writeUpdateRecord(final int fileId,
                                        final long id,
                                        final byte recordType,
                                        final EncodingSupport record,
                                        final int size,
                                        final ChannelBuffer bb)
   {
      bb.writeByte(UPDATE_RECORD);
      bb.writeInt(fileId);
      bb.writeLong(id);
      bb.writeInt(record.getEncodeSize());
      bb.writeByte(recordType);
      record.encode(bb);
      bb.writeInt(size);
   }

   /**
    * @param id
    * @param recordType
    * @param record
    * @param size
    * @param bb
    */
   public static void writeAddRecord(final int fileId,
                                     final long id,
                                     final byte recordType,
                                     final EncodingSupport record,
                                     final int size,
                                     final ChannelBuffer bb)
   {
      bb.writeByte(ADD_RECORD);
      bb.writeInt(fileId);
      bb.writeLong(id);
      bb.writeInt(record.getEncodeSize());
      bb.writeByte(recordType);
      record.encode(bb);
      bb.writeInt(size);
   }

   /**
    * @param txID
    * @param id
    * @param record
    * @param size
    * @param bb
    */
   public static void writeDeleteRecordTransactional(final int fileID,
                                                     final long txID,
                                                     final long id,
                                                     final EncodingSupport record,
                                                     final int size,
                                                     final ChannelBuffer bb)
   {
      bb.writeByte(DELETE_RECORD_TX);
      bb.writeInt(fileID);
      bb.writeLong(txID);
      bb.writeLong(id);
      bb.writeInt(record != null ? record.getEncodeSize() : 0);
      if (record != null)
      {
         record.encode(bb);
      }
      bb.writeInt(size);
   }

   /**
    * @param txID
    * @param id
    * @param recordType
    * @param record
    * @param recordLength
    * @param size
    * @param bb
    */
   public static void writeAddRecordTX(final int fileID,
                                       final long txID,
                                       final long id,
                                       final byte recordType,
                                       final EncodingSupport record,
                                       final int size,
                                       final ChannelBuffer bb)
   {
      bb.writeByte(ADD_RECORD_TX);
      bb.writeInt(fileID);
      bb.writeLong(txID);
      bb.writeLong(id);
      bb.writeInt(record.getEncodeSize());
      bb.writeByte(recordType);
      record.encode(bb);
      bb.writeInt(size);
   }

   public Map<Long, JournalRecord> getRecords()
   {
      return records;
   }

   public JournalFile getCurrentFile()
   {
      return currentFile;
   }

   public JournalCompactor getCompactor()
   {
      return compactor;
   }

   public static int readJournalFile(final SequentialFileFactory fileFactory,
                                     final JournalFile file,
                                     final JournalReaderCallback reader) throws Exception
   {

      file.getFile().open(1);
      ByteBuffer wholeFileBuffer = null;
      try
      {

         final int filesize = (int)file.getFile().size();

         wholeFileBuffer = fileFactory.newBuffer((int)filesize);

         final int journalFileSize = file.getFile().read(wholeFileBuffer);

         if (journalFileSize != filesize)
         {
            throw new RuntimeException("Invalid read! The system couldn't read the entire file into memory");
         }

         // First long is the ordering timestamp, we just jump its position
         wholeFileBuffer.position(SIZE_HEADER);

         int lastDataPos = SIZE_HEADER;

         while (wholeFileBuffer.hasRemaining())
         {
            final int pos = wholeFileBuffer.position();

            byte recordType = wholeFileBuffer.get();

            if (recordType < ADD_RECORD || recordType > ROLLBACK_RECORD)
            {
               // I - We scan for any valid record on the file. If a hole
               // happened on the middle of the file we keep looking until all
               // the possibilities are gone
               continue;
            }

            if (isInvalidSize(journalFileSize, wholeFileBuffer.position(), DataConstants.SIZE_INT))
            {
               reader.markAsDataFile(file);

               wholeFileBuffer.position(pos + 1);
               // II - Ignore this record, lets keep looking
               continue;
            }

            // III - Every record has the file-id.
            // This is what supports us from not re-filling the whole file
            int readFileId = wholeFileBuffer.getInt();

            long transactionID = 0;

            if (isTransaction(recordType))
            {
               if (isInvalidSize(journalFileSize, wholeFileBuffer.position(), DataConstants.SIZE_LONG))
               {
                  wholeFileBuffer.position(pos + 1);
                  reader.markAsDataFile(file);
                  continue;
               }

               transactionID = wholeFileBuffer.getLong();
            }

            long recordID = 0;

            // If prepare or commit
            if (!isCompleteTransaction(recordType))
            {
               if (isInvalidSize(journalFileSize, wholeFileBuffer.position(), DataConstants.SIZE_LONG))
               {
                  wholeFileBuffer.position(pos + 1);
                  reader.markAsDataFile(file);
                  continue;
               }

               recordID = wholeFileBuffer.getLong();
            }

            // We use the size of the record to validate the health of the
            // record.
            // (V) We verify the size of the record

            // The variable record portion used on Updates and Appends
            int variableSize = 0;

            // Used to hold extra data on transaction prepares
            int preparedTransactionExtraDataSize = 0;

            byte userRecordType = 0;

            byte record[] = null;

            if (isContainsBody(recordType))
            {
               if (isInvalidSize(journalFileSize, wholeFileBuffer.position(), DataConstants.SIZE_INT))
               {
                  wholeFileBuffer.position(pos + 1);
                  reader.markAsDataFile(file);
                  continue;
               }

               variableSize = wholeFileBuffer.getInt();

               if (isInvalidSize(journalFileSize, wholeFileBuffer.position(), variableSize))
               {
                  wholeFileBuffer.position(pos + 1);
                  continue;
               }

               if (recordType != DELETE_RECORD_TX)
               {
                  userRecordType = wholeFileBuffer.get();
               }

               record = new byte[variableSize];

               wholeFileBuffer.get(record);
            }

            // Case this is a transaction, this will contain the number of pendingTransactions on a transaction, at the
            // currentFile
            int transactionCheckNumberOfRecords = 0;

            if (recordType == PREPARE_RECORD || recordType == COMMIT_RECORD)
            {
               if (isInvalidSize(journalFileSize, wholeFileBuffer.position(), DataConstants.SIZE_INT))
               {
                  wholeFileBuffer.position(pos + 1);
                  continue;
               }

               transactionCheckNumberOfRecords = wholeFileBuffer.getInt();

               if (recordType == PREPARE_RECORD)
               {
                  // Add the variable size required for preparedTransactions
                  preparedTransactionExtraDataSize = wholeFileBuffer.getInt();
               }
               variableSize = 0;
            }

            int recordSize = getRecordSize(recordType);

            // VI - this is completing V, We will validate the size at the end
            // of the record,
            // But we avoid buffer overflows by damaged data
            if (isInvalidSize(journalFileSize, pos, recordSize + variableSize + preparedTransactionExtraDataSize))
            {
               // Avoid a buffer overflow caused by damaged data... continue
               // scanning for more pendingTransactions...
               trace("Record at position " + pos +
                     " recordType = " +
                     recordType +
                     " file:" +
                     file.getFile().getFileName() +
                     " recordSize: " +
                     recordSize +
                     " variableSize: " +
                     variableSize +
                     " preparedTransactionExtraDataSize: " +
                     preparedTransactionExtraDataSize +
                     " is corrupted and it is being ignored (II)");
               // If a file has damaged pendingTransactions, we make it a dataFile, and the
               // next reclaiming will fix it
               reader.markAsDataFile(file);
               wholeFileBuffer.position(pos + 1);

               continue;
            }

            int oldPos = wholeFileBuffer.position();

            wholeFileBuffer.position(pos + variableSize +
                                     recordSize +
                                     preparedTransactionExtraDataSize -
                                     DataConstants.SIZE_INT);

            int checkSize = wholeFileBuffer.getInt();

            // VII - The checkSize at the end has to match with the size
            // informed at the beggining.
            // This is like testing a hash for the record. (We could replace the
            // checkSize by some sort of calculated hash)
            if (checkSize != variableSize + recordSize + preparedTransactionExtraDataSize)
            {
               trace("Record at position " + pos +
                     " recordType = " +
                     recordType +
                     " file:" +
                     file.getFile().getFileName() +
                     " is corrupted and it is being ignored (III)");

               // If a file has damaged pendingTransactions, we make it a dataFile, and the
               // next reclaiming will fix it
               reader.markAsDataFile(file);

               wholeFileBuffer.position(pos + DataConstants.SIZE_BYTE);

               continue;
            }

            // This record is from a previous file-usage. The file was
            // reused and we need to ignore this record
            if (readFileId != file.getFileID())
            {
               // If a file has damaged pendingTransactions, we make it a dataFile, and the
               // next reclaiming will fix it
               reader.markAsDataFile(file);

               continue;
            }

            wholeFileBuffer.position(oldPos);

            // At this point everything is checked. So we relax and just load
            // the data now.

            switch (recordType)
            {
               case ADD_RECORD:
               {
                  reader.onReadAddRecord(new RecordInfo(recordID, userRecordType, record, false));
                  break;
               }

               case UPDATE_RECORD:
               {
                  reader.onReadUpdateRecord(new RecordInfo(recordID, userRecordType, record, true));
                  break;
               }

               case DELETE_RECORD:
               {
                  reader.onReadDeleteRecord(recordID);
                  break;
               }

               case ADD_RECORD_TX:
               {
                  reader.onReadAddRecordTX(transactionID, new RecordInfo(recordID, userRecordType, record, false));
                  break;
               }

               case UPDATE_RECORD_TX:
               {
                  reader.onReadUpdateRecordTX(transactionID, new RecordInfo(recordID, userRecordType, record, true));
                  break;
               }

               case DELETE_RECORD_TX:
               {
                  reader.onReadDeleteRecordTX(transactionID, new RecordInfo(recordID, (byte)0, record, true));
                  break;
               }

               case PREPARE_RECORD:
               {

                  byte extraData[] = new byte[preparedTransactionExtraDataSize];

                  wholeFileBuffer.get(extraData);

                  reader.onReadPrepareRecord(transactionID, extraData, transactionCheckNumberOfRecords);

                  break;
               }
               case COMMIT_RECORD:
               {

                  reader.onReadCommitRecord(transactionID, transactionCheckNumberOfRecords);
                  break;
               }
               case ROLLBACK_RECORD:
               {
                  reader.onReadRollbackRecord(transactionID);
                  break;
               }
               default:
               {
                  throw new IllegalStateException("Journal " + file.getFile().getFileName() +
                                                  " is corrupt, invalid record type " +
                                                  recordType);
               }
            }

            checkSize = wholeFileBuffer.getInt();

            // This is a sanity check about the loading code itself.
            // If this checkSize doesn't match, it means the reading method is
            // not doing what it was supposed to do
            if (checkSize != variableSize + recordSize + preparedTransactionExtraDataSize)
            {
               throw new IllegalStateException("Internal error on loading file. Position doesn't match with checkSize, file = " + file.getFile() +
                                               ", pos = " +
                                               pos);
            }

            lastDataPos = wholeFileBuffer.position();

         }

         return lastDataPos;
      }

      finally
      {
         if (wholeFileBuffer != null)
         {
            fileFactory.releaseBuffer(wholeFileBuffer);
         }

         try
         {
            file.getFile().close();
         }
         catch (Throwable ignored)
         {
         }
      }
   }

   // Journal implementation
   // ----------------------------------------------------------------

   public void appendAddRecord(final long id, final byte recordType, final byte[] record, final boolean sync) throws Exception
   {
      appendAddRecord(id, recordType, new ByteArrayEncoding(record), sync);
   }

   public void appendAddRecord(final long id, final byte recordType, final EncodingSupport record, final boolean sync) throws Exception
   {
      if (state != STATE_LOADED)
      {
         throw new IllegalStateException("Journal must be loaded first");
      }

      IOCallback callback = null;

      compactingLock.readLock().lock();

      try
      {
         int size = SIZE_ADD_RECORD + record.getEncodeSize();

         ChannelBuffer bb = newBuffer(size);

         writeAddRecord(-1, id, recordType, record, size, bb); // fileID will be filled later

         callback = getSyncCallback(sync);

         lock.acquire();
         try
         {
            JournalFile usedFile = appendRecord(bb, false, sync, null, callback);

            records.put(id, new JournalRecord(usedFile, size));
         }
         finally
         {
            lock.release();
         }
      }
      finally
      {
         compactingLock.readLock().unlock();
      }

      if (callback != null)
      {
         callback.waitCompletion();
      }
   }

   public void appendUpdateRecord(final long id, final byte recordType, final byte[] record, final boolean sync) throws Exception
   {
      appendUpdateRecord(id, recordType, new ByteArrayEncoding(record), sync);
   }

   public void appendUpdateRecord(final long id, final byte recordType, final EncodingSupport record, final boolean sync) throws Exception
   {
      if (state != STATE_LOADED)
      {
         throw new IllegalStateException("Journal must be loaded first");
      }

      IOCallback callback = null;

      compactingLock.readLock().lock();

      try
      {

         JournalRecord posFiles = records.get(id);

         if (posFiles == null)
         {
            if (!(compactor != null && compactor.lookupRecord(id)))
            {
               throw new IllegalStateException("Cannot find add info " + id);
            }
         }

         int size = SIZE_UPDATE_RECORD + record.getEncodeSize();

         ChannelBuffer bb = newBuffer(size);

         writeUpdateRecord(-1, id, recordType, record, size, bb);

         callback = getSyncCallback(sync);

         lock.acquire();
         try
         {
            JournalFile usedFile = appendRecord(bb, false, sync, null, callback);

            // record== null here could only mean there is a compactor, and computing the delete should be done after
            // compacting is done
            if (posFiles == null)
            {
               compactor.addCommandUpdate(id, usedFile, size);
            }
            else
            {
               posFiles.addUpdateFile(usedFile, size);
            }
         }
         finally
         {
            lock.release();
         }
      }
      finally
      {
         compactingLock.readLock().unlock();
      }

      if (callback != null)
      {
         callback.waitCompletion();
      }
   }

   public void appendDeleteRecord(final long id, final boolean sync) throws Exception
   {
      if (state != STATE_LOADED)
      {
         throw new IllegalStateException("Journal must be loaded first");
      }

      compactingLock.readLock().lock();

      IOCallback callback = null;

      try
      {

         JournalRecord record = records.remove(id);

         if (record == null)
         {
            if (!(compactor != null && compactor.lookupRecord(id)))
            {
               throw new IllegalStateException("Cannot find add info " + id);
            }
         }

         int size = SIZE_DELETE_RECORD;

         ChannelBuffer bb = newBuffer(size);

         bb.writeByte(DELETE_RECORD);
         bb.writeInt(-1); // skip ID part
         bb.writeLong(id);
         bb.writeInt(size);

         callback = getSyncCallback(sync);

         lock.acquire();
         try
         {
            JournalFile usedFile = appendRecord(bb, false, sync, null, callback);

            // record== null here could only mean there is a compactor, and computing the delete should be done after
            // compacting is done
            if (record == null)
            {
               compactor.addCommandDelete(id, usedFile);
            }
            else
            {
               record.delete(usedFile);
            }

         }
         finally
         {
            lock.release();
         }
      }
      finally
      {
         compactingLock.readLock().unlock();
      }

      if (callback != null)
      {
         callback.waitCompletion();
      }
   }

   public void appendAddRecordTransactional(final long txID, final long id, final byte recordType, final byte[] record) throws Exception
   {
      appendAddRecordTransactional(txID, id, recordType, new ByteArrayEncoding(record));

   }

   public void appendAddRecordTransactional(final long txID,
                                            final long id,
                                            final byte recordType,
                                            final EncodingSupport record) throws Exception
   {
      if (state != STATE_LOADED)
      {
         throw new IllegalStateException("Journal must be loaded first");
      }

      compactingLock.readLock().lock();

      try
      {

         int size = SIZE_ADD_RECORD_TX + record.getEncodeSize();

         ChannelBuffer bb = newBuffer(size);

         writeAddRecordTX(-1, txID, id, recordType, record, size, bb);

         JournalTransaction tx = getTransactionInfo(txID);

         lock.acquire();
         try
         {
            JournalFile usedFile = appendRecord(bb, false, false, tx, null);

            tx.addPositive(usedFile, id, size);
         }
         finally
         {
            lock.release();
         }
      }
      finally
      {
         compactingLock.readLock().unlock();
      }
   }

   public void appendUpdateRecordTransactional(final long txID,
                                               final long id,
                                               final byte recordType,
                                               final byte[] record) throws Exception
   {
      appendUpdateRecordTransactional(txID, id, recordType, new ByteArrayEncoding(record));
   }

   public void appendUpdateRecordTransactional(final long txID,
                                               final long id,
                                               final byte recordType,
                                               final EncodingSupport record) throws Exception
   {
      if (state != STATE_LOADED)
      {
         throw new IllegalStateException("Journal must be loaded first");
      }

      compactingLock.readLock().lock();

      try
      {

         int size = SIZE_UPDATE_RECORD_TX + record.getEncodeSize();

         ChannelBuffer bb = newBuffer(size);

         writeUpdateRecordTX(-1, txID, id, recordType, record, size, bb);

         JournalTransaction tx = getTransactionInfo(txID);

         lock.acquire();
         try
         {
            JournalFile usedFile = appendRecord(bb, false, false, tx, null);

            tx.addPositive(usedFile, id, size);
         }
         finally
         {
            lock.release();
         }
      }
      finally
      {
         compactingLock.readLock().unlock();
      }
   }

   public void appendDeleteRecordTransactional(final long txID, final long id, final byte[] record) throws Exception
   {
      appendDeleteRecordTransactional(txID, id, new ByteArrayEncoding(record));
   }

   public void appendDeleteRecordTransactional(final long txID, final long id, final EncodingSupport record) throws Exception
   {
      if (state != STATE_LOADED)
      {
         throw new IllegalStateException("Journal must be loaded first");
      }

      compactingLock.readLock().lock();

      try
      {
         int size = SIZE_DELETE_RECORD_TX + record.getEncodeSize();

         ChannelBuffer bb = newBuffer(size);

         writeDeleteRecordTransactional(-1, txID, id, record, size, bb);

         JournalTransaction tx = getTransactionInfo(txID);

         lock.acquire();
         try
         {
            JournalFile usedFile = appendRecord(bb, false, false, tx, null);

            tx.addNegative(usedFile, id);
         }
         finally
         {
            lock.release();
         }
      }
      finally
      {
         compactingLock.readLock().unlock();
      }
   }

   public void appendDeleteRecordTransactional(final long txID, final long id) throws Exception
   {
      appendDeleteRecordTransactional(txID, id, NullEncoding.instance);
   }

   /** 
    * 
    * <p>If the system crashed after a prepare was called, it should store information that is required to bring the transaction 
    *     back to a state it could be committed. </p>
    * 
    * <p> transactionData allows you to store any other supporting user-data related to the transaction</p>
    * 
    * <p> This method also uses the same logic applied on {@link JournalImpl#appendCommitRecord(long)}
    * 
    * @param txID
    * @param transactionData - extra user data for the prepare
    * @throws Exception
    */
   public void appendPrepareRecord(final long txID, final EncodingSupport transactionData, final boolean sync) throws Exception
   {
      if (state != STATE_LOADED)
      {
         throw new IllegalStateException("Journal must be loaded first");
      }

      JournalTransaction tx = getTransactionInfo(txID);

      if (sync)
      {
         tx.syncPreviousFiles(fileFactory.isSupportsCallbacks(), currentFile);
      }

      compactingLock.readLock().lock();

      try
      {

         int size = SIZE_COMPLETE_TRANSACTION_RECORD + transactionData.getEncodeSize() + DataConstants.SIZE_INT;
         ChannelBuffer bb = newBuffer(size);

         writeTransaction(-1, PREPARE_RECORD, txID, tx, transactionData, size, -1, bb);

         lock.acquire();
         try
         {
            JournalFile usedFile = appendRecord(bb, true, sync, tx, null);

            tx.prepare(usedFile);
         }
         finally
         {
            lock.release();
         }

      }
      finally
      {
         compactingLock.readLock().unlock();
      }

      // We should wait this outside of the lock, to increase throughput
      tx.waitCompletion();
   }

   /**
    * <p>A transaction record (Commit or Prepare), will hold the number of elements the transaction has on each file.</p>
    * <p>For example, a transaction was spread along 3 journal files with 10 pendingTransactions on each file. 
    *    (What could happen if there are too many pendingTransactions, or if an user event delayed pendingTransactions to come in time to a single file).</p>
    * <p>The element-summary will then have</p>
    * <p>FileID1, 10</p>
    * <p>FileID2, 10</p>
    * <p>FileID3, 10</p>
    * 
    * <br>
    * <p> During the load, the transaction needs to have 30 pendingTransactions spread across the files as originally written.</p>
    * <p> If for any reason there are missing pendingTransactions, that means the transaction was not completed and we should ignore the whole transaction </p>
    * <p> We can't just use a global counter as reclaiming could delete files after the transaction was successfully committed. 
    *     That also means not having a whole file on journal-reload doesn't mean we have to invalidate the transaction </p>
    *
    * @see JournalImpl#writeTransaction(byte, long, org.jboss.messaging.core.journal.impl.JournalImpl.JournalTransaction, EncodingSupport)
    */
   public void appendCommitRecord(final long txID, final boolean sync) throws Exception
   {
      if (state != STATE_LOADED)
      {
         throw new IllegalStateException("Journal must be loaded first");
      }

      JournalTransaction tx = transactions.remove(txID);

      compactingLock.readLock().lock();

      try
      {

         if (tx == null)
         {
            throw new IllegalStateException("Cannot find tx with id " + txID);
         }

         ChannelBuffer bb = newBuffer(SIZE_COMPLETE_TRANSACTION_RECORD);

         writeTransaction(-1, COMMIT_RECORD, txID, tx, null, SIZE_COMPLETE_TRANSACTION_RECORD, -1, bb);

         lock.acquire();
         try
         {
            JournalFile usedFile = appendRecord(bb, true, sync, tx, null);

            tx.commit(usedFile);
         }
         finally
         {
            lock.release();
         }

      }
      finally
      {
         compactingLock.readLock().unlock();
      }

      if (sync)
      {
         // We should wait this outside of the lock, to increase throuput
         tx.waitCompletion();
      }
   }

   public void appendRollbackRecord(final long txID, final boolean sync) throws Exception
   {
      if (state != STATE_LOADED)
      {
         throw new IllegalStateException("Journal must be loaded first");
      }

      compactingLock.readLock().lock();

      JournalTransaction tx = null;

      try
      {
         tx = transactions.remove(txID);

         if (tx == null)
         {
            throw new IllegalStateException("Cannot find tx with id " + txID);
         }

         int size = SIZE_ROLLBACK_RECORD;

         ChannelBuffer bb = newBuffer(size);

         bb.writeByte(ROLLBACK_RECORD);
         bb.writeInt(-1); // skip ID part
         bb.writeLong(txID);
         bb.writeInt(size);

         lock.acquire();
         try
         {
            JournalFile usedFile = appendRecord(bb, false, sync, tx, null);

            tx.rollback(usedFile);
         }
         finally
         {
            lock.release();
         }

      }
      finally
      {
         compactingLock.readLock().unlock();
      }

      // We should wait this outside of the lock, to increase throuput

      if (sync)
      {
         tx.waitCompletion();
      }

   }

   public int getAlignment() throws Exception
   {
      return fileFactory.getAlignment();
   }

   /**
    * @see JournalImpl#load(LoaderCallback)
    */
   public synchronized long load(final List<RecordInfo> committedRecords,
                                 final List<PreparedTransactionInfo> preparedTransactions) throws Exception
   {
      final Set<Long> recordsToDelete = new HashSet<Long>();
      final List<RecordInfo> records = new ArrayList<RecordInfo>();

      long maxID = load(new LoaderCallback()
      {
         public void addPreparedTransaction(final PreparedTransactionInfo preparedTransaction)
         {
            preparedTransactions.add(preparedTransaction);
         }

         public void addRecord(final RecordInfo info)
         {
            records.add(info);
         }

         public void updateRecord(final RecordInfo info)
         {
            records.add(info);
         }

         public void deleteRecord(final long id)
         {
            recordsToDelete.add(id);
         }
      });

      for (RecordInfo record : records)
      {
         if (!recordsToDelete.contains(record.id))
         {
            committedRecords.add(record);
         }
      }

      return maxID;
   }

   /**
    * 
    *  Note: This method can't be called from the main executor, as it will invoke other methods depending on it.
    */
   public void compact() throws Exception
   {
      if (compactor != null)
      {
         throw new IllegalStateException("There is pending compacting operation");
      }

      ArrayList<JournalFile> dataFilesToProcess = new ArrayList<JournalFile>(dataFiles.size());

      boolean previousReclaimValue = autoReclaim;

      try
      {

         // We need to guarantee that the journal is frozen for this short time
         // We don't freeze the journal as we compact, only for the short time where we replace records
         compactingLock.writeLock().lock();
         try
         {
            autoReclaim = false;

            // We need to move to the next file, as we need a clear start for negatives and positives counts
            moveNextFile(true);

            // Take the snapshots and replace the structures

            dataFilesToProcess.addAll(dataFiles);

            for (JournalFile file : pendingCloseFiles)
            {
               file.getFile().close();
            }

            dataFilesToProcess.addAll(pendingCloseFiles);
            pendingCloseFiles.clear();

            dataFiles.clear();

            if (dataFilesToProcess.size() == 0)
            {
               return;
            }

            compactor = new JournalCompactor(fileFactory, this, records.keySet(), dataFilesToProcess.get(0).getFileID());

            for (Map.Entry<Long, JournalTransaction> entry : transactions.entrySet())
            {
               compactor.addPendingTransaction(entry.getKey(), entry.getValue().getPositiveArray());
               entry.getValue().setCompacting();
            }

            // We will calculate the new records during compacting, what will take the position the records will take
            // after compacting
            records.clear();
         }
         finally
         {
            compactingLock.writeLock().unlock();
         }

         Collections.sort(dataFilesToProcess, new JournalFileComparator());

         // Read the files, and use the JournalCompactor class to create the new outputFiles, and the new collections as
         // well
         for (final JournalFile file : dataFilesToProcess)
         {
            log.info("Compacting file " + file.getFile().getFileName() + ", internalID = " + file.getFileID());
            readJournalFile(fileFactory, file, compactor);
         }

         compactor.flush();

         // pointcut for tests
         // We need to test concurrent updates on the journal, as the compacting is being performed.
         // Usually tests will use this to hold the compacting while other structures are being updated.
         onCompactDone();

         SequentialFile controlFile = createControlFile(dataFilesToProcess, compactor.getNewDataFiles());

         List<JournalFile> newDatafiles = null;

         JournalCompactor localCompactor = compactor;

         compactingLock.writeLock().lock();
         try
         {
            // Need to clear the compactor here, or the replay commands will send commands back (infinite loop)
            compactor = null;

            newDatafiles = localCompactor.getNewDataFiles();

            // Restore newRecords created during compacting
            for (Map.Entry<Long, JournalRecord> newRecordEntry : localCompactor.getNewRecords().entrySet())
            {
               records.put(newRecordEntry.getKey(), newRecordEntry.getValue());
            }

            // Restore compacted dataFiles
            for (int i = newDatafiles.size() - 1; i >= 0; i--)
            {
               JournalFile fileToAdd = newDatafiles.get(i);
               if (trace)
               {
                  trace("Adding file " + fileToAdd + " back as datafile");
               }
               dataFiles.addFirst(fileToAdd);
            }

            trace("There are " + dataFiles.size() + " datafiles Now");

            // Replay pending commands (including updates, deletes and commits)

            localCompactor.replayPendingCommands();

            // Merge transactions back after compacting
            // This has to be done after the replay pending commands, as we need to delete committs that happened during
            // the compacting

            for (JournalTransaction newTransaction : localCompactor.getNewTransactions().values())
            {
               if (trace)
               {
                  trace("Merging pending transaction " + newTransaction + " after compacting to the journal");
               }
               JournalTransaction liveTransaction = transactions.get(newTransaction.getId());
               if (liveTransaction == null)
               {
                  log.warn("Inconsistency: Can't merge transaction " + newTransaction.getId() +
                           " back into JournalTransactions");
               }
               else
               {
                  liveTransaction.merge(newTransaction);
               }
            }
         }
         finally
         {
            compactingLock.writeLock().unlock();
         }

         // At this point the journal is unlocked. We keep renaming files while the journal is already operational
         renameFiles(dataFilesToProcess, newDatafiles);
         deleteControlFile(controlFile);

      }
      finally
      {
         // An Exception was probably thrown, and the compactor was not cleared
         if (compactor != null)
         {
            try
            {
               compactor.flush();
            }
            catch (Throwable ignored)
            {
            }

            compactor = null;
         }
         autoReclaim = previousReclaimValue;
      }

   }

   /** 
    * <p>Load data accordingly to the record layouts</p>
    * 
    * <p>Basic record layout:</p>
    * <table border=1>
    *   <tr><td><b>Field Name</b></td><td><b>Size</b></td></tr>
    *   <tr><td>RecordType</td><td>Byte (1)</td></tr>
    *   <tr><td>FileID</td><td>Integer (4 bytes)</td></tr>
    *   <tr><td>TransactionID <i>(if record is transactional)</i></td><td>Long (8 bytes)</td></tr>
    *   <tr><td>RecordID</td><td>Long (8 bytes)</td></tr>
    *   <tr><td>BodySize(Add, update and delete)</td><td>Integer (4 bytes)</td></tr>
    *   <tr><td>UserDefinedRecordType (If add/update only)</td><td>Byte (1)</td</tr>
    *   <tr><td>RecordBody</td><td>Byte Array (size=BodySize)</td></tr>
    *   <tr><td>Check Size</td><td>Integer (4 bytes)</td></tr>
    * </table>
    * 
    * <p> The check-size is used to validate if the record is valid and complete </p>
    * 
    * <p>Commit/Prepare record layout:</p>
    * <table border=1>
    *   <tr><td><b>Field Name</b></td><td><b>Size</b></td></tr>
    *   <tr><td>RecordType</td><td>Byte (1)</td></tr>
    *   <tr><td>FileID</td><td>Integer (4 bytes)</td></tr>
    *   <tr><td>TransactionID <i>(if record is transactional)</i></td><td>Long (8 bytes)</td></tr>
    *   <tr><td>ExtraDataLength (Prepares only)</td><td>Integer (4 bytes)</td></tr>
    *   <tr><td>Number Of Files (N)</td><td>Integer (4 bytes)</td></tr>
    *   <tr><td>ExtraDataBytes</td><td>Bytes (sized by ExtraDataLength)</td></tr>
    *   <tr><td>* FileID(n)</td><td>Integer (4 bytes)</td></tr>
    *   <tr><td>* NumberOfElements(n)</td><td>Integer (4 bytes)</td></tr>
    *   <tr><td>CheckSize</td><td>Integer (4 bytes)</td</tr>
    * </table>
    * 
    * <p> * FileID and NumberOfElements are the transaction summary, and they will be repeated (N)umberOfFiles times </p> 
    * 
    * */
   public synchronized long load(final LoaderCallback loadManager) throws Exception
   {
      if (state != STATE_STARTED)
      {
         throw new IllegalStateException("Journal must be in started state");
      }

      checkControlFile();

      final Map<Long, TransactionHolder> loadTransactions = new LinkedHashMap<Long, TransactionHolder>();

      final List<JournalFile> orderedFiles = orderFiles();

      int lastDataPos = SIZE_HEADER;

      long maxID = -1;

      for (final JournalFile file : orderedFiles)
      {
         trace("Loading file " + file.getFile().getFileName());

         final AtomicBoolean hasData = new AtomicBoolean(false);

         int resultLastPost = readJournalFile(fileFactory, file, new JournalReaderCallback()
         {

            public void onReadAddRecord(final RecordInfo info) throws Exception
            {
               if (trace && LOAD_TRACE)
               {
                  trace("AddRecord: " + info);
               }
               hasData.set(true);

               loadManager.addRecord(info);

               records.put(info.id, new JournalRecord(file, info.data.length + SIZE_ADD_RECORD));
            }

            public void onReadUpdateRecord(final RecordInfo info) throws Exception
            {
               if (trace && LOAD_TRACE)
               {
                  trace("UpdateRecord: " + info);
               }
               hasData.set(true);

               loadManager.updateRecord(info);

               JournalRecord posFiles = records.get(info.id);

               if (posFiles != null)
               {
                  // It's legal for this to be null. The file(s) with the may
                  // have been deleted
                  // just leaving some updates in this file

                  posFiles.addUpdateFile(file, info.data.length + SIZE_UPDATE_RECORD);
               }
            }

            public void onReadDeleteRecord(final long recordID) throws Exception
            {
               if (trace && LOAD_TRACE)
               {
                  trace("DeleteRecord: " + recordID);
               }
               hasData.set(true);

               loadManager.deleteRecord(recordID);

               JournalRecord posFiles = records.remove(recordID);

               if (posFiles != null)
               {
                  posFiles.delete(file);
               }
            }

            public void onReadUpdateRecordTX(final long transactionID, final RecordInfo info) throws Exception
            {
               onReadAddRecordTX(transactionID, info);
            }

            public void onReadAddRecordTX(final long transactionID, final RecordInfo info) throws Exception
            {

               if (trace && LOAD_TRACE)
               {
                  trace((info.isUpdate ? "updateRecordTX: " : "addRecordTX: ") + info + ", txid = " + transactionID);
               }

               hasData.set(true);

               TransactionHolder tx = loadTransactions.get(transactionID);

               if (tx == null)
               {
                  tx = new TransactionHolder(transactionID);

                  loadTransactions.put(transactionID, tx);
               }

               tx.recordInfos.add(info);

               JournalTransaction tnp = transactions.get(transactionID);

               if (tnp == null)
               {
                  tnp = new JournalTransaction(info.id, JournalImpl.this);

                  transactions.put(transactionID, tnp);
               }

               tnp.addPositive(file, info.id, info.data.length + SIZE_ADD_RECORD_TX);
            }

            public void onReadDeleteRecordTX(final long transactionID, final RecordInfo info) throws Exception
            {
               if (trace && LOAD_TRACE)
               {
                  trace("DeleteRecordTX: " + transactionID + " info = " + info);
               }

               hasData.set(true);

               TransactionHolder tx = loadTransactions.get(transactionID);

               if (tx == null)
               {
                  tx = new TransactionHolder(transactionID);

                  loadTransactions.put(transactionID, tx);
               }

               tx.recordsToDelete.add(info);

               JournalTransaction tnp = transactions.get(transactionID);

               if (tnp == null)
               {
                  tnp = new JournalTransaction(transactionID, JournalImpl.this);

                  transactions.put(transactionID, tnp);
               }

               tnp.addNegative(file, info.id);

            }

            public void onReadPrepareRecord(final long transactionID, final byte[] extraData, final int numberOfRecords) throws Exception
            {
               if (trace && LOAD_TRACE)
               {
                  trace("prepareRecordTX: txid = " + transactionID);
               }

               hasData.set(true);

               TransactionHolder tx = loadTransactions.get(transactionID);

               if (tx == null)
               {
                  // The user could choose to prepare empty transactions
                  tx = new TransactionHolder(transactionID);

                  loadTransactions.put(transactionID, tx);
               }

               tx.prepared = true;

               tx.extraData = extraData;

               JournalTransaction journalTransaction = transactions.get(transactionID);

               if (journalTransaction == null)
               {
                  journalTransaction = new JournalTransaction(transactionID, JournalImpl.this);

                  transactions.put(transactionID, journalTransaction);
               }

               boolean healthy = checkTransactionHealth(file, journalTransaction, orderedFiles, numberOfRecords);

               if (healthy)
               {
                  journalTransaction.prepare(file);
               }
               else
               {
                  log.warn("Prepared transaction " + transactionID + " wasn't considered completed, it will be ignored");
                  tx.invalid = true;
               }
            }

            public void onReadCommitRecord(final long transactionID, final int numberOfRecords) throws Exception
            {
               if (trace && LOAD_TRACE)
               {
                  trace("commitRecord: txid = " + transactionID);
               }

               TransactionHolder tx = loadTransactions.remove(transactionID);

               // The commit could be alone on its own journal-file and the
               // whole transaction body was reclaimed but not the
               // commit-record
               // So it is completely legal to not find a transaction at this
               // point
               // If we can't find it, we assume the TX was reclaimed and we
               // ignore this
               if (tx != null)
               {
                  JournalTransaction journalTransaction = transactions.remove(transactionID);

                  if (journalTransaction == null)
                  {
                     throw new IllegalStateException("Cannot find tx " + transactionID);
                  }

                  boolean healthy = checkTransactionHealth(file, journalTransaction, orderedFiles, numberOfRecords);

                  if (healthy)
                  {
                     for (RecordInfo txRecord : tx.recordInfos)
                     {
                        if (txRecord.isUpdate)
                        {
                           loadManager.updateRecord(txRecord);
                        }
                        else
                        {
                           loadManager.addRecord(txRecord);
                        }
                     }

                     for (RecordInfo deleteValue : tx.recordsToDelete)
                     {
                        loadManager.deleteRecord(deleteValue.id);
                     }

                     journalTransaction.commit(file);
                  }
                  else
                  {
                     log.warn("Transaction " + transactionID +
                              " is missing elements so the transaction is being ignored");

                     journalTransaction.forget();
                  }

                  hasData.set(true);
               }

            }

            public void onReadRollbackRecord(final long transactionID) throws Exception
            {
               if (trace && LOAD_TRACE)
               {
                  trace("rollbackRecord: txid = " + transactionID);
               }

               TransactionHolder tx = loadTransactions.remove(transactionID);

               // The rollback could be alone on its own journal-file and the
               // whole transaction body was reclaimed but the commit-record
               // So it is completely legal to not find a transaction at this
               // point
               if (tx != null)
               {
                  JournalTransaction tnp = transactions.remove(transactionID);

                  if (tnp == null)
                  {
                     throw new IllegalStateException("Cannot find tx " + transactionID);
                  }

                  // There is no need to validate summaries/holes on
                  // Rollbacks.. We will ignore the data anyway.
                  tnp.rollback(file);

                  hasData.set(true);
               }
            }

            public void markAsDataFile(final JournalFile file)
            {
               if (trace && LOAD_TRACE)
               {
                  trace("Marking " + file + " as data file");
               }

               hasData.set(true);
            }

         });

         if (hasData.get())
         {
            lastDataPos = resultLastPost;
            dataFiles.add(file);
         }
         else
         {
            // Empty dataFiles with no data
            freeFiles.add(file);
         }
      }

      // Create any more files we need

      // FIXME - size() involves a scan
      int filesToCreate = minFiles - (dataFiles.size() + freeFiles.size());

      if (filesToCreate > 0)
      {
         for (int i = 0; i < filesToCreate; i++)
         {
            // Keeping all files opened can be very costly (mainly on AIO)
            freeFiles.add(createFile(false, false, true));
         }
      }

      // The current file is the last one

      Iterator<JournalFile> iter = dataFiles.iterator();

      while (iter.hasNext())
      {
         currentFile = iter.next();

         if (!iter.hasNext())
         {
            iter.remove();
         }
      }

      if (currentFile != null)
      {
         currentFile.getFile().open();

         currentFile.getFile().position(currentFile.getFile().calculateBlockStart(lastDataPos));
      }
      else
      {
         currentFile = freeFiles.remove();

         openFile(currentFile, true);
      }

      fileFactory.activate(currentFile.getFile());

      pushOpenedFile();

      for (TransactionHolder transaction : loadTransactions.values())
      {
         if (!transaction.prepared || transaction.invalid)
         {
            log.warn("Uncommitted transaction with id " + transaction.transactionID + " found and discarded");

            JournalTransaction transactionInfo = transactions.get(transaction.transactionID);

            if (transactionInfo == null)
            {
               throw new IllegalStateException("Cannot find tx " + transaction.transactionID);
            }

            // Reverse the refs
            transactionInfo.forget();

            // Remove the transactionInfo
            transactions.remove(transaction.transactionID);
         }
         else
         {
            PreparedTransactionInfo info = new PreparedTransactionInfo(transaction.transactionID, transaction.extraData);

            info.records.addAll(transaction.recordInfos);

            info.recordsToDelete.addAll(transaction.recordsToDelete);

            loadManager.addPreparedTransaction(info);
         }
      }

      state = STATE_LOADED;

      checkAndReclaimFiles();

      return maxID;
   }

   public void checkAndReclaimFiles() throws Exception
   {
      // We can't start compacting while compacting is working
      compactingLock.readLock().lock();
      try
      {
         checkReclaimStatus();

         for (JournalFile file : dataFiles)
         {
            if (file.isCanReclaim())
            {
               // File can be reclaimed or deleted

               if (trace)
               {
                  trace("Reclaiming file " + file);
               }

               if (!dataFiles.remove(file))
               {
                  log.warn("Could not remove file " + file);
               }

               addFreeFile(file);
            }
         }
      }
      finally
      {
         compactingLock.readLock().unlock();
      }
   }

   public void checkCompact() throws Exception
   {
      if (compactMinFiles == 0)
      {
         // compacting is disabled
         return;
      }

      JournalFile[] dataFiles = getDataFiles();

      long totalLiveSize = 0;

      for (JournalFile file : dataFiles)
      {
         totalLiveSize += file.getLiveSize();
      }

      long totalBytes = (long)dataFiles.length * (long)fileSize;

      long compactMargin = (long)(totalBytes * compactPercentage);

      if (totalLiveSize < compactMargin && compactorWait.getCount() == 0 && dataFiles.length > compactMinFiles)
      {

         log.info("Compacting being started, numberOfDataFiles = " + dataFiles.length +
                  ", liveSize = " +
                  totalLiveSize +
                  ", margin to start compacting = " +
                  compactMargin);

         compactorWait.waitCompletion();

         compactorWait.up();

         // We can't use the executor for the compacting... or we would lock files opening and creation (besides other
         // operations)
         // that would freeze the journal while compacting
         Thread t = new Thread()
         {
            public void run()
            {

               try
               {
                  JournalImpl.this.compact();
               }
               catch (Exception e)
               {
                  log.error(e.getMessage(), e);
               }
               finally
               {
                  compactorWait.down();
               }
            }
         };

         t.start();
      }
   }

   // TestableJournal implementation
   // --------------------------------------------------------------

   public void setAutoReclaim(final boolean autoReclaim)
   {
      this.autoReclaim = autoReclaim;
   }

   public boolean isAutoReclaim()
   {
      return autoReclaim;
   }

   public String debug() throws Exception
   {
      checkReclaimStatus();

      StringBuilder builder = new StringBuilder();

      for (JournalFile file : dataFiles)
      {
         builder.append("DataFile:" + file +
                        " posCounter = " +
                        file.getPosCount() +
                        " reclaimStatus = " +
                        file.isCanReclaim() +
                        " live size = " +
                        file.getLiveSize() +
                        "\n");
         if (file instanceof JournalFileImpl)
         {
            builder.append(((JournalFileImpl)file).debug());

         }
      }

      for (JournalFile file : freeFiles)
      {
         builder.append("FreeFile:" + file + "\n");
      }

      if (currentFile != null)
      {
         builder.append("CurrentFile:" + currentFile + " posCounter = " + currentFile.getPosCount() + "\n");

         if (currentFile instanceof JournalFileImpl)
         {
            builder.append(((JournalFileImpl)currentFile).debug());
         }
      }
      else
      {
         builder.append("CurrentFile: No current file at this point!");
      }

      builder.append("#Opened Files:" + openedFiles.size());

      return builder.toString();
   }

   /** Method for use on testcases.
    *  It will call waitComplete on every transaction, so any assertions on the file system will be correct after this */
   public void debugWait() throws Exception
   {
      compactorWait.waitCompletion();

      fileFactory.testFlush();

      for (JournalTransaction tx : transactions.values())
      {
         tx.waitCallbacks();
      }

      if (filesExecutor != null && !filesExecutor.isShutdown())
      {
         // Send something to the closingExecutor, just to make sure we went
         // until its end
         final CountDownLatch latch = new CountDownLatch(1);

         filesExecutor.execute(new Runnable()
         {
            public void run()
            {
               latch.countDown();
            }
         });

         latch.await();
      }

   }

   public int getDataFilesCount()
   {
      return dataFiles.size();
   }

   public JournalFile[] getDataFiles()
   {
      return (JournalFile[])dataFiles.toArray(new JournalFile[dataFiles.size()]);
   }

   public int getFreeFilesCount()
   {
      return freeFiles.size();
   }

   public int getOpenedFilesCount()
   {
      return openedFiles.size();
   }

   public int getIDMapSize()
   {
      return records.size();
   }

   public int getFileSize()
   {
      return fileSize;
   }

   public int getMinFiles()
   {
      return minFiles;
   }

   public String getFilePrefix()
   {
      return filePrefix;
   }

   public String getFileExtension()
   {
      return fileExtension;
   }

   public int getMaxAIO()
   {
      return maxAIO;
   }

   // In some tests we need to force the journal to move to a next file
   public void forceMoveNextFile() throws Exception
   {
      lock.acquire();
      try
      {
         moveNextFile(true);
         debugWait();
      }
      finally
      {
         lock.release();
      }
   }

   public void perfBlast(final int pages) throws Exception
   {
      new PerfBlast(pages).start();
   }

   // MessagingComponent implementation
   // ---------------------------------------------------

   public synchronized boolean isStarted()
   {
      return state != STATE_STOPPED;
   }

   public synchronized void start()
   {
      if (state != STATE_STOPPED)
      {
         throw new IllegalStateException("Journal is not stopped");
      }

      filesExecutor = Executors.newSingleThreadExecutor();

      fileFactory.start();

      state = STATE_STARTED;
   }

   public synchronized void stop() throws Exception
   {
      trace("Stopping the journal");

      if (state == STATE_STOPPED)
      {
         throw new IllegalStateException("Journal is already stopped");
      }

      lock.acquire();

      try
      {
         while (!compactorWait.waitCompletion(60000))
         {
            log.warn("Waiting the compactor to finish its operations");
         }

         filesExecutor.shutdown();

         if (!filesExecutor.awaitTermination(60, TimeUnit.SECONDS))
         {
            log.warn("Couldn't stop journal executor after 60 seconds");
         }

         fileFactory.stop();

         if (currentFile != null && currentFile.getFile().isOpen())
         {
            currentFile.getFile().close();
         }

         for (JournalFile file : openedFiles)
         {
            file.getFile().close();
         }

         currentFile = null;

         dataFiles.clear();

         freeFiles.clear();

         openedFiles.clear();

         state = STATE_STOPPED;
      }
      finally
      {
         lock.release();
      }
   }

   // Public
   // -----------------------------------------------------------------------------

   // Protected
   // -----------------------------------------------------------------------------

   protected void deleteControlFile(final SequentialFile controlFile) throws Exception
   {
      controlFile.delete();
   }

   /** being protected as testcases can override this method */
   protected void renameFiles(final List<JournalFile> oldFiles, final List<JournalFile> newFiles) throws Exception
   {
      for (JournalFile file : oldFiles)
      {
         addFreeFile(file);
      }

      for (JournalFile file : newFiles)
      {
         String newName = file.getFile().getFileName();
         newName = newName.substring(0, newName.lastIndexOf(".cmp"));
         file.getFile().renameTo(newName);
      }

   }

   /** This is an interception point for testcases, when the compacted files are written, before replacing the data structures */
   protected void onCompactDone()
   {
   }

   /**
    * @throws Exception
    */
   protected SequentialFile createControlFile(final List<JournalFile> files, final List<JournalFile> newFiles) throws Exception
   {
      return JournalCompactor.writeControlFile(fileFactory, files, newFiles);
   }

   // Private
   // -----------------------------------------------------------------------------

   /**
    * @param file
    * @throws Exception
    */
   private void addFreeFile(JournalFile file) throws Exception
   {
      // FIXME - size() involves a scan!!!
      if (freeFiles.size() + dataFiles.size() + 1 + openedFiles.size() < minFiles)
      {
         // Re-initialise it

         JournalFile jf = reinitializeFile(file);

         freeFiles.add(jf);
      }
      else
      {
         file.getFile().open(1);

         file.getFile().delete();
      }
   }

   private void checkReclaimStatus() throws Exception
   {
      reclaimer.scan(getDataFiles());
   }

   // Discard the old JournalFile and set it with a new ID
   private JournalFile reinitializeFile(final JournalFile file) throws Exception
   {
      int newFileID = generateFileID();

      SequentialFile sf = file.getFile();

      sf.open(1);

      sf.position(0);

      ByteBuffer bb = fileFactory.newBuffer(SIZE_HEADER);

      bb.putInt(newFileID);

      bb.rewind();

      sf.write(bb, true);

      JournalFile jf = new JournalFileImpl(sf, newFileID);

      sf.position(bb.limit());

      sf.close();

      return jf;
   }

   /**
    * <p> Check for holes on the transaction (a commit written but with an incomplete transaction) </p>
    * <p>This method will validate if the transaction (PREPARE/COMMIT) is complete as stated on the COMMIT-RECORD.</p>
    *     
    * <p>Look at the javadoc on {@link JournalImpl#appendCommitRecord(long)} about how the transaction-summary is recorded</p> 
    *     
    * @param journalTransaction
    * @param orderedFiles
    * @param recordedSummary
    * @return
    */
   private boolean checkTransactionHealth(final JournalFile currentFile,
                                          final JournalTransaction journalTransaction,
                                          final List<JournalFile> orderedFiles,
                                          final int numberOfRecords)
   {
      return journalTransaction.getCounter(currentFile) == numberOfRecords;
   }

   private static boolean isTransaction(final byte recordType)
   {
      return recordType == ADD_RECORD_TX || recordType == UPDATE_RECORD_TX ||
             recordType == DELETE_RECORD_TX ||
             isCompleteTransaction(recordType);
   }

   private static boolean isCompleteTransaction(final byte recordType)
   {
      return recordType == COMMIT_RECORD || recordType == PREPARE_RECORD || recordType == ROLLBACK_RECORD;
   }

   private static boolean isContainsBody(final byte recordType)
   {
      return recordType >= ADD_RECORD && recordType <= DELETE_RECORD_TX;
   }

   private static int getRecordSize(final byte recordType)
   {
      // The record size (without the variable portion)
      int recordSize = 0;
      switch (recordType)
      {
         case ADD_RECORD:
            recordSize = SIZE_ADD_RECORD;
            break;
         case UPDATE_RECORD:
            recordSize = SIZE_UPDATE_RECORD;
            break;
         case ADD_RECORD_TX:
            recordSize = SIZE_ADD_RECORD_TX;
            break;
         case UPDATE_RECORD_TX:
            recordSize = SIZE_UPDATE_RECORD_TX;
            break;
         case DELETE_RECORD:
            recordSize = SIZE_DELETE_RECORD;
            break;
         case DELETE_RECORD_TX:
            recordSize = SIZE_DELETE_RECORD_TX;
            break;
         case PREPARE_RECORD:
            recordSize = SIZE_PREPARE_RECORD;
            break;
         case COMMIT_RECORD:
            recordSize = SIZE_COMMIT_RECORD;
            break;
         case ROLLBACK_RECORD:
            recordSize = SIZE_ROLLBACK_RECORD;
            break;
         default:
            // Sanity check, this was previously tested, nothing different
            // should be on this switch
            throw new IllegalStateException("Record other than expected");

      }
      return recordSize;
   }

   /** 
    * This method requires bufferControl disabled, or the reads are going to be invalid
    * */
   private List<JournalFile> orderFiles() throws Exception
   {

      List<String> fileNames = fileFactory.listFiles(fileExtension);

      List<JournalFile> orderedFiles = new ArrayList<JournalFile>(fileNames.size());

      for (String fileName : fileNames)
      {
         SequentialFile file = fileFactory.createSequentialFile(fileName, maxAIO);

         file.open(1);

         ByteBuffer bb = fileFactory.newBuffer(SIZE_HEADER);

         file.read(bb);

         int fileID = bb.getInt();

         fileFactory.releaseBuffer(bb);

         bb = null;

         if (nextFileID.get() < fileID)
         {
            nextFileID.set(fileID);
         }

         orderedFiles.add(new JournalFileImpl(file, fileID));

         file.close();
      }

      // Now order them by ordering id - we can't use the file name for ordering
      // since we can re-use dataFiles

      Collections.sort(orderedFiles, new JournalFileComparator());

      return orderedFiles;
   }

   /** 
    * Note: You should aways guarantee locking the semaphore lock.
    * 
    * @param completeTransaction If the appendRecord is for a prepare or commit, where we should update the number of pendingTransactions on the current file
    * */
   private JournalFile appendRecord(final MessagingBuffer bb,
                                    final boolean completeTransaction,
                                    final boolean sync,
                                    final JournalTransaction tx,
                                    IOCallback callback) throws Exception
   {
      try
      {
         if (state != STATE_LOADED)
         {
            throw new IllegalStateException("The journal was stopped");
         }

         int size = bb.capacity();

         // We take into account the fileID used on the Header
         if (size > fileSize - currentFile.getFile().calculateBlockStart(SIZE_HEADER))
         {
            throw new IllegalArgumentException("Record is too large to store " + size);
         }

         // The buffer on the file can't be flushed or the currentFile could be affected
         currentFile.getFile().lockBuffer();

         if (!currentFile.getFile().fits(size))
         {
            currentFile.getFile().unlockBuffer();
            moveNextFile(false);
            currentFile.getFile().lockBuffer();

            // The same check needs to be done at the new file also
            if (!currentFile.getFile().fits(size))
            {
               // Sanity check, this should never happen
               throw new IllegalStateException("Invalid logic on buffer allocation");
            }
         }

         if (currentFile == null)
         {
            throw new NullPointerException("Current file = null");
         }

         if (tx != null)
         {
            if (callback != null)
            {
               // sanity check, it should not happen.
               throw new IllegalArgumentException("Invalid callback parameter. Use of tx is mutually exclusive with the callback");
            }

            // The callback of a transaction has to be taken inside the lock,
            // when we guarantee the currentFile will not be changed,
            // since we individualize the callback per file
            if (fileFactory.isSupportsCallbacks())
            {
               callback = tx.getCallback(currentFile);
            }

            if (sync)
            {
               // 99 % of the times this will be already synced, as previous files should be closed already.
               // This is to have 100% guarantee the transaction will be persisted and no loss of information would
               // happen
               tx.syncPreviousFiles(fileFactory.isSupportsCallbacks(), currentFile);
            }

            // We need to add the number of records on currentFile if prepare or commit
            if (completeTransaction)
            {
               // Filling the number of pendingTransactions at the current file
               tx.fillNumberOfRecords(currentFile, bb);
            }
         }

         // Adding fileID
         bb.writerIndex(DataConstants.SIZE_BYTE);
         bb.writeInt(currentFile.getFileID());

         if (callback != null)
         {
            currentFile.getFile().write(bb, sync, callback);
         }
         else
         {
            currentFile.getFile().write(bb, sync);
         }

         return currentFile;
      }
      finally
      {
         currentFile.getFile().unlockBuffer();
      }

   }

   /**
    * This method will create a new file on the file system, pre-fill it with FILL_CHARACTER
    * @param keepOpened
    * @return
    * @throws Exception
    */
   private JournalFile createFile(final boolean keepOpened, final boolean multiAIO, final boolean fill) throws Exception
   {
      int fileID = generateFileID();

      String fileName = filePrefix + "-" + fileID + "." + fileExtension;

      if (trace)
      {
         trace("Creating file " + fileName);
      }

      SequentialFile sequentialFile = fileFactory.createSequentialFile(fileName, maxAIO);

      if (multiAIO)
      {
         sequentialFile.open();
      }
      else
      {
         sequentialFile.open(1);
      }

      if (fill)
      {
         sequentialFile.fill(0, fileSize, FILL_CHARACTER);

         ByteBuffer bb = fileFactory.newBuffer(SIZE_HEADER);

         bb.putInt(fileID);

         bb.rewind();

         sequentialFile.write(bb, true);
      }

      JournalFile info = new JournalFileImpl(sequentialFile, fileID);

      if (!keepOpened)
      {
         sequentialFile.close();
      }

      return info;
   }

   private void openFile(final JournalFile file, final boolean multiAIO) throws Exception
   {
      if (multiAIO)
      {
         file.getFile().open();
      }
      else
      {
         file.getFile().open(1);
      }

      file.getFile().position(file.getFile().calculateBlockStart(SIZE_HEADER));
   }

   private int generateFileID()
   {
      return nextFileID.incrementAndGet();
   }

   // You need to guarantee lock.acquire() before calling this method
   private void moveNextFile(boolean synchronous) throws InterruptedException
   {
      closeFile(currentFile, synchronous);

      currentFile = enqueueOpenFile(synchronous);

      if (trace)
      {
         trace("moveNextFile: " + currentFile.getFile().getFileName());
      }

      fileFactory.activate(currentFile.getFile());
   }

   /** 
    * <p>This method will instantly return the opened file, and schedule opening and reclaiming.</p>
    * <p>In case there are no cached opened files, this method will block until the file was opened,
    * what would happen only if the system is under heavy load by another system (like a backup system, or a DB sharing the same box as JBM).</p> 
    * */
   private JournalFile enqueueOpenFile(boolean synchronous) throws InterruptedException
   {
      if (trace)
      {
         trace("enqueueOpenFile with openedFiles.size=" + openedFiles.size());
      }

      Runnable run = new Runnable()
      {
         public void run()
         {
            try
            {
               pushOpenedFile();
            }
            catch (Exception e)
            {
               log.error(e.getMessage(), e);
            }
         }
      };

      if (synchronous)
      {
         run.run();
      }
      else
      {
         filesExecutor.execute(run);
      }

      if (autoReclaim && !synchronous)
      {
         filesExecutor.execute(new Runnable()
         {
            public void run()
            {
               try
               {
                  checkAndReclaimFiles();
               }
               catch (Exception e)
               {
                  log.error(e.getMessage(), e);
               }
            }
         });
         filesExecutor.execute(new Runnable()
         {
            public void run()
            {
               try
               {
                  checkCompact();
               }
               catch (Exception e)
               {
                  log.error(e.getMessage(), e);
               }
            }
         });
      }

      JournalFile nextFile = null;

      while (nextFile == null)
      {
         nextFile = openedFiles.poll(60, TimeUnit.SECONDS);
         if (nextFile == null)
         {
            log.warn("Couldn't open a file in 60 Seconds", new Exception("Warning: Couldn't open a file in 60 Seconds"));
         }
      }

      return nextFile;
   }

   /** 
    * 
    * Open a file and place it into the openedFiles queue
    * */
   private void pushOpenedFile() throws Exception
   {
      JournalFile nextOpenedFile = getFile(true, true, true);

      openedFiles.offer(nextOpenedFile);
   }

   /**
    * @return
    * @throws Exception
    */
   JournalFile getFile(final boolean keepOpened, final boolean multiAIO, final boolean fill) throws Exception
   {
      JournalFile nextOpenedFile = null;
      try
      {
         nextOpenedFile = freeFiles.remove();
      }
      catch (NoSuchElementException ignored)
      {
      }

      if (nextOpenedFile == null)
      {
         nextOpenedFile = createFile(keepOpened, multiAIO, fill);
      }
      else
      {
         if (keepOpened)
         {
            openFile(nextOpenedFile, multiAIO);
         }
      }
      return nextOpenedFile;
   }

   private void closeFile(final JournalFile file, boolean synchronous)
   {
      fileFactory.deactivate(file.getFile());
      pendingCloseFiles.add(file);

      Runnable run = new Runnable()
      {
         public void run()
         {
            compactingLock.readLock().lock();
            try
            {
               dataFiles.add(file);
               pendingCloseFiles.remove(file);
               if (file.getFile().isOpen())
               {
                  file.getFile().close();
               }
            }
            catch (Exception e)
            {
               log.warn(e.getMessage(), e);
            }
            finally
            {
               compactingLock.readLock().unlock();
            }
         }
      };

      if (synchronous)
      {
         run.run();
      }
      else
      {
         filesExecutor.execute(run);
      }

   }

   private JournalTransaction getTransactionInfo(final long txID)
   {
      JournalTransaction tx = transactions.get(txID);

      if (tx == null)
      {
         tx = new JournalTransaction(txID, this);

         JournalTransaction trans = transactions.putIfAbsent(txID, tx);

         if (trans != null)
         {
            tx = trans;
         }
      }

      return tx;
   }

   private IOCallback getSyncCallback(final boolean sync)
   {
      if (fileFactory.isSupportsCallbacks())
      {
         if (sync)
         {
            return SimpleWaitIOCallback.getInstance();
         }
         else
         {
            return DummyCallback.getInstance();
         }
      }
      else
      {
         return null;
      }
   }

   /**
    * @return
    * @throws Exception
    */
   private void checkControlFile() throws Exception
   {
      ArrayList<String> dataFiles = new ArrayList<String>();
      ArrayList<String> newFiles = new ArrayList<String>();

      SequentialFile controlFile = JournalCompactor.readControlFile(fileFactory, dataFiles, newFiles);
      if (controlFile != null)
      {
         log.info("Journal Compactor was interrupted during renaming phase, renaming files");

         for (String dataFile : dataFiles)
         {
            SequentialFile file = fileFactory.createSequentialFile(dataFile, 1);
            log.info("Removing old compacted file" + file.getFileName());
            if (file.exists())
            {
               file.delete();
            }
         }

         for (String newFile : newFiles)
         {
            SequentialFile file = fileFactory.createSequentialFile(newFile, 1);
            log.info("Renaming file " + file.getFileName() + " as an part of the data files");
            if (file.exists())
            {
               final String originalName = file.getFileName();
               final String newName = originalName.substring(0, originalName.lastIndexOf(".cmp"));
               file.renameTo(newName);
            }
         }

         controlFile.delete();
      }

      List<String> leftFiles = fileFactory.listFiles(getFileExtension() + ".cmp");

      if (leftFiles.size() > 0)
      {
         log.warn("Compacted files were left unnatended on journal directory, deleting invalid files now");

         for (String fileToDelete : leftFiles)
         {
            log.warn("Deleting unnatended file " + fileToDelete);
            SequentialFile file = fileFactory.createSequentialFile(fileToDelete, 1);
            file.delete();
         }
      }

      return;
   }

   private static boolean isInvalidSize(final int fileSize, final int bufferPos, final int size)
   {
      if (size < 0)
      {
         return true;
      }
      else
      {
         final int position = bufferPos + size;
         return position > fileSize || position < 0;

      }
   }

   private ChannelBuffer newBuffer(final int size)
   {
      return ChannelBuffers.buffer(size);
   }

   // Inner classes
   // ---------------------------------------------------------------------------

   /** 
    * This holds the relationship a record has with other files in regard to reference counting.
    * Note: This class used to be called PosFiles
    * 
    * Used on the ref-count for reclaiming */
   public static class JournalRecord
   {
      private final JournalFile addFile;

      private final int size;

      private List<Pair<JournalFile, Integer>> updateFiles;

      JournalRecord(final JournalFile addFile, final int size)
      {
         this.addFile = addFile;

         this.size = size;

         addFile.incPosCount();

         addFile.addSize(size);
      }

      void addUpdateFile(final JournalFile updateFile, final int size)
      {
         if (updateFiles == null)
         {
            updateFiles = new ArrayList<Pair<JournalFile, Integer>>();
         }

         updateFiles.add(new Pair<JournalFile, Integer>(updateFile, size));

         updateFile.incPosCount();

         updateFile.addSize(size);
      }

      void delete(final JournalFile file)
      {
         file.incNegCount(addFile);
         addFile.decSize(size);

         if (updateFiles != null)
         {
            for (Pair<JournalFile, Integer> updFile : updateFiles)
            {
               file.incNegCount(updFile.a);
               updFile.a.decSize(updFile.b);
            }
         }
      }

      public String toString()
      {
         StringBuffer buffer = new StringBuffer();
         buffer.append("JournalRecord(add=" + addFile.getFile().getFileName());

         if (updateFiles != null)
         {

            for (Pair<JournalFile, Integer> update : updateFiles)
            {
               buffer.append(", update=" + update.a.getFile().getFileName());
            }

         }

         buffer.append(")");

         return buffer.toString();
      }
   }

   private static class NullEncoding implements EncodingSupport
   {

      static NullEncoding instance = new NullEncoding();

      public void decode(final MessagingBuffer buffer)
      {
      }

      public void encode(final MessagingBuffer buffer)
      {
      }

      public int getEncodeSize()
      {
         return 0;
      }

   }

   public static class ByteArrayEncoding implements EncodingSupport
   {

      final byte[] data;

      public ByteArrayEncoding(final byte[] data)
      {
         this.data = data;
      }

      // Public --------------------------------------------------------

      public void decode(final MessagingBuffer buffer)
      {
         throw new IllegalStateException("operation not supported");
      }

      public void encode(final MessagingBuffer buffer)
      {
         buffer.writeBytes(data);
      }

      public int getEncodeSize()
      {
         return data.length;
      }
   }

   // Used on Load
   private static class TransactionHolder
   {
      public TransactionHolder(final long id)
      {
         transactionID = id;
      }

      public final long transactionID;

      public final List<RecordInfo> recordInfos = new ArrayList<RecordInfo>();

      public final List<RecordInfo> recordsToDelete = new ArrayList<RecordInfo>();

      public boolean prepared;

      public boolean invalid;

      public byte[] extraData;

   }

   private static class JournalFileComparator implements Comparator<JournalFile>
   {
      public int compare(final JournalFile f1, final JournalFile f2)
      {
         int id1 = f1.getFileID();
         int id2 = f2.getFileID();

         return id1 < id2 ? -1 : id1 == id2 ? 0 : 1;
      }
   }

   private class PerfBlast extends Thread
   {
      private final int pages;

      private PerfBlast(final int pages)
      {
         this.pages = pages;
      }

      public void run()
      {
         try
         {
            lock.acquire();

            MessagingBuffer bb = newBuffer(128 * 1024);

            for (int i = 0; i < pages; i++)
            {
               appendRecord(bb, false, false, null, null);
            }

            lock.release();
         }
         catch (Exception e)
         {
            log.error("Failed to perf blast", e);
         }
      }
   }

}
