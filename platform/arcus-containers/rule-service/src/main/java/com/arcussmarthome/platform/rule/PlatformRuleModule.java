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
package com.arcussmarthome.platform.rule;

import com.google.inject.TypeLiteral;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.common.rule.action.Action;
import com.arcussmarthome.common.rule.condition.Condition;
import com.arcussmarthome.io.Deserializer;
import com.arcussmarthome.io.Serializer;
import com.arcussmarthome.io.java.JavaDeserializer;
import com.arcussmarthome.io.java.JavaSerializer;

/**
 * 
 */
public class PlatformRuleModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      bind(new TypeLiteral<Deserializer<Condition>>() {})
         .toInstance(JavaDeserializer.getInstance());
      bind(new TypeLiteral<Deserializer<Action>>() {})
         .toInstance(JavaDeserializer.getInstance());
      
      // don't create the serializers with a strict type, or they will leave out the polymorphic fields
      bind(new TypeLiteral<Serializer<Condition>>() {})
         .toInstance(JavaSerializer.getInstance());
      bind(new TypeLiteral<Serializer<Action>>() {})
         .toInstance(JavaSerializer.getInstance());
   }

}

