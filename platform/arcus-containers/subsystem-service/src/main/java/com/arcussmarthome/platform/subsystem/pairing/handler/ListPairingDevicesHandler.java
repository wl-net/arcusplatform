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
package com.arcussmarthome.platform.subsystem.pairing.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.inject.Singleton;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.ListPairingDevicesRequest;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.ListPairingDevicesResponse;
import com.arcussmarthome.messages.listener.annotation.Request;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.ModelStore;
import com.arcussmarthome.platform.subsystem.pairing.PairingUtils;

@Singleton
public class ListPairingDevicesHandler {

	@Request(ListPairingDevicesRequest.NAME)
	public MessageBody listPairingDevices(ModelStore store) {
		List<Map<String, Object>> devices = new ArrayList<>();
		for(Model model: store.getModels(PairingUtils.isPairingDevice())) {
			if(PairingUtils.isPairingDevice(model)) {
				devices.add(model.toMap());
			}
		}
		
		return 
			ListPairingDevicesResponse
				.builder()
				.withDevices(devices)
				.build();
	}
}

