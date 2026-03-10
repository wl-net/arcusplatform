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

import java.util.Collections;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.errors.NotFoundException;
import com.iris.platform.rule.automation.AutomationDao;
import com.iris.platform.rule.automation.AutomationDefinition;
import com.iris.platform.rule.catalog.action.config.LogActionConfig;
import com.iris.platform.rule.catalog.condition.config.SunriseSunsetConfig;
import com.iris.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({ AutomationDao.class })
@Modules({ RuleConfigJsonModule.class })
public class TestAutomationRequestHandler extends IrisMockTestCase {

   @Inject AutomationDao automationDao;
   AutomationRequestHandler handler;
   UUID placeId;

   @Before
   public void setUp() throws Exception {
      super.setUp();
      placeId = UUID.randomUUID();
      handler = new AutomationRequestHandler(automationDao);
   }

   @Test
   public void testEnable() {
      AutomationDefinition def = createDefinition(true);
      EasyMock.expect(automationDao.findById(placeId, 1)).andReturn(def);
      automationDao.save(def);
      EasyMock.expectLastCall();
      replay();

      MessageBody response = handler.handleEnable(placeId, addressForSeq(1));

      assertNotNull(response);
      assertFalse(def.isDisabled());
      verify();
   }

   @Test
   public void testDisable() {
      AutomationDefinition def = createDefinition(false);
      EasyMock.expect(automationDao.findById(placeId, 1)).andReturn(def);
      automationDao.save(def);
      EasyMock.expectLastCall();
      replay();

      MessageBody response = handler.handleDisable(placeId, addressForSeq(1));

      assertNotNull(response);
      assertTrue(def.isDisabled());
      verify();
   }

   @Test
   public void testDelete() {
      EasyMock.expect(automationDao.delete(placeId, 1)).andReturn(true);
      replay();

      MessageBody response = handler.handleDelete(placeId, addressForSeq(1));

      assertNotNull(response);
      verify();
   }

   @Test
   public void testDeleteNotFound() {
      EasyMock.expect(automationDao.delete(placeId, 99)).andReturn(false);
      replay();

      try {
         handler.handleDelete(placeId, addressForSeq(99));
         fail("Expected NotFoundException");
      } catch (NotFoundException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testEnableNotFound() {
      EasyMock.expect(automationDao.findById(placeId, 42)).andReturn(null);
      replay();

      try {
         handler.handleEnable(placeId, addressForSeq(42));
         fail("Expected NotFoundException");
      } catch (NotFoundException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testGetAttributes() {
      AutomationDefinition def = createDefinition(false);
      def.setName("My Automation");
      def.setDescription("Test description");
      EasyMock.expect(automationDao.findById(placeId, 1)).andReturn(def);
      replay();

      MessageBody response = handler.handleGetAttributes(placeId, addressForSeq(1));

      assertNotNull(response);
      assertEquals("base:GetAttributesResponse", response.getMessageType());
      assertEquals("My Automation", response.getAttributes().get("auto:name"));
      verify();
   }

   private AutomationDefinition createDefinition(boolean disabled) {
      AutomationDefinition def = new AutomationDefinition();
      def.setPlaceId(placeId);
      def.setSequenceId(1);
      def.setDisabled(disabled);
      def.setName("Test");

      SunriseSunsetConfig trigger = new SunriseSunsetConfig();
      trigger.setMode("SUNRISE");
      trigger.setOffsetMinutes(0);
      def.setTrigger(trigger);

      LogActionConfig action = new LogActionConfig("test action");
      def.setActions(Collections.singletonList(action));
      def.setConditions(Collections.emptyList());

      return def;
   }

   private Address addressForSeq(int seqId) {
      return Address.platformService(String.valueOf(seqId), "auto");
   }
}
