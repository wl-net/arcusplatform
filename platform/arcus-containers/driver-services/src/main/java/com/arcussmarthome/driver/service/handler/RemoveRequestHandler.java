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
package com.arcussmarthome.driver.service.handler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.driver.groovy.plugin.GroovyDriverPlugin;
import com.arcussmarthome.driver.service.DeviceService;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.capability.ProductCapability;
import com.arcussmarthome.messages.capability.DeviceCapability.RemoveResponse;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.model.serv.PairingDeviceModel;
import com.arcussmarthome.platform.pairing.PairingRemovalUtils;
import com.arcussmarthome.platform.pairing.ProductLoader;
import com.arcussmarthome.prodcat.ProductCatalogEntry;
import com.arcussmarthome.protocol.zwave.ZWaveProtocol;

public class RemoveRequestHandler extends AbstractRemoveRequestHandler {
   private static final Logger logger = LoggerFactory.getLogger(RemoveRequestHandler.class);
   static final long DEFAULT_REMOVE_DURATION = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

   private final DeviceService service;
   private final PlaceDAO placeDao;
   private final ProductLoader productLoader;
   
   @Inject
   public RemoveRequestHandler(
         DeviceDAO deviceDao,
         DeviceService service,
         PlaceDAO placeDao,
         ProductLoader productLoader,
         PlatformMessageBus platformBus,
         Set<GroovyDriverPlugin> plugins
   ) {
      super(deviceDao, platformBus, plugins);
      this.service = service;
      this.placeDao = placeDao;
      this.productLoader = productLoader;
   }

   @Override
   public String getMessageType() {
      return DeviceCapability.RemoveRequest.NAME;
   }

   @Override
   public MessageBody handleMessage(PlatformMessage message) throws Exception {
      logger.info("received a remove device request: {}", message);

      MessageBody body = message.getValue();
      Long duration = DeviceCapability.RemoveRequest.getTimeout(body);
      if (duration == null) {
         duration = DEFAULT_REMOVE_DURATION;
      }
      
      Device device = loadDevice(message);
      if(device.isLost()) {
         service.delete(device);
         return
            RemoveResponse
               .builder()
               .withMode(RemoveResponse.MODE_CLOUD) // hub doesn't beep
               .withSteps(PairingRemovalUtils.createDefaultRemovalStep(device.getName(), false))
               .build();
      }
      else {
         sendRemoveRequest(device, duration, false);
         return
               RemoveResponse
                  .builder()
                  .withMode(determineMode(device))
                  .withSteps(loadSteps(device))
                  .build();
      }
   }

   private String determineMode(Device device) {
      if(StringUtils.isEmpty(device.getHubId())) {
         return RemoveResponse.MODE_CLOUD;
      }
      else if(ZWaveProtocol.NAMESPACE.equals(device.getProtocol())) {
         return RemoveResponse.MODE_HUB_MANUAL;
      }
      else {
         return RemoveResponse.MODE_HUB_AUTOMATIC;
      }
   }

   private List<Map<String, Object>> loadSteps(Device device) {
      if(StringUtils.isEmpty(device.getProductId())) {
         return PairingRemovalUtils.createDefaultRemovalStep();
      }
      Address productAddress = Address.platformService(device.getProductId(), ProductCapability.NAMESPACE);
      try {
         Place place = placeDao.findById(device.getPlace());
         ProductCatalogEntry entry = productLoader.get(place, productAddress);
         return PairingRemovalUtils.loadRemovalSteps(entry);
      }
      catch(Exception e) {
         logger.warn("Unable to load removal steps for product [{}]", productAddress, e);
         return PairingRemovalUtils.createDefaultRemovalStep();
      }
   }
}

