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
import java.util.List;

import com.iris.platform.rule.catalog.condition.config.ConditionConfig;

/**
 * JSON-serializable wrapper for the automation chain's trigger and conditions.
 * Stored in the conditionconfig column of the RuleEnvironment table.
 *
 * Example JSON:
 * {
 *   "trigger": {"type": "value-change", "attribute": "mot:motion", ...},
 *   "conditions": [
 *     {"type": "presence", "mode": "OCCUPIED"},
 *     {"type": "time-window", "afterTime": "21:00:00", "beforeTime": "06:00:00"}
 *   ]
 * }
 */
public class AutomationChainConfig {

   private ConditionConfig trigger;
   private List<ConditionConfig> conditions = new ArrayList<>();
   private List<AutomationFlow> flows;

   public ConditionConfig getTrigger() {
      return trigger;
   }

   public void setTrigger(ConditionConfig trigger) {
      this.trigger = trigger;
   }

   public List<ConditionConfig> getConditions() {
      return conditions;
   }

   public void setConditions(List<ConditionConfig> conditions) {
      this.conditions = conditions != null ? conditions : new ArrayList<>();
   }

   public List<AutomationFlow> getFlows() {
      return flows;
   }

   public void setFlows(List<AutomationFlow> flows) {
      this.flows = flows;
   }
}
