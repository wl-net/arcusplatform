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

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.TimeZone;

import org.easymock.EasyMock;
import org.junit.Test;

import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.time.TimeOfDay;

public class TestTimeWindowGuard {

   @Test
   public void testInsideNormalWindow() {
      TimeWindowGuard guard = new TimeWindowGuard("test",
            new TimeOfDay(8, 0, 0), new TimeOfDay(17, 0, 0));

      ConditionContext context = mockContext(10, 30); // 10:30 AM
      assertTrue(guard.isSatisfiable(context));
      EasyMock.verify(context);
   }

   @Test
   public void testOutsideNormalWindow() {
      TimeWindowGuard guard = new TimeWindowGuard("test",
            new TimeOfDay(8, 0, 0), new TimeOfDay(17, 0, 0));

      ConditionContext context = mockContext(20, 0); // 8:00 PM
      assertFalse(guard.isSatisfiable(context));
      EasyMock.verify(context);
   }

   @Test
   public void testInsideOvernightWindow() {
      TimeWindowGuard guard = new TimeWindowGuard("test",
            new TimeOfDay(22, 0, 0), new TimeOfDay(6, 0, 0));

      ConditionContext context = mockContext(23, 30); // 11:30 PM
      assertTrue(guard.isSatisfiable(context));
      EasyMock.verify(context);
   }

   @Test
   public void testInsideOvernightWindowEarlyMorning() {
      TimeWindowGuard guard = new TimeWindowGuard("test",
            new TimeOfDay(22, 0, 0), new TimeOfDay(6, 0, 0));

      ConditionContext context = mockContext(3, 0); // 3:00 AM
      assertTrue(guard.isSatisfiable(context));
      EasyMock.verify(context);
   }

   @Test
   public void testOutsideOvernightWindow() {
      TimeWindowGuard guard = new TimeWindowGuard("test",
            new TimeOfDay(22, 0, 0), new TimeOfDay(6, 0, 0));

      ConditionContext context = mockContext(12, 0); // noon
      assertFalse(guard.isSatisfiable(context));
      EasyMock.verify(context);
   }

   @Test
   public void testShouldFireAlwaysFalse() {
      TimeWindowGuard guard = new TimeWindowGuard("test",
            new TimeOfDay(8, 0, 0), new TimeOfDay(17, 0, 0));

      ConditionContext context = mockContext(10, 0);
      assertFalse(guard.shouldFire(context, null));
      EasyMock.verify(context);
   }

   @Test
   public void testNotSimpleTrigger() {
      TimeWindowGuard guard = new TimeWindowGuard("test",
            new TimeOfDay(8, 0, 0), new TimeOfDay(17, 0, 0));
      assertFalse(guard.isSimpleTrigger());
   }

   @Test
   public void testHandlesNoEvents() {
      TimeWindowGuard guard = new TimeWindowGuard("test",
            new TimeOfDay(8, 0, 0), new TimeOfDay(17, 0, 0));
      assertFalse(guard.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT));
      assertFalse(guard.handlesEventsOfType(RuleEventType.ATTRIBUTE_VALUE_CHANGED));
   }

   private ConditionContext mockContext(int hour, int minute) {
      ConditionContext context = EasyMock.createMock(ConditionContext.class);
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
      cal.set(2026, Calendar.MARCH, 10, hour, minute, 0);
      cal.set(Calendar.MILLISECOND, 0);
      EasyMock.expect(context.getLocalTime()).andReturn(cal).anyTimes();
      EasyMock.replay(context);
      return context;
   }
}
