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
package com.arcussmarthome.voice.alexa.handlers;

import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.arcussmarthome.alexa.AlexaInterfaces;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.SimpleModel;
import com.arcussmarthome.messages.service.AlexaService;
import com.arcussmarthome.voice.alexa.AlexaConfig;

public class TestDirectiveTransformerReportState {

   private Model model;
   private AlexaConfig config;
   private MessageBody reportState;

   @Before
   public void setup() {
      model = new SimpleModel();
      model.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE));

      config = new AlexaConfig();

      reportState = AlexaService.ExecuteRequest.builder()
         .withDirective(AlexaInterfaces.REQUEST_REPORTSTATE)
         .build();
   }

   @Test
   public void testReportState() {
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(reportState).txfmRequest(reportState, model, config);
      assertFalse(optBody.isPresent());
   }
}

