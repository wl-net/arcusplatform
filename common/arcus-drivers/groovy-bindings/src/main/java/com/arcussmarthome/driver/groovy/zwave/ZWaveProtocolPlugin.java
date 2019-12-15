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
package com.arcussmarthome.driver.groovy.zwave;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Singleton;
import com.arcussmarthome.capability.definition.ProtocolDefinition;
import com.arcussmarthome.device.attributes.LegacyAttributeConverter;
import com.arcussmarthome.device.model.AttributeDefinition;
import com.arcussmarthome.driver.groovy.DriverBinding;
import com.arcussmarthome.driver.groovy.binding.EnvironmentBinding;
import com.arcussmarthome.driver.groovy.plugin.ProtocolPlugin;
import com.arcussmarthome.driver.handler.ContextualEventHandler;
import com.arcussmarthome.driver.metadata.EventMatcher;
import com.arcussmarthome.messages.capability.HubCapability;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.zwave.ZWaveProtocol;
import com.arcussmarthome.protocol.zwave.model.ZWaveAllCommandClasses;

@Singleton
public class ZWaveProtocolPlugin extends ProtocolPlugin {
   private final Map<String, AttributeDefinition> attributes;

   public ZWaveProtocolPlugin() {
      ProtocolDefinition protocolDef = ZWaveProtocol.INSTANCE.getDefinition();
      Map<String, AttributeDefinition> attributeDefs = new HashMap<String, AttributeDefinition>();
      for (com.arcussmarthome.capability.definition.AttributeDefinition def : protocolDef.getAttributes()) {
         attributeDefs.put(def.getName(), LegacyAttributeConverter.convertToLegacyAttributeDef(def));
      }
      attributes = Collections.unmodifiableMap(attributeDefs);
   }

   @Override
   protected void addRootProperties(EnvironmentBinding binding) {
      ZWaveAllCommandClasses.init();
      binding.setProperty("onZWaveMessage", new OnZWaveClosure(
            ZWaveAllCommandClasses.allClasses, binding));
      binding.setProperty("onZWaveNodeInfo", new OnZWaveNodeInfo(binding));

      ZWaveContext zwCtx = new ZWaveContext();
      binding.setProperty("ZWave", zwCtx);
      binding.setProperty("zwave", zwCtx);
   }

   @Override
   protected void addContextProperties(EnvironmentBinding binding) {
   }

   @Override
   public void postProcessEnvironment(EnvironmentBinding binding) {
      if (binding instanceof DriverBinding) {
         ZWaveContext zctx = (ZWaveContext)binding.getProperty("ZWave");
         zctx.processReflexes((DriverBinding)binding);
      } 

      super.postProcessEnvironment(binding);
   }

   @Override
   public ZWaveProtocol getProtocol() {
      return ZWaveProtocol.INSTANCE;
   }

   @Override
   public Map<String, AttributeDefinition> getMatcherAttributes() {
      return attributes;
   }

   @Override
   public ContextualEventHandler<ProtocolMessage> createHandler(List<EventMatcher> matchers) {
      ZWaveMessageHandler.Builder builder = null;
      for(EventMatcher matcher: matchers) {
         if(!(matcher instanceof ZWaveProtocolEventMatcher)) {
            continue;
         }
         if(builder == null) {
            builder = ZWaveMessageHandler.builder();
         }
         ZWaveProtocolEventMatcher zwaveMatcher = (ZWaveProtocolEventMatcher) matcher;
         if(zwaveMatcher.matchesAnyMessageType()) {
            builder.addWildcardHandler(zwaveMatcher.getHandler());
         } else if (zwaveMatcher.matchesAnyCommandClass()) {
            builder.addHandler(zwaveMatcher.getMessageType(), zwaveMatcher.getHandler());
         } else if (zwaveMatcher.matchesAnyCommandId()) {
            builder.addHandler(zwaveMatcher.getMessageType(), zwaveMatcher.getCommandClass(),
                  zwaveMatcher.getHandler());
         } else {
            builder.addHandler(zwaveMatcher.getMessageType(), zwaveMatcher.getCommandClass(),
                  zwaveMatcher.getCommandId(), zwaveMatcher.getHandler());
         }
      }
      if(builder == null) {
         // no zwave matchers, don't create an entry
         return null;
      }
      return builder.build();
   }

}

