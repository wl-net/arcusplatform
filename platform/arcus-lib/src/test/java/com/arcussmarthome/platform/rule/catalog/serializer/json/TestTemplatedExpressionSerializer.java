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
package com.arcussmarthome.platform.rule.catalog.serializer.json;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.arcussmarthome.io.json.JSON;
import com.arcussmarthome.platform.rule.catalog.template.TemplatedExpression;
import com.arcussmarthome.test.IrisTestCase;
import com.arcussmarthome.test.Modules;

@Modules({ RuleConfigJsonModule.class })
public class TestTemplatedExpressionSerializer extends IrisTestCase {


   @Test
   public void testDeserializer() {
      Object o = new Object();
      
      assertEquals(null, JSON.fromJson("null", TemplatedExpression.class));
      assertEquals("a", JSON.fromJson("'a'", TemplatedExpression.class).toTemplate().apply(ImmutableMap.of()));
      assertEquals(o, JSON.fromJson("'${o}'", TemplatedExpression.class).toTemplate().apply(ImmutableMap.of("o", o)));
      assertEquals("abc", JSON.fromJson("'a${o}c'", TemplatedExpression.class).toTemplate().apply(ImmutableMap.of("o", "b")));
   }


   @Test
   public void testSerializer() {
      assertEquals("\"a\"", JSON.toJson(new TemplatedExpression("a")));
      assertEquals("\"${o}\"", JSON.toJson(new TemplatedExpression("${o}")));
      assertEquals("\"a${o}c\"", JSON.toJson(new TemplatedExpression("a${o}c")));
   }
}

