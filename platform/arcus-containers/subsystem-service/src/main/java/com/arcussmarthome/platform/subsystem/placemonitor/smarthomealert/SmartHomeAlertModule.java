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
package com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert;

import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.core.template.TemplateModule;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.generators.CellModemNeededGenerator;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.generators.CellServiceErrorGenerator;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.generators.DeviceLowBatteryGenerator;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.generators.DeviceOfflineGenerator;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.generators.DoorObstructionGenerator;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.generators.HubOfflineGenerator;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.generators.LockJamGenerator;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.postprocessors.ObscureIfHubOfflinePostProcessor;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.postprocessors.OfflineBatteryPostProcessor;
import com.arcussmarthome.prodcat.ProductCatalogModule;
import com.netflix.governator.annotations.Modules;

@Modules(include={
   ProductCatalogModule.class,
   TemplateModule.class
})
public class SmartHomeAlertModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      // generators
      bindListToInstancesOf(AlertGenerator.class);
      bind(CellModemNeededGenerator.class);
      bind(CellServiceErrorGenerator.class);
      bind(DeviceLowBatteryGenerator.class);
      bind(DeviceOfflineGenerator.class);
      bind(DoorObstructionGenerator.class);
      bind(HubOfflineGenerator.class);
      bind(LockJamGenerator.class);

      // postprocessors
      bindListToInstancesOf(AlertPostProcessor.class);
      bind(ObscureIfHubOfflinePostProcessor.class);
      bind(OfflineBatteryPostProcessor.class);
   }
}

