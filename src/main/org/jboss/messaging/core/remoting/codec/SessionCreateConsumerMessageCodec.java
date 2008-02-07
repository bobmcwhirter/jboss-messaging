/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.core.remoting.codec;

import static org.jboss.messaging.core.remoting.wireformat.PacketType.SESS_CREATECONSUMER;

import org.jboss.messaging.core.remoting.wireformat.SessionCreateConsumerMessage;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>.
 */
public class SessionCreateConsumerMessageCodec extends
      AbstractPacketCodec<SessionCreateConsumerMessage>
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public SessionCreateConsumerMessageCodec()
   {
      super(SESS_CREATECONSUMER);
   }

   // Public --------------------------------------------------------

   // AbstractPacketCodec overrides ---------------------------------

   @Override
   protected void encodeBody(SessionCreateConsumerMessage request, RemotingBuffer out) throws Exception
   {
      String queueName = request.getQueueName();
      String filterString = request.getFilterString();
      boolean noLocal = request.isNoLocal();
      boolean autoDelete = request.isAutoDeleteQueue();

      int bodyLength = sizeof(queueName) + sizeof(filterString) + 2;

      out.putInt(bodyLength);
      out.putNullableString(queueName);
      out.putNullableString(filterString);
      out.putBoolean(noLocal);
      out.putBoolean(autoDelete);
   }

   @Override
   protected SessionCreateConsumerMessage decodeBody(RemotingBuffer in)
         throws Exception
   {
      int bodyLength = in.getInt();
      if (in.remaining() < bodyLength)
      {
         return null;
      }

      String queueName = in.getNullableString();
      String filterString = in.getNullableString();
      boolean noLocal = in.getBoolean();
      boolean autoDelete = in.getBoolean();
 
      return new SessionCreateConsumerMessage(queueName, filterString, noLocal, autoDelete);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
