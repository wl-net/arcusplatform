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
package com.arcussmarthome.agent.controller.hub;

import java.util.Date;

import org.eclipse.jdt.annotation.Nullable;

import com.arcussmarthome.agent.attributes.HubAttributesService;
import com.arcussmarthome.agent.hal.ButtonListener;
import com.arcussmarthome.agent.hal.IrisHal;
import com.arcussmarthome.agent.router.Port;
import com.arcussmarthome.agent.router.PortHandler;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.capability.HubButtonCapability;
import com.arcussmarthome.protocol.ProtocolMessage;

enum ButtonIrisHandler implements PortHandler, ButtonListener {
   INSTANCE;

   @Nullable
   private HubController controller;

   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> pressed = HubAttributesService.persisted(String.class, HubButtonCapability.ATTR_STATE, IrisHal.getButtonState());
   private static final HubAttributesService.Attribute<Integer> duration = HubAttributesService.persisted(Integer.class, HubButtonCapability.ATTR_DURATION, IrisHal.getButtonDuration());
   private static final HubAttributesService.Attribute<Long> lastChanged = HubAttributesService.persisted(Long.class, HubButtonCapability.ATTR_STATECHANGED, IrisHal.getButtonLastPressed());

   void start(HubController controller, Port parent) {
      this.controller = controller;
      IrisHal.addButtonListener(this);
   }

   @Nullable
   @Override
   public Object recv(Port port, PlatformMessage message) throws Exception {
      return null;
   }

   @Override
   public void recv(Port port, ProtocolMessage message) {
   }

   @Override
   public void recv(Port port, Object message) {
   }

    @Override
    public void buttonState(String state,int duration) {
        this.pressed.set(state);
        this.duration.set(duration);
        this.lastChanged.set(new Date().toInstant().toEpochMilli());
    }
}

