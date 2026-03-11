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
package com.iris.oculus.modules.automation.ux;

import java.util.function.Supplier;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.AutomationModel;
import com.iris.oculus.modules.BaseToolbar;
import com.iris.oculus.modules.automation.AutomationController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.widget.Toolbar;

public class AutomationToolbar extends BaseToolbar<AutomationModel> {
   private Action create;
   private Action enable;
   private Action disable;
   private Action delete;

   private JButton toggleButton;
   private ListenerRegistration listener;

   public AutomationToolbar(AutomationController controller) {
      this.create = Actions.build("New Automation", controller::createAutomation);
      this.enable = Actions.build("Enable", (Supplier<AutomationModel>) this::model, controller::enable);
      this.disable = Actions.build("Disable", (Supplier<AutomationModel>) this::model, controller::disable);
      this.delete = Actions.build("Delete", (Supplier<AutomationModel>) this::model, controller::delete);
      this.toggleButton = new JButton(enable);
      this.toggleButton.setEnabled(false);
   }

   @Override
   protected JPanel createComponent() {
      return Toolbar
            .builder()
            .left().addButton(create)
            .right().addComponent(toggleButton)
            .right().addButton(delete)
            .build();
   }

   @Override
   protected void setModel(AutomationModel model) {
      super.setModel(model);
      enable.setEnabled(true);
      disable.setEnabled(true);
      delete.setEnabled(true);
      syncToggleButtonState(model);
      if (listener != null) {
         listener.remove();
      }
      listener = model.addListener((p) -> syncToggleButtonState(model));
   }

   @Override
   protected void clearModel() {
      super.clearModel();
      enable.setEnabled(false);
      disable.setEnabled(false);
      delete.setEnabled(false);
      if (listener != null) {
         listener.remove();
         listener = null;
      }
   }

   private void syncToggleButtonState(AutomationModel model) {
      if (AutomationModel.STATE_ENABLED.equals(model.getState())) {
         toggleButton.setAction(disable);
      } else {
         toggleButton.setAction(enable);
      }
   }
}
