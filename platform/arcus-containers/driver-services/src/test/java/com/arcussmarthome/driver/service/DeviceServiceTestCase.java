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
package com.arcussmarthome.driver.service;

import java.util.UUID;

import org.easymock.EasyMock;

import com.google.inject.Inject;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.driver.DeviceDriverStateHolder;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.core.messaging.memory.InMemoryPlatformMessageBus;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;

/**
 * 
 */
@Mocks(DeviceDAO.class)
@Modules({ InMemoryMessageModule.class, TestDriverModule.class })
public class DeviceServiceTestCase extends IrisMockTestCase {

   @Inject
   protected DeviceService uut;
   
   @Inject 
   protected InMemoryPlatformMessageBus messages;
   @Inject 
   protected DeviceDAO mockDeviceDao;
   @Inject 
   protected DeviceDAO mockHubDao;

   protected void expectDeviceNotFound(UUID id) {
      EasyMock.expect(mockDeviceDao.findById(id)).andReturn(null).anyTimes();
   }

   protected void expectLoadDeviceAndState(Device device, DeviceDriverStateHolder state) {
      EasyMock.expect(mockDeviceDao.findById(device.getId())).andReturn(device).once();
      EasyMock.expect(mockDeviceDao.loadDriverState(device)).andReturn(state).once();
   }
}

