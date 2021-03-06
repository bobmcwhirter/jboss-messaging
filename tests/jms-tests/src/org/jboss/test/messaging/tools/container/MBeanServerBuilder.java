/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.messaging.tools.container;

import javax.management.MBeanServerDelegate;

//import org.jboss.mx.server.MBeanServerImpl;

/**
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class MBeanServerBuilder extends javax.management.MBeanServerBuilder
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------
   
   // Attributes ----------------------------------------------------

   // Constructors --------------------------------------------------

   public MBeanServerBuilder()
   {
   }

   // MBeanServerBuilder overrides ----------------------------------

   /*public MBeanServer newMBeanServer(String defaultDomain,
                                     MBeanServer outer,
                                     MBeanServerDelegate connectionFactory)
   {
      return new MBeanServerImpl("jboss", outer, connectionFactory);
   }
*/
   public MBeanServerDelegate	newMBeanServerDelegate()
   {
      return new MBeanServerDelegate();
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
