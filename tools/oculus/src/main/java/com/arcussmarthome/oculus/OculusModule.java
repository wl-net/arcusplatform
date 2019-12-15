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
package com.arcussmarthome.oculus;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;
import javax.swing.JMenu;

import com.google.inject.Provides;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.capability.definition.CapabilityDefinition;
import com.arcussmarthome.capability.definition.DefinitionRegistry;
import com.arcussmarthome.capability.definition.StaticDefinitionRegistry;
import com.arcussmarthome.client.IrisClient;
import com.arcussmarthome.client.IrisClientFactory;
import com.arcussmarthome.client.event.DefaultExecutor;
import com.arcussmarthome.client.impl.netty.NettyIrisClientFactory;
import com.arcussmarthome.oculus.menu.DevicesMenu;
import com.arcussmarthome.oculus.menu.HubsMenu;
import com.arcussmarthome.oculus.menu.PlaceMenu;
import com.arcussmarthome.oculus.menu.ServicesMenu;
import com.arcussmarthome.oculus.menu.SessionMenu;
import com.arcussmarthome.oculus.menu.WindowsMenu;
import com.arcussmarthome.oculus.modules.account.AccountController;
import com.arcussmarthome.oculus.modules.account.AccountSection;
import com.arcussmarthome.oculus.modules.behaviors.BehaviorSection;
import com.arcussmarthome.oculus.modules.capability.CapabilityController;
import com.arcussmarthome.oculus.modules.dashboard.DashboardController;
import com.arcussmarthome.oculus.modules.dashboard.DashboardSection;
import com.arcussmarthome.oculus.modules.device.DeviceController;
import com.arcussmarthome.oculus.modules.device.DeviceSection;
import com.arcussmarthome.oculus.modules.device.mockaction.MockActionsNexus;
import com.arcussmarthome.oculus.modules.hub.HubController;
import com.arcussmarthome.oculus.modules.hub.HubSection;
import com.arcussmarthome.oculus.modules.incident.IncidentController;
import com.arcussmarthome.oculus.modules.incident.IncidentSection;
import com.arcussmarthome.oculus.modules.pairing.PairingDeviceController;
import com.arcussmarthome.oculus.modules.pairing.PairingDeviceSection;
import com.arcussmarthome.oculus.modules.person.PersonController;
import com.arcussmarthome.oculus.modules.person.PersonSection;
import com.arcussmarthome.oculus.modules.place.PlaceController;
import com.arcussmarthome.oculus.modules.place.PlaceSection;
import com.arcussmarthome.oculus.modules.product.ProductController;
import com.arcussmarthome.oculus.modules.product.ProductSection;
import com.arcussmarthome.oculus.modules.rule.RuleController;
import com.arcussmarthome.oculus.modules.rule.RuleSection;
import com.arcussmarthome.oculus.modules.scene.SceneSection;
import com.arcussmarthome.oculus.modules.scheduler.SchedulerSection;
import com.arcussmarthome.oculus.modules.session.SessionController;
import com.arcussmarthome.oculus.modules.status.StatusController;
import com.arcussmarthome.oculus.modules.subsystem.SubsystemController;
import com.arcussmarthome.oculus.modules.subsystem.SubsystemSection;
import com.arcussmarthome.oculus.modules.video.VideoSection;
import com.arcussmarthome.oculus.util.ComponentWrapper;
import com.arcussmarthome.oculus.util.SwingExecutorService;
import com.arcussmarthome.oculus.view.SimpleViewModel;
import com.arcussmarthome.oculus.view.ViewModel;
import com.arcussmarthome.resource.Resource;
import com.arcussmarthome.resource.Resources;

/**
 *
 */
public class OculusModule extends AbstractIrisModule {
   public OculusModule() {
   }

   @Provides
   public ViewModel<CapabilityDefinition> capabilities() {
      return new SimpleViewModel<>(StaticDefinitionRegistry.getInstance().getCapabilities());
   }

   /* (non-Javadoc)
    * @see com.google.inject.AbstractModule#configure()
    */
   @Override
   protected void configure() {
      DefaultExecutor.setDefaultExecutor(SwingExecutorService.getInstance());

      // TODO load these from the server
      bind(DefinitionRegistry.class)
         .toInstance(StaticDefinitionRegistry.getInstance());

      IrisClientFactory.init(new NettyIrisClientFactory());
      bind(IrisClient.class)
         .toInstance(IrisClientFactory.getClient());

      bind(SessionController.class)
         .asEagerSingleton();
      bind(StatusController.class)
         .asEagerSingleton();
      bind(DashboardController.class)
         .asEagerSingleton();
      bind(DeviceController.class)
         .asEagerSingleton();
      bind(HubController.class)
         .asEagerSingleton();
      bind(ProductController.class)
         .asEagerSingleton();
      bind(RuleController.class)
         .asEagerSingleton();
      bind(PersonController.class)
      	.asEagerSingleton();
      bind(PlaceController.class)
      	.asEagerSingleton();
      bind(AccountController.class)
         .asEagerSingleton();
      bind(SubsystemController.class)
         .asEagerSingleton();
      bind(IncidentController.class)
         .asEagerSingleton();
      bind(PairingDeviceController.class)
         .asEagerSingleton();
      bind(CapabilityController.class)
      	.asEagerSingleton();

      bind(DashboardSection.class);
      bind(HubSection.class);
      bind(DeviceSection.class);
      bind(SubsystemSection.class);
      bind(ProductSection.class);
      bind(RuleSection.class);
      bind(SceneSection.class);
      bind(PersonSection.class);
      bind(VideoSection.class);
      bind(PlaceSection.class);
      bind(AccountSection.class);
      bind(SchedulerSection.class);
      bind(BehaviorSection.class);
      bind(IncidentSection.class);
      bind(PairingDeviceSection.class);

      bindListToInstancesOf(OculusSection.class);
   }

   @Provides
   public List<ComponentWrapper<JMenu>> menus(
         SessionMenu session,
         PlaceMenu places,
         ServicesMenu services,
         HubsMenu hubs,
         DevicesMenu devices,
         WindowsMenu windows
   ) {
      return Arrays.asList(session, hubs, devices, places, services, windows);
   }

   @Provides
   @Singleton
   public MockActionsNexus provideMockActionsNexus() {
   	Resource resource = Resources.getResource("classpath:/mock.json");
   	return new MockActionsNexus(resource);
   }
}

