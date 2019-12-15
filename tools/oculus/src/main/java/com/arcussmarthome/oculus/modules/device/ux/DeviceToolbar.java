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
package com.arcussmarthome.oculus.modules.device.ux;

import javax.swing.Action;
import javax.swing.JPanel;

import com.arcussmarthome.bootstrap.ServiceLocator;
import com.arcussmarthome.client.capability.DeviceMock;
import com.arcussmarthome.client.model.DeviceModel;
import com.arcussmarthome.oculus.Oculus;
import com.arcussmarthome.oculus.modules.BaseToolbar;
import com.arcussmarthome.oculus.modules.device.dialog.MockActionsDialog;
import com.arcussmarthome.oculus.modules.device.mockaction.MockActionsNexus;
import com.arcussmarthome.oculus.modules.scheduler.SchedulerController;
import com.arcussmarthome.oculus.util.Actions;
import com.arcussmarthome.oculus.widget.Toolbar;

/**
 * @author tweidlin
 *
 */
public class DeviceToolbar extends BaseToolbar<DeviceModel> {
   // TODO should these actions from from a per-device controller?
   private Action delete = Actions.build("Delete", () -> onDelete(false)); 
   private Action forceDelete = Actions.build("Force Delete", () -> onDelete(true));
   private Action schedule = Actions.build("Schedule", this::onSchedule);
   private Action actions = Actions.build("Mock Actions", this::onActions);

   private MockActionsDialog mockActionsDialog = null;

   public DeviceToolbar() {
   }

   @Override
   protected JPanel createComponent() {
      return 
         Toolbar
            .builder()
            .left().addButton(actions)
            .right().addButton(schedule)
            .right().addButton(delete)
            .right().addButton(forceDelete)
            .right().addButton(refresh())
            .right().addButton(save())
            .build();
   }

   protected void onActions() {
      getMockActionsDialog(true).show(model());
   }

   protected void onDelete(boolean force) {
      delete.setEnabled(false);
      forceDelete.setEnabled(false);

      if (!force) {
         this
            .model()
            .remove(300*1000L)
            .onFailure((error) -> onDeleteFailed(error))
            .onCompletion((v) -> { delete.setEnabled(model() != null); forceDelete.setEnabled(model() != null); })
            ;
      } else {
         this
            .model()
            .forceRemove()
            .onFailure((error) -> onDeleteFailed(error))
            .onCompletion((v) -> { delete.setEnabled(model() != null); forceDelete.setEnabled(model() != null); })
            ;
      }
   }

   protected void onSchedule() {
      ServiceLocator
         .getInstance(SchedulerController.class)
         .addScheduledEvent(model().getAddress());
   }
   
   protected MockActionsDialog getMockActionsDialog(boolean create) {
      if (mockActionsDialog == null && create) {
         mockActionsDialog = new MockActionsDialog(ServiceLocator.getInstance(MockActionsNexus.class));
      }
      return mockActionsDialog;
   }
   
   protected void onDeleteFailed(Throwable error) {
      Oculus.error("Unable to delete object: " + error.getMessage(), error);
   }

   @Override
   protected void setModel(DeviceModel model) {
      super.setModel(model);
      delete.setEnabled(true);
      forceDelete.setEnabled(true);
      schedule.setEnabled(true);
      if (model.getCaps().contains(DeviceMock.NAMESPACE)) {
         if(mockActionsDialog != null && mockActionsDialog.isVisible()) {
            mockActionsDialog.show(model);
         }
         actions.setEnabled(true);
      }
      else {
         actions.setEnabled(false);
      }
   }

   @Override
   protected void clearModel() {
      super.clearModel();
      MockActionsDialog actionsDialog = getMockActionsDialog(false);
      if (actionsDialog != null) {
         mockActionsDialog.setVisible(false);
      }
      actions.setEnabled(false);
      delete.setEnabled(false);
      forceDelete.setEnabled(false);
      schedule.setEnabled(false);
   }
}

