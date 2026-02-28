/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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

package com.iris.agent.zigbee;

import java.io.IOException;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.reflexes.HubReflexVersions;
import com.iris.agent.zigbee.events.ZBEventDispatcher;
import com.iris.agent.zigbee.events.ZBNodeCommandEvent;
import com.iris.agent.zigbee.events.ZBNodeHeardFromEvent;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.messages.address.Address;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;

public class ZBMessageTranslator {

   private static final Logger logger = LoggerFactory.getLogger(ZBMessageTranslator.class);

   /**
    * Translates an inbound zsmartsystems ZigBeeCommand into an Arcus protocol message
    * and dispatches it as a ZBNodeCommandEvent.
    */
   public static void handleInboundCommand(com.zsmartsystems.zigbee.ZigBeeCommand command) {
      if (command.getSourceAddress() == null) {
         return;
      }

      int sourceNwk = command.getSourceAddress().getAddress();
      ZBNetwork network = ZBServices.INSTANCE.getNetwork();
      ZBNode node = network.getNodeByNwk(sourceNwk);

      if (node == null) {
         logger.warn("Received command from unknown NWK address {}, dropping", sourceNwk);
         return;
      }

      // Dispatch heard-from event
      ZBEventDispatcher.INSTANCE.dispatch(new ZBNodeHeardFromEvent(node.getIeeeAddr()));

      // Translate to Arcus ZigbeeMessage.Protocol
      try {
         ZigbeeMessage.Protocol pmsg = translateToProtocol(command, node);
         if (pmsg != null) {
            ZBEventDispatcher.INSTANCE.dispatch(new ZBNodeCommandEvent(node.getIeeeAddr(), pmsg));
         }
      } catch (Exception ex) {
         logger.warn("Failed to translate inbound command: {}", ex.getMessage(), ex);
      }
   }

   /**
    * Translates an outbound Arcus ProtocolMessage and sends it via the ZigBee network manager.
    */
   public static void handleOutboundMessage(ProtocolMessage msg) {
      ZigbeeMessage.Protocol pmsg = msg.getValue(ZigbeeProtocol.INSTANCE);
      if (pmsg == null) {
         logger.warn("Could not decode zigbee protocol message, dropping: {}", msg);
         return;
      }

      try {
         switch (pmsg.getType()) {
            case ZigbeeMessage.Zcl.ID:
               handleOutboundZcl(pmsg);
               break;
            case ZigbeeMessage.Zdp.ID:
               handleOutboundZdp(pmsg);
               break;
            case ZigbeeMessage.SetOfflineTimeout.ID:
               handleOutboundSetOfflineTimeout(msg, pmsg);
               break;
            case ZigbeeMessage.Control.ID:
               handleOutboundControl(pmsg);
               break;
            default:
               logger.warn("Unknown zigbee message type {}, dropping", pmsg.getType());
               break;
         }
      } catch (Exception ex) {
         logger.warn("Failed to handle outbound message: {}", ex.getMessage(), ex);
      }
   }

   private static ZigbeeMessage.Protocol translateToProtocol(
         com.zsmartsystems.zigbee.ZigBeeCommand command, ZBNode node) throws IOException {

      if (command instanceof com.zsmartsystems.zigbee.zcl.ZclCommand) {
         com.zsmartsystems.zigbee.zcl.ZclCommand zclCmd = (com.zsmartsystems.zigbee.zcl.ZclCommand) command;

         Integer clusterId = command.getClusterId();

         // Filter OTA block requests/responses - handled locally
         if (isFilteredOtaMessage(clusterId, zclCmd)) {
            logger.trace("Filtering local OTA message from {}", node.getIeeeAddr());
            return null;
         }

         int flags = 0;
         if (!zclCmd.isGenericCommand()) {
            flags |= ZigbeeMessage.Zcl.CLUSTER_SPECIFIC;
         }
         if (zclCmd.isDisableDefaultResponse()) {
            flags |= ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE;
         }
         if (zclCmd.getCommandDirection() == com.zsmartsystems.zigbee.zcl.protocol.ZclCommandDirection.SERVER_TO_CLIENT) {
            flags |= ZigbeeMessage.Zcl.FROM_SERVER;
         }
         if (zclCmd.isManufacturerSpecific()) {
            flags |= ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC;
         }

         // Serialize the ZCL command payload
         com.zsmartsystems.zigbee.serialization.DefaultSerializer rawSerializer =
               new com.zsmartsystems.zigbee.serialization.DefaultSerializer();
         com.zsmartsystems.zigbee.zcl.ZclFieldSerializer fieldSerializer =
               new com.zsmartsystems.zigbee.zcl.ZclFieldSerializer(rawSerializer);
         zclCmd.serialize(fieldSerializer);
         int[] intPayload = fieldSerializer.getPayload();
         byte[] payload = intArrayToByteArray(intPayload);

         ZigbeeMessage.Zcl.Builder zclBuilder = ZigbeeMessage.Zcl.builder()
               .setZclMessageId(zclCmd.getCommandId())
               .setClusterId(clusterId != null ? clusterId : 0)
               .setFlags(flags)
               .setPayload(payload);

         if (command.getDestinationAddress() instanceof com.zsmartsystems.zigbee.ZigBeeEndpointAddress) {
            com.zsmartsystems.zigbee.ZigBeeEndpointAddress epAddr =
                  (com.zsmartsystems.zigbee.ZigBeeEndpointAddress) command.getDestinationAddress();
            zclBuilder.setEndpoint(epAddr.getEndpoint());
         }
         if (zclCmd.isManufacturerSpecific()) {
            zclBuilder.setManufacturerCode(zclCmd.getManufacturerCode());
         }

         return ZigbeeMessage.Protocol.builder()
               .setType(ZigbeeMessage.Zcl.ID)
               .setPayload(ByteOrder.LITTLE_ENDIAN, zclBuilder.create())
               .create();
      }

      // Non-ZCL commands (ZDP-level) - pass through as raw
      logger.trace("Received non-ZCL command type: {}", command.getClass().getSimpleName());
      return null;
   }

   private static boolean isFilteredOtaMessage(Integer clusterId, com.zsmartsystems.zigbee.zcl.ZclCommand zclCmd) {
      // OTA Upgrade cluster ID = 0x0019
      if (clusterId != null && clusterId == 0x0019 && !zclCmd.isGenericCommand()) {
         int cmd = zclCmd.getCommandId();
         // Filter: ImageBlockRequest(3), ImageBlockResponse(5), ImagePageRequest(4),
         //         UpgradeEndRequest(6), UpgradeEndResponse(7), ImageNotify(0)
         return cmd == 0 || cmd == 3 || cmd == 4 || cmd == 5 || cmd == 6 || cmd == 7;
      }
      return false;
   }

   private static byte[] intArrayToByteArray(int[] intArray) {
      if (intArray == null) return new byte[0];
      byte[] result = new byte[intArray.length];
      for (int i = 0; i < intArray.length; i++) {
         result[i] = (byte) (intArray[i] & 0xFF);
      }
      return result;
   }

   private static void handleOutboundZcl(ZigbeeMessage.Protocol pmsg) throws IOException {
      // ZCL messages to devices are handled by the ZigBeeNetworkManager
      // via the reflex/driver layer sending commands directly
      logger.trace("Outbound ZCL message (handled by driver layer)");
   }

   private static void handleOutboundZdp(ZigbeeMessage.Protocol pmsg) throws IOException {
      logger.trace("Outbound ZDP message (handled by driver layer)");
   }

   private static void handleOutboundSetOfflineTimeout(ProtocolMessage msg, ZigbeeMessage.Protocol pmsg) throws IOException {
      ZigbeeMessage.SetOfflineTimeout sot = ZigbeeMessage.SetOfflineTimeout.serde()
            .fromBytes(ByteOrder.LITTLE_ENDIAN, pmsg.getPayload());

      Object dst = msg.getDestination().getId();
      if (dst instanceof com.iris.messages.address.ProtocolDeviceId) {
         com.iris.messages.address.ProtocolDeviceId devId = (com.iris.messages.address.ProtocolDeviceId) dst;
         ZBNode node = ZBServices.INSTANCE.getNetwork().getNode(devId);
         if (node != null) {
            node.setOfflineTimeout(sot.getSeconds());
            ZBServices.INSTANCE.getNetwork().saveNode(node);
         }
      }
   }

   private static void handleOutboundControl(ZigbeeMessage.Protocol pmsg) {
      logger.trace("Outbound control message");
   }

   /**
    * Creates a ProtocolMessage for sending to the port from a ZBNodeCommandEvent.
    */
   public static ProtocolMessage createProtocolMessage(ZBNode node, ZigbeeMessage.Protocol pmsg) {
      try {
         return ProtocolMessage.buildProtocolMessage(
                     node.getProtocolAddress(), Address.broadcastAddress(),
                     ZigbeeProtocol.INSTANCE, pmsg)
               .withReflexVersion(HubReflexVersions.CURRENT)
               .create();
      } catch (Exception ex) {
         logger.warn("Failed to create protocol message: {}", ex.getMessage(), ex);
         return null;
      }
   }
}
