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
package com.arcussmarthome.alexa.shs.handlers.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.arcussmarthome.alexa.message.AlexaMessage;
import com.arcussmarthome.alexa.message.Header;
import com.arcussmarthome.alexa.message.v2.Appliance;
import com.arcussmarthome.alexa.message.v2.DoubleValue;
import com.arcussmarthome.alexa.message.v2.error.ErrorPayloadException;
import com.arcussmarthome.alexa.message.v2.error.UnwillingToSetValueError;
import com.arcussmarthome.alexa.message.v2.error.ValueOutOfRangeError;
import com.arcussmarthome.alexa.message.v2.request.IncrementTargetTemperatureRequest;
import com.arcussmarthome.alexa.message.v2.response.IncrementTargetTemperatureConfirmation;
import com.arcussmarthome.alexa.message.v2.response.TemperatureConfirmationPayload;
import com.arcussmarthome.alexa.shs.ShsAssertions;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.ThermostatCapability;
import com.arcussmarthome.messages.service.AlexaService;
import com.arcussmarthome.util.IrisUUID;

public class TestTxfmIncrementTargetTemperature {

   private AlexaMessage incTargetTemp;
   private Appliance app;

   @Before
   public void setup() {
      Header h = Header.v2(IrisUUID.randomUUID().toString(), "IncrementTargetTemperatureRequest", "Alexa.ConnectedHome.Control");

      app = new Appliance();
      app.setApplianceId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());

      IncrementTargetTemperatureRequest payload = new IncrementTargetTemperatureRequest();
      payload.setDeltaTemperature(new DoubleValue(2.00));
      payload.setAccessToken("token");
      payload.setAppliance(app);

      incTargetTemp = new AlexaMessage(h, payload);
   }

   @Test
   public void testIncrementTargetTemperature() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(incTargetTemp);

      ShsAssertions.assertExecuteRequest(
         platMsg,
         app.getApplianceId(),
         "IncrementTargetTemperatureRequest",
         ImmutableMap.of("deltaTemperature", 2.00),
         null,
         false
      );
   }

   @Test
   public void testSuccess() {
      MessageBody body = ThermostatCapability.IncrementIdealTemperatureResponse.builder()
         .withHvacmode(ThermostatCapability.HVACMODE_COOL)
         .withIdealTempSet(21.11)
         .withPrevIdealTemp(20.00)
         .withMaxSetPoint(35.00)
         .withMinSetPoint(1.67)
         .withResult(true)
         .build();

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withPayload(body.getAttributes())
         .build();

      AlexaMessage response = TxfmTestUtil.txfmResponse(incTargetTemp, resp);
      ShsAssertions.assertCommonResponseHeader(incTargetTemp, response, "IncrementTargetTemperatureConfirmation", "2");
      assertTrue(response.getPayload() instanceof IncrementTargetTemperatureConfirmation);
      IncrementTargetTemperatureConfirmation confirm = (IncrementTargetTemperatureConfirmation) response.getPayload();
      TemperatureConfirmationPayload.PreviousState previousState = confirm.getPreviousState();
      assertEquals(ThermostatCapability.HVACMODE_COOL, previousState.getMode().getValue());
      assertEquals(20.00, previousState.getTargetTemperature().getValue(), .001);
      assertEquals(ThermostatCapability.HVACMODE_COOL, confirm.getTemperatureMode().getValue());
      assertEquals(21.11, confirm.getTargetTemperature().getValue(), .001);
   }

   @Test
   public void testOutOfRange() {
      MessageBody body = ThermostatCapability.IncrementIdealTemperatureResponse.builder()
         .withHvacmode(ThermostatCapability.HVACMODE_COOL)
         .withIdealTempSet(21.11)
         .withPrevIdealTemp(20.00)
         .withMaxSetPoint(35.00)
         .withMinSetPoint(1.67)
         .withResult(false)
         .build();

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withPayload(body.getAttributes())
         .build();

      try {
         TxfmTestUtil.txfmResponse(incTargetTemp, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof ValueOutOfRangeError);
         ValueOutOfRangeError e = (ValueOutOfRangeError) epe.getPayload();
         assertEquals(1.67, e.getMinimumValue(), .001);
         assertEquals(35.00, e.getMaximumValue(), .001);
      }
   }

   @Test
   public void testThermostatOff() {
      MessageBody body = ThermostatCapability.IncrementIdealTemperatureResponse.builder()
         .withHvacmode(ThermostatCapability.HVACMODE_OFF)
         .withIdealTempSet(21.11)
         .withPrevIdealTemp(20.00)
         .withMaxSetPoint(35.00)
         .withMinSetPoint(1.67)
         .withResult(false)
         .build();

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withPayload(body.getAttributes())
         .build();

      try {
         TxfmTestUtil.txfmResponse(incTargetTemp, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof UnwillingToSetValueError);
         UnwillingToSetValueError e = (UnwillingToSetValueError) epe.getPayload();
         assertEquals("ThermostatIsOff", e.getErrorInfo().getCode());
      }
   }
}

