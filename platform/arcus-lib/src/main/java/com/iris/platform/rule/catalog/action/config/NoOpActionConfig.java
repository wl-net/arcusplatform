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
package com.iris.platform.rule.catalog.action.config;

import java.util.Map;

import com.iris.common.rule.action.stateful.NoOpAction;
import com.iris.common.rule.action.stateful.StatefulAction;

/**
 * An action config that produces a no-op action.
 * Used as an explicit "do nothing" in guarded automation flows.
 */
public class NoOpActionConfig extends BaseActionConfig {
   public static final String TYPE = "no-op";

   @Override
   public String getType() {
      return TYPE;
   }

   @Override
   public StatefulAction createAction(Map<String, Object> variables) {
      return NoOpAction.INSTANCE;
   }

   @Override
   public int hashCode() {
      return TYPE.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof NoOpActionConfig;
   }

   @Override
   public String toString() {
      return "NoOpActionConfig []";
   }
}
