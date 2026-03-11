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

import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;

/**
 * A single guard-to-action branch within an automation.
 * An automation can have multiple flows — when the trigger fires,
 * each flow's guards are checked and matching flows execute.
 */
public class AutomationFlow {

   private List<ConditionConfig> conditions = new ArrayList<>();
   private List<ActionConfig> actions = new ArrayList<>();

   public AutomationFlow() {
   }

   public AutomationFlow(List<ConditionConfig> conditions, List<ActionConfig> actions) {
      this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
      this.actions = actions != null ? new ArrayList<>(actions) : new ArrayList<>();
   }

   public List<ConditionConfig> getConditions() {
      return Collections.unmodifiableList(conditions);
   }

   public void setConditions(List<ConditionConfig> conditions) {
      this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
   }

   public List<ActionConfig> getActions() {
      return Collections.unmodifiableList(actions);
   }

   public void setActions(List<ActionConfig> actions) {
      this.actions = actions != null ? new ArrayList<>(actions) : new ArrayList<>();
   }

   @Override
   public String toString() {
      return "AutomationFlow [conditions=" + conditions + ", actions=" + actions + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((conditions == null) ? 0 : conditions.hashCode());
      result = prime * result + ((actions == null) ? 0 : actions.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      AutomationFlow other = (AutomationFlow) obj;
      if (conditions == null) {
         if (other.conditions != null) return false;
      }
      else if (!conditions.equals(other.conditions)) return false;
      if (actions == null) {
         if (other.actions != null) return false;
      }
      else if (!actions.equals(other.actions)) return false;
      return true;
   }
}
