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

import java.util.Calendar;

import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.time.TimeOfDay;

/**
 * A guard condition that checks whether the current time falls within
 * a time window. Used in automation chains as a condition block.
 *
 * Unlike TimeOfDayFilter, this doesn't wrap a delegate — it's a standalone
 * guard used with GuardedCondition to gate a trigger.
 */
@SuppressWarnings("serial")
public class TimeWindowGuard implements Condition {

   private final String description;
   private final TimeOfDay startTime;
   private final TimeOfDay endTime;

   public TimeWindowGuard(String description, TimeOfDay startTime, TimeOfDay endTime) {
      this.description = description;
      this.startTime = startTime;
      this.endTime = endTime;
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      Calendar localTime = context.getLocalTime();
      TimeOfDay current = new TimeOfDay(localTime);
      int currentSecs = current.toSeconds();
      int startSecs = startTime.toSeconds();
      int endSecs = endTime.toSeconds();

      if (startSecs <= endSecs) {
         // Normal window: e.g. 08:00 - 17:00
         return currentSecs >= startSecs && currentSecs < endSecs;
      } else {
         // Overnight window: e.g. 22:00 - 06:00
         return currentSecs >= startSecs || currentSecs < endSecs;
      }
   }

   @Override
   public boolean shouldFire(ConditionContext context, RuleEvent event) {
      // Guards never fire — they only gate triggers via isSatisfiable
      return false;
   }

   @Override
   public boolean handlesEventsOfType(RuleEventType type) {
      return false;
   }

   @Override
   public boolean isSimpleTrigger() {
      return false;
   }

   @Override
   public void activate(ConditionContext context) {
      // no-op — stateless guard
   }

   @Override
   public void deactivate(ConditionContext context) {
      // no-op — stateless guard
   }

   @Override
   public String toString() {
      return "Only " + description;
   }
}
