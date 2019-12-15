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
package com.arcussmarthome.platform.history.appender;

import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.platform.history.appender.devvc.DeviceButtonAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceCOAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceContactAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceDimmerAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceDoorLockAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceFanAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceGasLeakAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceGlassAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceMotionAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceMotorizedDoorAppender;
import com.arcussmarthome.platform.history.appender.devvc.DevicePetDoorAppender;
import com.arcussmarthome.platform.history.appender.devvc.DevicePowerAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceSmokeAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceSwitchAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceThermostatAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceTiltAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceValveAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceWaterLeakAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceWaterSoftenerAppender;

public class TestDeviceAppenderModule extends AbstractIrisModule {

	@Override
   protected void configure() {
	   bind(DeviceButtonAppender.class);
	   bind(DeviceCOAppender.class);
	   bind(DeviceContactAppender.class);
	   bind(DeviceDimmerAppender.class);
	   bind(DeviceDoorLockAppender.class);
	   bind(DevicePetDoorAppender.class);
	   bind(DeviceFanAppender.class);
	   bind(DeviceGasLeakAppender.class);
	   bind(DeviceGlassAppender.class);
	   bind(DeviceMotionAppender.class);
	   bind(DeviceMotorizedDoorAppender.class);
	   bind(DevicePowerAppender.class);
	   bind(DeviceSmokeAppender.class);
	   bind(DeviceSwitchAppender.class);
	   bind(DeviceTiltAppender.class);
	   bind(DeviceValveAppender.class);
	   bind(DeviceWaterLeakAppender.class);
	   bind(DeviceWaterSoftenerAppender.class);
	   bind(DeviceThermostatAppender.class);
   }

}

