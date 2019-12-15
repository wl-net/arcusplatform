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
package com.arcussmarthome.common.subsystem.safety;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.arcussmarthome.capability.key.NamespacedKey;
import com.arcussmarthome.common.subsystem.SubsystemTestCase;
import com.arcussmarthome.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.arcussmarthome.common.subsystem.alarm.smoke.SmokeAlarm;
import com.arcussmarthome.messages.capability.AlarmCapability;
import com.arcussmarthome.messages.capability.AlarmSubsystemCapability;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.SafetySubsystemCapability;
import com.arcussmarthome.messages.capability.SubsystemCapability;
import com.arcussmarthome.messages.model.subs.AlarmSubsystemModel;
import com.arcussmarthome.messages.model.subs.SafetySubsystemModel;
import com.arcussmarthome.messages.model.test.ModelFixtures;

public class TestSafetySubsystemV2 extends SubsystemTestCase<SafetySubsystemModel> {
	protected AlarmSubsystemModel alarmSubsystem;
	protected SafetySubsystem subsystem;
	
	@Override
	protected SafetySubsystemModel createSubsystemModel() {
      Map<String, Object> attributes =
            ModelFixtures
               .buildSubsystemAttributes(placeId, SafetySubsystemCapability.NAMESPACE)
               .create();
      return new SafetySubsystemModel(addModel(attributes));
	}

	@Before
	public void createAlarmSubsystem() {
		Map<String, Object> attributes = 
			ModelFixtures
				.buildSubsystemAttributes(placeId, AlarmSubsystemCapability.NAMESPACE)
				.put(Capability.ATTR_INSTANCES, ImmutableMap.of(SmokeAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE), CarbonMonoxideAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE)))
				.put(SubsystemCapability.ATTR_STATE, SubsystemCapability.STATE_SUSPENDED)
				.put(AlarmSubsystemCapability.ATTR_ACTIVEALERTS, ImmutableSet.of())
				.put(NamespacedKey.representation(AlarmCapability.ATTR_ALERTSTATE, SmokeAlarm.NAME), AlarmCapability.ALERTSTATE_INACTIVE)
				.put(NamespacedKey.representation(AlarmCapability.ATTR_DEVICES, SmokeAlarm.NAME), ImmutableSet.of())
				.put(NamespacedKey.representation(AlarmCapability.ATTR_ALERTSTATE, CarbonMonoxideAlarm.NAME), AlarmCapability.ALERTSTATE_INACTIVE)
				.put(NamespacedKey.representation(AlarmCapability.ATTR_DEVICES, CarbonMonoxideAlarm.NAME), ImmutableSet.of())
				.create();
		alarmSubsystem = new AlarmSubsystemModel(addModel(attributes));
	}
	
	@Before
	public void createSafetySubsystem() throws Exception {
		subsystem = new SafetySubsystem();
		init(subsystem);
	}
	
	protected void upgrade() {
		alarmSubsystem.setState(AlarmSubsystemCapability.STATE_ACTIVE);
	}
	
	@Test
	public void testInitialState() {
		assertEquals("1.0", model.getVersion());
		assertFalse(model.getAvailable());
		assertEquals(ImmutableSet.of(), model.getTotalDevices());
	}
	
	@Test
	public void testUpgradeNoDevices() {
		upgrade();
		store.commit();
		
		assertEquals("2.0", model.getVersion());
		assertFalse(model.getAvailable());
		assertEquals(ImmutableSet.of(), model.getTotalDevices());
	}
}

