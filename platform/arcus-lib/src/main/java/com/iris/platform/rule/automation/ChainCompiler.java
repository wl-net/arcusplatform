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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.iris.common.rule.action.stateful.GuardedAction;
import com.iris.common.rule.action.stateful.SequentialActionList;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.GuardedCondition;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;

/**
 * Compiles a chain definition (trigger + conditions + actions) into
 * runnable rule engine objects.
 *
 * The compilation model is:
 *   trigger → wrapped with guard conditions → single Condition
 *   actions → compiled into SequentialActionList → single StatefulAction
 *
 * Guard conditions are applied as nested filters around the trigger,
 * so the trigger fires only when all guards are satisfied.
 */
public final class ChainCompiler {

   private ChainCompiler() {}

   /**
    * Compiles a trigger and guard conditions into a single Condition.
    *
    * If there are no guard conditions, returns the trigger directly.
    * If there are guards, wraps them using GuardedCondition which checks
    * each guard predicate before allowing the trigger to fire.
    */
   public static Condition compileCondition(
         ConditionConfig trigger,
         List<ConditionConfig> guards,
         Map<String, Object> values) {

      Preconditions.checkNotNull(trigger, "trigger is required");

      Condition triggerCondition = trigger.generate(values);

      if (guards == null || guards.isEmpty()) {
         return triggerCondition;
      }

      // Build a list of guard conditions that must all be true
      GuardedCondition.Builder builder = GuardedCondition.builder(triggerCondition);
      for (ConditionConfig guard : guards) {
         builder.addGuard(guard.generate(values));
      }
      return builder.build();
   }

   /**
    * Compiles a list of action configs into a single StatefulAction.
    * If there's only one action, returns it directly.
    * If there are multiple, wraps them in a SequentialActionList.
    */
   public static StatefulAction compileActions(
         List<ActionConfig> actionConfigs,
         Map<String, Object> values) {

      Preconditions.checkNotNull(actionConfigs, "actions are required");
      Preconditions.checkArgument(!actionConfigs.isEmpty(), "at least one action is required");

      if (actionConfigs.size() == 1) {
         return actionConfigs.get(0).createAction(values);
      }

      SequentialActionList.Builder builder = new SequentialActionList.Builder();
      for (ActionConfig config : actionConfigs) {
         builder.addAction(config.createAction(values));
      }
      return builder.build();
   }

   /**
    * Compiles multiple flows into a single action. Each flow becomes a
    * GuardedAction that checks its conditions before executing.
    * All matching flows execute sequentially.
    */
   public static StatefulAction compileFlows(
         List<AutomationFlow> flows,
         Map<String, Object> values) {

      Preconditions.checkNotNull(flows, "flows are required");
      Preconditions.checkArgument(!flows.isEmpty(), "at least one flow is required");

      if (flows.size() == 1) {
         AutomationFlow flow = flows.get(0);
         StatefulAction action = compileActions(flow.getActions(), values);
         if (flow.getConditions().isEmpty()) {
            return action;
         }
         List<Condition> guards = new ArrayList<>();
         for (ConditionConfig guard : flow.getConditions()) {
            guards.add(guard.generate(values));
         }
         return new GuardedAction(guards, action);
      }

      SequentialActionList.Builder builder = new SequentialActionList.Builder();
      for (AutomationFlow flow : flows) {
         StatefulAction action = compileActions(flow.getActions(), values);
         if (flow.getConditions().isEmpty()) {
            builder.addAction(action);
         }
         else {
            List<Condition> guards = new ArrayList<>();
            for (ConditionConfig guard : flow.getConditions()) {
               guards.add(guard.generate(values));
            }
            builder.addAction(new GuardedAction(guards, action));
         }
      }
      return builder.build();
   }

   /**
    * Convenience method to compile an entire automation definition.
    */
   public static CompiledAutomation compile(AutomationDefinition definition) {
      Map<String, Object> values = Collections.emptyMap();
      List<AutomationFlow> effectiveFlows = definition.getEffectiveFlows();

      // Trigger — for multi-flow, guards are in the action layer
      Condition condition;
      if (effectiveFlows.size() <= 1 && !definition.getConditions().isEmpty()) {
         condition = compileCondition(definition.getTrigger(), definition.getConditions(), values);
      }
      else {
         condition = compileCondition(definition.getTrigger(), Collections.emptyList(), values);
      }

      // Actions — single flow uses simple compilation, multi-flow uses guarded dispatch
      StatefulAction action;
      if (effectiveFlows.size() <= 1) {
         List<ActionConfig> actionConfigs = effectiveFlows.isEmpty()
               ? definition.getActions() : effectiveFlows.get(0).getActions();
         action = compileActions(actionConfigs, values);
      }
      else {
         action = compileFlows(effectiveFlows, values);
      }

      return new CompiledAutomation(condition, action);
   }

   /**
    * Result of compiling an automation chain.
    */
   public static class CompiledAutomation {
      private final Condition condition;
      private final StatefulAction action;

      public CompiledAutomation(Condition condition, StatefulAction action) {
         this.condition = condition;
         this.action = action;
      }

      public Condition getCondition() {
         return condition;
      }

      public StatefulAction getAction() {
         return action;
      }
   }
}
