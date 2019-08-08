/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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
package com.iris.agent.zigbee;

import com.zsmartsystems.zigbee.ZigBeeNetworkManager;
import com.zsmartsystems.zigbee.dongle.ember.ZigBeeDongleEzsp;
import com.zsmartsystems.zigbee.transport.ZigBeePort;
import com.zsmartsystems.zigbee.transport.ZigBeeTransportTransmit;
import com.zsmartsystems.zigbee.serial.ZigBeeSerialPort;

public class ZigbeeController {

   public void connect() {
      ZigBeePort port = new ZigBeeSerialPort("/dev/ttyUSB1", 56700, ZigBeePort.FlowControl.FLOWCONTROL_OUT_XONOFF);

      final ZigBeeTransportTransmit dongle = new ZigBeeDongleEzsp(port);

      ZigBeeNetworkManager networkManager = new ZigBeeNetworkManager(dongle);

      networkManager.initialize();
   }
}
