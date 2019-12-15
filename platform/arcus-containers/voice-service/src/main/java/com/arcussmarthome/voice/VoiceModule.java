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
package com.arcussmarthome.voice;

import java.util.concurrent.ExecutorService;

import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.arcussmarthome.core.messaging.kafka.KafkaModule;
import com.arcussmarthome.messages.MessageConstants;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.service.VoiceService;
import com.arcussmarthome.platform.model.ModelDaoModule;
import com.arcussmarthome.prodcat.ProductCatalogModule;
import com.arcussmarthome.prodcat.ProductCatalogReloadListener;
import com.arcussmarthome.util.LoggingUncaughtExceptionHandler;
import com.arcussmarthome.util.ThreadPoolBuilder;
import com.arcussmarthome.voice.alexa.AlexaModule;
import com.arcussmarthome.voice.context.VoiceContextExecutorRegistry;
import com.arcussmarthome.voice.exec.CommandExecutor;
import com.arcussmarthome.voice.google.GoogleModule;
import com.arcussmarthome.voice.google.homegraph.HomeGraphAPI;
import com.arcussmarthome.voice.proactive.ProactiveReporter;
import com.arcussmarthome.voice.proactive.ProactiveReportingConfig;
import com.arcussmarthome.voice.service.VoiceServiceModule;
import com.netflix.governator.annotations.Modules;

import io.netty.util.HashedWheelTimer;

@Modules(include={
   CassandraResourceBundleDAOModule.class,
   KafkaModule.class,
   ModelDaoModule.class,
   VoiceServiceModule.class,
   GoogleModule.class,
   AlexaModule.class,
   ProductCatalogModule.class
})
public class VoiceModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      bind(VoiceContextExecutorRegistry.class).asEagerSingleton();
      bind(VoiceService.class).asEagerSingleton();
   }

   @Provides
   @Named(VoiceConfig.NAME_EXECUTOR)
   @Singleton
   public ExecutorService voiceServiceExecutor(VoiceConfig config) {
      return new ThreadPoolBuilder()
         .withBlockingBacklog()
         .withMaxPoolSize(config.getServiceMaxThreads())
         .withKeepAliveMs(config.getServiceThreadKeepAliveMs())
         .withNameFormat("voice-service-%d")
         .withMetrics("voice.service")
         .build();
   }

   @Provides
   @Named(ProactiveReporter.EXECUTOR_NAME)
   @Singleton
   public ExecutorService proactiveReporterExecutor(ProactiveReportingConfig config) {
      return new ThreadPoolBuilder()
         .withBlockingBacklog()
         .withMaxPoolSize(config.getReportingMaxThreads())
         .withKeepAliveMs(config.getReportingThreadKeepAliveMs())
         .withNameFormat("voice-service-proactive-%d")
         .withMetrics("voice.service.proactive")
         .build();
   }
   
   /**
    * Executor for sending report state after SYNC responses.  Report State is delayed to ensure the SYNC
    * reaches google.
    */
   @Provides
   @Named(HomeGraphAPI.EXECUTOR_NAME)
   @Singleton
   public HashedWheelTimer googleReportStateExecutor(ProactiveReportingConfig config) {
      return new HashedWheelTimer(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("voice-service-google-report-state-%d")
            .setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler(LoggerFactory.getLogger(CommandExecutor.class)))
            .build());
   }

   @Provides
   @Named(VoiceConfig.NAME_TIMEOUT_TIMER)
   @Singleton
   public HashedWheelTimer timeoutTimer() {
      return new HashedWheelTimer(new ThreadFactoryBuilder()
         .setDaemon(true)
         .setNameFormat("voice-execute-timeout-%d")
         .setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler(LoggerFactory.getLogger(CommandExecutor.class)))
         .build());
   }
   
   @Provides
   @Singleton
   @Named(ProductCatalogReloadListener.GENERIC_MESSAGE_BUS_ACTOR_ADDRESS)
   public Address provideMessageBusActorAddress() {
      return Address.fromString(MessageConstants.SERVICE + ":" + VoiceService.NAMESPACE + ":");
   }

}

