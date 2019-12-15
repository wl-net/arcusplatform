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
package com.arcussmarthome.driver.groovy.error;

import java.util.Map;

import com.arcussmarthome.driver.DeviceDriverContext;
import com.arcussmarthome.driver.handler.AbstractDispatchingHandler;
import com.arcussmarthome.driver.handler.ContextualEventHandler;
import com.arcussmarthome.messages.ErrorEvent;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.control.ControlProtocol;

public class ErrorMessageHandler
   extends AbstractDispatchingHandler<ErrorEvent>
   implements ContextualEventHandler<ProtocolMessage> {

   public static ErrorMessageHandler.Builder builder() {
      return new Builder();
   }

   protected ErrorMessageHandler(Map<String, ContextualEventHandler<? super ErrorEvent>> handlers) {
      super(handlers);
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, ProtocolMessage event)
         throws Exception {
      MessageBody body = event.getValue(ControlProtocol.INSTANCE);
      ErrorEvent error = null;
      if("Error".equals(body.getMessageType())) {
         error = ErrorEvent.fromCode((String)body.getAttributes().get(ErrorEvent.CODE_ATTR), (String)body.getAttributes().get(ErrorEvent.MESSAGE_ATTR));
      }

      if(error == null) {
         return false;
      }

      if (deliver(error.getCode(), context, error)) {
         return true;
      }

      if (deliver(WILDCARD, context, error)) {
         return true;
      }

      return false;
   }

   public static class Builder extends AbstractDispatchingHandler.Builder<ErrorEvent, ErrorMessageHandler> {
      private Builder() {
      }

      public Builder addWildcardHandler(ContextualEventHandler<? super ErrorEvent> handler) {
         doAddHandler(WILDCARD, handler);
         return this;
      }

      public Builder addHandler(String errorCode, ContextualEventHandler<? super ErrorEvent> handler) {
         doAddHandler(errorCode, handler);
         return this;
      }

      @Override
      protected ErrorMessageHandler create(Map<String, ContextualEventHandler<? super ErrorEvent>> handlers) {
         return new ErrorMessageHandler(handlers);
      }
   }
}

