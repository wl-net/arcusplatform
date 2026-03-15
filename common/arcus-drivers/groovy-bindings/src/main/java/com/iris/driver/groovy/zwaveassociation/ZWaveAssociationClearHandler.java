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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.ZWaveDirectAssociationCapability;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

/**
 * Handles zwda:ClearAssociation requests.
 * Sends Association Remove (0x85/0x04) with group only (no node IDs)
 * to clear all nodes from the group per Z-Wave spec.
 * Then sends Association Get for readback.
 */
public class ZWaveAssociationClearHandler implements ContextualEventHandler<Object> {
   private static final Logger log = LoggerFactory.getLogger(ZWaveAssociationClearHandler.class);

   private static final ZWaveCommandClass ASSOCIATION_CC = ZWaveAllCommandClasses.getClass("association");

   @Override
   public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
      MessageBody request;
      if (event instanceof PlatformMessage) {
         request = ((PlatformMessage) event).getValue();
      } else if (event instanceof MessageBody) {
         request = (MessageBody) event;
      } else {
         return false;
      }

      Integer group = ZWaveDirectAssociationCapability.ClearAssociationRequest.getGroup(request);
      if (group == null) {
         context.respondToPlatform(ZWaveDirectAssociationCapability.ClearAssociationResponse.builder()
               .withStatus("INVALID_PARAM")
               .build());
         return true;
      }

      log.debug("ClearAssociation group={}", group);

      // Send Association Remove with group only — clears all nodes per Z-Wave spec
      ZWaveCommand cmd = ZWaveAssociationUtil.buildCommand(ASSOCIATION_CC, "remove",
            (byte) (group & 0xFF));
      ZWaveCommandMessage msg = new ZWaveCommandMessage();
      msg.setDevice(ZWaveAssociationUtil.extractNode(context));
      msg.setCommand(cmd);
      context.sendToDevice(ZWaveProtocol.INSTANCE, msg, -1);

      // Send Association Get for readback
      ZWaveCommand getCmd = ZWaveAssociationUtil.buildCommand(ASSOCIATION_CC, "get",
            (byte) (group & 0xFF));
      ZWaveCommandMessage getMsg = new ZWaveCommandMessage();
      getMsg.setDevice(ZWaveAssociationUtil.extractNode(context));
      getMsg.setCommand(getCmd);
      context.sendToDevice(ZWaveProtocol.INSTANCE, getMsg, -1);

      context.respondToPlatform(ZWaveDirectAssociationCapability.ClearAssociationResponse.builder()
            .withStatus("PENDING")
            .build());
      return true;
   }
}
