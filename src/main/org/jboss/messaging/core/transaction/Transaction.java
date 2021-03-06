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

package org.jboss.messaging.core.transaction;

import org.jboss.messaging.core.exception.MessagingException;
import org.jboss.messaging.core.paging.PageTransactionInfo;
import org.jboss.messaging.core.server.MessageReference;
import org.jboss.messaging.core.server.ServerMessage;
import org.jboss.messaging.core.settings.HierarchicalRepository;
import org.jboss.messaging.core.settings.impl.QueueSettings;

import javax.transaction.xa.Xid;
import java.util.List;

/**
 * A JBoss Messaging internal transaction
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:andy.taylor@jboss.org>Andy Taylor</a>
 */
public interface Transaction
{
   void prepare() throws Exception;

   void commit() throws Exception;

   List<MessageReference> rollback(HierarchicalRepository<QueueSettings> queueSettingsRepository) throws Exception;

   void addMessage(ServerMessage message) throws Exception;

   void addAcknowledgement(MessageReference acknowledgement) throws Exception;
   
   int getAcknowledgementsCount();

   long getID();

   Xid getXid();

   boolean isEmpty();

   void suspend();

   void resume();

   State getState();

   boolean isContainsPersistent();

   void markAsRollbackOnly(MessagingException messagingException);

   void replay(List<MessageReference> messages, List<MessageReference> scheduledMessages, List<MessageReference> acknowledgements, PageTransactionInfo pageTransaction, State prepared) throws Exception;

   void addScheduledMessage(ServerMessage msg, long scheduledDeliveryTime) throws Exception;

   static enum State
   {
      ACTIVE, PREPARED, COMMITTED, ROLLEDBACK, SUSPENDED, ROLLBACK_ONLY;
   }
}
