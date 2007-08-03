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

package org.jboss.messaging.core.impl;

import java.util.Iterator;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jboss.messaging.core.contract.ClusterNotification;
import org.jboss.messaging.core.contract.ClusterNotificationListener;
import org.jboss.messaging.core.contract.ClusterNotifier;
import org.jboss.messaging.util.ConcurrentReaderHashSet;

import EDU.oswego.cs.dl.util.concurrent.Callable;
import EDU.oswego.cs.dl.util.concurrent.TimedCallable;


/**
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision: $</tt>21 Jun 2007
 *
 * $Id: $
 *
 */
public class DefaultClusterNotifier implements ClusterNotifier
{
   private static final Logger log = Logger.getLogger(DefaultClusterNotifier.class);
   
   private static final long NOTIFICATION_TIMEOUT = 3000;
	
	private Set listeners;
	
	public DefaultClusterNotifier()
	{
		listeners = new ConcurrentReaderHashSet();
	}

	public void registerListener(ClusterNotificationListener listener)
	{
		if (listeners.contains(listener))
		{
			throw new IllegalStateException("Listener " + listener + " is already registered");
		}
		
		listeners.add(listener);
	}

	public void sendNotification(final ClusterNotification notification)
	{
		Iterator iter = listeners.iterator();
		
		while (iter.hasNext())
		{
			final ClusterNotificationListener listener = (ClusterNotificationListener)iter.next();
			
			listener.notify(notification);
			
			//We used a timed callable to make sure the call completes in a reasonable time
			//This is because there have been issues with remoting hanging when closing message suckers
			//and we don't want this to cause the entire failover process to hang
			
			Callable callable = new Callable() { public Object call() { listener.notify(notification); return null; } };
			
			Callable timedCallable = new TimedCallable(callable, NOTIFICATION_TIMEOUT);
			
			try
			{
				timedCallable.call();
			}
			catch (Exception e)
			{
				log.error("Failed to make notification", e);
			}
		}
	}

	public void unregisterListener(ClusterNotificationListener listener)
	{
		if (!listeners.remove(listener))
		{
			throw new IllegalStateException("Listener " + listener + " is not registered");		
		}
	}

}
