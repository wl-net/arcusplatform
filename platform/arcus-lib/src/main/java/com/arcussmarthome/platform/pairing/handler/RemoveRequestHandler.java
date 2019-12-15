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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.capability.DeviceCapability.ForceRemoveResponse;
import com.arcussmarthome.messages.capability.PairingDeviceCapability;
import com.arcussmarthome.messages.capability.PairingDeviceCapability.ForceRemoveRequest;
import com.arcussmarthome.messages.capability.PairingDeviceCapability.RemoveRequest;
import com.arcussmarthome.messages.capability.PairingDeviceCapability.RemoveResponse;
import com.arcussmarthome.messages.listener.annotation.Request;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.model.serv.PairingDeviceMockModel;
import com.arcussmarthome.messages.model.serv.PairingDeviceModel;
import com.arcussmarthome.platform.model.PersistentModelWrapper;
import com.arcussmarthome.platform.pairing.PairingDevice;
import com.arcussmarthome.platform.pairing.PairingDeviceMock;
import com.arcussmarthome.platform.pairing.PairingRemovalUtils;
import com.arcussmarthome.platform.pairing.ProductLoader;
import com.arcussmarthome.prodcat.ProductCatalogEntry;
import com.arcussmarthome.protocol.Protocol;
import com.arcussmarthome.protocol.Protocols;
import com.arcussmarthome.protocol.RemoveProtocolRequest;

@Singleton
public class RemoveRequestHandler {
	private static final Logger logger = LoggerFactory.getLogger(RemoveRequestHandler.class);
	
	private final PlatformMessageBus bus;
	private final ProductLoader loader;
	
	
	@Inject
	public RemoveRequestHandler(
			PlatformMessageBus bus,
			ProductLoader loader
	) {
		this.bus = bus;
		this.loader = loader;
	}
	
	@Request(RemoveRequest.NAME)
	public MessageBody remove(Place place, PersistentModelWrapper<PairingDevice> context) {
		List<Map<String, Object>> steps = loadRemovalSteps(place, context);
		sendRemoveRequest(place, context, false);
		return 
			RemoveResponse.builder()
				.withSteps(steps)
				.withMode(determineMode(context.model()))
				.build();
	}	

	@Request(ForceRemoveRequest.NAME)
	public MessageBody forceRemove(Place place, PersistentModelWrapper<PairingDevice> context) {
		sendRemoveRequest(place, context, true);
		// always delete the pairing entry on a force remove
		context.delete();
		return ForceRemoveResponse.instance();
	}

	private void sendRemoveRequest(Place place, PersistentModelWrapper<PairingDevice> context, boolean force) {
		String deviceAddressStr = PairingDeviceModel.getDeviceAddress(context.model());
		if(StringUtils.isNotBlank(deviceAddressStr)) {
			//Send request to driver-services
			if(force) {
				PlatformMessage request =
              		PlatformMessage
                 		.request(Address.fromString(deviceAddressStr))
                 		.from(context.model().getAddress())
                 		.withPayload(DeviceCapability.ForceRemoveRequest.instance())
                 		.create();
				bus.send(request);
			}else{
				PlatformMessage request =
              		PlatformMessage
                 		.request(Address.fromString(deviceAddressStr))
                 		.from(context.model().getAddress())
                 		.withPayload(DeviceCapability.RemoveRequest.builder().build())  //did not specify a timeout
                 		.create();
				bus.send(request);
			}
		}else{
			RemoveProtocolRequest request = new RemoveProtocolRequest();
			request.setBridgeChild(false); // FIXME is there anyway to detect this without loading the device object?
			request.setForceRemove(force);
			request.setAccountId(place.getAccount());
			request.setPlaceId(place.getId());
			request.setProtocolAddress(context.model().getProtocolAddress());
			request.setSourceAddress(context.model().getAddress());
			request.setTimeoutMs(30000);
			Protocol<?> protocol = Protocols.getProtocolByName(context.model().getProtocolAddress().getProtocolName());
			PlatformMessage removeRequest = protocol.remove(request);
			bus.send(removeRequest);
		}
	}

	private List<Map<String, Object>> loadRemovalSteps(Place place, PersistentModelWrapper<PairingDevice> context) {
		PairingDevice model = context.model();
		String productAddress = PairingDeviceModel.getProductAddress(model, "");
		if (productAddress.isEmpty()) {
			if (model instanceof PairingDeviceMock) {
				productAddress = PairingDeviceMockModel.getTargetProductAddress(model, "");
			}
			else {
				return PairingRemovalUtils.createDefaultRemovalStep();
			}
		}
		try {
			ProductCatalogEntry entry = loader.get(place, Address.fromString(productAddress));
			return PairingRemovalUtils.loadRemovalSteps(entry);
		}
		catch(Exception e) {
			logger.warn("Unable to load removal steps for product [{}]", productAddress, e);
			return PairingRemovalUtils.createDefaultRemovalStep();
		}
	}

	private String determineMode(PairingDevice model) {
		String mode = model.getRemoveMode();
		switch (mode) {
			case PairingDeviceCapability.REMOVEMODE_CLOUD: return RemoveResponse.MODE_CLOUD;
			case PairingDeviceCapability.REMOVEMODE_HUB_AUTOMATIC: return RemoveResponse.MODE_HUB_AUTOMATIC;
			case PairingDeviceCapability.REMOVEMODE_HUB_MANUAL: return RemoveResponse.MODE_HUB_MANUAL;
		}
		logger.warn("This should not be happpen, but the removeMode in pairingDevice is null for [{}]", model.getId());
		return null;
	}

}

