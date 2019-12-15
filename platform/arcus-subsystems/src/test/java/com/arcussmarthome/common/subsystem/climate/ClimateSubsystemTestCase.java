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
package com.arcussmarthome.common.subsystem.climate;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.arcussmarthome.common.subsystem.SubsystemTestCase;
import com.arcussmarthome.common.subsystem.event.SubsystemLifecycleEvent;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.ClimateSubsystemCapability;
import com.arcussmarthome.messages.capability.PersonCapability;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.capability.SubsystemCapability;
import com.arcussmarthome.messages.event.Listener;
import com.arcussmarthome.messages.event.ModelEvent;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.SimpleModel;
import com.arcussmarthome.messages.model.subs.ClimateSubsystemModel;
import com.arcussmarthome.messages.model.test.ModelFixtures;
import com.arcussmarthome.util.IrisCollections;

/**
 * 
 */
public class ClimateSubsystemTestCase extends SubsystemTestCase<ClimateSubsystemModel> {

   private boolean started = false;
   
   protected ClimateSubsystem subsystem = new ClimateSubsystem();
   
   @Override
   protected ClimateSubsystemModel createSubsystemModel() {
      Map<String, Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, ClimateSubsystemCapability.NAMESPACE);
      return new ClimateSubsystemModel(new SimpleModel(attributes));
   }
   
   // TODO move start / add model / remove model down to SubsystemTestCase
   /**
    * Calling start will send the subsystem an added and started event.
    * Additionally before start is called any addModel / updateModel / removeModel
    * calls will simply affect the store, afterwards these events will result
    * in ModelEvents sent to the subsystem as well.
    */
   protected void start() {
      addModel(ModelFixtures.buildServiceAttributes(context.getPlaceId(), PlaceCapability.NAMESPACE).create());
      subsystem.onEvent(SubsystemLifecycleEvent.added(context.model().getAddress()), context);
      subsystem.onEvent(SubsystemLifecycleEvent.started(context.model().getAddress()), context);
      store.addListener(new Listener<ModelEvent>() {
         @Override
         public void onEvent(ModelEvent event) {
            subsystem.onEvent(event, context);
         }
      });
      started = true;
   }

   protected boolean isStarted() {
      return started;
   }
   
   protected Model addModel(Map<String, Object> attributes) {
      return store.addModel(attributes);
   }
   
   protected void updateModel(Address address, Map<String, Object> attributes) {
      Map<String, Object> update = new HashMap<>(attributes);
      update.put(Capability.ATTR_ADDRESS, address.getRepresentation());
      addModel(update);
   }
   
   protected void removeModel(String address) {
      store.removeModel(Address.fromString(address));
   }
   
   protected void removeModel(Address address) {
      store.removeModel(address);
   }
   
   protected void assertAllEmpty() {
      assertControlEmpty();
      assertTemperatureEmpty();
      assertHumidityEmpty();
      assertThermostatsEmpty();
   }
   
   protected void assertControlEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getControlDevices());
   }

   protected void assertTemperatureEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getTemperatureDevices());
   }

   protected void assertHumidityEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getHumidityDevices());
   }

   protected void assertThermostatsEmpty() {
      assertEquals(ImmutableSet.of(), context.model().getThermostats());
   }
   
   protected void assertControlEquals(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getControlDevices());
   }

   protected void assertThermostatsEquals(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getThermostats());
   }

   protected void assertTemperatureEquals(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getTemperatureDevices());
   }

   protected void assertHumidityEquals(String... addresses) {
      assertEquals(IrisCollections.setOf(addresses), context.model().getHumidityDevices());
   }

}

