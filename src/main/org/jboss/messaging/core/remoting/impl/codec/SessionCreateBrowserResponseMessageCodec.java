/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.core.remoting.impl.codec;

import static org.jboss.messaging.core.remoting.impl.wireformat.PacketType.SESS_CREATEBROWSER_RESP;

import org.jboss.messaging.core.remoting.impl.wireformat.SessionCreateBrowserResponseMessage;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>.
 */
public class SessionCreateBrowserResponseMessageCodec extends
      AbstractPacketCodec<SessionCreateBrowserResponseMessage>
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public SessionCreateBrowserResponseMessageCodec()
   {
      super(SESS_CREATEBROWSER_RESP);
   }

   // Public --------------------------------------------------------

   // AbstractPacketCodec overrides ---------------------------------

   @Override
   protected void encodeBody(SessionCreateBrowserResponseMessage response,
         RemotingBuffer out) throws Exception
   {
      long browserID = response.getBrowserTargetID();

      out.putInt(LONG_LENGTH);
      out.putLong(browserID);
   }

   @Override
   protected SessionCreateBrowserResponseMessage decodeBody(RemotingBuffer in)
         throws Exception
   {
      int bodyLength = in.getInt();
      if (in.remaining() < bodyLength)
      {
         return null;
      }

      long browserID = in.getLong();

      return new SessionCreateBrowserResponseMessage(browserID);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
