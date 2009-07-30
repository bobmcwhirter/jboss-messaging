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

package org.jboss.messaging.core.management;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 */
public enum NotificationType
{
   BINDING_ADDED(0),
   BINDING_REMOVED(1),
   CONSUMER_CREATED(2),
   CONSUMER_CLOSED(3),
   SECURITY_AUTHENTICATION_VIOLATION(6),
   SECURITY_PERMISSION_VIOLATION(7),
   DISCOVERY_GROUP_STARTED(8),
   DISCOVERY_GROUP_STOPPED(9),
   BROADCAST_GROUP_STARTED(10),
   BROADCAST_GROUP_STOPPED(11),
   BRIDGE_STARTED(12),
   BRIDGE_STOPPED(13);

   private final int value;

   private NotificationType(int value)
   {
      this.value = value;
   }

   public int intValue()
   {
      return value;
   }
}