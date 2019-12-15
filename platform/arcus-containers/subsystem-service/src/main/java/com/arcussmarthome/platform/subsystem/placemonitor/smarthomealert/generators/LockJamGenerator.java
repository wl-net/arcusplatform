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
package com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.generators;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.messages.capability.DeviceAdvancedCapability;
import com.arcussmarthome.messages.capability.DoorLockCapability;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.dev.DeviceAdvancedModel;
import com.arcussmarthome.messages.model.subs.PlaceMonitorSubsystemModel;
import com.arcussmarthome.messages.type.SmartHomeAlert;
import com.arcussmarthome.model.query.expression.ExpressionCompiler;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.AlertGenerator;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.AlertKeys;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.AlertScratchPad;
import com.arcussmarthome.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts;
import com.arcussmarthome.prodcat.ProductCatalogManager;

@Singleton
public class LockJamGenerator extends AlertGenerator {

   public static final String WARN_KEY = "WARN_JAM";

   private static final Predicate<Model> DOOR_LOCK = ExpressionCompiler.compile("base:caps contains '" + DoorLockCapability.NAMESPACE + "'");

   private static final Set<String> INTERESTING_ATTRS = ImmutableSet.of(
      DeviceAdvancedCapability.ATTR_ERRORS
   );

   private final ProductCatalogManager prodCat;

   @Inject
   public LockJamGenerator(ProductCatalogManager prodCat) {
      this.prodCat = prodCat;
   }

   @Override
   public void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad scratch) {
      context.models().getModels(DOOR_LOCK).forEach(door -> handleModelChanged(context, door, scratch));
   }

   @Override
   protected boolean isInterestedInAttributeChange(String attribute) {
      return INTERESTING_ATTRS.contains(attribute);
   }

   @Override
   protected void handleModelChanged(SubsystemContext<PlaceMonitorSubsystemModel> context, Model model, AlertScratchPad scratchPad) {
      if(!DOOR_LOCK.apply(model)) {
         return;
      }
      Map<String, String> errs = DeviceAdvancedModel.getErrors(model, ImmutableMap.of());
      if(errs.containsKey(WARN_KEY)) {
         scratchPad.putAlert(SmartHomeAlerts.create(
            SmartHomeAlert.ALERTTYPE_DEV_WARN_LOCK_JAM,
            SmartHomeAlert.SEVERITY_CRITICAL,
            model.getAddress(),
            SmartHomeAlerts.baseDeviceAttribues(model, prodCat),
            context.getPlaceId()
         ));
      } else {
         scratchPad.removeAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_DEV_WARN_LOCK_JAM, model.getAddress()));
      }
   }

}

