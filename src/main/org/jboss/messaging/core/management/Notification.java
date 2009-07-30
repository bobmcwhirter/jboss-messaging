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

package org.jboss.messaging.core.management;

import org.jboss.messaging.utils.TypedProperties;

/**
 * A Notification
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Created 22 Jan 2009 16:41:12
 *
 *
 */
public class Notification
{
   private final NotificationType type;

   private final TypedProperties properties;

   public Notification(String uid, final NotificationType type, final TypedProperties properties)
   {
      this.uid = uid;
      this.type = type;
      this.properties = properties;
   }

   public NotificationType getType()
   {
      return type;
   }

   public TypedProperties getProperties()
   {
      return properties;
   }

   private String uid;

   public String getUID()
   {
      return uid;
   }
   
   @Override
   public String toString()
   {
      return "Notification[uid=" + uid + ", type=" + type + ", properties=" + properties + "]";
   }
}
