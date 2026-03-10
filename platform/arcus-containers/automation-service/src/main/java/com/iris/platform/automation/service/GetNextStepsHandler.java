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
 * Handles auto:GetNextSteps — given a partial chain (trigger + any conditions
 * selected so far), returns the available next steps: more conditions the user
 * can add, plus available actions.
 *
 * This is the "smart" endpoint that powers the step-by-step UI. It knows what
 * blocks are available based on what the user has already picked.
 */
public class GetNextStepsHandler implements PlatformRequestMessageHandler {

   public static final String NAME = "auto:GetNextStepsRequest";

   private final BlockRegistry blockRegistry;

   @Inject
   public GetNextStepsHandler(BlockRegistry blockRegistry) {
      this.blockRegistry = blockRegistry;
   }

   @Override
   public String getMessageType() {
      return NAME;
   }

   @Override
   public MessageBody handleMessage(PlatformMessage message) throws Exception {
      Map<String, Object> attrs = message.getValue().getAttributes();
      String placeIdStr = (String) attrs.get("placeId");
      Errors.assertRequiredParam(placeIdStr, "placeId");
      Errors.assertPlaceMatches(message, placeIdStr);

      UUID placeId = UUID.fromString(placeIdStr);

      // Get available conditions and actions for this place
      List<Map<String, Object>> conditions = blockRegistry.getConditions(placeId);
      List<Map<String, Object>> actions = blockRegistry.getActions(placeId);

      // Filter out conditions that have already been selected
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> existingConditions =
            (List<Map<String, Object>>) attrs.get("conditions");
      if (existingConditions != null && !existingConditions.isEmpty()) {
         // Remove condition types that are already in the chain
         // (e.g., if they already have a presence guard, don't offer it again)
         List<String> usedTypes = existingConditions.stream()
               .map(c -> (String) c.get("type"))
               .collect(java.util.stream.Collectors.toList());
         conditions = conditions.stream()
               .filter(c -> !usedTypes.contains(c.get("type")))
               .collect(java.util.stream.Collectors.toList());
      }

      return MessageBody.buildMessage("auto:GetNextStepsResponse",
            ImmutableMap.of(
                  "conditions", conditions,
                  "actions", actions
            ));
   }
}
