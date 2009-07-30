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

package org.jboss.messaging.tests.integration;

import java.util.ArrayList;
import java.util.List;

import org.jboss.messaging.core.management.Notification;
import org.jboss.messaging.core.management.NotificationListener;
import org.jboss.messaging.core.management.NotificationService;

/**
 * A SimpleNotificationService
 *
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil
 *
 *
 */
public class SimpleNotificationService implements NotificationService
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private final List<NotificationListener> listeners = new ArrayList<NotificationListener>();

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // NotificationService implementation ----------------------------

   public void addNotificationListener(NotificationListener listener)
   {
      listeners.add(listener);
   }

   public void enableNotifications(boolean enable)
   {
   }

   public void removeNotificationListener(NotificationListener listener)
   {
      listeners.remove(listener);
   }

   public void sendNotification(Notification notification) throws Exception
   {
      for (NotificationListener listener : listeners)
      {
         listener.onNotification(notification);
      }
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

   public static class Listener implements NotificationListener
   {

      private final List<Notification> notifications = new ArrayList<Notification>();

      public void onNotification(Notification notification)
      {
         System.out.println(">>>>>>>>" + notification);
         notifications.add(notification);
      }

      public List<Notification> getNotifications()
      {
         return notifications;
      }

   }
}
