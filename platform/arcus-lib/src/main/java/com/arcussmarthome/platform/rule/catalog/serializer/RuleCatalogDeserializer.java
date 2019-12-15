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
package com.arcussmarthome.platform.rule.catalog.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.capability.registry.CapabilityRegistry;
import com.arcussmarthome.io.Deserializer;
import com.arcussmarthome.platform.rule.catalog.RuleCatalog;
import com.arcussmarthome.serializer.sax.SAXTagHandlers;
import com.arcussmarthome.validators.Validator;

@Singleton
public class RuleCatalogDeserializer implements Deserializer<Map<String,RuleCatalog>> {

   @Inject
   public RuleCatalogDeserializer(CapabilityRegistry registry) {
      // ensures one is registered for the processors to use
      Preconditions.checkNotNull(registry, "registry may not be null");
   }

   @Override
   public Map<String,RuleCatalog> deserialize(byte[] input) throws IllegalArgumentException {
      try {
         return deserialize(new ByteArrayInputStream(input));
      }
      catch(RuntimeException e) {
         throw e;
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public Map<String,RuleCatalog> deserialize(InputStream input) throws IOException, IllegalArgumentException {
      try {
         Validator v = new Validator();
         RuleCatalogProcessor handler = new RuleCatalogProcessor(v);
         SAXTagHandlers.parse(input, RuleCatalogProcessor.TAG, handler);
         v.throwIfErrors();
         return handler.getCatalogs();
      }
      catch(IOException | RuntimeException e) {
         throw e;
      }
      catch(Exception e) {
         throw new IOException("Error parsing file", e);
      }
   }

}

