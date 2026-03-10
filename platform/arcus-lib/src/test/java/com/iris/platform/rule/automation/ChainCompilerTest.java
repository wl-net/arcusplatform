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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.GuardedCondition;
import com.iris.common.rule.trigger.ValueChangeTrigger;
import com.iris.platform.rule.catalog.action.config.LogActionConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;
import com.iris.platform.rule.catalog.condition.config.PresenceConfig;
import com.iris.platform.rule.catalog.condition.config.ValueChangeConfig;
import com.iris.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({ RuleConfigJsonModule.class })
public class ChainCompilerTest extends IrisTestCase {

   @Test
   public void testCompileTriggerOnly() {
      ValueChangeConfig trigger = new ValueChangeConfig();
      trigger.setAttributeExpression(new TemplatedExpression("mot:motion"));
      trigger.setNewValueExpression(new TemplatedExpression("DETECTED"));

      Condition condition = ChainCompiler.compileCondition(
            trigger,
            Collections.emptyList(),
            ImmutableMap.of()
      );

      assertTrue("Expected ValueChangeTrigger, got " + condition.getClass(),
            condition instanceof ValueChangeTrigger);
   }

   @Test
   public void testCompileTriggerWithGuards() {
      ValueChangeConfig trigger = new ValueChangeConfig();
      trigger.setAttributeExpression(new TemplatedExpression("mot:motion"));
      trigger.setNewValueExpression(new TemplatedExpression("DETECTED"));

      PresenceConfig guard = new PresenceConfig();
      guard.setMode(PresenceConfig.MODE_OCCUPIED);

      Condition condition = ChainCompiler.compileCondition(
            trigger,
            Arrays.asList(guard),
            ImmutableMap.of()
      );

      assertTrue("Expected GuardedCondition, got " + condition.getClass(),
            condition instanceof GuardedCondition);
   }

   @Test
   public void testCompileSingleAction() {
      LogActionConfig action = new LogActionConfig("test");

      StatefulAction compiled = ChainCompiler.compileActions(
            Arrays.asList(action),
            ImmutableMap.of()
      );

      assertNotNull(compiled);
   }

   @Test
   public void testCompileMultipleActions() {
      LogActionConfig action1 = new LogActionConfig("first");
      LogActionConfig action2 = new LogActionConfig("second");

      StatefulAction compiled = ChainCompiler.compileActions(
            Arrays.asList(action1, action2),
            ImmutableMap.of()
      );

      assertNotNull(compiled);
   }

   @Test(expected = NullPointerException.class)
   public void testCompileNullTrigger() {
      ChainCompiler.compileCondition(null, Collections.emptyList(), ImmutableMap.of());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCompileEmptyActions() {
      ChainCompiler.compileActions(Collections.emptyList(), ImmutableMap.of());
   }
}
