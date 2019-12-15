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

import com.arcussmarthome.common.subsystem.Subsystem;
import com.arcussmarthome.common.subsystem.SubsystemExecutor;
import com.arcussmarthome.messages.context.PlaceContext;
import com.arcussmarthome.messages.event.AddressableEvent;
import com.arcussmarthome.messages.event.Listener;
import com.arcussmarthome.messages.model.subs.SubsystemModel;
import com.arcussmarthome.platform.subsystem.impl.PlatformSubsystemContext;

public interface SubsystemFactory {

   SubsystemExecutor createExecutor(PlaceContext rootContext);

   <M extends SubsystemModel> PlatformSubsystemContext<M> createContext(
         Subsystem<M> subsystem,
         Listener<AddressableEvent> eventBus,
         PlaceContext rootContext
   );

}

