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

import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.TimeWindowGuard;
import com.iris.common.rule.time.TimeOfDay;

/**
 * A guard condition that checks if the current time is within a window.
 * Used as a condition block in automation chains: "only between 8:00 and 17:00".
 *
 * Unlike TimeOfDayFilter (which wraps a delegate condition), this is a
 * standalone guard that can be composed via GuardedCondition.
 */
public class TimeWindowConfig implements ConditionConfig {

   public static final String TYPE = "time-window";

   private String startTime;
   private String endTime;

   public String getStartTime() {
      return startTime;
   }

   public void setStartTime(String startTime) {
      this.startTime = startTime;
   }

   public String getEndTime() {
      return endTime;
   }

   public void setEndTime(String endTime) {
      this.endTime = endTime;
   }

   @Override
   public String getType() {
      return TYPE;
   }

   @Override
   public Condition generate(Map<String, Object> values) {
      TimeOfDay start = TimeOfDay.fromString(startTime);
      TimeOfDay end = TimeOfDay.fromString(endTime);

      String description = "between " + startTime + " and " + endTime;

      // The guard returns isSatisfiable=true when any model matches.
      // We use the place model prefix so there's always exactly one match,
      // and the predicate checks the time window using the model's context.
      // Since guards are evaluated at trigger-fire time, we capture the
      // time check as a predicate that compares current time to the window.
      return new TimeWindowGuard(description, start, end);
   }

   @Override
   public String toString() {
      return "TimeWindowConfig [startTime=" + startTime + ", endTime=" + endTime + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((startTime == null) ? 0 : startTime.hashCode());
      result = prime * result + ((endTime == null) ? 0 : endTime.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      TimeWindowConfig other = (TimeWindowConfig) obj;
      if (startTime == null) {
         if (other.startTime != null) return false;
      }
      else if (!startTime.equals(other.startTime)) return false;
      if (endTime == null) {
         if (other.endTime != null) return false;
      }
      else if (!endTime.equals(other.endTime)) return false;
      return true;
   }
}
