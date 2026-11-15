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
package com.iris.driver.groovy.zwave;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriverContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;

public class ZWaveGetReplyListenerTest {
   private static final ZWaveCommandClass WAKE_UP = ZWaveAllCommandClasses.getClass("wake_up");
   private static final ZWaveCommandClass BATTERY = ZWaveAllCommandClasses.getClass("battery");
   private static final long REQUESTED = 1_700_000_000_000L;

   private DeviceDriverContext context;
   private long now;
   private ZWaveGetReplyListener listener;

   @Before
   public void setUp() {
      context = createMock(DeviceDriverContext.class);
      now = REQUESTED + TimeUnit.HOURS.toMillis(12);
      listener = new ZWaveGetReplyListener(() -> now);
   }

   @Test
   public void recordsTheAnswer() throws Exception {
      Capture<Map<String, String>> recorded = expectRecorded(pending("PENDING"));

      assertFalse(listener.handleEvent(context, fromDevice(WAKE_UP, "report", 0x00, 0x07, 0x08, 0x01)));

      verify(context);
      Map<String, String> answer = recorded.getValue();
      assertEquals("REPLIED", answer.get("status"));
      assertEquals("Wake Up report", answer.get("reply"));
      assertEquals("seconds1=0, seconds2=7, seconds3=8, node=1", answer.get("values"));
      assertEquals("00 07 08 01", answer.get("bytes"));
      assertEquals(Instant.ofEpochMilli(now).toString(), answer.get("replied"));
      assertEquals("what was asked stays", "get", answer.get("command"));
   }

   @Test
   public void aWakeUpNotificationIsNotTheAnswer() throws Exception {
      expect(context.getAttributeValue(DeviceAdvancedCapability.KEY_ZWAVEGET)).andReturn(pending("PENDING"));
      replay(context);

      assertFalse(listener.handleEvent(context, fromDevice(WAKE_UP, "notification")));

      verify(context);
   }

   @Test
   public void aMessageInAnotherCommandClassIsNotTheAnswer() throws Exception {
      expect(context.getAttributeValue(DeviceAdvancedCapability.KEY_ZWAVEGET)).andReturn(pending("PENDING"));
      replay(context);

      listener.handleEvent(context, fromDevice(BATTERY, "report", 0x50));

      verify(context);
   }

   @Test
   public void onlyTheFirstAnswerIsRecorded() throws Exception {
      expect(context.getAttributeValue(DeviceAdvancedCapability.KEY_ZWAVEGET)).andReturn(pending("REPLIED"));
      replay(context);

      listener.handleEvent(context, fromDevice(WAKE_UP, "report", 0x00, 0x0E, 0x10, 0x01));

      verify(context);
   }

   @Test
   public void nothingHappensWithoutARequest() throws Exception {
      expect(context.getAttributeValue(DeviceAdvancedCapability.KEY_ZWAVEGET)).andReturn(null);
      replay(context);

      listener.handleEvent(context, fromDevice(WAKE_UP, "report", 0x00, 0x0E, 0x10, 0x01));

      verify(context);
   }

   @Test
   public void aRequestNotAnsweredIn48HoursExpires() throws Exception {
      now = REQUESTED + TimeUnit.HOURS.toMillis(49);
      Capture<Map<String, String>> recorded = expectRecorded(pending("PENDING"));

      listener.handleEvent(context, fromDevice(WAKE_UP, "report", 0x00, 0x0E, 0x10, 0x01));

      verify(context);
      assertEquals("EXPIRED", recorded.getValue().get("status"));
      assertEquals(null, recorded.getValue().get("reply"));
   }

   @Test
   public void otherProtocolsAreIgnored() throws Exception {
      replay(context);

      ProtocolMessage zwave = fromDevice(WAKE_UP, "report", 0x00, 0x0E, 0x10, 0x01);
      ProtocolMessage other = ProtocolMessage.builder()
            .from(zwave.getSource())
            .to(zwave.getDestination())
            .withPayload("CTRL", zwave.getBuffer())
            .create();
      assertFalse(listener.handleEvent(context, other));
      assertFalse(listener.handleEvent(context, "not a protocol message"));

      verify(context);
   }

   private Capture<Map<String, String>> expectRecorded(Map<String, String> pending) {
      Capture<Map<String, String>> recorded = Capture.newInstance();
      expect(context.getAttributeValue(DeviceAdvancedCapability.KEY_ZWAVEGET)).andReturn(pending);
      expect(context.setAttributeValue(eq(DeviceAdvancedCapability.KEY_ZWAVEGET), capture(recorded))).andReturn(null);
      expect(context.getLogger()).andReturn(LoggerFactory.getLogger(ZWaveGetReplyListenerTest.class)).anyTimes();
      replay(context);
      return recorded;
   }

   private static Map<String, String> pending(String status) {
      Map<String, String> pending = new LinkedHashMap<>();
      pending.put("commandClass", "wake_up");
      pending.put("command", "get");
      pending.put("status", status);
      pending.put("requested", Instant.ofEpochMilli(REQUESTED).toString());
      return pending;
   }

   private static ProtocolMessage fromDevice(ZWaveCommandClass commandClass, String commandName, int... payload) {
      byte[] bytes = new byte[payload.length];
      for (int i = 0; i < payload.length; i++) {
         bytes[i] = (byte) payload[i];
      }
      ZWaveCommand command = new ZWaveCommand(commandClass.number, commandClass.get(commandName).commandNumber, bytes);
      command.recvBytes = bytes;
      return ProtocolMessage.builder()
            .from(Address.protocolAddress("ZWAV", new byte[] { 0x05 }))
            .to(Address.broadcastAddress())
            .withPayload(ZWaveProtocol.INSTANCE, new ZWaveCommandMessage(new ZWaveNode((byte) 0x05), command))
            .create();
   }
}
