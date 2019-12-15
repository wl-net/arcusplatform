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
package com.arcussmarthome.platform.subsystem.placemonitor;

import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.messages.event.ModelReportEvent;
import com.arcussmarthome.messages.event.ScheduledEvent;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.subs.PlaceMonitorSubsystemModel;

public interface PlaceMonitorHandler {
   
   void onAdded(SubsystemContext<PlaceMonitorSubsystemModel> context);
   void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context);
   void onDeviceAdded(Model model,SubsystemContext<PlaceMonitorSubsystemModel> context);
   void onDeviceRemoved(Model model,SubsystemContext<PlaceMonitorSubsystemModel> context);
   void onScheduledEvent(ScheduledEvent event, SubsystemContext<PlaceMonitorSubsystemModel> context);
   void onConnectivityChange(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context);
   void onHubConnectivityChange(Model hub, SubsystemContext<PlaceMonitorSubsystemModel> context);
   void onHubReportEvent(ModelReportEvent event, Model hub, SubsystemContext<PlaceMonitorSubsystemModel> context);
   void onDeviceBatteryChange(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context);
}

