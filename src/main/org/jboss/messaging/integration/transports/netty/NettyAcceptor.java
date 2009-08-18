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

package org.jboss.messaging.integration.transports.netty;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.exception.MessagingException;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.impl.ssl.SSLSupport;
import org.jboss.messaging.core.remoting.spi.Acceptor;
import org.jboss.messaging.core.remoting.spi.BufferHandler;
import org.jboss.messaging.core.remoting.spi.Connection;
import org.jboss.messaging.core.remoting.spi.ConnectionLifeCycleListener;
import org.jboss.messaging.utils.ConfigurationHelper;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.local.DefaultLocalServerChannelFactory;
import org.jboss.netty.channel.local.LocalAddress;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.ssl.SslHandler;

/**
 * A Netty TCP Acceptor that supports SSL
 *
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="tlee@redhat.com">Trustin Lee</a>
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 * @version $Rev$, $Date$
 */
public class NettyAcceptor implements Acceptor
{
   private static final Logger log = Logger.getLogger(NettyAcceptor.class);

   private ChannelFactory channelFactory;

   private volatile ChannelGroup serverChannelGroup;

   private volatile ChannelGroup channelGroup;

   private ServerBootstrap bootstrap;

   private final BufferHandler handler;

   private final ConnectionLifeCycleListener listener;

   private final boolean sslEnabled;

   private final boolean httpEnabled;

   private final long httpServerScanPeriod;

   private final long httpResponseTime;

   private final boolean useNio;

   private final boolean useInvm;

   private final String host;

   private final int port;

   private final String keyStorePath;

   private final String keyStorePassword;

   private final String trustStorePath;

   private final String trustStorePassword;

   private final boolean tcpNoDelay;

   private final int tcpSendBufferSize;

   private final int tcpReceiveBufferSize;

   private final HttpKeepAliveRunnable httpKeepAliveRunnable;

   private ConcurrentMap<Object, Connection> connections = new ConcurrentHashMap<Object, Connection>();

   private final Executor threadPool;

   public NettyAcceptor(final Map<String, Object> configuration,
                        final BufferHandler handler,
                        final ConnectionLifeCycleListener listener,
                        final Executor threadPool,
                        final ScheduledExecutorService scheduledThreadPool)
   {
      this.handler = handler;

      this.listener = listener;

      this.sslEnabled = ConfigurationHelper.getBooleanProperty(TransportConstants.SSL_ENABLED_PROP_NAME,
                                                               TransportConstants.DEFAULT_SSL_ENABLED,
                                                               configuration);
      this.httpEnabled = ConfigurationHelper.getBooleanProperty(TransportConstants.HTTP_ENABLED_PROP_NAME,
                                                                TransportConstants.DEFAULT_HTTP_ENABLED,
                                                                configuration);

      if (httpEnabled)
      {
         httpServerScanPeriod = ConfigurationHelper.getLongProperty(TransportConstants.HTTP_SERVER_SCAN_PERIOD_PROP_NAME,
                                                                    TransportConstants.DEFAULT_HTTP_SERVER_SCAN_PERIOD,
                                                                    configuration);
         httpResponseTime = ConfigurationHelper.getLongProperty(TransportConstants.HTTP_RESPONSE_TIME_PROP_NAME,
                                                                TransportConstants.DEFAULT_HTTP_RESPONSE_TIME,
                                                                configuration);
         httpKeepAliveRunnable = new HttpKeepAliveRunnable();
         Future<?> future = scheduledThreadPool.scheduleAtFixedRate(httpKeepAliveRunnable,
                                                                    httpServerScanPeriod,
                                                                    httpServerScanPeriod,
                                                                    TimeUnit.MILLISECONDS);
         httpKeepAliveRunnable.setFuture(future);
      }
      else
      {
         httpServerScanPeriod = 0;
         httpResponseTime = 0;
         httpKeepAliveRunnable = null;
      }
      this.useNio = ConfigurationHelper.getBooleanProperty(TransportConstants.USE_NIO_PROP_NAME,
                                                           TransportConstants.DEFAULT_USE_NIO,
                                                           configuration);

      this.useInvm = ConfigurationHelper.getBooleanProperty(TransportConstants.USE_INVM_PROP_NAME,
                                                            TransportConstants.DEFAULT_USE_INVM,
                                                            configuration);
      this.host = ConfigurationHelper.getStringProperty(TransportConstants.HOST_PROP_NAME,
                                                        TransportConstants.DEFAULT_HOST,
                                                        configuration);
      this.port = ConfigurationHelper.getIntProperty(TransportConstants.PORT_PROP_NAME,
                                                     TransportConstants.DEFAULT_PORT,
                                                     configuration);
      if (sslEnabled)
      {
         this.keyStorePath = ConfigurationHelper.getStringProperty(TransportConstants.KEYSTORE_PATH_PROP_NAME,
                                                                   TransportConstants.DEFAULT_KEYSTORE_PATH,
                                                                   configuration);
         this.keyStorePassword = ConfigurationHelper.getStringProperty(TransportConstants.KEYSTORE_PASSWORD_PROP_NAME,
                                                                       TransportConstants.DEFAULT_KEYSTORE_PASSWORD,
                                                                       configuration);
         this.trustStorePath = ConfigurationHelper.getStringProperty(TransportConstants.TRUSTSTORE_PATH_PROP_NAME,
                                                                     TransportConstants.DEFAULT_TRUSTSTORE_PATH,
                                                                     configuration);
         this.trustStorePassword = ConfigurationHelper.getStringProperty(TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME,
                                                                         TransportConstants.DEFAULT_TRUSTSTORE_PASSWORD,
                                                                         configuration);
      }
      else
      {
         this.keyStorePath = null;
         this.keyStorePassword = null;
         this.trustStorePath = null;
         this.trustStorePassword = null;
      }

      this.tcpNoDelay = ConfigurationHelper.getBooleanProperty(TransportConstants.TCP_NODELAY_PROPNAME,
                                                               TransportConstants.DEFAULT_TCP_NODELAY,
                                                               configuration);
      this.tcpSendBufferSize = ConfigurationHelper.getIntProperty(TransportConstants.TCP_SENDBUFFER_SIZE_PROPNAME,
                                                                  TransportConstants.DEFAULT_TCP_SENDBUFFER_SIZE,
                                                                  configuration);
      this.tcpReceiveBufferSize = ConfigurationHelper.getIntProperty(TransportConstants.TCP_RECEIVEBUFFER_SIZE_PROPNAME,
                                                                     TransportConstants.DEFAULT_TCP_RECEIVEBUFFER_SIZE,
                                                                     configuration);

      this.threadPool = threadPool;
   }

   public synchronized void start() throws Exception
   {
      if (channelFactory != null)
      {
         // Already started
         return;
      }

      VirtualExecutorService virtualExecutor = new VirtualExecutorService(threadPool);

      if (useInvm)
      {
         channelFactory = new DefaultLocalServerChannelFactory();
      }
      else if (useNio)
      {
         channelFactory = new NioServerSocketChannelFactory(virtualExecutor, virtualExecutor);
      }
      else
      {
         channelFactory = new OioServerSocketChannelFactory(virtualExecutor, virtualExecutor);
      }
      bootstrap = new ServerBootstrap(channelFactory);

      final SSLContext context;
      if (sslEnabled)
      {
         try
         {
            context = SSLSupport.createServerContext(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
         }
         catch (Exception e)
         {
            IllegalStateException ise = new IllegalStateException("Unable to create NettyAcceptor for " + host +
                                                                  ":" +
                                                                  port);
            ise.initCause(e);
            throw ise;
         }
      }
      else
      {
         context = null; // Unused
      }

      ChannelPipelineFactory factory = new ChannelPipelineFactory()
      {
         public ChannelPipeline getPipeline() throws Exception
         {
            ChannelPipeline pipeline = pipeline();
            if (sslEnabled)
            {
               ChannelPipelineSupport.addSSLFilter(pipeline, context, false);
            }
            if (httpEnabled)
            {
               pipeline.addLast("httpRequestDecoder", new HttpRequestDecoder());
               pipeline.addLast("httpResponseEncoder", new HttpResponseEncoder());
               pipeline.addLast("httphandler", new HttpAcceptorHandler(httpKeepAliveRunnable, httpResponseTime));
            }

            ChannelPipelineSupport.addCodecFilter(pipeline, handler);
            pipeline.addLast("handler", new MessagingServerChannelHandler(channelGroup, handler, new Listener()));
            return pipeline;
         }
      };
      bootstrap.setPipelineFactory(factory);

      // Bind
      bootstrap.setOption("child.tcpNoDelay", tcpNoDelay);
      if (tcpReceiveBufferSize != -1)
      {
         bootstrap.setOption("child.receiveBufferSize", tcpReceiveBufferSize);
      }
      if (tcpSendBufferSize != -1)
      {
         bootstrap.setOption("child.sendBufferSize", tcpSendBufferSize);
      }
      bootstrap.setOption("reuseAddress", true);
      bootstrap.setOption("child.reuseAddress", true);
      bootstrap.setOption("child.keepAlive", true);

      channelGroup = new DefaultChannelGroup("jbm-accepted-channels");

      serverChannelGroup = new DefaultChannelGroup("jbm-acceptor-channels");

      startServerChannels();

      paused = false;
   }

   private void startServerChannels()
   {
      String[] hosts = TransportConfiguration.splitHosts(host);
      for (String h : hosts)
      {
         SocketAddress address;
         if (useInvm)
         {
            address = new LocalAddress(h);
         }
         else
         {
            address = new InetSocketAddress(h, port);
         }
         Channel serverChannel = bootstrap.bind(address);
         serverChannelGroup.add(serverChannel);
      }
   }

   public synchronized void stop()
   {
      if (channelFactory == null)
      {
         return;
      }

      serverChannelGroup.close().awaitUninterruptibly();

      if (httpKeepAliveRunnable != null)
      {
         httpKeepAliveRunnable.close();
      }

      ChannelGroupFuture future = channelGroup.close().awaitUninterruptibly();

      if (!future.isCompleteSuccess())
      {
         log.warn("channel group did not completely close");
         Iterator<Channel> iterator = future.getGroup().iterator();
         while (iterator.hasNext())
         {
            Channel channel = (Channel)iterator.next();
            if (channel.isBound())
            {
               log.warn(channel + " is still connected to " + channel.getRemoteAddress());
            }
         }
      }

      channelFactory.releaseExternalResources();
      channelFactory = null;

      for (Connection connection : connections.values())
      {
         listener.connectionDestroyed(connection.getID());
      }

      connections.clear();
      
      paused = false;
   }

   public boolean isStarted()
   {
      return (channelFactory != null);
   }

   private boolean paused;

   public synchronized void pause()
   {
      if (paused)
      {
         return;
      }

      if (channelFactory == null)
      {
         return;
      }

      // We *pause* the acceptor so no new connections are made

      serverChannelGroup.close().awaitUninterruptibly();

      try
      {
         Thread.sleep(500);
      }
      catch (Exception e)
      {
      }

      paused = true;
   }

   public synchronized void resume()
   {
      if (!paused)
      {
         return;
      }

      startServerChannels();

      paused = false;
   }

   // Inner classes -----------------------------------------------------------------------------

   @ChannelPipelineCoverage("one")
   private final class MessagingServerChannelHandler extends MessagingChannelHandler
   {
      MessagingServerChannelHandler(ChannelGroup group, BufferHandler handler, ConnectionLifeCycleListener listener)
      {
         super(group, handler, listener);
      }

      @Override
      public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
      {
         new NettyConnection(e.getChannel(), new Listener());

         SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);
         if (sslHandler != null)
         {
            sslHandler.handshake(e.getChannel()).addListener(new ChannelFutureListener()
            {
               public void operationComplete(ChannelFuture future) throws Exception
               {
                  if (future.isSuccess())
                  {
                     active = true;
                  }
                  else
                  {
                     future.getChannel().close();
                  }
               }
            });
         }
         else
         {
            active = true;
         }
      }
   }

   private class Listener implements ConnectionLifeCycleListener
   {
      public void connectionCreated(final Connection connection)
      {
         if (connections.putIfAbsent(connection.getID(), connection) != null)
         {
            throw new IllegalArgumentException("Connection already exists with id " + connection.getID());
         }

         listener.connectionCreated(connection);
      }

      public void connectionDestroyed(final Object connectionID)
      {
         if (connections.remove(connectionID) != null)
         {
            // // Execute on different thread to avoid deadlocks
            // new Thread()
            // {
            // public void run()
            // {
            listener.connectionDestroyed(connectionID);
            // }
            // }.start();
         }
      }

      public void connectionException(final Object connectionID, final MessagingException me)
      {
         // Execute on different thread to avoid deadlocks
         new Thread()
         {
            public void run()
            {
               listener.connectionException(connectionID, me);
            }
         }.start();

      }
   }
}
