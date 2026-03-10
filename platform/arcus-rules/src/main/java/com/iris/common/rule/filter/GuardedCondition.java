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
package com.iris.common.rule.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;

/**
 * A filter condition that wraps a trigger with multiple guard conditions.
 * The trigger only fires when ALL guards evaluate to true.
 *
 * This is the runtime implementation of the automation chain's condition
 * pipeline: trigger fires, then each guard is checked in order.
 *
 * Guards are stateless predicates evaluated against the current context
 * (e.g., "someone is home", "alarm is armed", "it's between 9pm and 6am").
 * They do not fire on their own — they only gate the trigger.
 */
public class GuardedCondition extends StatefulFilterCondition {

   private final List<Condition> guards;

   public GuardedCondition(Condition trigger, List<Condition> guards) {
      super(trigger);
      Preconditions.checkNotNull(guards, "guards may not be null");
      Preconditions.checkArgument(!guards.isEmpty(), "must have at least one guard");
      this.guards = Collections.unmodifiableList(new ArrayList<>(guards));
   }

   public static Builder builder(Condition trigger) {
      return new Builder(trigger);
   }

   @Override
   protected boolean transitionsOnEventsOfType(RuleEventType type) {
      // Guards may change state on attribute changes or model additions/removals
      return type == RuleEventType.ATTRIBUTE_VALUE_CHANGED
            || type == RuleEventType.MODEL_ADDED
            || type == RuleEventType.MODEL_REMOVED;
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      return delegate.isSatisfiable(context);
   }

   @Override
   protected boolean matches(ConditionContext context) {
      for (Condition guard : guards) {
         if (!guard.isSatisfiable(context)) {
            context.logger().trace("Guard [{}] not satisfied, blocking trigger", guard);
            return false;
         }
      }
      return true;
   }

   @Override
   protected boolean update(ConditionContext context, RuleEvent event) {
      return matches(context);
   }

   @Override
   public String toString() {
      return "GuardedCondition [trigger=" + delegate + ", guards=" + guards + "]";
   }

   public static class Builder {
      private final Condition trigger;
      private final List<Condition> guards = new ArrayList<>();

      private Builder(Condition trigger) {
         this.trigger = Preconditions.checkNotNull(trigger, "trigger is required");
      }

      public Builder addGuard(Condition guard) {
         guards.add(Preconditions.checkNotNull(guard, "guard may not be null"));
         return this;
      }

      public GuardedCondition build() {
         return new GuardedCondition(trigger, guards);
      }
   }
}
