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

package org.jboss.messaging.core.settings.impl;

import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.server.Distributor;
import org.jboss.messaging.core.server.impl.RoundRobinDistributor;
import org.jboss.messaging.core.settings.Mergeable;
import org.jboss.messaging.utils.SimpleString;

/**
 * Configuration settings that are applied on the address level
 * 
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="tim.fox@jboss.com">Tim Fox</a>
 */
public class AddressSettings implements Mergeable<AddressSettings>
{
   private static Logger log = Logger.getLogger(AddressSettings.class);

   /**
    * defaults used if null, this allows merging
    */
   public static final Class<?> DEFAULT_DISTRIBUTION_POLICY_CLASS = new RoundRobinDistributor().getClass();

   public static final int DEFAULT_MAX_SIZE_BYTES = -1;

   public static final boolean DEFAULT_DROP_MESSAGES_WHEN_FULL = false;

   public static final int DEFAULT_PAGE_SIZE = 10 * 1024 * 1024;

   public static final int DEFAULT_MAX_DELIVERY_ATTEMPTS = 10;

   public static final int DEFAULT_MESSAGE_COUNTER_HISTORY_DAY_LIMIT = 0;

   public static final long DEFAULT_REDELIVER_DELAY = 0L;

   public static final boolean DEFAULT_LAST_VALUE_QUEUE = false;

   public static final long DEFAULT_REDISTRIBUTION_DELAY = -1;

   private Integer maxSizeBytes = null;

   private Integer pageSizeBytes = null;

   private Boolean dropMessagesWhenFull = null;

   private String distributionPolicyClass = null;

   private Integer maxDeliveryAttempts = null;

   private Integer messageCounterHistoryDayLimit = null;

   private Long redeliveryDelay = null;

   private SimpleString deadLetterAddress = null;

   private SimpleString expiryAddress = null;

   private Boolean lastValueQueue = null;

   private Long redistributionDelay = null;

   public boolean isLastValueQueue()
   {
      return lastValueQueue != null ? lastValueQueue : DEFAULT_LAST_VALUE_QUEUE;
   }

   public void setLastValueQueue(final boolean lastValueQueue)
   {
      this.lastValueQueue = lastValueQueue;
   }

   public int getPageSizeBytes()
   {
      return pageSizeBytes != null ? pageSizeBytes : DEFAULT_PAGE_SIZE;
   }

   public boolean isDropMessagesWhenFull()
   {
      return dropMessagesWhenFull != null ? dropMessagesWhenFull : DEFAULT_DROP_MESSAGES_WHEN_FULL;
   }

   public void setDropMessagesWhenFull(final boolean value)
   {
      dropMessagesWhenFull = value;
   }

   public void setPageSizeBytes(final int pageSize)
   {
      pageSizeBytes = pageSize;
   }

   public int getMaxSizeBytes()
   {
      return maxSizeBytes != null ? maxSizeBytes : DEFAULT_MAX_SIZE_BYTES;
   }

   public void setMaxSizeBytes(final int maxSizeBytes)
   {
      this.maxSizeBytes = maxSizeBytes;
   }

   public int getMaxDeliveryAttempts()
   {
      return maxDeliveryAttempts != null ? maxDeliveryAttempts : DEFAULT_MAX_DELIVERY_ATTEMPTS;
   }

   public void setMaxDeliveryAttempts(final int maxDeliveryAttempts)
   {
      this.maxDeliveryAttempts = maxDeliveryAttempts;
   }

   public int getMessageCounterHistoryDayLimit()
   {
      return messageCounterHistoryDayLimit != null ? messageCounterHistoryDayLimit
                                                  : DEFAULT_MESSAGE_COUNTER_HISTORY_DAY_LIMIT;
   }

   public void setMessageCounterHistoryDayLimit(final int messageCounterHistoryDayLimit)
   {
      this.messageCounterHistoryDayLimit = messageCounterHistoryDayLimit;
   }

   public long getRedeliveryDelay()
   {
      return redeliveryDelay != null ? redeliveryDelay : DEFAULT_REDELIVER_DELAY;
   }

   public void setRedeliveryDelay(final long redeliveryDelay)
   {
      this.redeliveryDelay = redeliveryDelay;
   }

   public String getDistributionPolicyClass()
   {
      return distributionPolicyClass;
   }

   public void setDistributionPolicyClass(final String distributionPolicyClass)
   {
      this.distributionPolicyClass = distributionPolicyClass;
   }

   public SimpleString getDeadLetterAddress()
   {
      return deadLetterAddress;
   }

   public void setDeadLetterAddress(final SimpleString deadLetterAddress)
   {
      this.deadLetterAddress = deadLetterAddress;
   }

   public SimpleString getExpiryAddress()
   {
      return expiryAddress;
   }

   public void setExpiryAddress(final SimpleString expiryAddress)
   {
      this.expiryAddress = expiryAddress;
   }

   public Distributor getDistributionPolicy()
   {
      try
      {
         if (distributionPolicyClass != null)
         {
            return (Distributor)getClass().getClassLoader().loadClass(distributionPolicyClass).newInstance();
         }
         else
         {
            return (Distributor)DEFAULT_DISTRIBUTION_POLICY_CLASS.newInstance();
         }
      }
      catch (Exception e)
      {
         throw new IllegalArgumentException("Error instantiating distribution policy '" + e + " '");
      }
   }

   public long getRedistributionDelay()
   {
      return redistributionDelay != null ? redistributionDelay : DEFAULT_REDISTRIBUTION_DELAY;
   }

   public void setRedistributionDelay(final long redistributionDelay)
   {
      this.redistributionDelay = redistributionDelay;
   }

   /**
    * merge 2 objects in to 1
    * @param merged
    */
   public void merge(final AddressSettings merged)
   {
      if (maxDeliveryAttempts == null)
      {
         maxDeliveryAttempts = merged.maxDeliveryAttempts;
      }
      if (dropMessagesWhenFull == null)
      {
         dropMessagesWhenFull = merged.dropMessagesWhenFull;
      }
      if (maxSizeBytes == null)
      {
         maxSizeBytes = merged.maxSizeBytes;
      }
      if (pageSizeBytes == null)
      {
         pageSizeBytes = merged.getPageSizeBytes();
      }
      if (messageCounterHistoryDayLimit == null)
      {
         messageCounterHistoryDayLimit = merged.messageCounterHistoryDayLimit;
      }
      if (redeliveryDelay == null)
      {
         redeliveryDelay = merged.redeliveryDelay;
      }
      if (distributionPolicyClass == null)
      {
         distributionPolicyClass = merged.distributionPolicyClass;
      }
      if (deadLetterAddress == null)
      {
         deadLetterAddress = merged.deadLetterAddress;
      }
      if (expiryAddress == null)
      {
         expiryAddress = merged.expiryAddress;
      }
      if (redistributionDelay == null)
      {
         redistributionDelay = merged.redistributionDelay;
      }
   }

}
