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
/**
 * 
 */
package com.arcussmarthome.driver.service.handler;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.driver.service.DeviceService;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.DeviceDriverAddress;
import com.arcussmarthome.messages.capability.DeviceConnectionCapability;
import com.arcussmarthome.messages.errors.NotFoundException;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;

@Mocks({
   DeviceDAO.class,
   HubDAO.class,
   DeviceService.class
})
@Modules({ InMemoryMessageModule.class})
public class TestLostRequestHandler extends IrisMockTestCase {

   @Inject private DeviceDAO devDao;
   @Inject private DeviceService service;
   @Inject LostRequestHandler handler;
   
   private Device device;
   
   protected Device createDevice() {
      Device device = Fixtures.createDevice();
      return device;
   }
   
   @Test
   public void handleRequest() throws Exception {
      device = createDevice();
      EasyMock.expect(devDao.findById(device.getId())).andReturn(device);
      service.lostDevice(Address.fromString(device.getAddress()));
      EasyMock.expectLastCall();
      replay();
      
      MessageBody body = DeviceConnectionCapability.LostDeviceRequest.instance();
      PlatformMessage message = PlatformMessage.buildRequest(body,DeviceDriverAddress.fromString(device.getAddress()),DeviceDriverAddress.fromString(device.getAddress())).create();
      handler.handleMessage(message);
      
      verify();
   }
   
   @Test
   public void handleRequestAddressNotFound() throws Exception {
      device = createDevice();
      EasyMock.expect(devDao.findById(device.getId())).andReturn(null);
      replay();
      
      MessageBody body = DeviceConnectionCapability.LostDeviceRequest.instance();
      PlatformMessage message = PlatformMessage.buildRequest(body,DeviceDriverAddress.fromString(device.getAddress()),DeviceDriverAddress.fromString(device.getAddress())).create();
      try{
         handler.handleMessage(message);
         fail();
      }
      catch(NotFoundException e){
         
      }
      
      verify();
   }
}

