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
package com.arcussmarthome.video.previewupload.server;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.BridgeConfigModule;
import com.arcussmarthome.bridge.server.client.ClientFactory;
import com.arcussmarthome.bridge.server.config.BridgeServerConfig;
import com.arcussmarthome.bridge.server.http.RequestAuthorizer;
import com.arcussmarthome.bridge.server.http.RequestHandler;
import com.arcussmarthome.bridge.server.http.RequestMatcher;
import com.arcussmarthome.bridge.server.http.handlers.CheckPage;
import com.arcussmarthome.bridge.server.http.handlers.IndexPage;
import com.arcussmarthome.bridge.server.http.handlers.RootRedirect;
import com.arcussmarthome.bridge.server.http.impl.auth.AlwaysAllow;
import com.arcussmarthome.bridge.server.http.impl.matcher.NeverMatcher;
import com.arcussmarthome.bridge.server.netty.BaseWebSocketServerHandlerProvider;
import com.arcussmarthome.bridge.server.session.DefaultSessionRegistryImpl;
import com.arcussmarthome.bridge.server.session.SessionFactory;
import com.arcussmarthome.bridge.server.session.SessionListener;
import com.arcussmarthome.bridge.server.session.SessionRegistry;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTlsContext;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTlsContextImpl;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTrustManagerFactory;
import com.arcussmarthome.bridge.server.ssl.TrustConfig;
import com.arcussmarthome.core.dao.cassandra.CassandraDeviceDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.arcussmarthome.core.dao.file.HubBlacklistDAOModule;
import com.arcussmarthome.core.metricsreporter.builder.MetricsTopicReporterBuilderModule;
import com.arcussmarthome.hubcom.server.session.HubSessionFactory;
import com.arcussmarthome.hubcom.server.session.listener.HubSessionListener;
import com.arcussmarthome.hubcom.server.session.listener.NoopHubSessionListener;
import com.arcussmarthome.hubcom.server.ssl.HubTrustManagerFactoryImpl;
import com.arcussmarthome.video.PreviewConfig;
import com.arcussmarthome.video.PreviewModule;
import com.arcussmarthome.video.previewupload.server.handlers.UploadHandler;
import com.arcussmarthome.video.previewupload.server.netty.HttpRequestInitializer;
import com.arcussmarthome.video.previewupload.server.session.VideoPreviewUploadClientFactory;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class VideoPreviewUploadServerModule extends AbstractIrisModule {

   @Inject
   public VideoPreviewUploadServerModule(
         BridgeConfigModule bridge,
         PreviewModule previews,
         CassandraDeviceDAOModule devDao,
         CassandraResourceBundleDAOModule resourceDao,
         HubBlacklistDAOModule blacklistDao,
         MetricsTopicReporterBuilderModule metrics
   ) {
   }

   @Override
   protected void configure() {
      bind(BridgeServerConfig.class);
      bind(TrustConfig.class);
      bind(PreviewConfig.class);
      bind(HubSessionListener.class).to(NoopHubSessionListener.class);
      bind(SessionFactory.class).to(HubSessionFactory.class);
      bind(SessionRegistry.class).to(DefaultSessionRegistryImpl.class);
      bind(ClientFactory.class).to(VideoPreviewUploadClientFactory.class);

      bind(BridgeServerTlsContext.class).to(BridgeServerTlsContextImpl.class);
      bind(BridgeServerTrustManagerFactory.class).to(HubTrustManagerFactoryImpl.class);
      bind(new TypeLiteral<ChannelInitializer<SocketChannel>>(){}).to(HttpRequestInitializer.class);

      bind(ChannelInboundHandler.class).toProvider(BaseWebSocketServerHandlerProvider.class);
      bind(RequestMatcher.class).annotatedWith(Names.named("WebSocketUpgradeMatcher")).to(NeverMatcher.class);
      bind(RequestAuthorizer.class).annotatedWith(Names.named("SessionAuthorizer")).to(AlwaysAllow.class);

      bind(VideoPreviewUploadServerConfig.class);

      // No Session Listeners
      Multibinder<SessionListener> slBindings = Multibinder.newSetBinder(binder(), SessionListener.class);

      // Bind Http Handlers
      Multibinder<RequestHandler> rhBindings = Multibinder.newSetBinder(binder(), RequestHandler.class);
      rhBindings.addBinding().to(RootRedirect.class);
      rhBindings.addBinding().to(CheckPage.class);
      rhBindings.addBinding().to(IndexPage.class);
      rhBindings.addBinding().to(UploadHandler.class);
   }

   @Provides @Singleton
   public BridgeMetrics provideBridgeMetrics() {
      return new BridgeMetrics("video-preview-ul");
   }
}

