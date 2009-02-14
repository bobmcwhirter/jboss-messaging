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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.messaging.core.journal.SequentialFile;
import org.jboss.messaging.core.journal.impl.JournalFile;
import org.jboss.messaging.core.journal.impl.Reclaimer;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.tests.util.UnitTestCase;
import org.jboss.messaging.util.Pair;

/**
 * 
 * A ReclaimerTest
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class ReclaimerTest extends UnitTestCase
{
   private static final Logger log = Logger.getLogger(ReclaimerTest.class);

   private JournalFile[] files;

   private Reclaimer reclaimer;

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      reclaimer = new Reclaimer();
   }

   public void testOneFilePosNegAll() throws Exception
   {
      setup(1);

      setupPosNeg(0, 10, 10);

      reclaimer.scan(files);

      assertCanDelete(0);
   }

   public void testOneFilePosNegNotAll() throws Exception
   {
      setup(1);

      setupPosNeg(0, 10, 7);

      reclaimer.scan(files);

      assertCantDelete(0);
   }

   public void testOneFilePosOnly() throws Exception
   {
      setup(1);

      setupPosNeg(0, 10);

      reclaimer.scan(files);

      assertCantDelete(0);
   }

   public void testOneFileNegOnly() throws Exception
   {
      setup(1);

      setupPosNeg(0, 0, 10);

      reclaimer.scan(files);

      assertCanDelete(0);
   }

   public void testTwoFilesPosNegAllDifferentFiles() throws Exception
   {
      setup(2);

      setupPosNeg(0, 10);
      setupPosNeg(1, 0, 10);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);

   }

   public void testTwoFilesPosNegAllSameFiles() throws Exception
   {
      setup(2);

      setupPosNeg(0, 10, 10);
      setupPosNeg(1, 10, 0, 10);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);

   }

   public void testTwoFilesPosNegMixedFiles() throws Exception
   {
      setup(2);

      setupPosNeg(0, 10, 7);
      setupPosNeg(1, 10, 3, 10);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);
   }

   public void testTwoFilesPosNegAllFirstFile() throws Exception
   {
      setup(2);

      setupPosNeg(0, 10, 10);
      setupPosNeg(1, 10);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCantDelete(1);
   }

   public void testTwoFilesPosNegAllSecondFile() throws Exception
   {
      setup(2);

      setupPosNeg(0, 10);
      setupPosNeg(1, 10, 0, 10);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCanDelete(1);
   }

   public void testTwoFilesPosOnly() throws Exception
   {
      setup(2);

      setupPosNeg(0, 10);
      setupPosNeg(1, 10);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCantDelete(1);
   }

   public void testTwoFilesxyz() throws Exception
   {
      setup(2);

      setupPosNeg(0, 10);
      setupPosNeg(1, 10, 10);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCantDelete(1);
   }

   // Can-can-can

   public void testThreeFiles1() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 10, 0, 0);
      setupPosNeg(1, 10, 0, 10, 0);
      setupPosNeg(2, 10, 0, 0, 10);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);
      assertCanDelete(2);
   }

   public void testThreeFiles2() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 7, 0, 0);
      setupPosNeg(1, 10, 3, 5, 0);
      setupPosNeg(2, 10, 0, 5, 10);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);
      assertCanDelete(2);
   }

   public void testThreeFiles3() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 1, 0, 0);
      setupPosNeg(1, 10, 6, 5, 0);
      setupPosNeg(2, 10, 3, 5, 10);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);
      assertCanDelete(2);
   }

   public void testThreeFiles3_1() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 1, 0, 0);
      setupPosNeg(1, 10, 6, 5, 0);
      setupPosNeg(2, 0, 3, 5, 0);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);
      assertCanDelete(2);
   }

   public void testThreeFiles3_2() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 1, 0, 0);
      setupPosNeg(1, 0, 6, 0, 0);
      setupPosNeg(2, 0, 3, 0, 0);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);
      assertCanDelete(2);
   }

   // Cant-can-can

   public void testThreeFiles4() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 10, 0, 5, 0);
      setupPosNeg(2, 10, 0, 5, 10);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCanDelete(1);
      assertCanDelete(2);
   }

   public void testThreeFiles5() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 10, 0, 5, 0);
      setupPosNeg(2, 0, 0, 5, 0);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCanDelete(1);
      assertCanDelete(2);
   }

   public void testThreeFiles6() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 0, 0, 0);
      setupPosNeg(1, 10, 0, 5, 0);
      setupPosNeg(2, 0, 0, 5, 10);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCanDelete(1);
      assertCanDelete(2);
   }

   public void testThreeFiles7() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 0, 0, 0);
      setupPosNeg(1, 10, 0, 5, 0);
      setupPosNeg(2, 0, 0, 5, 0);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCanDelete(1);
      assertCanDelete(2);
   }

   // Cant can cant

   public void testThreeFiles8() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 10, 0, 10, 0);
      setupPosNeg(2, 10, 0, 0, 2);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCanDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles9() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 10, 0, 10, 0);
      setupPosNeg(2, 10, 1, 0, 2);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCanDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles10() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 10, 0, 10, 0);
      setupPosNeg(2, 10, 1, 0, 0);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCanDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles11() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 0, 0, 0);
      setupPosNeg(1, 10, 0, 10, 0);
      setupPosNeg(2, 10, 0, 0, 0);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCanDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles12() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 0, 0, 0);
      setupPosNeg(1, 10, 0, 10, 0);
      setupPosNeg(2, 0, 3, 0, 0);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCanDelete(1);
      assertCantDelete(2);
   }

   // Cant-cant-cant

   public void testThreeFiles13() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 10, 2, 3, 0);
      setupPosNeg(2, 10, 1, 5, 7);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles14() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 0, 2, 0, 0);
      setupPosNeg(2, 10, 1, 0, 7);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles15() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 10, 2, 3, 0);
      setupPosNeg(2, 0, 1, 5, 0);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles16() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 0, 2, 0, 0);
      setupPosNeg(2, 0, 1, 0, 0);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles17() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 10, 0, 3, 0);
      setupPosNeg(2, 10, 1, 5, 7);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles18() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 10, 0, 3, 0);
      setupPosNeg(2, 10, 1, 0, 7);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles19() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 10, 0, 3, 0);
      setupPosNeg(2, 10, 1, 0, 0);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles20() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 3, 0, 0);
      setupPosNeg(1, 10, 0, 0, 0);
      setupPosNeg(2, 10, 1, 0, 0);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles21() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 0, 0, 0);
      setupPosNeg(1, 10, 0, 0, 0);
      setupPosNeg(2, 10, 0, 0, 0);

      reclaimer.scan(files);

      assertCantDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   // Can-can-cant

   public void testThreeFiles22() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 10, 0, 0);
      setupPosNeg(1, 10, 0, 10, 0);
      setupPosNeg(2, 10, 0, 0, 0);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles23() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 10, 0, 0);
      setupPosNeg(1, 10, 0, 10, 0);
      setupPosNeg(2, 10, 3, 0, 0);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles24() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 7, 0, 0);
      setupPosNeg(1, 10, 3, 10, 0);
      setupPosNeg(2, 10, 3, 0, 0);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles25() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 7, 0, 0);
      setupPosNeg(1, 0, 3, 10, 0);
      setupPosNeg(2, 10, 3, 0, 0);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles26() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 7, 0, 0);
      setupPosNeg(1, 0, 3, 10, 0);
      setupPosNeg(2, 10, 0, 0, 0);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCanDelete(1);
      assertCantDelete(2);
   }

   // Can-cant-cant

   public void testThreeFiles27() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 10, 0, 0);
      setupPosNeg(1, 10, 0, 0, 0);
      setupPosNeg(2, 10, 0, 0, 0);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles28() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 10, 0, 0);
      setupPosNeg(1, 10, 0, 3, 0);
      setupPosNeg(2, 10, 0, 0, 5);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles29() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 10, 0, 0);
      setupPosNeg(1, 10, 0, 3, 0);
      setupPosNeg(2, 10, 0, 6, 5);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   public void testThreeFiles30() throws Exception
   {
      setup(3);

      setupPosNeg(0, 10, 10, 0, 0);
      setupPosNeg(1, 10, 0, 3, 0);
      setupPosNeg(2, 0, 0, 6, 0);

      reclaimer.scan(files);

      assertCanDelete(0);
      assertCantDelete(1);
      assertCantDelete(2);
   }

   // Private
   // ------------------------------------------------------------------------

   private void setup(final int numFiles)
   {
      files = new JournalFile[numFiles];

      for (int i = 0; i < numFiles; i++)
      {
         files[i] = new MockJournalFile();
      }
   }

   private void setupPosNeg(final int fileNumber, final int pos, final int... neg)
   {
      JournalFile file = files[fileNumber];

      for (int i = 0; i < pos; i++)
      {
         file.incPosCount();
      }

      for (int i = 0; i < neg.length; i++)
      {
         JournalFile reclaimable2 = files[i];

         for (int j = 0; j < neg[i]; j++)
         {
            file.incNegCount(reclaimable2);
         }
      }
   }

   private void assertCanDelete(final int... fileNumber)
   {
      for (int num : fileNumber)
      {
         assertTrue(files[num].isCanReclaim());
      }
   }

   private void assertCantDelete(final int... fileNumber)
   {
      for (int num : fileNumber)
      {
         assertFalse(files[num].isCanReclaim());
      }
   }

   class MockJournalFile implements JournalFile
   {
      private final Set<Long> transactionIDs = new HashSet<Long>();

      private final Set<Long> transactionTerminationIDs = new HashSet<Long>();

      private final Set<Long> transactionPrepareIDs = new HashSet<Long>();

      private final Map<JournalFile, Integer> negCounts = new HashMap<JournalFile, Integer>();

      private int posCount;

      private boolean canDelete;

      private boolean linkedDependency;

      public void extendOffset(final int delta)
      {
      }

      public SequentialFile getFile()
      {
         return null;
      }

      public long getOffset()
      {
         return 0;
      }

      public int getOrderingID()
      {
         return 0;
      }

      public void setOffset(final long offset)
      {
      }

      public int getNegCount(final JournalFile file)
      {
         Integer count = negCounts.get(file);

         if (count != null)
         {
            return count.intValue();
         }
         else
         {
            return 0;
         }
      }

      public void incNegCount(final JournalFile file)
      {
         Integer count = negCounts.get(file);

         int c = count == null ? 1 : count.intValue() + 1;

         negCounts.put(file, c);
      }

      /* (non-Javadoc)
       * @see org.jboss.messaging.core.journal.impl.JournalFile#decNegCount(org.jboss.messaging.core.journal.impl.JournalFile)
       */
      public void decNegCount(JournalFile file)
      {
         Integer count = negCounts.get(file);

         int c = count == null ? 1 : count.intValue() - 1;

         negCounts.put(file, c);
      }

      public int getPosCount()
      {
         return posCount;
      }

      public void incPosCount()
      {
         posCount++;
      }

      public void decPosCount()
      {
         posCount--;
      }

      public boolean isCanReclaim()
      {
         return canDelete;
      }

      public void setCanReclaim(final boolean canDelete)
      {
         this.canDelete = canDelete;
      }

      public void addTransactionID(final long id)
      {
         transactionIDs.add(id);
      }

      public void addTransactionPrepareID(final long id)
      {
         transactionPrepareIDs.add(id);
      }

      public void addTransactionTerminationID(final long id)
      {
         transactionTerminationIDs.add(id);
      }

      public boolean containsTransactionID(final long id)
      {
         return transactionIDs.contains(id);
      }

      public boolean containsTransactionPrepareID(final long id)
      {
         return transactionPrepareIDs.contains(id);
      }

      public boolean containsTransactionTerminationID(final long id)
      {
         return transactionTerminationIDs.contains(id);
      }

      public Set<Long> getTranactionTerminationIDs()
      {
         return transactionTerminationIDs;
      }

      public Set<Long> getTransactionPrepareIDs()
      {
         return transactionPrepareIDs;
      }

      public Set<Long> getTransactionsIDs()
      {
         return transactionIDs;
      }

      /* (non-Javadoc)
       * @see org.jboss.messaging.core.journal.impl.JournalFile#getTotalNegCount()
       */
      public int getTotalNegCount()
      {
         return 0;
      }

      /* (non-Javadoc)
       * @see org.jboss.messaging.core.journal.impl.JournalFile#setTotalNegCount(int)
       */
      public void setTotalNegCount(int total)
      {
      }

      /* (non-Javadoc)
       * @see org.jboss.messaging.core.journal.impl.JournalFile#addCleanupInfo(long, org.jboss.messaging.core.journal.impl.JournalFile)
       */
      public void addCleanupInfo(long id, JournalFile deleteFile)
      {
      }

      /* (non-Javadoc)
       * @see org.jboss.messaging.core.journal.impl.JournalFile#getCleanupInfo(long)
       */
      public JournalFile getCleanupInfo(long id)
      {
         return null;
      }
      
      /**
       * @return the linkedDependency
       */
      public boolean isLinkedDependency()
      {
         return linkedDependency;
      }

      /**
       * @param linkedDependency the linkedDependency to set
       */
      public void setLinkedDependency(boolean linkedDependency)
      {
         this.linkedDependency = linkedDependency;
      }

      

   }
}
