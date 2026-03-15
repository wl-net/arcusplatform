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

import java.util.ArrayList;
import java.util.List;

import com.iris.driver.DeviceDriverContext;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;

/**
 * Shared utility methods for ZWaveAssociation handlers.
 */
class ZWaveAssociationUtil {

   static final String ASSOC_VAR_PREFIX = "zwda.assoc.";
   static final String MAX_GROUPS_VAR = "zwda.maxGroups";

   /**
    * Extracts the Z-Wave node from the device driver context.
    */
   static ZWaveNode extractNode(DeviceDriverContext context) {
      try {
         DeviceProtocolAddress address = (DeviceProtocolAddress) context.getProtocolAddress();
         if (address == null) {
            throw new IllegalStateException("Protocol address is not configured, can't send Z-Wave message");
         }
         ProtocolDeviceId deviceId = address.getProtocolDeviceId();
         return new ZWaveNode(deviceId.getBytes()[0]);
      } catch (ClassCastException | ArrayIndexOutOfBoundsException e) {
         throw new IllegalStateException("Protocol address [" + context.getProtocolAddress()
               + "] is not a Z-Wave address, can't send message", e);
      }
   }

   /**
    * Builds a ZWaveCommand with the specified send variables.
    */
   static ZWaveCommand buildCommand(ZWaveCommandClass commandClass, String commandName, byte... sendVars) {
      ZWaveCommand template = commandClass.commandsByName.get(commandName);
      ZWaveCommand command = new ZWaveCommand();
      command.commandClass = commandClass.number;
      command.commandNumber = template.commandNumber;
      command.commandName = template.commandName;
      for (int i = 0; i < sendVars.length; i++) {
         String name = "send" + i;
         command.addSendVariable(name);
         command.setSend(name, sendVars[i]);
      }
      return command;
   }

   /**
    * Returns the max groups value from driver variables, or 0 if not yet known.
    */
   static int getMaxGroups(DeviceDriverContext context) {
      try {
         Object val = context.getVariable(MAX_GROUPS_VAR);
         if (val instanceof Number) {
            return ((Number) val).intValue();
         }
      } catch (Exception e) {
         // not set yet
      }
      return 0;
   }

   /**
    * Stores the max groups value in driver variables.
    */
   static void storeMaxGroups(DeviceDriverContext context, int maxGroups) {
      context.setVariable(MAX_GROUPS_VAR, maxGroups);
   }

   /**
    * Stores a group's node list as a comma-separated string in driver variables.
    */
   static void storeGroupNodes(DeviceDriverContext context, int group, List<Integer> nodes) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < nodes.size(); i++) {
         if (i > 0) sb.append(", ");
         sb.append(nodes.get(i));
      }
      context.setVariable(ASSOC_VAR_PREFIX + group, sb.toString());
   }

   /**
    * Reads a group's node list from driver variables.
    */
   static String getGroupNodes(DeviceDriverContext context, int group) {
      try {
         Object val = context.getVariable(ASSOC_VAR_PREFIX + group);
         if (val instanceof String) {
            return (String) val;
         }
      } catch (Exception e) {
         // not set
      }
      return null;
   }

   /**
    * Builds the JSON associations map from all stored group data.
    * Returns a string like {"2": [5, 7], "3": [5]}.
    */
   static String buildAssociationsJson(DeviceDriverContext context) {
      int maxGroups = getMaxGroups(context);
      StringBuilder sb = new StringBuilder("{");
      boolean first = true;
      for (int g = 1; g <= maxGroups; g++) {
         String nodes = getGroupNodes(context, g);
         if (nodes != null && !nodes.isEmpty()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(g).append("\": [").append(nodes).append("]");
         }
      }
      sb.append("}");
      return sb.toString();
   }

   /**
    * Extracts node IDs from Association Report recv bytes starting at the given offset.
    */
   static List<Integer> extractNodeIds(byte[] recvBytes, int offset) {
      List<Integer> nodes = new ArrayList<>();
      for (int i = offset; i < recvBytes.length; i++) {
         int nodeId = recvBytes[i] & 0xFF;
         if (nodeId > 0) {
            nodes.add(nodeId);
         }
      }
      return nodes;
   }

   private ZWaveAssociationUtil() {}
}
