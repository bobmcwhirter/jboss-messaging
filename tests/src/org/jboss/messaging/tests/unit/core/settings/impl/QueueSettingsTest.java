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

package org.jboss.messaging.tests.unit.core.settings.impl;

import junit.framework.TestCase;

import org.jboss.messaging.core.settings.impl.AddressSettings;
import org.jboss.messaging.util.SimpleString;

/**
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 */
public class QueueSettingsTest extends TestCase
{
   public void testDefaults()
   {
      AddressSettings addressSettings = new AddressSettings();
      assertEquals(addressSettings.getDistributionPolicy().getClass(), AddressSettings.DEFAULT_DISTRIBUTION_POLICY_CLASS);
      assertEquals(addressSettings.getDistributionPolicyClass(), null);
      assertEquals(addressSettings.getDeadLetterAddress(), null);
      assertEquals(addressSettings.isClustered(), Boolean.valueOf(false));
      assertEquals(addressSettings.getExpiryAddress(), null);
      assertEquals(addressSettings.getMaxDeliveryAttempts(), AddressSettings.DEFAULT_MAX_DELIVERY_ATTEMPTS);
      assertEquals(addressSettings.getMaxSizeBytes(), AddressSettings.DEFAULT_MAX_SIZE_BYTES);
      assertEquals(addressSettings.getPageSizeBytes(), null);
      assertEquals(addressSettings.getMessageCounterHistoryDayLimit(), AddressSettings.DEFAULT_MESSAGE_COUNTER_HISTORY_DAY_LIMIT);
      assertEquals(addressSettings.getRedeliveryDelay(), AddressSettings.DEFAULT_REDELIVER_DELAY);

   }

   public void testSingleMerge()
   {
      AddressSettings addressSettings = new AddressSettings();
      AddressSettings addressSettingsToMerge = new AddressSettings();
      addressSettingsToMerge.setClustered(true);
      SimpleString DLQ = new SimpleString("testDLQ");
      SimpleString exp = new SimpleString("testExpiryQueue");
      addressSettingsToMerge.setDeadLetterAddress(DLQ);
      addressSettingsToMerge.setExpiryAddress(exp);
      addressSettingsToMerge.setMaxDeliveryAttempts(1000);
      addressSettingsToMerge.setDropMessagesWhenFull(true);
      addressSettingsToMerge.setMaxSizeBytes(1001);
      addressSettingsToMerge.setMessageCounterHistoryDayLimit(1002);
      addressSettingsToMerge.setRedeliveryDelay((long)1003);
      addressSettingsToMerge.setPageSizeBytes(1004);
      addressSettings.merge(addressSettingsToMerge);
      assertEquals(addressSettings.getDistributionPolicy().getClass(), AddressSettings.DEFAULT_DISTRIBUTION_POLICY_CLASS);
      assertEquals(addressSettings.getDistributionPolicyClass(), null);
      assertEquals(addressSettings.isClustered(), Boolean.valueOf(true));
      assertEquals(addressSettings.getDeadLetterAddress(), DLQ);
      assertEquals(addressSettings.getExpiryAddress(), exp);
      assertEquals(addressSettings.getMaxDeliveryAttempts(), Integer.valueOf(1000));
      assertEquals(addressSettings.getMaxSizeBytes(), Integer.valueOf(1001));
      assertEquals(addressSettings.getMessageCounterHistoryDayLimit(), Integer.valueOf(1002));
      assertEquals(addressSettings.getRedeliveryDelay(), Long.valueOf(1003));
      assertEquals(addressSettings.getPageSizeBytes(), (Integer)1004);
      assertTrue(addressSettings.isDropMessagesWhenFull());
   }

   public void testMultipleMerge()
   {
      AddressSettings addressSettings = new AddressSettings();
      AddressSettings addressSettingsToMerge = new AddressSettings();
      addressSettingsToMerge.setClustered(true);
      SimpleString DLQ = new SimpleString("testDLQ");
      SimpleString exp = new SimpleString("testExpiryQueue");
      addressSettingsToMerge.setDeadLetterAddress(DLQ);
      addressSettingsToMerge.setExpiryAddress(exp);
      addressSettingsToMerge.setMaxDeliveryAttempts(1000);
      addressSettingsToMerge.setMaxSizeBytes(1001);
      addressSettingsToMerge.setMessageCounterHistoryDayLimit(1002);
      addressSettings.merge(addressSettingsToMerge);

      AddressSettings addressSettingsToMerge2 = new AddressSettings();
      addressSettingsToMerge2.setClustered(true);
      SimpleString exp2 = new SimpleString("testExpiryQueue2");
      addressSettingsToMerge2.setExpiryAddress(exp2);
      addressSettingsToMerge2.setMaxSizeBytes(2001);
      addressSettingsToMerge2.setRedeliveryDelay((long)2003);
      addressSettings.merge(addressSettingsToMerge2);

      assertEquals(addressSettings.getDistributionPolicy().getClass(), AddressSettings.DEFAULT_DISTRIBUTION_POLICY_CLASS);
      assertEquals(addressSettings.getDistributionPolicyClass(), null);
      assertEquals(addressSettings.isClustered(), Boolean.valueOf(true));
      assertEquals(addressSettings.getDeadLetterAddress(), DLQ);
      assertEquals(addressSettings.getExpiryAddress(), exp);
      assertEquals(addressSettings.getMaxDeliveryAttempts(), Integer.valueOf(1000));
      assertEquals(addressSettings.getMaxSizeBytes(), Integer.valueOf(1001));
      assertEquals(addressSettings.getMessageCounterHistoryDayLimit(), Integer.valueOf(1002));
      assertEquals(addressSettings.getRedeliveryDelay(), Long.valueOf(2003));
   }

   public void testMultipleMergeAll()
   {
      AddressSettings addressSettings = new AddressSettings();
      AddressSettings addressSettingsToMerge = new AddressSettings();
      addressSettingsToMerge.setClustered(true);
      SimpleString DLQ = new SimpleString("testDLQ");
      SimpleString exp = new SimpleString("testExpiryQueue");
      addressSettingsToMerge.setDeadLetterAddress(DLQ);
      addressSettingsToMerge.setExpiryAddress(exp);
      addressSettingsToMerge.setMaxSizeBytes(1001);
      addressSettingsToMerge.setRedeliveryDelay((long)1003);
      addressSettings.merge(addressSettingsToMerge);

      AddressSettings addressSettingsToMerge2 = new AddressSettings();
      addressSettingsToMerge2.setClustered(false);
      SimpleString exp2 = new SimpleString("testExpiryQueue2");
      SimpleString DLQ2 = new SimpleString("testDlq2");
      addressSettingsToMerge2.setExpiryAddress(exp2);
      addressSettingsToMerge2.setDeadLetterAddress(DLQ2);
      addressSettingsToMerge2.setMaxDeliveryAttempts(2000);
      addressSettingsToMerge2.setMaxSizeBytes(2001);
      addressSettingsToMerge2.setMessageCounterHistoryDayLimit(2002);
      addressSettingsToMerge2.setRedeliveryDelay((long)2003);
      addressSettings.merge(addressSettingsToMerge2);

      assertEquals(addressSettings.getDistributionPolicy().getClass(), AddressSettings.DEFAULT_DISTRIBUTION_POLICY_CLASS);
      assertEquals(addressSettings.getDistributionPolicyClass(), null);
      assertEquals(addressSettings.isClustered(), Boolean.valueOf(true));
      assertEquals(addressSettings.getDeadLetterAddress(), DLQ);
      assertEquals(addressSettings.getExpiryAddress(), exp);
      assertEquals(addressSettings.getMaxDeliveryAttempts(), Integer.valueOf(2000));
      assertEquals(addressSettings.getMaxSizeBytes(), Integer.valueOf(1001));
      assertEquals(addressSettings.getMessageCounterHistoryDayLimit(), Integer.valueOf(2002));
      assertEquals(addressSettings.getRedeliveryDelay(), Long.valueOf(1003));
   }
}
