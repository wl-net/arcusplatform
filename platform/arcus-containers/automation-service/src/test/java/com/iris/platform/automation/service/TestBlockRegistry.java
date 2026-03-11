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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.messages.model.Model;
import com.iris.platform.model.ModelDao;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({ ModelDao.class })
public class TestBlockRegistry extends IrisMockTestCase {

   @Inject ModelDao modelDao;
   BlockRegistry registry;
   UUID placeId;

   @Before
   public void setUp() throws Exception {
      super.setUp();
      placeId = UUID.randomUUID();
      registry = new BlockRegistry(modelDao);
   }

   @Test
   public void testGetStartingPointsEmptyPlace() {
      expectModelsForPlace(new ArrayList<>());
      replay();

      List<Map<String, Object>> triggers = registry.getStartingPoints(placeId);

      // Should always have time-of-day, sunrise-sunset, and alarm-change even with no devices
      assertTrue(triggers.size() >= 3);
      assertHasBlockType(triggers, "time-of-day");
      assertHasBlockType(triggers, "sunrise-sunset");
      assertHasBlockType(triggers, "alarm-change");
      // No device triggers without devices
      assertNoBlockType(triggers, "value-change");

      verify();
   }

   @Test
   public void testGetStartingPointsWithDevices() {
      List<Model> models = new ArrayList<>();
      models.add(createDeviceModel("DRIV:dev:switch-1", "Switch", "swit"));
      models.add(createDeviceModel("DRIV:dev:motion-1", "Motion Sensor", "mot"));

      expectModelsForPlace(models);
      replay();

      List<Map<String, Object>> triggers = registry.getStartingPoints(placeId);

      assertHasBlockType(triggers, "value-change");
      assertHasBlockType(triggers, "time-of-day");
      assertHasBlockType(triggers, "sunrise-sunset");
      assertHasBlockType(triggers, "duration"); // motion sensor enables this
      assertHasBlockType(triggers, "alarm-change");

      verify();
   }

   @Test
   public void testGetConditionsAlwaysHasBaseConditions() {
      expectModelsForPlace(new ArrayList<>());
      replay();

      List<Map<String, Object>> conditions = registry.getConditions(placeId);

      assertHasBlockType(conditions, "time-window");
      assertHasBlockType(conditions, "day-of-week");
      assertHasBlockType(conditions, "presence");
      assertHasBlockType(conditions, "alarm-state");

      verify();
   }

   @Test
   public void testGetConditionsWithDevices() {
      List<Model> models = new ArrayList<>();
      models.add(createDeviceModel("DRIV:dev:switch-1", "Switch", "swit"));

      expectModelsForPlace(models);
      replay();

      List<Map<String, Object>> conditions = registry.getConditions(placeId);

      assertHasBlockType(conditions, "device-state");

      verify();
   }

   @Test
   public void testGetActionsAlwaysHasBaseActions() {
      expectModelsForPlace(new ArrayList<>());
      replay();

      List<Map<String, Object>> actions = registry.getActions(placeId);

      assertHasBlockType(actions, "notify");
      assertHasBlockType(actions, "delay");
      // No set-attribute without controllable devices
      assertNoBlockType(actions, "set-attribute");
      // fire-scene only present when scenes exist
      assertNoBlockType(actions, "fire-scene");

      verify();
   }

   @Test
   public void testGetActionsWithControllableDevices() {
      List<Model> models = new ArrayList<>();
      models.add(createDeviceModel("DRIV:dev:switch-1", "Switch", "swit"));

      expectModelsForPlace(models);
      replay();

      List<Map<String, Object>> actions = registry.getActions(placeId);

      assertHasBlockType(actions, "set-attribute");
      assertHasBlockType(actions, "notify");
      assertHasBlockType(actions, "delay");
      // fire-scene only present when scene models exist

      verify();
   }

   @Test
   public void testBlockStructure() {
      expectModelsForPlace(new ArrayList<>());
      replay();

      List<Map<String, Object>> triggers = registry.getStartingPoints(placeId);

      // Every block should have kind, type, label, category
      for (Map<String, Object> block : triggers) {
         assertNotNull("block should have 'kind'", block.get("kind"));
         assertNotNull("block should have 'type'", block.get("type"));
         assertNotNull("block should have 'label'", block.get("label"));
         assertNotNull("block should have 'category'", block.get("category"));
         assertEquals("trigger", block.get("kind"));
      }

      verify();
   }

   @SuppressWarnings("unchecked")
   private void expectModelsForPlace(Collection<Model> models) {
      EasyMock.expect(modelDao.loadModelsByPlace(
            EasyMock.eq(placeId),
            EasyMock.anyObject(Set.class)))
            .andReturn(models);
   }

   private Model createDeviceModel(String address, String name, String... namespaces) {
      Model model = EasyMock.createNiceMock(Model.class);
      EasyMock.expect(model.getAttribute("base:address")).andReturn(address).anyTimes();
      EasyMock.expect(model.getAttribute("dev:name")).andReturn(name).anyTimes();
      EasyMock.expect(model.getAttribute("dev:devtypehint")).andReturn(name).anyTimes();

      for (String ns : namespaces) {
         EasyMock.expect(model.supports(ns)).andReturn(true).anyTimes();
         // Also return a non-null attribute for typical attributes
         EasyMock.expect(model.getAttribute(EasyMock.startsWith(ns + ":")))
               .andReturn("someValue").anyTimes();
      }

      EasyMock.replay(model);
      return model;
   }

   private void assertHasBlockType(List<Map<String, Object>> blocks, String type) {
      for (Map<String, Object> block : blocks) {
         if (type.equals(block.get("type"))) {
            return;
         }
      }
      fail("Expected block type '" + type + "' not found in: " + blocks);
   }

   private void assertNoBlockType(List<Map<String, Object>> blocks, String type) {
      for (Map<String, Object> block : blocks) {
         if (type.equals(block.get("type"))) {
            fail("Unexpected block type '" + type + "' found in: " + blocks);
         }
      }
   }
}
