/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.server.container;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 */
public class LogInterceptor implements Interceptor
{
    // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(LogInterceptor.class);

    // Static --------------------------------------------------------

    // Attributes ----------------------------------------------------

    // Constructors --------------------------------------------------

    // Public --------------------------------------------------------

    // Interceptor implementation ------------------------------------

    public String getName()
    {
        return "LogInterceptor";
    }

    public Object invoke(Invocation invocation) throws Throwable
    {
        return invocation.invokeNext();
    }

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    // Inner classes -------------------------------------------------
}




