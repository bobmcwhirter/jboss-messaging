/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2009, Red Hat Middleware LLC, and individual contributors
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


package org.jboss.messaging.tests.integration.cluster.distribution;

import org.jboss.messaging.core.logging.Logger;

/**
 * A OnewayTwoNodeClusterTest
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Created 30 Jan 2009 18:03:28
 *
 *
 */
public class OnewayTwoNodeClusterTest extends ClusterTestBase
{
   private static final Logger log = Logger.getLogger(OnewayTwoNodeClusterTest.class);

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      setupServer(0, isFileStorage(), isNetty());
      setupServer(1, isFileStorage(), isNetty());            
   }

   @Override
   protected void tearDown() throws Exception
   {
      closeAllConsumers();
      
      closeAllSessionFactories();
      
      stopServers(0, 1);
      
      super.tearDown();
   }
   
   protected boolean isNetty()
   {
      return false;
   }
   
   protected boolean isFileStorage()
   {
      return false;
   }
   
   public void testStartTargetServerBeforeSourceServer() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());

      String myFilter = "zebra";
      
      createQueue(1, "queues.testaddress", "queue0", myFilter, false);
      addConsumer(0, 1, "queue0", null);

      waitForBindings(0, "queues.testaddress", 1, 1, false);

      send(0, "queues.testaddress", 10, false, myFilter);
      verifyReceiveAll(10, 0);
      verifyNotReceive(0);
      
      send(0, "queues.testaddress", 10, false, null);    
      verifyNotReceive(0);
   }
   
   public void testStartSourceServerBeforeTargetServer() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(0, 1);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      String myFilter = "bison";

      createQueue(1, "queues.testaddress", "queue0", myFilter, false);
      addConsumer(0, 1, "queue0", null);
          
      waitForBindings(0, "queues.testaddress", 1, 1, false);

      send(0, "queues.testaddress", 10, false, myFilter);
      verifyReceiveAll(10, 0);
      verifyNotReceive(0);
      
      send(0, "queues.testaddress", 10, false, null);
      verifyNotReceive(0);
   }
   
   public void testStopAndStartTarget() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(0, 1);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      String myFilter = "bison";

      createQueue(1, "queues.testaddress", "queue0", myFilter, false);
      addConsumer(0, 1, "queue0", null);
          
      waitForBindings(0, "queues.testaddress", 1, 1, false);

      send(0, "queues.testaddress", 10, false, myFilter);
      verifyReceiveAll(10, 0);
      verifyNotReceive(0);
      
      send(0, "queues.testaddress", 10, false, null);
      verifyNotReceive(0);
      
      removeConsumer(0);
      closeSessionFactory(1);
      
      long start = System.currentTimeMillis();
      
      stopServers(1);
      
      log.info("*** stopped service 1");
      
      log.info("** starting server 1");
      
      startServers(1);
      
      log.info("*** started service 1");
      
      long end = System.currentTimeMillis();
      
      //We time how long it takes to restart, since it has been known to hang in the past and wait for a timeout
      //Shutting down and restarting should be pretty quick
      
      assertTrue("Took too long to restart", end - start <= 5000);
      
      setupSessionFactory(1, isNetty());
      
      waitForBindings(0, "queues.testaddress", 0, 0, false);
      
      createQueue(1, "queues.testaddress", "queue0", myFilter, false);
      
      log.info("** adding consumer");
      
      addConsumer(0, 1, "queue0", null);
      
      log.info("** added consumer");
          
      waitForBindings(1, "queues.testaddress", 1, 1, true);
      waitForBindings(0, "queues.testaddress", 1, 1, false);

      send(0, "queues.testaddress", 10, false, myFilter);
      verifyReceiveAll(10, 0);
      verifyNotReceive(0);
      
      send(0, "queues.testaddress", 10, false, null);
      verifyNotReceive(0);
   }

   public void testBasicLocalReceive() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      addConsumer(0, 0, "queue0", null);

      send(0, "queues.testaddress", 10, false, null);
      verifyReceiveAll(10, 0);
      verifyNotReceive(0);

      addConsumer(1, 0, "queue0", null);
      verifyNotReceive(1);
   }

   public void testBasicRoundRobin() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);

      createQueue(1, "queues.testaddress", "queue0", null, false);

      addConsumer(0, 0, "queue0", null);

      addConsumer(1, 1, "queue0", null);

      waitForBindings(0, "queues.testaddress", 1, 1, true);
      waitForBindings(0, "queues.testaddress", 1, 1, false);

      send(0, "queues.testaddress", 10, false, null);
      verifyReceiveRoundRobin(10, 0, 1);
      verifyNotReceive(0, 1);
   }
   
   public void testRoundRobinMultipleQueues() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue0", null, false);
      
      createQueue(0, "queues.testaddress", "queue1", null, false);
      createQueue(1, "queues.testaddress", "queue1", null, false);
      
      createQueue(0, "queues.testaddress", "queue2", null, false);
      createQueue(1, "queues.testaddress", "queue2", null, false);

      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue0", null);
      
      addConsumer(2, 0, "queue1", null);
      addConsumer(3, 1, "queue1", null);
      
      addConsumer(4, 0, "queue2", null);
      addConsumer(5, 1, "queue2", null);

      waitForBindings(0, "queues.testaddress", 3, 3, true);
      waitForBindings(0, "queues.testaddress", 3, 3, false);

      send(0, "queues.testaddress", 10, false, null);
                  
      verifyReceiveRoundRobin(10, 0, 1);
      
      verifyReceiveRoundRobin(10, 2, 3);
      
      verifyReceiveRoundRobin(10, 4, 5);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5);
   }
         
   public void testMultipleNonLoadBalancedQueues() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(0, "queues.testaddress", "queue1", null, false);
      createQueue(0, "queues.testaddress", "queue2", null, false);
      createQueue(0, "queues.testaddress", "queue3", null, false);
      createQueue(0, "queues.testaddress", "queue4", null, false);
    
      
      createQueue(1, "queues.testaddress", "queue5", null, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(1, "queues.testaddress", "queue7", null, false);
      createQueue(1, "queues.testaddress", "queue8", null, false);
      createQueue(1, "queues.testaddress", "queue9", null, false);

      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      addConsumer(3, 0, "queue3", null);
      addConsumer(4, 0, "queue4", null);
      
      addConsumer(5, 1, "queue5", null);
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 1, "queue7", null);
      addConsumer(8, 1, "queue8", null);
      addConsumer(9, 1, "queue9", null);
           
      waitForBindings(0, "queues.testaddress", 5, 5, true);
      waitForBindings(0, "queues.testaddress", 5, 5, false);

      send(0, "queues.testaddress", 10, false, null);
                  
      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
   }
   
   public void testMixtureLoadBalancedAndNonLoadBalancedQueues() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(0, "queues.testaddress", "queue1", null, false);
      createQueue(0, "queues.testaddress", "queue2", null, false);
      createQueue(0, "queues.testaddress", "queue3", null, false);
      createQueue(0, "queues.testaddress", "queue4", null, false);
    
      
      createQueue(1, "queues.testaddress", "queue5", null, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(1, "queues.testaddress", "queue7", null, false);
      createQueue(1, "queues.testaddress", "queue8", null, false);
      createQueue(1, "queues.testaddress", "queue9", null, false);
      
      createQueue(0, "queues.testaddress", "queue10", null, false);
      createQueue(1, "queues.testaddress", "queue10", null, false);
      
      createQueue(0, "queues.testaddress", "queue11", null, false);
      createQueue(1, "queues.testaddress", "queue11", null, false);
      
      createQueue(0, "queues.testaddress", "queue12", null, false);
      createQueue(1, "queues.testaddress", "queue12", null, false);

      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      addConsumer(3, 0, "queue3", null);
      addConsumer(4, 0, "queue4", null);
      
      addConsumer(5, 1, "queue5", null);
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 1, "queue7", null);
      addConsumer(8, 1, "queue8", null);
      addConsumer(9, 1, "queue9", null);
      
      addConsumer(10, 0, "queue10", null);
      addConsumer(11, 1, "queue10", null);
      
      addConsumer(12, 0, "queue11", null);
      addConsumer(13, 1, "queue11", null);
      
      addConsumer(14, 0, "queue12", null);
      addConsumer(15, 1, "queue12", null);
           
      waitForBindings(0, "queues.testaddress", 8, 8, true);
      waitForBindings(0, "queues.testaddress", 8, 8, false);

      send(0, "queues.testaddress", 10, false, null);
                  
      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      
      verifyReceiveRoundRobin(10, 10, 11);
      verifyReceiveRoundRobin(10, 12, 13);
      verifyReceiveRoundRobin(10, 14, 15);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
   }
   
   public void testMixtureLoadBalancedAndNonLoadBalancedQueuesAddQueuesOnTargetBeforeStartSource() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1);
     
      setupSessionFactory(1, isNetty());
      
      createQueue(1, "queues.testaddress", "queue5", null, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(1, "queues.testaddress", "queue7", null, false);
      createQueue(1, "queues.testaddress", "queue8", null, false);
      createQueue(1, "queues.testaddress", "queue9", null, false);    
      
      createQueue(1, "queues.testaddress", "queue10", null, false);      
      createQueue(1, "queues.testaddress", "queue11", null, false);      
      createQueue(1, "queues.testaddress", "queue12", null, false);
      

      addConsumer(5, 1, "queue5", null);
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 1, "queue7", null);
      addConsumer(8, 1, "queue8", null);
      addConsumer(9, 1, "queue9", null); 
      
      addConsumer(11, 1, "queue10", null);      
      addConsumer(13, 1, "queue11", null);      
      addConsumer(15, 1, "queue12", null);
      
      startServers(0);
      
      waitForBindings(0, "queues.testaddress", 8, 8, false);
       
      setupSessionFactory(0, isNetty());
      
      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(0, "queues.testaddress", "queue1", null, false);
      createQueue(0, "queues.testaddress", "queue2", null, false);
      createQueue(0, "queues.testaddress", "queue3", null, false);
      createQueue(0, "queues.testaddress", "queue4", null, false);   
      
      createQueue(0, "queues.testaddress", "queue10", null, false);         
      createQueue(0, "queues.testaddress", "queue11", null, false);
      createQueue(0, "queues.testaddress", "queue12", null, false);
     
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      addConsumer(3, 0, "queue3", null);
      addConsumer(4, 0, "queue4", null);
      
      addConsumer(10, 0, "queue10", null);          
      addConsumer(12, 0, "queue11", null);           
      addConsumer(14, 0, "queue12", null);
                 
      send(0, "queues.testaddress", 10, false, null);
                  
      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      
      verifyReceiveRoundRobin(10, 11, 10);
      verifyReceiveRoundRobin(10, 13, 12);
      verifyReceiveRoundRobin(10, 15, 14);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
   }
   
   public void testMixtureLoadBalancedAndNonLoadBalancedQueuesAddQueuesOnSourceBeforeStartTarget() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(0);
     
      setupSessionFactory(0, isNetty());
      
      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(0, "queues.testaddress", "queue1", null, false);
      createQueue(0, "queues.testaddress", "queue2", null, false);
      createQueue(0, "queues.testaddress", "queue3", null, false);
      createQueue(0, "queues.testaddress", "queue4", null, false);   
      
      createQueue(0, "queues.testaddress", "queue10", null, false);         
      createQueue(0, "queues.testaddress", "queue11", null, false);
      createQueue(0, "queues.testaddress", "queue12", null, false);
     
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      addConsumer(3, 0, "queue3", null);
      addConsumer(4, 0, "queue4", null);
      
      addConsumer(10, 0, "queue10", null);          
      addConsumer(12, 0, "queue11", null);           
      addConsumer(14, 0, "queue12", null);
      
      startServers(1);
      
      setupSessionFactory(1, isNetty());
      
      createQueue(1, "queues.testaddress", "queue5", null, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(1, "queues.testaddress", "queue7", null, false);
      createQueue(1, "queues.testaddress", "queue8", null, false);
      createQueue(1, "queues.testaddress", "queue9", null, false);    
      
      createQueue(1, "queues.testaddress", "queue10", null, false);      
      createQueue(1, "queues.testaddress", "queue11", null, false);      
      createQueue(1, "queues.testaddress", "queue12", null, false);
      

      addConsumer(5, 1, "queue5", null);
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 1, "queue7", null);
      addConsumer(8, 1, "queue8", null);
      addConsumer(9, 1, "queue9", null); 
      
      addConsumer(11, 1, "queue10", null);      
      addConsumer(13, 1, "queue11", null);      
      addConsumer(15, 1, "queue12", null);
      
      waitForBindings(0, "queues.testaddress", 8, 8, false);
            
      send(0, "queues.testaddress", 10, false, null);
                  
      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      
      verifyReceiveRoundRobin(10, 10, 11);
      verifyReceiveRoundRobin(10, 12, 13);
      verifyReceiveRoundRobin(10, 14, 15);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
   }
   
   public void testNotRouteToNonMatchingAddress() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);                
      createQueue(1, "queues.testaddress", "queue1", null, false);
      
      createQueue(0, "queues.testaddress2", "queue2", null, false);
      createQueue(1, "queues.testaddress2", "queue2", null, false);
      createQueue(0, "queues.testaddress2", "queue3", null, false);
      createQueue(1, "queues.testaddress2", "queue4", null, false);
            
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      addConsumer(3, 1, "queue2", null);
      addConsumer(4, 0, "queue3", null);
      addConsumer(5, 1, "queue4", null);
                 
      waitForBindings(0, "queues.testaddress", 1, 1, true);
      waitForBindings(0, "queues.testaddress", 1, 1, false);
      
      waitForBindings(0, "queues.testaddress2", 2, 2, true);
      waitForBindings(0, "queues.testaddress2", 2, 2, false);

      send(0, "queues.testaddress", 10, false, null);
                  
      verifyReceiveAll(10, 0, 1);
      
      verifyNotReceive(2, 3, 4, 5);
   }
   
   public void testNonLoadBalancedQueuesWithFilters() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      String filter1 = "giraffe";
      String filter2 = "aardvark";

      createQueue(0, "queues.testaddress", "queue0", filter1, false);
      createQueue(0, "queues.testaddress", "queue1", filter2, false);
      createQueue(0, "queues.testaddress", "queue2", filter1, false);
      createQueue(0, "queues.testaddress", "queue3", filter2, false);
      createQueue(0, "queues.testaddress", "queue4", filter1, false);
    
      
      createQueue(1, "queues.testaddress", "queue5", filter2, false);
      createQueue(1, "queues.testaddress", "queue6", filter1, false);
      createQueue(1, "queues.testaddress", "queue7", filter2, false);
      createQueue(1, "queues.testaddress", "queue8", filter1, false);
      createQueue(1, "queues.testaddress", "queue9", filter2, false);
      
      createQueue(0, "queues.testaddress", "queue10", null, false);
      createQueue(1, "queues.testaddress", "queue11", null, false);

      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      addConsumer(3, 0, "queue3", null);
      addConsumer(4, 0, "queue4", null);
      
      addConsumer(5, 1, "queue5", null);
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 1, "queue7", null);
      addConsumer(8, 1, "queue8", null);
      addConsumer(9, 1, "queue9", null);
      
      addConsumer(10, 0, "queue10", null);
      
      addConsumer(11, 1, "queue11", null);
           
      waitForBindings(0, "queues.testaddress", 6, 6, true);
      waitForBindings(0, "queues.testaddress", 6, 6, false);

      send(0, "queues.testaddress", 10, false, filter1);
                  
      verifyReceiveAll(10, 0, 2, 4, 6, 8, 10, 11);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
      
      send(0, "queues.testaddress", 10, false, filter2);
      
      verifyReceiveAll(10, 1, 3, 5, 7, 9, 10, 11);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
   }
   
   public void testRoundRobinMultipleQueuesWithFilters() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      String filter1 = "giraffe";
      String filter2 = "aardvark";

      createQueue(0, "queues.testaddress", "queue0", filter1, false);
      createQueue(1, "queues.testaddress", "queue0", filter1, false);
      
      createQueue(0, "queues.testaddress", "queue1", filter1, false);
      createQueue(1, "queues.testaddress", "queue1", filter2, false);
      
      createQueue(0, "queues.testaddress", "queue2", filter2, false);
      createQueue(1, "queues.testaddress", "queue2", filter1, false);
      
      createQueue(0, "queues.testaddress", "queue3", filter2, false);
      createQueue(1, "queues.testaddress", "queue3", filter2, false);
      
      createQueue(0, "queues.testaddress", "queue4", null, false);
      createQueue(1, "queues.testaddress", "queue4", null, false);

      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue0", null);
      
      addConsumer(2, 0, "queue1", null);
      addConsumer(3, 1, "queue1", null);
      
      addConsumer(4, 0, "queue2", null);
      addConsumer(5, 1, "queue2", null);
      
      addConsumer(6, 0, "queue3", null);
      addConsumer(7, 1, "queue3", null);
      
      addConsumer(8, 0, "queue4", null);
      addConsumer(9, 1, "queue4", null);

      waitForBindings(0, "queues.testaddress", 5, 5, true);
      waitForBindings(0, "queues.testaddress", 5, 5, false);

      send(0, "queues.testaddress", 10, false, filter1);
                  
      verifyReceiveRoundRobin(10, 0, 1);
      verifyReceiveRoundRobin(10, 8, 9);
      
      verifyReceiveAll(10, 2, 5);
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            
      send(0, "queues.testaddress", 10, false, filter2);
      
      verifyReceiveRoundRobin(10, 6, 7);
      verifyReceiveRoundRobin(10, 8, 9);
      
      verifyReceiveAll(10, 3, 4);
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      
   }

   public void testRouteWhenNoConsumersFalseNonBalancedQueues() throws Exception
   {
      setupClusterConnection("cluster2", 0, 1, "queues2", false, 1, isNetty());
      
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      createQueue(0, "queues2.testaddress", "queue0", null, false);
      createQueue(0, "queues2.testaddress", "queue1", null, false);
      createQueue(0, "queues2.testaddress", "queue2", null, false);
      
      createQueue(1, "queues2.testaddress", "queue3", null, false);
      createQueue(1, "queues2.testaddress", "queue4", null, false);
      createQueue(1, "queues2.testaddress", "queue5", null, false);
      
      waitForBindings(0, "queues2.testaddress", 3, 0, true);
      waitForBindings(0, "queues2.testaddress", 3, 0, false);
      
      send(0, "queues2.testaddress", 10, false, null);
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      
      addConsumer(3, 1, "queue3", null);
      addConsumer(4, 1, "queue4", null);
      addConsumer(5, 1, "queue5", null);
                  
      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5);      
   }
   
   public void testRouteWhenNoConsumersTrueNonBalancedQueues() throws Exception
   {
      setupClusterConnection("cluster2", 0, 1, "queues2", true, 1, isNetty());
      
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      createQueue(0, "queues2.testaddress", "queue0", null, false);
      createQueue(0, "queues2.testaddress", "queue1", null, false);
      createQueue(0, "queues2.testaddress", "queue2", null, false);
      
      createQueue(1, "queues2.testaddress", "queue3", null, false);
      createQueue(1, "queues2.testaddress", "queue4", null, false);
      createQueue(1, "queues2.testaddress", "queue5", null, false);
      
      waitForBindings(0, "queues2.testaddress", 3, 0, true);
      waitForBindings(0, "queues2.testaddress", 3, 0, false);
      
      send(0, "queues2.testaddress", 10, false, null);
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      
      addConsumer(3, 1, "queue3", null);
      addConsumer(4, 1, "queue4", null);
      addConsumer(5, 1, "queue5", null);
                  
      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5);      
   }
   
   public void testRouteWhenNoConsumersFalseLoadBalancedQueues() throws Exception
   {
      setupClusterConnection("cluster2", 0, 1, "queues2", false, 1, isNetty());
      
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      createQueue(0, "queues2.testaddress", "queue0", null, false);
      createQueue(0, "queues2.testaddress", "queue1", null, false);
      createQueue(0, "queues2.testaddress", "queue2", null, false);
      
      createQueue(1, "queues2.testaddress", "queue0", null, false);
      createQueue(1, "queues2.testaddress", "queue1", null, false);
      createQueue(1, "queues2.testaddress", "queue2", null, false);
      
      waitForBindings(0, "queues2.testaddress", 3, 0, true);
      waitForBindings(0, "queues2.testaddress", 3, 0, false);
      
      send(0, "queues2.testaddress", 10, false, null);
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      
      addConsumer(3, 1, "queue0", null);
      addConsumer(4, 1, "queue1", null);
      addConsumer(5, 1, "queue2", null);
      
      //If route when no consumers is false but there is no consumer on the local queue then messages should be round robin'd
      //It's only in the case where there is a local consumer they shouldn't be round robin'd
      
      verifyReceiveRoundRobin(10, 0, 3);
      verifyReceiveRoundRobin(10, 1, 4);
      verifyReceiveRoundRobin(10, 2, 5);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5);  
   }
   
   public void testRouteWhenNoConsumersFalseLoadBalancedQueuesLocalConsumer() throws Exception
   {
      setupClusterConnection("cluster2", 0, 1, "queues2", false, 1, isNetty());
      
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      createQueue(0, "queues2.testaddress", "queue0", null, false);
      createQueue(0, "queues2.testaddress", "queue1", null, false);
      createQueue(0, "queues2.testaddress", "queue2", null, false);
      
      createQueue(1, "queues2.testaddress", "queue0", null, false);
      createQueue(1, "queues2.testaddress", "queue1", null, false);
      createQueue(1, "queues2.testaddress", "queue2", null, false);
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      
      waitForBindings(0, "queues2.testaddress", 3, 3, true);
      waitForBindings(0, "queues2.testaddress", 3, 0, false);
      
      send(0, "queues2.testaddress", 10, false, null);
                        
      addConsumer(3, 1, "queue0", null);
      addConsumer(4, 1, "queue1", null);
      addConsumer(5, 1, "queue2", null);
      
      //In this case, since the local queue has a consumer, it should receive all the messages
      
      verifyReceiveAll(10, 0, 1, 2);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5);  
   }
   
   public void testRouteWhenNoConsumersFalseLoadBalancedQueuesNoLocalQueue() throws Exception
   {
      setupClusterConnection("cluster2", 0, 1, "queues2", false, 1, isNetty());
      
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      createQueue(0, "queues2.testaddress", "queue0", null, false);
      createQueue(0, "queues2.testaddress", "queue1", null, false);
      
      createQueue(1, "queues2.testaddress", "queue0", null, false);
      createQueue(1, "queues2.testaddress", "queue1", null, false);
      
      waitForBindings(0, "queues2.testaddress", 2, 0, true);
      waitForBindings(0, "queues2.testaddress", 2, 0, false);
      
      send(0, "queues2.testaddress", 10, false, null);
                        
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
           
      addConsumer(2, 1, "queue0", null);
      addConsumer(3, 1, "queue1", null);
      
      verifyReceiveRoundRobin(10, 0, 2);
      verifyReceiveRoundRobin(10, 1, 3);
      
      verifyNotReceive(0, 1, 2, 3);  
   }
   
   public void testRouteWhenNoConsumersTrueLoadBalancedQueues() throws Exception
   {
      setupClusterConnection("cluster2", 0, 1, "queues2", true, 1, isNetty());
      
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      createQueue(0, "queues2.testaddress", "queue0", null, false);
      createQueue(0, "queues2.testaddress", "queue1", null, false);
      createQueue(0, "queues2.testaddress", "queue2", null, false);
      
      createQueue(1, "queues2.testaddress", "queue0", null, false);
      createQueue(1, "queues2.testaddress", "queue1", null, false);
      createQueue(1, "queues2.testaddress", "queue2", null, false);
      
      waitForBindings(0, "queues2.testaddress", 3, 0, true);
      waitForBindings(0, "queues2.testaddress", 3, 0, false);
      
      send(0, "queues2.testaddress", 10, false, null);
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      
      addConsumer(3, 1, "queue0", null);
      addConsumer(4, 1, "queue1", null);
      addConsumer(5, 1, "queue2", null);
      
      verifyReceiveRoundRobin(10, 0, 3);
      verifyReceiveRoundRobin(10, 1, 4);
      verifyReceiveRoundRobin(10, 2, 5);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5);  
   }
   
   public void testRouteWhenNoConsumersTrueLoadBalancedQueuesLocalConsumer() throws Exception
   {
      setupClusterConnection("cluster2", 0, 1, "queues2", true, 1, isNetty());
      
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      createQueue(0, "queues2.testaddress", "queue0", null, false);
      createQueue(0, "queues2.testaddress", "queue1", null, false);
      createQueue(0, "queues2.testaddress", "queue2", null, false);
      
      createQueue(1, "queues2.testaddress", "queue0", null, false);
      createQueue(1, "queues2.testaddress", "queue1", null, false);
      createQueue(1, "queues2.testaddress", "queue2", null, false);
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      
      waitForBindings(0, "queues2.testaddress", 3, 3, true);
      waitForBindings(0, "queues2.testaddress", 3, 0, false);
      
      send(0, "queues2.testaddress", 10, false, null);
                        
      addConsumer(3, 1, "queue0", null);
      addConsumer(4, 1, "queue1", null);
      addConsumer(5, 1, "queue2", null);
      
      verifyReceiveRoundRobin(10, 0, 3);
      verifyReceiveRoundRobin(10, 1, 4);
      verifyReceiveRoundRobin(10, 2, 5);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5);  
   }
   
   public void testRouteWhenNoConsumersTrueLoadBalancedQueuesNoLocalQueue() throws Exception
   {
      setupClusterConnection("cluster2", 0, 1, "queues2", true, 1, isNetty());
      
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      createQueue(0, "queues2.testaddress", "queue0", null, false);
      createQueue(0, "queues2.testaddress", "queue1", null, false);
      
      createQueue(1, "queues2.testaddress", "queue0", null, false);
      createQueue(1, "queues2.testaddress", "queue1", null, false);
      
      waitForBindings(0, "queues2.testaddress", 2, 0, true);
      waitForBindings(0, "queues2.testaddress", 2, 0, false);
      
      send(0, "queues2.testaddress", 10, false, null);
                        
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
           
      addConsumer(2, 1, "queue0", null);
      addConsumer(3, 1, "queue1", null);
      
      verifyReceiveRoundRobin(10, 0, 2);
      verifyReceiveRoundRobin(10, 1, 3);
      
      verifyNotReceive(0, 1, 2, 3);  
   }
   
   public void testNonLoadBalancedQueuesWithConsumersWithFilters() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      String filter1 = "giraffe";
      String filter2 = "aardvark";

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(0, "queues.testaddress", "queue1", null, false);
      createQueue(0, "queues.testaddress", "queue2", null, false);
      createQueue(0, "queues.testaddress", "queue3", null, false);
      createQueue(0, "queues.testaddress", "queue4", null, false);
    
      
      createQueue(1, "queues.testaddress", "queue5", null, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(1, "queues.testaddress", "queue7", null, false);
      createQueue(1, "queues.testaddress", "queue8", null, false);
      createQueue(1, "queues.testaddress", "queue9", null, false);
      
      createQueue(0, "queues.testaddress", "queue10", null, false);
      createQueue(1, "queues.testaddress", "queue11", null, false);

      
      addConsumer(0, 0, "queue0", filter1);
      addConsumer(1, 0, "queue1", filter2);
      addConsumer(2, 0, "queue2", filter1);
      addConsumer(3, 0, "queue3", filter2);
      addConsumer(4, 0, "queue4", filter1);
      
      addConsumer(5, 1, "queue5", filter2);
      addConsumer(6, 1, "queue6", filter1);
      addConsumer(7, 1, "queue7", filter2);
      addConsumer(8, 1, "queue8", filter1);
      addConsumer(9, 1, "queue9", filter2);
      
      addConsumer(10, 0, "queue10", null);
      
      addConsumer(11, 1, "queue11", null);
           
      waitForBindings(0, "queues.testaddress", 6, 6, true);
      waitForBindings(0, "queues.testaddress", 6, 6, false);

      send(0, "queues.testaddress", 10, false, filter1);
                  
      verifyReceiveAll(10, 0, 2, 4, 6, 8, 10, 11);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
      
      send(0, "queues.testaddress", 10, false, filter2);
      
      verifyReceiveAll(10, 1, 3, 5, 7, 9, 10, 11);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
   }
   
   public void testRoundRobinMultipleQueuesWithConsumersWithFilters() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty());
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      
      String filter1 = "giraffe";
      String filter2 = "aardvark";

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue0", null, false);
      
      createQueue(0, "queues.testaddress", "queue1", null, false);
      createQueue(1, "queues.testaddress", "queue1", null, false);
      
      createQueue(0, "queues.testaddress", "queue2", null, false);
      createQueue(1, "queues.testaddress", "queue2", null, false);
      
      createQueue(0, "queues.testaddress", "queue3", null, false);
      createQueue(1, "queues.testaddress", "queue3", null, false);
      
      createQueue(0, "queues.testaddress", "queue4", null, false);
      createQueue(1, "queues.testaddress", "queue4", null, false);

      addConsumer(0, 0, "queue0", filter1);
      addConsumer(1, 1, "queue0", filter1);
      
      addConsumer(2, 0, "queue1", filter1);
      addConsumer(3, 1, "queue1", filter2);
      
      addConsumer(4, 0, "queue2", filter2);
      addConsumer(5, 1, "queue2", filter1);
      
      addConsumer(6, 0, "queue3", filter2);
      addConsumer(7, 1, "queue3", filter2);
      
      addConsumer(8, 0, "queue4", null);
      addConsumer(9, 1, "queue4", null);

      waitForBindings(0, "queues.testaddress", 5, 5, true);
      waitForBindings(0, "queues.testaddress", 5, 5, false);

      send(0, "queues.testaddress", 10, false, filter1);
                  
      verifyReceiveRoundRobin(10, 0, 1);
      verifyReceiveRoundRobin(10, 8, 9);
      
      verifyReceiveAll(10, 2, 5);
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            
      send(0, "queues.testaddress", 10, false, filter2);
      
      verifyReceiveRoundRobin(10, 6, 7);
      verifyReceiveRoundRobin(10, 8, 9);
      
      verifyReceiveAll(10, 3, 4);
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      
   }
   
   public void testMultipleClusterConnections() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues1", false, 1, isNetty());
      setupClusterConnection("cluster2", 0, 1, "queues2", false, 1, isNetty());
      setupClusterConnection("cluster3", 0, 1, "queues3", false, 1, isNetty());
      
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());

      //Make sure the different connections don't conflict
      
      createQueue(0, "queues1.testaddress", "queue0", null, false);
      createQueue(0, "queues1.testaddress", "queue1", null, false);
      createQueue(0, "queues2.testaddress", "queue2", null, false);
      createQueue(0, "queues2.testaddress", "queue3", null, false);
      createQueue(0, "queues3.testaddress", "queue4", null, false);
      createQueue(0, "queues3.testaddress", "queue5", null, false);
    
      
      createQueue(1, "queues1.testaddress", "queue6", null, false);
      createQueue(1, "queues1.testaddress", "queue7", null, false);
      createQueue(1, "queues2.testaddress", "queue8", null, false);
      createQueue(1, "queues2.testaddress", "queue9", null, false);
      createQueue(1, "queues3.testaddress", "queue10", null, false);
      createQueue(1, "queues3.testaddress", "queue11", null, false);

      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 0, "queue1", null);
      addConsumer(2, 0, "queue2", null);
      addConsumer(3, 0, "queue3", null);
      addConsumer(4, 0, "queue4", null);
      addConsumer(5, 0, "queue5", null);
      
     
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 1, "queue7", null);
      addConsumer(8, 1, "queue8", null);
      addConsumer(9, 1, "queue9", null);
      addConsumer(10, 1, "queue10", null);
      addConsumer(11, 1, "queue11", null);
           
      waitForBindings(0, "queues1.testaddress", 2, 2, true);
      waitForBindings(0, "queues1.testaddress", 2, 2, false);
      
      waitForBindings(0, "queues2.testaddress", 2, 2, true);
      waitForBindings(0, "queues2.testaddress", 2, 2, false);
      
      waitForBindings(0, "queues3.testaddress", 2, 2, true);
      waitForBindings(0, "queues3.testaddress", 2, 2, false);

      send(0, "queues1.testaddress", 10, false, null);
                  
      verifyReceiveAll(10, 0, 1, 6, 7);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
      
      send(0, "queues2.testaddress", 10, false, null);
      
      verifyReceiveAll(10, 2, 3, 8, 9);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
      
      send(0, "queues3.testaddress", 10, false, null);
      
      verifyReceiveAll(10, 4, 5, 10, 11);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
   }

//   
//   public void testDurableAndRestart()
//   {      
//   }
//   
//   public void testWithNetty()
//   {      
//   }
   
//   public void testNetty() throws Exception
//   {      
//      //this.stopServers(0, 1);
//      
//      super.clearServer(0, 1);
//      
//      this.setupServer(0, true, true);
//      
//      //setupClusterConnection("cluster1", 0, 1, "queues", false, true);
//      startServers(0);
//      
//      log.info("started servers");
//
//      setupSessionFactory(0, true);
//   
//
//      createQueue(0, "queues.testaddress", "queue0", null, false);
//      //createQueue(1, "queues.testaddress", "queue0", null, false);
//      
//      addConsumer(0, 0, "queue0", null);
//     
//
//      waitForBindings(0, "queues.testaddress", 1, 1, true);
//
//      send(0, "queues.testaddress", 10, false, null);
//                  
//      verifyReceiveAll(0);            
//   }
//   
//   public void testRoundRobinMultipleQueuesNetty() throws Exception
//   {
//      
//      
//      //setupClusterConnection("cluster1", 0, 1, "queues", false, true);
//      startServers(1, 0);
//      
//      log.info("started servers");
//
//      setupSessionFactory(0, true);
//      setupSessionFactory(1, true);
//
//      createQueue(0, "queues.testaddress", "queue0", null, false);
//      createQueue(1, "queues.testaddress", "queue0", null, false);
//      
//      createQueue(0, "queues.testaddress", "queue1", null, false);
//      createQueue(1, "queues.testaddress", "queue1", null, false);
//      
//      createQueue(0, "queues.testaddress", "queue2", null, false);
//      createQueue(1, "queues.testaddress", "queue2", null, false);
//
//      addConsumer(0, 0, "queue0", null);
//      addConsumer(1, 1, "queue0", null);
//      
//      addConsumer(2, 0, "queue1", null);
//      addConsumer(3, 1, "queue1", null);
//      
//      addConsumer(4, 0, "queue2", null);
//      addConsumer(5, 1, "queue2", null);
//
//      waitForBindings(0, "queues.testaddress", 3, 3, true);
//      waitForBindings(0, "queues.testaddress", 3, 3, false);
//
//      send(0, "queues.testaddress", 10, false, null);
//                  
//      verifyReceiveRoundRobin(10, 0, 1);
//      
//      verifyReceiveRoundRobin(10, 2, 3);
//      
//      verifyReceiveRoundRobin(10, 4, 5);
//      
//      verifyNotReceive(0, 1, 2, 3, 4, 5);
//   }
   
}
