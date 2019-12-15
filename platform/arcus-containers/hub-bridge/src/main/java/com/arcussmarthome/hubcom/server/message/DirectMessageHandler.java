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
package com.arcussmarthome.hubcom.server.message;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arcussmarthome.bridge.bus.PlatformBusService;
import com.arcussmarthome.bridge.server.session.Session;
import com.arcussmarthome.bridge.server.session.SessionUtil;
import com.arcussmarthome.hubcom.server.session.HubSession;
import com.arcussmarthome.hubcom.server.session.HubSession.State;
import com.arcussmarthome.hubcom.server.session.HubSession.UnauthorizedReason;
import com.arcussmarthome.io.Serializer;
import com.arcussmarthome.io.json.JSON;
import com.arcussmarthome.messages.HubMessage;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.MessageConstants;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.model.Hub;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.hub.HubModel;
import com.arcussmarthome.messages.services.PlatformConstants;
import com.arcussmarthome.population.PlacePopulationCacheManager;

public abstract class DirectMessageHandler {

   private static final Logger log = LoggerFactory.getLogger(DirectMessageHandler.class);

   private final PlatformBusService platformBus;
   private final Serializer<PlatformMessage> platformMessageSerializer = JSON.createSerializer(PlatformMessage.class);
   private final Serializer<HubMessage> hubMessageSerializer = JSON.createSerializer(HubMessage.class);
   private final PlacePopulationCacheManager populationCacheMgr;

   protected DirectMessageHandler(PlatformBusService platformBus, PlacePopulationCacheManager populationCacheMgr) {
      this.platformBus = platformBus;
      this.populationCacheMgr = populationCacheMgr;
   }

   public abstract String supportsMessageType();

   public void handle(Session session, PlatformMessage msg) {
      if (!supportsMessageType().equals(msg.getMessageType())) {
         return;
      }
      doHandle(session, msg);
   }

   protected abstract void doHandle(Session session, PlatformMessage msg);

   protected void authorized(Session session, Model m, String correlationId) {
      authorized(session, m.getAddress(), HubModel.getPlace(m), correlationId);
   }

   protected void authorized(Session session, Hub hub, String correlationId) {
      authorized(session, Address.fromString(hub.getAddress()), String.valueOf(hub.getPlace()), correlationId);
   }

   private void authorized(Session session, Address hubAddress, String placeId, String correlationId) {
      if (!placeId.equals(session.getActivePlace())) {
         SessionUtil.setPlace(placeId, session);
      }
      updateSessionState(session, State.AUTHORIZED);
      updateUnauthorizedReason(session, null);

      sendToHub(session, PlatformMessage.buildMessage(
            MessageBody.buildMessage(MessageConstants.MSG_HUB_AUTHORIZED_EVENT, Collections.<String, Object>emptyMap()),
            Address.platformService(PlatformConstants.SERVICE_HUB),
            hubAddress)
            .withPlaceId(placeId)
            .withCorrelationId(correlationId)
            .create());
   }

   protected void sendToHub(Session session, PlatformMessage msg) {
      byte[] payload = platformMessageSerializer.serialize(msg);
      byte[] message = hubMessageSerializer.serialize(HubMessage.createPlatform(payload));
      if (session.getChannel().isActive()) {
         session.sendMessage(message);
      } else {
         log.warn("discarding message {} disconnected session {}", msg, session.getClientToken());
      }
   }

   protected void sendToPlatform(PlatformMessage msg) {
      platformBus.placeMessageOnPlatformBus(msg);
   }

   protected void updateSessionState(Session session, State newState) {
      HubSession hubSession = (HubSession) session;
      if (hubSession.getUnauthReason() == UnauthorizedReason.UNAUTHENTICATED) {
         throw new IllegalStateException("Can't change the state of an unauthenticated hub");
      }
      if (session.getChannel().isOpen()) {
         hubSession.setState(newState);
      }
   }

   protected void updateUnauthorizedReason(Session session, UnauthorizedReason reason) {
      if (session.getChannel().isOpen()) {
         ((HubSession) session).setUnauthReason(reason);
      }
   }

   protected PlacePopulationCacheManager getPlacePopulationCacheManager() {
      return populationCacheMgr;
   }
}

