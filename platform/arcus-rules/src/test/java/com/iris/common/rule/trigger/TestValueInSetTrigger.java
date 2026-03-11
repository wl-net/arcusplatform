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

import java.util.Collections;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;

public class TestValueInSetTrigger {

   private ConditionContext context;
   private Model model;
   private Address modelAddress;
   private ValueInSetTrigger trigger;

   @Before
   public void setUp() {
      context = EasyMock.createMock(ConditionContext.class);
      model = EasyMock.createMock(Model.class);
      modelAddress = Address.platformDriverAddress(UUID.randomUUID());

      EasyMock.expect(context.logger())
            .andReturn(LoggerFactory.getLogger(TestValueInSetTrigger.class))
            .anyTimes();

      trigger = new ValueInSetTrigger(
            "alarm:alertState",
            ImmutableSet.of("ON", "PARTIAL")
      );
   }

   @Test
   public void testHandlesValueChangeEvents() {
      assertTrue(trigger.handlesEventsOfType(RuleEventType.ATTRIBUTE_VALUE_CHANGED));
      assertFalse(trigger.handlesEventsOfType(RuleEventType.SCHEDULED_EVENT));
   }

   @Test
   public void testIsSatisfiableWithMatchingModel() {
      EasyMock.expect(context.getModels()).andReturn(ImmutableList.of(model));
      EasyMock.expect(model.getAttribute("alarm:alertState")).andReturn("DISARMED");
      EasyMock.replay(context, model);

      assertTrue(trigger.isSatisfiable(context));
      EasyMock.verify(context, model);
   }

   @Test
   public void testIsNotSatisfiableWithNoModels() {
      EasyMock.expect(context.getModels()).andReturn(Collections.emptyList());
      EasyMock.replay(context);

      assertFalse(trigger.isSatisfiable(context));
      EasyMock.verify(context);
   }

   @Test
   public void testTriggersWhenNewValueInSet() {
      AttributeValueChangedEvent event = AttributeValueChangedEvent.create(
            modelAddress, "alarm:alertState", "ON", "DISARMED");
      EasyMock.expect(context.getModelByAddress(modelAddress)).andReturn(model);
      EasyMock.replay(context, model);

      assertTrue(trigger.shouldTrigger(context, event));
      EasyMock.verify(context, model);
   }

   @Test
   public void testTriggersOnSecondAcceptedValue() {
      AttributeValueChangedEvent event = AttributeValueChangedEvent.create(
            modelAddress, "alarm:alertState", "PARTIAL", "DISARMED");
      EasyMock.expect(context.getModelByAddress(modelAddress)).andReturn(model);
      EasyMock.replay(context, model);

      assertTrue(trigger.shouldTrigger(context, event));
      EasyMock.verify(context, model);
   }

   @Test
   public void testDoesNotTriggerWhenNewValueNotInSet() {
      AttributeValueChangedEvent event = AttributeValueChangedEvent.create(
            modelAddress, "alarm:alertState", "DISARMED", "ON");
      EasyMock.expect(context.getModelByAddress(modelAddress)).andReturn(model);
      EasyMock.replay(context, model);

      assertFalse(trigger.shouldTrigger(context, event));
      EasyMock.verify(context, model);
   }

   @Test
   public void testDoesNotTriggerWhenValueUnchanged() {
      AttributeValueChangedEvent event = AttributeValueChangedEvent.create(
            modelAddress, "alarm:alertState", "ON", "ON");
      EasyMock.expect(context.getModelByAddress(modelAddress)).andReturn(model);
      EasyMock.replay(context, model);

      assertFalse(trigger.shouldTrigger(context, event));
      EasyMock.verify(context, model);
   }

   @Test
   public void testIgnoresWrongAttribute() {
      AttributeValueChangedEvent event = AttributeValueChangedEvent.create(
            modelAddress, "swit:state", "ON", "OFF");
      EasyMock.replay(context, model);

      assertFalse(trigger.shouldTrigger(context, event));
      EasyMock.verify(context, model);
   }

   @Test
   public void testIgnoresNonValueChangeEvent() {
      ScheduledEvent event = new ScheduledEvent(System.currentTimeMillis());
      EasyMock.replay(context, model);

      assertFalse(trigger.shouldTrigger(context, event));
      EasyMock.verify(context, model);
   }

   @Test
   public void testToString() {
      assertEquals("When alarm:alertState changes to one of [ON, PARTIAL]",
            trigger.toString());
   }

   @Test
   public void testEquality() {
      ValueInSetTrigger same = new ValueInSetTrigger(
            "alarm:alertState", ImmutableSet.of("ON", "PARTIAL"));
      ValueInSetTrigger different = new ValueInSetTrigger(
            "alarm:alertState", ImmutableSet.of("ON"));

      assertEquals(trigger, same);
      assertEquals(trigger.hashCode(), same.hashCode());
      assertNotEquals(trigger, different);
   }
}
