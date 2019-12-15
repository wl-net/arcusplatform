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
package com.arcussmarthome.common.subsystem.alarm.subs;

import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.common.subsystem.alarm.security.SecurityAlarm;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AlarmSubsystemCapability;
import com.arcussmarthome.messages.capability.SecuritySubsystemCapability;
import com.arcussmarthome.messages.model.subs.AlarmSubsystemModel;
import com.arcussmarthome.messages.model.subs.SecurityAlarmModeModel;

public class PreAlertState extends AlarmSubsystemState {
	private static final PreAlertState INSTANCE = new PreAlertState();
	
	public static PreAlertState instance() {
		return INSTANCE;
	}
	
	private PreAlertState() {
		
	}

	@Override
	public Name getName() {
		return Name.PREALERT;
	}

	@Override
	public Name onEnter(SubsystemContext<AlarmSubsystemModel> context) {
		context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_PREALERT);
		String mode = context.model().getSecurityMode();
		int prealertDelay = SecurityAlarmModeModel.getEntranceDelaySec(mode, context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE)));
		sendPreAlert(context, mode, prealertDelay);
		return super.onEnter(context);
	}

	@Override
	public Name onAlert(SubsystemContext<AlarmSubsystemModel> context, String alarm) {
		return Name.ALERT;
	}

	@Override
	public Name onAlertInactive(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		if(SecurityAlarm.NAME.equals(alert)) {
			return Name.DISARMED;
		}
		else {
			return Name.PREALERT;
		}
	}

	@Override
	public Name onAlertReady(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		if(SecurityAlarm.NAME.equals(alert)) {
			return Name.ARMED;
		}
		else {
			return Name.PREALERT;
		}
	}

	@Override
	public Name onAlertClearing(SubsystemContext<AlarmSubsystemModel> context, String alert) {
		if(SecurityAlarm.NAME.equals(alert)) {
			return Name.DISARMED;
		}
		else {
			return Name.PREALERT;
		}
	}

	@Override
	public Name onDisarmed(SubsystemContext<AlarmSubsystemModel> context) {
		return Name.DISARMED;
	}

}

