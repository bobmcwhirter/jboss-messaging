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
package org.jboss.jms.server.jndi;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import java.io.Serializable;
import java.util.Properties;

/**
 * used by the default context when running with thelocal configuration
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 */
public class InVMNameParser implements NameParser, Serializable
{
   // Constants -----------------------------------------------------

   private static final long serialVersionUID = 2925203703371001031L;

   // Static --------------------------------------------------------

   static Properties syntax;

   static
   {
      syntax = new Properties();
      syntax.put("jndi.syntax.direction", "left_to_right");
      syntax.put("jndi.syntax.ignorecase", "false");
      syntax.put("jndi.syntax.separator", "/");
   }

   // Attributes ----------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public static Properties getSyntax()
   {
      return syntax;
   }

   public Name parse(String name) throws NamingException
   {
      return new CompoundName(name, syntax);
   }


   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
