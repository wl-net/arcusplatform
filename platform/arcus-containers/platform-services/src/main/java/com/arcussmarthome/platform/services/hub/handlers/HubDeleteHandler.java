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
package com.arcussmarthome.platform.services.hub.handlers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.core.platform.ContextualRequestMessageHandler;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.HubAdvancedCapability;
import com.arcussmarthome.messages.capability.HubCapability;
import com.arcussmarthome.messages.model.Hub;
import com.arcussmarthome.messages.services.PlatformConstants;
import com.arcussmarthome.population.PlacePopulationCacheManager;

@Singleton
public class HubDeleteHandler implements ContextualRequestMessageHandler<Hub> {
	private static final Logger logger = LoggerFactory.getLogger(HubDeleteHandler.class);
   private final HubDAO hubDao;
   private final PlatformMessageBus bus;   
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public HubDeleteHandler(HubDAO hubDao, PlatformMessageBus bus, PlacePopulationCacheManager populationCacheMgr) {
      this.hubDao = hubDao;      
      this.bus = bus;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String getMessageType() {
      return HubCapability.DeleteRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Hub context, PlatformMessage msg) {
      deleteHub(context);
      return HubCapability.DeleteResponse.instance();
   }

   protected void deleteHub(Hub hub) {
   	UUID placeId = hub.getPlace();
   	String population = populationCacheMgr.getPopulationByPlaceId(hub.getPlace());
      PlatformMessage deregister = PlatformMessage.buildMessage(HubAdvancedCapability.DeregisterEvent.instance(),
            Address.platformService(PlatformConstants.SERVICE_HUB),
            Address.fromString(hub.getAddress()))
            .withPlaceId(placeId)
            .withPopulation(population)
            .create();
      bus.send(deregister);      
      hubDao.delete(hub);

      PlatformMessage deleted = PlatformMessage.buildBroadcast(
            MessageBody.buildMessage(Capability.EVENT_DELETED, ImmutableMap.of()),
            Address.fromString(hub.getAddress()))
            .withPlaceId(placeId)
            .withPopulation(population)
            .create();
      bus.send(deleted);
   }	
}

