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
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.capability.PlaceCapability.StartAddingDevicesRequest;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.model.Hub;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.services.PlatformConstants;

@Singleton
public class StartAddingDevicesHandler implements ContextualRequestMessageHandler<Place> {

   private final HubDAO hubDao;
   private final DeviceDAO deviceDao;
   
   private final PlatformMessageBus bus;

   @Inject
   public StartAddingDevicesHandler(HubDAO hubDao, DeviceDAO deviceDao,PlatformMessageBus bus) {
      this.hubDao = hubDao;
      this.bus = bus;
      this.deviceDao=deviceDao;
   }

   @Override
   public String getMessageType() {
      return StartAddingDevicesRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      Utils.assertNotNull(context, "Place is required to start adding devices");
      MessageBody request = msg.getValue();
      Map<String,Object> attributes = request.getAttributes();
      long time = ((Number) attributes.get("time")).longValue();

      Hub hub = hubDao.findHubForPlace(context.getId());
      if(hub != null) {
         MessageBody pairingRequest =
               HubCapability.PairingRequestRequest
                  .builder()
                  .withActionType(HubCapability.PairingRequestRequest.ACTIONTYPE_START_PAIRING)
                  .withTimeout(time)
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
         if(device.getCaps()!=null&&device.getCaps().contains(BridgeCapability.NAMESPACE)){
            sendStartPairing(context.getId(), device.getAddress(),time);
         }
      }
      return PlaceCapability.StartAddingDevicesResponse.instance();
   }
   
   private void sendStartPairing(UUID placeId, String deviceAddress, long timeout){
      MessageBody body = BridgeCapability.StartPairingRequest.builder().withTimeout(timeout).build();
      bus.send(PlatformMessage.buildRequest(
            body,
            Address.platformService(placeId,PlatformConstants.SERVICE_PLACES),
            Address.fromString(deviceAddress)).create());      
   }

}

