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

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.DismissAllResponse;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.FactoryResetResponse;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.ListHelpStepsResponse;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.SearchResponse;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.StopSearchingResponse;
import com.arcussmarthome.messages.service.BridgeService.RegisterDeviceRequest;
import com.arcussmarthome.platform.subsystem.pairing.state.PairingSubsystemTestCase;

public class TestPairingSteps_Ipcd extends PairingSubsystemTestCase {

	@Before
	public void stagePairingSteps() throws Exception {
		expectLoadProductAndReturn(productIpcd()).anyTimes();
		expectCurrentProductAndReturn(productIpcd()).anyTimes();

		replay();
		
		stagePairingStepsCloud();
		assertPairingStepsCloud(productAddress);
		assertNoRequests();
	}
	
	@Test
	public void testSearchWithNoForm() throws Exception {
		MessageBody response = search().get();
		assertError(SearchResponse.CODE_REQUEST_PARAM_INVALID, response);
		assertPairingStepsCloud(productAddress);
	}
	
	@Test
	public void testSearchWithForm() throws Exception {
		Map<String, String> form = ProductFixtures.ipcdForm();
		MessageBody response = search(productAddress, form).get();
		assertEquals(SearchResponse.NAME, response.getMessageType());
		assertEquals(SearchResponse.MODE_CLOUD, SearchResponse.getMode(response));
		
		SendAndExpect pairingRequest = popRequest();
		assertEquals(Address.fromString("BRDG::IPCD"), pairingRequest.getRequestAddress());
		assertEquals(RegisterDeviceRequest.NAME, pairingRequest.getMessage().getMessageType());
		assertEquals(form, pairingRequest.getMessage().getAttributes().get(RegisterDeviceRequest.ATTR_ATTRS));
		
		assertSearchingCloudNotFound(productAddress);
	}
	
	@Test
	public void testListHelpSteps() {
		MessageBody response = listHelpSteps().get();
		assertEquals(ListHelpStepsResponse.NAME, response.getMessageType());
		assertPairingStepsCloud(productAddress);
	}

	@Test
	public void testDismissAll() {
		MessageBody response = dismissAll().get();
		assertEquals(DismissAllResponse.NAME, response.getMessageType());
		assertEquals(ImmutableList.of(), response.getAttributes().get(DismissAllResponse.ATTR_ACTIONS));
		assertNoRequests();
		assertIdle();
	}

	@Test
	public void testFactoryReset() {
		MessageBody response = factoryReset().get();
		assertEquals(FactoryResetResponse.NAME, response.getMessageType());
		assertNoRequests();
		assertFactoryResetIdle(productAddress);
	}

	@Test
	public void testStopSearching() {
		MessageBody response = stopSearching().get();
		assertEquals(StopSearchingResponse.instance(), response);
		assertNoRequests();
		assertIdle();
	}

	@Test
	public void testTimeout() {
		sendTimeout();
		assertNoRequests();
		assertIdle();
	}

}

