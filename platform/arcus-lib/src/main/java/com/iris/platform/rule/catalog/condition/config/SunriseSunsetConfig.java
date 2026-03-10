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
import com.iris.common.rule.trigger.SunriseSunsetTrigger;

/**
 * Trigger config that fires at sunrise or sunset with an optional offset.
 */
public class SunriseSunsetConfig implements ConditionConfig {

   public static final String TYPE = "sunrise-sunset";

   private String mode = "SUNRISE";
   private int offsetMinutes = 0;

   public String getMode() {
      return mode;
   }

   public void setMode(String mode) {
      this.mode = mode;
   }

   public int getOffsetMinutes() {
      return offsetMinutes;
   }

   public void setOffsetMinutes(int offsetMinutes) {
      this.offsetMinutes = offsetMinutes;
   }

   @Override
   public String getType() {
      return TYPE;
   }

   @Override
   public Condition generate(Map<String, Object> values) {
      SunriseSunsetTrigger.Mode triggerMode = SunriseSunsetTrigger.Mode.valueOf(mode);
      return new SunriseSunsetTrigger(triggerMode, offsetMinutes);
   }

   @Override
   public String toString() {
      return "SunriseSunsetConfig [mode=" + mode + ", offsetMinutes=" + offsetMinutes + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((mode == null) ? 0 : mode.hashCode());
      result = prime * result + offsetMinutes;
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      SunriseSunsetConfig other = (SunriseSunsetConfig) obj;
      if (mode == null) {
         if (other.mode != null) return false;
      }
      else if (!mode.equals(other.mode)) return false;
      if (offsetMinutes != other.offsetMinutes) return false;
      return true;
   }
}
