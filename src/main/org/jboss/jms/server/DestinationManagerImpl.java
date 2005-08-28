/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.server;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import org.jboss.jms.destination.JBossDestination;
import org.jboss.jms.destination.JBossQueue;
import org.jboss.jms.destination.JBossTopic;
import org.jboss.jms.util.JNDIUtil;
import org.jboss.logging.Logger;
import org.jboss.messaging.core.Channel;

/**
 * Manages destinations. Manages JNDI mapping and delegates core destination management to a
 * CoreDestinationManager.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class DestinationManagerImpl implements DestinationManagerImplMBean
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(DestinationManagerImpl.class);
   
   public static final String DEFAULT_QUEUE_CONTEXT = "/queue";
   public static final String DEFAULT_TOPIC_CONTEXT = "/topic";
   
   // Static --------------------------------------------------------
   
   // Attributes ----------------------------------------------------

   protected ServerPeer serverPeer;
   protected Context initialContext;
   protected CoreDestinationManager coreDestinationManager;

   // < name - JNDI name>
   protected Map queueNameToJNDI;
   protected Map topicNameToJNDI;


   // Constructors --------------------------------------------------

   public DestinationManagerImpl(ServerPeer serverPeer) throws Exception
   {
      this.serverPeer = serverPeer;
      //TODO close this inital context when DestinationManager stops
      initialContext = new InitialContext();
      coreDestinationManager = new CoreDestinationManager(this);
      queueNameToJNDI = new HashMap();
      topicNameToJNDI = new HashMap();
   }

   // DestinationManager implementation -----------------------------


   public void createQueue(String name) throws Exception
   {
      createDestination(true, name, null);
   }

   public void createQueue(String name, String jndiName) throws Exception
   {
      createDestination(true, name, jndiName);
   }

   public void destroyQueue(String name) throws Exception
   {
      removeDestination(true, name);
   }

   public void createTopic(String name) throws Exception
   {
      createDestination(false, name, null);
   }

   public void createTopic(String name, String jndiName) throws Exception
   {
      createDestination(false, name, jndiName);
   }

   public void destroyTopic(String name) throws Exception
   {
      removeDestination(false, name);
   }

   // Public --------------------------------------------------------

   ServerPeer getServerPeer()
   {
      return serverPeer;
   }
   
   public DestinationManagerImpl getDestinationManager()
   {
      return this;
   }

   public void addTemporaryDestination(Destination jmsDestination) throws JMSException
   {
      coreDestinationManager.addCoreDestination(jmsDestination);
   }

   public void removeTemporaryDestination(Destination jmsDestination)
   {
      JBossDestination d = (JBossDestination)jmsDestination;
      boolean isQueue = d.isQueue();
      String name = d.getName();
      coreDestinationManager.removeCoreDestination(isQueue, name);
   }

   public Channel getCoreDestination(Destination jmsDestination) throws JMSException
   {
      JBossDestination d = (JBossDestination)jmsDestination;
      boolean isQueue = d.isQueue();
      String name = d.getName();
      return getCoreDestination(isQueue, name);
   }

   public Channel getCoreDestination(boolean isQueue, String name) throws JMSException
   {
      return coreDestinationManager.getCoreDestination(isQueue, name);
   }


   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   private void createDestination(boolean isQueue, String name, String jndiName) throws Exception
   {
      String parentContext;
      String jndiNameInContext;

      if (jndiName == null)
      {
         parentContext = (isQueue ? DEFAULT_QUEUE_CONTEXT : DEFAULT_TOPIC_CONTEXT);
         jndiNameInContext = name;
         jndiName = parentContext + "/" + jndiNameInContext;
      }
      else
      {
         // TODO more solid parsing + test cases
         int sepIndex = jndiName.lastIndexOf('/');
         if (sepIndex == -1)
         {
            parentContext = "";
         }
         else
         {
            parentContext = jndiName.substring(0, sepIndex);
         }
         jndiNameInContext = jndiName.substring(sepIndex + 1);
      }

      try
      {
         initialContext.lookup(jndiName);
         
         //Already exists - remove it - this allows the dest to be updated with any
         //new values
         //this.removeDestination(isQueue, name);
         throw new InvalidDestinationException("Destination " + name + " already exists");
            
      }
      catch(NameNotFoundException e)
      {
         // OK
      }

      Destination jmsDestination = isQueue ?
                                   (Destination) new JBossQueue(name) :
                                   (Destination) new JBossTopic(name);

      coreDestinationManager.addCoreDestination(jmsDestination);

      try
      {
         Context c = JNDIUtil.createContext(initialContext, parentContext);
         c.rebind(jndiNameInContext, jmsDestination);
         if (isQueue)
         {
            queueNameToJNDI.put(name, jndiName);
         }
         else
         {
            topicNameToJNDI.put(name, jndiName);
         }
      }
      catch(Exception e)
      {
         coreDestinationManager.removeCoreDestination(isQueue, name);
         throw e;
      }

      log.info((isQueue ? "Queue" : "Topic") + " " + name +
               " created and bound in JNDI as " + jndiName );
   }


   private void removeDestination(boolean isQueue, String name) throws Exception
   {

      coreDestinationManager.removeCoreDestination(isQueue, name);
      String jndiName = null;
      if (isQueue)
      {
         jndiName = (String)queueNameToJNDI.remove(name);
      }
      else
      {
         jndiName = (String)topicNameToJNDI.remove(name);
      }
      if (jndiName == null)
      {
         return;
      }
      initialContext.unbind(jndiName);
   }

   // Inner classes -------------------------------------------------
}
