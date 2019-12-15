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
package com.arcussmarthome.driver.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.capability.registry.CapabilityRegistry;
import com.arcussmarthome.capability.registry.CapabilityRegistryModule;
import com.arcussmarthome.common.scheduler.ExecutorScheduler;
import com.arcussmarthome.common.scheduler.Scheduler;
import com.arcussmarthome.core.dao.EmptyResourceBundle;
import com.arcussmarthome.core.dao.ResourceBundleDAO;
import com.arcussmarthome.driver.DeviceDriver;
import com.arcussmarthome.driver.Drivers;
import com.arcussmarthome.driver.platform.PlatformDriverExecutorRegistry;
import com.arcussmarthome.driver.platform.PlatformDriverService;
import com.arcussmarthome.driver.service.executor.DriverExecutorRegistry;
import com.arcussmarthome.driver.service.handler.DriverServiceRequestHandler;
import com.arcussmarthome.driver.service.handler.MessageHandler;
import com.arcussmarthome.driver.service.handler.UpgradeDriverRequestHandler;
import com.arcussmarthome.driver.service.registry.CompositeDriverRegistry;
import com.arcussmarthome.driver.service.registry.DriverRegistry;
import com.arcussmarthome.driver.service.registry.MapDriverRegistry;
import com.arcussmarthome.messages.MessageConstants;
import com.arcussmarthome.messages.address.AddressMatcher;
import com.arcussmarthome.messages.address.AddressMatchers;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.model.DriverId;
import com.arcussmarthome.messages.type.Population;
import com.arcussmarthome.model.Version;
import com.arcussmarthome.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.Modules;

@Modules(include = CapabilityRegistryModule.class)
public class TestDriverModule extends AbstractIrisModule {
   @Override
   protected void configure() {
      bind(DriverConfig.class);
      // required to inject drivers into ServiceLocatorDriverRegistry
      bindMapToInstancesOf(
            new TypeLiteral<Map<DriverId, DeviceDriver>> () {},
            new Function<DeviceDriver, DriverId>() {
               @Override
               public DriverId apply(DeviceDriver driver) {
                  return driver.getDriverId();
               }
            }
      );
      bind(PlatformDriverService.class)
         .asEagerSingleton();
      bind(ResourceBundleDAO.class)
         .to(EmptyResourceBundle.class);

      Multibinder<DriverServiceRequestHandler> handlers = bindSetOf(DriverServiceRequestHandler.class);
      handlers.addBinding().to(UpgradeDriverRequestHandler.class);
      handlers.addBinding().to(MessageHandler.class);

      bindSetOf(DriverRegistry.class)
         .addBinding()
         .to(MapDriverRegistry.class);

      bind(DriverExecutorRegistry.class).to(PlatformDriverExecutorRegistry.class);
   }
   
   @Provides @Singleton
   public Scheduler scheduler() {
      return new ExecutorScheduler(Executors.newScheduledThreadPool(1));
   }

   @Provides @Singleton @Named(DriverConfig.NAMED_EXECUTOR)
   public ThreadPoolExecutor driverExecutor(DriverConfig config) {
      return
            new ThreadPoolBuilder()
               .withBlockingBacklog()
               .withMaxPoolSize(config.getDriverThreadPoolSize())
               .withNameFormat("driver-thread-%d")
               .build()
               ;
   }
   
   @Provides @Singleton @Named("ProtocolMatchers")
   public Set<AddressMatcher> provideProtocolAddressMatchers() {
      return AddressMatchers.platformNamespaces(MessageConstants.BROADCAST, MessageConstants.DRIVER);
   }

   @Provides @Singleton @Named("PlatformMatchers")
   public Set<AddressMatcher> providePlatformAddressMatchers() {
      return AddressMatchers.platformNamespaces(MessageConstants.DRIVER);
   }

   @Provides @Singleton
   public DriverRegistry provideDriverRegistry(Set<DriverRegistry> registries) {
      return new CompositeDriverRegistry(registries.toArray(new DriverRegistry[registries.size()]));
   }
   
   @Provides
   @Named("Fallback")
   public DeviceDriver fallbackDriver(CapabilityRegistry registry) {
      return 
            Drivers
               .builder()
               .withName("Fallback")
               .withVersion(Version.fromRepresentation("1.0"))
               .withMatcher((a) -> false)
               .withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL, Population.NAME_BETA, Population.NAME_QA))
               .addCapabilityDefinition(registry.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE))
               .create(true)
               ;
   }
}

