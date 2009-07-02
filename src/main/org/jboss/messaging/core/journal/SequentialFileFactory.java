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

package org.jboss.messaging.core.journal;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 
 * A SequentialFileFactory
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 *
 */
public interface SequentialFileFactory
{
   SequentialFile createSequentialFile(String fileName, int maxIO);

   List<String> listFiles(String extension) throws Exception;

   boolean isSupportsCallbacks();
   
   ByteBuffer newBuffer(int size);
   
   void releaseBuffer(ByteBuffer buffer);
   
   /** The factory may need to do some initialization before the file is activated.
    *  this was added as a hook for AIO to initialize the Observer on TimedBuffer.
    *  It could be eventually done the same on NIO if we implement TimedBuffer on NIO */
   void activate(SequentialFile file);
   
   void deactivate(SequentialFile file);

   // To be used in tests only
   ByteBuffer wrapBuffer(byte[] bytes);

   int getAlignment();

   int calculateBlockSize(int bytes);

   void clearBuffer(ByteBuffer buffer);
   
   void start();
   
   void stop();
   
   /** 
    * Create the directory if it doesn't exist yet
    */
   void createDirs() throws Exception;
   
   // used on tests only
   void testFlush();


}
