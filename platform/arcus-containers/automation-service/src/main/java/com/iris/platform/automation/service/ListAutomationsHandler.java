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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.core.platform.PlatformRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.Errors;
import com.iris.platform.rule.automation.AutomationDao;
import com.iris.platform.rule.automation.AutomationDefinition;

public class ListAutomationsHandler implements PlatformRequestMessageHandler {

   public static final String NAME = "auto:ListAutomations";

   private final AutomationDao automationDao;

   @Inject
   public ListAutomationsHandler(AutomationDao automationDao) {
      this.automationDao = automationDao;
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
      List<AutomationDefinition> automations = automationDao.listByPlace(placeId);

      List<Map<String, Object>> results = automations.stream()
            .map(ListAutomationsHandler::automationToMap)
            .collect(Collectors.toList());

      return MessageBody.buildMessage("auto:ListAutomationsResponse",
            com.google.common.collect.ImmutableMap.of("automations", results));
   }

   static Map<String, Object> automationToMap(AutomationDefinition def) {
      Map<String, Object> map = new HashMap<>();
      map.put(Capability.ATTR_ID, def.getId().getRepresentation());
      map.put(Capability.ATTR_ADDRESS,
            Address.platformService(def.getId().getRepresentation(), "auto").getRepresentation());
      map.put(Capability.ATTR_TYPE, "auto");
      map.put(Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, "auto"));
      map.put("auto:name", def.getName());
      map.put("auto:description", def.getDescription());
      map.put("auto:created", def.getCreated());
      map.put("auto:modified", def.getModified());
      map.put("auto:state", def.isDisabled() ? "DISABLED" : "ENABLED");
      map.put("auto:trigger", def.getTrigger());
      map.put("auto:conditions", def.getConditions());
      map.put("auto:actions", def.getActions());
      map.put("auto:flows", def.getFlows());
      map.put("auto:lastExecuted", def.getLastExecuted());
      return map;
   }
}
