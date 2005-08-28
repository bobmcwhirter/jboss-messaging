/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.test.messaging.tools.jndi;

import org.jboss.messaging.util.NotYetImplementedException;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.Binding;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;


/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class InVMContext implements Context
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------
   
   // Attributes ----------------------------------------------------

   protected Map map;
   protected NameParser parser = new InVMNameParser();

   // Constructors --------------------------------------------------

   public InVMContext()
   {
      map = Collections.synchronizedMap(new HashMap());
   }

   // Context implementation ----------------------------------------

   public Object lookup(Name name) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public Object lookup(String name) throws NamingException
   {
      name = trimSlashes(name);
      int i = name.indexOf("/");
      String tok = i == -1 ? name : name.substring(0, i);
      Object value = map.get(tok);
      if (value == null)
      {
         throw new NameNotFoundException("Name not found: " + tok);
      }
      if (value instanceof InVMContext && i != -1)
      {
         return ((InVMContext)value).lookup(name.substring(i));
      }
      return value;
   }

   public void bind(Name name, Object obj) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public void bind(String name, Object obj) throws NamingException
   {
      internalBind(name, obj, false);
   }

   public void rebind(Name name, Object obj) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public void rebind(String name, Object obj) throws NamingException
   {
      internalBind(name, obj, true);
   }

   public void unbind(Name name) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public void unbind(String name) throws NamingException
   {
      name = trimSlashes(name);
      int i = name.indexOf("/");
      boolean terminal = i == -1;
      if (terminal)
      {
         map.remove(name);
      }
      else
      {
         String tok = name.substring(0, i);
         InVMContext c = (InVMContext)map.get(tok);
         if (c == null)
         {
            throw new NameNotFoundException("Context not found: " + tok);
         }
         c.unbind(name.substring(i));
      }
   }

   public void rename(Name oldName, Name newName) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public void rename(String oldName, String newName) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public NamingEnumeration list(Name name) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public NamingEnumeration list(String name) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public NamingEnumeration listBindings(Name name) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public NamingEnumeration listBindings(String contextName) throws NamingException
   {
      contextName = trimSlashes(contextName);
      if (!"".equals(contextName) && !".".equals(contextName))
      {
         try
         {
            return ((InVMContext)lookup(contextName)).listBindings("");
         }
         catch(Throwable t)
         {
            throw new NamingException(t.getMessage());
         }
      }

      List l = new ArrayList();
      for(Iterator i = map.keySet().iterator(); i.hasNext(); )
      {
         String name = (String)i.next();
         Object object = map.get(name);
         l.add(new Binding(name, object));
      }
      return new NamingEnumerationImpl(l.iterator());
   }

   public void destroySubcontext(Name name) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public void destroySubcontext(String name) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public Context createSubcontext(Name name) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public Context createSubcontext(String name) throws NamingException
   {
      if (map.get(name) != null)
      {
         throw new NameAlreadyBoundException(name);
      }
      InVMContext c = new InVMContext();
      map.put(name, c);
      return c;
   }

   public Object lookupLink(Name name) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public Object lookupLink(String name) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public NameParser getNameParser(Name name) throws NamingException
   {
      return getNameParser(name.toString());
   }

   public NameParser getNameParser(String name) throws NamingException
   {
      return parser;
   }

   public Name composeName(Name name, Name prefix) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public String composeName(String name, String prefix) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public Object addToEnvironment(String propName, Object propVal) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public Object removeFromEnvironment(String propName) throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public Hashtable getEnvironment() throws NamingException
   {
      throw new NotYetImplementedException();
   }

   public void close() throws NamingException
   {
   }

   public String getNameInNamespace() throws NamingException
   {
      throw new NotYetImplementedException();
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------

   private String trimSlashes(String s)
   {
      int i = 0;
      while(true)
      {
         if (i == s.length() || s.charAt(i) != '/')
         {
            break;
         }
         i++;
      }
      s = s.substring(i);
      i = s.length() - 1;
      while(true)
      {
         if (i == -1 || s.charAt(i) != '/')
         {
            break;
         }
         i--;
      }
      return s.substring(0, i + 1);
   }

   private void internalBind(String name, Object obj, boolean rebind) throws NamingException
   {
      name = trimSlashes(name);
      int i = name.lastIndexOf("/");
      InVMContext c = this;
      if (i != -1)
      {
         String path = name.substring(0, i);
         c = (InVMContext)lookup(path);
      }
      name = name.substring(i + 1);
      if (!rebind && c.map.get(name) != null)
      {
         throw new NameAlreadyBoundException(name);
      }
      c.map.put(name, obj);
   }

   // Inner classes -------------------------------------------------

   private class NamingEnumerationImpl implements NamingEnumeration
   {
      private Iterator iterator;

      NamingEnumerationImpl(Iterator bindingIterator)
      {
         this.iterator = bindingIterator;
      }

      public void close() throws NamingException
      {
         throw new NotYetImplementedException();
      }

      public boolean hasMore() throws NamingException
      {
         return iterator.hasNext();
      }

      public Object next() throws NamingException
      {
         return iterator.next();
      }

      public boolean hasMoreElements()
      {
         return iterator.hasNext();
      }

      public Object nextElement()
      {
         return iterator.next();
      }
   }
}

