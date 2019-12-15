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

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.platform.ContextualEventMessageHandler;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.PlatformServiceAddress;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.HubCapability;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.model.Hub;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.services.PlatformConstants;

@Singleton
public class PlaceValueChangeListener implements ContextualEventMessageHandler<Place> {
   private static final Logger logger = LoggerFactory.getLogger(PlaceValueChangeListener.class);

   private final HubDAO hubDao;
   private final PlaceDAO placeDao;
   private final PlatformMessageBus bus;

   @Inject
   public PlaceValueChangeListener(HubDAO hubDao, PlaceDAO placeDao, PlatformMessageBus bus) {
      this.bus = bus;
      this.hubDao = hubDao;
      this.placeDao = placeDao;
   }

   @Override
   public String getEventType() {
      return Capability.EVENT_VALUE_CHANGE;
   }

   @Override
   public void handleEvent(Place context, PlatformMessage message) {
      Hub hub = hubDao.findHubForPlace(context.getId());
      if (hub == null) {
         return;
      }

      MessageBody request = message.getValue();
      Map<String,Object> attributes = request.getAttributes();
      Object tzId = attributes.get(PlaceCapability.ATTR_TZID);
      if (tzId == null) {
         return;
      }

      Map<String,Object> hubAttrs = ImmutableMap.of(HubCapability.ATTR_TZ, tzId);
      MessageBody hubRequest = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, hubAttrs);

      Address src = Address.platformService(PlatformConstants.SERVICE_HUB);
      Address dst = Address.hubService(hub.getId(), PlatformConstants.SERVICE_HUB);

      PlatformMessage hubMessage = PlatformMessage.buildRequest(hubRequest, src, dst).create();
      bus.send(hubMessage);
   }

   @Override
   public void handleStaticEvent(PlatformMessage msg) {
      Address address = msg.getSource();
      if(!PlatformConstants.SERVICE_PLACES.equals(address.getGroup())) {
         return;
      }

      if(address instanceof PlatformServiceAddress) {
         Object placeId = address.getId();
         UUID placeIdUuid = null;
         try {
	         if (!(placeId instanceof UUID)) {
	        	 placeIdUuid = UUID.fromString(String.valueOf(placeId));
	         }else {
	        	 placeIdUuid = (UUID) placeId;
	         }
	         Place context = placeDao.findById(placeIdUuid);
	         if(context == null) {
	            logger.trace("Unable to handle msg [{}], place {} does not exist.", msg, placeId);
	         } else {
	            handleEvent(context, msg);
	         }
         }catch(IllegalArgumentException e) {
        	 if(placeIdUuid == null) {
        		 //This mean UUID parsing error
        		 logger.warn("Unable to handle event because PlatformMessage source contains invalid address "+placeId, e);
        	 }
         }
      }
   }
}

