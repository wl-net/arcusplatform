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
package com.arcussmarthome.device.attributes;

import java.util.HashSet;
import java.util.Set;

import com.arcussmarthome.capability.definition.AttributeType;
import com.arcussmarthome.capability.definition.AttributeType.RawType;
import com.arcussmarthome.device.model.AttributeDefinition;
import com.arcussmarthome.device.model.AttributeFlag;
import com.arcussmarthome.model.type.AttributeTypes;

//TODO: Stop-gap measure to use new attributes until legacy capabilities are removed from driver code.
public class LegacyAttributeConverter {
   
   public static AttributeDefinition convertToLegacyAttributeDef(
         com.arcussmarthome.capability.definition.AttributeDefinition def) {
      AttributeKey<?> key = convertToAttributeKey(def);
      com.arcussmarthome.model.type.AttributeType type = AttributeTypes.fromJavaType(def.getType().getJavaType());
      Set<AttributeFlag> flags = createAttributeFlags(def);
      return new AttributeDefinition(key, flags, def.getDescription(), def.getUnit(), type);
   }

   //TODO: Support Enum and Attributes types if support is ever needed.
   public static AttributeKey<?> convertToAttributeKey(
         com.arcussmarthome.capability.definition.AttributeDefinition def) {
      AttributeType attrType = def.getType();
      if (attrType.isCollection()) {
         AttributeType containedType = attrType.asCollection().getContainedType();
         if (attrType.getRawType() == RawType.SET) {
            return AttributeKey.createSetOf(def.getName(), containedType.getRawType().getJavaType());
         }
         else if (attrType.getRawType() == RawType.LIST) {
            return AttributeKey.createListOf(def.getName(), containedType.getRawType().getJavaType());
         }
         else if (attrType.getRawType() == RawType.MAP) {
            return AttributeKey.createMapOf(def.getName(), containedType.getRawType().getJavaType());
         }
         return null;
      }
      else {
         return AttributeKey.create(def.getName(), attrType.getRawType().getJavaType());
      }
   }
   
   private static Set<AttributeFlag> createAttributeFlags(
         com.arcussmarthome.capability.definition.AttributeDefinition def) {
      Set<AttributeFlag> flags = new HashSet<>();
      if (def.isReadable()) {
         flags.add(AttributeFlag.READABLE);
      }
      if (def.isWritable()) {
         flags.add(AttributeFlag.WRITABLE);
      }
      if (def.isOptional()) {
         flags.add(AttributeFlag.OPTIONAL);
      }
      return flags;
   }
}

