/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.destination;

import javax.jms.Topic;
import javax.jms.JMSException;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 */
public class JBossTopic extends JBossDestination implements Topic
{
   // Constants -----------------------------------------------------

   private static final long serialVersionUID = 3257845497845724981L;

   // Static --------------------------------------------------------
   
   // Attributes ----------------------------------------------------
   
   // Constructors --------------------------------------------------

   public JBossTopic(String name)
   {
      super(name);
   }

   // JBossDestination overrides ------------------------------------

   public boolean isTopic()
   {
      return true;
   }

   public boolean isQueue()
   {
      return false;
   }

   // Topic implementation ------------------------------------------

   public String getTopicName() throws JMSException
   {
      return getName();
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------   
}
