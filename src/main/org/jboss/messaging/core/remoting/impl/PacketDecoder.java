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

package org.jboss.messaging.core.remoting.impl;

import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.CREATESESSION;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.CREATESESSION_RESP;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.CREATE_QUEUE;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.DELETE_QUEUE;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.DISCONNECT;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.EXCEPTION;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.NULL_RESPONSE;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.PACKETS_CONFIRMED;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.PING;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.REATTACH_SESSION;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.REATTACH_SESSION_RESP;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.REPLICATE_ACKNOWLEDGE;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.REPLICATE_ADD_REMOTE_CONSUMER;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.REPLICATE_ADD_REMOTE_QUEUE_BINDING;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.REPLICATE_CREATESESSION;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.REPLICATE_REDISTRIBUTION;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.REPLICATE_REMOVE_REMOTE_CONSUMER;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.REPLICATE_REMOVE_REMOTE_QUEUE_BINDING;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.REPLICATE_STARTUP_INFO;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.REPLICATION_RESPONSE;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_ACKNOWLEDGE;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_BINDINGQUERY;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_BINDINGQUERY_RESP;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_CLOSE;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_COMMIT;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_CONSUMER_CLOSE;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_CREATECONSUMER;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_EXPIRED;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_FAILOVER_COMPLETE;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_FLOWTOKEN;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_QUEUEQUERY;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_QUEUEQUERY_RESP;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_RECEIVE_CONTINUATION;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_RECEIVE_MSG;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_REPLICATE_DELIVERY;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_ROLLBACK;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_SEND;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_SEND_CONTINUATION;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_SEND_LARGE;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_START;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_STOP;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_COMMIT;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_END;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_FORGET;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_GET_TIMEOUT;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_GET_TIMEOUT_RESP;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_INDOUBT_XIDS;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_INDOUBT_XIDS_RESP;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_JOIN;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_PREPARE;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_RESP;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_RESUME;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_ROLLBACK;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_SET_TIMEOUT;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_SET_TIMEOUT_RESP;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_START;
import static org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl.SESS_XA_SUSPEND;

import org.jboss.messaging.core.remoting.Packet;
import org.jboss.messaging.core.remoting.impl.wireformat.CreateQueueMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.CreateSessionMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.CreateSessionResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.MessagingExceptionMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.NullResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl;
import org.jboss.messaging.core.remoting.impl.wireformat.PacketsConfirmedMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.Ping;
import org.jboss.messaging.core.remoting.impl.wireformat.ReattachSessionMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.ReattachSessionResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.ReplicateCreateSessionMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.RollbackMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionAcknowledgeMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionBindingQueryMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionBindingQueryResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionCloseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionConsumerCloseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionConsumerFlowCreditMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionCreateConsumerMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionDeleteQueueMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionExpiredMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionFailoverCompleteMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionQueueQueryMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionQueueQueryResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionReceiveContinuationMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionReceiveMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionSendContinuationMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionSendLargeMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionSendMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXACommitMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXAEndMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXAForgetMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXAGetInDoubtXidsResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXAGetTimeoutResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXAJoinMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXAPrepareMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXAResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXAResumeMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXARollbackMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXASetTimeoutMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXASetTimeoutResponseMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.SessionXAStartMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.replication.ReplicateAcknowledgeMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.replication.ReplicateRedistributionMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.replication.ReplicateRemoteBindingAddedMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.replication.ReplicateRemoteBindingRemovedMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.replication.ReplicateRemoteConsumerAddedMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.replication.ReplicateRemoteConsumerRemovedMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.replication.ReplicateStartupInfoMessage;
import org.jboss.messaging.core.remoting.impl.wireformat.replication.SessionReplicateDeliveryMessage;
import org.jboss.messaging.core.remoting.spi.MessagingBuffer;

/**
 * A PacketDecoder
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 *
 */
public class PacketDecoder
{
   public Packet decode(final MessagingBuffer in)
   {
      final byte packetType = in.readByte();

      Packet packet;

      switch (packetType)
      {
         case PING:
         {
            packet = new Ping();
            break;
         }
         case DISCONNECT:
         {
            packet = new PacketImpl(DISCONNECT);
            break;
         }
         case EXCEPTION:
         {
            packet = new MessagingExceptionMessage();
            break;
         }
         case PACKETS_CONFIRMED:
         {
            packet = new PacketsConfirmedMessage();
            break;
         }
         case REPLICATION_RESPONSE:
         {
            packet = new PacketImpl(REPLICATION_RESPONSE);
            break;
         }
         case CREATESESSION:
         {
            packet = new CreateSessionMessage();
            break;
         }
         case REPLICATE_CREATESESSION:
         {
            packet = new ReplicateCreateSessionMessage();
            break;
         }
         case CREATESESSION_RESP:
         {
            packet = new CreateSessionResponseMessage();
            break;
         }
         case REATTACH_SESSION:
         {
            packet = new ReattachSessionMessage();
            break;
         }
         case REATTACH_SESSION_RESP:
         {
            packet = new ReattachSessionResponseMessage();
            break;
         }
         case SESS_FAILOVER_COMPLETE:
         {
            packet = new SessionFailoverCompleteMessage();
            break;
         }
         case SESS_CLOSE:
         {
            packet = new SessionCloseMessage();
            break;
         }
         case SESS_CREATECONSUMER:
         {
            packet = new SessionCreateConsumerMessage();
            break;
         }
         case SESS_ACKNOWLEDGE:
         {
            packet = new SessionAcknowledgeMessage();
            break;
         }
         case SESS_EXPIRED:
         {
            packet = new SessionExpiredMessage();
            break;
         }
         case SESS_COMMIT:
         {
            packet = new PacketImpl(PacketImpl.SESS_COMMIT);
            break;
         }
         case SESS_ROLLBACK:
         {
            packet = new RollbackMessage();
            break;
         }
         case SESS_QUEUEQUERY:
         {
            packet = new SessionQueueQueryMessage();
            break;
         }
         case SESS_QUEUEQUERY_RESP:
         {
            packet = new SessionQueueQueryResponseMessage();
            break;
         }
         case CREATE_QUEUE:
         {
            packet = new CreateQueueMessage();
            break;
         }
         case DELETE_QUEUE:
         {
            packet = new SessionDeleteQueueMessage();
            break;
         }
         case SESS_BINDINGQUERY:
         {
            packet = new SessionBindingQueryMessage();
            break;
         }
         case SESS_BINDINGQUERY_RESP:
         {
            packet = new SessionBindingQueryResponseMessage();
            break;
         }
         case SESS_XA_START:
         {
            packet = new SessionXAStartMessage();
            break;
         }
         case SESS_XA_END:
         {
            packet = new SessionXAEndMessage();
            break;
         }
         case SESS_XA_COMMIT:
         {
            packet = new SessionXACommitMessage();
            break;
         }
         case SESS_XA_PREPARE:
         {
            packet = new SessionXAPrepareMessage();
            break;
         }
         case SESS_XA_RESP:
         {
            packet = new SessionXAResponseMessage();
            break;
         }
         case SESS_XA_ROLLBACK:
         {
            packet = new SessionXARollbackMessage();
            break;
         }
         case SESS_XA_JOIN:
         {
            packet = new SessionXAJoinMessage();
            break;
         }
         case SESS_XA_SUSPEND:
         {
            packet = new PacketImpl(PacketImpl.SESS_XA_SUSPEND);
            break;
         }
         case SESS_XA_RESUME:
         {
            packet = new SessionXAResumeMessage();
            break;
         }
         case SESS_XA_FORGET:
         {
            packet = new SessionXAForgetMessage();
            break;
         }
         case SESS_XA_INDOUBT_XIDS:
         {
            packet = new PacketImpl(PacketImpl.SESS_XA_INDOUBT_XIDS);
            break;
         }
         case SESS_XA_INDOUBT_XIDS_RESP:
         {
            packet = new SessionXAGetInDoubtXidsResponseMessage();
            break;
         }
         case SESS_XA_SET_TIMEOUT:
         {
            packet = new SessionXASetTimeoutMessage();
            break;
         }
         case SESS_XA_SET_TIMEOUT_RESP:
         {
            packet = new SessionXASetTimeoutResponseMessage();
            break;
         }
         case SESS_XA_GET_TIMEOUT:
         {
            packet = new PacketImpl(PacketImpl.SESS_XA_GET_TIMEOUT);
            break;
         }
         case SESS_XA_GET_TIMEOUT_RESP:
         {
            packet = new SessionXAGetTimeoutResponseMessage();
            break;
         }
         case SESS_START:
         {
            packet = new PacketImpl(PacketImpl.SESS_START);
            break;
         }
         case SESS_STOP:
         {
            packet = new PacketImpl(PacketImpl.SESS_STOP);
            break;
         }
         case SESS_FLOWTOKEN:
         {
            packet = new SessionConsumerFlowCreditMessage();
            break;
         }
         case SESS_SEND:
         {
            packet = new SessionSendMessage();
            break;
         }
         case SESS_SEND_LARGE:
         {
            packet = new SessionSendLargeMessage();
            break;
         }
         case SESS_RECEIVE_MSG:
         {
            packet = new SessionReceiveMessage();
            break;
         }
         case SESS_CONSUMER_CLOSE:
         {
            packet = new SessionConsumerCloseMessage();
            break;
         }
         case NULL_RESPONSE:
         {
            packet = new NullResponseMessage();
            break;
         }
         case SESS_RECEIVE_CONTINUATION:
         {
            packet = new SessionReceiveContinuationMessage();
            break;
         }
         case SESS_SEND_CONTINUATION:
         {
            packet = new SessionSendContinuationMessage();
            break;
         }
         case REPLICATE_ADD_REMOTE_QUEUE_BINDING:
         {
            packet = new ReplicateRemoteBindingAddedMessage();
            break;
         }
         case REPLICATE_REMOVE_REMOTE_QUEUE_BINDING:
         {
            packet = new ReplicateRemoteBindingRemovedMessage();
            break;
         }
         case REPLICATE_ADD_REMOTE_CONSUMER:
         {
            packet = new ReplicateRemoteConsumerAddedMessage();
            break;
         }
         case REPLICATE_REMOVE_REMOTE_CONSUMER:
         {
            packet = new ReplicateRemoteConsumerRemovedMessage();
            break;
         }
         case SESS_REPLICATE_DELIVERY:
         {
            packet = new SessionReplicateDeliveryMessage();
            break;
         }
         case REPLICATE_STARTUP_INFO:
         {
            packet = new ReplicateStartupInfoMessage();
            break;
         }
         case REPLICATE_ACKNOWLEDGE:
         {
            packet = new ReplicateAcknowledgeMessage();
            break;
         }
         case REPLICATE_REDISTRIBUTION:
         {
            packet = new ReplicateRedistributionMessage();
            break;
         }
         default:
         {
            throw new IllegalArgumentException("Invalid type: " + packetType);
         }
      }

      packet.decode(in);

      return packet;
   }

}
