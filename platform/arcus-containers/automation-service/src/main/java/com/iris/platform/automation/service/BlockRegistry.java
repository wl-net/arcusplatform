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
package com.iris.platform.automation.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.platform.model.ModelDao;

/**
 * Introspects a place's devices and subsystems to build available
 * building blocks for the automation chain UI.
 *
 * Returns blocks grouped by category with the information a client
 * needs to render the step-by-step chain builder.
 */
@Singleton
public class BlockRegistry {

   private static final Set<String> TRACKED_TYPES =
         ImmutableSet.<String>builder()
            .add(AccountCapability.NAMESPACE)
            .add(DeviceCapability.NAMESPACE)
            .add(PlaceCapability.NAMESPACE)
            .add(PersonCapability.NAMESPACE)
            .add(HubCapability.NAMESPACE)
            .add(SceneCapability.NAMESPACE)
            .add(SubsystemCapability.NAMESPACE)
            .build();

   private final ModelDao modelDao;

   @Inject
   public BlockRegistry(ModelDao modelDao) {
      this.modelDao = modelDao;
   }

   /**
    * Returns available trigger (starting point) blocks for a place.
    * Each block has: type, label, category, and a params schema describing
    * what the user needs to select.
    */
   public List<Map<String, Object>> getStartingPoints(UUID placeId) {
      Collection<Model> models = getModels(placeId);
      List<Map<String, Object>> triggers = new ArrayList<>();

      // Category: Device Events — exact value match
      List<Map<String, Object>> devices = getDevicesWithCapability(models,
            "mot:motion", "cont:contact", "but:state", "swit:state",
            "doorlock:lockstate", "temp:temperature", "humid:humidity",
            "pres:presence", "pow:instantaneous");
      if (!devices.isEmpty()) {
         triggers.add(triggerBlock("value-change",
               "A device attribute changes",
               "Device Events",
               ImmutableMap.of(
                     "devices", devices,
                     "description", "Triggers when a device attribute changes to a specific value",
                     "params", ImmutableMap.of(
                           "newValue", ImmutableMap.of("type", "string", "label", "New value (leave blank for any)")
                     )
               )));

         // Multi-value match for enum attributes
         triggers.add(triggerBlock("value-in-set",
               "A device attribute becomes one of...",
               "Device Events",
               ImmutableMap.of(
                     "devices", devices,
                     "description", "Triggers when a device attribute changes to any of the selected values",
                     "params", ImmutableMap.of(
                           "acceptedValues", ImmutableMap.of("type", "string-list",
                                 "label", "Accepted values (comma-separated)")
                     )
               )));
      }

      // Category: Device Events — threshold crossing
      List<Map<String, Object>> numericDevices = getDevicesWithCapability(models,
            "temp:temperature", "humid:humidity", "pow:instantaneous",
            "dim:brightness", "fan:speed");
      if (!numericDevices.isEmpty()) {
         triggers.add(triggerBlock("value-threshold",
               "A reading crosses a threshold",
               "Device Events",
               ImmutableMap.of(
                     "devices", numericDevices,
                     "description", "Triggers when a numeric attribute goes above or below a threshold",
                     "params", ImmutableMap.of(
                           "direction", ImmutableMap.of("type", "enum", "label", "Direction",
                                 "values", ImmutableList.of("ABOVE", "BELOW")),
                           "threshold", ImmutableMap.of("type", "double", "label", "Threshold value"),
                           "sensitivity", ImmutableMap.of("type", "double", "label", "Sensitivity (hysteresis)",
                                 "default", 0.0)
                     )
               )));
      }

      // Category: Time — schedule with optional day-of-week
      Map<String, Object> timeParams = new LinkedHashMap<>();
      timeParams.put("time", ImmutableMap.of("type", "time", "label", "Time of day"));
      timeParams.put("days", ImmutableMap.of("type", "day-set", "label", "On days"));
      triggers.add(triggerBlock("time-of-day",
            "At a scheduled time",
            "Time",
            ImmutableMap.of(
                  "description", "Triggers at a specific time, optionally on selected days of the week",
                  "params", timeParams
            )));

      // Category: Sunrise/Sunset
      triggers.add(triggerBlock("sunrise-sunset",
            "At sunrise or sunset",
            "Time",
            ImmutableMap.of(
                  "description", "Triggers at sunrise or sunset with optional offset",
                  "params", ImmutableMap.of(
                        "mode", ImmutableMap.of("type", "enum", "label", "Event",
                              "values", ImmutableList.of("SUNRISE", "SUNSET")),
                        "offsetMinutes", ImmutableMap.of("type", "int", "label", "Offset (minutes)",
                              "default", 0)
                  )
            )));

      // Category: Duration
      if (hasCapability(models, "mot:motion")) {
         triggers.add(triggerBlock("duration",
               "No activity for a period",
               "Time",
               ImmutableMap.of(
                     "description", "Triggers when a condition is true for a duration",
                     "params", ImmutableMap.of(
                           "duration", ImmutableMap.of("type", "duration", "label", "Duration"),
                           "attribute", ImmutableMap.of("type", "attribute", "label", "Attribute")
                     )
               )));
      }

      // Category: Presence
      if (hasCapability(models, "pres:presence")) {
         triggers.add(triggerBlock("presence-change",
               "Someone arrives or leaves",
               "Presence",
               ImmutableMap.of(
                     "description", "Triggers when a person arrives or departs",
                     "modes", ImmutableList.of(
                           ImmutableMap.of("value", "ANY", "label", "Any change"),
                           ImmutableMap.of("value", "PRESENT", "label", "Arrives"),
                           ImmutableMap.of("value", "ABSENT", "label", "Leaves")
                     )
               )));
      }

      // Category: Alarm — with selectable states
      triggers.add(triggerBlock("alarm-change",
            "Alarm state changes",
            "Security",
            ImmutableMap.of(
                  "description", "Triggers when the security alarm changes to a selected state",
                  "modes", ImmutableList.of(
                        ImmutableMap.of("value", "ANY", "label", "Any change"),
                        ImmutableMap.of("value", "DISARMED", "label", "Disarmed"),
                        ImmutableMap.of("value", "ON", "label", "Armed (On)"),
                        ImmutableMap.of("value", "PARTIAL", "label", "Armed (Partial)"),
                        ImmutableMap.of("value", "ALERT", "label", "Alert (Triggered)")
                  )
            )));

      return triggers;
   }

   /**
    * Returns available condition (guard) blocks for a place.
    * These are the "only if" steps in the chain.
    */
   public List<Map<String, Object>> getConditions(UUID placeId) {
      Collection<Model> models = getModels(placeId);
      List<Map<String, Object>> conditions = new ArrayList<>();

      // Time window
      conditions.add(conditionBlock("time-window",
            "It's a certain time",
            "Time",
            ImmutableMap.of(
                  "description", "Only if between certain times of day",
                  "params", ImmutableMap.of(
                        "afterTime", ImmutableMap.of("type", "time", "label", "After"),
                        "beforeTime", ImmutableMap.of("type", "time", "label", "Before")
                  )
            )));

      // Day of week
      conditions.add(conditionBlock("day-of-week",
            "It's a certain day",
            "Time",
            ImmutableMap.of(
                  "description", "Only on selected days of the week",
                  "params", ImmutableMap.of(
                        "days", ImmutableMap.of("type", "day-set", "label", "Days")
                  )
            )));

      // Presence
      conditions.add(conditionBlock("presence",
            "Someone is home/away",
            "Presence",
            ImmutableMap.of(
                  "description", "Only if someone is home or everyone is away",
                  "modes", ImmutableList.of(
                        ImmutableMap.of("value", "OCCUPIED", "label", "Someone is home"),
                        ImmutableMap.of("value", "UNOCCUPIED", "label", "Nobody is home")
                  )
            )));

      // Alarm state
      conditions.add(conditionBlock("alarm-state",
            "Alarm is in a mode",
            "Security",
            ImmutableMap.of(
                  "description", "Only if the alarm is in a specific state",
                  "modes", ImmutableList.of(
                        ImmutableMap.of("value", "DISARMED", "label", "Disarmed"),
                        ImmutableMap.of("value", "ON", "label", "Armed (On)"),
                        ImmutableMap.of("value", "PARTIAL", "label", "Armed (Partial)")
                  )
            )));

      // Device state
      List<Map<String, Object>> devices = getDevicesWithCapability(models,
            "swit:state", "doorlock:lockstate", "cont:contact", "mot:motion",
            "temp:temperature");
      if (!devices.isEmpty()) {
         conditions.add(conditionBlock("device-state",
               "A device is in a state",
               "Devices",
               ImmutableMap.of(
                     "description", "Only if a device attribute has a specific value",
                     "devices", devices
               )));
      }

      return conditions;
   }

   /**
    * Returns available action blocks for a place.
    */
   public List<Map<String, Object>> getActions(UUID placeId) {
      Collection<Model> models = getModels(placeId);
      List<Map<String, Object>> actions = new ArrayList<>();

      // Control a device
      List<Map<String, Object>> controllable = getDevicesWithCapability(models,
            "swit:state", "doorlock:lockstate", "dim:brightness", "color:hue",
            "therm:hvacmode", "fan:speed", "vent:level");
      if (!controllable.isEmpty()) {
         actions.add(actionBlock("set-attribute",
               "Control a device",
               "Devices",
               ImmutableMap.of(
                     "description", "Set a device attribute to a value",
                     "devices", controllable
               )));
      }

      // Send notification
      actions.add(actionBlock("notify",
            "Send a notification",
            "Notifications",
            ImmutableMap.of(
                  "description", "Send a push notification or email"
            )));

      // Fire a scene
      actions.add(actionBlock("fire-scene",
            "Run a scene",
            "Scenes",
            ImmutableMap.of(
                  "description", "Execute an existing scene"
            )));

      // Delay
      actions.add(actionBlock("delay",
            "Wait, then continue",
            "Flow",
            ImmutableMap.of(
                  "description", "Pause for a duration before the next action",
                  "params", ImmutableMap.of(
                        "duration", ImmutableMap.of("type", "duration", "label", "Wait for")
                  )
            )));

      return actions;
   }

   private Collection<Model> getModels(UUID placeId) {
      return modelDao.loadModelsByPlace(placeId, TRACKED_TYPES);
   }

   private boolean hasCapability(Collection<Model> models, String attribute) {
      for (Model model : models) {
         if (model.getAttribute(attribute) != null || model.supports(attribute)) {
            return true;
         }
      }
      return false;
   }

   private List<Map<String, Object>> getDevicesWithCapability(
         Collection<Model> models, String... attributes) {
      List<Map<String, Object>> devices = new ArrayList<>();
      for (Model model : models) {
         for (String attr : attributes) {
            String ns = attr.contains(":") ? attr.substring(0, attr.indexOf(':')) : attr;
            if (model.supports(ns) || model.getAttribute(attr) != null) {
               Map<String, Object> device = new LinkedHashMap<>();
               device.put("address", String.valueOf(model.getAttribute("base:address")));
               device.put("name", model.getAttribute("dev:name"));
               device.put("type", model.getAttribute("dev:devtypehint"));

               // List the supported capabilities relevant to automation
               List<String> caps = new ArrayList<>();
               for (String a : attributes) {
                  String n = a.contains(":") ? a.substring(0, a.indexOf(':')) : a;
                  if (model.supports(n) || model.getAttribute(a) != null) {
                     caps.add(a);
                  }
               }
               device.put("attributes", caps);
               devices.add(device);
               break; // don't add same device twice
            }
         }
      }
      return devices;
   }

   private static Map<String, Object> triggerBlock(String type, String label,
         String category, Map<String, Object> details) {
      return buildBlock("trigger", type, label, category, details);
   }

   private static Map<String, Object> conditionBlock(String type, String label,
         String category, Map<String, Object> details) {
      return buildBlock("condition", type, label, category, details);
   }

   private static Map<String, Object> actionBlock(String type, String label,
         String category, Map<String, Object> details) {
      return buildBlock("action", type, label, category, details);
   }

   private static Map<String, Object> buildBlock(String kind, String type,
         String label, String category, Map<String, Object> details) {
      Map<String, Object> block = new LinkedHashMap<>();
      block.put("kind", kind);
      block.put("type", type);
      block.put("label", label);
      block.put("category", category);
      block.putAll(details);
      return block;
   }
}
