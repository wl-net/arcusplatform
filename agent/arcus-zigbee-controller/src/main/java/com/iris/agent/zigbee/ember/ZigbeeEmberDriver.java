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
package com.iris.agent.zigbee.ember;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.hal.IrisHal;
import com.zsmartsystems.zigbee.ZigBeeChannel;
import com.zsmartsystems.zigbee.ZigBeeCommand;
import com.zsmartsystems.zigbee.ZigBeeNetworkManager;
import com.zsmartsystems.zigbee.ZigBeeNetworkNodeListener;
import com.zsmartsystems.zigbee.ZigBeeNode;
import com.zsmartsystems.zigbee.ZigBeeStatus;
import com.zsmartsystems.zigbee.dongle.ember.ZigBeeDongleEzsp;
import com.zsmartsystems.zigbee.serialization.DefaultDeserializer;
import com.zsmartsystems.zigbee.serialization.DefaultSerializer;
import com.zsmartsystems.zigbee.transport.ZigBeePort;
import com.zsmartsystems.zigbee.ZigBeeCommandListener;
import com.zsmartsystems.zigbee.ZigBeeAnnounceListener;

public class ZigbeeEmberDriver implements ZigbeeDriver {
   private static final Logger logger = LoggerFactory.getLogger(ZigbeeEmberDriver.class);

   private final ZigBeeDongleEzsp dongle;
   private ZigBeeNetworkManager networkManager;
   private ZBNetworkCallbacks callbacks;

   public ZigbeeEmberDriver(ZigBeePort port) {
      this.dongle = new ZigBeeDongleEzsp(port);
   }

   @Override
   public void initialize(ZBNetworkCallbacks callbacks) {
      this.callbacks = callbacks;

      logger.info("Resetting ZigBee chip...");
      IrisHal.resetZigbeeChip();

      networkManager = new ZigBeeNetworkManager(dongle);
      networkManager.setSerializer(DefaultSerializer.class, DefaultDeserializer.class);

      ZigBeeStatus initStatus = networkManager.initialize();
      if (initStatus != ZigBeeStatus.SUCCESS) {
         logger.error("Failed to initialize ZigBee network manager: {}", initStatus);
         return;
      }

      // Register listeners to bridge zsmartsystems events to our callback interface
      networkManager.addNetworkNodeListener(new ZigBeeNetworkNodeListener() {
         @Override
         public void nodeAdded(ZigBeeNode node) {
            logger.info("ZigBee node added: {}", node.getIeeeAddress());
            if (callbacks != null) {
               callbacks.onNodeAdded(node);
            }
         }

         @Override
         public void nodeUpdated(ZigBeeNode node) {
            logger.debug("ZigBee node updated: {}", node.getIeeeAddress());
            if (callbacks != null) {
               callbacks.onNodeUpdated(node);
            }
         }

         @Override
         public void nodeRemoved(ZigBeeNode node) {
            logger.info("ZigBee node removed: {}", node.getIeeeAddress());
            if (callbacks != null) {
               callbacks.onNodeRemoved(node);
            }
         }
      });

      networkManager.addCommandListener(new ZigBeeCommandListener() {
         @Override
         public void commandReceived(ZigBeeCommand command) {
            logger.trace("ZigBee command received: {}", command);
            if (callbacks != null) {
               callbacks.onCommandReceived(command);
            }
         }
      });

      networkManager.addAnnounceListener(new ZigBeeAnnounceListener() {
         @Override
         public void deviceStatusUpdate(com.zsmartsystems.zigbee.ZigBeeNodeStatus deviceStatus, Integer networkAddress, com.zsmartsystems.zigbee.IeeeAddress ieeeAddress) {
            logger.info("ZigBee device announce: nwk={}, ieee={}", networkAddress, ieeeAddress);
            if (callbacks != null) {
               callbacks.onAnnounce(networkAddress, ieeeToLong(ieeeAddress));
            }
         }
      });

      ZigBeeStatus startStatus = networkManager.startup(false);
      if (startStatus != ZigBeeStatus.SUCCESS) {
         logger.error("Failed to start ZigBee network manager: {}", startStatus);
         return;
      }

      logger.info("ZigBee network manager started successfully");
   }

   @Override
   public void shutdown() {
      if (networkManager != null) {
         networkManager.shutdown();
         networkManager = null;
      }
   }

   @Override
   public void permitJoin(int durationInSeconds) {
      if (networkManager != null) {
         networkManager.permitJoin(durationInSeconds);
      }
   }

   @Override
   public void denyJoin() {
      if (networkManager != null) {
         networkManager.permitJoin(0);
      }
   }

   @Override
   public void leave(long ieeeAddr) {
      if (networkManager != null) {
         com.zsmartsystems.zigbee.IeeeAddress addr = new com.zsmartsystems.zigbee.IeeeAddress(
               String.format("%016X", ieeeAddr));
         ZigBeeNode node = networkManager.getNode(addr);
         if (node != null) {
            networkManager.leave(node.getNetworkAddress(), node.getIeeeAddress());
         } else {
            logger.warn("Cannot send leave to unknown node: {}", Long.toHexString(ieeeAddr));
         }
      }
   }

   @Override
   public void send(ZigBeeCommand command) {
      if (networkManager != null) {
         networkManager.sendCommand(command);
      }
   }

   @Override
   public ZigBeeNetworkManager getNetworkManager() {
      return networkManager;
   }

   @Override
   public long getCoordinatorEui64() {
      if (networkManager != null) {
         com.zsmartsystems.zigbee.IeeeAddress localAddr = networkManager.getLocalIeeeAddress();
         if (localAddr != null) {
            return ieeeToLong(localAddr);
         }
      }
      return 0;
   }

   private static long ieeeToLong(com.zsmartsystems.zigbee.IeeeAddress addr) {
      int[] value = addr.getValue();
      long result = 0;
      for (int i = value.length - 1; i >= 0; i--) {
         result = (result << 8) | (value[i] & 0xFF);
      }
      return result;
   }
}
