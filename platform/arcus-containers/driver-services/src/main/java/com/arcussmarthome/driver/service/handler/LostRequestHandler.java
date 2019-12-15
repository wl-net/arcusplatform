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

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.driver.service.DeviceService;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.DeviceDriverAddress;
import com.arcussmarthome.messages.capability.DeviceConnectionCapability.LostDeviceRequest;
import com.arcussmarthome.messages.capability.DeviceConnectionCapability.LostDeviceResponse;
import com.arcussmarthome.messages.errors.NotFoundException;
import com.arcussmarthome.messages.model.Device;

public class LostRequestHandler implements DriverServiceRequestHandler{
   private static final Logger logger = LoggerFactory.getLogger(LostRequestHandler.class);
   
   private DeviceService deviceService;
   private DeviceDAO deviceDao;
   
   @Inject
   public LostRequestHandler(
         DeviceDAO deviceDao,
         PlatformMessageBus platformBus,
         DeviceService deviceService) {
      this.deviceService=deviceService;
      this.deviceDao=deviceDao;
   }

   protected Device loadDevice(PlatformMessage message) throws NotFoundException {
      Address destination = message.getDestination();
      UUID deviceId = ((DeviceDriverAddress) destination).getDeviceId();
      Device dev = deviceDao.findById(deviceId);
      if (dev == null) {
         logger.warn("Message sent to non-existent device [{}]", (deviceId));
         throw new NotFoundException(destination);
      }
      return dev;
   }

   @Override
   public MessageBody handleMessage(PlatformMessage message) throws Exception {
      logger.info("received a lost device request: {}", message);
      Device device = loadDevice(message);
      deviceService.lostDevice(Address.fromString(device.getAddress()));
      return LostDeviceResponse.instance();
   }

   @Override
   public String getMessageType() {
      return LostDeviceRequest.NAME;
   }
}

