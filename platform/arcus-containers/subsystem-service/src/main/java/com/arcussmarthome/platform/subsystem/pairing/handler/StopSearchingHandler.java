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

import com.google.inject.Singleton;
import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.StopSearchingRequest;
import com.arcussmarthome.messages.capability.PairingSubsystemCapability.StopSearchingResponse;
import com.arcussmarthome.messages.listener.annotation.Request;
import com.arcussmarthome.messages.model.subs.PairingSubsystemModel;
import com.arcussmarthome.platform.subsystem.pairing.state.PairingStateMachine;

@Singleton
public class StopSearchingHandler {

	
	@Request(StopSearchingRequest.NAME)
	public MessageBody stopSearching(SubsystemContext<PairingSubsystemModel> context) {
		PairingStateMachine.get(context).stopPairing();
		return StopSearchingResponse.instance();
	}
}

