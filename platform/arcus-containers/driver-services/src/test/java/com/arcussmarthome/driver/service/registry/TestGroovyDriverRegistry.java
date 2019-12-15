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

import groovy.util.GroovyScriptEngine;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.arcussmarthome.bootstrap.ServiceLocator;
import com.arcussmarthome.capability.registry.CapabilityRegistry;
import com.arcussmarthome.capability.registry.CapabilityRegistryModule;
import com.arcussmarthome.device.attributes.AttributeMap;
import com.arcussmarthome.device.attributes.AttributeValue;
import com.arcussmarthome.driver.groovy.GroovyDriverFactory;
import com.arcussmarthome.driver.groovy.GroovyProtocolPluginModule;
import com.arcussmarthome.driver.groovy.customizer.DriverCompilationCustomizer;
import com.arcussmarthome.driver.registry.GroovyDriverRegistry;
import com.arcussmarthome.driver.service.DriverConfig;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.model.DriverId;
import com.arcussmarthome.test.IrisTestCase;
import com.arcussmarthome.test.Modules;
import com.netflix.governator.annotations.WarmUp;

@Modules({ CapabilityRegistryModule.class, GroovyProtocolPluginModule.class } )
public class TestGroovyDriverRegistry extends IrisTestCase {
   private GroovyDriverRegistry groovyDriverRegistry;

   @Provides
   @Singleton
   public GroovyDriverRegistry provideGroovyDriverRegistry(TestDriverConfig config, GroovyDriverFactory factory) {
      return new GroovyDriverRegistry(config, factory);
   }

   @Provides
   @Singleton
   public GroovyScriptEngine provideGroovyScriptEngine(TestDriverConfig driverConfig, CapabilityRegistry registry) throws MalformedURLException {
      File driverDir = new File(driverConfig.getDriverDirectory());
      GroovyScriptEngine engine = new GroovyScriptEngine(new URL[] {
            driverDir.toURI().toURL(),
            new File("src/main/resources").toURI().toURL()
      } );
      engine.getConfig().addCompilationCustomizers(new DriverCompilationCustomizer(registry));
      return engine;
   }

   @Provides
   public TestDriverConfig provideTestDriverConfig() {
      return new TestDriverConfig(getDriverDir().getAbsolutePath());
   }

   @WarmUp
   public void warmUp() throws Exception {
      groovyDriverRegistry = ServiceLocator.getInstance(GroovyDriverRegistry.class);
      groovyDriverRegistry.warmUp();
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
   }

   @Test
   public void testDriverMatchesNone() {
      AttributeMap attributes = AttributeMap.emptyMap();
      assertEquals(null, groovyDriverRegistry.findDriverFor("general", attributes, 0));
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Test
   public void testDriverMatchesOne() {
      AttributeMap attributes = AttributeMap.mapOf(
            new AttributeValue(DeviceCapability.KEY_VENDOR, "Iris"),
            new AttributeValue(DeviceCapability.KEY_MODEL, "nifty-002")
      );
      assertEquals(new DriverId("Driver2", new com.arcussmarthome.model.Version(1)), groovyDriverRegistry.findDriverFor("general", attributes, 0).getDriverId());
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Test
   public void testDriverMatchesTwo() {
      AttributeMap attributes = AttributeMap.mapOf(
            new AttributeValue(DeviceCapability.KEY_VENDOR, "Iris"),
            new AttributeValue(DeviceCapability.KEY_MODEL, "nifty-001")
      );
      assertEquals(new DriverId("Driver1", new com.arcussmarthome.model.Version(2)), groovyDriverRegistry.findDriverFor("general", attributes, 0).getDriverId());
   }
   
   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Test
   public void testDriverMatchesThree() {
      AttributeMap attributes = AttributeMap.mapOf(
            new AttributeValue(DeviceCapability.KEY_VENDOR, "Iris"),
            new AttributeValue(DeviceCapability.KEY_MODEL, "nifty-003")
      );
      assertEquals(new DriverId("Driver3", new com.arcussmarthome.model.Version(1)), groovyDriverRegistry.findDriverFor("general", attributes, 0).getDriverId());
   
      attributes = AttributeMap.mapOf(
              new AttributeValue(DeviceCapability.KEY_VENDOR, "Iris"),
              new AttributeValue(DeviceCapability.KEY_MODEL, "nifty-003A")
        );
      assertEquals(new DriverId("Driver3", new com.arcussmarthome.model.Version(1)), groovyDriverRegistry.findDriverFor("general", attributes, 0).getDriverId());
     
      attributes = AttributeMap.mapOf(
              new AttributeValue(DeviceCapability.KEY_VENDOR, "Iris"),
              new AttributeValue(DeviceCapability.KEY_MODEL, "nifty-003BCE")
        );
      assertEquals(new DriverId("Driver3", new com.arcussmarthome.model.Version(1)), groovyDriverRegistry.findDriverFor("general", attributes, 0).getDriverId());

   }

   @Test
   public void testLoadDriverByName() {
      assertNotNull(groovyDriverRegistry.loadDriverByName("general", "Driver1", 0));
      assertEquals(new DriverId("Driver1", new com.arcussmarthome.model.Version(2)), groovyDriverRegistry.loadDriverByName("general", "Driver1", 0).getDriverId());
      assertNotNull(groovyDriverRegistry.loadDriverByName("general", "Driver2", 0));
      assertNull(groovyDriverRegistry.loadDriverByName("general", "Driver1V1", 0));
   }

   @Test
   public void testLoadDriverById() {
      assertEquals(new DriverId("Driver1", new com.arcussmarthome.model.Version(1)), groovyDriverRegistry.loadDriverById("Driver1", new com.arcussmarthome.model.Version(1)).getDriverId());
      assertEquals(new DriverId("Driver1", new com.arcussmarthome.model.Version(2)), groovyDriverRegistry.loadDriverById("Driver1", new com.arcussmarthome.model.Version(2)).getDriverId());
      assertEquals(new DriverId("Driver1", new com.arcussmarthome.model.Version(2)), groovyDriverRegistry.loadDriverByName("general", "Driver1", 0).getDriverId());
      assertEquals(new DriverId("Driver2", new com.arcussmarthome.model.Version(1)),   groovyDriverRegistry.loadDriverByName("general", "Driver2", 0).getDriverId());
      assertNull(groovyDriverRegistry.loadDriverById("Driver1", new com.arcussmarthome.model.Version(3)));
   }

   private File getDriverDir() {
      return new File("src/test/resources");
   }

   private static class TestDriverConfig extends DriverConfig {
      private final String directory;

      TestDriverConfig(String directory) {
         this.directory = directory;
      }

      @Override
      public String getDriverDirectory() {
         return directory;
      }

   }
}

