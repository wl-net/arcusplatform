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
package com.arcussmarthome.driver.service.registry;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.arcussmarthome.annotation.Version;
import com.arcussmarthome.bootstrap.guice.Injectors;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.dao.PopulationDAO;
import com.arcussmarthome.core.dao.file.FileDAOModule;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.device.attributes.AttributeKey;
import com.arcussmarthome.device.attributes.AttributeMap;
import com.arcussmarthome.driver.DeviceDriver;
import com.arcussmarthome.driver.DeviceDriverContext;
import com.arcussmarthome.driver.DeviceDriverDefinition;
import com.arcussmarthome.driver.event.DriverEvent;
import com.arcussmarthome.driver.service.TestDriverModule;
import com.arcussmarthome.messages.ErrorEvent;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.model.DriverId;
import com.arcussmarthome.messages.type.Population;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;

@Mocks({ HubDAO.class, DeviceDAO.class, PlaceDAO.class, PopulationDAO.class })
@Modules({TestDriverModule.class, FileDAOModule.class, InMemoryMessageModule.class })
public class TestCompositeDriverRegistry extends IrisMockTestCase {
   @Inject CompositeDriverRegistry compositeRegistry;

   @Provides
   @Singleton
   public CompositeDriverRegistry provideDriverRegistry(Driver1 driver1, Driver1V1 driver1v1, Driver1V2 driver1v2, Driver2 driver2) {
      Map<DriverId, DeviceDriver> map0 = new HashMap<>();
      map0.put(driver1.getDriverId(), driver1);
      Map<DriverId, DeviceDriver> map1 = new HashMap<>();
      map1.put(driver1v1.getDriverId(), driver1v1);
      Map<DriverId, DeviceDriver> map2 = new HashMap<>();
      map2.put(driver1v2.getDriverId(), driver1v2);
      Map<DriverId, DeviceDriver> map3 = new HashMap<>();
      map3.put(driver2.getDriverId(), driver2);

      return new CompositeDriverRegistry(new MapDriverRegistry(map0), new MapDriverRegistry(map1), new MapDriverRegistry(map2), new MapDriverRegistry(map3));
   }
   
   @Test
   public void testDriverMatchesNone() {
      AttributeMap attributes = AttributeMap.emptyMap();
      assertEquals(null, compositeRegistry.findDriverFor("general", attributes, 0));

   }

   @Test
   public void testDriverMatchesOne() {
      AttributeMap attributes = AttributeMap.mapOf(
            MATCH_DRIVER2.valueOf(Boolean.TRUE)
      );
      assertEquals(new DriverId("Driver2", com.arcussmarthome.model.Version.UNVERSIONED), compositeRegistry.findDriverFor("general", attributes, 0).getDriverId());
   }

   @Test
   public void testDriverMatchesAll() {
      AttributeMap attributes = AttributeMap.mapOf(
            MATCH_DRIVER1V1.valueOf(Boolean.TRUE),
            MATCH_DRIVER1V2.valueOf(Boolean.TRUE),
            MATCH_DRIVER2.valueOf(Boolean.TRUE)
      );
      assertEquals(new DriverId("Driver1", new com.arcussmarthome.model.Version(2)), compositeRegistry.findDriverFor("general", attributes, 0).getDriverId());
   }

   @Test
   public void testLoadDriverByName() {
      assertEquals(Driver1V2.class, compositeRegistry.loadDriverByName("general", "Driver1", 0).getClass());
      assertEquals(Driver2.class, compositeRegistry.loadDriverByName("general", "Driver2", 0).getClass());
      assertNull(compositeRegistry.loadDriverByName("general", "Driver0", 0));
      assertNull(compositeRegistry.loadDriverByName("general", "Driver3", 0));
   }

   @Test
   public void testLoadDriverById() {
      assertEquals(Driver1.class, compositeRegistry.loadDriverById("Driver1", com.arcussmarthome.model.Version.UNVERSIONED).getClass());
      assertEquals(Driver1V1.class, compositeRegistry.loadDriverById("Driver1", new com.arcussmarthome.model.Version(1)).getClass());
      assertEquals(Driver1V2.class, compositeRegistry.loadDriverById("Driver1", new com.arcussmarthome.model.Version(2)).getClass());
      assertEquals(Driver2.class,   compositeRegistry.loadDriverById("Driver2", com.arcussmarthome.model.Version.UNVERSIONED).getClass());

      assertNull(compositeRegistry.loadDriverById("Driver1", new com.arcussmarthome.model.Version(3)));
   }

   public static abstract class DriverStub implements DeviceDriver {
      private DeviceDriverDefinition definition =
            DeviceDriverDefinition
               .builder()
               .withName(Injectors.getServiceName(this))
               .withVersion(Injectors.getServiceVersion(this, com.arcussmarthome.model.Version.UNVERSIONED))
               .withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL, Population.NAME_BETA, Population.NAME_QA))
               .create();

      @Override
      public boolean supports(AttributeMap attributes) {
         return false;
      }

      @Override
      public void handleDriverEvent(DriverEvent event, DeviceDriverContext context) {

      }

      @Override
      public DeviceDriverDefinition getDefinition() {
         return definition;
      }

      @Override
      public void onRestored(DeviceDriverContext context) {
         // TODO Auto-generated method stub
         
      }

      @Override
      public void onUpgraded(DriverEvent event, DriverId previous, DeviceDriverContext context) {
         // TODO Auto-generated method stub
         
      }

      @Override
      public void onSuspended(DeviceDriverContext context) {
         // TODO Auto-generated method stub
         
      }

      @Override
      public void onAttributesUpdated(DeviceDriverContext context, Map<AttributeKey<?>,Object> attrs, Integer reflexVersion, boolean isDeviceMessage) {
      }

      @Override
      public void handleProtocolMessage(ProtocolMessage message,
            DeviceDriverContext context) {
         // TODO Auto-generated method stub

      }

      @Override
      public void handlePlatformMessage(PlatformMessage message,
            DeviceDriverContext context) {
         // TODO Auto-generated method stub

      }

      @Override
      public void handleError(ErrorEvent error, DeviceDriverContext context) {
         // TODO Auto-generated method stub

      }

      @Override
      public AttributeMap getBaseAttributes() {
         return AttributeMap.emptyMap();
      }

   }

   @Named("Driver1")
   public static class Driver1 extends DriverStub {

      @Override
      public boolean supports(AttributeMap attributes) {
         return attributes.containsKey(MATCH_DRIVER1V1);
      }

      @Override
      public void onAttributesUpdated(DeviceDriverContext context, Map<AttributeKey<?>, Object> attributes, Integer reflexVersion, boolean isDeviceMessage) {

      }

      @Override
      public DriverId getDriverId() {
         return new DriverId("Driver1", com.arcussmarthome.model.Version.UNVERSIONED);
      }
   }

   @Named("Driver1")
   @Version(1)
   public static class Driver1V1 extends DriverStub {

      @Override
      public boolean supports(AttributeMap attributes) {
         return attributes.containsKey(MATCH_DRIVER1V1);
      }

      @Override
      public void onAttributesUpdated(DeviceDriverContext context, Map<AttributeKey<?>, Object> attributes, Integer reflexVersion, boolean isDeviceMessage) {

      }

      @Override
      public DriverId getDriverId() {
         return new DriverId("Driver1", new com.arcussmarthome.model.Version(1));
      }
   }

   @Named("Driver1")
   @Version(2)
   public static class Driver1V2 extends DriverStub {

      @Override
      public boolean supports(AttributeMap attributes) {
         return attributes.containsKey(MATCH_DRIVER1V2);
      }

      @Override
      public void onAttributesUpdated(DeviceDriverContext context, Map<AttributeKey<?>, Object> attributes, Integer reflexVersion, boolean isDeviceMessage) {

      }

      @Override
      public DriverId getDriverId() {
         return new DriverId("Driver1", new com.arcussmarthome.model.Version(2));
      }
   }

   public static class Driver2 extends DriverStub {

      @Override
      public boolean supports(AttributeMap attributes) {
         return attributes.containsKey(MATCH_DRIVER2);
      }

      @Override
      public void onAttributesUpdated(DeviceDriverContext context, Map<AttributeKey<?>, Object> attributes, Integer reflexVersion, boolean isDeviceMessage) {

      }

      @Override
      public DriverId getDriverId() {
         return new DriverId(getClass().getSimpleName(), com.arcussmarthome.model.Version.UNVERSIONED);
      }
   }

   private static AttributeKey<Boolean> MATCH_DRIVER1V1 =
         AttributeKey.create("MatchDriver1V1", Boolean.class);
   private static AttributeKey<Boolean> MATCH_DRIVER1V2 =
         AttributeKey.create("MatchDriver1V2", Boolean.class);
   private static AttributeKey<Boolean> MATCH_DRIVER2 =
         AttributeKey.create("MatchDriver2", Boolean.class);
}

