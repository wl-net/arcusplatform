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

import java.util.Calendar;
import java.util.Date;

import com.google.common.base.Preconditions;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.sunrise.GeoLocation;
import com.iris.common.sunrise.ReedellCalculatorWrapper;
import com.iris.common.sunrise.SunriseSunsetCalc;
import com.iris.common.sunrise.SunriseSunsetInfo;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Model;

/**
 * Fires at sunrise or sunset for the place's location, with an optional
 * offset in minutes (positive = after, negative = before).
 */
@SuppressWarnings("serial")
public class SunriseSunsetTrigger extends SimpleTrigger {
   private static final long JITTER_MS = 1000;
   private static final long ONE_DAY_MS = 24 * 60 * 60 * 1000;
   private static final double DEFAULT_LAT = 38.8977;  // Washington DC
   private static final double DEFAULT_LNG = -77.0365;

   public enum Mode { SUNRISE, SUNSET }

   private final SunriseSunsetCalc calculator;
   private final Mode mode;
   private final int offsetMinutes;

   private volatile long nextFireTime = -1;

   public SunriseSunsetTrigger(Mode mode, int offsetMinutes) {
      this(mode, offsetMinutes, new ReedellCalculatorWrapper());
   }

   public SunriseSunsetTrigger(Mode mode, int offsetMinutes, SunriseSunsetCalc calculator) {
      Preconditions.checkNotNull(mode, "mode may not be null");
      Preconditions.checkNotNull(calculator, "calculator may not be null");
      this.mode = mode;
      this.offsetMinutes = offsetMinutes;
      this.calculator = calculator;
   }

   @Override
   public void activate(ConditionContext context) {
      reschedule(context, context.getLocalTime());
   }

   @Override
   public boolean handlesEventsOfType(RuleEventType type) {
      return RuleEventType.SCHEDULED_EVENT == type;
   }

   @Override
   public boolean shouldTrigger(ConditionContext context, RuleEvent event) {
      Calendar localTime = context.getLocalTime();
      if (nextFireTime < 0) {
         reschedule(context, localTime);
         return false;
      }

      if (localTime.getTimeInMillis() < (nextFireTime - JITTER_MS)) {
         return false;
      }

      reschedule(context, localTime);
      return true;
   }

   private void reschedule(ConditionContext context, Calendar localTime) {
      GeoLocation location = getLocation(context);
      SunriseSunsetInfo info = calculator.calculateSunriseSunset(localTime, location);

      Calendar target;
      if (mode == Mode.SUNRISE) {
         target = info.getSunrise();
      } else {
         target = info.getSunset();
      }

      long targetMs = target.getTimeInMillis() + (offsetMinutes * 60L * 1000L);

      if (targetMs - localTime.getTimeInMillis() < JITTER_MS) {
         // Already passed today — calculate for tomorrow
         Calendar tomorrow = (Calendar) localTime.clone();
         tomorrow.add(Calendar.DAY_OF_YEAR, 1);
         info = calculator.calculateSunriseSunset(tomorrow, location);
         target = (mode == Mode.SUNRISE) ? info.getSunrise() : info.getSunset();
         targetMs = target.getTimeInMillis() + (offsetMinutes * 60L * 1000L);
      }

      nextFireTime = targetMs;
      context.wakeUpAt(new Date(nextFireTime));
   }

   private GeoLocation getLocation(ConditionContext context) {
      Model place = context.getModelByAddress(
            Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE));
      if (place != null) {
         Object lat = place.getAttribute(PlaceCapability.ATTR_ADDRLATITUDE);
         Object lng = place.getAttribute(PlaceCapability.ATTR_ADDRLONGITUDE);
         if (lat instanceof Number && lng instanceof Number) {
            return GeoLocation.fromCoordinates(
                  ((Number) lat).doubleValue(),
                  ((Number) lng).doubleValue());
         }
      }
      return GeoLocation.fromCoordinates(DEFAULT_LAT, DEFAULT_LNG);
   }

   @Override
   public String toString() {
      String desc = (mode == Mode.SUNRISE) ? "At sunrise" : "At sunset";
      if (offsetMinutes > 0) {
         desc += " + " + offsetMinutes + " min";
      } else if (offsetMinutes < 0) {
         desc += " - " + (-offsetMinutes) + " min";
      }
      return desc;
   }
}
