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

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceAdvancedCapability.SendZWaveGetRequest;
import com.iris.messages.capability.DeviceAdvancedCapability.SendZWaveGetResponse;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;

/**
 * devadv:SendZWaveGet through a loaded driver: the Get goes to the device, and the answer
 * is recorded in devadv:zwaveGet while the driver's own handler still sees it.
 */
public class ZWaveGetTest extends GroovyDriverTestCase {
   private static final ZWaveCommandClass WAKE_UP = ZWaveAllCommandClasses.getClass("wake_up");

   @Inject
   private InMemoryPlatformMessageBus platformBus;
   @Inject
   private InMemoryProtocolMessageBus protocolBus;

   private DeviceDriver driver;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load("ZWaveMessageHandlerObjects.driver");
   }

   @Test
   public void theGetGoesToTheDeviceAndItsAnswerIsRecorded() throws Exception {
      DeviceDriverContext context = context("ZWAV");

      driver.handlePlatformMessage(request(context, "wake_up", "get"), context);

      ZWaveCommand sent = ((ZWaveCommandMessage) protocolBus.take().getValue(ZWaveProtocol.INSTANCE)).getCommand();
      assertEquals(WAKE_UP.number, sent.commandClass);
      assertEquals(WAKE_UP.get("get").commandNumber, sent.commandNumber);
      MessageBody response = platformBus.take().getValue();
      assertEquals(SendZWaveGetResponse.NAME, response.getMessageType());
      assertEquals(SendZWaveGetResponse.STATUS_SENT, SendZWaveGetResponse.getStatus(response));
      assertEquals("Wake Up report", SendZWaveGetResponse.getExpectedReply(response));
      assertEquals("PENDING", zwaveGet(context).get("status"));

      // Wake Up Notification is in the same command class but doesn't answer the Get
      driver.handleProtocolMessage(fromDevice("notification"), context);
      assertEquals("PENDING", zwaveGet(context).get("status"));

      // 12 hours (43200 s = 0x00A8C0) for node 1
      driver.handleProtocolMessage(fromDevice("report", 0x00, 0xA8, 0xC0, 0x01), context);

      Map<String, String> answer = zwaveGet(context);
      assertEquals("REPLIED", answer.get("status"));
      assertEquals("Wake Up report", answer.get("reply"));
      assertEquals("seconds1=0, seconds2=168, seconds3=192, node=1", answer.get("values"));
      assertEquals("00 A8 C0 01", answer.get("bytes"));
      assertNotNull(answer.get("replied"));
      assertEquals("the driver's own handler still runs", "protocol", context.getVariable("match"));
   }

   @Test
   public void aDeviceThatIsNotZWaveIsToldSo() throws Exception {
      DeviceDriverContext context = context("ZIGB");

      driver.handlePlatformMessage(request(context, "wake_up", "get"), context);

      MessageBody response = platformBus.take().getValue();
      assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
      assertNull(protocolBus.poll());
      assertNull(context.getAttributeValue(DeviceAdvancedCapability.KEY_ZWAVEGET));
   }

   @Test
   public void aCommandThatIsNotAGetIsRefused() throws Exception {
      DeviceDriverContext context = context("ZWAV");

      driver.handlePlatformMessage(request(context, "wake_up", "set"), context);

      MessageBody response = platformBus.take().getValue();
      assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
      assertNull(protocolBus.poll());
   }

   private DeviceDriverContext context(String protocol) {
      Device device = createDevice(driver);
      device.setProtocolAddress(Address.protocolAddress(protocol, new byte[] { 0x05 }).getRepresentation());
      return new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
   }

   private PlatformMessage request(DeviceDriverContext context, String commandClass, String command) {
      PlatformMessage message = PlatformMessage.builder()
            .from(Fixtures.createClientAddress())
            .to(context.getDriverAddress())
            .withPayload(SendZWaveGetRequest.builder().withCommandClass(commandClass).withCommand(command).build())
            .isRequestMessage(true)
            .create();
      ((PlatformDeviceDriverContext) context).setMessageContext(message);
      return message;
   }

   private ProtocolMessage fromDevice(String commandName, int... payload) {
      ZWaveCommand template = WAKE_UP.get(commandName);
      byte[] bytes = new byte[payload.length];
      for (int i = 0; i < payload.length; i++) {
         bytes[i] = (byte) payload[i];
      }
      ZWaveCommand command = new ZWaveCommand(WAKE_UP.number, template.commandNumber, bytes);
      command.recvBytes = bytes;
      return ProtocolMessage.builder()
            .from(Address.protocolAddress("ZWAV", new byte[] { 0x05 }))
            .to(Address.broadcastAddress())
            .withPayload(ZWaveProtocol.INSTANCE, new ZWaveCommandMessage(new ZWaveNode((byte) 0x05), command))
            .create();
   }

   private static Map<String, String> zwaveGet(DeviceDriverContext context) {
      return context.getAttributeValue(DeviceAdvancedCapability.KEY_ZWAVEGET);
   }
}
