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
package com.arcussmarthome.bus;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.arcussmarthome.bootstrap.Bootstrap;
import com.arcussmarthome.bootstrap.BootstrapException;
import com.arcussmarthome.bootstrap.ServiceLocator;
import com.arcussmarthome.bootstrap.guice.GuiceServiceLocator;
import com.arcussmarthome.core.messaging.MessagesModule;
import com.arcussmarthome.core.messaging.kafka.KafkaModule;
import com.arcussmarthome.core.protocol.ProtocolMessageBus;
import com.arcussmarthome.io.json.gson.GsonModule;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.protocol.ipcd.IpcdProtocol;
import com.arcussmarthome.protocol.ipcd.message.model.Device;
import com.arcussmarthome.protocol.ipcd.message.model.IpcdEvent;
import com.arcussmarthome.protocol.ipcd.message.model.SetParameterValuesCommand;
import com.arcussmarthome.protocol.ipcd.message.model.ValueChange;

public class ProtocolPush {

   public static void main(String[] args) {
      ProtocolPush pusher = new ProtocolPush();
      pusher.sendMessage();
   }

   @SuppressWarnings("unchecked")
   public void sendMessage() {
      File propFile = new File(System.getProperty("user.dir"), "src/test/java/com/iris/bus/protocol.properties");
      Bootstrap bootstrap = Bootstrap.builder()
               .withModuleClasses(GsonModule.class, MessagesModule.class, KafkaModule.class)
               .withConfigFiles(propFile)
               .build();
      try {
         ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
      } catch (BootstrapException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      Device ipcdDevice = new Device();
      ipcdDevice.setIpcdver("0.3");
      ipcdDevice.setVendor("BlackBox");
      ipcdDevice.setModel("Switch1");
      ipcdDevice.setSn("123456789");

      Map<String, Object> values = new HashMap<>();
      values.put("bb.switch", "on");

      SetParameterValuesCommand setParametersCommand = new SetParameterValuesCommand();
      setParametersCommand.setValues(values);

      String deviceId = getClientId(ipcdDevice);
      //TODO: Fix Test
      ProtocolMessage protocolMessage = ProtocolMessage.createProtocolMessage(
            Address.platformDriverAddress(UUID.randomUUID()),
            Address.protocolAddress("IPCD", deviceId),
            IpcdProtocol.INSTANCE,
            setParametersCommand
      );

      ProtocolMessageBus protocolMessageBus = ServiceLocator.getInstance(ProtocolMessageBus.class);
      protocolMessageBus.send(protocolMessage);

      ServiceLocator.destroy();
   }

   protected IpcdEvent createValueChangeEvent(Device ipcdDevice, List<ValueChange> changes) {
      IpcdEvent action = new IpcdEvent();
      action.setDevice(ipcdDevice);
      action.setEvents(Arrays.asList("onValueChange"));
      action.setValueChanges(changes);
      return action;
   }

   private String getClientId(Device device) {
      return device.getVendor() + '-' + device.getModel() + '-' + device.getSn();
   }
}

