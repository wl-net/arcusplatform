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
package com.arcussmarthome.common.subsystem.security;

import java.util.Map;
import java.util.UUID;

import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AlertCapability;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.ContactCapability;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.capability.DeviceConnectionCapability;
import com.arcussmarthome.messages.capability.GlassCapability;
import com.arcussmarthome.messages.capability.KeyPadCapability;
import com.arcussmarthome.messages.capability.MotionCapability;
import com.arcussmarthome.messages.model.test.ModelFixtures;
import com.arcussmarthome.util.IrisCollections;

public class SecurityFixtures extends ModelFixtures {

   static Map<String, Object> createGlassbreakFixture() {
      UUID id = UUID.randomUUID();
      return IrisCollections
      .<String, Object>map()
      .put(Capability.ATTR_ID, id.toString())
      .put(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE)
      .put(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation())
      .put(Capability.ATTR_CAPS, IrisCollections.setOf(Capability.NAMESPACE,GlassCapability.NAMESPACE,DeviceConnectionCapability.NAMESPACE))
      .put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE)
      .put(GlassCapability.ATTR_BREAK, GlassCapability.BREAK_SAFE).create();

   }

   static Map<String, Object> createMotionFixture() {
      UUID id = UUID.randomUUID();
      return IrisCollections
      .<String, Object>map()
      .put(Capability.ATTR_ID, id.toString())
      .put(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE)
      .put(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation())
      .put(Capability.ATTR_CAPS, IrisCollections.setOf(Capability.NAMESPACE,MotionCapability.NAMESPACE))
      .put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE)
      .put(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE).create();

   }
   static Map<String, Object> createKeypadFixture() {
      UUID id = UUID.randomUUID();
      return IrisCollections
      .<String, Object>map()
      .put(Capability.ATTR_ID, id.toString())
      .put(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE)
      .put(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation())
      .put(Capability.ATTR_CAPS, IrisCollections.setOf(Capability.NAMESPACE,KeyPadCapability.NAMESPACE,AlertCapability.NAMESPACE,MotionCapability.NAMESPACE))
      .put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE).create();
   }   

   static Map<String, Object> createContactFixture() {
      UUID id = UUID.randomUUID();
      return IrisCollections
      .<String, Object>map()
      .put(Capability.ATTR_ID, id.toString())
      .put(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE)
      .put(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation())
      .put(Capability.ATTR_CAPS, IrisCollections.setOf(Capability.NAMESPACE,ContactCapability.NAMESPACE))
      .put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE)
      .put(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED).create();

   }

   static Map<String, Object> createAlertFixture() {
      UUID id = UUID.randomUUID();
      return IrisCollections
      .<String, Object>map()
      .put(Capability.ATTR_ID, id.toString())
      .put(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE)
      .put(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation())
      .put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE)
      .put(Capability.ATTR_CAPS, IrisCollections.setOf(Capability.NAMESPACE,AlertCapability.NAMESPACE)).create();
   }   

}

