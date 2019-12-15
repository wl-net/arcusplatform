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
package com.arcussmarthome.common.subsystem.care.behavior.evaluators;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.common.subsystem.SubsystemUtils;
import com.arcussmarthome.common.subsystem.care.behavior.BehaviorUtil;
import com.arcussmarthome.common.subsystem.care.behavior.CareBehaviorTypeWrapper;
import com.arcussmarthome.common.subsystem.care.behavior.SubsystemVariableKey;
import com.arcussmarthome.common.subsystem.care.behavior.WeeklyTimeWindow;
import com.arcussmarthome.common.time.TimeOfDay;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.PresenceCapability;
import com.arcussmarthome.messages.errors.ErrorEventException;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.event.ScheduledEvent;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.dev.PresenceModel;
import com.arcussmarthome.messages.model.subs.CareSubsystemModel;
import com.arcussmarthome.messages.type.CareBehaviorPresence;

public class PresenceEvaluator extends BaseBehaviorEvaluator {
   
   public final static SubsystemVariableKey CURFEW_TIMEOUT = new SubsystemVariableKey("curfew-timeout");

   private final CareBehaviorPresence config;
   public PresenceEvaluator(CareBehaviorPresence config) {
      this.config = config;
   }
   
   public PresenceEvaluator(Map<String,Object> config) {
      this.config = new CareBehaviorPresence(config);
   }

   
   @Override
   public void onStart(SubsystemContext<CareSubsystemModel> context) {
      scheduleNextCheck(context);
   }

   @Override
   public void onTimeout(ScheduledEvent event, SubsystemContext<CareSubsystemModel> context) {
      if(SubsystemUtils.isMatchingTimeout(event, context,CURFEW_TIMEOUT.create(config.getId()))){
         checkPresence(context);
         scheduleNextCheck(context);
      }
   }
   
   @Override
   public void onAlarmModeChange(SubsystemContext<CareSubsystemModel> context) {
      if(context.model().isAlarmModeON()){
         scheduleNextCheck(context);
      }
      else{
         SubsystemUtils.clearTimeout(context, CURFEW_TIMEOUT.create(config.getId()));
      }
   }

   private void scheduleNextCheck(SubsystemContext<CareSubsystemModel> context){
      if(context.model().isAlarmModeON()){
         Date nextCurfewCheck = BehaviorUtil.nextDailyOccurence(context.getLocalTime(), this.config.getPresenceRequiredTime());
         SubsystemUtils.setTimeout(nextCurfewCheck, context, CURFEW_TIMEOUT.create(config.getId()));
      }
   }

   private void checkPresence(SubsystemContext<CareSubsystemModel> context){
      boolean allHome = true;
      if(config.getDevices()==null){
         return;
      }
      for(String deviceAddress:config.getDevices()){
         Model model=context.models().getModelByAddress(Address.fromString(deviceAddress));
         if(PresenceModel.getPresence(model).equals(PresenceCapability.PRESENCE_ABSENT)){
            addToLastTriggeredDevice(model.getAddress().getRepresentation(), context);
            allHome=false;
            break;
         }
      }
      if(!allHome){
         scheduleMonitorAlertTimeout(0, context);
      }
   }

   @Override
   public void validateConfig(SubsystemContext<CareSubsystemModel> context) {
      String curfewTime = this.config.getPresenceRequiredTime();
      if(curfewTime==null){
         throw new ErrorEventException(Errors.missingParam(CareBehaviorPresence.ATTR_PRESENCEREQUIREDTIME));
      }
      if(!TimeOfDay.isValidFormat(curfewTime)){
         throw new ErrorEventException(Errors.invalidParam(CareBehaviorPresence.ATTR_PRESENCEREQUIREDTIME));
      }
   }

   @Override
   public CareBehaviorTypeWrapper getCareBehavior() {
      return new CareBehaviorTypeWrapper(config.toMap());
   }

   @Override
   public List<WeeklyTimeWindow> getWeeklyTimeWindows() {
      return convertTimeWindows(config.getTimeWindows());
   }

   @Override
   public void onRemoved(SubsystemContext<CareSubsystemModel> context) {
      clearVar(CURFEW_TIMEOUT.create(getBehaviorId()), context);
   }
}

