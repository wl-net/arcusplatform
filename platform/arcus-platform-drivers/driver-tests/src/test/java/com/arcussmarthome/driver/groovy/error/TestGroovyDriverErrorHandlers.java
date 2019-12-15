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
package com.arcussmarthome.driver.groovy.error;

import java.util.Set;

import org.junit.Test;

import com.arcussmarthome.driver.DeviceDriver;
import com.arcussmarthome.driver.DeviceDriverContext;
import com.arcussmarthome.driver.PlatformDeviceDriverContext;
import com.arcussmarthome.driver.groovy.GroovyDriverTestCase;
import com.arcussmarthome.driver.groovy.control.ControlProtocolPlugin;
import com.arcussmarthome.driver.groovy.plugin.GroovyDriverPlugin;
import com.arcussmarthome.driver.groovy.zwave.ZWaveProtocolPlugin;
import com.arcussmarthome.messages.ErrorEvent;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.control.ControlProtocol;
import com.arcussmarthome.util.IrisCollections;

public class TestGroovyDriverErrorHandlers extends GroovyDriverTestCase {
   private final static String OFFLINE_CODE = "DeviceOffline";
   private final static String OFFLINE_MSG = "The doomsday device isn't responding.";
   private final ControlProtocol controlProtocol = ControlProtocol.INSTANCE;
   private DeviceDriver driver;
   private DeviceDriverContext context;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load("ErrorMessageHandler.driver");
      context = new PlatformDeviceDriverContext(createDevice(driver), driver, mockPopulationCacheMgr);
   }

   @Test
   public void testMatchError() throws Exception {
      ProtocolMessage message = createErrorMessage(ErrorEvent.fromCode(OFFLINE_CODE, OFFLINE_MSG));
      driver.handleProtocolMessage(message, context);
      assertEquals(OFFLINE_CODE, context.getVariable("code"));
      assertEquals(OFFLINE_MSG, context.getVariable("errorMsg"));
   }

   @Override
   protected Set<GroovyDriverPlugin> getPlugins() {
      return IrisCollections.<GroovyDriverPlugin>setOf(new ControlProtocolPlugin(), new ZWaveProtocolPlugin());
   }

   private ProtocolMessage createErrorMessage(ErrorEvent errorEvent) {
      return ProtocolMessage.builder()
               .from(Fixtures.createProtocolAddress(ControlProtocol.NAMESPACE))
               .to(Fixtures.createDeviceAddress())
               .withPayload(controlProtocol, errorEvent)
               .create();
   }
}

