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
 * 
 * A ReattachSessionMessage
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class ReattachSessionMessage extends PacketImpl
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private String name;
   
   private int lastReceivedCommandID;
   
   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public ReattachSessionMessage(final String name, final int lastReceivedCommandID)
   {
      super(REATTACH_SESSION);

      this.name = name;
      
      this.lastReceivedCommandID = lastReceivedCommandID;
   }
   
   public ReattachSessionMessage()
   {
      super(REATTACH_SESSION);
   }

   // Public --------------------------------------------------------

   public String getName()
   {
      return name;
   }
   
   public int getLastReceivedCommandID()
   {
      return lastReceivedCommandID;
   }
   
   public void encodeBody(final MessagingBuffer buffer)
   {
      buffer.putString(name);
      buffer.putInt(lastReceivedCommandID);
   }
   
   public void decodeBody(final MessagingBuffer buffer)
   {
      name = buffer.getString();
      lastReceivedCommandID = buffer.getInt();
   }

   public boolean equals(Object other)
   {
      if (other instanceof ReattachSessionMessage == false)
      {
         return false;
      }
            
      ReattachSessionMessage r = (ReattachSessionMessage)other;
      
      return super.equals(other) && this.name.equals(r.name);
   }
   
   public final boolean isRequiresConfirmations()
   {
      return false;
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}

