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
package com.arcussmarthome.driver.groovy.ipcd;

import java.util.Set;

import com.arcussmarthome.device.model.CapabilityDefinition;
import com.arcussmarthome.driver.DeviceDriver;
import com.arcussmarthome.driver.DeviceDriverContext;
import com.arcussmarthome.driver.PlatformDeviceDriverContext;
import com.arcussmarthome.driver.groovy.GroovyDriverTestCase;
import com.arcussmarthome.driver.groovy.control.ControlProtocolPlugin;
import com.arcussmarthome.driver.groovy.plugin.GroovyDriverPlugin;
import com.arcussmarthome.driver.groovy.zigbee.ZigbeeProtocolPlugin;
import com.arcussmarthome.driver.groovy.zwave.ZWaveProtocolPlugin;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.ipcd.IpcdProtocol;
import com.arcussmarthome.protocol.ipcd.message.IpcdMessage;
import com.arcussmarthome.util.IrisCollections;

public class IpcdHandlersTestCase extends GroovyDriverTestCase {
   private String driverFile;
   protected DeviceDriver driver;
   protected DeviceDriverContext context;
   
   protected IpcdHandlersTestCase(String driverFile) {
      this.driverFile = driverFile;
   }
   
   @Override
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load("com/iris/driver/groovy/ipcd/" + driverFile);
      Device device = createDevice(driver);
      device.setProtocolAttributes(IpcdFixtures.createProtocolAttributes());
      for(CapabilityDefinition def: driver.getDefinition().getCapabilities()) {
          device.getCaps().add(def.getNamespace());
      }
      device.setDrivername(driver.getDefinition().getName());
      device.setDriverversion(driver.getDefinition().getVersion());
      context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
   }
   
   @Override
   protected Set<GroovyDriverPlugin> getPlugins() {
      return IrisCollections.<GroovyDriverPlugin>setOf(new IpcdProtocolPlugin(), new ZigbeeProtocolPlugin(), new ZWaveProtocolPlugin(), new ControlProtocolPlugin());
   }
   
   protected void sendMessage(IpcdMessage msg) {
      ProtocolMessage protocolMessage = createProtocolMessage(msg);
      driver.handleProtocolMessage(protocolMessage, context);
   }
   
   protected static ProtocolMessage createProtocolMessage(IpcdMessage msg) {
      return ProtocolMessage.builder()
                  .from(Fixtures.createProtocolAddress(IpcdProtocol.NAMESPACE))
                  .to(Fixtures.createDeviceAddress())
                  .withPayload(IpcdProtocol.INSTANCE, msg)
                  .create();
   }
}

