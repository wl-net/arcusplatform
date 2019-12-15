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
package com.arcussmarthome.platform.rule.catalog.action;

import com.arcussmarthome.io.json.JSON;
import com.arcussmarthome.messages.MessagesModule;
import com.arcussmarthome.platform.rule.catalog.action.config.ActionConfig;
import com.arcussmarthome.platform.rule.catalog.serializer.json.ActionConfigJsonModule;
import com.arcussmarthome.test.IrisTestCase;
import com.arcussmarthome.test.Modules;

@Modules({ ActionConfigJsonModule.class, MessagesModule.class })
public class BaseActionTest extends IrisTestCase{
   
   protected ActionConfig serializeDeserialize(ActionConfig config){
      String json = JSON.toJson(config);
      return JSON.fromJson(json, ActionConfig.class);
   }
}

