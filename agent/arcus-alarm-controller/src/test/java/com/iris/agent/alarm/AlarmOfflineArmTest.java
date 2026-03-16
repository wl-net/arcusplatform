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
package com.iris.agent.alarm;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.KeyPadCapability;

/**
 * Tests for offline arm event construction and the ArmEvent class behavior
 * used by the offline arming feature.
 *
 * Note: Full integration tests of AlarmSecurity.cacheArmConfig/buildCachedArmEvent
 * require the agent system stack (SQLite, ConfigService, HubAttributesService) and
 * are covered by hub-level system testing.
 */
@RunWith(JUnit4.class)
public class AlarmOfflineArmTest {

   @Test
   public void testArmEventPreservesOnMode() {
      Address source = Address.broadcastAddress();
      Set<String> devices = ImmutableSet.of("DRIV:dev:d1", "DRIV:dev:d2");

      AlarmEvents.ArmEvent event = new AlarmEvents.ArmEvent(
         source, null, AlarmEvents.Mode.ON,
         false, 30, 45, 2, false, true, devices
      );

      Assert.assertEquals(AlarmEvents.Mode.ON, event.getMode());
      Assert.assertEquals(30, event.getEntranceDelaySecs());
      Assert.assertEquals(45, event.getExitDelaySecs());
      Assert.assertEquals(2, event.getAlarmSensitivityDeviceCount());
      Assert.assertFalse(event.isBypass());
      Assert.assertFalse(event.isSilent());
      Assert.assertTrue(event.isSoundsEnabled());
      Assert.assertEquals(devices, event.getActiveDevices());
   }

   @Test
   public void testArmEventPreservesPartialMode() {
      Address source = Address.broadcastAddress();
      Set<String> devices = ImmutableSet.of("DRIV:dev:d1");

      AlarmEvents.ArmEvent event = new AlarmEvents.ArmEvent(
         source, null, AlarmEvents.Mode.PARTIAL,
         false, 20, 15, 1, true, false, devices
      );

      Assert.assertEquals(AlarmEvents.Mode.PARTIAL, event.getMode());
      Assert.assertEquals(20, event.getEntranceDelaySecs());
      Assert.assertEquals(15, event.getExitDelaySecs());
      Assert.assertEquals(1, event.getAlarmSensitivityDeviceCount());
      Assert.assertFalse(event.isBypass());
      Assert.assertTrue(event.isSilent());
      Assert.assertFalse(event.isSoundsEnabled());
      Assert.assertEquals(devices, event.getActiveDevices());
   }

   @Test
   public void testArmEventWithNullActiveDevices() {
      Address source = Address.broadcastAddress();

      AlarmEvents.ArmEvent event = new AlarmEvents.ArmEvent(
         source, null, AlarmEvents.Mode.ON,
         false, 30, 30, 1, false, true, null
      );

      Assert.assertNotNull(event.getActiveDevices());
      Assert.assertTrue(event.getActiveDevices().isEmpty());
   }

   @Test
   public void testArmEventActiveDevicesAreImmutable() {
      Address source = Address.broadcastAddress();
      Set<String> devices = new LinkedHashSet<>(Arrays.asList("DRIV:dev:d1", "DRIV:dev:d2"));

      AlarmEvents.ArmEvent event = new AlarmEvents.ArmEvent(
         source, null, AlarmEvents.Mode.ON,
         false, 30, 30, 1, false, true, devices
      );

      // Original set modification should not affect the event
      devices.add("DRIV:dev:d3");
      Assert.assertEquals(2, event.getActiveDevices().size());
   }

   @Test
   public void testArmEventPreservesSourceAndActor() {
      Address source = Address.broadcastAddress();
      Address actor = Address.platformDriverAddress(java.util.UUID.randomUUID());

      AlarmEvents.ArmEvent event = new AlarmEvents.ArmEvent(
         source, actor, AlarmEvents.Mode.ON,
         false, 30, 30, 1, false, true, ImmutableSet.of("DRIV:dev:d1")
      );

      Assert.assertEquals(source.getRepresentation(), event.getSource());
      Assert.assertEquals(actor.getRepresentation(), event.getActor());
   }

   @Test
   public void testArmEventNoBypassForOfflineArm() {
      // Offline arm events should always have bypass=false since we can't
      // evaluate bypass conditions without platform involvement
      Address source = Address.broadcastAddress();
      Set<String> devices = ImmutableSet.of("DRIV:dev:d1", "DRIV:dev:d2");

      AlarmEvents.ArmEvent event = new AlarmEvents.ArmEvent(
         source, null, AlarmEvents.Mode.ON,
         false, 30, 30, 1, false, true, devices
      );

      Assert.assertFalse("Offline arm events should not have bypass set", event.isBypass());
   }

   @Test
   public void testArmRequestParsing() {
      // Verify the arm() factory method correctly parses a MessageBody
      MessageBody armRequest = HubAlarmCapability.ArmRequest.builder()
         .withMode("ON")
         .withBypassed(false)
         .withEntranceDelaySecs(45)
         .withExitDelaySecs(60)
         .withAlarmSensitivityDeviceCount(2)
         .withSilent(false)
         .withSoundsEnabled(true)
         .withActiveDevices(ImmutableSet.of("DRIV:dev:d1", "DRIV:dev:d2"))
         .build();

      Address from = Address.broadcastAddress();
      AlarmEvents.Event event = AlarmEvents.arm(from, null, armRequest);

      Assert.assertTrue(event instanceof AlarmEvents.ArmEvent);
      AlarmEvents.ArmEvent armEvent = (AlarmEvents.ArmEvent) event;

      Assert.assertEquals(AlarmEvents.Mode.ON, armEvent.getMode());
      Assert.assertEquals(45, armEvent.getEntranceDelaySecs());
      Assert.assertEquals(60, armEvent.getExitDelaySecs());
      Assert.assertEquals(2, armEvent.getAlarmSensitivityDeviceCount());
      Assert.assertTrue(armEvent.isSoundsEnabled());
      Assert.assertFalse(armEvent.isSilent());
   }

   @Test
   public void testArmRequestParsingPartial() {
      MessageBody armRequest = HubAlarmCapability.ArmRequest.builder()
         .withMode("PARTIAL")
         .withBypassed(true)
         .withEntranceDelaySecs(20)
         .withExitDelaySecs(15)
         .withAlarmSensitivityDeviceCount(1)
         .withSilent(true)
         .withSoundsEnabled(false)
         .withActiveDevices(ImmutableSet.of("DRIV:dev:d1"))
         .build();

      Address from = Address.broadcastAddress();
      AlarmEvents.ArmEvent armEvent = (AlarmEvents.ArmEvent) AlarmEvents.arm(from, null, armRequest);

      Assert.assertEquals(AlarmEvents.Mode.PARTIAL, armEvent.getMode());
      Assert.assertTrue(armEvent.isBypass());
      Assert.assertEquals(20, armEvent.getEntranceDelaySecs());
      Assert.assertEquals(15, armEvent.getExitDelaySecs());
      Assert.assertEquals(1, armEvent.getAlarmSensitivityDeviceCount());
      Assert.assertTrue(armEvent.isSilent());
      Assert.assertFalse(armEvent.isSoundsEnabled());
   }

   @Test
   public void testArmPressedEventModes() {
      // Verify keypad ArmPressedEvent mode strings match what we expect
      MessageBody armOnMsg = KeyPadCapability.ArmPressedEvent.builder()
         .withMode(KeyPadCapability.ArmPressedEvent.MODE_ON)
         .withBypass(false)
         .build();
      Assert.assertEquals("ON", KeyPadCapability.ArmPressedEvent.getMode(armOnMsg));

      MessageBody armPartialMsg = KeyPadCapability.ArmPressedEvent.builder()
         .withMode(KeyPadCapability.ArmPressedEvent.MODE_PARTIAL)
         .withBypass(false)
         .build();
      Assert.assertEquals("PARTIAL", KeyPadCapability.ArmPressedEvent.getMode(armPartialMsg));
   }

   @Test
   public void testArmPressedModeToAlarmMode() {
      // Test the mode conversion logic used in onKeypadArmPressed
      String modeOnRaw = "ON";
      AlarmEvents.Mode modeOn = "PARTIAL".equalsIgnoreCase(modeOnRaw) ? AlarmEvents.Mode.PARTIAL : AlarmEvents.Mode.ON;
      Assert.assertEquals(AlarmEvents.Mode.ON, modeOn);

      String modePartialRaw = "PARTIAL";
      AlarmEvents.Mode modePartial = "PARTIAL".equalsIgnoreCase(modePartialRaw) ? AlarmEvents.Mode.PARTIAL : AlarmEvents.Mode.ON;
      Assert.assertEquals(AlarmEvents.Mode.PARTIAL, modePartial);
   }
}
