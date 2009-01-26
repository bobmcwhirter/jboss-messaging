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

package org.jboss.messaging.tests.integration.persistence;

import java.io.File;

import org.jboss.messaging.core.config.impl.FileConfiguration;
import org.jboss.messaging.core.persistence.impl.journal.JournalStorageManager;
import org.jboss.messaging.core.server.JournalType;
import org.jboss.messaging.core.server.LargeServerMessage;
import org.jboss.messaging.core.server.ServerMessage;
import org.jboss.messaging.tests.util.ServiceTestBase;
import org.jboss.messaging.tests.util.UnitTestCase;

/**
 * A JournalStorageManagerIntegrationTest
 *
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * 
 * Created Jan 24, 2009 11:14:13 PM
 *
 *
 */
public class JournalStorageManagerIntegrationTest extends ServiceTestBase
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testLargeMessageCopy() throws Exception
   {
      clearData();
      FileConfiguration configuration = createFileConfig();

      configuration.start();

      configuration.setJournalType(JournalType.NIO);

      final JournalStorageManager journal = new JournalStorageManager(configuration);
      journal.start();

      LargeServerMessage msg = journal.createLargeMessage();
      msg.setMessageID(1);

      byte[] data = new byte[1024];

      for (int i = 0; i < 110; i++)
         msg.addBytes(data);

      ServerMessage msg2 = msg.copy(2);
      
      assertEquals(110 * 1024, msg.getBodySize());
      assertEquals(110 * 1024, msg2.getBodySize());
      
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}