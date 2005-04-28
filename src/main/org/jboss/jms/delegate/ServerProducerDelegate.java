/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.delegate;

import org.jboss.messaging.core.Receiver;
import org.jboss.messaging.core.Routable;
import org.jboss.messaging.util.NotYetImplementedException;
import org.jboss.logging.Logger;
import org.jboss.util.id.GUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Destination;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 */
public class ServerProducerDelegate implements ProducerDelegate
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(ServerProducerDelegate.class);

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   protected String id;
   //protected Receiver destination;
   /** I need this to set up the JMSDestination header on outgoing messages */
   protected Destination jmsDestination;
   protected ServerSessionDelegate sessionEndpoint;

   // Constructors --------------------------------------------------

   public ServerProducerDelegate(String id, Receiver destination,
                                 Destination jmsDestination, ServerSessionDelegate parent)
   {
      this.id = id;
      //this.destination = destination;
      this.jmsDestination = jmsDestination;
      sessionEndpoint = parent;
   }

   // ProducerDelegate implementation ------------------------
   
   public void close()
      throws JMSException
   {
      //Currently this does nothing
      System.out.println("In ServerProducerDelegate.close()");
   }
   
   public void closing()
      throws JMSException
   {
      //Currently this does nothing
      System.out.println("In ServerProducerDelegate.closing()");
   }

   public void send(Message m)
      throws JMSException
   {
      if (log.isTraceEnabled()) { log.trace("sending message " + m + " to the core"); }

      sessionEndpoint.sendMessage(m);
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
