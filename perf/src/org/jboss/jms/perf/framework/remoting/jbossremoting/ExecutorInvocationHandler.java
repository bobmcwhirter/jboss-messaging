/*
 * JBoss, the OpenSource J2EE webOS
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.jms.perf.framework.remoting.jbossremoting;

import javax.management.MBeanServer;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.jms.perf.framework.remoting.Request;
import org.jboss.jms.perf.framework.remoting.Executor;

/**
 * @author <a href="tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version $Revision$
 *
 * $Id$
 */
public class ExecutorInvocationHandler implements ServerInvocationHandler
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(ExecutorInvocationHandler.class);

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   private Executor delegateExecutor;

   // Constructors --------------------------------------------------

   public ExecutorInvocationHandler(JBossRemotingExecutor remotingExecutor)
   {
      delegateExecutor = new Executor(remotingExecutor);
   }

   // ServerInvocationHandler implementation -----------------------

   public synchronized Object invoke(InvocationRequest invocation) throws Throwable
   {
      Request request = (Request)invocation.getParameter();
      return delegateExecutor.execute(request);
   }

   public void addListener(InvokerCallbackHandler callbackHandler)
   {
      log.debug("addListener(" + callbackHandler + ") ignored");
   }

   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
      log.debug("removeListener(" + callbackHandler + ") ignored");
   }

   public void setMBeanServer(MBeanServer server)
   {
      log.debug("setMBeanServer(" + server + ") ignored");
   }

   public void setInvoker(ServerInvoker invoker)
   {
      log.debug("setInvoker(" + invoker + ") ignored");
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------



}