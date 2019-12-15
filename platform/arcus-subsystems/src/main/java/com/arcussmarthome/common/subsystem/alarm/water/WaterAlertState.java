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
package com.arcussmarthome.common.subsystem.alarm.water;

import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.common.subsystem.alarm.AlarmUtil;
import com.arcussmarthome.common.subsystem.alarm.generic.AlertState;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AlarmCapability;
import com.arcussmarthome.messages.model.subs.SubsystemModel;

public class WaterAlertState extends AlertState {
	private static final WaterAlertState INSTANCE = new WaterAlertState();

	public static WaterAlertState instance() {
		return INSTANCE;
	}
	
	
	@Override
	public String onEnter(SubsystemContext<? extends SubsystemModel> context, String name) {
		AlarmUtil.shutoffValvesIfNeeded(context);
		return AlarmCapability.ALERTSTATE_ALERT;
	}
	
	// even if its the same event, shut 'em all down again if a new leak is detected
	@Override
	public String onSensorTriggered(SubsystemContext<? extends SubsystemModel> context, String name, Address sensor, TriggerEvent trigger) {
		AlarmUtil.shutoffValvesIfNeeded(context);
		addTrigger(context, name, sensor, trigger);
		return AlarmCapability.ALERTSTATE_ALERT;
	}

}

