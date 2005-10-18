/*
 * JBoss, the OpenSource J2EE webOS
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.messaging.jms.perf;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 */
public class FillJob extends BaseJob
{
   private static final long serialVersionUID = 339586193389055268L;

   private static final Logger log = Logger.getLogger(FillJob.class);
   
   protected int numMessages;
   
   protected int deliveryMode;
   
   protected int msgSize;
   
   protected MessageFactory mf;
   
   public String getName()
   {
      return "Fill Job";
   }
   
   public Object getResult()
   {
      return null;
   }
   
   public void logInfo()
   {
      super.logInfo();
      log.info("numMessages: " + this.numMessages);
      log.info("Delivery mode: " + this.deliveryMode);
      log.info("Message size: " + this.msgSize);
      log.info("Message type: " + this.mf.getClass().getName());
   }

   public void run()
   {
            
      Connection conn = null;
      
      try
      {
         log.info("==============Running job:" + this.getName());
         
         super.setup();
         
         conn = cf.createConnection();
         
         long count = 0;
         
         Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
         
         MessageProducer prod = sess.createProducer(dest);
         prod.setDeliveryMode(deliveryMode);
         
         for (int i = 0; i < numMessages; i++)
         {
            Message m = mf.getMessage(sess, msgSize);
            prod.send(m);
            count++;
         }
           
         log.info("==========================Finished running job");
           
      }
      catch (Exception e)
      {
         log.error("Failed to fill destination", e);
         failed = true;
      }
      finally
      {
         if (conn != null)
         {
            try
            {
               conn.close();
            }
            catch (Exception e)
            {
               log.error("Failed to close connection", e);
               failed = true;
            }
         }
      }

   } 
   
   public FillJob(String serverURL, String destName, int numMessages, int messageSize, MessageFactory mf,
         int deliveryMode)
   {
      super(serverURL, destName);
      this.numMessages = numMessages;
      this.mf = mf;
      this.deliveryMode = deliveryMode;
      
   }
      

   /**
    * Set the deliveryMode.
    * 
    * @param deliveryMode The deliveryMode to set.
    */
   public void setDeliveryMode(int deliveryMode)
   {
      this.deliveryMode = deliveryMode;
   }



   /**
    * Set the mf.
    * 
    * @param mf The mf to set.
    */
   public void setMf(MessageFactory mf)
   {
      this.mf = mf;
   }



   /**
    * Set the msgSize.
    * 
    * @param msgSize The msgSize to set.
    */
   public void setMsgSize(int msgSize)
   {
      this.msgSize = msgSize;
   }



   /**
    * Set the numMessages.
    * 
    * @param numMessages The numMessages to set.
    */
   public void setNumMessages(int numMessages)
   {
      this.numMessages = numMessages;
   }

}