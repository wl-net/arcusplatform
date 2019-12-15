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
package com.arcussmarthome.common.subsystem.alarm;

import java.util.List;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.arcussmarthome.common.subsystem.alarm.generic.AlarmState.TriggerEvent;
import com.arcussmarthome.common.subsystem.alarm.security.SecurityErrors;
import com.arcussmarthome.messages.ErrorEvent;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AlarmCapability;
import com.arcussmarthome.messages.capability.AlarmIncidentCapability;
import com.arcussmarthome.messages.capability.AlarmSubsystemCapability;
import com.arcussmarthome.messages.capability.SmokeCapability;
import com.arcussmarthome.messages.event.MessageReceivedEvent;
import com.arcussmarthome.messages.model.serv.AlarmModel;
import com.arcussmarthome.messages.type.IncidentTrigger;

public class TestAlarmSubsystem_InactiveRequests extends PlatformAlarmSubsystemTestCase {
	UUID incidentId = UUID.randomUUID();

	
	@Before
	public void startSubsystem() throws Exception {
		init(subsystem);
	}
	
	@Test
	public void testArm() {
		MessageReceivedEvent request = armRequest(AlarmSubsystemCapability.SECURITYMODE_ON);
		replay();
		
		subsystem.onEvent(request, context);
		MessageBody error = responses.getValue();
		assertEquals(SecurityErrors.CODE_ARM_INVALID, error.getAttributes().get(ErrorEvent.CODE_ATTR));
		
		assertInactive();
		
		verify();
	}

	@Test
	public void testArmBypassed() {
		MessageReceivedEvent request = armBypassedRequest(AlarmSubsystemCapability.SECURITYMODE_ON);
		replay();
		
		subsystem.onEvent(request, context);
		MessageBody error = responses.getValue();
		assertEquals(SecurityErrors.CODE_ARM_INVALID, error.getAttributes().get(ErrorEvent.CODE_ATTR));
		
		assertInactive();
		
		verify();
	}

	@Test
	public void testDisarm() {
		MessageReceivedEvent request = disarmRequest();
		replay();
		
		subsystem.onEvent(request, context);
		MessageBody empty = responses.getValue();
		assertEquals(MessageBody.emptyMessage(), empty);
		
		assertInactive();
		
		verify();
	}

	/**
	 * The only alarm that can go from inactive to alert.
	 */
	@Test
	public void testPanic() {
		MessageReceivedEvent request = panicRequest();
		
		Capture<List<IncidentTrigger>> triggerCapture = EasyMock.newCapture();
		EasyMock
			.expect(
					incidentService.addAlert(EasyMock.eq(context), EasyMock.eq(AlarmSubsystemCapability.ACTIVEALERTS_PANIC), EasyMock.capture(triggerCapture))
			)
			.andReturn(Address.platformService(UUID.randomUUID(), AlarmIncidentCapability.NAMESPACE));

		Capture<List<IncidentTrigger>> updateCapture = EasyMock.newCapture();
		incidentService.updateIncident(EasyMock.eq(context), EasyMock.capture(triggerCapture));
		EasyMock.expectLastCall();

		replay();
		
		MessageBody empty = sendRequest(request);
		assertEquals(MessageBody.emptyMessage(), empty);
		
		assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(AlarmSubsystemCapability.ACTIVEALERTS_PANIC, model));
		assertAlerting(AlarmSubsystemCapability.ACTIVEALERTS_PANIC);
		
		List<IncidentTrigger> triggers = triggerCapture.getValue();
		assertEquals(1, triggers.size());
		IncidentTrigger trigger = triggers.get(0);
		assertEquals(TriggerEvent.VERIFIED_ALARM.name(), trigger.getEvent());
		
		verify();
	}
	
}

