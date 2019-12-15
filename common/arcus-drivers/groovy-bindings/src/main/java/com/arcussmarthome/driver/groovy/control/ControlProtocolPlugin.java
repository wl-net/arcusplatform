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
package com.arcussmarthome.driver.groovy.control;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.arcussmarthome.device.model.AttributeDefinition;
import com.arcussmarthome.driver.groovy.binding.EnvironmentBinding;
import com.arcussmarthome.driver.groovy.error.ErrorMessageHandler;
import com.arcussmarthome.driver.groovy.error.ErrorProtocolEventMatcher;
import com.arcussmarthome.driver.groovy.error.OnErrorClosure;
import com.arcussmarthome.driver.groovy.plugin.ProtocolPlugin;
import com.arcussmarthome.driver.handler.ContextualEventHandler;
import com.arcussmarthome.driver.metadata.EventMatcher;
import com.arcussmarthome.protocol.Protocol;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.control.ControlProtocol;

public class ControlProtocolPlugin extends ProtocolPlugin {

   @Override
   protected void addRootProperties(EnvironmentBinding binding) {
      ControlContext ctCtx = new ControlContext();
      binding.setProperty("Control", ctCtx);
      binding.setProperty("control", ctCtx);
      binding.setProperty("onControl", new OnControlClosure(binding));
      binding.setProperty("onError", new OnErrorClosure(binding));
   }

   @Override
   protected void addContextProperties(EnvironmentBinding binding) {
   }

   @Override
   public Protocol<?> getProtocol() {
      return ControlProtocol.INSTANCE;
   }

   @Override
   public Map<String, AttributeDefinition> getMatcherAttributes() {
      return Collections.<String,AttributeDefinition>emptyMap();
   }

   @Override
   public ContextualEventHandler<ProtocolMessage> createHandler(List<EventMatcher> matchers) {
      ControlMessageHandler.Builder builder = null;
      ErrorMessageHandler.Builder errorBuilder = null;
      for (EventMatcher matcher: matchers) {
         if (matcher instanceof ControlProtocolEventMatcher) {
            if (builder == null) {
               builder = ControlMessageHandler.builder();
            }
            ControlProtocolEventMatcher msgMatcher = (ControlProtocolEventMatcher) matcher;
            if (msgMatcher.matchesAnyMessageType()) {
               builder.addWildcardHandler(msgMatcher.getHandler());
            }
            else {
               builder.addHandler(msgMatcher.getMessageType(), msgMatcher.getHandler());
            }
         } else if(matcher instanceof ErrorProtocolEventMatcher) {
            if(errorBuilder == null) {
               errorBuilder = ErrorMessageHandler.builder();
            }
            ErrorProtocolEventMatcher errMatcher = (ErrorProtocolEventMatcher) matcher;
            if(errMatcher.matchesAnyErrorCode()) {
               errorBuilder.addWildcardHandler(errMatcher.getHandler());
            } else {
               errorBuilder.addHandler(errMatcher.getErrorCode(), errMatcher.getHandler());
            }
         }
      }

      if(builder == null) {
         if(errorBuilder == null) {
            return null;
         }
         builder = ControlMessageHandler.builder();
      }

      builder.addErrorHandler(errorBuilder == null ? null : errorBuilder.build());

      return builder.build();
   }
}

