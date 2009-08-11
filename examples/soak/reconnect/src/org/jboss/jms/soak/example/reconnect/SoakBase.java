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
package org.jboss.jms.soak.example.reconnect;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

/**
 * 
 * A SoakBase
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 *
 *
 */
public class SoakBase
{
   private static final Logger log = Logger.getLogger(SoakBase.class.getName());

   private static final String DEFAULT_PERF_PROPERTIES_FILE_NAME = "perf.properties";

   public static byte[] randomByteArray(final int length)
   {
      byte[] bytes = new byte[length];

      Random random = new Random();

      for (int i = 0; i < length; i++)
      {
         bytes[i] = Integer.valueOf(random.nextInt()).byteValue();
      }

      return bytes;
   }

   protected static String getPerfFileName(String[] args)
   {
      String fileName;

      if (args.length > 0)
      {
         fileName = args[0];
      }
      else
      {
         fileName = DEFAULT_PERF_PROPERTIES_FILE_NAME;
      }

      return fileName;
   }

   protected static SoakParams getParams(final String fileName) throws Exception
   {
      Properties props = null;

      InputStream is = null;

      try
      {
         is = new FileInputStream(fileName);

         props = new Properties();

         props.load(is);
      }
      finally
      {
         if (is != null)
         {
            is.close();
         }
      }

      int noOfMessages = Integer.valueOf(props.getProperty("num-messages"));
      int noOfWarmupMessages = Integer.valueOf(props.getProperty("num-warmup-messages"));
      int messageSize = Integer.valueOf(props.getProperty("message-size"));
      boolean durable = Boolean.valueOf(props.getProperty("durable"));
      boolean transacted = Boolean.valueOf(props.getProperty("transacted"));
      int batchSize = Integer.valueOf(props.getProperty("batch-size"));
      boolean drainQueue = Boolean.valueOf(props.getProperty("drain-queue"));
      String destinationLookup = props.getProperty("destination-lookup");
      String connectionFactoryLookup = props.getProperty("connection-factory-lookup");
      int throttleRate = Integer.valueOf(props.getProperty("throttle-rate"));
      boolean dupsOK = Boolean.valueOf(props.getProperty("dups-ok-acknowlege"));
      boolean disableMessageID = Boolean.valueOf(props.getProperty("disable-message-id"));
      boolean disableTimestamp = Boolean.valueOf(props.getProperty("disable-message-timestamp"));

      log.info("num-messages: " + noOfMessages);
      log.info("num-warmup-messages: " + noOfWarmupMessages);
      log.info("message-size: " + messageSize);
      log.info("durable: " + durable);
      log.info("transacted: " + transacted);
      log.info("batch-size: " + batchSize);
      log.info("drain-queue: " + drainQueue);
      log.info("throttle-rate: " + throttleRate);
      log.info("connection-factory-lookup: " + connectionFactoryLookup);
      log.info("destination-lookup: " + destinationLookup);
      log.info("disable-message-id: " + disableMessageID);
      log.info("disable-message-timestamp: " + disableTimestamp);
      log.info("dups-ok-acknowledge: " + dupsOK);

      SoakParams perfParams = new SoakParams();
      perfParams.setNoOfMessagesToSend(noOfMessages);
      perfParams.setNoOfWarmupMessages(noOfWarmupMessages);
      perfParams.setMessageSize(messageSize);
      perfParams.setDurable(durable);
      perfParams.setSessionTransacted(transacted);
      perfParams.setBatchSize(batchSize);
      perfParams.setDrainQueue(drainQueue);
      perfParams.setConnectionFactoryLookup(connectionFactoryLookup);
      perfParams.setDestinationLookup(destinationLookup);
      perfParams.setThrottleRate(throttleRate);
      perfParams.setDisableMessageID(disableMessageID);
      perfParams.setDisableTimestamp(disableTimestamp);
      perfParams.setDupsOK(dupsOK);

      return perfParams;
   }
}
