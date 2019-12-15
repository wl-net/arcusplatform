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
package com.arcussmarthome.alexa.shs.handlers.v3;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.arcussmarthome.alexa.AlexaInterfaces;
import com.arcussmarthome.alexa.message.AlexaMessage;
import com.arcussmarthome.alexa.message.Endpoint;
import com.arcussmarthome.alexa.message.Header;
import com.arcussmarthome.alexa.message.Scope;
import com.arcussmarthome.alexa.shs.ShsAssertions;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.util.IrisUUID;

public class TestTxfmActivate {

   private AlexaMessage activate;
   private Endpoint e;

   @Before
   public void setup() {
      Header h = Header.v3(IrisUUID.randomUUID().toString(), AlexaInterfaces.SceneController.REQUEST_ACTIVATE, AlexaInterfaces.SceneController.NAMESPACE, "corrtok");
      Scope s = new Scope("BearerToken", "token");
      e = new Endpoint(s, Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation(), null);
      activate = new AlexaMessage(h, ImmutableMap.of(), e, null);
   }

   @Test
   public void testActivate() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(activate);
      ShsAssertions.assertExecuteRequest(
         platMsg,
         e.getEndpointId(),
         AlexaInterfaces.SceneController.REQUEST_ACTIVATE,
         ImmutableMap.of(),
         "corrtok",
        true
      );
   }

   @Test
   public void testActivateResponse() {
      Map<String, Object> payload = ImmutableMap.of(
         "cause", ImmutableMap.of("type", "VOICE_INTERACTION")
      );

      MessageBody resp = TxfmTestUtil.executeResponse(payload, null, false);
      AlexaMessage msg = TxfmTestUtil.txfmResponse(activate, resp);
      ShsAssertions.assertCommonResponseHeader(activate, msg, AlexaInterfaces.SceneController.RESPONSE_ACTIVATE, "3");
      assertEquals(payload, msg.getPayload());
   }

}

