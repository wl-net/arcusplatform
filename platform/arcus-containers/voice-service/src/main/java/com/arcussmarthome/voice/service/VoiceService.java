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
package com.arcussmarthome.voice.service;

import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.alexa.AlexaUtil;
import com.arcussmarthome.core.messaging.MessageListener;
import com.arcussmarthome.core.platform.AbstractPlatformService;
import com.arcussmarthome.core.platform.PlatformDispatcherFactory;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.google.Constants;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.AddressMatchers;
import com.arcussmarthome.voice.VoiceConfig;
import com.arcussmarthome.voice.context.VoiceContextExecutorRegistry;
import com.arcussmarthome.voice.context.VoiceContextExecutorResolver;
import com.arcussmarthome.voice.context.VoiceContextResolver;
import com.arcussmarthome.voice.context.VoiceDAO;
import com.arcussmarthome.voice.proactive.ProactiveCredsDAO;

@Singleton
public class VoiceService extends AbstractPlatformService {

   private static final Address ADDRESS = Address.platformService(com.arcussmarthome.messages.service.VoiceService.NAMESPACE);

   private final MessageListener<PlatformMessage> dispatcher;

   @Inject
   public VoiceService(
      PlatformMessageBus platformBus,
      @Named(VoiceConfig.NAME_EXECUTOR) ExecutorService executor,
      VoiceDAO voiceDao,
      ProactiveCredsDAO proactiveCredsDao,
      VoiceContextExecutorRegistry registry,
      VoiceContextExecutorResolver executorResolver,
      VoiceContextResolver contextResolver,
      PlatformDispatcherFactory factory
   ) {
      super(platformBus, ADDRESS, executor);
      this.dispatcher = factory
         .buildDispatcher()
         .addArgumentResolverFactory(executorResolver)
         .addArgumentResolverFactory(contextResolver)
         .addAnnotatedHandler(new StartPlaceHandler(voiceDao, registry))
         .addAnnotatedHandler(new StopPlaceHandler(voiceDao, proactiveCredsDao,registry))
         .addAnnotatedHandler(new EventHandler())
         .addAnnotatedHandler(new RequestHandler())
         .build();
   }

   @Override
   protected void doHandleMessage(PlatformMessage message) {
      dispatcher.onMessage(message);
   }

   @Override
   protected void onStart() {
      super.onStart();
      addListeners(
         AddressMatchers.equals(ADDRESS),
         AddressMatchers.equals(Constants.SERVICE_ADDRESS),
         AddressMatchers.equals(AlexaUtil.ADDRESS_SERVICE),
         AddressMatchers.BROADCAST_MESSAGE_MATCHER
      );
   }
}

