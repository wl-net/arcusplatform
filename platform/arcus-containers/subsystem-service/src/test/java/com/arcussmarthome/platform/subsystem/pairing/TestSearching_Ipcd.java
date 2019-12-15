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

import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.test.ModelFixtures;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.DismissAllResponse;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.FactoryResetResponse;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.ListHelpStepsResponse;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.SearchResponse;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.StopSearchingResponse;
import com.arcussmarthome.messages.errors.ErrorEventException;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.platform.subsystem.pairing.state.PairingSubsystemTestCase;

public class TestSearching_Ipcd extends PairingSubsystemTestCase {

	@Before
	public void stageFactoryReset() throws Exception {
		expectLoadProductAndReturn(productIpcd()).anyTimes();
		expectCurrentProductAndReturn(productIpcd()).anyTimes();

		replay();
		
		stageSearchingIpcd();
		assertSearchingCloudNotFound(productAddress);
		assertBridgePairingRequest(popRequest());
		assertNoRequests();
	}
	
	@Test
	public void testSearchWithNoForm() throws Exception {
		MessageBody response = search().get();
		assertError(SearchResponse.CODE_REQUEST_PARAM_INVALID, response);
		assertSearchingCloudNotFound(productAddress);
	}
	
	@Test
	public void testSearchWithForm() throws Exception {
		Map<String, String> form = ProductFixtures.ipcdForm();
		MessageBody response = search(productAddress, form).get();
		assertEquals(SearchResponse.NAME, response.getMessageType());
		assertEquals(SearchResponse.MODE_CLOUD, SearchResponse.getMode(response));
		
		assertBridgePairingRequest(popRequest());
		// FIXME verify timeouts were reset
		
		assertSearchingCloudNotFound(productAddress);
	}
	
	@Test
	public void testSearchSucceeds() throws Exception {
		Model hub = addModel(ModelFixtures.createHubAttributes());

		Map<String, String> form = ProductFixtures.ipcdForm();
		MessageBody response = search(productAddress, form).get();
		assertEquals(SearchResponse.NAME, response.getMessageType());
		assertEquals(SearchResponse.MODE_CLOUD, SearchResponse.getMode(response));
		
		SendAndExpect ipcdRequest = popRequest();
		assertBridgePairingRequest(ipcdRequest);
		assertSearchingCloudNotFound(productAddress);

		PlatformMessage message = PlatformMessage
				.respondTo(PlatformMessage.buildRequest(ipcdRequest.getMessage(), ipcdRequest.getRequestAddress(), hub.getAddress()).create())
				.withPayload(MessageBody.emptyMessage()).create();

		ipcdRequest.getAction().onResponse(context, message);
		assertSearching(productAddress, PairingSubsystemCapability.PAIRINGMODE_CLOUD, false, false, new String[0]);
	}
	
	@Test
	public void testSearchRetries() throws Exception {
		Model hub = addModel(ModelFixtures.createHubAttributes());
		Map<String, String> form = ProductFixtures.ipcdForm();
		MessageBody response = search(productAddress, form).get();
		assertEquals(SearchResponse.NAME, response.getMessageType());
		assertEquals(SearchResponse.MODE_CLOUD, SearchResponse.getMode(response));

		{
			SendAndExpect ipcdRequest = popRequest();
			assertBridgePairingRequest(ipcdRequest);
			assertSearchingCloudNotFound(productAddress);

			PlatformMessage message = PlatformMessage
					.respondTo(PlatformMessage.buildRequest(ipcdRequest.getMessage(), ipcdRequest.getRequestAddress(), hub.getAddress()).create())
					.withPayload(Errors.fromCode(Errors.CODE_NOT_FOUND, "testing")).create();

			ipcdRequest.getAction().onResponse(context, message);
			assertSearchingCloudNotFound(productAddress);
		}
		
		{
			sendTimeout(); // idle
			SendAndExpect ipcdRequest = popRequest();
			assertBridgePairingRequest(ipcdRequest);
			assertSearchingCloudIdle(productAddress);
			ipcdRequest.getAction().onError(context, new ErrorEventException(Errors.CODE_NOT_FOUND, "testing"));
			// the timeout doesn't match the assertion here
//			assertSearchingCloudIdle(productAddress);
		}
		
		{
			sendTimeout(); // retry timeout
			SendAndExpect ipcdRequest = popRequest();
			assertBridgePairingRequest(ipcdRequest);
			assertSearchingCloudIdle(productAddress);
			sendTimeout(); // searching timeout
			assertIdle();
			
			// verify that since we're idle this doesn't change the state or set a timeout
			ipcdRequest.getAction().onError(context, new ErrorEventException(Errors.CODE_NOT_FOUND, "testing"));
			assertIdle();
		}
		
	}
	
	@Test
	public void testSearchFails() throws Exception {
		Map<String, String> form = ProductFixtures.ipcdForm();
		MessageBody response = search(productAddress, form).get();
		assertEquals(SearchResponse.NAME, response.getMessageType());
		assertEquals(SearchResponse.MODE_CLOUD, SearchResponse.getMode(response));

		SendAndExpect ipcdRequest = popRequest();
		assertBridgePairingRequest(ipcdRequest);
		assertSearchingCloudNotFound(productAddress);
		ipcdRequest.getAction().onError(context, new RuntimeException("BOOM"));
		assertIdle();
	}
	
	@Test
	public void testSearchTimeoutRetries() throws Exception {
		Map<String, String> form = ProductFixtures.ipcdForm();
		MessageBody response = search(productAddress, form).get();
		assertEquals(SearchResponse.NAME, response.getMessageType());
		assertEquals(SearchResponse.MODE_CLOUD, SearchResponse.getMode(response));

		SendAndExpect ipcdRequest = popRequest();
		assertBridgePairingRequest(ipcdRequest);
		assertSearchingCloudNotFound(productAddress);
		ipcdRequest.getAction().onTimeout(context);
		assertSearchingCloudNotFound(productAddress);
		
		ipcdRequest = popRequest();
		assertBridgePairingRequest(ipcdRequest);
		sendTimeout();
		sendTimeout();
		assertIdle();
		
		ipcdRequest.getAction().onTimeout(context); // verify this doesn't retry since we timedout in the meantime
		assertEquals(ImmutableList.of(), sendAndExpectOperations);
		assertIdle();
	}

	@Test
	public void testListHelpSteps() {
		MessageBody response = listHelpSteps().get();
		assertEquals(ListHelpStepsResponse.NAME, response.getMessageType());
		assertSearchingCloudNotFound(productAddress);
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
		assertSearchingCloudIdle(productAddress);
		
		sendTimeout();
		assertNoRequests();
		assertIdle();
	}

}

