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
package org.jboss.messaging.tests.unit.core.remoting;

import org.jboss.messaging.core.remoting.ConnectionRegistry;
import org.jboss.messaging.core.remoting.impl.ConnectionRegistryImpl;
import org.jboss.messaging.tests.util.UnitTestCase;

/**
 * 
 * A ConnectionRegistryLocatorTest
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class ConnectionRegistryLocatorTest extends UnitTestCase
{
   public void testSingleton()
   {
      ConnectionRegistry reg1 = ConnectionRegistryImpl.instance;
      
      ConnectionRegistry reg2 = ConnectionRegistryImpl.instance;
      
      assertTrue(reg1 == reg2);
      
      assertTrue(reg1 instanceof ConnectionRegistryImpl);
      
      assertTrue(reg2 instanceof ConnectionRegistryImpl);
   }   
}
