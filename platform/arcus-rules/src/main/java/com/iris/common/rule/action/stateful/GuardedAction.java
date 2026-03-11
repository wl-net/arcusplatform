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
package com.iris.common.rule.action.stateful;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.event.RuleEvent;

/**
 * A StatefulAction that only executes its delegate when all guard
 * conditions are satisfied. Used in multi-flow automations where
 * each flow has its own guards and actions.
 *
 * At runtime, the ActionContext is actually a RuleContext which
 * implements both ActionContext and ConditionContext, allowing
 * guard evaluation.
 */
public class GuardedAction extends BaseStatefulAction {

   private final List<Condition> guards;
   private final StatefulAction delegate;

   public GuardedAction(List<Condition> guards, StatefulAction delegate) {
      Preconditions.checkNotNull(guards, "guards may not be null");
      Preconditions.checkNotNull(delegate, "delegate action is required");
      this.guards = Collections.unmodifiableList(guards);
      this.delegate = delegate;
   }

   @Override
   public String getName() {
      return "guarded(" + delegate.getName() + ")";
   }

   @Override
   public String getDescription() {
      if (guards.isEmpty()) {
         return delegate.getDescription();
      }
      return "if " + guards + " then " + delegate.getDescription();
   }

   @Override
   public boolean isSatisfiable(ActionContext context) {
      return delegate.isSatisfiable(context);
   }

   @Override
   public void activate(ActionContext context) {
      delegate.activate(context);
   }

   @Override
   public void deactivate(ActionContext context) {
      delegate.deactivate(context);
   }

   @Override
   public ActionState execute(ActionContext context) {
      if (!guardsPass(context)) {
         context.logger().trace("Guards not satisfied for [{}], skipping", delegate.getName());
         return ActionState.IDLE;
      }
      return delegate.execute(context);
   }

   @Override
   public ActionState keepFiring(ActionContext context, RuleEvent event, boolean conditionMatches) {
      return delegate.keepFiring(context, event, conditionMatches);
   }

   private boolean guardsPass(ActionContext context) {
      if (guards.isEmpty()) {
         return true;
      }
      if (!(context instanceof RuleContext)) {
         context.logger().warn("ActionContext is not a RuleContext, cannot evaluate guards");
         return true;
      }
      RuleContext ruleContext = (RuleContext) context;
      for (Condition guard : guards) {
         if (!guard.isSatisfiable(ruleContext)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public String toString() {
      return getDescription();
   }
}
