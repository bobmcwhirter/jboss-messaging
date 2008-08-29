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

package org.jboss.messaging.core.remoting.impl.wireformat;

import org.jboss.messaging.core.remoting.spi.MessagingBuffer;

/**
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>.
 * 
 * @version <tt>$Revision$</tt>
 */
public class CreateSessionResponseMessage extends PacketImpl
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private long sessionTargetID;
   
   private long commandResponseTargetID;
   
   private int serverVersion;
   
   private int packetConfirmationBatchSize;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public CreateSessionResponseMessage(final long sessionTargetID, final long commandResponseTargetID,
                                       final int serverVersion, final int packetConfirmationBatchSize)
   {
      super(CREATESESSION_RESP);

      this.sessionTargetID = sessionTargetID;
      
      this.commandResponseTargetID = commandResponseTargetID;
      
      this.serverVersion = serverVersion;
      
      this.packetConfirmationBatchSize = packetConfirmationBatchSize;
   }
   
   public CreateSessionResponseMessage()
   {
      super(CREATESESSION_RESP);
   }

   // Public --------------------------------------------------------

   public long getSessionID()
   {
      return sessionTargetID;
   }
   
   public long getCommandResponseTargetID()
   {
      return commandResponseTargetID;
   }
   
   public int getServerVersion()
   {
      return serverVersion;
   }
   
   public int getPacketConfirmationBatchSize()
   {
      return packetConfirmationBatchSize;
   }
   
   public void encodeBody(final MessagingBuffer buffer)
   {
      buffer.putLong(sessionTargetID);
      buffer.putLong(commandResponseTargetID);
      buffer.putInt(serverVersion);      
      buffer.putInt(packetConfirmationBatchSize);
   }
   
   public void decodeBody(final MessagingBuffer buffer)
   {
      sessionTargetID = buffer.getLong();
      commandResponseTargetID = buffer.getLong();
      serverVersion = buffer.getInt();
      packetConfirmationBatchSize = buffer.getInt();
   }

   @Override
   public String toString()
   {
      return getParentString() + ", sessionTargetID=" + sessionTargetID
            + "]";
   }
   
   public boolean equals(Object other)
   {
      if (other instanceof CreateSessionResponseMessage == false)
      {
         return false;
      }
            
      CreateSessionResponseMessage r = (CreateSessionResponseMessage)other;
      
      boolean matches = super.equals(other) &&
                        this.sessionTargetID == r.sessionTargetID &&
                        this.commandResponseTargetID == r.commandResponseTargetID &&
                        this.packetConfirmationBatchSize == r.packetConfirmationBatchSize;
      
      return matches;
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}