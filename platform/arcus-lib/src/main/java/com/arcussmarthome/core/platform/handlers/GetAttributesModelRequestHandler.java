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
package com.arcussmarthome.core.platform.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.Singleton;
import com.arcussmarthome.Utils;
import com.arcussmarthome.capability.definition.AttributeType;
import com.arcussmarthome.capability.definition.AttributeTypes;
import com.arcussmarthome.core.platform.ContextualRequestMessageHandler;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.model.Model;

/**
 * 
 */
@Singleton
public class GetAttributesModelRequestHandler implements ContextualRequestMessageHandler<Model> {
   private static final AttributeType TYPE_STRING_SET = 
         AttributeTypes.parse("set<string>");
   
   @Override
   public String getMessageType() {
      return Capability.CMD_GET_ATTRIBUTES;
   }

   @Override
   public MessageBody handleRequest(Model model, PlatformMessage message) {
      MessageBody request = message.getValue();
      Set<String> names = (Set<String>) TYPE_STRING_SET.coerce(request.getAttributes().get("name"));
      Map<String, Object> attributes;
      if(names == null || names.isEmpty()) {
         attributes = model.toMap();
      }
      else {
         Set<String> attributeNames = new HashSet<>();
         Set<String> attributeNamespaces = new HashSet<>();
         for(String name: names) {
            if(Utils.isNamespaced(name)) {
               attributeNames.add(name);
            }
            else {
               attributeNamespaces.add(name);
            }
         }
         
         attributes = new HashMap<String, Object>();
         for(Map.Entry<String, Object> entry: model.toMap().entrySet()) {
            String name = entry.getKey();
            if(attributeNames.contains(name)) {
               attributes.put(name, entry.getValue());
            }
            else if(attributeNamespaces.contains(Utils.getNamespace(name))){
               attributes.put(name, entry.getValue());
            }
         }
      }

      filter(attributes);
      return MessageBody.buildMessage(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, attributes);
   }

   protected void filter(Map<String, Object> attributes) {
      // no-op
   }
}

