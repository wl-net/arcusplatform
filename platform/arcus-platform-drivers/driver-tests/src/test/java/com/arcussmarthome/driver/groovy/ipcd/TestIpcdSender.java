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

import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provides;
import com.arcussmarthome.bootstrap.Bootstrap;
import com.arcussmarthome.bootstrap.ServiceLocator;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.bootstrap.guice.GuiceServiceLocator;
import com.arcussmarthome.core.dao.PersonDAO;
import com.arcussmarthome.core.dao.PersonPlaceAssocDAO;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.core.messaging.memory.InMemoryProtocolMessageBus;
import com.arcussmarthome.device.attributes.AttributeMap;
import com.arcussmarthome.driver.DeviceDriver;
import com.arcussmarthome.driver.DeviceDriverContext;
import com.arcussmarthome.driver.DeviceDriverDefinition;
import com.arcussmarthome.driver.PlatformDeviceDriverContext;
import com.arcussmarthome.driver.groovy.ClasspathResourceConnector;
import com.arcussmarthome.driver.groovy.GroovyContextObject;
import com.arcussmarthome.driver.groovy.GroovyDriverTestCase;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.ProtocolDeviceId;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.ipcd.IpcdProtocol;
import com.arcussmarthome.protocol.ipcd.message.IpcdMessage;
import com.arcussmarthome.protocol.ipcd.message.model.CommandType;
import com.arcussmarthome.protocol.ipcd.message.model.IpcdCommand;
import com.arcussmarthome.protocol.ipcd.message.model.MessageType;
import com.arcussmarthome.protocol.ipcd.message.model.SetDeviceInfoCommand;

import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

public class TestIpcdSender extends GroovyDriverTestCase {
   private GroovyScriptEngine engine;
   private Script script;
   private DeviceDriverContext context;
   private InMemoryProtocolMessageBus bus;
   private IpcdProtocol protocol;

   private Address driverAddress = Fixtures.createDeviceAddress();
   private Address protocolAddress = Address.protocolAddress(IpcdProtocol.NAMESPACE, ProtocolDeviceId.hashDeviceId("BlackBox:ms2:1234"));

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() throws Exception {
      protocol = IpcdProtocol.INSTANCE;
      Device device = Fixtures.createDevice();
      device.setAddress(driverAddress.getRepresentation());
      device.setProtocolAddress(protocolAddress.getRepresentation());
      device.setProtocolAttributes(IpcdFixtures.createProtocolAttributes());
      DeviceDriver driver = EasyMock.createNiceMock(DeviceDriver.class);
      EasyMock.expect(driver.getDefinition()).andReturn(DeviceDriverDefinition.builder().withName("TestDriver").create()).anyTimes();
      EasyMock.expect(driver.getBaseAttributes()).andReturn(AttributeMap.emptyMap()).anyTimes();
      EasyMock.replay(driver);

      ServiceLocator.init(GuiceServiceLocator.create(
            Bootstrap.builder()
               .withModuleClasses(InMemoryMessageModule.class)
               .withModules(new AbstractIrisModule() {

                  @Override
                  protected void configure() {
                     bind(IpcdContext.class);
                  }
                  @Provides
                  public PersonDAO personDao() {
                     return EasyMock.createNiceMock(PersonDAO.class);
                  }
                  @Provides
                  public PersonPlaceAssocDAO personPlaceAssocDao() {
                     return EasyMock.createNiceMock(PersonPlaceAssocDAO.class);
                  }

               }).build().bootstrap()
            ));
      bus = ServiceLocator.getInstance(InMemoryProtocolMessageBus.class);
      engine = new GroovyScriptEngine(new ClasspathResourceConnector(this.getClass()));
      context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
      script = engine.createScript("TestIpcdSend.gscript", new Binding());
      script.run();
      script.setProperty("Ipcd", ServiceLocator.getInstance(IpcdContext.class));

      GroovyContextObject.setContext(context);
   }

   @Test
   public void testSendRawJsonFromIpcd() throws Exception {
      script.invokeMethod("sendRawJsonFromIpcd", new Object[0]);
      IpcdCommand cmd = extractMessage(bus.take());
      Assert.assertEquals(CommandType.GetDeviceInfo.name(), cmd.getCommand());
      Assert.assertNull(cmd.getTxnid());
   }

   @Test
   public void testSendGetDeviceInfo() throws Exception {
      script.invokeMethod("sendGetDeviceInfo", new Object[0]);
      IpcdCommand cmd = extractMessage(bus.take());
      Assert.assertEquals(CommandType.GetDeviceInfo.name(), cmd.getCommand());
      Assert.assertEquals("1000", cmd.getTxnid());
   }

   @Test
   public void testSendSetDeviceInfo() throws Exception {
      script.invokeMethod("sendSetDeviceInfo", new Object[0]);
      IpcdCommand cmd = extractMessage(bus.take());
      Assert.assertEquals(CommandType.SetDeviceInfo.name(), cmd.getCommand());
      Assert.assertEquals("1001", cmd.getTxnid());
      SetDeviceInfoCommand sdi = (SetDeviceInfoCommand)cmd;
      Map<String, Object> values = sdi.getValues();
      Assert.assertEquals("https://things.iot.net/ipcd", values.get("connectUrl"));
   }

   private IpcdCommand extractMessage(ProtocolMessage msg) {
      Assert.assertEquals(driverAddress, msg.getSource());
      Assert.assertEquals(protocolAddress, msg.getDestination());

      IpcdMessage ipcdMsg = msg.getValue(protocol);
      Assert.assertEquals(MessageType.command, ipcdMsg.getMessageType());

      return (IpcdCommand)ipcdMsg;
   }
}

