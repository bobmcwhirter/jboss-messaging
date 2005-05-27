/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.core.distributed;

import org.jboss.messaging.core.Routable;
import org.jboss.messaging.core.Acknowledgment;
import org.jboss.messaging.core.util.RpcServerCall;
import org.jboss.messaging.core.util.AcknowledgmentImpl;
import org.jboss.messaging.core.local.SingleOutputChannelSupport;
import org.jboss.logging.Logger;
import org.jgroups.Address;
import org.jgroups.blocks.RpcDispatcher;

import java.io.Serializable;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

/**
 * The input end of a distributed pipe - a channel with only one output that forwards messages to a
 * remote receiver running in a different address space than the sender.
 * <p>
 * The output end of the distributed pipe its identified by a JGroups address. When configuring the
 * output end of the pipe, the remote end must use the same pipeID as the one used to configure the
 * pipe's input end.
 * <p>
 * The asynchronous/synchronous behaviour of the distributed pipe can be configured using
 * setSynchronous().
 * <p>
 * Multiple distributed pipes can share the same PipeOutput instance (and implicitly the
 * pipeID), as long the DistributedPipeIntput instances are different.
 *
 * @see org.jboss.messaging.core.Receiver
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 */
public class Pipe extends SingleOutputChannelSupport
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(Pipe.class);

   // Attributes ----------------------------------------------------

   protected RpcDispatcher dispatcher;
   protected Address outputAddress;
   protected Serializable pipeID;

   /** speed optimization, avoid creating unnecessary instances **/
   protected Acknowledgment ACK, NACK;

   // Constructors --------------------------------------------------

   /**
    * Pipe constructor.
    *
    * @param mode - true for synchronous behaviour, false otherwise.
    * @param dispatcher - the RPCDipatcher to delegate the transport to. The underlying JChannel
    *        doesn't necessarily have to be connected at the time the Pipe is
    *        created.
    * @param outputAddress - the address of the group member the receiving end is available on.
    * @param pipeID - the unique identifier of the distributed pipe. This pipe's output end must
    *        be initialized with the same pipeID.
    */
   public Pipe(boolean mode,
               RpcDispatcher dispatcher,
               Address outputAddress,
               Serializable pipeID)
   {
      super(mode);
      this.dispatcher = dispatcher;
      this.outputAddress = outputAddress;
      this.pipeID = pipeID;
      ACK = new AcknowledgmentImpl(pipeID, true);
      NACK = new AcknowledgmentImpl(pipeID, false);
      log.debug(this + " created");
   }

   // Channel implementation ----------------------------------------

   public Serializable getReceiverID()
   {
      return pipeID;
   }

   public boolean deliver()
   {
      lock();

      if (log.isTraceEnabled()) { log.trace("asynchronous delivery triggered on " + getReceiverID()); }

      Set nackedMessages = new HashSet();

      try
      {
         for(Iterator i = localAcknowledgmentStore.getUnacknowledged(null).iterator(); i.hasNext();)
         {
            nackedMessages.add(i.next());
         }
      }
      finally
      {
         unlock();
      }

      // flush unacknowledged messages

      for(Iterator i = nackedMessages.iterator(); i.hasNext(); )
      {
         Serializable messageID = (Serializable)i.next();
         Routable r = (Routable)messages.get(messageID);

         if (r.isExpired())
         {
            removeLocalMessage(messageID);
            i.remove();
            continue;
         }

         if (log.isTraceEnabled()) { log.trace(this + " attempting to redeliver " + r); }

         Acknowledgment ack = handleSynchronously(r);
         Set acks = ack == null ? null : Collections.singleton(ack);
         updateLocalAcknowledgments(r, acks);
      }

      return !hasMessages();

   }

   // ChannelSupport implementation ---------------------------------

   public boolean nonTransactionalHandle(Routable r)
   {
      Acknowledgment ack = handleSynchronously(r);
      if (ack != null && ack.isPositive())
      {
         // successful synchronous delivery
         return true;
      }
      Set acks = ack == null ? null : Collections.singleton(ack);
      return updateAcknowledgments(r, acks);
   }

   // SingleOutputChannelSupport overrides --------------------------

   public Serializable getOutputID()
   {
      try
      {
         return (Serializable)call("getOutputID", new Object[0], new String[0]);
      }
      catch(Throwable e)
      {
         log.error("Could not get output ID from the PipeOutput", e);
      }
      return null;
   }

   // Public --------------------------------------------------------

   public Address getOutputAddress()
   {
      return outputAddress;
   }

   public void setOutputAddress(Address a)
   {
      outputAddress = a;
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("Pipe[");
      sb.append(isSynchronous()?"SYNCH":"ASYNCH");
      sb.append("][");
      sb.append(pipeID);
      sb.append("] -> ");
      sb.append(outputAddress);
      return sb.toString();
   }

   // Protected -----------------------------------------------------


   // Private -------------------------------------------------------

   /**
    * @return a positive, negative or null Acknowledgment. null means the receiver could not
    *         be contacted at all or there is no receiver.
    */
   private Acknowledgment handleSynchronously(Routable r)
   {
      // Check if the message was sent remotely; in this case, I must not resend it to avoid
      // endless loops among peers or worse, deadlock on distributed RPC if deadlock detection
      // was not enabled.

      if (r.getHeader(Routable.REMOTE_ROUTABLE) != null)
      {
         // don't send
         return null;
      }

      try
      {
         // call on the PipeOutput unique server delegate
         Boolean result = (Boolean)call("handle",
                                        new Object[] {r},
                                        new String[] {"org.jboss.messaging.core.Routable"});
         return result.booleanValue() ? ACK : NACK;
      }
      catch(Throwable e)
      {
         log.error("Remote call handle() on " + outputAddress +  "." + pipeID + " failed", e);
         return null;
      }
   }

   /**
    * Synchronous remote call.
    */
   private Object call(String methodName, Object[] args, String[] argTypes) throws Throwable
   {
      if (outputAddress == null)
      {
         // A distributed pipe must be configured with a valid output address
         throw new Exception(this + " has a null output address");
      }
      RpcServerCall rpcServerCall =  new RpcServerCall(pipeID, methodName, args, argTypes);
      return rpcServerCall.remoteInvoke(dispatcher, outputAddress, 30000);
   }
}
