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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.inject.Inject;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import com.iris.client.model.AutomationModel;
import com.iris.oculus.modules.BaseSection;
import com.iris.oculus.modules.BaseToolbar;
import com.iris.oculus.modules.automation.ux.AutomationToolbar;
import com.iris.oculus.modules.capability.ux.ModelStoreViewBuilder;
import com.iris.oculus.util.Models;
import com.iris.oculus.widget.table.Table;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;

public class AutomationSection extends BaseSection<AutomationModel> {
   private AutomationController controller;

   @Inject
   public AutomationSection(AutomationController controller) {
      super(controller);
      this.controller = controller;
   }

   @Override
   public String getName() {
      return "Automations";
   }

   private TableModel<AutomationModel> createTableModel() {
      return TableModelBuilder
            .builder(controller.getStore())
            .columnBuilder()
               .withName("Name")
               .withGetter(AutomationModel::getName)
               .add()
            .columnBuilder()
               .withName("Description")
               .withGetter(AutomationModel::getDescription)
               .add()
            .columnBuilder()
               .withName("State")
               .withGetter(AutomationModel::getState)
               .add()
            .columnBuilder()
               .withName("Last Executed")
               .withGetter(AutomationModel::getLastExecuted)
               .add()
            .columnBuilder()
               .withName("Modified")
               .withGetter(AutomationModel::getModified)
               .add()
            .build();
   }

   protected Component createSelectionTable() {
      AutomationToolbar toolbar = new AutomationToolbar(controller);
      TableModel<AutomationModel> model = createTableModel();
      Table<AutomationModel> table = new Table<>(model);
      table.setRowSorter(new TableRowSorter<TableModel<AutomationModel>>(model));
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
         @Override
         public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
               return;
            }
            int index = table.getSelectionModel().getMinSelectionIndex();
            if (index == -1) {
               toolbar.onEvent(null);
            } else {
               AutomationModel model = table.getModel().getValue(index);
               toolbar.onEvent(model);
            }
         }
      });
      table.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent me) {
            if (me.getClickCount() == 2) {
               Point p = me.getPoint();
               int row = table.rowAtPoint(p);
               int offset = table.convertRowIndexToModel(row);
               AutomationModel model = table.getModel().getValue(offset);
               controller.getSelection().setSelection(model);
            }
         }
      });

      JPanel contents = new JPanel(new BorderLayout());
      contents.add(new JScrollPane(table), BorderLayout.CENTER);
      contents.add(toolbar.getComponent(), BorderLayout.SOUTH);
      return contents;
   }

   @Override
   protected Component createComponent() {
      BaseToolbar<AutomationModel> toolbar = createToolbar();
      controller.addSelectedListener(toolbar);
      return ModelStoreViewBuilder
            .builder(controller.getStore())
            .withTypeName("Automation")
            .withListView(createSelectionTable())
            .withModelSelector(
                  Models::nameOf,
                  controller.getSelection(),
                  controller.actionReload()
            )
            .withToolbarComponent(toolbar.getComponent())
            .addShowListener((e) -> controller.reload())
            .build();
   }
}
