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
package com.arcussmarthome.voice.google;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.core.messaging.MessageListener;
import com.arcussmarthome.core.platform.PlatformDispatcherFactory;
import com.arcussmarthome.google.Constants;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.voice.VoiceProvider;
import com.arcussmarthome.voice.context.VoiceContextResolver;

@Singleton
public class GoogleService implements VoiceProvider {

   private final MessageListener<PlatformMessage> dispatcher;

   @Inject
   public GoogleService(
      VoiceContextResolver contextResolver,
      PlatformDispatcherFactory factory,
      RequestHandler requestHandler
   ) {
      this.dispatcher = factory
         .buildDispatcher()
         .addArgumentResolverFactory(contextResolver)
         .addAnnotatedHandler(requestHandler)
         .build();
   }

   @Override
   public Address address() {
      return Constants.SERVICE_ADDRESS;
   }

   @Override
   public void onMessage(PlatformMessage msg) {
      dispatcher.onMessage(msg);
   }
}

