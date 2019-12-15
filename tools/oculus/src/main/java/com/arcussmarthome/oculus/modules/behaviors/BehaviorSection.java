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
package com.arcussmarthome.oculus.modules.behaviors;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.arcussmarthome.client.ClientEvent;
import com.arcussmarthome.client.IrisClientFactory;
import com.arcussmarthome.client.bean.CareBehavior;
import com.arcussmarthome.client.bean.TimeWindow;
import com.arcussmarthome.client.capability.Capability.ValueChangeEvent;
import com.arcussmarthome.client.capability.CareSubsystem;
import com.arcussmarthome.oculus.OculusSection;
import com.arcussmarthome.oculus.util.BaseComponentWrapper;
import com.arcussmarthome.oculus.widget.table.Table;
import com.arcussmarthome.oculus.widget.table.TableModel;
import com.arcussmarthome.oculus.widget.table.TableModelBuilder;

public class BehaviorSection extends BaseComponentWrapper<Component>implements OculusSection {
   private BehaviorController controller;

   @Inject
   public BehaviorSection(BehaviorController controller) {
      this.controller = controller;
   }

   @Override
   public String getName() {
      return "Behaviors";
   }

   protected Table<CareBehavior> createBehaviorTable() {
      TableModel<CareBehavior> behavior = TableModelBuilder
            .builder(controller.getBehaviors())
            .columnBuilder()
            .withName("Name")
            .withGetter((entry) -> entry.getName())
            .add()
            .columnBuilder()
            .withName("Active")
            .withGetter((entry) -> entry.getActive())
            .add()
            .columnBuilder()
            .withName("Enabled")
            .withGetter((entry) -> entry.getEnabled())
            .add()
            .columnBuilder()
            .withName("Type")
            .withGetter((entry) -> entry.getType())
            .add()
            .columnBuilder()
            .withName("Time Windows")
            .withGetter((entry) -> formatTimeWindow(entry.getTimeWindows()))
            .add()
            .columnBuilder()
            .withName("Last Activated")
            .withGetter((entry) -> entry.getLastActivated())
            .add()
            .columnBuilder()
            .withName("Last Fired")
            .withGetter((entry) -> entry.getLastFired())
            .add()
            .columnBuilder()
            .withName("Devices")
            .withGetter((entry) -> entry.getDevices())
            .add()
            .build();
      Table<CareBehavior> table = new Table<>(behavior);
      table.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent me) {
            Point p = me.getPoint();
            int row = table.rowAtPoint(p);
            int offset = table.convertRowIndexToModel(row);
            CareBehavior model = table.getModel().getValue(offset);
            if (me.getClickCount() == 1) {
               controller.getSelectionModel().setSelection(model);
            }
            if (me.getClickCount() == 2) {
               // save this yummy dbl click event for later
            }
         }
      });
      return table;
   }

   private String formatTimeWindow(List<Map<String, Object>> windows) {
      if(windows==null){
         return "N/A";
      }
      List<String> times = new ArrayList<String>(windows.size());
      for (Map<String, Object> window : windows){
         TimeWindow tw = new TimeWindow(window);
         times.add(String.format("%s %s (%ss)", tw.getDay(), tw.getStartTime(), tw.getDurationSecs()));
      }
      return String.join(",", times);
   }

   protected JPanel createToolbar() {
      JPanel toolbar = new JPanel();
      toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
      toolbar.add(new JButton(controller.refreshAction()));
      toolbar.add(Box.createHorizontalGlue());
      toolbar.add(new JButton(controller.deleteAction()));
      return toolbar;
   }

   @Override
   protected Component createComponent() {
      JPanel content = new JPanel(new BorderLayout());
      content.add(new JScrollPane(createBehaviorTable()), BorderLayout.CENTER);
      content.add(createToolbar(), BorderLayout.SOUTH);
      content.addComponentListener(new ComponentAdapter() {

         @Override
         public void componentShown(ComponentEvent e) {
            controller.refreshBehaviors();
         }

      });

      IrisClientFactory
            .getClient().addMessageListener(l -> {
               if (isBehavior().apply(l.getEvent())) {
                  controller.refreshBehaviors();
               }
            });
      return content;
   }

   private Predicate<ClientEvent> isBehavior() {
      return new Predicate<ClientEvent>() {
         @Override
         public boolean apply(ClientEvent event) {
            if (ValueChangeEvent.NAME.equals(event.getType()) &&
                  (event.getAttributes().containsKey(CareSubsystem.ATTR_BEHAVIORS) ||
                        event.getAttributes().containsKey(CareSubsystem.ATTR_ACTIVEBEHAVIORS))) {
               return true;
            }
            return false;
         }
      };
   }
}

