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
package com.arcussmarthome.driver.groovy.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arcussmarthome.capability.definition.ProtocolDefinition;
import com.arcussmarthome.device.attributes.LegacyAttributeConverter;
import com.arcussmarthome.device.model.AttributeDefinition;
import com.arcussmarthome.driver.groovy.binding.EnvironmentBinding;
import com.arcussmarthome.driver.groovy.plugin.ProtocolPlugin;
import com.arcussmarthome.driver.handler.ContextualEventHandler;
import com.arcussmarthome.driver.metadata.EventMatcher;
import com.arcussmarthome.messages.capability.DeviceAdvancedCapability;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.capability.DeviceCapability.DeviceRemovedEvent;
import com.arcussmarthome.protocol.Protocol;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.mock.MockProtocol;

public class MockProtocolPlugin extends ProtocolPlugin {
   private final Map<String, AttributeDefinition> attributes;
   
   public MockProtocolPlugin() {
      ProtocolDefinition protocolDef = MockProtocol.INSTANCE.getDefinition();
      Map<String, AttributeDefinition> attributeDefs = new HashMap<>();
      for (com.arcussmarthome.capability.definition.AttributeDefinition def : protocolDef.getAttributes()) {
         attributeDefs.put(def.getName(), LegacyAttributeConverter.convertToLegacyAttributeDef(def));
      }
      attributes = Collections.unmodifiableMap(attributeDefs);
   }

   @Override
   protected void addRootProperties(EnvironmentBinding binding) {
   }

   @Override
   protected void addContextProperties(EnvironmentBinding binding) {
   }

   @Override
   public Protocol<?> getProtocol() {
      return MockProtocol.INSTANCE;
   }

   @Override
   public Map<String, AttributeDefinition> getMatcherAttributes() {
      return attributes;
   }

   @Override
   public ContextualEventHandler<ProtocolMessage> createHandler(List<EventMatcher> matcher) {
      return null;
   }

}

