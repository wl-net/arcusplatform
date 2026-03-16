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

public class TestZWaveAssociationClearHandler {

   private static final ZWaveCommandClass ASSOCIATION_CC = ZWaveAllCommandClasses.getClass("association");

   private DeviceDriverContext context;
   private ZWaveAssociationClearHandler handler;

   @Before
   public void setUp() {
      handler = new ZWaveAssociationClearHandler();
      context = createNiceMock(DeviceDriverContext.class);
   }

   @Test
   public void testRejectsNullGroup() throws Exception {
      MessageBody request = ZWaveDirectAssociationCapability.ClearAssociationRequest.builder()
            .withGroup(null)
            .build();

      Capture<MessageBody> responseCapture = Capture.newInstance();
      context.respondToPlatform(capture(responseCapture));
      expectLastCall();
      replay(context);

      assertTrue(handler.handleEvent(context, request));
      assertEquals("INVALID_PARAM",
            ZWaveDirectAssociationCapability.ClearAssociationResponse.getStatus(responseCapture.getValue()));
      verify(context);
   }

   @Test
   public void testSendsRemoveWithGroupOnlyThenGet() throws Exception {
      MessageBody request = ZWaveDirectAssociationCapability.ClearAssociationRequest.builder()
            .withGroup(3)
            .build();

      Capture<ZWaveCommandMessage> removeCapture = Capture.newInstance();
      Capture<ZWaveCommandMessage> getCapture = Capture.newInstance();

      expect(context.getProtocolAddress())
            .andReturn(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .anyTimes();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(removeCapture), eq(-1));
      expectLastCall();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(getCapture), eq(-1));
      expectLastCall();

      Capture<MessageBody> responseCapture = Capture.newInstance();
      context.respondToPlatform(capture(responseCapture));
      expectLastCall();

      replay(context);

      assertTrue(handler.handleEvent(context, request));

      // Verify the Remove command has only 1 send var (group only, no node)
      ZWaveCommand removeCmd = removeCapture.getValue().getCommand();
      assertEquals(ASSOCIATION_CC.number, removeCmd.commandClass);
      assertEquals(1, removeCmd.sendVariables.size());
      byte[] removeBytes = removeCmd.toBytes();
      assertEquals(3, removeBytes.length);  // CC + CMD + group
      assertEquals(ASSOCIATION_CC.number, removeBytes[0]);
      assertEquals(ASSOCIATION_CC.commandsByName.get("remove").commandNumber, removeBytes[1]);
      assertEquals((byte) 3, removeBytes[2]);  // group

      // Verify the Association Get command bytes: [CC, CMD, group]
      ZWaveCommand getCmd = getCapture.getValue().getCommand();
      byte[] getBytes = getCmd.toBytes();
      assertEquals(3, getBytes.length);
      assertEquals((byte) 3, getBytes[2]);  // group

      // Verify response
      assertEquals("PENDING",
            ZWaveDirectAssociationCapability.ClearAssociationResponse.getStatus(responseCapture.getValue()));
      verify(context);
   }

   @Test
   public void testIgnoresNonPlatformMessage() throws Exception {
      replay(context);
      assertFalse(handler.handleEvent(context, "not a platform message"));
      verify(context);
   }
}
