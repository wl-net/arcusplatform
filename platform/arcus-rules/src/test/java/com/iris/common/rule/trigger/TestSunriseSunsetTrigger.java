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

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.event.ScheduledEventHandle;
import com.iris.common.sunrise.GeoLocation;
import com.iris.common.sunrise.SunriseSunsetCalc;
import com.iris.common.sunrise.SunriseSunsetInfo;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Model;

public class TestSunriseSunsetTrigger {

   private ConditionContext context;
   private SunriseSunsetCalc calculator;
   private Model placeModel;
   private UUID placeId;
   private Capture<Date> wakeUpCapture;

   @Before
   public void setUp() {
      placeId = UUID.randomUUID();
      context = EasyMock.createMock(ConditionContext.class);
      calculator = EasyMock.createMock(SunriseSunsetCalc.class);
      placeModel = EasyMock.createMock(Model.class);
      wakeUpCapture = EasyMock.newCapture();
   }

   @Test
   public void testHandlesScheduledEvents() {
      SunriseSunsetTrigger trigger = new SunriseSunsetTrigger(
            SunriseSunsetTrigger.Mode.SUNRISE, 0, calculator);
      assertTrue(trigger.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT));
      assertFalse(trigger.handlesEventsOfType(RuleEventType.ATTRIBUTE_VALUE_CHANGED));
   }

   @Test
   public void testActivateSchedulesWakeup() {
      Calendar now = calendar(2026, 3, 10, 6, 0);
      Calendar sunrise = calendar(2026, 3, 10, 7, 15);
      Calendar sunset = calendar(2026, 3, 10, 19, 30);

      expectLocationLookup(now, 38.9, -77.0);
      SunriseSunsetInfo info = new SunriseSunsetInfo(sunrise, sunset);
      EasyMock.expect(calculator.calculateSunriseSunset(
            EasyMock.anyObject(Calendar.class),
            EasyMock.anyObject(GeoLocation.class)))
            .andReturn(info);
      EasyMock.expect(context.wakeUpAt(EasyMock.capture(wakeUpCapture)))
            .andReturn(EasyMock.createMock(ScheduledEventHandle.class));

      EasyMock.replay(context, calculator, placeModel);

      SunriseSunsetTrigger trigger = new SunriseSunsetTrigger(
            SunriseSunsetTrigger.Mode.SUNRISE, 0, calculator);
      trigger.activate(context);

      assertTrue(wakeUpCapture.hasCaptured());
      assertEquals(sunrise.getTimeInMillis(), wakeUpCapture.getValue().getTime());

      EasyMock.verify(context, calculator, placeModel);
   }

   @Test
   public void testSunsetWithOffset() {
      Calendar now = calendar(2026, 3, 10, 12, 0);
      Calendar sunrise = calendar(2026, 3, 10, 7, 15);
      Calendar sunset = calendar(2026, 3, 10, 19, 30);

      expectLocationLookup(now, 38.9, -77.0);
      SunriseSunsetInfo info = new SunriseSunsetInfo(sunrise, sunset);
      EasyMock.expect(calculator.calculateSunriseSunset(
            EasyMock.anyObject(Calendar.class),
            EasyMock.anyObject(GeoLocation.class)))
            .andReturn(info);
      EasyMock.expect(context.wakeUpAt(EasyMock.capture(wakeUpCapture)))
            .andReturn(EasyMock.createMock(ScheduledEventHandle.class));

      EasyMock.replay(context, calculator, placeModel);

      SunriseSunsetTrigger trigger = new SunriseSunsetTrigger(
            SunriseSunsetTrigger.Mode.SUNSET, -30, calculator);
      trigger.activate(context);

      assertTrue(wakeUpCapture.hasCaptured());
      // Sunset 19:30 minus 30 minutes = 19:00
      long expectedMs = sunset.getTimeInMillis() - (30 * 60 * 1000L);
      assertEquals(expectedMs, wakeUpCapture.getValue().getTime());

      EasyMock.verify(context, calculator, placeModel);
   }

   @Test
   public void testToString() {
      SunriseSunsetTrigger sunrise = new SunriseSunsetTrigger(
            SunriseSunsetTrigger.Mode.SUNRISE, 0, calculator);
      assertEquals("At sunrise", sunrise.toString());

      SunriseSunsetTrigger sunsetPlus = new SunriseSunsetTrigger(
            SunriseSunsetTrigger.Mode.SUNSET, 15, calculator);
      assertEquals("At sunset + 15 min", sunsetPlus.toString());

      SunriseSunsetTrigger sunriseMinus = new SunriseSunsetTrigger(
            SunriseSunsetTrigger.Mode.SUNRISE, -10, calculator);
      assertEquals("At sunrise - 10 min", sunriseMinus.toString());
   }

   private void expectLocationLookup(Calendar now, double lat, double lng) {
      EasyMock.expect(context.getLocalTime()).andReturn(now).anyTimes();
      EasyMock.expect(context.getPlaceId()).andReturn(placeId).anyTimes();
      EasyMock.expect(context.getModelByAddress(
            Address.platformService(placeId, PlaceCapability.NAMESPACE)))
            .andReturn(placeModel).anyTimes();
      EasyMock.expect(placeModel.getAttribute(PlaceCapability.ATTR_ADDRLATITUDE))
            .andReturn(lat).anyTimes();
      EasyMock.expect(placeModel.getAttribute(PlaceCapability.ATTR_ADDRLONGITUDE))
            .andReturn(lng).anyTimes();
   }

   private Calendar calendar(int year, int month, int day, int hour, int minute) {
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
      cal.set(year, month - 1, day, hour, minute, 0);
      cal.set(Calendar.MILLISECOND, 0);
      return cal;
   }
}
