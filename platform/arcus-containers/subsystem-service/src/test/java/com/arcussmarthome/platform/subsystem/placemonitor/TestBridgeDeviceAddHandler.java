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
package com.arcussmarthome.platform.subsystem.placemonitor;

import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.arcussmarthome.common.subsystem.Subsystem;
import com.arcussmarthome.common.subsystem.SubsystemTestCase;
import com.arcussmarthome.common.subsystem.event.SubsystemLifecycleEvent;
import com.arcussmarthome.core.template.TemplateService;
import com.arcussmarthome.messages.capability.BridgeCapability;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.capability.PlaceMonitorSubsystemCapability;
import com.arcussmarthome.messages.capability.SubsystemCapability;
import com.arcussmarthome.messages.capability.SwitchCapability;
import com.arcussmarthome.messages.event.Listener;
import com.arcussmarthome.messages.event.ModelEvent;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.SimpleModel;
import com.arcussmarthome.messages.model.subs.PlaceMonitorSubsystemModel;
import com.arcussmarthome.messages.model.test.ModelFixtures;
import com.arcussmarthome.platform.subsystem.placemonitor.pairing.BridgeDeviceAddHandler;

public class TestBridgeDeviceAddHandler extends SubsystemTestCase<PlaceMonitorSubsystemModel> {

   private PlaceMonitorSubsystem subsystem = null;
   BridgeDeviceAddHandler handler=null;

   Model bridgeDevice1 =null;
   Model bridgeDevice2 =null;

   Map<String, Object> place = ModelFixtures.createPlaceAttributes();

   Model accountModel = null;
   Model owner = null;
   Model hub = null;

   TemplateService templateService = EasyMock.createMock(TemplateService.class);


   @Override
   protected PlaceMonitorSubsystemModel createSubsystemModel() {
      Map<String, Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, PlaceMonitorSubsystemCapability.NAMESPACE);
      return new PlaceMonitorSubsystemModel(new SimpleModel(attributes));
   }

   @Override
   protected Subsystem<PlaceMonitorSubsystemModel> subsystem() {
      return subsystem;
   }

   protected void reloadStore(){
      store.addModel(place);
      store.addModel(ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).create());

   }

   protected void start() {
      reloadStore();
      subsystem.onEvent(SubsystemLifecycleEvent.added(context.model().getAddress()), context);
      subsystem.onEvent(SubsystemLifecycleEvent.started(context.model().getAddress()), context);
      store.addListener(new Listener<ModelEvent>() {
         @Override
         public void onEvent(ModelEvent event) {
            subsystem.onEvent(event, context);
         }
      });
   }

   @Before
   public void init() {
      subsystem = new PlaceMonitorSubsystem(ImmutableMap.<String, PlaceMonitorHandler>of(),new PlaceMonitorNotifications(null), templateService);
      bridgeDevice1 = new SimpleModel(ModelFixtures.buildDeviceAttributes(BridgeCapability.NAMESPACE).create());
      bridgeDevice1.setAttribute(DeviceCapability.ATTR_NAME, "Bridge1");

      handler = new BridgeDeviceAddHandler();
      subsystem.handlers=ImmutableMap.<String, PlaceMonitorHandler>of(BridgeDeviceAddHandler.class.getName(),handler);
      place.put(PlaceCapability.ATTR_ID,placeId.toString());
   }


   @Test
   public void testPairOnAdded() {
      start();
      store.addModel(bridgeDevice1.toMap());
      assertEquals(1, requests.getValues().size());
      assertEquals(BridgeCapability.StartPairingRequest.NAME, requests.getValues().get(0).getMessageType());
   }
}

