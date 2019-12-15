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
package com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert;

import java.util.UUID;

import org.easymock.EasyMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.messages.capability.CellBackupSubsystemCapability;
import com.arcussmarthome.messages.capability.DeviceAdvancedCapability;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.capability.DevicePowerCapability;
import com.arcussmarthome.messages.capability.DoorLockCapability;
import com.arcussmarthome.messages.capability.HubPowerCapability;
import com.arcussmarthome.messages.capability.SubsystemCapability;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.SimpleModel;
import com.arcussmarthome.messages.model.SimpleModelStore;
import com.arcussmarthome.messages.model.subs.PlaceMonitorSubsystemModel;
import com.arcussmarthome.messages.model.test.ModelFixtures;
import com.arcussmarthome.messages.type.Population;
import com.arcussmarthome.messages.type.SmartHomeAlert;
import com.arcussmarthome.prodcat.ProductCatalog;
import com.arcussmarthome.prodcat.ProductCatalogEntry;
import com.arcussmarthome.prodcat.ProductCatalogManager;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.util.IrisUUID;

@Mocks({SubsystemContext.class, ProductCatalogManager.class, ProductCatalog.class})
public class SmartHomeAlertTestCase extends IrisMockTestCase {

   protected static final UUID PLACE_ID = IrisUUID.randomUUID();

   private static final Logger logger = LoggerFactory.getLogger(SmartHomeAlertTestCase.class);

   @Inject protected SubsystemContext context;
   @Inject protected ProductCatalogManager prodCatManager;
   @Inject private ProductCatalog prodCat;

   protected SimpleModelStore modelStore;

   protected Model hub;
   protected Model cellbackup;
   protected Model lock;
   protected Model door;
   protected PlaceMonitorSubsystemModel model;
   protected AlertScratchPad scratchPad;

   protected ProductCatalogEntry entry;

   @Override
   public void setUp() throws Exception {
      super.setUp();

      hub = new SimpleModel(ModelFixtures.createHubAttributes());
      hub.setAttribute(HubPowerCapability.ATTR_SOURCE, HubPowerCapability.SOURCE_MAINS);

      cellbackup = new SimpleModel(ModelFixtures.createServiceAttributes(PLACE_ID.toString(), CellBackupSubsystemCapability.NAMESPACE, SubsystemCapability.NAMESPACE));
      cellbackup.setAttribute(CellBackupSubsystemCapability.ATTR_STATUS, CellBackupSubsystemCapability.STATUS_NOTREADY);
      cellbackup.setAttribute(CellBackupSubsystemCapability.ATTR_NOTREADYSTATE, CellBackupSubsystemCapability.NOTREADYSTATE_BOTH);

      lock = new SimpleModel(ModelFixtures.buildDeviceAttributes(DoorLockCapability.NAMESPACE, DevicePowerCapability.NAMESPACE, DeviceAdvancedCapability.NAMESPACE)
         .online()
         .put(DeviceCapability.ATTR_PRODUCTID, "1")
         .put(DeviceCapability.ATTR_DEVTYPEHINT, "Door Lock")
         .put(DevicePowerCapability.ATTR_SOURCE, DevicePowerCapability.SOURCE_BATTERY)
         .put(DevicePowerCapability.ATTR_BATTERY, 30)
         .put(DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.<String, String>of())
         .put(DeviceAdvancedCapability.ATTR_PROTOCOL, "ZIGB")
         .create()
      );

      door = new SimpleModel(ModelFixtures.createMotorizedDoorFixture());
      door.setAttribute(DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.<String, String>of());
      door.setAttribute(DeviceCapability.ATTR_PRODUCTID, "1");
      door.setAttribute(DeviceCapability.ATTR_DEVTYPEHINT, "Garage Door");

      model = new PlaceMonitorSubsystemModel(new SimpleModel());

      modelStore = new SimpleModelStore();
      modelStore.addModel(hub.toMap());
      modelStore.addModel(cellbackup.toMap());
      modelStore.addModel(lock.toMap());
      modelStore.addModel(door.toMap());

      scratchPad = new AlertScratchPad();

      entry = new ProductCatalogEntry();
      entry.setId("1");
      entry.setBatteryPrimSize(ProductCatalogEntry.BatterySize.AA);
      entry.setBatteryPrimNum(4);
      entry.setVendor("Test");

      EasyMock.expect(context.models()).andReturn(modelStore).anyTimes();
      EasyMock.expect(context.getPlaceId()).andReturn(PLACE_ID).anyTimes();
      EasyMock.expect(context.logger()).andReturn(logger).anyTimes();
      EasyMock.expect(context.model()).andReturn(model).anyTimes();
      context.setVariable("alert.scratch", this.scratchPad);
      EasyMock.expectLastCall().anyTimes();
      EasyMock.expect(prodCatManager.getCatalog(Population.NAME_GENERAL)).andReturn(prodCat).anyTimes();
      EasyMock.expect(prodCat.getProductById("1")).andReturn(entry).anyTimes();
   }

   protected void assertScratchPadHasAlert(String key) {
      assertScratchPadHasAlert(scratchPad, key);
   }

   protected void assertScratchPadHasAlert(AlertScratchPad scratchPad, String key) {
      assertTrue("scratchpad expected to have " + key, scratchPad.hasAlert(key));
   }

   protected void assertScratchPadNoAlert(String key) {
      assertScratchPadNoAlert(scratchPad, key);
   }

   protected void assertScratchPadNoAlert(AlertScratchPad scratchPad, String key) {
      assertFalse("scratchpad should not have " + key, scratchPad.hasAlert(key));
   }

   protected void assertAlert(SmartHomeAlert expected, SmartHomeAlert actual) {
      // ignore dates
      expected.setCreated(null);
      actual.setCreated(null);

      assertEquals(expected.toMap(), actual.toMap());
   }


}

