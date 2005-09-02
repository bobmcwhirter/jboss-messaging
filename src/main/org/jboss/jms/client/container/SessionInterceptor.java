/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.client.container;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import javax.jms.IllegalStateException;
import javax.jms.Session;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.jms.delegate.SessionDelegate;
import org.jboss.jms.tx.AckInfo;
import org.jboss.logging.Logger;

/**
 * This interceptor handles JMS session related logic
 * 
 * Important! There is one instance of this interceptor per instance of Session
 * and Connection
 *
 * @author <a href="mailto:tim.l.fox@gmail.com>Tim Fox</a>
 */
public class SessionInterceptor implements Interceptor, Serializable
{
   // Constants -----------------------------------------------------
   
   private static final long serialVersionUID = -8567252489464374932L;
   
   private static final Logger log = Logger.getLogger(SessionInterceptor.class);
   
   // Attributes ----------------------------------------------------
   
   protected int ackMode;
   
   protected boolean transacted;
   
   protected boolean XA;      //Hmmm... should really be in TransactionInterceptor
   
   //protected Object txID; 
   
   protected ArrayList unacked = new ArrayList();

   // Static --------------------------------------------------------
   
   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   // Interceptor implementation ----------------------------------
   
   public String getName()
   {
      return "SessionInterceptor";
   }

   public Object invoke(Invocation invocation) throws Throwable
   {      
      if (!(invocation instanceof MethodInvocation))
      {
         return invocation.invokeNext();
      }
      
      MethodInvocation mi = (MethodInvocation)invocation;
      String methodName = mi.getMethod().getName();
      
      if (log.isTraceEnabled()) log.trace("In SessionInterceptor: method is " + methodName);
      
      if ("createSessionDelegate".equals(methodName))
      {
         if (log.isTraceEnabled()) { log.trace("Creating session delegate"); }
         SessionDelegate sessionDelegate = (SessionDelegate)invocation.invokeNext();
         boolean transacted = ((Boolean)mi.getArguments()[0]).booleanValue();
         int ackMode = ((Integer)mi.getArguments()[1]).intValue();
         boolean isXA = ((Boolean)mi.getArguments()[2]).booleanValue();
         sessionDelegate.setTransacted(transacted);
         sessionDelegate.setAcknowledgeMode(ackMode);
         sessionDelegate.setXA(isXA);
         if (log.isTraceEnabled()) { log.trace("XA?:" + isXA); }
         return sessionDelegate;
      }
      else if ("acknowledgeSession".equals(methodName))
      {
         //Acknowledge all the messages received in this session
         if (log.isTraceEnabled()) { log.trace("acknowledgeSession called"); }
         
         //SessionState state = getSessionState(mi);
         
         //This only does anything if in client acknowledge mode
         if (ackMode != Session.CLIENT_ACKNOWLEDGE)
         {
            return null;
         }                        
         
         if (transacted)
         {
            //Transacted session - so do nothing
            return null;
         }
         else
         {
            if (log.isTraceEnabled()) 
               log.trace("I have " + unacked.size() + " messages in the session to ack");
            Iterator iter = unacked.iterator();
            try
            {
               while (iter.hasNext())
               {
                  AckInfo ackInfo = (AckInfo)iter.next();
                  getDelegate(mi).acknowledge(ackInfo.messageID, ackInfo.receiverID);
               }
            }
            finally
            {
               unacked.clear();
            }
            return null;
         }
      }     
      else if ("postDeliver".equals(methodName))
      {     
         String messageID = (String)mi.getArguments()[0];
         String receiverID = (String)mi.getArguments()[1];         
         
         //SessionState state = getSessionState(mi);
         
         if (log.isTraceEnabled()) { log.trace("Session ack mode is:" + ackMode); }
         
         if (transacted)
         {
            if (log.isTraceEnabled()) log.trace("Session is transacted - doing nothing");
            return null;
         }
         else if (ackMode == Session.AUTO_ACKNOWLEDGE)
         {
            //Just acknowledge now
            if (log.isTraceEnabled()) log.trace("Auto-acking message");
            getDelegate(mi).acknowledge(messageID, receiverID);
         }
         else if (ackMode == Session.DUPS_OK_ACKNOWLEDGE)
         {
            //TODO Lazy acks - for now we ack individually
            if (log.isTraceEnabled()) log.trace("Lazy acking message");
            getDelegate(mi).acknowledge(messageID, receiverID);
         }
         else if (ackMode == Session.CLIENT_ACKNOWLEDGE)
         {
            if (log.isTraceEnabled()) log.trace("Client acknowledge so storing in unacked msgs");
            unacked.add(new AckInfo(messageID, receiverID));
            if (log.isTraceEnabled())
            {
               log.trace("There are now " + unacked.size() + " messages");
            }
         }         
         return null;
      }
      else if ("close".equals(methodName))
      {
         if (mi.getMethod().getDeclaringClass().equals(SessionDelegate.class))
         {
            //SessionState state = getSessionState(mi);
            unacked.clear();
         }
         
         //need to rollback
         //getDelegate(mi).rollback();                          
      }
      else if ("recover".equals(methodName))
      {
         //SessionState state = getSessionState(mi);
         if (log.isTraceEnabled()) { log.trace("recover called"); }
         if (transacted)
         {
            throw new IllegalStateException("Cannot recover a transacted session");
         }
         unacked.clear();
         
         //Tell the server to redeliver any un-acked messages
         if (log.isTraceEnabled()) { log.trace("redelivering messages"); }
         getDelegate(mi).redeliver();
         return null;         
      }
      else if ("getAcknowledgeMode".equals(methodName))
      {
         //SessionState state = getSessionState(mi);
         return new Integer(ackMode);
      }
      else if ("getTransacted".equals(methodName))
      {
         //SessionState state = getSessionState(mi);
         return new Boolean(transacted);
      }
      else if ("getXA".equals(methodName))
      {
         //SessionState state = getSessionState(mi);
         return new Boolean(XA);
      }
      else if ("setAcknowledgeMode".equals(methodName))
      {
         this.ackMode = ((Integer)mi.getArguments()[0]).intValue();
         return null;
      }
      else if ("setTransacted".equals(methodName))
      {
         this.transacted = ((Boolean)mi.getArguments()[0]).booleanValue();
         return null;
      }
      else if ("setXA".equals(methodName))
      {
         this.XA = ((Boolean)mi.getArguments()[0]).booleanValue();
         return null;
      }
      
      return invocation.invokeNext();
   }
   
   // Class YYY overrides -------------------------------------------

   // Protected -----------------------------------------------------

   // Package Private -----------------------------------------------

   // Private -------------------------------------------------------
   
   private JMSInvocationHandler getHandler(Invocation invocation)
   {
      return ((JMSMethodInvocation)invocation).getHandler();
   }
   
   
   private SessionDelegate getDelegate(Invocation invocation)
   {
      return (SessionDelegate)getHandler(invocation).getDelegate();
   }
   
   
   
   // Inner Classes -------------------------------------------------
   
	
}

