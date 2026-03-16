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
package com.iris.driver.groovy.zwaveassociation;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import com.iris.driver.DeviceDriverContext;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.ZWaveDirectAssociationCapability;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

public class TestZWaveAssociationSetHandler {

   private static final ZWaveCommandClass ASSOCIATION_CC = ZWaveAllCommandClasses.getClass("association");

   private DeviceDriverContext context;
   private ZWaveAssociationSetHandler handler;

   @Before
   public void setUp() {
      handler = new ZWaveAssociationSetHandler();
      context = createNiceMock(DeviceDriverContext.class);
   }

   @Test
   public void testRejectsNullGroup() throws Exception {
      MessageBody request = ZWaveDirectAssociationCapability.SetAssociationRequest.builder()
            .withGroup(null)
            .withNode(5)
            .build();

      Capture<MessageBody> responseCapture = Capture.newInstance();
      context.respondToPlatform(capture(responseCapture));
      expectLastCall();
      replay(context);

      assertTrue(handler.handleEvent(context, request));
      assertEquals("INVALID_PARAM",
            ZWaveDirectAssociationCapability.SetAssociationResponse.getStatus(responseCapture.getValue()));
      verify(context);
   }

   @Test
   public void testRejectsNullNode() throws Exception {
      MessageBody request = ZWaveDirectAssociationCapability.SetAssociationRequest.builder()
            .withGroup(2)
            .withNode(null)
            .build();

      Capture<MessageBody> responseCapture = Capture.newInstance();
      context.respondToPlatform(capture(responseCapture));
      expectLastCall();
      replay(context);

      assertTrue(handler.handleEvent(context, request));
      assertEquals("INVALID_PARAM",
            ZWaveDirectAssociationCapability.SetAssociationResponse.getStatus(responseCapture.getValue()));
      verify(context);
   }

   @Test
   public void testSendsAssociationSetAndGet() throws Exception {
      MessageBody request = ZWaveDirectAssociationCapability.SetAssociationRequest.builder()
            .withGroup(2)
            .withNode(5)
            .build();

      // Capture the two Z-Wave messages sent (set + get)
      Capture<ZWaveCommandMessage> setCapture = Capture.newInstance();
      Capture<ZWaveCommandMessage> getCapture = Capture.newInstance();

      expect(context.getProtocolAddress())
            .andReturn(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .anyTimes();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(setCapture), eq(-1));
      expectLastCall();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(getCapture), eq(-1));
      expectLastCall();

      Capture<MessageBody> responseCapture = Capture.newInstance();
      context.respondToPlatform(capture(responseCapture));
      expectLastCall();

      replay(context);

      assertTrue(handler.handleEvent(context, request));

      // Verify the Association Set command bytes: [CC, CMD, group, node]
      ZWaveCommand setCmd = setCapture.getValue().getCommand();
      assertEquals(ASSOCIATION_CC.number, setCmd.commandClass);
      byte[] setBytes = setCmd.toBytes();
      assertEquals(4, setBytes.length);
      assertEquals(ASSOCIATION_CC.number, setBytes[0]);
      assertEquals(ASSOCIATION_CC.commandsByName.get("set").commandNumber, setBytes[1]);
      assertEquals((byte) 2, setBytes[2]);  // group
      assertEquals((byte) 5, setBytes[3]);  // node

      // Verify the Association Get command bytes: [CC, CMD, group]
      ZWaveCommand getCmd = getCapture.getValue().getCommand();
      assertEquals(ASSOCIATION_CC.number, getCmd.commandClass);
      byte[] getBytes = getCmd.toBytes();
      assertEquals(3, getBytes.length);
      assertEquals(ASSOCIATION_CC.number, getBytes[0]);
      assertEquals(ASSOCIATION_CC.commandsByName.get("get").commandNumber, getBytes[1]);
      assertEquals((byte) 2, getBytes[2]);  // group

      // Verify response
      assertEquals("PENDING",
            ZWaveDirectAssociationCapability.SetAssociationResponse.getStatus(responseCapture.getValue()));
      verify(context);
   }

   @Test
   public void testIgnoresNonPlatformMessage() throws Exception {
      replay(context);
      assertFalse(handler.handleEvent(context, "not a platform message"));
      verify(context);
   }
}
