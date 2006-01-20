/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.server.destination;

/**
 * A deployable JBoss Messaging queue.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class Queue extends DeployableDestinationSupport
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------
   
   // Attributes ----------------------------------------------------

   // Constructors --------------------------------------------------

   // JMX managed attributes ----------------------------------------

   // JMX managed operations ----------------------------------------

   // TODO implement these:

//   int getQueueDepth() throws java.lang.Exception;
//
//   int getScheduledMessageCount() throws java.lang.Exception;
//
//   int getReceiversCount();
//
//   java.util.List listReceivers();
//
//   java.util.List listMessages() throws java.lang.Exception;
//
//   java.util.List listMessages(java.lang.String selector) throws java.lang.Exception;

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   protected boolean isQueue()
   {
      return true;
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
