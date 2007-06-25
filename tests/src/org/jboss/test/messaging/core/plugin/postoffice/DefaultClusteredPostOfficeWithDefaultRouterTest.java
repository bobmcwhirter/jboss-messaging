/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
package org.jboss.test.messaging.core.plugin.postoffice;

import java.util.List;

import org.jboss.messaging.core.contract.PostOffice;
import org.jboss.test.messaging.core.PostOfficeTestBase;
import org.jboss.test.messaging.core.SimpleCondition;
import org.jboss.test.messaging.core.SimpleReceiver;

/**
 * 
 * A DefaultClusteredPostOfficeWithDefaultRouterTest
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a> 
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 *
 */
public class DefaultClusteredPostOfficeWithDefaultRouterTest extends PostOfficeTestBase
{
   // Constants ------------------------------------------------------------------------------------

   // Static ---------------------------------------------------------------------------------------
   
   // Attributes -----------------------------------------------------------------------------------
    
   // Constructors ---------------------------------------------------------------------------------

   public DefaultClusteredPostOfficeWithDefaultRouterTest(String name)
   {
      super(name);
   }

   // Public ---------------------------------------------------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();
   }

   public void tearDown() throws Exception
   {      
      super.tearDown();
   }
   
//   public void testNotLocalPersistent() throws Throwable
//   {
//      notLocal(true);
//   }
//   
//   public void testNotLocalNonPersistent() throws Throwable
//   {
//      notLocal(false);
//   }
//   
//   public void testLocalPersistent() throws Throwable
//   {
//      local(true);
//   }
//   
//   public void testLocalNonPersistent() throws Throwable
//   {
//      local(false);
//   }
//   
//   protected void notLocal(boolean persistent) throws Throwable
//   {
//      PostOffice office1 = null;
//      
//      PostOffice office2 = null;
//      
//      PostOffice office3 = null;
//      
//      PostOffice office4 = null;
//      
//      PostOffice office5 = null;
//      
//      PostOffice office6 = null;
//          
//      try
//      {   
//         office1 = createPostOffice(1, "testgroup", sc, ms, pm, tr);
//         
//         office2 = createPostOffice(2, "testgroup", sc, ms, pm, tr);
//         
//         office3 = createPostOffice(3, "testgroup", sc, ms, pm, tr);
//         
//         office4 = createPostOffice(4, "testgroup", sc, ms, pm, tr);
//         
//         office5 = createPostOffice(5, "testgroup", sc, ms, pm, tr);
//         
//         office6 = createPostOffice(6, "testgroup", sc, ms, pm, tr);
//         
//         LocalClusteredQueue queue1 =
//            new LocalClusteredQueue(2, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, false, -1, null);
//         office2.bindQueue(new SimpleCondition("topic"), queue1);
//         SimpleReceiver receiver1 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING);
//         queue1.add(receiver1);
//         
//         LocalClusteredQueue queue2 =
//            new LocalClusteredQueue(3, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, false, -1, null);
//         office3.bindQueue(new SimpleCondition("topic"), queue2);
//         SimpleReceiver receiver2 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING);
//         queue2.add(receiver2);
//         
//         LocalClusteredQueue queue3 =
//            new LocalClusteredQueue(4, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, false, -1, null);
//         office4.bindQueue(new SimpleCondition("topic"), queue3);
//         SimpleReceiver receiver3 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING);
//         queue3.add(receiver3);
//         
//         LocalClusteredQueue queue4 =
//            new LocalClusteredQueue(5, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, false, -1, null);
//         office5.bindQueue(new SimpleCondition("topic"), queue4);
//         SimpleReceiver receiver4 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING);
//         queue4.add(receiver4);
//         
//         LocalClusteredQueue queue5 =
//            new LocalClusteredQueue(6, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, false, -1, null);
//         office6.bindQueue(new SimpleCondition("topic"), queue5);
//         SimpleReceiver receiver5 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING);
//         queue5.add(receiver5);
//               
//         List msgs = sendMessages("topic", persistent, office1, 1, null);         
//         checkContainsAndAcknowledge(msgs, receiver1, queue1);         
//         checkEmpty(receiver2);
//         checkEmpty(receiver3);
//         checkEmpty(receiver4);
//         checkEmpty(receiver5);
//         
//         msgs = sendMessages("topic", persistent, office1, 1, null);         
//         checkEmpty(receiver1);
//         checkContainsAndAcknowledge(msgs, receiver2, queue1);                  
//         checkEmpty(receiver3);
//         checkEmpty(receiver4);
//         checkEmpty(receiver5);
//         
//         msgs = sendMessages("topic", persistent, office1, 1, null);         
//         checkEmpty(receiver1);
//         checkEmpty(receiver2);
//         checkContainsAndAcknowledge(msgs, receiver3, queue1);                           
//         checkEmpty(receiver4);
//         checkEmpty(receiver5);
//         
//         msgs = sendMessages("topic", persistent, office1, 1, null);         
//         checkEmpty(receiver1);
//         checkEmpty(receiver2);
//         checkEmpty(receiver3);
//         checkContainsAndAcknowledge(msgs, receiver4, queue1);                                    
//         checkEmpty(receiver5);
//         
//         msgs = sendMessages("topic", persistent, office1, 1, null);         
//         checkEmpty(receiver1);
//         checkEmpty(receiver2);
//         checkEmpty(receiver3);
//         checkEmpty(receiver4);
//         checkContainsAndAcknowledge(msgs, receiver5, queue1); 
//         
//         msgs = sendMessages("topic", persistent, office1, 1, null);         
//         checkContainsAndAcknowledge(msgs, receiver1, queue1);         
//         checkEmpty(receiver2);
//         checkEmpty(receiver3);
//         checkEmpty(receiver4);
//         checkEmpty(receiver5);
//         
//         msgs = sendMessages("topic", persistent, office1, 1, null);         
//         checkEmpty(receiver1);
//         checkContainsAndAcknowledge(msgs, receiver2, queue1);                  
//         checkEmpty(receiver3);
//         checkEmpty(receiver4);
//         checkEmpty(receiver5);
//         
//                     
//      }
//      finally
//      {
//         if (office1 != null)
//         {            
//            office1.stop();
//         }
//         
//         if (office2 != null)
//         {
//            office2.stop();
//         }
//         
//         if (office3 != null)
//         {            
//            office3.stop();
//         }
//         
//         if (office4 != null)
//         {
//            office4.stop();
//         }
//         
//         if (office5 != null)
//         {            
//            office5.stop();
//         }
//         
//         if (office6 != null)
//         {
//            office6.stop();
//         }
//      }
//   }
//   
//   
//   
//   
//   protected void local(boolean persistent) throws Throwable
//   {
//      PostOffice office1 = null;
//      PostOffice office2 = null;
//      PostOffice office3 = null;
//      PostOffice office4 = null;
//      PostOffice office5 = null;
//      PostOffice office6 = null;
//          
//      try
//      {   
//         office1 = createPostOffice(1, "testgroup", sc, ms, pm, tr);
//         office2 = createPostOffice(2, "testgroup", sc, ms, pm, tr);
//         office3 = createPostOffice(3, "testgroup", sc, ms, pm, tr);
//         office4 = createPostOffice(4, "testgroup", sc, ms, pm, tr);
//         office5 = createPostOffice(5, "testgroup", sc, ms, pm, tr);
//         office6 = createPostOffice(6, "testgroup", sc, ms, pm, tr);
//         
//         LocalClusteredQueue queue1 =
//            new LocalClusteredQueue(2, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, false, -1, null);
//         office2.bindQueue(new SimpleCondition("topic"), queue1);
//         SimpleReceiver receiver1 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING);
//         queue1.add(receiver1);
//         
//         LocalClusteredQueue queue2 =
//            new LocalClusteredQueue(3, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, false, -1, null);
//         office3.bindQueue(new SimpleCondition("topic"), queue2);
//         SimpleReceiver receiver2 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING);
//         queue2.add(receiver2);
//         
//         LocalClusteredQueue queue3 =
//            new LocalClusteredQueue(4, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, false, -1, null);
//         office4.bindQueue(new SimpleCondition("topic"), queue3);
//         SimpleReceiver receiver3 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING);
//         queue3.add(receiver3);
//         
//         LocalClusteredQueue queue4 =
//            new LocalClusteredQueue(5, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, false, -1, null);
//         office5.bindQueue(new SimpleCondition("topic"), queue4);
//         SimpleReceiver receiver4 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING);
//         queue4.add(receiver4);
//         
//         LocalClusteredQueue queue5 =
//            new LocalClusteredQueue(6, "queue1", channelIDManager.getID(), ms, pm,
//                                    true, false, -1, null);
//         office6.bindQueue(new SimpleCondition("topic"), queue5);
//         SimpleReceiver receiver5 = new SimpleReceiver("blah", SimpleReceiver.ACCEPTING);
//         queue5.add(receiver5);
//               
//         List msgs = sendMessages("topic", persistent, office2, 3, null);         
//         checkContainsAndAcknowledge(msgs, receiver1, queue1);         
//         checkEmpty(receiver2);
//         checkEmpty(receiver3);
//         checkEmpty(receiver4);
//         checkEmpty(receiver5);
//         
//         msgs = sendMessages("topic", persistent, office2, 3, null);         
//         checkContainsAndAcknowledge(msgs, receiver1, queue1);         
//         checkEmpty(receiver2);
//         checkEmpty(receiver3);
//         checkEmpty(receiver4);
//         checkEmpty(receiver5);
//         
//         msgs = sendMessages("topic", persistent, office2, 3, null);         
//         checkContainsAndAcknowledge(msgs, receiver1, queue1);         
//         checkEmpty(receiver2);
//         checkEmpty(receiver3);
//         checkEmpty(receiver4);
//         checkEmpty(receiver5);
//         
//         
//         msgs = sendMessages("topic", persistent, office3, 3, null); 
//         checkEmpty(receiver1);
//         checkContainsAndAcknowledge(msgs, receiver2, queue1);                  
//         checkEmpty(receiver3);
//         checkEmpty(receiver4);
//         checkEmpty(receiver5);
//         
//         msgs = sendMessages("topic", persistent, office3, 3, null); 
//         checkEmpty(receiver1);
//         checkContainsAndAcknowledge(msgs, receiver2, queue1);                  
//         checkEmpty(receiver3);
//         checkEmpty(receiver4);
//         checkEmpty(receiver5);
//         
//         msgs = sendMessages("topic", persistent, office3, 3, null); 
//         checkEmpty(receiver1);
//         checkContainsAndAcknowledge(msgs, receiver2, queue1);                  
//         checkEmpty(receiver3);
//         checkEmpty(receiver4);
//         checkEmpty(receiver5);
//         
//                     
//      }
//      finally
//      {
//         if (office1 != null)
//         {            
//            office1.stop();
//         }
//         
//         if (office2 != null)
//         {
//            office2.stop();
//         }
//         
//         if (office3 != null)
//         {            
//            office3.stop();
//         }
//         
//         if (office4 != null)
//         {
//            office4.stop();
//         }
//         
//         if (office5 != null)
//         {            
//            office5.stop();
//         }
//         
//         if (office6 != null)
//         {
//            office6.stop();
//         }
//      }
//   }
   
   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------

}



