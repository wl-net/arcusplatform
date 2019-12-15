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
import java.util.UUID;

import com.google.inject.Inject;
import com.arcussmarthome.Utils;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.core.platform.ContextualRequestMessageHandler;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.BridgeCapability;
import com.arcussmarthome.messages.capability.HubCapability;
import com.arcussmarthome.messages.capability.HubCapability.PairingRequestRequest;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.capability.PlaceCapability.StopAddingDevicesRequest;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.model.Hub;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.services.PlatformConstants;

public class StopAddingDevicesHandler implements ContextualRequestMessageHandler<Place> {

   private final HubDAO hubDao;
   private final PlatformMessageBus bus;
   private final DeviceDAO deviceDao;

   @Inject
   public StopAddingDevicesHandler(HubDAO hubDao, DeviceDAO deviceDao, PlatformMessageBus bus) {
      this.hubDao = hubDao;
      this.bus = bus;
      this.deviceDao = deviceDao;
   }

   @Override
   public String getMessageType() {
      return StopAddingDevicesRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      Utils.assertNotNull(context, "Place is required to stop adding devices");

      Hub hub = hubDao.findHubForPlace(context.getId());
      if(hub != null) {
         MessageBody pairingRequest =
               HubCapability.PairingRequestRequest
                  .builder()
                  .withActionType(PairingRequestRequest.ACTIONTYPE_STOP_PAIRING)
                  .build()
                  ;

         bus.send(PlatformMessage.buildMessage(
               pairingRequest,
               Address.platformService(PlatformConstants.SERVICE_PLACES),
               Address.hubService(hub.getId(),  "hub"))
               .withPlaceId(context.getId())
               .withPopulation(context.getPopulation())
               .create());
      }

      List<Device>devices=deviceDao.listDevicesByPlaceId(context.getId());
      for(Device device:devices){
         if(device.getCaps()!=null && device.getCaps().contains(BridgeCapability.NAMESPACE)){
            sendStopParing(context.getId(), device.getAddress());
         }
      }

      return PlaceCapability.StopAddingDevicesResponse.instance();
   }
   private void sendStopParing(UUID placeId, String deviceAddress){
      MessageBody body = BridgeCapability.StopPairingRequest.instance();
      bus.send(PlatformMessage.buildRequest(
            body,
            Address.platformService(placeId,PlatformConstants.SERVICE_PLACES),
            Address.fromString(deviceAddress)).create());      
   }
}

