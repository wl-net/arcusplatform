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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.collect.ImmutableSet;
import com.iris.agent.alarm.AlarmEvents.ArmEvent;
import com.iris.agent.alarm.AlarmEvents.Mode;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.messages.address.Address;

@RunWith(JUnit4.class)
public class OfflineArmCacheTest {
   // In-memory attributes, so the cache can be exercised without the hub's config database.
   private static final OfflineArmCache.AttributeFactory EPHEMERAL = new OfflineArmCache.AttributeFactory() {
      @Override
      public <T> HubAttributesService.Attribute<T> create(Class<T> type, String name, T def) {
         return HubAttributesService.ephemeral(type, name, def);
      }

      @Override
      public HubAttributesService.Attribute<Set<String>> createSet(String name) {
         return HubAttributesService.ephemeralSet(String.class, name, new LinkedHashSet<>());
      }
   };

   private final Address platform = Address.platformService(UUID.randomUUID(), "subalarm");
   private final Address keypad = Address.platformDriverAddress(UUID.randomUUID());

   private OfflineArmCache cache;

   @Before
   public void setUp() {
      // attributes are registered globally by name, so give each test its own names
      cache = new OfflineArmCache(EPHEMERAL, "test:" + UUID.randomUUID() + ":cached");
   }

   @Test
   public void testNeverArmed() {
      Assert.assertNull(cache.build(keypad, null, Mode.ON, false));
      Assert.assertNull(cache.build(keypad, null, Mode.PARTIAL, false));
   }

   @Test
   public void testRoundTrip() {
      Set<String> devices = ImmutableSet.of("DRIV:dev:door", "DRIV:dev:motion");
      cache.cache(new ArmEvent(platform, null, Mode.ON, false, 30, 60, 2, false, true, devices));

      ArmEvent event = cache.build(keypad, null, Mode.ON, false);
      Assert.assertNotNull(event);
      Assert.assertEquals(Mode.ON, event.getMode());
      Assert.assertEquals(30, event.getEntranceDelaySecs());
      Assert.assertEquals(60, event.getExitDelaySecs());
      Assert.assertEquals(2, event.getAlarmSensitivityDeviceCount());
      Assert.assertFalse(event.isSilent());
      Assert.assertTrue(event.isSoundsEnabled());
      Assert.assertEquals(devices, event.getActiveDevices());
      Assert.assertEquals("offline arm is attributed to the keypad", keypad.getRepresentation(), event.getSource());
   }

   @Test
   public void testModesAreCachedSeparately() {
      cache.cache(new ArmEvent(platform, null, Mode.ON, false, 30, 60, 2, false, true, ImmutableSet.of("DRIV:dev:door", "DRIV:dev:motion")));
      Assert.assertNull("arming ON must not fill PARTIAL", cache.build(keypad, null, Mode.PARTIAL, false));

      cache.cache(new ArmEvent(platform, null, Mode.PARTIAL, false, 15, 20, 1, true, false, ImmutableSet.of("DRIV:dev:door")));

      ArmEvent partial = cache.build(keypad, null, Mode.PARTIAL, false);
      Assert.assertEquals(15, partial.getEntranceDelaySecs());
      Assert.assertEquals(20, partial.getExitDelaySecs());
      Assert.assertTrue(partial.isSilent());
      Assert.assertFalse(partial.isSoundsEnabled());
      Assert.assertEquals(ImmutableSet.of("DRIV:dev:door"), partial.getActiveDevices());

      Assert.assertEquals(60, cache.build(keypad, null, Mode.ON, false).getExitDelaySecs());
   }

   @Test
   public void testBypassIsPassedThrough() {
      cache.cache(new ArmEvent(platform, null, Mode.ON, false, 30, 30, 1, false, true, ImmutableSet.of("DRIV:dev:door")));

      Assert.assertFalse(cache.build(keypad, null, Mode.ON, false).isBypass());
      Assert.assertTrue(cache.build(keypad, null, Mode.ON, true).isBypass());
   }

   @Test
   public void testZeroSensitivityCachedAsOne() {
      cache.cache(new ArmEvent(platform, null, Mode.ON, false, 30, 30, 0, false, true, ImmutableSet.of("DRIV:dev:door")));

      Assert.assertEquals(1, cache.build(keypad, null, Mode.ON, false).getAlarmSensitivityDeviceCount());
   }

   @Test
   public void testLatestArmWins() {
      cache.cache(new ArmEvent(platform, null, Mode.ON, false, 30, 30, 1, false, true, ImmutableSet.of("DRIV:dev:door")));
      cache.cache(new ArmEvent(platform, null, Mode.ON, false, 45, 90, 1, false, true, ImmutableSet.of("DRIV:dev:door", "DRIV:dev:window")));

      ArmEvent event = cache.build(keypad, null, Mode.ON, false);
      Assert.assertEquals(45, event.getEntranceDelaySecs());
      Assert.assertEquals(90, event.getExitDelaySecs());
      Assert.assertEquals(ImmutableSet.of("DRIV:dev:door", "DRIV:dev:window"), event.getActiveDevices());
   }
}
