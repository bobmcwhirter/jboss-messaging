/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.jms.client.api;

import org.jboss.messaging.util.MessagingException;

/**
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 */
public interface ClientConnection
{    
   ClientSession createClientSession(boolean xa, boolean autoCommitSends, boolean autoCommitAcks,
                                     int ackBatchSize) throws MessagingException;

   void start() throws MessagingException;

   void stop() throws MessagingException;

   FailureListener getFailureListener() throws MessagingException;
   
   void setFailureListener(FailureListener listener) throws MessagingException;
  
   void close() throws MessagingException;
   
   boolean isClosed();
}
