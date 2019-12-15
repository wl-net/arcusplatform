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
package com.arcussmarthome.core.messaging.memory;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.arcussmarthome.capability.attribute.transform.AttributeMapTransformModule;
import com.arcussmarthome.core.dao.EmptyResourceBundle;
import com.arcussmarthome.core.dao.ResourceBundleDAO;
import com.arcussmarthome.core.messaging.MessagesModule;
import com.arcussmarthome.core.platform.IntraServiceMessageBus;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.core.protocol.ProtocolMessageBus;
import com.netflix.governator.annotations.Modules;

/**
 * 
 */
@Modules(include = { MessagesModule.class, AttributeMapTransformModule.class })
public class InMemoryMessageModule extends AbstractModule {

   @Override
   protected void configure() {
      bind(InMemoryPlatformMessageBus.class).in(Singleton.class);
      bind(InMemoryProtocolMessageBus.class).in(Singleton.class);
      bind(PlatformMessageBus.class).to(InMemoryPlatformMessageBus.class);
      bind(ProtocolMessageBus.class).to(InMemoryProtocolMessageBus.class);
      bind(ResourceBundleDAO.class).to(EmptyResourceBundle.class);

      bind(IntraServiceMessageBus.class).to(InMemoryIntraServiceMessageBus.class).in(Singleton.class);
   }
   
}

