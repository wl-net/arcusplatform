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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.messages.model.Model;

/**
 * A stateless guard condition that checks a predicate against models
 * in the context. Used by the automation chain engine for guard conditions
 * like "someone is home" or "alarm is armed".
 *
 * This is NOT a trigger — it doesn't fire events. It's used by
 * {@link GuardedCondition} to gate a trigger's execution.
 *
 * The predicate is evaluated against all models matching a given
 * address prefix. If any matching model satisfies the predicate,
 * the guard is considered met.
 */
public class GuardConditionAdapter implements Condition {

   private static final long serialVersionUID = 1L;

   private final String description;
   private final String addressPrefix;
   private final Predicate<Model> predicate;

   /**
    * @param description Human-readable description (e.g., "someone is home")
    * @param addressPrefix Address prefix to filter models (e.g., "subspres:")
    * @param predicate The predicate to evaluate against matching models
    */
   public GuardConditionAdapter(String description, String addressPrefix, Predicate<Model> predicate) {
      this.description = Preconditions.checkNotNull(description, "description is required");
      this.addressPrefix = Preconditions.checkNotNull(addressPrefix, "addressPrefix is required");
      this.predicate = Preconditions.checkNotNull(predicate, "predicate is required");
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      // Check if any model matches our predicate
      for (Model model : context.getModels()) {
         if (matchesPrefix(model) && predicate.apply(model)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean handlesEventsOfType(RuleEventType type) {
      // Guards respond to attribute changes (subsystem state changes)
      return type == RuleEventType.ATTRIBUTE_VALUE_CHANGED;
   }

   @Override
   public void activate(ConditionContext context) {
      // Stateless — nothing to do
   }

   @Override
   public void deactivate(ConditionContext context) {
      // Stateless — nothing to do
   }

   @Override
   public boolean shouldFire(ConditionContext context, RuleEvent event) {
      // Guards never fire on their own — they only gate triggers
      return false;
   }

   @Override
   public boolean isSimpleTrigger() {
      return false;
   }

   private boolean matchesPrefix(Model model) {
      if (model == null) return false;
      Object addr = model.getAttribute("base:address");
      return addr != null && addr.toString().contains(addressPrefix);
   }

   @Override
   public String toString() {
      return "Guard[" + description + "]";
   }
}
