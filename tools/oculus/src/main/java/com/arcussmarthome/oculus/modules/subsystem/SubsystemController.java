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
package com.arcussmarthome.oculus.modules.subsystem;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.swing.Action;

import com.arcussmarthome.client.IrisClientFactory;
import com.arcussmarthome.client.capability.SafetySubsystem;
import com.arcussmarthome.client.capability.SecuritySubsystem;
import com.arcussmarthome.client.event.ClientFuture;
import com.arcussmarthome.client.model.SubsystemModel;
import com.arcussmarthome.client.service.SubsystemService;
import com.arcussmarthome.client.service.SubsystemService.ListSubsystemsResponse;
import com.arcussmarthome.oculus.Oculus;
import com.arcussmarthome.oculus.modules.BaseController;
import com.arcussmarthome.oculus.util.Actions;

/**
 * 
 */
public class SubsystemController extends BaseController<SubsystemModel> {

   public static String getState(SubsystemModel model) {
      if(Boolean.FALSE.equals(model.getAvailable())) {
         return "UNAVAILABLE";
      }
      if(model instanceof SecuritySubsystem) {
         return ((SecuritySubsystem) model).getAlarmState();
      }
      if(model instanceof SafetySubsystem) {
         return ((SafetySubsystem) model).getAlarm();
      }
      
      return "AVAILABLE";
   }
   
   private Action flushSubsystems = Actions.build("Flush", this::flushSubsystems);
   
   @Inject
   public SubsystemController() {
      super(SubsystemModel.class);
   }
   
   public Action actionFlushSubsystems() {
      return flushSubsystems;
   }
   
   @Override
   protected ClientFuture<? extends Collection<Map<String, Object>>> doLoad() {
      return
         IrisClientFactory.getService(SubsystemService.class)
            .listSubsystems(getPlaceId())
            .transform(ListSubsystemsResponse::getSubsystems);
   }

   public void flushSubsystems() {
      SubsystemService service = IrisClientFactory.getService(SubsystemService.class);
      service
         .reload()
         .onFailure((e) -> Oculus.warn("Error flushing subsystems", e))
         ;
   }

   @Override
   protected void onPlaceChanged(String newPlaceId) {
      super.onPlaceChanged(newPlaceId);
      reload();
   }

}

