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
package com.arcussmarthome.driver.groovy.zwave;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.driver.DeviceDriver;
import com.arcussmarthome.driver.DeviceDriverContext;
import com.arcussmarthome.driver.PlatformDeviceDriverContext;
import com.arcussmarthome.driver.groovy.GroovyDriverTestCase;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.ProtocolDeviceId;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.zwave.ZWaveProtocol;
import com.arcussmarthome.protocol.zwave.message.ZWaveCommandMessage;
import com.arcussmarthome.protocol.zwave.message.ZWaveNodeInfoMessage;

@RunWith(Parameterized.class)
public class TestZWaveMessageHandler extends GroovyDriverTestCase {
   private String driverFile;
   private DeviceDriver driver;
   private DeviceDriverContext context;

   private byte nodeId = 1;

   private Address zwaveDevice = Address.hubProtocolAddress(
         "LWW-1234",
         ZWaveProtocol.NAMESPACE,
         ProtocolDeviceId.hashDeviceId("LWW-1234-0-" + nodeId)
   );
   
   @Inject DeviceDAO mockDeviceDao;

   @Parameters(name="{0}")
   public static Iterable<Object []> files() {
      return ImmutableList.of(
            new Object [] { "ZWaveMessageHandlerBytes.driver" },
            new Object [] { "ZWaveMessageHandlerObjects.driver" },
            new Object [] { "ZWaveMessageHandlerStrings.driver" },
            new Object [] { "ZWaveMessageHandlerCapability.driver" }
      );
   }

   public TestZWaveMessageHandler(String driverFile) {
      this.driverFile = driverFile;
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load(driverFile);
      context = new PlatformDeviceDriverContext(createDevice(driver), driver, mockPopulationCacheMgr);
   }

   @Test
   public void testExactMatch() throws Exception {
      ZWaveCommandMessage command =
            ZWaveFixtures
               .createCommandMessage(nodeId, ZWaveFixtures.BINARY_SWITCH, ZWaveFixtures.BINARY_SWITCH_REPORT, (byte) 0);
      ProtocolMessage message =
            ProtocolMessage
               .builder()
               .from(zwaveDevice)
               .withPayload(ZWaveProtocol.INSTANCE, command)
               .create();
      driver.handleProtocolMessage(message, context);
      assertEquals("exact", context.getVariable("match"));
   }

   @Test
   public void testCommandClassMatch() throws Exception {
      ZWaveCommandMessage command =
            ZWaveFixtures
               .createCommandMessage(nodeId, ZWaveFixtures.BINARY_SWITCH, ZWaveFixtures.GET_COMMAND);
      ProtocolMessage message =
            ProtocolMessage
               .builder()
               .from(zwaveDevice)
               .withPayload(ZWaveProtocol.INSTANCE, command)
               .create();
      driver.handleProtocolMessage(message, context);
      assertEquals("commandClass", context.getVariable("match"));
   }

   @Test
   public void testProtocolMatch() throws Exception {
      ZWaveCommandMessage command =
            ZWaveFixtures
               .createCommandMessage(nodeId, (byte) 42, ZWaveFixtures.GET_COMMAND);
      ProtocolMessage message =
            ProtocolMessage
               .builder()
               .from(zwaveDevice)
               .withPayload(ZWaveProtocol.INSTANCE, command)
               .create();
      driver.handleProtocolMessage(message, context);
      assertEquals("protocol", context.getVariable("match"));
   }

   @Test
   public void testNodeInfoMatch() throws Exception {
      ZWaveNodeInfoMessage info =
            ZWaveFixtures
               .createNodeInfoMessage(nodeId);
      ProtocolMessage message =
            ProtocolMessage
               .builder()
               .from(zwaveDevice)
               .withPayload(ZWaveProtocol.INSTANCE, info)
               .create();
      driver.handleProtocolMessage(message, context);
      assertEquals("NodeInfo", context.getVariable("match"));
   }

}

