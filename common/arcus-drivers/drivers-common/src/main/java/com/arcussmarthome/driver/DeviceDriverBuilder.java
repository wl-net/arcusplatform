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
/**
 *
 */
package com.arcussmarthome.driver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Predicate;
import com.arcussmarthome.Utils;
import com.arcussmarthome.capability.key.NamespacedKey;
import com.arcussmarthome.device.attributes.AttributeKey;
import com.arcussmarthome.device.attributes.AttributeMap;
import com.arcussmarthome.device.attributes.AttributeValue;
import com.arcussmarthome.device.model.AttributeDefinition;
import com.arcussmarthome.device.model.CapabilityDefinition;
import com.arcussmarthome.device.model.CommandDefinition;
import com.arcussmarthome.device.model.EventDefinition;
import com.arcussmarthome.driver.capability.Capability;
import com.arcussmarthome.driver.event.DriverEvent;
import com.arcussmarthome.driver.handler.AttributeBindingHandler;
import com.arcussmarthome.driver.handler.ContextualEventHandler;
import com.arcussmarthome.driver.handler.ContextualEventHandlers;
import com.arcussmarthome.driver.handler.DriverEventHandler;
import com.arcussmarthome.driver.handler.MessageBodyHandler;
import com.arcussmarthome.driver.handler.PlatformMessageHandler;
import com.arcussmarthome.driver.handler.ProtocolMessageHandler;
import com.arcussmarthome.driver.reflex.ReflexDriver;
import com.arcussmarthome.driver.reflex.ReflexDriverDefinition;
import com.arcussmarthome.messages.Message;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.model.Version;
import com.arcussmarthome.protocol.ProtocolMessage;

/**
 *
 */
public class DeviceDriverBuilder {
   private DeviceDriverDefinition.Builder definitionBuilder = DeviceDriverDefinition.builder();
   private AttributeMap attributes = AttributeMap.newMap();
   private Predicate<AttributeMap> matcher;
   private AttributeBindingHandler attrBindingHandler = AttributeBindingHandler.NoBindingsHandler.INSTANCE;
   private List<Capability> capabilities = new ArrayList<>();
   private DriverEventHandler.Builder driverEventHandlerBuilder =
         DriverEventHandler.builder();
   private ProtocolMessageHandler.Builder protocolMessageHandlerBuilder =
         ProtocolMessageHandler.builder();
   private PlatformMessageHandler.Builder platformMessageHandlerBuilder =
         PlatformMessageHandler.builder();
   private MessageBodyHandler.Builder messageHandlerBuilder =
         MessageBodyHandler.builder();

   DeviceDriverBuilder() {
   }

   public DeviceDriverBuilder withName(String name) {
      this.definitionBuilder.withName(name);
      return this;
   }

   public DeviceDriverBuilder withVersion(Version version) {
      this.definitionBuilder.withVersion(version);
      return this;
   }

   public DeviceDriverBuilder withDescription(String description) {
      definitionBuilder.withDescription(description);
      return this;
   }

   public DeviceDriverBuilder withCommit(String commit) {
      definitionBuilder.withCommit(commit);
      return this;
   }

   public DeviceDriverBuilder withHash(String hash) {
      definitionBuilder.withHash(hash);
      return this;
   }

   public DeviceDriverBuilder withMatcher(Predicate<AttributeMap> matcher) {
      this.matcher = matcher;
      return this;
   }

   public DeviceDriverBuilder withAttributeBindingHandler(AttributeBindingHandler attrBindingHandler) {
      this.attrBindingHandler = attrBindingHandler;
      return this;
   }

   public DeviceDriverBuilder addAttribute(AttributeDefinition attribute) {
      definitionBuilder.addAttribute(attribute);
      return this;
   }

   public DeviceDriverBuilder addAttributeDefinitions(Collection<AttributeDefinition> attributes) {
      definitionBuilder.addAttributes(attributes);
      return this;
   }

   public DeviceDriverBuilder withAttributeValues(AttributeMap attributes) {
      this.attributes = AttributeMap.copyOf(attributes);
      return this;
   }

   public <T> DeviceDriverBuilder addAttribute(AttributeKey<T> key, T value) {
      attributes.set(key, value);
      return this;
   }

   public DeviceDriverBuilder addAttribute(AttributeValue<?> attribute) {
      attributes.add(attribute);
      return this;
   }

   public DeviceDriverBuilder addCommandDefinition(CommandDefinition commandDefinition) {
      definitionBuilder.addCommand(commandDefinition);
      return this;
   }

   public DeviceDriverBuilder addEvent(EventDefinition event) {
      definitionBuilder.addEvent(event);
      return this;
   }

   public DeviceDriverBuilder addCapabilityDefinition(CapabilityDefinition capability) {
      definitionBuilder.addCapability(capability);
      return this;
   }

   public DeviceDriverBuilder addCapabilityDefinitions(Collection<CapabilityDefinition> capabilities) {
      definitionBuilder.addCapabilities(capabilities);
      return this;
   }
   // TODO add capabilities

   //public DeviceDriverBuilder addReflexDefinitions(Collection<ReflexDefinition> reflexes) {
      //definitionBuilder.addReflexes(reflexes);
      //return this;
   //}

   public DeviceDriverBuilder withOfflineTimeout(long offlineTimeout) {
      definitionBuilder.withOfflineTimeout(offlineTimeout);
      return this;
   }

   public DeviceDriverBuilder addDriverEventHandler(ContextualEventHandler<DriverEvent> driverEventHandler) {
      driverEventHandlerBuilder.addWildcardHandler(driverEventHandler);
      return this;
   }

   public DeviceDriverBuilder addDriverEventHandler(
         Class<? extends DriverEvent> eventType,
         ContextualEventHandler<? super DriverEvent> handler
   ) {
      driverEventHandlerBuilder.addHandler(eventType, handler);
      return this;
   }

   public DeviceDriverBuilder addProtocolMessageHandler(ContextualEventHandler<? super ProtocolMessage> handler) {
      protocolMessageHandlerBuilder.addWildcardHandler(handler);
      return this;
   }

   public DeviceDriverBuilder addProtocolMessageHandler(
         String protocolName,
         ContextualEventHandler<? super ProtocolMessage> handler) {
      protocolMessageHandlerBuilder.addHandler(protocolName, handler);
      return this;
   }

   public DeviceDriverBuilder addPlatformMessageHandler(ContextualEventHandler<? super PlatformMessage> handler) {
      platformMessageHandlerBuilder.addWildcardHandler(handler);
      return this;
   }

   public DeviceDriverBuilder addPlatformMessageHandler(Class<? extends MessageBody> type, ContextualEventHandler<? super PlatformMessage> handler) {
      platformMessageHandlerBuilder.addHandler(type, handler);
      return this;
   }

   public DeviceDriverBuilder addHandler(ContextualEventHandler<? super MessageBody> handler) {
      messageHandlerBuilder.addWildcardHandler(handler);
      return this;
   }

   public DeviceDriverBuilder addHandler(
         NamespacedKey method,
         ContextualEventHandler<? super MessageBody> handler
   ) {
      messageHandlerBuilder.addHandler(method, handler);
      return this;
   }

   public DeviceDriverBuilder addCapability(Capability capability) {
      capabilities.add(capability);
      return this;
   }
   
   public DeviceDriverBuilder withPopulations(List<String> populations) {
   	definitionBuilder.withPopulations(populations);
   	return this;
   }

   public DeviceDriver create(boolean runReflexLifecycleActions) {
      return create(createDefinition(), runReflexLifecycleActions);
   }

   public DeviceDriver create(DeviceDriverDefinition definition, boolean runReflexLifecycleActions) {
      validate();

      ReflexDriver rdriver = null;
      ReflexDriverDefinition rdefinition = definition.getReflexes();
      if (rdefinition != null && 
          ((rdefinition.getReflexes() != null && !rdefinition.getReflexes().isEmpty()) ||
           (rdefinition.getDfa() != null && rdefinition.getDfa().getDfa() != null))) {
         rdriver = ReflexDriver.create(definition.getName(), definition.getVersion(), definition.getHash(), rdefinition.getCapabilities(), rdefinition.getReflexes(), rdefinition.getDfa());
      }

      return new DeviceDriverImpl(
            definition,
            matcher,
            createAttributeMap(attributes, capabilities),
            driverEventHandlerBuilder.build(),
            createMessageHandler(protocolMessageHandlerBuilder, platformMessageHandlerBuilder, messageHandlerBuilder, capabilities),
            attrBindingHandler,
            rdriver,
            runReflexLifecycleActions
      );
   }

   public DeviceDriverDefinition createDefinition() {
      return definitionBuilder.create();
   }

   protected void validate() {
      Utils.assertNotNull(matcher, "Must specify a matcher");
   }

   private static AttributeMap createAttributeMap(AttributeMap map, List<Capability> capabilities) {
      AttributeMap rval = AttributeMap.copyOf(map);
      for(Capability capability: capabilities) {
         for(AttributeValue<?> v: capability.getAttributes().entries()) {
            if(!rval.containsKey(v.getKey())) {
               rval.add(v);
            }
         }
      }
      return rval;
   }

   private static ContextualEventHandler<Message> createMessageHandler(
         ProtocolMessageHandler.Builder protocolMessageHandlerBuilder,
         PlatformMessageHandler.Builder platformMessageHandlerBuilder,
         MessageBodyHandler.Builder messageHandlerBuilder,
         List<Capability> capabilities
   ) {
      // add driver platform handlers
      List<ContextualEventHandler<PlatformMessage>> platformHandlers = new ArrayList<>();
      if(messageHandlerBuilder.hasAnyHandlers()) {
         platformHandlers.add(messageHandlerBuilder.build());
      }
      if(platformMessageHandlerBuilder.hasAnyHandlers()) {
         platformHandlers.add(platformMessageHandlerBuilder.build());
      }

      // add driver protocol handlers
      List<ContextualEventHandler<ProtocolMessage>> protocolHandlers = new ArrayList<>();
      if(protocolMessageHandlerBuilder.hasAnyHandlers()) {
         protocolHandlers.add(protocolMessageHandlerBuilder.build());
      }

      // add capability handlers
      for(Capability capability: capabilities) {
         ContextualEventHandler<PlatformMessage> platformHandler = capability.getPlatformMessageHandler();
         if(platformHandler != null) {
            platformHandlers.add(platformHandler);
         }
         ContextualEventHandler<ProtocolMessage> protocolHandler = capability.getProtocolMessageHandler();
         if(protocolHandler != null) {
            protocolHandlers.add(protocolHandler);
         }
      }

      return new DeviceDriverEventHandler(
            ContextualEventHandlers.marshalDispatcher(platformHandlers),
            ContextualEventHandlers.marshalDispatcher(protocolHandlers)
      );
   }

}

