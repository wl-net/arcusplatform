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
package com.arcussmarthome.common.subsystem.alarm;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.arcussmarthome.common.subsystem.alarm.smoke.SmokeAlarm;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.FanCapability;
import com.arcussmarthome.messages.capability.SpaceHeaterCapability;
import com.arcussmarthome.messages.capability.SwitchCapability;
import com.arcussmarthome.messages.capability.ThermostatCapability;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.subs.AlarmSubsystemModel;
import com.arcussmarthome.model.query.expression.ExpressionCompiler;

public class FanShutoffAdapter
{
   //fans, thermostats or space heaters
   public static final String QUERY_FAN_SHUTOFF_CAPABLE_DEVICE = "base:caps contains 'fan' or base:caps contains 'therm' or base:caps contains 'spaceheater'";
   public static final Predicate<Model> IS_FAN_SHUTOFF_CAPABLE_DEVICE = ExpressionCompiler.compile(QUERY_FAN_SHUTOFF_CAPABLE_DEVICE);
   @SuppressWarnings("unchecked")
   public static final Predicate<Model> SHOULD_FAN_SHUTOFF_THE_DEVICE = Predicates.or(
      Predicates.and( com.arcussmarthome.model.predicate.Predicates.isA(FanCapability.NAMESPACE), com.arcussmarthome.model.predicate.Predicates.attributeNotEquals(FanCapability.ATTR_SPEED, 0) ),
      Predicates.and( com.arcussmarthome.model.predicate.Predicates.isA(SpaceHeaterCapability.NAMESPACE), com.arcussmarthome.model.predicate.Predicates.attributeNotEquals(SpaceHeaterCapability.ATTR_HEATSTATE, SpaceHeaterCapability.HEATSTATE_OFF) ),
      Predicates.and( com.arcussmarthome.model.predicate.Predicates.isA(ThermostatCapability.NAMESPACE), com.arcussmarthome.model.predicate.Predicates.attributeNotEquals(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF) )
  );
   
   private static final MessageBody SHUTOFF_MESSAGE_BODY_FOR_FAN = MessageBody.messageBuilder(Capability.CMD_SET_ATTRIBUTES)
      .withAttribute(FanCapability.ATTR_SPEED, new Integer(0))
      .create();
   
   private static final MessageBody SHUTOFF_MESSAGE_BODY_FOR_FAN_WITH_SWITCH = MessageBody.messageBuilder(Capability.CMD_SET_ATTRIBUTES)
      .withAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF)
      .create();
   
   private static final MessageBody SHUTOFF_MESSAGE_BODY_FOR_SPACEHEATER = MessageBody.messageBuilder(Capability.CMD_SET_ATTRIBUTES)
      .withAttribute(SpaceHeaterCapability.ATTR_HEATSTATE, SpaceHeaterCapability.HEATSTATE_OFF)
      .create();
   
   private static final MessageBody SHUTOFF_MESSAGE_BODY_FOR_THERMOSTAT = MessageBody.messageBuilder(Capability.CMD_SET_ATTRIBUTES)
      .withAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF)
      .create();

   private final SubsystemContext<? extends AlarmSubsystemModel> context;

   public FanShutoffAdapter(SubsystemContext<? extends AlarmSubsystemModel> context) {
      this.context = context;
   }
   
   public void fanShutoffIfNecessary(String alarm) {
      AlarmSubsystemModel alarmSubsystemModel = context.model();
      if(alarmSubsystemModel.getFanShutoffSupported(false) ) {
         if(CarbonMonoxideAlarm.NAME.equals(alarm)) {
            if(alarmSubsystemModel.getFanShutoffOnCO(true)) {
               fanShutoff();
            }
         }else if(SmokeAlarm.NAME.equals(alarm)) {
            if(alarmSubsystemModel.getFanShutoffOnSmoke(false)) {
               fanShutoff();
            }
         }
      }                 
   }
   
   private void fanShutoff() {
      Iterable<Model> capableDevices = context.models().getModels(SHOULD_FAN_SHUTOFF_THE_DEVICE);
      if(capableDevices != null) {
         for(Model curDevice : capableDevices) {
            fanShutoffForModel(curDevice);
         }         
      }
   }

   private void fanShutoffForModel(Model device)
   {
      MessageBody shutoffMsgBody = null;
      if(device.supports(FanCapability.NAMESPACE)) {
         shutoffMsgBody = createFanShutoffMessageForFan(device);         
      }else if(device.supports(SpaceHeaterCapability.NAMESPACE)) {
         shutoffMsgBody = createFanShutoffMessageForSpaceheater(device);
      }else if(device.supports(ThermostatCapability.NAMESPACE)) {
         shutoffMsgBody = createFanShutoffMessageForThermostat(device);
      }
      
      if(shutoffMsgBody != null) {
         AlarmSubsystemModel alarmSubsystem = context.model();
         context.setActor(Address.fromString(alarmSubsystem.getCurrentIncident()));
         try{
            context.request(device.getAddress(), shutoffMsgBody);
         }finally{
            context.setActor(null);
         }
      }
      
   }
   
   private MessageBody createFanShutoffMessageForThermostat(Model device)
   {
      return SHUTOFF_MESSAGE_BODY_FOR_THERMOSTAT;     
   }

   private MessageBody createFanShutoffMessageForFan(Model device) {      
      if(device.supports(SwitchCapability.NAMESPACE)) {
         return SHUTOFF_MESSAGE_BODY_FOR_FAN_WITH_SWITCH;
      }else{
         return SHUTOFF_MESSAGE_BODY_FOR_FAN;   
      }
   }
   
   private MessageBody createFanShutoffMessageForSpaceheater(Model device) {
      return SHUTOFF_MESSAGE_BODY_FOR_SPACEHEATER;     
   }
   
   
}

