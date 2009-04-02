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
package org.jboss.messaging.core.deployers.impl;

import org.jboss.messaging.core.deployers.DeploymentManager;
import org.jboss.messaging.core.security.JBMUpdateableSecurityManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * deployer for adding security loaded from the file "jbm-security.xml"
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 */
public class BasicSecurityDeployer extends XmlDeployer
{
   private JBMUpdateableSecurityManager jbmSecurityManager;
   private static final String PASSWORD_ATTRIBUTE = "password";
   private static final String ROLES_NODE = "role";
   private static final String ROLE_ATTR_NAME = "name";

   private static  final String DEFAULT_USER = "defaultuser";

   private static  final String USER = "user";

   public BasicSecurityDeployer(final DeploymentManager deploymentManager)
   {
      super(deploymentManager);
   }

   public String[] getElementTagName()
   {
      return new String[]{DEFAULT_USER, USER};
   }

   @Override
   public void validate(Node rootNode) throws Exception
   {
      org.jboss.messaging.utils.XMLUtil.validate(rootNode, "jbm-security.xsd");
   }

   public void deploy(final Node node) throws Exception
   {
      String username = node.getAttributes().getNamedItem(getKeyAttribute()).getNodeValue();
      String password = node.getAttributes().getNamedItem(PASSWORD_ATTRIBUTE).getNodeValue();
      //add the user
      jbmSecurityManager.addUser(username, password);
      String nodeName = node.getNodeName();
      if (DEFAULT_USER.equalsIgnoreCase(nodeName))
      {
         jbmSecurityManager.setDefaultUser(username);
      }
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++)
      {
         Node child = children.item(i);
         //and add any roles
         if (ROLES_NODE.equalsIgnoreCase(child.getNodeName()))
         {
            String role = child.getAttributes().getNamedItem(ROLE_ATTR_NAME).getNodeValue();
            jbmSecurityManager.addRole(username, role);
         }
      }
   }

   public void undeploy(final Node node) throws Exception
   {
      String username = node.getAttributes().getNamedItem(getKeyAttribute()).getNodeValue();
      jbmSecurityManager.removeUser(username);
   }

   public String[] getDefaultConfigFileNames()
   {
      return new String[] {"jbm-security.xml"};
   }

   public void setJbmSecurityManager(final JBMUpdateableSecurityManager jbmSecurityManager)
   {
      this.jbmSecurityManager = jbmSecurityManager;
   }
}
