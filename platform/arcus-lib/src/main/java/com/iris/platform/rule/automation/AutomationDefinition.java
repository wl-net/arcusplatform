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
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.condition.Condition;
import com.iris.platform.rule.BaseDefinition;
import com.iris.platform.rule.RuleEnvironment;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.action.config.ActionListConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;

/**
 * Represents a user-composed automation chain: trigger -> conditions -> actions.
 * Unlike template-based rules, automations are built from composable building
 * blocks that users select step-by-step.
 */
public class AutomationDefinition extends BaseDefinition<AutomationDefinition> {

   public static final String TYPE = "automation";

   private boolean disabled;
   private ConditionConfig trigger;
   private List<ConditionConfig> conditions = new ArrayList<>();
   private List<ActionConfig> actions = new ArrayList<>();
   private Date lastExecuted;

   @Override
   public String getType() {
      return TYPE;
   }

   public boolean isDisabled() {
      return disabled;
   }

   public void setDisabled(boolean disabled) {
      this.disabled = disabled;
   }

   /**
    * The starting point trigger for this automation.
    */
   public ConditionConfig getTrigger() {
      return trigger;
   }

   public void setTrigger(ConditionConfig trigger) {
      this.trigger = trigger;
   }

   /**
    * Guard conditions that must all be true when the trigger fires.
    */
   public List<ConditionConfig> getConditions() {
      return Collections.unmodifiableList(conditions);
   }

   public void setConditions(List<ConditionConfig> conditions) {
      this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
   }

   /**
    * The actions to execute when trigger fires and conditions are met.
    */
   public List<ActionConfig> getActions() {
      return Collections.unmodifiableList(actions);
   }

   public void setActions(List<ActionConfig> actions) {
      this.actions = actions != null ? new ArrayList<>(actions) : new ArrayList<>();
   }

   public Date getLastExecuted() {
      return lastExecuted;
   }

   public void setLastExecuted(Date lastExecuted) {
      this.lastExecuted = lastExecuted;
   }

   /**
    * Compiles the trigger + conditions into a single Condition that can be
    * used by the rule engine. The trigger is wrapped with guard conditions
    * using IfConditionConfig semantics.
    */
   public Condition createCondition(RuleEnvironment environment) {
      Map<String, Object> values = Collections.emptyMap();
      return ChainCompiler.compileCondition(trigger, conditions, values);
   }

   /**
    * Compiles the action list into a StatefulAction.
    */
   public StatefulAction createAction(RuleEnvironment environment) {
      Map<String, Object> values = Collections.emptyMap();
      return ChainCompiler.compileActions(actions, values);
   }

   @Override
   public AutomationDefinition copy() {
      try {
         AutomationDefinition copy = (AutomationDefinition) super.clone();
         copy.conditions = new ArrayList<>(this.conditions);
         copy.actions = new ArrayList<>(this.actions);
         if (this.lastExecuted != null) {
            copy.lastExecuted = new Date(this.lastExecuted.getTime());
         }
         return copy;
      }
      catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public String toString() {
      return "AutomationDefinition [placeId=" + getPlaceId()
            + ", sequenceId=" + getSequenceId()
            + ", name=" + getName()
            + ", disabled=" + disabled
            + ", trigger=" + trigger
            + ", conditions=" + conditions
            + ", actions=" + actions
            + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (disabled ? 1231 : 1237);
      result = prime * result + ((trigger == null) ? 0 : trigger.hashCode());
      result = prime * result + ((conditions == null) ? 0 : conditions.hashCode());
      result = prime * result + ((actions == null) ? 0 : actions.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      AutomationDefinition other = (AutomationDefinition) obj;
      if (disabled != other.disabled) return false;
      if (trigger == null) {
         if (other.trigger != null) return false;
      }
      else if (!trigger.equals(other.trigger)) return false;
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
