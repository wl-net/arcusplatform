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

import org.junit.Before;
import org.junit.Test;

import com.iris.device.attributes.AttributeKey;
import com.iris.driver.DeviceDriverContext;
import com.iris.messages.capability.ZWaveDirectAssociationCapability;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;

public class TestZWaveAssociationReportHandler {

   private static final ZWaveCommandClass ASSOCIATION_CC = ZWaveAllCommandClasses.getClass("association");
   private static final ZWaveCommand ASSOCIATION_REPORT_CMD = ASSOCIATION_CC.commandsByName.get("report");
   private static final ZWaveCommand GROUPINGS_REPORT_CMD = ASSOCIATION_CC.commandsByName.get("groupings_report");

   private DeviceDriverContext context;
   private ZWaveAssociationReportHandler handler;

   @Before
   public void setUp() {
      handler = new ZWaveAssociationReportHandler();
      context = createNiceMock(DeviceDriverContext.class);
   }

   @Test
   public void testIgnoresNonProtocolMessage() throws Exception {
      replay(context);
      assertFalse(handler.handleEvent(context, "not a protocol message"));
      verify(context);
   }

   @Test
   public void testIgnoresNonAssociationCommand() throws Exception {
      // Build a Switch Binary Report (different command class)
      ZWaveCommandClass switchCC = ZWaveAllCommandClasses.getClass("switch_binary");
      ZWaveCommand switchReport = switchCC.commandsByName.get("report");

      ZWaveCommand command = new ZWaveCommand(switchReport);
      command.commandClass = switchCC.number;

      ZWaveCommandMessage zwaveMessage = new ZWaveCommandMessage();
      zwaveMessage.setDevice(new ZWaveNode((byte) 0x02));
      zwaveMessage.setCommand(command);

      ProtocolMessage protocolMessage = ProtocolMessage.builder()
            .from(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .to(com.iris.messages.address.Address.broadcastAddress())
            .withPayload(ZWaveProtocol.INSTANCE, zwaveMessage)
            .create();

      replay(context);
      assertFalse(handler.handleEvent(context, protocolMessage));
      verify(context);
   }

   @Test
   public void testProcessesAssociationReport() throws Exception {
      // Association Report: group=2, maxNodes=10, reportsToFollow=0, node5, node7
      byte[] recvBytes = new byte[]{2, 10, 0, 5, 7};
      ProtocolMessage protocolMessage = buildAssociationReportMessage(recvBytes);

      context.setVariable("zwda.assoc.2", "5, 7");
      expectLastCall();
      expect(context.setAttributeValue(anyObject(AttributeKey.class), anyObject()))
            .andReturn(null).anyTimes();
      replay(context);

      boolean result = handler.handleEvent(context, protocolMessage);
      assertFalse(result);
      verify(context);
   }

   @Test
   public void testProcessesAssociationReportEmptyGroup() throws Exception {
      // Association Report: group=3, maxNodes=10, reportsToFollow=0, no nodes
      byte[] recvBytes = new byte[]{3, 10, 0};
      ProtocolMessage protocolMessage = buildAssociationReportMessage(recvBytes);

      context.setVariable("zwda.assoc.3", "");
      expectLastCall();
      expect(context.setAttributeValue(anyObject(AttributeKey.class), anyObject()))
            .andReturn(null).anyTimes();
      replay(context);

      boolean result = handler.handleEvent(context, protocolMessage);
      assertFalse(result);
      verify(context);
   }

   @Test
   public void testProcessesGroupingsReport() throws Exception {
      // Groupings Report: supportedGroupings=9
      byte[] recvBytes = new byte[]{9};
      ProtocolMessage protocolMessage = buildGroupingsReportMessage(recvBytes);

      context.setVariable("zwda.maxGroups", 9);
      expectLastCall();
      expect(context.setAttributeValue(anyObject(AttributeKey.class), anyObject()))
            .andReturn(null).anyTimes();
      replay(context);

      boolean result = handler.handleEvent(context, protocolMessage);
      assertFalse(result);
      verify(context);
   }

   @Test
   public void testIgnoresShortAssociationReport() throws Exception {
      // recvBytes too short (length < 3)
      byte[] recvBytes = new byte[]{2, 10};
      ProtocolMessage protocolMessage = buildAssociationReportMessage(recvBytes);

      replay(context);
      boolean result = handler.handleEvent(context, protocolMessage);
      assertFalse(result);
      verify(context);
   }

   /**
    * Builds a ProtocolMessage containing an Association Report.
    * Uses the 3-arg ZWaveCommand constructor so the payload bytes survive
    * the serialize/deserialize round-trip through ProtocolMessage.
    */
   private ProtocolMessage buildAssociationReportMessage(byte[] payload) {
      ZWaveCommand command = new ZWaveCommand(ASSOCIATION_CC.number, ASSOCIATION_REPORT_CMD.commandNumber, payload);
      command.commandClass = ASSOCIATION_CC.number;
      command.recvBytes = payload;

      ZWaveCommandMessage zwaveMessage = new ZWaveCommandMessage();
      zwaveMessage.setDevice(new ZWaveNode((byte) 0x02));
      zwaveMessage.setCommand(command);

      return ProtocolMessage.builder()
            .from(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .to(com.iris.messages.address.Address.broadcastAddress())
            .withPayload(ZWaveProtocol.INSTANCE, zwaveMessage)
            .create();
   }

   /**
    * Builds a ProtocolMessage containing a Groupings Report.
    */
   private ProtocolMessage buildGroupingsReportMessage(byte[] payload) {
      ZWaveCommand command = new ZWaveCommand(ASSOCIATION_CC.number, GROUPINGS_REPORT_CMD.commandNumber, payload);
      command.commandClass = ASSOCIATION_CC.number;
      command.recvBytes = payload;

      ZWaveCommandMessage zwaveMessage = new ZWaveCommandMessage();
      zwaveMessage.setDevice(new ZWaveNode((byte) 0x02));
      zwaveMessage.setCommand(command);

      return ProtocolMessage.builder()
            .from(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .to(com.iris.messages.address.Address.broadcastAddress())
            .withPayload(ZWaveProtocol.INSTANCE, zwaveMessage)
            .create();
   }
}
