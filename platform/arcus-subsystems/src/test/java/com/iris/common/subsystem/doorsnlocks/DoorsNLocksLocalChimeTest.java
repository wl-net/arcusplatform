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
package com.iris.common.subsystem.doorsnlocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DoorsNLocksSubsystemCapability;
import com.iris.messages.capability.HubChimeCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.type.DoorChimeConfig;

public class DoorsNLocksLocalChimeTest extends DoorsNLocksSubsystemTestCase {
   private static final String PROTOCOL = "ZIGB";
   private static final String PROTOCOL_ID = "0000000000000001";
   private static final String SENSOR_KEY = PROTOCOL + ":" + PROTOCOL_ID;

   private Address hubAddr;
   private Address keypadAddr;
   private String sensorAddr;

   private void addHub(boolean supportsLocalChime) {
      Map<String,Object> hub = new HashMap<>(DoorsNLocksFixtures.createHubAttributes());
      if (supportsLocalChime) {
         hub.put(HubChimeCapability.ATTR_SUPPORTSLOCALCHIME, Boolean.TRUE);
      }
      hubAddr = addModel(hub).getAddress();
   }

   private void addKeypad() {
      keypadAddr = addModel(DoorsNLocksFixtures.buildKeypad().create()).getAddress();
   }

   private void addSensor(boolean withProtocol) {
      Map<String,Object> sensor = new HashMap<>(DoorsNLocksFixtures.buildContact().create());
      if (withProtocol) {
         sensor.put(DeviceAdvancedCapability.ATTR_PROTOCOL, PROTOCOL);
         sensor.put(DeviceAdvancedCapability.ATTR_PROTOCOLID, PROTOCOL_ID);
      }
      sensorAddr = addModel(sensor).getAddress().getRepresentation();
   }

   private Set<Map<String,Object>> chimeConfig(boolean enabled) {
      DoorChimeConfig cfg = new DoorChimeConfig();
      cfg.setDevice(sensorAddr);
      cfg.setEnabled(enabled);
      return ImmutableSet.of(cfg.toMap());
   }

   // Changes the chime config the way a client would and runs the handler the
   // platform calls once that change commits.
   private void setChimeEnabled(boolean enabled) {
      Set<Map<String,Object>> old = context.model().getChimeConfig();
      Set<Map<String,Object>> updated = chimeConfig(enabled);
      context.model().setChimeConfig(updated);
      subsystem.onChimeConfigChanged(
            ModelChangedEvent.create(context.model().getAddress(), DoorsNLocksSubsystemCapability.ATTR_CHIMECONFIG, updated, old),
            context);
   }

   // Starts the subsystem again with its saved state, as after a service restart.
   private void restart() {
      subsystem.onEvent(SubsystemLifecycleEvent.started(context.model().getAddress()), context);
   }

   private List<Integer> requestsOfType(String type) {
      List<Integer> found = new ArrayList<>();
      List<MessageBody> bodies = requests.getValues();
      for (int i = 0; i < bodies.size(); i++) {
         if (type.equals(bodies.get(i).getMessageType())) {
            found.add(i);
         }
      }
      return found;
   }

   private MessageBody onlySyncRequest() {
      List<Integer> syncs = requestsOfType(HubChimeCapability.SyncChimeConfigRequest.NAME);
      assertEquals("SyncChimeConfig requests", 1, syncs.size());
      int index = syncs.get(0);
      assertEquals(hubAddr, requestAddresses.getValues().get(index));
      return requests.getValues().get(index);
   }

   private void assertNoSyncRequests() {
      assertEquals(0, requestsOfType(HubChimeCapability.SyncChimeConfigRequest.NAME).size());
   }

   private void openSensor() {
      updateModel(sensorAddr, ImmutableMap.<String,Object>of(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED));
   }

   @Test
   public void pushesConfigOnRestart() {
      addHub(true);
      addSensor(true);
      start();
      setChimeEnabled(true);
      clearRequests();

      restart();

      MessageBody sync = onlySyncRequest();
      assertEquals(ImmutableSet.of(SENSOR_KEY), HubChimeCapability.SyncChimeConfigRequest.getEnabledDevices(sync));
   }

   @Test
   public void startThatChangesConfigLeavesPushToChangeHandler() {
      addHub(true);
      addSensor(true);

      // The first start adds the sensor to chimeConfig, and that change pushes.
      start();

      assertNoSyncRequests();
   }

   @Test
   public void pushesConfigWhenItChanges() {
      addHub(true);
      addSensor(true);
      start();
      clearRequests();

      setChimeEnabled(true);

      MessageBody sync = onlySyncRequest();
      assertEquals(ImmutableSet.of(SENSOR_KEY), HubChimeCapability.SyncChimeConfigRequest.getEnabledDevices(sync));
   }

   @Test
   public void pushesEmptyConfigWhenChimeDisabled() {
      addHub(true);
      addSensor(true);
      start();
      setChimeEnabled(true);
      clearRequests();

      setChimeEnabled(false);

      MessageBody sync = onlySyncRequest();
      assertEquals(ImmutableSet.of(), HubChimeCapability.SyncChimeConfigRequest.getEnabledDevices(sync));
   }

   @Test
   public void pushesConfigWhenHubReconnects() {
      addHub(true);
      addSensor(true);
      start();
      setChimeEnabled(true);
      clearRequests();

      updateModel(hubAddr, ImmutableMap.<String,Object>of(HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_ONLINE));

      MessageBody sync = onlySyncRequest();
      assertEquals(ImmutableSet.of(SENSOR_KEY), HubChimeCapability.SyncChimeConfigRequest.getEnabledDevices(sync));
   }

   @Test
   public void pushesConfigWhenHubStartsReportingLocalChime() {
      addHub(false);
      addSensor(true);
      start();
      setChimeEnabled(true);
      clearRequests();

      updateModel(hubAddr, ImmutableMap.<String,Object>of(HubChimeCapability.ATTR_SUPPORTSLOCALCHIME, Boolean.TRUE));

      onlySyncRequest();
   }

   @Test
   public void neverPushesToHubWithoutLocalChime() {
      addHub(false);
      addSensor(true);
      start();
      setChimeEnabled(true);
      restart();
      updateModel(hubAddr, ImmutableMap.<String,Object>of(HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_ONLINE));

      assertNoSyncRequests();
   }

   @Test
   public void leavesOutSensorsWithoutProtocolAddress() {
      addHub(true);
      addSensor(false);
      start();
      clearRequests();

      setChimeEnabled(true);

      MessageBody sync = onlySyncRequest();
      assertEquals(ImmutableSet.of(), HubChimeCapability.SyncChimeConfigRequest.getEnabledDevices(sync));
   }

   @Test
   public void hubChimeKeyIsProtocolAndProtocolId() {
      Map<String,Object> device = new HashMap<>(DoorsNLocksFixtures.buildContact().create());
      device.put(DeviceAdvancedCapability.ATTR_PROTOCOL, PROTOCOL);
      device.put(DeviceAdvancedCapability.ATTR_PROTOCOLID, PROTOCOL_ID);
      assertEquals(SENSOR_KEY, DoorsNLocksContextAdapter.hubChimeKey(new SimpleModel(device)));

      Map<String,Object> cloudDevice = new HashMap<>(DoorsNLocksFixtures.buildContact().create());
      assertNull(DoorsNLocksContextAdapter.hubChimeKey(new SimpleModel(cloudDevice)));
   }

   @Test
   public void hubChimeNamesTheSensor() {
      addHub(true);
      addKeypad();
      addSensor(true);
      start();
      setChimeEnabled(true);
      clearRequests();

      openSensor();

      List<Integer> hubChimes = requestsOfType(HubChimeCapability.chimeRequest.NAME);
      assertEquals(1, hubChimes.size());
      assertEquals(hubAddr, requestAddresses.getValues().get(hubChimes.get(0)));
      assertEquals(SENSOR_KEY, HubChimeCapability.chimeRequest.getDevice(requests.getValues().get(hubChimes.get(0))));

      List<Integer> keypadChimes = requestsOfType(KeyPadCapability.ChimeRequest.NAME);
      assertEquals(1, keypadChimes.size());
      assertEquals(keypadAddr, requestAddresses.getValues().get(keypadChimes.get(0)));
      assertEquals(new HashSet<>(), requests.getValues().get(keypadChimes.get(0)).getAttributes().keySet());
   }

   @Test
   public void hubWithoutLocalChimeStillGetsChime() {
      addHub(false);
      addSensor(true);
      start();
      setChimeEnabled(true);
      clearRequests();

      openSensor();

      List<Integer> hubChimes = requestsOfType(HubChimeCapability.chimeRequest.NAME);
      assertEquals(1, hubChimes.size());
      assertEquals(hubAddr, requestAddresses.getValues().get(hubChimes.get(0)));
   }

   @Test
   public void hubChimeForSensorWithoutProtocolAddressNamesNoSensor() {
      addHub(true);
      addSensor(false);
      start();
      setChimeEnabled(true);
      clearRequests();

      openSensor();

      List<Integer> hubChimes = requestsOfType(HubChimeCapability.chimeRequest.NAME);
      assertEquals(1, hubChimes.size());
      assertNull(HubChimeCapability.chimeRequest.getDevice(requests.getValues().get(hubChimes.get(0))));
   }
}
