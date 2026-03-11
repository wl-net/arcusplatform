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
package com.iris.platform.automation.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.core.platform.PlatformRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.Errors;

/**
 * Handles auto:GetStartingPoints — returns available trigger blocks
 * for the first step of building an automation chain.
 */
public class GetStartingPointsHandler implements PlatformRequestMessageHandler {

   public static final String NAME = "auto:GetStartingPoints";

   private final BlockRegistry blockRegistry;

   @Inject
   public GetStartingPointsHandler(BlockRegistry blockRegistry) {
      this.blockRegistry = blockRegistry;
   }

   @Override
   public String getMessageType() {
      return NAME;
   }

   @Override
   public MessageBody handleMessage(PlatformMessage message) throws Exception {
      String placeIdStr = (String) message.getValue().getAttributes().get("placeId");
      Errors.assertRequiredParam(placeIdStr, "placeId");
      Errors.assertPlaceMatches(message, placeIdStr);

      UUID placeId = UUID.fromString(placeIdStr);
      List<Map<String, Object>> triggers = blockRegistry.getStartingPoints(placeId);

      return MessageBody.buildMessage("auto:GetStartingPointsResponse",
            ImmutableMap.of("triggers", triggers));
   }
}
