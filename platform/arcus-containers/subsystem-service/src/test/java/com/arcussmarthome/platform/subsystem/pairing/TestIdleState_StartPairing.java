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
package com.arcussmarthome.platform.subsystem.pairing;

import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.arcussmarthome.messages.ErrorEvent;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.capability.HubCapability;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.StartPairingResponse;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.test.ModelFixtures;
import com.arcussmarthome.platform.subsystem.pairing.state.PairingSubsystemTestCase;

public class TestIdleState_StartPairing extends PairingSubsystemTestCase {

	@Before
	public void stage() throws Exception {
		// the subsystem will set itself to idle on added
		start();
		assertIdle();
	}
	
	@Test
	public void testStartPairingCloudDevice() {
		expectLoadProductAndReturn(productIpcd()).times(2);
		replay();
		
		MessageBody response = startPairing().get();
		assertEquals(StartPairingResponse.NAME, response.getMessageType());
		assertEquals(StartPairingResponse.MODE_CLOUD, StartPairingResponse.getMode(response));
		// FIXME validate form
		// FIXME validate steps
		
		assertPairingStepsCloud(productAddress);
	}

	@Test
	public void testStartPairingOauthDevice() {
		expectLoadProductAndReturn(productOauth()).times(2);
		replay();
		
		MessageBody response = startPairing().get();
		assertEquals(StartPairingResponse.NAME, response.getMessageType());
		assertEquals(StartPairingResponse.MODE_OAUTH, StartPairingResponse.getMode(response));
		// FIXME validate oauth url
		// FIXME validate steps
		
		assertPairingStepsOAuth(productAddress);
	}

	@Test
	public void testStartPairingHubDeviceNoHub() {
		expectLoadProductAndReturn(productZigbee());
		replay();
		
		MessageBody response = startPairing().get();
		assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
		assertEquals(PairingSubsystemCapability.HubMissingException.CODE_HUB_MISSING, ((ErrorEvent) response).getCode());
		
		assertIdle();
	}

	@Test
	public void testStartPairingHubDeviceWithHubOffline() {
		Map<String, Object> attibutes = ModelFixtures.createHubAttributes();
		attibutes.put(HubCapability.ATTR_STATE, HubCapability.STATE_DOWN);
		Model hub = addModel(attibutes);
		expectLoadProductAndReturn(productZigbee());
		replay();
		
		// send the request

		MessageBody response = startPairing().get();
		assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
		assertEquals(PairingSubsystemCapability.HubOfflineException.CODE_HUB_OFFLINE, ((ErrorEvent) response).getCode());
		
		assertIdle();

		
	}
	
	@Test
	public void testStartPairingHubDeviceWithHub() {
		Model hub = addModel(ModelFixtures.createHubAttributes());
		expectLoadProductAndReturn(productZigbee()).times(2);
		replay();
		
		// send the request
		{
			Optional<MessageBody> response = startPairing();
			// no response until the hub responds
			assertEquals(Optional.empty(), response);
			assertIdlePendingPairingSteps();
		}
		// complete the action
		{
			SendAndExpect request = popRequest();
			assertStartPairingRequestSent(hub.getAddress(), request);
			request.getAction().onResponse(context, buildStartPairingResponse(hub.getAddress()));
			MessageBody response = responses.getValues().get(responses.getValues().size() - 1);
			assertEquals(StartPairingResponse.NAME, response.getMessageType());
			assertEquals(StartPairingResponse.MODE_HUB, StartPairingResponse.getMode(response));
			assertPairingStepsHub(productAddress);
		}
	}
	
}

