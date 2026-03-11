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
package com.iris.common.rule.trigger;

import java.util.Objects;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.messages.model.Model;

/**
 * Triggers when an attribute changes to any value in a given set.
 * Useful for enum-type attributes where the user wants to match
 * multiple values, e.g. alarm state is "ON" or "PARTIAL".
 */
@SuppressWarnings("serial")
public class ValueInSetTrigger extends SimpleTrigger {
   private final String attributeName;
   private final Set<Object> acceptedValues;
   private final Predicate<Model> queryPredicate;

   public ValueInSetTrigger(
         String attributeName,
         Set<Object> acceptedValues,
         Predicate<Model> queryPredicate
   ) {
      this.attributeName = attributeName;
      this.acceptedValues = ImmutableSet.copyOf(acceptedValues);
      this.queryPredicate = queryPredicate;
   }

   public ValueInSetTrigger(String attributeName, Set<Object> acceptedValues) {
      this(attributeName, acceptedValues, Predicates.alwaysTrue());
   }

   @Override
   public boolean handlesEventsOfType(RuleEventType type) {
      return RuleEventType.ATTRIBUTE_VALUE_CHANGED.equals(type);
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      for (Model model : context.getModels()) {
         if (model.getAttribute(attributeName) != null && queryPredicate.apply(model)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean shouldTrigger(ConditionContext context, RuleEvent event) {
      if (!(event instanceof AttributeValueChangedEvent)) {
         return false;
      }

      AttributeValueChangedEvent valueChange = (AttributeValueChangedEvent) event;
      if (!attributeName.equals(valueChange.getAttributeName())) {
         return false;
      }

      if (!queryPredicate.apply(context.getModelByAddress(valueChange.getAddress()))) {
         return false;
      }

      Object newValue = valueChange.getAttributeValue();
      Object oldValue = valueChange.getOldValue();

      if (Objects.equals(oldValue, newValue)) {
         context.logger().trace("Not firing because old value and new value are both [{}]", oldValue);
         return false;
      }

      if (!acceptedValues.contains(newValue)) {
         context.logger().trace("Not firing because new value [{}] is not in accepted set {}", newValue, acceptedValues);
         return false;
      }

      context.logger().debug("Firing on ValueInSet, attribute=[{}] new=[{}] accepted={}",
            attributeName, newValue, acceptedValues);
      return true;
   }

   @Override
   public String toString() {
      return "When " + attributeName + " changes to one of " + acceptedValues;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((attributeName == null) ? 0 : attributeName.hashCode());
      result = prime * result + ((acceptedValues == null) ? 0 : acceptedValues.hashCode());
      result = prime * result + ((queryPredicate == null) ? 0 : queryPredicate.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ValueInSetTrigger other = (ValueInSetTrigger) obj;
      if (!Objects.equals(attributeName, other.attributeName)) return false;
      if (!Objects.equals(acceptedValues, other.acceptedValues)) return false;
      if (!Objects.equals(queryPredicate, other.queryPredicate)) return false;
      return true;
   }
}
