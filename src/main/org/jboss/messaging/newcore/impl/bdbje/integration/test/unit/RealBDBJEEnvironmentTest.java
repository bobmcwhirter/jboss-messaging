package org.jboss.messaging.newcore.impl.bdbje.integration.test.unit;

import java.io.File;

import org.jboss.messaging.newcore.impl.bdbje.BDBJEEnvironment;
import org.jboss.messaging.newcore.impl.bdbje.integration.RealBDBJEEnvironment;
import org.jboss.messaging.newcore.impl.bdbje.test.unit.BDBJEEnvironmentTestBase;

/**
 * 
 * A RealBDBJEEnvironmentTest
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class RealBDBJEEnvironmentTest extends BDBJEEnvironmentTestBase
{
   protected void setUp() throws Exception
   {   
      System.out.println("Creating env dir");
      
      createDir(ENV_DIR);
      
      env = createEnvironment();
      
      env.setEnvironmentPath(ENV_DIR);
      
      env.start();
      
      database = env.getDatabase("test-db");            
   }
   
   protected BDBJEEnvironment createEnvironment() throws Exception
   {
      BDBJEEnvironment env = new RealBDBJEEnvironment(true);
      
      env.setTransacted(true);
      
      return env;
   }    
   
   protected void createDir(String path)
   {  
      File file = new File(path);
      
      boolean deleted = deleteDirectory(file);
      
      System.out.println("Deleted: " + deleted);
      
      file.mkdir();
   }
}
