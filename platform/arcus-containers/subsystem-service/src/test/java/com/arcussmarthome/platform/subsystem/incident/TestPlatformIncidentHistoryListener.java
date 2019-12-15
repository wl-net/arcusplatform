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
package com.arcussmarthome.platform.subsystem.incident;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.arcussmarthome.common.alarm.AlertType;
import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.core.messaging.memory.InMemoryPlatformMessageBus;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AlarmIncidentCapability;
import com.arcussmarthome.messages.capability.AlarmIncidentCapability.HistoryAddedEvent;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.CarbonMonoxideCapability;
import com.arcussmarthome.messages.capability.ContactCapability;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.capability.GlassCapability;
import com.arcussmarthome.messages.capability.KeyPadCapability;
import com.arcussmarthome.messages.capability.LeakH2OCapability;
import com.arcussmarthome.messages.capability.MotionCapability;
import com.arcussmarthome.messages.capability.RuleCapability;
import com.arcussmarthome.messages.capability.SmokeCapability;
import com.arcussmarthome.messages.capability.TiltCapability;
import com.arcussmarthome.messages.model.ChildId;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.SimpleModelStore;
import com.arcussmarthome.messages.model.test.ModelFixtures;
import com.arcussmarthome.messages.type.HistoryLog;
import com.arcussmarthome.messages.type.IncidentTrigger;
import com.arcussmarthome.messages.type.Population;
import com.arcussmarthome.platform.alarm.incident.Trigger;
import com.arcussmarthome.platform.rule.RuleDao;
import com.arcussmarthome.platform.rule.RuleDefinition;
import com.arcussmarthome.platform.rule.StatefulRuleDefinition;
import com.arcussmarthome.population.PlacePopulationCacheManager;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;
import com.arcussmarthome.util.IrisUUID;

@Mocks({RuleDao.class, PlacePopulationCacheManager.class})
@Modules(InMemoryMessageModule.class)
public class TestPlatformIncidentHistoryListener extends IrisMockTestCase {
	@Inject
	private SubsystemContext<?> context;
	@Inject
	private InMemoryPlatformMessageBus platformBus;
	@Inject
	private PlatformIncidentHistoryListener uut;
	@Inject RuleDao ruleDao;
	@Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;
	
	private UUID placeId = UUID.randomUUID();
	private Address incidentAddress = Address.platformService(IrisUUID.randomUUID(), AlarmIncidentCapability.NAMESPACE);
	private SimpleModelStore store;
	
	@Provides
	public SubsystemContext<?> setupContext() {
		store = new SimpleModelStore();
		context = EasyMock.createMock(SubsystemContext.class);
		EasyMock
			.expect(context.getPlaceId())
			.andReturn(placeId)
			.anyTimes();
		EasyMock
			.expect(context.models())
			.andReturn(store)
			.anyTimes();
		EasyMock
		.expect(context.getPopulation())
		.andReturn(Population.NAME_GENERAL)
		.anyTimes();
		EasyMock.replay(context);		
		return context;
	}
	
	@Before
	public void setupMocks() {
   	EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }	
	
	private void testDeviceTrigger(
			Set<String> sensorType,
			AlertType alarm,
			Trigger.Event event,
			String messageKey
	) throws Exception {
		Map<String, Object> attributes =
				ModelFixtures
					.buildDeviceAttributes(sensorType.toArray(new String[] {}))
					.put(DeviceCapability.ATTR_NAME, "Test Device")
					.create();
				
		testTrigger(
				attributes,
				alarm,
				event,
				messageKey
		);
	}
	
	private void testTrigger(
			Map<String, Object> sourceAttributes,
			AlertType alarm,
			Trigger.Event event,
			String messageKey
	) throws Exception {
		Model source = store.addModel(sourceAttributes);
		String expectedValue = null;
		if(source.supports(DeviceCapability.NAMESPACE)) {
			expectedValue = (String) source.getAttribute(DeviceCapability.ATTR_NAME);
		}
		else if(source.supports(RuleCapability.NAMESPACE)) {
			expectedValue = (String) "the rule " + source.getAttribute(RuleCapability.ATTR_NAME);
		}
		else {
			fail("Unrecognized trigger of type " + source.getType());
		} 
		
		testTrigger(source.getAddress(), expectedValue, alarm, event, messageKey);
	}
	
	private void testTrigger(
			Address sourceAddress,
			String expectedValue,
			AlertType alarm,
			Trigger.Event event,
			String messageKey
	) throws Exception {		
		IncidentTrigger trigger = new IncidentTrigger();
		trigger.setSource(sourceAddress.getRepresentation());
		trigger.setAlarm(alarm.name());
		trigger.setEvent(event.name());
		trigger.setTime(new Date());
		uut.onTriggersAdded(context, incidentAddress, ImmutableList.of(trigger));
		
		PlatformMessage message = platformBus.take();
		assertEquals(placeId.toString(), message.getPlaceId());
		assertEquals(incidentAddress, message.getSource());
		assertEquals(Address.broadcastAddress(), message.getDestination());
		assertEquals(AlarmIncidentCapability.HistoryAddedEvent.NAME, message.getMessageType());
		
		List<Map<String, Object>> events = HistoryAddedEvent.getEvents(message.getValue());
		assertEquals(1, events.size());
		
		Map<String, Object> e = events.get(0);
		assertEquals(messageKey, e.get(HistoryLog.ATTR_KEY));
		assertEquals(sourceAddress.getRepresentation(), e.get(HistoryLog.ATTR_SUBJECTADDRESS));
		assertEquals(ImmutableList.of(expectedValue), e.get("values"));		
	}
	
   @Test
   public void testTriggerTilt() throws Exception {
      testDeviceTrigger(
            ImmutableSet.of(TiltCapability.NAMESPACE),
            AlertType.SECURITY,
            Trigger.Event.CONTACT,
            "alarm.contact"
      );
   }

   @Test
   public void testTriggerContact() throws Exception {
      testDeviceTrigger(
            ImmutableSet.of(ContactCapability.NAMESPACE),
            AlertType.SECURITY,
            Trigger.Event.CONTACT,
            "alarm.contact"
      );
   }

   @Test
   public void testTriggerMotion() throws Exception {
      testDeviceTrigger(
            ImmutableSet.of(MotionCapability.NAMESPACE),
            AlertType.SECURITY,
            Trigger.Event.MOTION,
            "alarm.motion"
      );
   }

   @Test
   public void testTriggerGlass() throws Exception {
      testDeviceTrigger(
            ImmutableSet.of(GlassCapability.NAMESPACE),
            AlertType.SECURITY,
            Trigger.Event.GLASS,
            "alarm.glass"
      );
   }

   @Test
   public void testTriggerSmoke() throws Exception {
      testDeviceTrigger(
            ImmutableSet.of(SmokeCapability.NAMESPACE),
            AlertType.SMOKE,
            Trigger.Event.SMOKE,
            "alarm.smoke"
      );
   }

   @Test
   public void testTriggerCO() throws Exception {
      testDeviceTrigger(
            ImmutableSet.of(CarbonMonoxideCapability.NAMESPACE),
            AlertType.CO,
            Trigger.Event.CO,
            "alarm.co"
      );
   }

   @Test
   public void testTriggerLeak() throws Exception {
      testDeviceTrigger(
            ImmutableSet.of(LeakH2OCapability.NAMESPACE),
            AlertType.WATER,
            Trigger.Event.LEAK,
            "alarm.leak"
      );
   }

   @Test
   public void testPanicRule() throws Exception {
	   RuleDefinition ruleDef = new StatefulRuleDefinition();
	   int sequenceId = 1;
	   String ruleName = "testRule";
	   ruleDef.setId(new ChildId(placeId, sequenceId));
	   ruleDef.setName(ruleName);
	   ruleDef.setCreated(new Date());
	   ruleDef.setModified(new Date());
	   Address ruleAddress = Address.platformService(placeId, RuleCapability.NAMESPACE, sequenceId);
	   ruleDef.setVariables(ImmutableMap.<String, Object>of(Capability.ATTR_ADDRESS, ruleAddress));
	   EasyMock.expect(ruleDao.findById(placeId, sequenceId)).andReturn(ruleDef);
	   replay();
      testTrigger(
    		  ruleAddress,
    		  "the rule "+ruleName,
            AlertType.PANIC,
            Trigger.Event.RULE,
            "alarm.panic"
      );
   }

   @Test
   public void testPanicDevice() throws Exception {
      testDeviceTrigger(
            ImmutableSet.of(KeyPadCapability.NAMESPACE),
            AlertType.PANIC,
            Trigger.Event.KEYPAD,
            "alarm.panic"
      );
   }

}

