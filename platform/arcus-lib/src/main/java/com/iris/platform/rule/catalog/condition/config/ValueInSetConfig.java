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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.trigger.ValueInSetTrigger;

/**
 * Config for ValueInSetTrigger — fires when an attribute changes
 * to any value in a specified set. Useful for enum attributes
 * where the user wants to match multiple values (e.g. alarm ON or PARTIAL).
 */
public class ValueInSetConfig implements ConditionConfig {

   public static final String TYPE = "value-in-set";

   private String attribute;
   private Set<Object> acceptedValues = new LinkedHashSet<>();

   public String getAttribute() {
      return attribute;
   }

   public void setAttribute(String attribute) {
      this.attribute = attribute;
   }

   public Set<Object> getAcceptedValues() {
      return acceptedValues;
   }

   public void setAcceptedValues(Set<Object> acceptedValues) {
      this.acceptedValues = acceptedValues;
   }

   @Override
   public String getType() {
      return TYPE;
   }

   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(attribute != null, "must specify an attribute name");
      Preconditions.checkState(acceptedValues != null && !acceptedValues.isEmpty(),
            "must specify at least one accepted value");
      return new ValueInSetTrigger(attribute, ImmutableSet.copyOf(acceptedValues));
   }

   @Override
   public String toString() {
      return "ValueInSetConfig [attribute=" + attribute
            + ", acceptedValues=" + acceptedValues + "]";
   }

   @Override
   public int hashCode() {
      return Objects.hash(attribute, acceptedValues);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      ValueInSetConfig other = (ValueInSetConfig) obj;
      return Objects.equals(attribute, other.attribute)
            && Objects.equals(acceptedValues, other.acceptedValues);
   }
}
