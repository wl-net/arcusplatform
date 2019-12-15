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
package com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.generators;

import static com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_HUBID;
import static com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_POWERSRC;

import java.util.Date;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.HubCapability;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.capability.PlaceMonitorSubsystemCapability;
import com.arcussmarthome.messages.type.SmartHomeAlert;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertTestCase;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;

public class TestHubOfflineGenerator extends SmartHomeAlertTestCase {

   private HubOfflineGenerator generator;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      generator = new HubOfflineGenerator();
   }

   @Test
   public void testHubOfflineAddsAlert() {
      replay();
      MessageBody body = PlaceMonitorSubsystemCapability.HubOfflineEvent.builder()
         .withHubAddress(hub.getAddress().getRepresentation())
         .withLastOnlineTime(new Date())
         .build();

      PlatformMessage msg = PlatformMessage.buildBroadcast(body, Address.platformService(PLACE_ID, PlaceMonitorSubsystemCapability.NAMESPACE))
         .withPlaceId(PLACE_ID)
         .create();

      generator.handleMessage(context, scratchPad, msg, hub);

      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE, Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE));
      assertScratchPadHasAlert(key);

      SmartHomeAlert expected = createAlert();
      assertAlert(expected, scratchPad.getAlert(key));
   }

   @Test
   public void testHubOnlineClearsAlert() {
      replay();
      MessageBody body = PlaceMonitorSubsystemCapability.HubOnlineEvent.builder()
         .withHubAddress(hub.getAddress().getRepresentation())
         .withOnlineTime(new Date())
         .build();

      PlatformMessage msg = PlatformMessage.buildBroadcast(body, Address.platformService(PLACE_ID, PlaceMonitorSubsystemCapability.NAMESPACE))
         .withPlaceId(PLACE_ID)
         .create();

      scratchPad.putAlert(createAlert());

      generator.handleMessage(context, scratchPad, msg, hub);

      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE, Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE));
      assertScratchPadNoAlert(key);
   }

   @Test
   public void testHubConnected() {
      replay();
      MessageBody body = HubCapability.HubConnectedEvent.instance();
      PlatformMessage msg = PlatformMessage.buildBroadcast(body, hub.getAddress())
         .withPlaceId(PLACE_ID)
         .create();

      scratchPad.putAlert(createAlert());

      generator.handleMessage(context, scratchPad, msg, hub);

      String key = AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE, Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE));
      assertScratchPadNoAlert(key);
   }

   private SmartHomeAlert createAlert() {
      return SmartHomeAlerts.create(
         SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE,
         SmartHomeAlert.SEVERITY_BLOCK,
         Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE),
         ImmutableMap.<String, Object>builder()
            .put(CONTEXT_ATTR_HUBID, hub.getId())
            .put(CONTEXT_ATTR_POWERSRC, "line powered")
            .build(),
         PLACE_ID
      );
   }
}

