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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.driver.DeviceDriverContext;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceAdvancedCapability.SendZWaveGetRequest;
import com.iris.messages.capability.DeviceAdvancedCapability.SendZWaveGetResponse;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

public class ZWaveGetHandlerTest {
   private static final long NOW = 1_700_000_000_000L;
   private static final Address ZWAVE_DEVICE = Address.protocolAddress("ZWAV", new byte[] { 0x05 });

   private DeviceDriverContext context;
   private ZWaveGetHandler handler;

   @Before
   public void setUp() {
      context = createMock(DeviceDriverContext.class);
      handler = new ZWaveGetHandler(() -> NOW);
   }

   @Test
   public void sendsTheGetAndRecordsItAsPending() throws Exception {
      Capture<ZWaveCommandMessage> sent = Capture.newInstance();
      Capture<Map<String, String>> pending = Capture.newInstance();
      Capture<MessageBody> response = Capture.newInstance();
      expectZWave();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(sent), eq(-1));
      expectLastCall();
      expect(context.setAttributeValue(eq(DeviceAdvancedCapability.KEY_ZWAVEGET), capture(pending))).andReturn(null);
      context.respondToPlatform(capture(response));
      expectLastCall();
      replay(context);

      assertTrue(handler.handleEvent(context, request("wake_up", "interval_capabilities_get", null)));

      verify(context);
      ZWaveCommandClass wakeUp = ZWaveAllCommandClasses.getClass("wake_up");
      assertEquals(wakeUp.number, sent.getValue().getCommand().commandClass);
      assertEquals(wakeUp.get("interval_capabilities_get").commandNumber, sent.getValue().getCommand().commandNumber);
      assertEquals(ImmutableMap.of(
            "commandClass", "wake_up",
            "command", "interval_capabilities_get",
            "status", "PENDING",
            "requested", "2023-11-14T22:13:20Z"), pending.getValue());
      assertEquals(SendZWaveGetResponse.STATUS_SENT, SendZWaveGetResponse.getStatus(response.getValue()));
      assertEquals("Wake Up interval_capabilities_report", SendZWaveGetResponse.getExpectedReply(response.getValue()));
   }

   @Test
   public void sendsTheArgumentsNamed() throws Exception {
      Capture<ZWaveCommandMessage> sent = Capture.newInstance();
      Capture<Map<String, String>> pending = Capture.newInstance();
      expectZWave();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(sent), eq(-1));
      expectLastCall();
      expect(context.setAttributeValue(eq(DeviceAdvancedCapability.KEY_ZWAVEGET), capture(pending))).andReturn(null);
      context.respondToPlatform(anyObject(MessageBody.class));
      expectLastCall();
      replay(context);

      handler.handleEvent(context, request("configuration", "get", Collections.singletonMap("param", 3)));

      verify(context);
      assertEquals(3, sent.getValue().getCommand().getSend("param"));
      assertEquals("param=3", pending.getValue().get("args"));
   }

   @Test
   public void refusesACommandThatIsNotAGet() throws Exception {
      assertRefused(request("wake_up", "set", null), ErrorEvent.MESSAGE_TYPE);
   }

   @Test
   public void refusesSecurityGets() throws Exception {
      assertRefused(request("security", "nonce_get", null), ErrorEvent.MESSAGE_TYPE);
   }

   @Test
   public void refusesTheFirmwareUpdateGetThatStartsAnUpdate() throws Exception {
      assertRefused(request("firmware_update_md", "request_get", null), ErrorEvent.MESSAGE_TYPE);
   }

   @Test
   public void refusesAUserCodeGetWhoseAnswerIsACode() throws Exception {
      assertRefused(request("user_code", "get", null), ErrorEvent.MESSAGE_TYPE);
   }

   @Test
   public void refusesAnArgumentThatDoesNotFitInAByte() throws Exception {
      assertRefused(request("configuration", "get", Collections.singletonMap("param", 256)), ErrorEvent.MESSAGE_TYPE);
   }

   @Test
   public void refusesAnUnknownCommandClass() throws Exception {
      assertRefused(request("no_such_class", "get", null), ErrorEvent.MESSAGE_TYPE);
   }

   @Test
   public void refusesAnUnknownCommand() throws Exception {
      assertRefused(request("wake_up", "no_such_get", null), ErrorEvent.MESSAGE_TYPE);
   }

   @Test
   public void refusesAnArgumentTheGetDoesNotSend() throws Exception {
      assertRefused(request("battery", "get", Collections.singletonMap("level", 1)), ErrorEvent.MESSAGE_TYPE);
   }

   @Test
   public void refusesADeviceThatIsNotZWave() throws Exception {
      Capture<MessageBody> response = Capture.newInstance();
      expect(context.getProtocolAddress()).andReturn(Address.protocolAddress("ZIGB", new byte[] { 0x05 })).anyTimes();
      context.respondToPlatform(capture(response));
      expectLastCall();
      replay(context);

      assertTrue(handler.handleEvent(context, request("wake_up", "get", null)));

      verify(context);
      assertEquals(ErrorEvent.MESSAGE_TYPE, response.getValue().getMessageType());
   }

   @Test
   public void expectsTheReportTheCommandModelOrTheNameGives() {
      ZWaveCommandClass basic = ZWaveAllCommandClasses.getClass("basic");
      ZWaveCommandClass wakeUp = ZWaveAllCommandClasses.getClass("wake_up");
      ZWaveCommandClass version = ZWaveAllCommandClasses.getClass("version");

      assertEquals("Basic report", ZWaveGetHandler.describe(basic, ZWaveGetHandler.expectedReplies(basic, basic.get("get"))));
      assertEquals("Wake Up report", ZWaveGetHandler.describe(wakeUp, ZWaveGetHandler.expectedReplies(wakeUp, wakeUp.get("get"))));
      assertEquals("Version class report", ZWaveGetHandler.describe(version, ZWaveGetHandler.expectedReplies(version, version.get("class get"))));
   }

   /** Nothing is sent and nothing recorded, only an error answered */
   private void assertRefused(MessageBody request, String expectedType) throws Exception {
      Capture<MessageBody> response = Capture.newInstance();
      expectZWave();
      context.respondToPlatform(capture(response));
      expectLastCall();
      replay(context);

      assertTrue(handler.handleEvent(context, request));

      verify(context);
      assertEquals(expectedType, response.getValue().getMessageType());
   }

   private void expectZWave() {
      expect(context.getProtocolAddress()).andReturn(ZWAVE_DEVICE).anyTimes();
      expect(context.getLogger()).andReturn(LoggerFactory.getLogger(ZWaveGetHandlerTest.class)).anyTimes();
   }

   private static MessageBody request(String commandClass, String command, Map<String, Integer> args) {
      SendZWaveGetRequest.Builder builder = SendZWaveGetRequest.builder().withCommandClass(commandClass).withCommand(command);
      if (args != null) {
         builder.withArgs(args);
      }
      return builder.build();
   }
}
