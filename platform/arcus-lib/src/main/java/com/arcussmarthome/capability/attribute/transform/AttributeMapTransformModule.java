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
package com.arcussmarthome.capability.attribute.transform;

import java.util.Arrays;
import java.util.HashSet;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.capability.registry.CapabilityRegistry;
import com.arcussmarthome.capability.registry.CapabilityRegistryModule;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.SceneTemplateCapability;
import com.arcussmarthome.messages.model.Account;
import com.arcussmarthome.messages.model.Device;
import com.arcussmarthome.messages.model.Hub;
import com.arcussmarthome.messages.model.MobileDevice;
import com.arcussmarthome.messages.model.Person;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.type.ActivityInterval;
import com.arcussmarthome.platform.history.HistoryLogEntry;
import com.arcussmarthome.platform.scene.SceneDefinition;
import com.arcussmarthome.platform.scene.SceneTemplateEntity;
import com.arcussmarthome.prodcat.ProductCatalogEntry;

public class AttributeMapTransformModule extends AbstractIrisModule {

   @Inject
   public AttributeMapTransformModule(CapabilityRegistryModule capabilityRegistry) {
   }

   @Override
   protected void configure() {
      bind(AttributeMapTransformer.class).to(CapabilityDrivenAttributeMapTransformer.class);
      bind(ProtocolDrivenAttributeMapTransformer.class);
      bind(new Key<BeanAttributesTransformer<HistoryLogEntry>>() {})
         .to(HistoryLogEntryTransformer.class);
      bind(new Key<BeanAttributesTransformer<ActivityInterval>>() {})
      .to(ActivityIntervalTransformer.class);
   }

   @Provides @Singleton
   public BeanAttributesTransformer<Account> accountAttributeTransformer(CapabilityRegistry capabilityRegistry) {
      return new ReflectiveBeanAttributesTransformer<Account>(capabilityRegistry, new HashSet<String>(Arrays.asList("account", "base")), Account.class);
   }

   @Provides @Singleton
   public BeanAttributesTransformer<Place> placeAttributeTransformer(CapabilityRegistry capabilityRegistry) {
      return new ReflectiveBeanAttributesTransformer<Place>(capabilityRegistry, new HashSet<String>(Arrays.asList("place", "base")), Place.class);
   }

   @Provides @Singleton
   public BeanAttributesTransformer<Person> personAttributeTransformer(CapabilityRegistry capabilityRegistry) {
      return new PersonAttributesTransformer(capabilityRegistry);
   }

   @Provides @Singleton
   public BeanAttributesTransformer<Device> deviceAttributeTransformer(CapabilityRegistry capabilityRegistry) {
      return new ReflectiveBeanAttributesTransformer<Device>(capabilityRegistry, new HashSet<String>(Arrays.asList("dev", "devadv", "base")), Device.class);
   }

   @Provides @Singleton
   public BeanAttributesTransformer<Hub> hubAttributeTransformer(CapabilityRegistry capabilityRegistry) {
      return new ReflectiveBeanAttributesTransformer<Hub>(capabilityRegistry, new HashSet<String>(Arrays.asList("hub", "hubadv", "base")), Hub.class);
   }

   @Provides @Singleton
   public BeanAttributesTransformer<MobileDevice> mobileDeviceAttributeTransformer(CapabilityRegistry capabilityRegistry) {
      return new MobileDeviceAttributesTransformer(capabilityRegistry);
   }  
   
   @Provides @Singleton
   public BeanAttributesTransformer<ProductCatalogEntry> productAttributeTransformer(CapabilityRegistry capabilityRegistry) {
      return new ProductCatalogEntryTransformer(capabilityRegistry);
   }
   
   @Provides @Singleton
   public BeanAttributesTransformer<SceneDefinition> sceneAttributeTransformer(CapabilityRegistry capabilityRegistry) {
      return new SceneDefinitionTransformer(capabilityRegistry);
   }
   
   @Provides @Singleton
   public BeanAttributesTransformer<SceneTemplateEntity> sceneTemplateAttributeTransformer(CapabilityRegistry capabilityRegistry) {
      return new ReflectiveBeanAttributesTransformer<SceneTemplateEntity>(capabilityRegistry, new HashSet<String>(Arrays.asList(SceneTemplateCapability.NAMESPACE, Capability.NAMESPACE)), SceneTemplateEntity.class);
   }
   
}

