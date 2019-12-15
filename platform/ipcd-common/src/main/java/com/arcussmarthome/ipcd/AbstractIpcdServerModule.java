/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arcussmarthome.ipcd;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.bridge.bus.PlatformBusListener;
import com.arcussmarthome.bridge.bus.PlatformBusService;
import com.arcussmarthome.bridge.bus.ProtocolBusListener;
import com.arcussmarthome.bridge.bus.ProtocolBusService;
import com.arcussmarthome.bridge.server.client.ClientFactory;
import com.arcussmarthome.bridge.server.config.BridgeServerConfig;
import com.arcussmarthome.bridge.server.http.RequestAuthorizer;
import com.arcussmarthome.bridge.server.http.RequestHandler;
import com.arcussmarthome.bridge.server.http.RequestMatcher;
import com.arcussmarthome.bridge.server.http.handlers.RootRedirect;
import com.arcussmarthome.bridge.server.http.impl.auth.AlwaysAllow;
import com.arcussmarthome.bridge.server.http.impl.matcher.WebSocketUpgradeMatcher;
import com.arcussmarthome.bridge.server.netty.Authenticator;
import com.arcussmarthome.bridge.server.netty.Bridge10ChannelInitializer;
import com.arcussmarthome.bridge.server.netty.Text10WebSocketServerHandlerProvider;
import com.arcussmarthome.bridge.server.noauth.NoopAuthenticator;
import com.arcussmarthome.bridge.server.session.DefaultSessionRegistryImpl;
import com.arcussmarthome.bridge.server.session.SessionListener;
import com.arcussmarthome.bridge.server.session.SessionRegistry;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTlsContext;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTlsContextImpl;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTrustManagerFactory;
import com.arcussmarthome.bridge.server.ssl.SimpleTrustManagerFactoryImpl;
import com.arcussmarthome.bridge.server.ssl.TrustConfig;
import com.arcussmarthome.ipcd.bus.IpcdPlatformBusServiceImpl;
import com.arcussmarthome.ipcd.bus.IpcdProtocolBusListener;
import com.arcussmarthome.ipcd.bus.IpcdServiceEventListener;
import com.arcussmarthome.ipcd.bus.ProtocolBusServiceImpl;
import com.arcussmarthome.ipcd.delivery.DefaultIpcdDeliveryStrategy;
import com.arcussmarthome.ipcd.delivery.IpcdDeliveryStrategy;
import com.arcussmarthome.ipcd.session.IpcdClientFactory;
import com.arcussmarthome.ipcd.session.SessionHeartBeater;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public abstract class AbstractIpcdServerModule extends AbstractIrisModule {

   @Override
   protected final void configure() {
      bind(BridgeServerConfig.class);
      bind(TrustConfig.class);
      bind(BridgeServerTlsContext.class).to(BridgeServerTlsContextImpl.class);
      bind(BridgeServerTrustManagerFactory.class).to(SimpleTrustManagerFactoryImpl.class);
      bind(ProtocolBusService.class).to(ProtocolBusServiceImpl.class).asEagerSingleton();
      bindSetOf(ProtocolBusListener.class).addBinding().to(IpcdProtocolBusListener.class);
      bind(PlatformBusService.class).to(IpcdPlatformBusServiceImpl.class).asEagerSingleton();
      Multibinder<PlatformBusListener> platformListenerBinder = bindSetOf(PlatformBusListener.class);
      platformListenerBinder.addBinding().to(IpcdServiceEventListener.class);      
      bindMessageHandler();
      bind(Authenticator.class).to(NoopAuthenticator.class);
      bind(ClientFactory.class).to(IpcdClientFactory.class);
      bind(new TypeLiteral<ChannelInitializer<SocketChannel>>(){}).to(Bridge10ChannelInitializer.class);
      bind(ChannelInboundHandler.class).toProvider(Text10WebSocketServerHandlerProvider.class);
      bindSessionFactory();
      bind(SessionRegistry.class).to(DefaultSessionRegistryImpl.class);
      bindSessionSupplier();
      bind(SessionHeartBeater.class);

      bind(DefaultIpcdDeliveryStrategy.class);
      bindDeliveryStrategies();
      bind(RequestMatcher.class).annotatedWith(Names.named("WebSocketUpgradeMatcher")).to(WebSocketUpgradeMatcher.class);
      bind(RequestAuthorizer.class).annotatedWith(Names.named("SessionAuthorizer")).to(AlwaysAllow.class);

      Multibinder<SessionListener> slBindings = Multibinder.newSetBinder(binder(), SessionListener.class);
      bindSessionListener(slBindings);

      Multibinder<RequestHandler> rhBindings = Multibinder.newSetBinder(binder(), RequestHandler.class);
      rhBindings.addBinding().to(RootRedirect.class);
      bindHttpHandlers(rhBindings);
   }

   protected abstract void bindMessageHandler();

   protected abstract void bindSessionFactory();

   protected abstract void bindSessionSupplier();

   protected void bindDeliveryStrategies() {
      // default to an empty map so the default strategy is always used
      MapBinder<String, IpcdDeliveryStrategy> strategyBinder = bindMapOf(String.class, IpcdDeliveryStrategy.class);
   }

   protected void bindSessionListener(Multibinder<SessionListener> slBindings) {
   }

   protected void bindHttpHandlers(Multibinder<RequestHandler> rhBindings) {
   }
}

