/*
 * JBoss, the OpenSource J2EE webOS
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.messaging.jms.perf.framework;

import java.io.FileInputStream;
import java.util.Date;
import java.util.Properties;

import javax.jms.DeliveryMode;
import javax.jms.Session;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.test.messaging.jms.perf.framework.data.Benchmark;
import org.jboss.test.messaging.jms.perf.framework.data.Execution;
import org.jboss.test.messaging.jms.perf.framework.data.Measurement;
import org.jboss.test.messaging.jms.perf.framework.factories.BytesMessageMessageFactory;
import org.jboss.test.messaging.jms.perf.framework.factories.ForeignMessageMessageFactory;
import org.jboss.test.messaging.jms.perf.framework.factories.MapMessageMessageFactory;
import org.jboss.test.messaging.jms.perf.framework.factories.MessageFactory;
import org.jboss.test.messaging.jms.perf.framework.factories.MessageMessageFactory;
import org.jboss.test.messaging.jms.perf.framework.factories.ObjectMessageMessageFactory;
import org.jboss.test.messaging.jms.perf.framework.factories.StreamMessageMessageFactory;
import org.jboss.test.messaging.jms.perf.framework.factories.TextMessageMessageFactory;
import org.jboss.test.messaging.jms.perf.framework.persistence.JDBCPersistenceManager;
import org.jboss.test.messaging.jms.perf.framework.persistence.PersistenceManager;

/**
 * 
 * Runs all the tests in the performance test suite
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 */
public class PerfRunner
{
   private static final Logger log = Logger.getLogger(PerfRunner.class);   
   
   protected Properties jndiProperties;
   
   protected String connectionFactoryJndiName;
   
   protected String queueName;
   
   protected String topicName;
   
   protected String subscriptionName;
   
   protected String queueNamePrefix;
   
   protected String topicNamePrefix;
   
   protected String providerName;
   
   protected PersistenceManager pm;
   
   protected int numMessages;
   
   protected int numWarmupMessages;
   
   protected int scaleNumber;
   
   protected String[] slaveURLs;
   
   protected int standardMessageSize;
   
   protected int initialPause;
   
   protected String clientID;
   
   protected int numSlaves;
   
   public PerfRunner()
   {      
   }
   
   public static void main(String[] args)
   {
      new PerfRunner().run();
   }
   
   public void run()
   {
      setUp(); //Only need to do once - junit does for every test
      
      testQueue1();
      testQueue2();
      testQueue3();
      testQueue4();
      testQueue5();
      testQueue6();
      testQueue7();
      testQueue8();
      testQueue9();
      testQueue10();
      testQueue11();
      testQueue12();
      testQueue13();
      testQueue14();
      testQueue15();
      testQueue16();
      testQueue17();
      testQueue18();
      testQueue19();
      testQueue20();
      
      testTopic1();
      testTopic2();
      testTopic3();
      testTopic4();
      testTopic5();
      testTopic6();
      testTopic7();
      testTopic8();
      testTopic9();
      testTopic10();
      
      testMessageSizeThroughput();
      
      testQueueScale1();
      testQueueScale2();
      testQueueScale3();
      
      testTopicScale1();
      testTopicScale2();
      testTopicScale3();
      
      tearDown();
   }
   
   protected Properties loadProps(String filename) throws Exception
   {
      FileInputStream fis = null;
      try
      {
         fis = new FileInputStream(filename);         
         Properties props = new Properties();
         props.load(fis);
         return props;
      }
      finally
      {
         if (fis != null) fis.close();
      }
   }
   
   public void setUp()
   {
      try
      {
      
         log.trace("Setting up PerfRunner");
         
         String jndiPropertiesFileName = System.getProperty("perf.jndiProperties", "jndi.properties");
         
         this.jndiProperties = loadProps(jndiPropertiesFileName);
         
         String perfPropertiesFileName = System.getProperty("perf.properties", "perf.properties");
         
         Properties perfProperties = loadProps(perfPropertiesFileName);
         
         connectionFactoryJndiName = perfProperties.getProperty("perf.connectionFactoryJndiName", "/ConnectionFactory");
         queueName = perfProperties.getProperty("perf.queueName", "queue/perfTestQueue");
         topicName = perfProperties.getProperty("perf.topicName", "topic/perfTestTopic");
         subscriptionName = perfProperties.getProperty("perf.subscriptionName", "perfTestSub");      
         queueNamePrefix = perfProperties.getProperty("perf.queueNamePrefix", "queue/perfTestQueue");
         topicNamePrefix = perfProperties.getProperty("perf.topicNamePrefix", "topic/perfTestTopic");
         numMessages = new Integer(perfProperties.getProperty("perf.numMessages", "200")).intValue();
         numWarmupMessages = new Integer(perfProperties.getProperty("perf.numWarmupMessages", "100")).intValue();
         providerName = perfProperties.getProperty("perf.providerName", "JBossMessaging");
         String dbURL = perfProperties.getProperty("perf.dbURL", "jdbc:hsqldb:hsql://localhost:7776");
         scaleNumber = new Integer(perfProperties.getProperty("perf.scaleNumber", "8")).intValue();
         standardMessageSize = new Integer(perfProperties.getProperty("perf.standardMessageSize", "1024")).intValue();
         log.info(perfProperties.getProperty("perf.initialPause"));
         initialPause = new Integer(perfProperties.getProperty("perf.initialPause", "5000")).intValue();
         clientID = perfProperties.getProperty("perf.clientID", "perfTestClientID");
         numSlaves = new Integer(perfProperties.getProperty("perf.numSlaves", "2")).intValue();
   
         slaveURLs = new String[numSlaves];
         for (int i = 0; i < numSlaves; i++)
         {
            slaveURLs[i] = perfProperties.getProperty("perf.slaveURL" + (i + 1), "socket://localhost:1234/?socketTimeout=0");
         }
         
         //log.info("jndiServerURL:" + this.jndiServerURL);
         log.info("jndiProperties file:" + jndiPropertiesFileName);
         log.info("queueName:" + this.queueName);
         log.info("topicName:" + this.topicName);
         log.info("subscriptionName:" + this.subscriptionName);
         log.info("queueNamePrefix:" + this.queueNamePrefix);
         log.info("topicNamePrefix:" + this.topicNamePrefix);
         log.info("numMessages:" + this.numMessages);
         log.info("numWarmupMessages:" + this.numWarmupMessages);
         log.info("providerName:" + this.providerName);
         log.info("dbURL:" + dbURL);
         log.info("scaleNumber:" + this.scaleNumber);
         log.info("standardMessageSize:" + this.standardMessageSize);  
         log.info("InitialPause:" + this.initialPause);
         log.info("clientID:" + this.clientID);
         log.info("numSlaves:" + this.numSlaves);
         for (int i = 0; i < numSlaves; i++)
         {
            log.info("slaveURL" + (i+1) + ":" + slaveURLs[i]);
         }        
                  
         pm = new JDBCPersistenceManager(dbURL);
         pm.start();
      }
      catch (Exception e)
      {
         log.error("Failed to setup", e);
      }
   }
   
   public void tearDown()
   {
      log.trace("Tearing down PerfRunner");
      pm.stop();
   }
   
   protected Execution createExecution(String benchmarkName)
   {
      //Benchmark bm = pm.getBenchmark(benchmarkName);
      Execution exec = new Execution(new Benchmark(benchmarkName), new Date(), providerName);
      return exec;
   }
   
   /*
    * Send numMessages non-persistent messages of standardMessageSize bytes each to single queue non transactionally.
    * The queue has no consumers.
    * Measure time taken
    */
   public void testQueue1()
   {
      log.info("Running test Queue1");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      SenderJob sender = createDefaultSenderJob(queueName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      
      long result = runJob(sender).getTestTime();
      
      Execution execution = createExecution("Queue1");
      
      execution.addMeasurement(new Measurement("msgsSentPerSec", 1000 * (double)numMessages / result));
      pm.saveExecution(execution);
      
      log.info("Test Queue1 finished");
   }
   
   /*
    * Send numMessages persistent messages of standardMessageSize bytes each to single queue non transactionally.
    * The queue has no consumers.
    * Measure time taken
    */
   public void testQueue2()
   {
      log.info("Running test Queue2");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      SenderJob sender = createDefaultSenderJob(queueName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.PERSISTENT);
      
      long result = runJob(sender).getTestTime();
      
      Execution execution = createExecution("Queue2");
      
      execution.addMeasurement(new Measurement("msgsSentPerSec", 1000 * (double)numMessages / result));
      pm.saveExecution(execution);
      
      log.info("Test Queue2 finished");
   }
   
   
   /*
    * Send numMessages non-persistent messages of standardMessageSize bytes each to single queue transactionally.
    * Transaction size is 100
    * The queue has no consumers.
    * Measure time taken
    */
   public void testQueue3()
   {
      log.info("Running test Queue3");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, true, DeliveryMode.NON_PERSISTENT, Session.SESSION_TRANSACTED);
      
      SenderJob sender = createDefaultSenderJob(queueName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(true);
      sender.setTransactionSize(100);
      sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      
      long result = runJob(sender).getTestTime();
      
      Execution execution = createExecution("Queue3");
      
      execution.addMeasurement(new Measurement("msgsSentPerSec", 1000 * (double)numMessages / result));
      pm.saveExecution(execution);
      
      log.info("Test Queue3 finished");
   }
   
   
   /*
    * Send numMessages persistent messages of standardMessageSize bytes each to single queue transactionally.
    * Transaction size is 100
    * The queue has no consumers.
    * Measure time taken
    */
   public void testQueue4()
   {
      log.info("Running test Queue4");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, true, DeliveryMode.PERSISTENT, Session.SESSION_TRANSACTED);
      
      SenderJob sender = createDefaultSenderJob(queueName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(true);
      sender.setTransactionSize(100);
      sender.setDeliveryMode(DeliveryMode.PERSISTENT);
      
      long result = runJob(sender).getTestTime();
      
      Execution execution = createExecution("Queue4");
      
      execution.addMeasurement(new Measurement("msgsSentPerSec", 1000 * (double)numMessages / result));
      pm.saveExecution(execution);
      
      log.info("Test Queue4 finished");
   }
   
   /*
    * Send numMessages non-persistent messages of standardMessageSize bytes each to single queue non-transactionally.
    * Concurrently receive them non-transactionally with ackmode=AUTO_ACKNOWLEDGE.
    * Measure total time taken from first send to last receive
    */
   public void testQueue5()
   {
      log.info("Running test Queue5");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      SenderJob sender = createDefaultSenderJob(queueName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {sender, receiver});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentiall different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[1].getTestTime() + results[1].getInitTime() - results[0].getInitTime();
      
      
      Execution execution = createExecution("Queue5");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Queue5 finished");
   }
   
   
   /*
    * Send numMessages persistent messages of standardMessageSize bytes each to single queue non-transactionally.
    * Concurrently receive them non-transactionally with ackmode=AUTO_ACKNOWLEDGE.
    * Measure total time taken from first send to last receive
    */
   public void testQueue6()
   {
      log.info("Running test Queue6");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      SenderJob sender = createDefaultSenderJob(queueName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.PERSISTENT);
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {sender, receiver});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[1].getTestTime() + results[1].getInitTime() - results[0].getInitTime();
      
      
      Execution execution = createExecution("Queue6");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Queue6 finished");
   }
   
   /*
    * Send numMessages non-persistent messages of standardMessageSize bytes each to single queue transactionally, transaction size =100
    * Concurrently receive them transactionally with transaction size = 100
    * Measure total time taken from first send to last receive
    */
   public void testQueue7()
   {
      log.info("Running test Queue7");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, true, DeliveryMode.NON_PERSISTENT, Session.SESSION_TRANSACTED);
      
      SenderJob sender = createDefaultSenderJob(queueName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(true);
      sender.setTransactionSize(100);
      sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(true);
      receiver.setTransactionSize(100);
      receiver.setNumMessages(numMessages);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {sender, receiver});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[1].getTestTime() + results[1].getInitTime() - results[0].getInitTime();
      
      
      Execution execution = createExecution("Queue7");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Queue7 finished");
   }
   
   /*
    * Send numMessages persistent messages of standardMessageSize bytes each to single queue transactionally, transaction size =100
    * Concurrently receive them transactionally with transaction size = 100
    * Measure total time taken from first send to last receive
    */
   public void testQueue8()
   {
      log.info("Running test Queue8");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, true, DeliveryMode.PERSISTENT, Session.SESSION_TRANSACTED);
      
      SenderJob sender = createDefaultSenderJob(queueName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(true);
      sender.setTransactionSize(100);
      sender.setDeliveryMode(DeliveryMode.PERSISTENT);
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(true);
      receiver.setTransactionSize(100);
      receiver.setNumMessages(numMessages);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {sender, receiver});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[1].getTestTime() + results[1].getInitTime() - results[0].getInitTime();
      
      
      Execution execution = createExecution("Queue8");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Queue8 finished");
   }
   
   /*
    * Send numMessages non-persistent messages of standardMessageSize bytes each to single queue non-transactionally
    * Concurrently receive them non-transactionally with ack mode of DUPS_OK_ACKNOWLEDGE
    * Measure total time taken from first send to last receive
    */
   public void testQueue9()
   {
      log.info("Running test Queue9");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.DUPS_OK_ACKNOWLEDGE);
      
      SenderJob sender = createDefaultSenderJob(queueName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      receiver.setAckMode(Session.DUPS_OK_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setNumMessages(numMessages);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {sender, receiver});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[1].getTestTime() + results[1].getInitTime() - results[0].getInitTime();
      
      
      Execution execution = createExecution("Queue9");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Queue9 finished");
   }
   
   /*
    * Send numMessages persistent messages of standardMessageSize bytes each to single queue non-transactionally
    * Concurrently receive them non-transactionally with ack mode of DUPS_OK_ACKNOWLEDGE
    * Measure total time taken from first send to last receive
    */
   public void testQueue10()
   {
      log.info("Running test Queue10");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.PERSISTENT, Session.DUPS_OK_ACKNOWLEDGE);
      
      SenderJob sender = createDefaultSenderJob(queueName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.PERSISTENT);
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      receiver.setAckMode(Session.DUPS_OK_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setNumMessages(numMessages);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {sender, receiver});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[1].getTestTime() + results[1].getInitTime() - results[0].getInitTime();
      
      
      Execution execution = createExecution("Queue10");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Queue10 finished");
   }
   
   
   /*
    * Receive only numMessages pre-existing persistent messages of size standardMessageSize bytes from queue.
    * Receive non-transactionally, using ack mode of AUTO_ACKNOWLEDGE
    */
   public void testQueue11()
   {
      log.info("Running test Queue11");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      //Now fill it with numMessages messages
      FillJob fillJob = createDefaultFillJob(queueName, numMessages);
      fillJob.setDeliveryMode(DeliveryMode.PERSISTENT);
      fillJob.setMsgSize(standardMessageSize);
      fillJob.setMf(new BytesMessageMessageFactory());
      runJob(fillJob);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setNumMessages(numMessages);
      
      JobTimings result = runJob(receiver);
      
      Execution execution = createExecution("Queue11");
      
      execution.addMeasurement(new Measurement("msgsReceivedPerSec", (double)(numMessages * 1000) / result.getTestTime()));
      pm.saveExecution(execution);
      
      log.info("Test Queue11 finished");
   }
   
   /*
    * Receive only numMessages pre-existing non-persistent messages of size standardMessageSize bytes from queue.
    * Receive non-transactionally, using ack mode of AUTO_ACKNOWLEDGE
    */
   public void testQueue12()
   {
      log.info("Running test Queue12");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      //Now fill it with numMessages messages
      FillJob fillJob = createDefaultFillJob(queueName, numMessages);
      fillJob.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      fillJob.setMsgSize(standardMessageSize);
      fillJob.setMf(new BytesMessageMessageFactory());
      runJob(fillJob);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setNumMessages(numMessages);
      
      JobTimings result = runJob(receiver);
      
      Execution execution = createExecution("Queue12");
      
      execution.addMeasurement(new Measurement("msgsReceivedPerSec", (double)(numMessages * 1000) / result.getTestTime()));
      pm.saveExecution(execution);
      
      log.info("Test Queue12 finished");
   }
   
   /*
    * Receive only numMessages pre-existing persistent messages of size standardMessageSize bytes from queue.
    * Receive transactionally with transaction size of 100
    */
   public void testQueue13()
   {
      log.info("Running test Queue13");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, true, DeliveryMode.PERSISTENT, Session.SESSION_TRANSACTED);
      
      //Now fill it with numMessages messages
      FillJob fillJob = createDefaultFillJob(queueName, numMessages);
      fillJob.setDeliveryMode(DeliveryMode.PERSISTENT);
      fillJob.setMsgSize(standardMessageSize);
      fillJob.setMf(new BytesMessageMessageFactory());
      runJob(fillJob);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      receiver.setTransacted(true);
      receiver.setTransactionSize(100);
      receiver.setNumMessages(numMessages);
      
      JobTimings result = runJob(receiver);
      
      Execution execution = createExecution("Queue13");
      
      execution.addMeasurement(new Measurement("msgsReceivedPerSec", (double)(numMessages * 1000) / result.getTestTime()));
      pm.saveExecution(execution);
      
      log.info("Test Queue13 finished");
   }
   
   /*
    * Receive only numMessages pre-existing non-persistent messages of size standardMessageSize bytes from queue.
    * Receive transactionally with transaction size of 100
    */
   public void testQueue14()
   {
      log.info("Running test Queue14");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, true, DeliveryMode.NON_PERSISTENT, Session.SESSION_TRANSACTED);
      
      //Now fill it with numMessages messages
      FillJob fillJob = createDefaultFillJob(queueName, numMessages);
      fillJob.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      fillJob.setMsgSize(standardMessageSize);
      fillJob.setMf(new BytesMessageMessageFactory());
      runJob(fillJob);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      receiver.setTransacted(true);
      receiver.setTransactionSize(100);
      receiver.setNumMessages(numMessages);
      
      JobTimings result = runJob(receiver);
      
      Execution execution = createExecution("Queue14");
      
      execution.addMeasurement(new Measurement("msgsReceivedPerSec", (double)(numMessages * 1000) / result.getTestTime()));
      pm.saveExecution(execution);
      
      log.info("Test Queue14 finished");
   }
   
   
   /*
    * Receive only numMessages pre-existing persistent messages of size standardMessageSize bytes from queue.
    * Receive non-transactionally with ack mode of AUTO_ACKNOWLEDGE and selector
    */
   public void testQueue15()
   {
      log.info("Running test Queue15");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      //Now fill it with numMessages messages
      FillJob fillJob = createDefaultFillJob(queueName, numMessages);
      fillJob.setDeliveryMode(DeliveryMode.PERSISTENT);
      fillJob.setMsgSize(standardMessageSize);
      fillJob.setMf(new BytesMessageMessageFactory());
      runJob(fillJob);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      //This should always be true
      String selector = "JMSMessageID IS NOT NULL AND NonExistentProperty IS NULL";

      receiver.setTransacted(false);
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setSelector(selector);
      receiver.setNumMessages(numMessages);
      
      JobTimings result = runJob(receiver);
      
      Execution execution = createExecution("Queue15");
      
      execution.addMeasurement(new Measurement("msgsReceivedPerSec", (double)(numMessages * 1000) / result.getTestTime()));
      pm.saveExecution(execution);
      
      log.info("Test Queue15 finished");
   }
   
   /*
    * Receive only numMessages pre-existing non-persistent messages of size standardMessageSize bytes from queue.
    * Receive non-transactionally with ack mode of AUTO_ACKNOWLEDGE and selector
    */
   public void testQueue16()
   {
      log.info("Running test Queue16");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      //Now fill it with numMessages messages
      FillJob fillJob = createDefaultFillJob(queueName, numMessages);
      fillJob.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      fillJob.setMsgSize(standardMessageSize);
      fillJob.setMf(new BytesMessageMessageFactory());
      runJob(fillJob);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      //This should always be true
      String selector = "JMSMessageID IS NOT NULL AND NonExistentProperty IS NULL";

      
      receiver.setTransacted(false);
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setSelector(selector);
      receiver.setNumMessages(numMessages);
      
      JobTimings result = runJob(receiver);
      
      Execution execution = createExecution("Queue16");
      
      execution.addMeasurement(new Measurement("msgsReceivedPerSec", (double)(numMessages * 1000) / result.getTestTime()));
      pm.saveExecution(execution);
      
      log.info("Test Queue16 finished");
   }
   
   /*
    * Browse numMessages persistent messages of size standardMessageSize bytes in queue.
    */
   public void testQueue17()
   {
      log.info("Running test Queue17");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      //Now fill it with numMessages messages
      FillJob fillJob = createDefaultFillJob(queueName, numMessages);
      fillJob.setDeliveryMode(DeliveryMode.PERSISTENT);
      fillJob.setMsgSize(standardMessageSize);
      fillJob.setMf(new BytesMessageMessageFactory());
      runJob(fillJob);
      
      BrowserJob job  = createDefaultBrowserJob(queueName);
      JobTimings result = runJob(job);
      
      Execution execution = createExecution("Queue17");
      
      execution.addMeasurement(new Measurement("messagesPerSec", 1000 * (double)numMessages / result.getTestTime()));
      pm.saveExecution(execution);
      
      log.info("Test Queue17 finished");
   }
   
   /*
    * Browse numMessages non-persistent messages of size standardMessageSize bytes in queue.
    */
   public void testQueue18()
   {
      log.info("Running test Queue18");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      //Now fill it with numMessages messages
      FillJob fillJob = createDefaultFillJob(queueName, numMessages);
      fillJob.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      fillJob.setMsgSize(standardMessageSize);
      fillJob.setMf(new BytesMessageMessageFactory());
      runJob(fillJob);
      
      BrowserJob job  = createDefaultBrowserJob(queueName);
      JobTimings result = runJob(job);
      
      Execution execution = createExecution("Queue18");
      
      execution.addMeasurement(new Measurement("messagesPerSec", 1000 * (double)numMessages / result.getTestTime()));
      
      pm.saveExecution(execution);
      
      log.info("Test Queue18 finished");
   }
   
   /*
    * Browse numMessages non-persistent messages of size standardMessageSize bytes in queue with selector
    */
   public void testQueue19()
   {
      log.info("Running test Queue19");
      
      //First drain the destination
      runJob(createDefaultDrainJob(queueName));
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      //Now fill it with numMessages messages
      FillJob fillJob = createDefaultFillJob(queueName, numMessages);
      fillJob.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      fillJob.setMsgSize(standardMessageSize);
      fillJob.setMf(new BytesMessageMessageFactory());
      runJob(fillJob);
      
      //This should always be true
      String selector = "JMSMessageID IS NOT NULL AND NonExistentProperty IS NULL";

      BrowserJob job  = createDefaultBrowserJob(queueName);
      job.setSelector(selector);
      JobTimings result = runJob(job);
      
      Execution execution = createExecution("Queue19");
      
      execution.addMeasurement(new Measurement("timeTaken", result.getTestTime()));
      pm.saveExecution(execution);
      
      log.info("Test Queue19 finished");
   }
   
   public void testQueue20()
   {      
      //TODO No-local perf test
   }
   
   /*
    * Send numMessages non-persistent messages of standardMessageSize bytes each to single topic non-transactionally
    * Receive them non-transactionally with single non durable subscriber and ack mode of AUTO_ACKNOWLEDGE
    * Measure total time taken from first send to last receive
    */
   public void testTopic1()
   {
      log.info("Running test Topic1");
      
      warmupDestination(topicName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      SenderJob sender = createDefaultSenderJob(topicName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      sender.setInitialPause(initialPause); //enough time for receiver to get ready
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(topicName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setNumMessages(numMessages);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {receiver, sender});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[0].getTestTime() + results[0].getInitTime() - results[1].getInitTime();
      
      
      Execution execution = createExecution("Topic1");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Topic1 finished");
   }
   
   /*
    * Send numMessages persistent messages of standardMessageSize bytes each to single topic non-transactionally
    * Receive them non-transactionally with single non durable subscriber and with ack mode of AUTO_ACKNOWLEDGE
    * Measure total time taken from first send to last receive
    */
   public void testTopic2()
   {
      log.info("Running test Topic2");
      
      warmupDestination(topicName, standardMessageSize, false, DeliveryMode.PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      
      SenderJob sender = createDefaultSenderJob(topicName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.PERSISTENT);
      sender.setInitialPause(initialPause); //enough time for receiver to get ready
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(topicName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setNumMessages(numMessages);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {receiver, sender});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[0].getTestTime() + results[0].getInitTime() - results[1].getInitTime();
      
      Execution execution = createExecution("Topic2");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Topic2 finished");
   }
   
   
   /*
    * Send numMessages non-persistent messages of standardMessageSize bytes each to single topic transactionally, tx size = 100
    * Receive them transactionally with single non durable subscriber and with tx size of 100
    * Measure total time taken from first send to last receive
    */
   public void testTopic3()
   {
      log.info("Running test Topic3");
      
      warmupDestination(topicName, standardMessageSize, true, DeliveryMode.NON_PERSISTENT, Session.SESSION_TRANSACTED);      
      
      SenderJob sender = createDefaultSenderJob(topicName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(true);
      sender.setTransactionSize(100);
      sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      sender.setInitialPause(initialPause); //enough time for receiver to get ready
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(topicName);
      
      receiver.setTransacted(true);
      receiver.setTransactionSize(100);
      receiver.setNumMessages(numMessages);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {receiver, sender});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[0].getTestTime() + results[0].getInitTime() - results[1].getInitTime();
      
      Execution execution = createExecution("Topic3");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Topic3 finished");
   }
   
   /*
    * Send numMessages persistent messages of standardMessageSize bytes each to single topic transactionally, tx size = 100
    * Receive them transactionally with single non durable subscriber and with tx size of 100
    * Measure total time taken from first send to last receive
    */
   public void testTopic4()
   {
      log.info("Running test Topic4");
      
      warmupDestination(topicName, standardMessageSize, false, DeliveryMode.PERSISTENT, Session.SESSION_TRANSACTED);
      
      
      SenderJob sender = createDefaultSenderJob(topicName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(true);
      sender.setTransactionSize(100);
      sender.setDeliveryMode(DeliveryMode.PERSISTENT);
      sender.setInitialPause(initialPause); //enough time for receiver to get ready
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(topicName);
      
      receiver.setTransacted(true);
      receiver.setTransactionSize(100);
      receiver.setNumMessages(numMessages);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {receiver, sender});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[0].getTestTime() + results[0].getInitTime() - results[1].getInitTime();
      
      Execution execution = createExecution("Topic4");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Topic4 finished");
   }
   
   /*
    * Send numMessages non-persistent messages of standardMessageSize bytes each to single topic non-transactionally
    * Receive them non-transactionally with single non durable subscriber and ack mode of DUPS_OK_ACKNOWLEDGE
    * Measure total time taken from first send to last receive
    */
   public void testTopic5()
   {
      log.info("Running test Topic5");
      
      warmupDestination(topicName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.DUPS_OK_ACKNOWLEDGE);
      
      
      SenderJob sender = createDefaultSenderJob(topicName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      sender.setInitialPause(initialPause); //enough time for receiver to get ready
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(topicName);
      
      receiver.setAckMode(Session.DUPS_OK_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setNumMessages(numMessages);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {receiver, sender});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[0].getTestTime() + results[0].getInitTime() - results[1].getInitTime();
      
      Execution execution = createExecution("Topic5");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Topic5 finished");
   }
   
   /*
    * Send numMessages persistent messages of standardMessageSize bytes each to single topic non-transactionally
    * Receive them non-transactionally with single non durable subscriber and with ack mode of DUPS_OK_ACKNOWLEDGE
    * Measure total time taken from first send to last receive
    */
   public void testTopic6()
   {
      log.info("Running test Topic6");
      
      warmupDestination(topicName, standardMessageSize, false, DeliveryMode.PERSISTENT, Session.DUPS_OK_ACKNOWLEDGE);      
      
      SenderJob sender = createDefaultSenderJob(topicName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.PERSISTENT);
      sender.setInitialPause(initialPause); //enough time for receiver to get ready
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(topicName);
      
      receiver.setAckMode(Session.DUPS_OK_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setNumMessages(numMessages);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {receiver, sender});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[0].getTestTime() + results[0].getInitTime() - results[1].getInitTime();
      
      Execution execution = createExecution("Topic6");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Topic6 finished");
   }
   
   
   /*
    * Send numMessages non-persistent messages of standardMessageSize bytes each to single topic non-transactionally
    * Receive them non-transactionally with single non durable subscriber and ack mode of AUTO_ACKNOWLEDGE
    * and using a selector
    * Measure total time taken from first send to last receive
    */
   public void testTopic7()
   {
      log.info("Running test Topic7");
      
      warmupDestination(topicName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      
      SenderJob sender = createDefaultSenderJob(topicName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      sender.setInitialPause(initialPause); //enough time for receiver to get ready
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(topicName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setSlaveURL(slaveURLs[1]);
      
      //This should always be true
      String selector = "JMSMessageID IS NOT NULL AND NonExistentProperty IS NULL";

      receiver.setSelector(selector);
      receiver.setNumMessages(numMessages);
      
      JobTimings[] results = runJobs(new Job[] {receiver, sender});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[0].getTestTime() + results[0].getInitTime() - results[1].getInitTime();
      
      Execution execution = createExecution("Topic7");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Topic7 finished");
   }
   
   /*
    * Send numMessages persistent messages of standardMessageSize bytes each to single topic non-transactionally
    * Receive them non-transactionally with single non durable subscriber and with ack mode of AUTO_ACKNOWLEDGE
    * and using selector
    * Measure total time taken from first send to last receive
    */
   public void testTopic8()
   {
      log.info("Running test Topic8");
      
      warmupDestination(topicName, standardMessageSize, false, DeliveryMode.PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      
      SenderJob sender = createDefaultSenderJob(topicName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.PERSISTENT);
      sender.setInitialPause(initialPause); //enough time for receiver to get ready
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(topicName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setNumMessages(numMessages);
      receiver.setSlaveURL(slaveURLs[1]);
      
      //TODO - Choose better selector
      
      //This should always be true
      String selector = "JMSMessageID IS NOT NULL AND NonExistentProperty IS NULL";
      
      receiver.setSelector(selector);
      
      JobTimings[] results = runJobs(new Job[] {receiver, sender});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[0].getTestTime() + results[0].getInitTime() - results[1].getInitTime();
      
      Execution execution = createExecution("Topic8");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Topic8 finished");
   }
   
   /*
    * Send numMessages persistent messages of standardMessageSize bytes each to single topic non-transactionally
    * Receive them non-transactionally with single durable subscriber and with ack mode of AUTO_ACKNOWLEDGE
    * Measure total time taken from first send to last receive
    */
   public void testTopic9()
   {
      log.info("Running test Topic9");
      
      warmupDestination(topicName, standardMessageSize, false, DeliveryMode.PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      
      //First drain the durable subscription
      DrainJob drainJob = createDefaultDrainJob(topicName);
      drainJob.setSubName(subscriptionName);
      drainJob.setClientID(clientID);
      runJob(drainJob);
      
      SenderJob sender = createDefaultSenderJob(topicName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.PERSISTENT);
      sender.setInitialPause(initialPause); //enough time for receiver to get ready
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(topicName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setSubName("testSubscription");
      receiver.setNumMessages(numMessages);
      receiver.setClientID(clientID);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {receiver, sender});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[0].getTestTime() + results[0].getInitTime() - results[1].getInitTime();
      
      Execution execution = createExecution("Topic9");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Topic9 finished");
   }
   
   /*
    * Send numMessages persistent messages of standardMessageSize bytes each to single topic transactionally, tx size of 100
    * Receive them transactionally with single durable subscriber, tx size of 100
    * Measure total time taken from first send to last receive
    */
   public void testTopic10()
   {
      log.info("Running test Topic10");
      
      warmupDestination(topicName, standardMessageSize, true, DeliveryMode.PERSISTENT, Session.SESSION_TRANSACTED);
      
      
      //First drain the durable subscription
      DrainJob drainJob = createDefaultDrainJob(topicName);
      drainJob.setSubName(subscriptionName);
      drainJob.setClientID(clientID);
      runJob(drainJob);
      
      SenderJob sender = createDefaultSenderJob(topicName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(true);
      sender.setTransactionSize(100);
      sender.setInitialPause(initialPause); //enough time for receiver to get ready
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(topicName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(true);
      receiver.setTransactionSize(100);
      receiver.setSubName("testSubscription");
      receiver.setNumMessages(numMessages);
      receiver.setClientID(clientID);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {receiver, sender});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentially different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[0].getTestTime() + results[0].getInitTime() - results[1].getInitTime();
      
      Execution execution = createExecution("Topic10");
      
      execution.addMeasurement(new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken));
      pm.saveExecution(execution);
      
      log.info("Test Topic10 finished");
   }
   
   
   /* Send numMessages persistent messages non-transactionally to topic with one non durable subscriber.
    * Concurrently receive them non-transactionally with ack mode of AUTO_ACKNOWLEDGE
    * Measure total time taken from first send to last receive.
    * Vary the message size and repeat with the following values:
    * 0 bytes
    * standardMessageSize bytes
    * 16384 bytes
    * 65536 bytes
    * 262144 bytes
    * 1048576 bytes
    * 8388608 bytes
    * 
    */
   public void testMessageSizeThroughput()
   {
      log.info("Running test testMessageSizeThroughput");
      
      warmupDestination(topicName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
            
      Execution execution = createExecution("MessageSizeThroughput");
      
      //int[] msgsSize = new int[] {0, standardMessageSize, 16384, 65536, 262144, 1048576, 8388608};
      
      int[] msgsSize = new int[] {0, 8192, 16384, 32768, 65536};
      
      for (int i = 0; i < msgsSize.length; i++)
      {
        
         int msgSize = msgsSize[i];
         
         log.trace("Doing message size " + msgSize);
         
      
         SenderJob sender = createDefaultSenderJob(topicName);
         sender.setNumMessages(numMessages);
         sender.setMsgSize(msgSize);
         sender.setMf(new BytesMessageMessageFactory());
         sender.setTransacted(false);
         sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
         sender.setInitialPause(initialPause); //enough time for receiver to get ready
         sender.setSlaveURL(slaveURLs[0]);
         
         ReceiverJob receiver = createDefaultReceiverJob(topicName);
         
         receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
         receiver.setTransacted(false);
         receiver.setNumMessages(numMessages);
         receiver.setSlaveURL(slaveURLs[1]);
         
         JobTimings[] results = runJobs(new Job[] {sender, receiver});
         
         /*
          * NB.
          * When we calculating the total time taken from first send to last receive, there is some
          * error involved due to the differences in network latency and other remoting overhead
          * when sending the jobs to be executed on potentially different remote machines.
          * We make the assumption that this this difference is very small compared with time taken
          * to run the tests as to be negligible.
          */
         
         long totalTimeTaken = results[1].getTestTime() + results[1].getInitTime() - results[0].getInitTime();
         
         Measurement measure = new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken);
         measure.setVariableValue("messageSize", msgSize);
         execution.addMeasurement(measure);
         
         
      }
      
      pm.saveExecution(execution);
      
      
      log.info("Test testMessageSizeThroughput finished");
   }
   
   /* Send numMessages non-persistent messages of size standardMessageSize bytes, non transactionally to queue
    * Concurrent receive messages from queue non-transactionally with ack mode AUTO_ACKNOWLEDGE.
    * Measure the throughput as the number of distinct queues is increased
    */
   public void testQueueScale1()
   {
      log.info("Running test testQueueScale1");
      
      Execution execution = createExecution("QueueScale1");
      
      for (int i = 1; i <= scaleNumber; i++)
      {
         log.trace("Running with " + i + " queue(s)");
         
         warmupDestination(queueNamePrefix + (i-1), standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
         
         //First drain the destinations
         log.trace("Draining destinations");
         for (int j = 0; j < i; j++)
         {
            runJob(createDefaultDrainJob(queueNamePrefix + j));
            
         }
         log.trace("Destinations drained");
         
         //Create the jobs
         
         Job[] jobs = new Job[2 * i];
         
         for (int j = 0; j < i; j++)
         {
         
            SenderJob sender = createDefaultSenderJob(queueNamePrefix + j);
            jobs[j * 2] = sender;
            sender.setNumMessages(numMessages);
            sender.setMsgSize(standardMessageSize);
            sender.setMf(new BytesMessageMessageFactory());
            sender.setTransacted(false);
            sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            sender.setSlaveURL(slaveURLs[j % this.numSlaves]);
         
            ReceiverJob receiver = createDefaultReceiverJob(queueNamePrefix + j);
            jobs[j * 2 + 1] = receiver;
            
            receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
            receiver.setTransacted(false);
            receiver.setNumMessages(numMessages);
            receiver.setSlaveURL(slaveURLs[j % this.numSlaves]);
         }
         
         JobTimings[] results = runJobs(jobs);
         
         /*
          * NB.
          * When we calculating the total time taken from first send to last receive, there is some
          * error involved due to the differences in network latency and other remoting overhead
          * when sending the jobs to be executed on potentially different remote machines.
          * We make the assumption that this this difference is very small compared with time taken
          * to run the tests as to be negligible.
          */
         
         long minTimeOfFirstSend = Long.MAX_VALUE;
         long maxTimeOfLastReceive = Long.MIN_VALUE;
         for (int j = 0; j < i; j++)
         {
            JobTimings senderResult = results[j * 2];
            JobTimings receiverResult = results[j * 2 + 1];
            long timeOfFirstSend = senderResult.getInitTime();
            minTimeOfFirstSend = Math.min(minTimeOfFirstSend, timeOfFirstSend);
            long timeOfLastReceive = receiverResult.getInitTime() + receiverResult.getTestTime();
            maxTimeOfLastReceive = Math.max(maxTimeOfLastReceive, timeOfLastReceive);
         }
         
         long totalTimeTaken = maxTimeOfLastReceive - minTimeOfFirstSend;
         long totalMessagesSent = i * numMessages;
         
         Measurement measure = new Measurement("throughput", 1000 * ((double)totalMessagesSent) / totalTimeTaken);
         measure.setVariableValue("numberOfQueues", i);
         execution.addMeasurement(measure);
      }
      
      pm.saveExecution(execution);
      
      log.info("Test testQueueScale1 finished");
      
   }
   
   
   /* Send numMessages non-persistent messages of size standardMessageSize bytes, non transactionally to queue
    * Concurrent receive messages from queue non-transactionally with ack mode AUTO_ACKNOWLEDGE.
    * Measure the throughput as the number of distinct connections is increased.
    * Each conncetion is made from separate job
    */
   public void testQueueScale2()
   {
      log.info("Running test testQueueScale2");
      
      warmupDestination(this.queueName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
            
      Execution execution = createExecution("QueueScale2");
      
      for (int i = 1; i <= scaleNumber; i++)
      {
         log.trace("Running with " + i + " connection(s)");
         
         
         //First drain the destinations
         log.trace("Draining destination");
         runJob(createDefaultDrainJob(this.queueName));
         log.trace("Destination drained");
         
         //Create the jobs
         
         Job[] jobs = new Job[i + 1];
         
         for (int j = 0; j < i; j++)
         {
         
            SenderJob sender = createDefaultSenderJob(this.queueName);
            jobs[j] = sender;
            sender.setNumMessages(numMessages);
            sender.setMf(new BytesMessageMessageFactory());
            sender.setTransacted(false);
            sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            sender.setSlaveURL(slaveURLs[j % this.numSlaves]);                     
         }
         
         //and one receiver
         ReceiverJob receiver = createDefaultReceiverJob(this.queueName);
         jobs[i] = receiver;
         
         receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
         receiver.setTransacted(false);
         receiver.setNumMessages(numMessages * i);
         receiver.setSlaveURL(slaveURLs[i % this.numSlaves]);
         
         JobTimings[] results = runJobs(jobs);
         
         /*
          * NB.
          * When we calculating the total time taken from first send to last receive, there is some
          * error involved due to the differences in network latency and other remoting overhead
          * when sending the jobs to be executed on potentially different remote machines.
          * We make the assumption that this this difference is very small compared with time taken
          * to run the tests as to be negligible.
          */
         
         long minTimeOfFirstSend = Long.MAX_VALUE;
         long maxTimeOfLastReceive = results[i].getTestTime() + results[i].getInitTime();
         for (int j = 0; j < i; j++)
         {
            JobTimings senderResult = results[j];
            long timeOfFirstSend = senderResult.getInitTime();
            minTimeOfFirstSend = Math.min(minTimeOfFirstSend, timeOfFirstSend);
         }
         
         long totalTimeTaken = maxTimeOfLastReceive - minTimeOfFirstSend;
         long totalMessagesSent = i * numMessages;
         
         Measurement measure = new Measurement("throughput", 1000 * ((double)totalMessagesSent) / totalTimeTaken);
         measure.setVariableValue("numberOfConnections", i);
         execution.addMeasurement(measure);
      }
      
      pm.saveExecution(execution);
      
      log.info("Test testQueueScale2 finished");
      
   }
   
   
   
   /* Send numMessages non-persistent messages of size standardMessageSize bytes, non transactionally to queue
    * Concurrent receive messages from queue non-transactionally with ack mode AUTO_ACKNOWLEDGE.
    * Measure the throughput as the number of sending sessions is increased.
    * Each sending session shares the same connection
    */
   public void testQueueScale3()
   {
      log.info("Running test testQueueScale3");
      
      Execution execution = createExecution("QueueScale3");
            
      for (int i = 1; i <= scaleNumber; i++)
      {
         log.trace("Running with " + i + " session(s)");
         
         //First drain the destination
         runJob(createDefaultDrainJob(queueName));
         warmupDestination(queueName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
         
         
         SenderJob sender = createDefaultSenderJob(queueName);
         sender.setNumMessages(numMessages);
         sender.setMsgSize(standardMessageSize);
         sender.setMf(new BytesMessageMessageFactory());
         sender.setTransacted(false);
         sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
         sender.setNumConnections(1);
         sender.setNumSessions(i);         
      
         ReceiverJob receiver = createDefaultReceiverJob(queueName);

         receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
         receiver.setTransacted(false);
         receiver.setNumMessages(numMessages * i);
         
         
         JobTimings[] results = runJobs(new Job[] {sender, receiver});
         
         /*
          * NB.
          * When we calculating the total time taken from first send to last receive, there is some
          * error involved due to the differences in network latency and other remoting overhead
          * when sending the jobs to be executed on potentially different remote machines.
          * We make the assumption that this this difference is very small compared with time taken
          * to run the tests as to be negligible.
          */
         
         long totalTimeTaken = results[1].getTestTime() + results[1].getInitTime() - results[0].getInitTime();
         
         int totalMessages = i * numMessages;
         
         Measurement measure = new Measurement("throughput", 1000 * (double)totalMessages / totalTimeTaken);
         measure.setVariableValue("numberOfSessions", i);
         execution.addMeasurement(measure);
      }
      
      pm.saveExecution(execution);
      
      log.info("Test testQueueScale3 finished");
      
   }
   
   /* Send numMessages non-persistent messages of size standardMessageSize bytes, non transactionally to topic
    * Concurrent receive messages from topic non-transactionally with ack mode AUTO_ACKNOWLEDGE.
    * Measure the throughput as the number of distinct topics is increased
    */
   public void testTopicScale1()
   {
      log.info("Running test testTopicScale1");
              
      Execution execution = createExecution("TopicScale1");
            
      for (int i = 1; i <= scaleNumber; i++)
      {                 
         log.trace("Running with " + i + " topic(s)");
         
         warmupDestination(topicNamePrefix + i, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
         
         
         //Create the jobs
         
         Job[] jobs = new Job[2 * i];
         
         for (int j = 0; j < i; j++)
         {
            ReceiverJob receiver = createDefaultReceiverJob(topicNamePrefix + j);
            jobs[j * 2] = receiver;
            
            receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
            receiver.setTransacted(false);
            receiver.setNumMessages(numMessages); 
            receiver.setSlaveURL(slaveURLs[j % this.numSlaves]);
            
            
            SenderJob sender = createDefaultSenderJob(topicNamePrefix + j);
            jobs[j * 2 + 1] = sender;
            sender.setNumMessages(numMessages);
            sender.setMsgSize(standardMessageSize);
            sender.setMf(new BytesMessageMessageFactory());
            sender.setTransacted(false);
            sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            sender.setInitialPause(initialPause); //enough time for receiver to get ready
            sender.setSlaveURL(slaveURLs[j % this.numSlaves]);
         
            
         }
         
         JobTimings[] results = runJobs(jobs);
         
         /*
          * NB.
          * When we calculating the total time taken from first send to last receive, there is some
          * error involved due to the differences in network latency and other remoting overhead
          * when sending the jobs to be executed on potentially different remote machines.
          * We make the assumption that this this difference is very small compared with time taken
          * to run the tests as to be negligible.
          */
         
         long minTimeOfFirstSend = Long.MAX_VALUE;
         long maxTimeOfLastReceive = Long.MIN_VALUE;
         for (int j = 0; j < i; j++)
         {
            JobTimings senderResult = results[j * 2 + 1];
            JobTimings receiverResult = results[j * 2];
            long timeOfFirstSend = senderResult.getInitTime();
            minTimeOfFirstSend = Math.min(minTimeOfFirstSend, timeOfFirstSend);
            long timeOfLastReceive = receiverResult.getInitTime() + receiverResult.getTestTime();
            maxTimeOfLastReceive = Math.max(maxTimeOfLastReceive, timeOfLastReceive);
         }
         
         long totalTimeTaken = maxTimeOfLastReceive - minTimeOfFirstSend;
         long totalMessagesSent = i * numMessages;
         
         Measurement measure = new Measurement("throughput", 1000 * ((double)totalMessagesSent) / totalTimeTaken);
         measure.setVariableValue("numberOfTopics", i);
         execution.addMeasurement(measure);
      }
      
      pm.saveExecution(execution);
      
      
      log.info("Test testTopicScale1 finished");
      
   }
   
   /* Send numMessages non-persistent messages of size standardMessageSize bytes, non transactionally to topic
    * Concurrent receive messages from topic non-transactionally with ack mode AUTO_ACKNOWLEDGE.
    * Measure the throughput as the number of receiving topic subscribers is increased.
    * Each topic subscriber uses it's own session and connection and is in a separate job
    */
   public void testTopicScale2()
   {
      log.info("Running test testTopicScale2");
      
      warmupDestination(this.topicName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
            
      Execution execution = createExecution("TopicScale2");
      
      for (int i = 1; i <= scaleNumber; i++)
      {
         log.trace("Running with " + i + " connection(s)");
         
         //Create the jobs
         
         Job[] jobs = new Job[i + 1];
         
         //One sender - many receivers
         
         for (int j = 0; j < i; j++)
         {         
            ReceiverJob receiver = createDefaultReceiverJob(this.topicName);
            jobs[j] = receiver;
            
            receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
            receiver.setTransacted(false);
            receiver.setNumMessages(numMessages);
            receiver.setSlaveURL(slaveURLs[j % this.numSlaves]);
         }
         
         SenderJob sender = createDefaultSenderJob(this.topicName);
         jobs[i] = sender;
         sender.setNumMessages(numMessages);
         sender.setMsgSize(standardMessageSize);
         sender.setMf(new BytesMessageMessageFactory());
         sender.setTransacted(false);
         sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
         sender.setSlaveURL(slaveURLs[i % this.numSlaves]);
         sender.setInitialPause(initialPause);
         
         JobTimings[] results = runJobs(jobs);
         
         /*
          * NB.
          * When we calculating the total time taken from first send to last receive, there is some
          * error involved due to the differences in network latency and other remoting overhead
          * when sending the jobs to be executed on potentially different remote machines.
          * We make the assumption that this this difference is very small compared with time taken
          * to run the tests as to be negligible.
          */
         
         long minTimeOfFirstSend = results[i].getInitTime();
         long maxTimeOfLastReceive = Long.MIN_VALUE;
         for (int j = 0; j < i; j++)
         {
            //JobTimings senderResult = results[j * 2];
            JobTimings receiverResult = results[j];
            //long timeOfFirstSend = senderResult.getInitTime();
            //minTimeOfFirstSend = Math.min(minTimeOfFirstSend, timeOfFirstSend);
            long timeOfLastReceive = receiverResult.getInitTime() + receiverResult.getTestTime();
            maxTimeOfLastReceive = Math.max(maxTimeOfLastReceive, timeOfLastReceive);
         }
         
         long totalTimeTaken = maxTimeOfLastReceive - minTimeOfFirstSend;
         
         Measurement measure = new Measurement("throughput", 1000 * (double)numMessages / totalTimeTaken);
         measure.setVariableValue("numberOfConnections", i);
         execution.addMeasurement(measure);
      }
      
      pm.saveExecution(execution);
      
      log.info("Test testTopicScale2 finished");
      
   }
   
   
   
   /* Send numMessages non-persistent messages of size standardMessageSize bytes, non transactionally to topic
    * Concurrent receive messages from topic non-transactionally with ack mode AUTO_ACKNOWLEDGE.
    * Measure the throughput as the number of receiving topic subscribers is increased.
    * Each topic subscriber uses it's own session but shares a connection
    */
   public void testTopicScale3()
   {
      log.info("Running test testTopicScale3");
      
      warmupDestination(topicName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      Execution execution = createExecution("TopicScale3");
            
      for (int i = 1; i <= scaleNumber; i++)
      {
         log.trace("Running with " + i + " consumer(s)");
         
         SenderJob sender = createDefaultSenderJob(topicName);
         sender.setNumMessages(numMessages);
         sender.setMsgSize(standardMessageSize);
         sender.setMf(new BytesMessageMessageFactory());
         sender.setTransacted(false);
         sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);             
         sender.setInitialPause(initialPause); //enough time for receiver to get ready
         sender.setNumMessages(numMessages);
      
         ReceiverJob receiver = createDefaultReceiverJob(topicName);

         receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
         receiver.setTransacted(false);
         receiver.setNumConnections(1);
         receiver.setNumSessions(i);
         receiver.setNumMessages(numMessages);
         
         
         
         JobTimings[] results = runJobs(new Job[] {receiver, sender});
         
         /*
          * NB.
          * When we calculating the total time taken from first send to last receive, there is some
          * error involved due to the differences in network latency and other remoting overhead
          * when sending the jobs to be executed on potentially different remote machines.
          * We make the assumption that this this difference is very small compared with time taken
          * to run the tests as to be negligible.
          */
         
         long totalTimeTaken = results[0].getTestTime() + results[0].getInitTime() - results[1].getInitTime();
         
         int totalMessages = numMessages;
         
         Measurement measure = new Measurement("throughput", 1000 * (double)totalMessages / totalTimeTaken);
         measure.setVariableValue("numberOfSubscribers", i);
         execution.addMeasurement(measure);
      }
      
      pm.saveExecution(execution);
      
      
      log.info("Test testTopicScale3 finished");
      
   }
   
   //TODO Browser scalability 
   
   
   public void testMessageTypes()
   {
      log.info("Running test MessageTypes");
      
      // First drain the destination     
      warmupDestination(queueName, standardMessageSize, false, DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
      
      Execution execution = createExecution("MessageTypes");
      
      runMessageTypeJob(execution, new MessageMessageFactory(), "javax.jms.Message", 1);
      runMessageTypeJob(execution, new BytesMessageMessageFactory(), "javax.jms.BytesMessage", 2);
      runMessageTypeJob(execution, new MapMessageMessageFactory(), "javax.jms.MapMessage", 3);
      runMessageTypeJob(execution, new ObjectMessageMessageFactory(), "javax.jms.ObjectMessage", 4);
      runMessageTypeJob(execution, new StreamMessageMessageFactory(), "javax.jms.StreamMessage", 5);
      runMessageTypeJob(execution, new TextMessageMessageFactory(), "javax.jms.TextMessage", 6);
      runMessageTypeJob(execution, new ForeignMessageMessageFactory(), "ForeignMessage", 7);
      
      pm.saveExecution(execution);
      
      log.info("Test MessageTypes finished");
   }
   
   protected void runMessageTypeJob(Execution execution, MessageFactory mf, String messageType, int type)
   {
      runJob(createDefaultDrainJob(queueName));

      SenderJob sender = createDefaultSenderJob(queueName);
      sender.setNumMessages(numMessages);
      sender.setMsgSize(4096);
      sender.setMf(mf);
      sender.setTransacted(false);
      sender.setDeliveryMode(DeliveryMode.PERSISTENT);
      sender.setSlaveURL(slaveURLs[0]);
      
      ReceiverJob receiver = createDefaultReceiverJob(queueName);
      
      receiver.setAckMode(Session.AUTO_ACKNOWLEDGE);
      receiver.setTransacted(false);
      receiver.setSlaveURL(slaveURLs[1]);
      
      JobTimings[] results = runJobs(new Job[] {sender, receiver});
      
      /*
       * NB.
       * When we calculating the total time taken from first send to last receive, there is some
       * error involved due to the differences in network latency and other remoting overhead
       * when sending the jobs to be executed on potentiall different remote machines.
       * We make the assumption that this this difference is very small compared with time taken
       * to run the tests as to be negligible.
       */
      
      long totalTimeTaken = results[1].getTestTime() + results[1].getInitTime() - results[0].getInitTime();
      
      Measurement measure = new Measurement("msgsSentPerSec", 1000 * (double)numMessages / totalTimeTaken);
      
      measure.setVariableValue("messageType", (double)type);
      execution.addMeasurement(measure);
   }


   protected JobTimings runJob(Job job)
   {
      JobTimings timings = sendRequestToSlave(job.getSlaveURL(), new RunRequest(job));
      return timings;
   }
   
   /*
    * Run the jobs concurrently
    */
   protected JobTimings[] runJobs(Job[] jobs)
   {      
      JobRunner[] runners = new JobRunner[jobs.length];
      for (int i = 0; i < jobs.length; i++)
      {
         runners[i] = new JobRunner(jobs[i]);
         Thread t = new Thread(runners[i]);
         runners[i].thread = t;
         t.start();
      }
      
      for (int i = 0; i < jobs.length; i++)
      {
         try
         {
            runners[i].thread.join();
         }
         catch (InterruptedException e)
         {}
      } 
      JobTimings[] results = new JobTimings[jobs.length];
      for (int i = 0; i < jobs.length; i++)
      {
         results[i] = runners[i].result;
      }
      return results;
      
   }
   
   class JobRunner implements Runnable
   {
      Job job;
      
      JobTimings result;
      
      Thread thread;

      JobRunner(Job job)
      {
         this.job = job;
      }
      
      public void run()
      {
         result = runJob(job);
      }
   }
   
   protected void warmupDestination(String destName, int msgSize, boolean transacted, int deliveryMode, int ackMode)
   {
      log.trace("Warming up destination");

      SenderJob sender = createDefaultSenderJob(destName);
      sender.setNumMessages(numWarmupMessages);
      sender.setMsgSize(standardMessageSize);
      sender.setMf(new BytesMessageMessageFactory());
      sender.setTransacted(transacted);
      if (transacted)
      {       
         sender.setTransactionSize(50);
      }
      sender.setDeliveryMode(deliveryMode);
      sender.setInitialPause(initialPause);
      
      ReceiverJob receiver = createDefaultReceiverJob(destName);
      
      receiver.setTransacted(transacted);
      if (!transacted)
      {
         receiver.setAckMode(ackMode);
      }
      else
      {
         receiver.setTransactionSize(numWarmupMessages);
      }
      receiver.setNumMessages(numWarmupMessages);
      
      runJobs(new Job[] {sender, receiver});
      
      log.trace("Warmed-up destination");
   }
   
   protected SenderJob createDefaultSenderJob(String destName)
   {
      return new SenderJob(slaveURLs[0], jndiProperties, destName, connectionFactoryJndiName,  1,
            1, false, 0, numMessages,
            false, standardMessageSize,
            new BytesMessageMessageFactory(), DeliveryMode.NON_PERSISTENT, 0);           
   }
   
   protected ReceiverJob createDefaultReceiverJob(String destName)
   {
      return new ReceiverJob(slaveURLs[0], jndiProperties, destName, connectionFactoryJndiName, 1,
            1, false, 0, numMessages,
            Session.AUTO_ACKNOWLEDGE, null, null, false, false, null);
   }
   
   protected BrowserJob createDefaultBrowserJob(String destName)
   {
      return new BrowserJob(slaveURLs[0], jndiProperties, destName, connectionFactoryJndiName, 1,
            1, numMessages, null);
   }
   
   protected DrainJob createDefaultDrainJob(String destName)
   {
      return new DrainJob(slaveURLs[0], jndiProperties, destName, connectionFactoryJndiName, null, null);
   }
   
   protected FillJob createDefaultFillJob(String destName, int numMessages)
   {
      return new FillJob(slaveURLs[0], jndiProperties, destName, connectionFactoryJndiName, numMessages, standardMessageSize, new BytesMessageMessageFactory(),
            DeliveryMode.NON_PERSISTENT);      
   }
   
   
   protected JobTimings sendRequestToSlave(String slaveURL, ServerRequest request)
   {
      try
      {
         InvokerLocator locator = new InvokerLocator(slaveURL);
         Client client = new Client(locator, "perftest");
         Object res = client.invoke(request);
         
         if (res == null)
         {
            log.error("Job Failed!!!!!!!!!!!!!!!");
         }
         
         return (JobTimings)res;
      }
      catch (Throwable t)
      {
         log.error("Failed to run job", t);
         return null;
      }
   }
}
