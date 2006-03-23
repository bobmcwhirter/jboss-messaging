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
package org.jboss.jms.client.state;

import java.util.HashSet;

import org.jboss.jms.client.remoting.JMSRemotingConnection;
import org.jboss.jms.delegate.ConnectionDelegate;
import org.jboss.jms.message.MessageIdGenerator;
import org.jboss.jms.server.Version;
import org.jboss.jms.tx.ResourceManager;
import org.jboss.logging.Logger;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.SyncSet;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

/**
 * 
 * State corresponding to a connection. This state is acessible inside aspects/interceptors.
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class ConnectionState extends HierarchicalStateSupport
{
   private static final Logger log = Logger.getLogger(ConnectionState.class);
   
   private JMSRemotingConnection remotingConnection;
   
   private ResourceManager resourceManager;
   
   private MessageIdGenerator idGenerator;
   
   //Thread pool used for making asynch calls to server - e.g. activateConsumer
   private PooledExecutor pooledExecutor;
 
   private Version versionToUse;
   
   public ConnectionState(ConnectionDelegate delegate,
                          JMSRemotingConnection remotingConnection, Version versionToUse,
                          ResourceManager rm,
                          MessageIdGenerator gen)
      throws Exception
   {
      super(null, delegate);
      
      if (log.isTraceEnabled()) { log.trace("Creating connection state"); }
      
      children = new SyncSet(new HashSet(), new WriterPreferenceReadWriteLock());
            
      //TODO size should be configurable
      pooledExecutor = new PooledExecutor(new LinkedQueue(), 50);
      pooledExecutor.setMinimumPoolSize(50);
      
      this.remotingConnection = remotingConnection;
      
      this.versionToUse = versionToUse;
      
      this.resourceManager = rm;
      
      this.idGenerator = gen;
   }
    
   public ResourceManager getResourceManager()
   {
      return resourceManager;
   }
   
   public MessageIdGenerator getIdGenerator()
   {
      return idGenerator;
   }
   
   public PooledExecutor getPooledExecutor()
   {
      return pooledExecutor;
   }
   
   public JMSRemotingConnection getRemotingConnection()
   {
      return remotingConnection;
   }
   
   public Version getVersionToUse()
   {
      return versionToUse;
   }
    
}
