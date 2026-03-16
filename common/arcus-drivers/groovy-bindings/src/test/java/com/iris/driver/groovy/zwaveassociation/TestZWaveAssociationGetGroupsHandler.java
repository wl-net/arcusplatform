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

public class TestZWaveAssociationGetGroupsHandler {

   private static final ZWaveCommandClass ASSOCIATION_CC = ZWaveAllCommandClasses.getClass("association");

   private DeviceDriverContext context;
   private ZWaveAssociationGetGroupsHandler handler;

   @Before
   public void setUp() {
      handler = new ZWaveAssociationGetGroupsHandler();
      context = createNiceMock(DeviceDriverContext.class);
   }

   @Test
   public void testSendsGroupingsGet() throws Exception {
      MessageBody request = ZWaveDirectAssociationCapability.GetSupportedGroupsRequest.instance();

      Capture<ZWaveCommandMessage> capture = Capture.newInstance();

      expect(context.getProtocolAddress())
            .andReturn(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .anyTimes();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(capture), eq(-1));
      expectLastCall();

      Capture<MessageBody> responseCapture = Capture.newInstance();
      context.respondToPlatform(capture(responseCapture));
      expectLastCall();

      replay(context);

      assertTrue(handler.handleEvent(context, request));

      // Verify the Groupings Get command bytes: [CC, CMD] — 2 bytes, no payload
      ZWaveCommand cmd = capture.getValue().getCommand();
      assertEquals(ASSOCIATION_CC.number, cmd.commandClass);
      byte[] bytes = cmd.toBytes();
      assertEquals(2, bytes.length);
      assertEquals(ASSOCIATION_CC.number, bytes[0]);
      assertEquals(ASSOCIATION_CC.commandsByName.get("groupings_get").commandNumber, bytes[1]);

      // Verify response
      assertEquals("PENDING",
            ZWaveDirectAssociationCapability.GetSupportedGroupsResponse.getStatus(responseCapture.getValue()));
      verify(context);
   }

   @Test
   public void testIgnoresNonPlatformMessage() throws Exception {
      replay(context);
      assertFalse(handler.handleEvent(context, "not a platform message"));
      verify(context);
   }
}
