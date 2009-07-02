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

package org.jboss.messaging.core.journal.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.messaging.core.exception.MessagingException;
import org.jboss.messaging.core.remoting.spi.MessagingBuffer;
import org.jboss.messaging.utils.DataConstants;
import org.jboss.messaging.utils.Pair;

/**
 * A JournalTransaction
 *
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 *
 *
 */
public class JournalTransaction
{

   private final JournalImpl journal;

   private List<JournalUpdate> pos;

   private List<JournalUpdate> neg;

   private final long id;

   // All the files this transaction is touching on.
   // We can't have those files being reclaimed if there is a pending transaction
   private Set<JournalFile> pendingFiles;

   private TransactionCallback currentCallback;

   private boolean compacting = false;

   private Map<JournalFile, TransactionCallback> callbackList;

   private JournalFile lastFile = null;

   private final AtomicInteger counter = new AtomicInteger();

   public JournalTransaction(final long id, final JournalImpl journal)
   {
      this.id = id;
      this.journal = journal;
   }

   /**
    * @return the id
    */
   public long getId()
   {
      return id;
   }

   public int getCounter(final JournalFile file)
   {
      return internalgetCounter(file).intValue();
   }

   public void incCounter(final JournalFile file)
   {
      internalgetCounter(file).incrementAndGet();
   }

   public long[] getPositiveArray()
   {
      if (pos == null)
      {
         return new long[0];
      }
      else
      {
         int i = 0;
         long[] ids = new long[pos.size()];
         for (JournalUpdate el : pos)
         {
            ids[i++] = el.getId();
         }
         return ids;
      }
   }

   public void setCompacting()
   {
      compacting = true;

      // Everything is cleared on the transaction...
      // since we are compacting, everything is at the compactor's level
      clear();
   }

   /** This is used to merge transactions from compacting */
   public void merge(JournalTransaction other)
   {
      if (other.pos != null)
      {
         if (pos == null)
         {
            pos = new ArrayList<JournalUpdate>();
         }

         pos.addAll(other.pos);
      }

      if (other.neg != null)
      {
         if (neg == null)
         {
            neg = new ArrayList<JournalUpdate>();
         }

         neg.addAll(other.neg);
      }

      if (other.pendingFiles != null)
      {
         if (pendingFiles == null)
         {
            pendingFiles = new HashSet<JournalFile>();
         }

         pendingFiles.addAll(other.pendingFiles);
      }

      this.compacting = false;
   }

   /**
    * 
    */
   public void clear()
   {
      // / Compacting is recreating all the previous files and everything
      // / so we just clear the list of previous files, previous pos and previous adds
      // / The transaction may be working at the top from now

      if (pendingFiles != null)
      {
         pendingFiles.clear();
      }

      if (callbackList != null)
      {
         callbackList.clear();
      }

      if (pos != null)
      {
         pos.clear();
      }

      if (neg != null)
      {
         neg.clear();
      }

      counter.set(0);

      lastFile = null;

      currentCallback = null;
   }

   /**
    * @param currentFile
    * @param bb
    */
   public void fillNumberOfRecords(final JournalFile currentFile, final MessagingBuffer bb)
   {
      bb.writerIndex(DataConstants.SIZE_BYTE + DataConstants.SIZE_INT + DataConstants.SIZE_LONG);

      bb.writeInt(getCounter(currentFile));

   }

   /** 99.99 % of the times previous files will be already synced, since they are scheduled to be closed.
    *  Because of that, this operation should be almost very fast.*/
   public void syncPreviousFiles(final boolean callbacks, final JournalFile currentFile) throws Exception
   {
      if (callbacks)
      {
         if (callbackList != null)
         {
            for (Map.Entry<JournalFile, TransactionCallback> entry : callbackList.entrySet())
            {
               if (entry.getKey() != currentFile)
               {
                  entry.getValue().waitCompletion();
               }
            }
         }
      }
      else
      {
         for (JournalFile file : pendingFiles)
         {
            if (file != currentFile)
            {
               file.getFile().waitForClose();
            }
         }
      }
   }

   /**
    * @return
    */
   public TransactionCallback getCallback(final JournalFile file) throws Exception
   {
      if (callbackList == null)
      {
         callbackList = new HashMap<JournalFile, TransactionCallback>();
      }

      currentCallback = callbackList.get(file);

      if (currentCallback == null)
      {
         currentCallback = new TransactionCallback();
         callbackList.put(file, currentCallback);
      }

      if (currentCallback.getErrorMessage() != null)
      {
         throw new MessagingException(currentCallback.getErrorCode(), currentCallback.getErrorMessage());
      }

      currentCallback.countUp();

      return currentCallback;
   }

   public void addPositive(final JournalFile file, final long id, final int size)
   {
      incCounter(file);

      addFile(file);

      if (pos == null)
      {
         pos = new ArrayList<JournalUpdate>();
      }

      pos.add(new JournalUpdate(file, id, size));
   }

   public void addNegative(final JournalFile file, final long id)
   {
      incCounter(file);

      addFile(file);

      if (neg == null)
      {
         neg = new ArrayList<JournalUpdate>();
      }

      neg.add(new JournalUpdate(file, id, 0));
   }

   /** 
    * The caller of this method needs to guarantee lock.acquire at the journal. (unless this is being called from load what is a single thread process).
    * */
   public void commit(final JournalFile file)
   {
      JournalCompactor compactor = journal.getCompactor();

      if (compacting)
      {
         compactor.addCommandCommit(this, file);
      }
      else
      {

         if (pos != null)
         {
            for (JournalUpdate trUpdate : pos)
            {
               JournalImpl.JournalRecord posFiles = journal.getRecords().get(trUpdate.id);

               if (compactor != null && compactor.lookupRecord(trUpdate.id))
               {
                  // This is a case where the transaction was opened after compacting was started,
                  // but the commit arrived while compacting was working
                  // We need to cache the counter update, so compacting will take the correct files when it is done
                  compactor.addCommandUpdate(trUpdate.id, trUpdate.file, trUpdate.size);
               }
               else if (posFiles == null)
               {
                  posFiles = new JournalImpl.JournalRecord(trUpdate.file, trUpdate.size);

                  journal.getRecords().put(trUpdate.id, posFiles);
               }
               else
               {
                  posFiles.addUpdateFile(trUpdate.file, trUpdate.size);
               }
            }
         }

         if (neg != null)
         {
            for (JournalUpdate trDelete : neg)
            {
               JournalImpl.JournalRecord posFiles = journal.getRecords().remove(trDelete.id);

               if (posFiles != null)
               {
                  posFiles.delete(trDelete.file);
               }
               else if (compactor != null && compactor.lookupRecord(trDelete.id))
               {
                  // This is a case where the transaction was opened after compacting was started,
                  // but the commit arrived while compacting was working
                  // We need to cache the counter update, so compacting will take the correct files when it is done
                  compactor.addCommandDelete(trDelete.id, trDelete.file);
               }
            }
         }

         // Now add negs for the pos we added in each file in which there were
         // transactional operations

         for (JournalFile jf : pendingFiles)
         {
            file.incNegCount(jf);
         }
      }
   }

   public void waitCallbacks() throws Exception
   {
      if (callbackList != null)
      {
         for (TransactionCallback callback : callbackList.values())
         {
            callback.waitCompletion();
         }
      }
   }

   /** Wait completion at the latest file only */
   public void waitCompletion() throws Exception
   {
      if (currentCallback != null)
      {
         currentCallback.waitCompletion();
      }
   }

   /** 
    * The caller of this method needs to guarantee lock.acquire before calling this method if being used outside of the lock context.
    * or else potFilesMap could be affected
    * */
   public void rollback(final JournalFile file)
   {
      JournalCompactor compactor = journal.getCompactor();

      if (compacting && compactor != null)
      {
         compactor.addCommandRollback(this, file);
      }
      else
      {
         // Now add negs for the pos we added in each file in which there were
         // transactional operations
         // Note that we do this on rollback as we do on commit, since we need
         // to ensure the file containing
         // the rollback record doesn't get deleted before the files with the
         // transactional operations are deleted
         // Otherwise we may run into problems especially with XA where we are
         // just left with a prepare when the tx
         // has actually been rolled back

         for (JournalFile jf : pendingFiles)
         {
            file.incNegCount(jf);
         }
      }
   }

   /** 
    * The caller of this method needs to guarantee lock.acquire before calling this method if being used outside of the lock context.
    * or else potFilesMap could be affected
    * */
   public void prepare(final JournalFile file)
   {
      // We don't want the prepare record getting deleted before time

      addFile(file);
   }

   /** Used by load, when the transaction was not loaded correctly */
   public void forget()
   {
      // The transaction was not committed or rolled back in the file, so we
      // reverse any pos counts we added
      for (JournalFile jf : pendingFiles)
      {
         jf.decPosCount();
      }

   }

   public String toString()
   {
      return "JournalTransaction(" + this.id + ")";
   }

   private AtomicInteger internalgetCounter(final JournalFile file)
   {
      if (lastFile != file)

      {
         lastFile = file;
         counter.set(0);
      }
      return counter;
   }

   private void addFile(final JournalFile file)
   {
      if (pendingFiles == null)
      {
         pendingFiles = new HashSet<JournalFile>();
      }

      if (!pendingFiles.contains(file))
      {
         pendingFiles.add(file);

         // We add a pos for the transaction itself in the file - this
         // prevents any transactional operations
         // being deleted before a commit or rollback is written
         file.incPosCount();
      }
   }

   static class JournalUpdate
   {
      JournalFile file;

      long id;

      int size;
      
      
      /**
       * @param file
       * @param id
       * @param size
       */
      public JournalUpdate(JournalFile file, long id, int size)
      {
         super();
         this.file = file;
         this.id = id;
         this.size = size;
      }

      /**
       * @return the file
       */
      public JournalFile getFile()
      {
         return file;
      }

      /**
       * @return the id
       */
      public long getId()
      {
         return id;
      }

      /**
       * @return the size
       */
      public int getSize()
      {
         return size;
      }

   }
}
