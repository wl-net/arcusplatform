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
package com.arcussmarthome.video.recording.server;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.BridgeEventLoopModule;
import com.arcussmarthome.bridge.server.config.BridgeServerConfig;
import com.arcussmarthome.bridge.server.netty.BridgeServerEventLoopProvider;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTlsContext;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTlsContextImpl;
import com.arcussmarthome.bridge.server.ssl.BridgeServerTrustManagerFactory;
import com.arcussmarthome.bridge.server.ssl.NullTrustManagerFactoryImpl;
import com.arcussmarthome.bridge.server.thread.DaemonThreadFactory;
import com.arcussmarthome.bridge.server.traffic.DefaultTrafficProvider;
import com.arcussmarthome.bridge.server.traffic.TrafficHandler;
import com.arcussmarthome.core.dao.cassandra.CassandraPlaceDAOModule;
import com.arcussmarthome.population.PlacePopulationCacheModule;
import com.arcussmarthome.video.VideoSessionRegistry;
import com.arcussmarthome.video.VideoStorageModule;
import com.arcussmarthome.video.cql.v2.CassandraVideoV2Module;
import com.arcussmarthome.video.recording.PlaceServiceLevelCacheModule;
import com.arcussmarthome.video.recording.StopRecordingSnooper;
import com.arcussmarthome.video.recording.StopRecordingSnooperConfig;
import com.netflix.governator.annotations.Modules;

import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;

@Modules(include= {
      BridgeEventLoopModule.class,
      CassandraVideoV2Module.class,
      VideoStorageModule.class,
      CassandraPlaceDAOModule.class,
      PlacePopulationCacheModule.class,
      PlaceServiceLevelCacheModule.class
})
public class VideoRecordingServerModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      bind(BridgeServerConfig.class);
      bind(VideoRecordingServerConfig.class);
      bind(VideoSessionRegistry.class).asEagerSingleton();
      bind(StopRecordingSnooperConfig.class).asEagerSingleton();
      bind(StopRecordingSnooper.class).asEagerSingleton();

      bind(BridgeServerTlsContext.class).to(BridgeServerTlsContextImpl.class);
      bind(BridgeServerTrustManagerFactory.class).to(NullTrustManagerFactoryImpl.class);

      bind(TrafficHandler.class).toProvider(DefaultTrafficProvider.class).asEagerSingleton();
   }

   @Provides @Named("videoTcpChannelOptions")
   public Map<ChannelOption<?>, Object> provideVideoTcpChannelOptions(VideoRecordingServerConfig serverConfig) {
      return ImmutableMap.of(
         ChannelOption.SO_KEEPALIVE, serverConfig.isSoKeepAlive()
      );
   }

   @Provides @Named("videoTcpParentChannelOptions")
   public Map<ChannelOption<?>, Object> provideVideoTcpParentChannelOptions(VideoRecordingServerConfig serverConfig) {
      return ImmutableMap.of(
         ChannelOption.SO_BACKLOG, serverConfig.getSoBacklog()
      );
   }

   @Provides @Named("videoBossGroup")
   public EventLoopGroup provideVideoBossGroup(VideoRecordingServerConfig serverConfig,
         @Named("videoBossThreadFactory") DaemonThreadFactory threadFactory,
         @Named("bridgeEventLoopProvider") BridgeServerEventLoopProvider evProvider) {
      return evProvider.create(serverConfig.getBossThreadCount(), threadFactory);
   }

   @Provides @Named("videoWorkerGroup")
   public EventLoopGroup provideVideoWorkerGroup(VideoRecordingServerConfig serverConfig,
         @Named("videoWorkerThreadFactory") DaemonThreadFactory threadFactory,
         @Named("bridgeEventLoopProvider") BridgeServerEventLoopProvider evProvider) {
      return evProvider.create(serverConfig.getWorkerThreadCount(), threadFactory);
   }

   @Provides @Named("videoBossThreadFactory")
   public DaemonThreadFactory provideVideoBossThreadFactory() {
      return new DaemonThreadFactory("video-boss");
   }

   @Provides @Named("videoWorkerThreadFactory")
   public DaemonThreadFactory provideVideoWorkerThreadFactory() {
      return new DaemonThreadFactory("video-worker");
   }
   
   @Provides @Singleton
   public BridgeMetrics provideBridgeMetrics() {
      return new BridgeMetrics("video-recording");
   }
}

