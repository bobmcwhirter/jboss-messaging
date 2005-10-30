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
package org.jboss.test.messaging.core;


/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 */
public class LocalTopicAsChannelTest extends TransactionalChannelSupportTest
{
   // Constructors --------------------------------------------------

   public LocalTopicAsChannelTest(String name)
   {
      super(name);
   }
//
//   public void setUp() throws Exception
//   {
//      // Create a LocalTopic to be tested by the superclass tests
//      channel = new LocalTopic("LocalTopicID");
//      receiverOne = new ReceiverImpl("ReceiverOne", ReceiverImpl.HANDLING);
//      ((LocalTopic)channel).add(receiverOne);
//
//      super.setUp();
//   }
//
//   public void tearDown()throws Exception
//   {
//      ((LocalTopic)channel).clear();
//      channel = null;
//      receiverOne = null;
//      super.tearDown();
//   }
//
//   //
//   // This test also runs all ChannelSupportTest's tests
//   //
//
//   public void testDefaultAsynchronous()
//   {
//      assertTrue(!channel.isSynchronous());
//   }
//
//
//   public void testTopicOneReceiver() throws Exception
//   {
//      LocalTopic topic = new LocalTopic("");
//
//      // send without a receiver
//
//      Routable r = new MessageSupport("");
//      assertTrue(topic.handle(r));
//
//      // send with one receiver
//
//      ReceiverImpl rOne = new ReceiverImpl("ReceiverONE", ReceiverImpl.HANDLING);
//      assertTrue(topic.add(rOne));
//
//      r = new MessageSupport("");
//      assertTrue(topic.handle(r));
//
//      Iterator i = rOne.iterator();
//      assertTrue(r == i.next());
//      assertFalse(i.hasNext());
//   }
//
//   public void testTopicTwoReceivers() throws Exception
//   {
//      LocalTopic topic = new LocalTopic("");
//
//      ReceiverImpl rOne = new ReceiverImpl("ReceiverONE", ReceiverImpl.HANDLING);
//      assertTrue(topic.add(rOne));
//
//      ReceiverImpl rTwo = new ReceiverImpl("ReceiverTWO", ReceiverImpl.HANDLING);
//      assertTrue(topic.add(rTwo));
//
//      Routable r = new MessageSupport("");
//      assertTrue(topic.handle(r));
//
//      Iterator iOne = rOne.iterator();
//      assertTrue(r == iOne.next());
//      assertFalse(iOne.hasNext());
//
//      Iterator iTwo = rTwo.iterator();
//      assertTrue(r == iTwo.next());
//      assertFalse(iTwo.hasNext());
//   }
//
//
//   public void testNackingReceiver() throws Exception
//   {
//      LocalTopic topic = new LocalTopic("");
//
//      ReceiverImpl nacking = new ReceiverImpl("ReceiverONE", ReceiverImpl.NACKING);
//      assertTrue(topic.add(nacking));
//
//      Routable r = new MessageSupport("");
//      assertTrue(topic.handle(r));
//      assertTrue(topic.hasMessages());
//
//      Iterator i = nacking.iterator();
//      assertFalse(i.hasNext());
//
//      // enable the nacking receiver
//
//      nacking.setState(ReceiverImpl.HANDLING);
//
//      assertTrue(topic.deliver());
//      assertFalse(topic.hasMessages());
//
//      i = nacking.iterator();
//      assertTrue(r == i.next());
//      assertFalse(i.hasNext());
//   }
//
//   public void testNackingAndHandlingReceivers() throws Exception
//   {
//      LocalTopic topic = new LocalTopic("");
//
//      Routable rOne = new MessageSupport("ONE");
//      assertTrue(topic.handle(rOne));
//      assertFalse(topic.hasMessages());
//
//      ReceiverImpl nacking = new ReceiverImpl("ReceiverA", ReceiverImpl.NACKING);
//      assertTrue(topic.add(nacking));
//
//      Routable rTwo = new MessageSupport("TWO");
//      assertTrue(topic.handle(rTwo));
//      assertTrue(topic.hasMessages());
//      Iterator i = nacking.iterator();
//      assertFalse(i.hasNext());
//
//      ReceiverImpl handling = new ReceiverImpl("ReceiverB", ReceiverImpl.HANDLING);
//      assertTrue(topic.add(handling));
//      i = handling.iterator();
//      assertFalse(i.hasNext());
//
//      Routable rThree = new MessageSupport("THREE");
//      assertTrue(topic.handle(rThree));
//      assertTrue(topic.hasMessages());
//
//      i = nacking.iterator();
//      assertFalse(i.hasNext());
//      i = handling.iterator();
//      assertTrue(rThree == i.next());
//      assertFalse(i.hasNext());
//      handling.clear();
//
//      // enable ReceiverA
//
//      nacking.setState(ReceiverImpl.HANDLING);
//      assertTrue(topic.deliver());
//      assertFalse(topic.hasMessages());
//
//      List l = nacking.getMessages();
//      assertEquals(2, l.size());
//      assertTrue(l.contains(rTwo));
//      assertTrue(l.contains(rThree));
//      i = handling.iterator();
//      assertFalse(i.hasNext());
//   }
//
//   public void testBrokenReceiver() throws Exception
//   {
//      LocalTopic topic = new LocalTopic("");
//
//      ReceiverImpl broken = new ReceiverImpl("ReceiverONE", ReceiverImpl.BROKEN);
//      assertTrue(topic.add(broken));
//
//      Routable r = new MessageSupport("");
//      assertTrue(topic.handle(r));
//      assertFalse(topic.hasMessages());
//
//      Iterator i = broken.iterator();
//      assertFalse(i.hasNext());
//
//      ReceiverImpl handling = new ReceiverImpl("ReceiverTWO", ReceiverImpl.HANDLING);
//      assertTrue(topic.add(handling));
//      assertTrue(topic.handle(r));
//
//      i = broken.iterator();
//      assertFalse(i.hasNext());
//
//      i = handling.iterator();
//      assertTrue(r == i.next());
//      assertFalse(i.hasNext());
//   }
}
