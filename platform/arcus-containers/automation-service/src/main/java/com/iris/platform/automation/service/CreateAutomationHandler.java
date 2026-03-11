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
import java.util.LinkedHashMap;
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
import com.iris.platform.rule.automation.AutomationFlow;
import com.iris.platform.rule.automation.ChainCompiler;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;

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

      // Support either legacy (conditions + actions) or multi-flow format
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> flowObjs = (List<Map<String, Object>>) attrs.get("flows");
      List<AutomationFlow> flows = new ArrayList<>();

      if (flowObjs != null && !flowObjs.isEmpty()) {
         // Multi-flow format
         for (Map<String, Object> flowObj : flowObjs) {
            @SuppressWarnings("unchecked")
            List<Object> flowCondObjs = (List<Object>) flowObj.get("conditions");
            @SuppressWarnings("unchecked")
            List<Object> flowActObjs = (List<Object>) flowObj.get("actions");
            if (flowActObjs == null || flowActObjs.isEmpty()) {
               throw new com.iris.messages.errors.ErrorEventException(
                     "invalid.param", "Each flow must have at least one action");
            }
            AutomationFlow.GuardLogic logic = AutomationFlow.GuardLogic.AND;
            String logicStr = (String) flowObj.get("guardLogic");
            if ("OR".equalsIgnoreCase(logicStr)) {
               logic = AutomationFlow.GuardLogic.OR;
            }
            flows.add(new AutomationFlow(
                  deserializeConditions(flowCondObjs),
                  deserializeActions(flowActObjs),
                  logic));
         }
      }
      else {
         // Legacy single-flow format
         @SuppressWarnings("unchecked")
         List<Object> conditionObjs = (List<Object>) attrs.get("conditions");
         List<ConditionConfig> conditions = deserializeConditions(conditionObjs);

         @SuppressWarnings("unchecked")
         List<Object> actionObjs = (List<Object>) attrs.get("actions");
         Errors.assertRequiredParam(actionObjs, "actions");
         List<ActionConfig> actions = deserializeActions(actionObjs);

         flows.add(new AutomationFlow(conditions, actions));
      }

      // Validate by compiling — this will throw if configs are invalid
      AutomationDefinition definition = new AutomationDefinition();
      definition.setPlaceId(placeId);
      definition.setName(name);
      definition.setDescription(description);
      definition.setTrigger(trigger);
      definition.setFlows(flows);
      definition.setDisabled(false);

      try {
         ChainCompiler.compile(definition);
      }
      catch (Exception e) {
         logger.warn("Failed to compile automation chain", e);
         throw new com.iris.messages.errors.ErrorEventException(
               "invalid.param",
               "Invalid automation chain: " + e.getMessage());
      }

      automationDao.save(definition);

      logger.info("Created automation [{}] for place [{}]: {}", definition.getSequenceId(), placeId, name);

      return MessageBody.buildMessage("auto:CreateResponse",
            ImmutableMap.of("automation", definition.getAddress()));
   }

   @SuppressWarnings("unchecked")
   private ConditionConfig deserializeCondition(Object obj) {
      Map<String, Object> blockMap = (Map<String, Object>) obj;
      Map<String, Object> condMap = translateConditionBlock(blockMap);
      String json = JSON.toJson(condMap);
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

   /**
    * Translates a UI block into a ConditionConfig-compatible map.
    * Merges selectedDevice/selectedAttribute/selectedMode/paramValues
    * into the flat structure expected by ConditionConfig subtypes.
    */
   @SuppressWarnings("unchecked")
   private Map<String, Object> translateConditionBlock(Map<String, Object> block) {
      String blockType = (String) block.get("type");
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("type", blockType);

      // Map UI-facing trigger types to ConditionConfig types
      if ("presence-change".equals(blockType)) {
         result.put("type", "presence");
      } else if ("alarm-change".equals(blockType)) {
         result.put("type", "alarm-state");
      }

      // Copy device selection into config fields
      if (block.containsKey("selectedDevice")) {
         result.put("address", block.get("selectedDevice"));
      }
      if (block.containsKey("selectedAttribute")) {
         result.put("attribute", block.get("selectedAttribute"));
      }
      if (block.containsKey("selectedValue")) {
         result.put("value", block.get("selectedValue"));
      }
      if (block.containsKey("selectedMode")) {
         result.put("mode", block.get("selectedMode"));
      }

      // Merge param values into the config
      Map<String, Object> paramValues = (Map<String, Object>) block.get("paramValues");
      if (paramValues != null) {
         result.putAll(paramValues);
      }

      return result;
   }

   private List<ActionConfig> deserializeActions(List<Object> objs) {
      if (objs == null || objs.isEmpty()) {
         throw new IllegalArgumentException("At least one action is required");
      }
      List<ActionConfig> configs = new ArrayList<>();
      for (Object obj : objs) {
         @SuppressWarnings("unchecked")
         Map<String, Object> blockMap = (Map<String, Object>) obj;
         Map<String, Object> actionMap = translateActionBlock(blockMap);
         String json = JSON.toJson(actionMap);
         configs.add(JSON.fromJson(json, ActionConfig.class));
      }
      return configs;
   }

   /**
    * Translates a UI block (from BlockRegistry) into a proper ActionConfig-compatible map.
    * UI blocks have types like "set-attribute", "notify", "fire-scene", "delay", "no-op".
    * ActionConfig subtypes use types like "set-attr", "send-notification", "send", "log", "no-op".
    */
   @SuppressWarnings("unchecked")
   private Map<String, Object> translateActionBlock(Map<String, Object> block) {
      String blockType = (String) block.get("type");
      if (blockType == null) {
         return block;
      }

      switch (blockType) {
         case "set-attribute": {
            // Translate to SetAttributeActionConfig
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("type", "set-attr");
            action.put("targetAttribute", block.get("selectedAttribute"));
            action.put("attributeValue", block.get("selectedValue"));
            action.put("address", block.get("selectedDevice"));
            return action;
         }
         case "notify": {
            // Translate to SendNotificationActionConfig
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("type", "send-notification");
            Map<String, Object> paramValues = (Map<String, Object>) block.get("paramValues");
            if (paramValues != null) {
               action.putAll(paramValues);
            }
            return action;
         }
         case "fire-scene": {
            // Translate to SendActionConfig — sends scene:Fire to the scene address
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("type", "send");
            action.put("sendActionType", "scene:Fire");
            action.put("address", block.get("selectedScene"));
            return action;
         }
         case "delay": {
            // Translate to a log action as placeholder (delay is handled by SequentialActionList timing)
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("type", "log");
            Map<String, Object> paramValues = (Map<String, Object>) block.get("paramValues");
            int duration = 5;
            if (paramValues != null && paramValues.get("duration") instanceof Number) {
               duration = ((Number) paramValues.get("duration")).intValue();
            }
            action.put("message", "Delay " + duration + " minutes");
            return action;
         }
         case "no-op": {
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("type", "no-op");
            return action;
         }
         default:
            // Unknown block type — try direct deserialization
            return block;
      }
   }
}
