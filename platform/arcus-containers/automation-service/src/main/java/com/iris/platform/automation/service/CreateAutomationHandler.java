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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.core.platform.PlatformRequestMessageHandler;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.Errors;
import com.iris.platform.rule.automation.AutomationDao;
import com.iris.platform.rule.automation.AutomationDefinition;
import com.iris.platform.rule.automation.ChainCompiler;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;
import com.iris.util.TypeMarker;

/**
 * Handles auto:Create — validates and persists a new automation chain.
 *
 * The client sends the complete chain (trigger + conditions + actions) as
 * JSON objects. This handler:
 * 1. Deserializes the configs
 * 2. Validates by compiling them (ensures they produce valid rule objects)
 * 3. Persists the automation definition
 * 4. Returns the address of the new automation
 */
public class CreateAutomationHandler implements PlatformRequestMessageHandler {

   public static final String NAME = "auto:Create";

   private static final Logger logger = LoggerFactory.getLogger(CreateAutomationHandler.class);

   private final AutomationDao automationDao;

   @Inject
   public CreateAutomationHandler(AutomationDao automationDao) {
      this.automationDao = automationDao;
   }

   @Override
   public String getMessageType() {
      return NAME;
   }

   @Override
   public MessageBody handleMessage(PlatformMessage message) throws Exception {
      Map<String, Object> attrs = message.getValue().getAttributes();
      String placeIdStr = (String) attrs.get("placeId");
      String name = (String) attrs.get("name");
      String description = (String) attrs.get("description");

      Errors.assertRequiredParam(placeIdStr, "placeId");
      Errors.assertRequiredParam(name, "name");
      Errors.assertPlaceMatches(message, placeIdStr);

      UUID placeId = UUID.fromString(placeIdStr);

      // Deserialize the chain blocks from the request
      Object triggerObj = attrs.get("trigger");
      Errors.assertRequiredParam(triggerObj, "trigger");
      ConditionConfig trigger = deserializeCondition(triggerObj);

      @SuppressWarnings("unchecked")
      List<Object> conditionObjs = (List<Object>) attrs.get("conditions");
      List<ConditionConfig> conditions = deserializeConditions(conditionObjs);

      @SuppressWarnings("unchecked")
      List<Object> actionObjs = (List<Object>) attrs.get("actions");
      Errors.assertRequiredParam(actionObjs, "actions");
      List<ActionConfig> actions = deserializeActions(actionObjs);

      // Validate by compiling — this will throw if configs are invalid
      try {
         ChainCompiler.compileCondition(trigger, conditions, Collections.emptyMap());
         ChainCompiler.compileActions(actions, Collections.emptyMap());
      }
      catch (Exception e) {
         logger.warn("Failed to compile automation chain", e);
         throw new com.iris.messages.errors.ErrorEventException(
               "invalid.param",
               "Invalid automation chain: " + e.getMessage());
      }

      // Build and persist
      AutomationDefinition definition = new AutomationDefinition();
      definition.setPlaceId(placeId);
      definition.setName(name);
      definition.setDescription(description);
      definition.setTrigger(trigger);
      definition.setConditions(conditions);
      definition.setActions(actions);
      definition.setDisabled(false);

      automationDao.save(definition);

      logger.info("Created automation [{}] for place [{}]: {}", definition.getSequenceId(), placeId, name);

      return MessageBody.buildMessage("auto:CreateResponse",
            ImmutableMap.of("automation", definition.getAddress()));
   }

   private ConditionConfig deserializeCondition(Object obj) {
      String json = JSON.toJson(obj);
      return JSON.fromJson(json, ConditionConfig.class);
   }

   private List<ConditionConfig> deserializeConditions(List<Object> objs) {
      if (objs == null || objs.isEmpty()) {
         return Collections.emptyList();
      }
      List<ConditionConfig> configs = new ArrayList<>();
      for (Object obj : objs) {
         configs.add(deserializeCondition(obj));
      }
      return configs;
   }

   private List<ActionConfig> deserializeActions(List<Object> objs) {
      if (objs == null || objs.isEmpty()) {
         throw new IllegalArgumentException("At least one action is required");
      }
      List<ActionConfig> configs = new ArrayList<>();
      for (Object obj : objs) {
         String json = JSON.toJson(obj);
         configs.add(JSON.fromJson(json, ActionConfig.class));
      }
      return configs;
   }
}
