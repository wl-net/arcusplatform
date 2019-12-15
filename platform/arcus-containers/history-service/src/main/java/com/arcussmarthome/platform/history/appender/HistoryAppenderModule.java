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
package com.arcussmarthome.platform.history.appender;

import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.platform.history.appender.devvc.DeviceButtonAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceCOAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceContactAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceDimmerAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceDoorLockAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceFanAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceGlassAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceMotionAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceMotorizedDoorAppender;
import com.arcussmarthome.platform.history.appender.devvc.DevicePetDoorAppender;
import com.arcussmarthome.platform.history.appender.devvc.DevicePowerAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceSmokeAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceSpaceHeaterAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceSwitchAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceThermostatAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceTiltAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceValveAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceVentAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceWaterLeakAppender;
import com.arcussmarthome.platform.history.appender.devvc.DeviceWaterSoftenerAppender;
import com.arcussmarthome.platform.history.appender.hub.HubConnectionAppender;
import com.arcussmarthome.platform.history.appender.incident.AlertEventAppender;
import com.arcussmarthome.platform.history.appender.incident.HistoryAddedAppender;
import com.arcussmarthome.platform.history.appender.person.InvitationDeclinedAppender;
import com.arcussmarthome.platform.history.appender.person.PersonAddedAppender;
import com.arcussmarthome.platform.history.appender.person.PersonRemovedAppender;
import com.arcussmarthome.platform.history.appender.scene.SceneFiredAppender;
import com.arcussmarthome.platform.history.appender.subsys.CareSubsystemActivityAppender;
import com.arcussmarthome.platform.history.appender.subsys.CareSubsystemEventsAppender;
import com.arcussmarthome.platform.history.appender.subsys.DoorsNLocksSubsystemAppender;
import com.arcussmarthome.platform.history.appender.subsys.LawnNGardenSubsystemAppender;
import com.arcussmarthome.platform.history.appender.subsys.PlaceMonitorSubsystemAppender;
import com.arcussmarthome.platform.history.appender.subsys.PresenceSubsystemAppender;
import com.arcussmarthome.platform.history.appender.subsys.SafetySubsystemAppender;
import com.arcussmarthome.platform.history.appender.subsys.SecuritySubsystemEventsAppender;
import com.arcussmarthome.platform.history.appender.subsys.WaterSubsystemEventsAppender;
import com.arcussmarthome.platform.history.appender.video.VideoAddedAppender;

/**
 *
 */
public class HistoryAppenderModule extends AbstractIrisModule {

   /* (non-Javadoc)
    * @see com.google.inject.AbstractModule#configure()
    */
   @Override
   protected void configure() {
      bindListToInstancesOf(HistoryAppender.class);

      bind(DeviceEventAppender.class);
      bind(DevicePowerAppender.class);

      bind(DeviceButtonAppender.class);
      bind(DeviceCOAppender.class);
      bind(DeviceContactAppender.class);
      bind(DeviceDimmerAppender.class);
      bind(DeviceDoorLockAppender.class);
      bind(DevicePetDoorAppender.class);
      bind(DeviceFanAppender.class);
      bind(DeviceGlassAppender.class);
      bind(DeviceMotionAppender.class);
      bind(DeviceMotorizedDoorAppender.class);
      bind(DeviceSmokeAppender.class);
      bind(DeviceSwitchAppender.class);
      bind(DeviceThermostatAppender.class);
      bind(DeviceTiltAppender.class);
      bind(DeviceValveAppender.class);
      bind(DeviceVentAppender.class);
      bind(DeviceWaterLeakAppender.class);
      bind(DeviceWaterSoftenerAppender.class);
      
//      bind(HubBatteryAppender.class);
      bind(HubConnectionAppender.class);
//      bind(HubConnectionTypeAppender.class);

      bind(SecuritySubsystemEventsAppender.class);
      bind(SafetySubsystemAppender.class);
      bind(DoorsNLocksSubsystemAppender.class);
      bind(PresenceSubsystemAppender.class);
      bind(CareSubsystemActivityAppender.class);
      bind(CareSubsystemEventsAppender.class);
      bind(LawnNGardenSubsystemAppender.class);
      bind(WaterSubsystemEventsAppender.class);
      bind(PlaceMonitorSubsystemAppender.class);
      bind(SceneFiredAppender.class);
      bind(DeviceSpaceHeaterAppender.class);
      
      bind(AlertEventAppender.class);
      bind(HistoryAddedAppender.class);
      
      bind(VideoAddedAppender.class);
      bind(PersonAddedAppender.class);
      bind(PersonRemovedAppender.class);
      bind(InvitationDeclinedAppender.class);
      
   }


}

