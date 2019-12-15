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
/**
 * 
 */
package com.arcussmarthome.platform.pairing.handler;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.name.Named;
import com.arcussmarthome.device.attributes.AttributeKey;
import com.arcussmarthome.device.attributes.AttributeMap;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.MessageConstants;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.PairingDeviceCapability;
import com.arcussmarthome.messages.capability.PairingDeviceMockCapability.UpdatePairingPhaseRequest;
import com.arcussmarthome.messages.capability.PairingDeviceMockCapability.UpdatePairingPhaseResponse;
import com.arcussmarthome.messages.errors.ErrorEventException;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.listener.annotation.Request;
import com.arcussmarthome.messages.model.serv.PairingDeviceMockModel;
import com.arcussmarthome.messages.model.serv.PairingDeviceModel;
import com.arcussmarthome.messages.service.DeviceService;
import com.arcussmarthome.platform.model.PersistentModelWrapper;
import com.arcussmarthome.platform.pairing.PairingDevice;
import com.arcussmarthome.protocol.constants.MockConstants;
import com.arcussmarthome.protocol.mock.MockProtocol;

/**
 * @author tweidlin
 *
 */
public class UpdatePairingPhaseRequestHandler {
	private static final Logger logger = LoggerFactory.getLogger(UpdatePairingPhaseRequestHandler.class);
	
	private static final List<String> OrderedPairingPhases =
			ImmutableList.of(
					PairingDeviceCapability.PAIRINGPHASE_JOIN, 
					PairingDeviceCapability.PAIRINGPHASE_CONNECT, 
					PairingDeviceCapability.PAIRINGPHASE_IDENTIFY, 
					PairingDeviceCapability.PAIRINGPHASE_PREPARE,
					PairingDeviceCapability.PAIRINGPHASE_CONFIGURE
			);

	@Request(UpdatePairingPhaseRequest.NAME)
	public MessageBody updatePairingPhase(PersistentModelWrapper<PairingDevice> context, @Named(UpdatePairingPhaseRequest.ATTR_PHASE) String phase) {
		String currentPhase = PairingDeviceModel.getPairingPhase(context.model(), "");
		if(currentPhase.equals(phase)) {
			return UpdatePairingPhaseResponse.instance();
		}
		
		int curr = OrderedPairingPhases.indexOf(currentPhase);
		if(curr == -1) {
			throw new ErrorEventException(Errors.invalidRequest(String.format("%s is a terminal phase, can't be updated any longer", currentPhase)));
		}
		
		int dest;
		if(StringUtils.isEmpty(phase)) {
			dest = curr + 1;
		}
		else {
			dest = OrderedPairingPhases.indexOf(phase);
		}
		if(dest != -1 && dest < curr) {
			throw new ErrorEventException(Errors.invalidRequest(String.format("Can't go from %s to %s", PairingDeviceModel.getPairingState(context.model()), phase)));
		}
		else if(dest == OrderedPairingPhases.size() || PairingDeviceCapability.PAIRINGPHASE_PAIRED.equals(phase)) {
			// the actual change to PAIRED should happen when the device is added
			tryCreateDevice(context);
		}
		else if(PairingDeviceCapability.PAIRINGPHASE_FAILED.equals(phase)) {
			PairingDeviceModel.setPairingState(context.model(), PairingDeviceCapability.PAIRINGSTATE_MISPAIRED);
			PairingDeviceModel.setPairingPhase(context.model(), PairingDeviceCapability.PAIRINGPHASE_FAILED);
		}
		else {
			PairingDeviceModel.setPairingPhase(context.model(), OrderedPairingPhases.get(dest));
		}
		context.save();
		return UpdatePairingPhaseResponse.instance();
	}

	private void tryCreateDevice(PersistentModelWrapper<PairingDevice> context) {
		logger.debug("Attempting to create a mock for [{}]", context.model().getAddress());
		
		Address productAddress = Address.fromString(PairingDeviceMockModel.getTargetProductAddress(context.model()));
		AttributeMap protocolAttributes = AttributeMap.newMap();
		protocolAttributes.add(
			AttributeKey
				.create(MockConstants.ATTR_PRODUCTID, String.class)
				.valueOf((String) productAddress.getId())
		);
		MessageBody addDevice =
			MessageBody
				.messageBuilder(MessageConstants.MSG_ADD_DEVICE_REQUEST)
				.withAttribute("protocolName", MockProtocol.NAMESPACE)
				.withAttribute("deviceId", context.model().getProtocolAddress().getProtocolDeviceId().getRepresentation())
				.withAttribute("placeId", context.model().getPlaceId().toString())
				.withAttribute("protocolAttributes", protocolAttributes)
				.create();
		
		context.send(DeviceService.ADDRESS, addDevice);
	}
}

