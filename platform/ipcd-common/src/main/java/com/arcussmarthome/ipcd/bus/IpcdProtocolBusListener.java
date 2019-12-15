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
package com.arcussmarthome.ipcd.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.bridge.bus.ProtocolBusListener;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.session.ClientToken;
import com.arcussmarthome.ipcd.delivery.IpcdDeliveryStrategy;
import com.arcussmarthome.ipcd.delivery.IpcdDeliveryStrategyRegistry;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.ipcd.IpcdProtocol;
import com.arcussmarthome.protocol.ipcd.message.IpcdMessage;

@Singleton
public class IpcdProtocolBusListener implements ProtocolBusListener {
   private static final Logger logger = LoggerFactory.getLogger(IpcdProtocolBusListener.class);
   private final IpcdDeliveryStrategyRegistry deliveryStrategies;
   private final BridgeMetrics metrics;

   @Inject
   public IpcdProtocolBusListener(IpcdDeliveryStrategyRegistry deliveryStrategies, BridgeMetrics metrics) {
      this.deliveryStrategies = deliveryStrategies;
      this.metrics = metrics;
   }

   @Override
   public void onMessage(ClientToken ct, ProtocolMessage msg) {
      logger.trace("Received Protocol Message [{}]", msg);
      try {
         if (IpcdProtocol.NAMESPACE.equals(msg.getMessageType())) {
            IpcdMessage ipcdMessage = msg.getValue(IpcdProtocol.INSTANCE);
            if (ipcdMessage != null && ipcdMessage.getMessageType().isServer()) {
               IpcdDeliveryStrategy deliveryStrategy = deliveryStrategies.deliveryStrategyFor(ct);
               if(!deliveryStrategy.deliverToDevice(ct, msg.getPlaceId(), ipcdMessage)) {
                  logger.trace("[{}] has no session, it is likely talking to a different bridge", ct);
               }
            }
            else {
               metrics.incProtocolMsgDiscardedCounter();
               logger.debug("Ignoring unsupported message [{}]", ipcdMessage);
            }
         }
         else {
            metrics.incProtocolMsgDiscardedCounter();
            logger.debug("Ignoring non-IPCD message [{}]", msg);
         }
      } catch(Exception e) {
         logger.warn("[{}]:  failed to handle protocol message: {}", ct.getRepresentation(), msg, e);
      }
   }
}

