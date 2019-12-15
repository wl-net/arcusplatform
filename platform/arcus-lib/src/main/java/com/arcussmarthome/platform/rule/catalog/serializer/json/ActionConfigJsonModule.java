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
/**
 * 
 */
package com.arcussmarthome.platform.rule.catalog.serializer.json;

import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.inject.TypeLiteral;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.io.json.gson.GsonModule;
import com.arcussmarthome.platform.rule.catalog.action.config.ActionConfig;
import com.arcussmarthome.platform.rule.catalog.action.config.ActionListConfig;
import com.arcussmarthome.platform.rule.catalog.action.config.ForEachModelActionConfig;
import com.arcussmarthome.platform.rule.catalog.action.config.LogActionConfig;
import com.arcussmarthome.platform.rule.catalog.action.config.SendActionConfig;
import com.arcussmarthome.platform.rule.catalog.action.config.SendNotificationActionConfig;
import com.arcussmarthome.platform.rule.catalog.action.config.SetAttributeActionConfig;
import com.netflix.governator.annotations.Modules;

/**
 * 
 */
@Modules(include = GsonModule.class)
public class ActionConfigJsonModule extends AbstractIrisModule {
   private static final String ATT_TYPE = "_type";
   
   @Override
   protected void configure() {
      bindSetOf(TypeAdapterFactory.class)
         .addBinding()
         .toInstance(createActionConfigSerializer());
      bindSetOf(new TypeLiteral<TypeAdapter<?>>() {})
         .addBinding()
         .toInstance(new AttributeTypeAdapter());
      bindSetOf(new TypeLiteral<TypeAdapter<?>>() {})
         .addBinding()
         .toInstance(new TemplatedExpressionTypeAdapter());
   }
   
   private TypeAdapterFactory createActionConfigSerializer() {
            RuntimeTypeAdapterFactory<ActionConfig> actionConfigFactory = RuntimeTypeAdapterFactory.of(ActionConfig.class,ATT_TYPE);
            actionConfigFactory.registerSubtype(SendActionConfig.class);
            actionConfigFactory.registerSubtype(SendNotificationActionConfig.class);
            actionConfigFactory.registerSubtype(ForEachModelActionConfig.class);
            actionConfigFactory.registerSubtype(LogActionConfig.class);
            actionConfigFactory.registerSubtype(ActionListConfig.class);
            actionConfigFactory.registerSubtype(SetAttributeActionConfig.class, SetAttributeActionConfig.TYPE);
            return actionConfigFactory;
   }

}

