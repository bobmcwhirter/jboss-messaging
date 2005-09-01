/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.core.distributed.pipe;

import org.jboss.messaging.core.Receiver;
import org.jboss.messaging.core.Delivery;
import org.jboss.messaging.core.DeliveryObserver;
import org.jboss.messaging.core.Routable;
import org.jboss.messaging.core.distributed.DistributedQueue;
import org.jboss.messaging.core.distributed.util.RpcServerCall;
import org.jboss.logging.Logger;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.Address;

import java.io.Serializable;

/**
 * The input end of a distributed pipe that synchronously forwards messages to a receiver in a
 * different address space.
 *
 * The output end of the pipe its identified by a JGroups address and the pipe ID. Multiple
 * distributed pipes can share the same DistributedPipeOutput instance (and implicitly the pipeID),
 * as long the input instances are different.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class DistributedPipe implements Receiver
 {
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(DistributedQueue.class);

   private static final long TIMEOUT = 3000;

   // Static --------------------------------------------------------
   
   // Attributes ----------------------------------------------------

   protected Serializable id;
   protected RpcDispatcher dispatcher;
   protected Address outputAddress;

   // Constructors --------------------------------------------------

   public DistributedPipe(Serializable id, RpcDispatcher dispatcher, Address outputAddress)
   {
      this.dispatcher = dispatcher;
      this.outputAddress = outputAddress;
      this.id = id;
      log.debug(this + " created");
   }

   // Receiver implementation ---------------------------------------

   public Delivery handle(DeliveryObserver observer, Routable r)
   {

      // TODO for the time being, this end always makes synchonous calls and always returns "done"
      // TODO deliveries

      // Check if the message was sent remotely; in this case, I must not resend it to avoid
      // endless loops among peers or deadlock on distributed RPC if deadlock detection is not
      // enabled.

      if (r.getHeader(Routable.REMOTE_ROUTABLE) != null)
      {
         return null;
      }

      try
      {
         return (Delivery)call("handle",
                               new Object[] {r},
                               new String[] {"org.jboss.messaging.core.Routable"});
      }
      catch(Throwable e)
      {
         log.error("Remote call handle() on " + id + ":" + outputAddress + " failed", e);
         return null;
      }
   }

   // Public --------------------------------------------------------

   public Address getOutputAddress()
   {
      return outputAddress;
   }

   public String toString()
   {
      StringBuffer sb = new StringBuffer();

      sb.append("Pipe[");
      sb.append(id);
      sb.append(" -> ");
      sb.append(outputAddress);
      sb.append("]");
      return sb.toString();
   }


   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------

   /**
    * Synchronous remote call.
    */
   private Object call(String methodName, Object[] args, String[] argTypes) throws Throwable
   {
      if (outputAddress == null)
      {
         throw new IllegalStateException(this + " has a null output address");
      }

      RpcServerCall rpcServerCall =  new RpcServerCall(id, methodName, args, argTypes);

      // TODO use the timout when I'll change the send() signature or deal with the timeout
      return rpcServerCall.remoteInvoke(dispatcher, outputAddress, TIMEOUT);
   }


   // Inner classes -------------------------------------------------
}
