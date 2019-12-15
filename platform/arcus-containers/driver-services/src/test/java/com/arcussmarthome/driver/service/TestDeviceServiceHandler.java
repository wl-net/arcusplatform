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
package com.arcussmarthome.driver.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.core.dao.PersonDAO;
import com.arcussmarthome.core.dao.PersonPlaceAssocDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.dao.PopulationDAO;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.core.messaging.memory.InMemoryPlatformMessageBus;
import com.arcussmarthome.core.protocol.ipcd.IpcdDeviceDao;
import com.arcussmarthome.driver.service.executor.DriverExecutorRegistry;
import com.arcussmarthome.driver.service.registry.DriverRegistry;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.services.PlatformConstants;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;

@Mocks({
   DeviceDAO.class,
   HubDAO.class,
   IpcdDeviceDao.class,
   HubDAO.class,
   PersonDAO.class,
   PersonPlaceAssocDAO.class,
   DriverRegistry.class,
   DriverExecutorRegistry.class,
   PlaceDAO.class,
   PopulationDAO.class
})
@Modules({ InMemoryMessageModule.class })
public class TestDeviceServiceHandler extends IrisMockTestCase {

   @Inject private DeviceDAO devDao;
   @Inject private HubDAO hubDao;
   @Inject private IpcdDeviceDao ipcdDevDao;
   @Inject private PersonDAO personDao;
   @Inject private PersonPlaceAssocDAO personPlaceAssocDao;
   @Inject private DeviceService service;
   @Inject private DriverExecutorRegistry execReg;
   @Inject private InMemoryPlatformMessageBus platformBus;

   private DeviceServiceHandler handler;
   private UUID placeId;
   private List<Device> devices;

   @Override
   public void setUp() throws Exception {
      super.setUp();

      handler = new DeviceServiceHandler(platformBus, new DriverServiceConfig(), devDao, hubDao, ipcdDevDao, personDao, personPlaceAssocDao, service, null);

      placeId = UUID.randomUUID();
      devices = new ArrayList<>();
      Device d1 = new Device();
      d1.setId(UUID.randomUUID());
      d1.setHubId("ABC-1234");
      d1.setCreated(new Date());
      d1.setAddress(Address.platformDriverAddress(d1.getId()).getRepresentation());
      devices.add(d1);
      Device d2 = new Device();
      d2.setId(UUID.randomUUID());
      d2.setCreated(new Date());
      d2.setAddress(Address.platformDriverAddress(d2.getId()).getRepresentation());
      devices.add(d2);
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testOnPlaceDeletedOnlyRemovesWithoutHubId() throws Exception {
      EasyMock.expect(devDao.listDevicesByPlaceId(placeId, true)).andReturn(devices);
      EasyMock.expect(execReg.delete(Address.fromString(devices.get(1).getAddress()))).andReturn(true);
      replay();

      handler.handleEvent(createPlaceDeleted());
   }

   private PlatformMessage createPlaceDeleted() {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_DELETED, ImmutableMap.of());
      return PlatformMessage.buildBroadcast(body, Address.platformService(placeId, PlatformConstants.SERVICE_PLACES))
            .withPlaceId(placeId)
            .create();
   }

}

