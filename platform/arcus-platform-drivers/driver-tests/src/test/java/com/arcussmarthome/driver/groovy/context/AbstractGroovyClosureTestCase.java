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
package com.arcussmarthome.driver.groovy.context;

import java.util.Date;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;

import com.google.inject.Inject;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.dao.PersonDAO;
import com.arcussmarthome.core.dao.PersonPlaceAssocDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.dao.file.FileDAOModule;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.core.messaging.memory.InMemoryPlatformMessageBus;
import com.arcussmarthome.core.messaging.memory.InMemoryProtocolMessageBus;
import com.arcussmarthome.device.attributes.AttributeMap;
import com.arcussmarthome.driver.DeviceDriver;
import com.arcussmarthome.driver.DeviceDriverContext;
import com.arcussmarthome.driver.DeviceDriverDefinition;
import com.arcussmarthome.driver.PlatformDeviceDriverContext;
import com.arcussmarthome.driver.groovy.ClasspathResourceConnector;
import com.arcussmarthome.driver.groovy.GroovyContextObject;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.population.PlacePopulationCacheManager;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;

import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

@Mocks({ DeviceDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PlacePopulationCacheManager.class })
@Modules({ InMemoryMessageModule.class, FileDAOModule.class })
public abstract class AbstractGroovyClosureTestCase extends IrisMockTestCase {
   protected Script script;
   protected DeviceDriverContext context;
   @Inject protected DeviceDAO mockDeviceDao;
   @Inject protected InMemoryProtocolMessageBus protocolBus;
   @Inject protected InMemoryPlatformMessageBus platformBus;
   @Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;
   
   protected Address driverAddress = Fixtures.createDeviceAddress();
   protected Address protocolAddress = Fixtures.createProtocolAddress();

   protected void initTest(String scriptName) throws Exception {
      Capture<Device> deviceRef = Capture.newInstance();
      EasyMock
         .expect(mockDeviceDao.save(EasyMock.capture(deviceRef)))
         .andAnswer(() -> {
            Device device = deviceRef.getValue().copy();
            if(device.getId() == null) {
               device.setId(UUID.randomUUID());
            }
            if(device.getCreated() == null) {
               device.setCreated(new Date());
            }
            device.setModified(new Date());
            return device;
         })
         .anyTimes();
      mockDeviceDao.updateDriverState(EasyMock.notNull(), EasyMock.notNull());
      EasyMock.expectLastCall().anyTimes();
      EasyMock.replay(mockDeviceDao);
      
      Device device = Fixtures.createDevice();
      device.setDriverAddress(driverAddress.getRepresentation());
      device.setProtocolAddress(protocolAddress.getRepresentation());
      DeviceDriver driver = EasyMock.createNiceMock(DeviceDriver.class);
      EasyMock.expect(driver.getDefinition()).andReturn(DeviceDriverDefinition.builder().withName("TestDriver").create()).anyTimes();
      EasyMock.expect(driver.getBaseAttributes()).andReturn(AttributeMap.emptyMap()).anyTimes();
      EasyMock.replay(driver);

      GroovyScriptEngine engine = new GroovyScriptEngine(new ClasspathResourceConnector(TestGroovyCapabilityDefinition.class));
      context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
      script = engine.createScript(scriptName, new Binding());
      script.run();
      GroovyContextObject.setContext(context);
   }

   @After
   @Override
   public void tearDown() throws Exception {
      GroovyContextObject.clearContext();
      super.tearDown();
   }

}

