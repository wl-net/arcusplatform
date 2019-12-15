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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.arcussmarthome.bootstrap.ServiceLocator;
import com.arcussmarthome.core.messaging.memory.InMemoryProtocolMessageBus;
import com.arcussmarthome.driver.DeviceDriver;
import com.arcussmarthome.driver.DeviceDriverContext;
import com.arcussmarthome.driver.PlatformDeviceDriverContext;
import com.arcussmarthome.driver.event.DriverEvent;
import com.arcussmarthome.driver.groovy.GroovyDriverTestCase;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.zwave.ZWaveProtocol;
import com.arcussmarthome.protocol.zwave.message.ZWaveCommandMessage;

@RunWith(Parameterized.class)
public class TestZWaveContextBinding extends GroovyDriverTestCase {
   private String driverFile;

   private DeviceDriver driver;
   private DeviceDriverContext context;
   private InMemoryProtocolMessageBus bus;

   @Parameters(name="{0}")
   public static Iterable<Object []> files() {
      return Arrays.asList(
            new Object [] { "ZWaveContextBinding.driver" },
            new Object [] { "ZWaveContextBindingCapability.driver" }
      );
   }

   public TestZWaveContextBinding(String driverFile) {
      this.driverFile = driverFile;
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load(driverFile);
      context = new PlatformDeviceDriverContext(Fixtures.createDevice(), driver, mockPopulationCacheMgr);
      bus = ServiceLocator.getInstance(InMemoryProtocolMessageBus.class);
   }

   @Test
   public void testZWaveSend() throws Exception {
      driver.handleDriverEvent(DriverEvent.createConnected(0), context);

      ProtocolMessage message = bus.take();
      ZWaveCommandMessage zm = (ZWaveCommandMessage) ZWaveProtocol.INSTANCE.createDeserializer().deserialize(message.getBuffer());
      assertEquals(37, zm.getCommand().commandClass);
      assertEquals(2, zm.getCommand().commandNumber);
      assertEquals(Collections.emptyList(), zm.getCommand().sendNames);
   }

}

