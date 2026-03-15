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
package com.iris.driver.groovy.zwaveassociation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.GetAttributesProvider;
import com.iris.messages.capability.ZWaveDirectAssociationCapability;

/**
 * Provides the zwda:associations and zwda:maxGroups attributes by
 * reading current values from driver variables.
 */
public class ZWaveAssociationGetAttributesProvider implements GetAttributesProvider {

   @Override
   public String getNamespace() {
      return ZWaveDirectAssociationCapability.NAMESPACE;
   }

   @Override
   public Map<String, Object> getAttributes(DeviceDriverContext context, Set<String> names) {
      Map<String, Object> result = new HashMap<>(2);

      int maxGroups = ZWaveAssociationUtil.getMaxGroups(context);
      if (maxGroups > 0) {
         result.put(ZWaveDirectAssociationCapability.ATTR_MAXGROUPS, maxGroups);
      }

      result.put(ZWaveDirectAssociationCapability.ATTR_ASSOCIATIONS,
            ZWaveAssociationUtil.buildAssociationsJson(context));

      return result;
   }
}
