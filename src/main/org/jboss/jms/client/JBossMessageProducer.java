/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.client;

import org.jboss.jms.delegate.ProducerDelegate;
import org.jboss.jms.message.JBossBytesMessage;
import org.jboss.jms.message.JBossMessage;
import org.jboss.logging.Logger;

import javax.jms.MessageProducer;
import javax.jms.JMSException;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.DeliveryMode;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.QueueSender;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:tim.l.fox@gmail.com">Tim Fox</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
class JBossMessageProducer implements MessageProducer, QueueSender, TopicPublisher
{
   // Constants -----------------------------------------------------  
   
   // Static --------------------------------------------------------
   
   private static final Logger log = Logger.getLogger(JBossMessageProducer.class);
   
   // Attributes ----------------------------------------------------
   
   protected ProducerDelegate delegate;
   
   protected int deliveryMode;
   protected boolean isMessageIDDisabled;
   protected boolean isTimestampDisabled;
   protected int priority;
   protected long timeToLive;
   
   protected Destination destination;
   
   // Constructors --------------------------------------------------
   
   public JBossMessageProducer(ProducerDelegate delegate, Destination destination)
   {      
      this.delegate = delegate;
      this.destination = destination;
      deliveryMode = DeliveryMode.PERSISTENT;
      isMessageIDDisabled = false;
      isTimestampDisabled = false;
      timeToLive = 0l;
      priority = 4;
   }
   
   // MessageProducer implementation --------------------------------
   
   public void setDisableMessageID(boolean value) throws JMSException
   {
      log.warn("JBoss Messaging does not support disabling message ID generation");
   }
   
   public boolean getDisableMessageID() throws JMSException
   {
      return isMessageIDDisabled;
   }
   
   public void setDisableMessageTimestamp(boolean value) throws JMSException
   {
      isTimestampDisabled = value;
   }
   
   public boolean getDisableMessageTimestamp() throws JMSException
   {
      return isTimestampDisabled;
   }
   
   public void setDeliveryMode(int deliveryMode) throws JMSException
   {
      this.deliveryMode = deliveryMode;
   }
   
   public int getDeliveryMode() throws JMSException
   {
      return deliveryMode;
   }
   
   public void setPriority(int defaultPriority) throws JMSException
   {
      priority = defaultPriority;
   }
   
   public int getPriority() throws JMSException
   {
      return priority;
   }
   
   public void setTimeToLive(long timeToLive) throws JMSException
   {
      this.timeToLive = timeToLive;
   }
   
   public long getTimeToLive() throws JMSException
   {
      return timeToLive;
   }
   
   public Destination getDestination() throws JMSException
   {
      return destination;
   }
   
   public void close() throws JMSException
   {
      // Don't need to do anything
   }
   
   public void send(Message message) throws JMSException
   {
      // by default the message never expires
      send(message, this.deliveryMode, this.priority, this.timeToLive);
   }
   
   /**
    * @param timeToLive - 0 means never expire.
    */
   public void send(Message message, int deliveryMode, int priority, long timeToLive)
      throws JMSException
   { 
      send(destination, message, deliveryMode, priority, timeToLive);
   }
   
   public void send(Destination destination, Message message) throws JMSException
   {      
      send(destination, message, this.deliveryMode, this.priority, this.timeToLive);
   }
   
   public void send(Destination destination,
                    Message message,
                    int deliveryMode,
                    int priority,
                    long timeToLive) throws JMSException
   {
      configure(message, deliveryMode, priority, timeToLive, destination);
      delegate.send(message);
   }
   
   // TopicPublisher Implementation
   //--------------------------------------- 
   
   public Topic getTopic() throws JMSException
   {
      return (Topic)destination;
   }
   
   public void publish(Message message) throws JMSException
   {
      send(message);
   }
   
   public void publish(Topic topic, Message message) throws JMSException
   {
      send(topic, message);
   }
   
   public void publish(Message message, int deliveryMode, int priority, long timeToLive)
      throws JMSException
   {
      send(message, deliveryMode, priority, timeToLive);
   }
   
   public void publish(Topic topic, Message message, int deliveryMode,
                       int priority, long timeToLive) throws JMSException
   {
      send(topic, message, deliveryMode, priority, timeToLive);
   }
   
   // QueueSender Implementation
   //---------------------------------------
   public void send(Queue queue, Message message) throws JMSException
   {
      send(queue, message);
   }
   
   public void send(Queue queue, Message message, int deliveryMode, int priority,
                    long timeToLive) throws JMSException
   {
      send(queue, message, deliveryMode, priority, timeToLive);
   }
   
   public Queue getQueue() throws JMSException
   {
      return (Queue)destination;
   }
   
   // Public --------------------------------------------------------
   
   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   /**
    * Set the headers.
    */
   protected void configure(Message m, int deliveryMode, int priority,
                            long timeToLive, Destination dest) throws JMSException
   {   	   	      
      if (log.isTraceEnabled()) log.trace("In configure()");
      if (m instanceof JBossBytesMessage)
      {
         if (log.isTraceEnabled()) log.trace("Calling reset()");
         ((JBossBytesMessage)m).reset();
      }
      
      ((JBossMessage)m).setPropertiesReadWrite(false);
      
      m.setJMSDeliveryMode(deliveryMode);
      if (isTimestampDisabled)
      {
         m.setJMSTimestamp(0l);
      }
      else
      {
         m.setJMSTimestamp(System.currentTimeMillis());
      }
      
      if (timeToLive == 0)
      {
         m.setJMSExpiration(Long.MAX_VALUE);
      }
      else
      {
         m.setJMSExpiration(System.currentTimeMillis() + timeToLive);
      }
      
      m.setJMSDestination(dest);
   }
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
}
