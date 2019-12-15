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
package com.arcussmarthome.platform.services.account.handlers;

import static com.arcussmarthome.billing.client.model.Constants.STATE_ACTIVE;
import static com.arcussmarthome.messages.capability.Capability.ATTR_ID;
import static com.arcussmarthome.messages.model.ServiceLevel.BASIC;
import static com.arcussmarthome.messages.model.ServiceLevel.PREMIUM;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.arcussmarthome.capability.attribute.transform.AttributeMapTransformModule;
import com.arcussmarthome.capability.registry.CapabilityRegistryModule;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.core.notification.Notifications;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AccountCapability;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.model.ServiceAddon;
import com.arcussmarthome.messages.model.ServiceLevel;
import com.arcussmarthome.platform.address.validation.smartystreets.DetailedStreetAddress;
import com.arcussmarthome.platform.address.validation.smartystreets.HttpSmartyStreetsClient;
import com.arcussmarthome.platform.address.validation.smartystreets.SmartyStreetsClient;
import com.arcussmarthome.platform.location.LocationService;
import com.arcussmarthome.platform.services.BillingTestCase;
import com.arcussmarthome.platform.subscription.SubscriptionUpdater;
import com.arcussmarthome.platform.subscription.SubscriptionUpdaterImpl;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;
import com.arcussmarthome.util.IrisCollections;

@Modules({InMemoryMessageModule.class, AttributeMapTransformModule.class, CapabilityRegistryModule.class})
@Mocks({LocationService.class, HttpSmartyStreetsClient.class})
public class TestAddPlaceHandler extends BillingTestCase {
   
   @Inject
   private SmartyStreetsClient smartyStreetsClient;

   @Inject
   private AddPlaceHandler handler;
   
   @Override
   protected Set<String> configs()
   {
      Set<String> configs = super.configs();
      configs.add("src/test/resources/platform-services.properties");
      return configs;
   }
   
   @Override
   protected Set<Module> modules()
   {
      Set<Module> modules = super.modules();

      modules.add(new AbstractModule()
      {
         @Override
         protected void configure()
         {
            bind(SmartyStreetsClient.class).to(HttpSmartyStreetsClient.class);
         }
      });

      return modules;
   }
   
   @Provides
   @Singleton
   public SubscriptionUpdater provideSubscriptionUpdater() {
      return new SubscriptionUpdaterImpl(accountDao, placeDao, client, subManager,platformBus);
   }
   
   @Override
   public void setUp() throws Exception {
      super.setUp();
      initData();
   }
   
   @Test
   public void testAddPlace() throws Exception {
      PlaceData data = new PlaceData();
      data.attrs.put(ATTR_ID, "add16de2-252b-41f4-938e-41610317d9b5");
      handleMsg(data, BASIC);
      
      Place newPlace = findPlaceByName(data.name);
      verifyAccount(account.getId(), firstPlace.getId(), newPlace.getId());
      verifyPlace(newPlace.getId(), data, BASIC);
      // Should be two BASIC plans the original and the new place.
      verifyBilling(BASIC, STATE_ACTIVE, 2);
      verifyAddedEvent(platformBus.take(), account.getId(), newPlace.getId(), PlaceData.NEW_PLACE_NAME, BASIC);
      verifyNotificationMsg(platformBus.take());
      verifyNoMoreMsgs();
   } 
   
   @Test
   public void testAddPlacePremium() throws Exception {
      PlaceData data = new PlaceData();
      data.attrs.put(ATTR_ID, "add16de2-252b-41f4-938e-41610317d9b5");
      handleMsg(data, PREMIUM);
      
      Place newPlace = findPlaceByName(data.name);
      verifyAccount(account.getId(), firstPlace.getId(), newPlace.getId());
      verifyPlace(newPlace.getId(), data, PREMIUM);
      // Should be one basic plan and one premium plan.
      verifyBilling(BASIC, STATE_ACTIVE, 1);
      verifyBilling(PREMIUM, STATE_ACTIVE, 1);
      verifyAddedEvent(platformBus.take(), account.getId(), newPlace.getId(), PlaceData.NEW_PLACE_NAME, PREMIUM);
      verifyNotificationMsg(platformBus.take());
      verifyNoMoreMsgs();
   }
   
   @Test
   public void testAddPlaceWithAddons() throws Exception {
      PlaceData data = new PlaceData();
      data.attrs.put(ATTR_ID, "add16de2-252b-41f4-938e-41610317d9b5");
      String[] addons = new String[] { ServiceAddon.CELLBACKUP.name() };
      handleMsg(data, BASIC, addons);
      
      Place newPlace = findPlaceByName(data.name);
      verifyAccount(account.getId(), firstPlace.getId(), newPlace.getId());
      verifyPlace(newPlace.getId(), data, BASIC, addons);
      // Should be two BASIC plans the original and the new place.
      verifyBilling(BASIC, STATE_ACTIVE, 2, addons, 1, 1);
      verifyAddedEvent(platformBus.take(), account.getId(), newPlace.getId(), PlaceData.NEW_PLACE_NAME, BASIC, addons);
      verifyNotificationMsg(platformBus.take());
      verifyNoMoreMsgs();
   }
   
   @Test
   public void testAddPlacePremiumWithAddons() throws Exception {
      PlaceData data = new PlaceData();
      data.attrs.put(ATTR_ID, "add16de2-252b-41f4-938e-41610317d9b5");
      String[] addons = new String[] { ServiceAddon.CELLBACKUP.name() };
      handleMsg(data, PREMIUM, addons);
      
      Place newPlace = findPlaceByName(data.name);
      verifyAccount(account.getId(), firstPlace.getId(), newPlace.getId());
      verifyPlace(newPlace.getId(), data, PREMIUM, addons);
      verifyBilling(BASIC, STATE_ACTIVE, 1);
      verifyBilling(PREMIUM, STATE_ACTIVE, 1, addons, 1, 1);
      verifyAddedEvent(platformBus.take(), account.getId(), newPlace.getId(), PlaceData.NEW_PLACE_NAME, PREMIUM, addons);
      verifyNotificationMsg(platformBus.take());
      verifyNoMoreMsgs();
   }
   
   @Override
   protected String getSourceAddressRepresentation() {
      return Addresses.ACCOUNT;
   }

   @Override
   protected String getMsgKey() {
      return Notifications.PlaceAdded.KEY;
   }

         
   private void verifyAddedEvent(PlatformMessage msg, UUID accountId, UUID placeId, String name, ServiceLevel level, String... addons) {
      Assert.assertNotNull(msg);
      Assert.assertTrue(msg.getDestination().isBroadcast());
      Assert.assertTrue(msg.getSource().getRepresentation().startsWith("SERV:place:"));
      List<String> expectedCaps = Arrays.asList("place", "base");
      
      MessageBody body = msg.getValue();
      Assert.assertEquals(Capability.EVENT_ADDED, body.getMessageType());
      Assert.assertEquals(accountId.toString(), body.getAttributes().get(PlaceCapability.ATTR_ACCOUNT));
      Assert.assertEquals(expectedCaps, body.getAttributes().get(Capability.ATTR_CAPS));
      Assert.assertEquals("place", body.getAttributes().get(Capability.ATTR_TYPE));
      Assert.assertEquals(name, body.getAttributes().get(PlaceCapability.ATTR_NAME));
      Assert.assertEquals(placeId.toString(), body.getAttributes().get(Capability.ATTR_ID));
      Assert.assertEquals("SERV:place:" + placeId.toString(), body.getAttributes().get(Capability.ATTR_ADDRESS));
      Assert.assertEquals(level, ServiceLevel.fromString((String)body.getAttributes().get(PlaceCapability.ATTR_SERVICELEVEL)));
      Assert.assertEquals(IrisCollections.setOf(addons), 
            ((List<String>)body.getAttributes().get(PlaceCapability.ATTR_SERVICEADDONS)).stream().collect(Collectors.toSet()));
   }

   private void handleMsg(PlaceData data, ServiceLevel level, String... addons) {
      List<DetailedStreetAddress> dummyAddresses = new ArrayList<>();
      dummyAddresses.add(new DetailedStreetAddress());
      expect(smartyStreetsClient.getDetailedSuggestions(anyObject())).andReturn(dummyAddresses);

      replay();

      String serviceLevel = level == ServiceLevel.PREMIUM
            ? AccountCapability.UpdateServicePlanRequest.SERVICELEVEL_PREMIUM
            : AccountCapability.UpdateServicePlanRequest.SERVICELEVEL_BASIC;
      AccountCapability.AddPlaceRequest.Builder builder = AccountCapability.AddPlaceRequest.builder();
      builder.withPlace(data.asAttributes());
      builder.withServiceLevel(serviceLevel);
      if (addons != null) {
         Map<String, Object> addOnMap = Arrays.asList(addons).stream().collect(Collectors.toMap(s -> s, s -> Boolean.TRUE));
         builder.withAddons(addOnMap);
      }
      
      PlatformMessage msg = PlatformMessage.createBroadcast(builder.build(), Address.platformService("bogus")); // The source and destination are unnecessary for this test.
      handler.handleRequest(account, msg);
   }

   @After
   @Override
   public void tearDown() throws Exception
   {
      super.tearDown();

      verify();

      reset();
   }
}

