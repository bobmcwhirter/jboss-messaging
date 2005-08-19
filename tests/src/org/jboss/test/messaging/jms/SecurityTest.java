/*
 * JBoss, the OpenSource J2EE webOS
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.messaging.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.IllegalStateException;
import javax.jms.JMSSecurityException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.InitialContext;

import org.jboss.logging.Logger;
import org.jboss.test.messaging.MessagingTestCase;
import org.jboss.test.messaging.tools.ServerManagement;

/**
 * 
 * Test JMS Security.
 * 
 * This test must be run with the Test security config. on the server
 * 
 * @author <a href="mailto:tim.l.fox@gmail.com">Tim Fox</a>
 * 
 * Much of the basic idea of the tests come from SecurityUnitTestCase.java in JBossMQ by:
 * @author <a href="pra@tim.se">Peter Antman</a>
 * 
 * In order for this test to run you must ensure:
 * 
 * A JBoss instance is running at localhost
 * jboss-messaging.sar has been deployed to JBoss
 * login-config.xml should have an application-policy as follows:
 * 
 *   <application-policy name="jbossmessaging">
 *    <authentication>
 *     <login-module code="org.jboss.security.auth.spi.DatabaseServerLoginModule"
 *       flag="required">
 *       <module-option name="unauthenticatedIdentity">guest</module-option>
 *       <module-option name="dsJndiName">java:/DefaultDS</module-option>
 *        <module-option name="principalsQuery">SELECT PASSWD FROM JMS_USERS WHERE USERID=?</module-option>
 *       <module-option name="rolesQuery">SELECT ROLEID, 'Roles' FROM JMS_ROLES WHERE USERID=?</module-option>
 *     </login-module>
 *    </authentication>
 *   </application-policy>
 *   
 *   
 * The following queues and topics should be in jboss-messaging-service.xml:
 * 
 *  <mbean code="org.jboss.jms.server.jmx.Topic"
      name="jboss.messaging.destination:service=Topic,name=testTopic">
      <depends optional-attribute-name="ServerPeer">jboss.messaging:service=ServerPeer</depends>
      <depends optional-attribute-name="SecurityManager">jboss.messaging:service=SecurityManager</depends>
      <attribute name="SecurityConf">
         <security>
            <role name="guest" read="true" write="true"/>
            <role name="publisher" read="true" write="true" create="false"/>
            <role name="durpublisher" read="true" write="true" create="true"/>
         </security>
      </attribute>
   </mbean>
   
   <mbean code="org.jboss.jms.server.jmx.Topic"
      name="jboss.messaging.destination:service=Topic,name=securedTopic">
      <depends optional-attribute-name="ServerPeer">jboss.messaging:service=ServerPeer</depends>
      <depends optional-attribute-name="SecurityManager">jboss.messaging:service=SecurityManager</depends>
      <attribute name="SecurityConf">
         <security>
            <role name="publisher" read="true" write="true" create="false"/>
         </security>
      </attribute>
   </mbean>

   <mbean code="org.jboss.jms.server.jmx.Topic"
      name="jboss.messaging.destination:service=Topic,name=testDurableTopic">
      <depends optional-attribute-name="ServerPeer">jboss.messaging:service=ServerPeer</depends>
      <depends optional-attribute-name="SecurityManager">jboss.messaging:service=SecurityManager</depends>
      <attribute name="SecurityConf">
         <security>
            <role name="guest" read="true" write="true"/>
            <role name="publisher" read="true" write="true" create="false"/>
            <role name="durpublisher" read="true" write="true" create="true"/>
         </security>
      </attribute>
   </mbean>

   <mbean code="org.jboss.jms.server.jmx.Queue"
      name="jboss.messaging.destination:service=Queue,name=testQueue">
      <depends optional-attribute-name="ServerPeer">jboss.messaging:service=ServerPeer</depends>
      <depends optional-attribute-name="SecurityManager">jboss.messaging:service=SecurityManager</depends>
      <attribute name="SecurityConf">
         <security>
            <role name="guest" read="true" write="true"/>
            <role name="publisher" read="true" write="true" create="false"/>
            <role name="noacc" read="false" write="false" create="false"/>
         </security>
      </attribute>
   </mbean>
     
     
   The following user and role information should be in the HSQLDB database:
   
   INSERT INTO JMS_USERS (USERID, PASSWD) VALUES ('guest', 'guest')
   INSERT INTO JMS_USERS (USERID, PASSWD) VALUES ('j2ee', 'j2ee')
   INSERT INTO JMS_USERS (USERID, PASSWD, CLIENTID) VALUES ('john', 'needle', 'DurableSubscriberExample')
   INSERT INTO JMS_USERS (USERID, PASSWD) VALUES ('nobody', 'nobody')
   INSERT INTO JMS_USERS (USERID, PASSWD) VALUES ('dynsub', 'dynsub')
   INSERT INTO JMS_ROLES (ROLEID, USERID) VALUES ('guest','guest')
   INSERT INTO JMS_ROLES (ROLEID, USERID) VALUES ('j2ee','guest')
   INSERT INTO JMS_ROLES (ROLEID, USERID) VALUES ('john','guest')
   INSERT INTO JMS_ROLES (ROLEID, USERID) VALUES ('subscriber','john')
   INSERT INTO JMS_ROLES (ROLEID, USERID) VALUES ('publisher','john')
   INSERT INTO JMS_ROLES (ROLEID, USERID) VALUES ('publisher','dynsub')
   INSERT INTO JMS_ROLES (ROLEID, USERID) VALUES ('durpublisher','john')
   INSERT INTO JMS_ROLES (ROLEID, USERID) VALUES ('durpublisher','dynsub')
   INSERT INTO JMS_ROLES (ROLEID, USERID) VALUES ('noacc','nobody')
   
   This should be done by the StateManager on start-up
 * 
 * 
 * @version <tt>$Revision$</tt>
 *
 */
public class SecurityTest extends MessagingTestCase
{
   protected Logger log = Logger.getLogger(getClass());
   
   protected static final String TEST_QUEUE = "queue/testQueue";
   protected static final String TEST_TOPIC = "topic/testTopic";
   protected static final String SECURED_TOPIC = "topic/securedTopic";
   
   protected ConnectionFactory cf;
   protected Queue testQueue;
   protected Topic testTopic;
   protected Topic securedTopic;
   
   // Constructors --------------------------------------------------

   public SecurityTest(String name)
   {
      super(name);
   }

   // TestCase overrides -------------------------------------------

   protected void setUp() throws Exception
   {
      
      try
      {
         
         log.info("========= Start test: " + getName());
         
         ServerManagement.setRemote(true);
         
         InitialContext ic = new InitialContext(ServerManagement.getJNDIEnvironment());
         
         cf = (ConnectionFactory)ic.lookup("/ConnectionFactory");
         
         testQueue = (Queue)ic.lookup("/queue/testQueue");
         testTopic = (Topic)ic.lookup("/topic/testTopic");
         securedTopic = (Topic)ic.lookup("/topic/securedTopic");
         
         log.info("Got connection factory:" + cf);
         
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      
   }
   
   protected void tearDown() throws Exception
   {
      log.info("========== Stop test: " + getName());
   }

   
   // Constructors --------------------------------------------------
   
   
   // Public --------------------------------------------------------
   
   
   
   private boolean canReadDestination(Connection conn, Destination dest) throws Exception
   {
      
      Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      
      try
      {
         MessageConsumer consumer = sess.createConsumer(dest);
         return true;
      }
      catch (JMSSecurityException e)
      {
         log.trace("Can't read destination");
         return false;
      }     
   }
   
   private boolean canWriteDestination(Connection conn, Destination dest) throws Exception
   {
      
      Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      
      boolean namedSucceeded = true;
      try
      {
         MessageProducer producer = sess.createProducer(dest);         
      }
      catch (JMSSecurityException e)
      {
         log.trace("Can't write to destination using named producer");
         namedSucceeded = false;
      }
      
      boolean anonSucceeded = true;
      try
      {         
         MessageProducer producerAnon = sess.createProducer(null);
         Message m = sess.createTextMessage("Kippers");
         producerAnon.send(dest, m);
      }
      catch (JMSSecurityException e)
      {
         log.trace("Can't write to destination using named producer");
         anonSucceeded = false;
      }
      
      log.trace("namedSucceeded:" + namedSucceeded + ", anonSucceeded:" + anonSucceeded);
      return namedSucceeded || anonSucceeded;
     
   }
   
   private boolean canCreateDurableSub(Connection conn, Topic topic, String subName) throws Exception
   {
      
      Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      
      try
      {
         sess.createDurableSubscriber(topic, subName);
         sess.unsubscribe(subName);
         log.trace("Successfully created and unsubscribed subscription");
         return true;
      }
      catch (JMSSecurityException e)
      {
         log.trace("Can't create durable sub");
         return false;
      }    
   }
   
   /**
    * Login with no user, no password
    * Should allow login (equivalent to guest)
    */
   public void testLoginNoUserNoPassword() throws Exception
   {
      Connection conn1 = null;
      Connection conn2 = null;
      try
      {
         conn1 = cf.createConnection();
         conn2 = cf.createConnection(null, null);
      }
      finally
      {
         if (conn1 != null) conn1.close();
         if (conn2 != null) conn2.close();
      }
   }
   
   /** 
    * Login with valid user and password
    * Should allow
    */
   public void testLoginValidUserAndPassword() throws Exception
   {
      Connection conn1 = null;
      try
      {
         conn1 = cf.createConnection("john", "needle");
      }
      finally
      {
         if (conn1 != null) conn1.close();
      }
   }
   
   /** 
    * Login with valid user and invalid password
    * Should allow
    */
   public void testLoginValidUserInvalidPassword() throws Exception
   {
      Connection conn1 = null;
      try
      {
         conn1 = cf.createConnection("john", "blobby");
      }
      catch (JMSSecurityException e)
      {
         //Expected
      }
      finally
      {
         if (conn1 != null) conn1.close();
      }
   }
   
   /** 
    * Login with invalid user and invalid password
    * Should allow
    */
   public void testLoginInvalidUserInvalidPassword() throws Exception
   {
      Connection conn1 = null;
      try
      {
         conn1 = cf.createConnection("osama", "blah");
         fail();
      }
      catch (JMSSecurityException e)
      {
         //Expected
      }
      finally
      {
         if (conn1 != null) conn1.close();
      }
   }
   
   /* Now some client id tests */
   
   
   
   /*
    * user/pwd with preconfigured clientID, should return preconf
    */
   /*
    
    
    This test will not work until client id is automatically preconfigured into
    connection for specific user
    
    public void testPreConfClientID() throws Exception
    {
    Connection conn = null;
    try
    {
    conn = cf.createConnection("john", "needle");
    String clientID = conn.getClientID();
    assertEquals("Invalid ClientID", "DurableSubscriberExample", clientID);
    }
    finally
    {
    if (conn != null) conn.close();
    }
    }
    */
   /*
    * Try setting client ID
    */
   public void testSetClientID() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection();
         conn.setClientID("myID");
         String clientID = conn.getClientID();
         assertEquals("Invalid ClientID", "myID", clientID);
      }
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   /*
    * Try setting client ID on preconfigured connection - should throw exception
    */
   /*
    * 
    
    
    This test will not work until client id is automatically preconfigured into
    connection for specific user
    
    public void testSetClientIDPreConf() throws Exception
    {
    Connection conn = null;
    try
    {
    conn = cf.createConnection("john", "needle");
    conn.setClientID("myID");
    fail();
    }
    catch (InvalidClientIDException e)
    {
    //Expected
     }
     finally
     {
     if (conn != null) conn.close();
     }
     }
     */
   
   /*
    * Try setting client ID after an operation has been performed on the connection
    */
   public void testSetClientIDAfterOp() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection();
         Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
         conn.setClientID("myID");
         fail();
      }
      catch (IllegalStateException e)
      {
         //Expected
      }
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   
   /* Authorization tests */
   
   
   /*
    * Test valid topic publisher
    */
   public void testValidTopicPublisher() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("john", "needle");        
         assertTrue(this.canWriteDestination(conn, testTopic));
      }        
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   
   /*
    * Test invalid topic publisher
    */
   public void testInvalidTopicPublisher() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("nobody", "nobody");        
         assertFalse(this.canWriteDestination(conn, testTopic));
      }    
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   
   /*
    * Test valid topic subscriber
    */
   public void testValidTopicSubscriber() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("john", "needle");        
         assertTrue(this.canReadDestination(conn, testTopic));
      }    
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   /*
    * Test invalid topic subscriber
    */
   public void testInvalidTopicSubscriber() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("nobody", "nobody");        
         assertFalse(this.canReadDestination(conn, testTopic));
      }    
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   
   /*
    * Test valid queue browser
    */
   public void testValidQueueBrowser() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("john", "needle");        
         Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
         QueueBrowser browser = sess.createBrowser(testQueue);
      }    
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   /*
    * Test invalid queue browser
    */
   public void testInvalidQueueBrowser() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("nobody", "nobody");        
         Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
         QueueBrowser browser = sess.createBrowser(testQueue);
         fail();
      }    
      catch (JMSSecurityException e)
      {
         //Expected
      }
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   
   
   /*
    * Test valid queue sender
    */
   public void testValidQueueSender() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("john", "needle");        
         assertTrue(this.canWriteDestination(conn, testQueue));
      }        
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   
   /*
    * Test invalid queue sender
    */
   public void testInvalidQueueSender() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("nobody", "nobody");        
         assertFalse(this.canWriteDestination(conn, testQueue));
      }    
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   
   
   /*
    * Test valid queue receiver
    */
   public void testValidQueueReceiver() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("john", "needle");        
         assertTrue(this.canReadDestination(conn, testQueue));
      }    
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   /*
    * Test invalid queue receiver
    */
   public void testInvalidQueueReceiver() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("nobody", "nobody");        
         assertFalse(this.canReadDestination(conn, testQueue));
      }    
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   
   /*
    * Test valid durable subscription creation for connection preconfigured with client id
    */
   
   /*
   
   This test will not work until client id is automatically preconfigured into
   connection for specific user
   
   public void testValidDurableSubscriptionCreationPreConf() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("john", "needle");        
         assertTrue(this.canCreateDurableSub(conn, testTopic, "sub2"));
      }          
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   */
   
   /*
    * Test invalid durable subscription creation for connection preconfigured with client id
    */
   
   
   /*
   
   This test will not work until client id is automatically preconfigured into
   connection for specific user
   public void testInvalidDurableSubscriptionCreationPreConf() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("john", "needle");        
         assertFalse(this.canCreateDurableSub(conn, securedTopic, "sub3"));
      }    
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   */
   
   
   /*
    * Test valid durable subscription creation for connection not preconfigured with client id
    */
   
   public void testValidDurableSubscriptionCreationNotPreConf() throws Exception
   {
      Connection conn = null;
      try
      {
         conn = cf.createConnection("dynsub", "dynsub");        
         conn.setClientID("myID");
         assertTrue(this.canCreateDurableSub(conn, testTopic, "sub4"));
      }          
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   
   
   /*
    * Test invalid durable subscription creation for connection not preconfigured with client id
    */

   
   public void testInvalidDurableSubscriptionCreationNotPreConf() throws Exception
   {
      Connection conn = null;
      try
      {        
         conn = cf.createConnection("dynsub", "dynsub");       
         conn.setClientID("myID2");
         assertFalse(this.canCreateDurableSub(conn, securedTopic, "sub5"));
      }    
      catch (Throwable t)
      {
         t.printStackTrace();
      }
      finally
      {
         if (conn != null) conn.close();
      }
   }
   
   
   
   
   
   // Protected -----------------------------------------------------
   
   
   }


