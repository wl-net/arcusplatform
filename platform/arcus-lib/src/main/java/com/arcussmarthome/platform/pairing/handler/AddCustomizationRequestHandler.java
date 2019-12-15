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
package com.arcussmarthome.platform.pairing.handler;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.capability.PairingDeviceCapability.AddCustomizationRequest;
import com.arcussmarthome.messages.capability.PairingDeviceCapability.AddCustomizationResponse;
import com.arcussmarthome.messages.listener.annotation.Request;
import com.arcussmarthome.messages.model.serv.PairingDeviceModel;
import com.arcussmarthome.platform.model.PersistentModelWrapper;
import com.arcussmarthome.platform.pairing.PairingDevice;

@Singleton
public class AddCustomizationRequestHandler {

	@Request(AddCustomizationRequest.NAME)
	public MessageBody dismiss(PersistentModelWrapper<PairingDevice> context, @Named(AddCustomizationRequest.ATTR_CUSTOMIZATION) String customization) {
		Set<String> customizations = new HashSet<>(PairingDeviceModel.getCustomizations(context.model(), ImmutableSet.of()));
		// FIXME verify this is a recognized customization?
		customizations.add(customization);
		PairingDeviceModel.setCustomizations(context.model(), customizations);
		context.save();
		return AddCustomizationResponse.instance();
	}
}

