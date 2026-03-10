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
package com.iris.platform.rule.catalog.condition.config;

import java.util.Collections;

import org.junit.Test;

import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.GuardConditionAdapter;
import com.iris.io.json.JSON;
import com.iris.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({ RuleConfigJsonModule.class })
public class TestGuardConfigs extends IrisTestCase {

   @Test
   public void testPresenceConfigSerialize() {
      PresenceConfig config = new PresenceConfig();
      config.setMode(PresenceConfig.MODE_OCCUPIED);

      String json = JSON.toJson(config);
      ConditionConfig deserialized = JSON.fromJson(json, ConditionConfig.class);

      assertEquals(config, deserialized);
      assertTrue(deserialized instanceof PresenceConfig);
      assertEquals(PresenceConfig.MODE_OCCUPIED, ((PresenceConfig) deserialized).getMode());
   }

   @Test
   public void testPresenceConfigPersonHome() {
      PresenceConfig config = new PresenceConfig();
      config.setMode(PresenceConfig.MODE_PERSON_HOME);
      config.setPersonAddress("SERV:person:abc-123");

      String json = JSON.toJson(config);
      ConditionConfig deserialized = JSON.fromJson(json, ConditionConfig.class);

      assertEquals(config, deserialized);
      PresenceConfig dc = (PresenceConfig) deserialized;
      assertEquals(PresenceConfig.MODE_PERSON_HOME, dc.getMode());
      assertEquals("SERV:person:abc-123", dc.getPersonAddress());
   }

   @Test
   public void testPresenceConfigGenerate() {
      PresenceConfig config = new PresenceConfig();
      config.setMode(PresenceConfig.MODE_OCCUPIED);

      Condition condition = config.generate(Collections.emptyMap());
      assertNotNull(condition);
      assertTrue(condition instanceof GuardConditionAdapter);
   }

   @Test
   public void testAlarmStateConfigSerialize() {
      AlarmStateConfig config = new AlarmStateConfig();
      config.setState(AlarmStateConfig.STATE_ON);

      String json = JSON.toJson(config);
      ConditionConfig deserialized = JSON.fromJson(json, ConditionConfig.class);

      assertEquals(config, deserialized);
      assertTrue(deserialized instanceof AlarmStateConfig);
      assertEquals(AlarmStateConfig.STATE_ON, ((AlarmStateConfig) deserialized).getState());
   }

   @Test
   public void testAlarmStateConfigGenerate() {
      AlarmStateConfig config = new AlarmStateConfig();
      config.setState(AlarmStateConfig.STATE_PARTIAL);

      Condition condition = config.generate(Collections.emptyMap());
      assertNotNull(condition);
      assertTrue(condition instanceof GuardConditionAdapter);
   }

   @Test
   public void testDeviceStateConfigSerialize() {
      DeviceStateConfig config = new DeviceStateConfig();
      config.setAddress("DRIV:dev:abc-123");
      config.setAttribute("doorlock:lockstate");
      config.setValue("LOCKED");

      String json = JSON.toJson(config);
      ConditionConfig deserialized = JSON.fromJson(json, ConditionConfig.class);

      assertEquals(config, deserialized);
      assertTrue(deserialized instanceof DeviceStateConfig);
      DeviceStateConfig dc = (DeviceStateConfig) deserialized;
      assertEquals("DRIV:dev:abc-123", dc.getAddress());
      assertEquals("doorlock:lockstate", dc.getAttribute());
      assertEquals("LOCKED", dc.getValue());
   }

   @Test
   public void testDeviceStateConfigGenerate() {
      DeviceStateConfig config = new DeviceStateConfig();
      config.setAddress("DRIV:dev:abc-123");
      config.setAttribute("swit:state");
      config.setValue("ON");

      Condition condition = config.generate(Collections.emptyMap());
      assertNotNull(condition);
      assertTrue(condition instanceof GuardConditionAdapter);
   }
}
