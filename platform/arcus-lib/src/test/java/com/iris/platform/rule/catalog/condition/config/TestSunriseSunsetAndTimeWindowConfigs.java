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
import com.iris.common.rule.filter.TimeWindowGuard;
import com.iris.common.rule.trigger.SunriseSunsetTrigger;
import com.iris.io.json.JSON;
import com.iris.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({ RuleConfigJsonModule.class })
public class TestSunriseSunsetAndTimeWindowConfigs extends IrisTestCase {

   @Test
   public void testSunriseSunsetConfigSerialize() {
      SunriseSunsetConfig config = new SunriseSunsetConfig();
      config.setMode("SUNSET");
      config.setOffsetMinutes(-15);

      String json = JSON.toJson(config);
      assertTrue(json.contains("\"mode\":\"SUNSET\""));
      assertTrue(json.contains("\"offsetMinutes\":-15"));
   }

   @Test
   public void testSunriseSunsetConfigDeserialize() {
      String json = "{\"type\":\"sunrise-sunset\",\"mode\":\"SUNRISE\",\"offsetMinutes\":30}";
      ConditionConfig config = JSON.fromJson(json, ConditionConfig.class);
      assertTrue(config instanceof SunriseSunsetConfig);
      SunriseSunsetConfig ssc = (SunriseSunsetConfig) config;
      assertEquals("SUNRISE", ssc.getMode());
      assertEquals(30, ssc.getOffsetMinutes());
   }

   @Test
   public void testSunriseSunsetConfigGenerate() {
      SunriseSunsetConfig config = new SunriseSunsetConfig();
      config.setMode("SUNRISE");
      config.setOffsetMinutes(10);

      Condition condition = config.generate(Collections.emptyMap());
      assertNotNull(condition);
      assertTrue(condition instanceof SunriseSunsetTrigger);
      assertTrue(condition.isSimpleTrigger());
   }

   @Test
   public void testTimeWindowConfigSerialize() {
      TimeWindowConfig config = new TimeWindowConfig();
      config.setStartTime("08:00:00");
      config.setEndTime("17:00:00");

      String json = JSON.toJson(config);
      assertTrue(json.contains("\"startTime\":\"08:00:00\""));
      assertTrue(json.contains("\"endTime\":\"17:00:00\""));
   }

   @Test
   public void testTimeWindowConfigDeserialize() {
      String json = "{\"type\":\"time-window\",\"startTime\":\"22:00:00\",\"endTime\":\"06:00:00\"}";
      ConditionConfig config = JSON.fromJson(json, ConditionConfig.class);
      assertTrue(config instanceof TimeWindowConfig);
      TimeWindowConfig twc = (TimeWindowConfig) config;
      assertEquals("22:00:00", twc.getStartTime());
      assertEquals("06:00:00", twc.getEndTime());
   }

   @Test
   public void testTimeWindowConfigGenerate() {
      TimeWindowConfig config = new TimeWindowConfig();
      config.setStartTime("08:00:00");
      config.setEndTime("17:00:00");

      Condition condition = config.generate(Collections.emptyMap());
      assertNotNull(condition);
      assertTrue(condition instanceof TimeWindowGuard);
      assertFalse(condition.isSimpleTrigger());
   }

   @Test
   public void testSunriseSunsetConfigEquals() {
      SunriseSunsetConfig a = new SunriseSunsetConfig();
      a.setMode("SUNRISE");
      a.setOffsetMinutes(10);

      SunriseSunsetConfig b = new SunriseSunsetConfig();
      b.setMode("SUNRISE");
      b.setOffsetMinutes(10);

      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
   }

   @Test
   public void testTimeWindowConfigEquals() {
      TimeWindowConfig a = new TimeWindowConfig();
      a.setStartTime("08:00:00");
      a.setEndTime("17:00:00");

      TimeWindowConfig b = new TimeWindowConfig();
      b.setStartTime("08:00:00");
      b.setEndTime("17:00:00");

      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
   }
}
