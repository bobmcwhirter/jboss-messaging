/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.jms.server.endpoint;


import javax.jms.JMSException;

import org.jboss.jms.client.Closeable;
import org.jboss.jms.delegate.BrowserDelegate;
import org.jboss.jms.delegate.ConsumerDelegate;
import org.jboss.jms.delegate.ProducerDelegate;
import org.jboss.jms.destination.JBossDestination;
import org.jboss.jms.destination.JBossQueue;
import org.jboss.jms.destination.JBossTopic;

/**
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * 
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public interface SessionEndpoint extends Closeable
{
   ProducerDelegate createProducerDelegate(JBossDestination destination) throws JMSException;

   ConsumerDelegate createConsumerDelegate(JBossDestination destination, String selector,
                                           boolean noLocal, String subscriptionName,
                                           boolean connectionConsumer) throws JMSException;   
   
   BrowserDelegate createBrowserDelegate(JBossDestination queue, String messageSelector)
      throws JMSException;

   /**
    * Creates a queue identity given a Queue name. Does NOT create the physical queue. The physical
    * creation of queues is an administrative task and is not to be initiated by the JMS API, with
    * the exception of temporary queues.
    */
   JBossQueue createQueue(String queueName) throws JMSException;

   /**
    * Creates a topic identity given a Queue name. Does NOT create the physical topic. The physical
    * creation of topics is an administrative task and is not to be initiated by the JMS API, with
    * the exception of temporary topics.
    */
   JBossTopic createTopic(String topicName) throws JMSException;

   /**
    * Acknowledges the session
    */ 
   void acknowledge() throws JMSException;
   
   void cancelDeliveries() throws JMSException;
   
   /**
    * Add a temporary destination.
    */
   void addTemporaryDestination(JBossDestination destination) throws JMSException;
   
   /**
    * Delete a temporary destination
    */
   void deleteTemporaryDestination(JBossDestination destination) throws JMSException;
   
   /**
    * Unsubscribe the client from the durable subscription
    * specified by subscriptionName
    * 
    * @param subscriptionName the Name of the durable subscription to unsubscribe from
    * @throws JMSException if the unsubscribe fails
    */
   void unsubscribe(String subscriptionName) throws JMSException;
     
}

