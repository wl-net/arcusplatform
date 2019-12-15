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
package com.arcussmarthome.common.subsystem.doorsnlocks;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.ContactCapability;
import com.arcussmarthome.messages.capability.DoorLockCapability;
import com.arcussmarthome.messages.capability.HubCapability;
import com.arcussmarthome.messages.capability.HubChimeCapability;
import com.arcussmarthome.messages.capability.KeyPadCapability;
import com.arcussmarthome.messages.capability.MotorizedDoorCapability;
import com.arcussmarthome.messages.model.test.ModelFixtures;

public class DoorsNLocksFixtures extends ModelFixtures {

   public static DeviceBuilder buildLock() {
      return ModelFixtures
         .buildDeviceAttributes(DoorLockCapability.NAMESPACE)
         .put(DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_LOCKED)
         .put(DoorLockCapability.ATTR_NUMPINSSUPPORTED, 5)
         .put(DoorLockCapability.ATTR_SLOTS, ImmutableMap.<String,String>of());
   }

   public static DeviceBuilder buildDoor() {
      return ModelFixtures
         .buildDeviceAttributes(MotorizedDoorCapability.NAMESPACE)
         .put(MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSED);
   }

   public static DeviceBuilder buildContact() {
      return ModelFixtures
         .buildDeviceAttributes(ContactCapability.NAMESPACE)
         .put(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED)
         .put(ContactCapability.ATTR_USEHINT, ContactCapability.USEHINT_DOOR);
   }

   public static DeviceBuilder buildKeypad() {
      return ModelFixtures
            .buildDeviceAttributes(KeyPadCapability.NAMESPACE);
   }

   public static Map<String,Object> createHubAttributes() {
      return buildServiceAttributes(HubCapability.NAMESPACE, HubChimeCapability.NAMESPACE)
            .put(Capability.ATTR_TYPE, "hub")
            .put(HubCapability.ATTR_NAME, "My Hub")
            .put(HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL)
            .put(HubCapability.ATTR_REGISTRATIONSTATE, HubCapability.REGISTRATIONSTATE_REGISTERED)
            .put(HubCapability.ATTR_ID, "ABC-1234")
            .create();
   }

}

