package org.jboss.messaging.core.deployers;

import java.net.URL;

/**
 * Extended to allow objects to be deployed via the Deploymwntmanager
 *
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 */
public interface Deployable
{
   /**
    * The name of the configuration file name to look for for deployment
    *
    * @return The name of the config file
    */
   String getConfigFileName();

   /**
    * Deploy the URL for the first time
    *
    * @param url The resource todeploy
    * @throws Exception .
    */
   void deploy(URL url) throws Exception;

   /**
    * Redeploys a URL if changed
    *
    * @param url The resource to redeploy
    * @throws Exception .
    */
   void redeploy(URL url) throws Exception;

   /**
    * Undeploys a resource that has been removed
    * @param url The Resource that was deleted
    * @throws Exception .
    */
   void undeploy(URL url) throws Exception;
}
