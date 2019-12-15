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
package com.arcussmarthome.platform.rule.environment;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.capability.definition.DefinitionRegistry;
import com.arcussmarthome.core.platform.handlers.SetAttributesModelRequestHandler;
import com.arcussmarthome.messages.capability.SceneCapability;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.type.Action;

@Singleton
public class SceneSetAttributesHandler extends SetAttributesModelRequestHandler {

   @Inject
   public SceneSetAttributesHandler(DefinitionRegistry registry) {
      super(registry);
   }

   @Override
   protected void setAttribute(Model model, String name, Object value) throws Exception {
      if(Objects.equals(SceneCapability.ATTR_ACTIONS, name)) {
         List<Map<String,Object>> actions = (List<Map<String,Object>>) value;
         if(actions != null && !actions.isEmpty()) {
            value = actions.stream().filter((m) -> {
               if(m == null) {
                  return false;
               }
               Map<String, Object> context = (Map<String, Object>) m.get(Action.ATTR_CONTEXT);
               return context != null && !context.isEmpty();
            })
            .collect(Collectors.toList());
         }
      }
      super.setAttribute(model, name, value);
   }
}

