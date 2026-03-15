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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.capability.ZWaveDirectAssociationCapability;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

/**
 * Protocol handler for Z-Wave Association Report (0x85/0x03) and
 * Groupings Report (0x85/0x06). Updates zwda:associations and
 * zwda:maxGroups attributes.
 *
 * Returns false so existing driver onZWaveMessage.association.report
 * handlers can also process the message.
 */
public class ZWaveAssociationReportHandler implements ContextualEventHandler<Object> {
   private static final Logger log = LoggerFactory.getLogger(ZWaveAssociationReportHandler.class);

   private static final ZWaveCommandClass ASSOCIATION_CC = ZWaveAllCommandClasses.getClass("association");
   private static final ZWaveCommand ASSOCIATION_REPORT_CMD = ASSOCIATION_CC.commandsByName.get("report");
   private static final ZWaveCommand GROUPINGS_REPORT_CMD = ASSOCIATION_CC.commandsByName.get("groupings_report");

   @Override
   @SuppressWarnings("unchecked")
   public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
      if (!(event instanceof com.iris.protocol.ProtocolMessage)) {
         return false;
      }

      com.iris.protocol.ProtocolMessage protocolMessage = (com.iris.protocol.ProtocolMessage) event;
      com.iris.protocol.zwave.message.ZWaveMessage zwaveMessage;
      try {
         zwaveMessage = protocolMessage.getValue(ZWaveProtocol.INSTANCE);
      } catch (Exception e) {
         log.debug("Failed to decode Z-Wave message", e);
         return false;
      }

      if (!(zwaveMessage instanceof ZWaveCommandMessage)) {
         return false;
      }

      ZWaveCommand command = ((ZWaveCommandMessage) zwaveMessage).getCommand();
      if (command == null || command.commandClass != ASSOCIATION_CC.number) {
         return false;
      }

      if (command.commandNumber == ASSOCIATION_REPORT_CMD.commandNumber) {
         return processAssociationReport(context, command);
      }

      if (command.commandNumber == GROUPINGS_REPORT_CMD.commandNumber) {
         return processGroupingsReport(context, command);
      }

      return false;
   }

   /**
    * Association Report format (recvBytes):
    *   [0] grouping identifier
    *   [1] max nodes supported
    *   [2] reports to follow
    *   [3..] node ID list
    */
   private boolean processAssociationReport(DeviceDriverContext context, ZWaveCommand command) {
      byte[] recvBytes = command.recvBytes;
      if (recvBytes == null || recvBytes.length < 3) {
         log.trace("Association Report too short, ignoring");
         return false;
      }

      int group = recvBytes[0] & 0xFF;
      int maxNodes = recvBytes[1] & 0xFF;
      int reportsToFollow = recvBytes[2] & 0xFF;
      List<Integer> nodes = ZWaveAssociationUtil.extractNodeIds(recvBytes, 3);

      log.debug("Association Report: group={}, maxNodes={}, reportsToFollow={}, nodes={}",
            group, maxNodes, reportsToFollow, nodes);

      ZWaveAssociationUtil.storeGroupNodes(context, group, nodes);

      // Update the zwda:associations attribute
      context.setAttributeValue(
            (com.iris.device.attributes.AttributeKey) ZWaveDirectAssociationCapability.KEY_ASSOCIATIONS,
            ZWaveAssociationUtil.buildAssociationsJson(context));

      // Return false to allow driver-specific handlers to also process
      return false;
   }

   /**
    * Groupings Report format (recvBytes):
    *   [0] supported groupings
    */
   private boolean processGroupingsReport(DeviceDriverContext context, ZWaveCommand command) {
      byte[] recvBytes = command.recvBytes;
      if (recvBytes == null || recvBytes.length < 1) {
         log.trace("Groupings Report too short, ignoring");
         return false;
      }

      int supportedGroupings = recvBytes[0] & 0xFF;

      log.debug("Groupings Report: supportedGroupings={}", supportedGroupings);

      ZWaveAssociationUtil.storeMaxGroups(context, supportedGroupings);

      // Update the zwda:maxGroups attribute
      context.setAttributeValue(
            (com.iris.device.attributes.AttributeKey) ZWaveDirectAssociationCapability.KEY_MAXGROUPS,
            supportedGroupings);

      // Return false to allow driver-specific handlers to also process
      return false;
   }
}
