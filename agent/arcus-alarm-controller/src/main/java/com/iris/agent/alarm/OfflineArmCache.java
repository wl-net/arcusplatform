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
package com.iris.agent.alarm;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.alarm.AlarmEvents.ArmEvent;
import com.iris.agent.alarm.AlarmEvents.Mode;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.messages.address.Address;

/**
 * Remembers the last platform-provided arm configuration for each security mode so the hub can
 * arm from a keypad while it is disconnected from the platform.
 *
 * The cache only changes when the hub arms, so an offline arm uses the delays, sensitivity and
 * participating devices from the last time that mode was armed. Devices added since then are not
 * included, and a mode that has never been armed cannot be armed offline.
 */
final class OfflineArmCache {
   private static final Logger log = LoggerFactory.getLogger(OfflineArmCache.class);

   interface AttributeFactory {
      <T> HubAttributesService.Attribute<T> create(Class<T> type, String name, T def);
      HubAttributesService.Attribute<Set<String>> createSet(String name);
   }

   static final AttributeFactory PERSISTED = new AttributeFactory() {
      @Override
      public <T> HubAttributesService.Attribute<T> create(Class<T> type, String name, T def) {
         return unreported(HubAttributesService.persisted(type, name, def));
      }

      @Override
      public HubAttributesService.Attribute<Set<String>> createSet(String name) {
         return unreported(HubAttributesService.persistedSet(String.class, name, new LinkedHashSet<>()));
      }

      private <T> HubAttributesService.Attribute<T> unreported(HubAttributesService.Attribute<T> attr) {
         attr.setReportedOnConnect(false);
         attr.setReportedOnValueChange(false);
         return attr;
      }
   };

   private final ModeConfig on;
   private final ModeConfig partial;

   OfflineArmCache(AttributeFactory attributes) {
      this(attributes, "hubalarm:cached");
   }

   OfflineArmCache(AttributeFactory attributes, String prefix) {
      this.on = new ModeConfig(attributes, prefix + "On");
      this.partial = new ModeConfig(attributes, prefix + "Partial");
   }

   void cache(ArmEvent event) {
      ModeConfig config = config(event.getMode());
      if (config == null) {
         log.warn("not caching arm config for unknown mode: {}", event.getMode());
         return;
      }

      config.entranceDelay.set(event.getEntranceDelaySecs());
      config.exitDelay.set(event.getExitDelaySecs());
      config.sensitivity.set(event.getAlarmSensitivityDeviceCount() > 0 ? event.getAlarmSensitivityDeviceCount() : 1);
      config.soundsEnabled.set(event.isSoundsEnabled());
      config.silent.set(event.isSilent());
      config.activeDevices.set(new LinkedHashSet<>(event.getActiveDevices()));
      log.debug("cached {} arm config: entranceDelay={}, exitDelay={}, sensitivity={}, activeDevices={}",
         event.getMode(), event.getEntranceDelaySecs(), event.getExitDelaySecs(), event.getAlarmSensitivityDeviceCount(), event.getActiveDevices().size());
   }

   @Nullable
   ArmEvent build(Address source, @Nullable Address actor, Mode mode, boolean bypass) {
      ModeConfig config = config(mode);
      if (config == null) {
         log.warn("no cached arm config for unknown mode: {}", mode);
         return null;
      }

      Set<String> activeDevices = config.activeDevices.get();
      if (activeDevices == null || activeDevices.isEmpty()) {
         log.info("no cached arm config for mode {}, never armed before", mode);
         return null;
      }

      log.info("building cached arm event for offline arm: mode={}, bypass={}, activeDevices={}", mode, bypass, activeDevices.size());
      return new ArmEvent(source, actor, mode, bypass,
         config.entranceDelay.get(), config.exitDelay.get(), config.sensitivity.get(),
         config.silent.get(), config.soundsEnabled.get(), activeDevices);
   }

   @Nullable
   private ModeConfig config(Mode mode) {
      switch (mode) {
         case ON:
            return on;
         case PARTIAL:
            return partial;
         default:
            return null;
      }
   }

   private static final class ModeConfig {
      private final HubAttributesService.Attribute<Integer> entranceDelay;
      private final HubAttributesService.Attribute<Integer> exitDelay;
      private final HubAttributesService.Attribute<Integer> sensitivity;
      private final HubAttributesService.Attribute<Boolean> soundsEnabled;
      private final HubAttributesService.Attribute<Boolean> silent;
      private final HubAttributesService.Attribute<Set<String>> activeDevices;

      ModeConfig(AttributeFactory attributes, String prefix) {
         this.entranceDelay = attributes.create(Integer.class, prefix + "EntranceDelay", 30);
         this.exitDelay = attributes.create(Integer.class, prefix + "ExitDelay", 30);
         this.sensitivity = attributes.create(Integer.class, prefix + "Sensitivity", 1);
         this.soundsEnabled = attributes.create(Boolean.class, prefix + "SoundsEnabled", true);
         this.silent = attributes.create(Boolean.class, prefix + "Silent", false);
         this.activeDevices = attributes.createSet(prefix + "ActiveDevices");
      }
   }
}
