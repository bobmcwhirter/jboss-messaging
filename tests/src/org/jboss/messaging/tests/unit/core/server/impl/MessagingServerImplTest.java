/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.messaging.tests.unit.core.server.impl;

import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.tests.util.UnitTestCase;

/**
 * 
 * A MessagingServerImplTest
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class MessagingServerImplTest extends UnitTestCase
{
   private static final Logger log = Logger.getLogger(MessagingServerImplTest.class);
   
   // Private -----------------------------------------------------------------------------------------------------------
   
   public void testDummy()
   {      
   }
   
//   public void testConstructor()
//   {
//      MessagingServer server = new MessagingServerImpl();
//      
//      Version version = VersionLoader.load();      
//      assertEquals(version, server.getVersion());      
//      assertNull(server.getConfiguration());     
//      assertNull(server.getRemotingService());      
//      assertNull(server.getSecurityManager());     
//      assertNull(server.getStorageManager());      
//   }
//   
//   public void testSetGetPlugins() throws Exception
//   {
//      MessagingServer server = new MessagingServerImpl();
//      
//      Configuration config = EasyMock.createMock(Configuration.class);
//      server.setConfiguration(config);
//      assertTrue(config == server.getConfiguration());
//      
//      StorageManager sm = EasyMock.createMock(StorageManager.class);
//      server.setStorageManager(sm);
//      assertTrue(sm == server.getStorageManager());
//      
//      RemotingService rs = EasyMock.createMock(RemotingService.class);
//      server.setRemotingService(rs);
//      assertTrue(rs == server.getRemotingService());
//      
//      JBMSecurityManager jsm = EasyMock.createMock(JBMSecurityManager.class);
//      server.setSecurityManager(jsm);
//      assertTrue(jsm == server.getSecurityManager());
//   }
//   
//   public void testStartStop() throws Exception
//   {
//      MessagingServer server = new MessagingServerImpl();
//      
//      try
//      {
//         server.start();
//         fail("Should throw exception");
//      }
//      catch (IllegalStateException e)
//      {
//         //Ok
//      }
//      
//      server.setConfiguration(new ConfigurationImpl());
//      
//      try
//      {
//         server.start();
//         fail("Should throw exception");
//      }
//      catch (IllegalStateException e)
//      {
//         //Ok
//      }
//      
//      StorageManager sm = EasyMock.createMock(StorageManager.class);
//      
//      server.setStorageManager(sm);
//      
//      try
//      {
//         server.start();
//         fail("Should throw exception");
//      }
//      catch (IllegalStateException e)
//      {
//         //Ok
//      }
//      
//      RemotingService rs = EasyMock.createMock(RemotingService.class);
//      
//      server.setRemotingService(rs);
//      
//      try
//      {
//         server.start();
//         fail("Should throw exception");
//      }
//      catch (IllegalStateException e)
//      {
//         //Ok
//      }
//      
//      JBMSecurityManager sem = EasyMock.createMock(JBMSecurityManager.class);
//                 
//      server.setSecurityManager(sem);
//      
//      try
//      {
//         server.start();
//         fail("Should throw exception");
//      }
//      catch (IllegalStateException e)
//      {
//         //Ok
//      }
//      
//      ManagementService ms = EasyMock.createMock(ManagementService.class);
//      server.setManagementService(ms);
//      
//      try
//      {
//         server.start();
//         fail("Should throw exception");
//      }
//      catch (IllegalStateException e)
//      {
//         //Ok
//      }
//      
//      EasyMock.reset(sm, rs);
//      
//      EasyMock.expect(sm.isStarted()).andStubReturn(true);
//      EasyMock.expect(rs.isStarted()).andStubReturn(false);
//
//      EasyMock.replay(sm, rs);
//      
//      try
//      {
//         server.start();
//         fail("Should throw exception");
//      }
//      catch (IllegalStateException e)
//      {
//         //Ok
//      }
//      
//      EasyMock.reset(sm, rs);
//      
//      PacketDispatcher pd = EasyMock.createMock(PacketDispatcher.class);
//      EasyMock.expect(rs.getDispatcher()).andReturn(pd);
//      EasyMock.expect(sm.isStarted()).andStubReturn(true);
//      EasyMock.expect(rs.isStarted()).andStubReturn(true);
//      sm.loadBindings(EasyMock.isA(QueueFactoryImpl.class), EasyMock.isA(ArrayList.class), EasyMock.isA(ArrayList.class));
//      sm.loadMessages(EasyMock.isA(PostOfficeImpl.class), EasyMock.isA(Map.class));
//            
//      pd.register(EasyMock.isA(MessagingServerPacketHandler.class));
//      
//      MessagingServerControlMBean managedServer = EasyMock.createMock(MessagingServerControlMBean.class);
//      expect(ms.registerServer(isA(PostOffice.class), eq(sm), eq(server
//            .getConfiguration()), isA(HierarchicalRepository.class), eq(server
//            .getQueueSettingsRepository()), eq(server))).andReturn(managedServer);
//
//      EasyMock.replay(sm, rs, pd, ms);
//      
//      assertFalse(server.isStarted());
//      
//      server.start();
//      
//      assertTrue(server.isStarted());
//      
//      EasyMock.verify(sm, rs, pd, ms);
//      
//      EasyMock.reset(sm, rs, pd);
//      
//      assertNotNull(server.getQueueSettingsRepository());
//      
//      //Starting again should do nothing
//      
//      EasyMock.replay(sm, rs, pd);
//      
//      server.start();
//      
//      assertTrue(server.isStarted());
//      
//      EasyMock.verify(sm, rs, pd);
//      
//      EasyMock.reset(sm, rs, pd);
//      
//      //Can't set the plugins when server is started
//      
//      try
//      {
//         server.setConfiguration(new ConfigurationImpl());
//         fail("Should throw exception");
//      }
//      catch (IllegalStateException e)
//      {
//         //Ok
//      }
//      
//      try
//      {
//         server.setStorageManager(sm);
//         fail("Should throw exception");
//      }
//      catch (IllegalStateException e)
//      {
//         //Ok
//      }
//      
//      try
//      {
//         server.setRemotingService(rs);
//         fail("Should throw exception");
//      }
//      catch (IllegalStateException e)
//      {
//         //Ok
//      }
//      
//      try
//      {
//         server.setSecurityManager(sem);
//         fail("Should throw exception");
//      }
//      catch (IllegalStateException e)
//      {
//         //Ok
//      }
//      
//      pd.unregister(0);
//      
//      EasyMock.replay(sm, rs, pd);
//      
//      server.stop();
//      
//      assertFalse(server.isStarted());
//      
//      EasyMock.verify(sm, rs, pd);
//      
//      EasyMock.reset(sm, rs, pd);
//      
//      //Stopping again should do nothing
//      
//      EasyMock.replay(sm, rs, pd);
//      
//      server.stop();
//      
//      assertFalse(server.isStarted());
//      
//      EasyMock.verify(sm, rs, pd);
//      
//      EasyMock.reset(sm, rs, pd, ms);
//      
//      EasyMock.expect(rs.getDispatcher()).andReturn(pd);
//      EasyMock.expect(sm.isStarted()).andStubReturn(true);
//      EasyMock.expect(rs.isStarted()).andStubReturn(true);
//      sm.loadBindings(EasyMock.isA(QueueFactoryImpl.class), EasyMock.isA(ArrayList.class), EasyMock.isA(ArrayList.class));
//      sm.loadMessages(EasyMock.isA(PostOfficeImpl.class), EasyMock.isA(Map.class));
//            
//      pd.register(EasyMock.isA(MessagingServerPacketHandler.class));
//      
//      expect(ms.registerServer(isA(PostOffice.class), eq(sm), eq(server
//            .getConfiguration()), isA(HierarchicalRepository.class), eq(server
//            .getQueueSettingsRepository()), eq(server))).andReturn(managedServer);
//
//      EasyMock.replay(sm, rs, pd, ms);
//                 
//      //Should be able to start again
//      server.start();
//      
//      EasyMock.verify(sm, rs, pd, ms);
//      
//      assertTrue(server.isStarted());
//      
//      assertNotNull(server.getQueueSettingsRepository());
//      
//      EasyMock.reset(sm, rs, pd);
//      
//      pd.unregister(0);
//  
//      EasyMock.replay(sm, rs, pd);
//      
//      //And stop
//      server.stop();
//      
//      assertFalse(server.isStarted());
//      
//      EasyMock.verify(sm, rs, pd);
//   }
//   
//   public void testCreateConnectionIncompatibleVersion() throws Exception
//   {
//      Version version = VersionLoader.load();
//      
//      MessagingServer server = new MessagingServerImpl();
//                  
//      try
//      {
//         server.createSession("blah", null, null, version.getIncrementingVersion() + 1,
//                  null, false, false, false, 124152);
//         fail("Should throw exception");
//      }
//      catch (MessagingException e)
//      {
//         assertEquals(MessagingException.INCOMPATIBLE_CLIENT_SERVER_VERSIONS, e.getCode());
//      }            
//   }
//   
//   public void testCreateConnectionFailAuthentication() throws Exception
//   {      
//      MessagingServerImpl server = new MessagingServerImpl();          
//      server.setConfiguration(new ConfigurationImpl());            
//      StorageManager sm = EasyMock.createMock(StorageManager.class);      
//      server.setStorageManager(sm);      
//      RemotingService rs = EasyMock.createMock(RemotingService.class);      
//      server.setRemotingService(rs);
//      
//      JBMSecurityManager sem = new JBMSecurityManager()
//      {
//         public boolean validateUser(String user, String password)
//         {
//            return false;
//         }
//
//         public boolean validateUserAndRole(String user, String password, Set<Role> roles, CheckType checkType)
//         {
//            return false;
//         }
//      };
//      
//      server.setSecurityManager(sem);
//      
//      ManagementService mr = EasyMock.createMock(ManagementService.class);
//      MessagingServerControlMBean managedServer = EasyMock.createMock(MessagingServerControlMBean.class);
//      expect(mr.registerServer(isA(PostOffice.class), eq(sm), eq(server
//            .getConfiguration()), isA(HierarchicalRepository.class), eq(server
//            .getQueueSettingsRepository()), eq(server))).andReturn(managedServer);
//      server.setManagementService(mr);
//
//      PacketDispatcher pd = EasyMock.createMock(PacketDispatcher.class);
//      EasyMock.expect(rs.getDispatcher()).andReturn(pd);
//      
//      sm.loadBindings(EasyMock.isA(QueueFactoryImpl.class), EasyMock.isA(ArrayList.class), EasyMock.isA(ArrayList.class));
//      sm.loadMessages(EasyMock.isA(PostOfficeImpl.class), EasyMock.isA(Map.class));
//
//      pd.register(EasyMock.isA(MessagingServerPacketHandler.class));      
//      EasyMock.expect(sm.isStarted()).andStubReturn(true);
//      EasyMock.expect(rs.isStarted()).andStubReturn(true);
//      
//      EasyMock.replay(rs, sm, pd);
//      
//      server.start();
//            
//      Version version = VersionLoader.load();
//                     
//      try
//      {
//         server.createSession("blah", "kajsjk", "asuh", version.getIncrementingVersion(),
//                  null, false, false, false, 124152);
//         fail("Should throw exception");
//      }
//      catch (MessagingException e)
//      {
//         assertEquals(MessagingException.SECURITY_EXCEPTION, e.getCode());
//      }   
//   }
//   
//   public void testCreateSessionOK() throws Exception
//   {      
//      MessagingServerImpl server = new MessagingServerImpl();          
//      server.setConfiguration(new ConfigurationImpl());            
//      StorageManager sm = EasyMock.createMock(StorageManager.class);      
//      server.setStorageManager(sm);      
//      RemotingService rs = EasyMock.createMock(RemotingService.class);      
//      server.setRemotingService(rs);
//      JBMSecurityManager sem = new JBMSecurityManager()
//      {
//         public boolean validateUser(String user, String password)
//         {
//            return true;
//         }
//
//         public boolean validateUserAndRole(String user, String password, Set<Role> roles, CheckType checkType)
//         {
//            return true;
//         }
//      };
//      
//      server.setSecurityManager(sem);
//      
//      ManagementService mr = EasyMock.createMock(ManagementService.class);
//      MessagingServerControlMBean managedServer = EasyMock.createMock(MessagingServerControlMBean.class);
//      expect(mr.registerServer(isA(PostOffice.class), eq(sm), eq(server
//            .getConfiguration()), isA(HierarchicalRepository.class), eq(server
//            .getQueueSettingsRepository()), eq(server))).andReturn(managedServer);
//      server.setManagementService(mr);
//
//      PacketDispatcher pd = EasyMock.createMock(PacketDispatcher.class);
//      EasyMock.expect(rs.getDispatcher()).andReturn(pd);
//      
//      sm.loadBindings(EasyMock.isA(QueueFactoryImpl.class), EasyMock.isA(ArrayList.class), EasyMock.isA(ArrayList.class));
//      sm.loadMessages(EasyMock.isA(PostOfficeImpl.class), EasyMock.isA(Map.class));
//      
//      pd.register(EasyMock.isA(MessagingServerPacketHandler.class));      
//      EasyMock.expect(sm.isStarted()).andStubReturn(true);
//      EasyMock.expect(rs.isStarted()).andStubReturn(true);
//            
//      final long localCommandResponseID = 87879;
//      EasyMock.expect(pd.generateID()).andReturn(localCommandResponseID);
//      
//      final long id = 129812;
//      EasyMock.expect(pd.generateID()).andReturn(id);
//
//      pd.register(EasyMock.isA(ServerSessionPacketHandler.class));
//            
//      RemotingConnection rc = EasyMock.createStrictMock(RemotingConnection.class);  
//      EasyMock.expect(rc.getPacketDispatcher()).andStubReturn(pd);
//      
//      long txID = 12767126;
//      EasyMock.expect(sm.generateTransactionID()).andReturn(txID);
//      
//      pd.register(EasyMock.isA(CommandManagerImpl.class));
//      
//      rc.addFailureListener(EasyMock.isA(ServerSessionImpl.class));
//      
//      EasyMock.replay(rs, sm, pd, rc);
//      
//      server.start();
//      final String username = "okasokas";
//      final String password = "oksokasws";
//      
//      
//      final String name = "blah";
//      CreateSessionResponseMessage resp = server.createSession(name, username, password, 45, rc,
//               false, false, false, 12345);
//      
//      EasyMock.verify(rs, sm, pd, rc);
//      
//      assertEquals(VersionLoader.load().getIncrementingVersion(), resp.getServerVersion());
//      assertEquals(id, resp.getSessionID());       
//      assertEquals(localCommandResponseID, resp.getCommandResponseTargetID());  
//      assertEquals(1, server.getSessions().size());
//      ServerSession session = server.getSessions().get(name);
//      assertEquals(username, session.getUsername());
//      assertEquals(password, session.getPassword());
//   }
//   
  
}
