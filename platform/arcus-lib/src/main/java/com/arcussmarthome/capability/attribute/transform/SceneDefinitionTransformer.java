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
package com.arcussmarthome.capability.attribute.transform;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.arcussmarthome.capability.registry.CapabilityRegistry;
import com.arcussmarthome.device.model.AttributeDefinition;
import com.arcussmarthome.io.json.JSON;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.SceneCapability;
import com.arcussmarthome.messages.type.Action;
import com.arcussmarthome.platform.scene.SceneDefinition;
import com.arcussmarthome.util.TypeMarker;

/**
 * 
 */
public class SceneDefinitionTransformer extends ReflectiveBeanAttributesTransformer<SceneDefinition> {
   private static final TypeMarker<List<Map<String, Object>>> TYPE_ACTION = new TypeMarker<List<Map<String,Object>>>() {};
   
   public SceneDefinitionTransformer(CapabilityRegistry registry) {
      super(
            registry, 
            ImmutableSet.of(SceneCapability.NAMESPACE, Capability.NAMESPACE), 
            SceneDefinition.class
      );
   }

   /* (non-Javadoc)
    * @see com.iris.capability.attribute.transform.ReflectiveBeanAttributesTransformer#getValue(java.lang.Object, com.iris.device.model.AttributeDefinition)
    */
   @Override
   protected Object getValue(SceneDefinition scene, AttributeDefinition definition) throws Exception {
      if(SceneCapability.ATTR_ACTIONS.equals(definition.getName())) {
         if(scene.getAction() == null || scene.getAction().length == 0) {
            return ImmutableList.of();
         }
         else {
            List<Map<String, Object>> actions = JSON.fromJson(new String(scene.getAction()), TYPE_ACTION);
            return actions.stream().filter((m) -> {
               if(m == null) {
                  return false;
               }
               Map<String, Object> context = (Map<String, Object>) m.get(Action.ATTR_CONTEXT);
               return context != null && !context.isEmpty();
            })
            .collect(Collectors.toList());
         }
      }
      else if(SceneCapability.ATTR_FIRING.equals(definition.getName())) {
         // TODO implement firing flag
         return false;
      }
      else {
         return super.getValue(scene, definition);
      }
   }

   /* (non-Javadoc)
    * @see com.iris.capability.attribute.transform.ReflectiveBeanAttributesTransformer#setValue(java.lang.Object, java.lang.Object, com.iris.device.model.AttributeDefinition)
    */
   @Override
   protected void setValue(
         SceneDefinition bean, Object value, AttributeDefinition definition)
         throws Exception {
      if(SceneCapability.ATTR_ACTIONS.equals(definition.getName())) {
         bean.setAction(JSON.toJson(value).getBytes());
      }
      else {
         super.setValue(bean, value, definition);
      }
   }

}

