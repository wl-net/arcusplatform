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
package com.arcussmarthome.platform.services.place.handlers;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.Utils;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.platform.ContextualRequestMessageHandler;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.model.Place;

@Singleton
public class ListDevicesHandler implements ContextualRequestMessageHandler<Place> {

   private final DeviceDAO deviceDao;

   @Inject
   public ListDevicesHandler(DeviceDAO deviceDao) {
      this.deviceDao = deviceDao;
   }

   @Override
   public String getMessageType() {
      return PlaceCapability.ListDevicesRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      Utils.assertNotNull(context, "The place is required");
      List<Map<String, Object>> devices = deviceDao.listDeviceAttributesByPlaceId(context.getId());
      return
            PlaceCapability.ListDevicesResponse
               .builder()
               .withDevices(devices)
               .build()
               ;
   }

}

