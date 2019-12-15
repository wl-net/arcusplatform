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
package com.arcussmarthome.common.subsystem.care;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;

import com.google.common.collect.ImmutableSet;
import com.arcussmarthome.common.subsystem.SubsystemTestCase;
import com.arcussmarthome.common.subsystem.event.SubsystemLifecycleEvent;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AccountCapability;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.capability.PresenceSubsystemCapability;
import com.arcussmarthome.messages.capability.SubsystemCapability;
import com.arcussmarthome.messages.event.Listener;
import com.arcussmarthome.messages.event.ModelEvent;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.subs.CareSubsystemModel;
import com.arcussmarthome.messages.model.subs.PresenceSubsystemModel;
import com.arcussmarthome.messages.model.test.ModelFixtures;

public class CareSubsystemTestCase extends SubsystemTestCase<CareSubsystemModel> {

   CareSubsystem   careSS;

   // For dummy client. (Should this be in SubsystemTestCase?
   protected Address clientAddress = Address.clientAddress("test", "session");

   protected Model owner = null;

   @Override
   protected CareSubsystemModel createSubsystemModel() {
      return CareSubsystemTestFixtures.createCareSubsystemModel();
   }
   
   @Before
   public void setUp() {
      super.setUp();
      
      owner = addModel(ModelFixtures.createPersonAttributes());
      
      placeModel.setAttribute(PlaceCapability.ATTR_SERVICELEVEL, PlaceCapability.SERVICELEVEL_PREMIUM);
      placeModel = addModel(placeModel.toMap());

      accountModel.setAttribute(AccountCapability.ATTR_OWNER, owner.getId());
      accountModel = addModel(accountModel.toMap());

      careSS = new CareSubsystem();
      store.addListener(new Listener<ModelEvent>() {
         @Override
         public void onEvent(ModelEvent event) {
            careSS.onEvent(event, context);
         }
      });      
      careSS.onEvent(SubsystemLifecycleEvent.added(context.model().getAddress()), context);
      careSS.onEvent(SubsystemLifecycleEvent.started(context.model().getAddress()), context);
   }
 
   @After
   public void destroy() {      
      careSS = null;
   }
   
   protected void assertNoCareCapableDevices() {
      assertEquals(ImmutableSet.<String>of(), context.model().getCareCapableDevices());
   }

   protected void assertNoCareDevices() {
      assertEquals(ImmutableSet.<String>of(), context.model().getCareDevices());
   }

   protected void assertCareCabableDevices(String... addresses) {
      assertEquals(ImmutableSet.copyOf(Arrays.asList(addresses)), context.model().getCareCapableDevices());
   }

   protected void assertCareDevices(String... addresses) {
      assertEquals(ImmutableSet.copyOf(Arrays.asList(addresses)), context.model().getCareDevices());
   }

   protected void assertActiveDevices(String... addresses) {
      assertEquals(ImmutableSet.copyOf(Arrays.asList(addresses)), context.model().getTriggeredDevices());
   }

   protected void assertInactiveDevices(String... addresses) {
      assertEquals(ImmutableSet.copyOf(Arrays.asList(addresses)), context.model().getInactiveDevices());
   }
   
   protected void assertAlerted() {
      assertTrue(context.model().isAlarmStateALERT());
   }

   protected void assertReady() {
      assertTrue(context.model().isAlarmStateREADY());
   }
   
   protected void assertLastCauseSet() {
      assertTrue(context.model().getLastAlertTime() != null);
      assertTrue(context.model().getLastAlertCause() != null);
   }
   
   protected void assertLastAckSet() {
      assertTrue(context.model().getLastAcknowledgementTime() != null);
      assertTrue(context.model().getLastAcknowledgedBy() != null);
   }

   protected void assertLastClearSet() {
      assertTrue(context.model().getLastClearTime() != null);
      assertTrue(context.model().getLastClearedBy() != null);
   }

   
   protected PlatformMessage carePlatformMessage(MessageBody body) {
      return carePlatformMessage(body, clientAddress);
   }
   
   protected PlatformMessage carePlatformMessage(MessageBody body, Address fromAddress) {
         PlatformMessage message =
               PlatformMessage
                     .request(model.getAddress())
                     .from(fromAddress)
                     .withActor(owner.getAddress())
                     .withPayload(body)
                     .create();
         return message;
      }
   
}

