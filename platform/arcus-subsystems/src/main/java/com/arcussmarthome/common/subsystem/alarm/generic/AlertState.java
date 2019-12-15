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
package com.arcussmarthome.common.subsystem.alarm.generic;

import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AlarmCapability;
import com.arcussmarthome.messages.model.subs.SubsystemModel;

public class AlertState extends AlarmState<SubsystemModel> {
	private static final AlertState INSTANCE = new AlertState();

	public static AlertState instance() {
		return INSTANCE;
	}
	
	protected AlertState() {
	}

	@Override
	public String getName() {
		return AlarmCapability.ALERTSTATE_ALERT;
	}

	@Override
	public void onExit(SubsystemContext<? extends SubsystemModel> context, String name) {
		clearTriggers(context, name);
		super.onExit(context, name);
	}

	@Override
	public String onSensorTriggered(SubsystemContext<? extends SubsystemModel> context, String name, Address sensor, TriggerEvent trigger) {
		addTrigger(context, name, sensor, trigger);
		return AlarmCapability.ALERTSTATE_ALERT;
	}

	@Override
	public String onTriggered(SubsystemContext<? extends SubsystemModel> context, String name, Address triggeredBy, TriggerEvent trigger) {
		addTrigger(context, name, triggeredBy, trigger);
		return AlarmCapability.ALERTSTATE_ALERT;
	}

	@Override
	public String cancel(SubsystemContext<? extends SubsystemModel> context, String name, PlatformMessage message) {
		// TODO add cancelledTime, cancelledBy, cancelledFrom?
		return AlarmCapability.ALERTSTATE_CLEARING;
	}

}

