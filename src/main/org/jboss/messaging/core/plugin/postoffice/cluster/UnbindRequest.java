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
package org.jboss.messaging.core.plugin.postoffice.cluster;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * A UnbindRequest
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision: 1.1 $</tt>
 *
 * $Id$
 *
 */
class UnbindRequest extends ClusterRequest
{
   static final int TYPE = 9;

   private int nodeId;   
   
   private String queueName;
   
   UnbindRequest()
   {      
   }

   UnbindRequest(int nodeId, String queueName)
   {
      this.nodeId = nodeId;
      
      this.queueName = queueName;
   }

   Object execute(PostOfficeInternal office) throws Exception
   {
      // TODO: Due to failoever we can't guarantee unique key by nodeId and queueName any more.
      // We might need to revist this
      office.removeBindingFromCluster(nodeId, queueName);
      
      return null;
   }
   
   byte getType()
   {
      return TYPE;
   }

   public void read(DataInputStream in) throws Exception
   {
      nodeId = in.readInt();
      
      queueName = in.readUTF();
   }

   public void write(DataOutputStream out) throws Exception
   {
      out.writeInt(nodeId);
      
      out.writeUTF(queueName);
   }      
}
