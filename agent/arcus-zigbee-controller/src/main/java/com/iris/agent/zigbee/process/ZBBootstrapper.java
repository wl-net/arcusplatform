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

package com.iris.agent.zigbee.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.zigbee.ZBNetwork;
import com.iris.agent.zigbee.ZBServices;
import com.iris.agent.zigbee.db.ZBDao;
import com.iris.agent.zigbee.ember.ZigbeeDriver;
import com.iris.agent.zigbee.events.ZBBootstrapFinishedEvent;
import com.iris.agent.zigbee.events.ZBEventDispatcher;
import com.iris.agent.zigbee.events.ZBNodeAddedEvent;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.agent.zigbee.node.ZBNodeBuilder;
import com.zsmartsystems.zigbee.ZigBeeNode;
import com.zsmartsystems.zigbee.zdo.field.NodeDescriptor;

public class ZBBootstrapper {
   private static final Logger logger = LoggerFactory.getLogger(ZBBootstrapper.class);

   public static final ZBBootstrapper INSTANCE = new ZBBootstrapper();

   private ZBBootstrapper() {}

   public void bootstrap(ZigbeeDriver driver) {
      logger.info("Starting ZigBee bootstrap process...");

      // Store the driver in the service locator
      ZBServices.INSTANCE.setDriver(driver);

      // Start the database
      ZBDao.start();

      // Initialize the network (loads nodes from DB)
      ZBNetwork network = ZBServices.INSTANCE.getNetwork();
      network.initialize();

      // Initialize the ZigBee driver with callbacks
      driver.initialize(new ZigbeeDriver.ZBNetworkCallbacks() {
         @Override
         public void onNodeAdded(ZigBeeNode zsNode) {
            long ieeeAddr = ieeeToLong(zsNode.getIeeeAddress());
            ZBNode existing = network.getNode(ieeeAddr);
            if (existing != null) {
               // Node already known, update NWK address
               existing.setNwkAddr(zsNode.getNetworkAddress());
               network.saveNode(existing);
               return;
            }

            // Create a new node from the zsmartsystems node
            ZBNodeBuilder builder = ZBNode.builder(ieeeAddr)
                  .setNwkAddr(zsNode.getNetworkAddress());

            NodeDescriptor nd = zsNode.getNodeDescriptor();
            if (nd != null) {
               builder.setMaximumIncomingTransferSize(nd.getIncomingTransferSize())
                     .setMaximumOutgoingTransferSize(nd.getOutGoingTransferSize())
                     .setMaximumBufferSize(nd.getBufferSize())
                     .setManufacturerCode(nd.getManufacturerCode());
            }

            if (zsNode.getPowerDescriptor() != null) {
               builder.setPowerDescriptor(zsNode.getPowerDescriptor().getPowerLevel().ordinal());
            }

            ZBNode node = builder.build();
            ZBEventDispatcher.INSTANCE.dispatch(new ZBNodeAddedEvent(node));
         }

         @Override
         public void onNodeRemoved(ZigBeeNode zsNode) {
            long ieeeAddr = ieeeToLong(zsNode.getIeeeAddress());
            ZBEventDispatcher.INSTANCE.dispatch(
                  new com.iris.agent.zigbee.events.ZBNodeRemovedEvent(ieeeAddr));
         }

         @Override
         public void onNodeUpdated(ZigBeeNode zsNode) {
            long ieeeAddr = ieeeToLong(zsNode.getIeeeAddress());
            ZBNode node = network.getNode(ieeeAddr);
            if (node != null) {
               node.setNwkAddr(zsNode.getNetworkAddress());
               network.saveNode(node);
            }
         }

         @Override
         public void onCommandReceived(com.zsmartsystems.zigbee.ZigBeeCommand command) {
            com.iris.agent.zigbee.ZBMessageTranslator.handleInboundCommand(command);
         }

         @Override
         public void onAnnounce(int nwkAddr, long ieeeAddr) {
            ZBNode node = network.getNode(ieeeAddr);
            if (node != null) {
               node.setNwkAddr(nwkAddr);
               network.saveNode(node);
               ZBEventDispatcher.INSTANCE.dispatch(
                     new com.iris.agent.zigbee.events.ZBNodeHeardFromEvent(ieeeAddr));
            }
         }
      });

      logger.info("ZigBee bootstrap complete");
      ZBEventDispatcher.INSTANCE.dispatch(new ZBBootstrapFinishedEvent());
   }

   /**
    * Converts a zsmartsystems IeeeAddress (backed by int[]) to a long.
    */
   private static long ieeeToLong(com.zsmartsystems.zigbee.IeeeAddress addr) {
      int[] value = addr.getValue();
      long result = 0;
      for (int i = value.length - 1; i >= 0; i--) {
         result = (result << 8) | (value[i] & 0xFF);
      }
      return result;
   }
}
