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
package com.arcussmarthome.platform.subsystem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.common.scheduler.ExecutorScheduler;
import com.arcussmarthome.common.scheduler.Scheduler;
import com.arcussmarthome.common.subsystem.Subsystem;
import com.arcussmarthome.common.subsystem.SubsystemContext.ResponseAction;
import com.arcussmarthome.common.subsystem.alarm.AlarmSubsystem;
import com.arcussmarthome.common.subsystem.cameras.CamerasSubsystem;
import com.arcussmarthome.common.subsystem.care.CareSubsystem;
import com.arcussmarthome.common.subsystem.climate.ClimateSubsystem;
import com.arcussmarthome.common.subsystem.doorsnlocks.DoorsNLocksSubsystem;
import com.arcussmarthome.common.subsystem.lawnngarden.LawnNGardenSubsystem;
import com.arcussmarthome.common.subsystem.lightsnswitches.LightsNSwitchesSubsystem;
import com.arcussmarthome.common.subsystem.presence.PresenceSubsystem;
import com.arcussmarthome.common.subsystem.safety.SafetySubsystem;
import com.arcussmarthome.common.subsystem.security.SecuritySubsystem;
import com.arcussmarthome.common.subsystem.water.WaterSubsystem;
import com.arcussmarthome.common.subsystem.weather.WeatherSubsystem;
import com.arcussmarthome.core.dao.PopulationDAO;
import com.arcussmarthome.core.dao.file.PopulationManager;
import com.arcussmarthome.messages.event.AddressableEvent;
import com.arcussmarthome.messages.event.Listener;
import com.arcussmarthome.platform.subsystem.cellbackup.CellBackupNotifications;
import com.arcussmarthome.platform.subsystem.cellbackup.CellBackupSubsystem;
import com.arcussmarthome.platform.subsystem.impl.CachingSubsystemRegistry;
import com.arcussmarthome.platform.subsystem.impl.PlatformSubsystemFactory;
import com.arcussmarthome.platform.subsystem.incident.AlarmIncidentServiceImpl;
import com.arcussmarthome.platform.subsystem.pairing.PairingSubsystem;
import com.arcussmarthome.platform.subsystem.placemonitor.PlaceMonitorHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.PlaceMonitorNotifications;
import com.arcussmarthome.platform.subsystem.placemonitor.PlaceMonitorSubsystem;
import com.arcussmarthome.platform.subsystem.placemonitor.defaultrules.DefaultRuleHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.offlinenotifications.CriticalBatteryNotificationsHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.offlinenotifications.DeadBatteryNotificationsHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.offlinenotifications.FullBatteryNotificationsHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.offlinenotifications.LowBatteryNotificationsHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.offlinenotifications.OfflineNotificationsHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.offlinenotifications.VeryLowBatteryNotificationsHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.ota.DeviceOTAHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.pairing.BridgeDeviceAddHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.pairing.PlacePairingModeHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertHandler;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertModule;
import com.arcussmarthome.platform.util.LazyReference;
import com.arcussmarthome.util.IrisCorrelator;
import com.arcussmarthome.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.Modules;

@Modules(include={
      SmartHomeAlertModule.class
   })
public class SubsystemModule extends AbstractIrisModule {
	private SubsystemConfig config;
	private LazyReference<ExecutorService> executorRef = 
			LazyReference.fromCallable(() -> {
				return
		            new ThreadPoolBuilder()
		               .withBlockingBacklog()
		               .withMaxPoolSize(config.getMaxThreads())
		               .withKeepAliveMs(config.getThreadKeepAliveMs())
		               .withNameFormat("subsystem-service-%d")
		               .withMetrics("subsystem.service")
		               .build()
		               ;
			});
	
   @Override
   protected void configure() {
      bind(SubsystemServiceImpl.class).asEagerSingleton();
      bind(SubsystemLoader.class).asEagerSingleton();
      bind(SubsystemCatalog.class).asEagerSingleton();
      bind(SubsystemRegistry.class).to(CachingSubsystemRegistry.class);
      bind(SubsystemFactory.class).to(PlatformSubsystemFactory.class);
      bind(PopulationDAO.class).to(PopulationManager.class);

      bind(new Key<Listener<AddressableEvent>>() {}).to(SubsystemServiceImpl.class);
      bind(new Key<IrisCorrelator<ResponseAction<?>>>() {}).toInstance(new IrisCorrelator<>());

      Multibinder<Subsystem<?>> subsystems = bindSetOf(new Key<Subsystem<?>>() {}.getTypeLiteral());
      subsystems.addBinding().to(SafetySubsystem.class);
      subsystems.addBinding().to(SecuritySubsystem.class);
      subsystems.addBinding().to(ClimateSubsystem.class).asEagerSingleton();
      subsystems.addBinding().to(DoorsNLocksSubsystem.class);
      subsystems.addBinding().to(CamerasSubsystem.class);
      subsystems.addBinding().to(PlaceMonitorSubsystem.class);
      subsystems.addBinding().to(PresenceSubsystem.class);
      subsystems.addBinding().to(LightsNSwitchesSubsystem.class);
      subsystems.addBinding().to(WaterSubsystem.class);
      subsystems.addBinding().to(CareSubsystem.class);
      subsystems.addBinding().to(LawnNGardenSubsystem.class);
      subsystems.addBinding().to(CellBackupSubsystem.class);
      subsystems.addBinding().to(WeatherSubsystem.class);
      subsystems.addBinding().to(AlarmSubsystem.class);
      subsystems.addBinding().to(PairingSubsystem.class);

      MapBinder<String,PlaceMonitorHandler> handlers = MapBinder.newMapBinder(binder(),new TypeLiteral<String>(){},new TypeLiteral<PlaceMonitorHandler>(){});
      handlers.addBinding(DeviceOTAHandler.class.getName()).to(DeviceOTAHandler.class);
      handlers.addBinding(DefaultRuleHandler.class.getName()).to(DefaultRuleHandler.class);
      handlers.addBinding(OfflineNotificationsHandler.class.getName()).to(OfflineNotificationsHandler.class);
      handlers.addBinding(FullBatteryNotificationsHandler.class.getName()).to(FullBatteryNotificationsHandler.class);
      handlers.addBinding(LowBatteryNotificationsHandler.class.getName()).to(LowBatteryNotificationsHandler.class);
      handlers.addBinding(VeryLowBatteryNotificationsHandler.class.getName()).to(VeryLowBatteryNotificationsHandler.class);
      handlers.addBinding(CriticalBatteryNotificationsHandler.class.getName()).to(CriticalBatteryNotificationsHandler.class);
      handlers.addBinding(DeadBatteryNotificationsHandler.class.getName()).to(DeadBatteryNotificationsHandler.class);
      handlers.addBinding(PlacePairingModeHandler.class.getName()).to(PlacePairingModeHandler.class);
      handlers.addBinding(BridgeDeviceAddHandler.class.getName()).to(BridgeDeviceAddHandler.class);
      handlers.addBinding(SmartHomeAlertHandler.class.getName()).to(SmartHomeAlertHandler.class);

      bind(PlaceMonitorNotifications.class).asEagerSingleton();
      bind(CellBackupNotifications.class).asEagerSingleton();

   }

   @Provides
   public Scheduler scheduler(SubsystemConfig config) {
      ScheduledExecutorService executor =
            Executors
               .newScheduledThreadPool(
                     config.getSchedulerThreads(),
                     ThreadPoolBuilder
                        .defaultFactoryBuilder()
                        .setNameFormat("subsystem-scheduler-%d")
                        .build()
               );
      return new ExecutorScheduler(executor);
   }
   
   @Provides @Named(SubsystemLoader.NAME_EXECUTOR)
   public ExecutorService subsystemLoaderExecutor(SubsystemConfig config) {
   	this.config = config;
   	return executorRef.get();
   }
   
   @Provides @Named(SubsystemServiceImpl.NAME_EXECUTOR)
   public ExecutorService subsystemServiceExecutor(SubsystemConfig config) {
   	this.config = config;
   	return executorRef.get();
   }

    @Provides @Named(AlarmIncidentServiceImpl.NAME_EXECUTOR_POOL)
    public ExecutorService incidentServiceExecutor(SubsystemConfig config) {
        this.config = config;
        return executorRef.get();
    }
}

