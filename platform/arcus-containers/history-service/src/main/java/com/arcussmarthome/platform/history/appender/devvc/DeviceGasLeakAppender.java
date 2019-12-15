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
package com.arcussmarthome.platform.history.appender.devvc;

import static com.arcussmarthome.messages.capability.LeakGasCapability.ATTR_STATE;
import static com.arcussmarthome.messages.capability.LeakGasCapability.STATE_LEAK;
import static com.arcussmarthome.messages.capability.LeakGasCapability.STATE_SAFE;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.platform.history.HistoryAppenderDAO;
import com.arcussmarthome.platform.history.appender.AnnotatedDeviceValueChangeAppender;
import com.arcussmarthome.platform.history.appender.ObjectNameCache;
import com.arcussmarthome.platform.history.appender.annotation.AutoTranslate;
import com.arcussmarthome.platform.history.appender.annotation.EnumValue;
import com.arcussmarthome.platform.history.appender.annotation.Group;

@Singleton
@Group(DeviceCapability.NAMESPACE)
@AutoTranslate()
@EnumValue(attr=ATTR_STATE, val=STATE_LEAK, tpl="device.gasleak.detected", critical=true)
@EnumValue(attr=ATTR_STATE, val=STATE_SAFE, tpl="device.gasleak.none",     critical=false)
public class DeviceGasLeakAppender extends AnnotatedDeviceValueChangeAppender {
	
	@Inject
	public DeviceGasLeakAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache);
	}
}

