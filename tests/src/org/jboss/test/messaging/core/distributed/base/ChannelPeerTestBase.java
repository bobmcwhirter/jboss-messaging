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
package org.jboss.test.messaging.core.distributed.base;


import org.jboss.test.messaging.core.base.ChannelTestBase;
import org.jboss.messaging.core.distributed.Peer;
import org.jboss.messaging.core.distributed.DistributedException;
import org.jboss.messaging.core.distributed.PeerIdentity;
import org.jboss.messaging.core.distributed.util.RpcServer;
import org.jboss.messaging.core.MessageStore;
import org.jboss.messaging.core.Channel;
import org.jgroups.JChannel;
import org.jgroups.blocks.RpcDispatcher;

import java.util.Set;


/**
 * The test strategy is to group at this level all peer-related tests. It assumes two distinct
 * JGroups JChannel instances and two channel peers (channel and channel2)
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public abstract class ChannelPeerTestBase extends ChannelTestBase
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------

   /**
    * @param PING_timeout in ms
    * @param PING_num_initial_members
    */
   public static String generateProperties(int PING_timeout,
                                           int PING_num_initial_members)
   {
      return
         "UDP(mcast_addr=228.8.8.8;mcast_port=45566;ip_ttl=32):"+
         "PING(timeout=" + PING_timeout + ";num_initial_members=" + PING_num_initial_members + "):"+
         "FD(timeout=3000):"+
         "VERIFY_SUSPECT(timeout=1500):"+
         "pbcast.NAKACK(gc_lag=10;retransmit_timeout=600,1200,2400,4800):"+
         "UNICAST(timeout=600,1200,2400,4800):"+
         "pbcast.STABLE(desired_avg_gossip=10000):"+
         "FRAG:"+
         "pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=true;print_local_addr=true)";


   }

   // Attributes ----------------------------------------------------

   protected JChannel jchannel, jchannel2, jchannel3;
   protected RpcDispatcher dispatcher, dispatcher2, dispatcher3;

   protected MessageStore ms2, ms3;
   protected Channel channel2, channel3;

   // Constructors --------------------------------------------------

   public ChannelPeerTestBase(String name)
   {
      super(name);
   }

   // Public --------------------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();

      jchannel = new JChannel(generateProperties(50, 1));
      jchannel2 = new JChannel(generateProperties(900000, 1));
      jchannel3 = new JChannel(generateProperties(900000, 2));

      dispatcher = new RpcDispatcher(jchannel, null, null, new RpcServer("1"));
      dispatcher2 = new RpcDispatcher(jchannel2, null, null, new RpcServer("2"));
      dispatcher3 = new RpcDispatcher(jchannel3, null, null, new RpcServer("3"));

      // connect only the first JChannel
      jchannel.connect("testGroup");

      assertEquals(1, jchannel.getView().getMembers().size());

   }

   public void tearDown() throws Exception
   {
      jchannel.close();
      jchannel2.close();
      jchannel3.close();

      super.tearDown();
   }

   public void testJGroupsChannelNotConnected() throws Exception
   {
      Peer peer = (Peer)channel;

      assertTrue(jchannel.isConnected());
      jchannel.close();

      try
      {
         peer.join();
         fail("should throw DistributedException");
      }
      catch(DistributedException e)
      {
         //OK
      }
   }


   public void testPeerInGroupOfOne() throws Exception
   {
      Peer peer = (Peer)channel;

      assertTrue(jchannel.isConnected());

      PeerIdentity peerIdentity = peer.getPeerIdentity();
      assertEquals(channel.getChannelID(), peerIdentity.getDistributedID());

      peer.join();

      assertTrue(peer.hasJoined());

      assertEquals(peerIdentity, peer.getPeerIdentity());

      Set view = peer.getView();

      assertEquals(1, view.size());
      assertTrue(view.contains(peerIdentity));

      peer.leave();

      assertFalse(peer.hasJoined());

      assertEquals(peerIdentity, peer.getPeerIdentity());

      view = peer.getView();
      assertEquals(0, view.size());
   }

   public void testPeerInGroupOfTwo() throws Exception
   {
      jchannel2.connect("testGroup");

      // allow the group time to form
      Thread.sleep(1000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());

      // make sure both jchannel joined the group
      assertEquals(2, jchannel.getView().getMembers().size());
      assertEquals(2, jchannel2.getView().getMembers().size());

      Peer peer = (Peer)channel;
      Peer peer2 = (Peer)channel2;

      PeerIdentity peerIdentity = peer.getPeerIdentity();
      PeerIdentity peer2Identity = peer2.getPeerIdentity();

      assertEquals(channel.getChannelID(), peerIdentity.getDistributedID());
      assertEquals(channel.getChannelID(), peer2Identity.getDistributedID());
      assertFalse(peerIdentity.getPeerID().equals(peer2Identity.getPeerID()));

      peer.join();
      log.debug("peer has joined");

      assertTrue(peer.hasJoined());

      Set view = peer.getView();
      assertEquals(1, view.size());
      assertTrue(view.contains(peerIdentity));

      peer2.join();
      log.debug("peer2 has joined");

      assertTrue(peer2.hasJoined());

      view = peer.getView();
      assertEquals(2, view.size());
      assertTrue(view.contains(peerIdentity));
      assertTrue(view.contains(peer2Identity));

      view = peer2.getView();
      assertEquals(2, view.size());
      assertTrue(view.contains(peerIdentity));
      assertTrue(view.contains(peer2Identity));

      peer.leave();
      log.debug("peer has left");

      assertFalse(peer.hasJoined());

      view = peer.getView();
      assertEquals(0, view.size());
      view = peer2.getView();
      assertEquals(1, view.size());
      assertTrue(view.contains(peer2Identity));

      peer2.leave();
      log.debug("peer2 has left");

      assertFalse(peer2.hasJoined());

      view = peer.getView();
      assertEquals(0, view.size());
      view = peer2.getView();
      assertEquals(0, view.size());
   }

   public void testPeerInGroupOfThree() throws Exception
   {
      jchannel2.connect("testGroup");
      jchannel3.connect("testGroup");

      // allow the group time to form
      Thread.sleep(2000);

      assertTrue(jchannel.isConnected());
      assertTrue(jchannel2.isConnected());
      assertTrue(jchannel3.isConnected());

      // make sure all three jchannels joined the group
      assertEquals(3, jchannel.getView().getMembers().size());
      assertEquals(3, jchannel2.getView().getMembers().size());
      assertEquals(3, jchannel3.getView().getMembers().size());

      Peer peer = (Peer)channel;
      Peer peer2 = (Peer)channel2;
      Peer peer3 = (Peer)channel3;

      PeerIdentity peerIdentity = peer.getPeerIdentity();
      PeerIdentity peer2Identity = peer2.getPeerIdentity();
      PeerIdentity peer3Identity = peer3.getPeerIdentity();

      assertEquals(channel.getChannelID(), peerIdentity.getDistributedID());
      assertEquals(channel.getChannelID(), peer2Identity.getDistributedID());
      assertEquals(channel.getChannelID(), peer3Identity.getDistributedID());

      assertFalse(peerIdentity.getPeerID().equals(peer2Identity.getPeerID()));
      assertFalse(peerIdentity.getPeerID().equals(peer3Identity.getPeerID()));
      assertFalse(peer2Identity.getPeerID().equals(peer3Identity.getPeerID()));

      peer.join();
      log.debug("peer has joined");

      assertTrue(peer.hasJoined());

      Set view = peer.getView();
      assertEquals(1, view.size());
      assertTrue(view.contains(peerIdentity));

      peer2.join();
      log.debug("peer2 has joined");

      assertTrue(peer2.hasJoined());

      view = peer.getView();
      assertEquals(2, view.size());
      assertTrue(view.contains(peerIdentity));
      assertTrue(view.contains(peer2Identity));

      view = peer2.getView();
      assertEquals(2, view.size());
      assertTrue(view.contains(peerIdentity));
      assertTrue(view.contains(peer2Identity));

      peer3.join();
      log.debug("peer3 has joined");

      assertTrue(peer3.hasJoined());

      view = peer.getView();
      assertEquals(3, view.size());
      assertTrue(view.contains(peerIdentity));
      assertTrue(view.contains(peer2Identity));
      assertTrue(view.contains(peer3Identity));

      view = peer2.getView();
      assertEquals(3, view.size());
      assertTrue(view.contains(peerIdentity));
      assertTrue(view.contains(peer2Identity));
      assertTrue(view.contains(peer3Identity));

      view = peer3.getView();
      assertEquals(3, view.size());
      assertTrue(view.contains(peerIdentity));
      assertTrue(view.contains(peer2Identity));
      assertTrue(view.contains(peer3Identity));

      peer.leave();
      log.debug("peer has left");

      assertFalse(peer.hasJoined());

      view = peer.getView();
      assertEquals(0, view.size());

      view = peer2.getView();
      assertEquals(2, view.size());
      assertTrue(view.contains(peer2Identity));
      assertTrue(view.contains(peer3Identity));

      view = peer3.getView();
      assertEquals(2, view.size());
      assertTrue(view.contains(peer2Identity));
      assertTrue(view.contains(peer3Identity));

      peer2.leave();
      log.debug("peer2 has left");

      assertFalse(peer2.hasJoined());

      view = peer2.getView();
      assertEquals(0, view.size());

      view = peer3.getView();
      assertEquals(1, view.size());
      assertTrue(view.contains(peer3Identity));

      peer3.leave();
      log.debug("peer3 has left");

      assertFalse(peer3.hasJoined());

      view = peer3.getView();
      assertEquals(0, view.size());

   }

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
