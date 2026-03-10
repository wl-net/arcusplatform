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

import java.util.Map;

import com.google.common.base.Predicate;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.GuardConditionAdapter;
import com.iris.messages.model.Model;

/**
 * A guard condition that checks the security alarm state.
 *
 * Checks the SecurityAlarmSubsystem model for current alarm mode:
 * - DISARMED
 * - ON (fully armed)
 * - PARTIAL (armed with some sensors bypassed)
 */
public class AlarmStateConfig implements ConditionConfig {

   public static final String TYPE = "alarm-state";

   public static final String STATE_DISARMED = "DISARMED";
   public static final String STATE_ON = "ON";
   public static final String STATE_PARTIAL = "PARTIAL";

   private String state = STATE_DISARMED;

   public String getState() {
      return state;
   }

   public void setState(String state) {
      this.state = state;
   }

   @Override
   public String getType() {
      return TYPE;
   }

   @Override
   public Condition generate(Map<String, Object> values) {
      String description = "alarm is " + state;
      Predicate<Model> predicate = model -> {
         if (model == null) return false;
         String address = String.valueOf(model.getAttribute("base:address"));
         if (!address.contains("subsecurityalarm")) return false;
         Object alarmMode = model.getAttribute("subsecurityalarm:alarmMode");
         if (alarmMode == null) return STATE_DISARMED.equals(state);
         return state.equals(alarmMode.toString());
      };

      return new GuardConditionAdapter(description, "subsecurityalarm:", predicate);
   }

   @Override
   public String toString() {
      return "AlarmStateConfig [state=" + state + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((state == null) ? 0 : state.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      AlarmStateConfig other = (AlarmStateConfig) obj;
      if (state == null) {
         if (other.state != null) return false;
      }
      else if (!state.equals(other.state)) return false;
      return true;
   }
}
