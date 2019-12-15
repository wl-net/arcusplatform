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
package com.arcussmarthome.messages.model;

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Predicate;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.event.Listener;
import com.arcussmarthome.messages.event.ModelEvent;
import com.arcussmarthome.util.Subscription;

public interface ModelStore {

   Collection<Model> getModels();
   
   Iterable<Model> getModelsByType(String type);
   
   Iterable<Model> getModels(Predicate<? super Model> p);
   
   @Nullable
   Model getModelByAddress(Address address);

   @Nullable
   Object getAttributeValue(Address address, String attributeName);

   void update(PlatformMessage message);

   Subscription addListener(Listener<ModelEvent> listener);

}

