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
package com.iris.oculus.modules.automation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import com.iris.client.IrisClientFactory;
import com.iris.client.event.ClientFuture;
import com.iris.client.model.AutomationModel;
import com.iris.client.service.AutomationService;
import com.iris.client.service.AutomationService.ListAutomationsResponse;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.BaseController;
import com.iris.oculus.modules.automation.ux.AutomationCreatorWizard;

@Singleton
public class AutomationController extends BaseController<AutomationModel> {

   public AutomationController() {
      super(AutomationModel.class);
   }

   public void createAutomation() {
      AutomationCreatorWizard.create(getPlaceId().toString());
   }

   public void enable(AutomationModel model) {
      Oculus.showProgress(
            model.enable(),
            "Enabling automation..."
      );
   }

   public void disable(AutomationModel model) {
      Oculus.showProgress(
            model.disable(),
            "Disabling automation..."
      );
   }

   public void delete(AutomationModel model) {
      Oculus.showProgress(
            model.delete(),
            "Deleting automation..."
      );
   }

   public ClientFuture<List<Map<String, Object>>> getStartingPoints() {
      return IrisClientFactory
            .getService(AutomationService.class)
            .getStartingPoints(getPlaceId().toString())
            .transform(r -> r.getTriggers());
   }

   public ClientFuture<AutomationService.GetNextStepsResponse> getNextSteps(
         Map<String, Object> trigger,
         List<Map<String, Object>> conditions) {
      return IrisClientFactory
            .getService(AutomationService.class)
            .getNextSteps(getPlaceId().toString(), trigger, conditions);
   }

   public ClientFuture<AutomationService.CreateResponse> create(
         String name,
         String description,
         Map<String, Object> trigger,
         List<Map<String, Object>> conditions,
         List<Map<String, Object>> actions) {
      return IrisClientFactory
            .getService(AutomationService.class)
            .create(getPlaceId().toString(), name, description, trigger, conditions, actions);
   }

   @Override
   protected ClientFuture<? extends Collection<Map<String, Object>>> doLoad() {
      return IrisClientFactory
            .getService(AutomationService.class)
            .listAutomations(getPlaceId().toString())
            .transform(ListAutomationsResponse::getAutomations);
   }
}
