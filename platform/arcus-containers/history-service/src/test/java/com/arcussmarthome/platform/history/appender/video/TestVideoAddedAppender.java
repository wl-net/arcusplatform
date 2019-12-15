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
/**
 * 
 */
package com.arcussmarthome.platform.history.appender.video;

import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AlarmIncidentCapability;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.PersonCapability;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.capability.RecordingCapability;
import com.arcussmarthome.messages.capability.RuleCapability;
import com.arcussmarthome.messages.capability.SceneCapability;
import com.arcussmarthome.messages.model.test.ModelFixtures;
import com.arcussmarthome.platform.history.HistoryAppenderDAO;
import com.arcussmarthome.platform.history.HistoryLogEntry;
import com.arcussmarthome.platform.history.HistoryLogEntryType;
import com.arcussmarthome.platform.history.appender.EventAppenderTestCase;
import com.arcussmarthome.platform.history.appender.ObjectNameCache;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.util.IrisUUID;

@Mocks({ HistoryAppenderDAO.class, ObjectNameCache.class})
public class TestVideoAddedAppender extends EventAppenderTestCase {
	UUID placeId = UUID.randomUUID();
	UUID cameraId = UUID.randomUUID();
	Address cameraAddress = Address.platformDriverAddress(cameraId);
	String cameraName = "Test Cam";
	Address incidentAddress = Address.platformService(UUID.randomUUID(), AlarmIncidentCapability.NAMESPACE);
	Address videoAddress = Address.platformService(IrisUUID.timeUUID(), RecordingCapability.NAMESPACE);

	@Inject VideoAddedAppender appender;
	
	protected PlatformMessage videoAdded(@Nullable Address actor) {
		Map<String, Object> attributes =
				ModelFixtures
					.buildRecordingAttributes((UUID) videoAddress.getId())
					.put(RecordingCapability.ATTR_PLACEID, placeId.toString())
					.put(RecordingCapability.ATTR_CAMERAID, cameraId.toString())
					.create();
		MessageBody added = MessageBody.buildMessage(Capability.EVENT_ADDED, attributes);
		return 
				PlatformMessage
					.buildBroadcast(added, Address.fromString((String) attributes.get(Capability.ATTR_ADDRESS)))
					.withActor(actor)
					.withPlaceId(placeId)
					.create();
	}
	
	protected void assertHistoryEventMatches(HistoryLogEntry value, HistoryLogEntryType type, Object id) {
		assertEquals(type, value.getType());
		assertEquals(id, value.getId());
		assertEquals("alarm.security.recording", value.getMessageKey());
		assertEquals(videoAddress.getRepresentation(), value.getSubjectAddress());
		assertEquals(cameraName, value.getValues().get(0));
	}
	
	@Before
	public void stagePlaceLookup() {
		EasyMock
			.expect(mockNameCache.getPlaceName(Address.platformService(placeId, PlaceCapability.NAMESPACE)))
			.andReturn("My House")
			.anyTimes();
	}
	
	@After
	public void verify() {
		super.verify();
	}
	
	@Test
	public void testIncidentVideoAdded() {
		expectNameLookup(cameraAddress, cameraName);
		Capture<HistoryLogEntry> dashboard = expectAndCaptureAppend();
		Capture<HistoryLogEntry> details = expectAndCaptureAppend();
		Capture<HistoryLogEntry> incident = expectAndCaptureAppend();
		Capture<HistoryLogEntry> device = expectAndCaptureAppend();
		replay();
		
		PlatformMessage message = videoAdded(incidentAddress);
		assertTrue(appender.append(message));
		assertHistoryEventMatches(dashboard.getValue(), HistoryLogEntryType.CRITICAL_PLACE_LOG, placeId);
		assertHistoryEventMatches(details.getValue(),   HistoryLogEntryType.DETAILED_PLACE_LOG, placeId);
		assertHistoryEventMatches(incident.getValue(),  HistoryLogEntryType.DETAILED_ALARM_LOG, incidentAddress.getId());
		assertHistoryEventMatches(device.getValue(),    HistoryLogEntryType.DETAILED_DEVICE_LOG, cameraAddress.getId());
	}

	@Test
	public void testManualRecordingAdded() {
		replay();
		
		PlatformMessage message = videoAdded(Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE));
		assertFalse(appender.append(message));
	}

	@Test
	public void testRuleRecordingAdded() {
		replay();
		
		PlatformMessage message = videoAdded(Address.platformService(UUID.randomUUID(), RuleCapability.NAMESPACE, 1));
		assertFalse(appender.append(message));
	}

	@Test
	public void testSceneRecordingAdded() {
		replay();
		
		PlatformMessage message = videoAdded(Address.platformService(UUID.randomUUID(), SceneCapability.NAMESPACE, 2));
		assertFalse(appender.append(message));
	}

	@Test
	public void testNoAttributionRecordingAdded() {
		replay();
		
		PlatformMessage message = videoAdded(null);
		assertFalse(appender.append(message));
	}

}

