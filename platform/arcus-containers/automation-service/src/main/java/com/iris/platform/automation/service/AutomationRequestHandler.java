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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.NotFoundException;
import com.iris.platform.rule.automation.AutomationDao;
import com.iris.platform.rule.automation.AutomationDefinition;
import com.iris.platform.rule.automation.ChainCompiler;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;

/**
 * Handles instance-level automation requests:
 * - auto:Enable
 * - auto:Disable
 * - auto:Delete
 * - auto:Update
 * - base:GetAttributes
 * - base:SetAttributes
 */
@Singleton
public class AutomationRequestHandler {

   private static final Logger logger = LoggerFactory.getLogger(AutomationRequestHandler.class);

   private final AutomationDao automationDao;

   @Inject
   public AutomationRequestHandler(AutomationDao automationDao) {
      this.automationDao = automationDao;
   }

   public MessageBody handleEnable(UUID placeId, Address destination) {
      AutomationDefinition def = findOrThrow(placeId, destination);
      def.setDisabled(false);
      automationDao.save(def);
      logger.info("Enabled automation [{}]", destination);
      return MessageBody.emptyMessage();
   }

   public MessageBody handleDisable(UUID placeId, Address destination) {
      AutomationDefinition def = findOrThrow(placeId, destination);
      def.setDisabled(true);
      automationDao.save(def);
      logger.info("Disabled automation [{}]", destination);
      return MessageBody.emptyMessage();
   }

   public MessageBody handleDelete(UUID placeId, Address destination) {
      int seqId = getSequenceId(destination);
      boolean deleted = automationDao.delete(placeId, seqId);
      if (!deleted) {
         throw new NotFoundException(destination);
      }
      logger.info("Deleted automation [{}]", destination);
      return MessageBody.emptyMessage();
   }

   public MessageBody handleUpdate(UUID placeId, Address destination, MessageBody body) {
      AutomationDefinition def = findOrThrow(placeId, destination);
      Map<String, Object> attrs = body.getAttributes();

      if (attrs.containsKey("name")) {
         def.setName((String) attrs.get("name"));
      }
      if (attrs.containsKey("description")) {
         def.setDescription((String) attrs.get("description"));
      }
      if (attrs.containsKey("trigger")) {
         ConditionConfig trigger = deserializeCondition(attrs.get("trigger"));
         def.setTrigger(trigger);
      }
      if (attrs.containsKey("conditions")) {
         @SuppressWarnings("unchecked")
         List<Object> condObjs = (List<Object>) attrs.get("conditions");
         def.setConditions(deserializeConditions(condObjs));
      }
      if (attrs.containsKey("actions")) {
         @SuppressWarnings("unchecked")
         List<Object> actionObjs = (List<Object>) attrs.get("actions");
         def.setActions(deserializeActions(actionObjs));
      }

      // Validate the updated chain compiles
      try {
         ChainCompiler.compile(def);
      }
      catch (Exception e) {
         logger.warn("Failed to compile updated automation chain", e);
         throw new com.iris.messages.errors.ErrorEventException(
               "invalid.param",
               "Invalid automation chain: " + e.getMessage());
      }

      automationDao.save(def);
      logger.info("Updated automation [{}]", destination);
      return MessageBody.emptyMessage();
   }

   public MessageBody handleGetAttributes(UUID placeId, Address destination) {
      AutomationDefinition def = findOrThrow(placeId, destination);
      return MessageBody.buildMessage("base:GetAttributesResponse",
            ListAutomationsHandler.automationToMap(def));
   }

   private AutomationDefinition findOrThrow(UUID placeId, Address destination) {
      int seqId = getSequenceId(destination);
      AutomationDefinition def = automationDao.findById(placeId, seqId);
      if (def == null) {
         throw new NotFoundException(destination);
      }
      return def;
   }

   private int getSequenceId(Address destination) {
      Object id = destination.getId();
      if (id instanceof Integer) {
         return (Integer) id;
      }
      // Address context qualifier is typically a string like "placeId.seqId"
      String idStr = String.valueOf(id);
      if (idStr.contains(".")) {
         return Integer.parseInt(idStr.substring(idStr.lastIndexOf('.') + 1));
      }
      return Integer.parseInt(idStr);
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
