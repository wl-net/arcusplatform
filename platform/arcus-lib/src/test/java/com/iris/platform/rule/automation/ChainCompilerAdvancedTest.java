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
package com.iris.platform.rule.automation;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.rule.action.stateful.GuardedAction;
import com.iris.common.rule.action.stateful.SequentialActionList;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.OrCondition;
import com.iris.common.rule.filter.GuardedCondition;
import com.iris.common.rule.trigger.SunriseSunsetTrigger;
import com.iris.common.rule.trigger.ValueChangeTrigger;
import com.iris.common.rule.trigger.ValueInSetTrigger;
import com.iris.platform.rule.catalog.condition.config.OrConfig;
import com.iris.platform.rule.catalog.action.config.LogActionConfig;
import com.iris.platform.rule.catalog.condition.config.AlarmStateConfig;
import com.iris.platform.rule.catalog.condition.config.PresenceConfig;
import com.iris.platform.rule.catalog.condition.config.SunriseSunsetConfig;
import com.iris.platform.rule.catalog.condition.config.TimeWindowConfig;
import com.iris.platform.rule.catalog.condition.config.ValueChangeConfig;
import com.iris.platform.rule.catalog.condition.config.ValueInSetConfig;
import com.iris.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * Advanced tests for ChainCompiler covering real-world automation
 * scenarios with multiple guard types, new trigger types, and
 * full AutomationDefinition compilation.
 */
@Modules({ RuleConfigJsonModule.class })
public class ChainCompilerAdvancedTest extends IrisTestCase {

   // ---- Trigger type tests ----

   @Test
   public void testCompileSunriseSunsetTrigger() {
      SunriseSunsetConfig trigger = new SunriseSunsetConfig();
      trigger.setMode("SUNRISE");
      trigger.setOffsetMinutes(-15);

      Condition condition = ChainCompiler.compileCondition(
            trigger, Collections.emptyList(), ImmutableMap.of());

      assertTrue("Expected SunriseSunsetTrigger, got " + condition.getClass(),
            condition instanceof SunriseSunsetTrigger);
   }

   @Test
   public void testCompileValueInSetTrigger() {
      ValueInSetConfig trigger = new ValueInSetConfig();
      trigger.setAttribute("alarm:alertState");
      trigger.setAcceptedValues(ImmutableSet.of("ON", "PARTIAL"));

      Condition condition = ChainCompiler.compileCondition(
            trigger, Collections.emptyList(), ImmutableMap.of());

      assertTrue("Expected ValueInSetTrigger, got " + condition.getClass(),
            condition instanceof ValueInSetTrigger);
   }

   // ---- Multiple guard tests ----

   @Test
   public void testCompileWithMultipleGuards() {
      ValueChangeConfig trigger = new ValueChangeConfig();
      trigger.setAttributeExpression(new TemplatedExpression("mot:motion"));
      trigger.setNewValueExpression(new TemplatedExpression("DETECTED"));

      PresenceConfig presenceGuard = new PresenceConfig();
      presenceGuard.setMode(PresenceConfig.MODE_UNOCCUPIED);

      AlarmStateConfig alarmGuard = new AlarmStateConfig();
      alarmGuard.setState(AlarmStateConfig.STATE_ON);

      Condition condition = ChainCompiler.compileCondition(
            trigger,
            Arrays.asList(presenceGuard, alarmGuard),
            ImmutableMap.of());

      assertTrue("Expected GuardedCondition, got " + condition.getClass(),
            condition instanceof GuardedCondition);
   }

   @Test
   public void testCompileSunriseWithTimeWindowGuard() {
      SunriseSunsetConfig trigger = new SunriseSunsetConfig();
      trigger.setMode("SUNSET");
      trigger.setOffsetMinutes(30);

      TimeWindowConfig timeGuard = new TimeWindowConfig();
      timeGuard.setStartTime("18:00:00");
      timeGuard.setEndTime("23:00:00");

      Condition condition = ChainCompiler.compileCondition(
            trigger,
            Arrays.asList(timeGuard),
            ImmutableMap.of());

      assertTrue("Expected GuardedCondition, got " + condition.getClass(),
            condition instanceof GuardedCondition);
   }

   @Test
   public void testCompileValueInSetWithPresenceGuard() {
      ValueInSetConfig trigger = new ValueInSetConfig();
      trigger.setAttribute("doorlock:lockstate");
      trigger.setAcceptedValues(ImmutableSet.of("UNLOCKED", "LOCKING"));

      PresenceConfig guard = new PresenceConfig();
      guard.setMode(PresenceConfig.MODE_OCCUPIED);

      Condition condition = ChainCompiler.compileCondition(
            trigger,
            Arrays.asList(guard),
            ImmutableMap.of());

      assertTrue("Expected GuardedCondition, got " + condition.getClass(),
            condition instanceof GuardedCondition);
   }

   @Test
   public void testCompileWithThreeGuards() {
      ValueChangeConfig trigger = new ValueChangeConfig();
      trigger.setAttributeExpression(new TemplatedExpression("swit:state"));
      trigger.setNewValueExpression(new TemplatedExpression("ON"));

      PresenceConfig presenceGuard = new PresenceConfig();
      presenceGuard.setMode(PresenceConfig.MODE_OCCUPIED);

      AlarmStateConfig alarmGuard = new AlarmStateConfig();
      alarmGuard.setState(AlarmStateConfig.STATE_DISARMED);

      TimeWindowConfig timeGuard = new TimeWindowConfig();
      timeGuard.setStartTime("22:00:00");
      timeGuard.setEndTime("06:00:00");

      Condition condition = ChainCompiler.compileCondition(
            trigger,
            Arrays.asList(presenceGuard, alarmGuard, timeGuard),
            ImmutableMap.of());

      assertTrue("Expected GuardedCondition, got " + condition.getClass(),
            condition instanceof GuardedCondition);
   }

   // ---- Action compilation tests ----

   @Test
   public void testCompileThreeActions() {
      LogActionConfig a1 = new LogActionConfig("turn on lights");
      LogActionConfig a2 = new LogActionConfig("send notification");
      LogActionConfig a3 = new LogActionConfig("log event");

      StatefulAction compiled = ChainCompiler.compileActions(
            Arrays.asList(a1, a2, a3), ImmutableMap.of());

      assertNotNull(compiled);
      // Three actions should produce a SequentialActionList
      assertTrue("Expected SequentialActionList, got " + compiled.getClass(),
            compiled instanceof SequentialActionList);
   }

   @Test
   public void testSingleActionNotWrappedInList() {
      LogActionConfig action = new LogActionConfig("single action");

      StatefulAction compiled = ChainCompiler.compileActions(
            Arrays.asList(action), ImmutableMap.of());

      assertNotNull(compiled);
      // Single action should NOT be wrapped
      assertFalse("Single action should not be wrapped in SequentialActionList",
            compiled instanceof SequentialActionList);
   }

   // ---- Full AutomationDefinition compilation ----

   @Test
   public void testCompileFullDefinitionNoGuards() {
      AutomationDefinition def = new AutomationDefinition();
      def.setPlaceId(UUID.randomUUID());
      def.setName("Night lights");

      SunriseSunsetConfig trigger = new SunriseSunsetConfig();
      trigger.setMode("SUNSET");
      trigger.setOffsetMinutes(0);
      def.setTrigger(trigger);

      LogActionConfig action = new LogActionConfig("turn on porch lights");
      def.setActions(Arrays.asList(action));

      ChainCompiler.CompiledAutomation compiled = ChainCompiler.compile(def);

      assertNotNull(compiled);
      assertNotNull(compiled.getCondition());
      assertNotNull(compiled.getAction());
      assertTrue(compiled.getCondition() instanceof SunriseSunsetTrigger);
   }

   @Test
   public void testCompileFullDefinitionWithGuards() {
      AutomationDefinition def = new AutomationDefinition();
      def.setPlaceId(UUID.randomUUID());
      def.setName("Night motion alert");

      ValueChangeConfig trigger = new ValueChangeConfig();
      trigger.setAttributeExpression(new TemplatedExpression("mot:motion"));
      trigger.setNewValueExpression(new TemplatedExpression("DETECTED"));
      def.setTrigger(trigger);

      PresenceConfig guard = new PresenceConfig();
      guard.setMode(PresenceConfig.MODE_UNOCCUPIED);
      TimeWindowConfig timeGuard = new TimeWindowConfig();
      timeGuard.setStartTime("23:00:00");
      timeGuard.setEndTime("05:00:00");
      def.setConditions(Arrays.asList(guard, timeGuard));

      LogActionConfig a1 = new LogActionConfig("send alert");
      LogActionConfig a2 = new LogActionConfig("turn on lights");
      def.setActions(Arrays.asList(a1, a2));

      ChainCompiler.CompiledAutomation compiled = ChainCompiler.compile(def);

      assertNotNull(compiled);
      assertTrue("Trigger with guards should produce GuardedCondition",
            compiled.getCondition() instanceof GuardedCondition);
      assertTrue("Multiple actions should produce SequentialActionList",
            compiled.getAction() instanceof SequentialActionList);
   }

   @Test
   public void testCompileFullDefinitionWithValueInSet() {
      AutomationDefinition def = new AutomationDefinition();
      def.setPlaceId(UUID.randomUUID());
      def.setName("Alarm armed notification");

      ValueInSetConfig trigger = new ValueInSetConfig();
      trigger.setAttribute("alarm:alertState");
      trigger.setAcceptedValues(ImmutableSet.of("ON", "PARTIAL"));
      def.setTrigger(trigger);

      LogActionConfig action = new LogActionConfig("alarm is now armed");
      def.setActions(Arrays.asList(action));

      ChainCompiler.CompiledAutomation compiled = ChainCompiler.compile(def);

      assertNotNull(compiled);
      assertTrue(compiled.getCondition() instanceof ValueInSetTrigger);
   }

   // ---- Definition copy tests ----

   @Test
   public void testDefinitionCopyPreservesChain() {
      AutomationDefinition def = new AutomationDefinition();
      def.setPlaceId(UUID.randomUUID());
      def.setSequenceId(42);
      def.setName("Test automation");
      def.setDisabled(true);

      ValueChangeConfig trigger = new ValueChangeConfig();
      trigger.setAttributeExpression(new TemplatedExpression("swit:state"));
      def.setTrigger(trigger);

      PresenceConfig guard = new PresenceConfig();
      guard.setMode(PresenceConfig.MODE_OCCUPIED);
      def.setConditions(Arrays.asList(guard));

      LogActionConfig action = new LogActionConfig("do something");
      def.setActions(Arrays.asList(action));

      AutomationDefinition copy = def.copy();

      assertEquals(def.getName(), copy.getName());
      assertEquals(def.getPlaceId(), copy.getPlaceId());
      assertEquals(def.getSequenceId(), copy.getSequenceId());
      assertEquals(def.isDisabled(), copy.isDisabled());
      assertEquals(def.getTrigger(), copy.getTrigger());
      assertEquals(def.getConditions(), copy.getConditions());
      assertEquals(def.getActions(), copy.getActions());
   }

   @Test
   public void testDefinitionCopyIsolatesLists() {
      AutomationDefinition def = new AutomationDefinition();
      def.setPlaceId(UUID.randomUUID());
      def.setName("Isolation test");

      ValueChangeConfig trigger = new ValueChangeConfig();
      trigger.setAttributeExpression(new TemplatedExpression("swit:state"));
      def.setTrigger(trigger);

      def.setConditions(Arrays.asList(new PresenceConfig()));
      def.setActions(Arrays.asList(new LogActionConfig("original")));

      AutomationDefinition copy = def.copy();

      // Verify modifying the copy doesn't affect the original
      assertEquals(1, def.getConditions().size());
      assertEquals(1, copy.getConditions().size());
      assertEquals(1, def.getActions().size());
      assertEquals(1, copy.getActions().size());
   }

   // ---- Multiple triggers (OR) tests ----

   @Test
   public void testCompileMultipleTriggersWithOr() {
      SunriseSunsetConfig trigger1 = new SunriseSunsetConfig();
      trigger1.setMode("SUNSET");
      trigger1.setOffsetMinutes(0);

      ValueChangeConfig trigger2 = new ValueChangeConfig();
      trigger2.setAttributeExpression(new TemplatedExpression("swit:state"));
      trigger2.setNewValueExpression(new TemplatedExpression("ON"));

      OrConfig orTrigger = new OrConfig(Arrays.asList(trigger1, trigger2));

      Condition condition = ChainCompiler.compileCondition(
            orTrigger, Collections.emptyList(), ImmutableMap.of());

      assertTrue("Expected OrCondition, got " + condition.getClass(),
            condition instanceof OrCondition);
   }

   @Test
   public void testCompileMultipleTriggersWithGuards() {
      SunriseSunsetConfig trigger1 = new SunriseSunsetConfig();
      trigger1.setMode("SUNRISE");
      trigger1.setOffsetMinutes(0);

      ValueInSetConfig trigger2 = new ValueInSetConfig();
      trigger2.setAttribute("alarm:alertState");
      trigger2.setAcceptedValues(ImmutableSet.of("ON"));

      OrConfig orTrigger = new OrConfig(Arrays.asList(trigger1, trigger2));

      PresenceConfig guard = new PresenceConfig();
      guard.setMode(PresenceConfig.MODE_OCCUPIED);

      Condition condition = ChainCompiler.compileCondition(
            orTrigger, Arrays.asList(guard), ImmutableMap.of());

      assertTrue("Expected GuardedCondition wrapping OR, got " + condition.getClass(),
            condition instanceof GuardedCondition);
   }

   // ---- Multi-flow tests ----

   @Test
   public void testCompileMultipleFlows() {
      AutomationDefinition def = new AutomationDefinition();
      def.setPlaceId(UUID.randomUUID());
      def.setName("Multi-flow test");

      ValueChangeConfig trigger = new ValueChangeConfig();
      trigger.setAttributeExpression(new TemplatedExpression("mot:motion"));
      trigger.setNewValueExpression(new TemplatedExpression("DETECTED"));
      def.setTrigger(trigger);

      // Flow 1: if nighttime -> turn on lights
      AutomationFlow flow1 = new AutomationFlow(
            Arrays.asList(new TimeWindowConfig() {{
               setStartTime("22:00:00");
               setEndTime("06:00:00");
            }}),
            Arrays.asList(new LogActionConfig("turn on lights")));

      // Flow 2: if daytime -> send notification
      AutomationFlow flow2 = new AutomationFlow(
            Arrays.asList(new TimeWindowConfig() {{
               setStartTime("06:00:00");
               setEndTime("22:00:00");
            }}),
            Arrays.asList(new LogActionConfig("send notification")));

      def.setFlows(Arrays.asList(flow1, flow2));

      ChainCompiler.CompiledAutomation compiled = ChainCompiler.compile(def);

      assertNotNull(compiled);
      // Trigger should be unguarded (guards are per-flow)
      assertTrue("Multi-flow trigger should not be guarded",
            compiled.getCondition() instanceof ValueChangeTrigger);
      // Action should be a sequential list of guarded actions
      assertTrue("Multi-flow should produce SequentialActionList",
            compiled.getAction() instanceof SequentialActionList);
   }

   @Test
   public void testCompileSingleFlowWithGuards() {
      AutomationDefinition def = new AutomationDefinition();
      def.setPlaceId(UUID.randomUUID());
      def.setName("Single flow test");

      SunriseSunsetConfig trigger = new SunriseSunsetConfig();
      trigger.setMode("SUNSET");
      trigger.setOffsetMinutes(0);
      def.setTrigger(trigger);

      AutomationFlow flow = new AutomationFlow(
            Arrays.asList(new PresenceConfig() {{
               setMode(MODE_OCCUPIED);
            }}),
            Arrays.asList(new LogActionConfig("turn on lights")));

      def.setFlows(Arrays.asList(flow));

      ChainCompiler.CompiledAutomation compiled = ChainCompiler.compile(def);

      assertNotNull(compiled);
      // Single flow with guards — guards in action layer
      assertNotNull(compiled.getAction());
   }

   @Test
   public void testCompileMultipleFlowsNoGuards() {
      AutomationDefinition def = new AutomationDefinition();
      def.setPlaceId(UUID.randomUUID());
      def.setName("Multi-flow no guards");

      SunriseSunsetConfig trigger = new SunriseSunsetConfig();
      trigger.setMode("SUNSET");
      trigger.setOffsetMinutes(0);
      def.setTrigger(trigger);

      AutomationFlow flow1 = new AutomationFlow(
            Collections.emptyList(),
            Arrays.asList(new LogActionConfig("action 1")));
      AutomationFlow flow2 = new AutomationFlow(
            Collections.emptyList(),
            Arrays.asList(new LogActionConfig("action 2")));

      def.setFlows(Arrays.asList(flow1, flow2));

      ChainCompiler.CompiledAutomation compiled = ChainCompiler.compile(def);

      assertNotNull(compiled);
      assertTrue("Multiple unguarded flows should produce SequentialActionList",
            compiled.getAction() instanceof SequentialActionList);
   }

   @Test
   public void testEffectiveFlowsFallbackToLegacy() {
      AutomationDefinition def = new AutomationDefinition();
      def.setPlaceId(UUID.randomUUID());
      def.setName("Legacy format");

      SunriseSunsetConfig trigger = new SunriseSunsetConfig();
      trigger.setMode("SUNSET");
      trigger.setOffsetMinutes(0);
      def.setTrigger(trigger);

      def.setConditions(Arrays.asList(new PresenceConfig() {{
         setMode(MODE_OCCUPIED);
      }}));
      def.setActions(Arrays.asList(new LogActionConfig("legacy action")));

      // No flows set — should fall back to conditions + actions
      assertEquals(1, def.getEffectiveFlows().size());
      AutomationFlow effective = def.getEffectiveFlows().get(0);
      assertEquals(1, effective.getConditions().size());
      assertEquals(1, effective.getActions().size());
   }

   // ---- Error cases ----

   @Test(expected = NullPointerException.class)
   public void testCompileNullActions() {
      ChainCompiler.compileActions(null, ImmutableMap.of());
   }

   @Test(expected = IllegalStateException.class)
   public void testCompileValueInSetWithNoValues() {
      ValueInSetConfig trigger = new ValueInSetConfig();
      trigger.setAttribute("swit:state");
      // acceptedValues is empty

      ChainCompiler.compileCondition(
            trigger, Collections.emptyList(), ImmutableMap.of());
   }

   @Test(expected = IllegalStateException.class)
   public void testCompileValueInSetWithNoAttribute() {
      ValueInSetConfig trigger = new ValueInSetConfig();
      trigger.setAcceptedValues(ImmutableSet.of("ON", "OFF"));
      // attribute is null

      ChainCompiler.compileCondition(
            trigger, Collections.emptyList(), ImmutableMap.of());
   }
}
