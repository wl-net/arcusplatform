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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.platform.rule.automation.AutomationDao;
import com.iris.platform.rule.automation.AutomationDefinition;
import com.iris.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({ AutomationDao.class })
@Modules({ RuleConfigJsonModule.class })
public class TestCreateAutomationHandler extends IrisMockTestCase {

   @Inject AutomationDao automationDao;
   CreateAutomationHandler handler;
   UUID placeId;

   @Before
   public void setUp() throws Exception {
      super.setUp();
      placeId = UUID.randomUUID();
      handler = new CreateAutomationHandler(automationDao);
   }

   @Test
   public void testCreateSimpleAutomation() throws Exception {
      Map<String, Object> trigger = ImmutableMap.of(
            "type", "sunrise-sunset",
            "mode", "SUNRISE",
            "offsetMinutes", 0
      );
      List<Object> actions = ImmutableList.of(
            ImmutableMap.of(
                  "type", "log",
                  "message", "Automation fired"
            )
      );

      Map<String, Object> attrs = new HashMap<>();
      attrs.put("placeId", placeId.toString());
      attrs.put("name", "Test Automation");
      attrs.put("trigger", trigger);
      attrs.put("actions", actions);

      PlatformMessage message = createMessage(attrs);

      Capture<AutomationDefinition> captured = EasyMock.newCapture();
      automationDao.save(EasyMock.capture(captured));
      EasyMock.expectLastCall().andAnswer(() -> {
         AutomationDefinition def = captured.getValue();
         def.setSequenceId(1);
         def.setCreated(new java.util.Date());
         return null;
      });
      replay();

      MessageBody response = handler.handleMessage(message);

      assertNotNull(response);
      assertEquals("auto:CreateResponse", response.getMessageType());

      AutomationDefinition saved = captured.getValue();
      assertEquals("Test Automation", saved.getName());
      assertEquals(placeId, saved.getPlaceId());
      assertNotNull(saved.getTrigger());
      assertNotNull(saved.getActions());
      assertEquals(1, saved.getActions().size());
      assertFalse(saved.isDisabled());

      verify();
   }

   @Test
   public void testCreateWithConditions() throws Exception {
      Map<String, Object> trigger = ImmutableMap.of(
            "type", "sunrise-sunset",
            "mode", "SUNSET",
            "offsetMinutes", -15
      );
      List<Object> conditions = ImmutableList.of(
            ImmutableMap.of(
                  "type", "presence",
                  "mode", "OCCUPIED"
            )
      );
      List<Object> actions = ImmutableList.of(
            ImmutableMap.of(
                  "type", "log",
                  "message", "Sunset with guard"
            )
      );

      Map<String, Object> attrs = new HashMap<>();
      attrs.put("placeId", placeId.toString());
      attrs.put("name", "Guarded Automation");
      attrs.put("trigger", trigger);
      attrs.put("conditions", conditions);
      attrs.put("actions", actions);

      PlatformMessage message = createMessage(attrs);

      Capture<AutomationDefinition> captured = EasyMock.newCapture();
      automationDao.save(EasyMock.capture(captured));
      EasyMock.expectLastCall().andAnswer(() -> {
         AutomationDefinition def = captured.getValue();
         def.setSequenceId(1);
         def.setCreated(new java.util.Date());
         return null;
      });
      replay();

      MessageBody response = handler.handleMessage(message);

      assertNotNull(response);
      AutomationDefinition saved = captured.getValue();
      assertEquals("Guarded Automation", saved.getName());
      assertNotNull(saved.getConditions());
      assertEquals(1, saved.getConditions().size());

      verify();
   }

   @Test
   public void testCreateMissingNameFails() throws Exception {
      Map<String, Object> attrs = new HashMap<>();
      attrs.put("placeId", placeId.toString());
      attrs.put("trigger", ImmutableMap.of("type", "sunrise-sunset",
            "mode", "SUNRISE", "offsetMinutes", 0));
      attrs.put("actions", ImmutableList.of(ImmutableMap.of(
            "type", "log", "message", "test")));

      PlatformMessage message = createMessage(attrs);
      replay();

      try {
         handler.handleMessage(message);
         fail("Expected exception for missing name");
      } catch (Exception e) {
         // expected
      }

      verify();
   }

   @Test
   public void testCreateMissingTriggerFails() throws Exception {
      Map<String, Object> attrs = new HashMap<>();
      attrs.put("placeId", placeId.toString());
      attrs.put("name", "No Trigger");
      attrs.put("actions", ImmutableList.of(ImmutableMap.of(
            "type", "log", "message", "test")));

      PlatformMessage message = createMessage(attrs);
      replay();

      try {
         handler.handleMessage(message);
         fail("Expected exception for missing trigger");
      } catch (Exception e) {
         // expected
      }

      verify();
   }

   @Test
   public void testCreateMissingActionsFails() throws Exception {
      Map<String, Object> attrs = new HashMap<>();
      attrs.put("placeId", placeId.toString());
      attrs.put("name", "No Actions");
      attrs.put("trigger", ImmutableMap.of("type", "sunrise-sunset",
            "mode", "SUNRISE", "offsetMinutes", 0));

      PlatformMessage message = createMessage(attrs);
      replay();

      try {
         handler.handleMessage(message);
         fail("Expected exception for missing actions");
      } catch (Exception e) {
         // expected
      }

      verify();
   }

   private PlatformMessage createMessage(Map<String, Object> attrs) {
      MessageBody body = MessageBody.buildMessage("auto:Create", attrs);
      return PlatformMessage.buildRequest(body,
            Address.clientAddress("test", "session"),
            Address.platformService("auto"))
            .withPlaceId(placeId)
            .create();
   }
}
