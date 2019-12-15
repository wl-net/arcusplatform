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

import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.bridge.bus.PlatformBusService;
import com.arcussmarthome.bridge.server.session.Session;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.hubcom.server.session.HubSession;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.MessageConstants;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.hub.HubModel;
import com.arcussmarthome.platform.partition.Partitioner;
import com.arcussmarthome.population.PlacePopulationCacheManager;

@Singleton
public class HubRegisteredResponseHandler extends DirectMessageHandler {

   private final HubDAO hubDao;
   private final Partitioner partitioner;

   @Inject
   public HubRegisteredResponseHandler(
         PlatformBusService bus,
         HubDAO hubDao,
         Partitioner partitioner,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      super(bus, populationCacheMgr);
      this.hubDao = hubDao;
      this.partitioner = partitioner;
   }

   @Override
   public String supportsMessageType() {
      return MessageConstants.MSG_HUB_REGISTERED_RESPONSE;
   }

   @Override
   protected void doHandle(Session session, PlatformMessage msg) {
      Model m = hubDao.findHubModel(msg.getSource().getHubId());      
      authorized(session, m, msg.getCorrelationId());
      String placeId = HubModel.getPlace(m);
      sendHubAdded(m, msg.getCorrelationId(), getPlacePopulationCacheManager().getPopulationByPlaceId(UUID.fromString(placeId)));
      ((HubSession) session).setPartition(partitioner.getPartitionForPlaceId(placeId));
   }

   private void sendHubAdded(Model hub, String correlationId, String population) {
      MessageBody hubAdded = MessageBody.buildMessage(Capability.EVENT_ADDED, hub.toMap());
      sendToPlatform(PlatformMessage.buildBroadcast(
            hubAdded,
            hub.getAddress())
            .withCorrelationId(correlationId)
            .withPlaceId(HubModel.getPlace(hub))
            .withPopulation(population)
            .create());
   }
}

