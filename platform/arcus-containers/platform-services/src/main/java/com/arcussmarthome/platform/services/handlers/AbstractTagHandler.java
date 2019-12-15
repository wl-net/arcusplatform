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
package com.arcussmarthome.platform.services.handlers;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.arcussmarthome.core.dao.CRUDDao;
import com.arcussmarthome.core.platform.ContextualRequestMessageHandler;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.errors.ErrorEventException;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.model.BaseEntity;

abstract class AbstractTagHandler <T extends BaseEntity<?, T>> implements ContextualRequestMessageHandler<T> {

   private final CRUDDao<?, T> dao;
   private final PlatformMessageBus platformBus;

   protected AbstractTagHandler(CRUDDao<?, T> dao, PlatformMessageBus platformBus) {
      this.dao = dao;
      this.platformBus = platformBus;
   }
   
   protected void assertAccessible(T context, PlatformMessage msg) {
   	if(context == null) {
   		throw new ErrorEventException(Errors.notFound(msg.getDestination()));
   	}
   }

   @Override
   public MessageBody handleRequest(T context, PlatformMessage msg) {
      assertAccessible(context, msg);
      
      MessageBody request = msg.getValue();
      Set<String> tags = Capability.AddTagsRequest.getTags(request);
      if(tags == null || tags.isEmpty()) {
         return MessageBody.emptyMessage();
      }

      Set<String> curTags = context.getTags();
      Set<String> newTags = updateTags(curTags, tags);
      if(!curTags.equals(newTags)) {
         context.setTags(newTags);
         dao.save(context);
         sendValueChangeEvent(msg, ImmutableMap.of(Capability.ATTR_TAGS, newTags));
      }

      return MessageBody.emptyMessage();
   }

   protected abstract Set<String> updateTags(Set<String> curTags, Set<String> updates);

   private void sendValueChangeEvent(PlatformMessage request, Map<String, Object> changes) {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, changes);
      PlatformMessage msg = PlatformMessage.buildBroadcast(body, request.getDestination())
            .withPlaceId(request.getPlaceId())
            .withPopulation(request.getPopulation())
            .create();
      platformBus.send(msg);
   }
}

