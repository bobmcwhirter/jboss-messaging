/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.server;

import org.jboss.remoting.InvokerLocator;
import org.jboss.jms.delegate.ConnectionFactoryDelegate;
import org.jboss.jms.server.security.SecurityManager;
import org.jboss.jms.server.endpoint.ServerConnectionFactoryDelegate;
import org.jboss.jms.server.container.JMSAdvisor;
import org.jboss.jms.server.remoting.JMSServerInvocationHandler;
import org.jboss.jms.client.JBossConnectionFactory;
import org.jboss.jms.client.container.JMSInvocationHandler;
import org.jboss.jms.client.container.InvokerInterceptor;
import org.jboss.jms.util.JNDIUtil;
import org.jboss.aop.ClassAdvisor;
import org.jboss.aop.DomainDefinition;
import org.jboss.aop.AspectManager;
import org.jboss.aop.Dispatcher;
import org.jboss.aop.util.PayloadKey;
import org.jboss.aop.metadata.SimpleMetaData;
import org.jboss.aop.advice.AdviceStack;
import org.jboss.aop.advice.Interceptor;
import org.jboss.messaging.core.MessageStore;
import org.jboss.messaging.core.PersistenceManager;
import org.jboss.messaging.core.message.PersistentMessageStore;
import org.jboss.messaging.core.persistence.HSQLDBPersistenceManager;
import org.jboss.logging.Logger;
import org.w3c.dom.Element;

import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.transaction.TransactionManager;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

/**
 * A JMS server peer.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class ServerPeer
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(ServerPeer.class);

   private static final String CONNECTION_FACTORY_JNDI_NAME = "ConnectionFactory";
   private static final String XACONNECTION_FACTORY_JNDI_NAME = "XAConnectionFactory";
   private static ObjectName DESTINATION_MANAGER_OBJECT_NAME;

   static
   {
      try
      {
         DESTINATION_MANAGER_OBJECT_NAME =
         new ObjectName("jboss.messaging:service=DestinationManager");
      }
      catch(Exception e)
      {
         log.error(e);
      }
   }


   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   protected String serverPeerID;
   protected InvokerLocator locator;
   protected ObjectName connector;
   //protected ObjectName securityManagerON;


   protected ClientManager clientManager;
   protected DestinationManagerImpl destinationManager;
   protected SecurityManager securityManager;
   protected Map connFactoryDelegates;
   protected MBeanServer mbeanServer;

   protected boolean started;

   protected ClassAdvisor connFactoryAdvisor;
   protected ClassAdvisor connAdvisor;
   protected ClassAdvisor sessionAdvisor;
   protected ClassAdvisor producerAdvisor;
   protected ClassAdvisor consumerAdvisor;
	protected ClassAdvisor browserAdvisor;
   protected ClassAdvisor genericAdvisor;


   protected PooledExecutor threadPool;

   protected MessageStore ms;

   protected TransactionManager tm;
   protected PersistenceManager pm;

   protected int connFactoryIDSequence;

   // Constructors --------------------------------------------------


   public ServerPeer(String serverPeerID) throws Exception
   {
      this.serverPeerID = serverPeerID;
      this.connFactoryDelegates = new HashMap();

      // the default value to use, unless the JMX attribute is modified
      connector = new ObjectName("jboss.remoting:service=Connector,transport=socket");
      securityManager = new SecurityManager();
      started = false;
   }

   // Public --------------------------------------------------------

   //
   // JMX operations
   //

   public synchronized void create()
   {
      log.debug(this + " created");
   }

   public synchronized void start() throws Exception
   {
      if (started)
      {
         return;
      }

      log.debug(this + " starting");

      mbeanServer = findMBeanServer();
      tm = findTransactionManager();

      // TODO: this should be configurable
      pm = new HSQLDBPersistenceManager();

      // TODO: is should be possible to share this with other peers
      ms = new PersistentMessageStore(serverPeerID, pm, tm);

      clientManager = new ClientManager(this);
      destinationManager = new DestinationManagerImpl(this);
      threadPool = new PooledExecutor();

      initializeRemoting();
      initializeAdvisors();

      mbeanServer.registerMBean(destinationManager, DESTINATION_MANAGER_OBJECT_NAME);
      
      securityManager.init();
      
      setupConnectionFactories();

      started = true;

      log.info("JMS " + this + " started");
   }



   public synchronized void stop() throws Exception
   {
      if (!started)
      {
         return;
      }

      log.debug(this + " stopping");

      tearDownConnectionFactories();

      mbeanServer.unregisterMBean(DESTINATION_MANAGER_OBJECT_NAME);
      tearDownAdvisors();


      started = false;

      log.info("JMS " + this + " stopped");

   }

   public synchronized void destroy()
   {
      log.debug(this + " destroyed");
   }
   
   public ServerPeer getServerPeer()
   {
      return this;
   }


   //
   // end of JMX operations
   //

   //
   // JMX attributes
   //

   public String getServerPeerID()
   {
      return serverPeerID;
   }

   public String getLocatorURI()
   {
      if (locator == null)
      {
         return null;
      }
      return locator.getLocatorURI();
   }


   public ObjectName getConnector()
   {
      return connector;
   }

   public void setConnector(ObjectName on)
   {
      connector = on;
   }
   
   public void setSecurityDomain(String securityDomain)
   {
      securityManager.setSecurityDomain(securityDomain);
   }
   
   public String getSecurityDomain()
   {
      return securityManager.getSecurityDomain();
   }

   public void setDefaultSecurityConfig(Element conf)
      throws Exception
   {
      securityManager.setDefaultSecurityConfig(conf);
   }
   
   public Element getDefaultSecurityConfig()
   {
      return securityManager.getDefaultSecurityConfig();
   }
   
   public void setSecurityConfig(String dest, Element conf)
      throws Exception
   {
      securityManager.setSecurityConfig(dest, conf);
   }
   

   //
   // end of JMX attributes
   //

   public synchronized boolean isStarted()
   {
      return started;
   }

   public InvokerLocator getLocator()
   {
      return locator;
   }

   public ClientManager getClientManager()
   {
      return clientManager;
   }

   public DestinationManagerImpl getDestinationManager()
   {
      return destinationManager;
   }
   
   public SecurityManager getSecurityManager()
   {
      return securityManager;
   }

   public ConnectionFactoryDelegate getConnectionFactoryDelegate(String connectionFactoryID)
   {
      return (ConnectionFactoryDelegate)connFactoryDelegates.get(connectionFactoryID);
   }



   public ClassAdvisor getConnectionFactoryAdvisor()
   {
      return connFactoryAdvisor;
   }

   public ClassAdvisor getConnectionAdvisor()
   {
      return connAdvisor;
   }

   public ClassAdvisor getSessionAdvisor()
   {
      return sessionAdvisor;
   }

   public ClassAdvisor getProducerAdvisor()
   {
      return producerAdvisor;
   }

   public ClassAdvisor getBrowserAdvisor()
   {
      return browserAdvisor;
   }

   public ClassAdvisor getConsumerAdvisor()
   {
      return consumerAdvisor;
   }



   public PooledExecutor getThreadPool()
   {
      return threadPool;
   }

   public MessageStore getMessageStore()
   {
      return ms;
   }

   public TransactionManager getTransactionManager()
   {
      return tm;
   }

   public PersistenceManager getPersistenceManager()
   {
      return pm;
   }


   public String toString()
   {
      StringBuffer sb = new StringBuffer();
      sb.append("ServerPeer (id=");
      sb.append(getServerPeerID());
      sb.append(")");
      return sb.toString();
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   private static String[] domainNames = { "ServerConnectionFactoryDelegate",
                                           "ServerConnectionDelegate",
                                           "ServerSessionDelegate",
                                           "ServerProducerDelegate",
														 "ServerConsumerDelegate",
                                           "ServerBrowserDelegate",
                                           "GenericTarget"};

   private void initializeAdvisors() throws Exception
   {

      ClassAdvisor[] advisors = new ClassAdvisor[7];

      for(int i = 0; i < domainNames.length; i++)
      {
         DomainDefinition domainDefinition = AspectManager.instance().getContainer(domainNames[i]);
         if (domainDefinition == null)
         {
            throw new RuntimeException("Domain " + domainNames[i] + " not found");
         }

         //TODO Use AOPManager interface once that has stabilized
         advisors[i] = new JMSAdvisor(domainNames[i], (AspectManager)domainDefinition.getManager(), this);
         Class c = Class.forName("org.jboss.jms.server.endpoint." + domainNames[i]);
         advisors[i].attachClass(c);

         // register the advisor with the Dispatcher
         Dispatcher.singleton.registerTarget(advisors[i].getName(), advisors[i]);
      }
      connFactoryAdvisor = advisors[0];
      connAdvisor = advisors[1];
      sessionAdvisor = advisors[2];
      producerAdvisor = advisors[3];
		consumerAdvisor = advisors[4];
      browserAdvisor = advisors[5];
      genericAdvisor = advisors[6];
   }

   private void tearDownAdvisors() throws Exception
   {
      for(int i = 0; i < domainNames.length; i++)
      {
         Dispatcher.singleton.unregisterTarget(domainNames[i]);
      }
      connFactoryAdvisor = null;
      connAdvisor = null;
      sessionAdvisor = null;
      producerAdvisor = null;
      browserAdvisor = null;
   }

   private ConnectionFactory createConnectionFactory(String connFactoryID) throws Exception
   {
      ConnectionFactoryDelegate proxy = (ConnectionFactoryDelegate)createProxy(connFactoryID);
      return new JBossConnectionFactory(proxy);
   }

   private Object createProxy(String connFactoryID) throws Exception
   {
      Serializable oid = connFactoryAdvisor.getName();
      String stackName = "ConnectionFactoryStack";
      AdviceStack stack = AspectManager.instance().getAdviceStack(stackName);
      // TODO why do I need an advisor to create an interceptor stack?
      Interceptor[] interceptors = stack.createInterceptors(connFactoryAdvisor, null);
      JMSInvocationHandler h = new JMSInvocationHandler(interceptors);

      SimpleMetaData metadata = new SimpleMetaData();
      // TODO: The ConnectionFactoryDelegate and ConnectionDelegate share the same locator (TCP/IP connection?). Performance?
      metadata.addMetaData(Dispatcher.DISPATCHER, Dispatcher.OID, oid, PayloadKey.AS_IS);
      metadata.addMetaData(InvokerInterceptor.REMOTING,
                           InvokerInterceptor.INVOKER_LOCATOR,
                           locator,
                           PayloadKey.AS_IS);
      metadata.addMetaData(InvokerInterceptor.REMOTING,
                           InvokerInterceptor.INVOKER_LOCATOR,
                           locator,
                           PayloadKey.AS_IS);
      metadata.addMetaData(InvokerInterceptor.REMOTING,
                           InvokerInterceptor.SUBSYSTEM,
                           "JMS",
                           PayloadKey.AS_IS);
      metadata.addMetaData(JMSAdvisor.JMS, JMSAdvisor.CONNECTION_FACTORY_ID, connFactoryID, PayloadKey.AS_IS);

      h.getMetaData().mergeIn(metadata);

      // TODO
      ClassLoader loader = getClass().getClassLoader();
      Class[] interfaces = new Class[] { ConnectionFactoryDelegate.class };
      return Proxy.newProxyInstance(loader, interfaces, h);
   }


   /**
    * @return - may return null if it doesn't find a "jboss" MBeanServer.
    */
   private MBeanServer findMBeanServer()
   {
      System.setProperty("jmx.invoke.getters", "true");


      MBeanServer result = null;
      ArrayList l = MBeanServerFactory.findMBeanServer(null);
      for(Iterator i = l.iterator(); i.hasNext(); )
      {
         MBeanServer s = (MBeanServer)l.iterator().next();
         if ("jboss".equals(s.getDefaultDomain()))
         {
            result = s;
            break;
         }
      }
      return result;
   }

   private void initializeRemoting() throws Exception
   {
      String s = (String)mbeanServer.invoke(connector, "getInvokerLocator",
                                            new Object[0], new String[0]);
      locator = new InvokerLocator(s);

      log.debug("LocatorURI: " + getLocatorURI());

      // add the JMS subsystem
      mbeanServer.invoke(connector, "addInvocationHandler",
                         new Object[] {"JMS", new JMSServerInvocationHandler()},
                         new String[] {"java.lang.String",
                                       "org.jboss.remoting.ServerInvocationHandler"});

      // TODO what happens if there is a JMS subsystem already registered? Normally, nothing bad,
      // TODO since it delegates to a static dispatcher, but make sure

      // TODO if this is ServerPeer is stopped, the InvocationHandler will be left hanging
   }

   private TransactionManager findTransactionManager() throws Exception
   {
      TransactionManager tm = null;
      InitialContext ic = new InitialContext();
      try
      {
         tm = (TransactionManager)ic.lookup("java:/TransactionManager");
      }
      catch(NameNotFoundException e)
      {}

      log.debug("TransactionManager: " + tm);

      ic.close();
      return tm;
   }


   private void setupConnectionFactories() throws Exception
   {

      ConnectionFactory cf = setupConnectionFactory(null);
      InitialContext ic = new InitialContext();
      ic.rebind(CONNECTION_FACTORY_JNDI_NAME, cf);
      ic.rebind(XACONNECTION_FACTORY_JNDI_NAME, cf);

      //And now the connection factories and links as required by the TCK
      //See section 4.4.15 of the TCK user guide.
      //FIXME - this is a hack. It should be removed once a better way to manage
      //connection factories is implemented

      Context jmsContext = JNDIUtil.createContext(ic, "jms");
      jmsContext.rebind("QueueConnectionFactory", cf);
      jmsContext.rebind("TopicConnectionFactory", cf);

      jmsContext.rebind("DURABLE_SUB_CONNECTION_FACTORY", setupConnectionFactory("cts"));
      jmsContext.rebind("MDBTACCESSTEST_FACTORY", setupConnectionFactory("cts1"));
      jmsContext.rebind("DURABLE_BMT_CONNECTION_FACTORY", setupConnectionFactory("cts2"));
      jmsContext.rebind("DURABLE_CMT_CONNECTION_FACTORY", setupConnectionFactory("cts3"));
      jmsContext.rebind("DURABLE_BMT_XCONNECTION_FACTORY", setupConnectionFactory("cts4"));
      jmsContext.rebind("DURABLE_CMT_XCONNECTION_FACTORY", setupConnectionFactory("cts5"));
      jmsContext.rebind("DURABLE_CMT_TXNS_XCONNECTION_FACTORY", setupConnectionFactory("cts6"));

      ic.close();

   }

   private ConnectionFactory setupConnectionFactory(String clientID)
      throws Exception
   {
      String connFactoryID = genConnFactoryID();
      ServerConnectionFactoryDelegate serverDelegate = new ServerConnectionFactoryDelegate(this, clientID);
      this.connFactoryDelegates.put(connFactoryID, serverDelegate);
      ConnectionFactory clientDelegate = createConnectionFactory(connFactoryID);
      return clientDelegate;
   }

   private void tearDownConnectionFactories()
      throws Exception
   {

      InitialContext ic = new InitialContext();


      //TODO
      //FIXME - this is a hack. It should be removed once a better way to manage
      //connection factories is implemented
      ic.unbind("jms/DURABLE_SUB_CONNECTION_FACTORY");
      ic.unbind("jms/MDBTACCESSTEST_FACTORY");
      ic.unbind("jms/DURABLE_BMT_CONNECTION_FACTORY");
      ic.unbind("jms/DURABLE_CMT_CONNECTION_FACTORY");
      ic.unbind("jms/DURABLE_BMT_XCONNECTION_FACTORY");
      ic.unbind("jms/DURABLE_CMT_XCONNECTION_FACTORY");
      ic.unbind("jms/DURABLE_CMT_TXNS_XCONNECTION_FACTORY");

      ic.unbind(CONNECTION_FACTORY_JNDI_NAME);
      ic.unbind(XACONNECTION_FACTORY_JNDI_NAME);

   }



   private synchronized String genConnFactoryID()
   {
      return "CONNFACTORY" + connFactoryIDSequence++;
   }




   // Inner classes -------------------------------------------------
}
