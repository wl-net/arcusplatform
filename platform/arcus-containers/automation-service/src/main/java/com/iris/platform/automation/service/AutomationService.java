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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.AbstractPlatformService;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.RequestHandlers;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.Errors;

/**
 * Service handler for automation:* messages.
 * Routes requests to individual handlers for ListAutomations, GetStartingPoints,
 * GetNextSteps, and Create.
 */
@Singleton
public class AutomationService extends AbstractPlatformService {
   public static final String PROP_THREADPOOL = "service.automation.threadpool";
   public static final String NAMESPACE = "auto";

   private static final Logger logger = LoggerFactory.getLogger(AutomationService.class);

   private final Consumer<PlatformMessage> dispatcher;
   private final AutomationRequestHandler automationHandler;

   @Inject
   public AutomationService(
         @Named(PROP_THREADPOOL) Executor executor,
         PlatformMessageBus platformBus,
         ListAutomationsHandler listAutomations,
         GetStartingPointsHandler getStartingPoints,
         GetNextStepsHandler getNextSteps,
         CreateAutomationHandler createAutomation,
         AutomationRequestHandler automationHandler
   ) {
      super(platformBus, NAMESPACE, executor);
      this.dispatcher = RequestHandlers.toDispatcher(
            platformBus,
            listAutomations,
            getStartingPoints,
            getNextSteps,
            createAutomation
      );
      this.automationHandler = automationHandler;
   }

   @Override
   protected void onStart() {
      super.onStart();
      addListeners(getAddress());
   }

   @Override
   protected void handleRequestAndSendResponse(PlatformMessage message) {
      String type = message.getMessageType();
      Address dest = message.getDestination();

      // Instance-level operations on a specific automation
      if (isInstanceMessage(type) && dest.getId() != null) {
         getMessageBus().invokeAndSendResponse(message, () -> {
            UUID placeId = getPlaceId(message);
            switch (type) {
               case "auto:Enable":
                  return automationHandler.handleEnable(placeId, dest);
               case "auto:Disable":
                  return automationHandler.handleDisable(placeId, dest);
               case "auto:Delete":
                  return automationHandler.handleDelete(placeId, dest);
               case "auto:Update":
                  return automationHandler.handleUpdate(placeId, dest, message.getValue());
               case "base:GetAttributes":
                  return automationHandler.handleGetAttributes(placeId, dest);
               default:
                  return Errors.unsupportedMessageType(type);
            }
         });
         return;
      }

      this.dispatcher.accept(message);
   }

   private boolean isInstanceMessage(String type) {
      return "auto:Enable".equals(type)
            || "auto:Disable".equals(type)
            || "auto:Delete".equals(type)
            || "auto:Update".equals(type)
            || "base:GetAttributes".equals(type);
   }

   private UUID getPlaceId(PlatformMessage message) {
      Map<String, Object> attrs = message.getValue().getAttributes();
      if (attrs != null && attrs.containsKey("placeId")) {
         return UUID.fromString((String) attrs.get("placeId"));
      }
      String placeHeader = message.getPlaceId();
      Errors.assertRequiredParam(placeHeader, "placeId");
      return UUID.fromString(placeHeader);
   }
}
