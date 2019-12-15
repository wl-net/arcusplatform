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

import java.util.Optional;
import java.util.UUID;

import com.google.inject.name.Named;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.listener.annotation.Request;
import com.arcussmarthome.messages.service.VoiceService.StartPlaceRequest;
import com.arcussmarthome.util.IrisUUID;
import com.arcussmarthome.voice.context.VoiceContext;
import com.arcussmarthome.voice.context.VoiceContextExecutorRegistry;
import com.arcussmarthome.voice.context.VoiceDAO;

class StartPlaceHandler {

   private final VoiceDAO voiceDao;
   private final VoiceContextExecutorRegistry registry;

   StartPlaceHandler(VoiceDAO voiceDao, VoiceContextExecutorRegistry registry) {
      this.voiceDao = voiceDao;
      this.registry = registry;
   }

   @Request(value = StartPlaceRequest.NAME, service = true)
   public void handleStartPlace(
      PlatformMessage message,
      Optional<VoiceContext> context,
      @Named(StartPlaceRequest.ATTR_ASSISTANT) String assistant
   ) {
      long startTime = System.nanoTime();
      try {
         if(context.isPresent()) {
            voiceDao.recordAssistant(context.get().getPlaceId(), assistant);
            context.get().addAssistant(assistant);
            VoiceServiceMetrics.timeHandlerSuccess(StartPlaceRequest.NAME, startTime);
         } else {
            UUID id = IrisUUID.fromString(message.getPlaceId());
            voiceDao.recordAssistant(id, assistant);
            registry.add(id);
         }
         VoiceServiceMetrics.timeHandlerSuccess(StartPlaceRequest.NAME, startTime);
      } catch(RuntimeException e) {
         VoiceServiceMetrics.timeHandlerFailure(StartPlaceRequest.NAME, startTime);
         throw e;
      }
   }
}

