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

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.messaging.memory.InMemoryPlatformMessageBus;
import com.arcussmarthome.driver.service.DeviceService;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.ProtocolDeviceId;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.capability.DeviceCapability.ForceRemoveResponse;
import com.arcussmarthome.messages.errors.NotFoundException;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.protocol.mock.MockProtocol;

public class TestForceRemoveRequestHandler extends RemoveRequestHandlerTestCase {

   @Inject private DeviceDAO deviceDao;
   @Inject private DeviceService service;
   @Inject private InMemoryPlatformMessageBus platformBus;
   
   @Inject ForceRemoveRequestHandler handler;
   
   private Device device;
   
   @Before
   public void initializeDevice() {
      device = Fixtures.createDevice();
      device.setPlace(UUID.randomUUID());
      device.setProtocol(MockProtocol.NAMESPACE);
      device.setProtocolid(ProtocolDeviceId.hashDeviceId("test").getRepresentation());
      device.setProtocolAddress(Address.protocolAddress(MockProtocol.NAMESPACE, ProtocolDeviceId.hashDeviceId("test")).getRepresentation());
   }
   
   public PlatformMessage createRemoveRequest() {
      return
            PlatformMessage
               .request(Address.fromString(device.getAddress()))
               .from(Fixtures.createClientAddress())
               .withPayload(DeviceCapability.ForceRemoveRequest.instance())
               .create();
   }
   
   /**
    * This should send a protocol level ForceRemove and tombstone the device.
    */
   @Test
   public void testForceRemoveActiveDevice() throws Exception {
      device.setState(Device.STATE_ACTIVE_SUPPORTED);
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      service.tombstone(device);
      EasyMock.expectLastCall();
      replay();

      MessageBody response = handler.handleMessage(createRemoveRequest());
      assertEquals(ForceRemoveResponse.instance(), response);

      PlatformMessage message = platformBus.take();
      assertDeviceRemovedMessage(device, message);
      
      assertNull(platformBus.poll());
      
      verify();
   }
   
   @Test
   public void testForceRemoveMissingDevice() throws Exception {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(null);
      replay();

      try {
         handler.handleMessage(createRemoveRequest());
         fail();
      }
      catch(NotFoundException e) {
         // expected
      }
      
      assertNull(platformBus.poll());
      
      verify();
   }

   @Test
   public void testForceRemoveLostDevice() throws Exception {
      device.setState(Device.STATE_LOST_RECOVERABLE);
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      EasyMock.expect(service.delete(device)).andReturn(true);
      replay();

      MessageBody response = handler.handleMessage(createRemoveRequest());
      assertEquals(ForceRemoveResponse.instance(), response);
      
      // NOTE device service is responsible for sending out the deleted event
      
      assertNull(platformBus.poll());
      
      verify();
   }

   /**
    * In this case we should just try deleting it again, why not?  It doesn't "un-tombstone"
    * the device.
    * @throws Exception
    */
   @Test
   public void testForceRemoveTombstonedDevice() throws Exception {
      device.setState(Device.STATE_TOMBSTONED);
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      service.tombstone(device);
      EasyMock.expectLastCall();
      replay();

      MessageBody response = handler.handleMessage(createRemoveRequest());
      assertEquals(ForceRemoveResponse.instance(), response);

      PlatformMessage message = platformBus.take();
      assertDeviceRemovedMessage(device, message);
      
      assertNull(platformBus.poll());
      
      verify();
   }
   
}

