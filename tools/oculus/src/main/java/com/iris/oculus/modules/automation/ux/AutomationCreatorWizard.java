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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.iris.client.IrisClientFactory;
import com.iris.client.service.AutomationService;
import com.iris.oculus.Oculus;
import com.iris.oculus.widget.Dialog;

/**
 * Multi-step wizard for creating an automation chain.
 *
 * Flow: Name/Description -> Select Trigger -> Add Conditions -> Add Actions -> Create
 */
public class AutomationCreatorWizard extends Dialog<Void> {

   private String placeId;

   // Chain state
   private Map<String, Object> selectedTrigger;
   private List<Map<String, Object>> selectedConditions = new ArrayList<>();
   private List<Map<String, Object>> selectedActions = new ArrayList<>();

   // UI components
   private JTextField nameField;
   private JTextField descriptionField;
   private JComboBox<BlockItem> triggerCombo;
   private DefaultListModel<String> conditionListModel;
   private DefaultListModel<String> actionListModel;
   private JComboBox<BlockItem> conditionCombo;
   private JComboBox<BlockItem> actionCombo;

   // Available blocks from server
   private List<Map<String, Object>> availableTriggers;
   private List<Map<String, Object>> availableConditions;
   private List<Map<String, Object>> availableActions;

   public static void create(String placeId) {
      AutomationCreatorWizard wizard = new AutomationCreatorWizard(placeId);
      wizard.prompt();
   }

   private AutomationCreatorWizard(String placeId) {
      this.placeId = placeId;
      setTitle("Create Automation");
      setPreferredSize(new Dimension(600, 550));
   }

   @Override
   protected Component createContents() {
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.setBorder(new EmptyBorder(10, 10, 10, 10));

      // Name / Description
      JPanel namePanel = new JPanel();
      namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
      namePanel.setBorder(new TitledBorder("Automation Details"));
      nameField = new JTextField();
      descriptionField = new JTextField();
      namePanel.add(labeledRow("Name:", nameField));
      namePanel.add(labeledRow("Description:", descriptionField));
      panel.add(namePanel);
      panel.add(Box.createVerticalStrut(8));

      // Trigger selection
      JPanel triggerPanel = new JPanel(new BorderLayout());
      triggerPanel.setBorder(new TitledBorder("1. Starting Point (Trigger)"));
      triggerCombo = new JComboBox<>();
      triggerCombo.addItem(new BlockItem("Loading...", null));
      triggerPanel.add(triggerCombo, BorderLayout.CENTER);
      panel.add(triggerPanel);
      panel.add(Box.createVerticalStrut(8));

      // Conditions
      JPanel condPanel = new JPanel(new BorderLayout());
      condPanel.setBorder(new TitledBorder("2. Conditions (Optional Guards)"));
      conditionListModel = new DefaultListModel<>();
      JList<String> condList = new JList<>(conditionListModel);
      condList.setVisibleRowCount(3);
      conditionCombo = new JComboBox<>();
      conditionCombo.addItem(new BlockItem("Loading...", null));
      JButton addCondBtn = new JButton("Add");
      addCondBtn.addActionListener(e -> addCondition());
      JButton removeCondBtn = new JButton("Remove");
      removeCondBtn.addActionListener(e -> {
         int idx = condList.getSelectedIndex();
         if (idx >= 0) {
            conditionListModel.remove(idx);
            selectedConditions.remove(idx);
         }
      });
      JPanel condControls = new JPanel();
      condControls.add(conditionCombo);
      condControls.add(addCondBtn);
      condControls.add(removeCondBtn);
      condPanel.add(new JScrollPane(condList), BorderLayout.CENTER);
      condPanel.add(condControls, BorderLayout.SOUTH);
      panel.add(condPanel);
      panel.add(Box.createVerticalStrut(8));

      // Actions
      JPanel actPanel = new JPanel(new BorderLayout());
      actPanel.setBorder(new TitledBorder("3. Actions"));
      actionListModel = new DefaultListModel<>();
      JList<String> actList = new JList<>(actionListModel);
      actList.setVisibleRowCount(3);
      actionCombo = new JComboBox<>();
      actionCombo.addItem(new BlockItem("Loading...", null));
      JButton addActBtn = new JButton("Add");
      addActBtn.addActionListener(e -> addAction());
      JButton removeActBtn = new JButton("Remove");
      removeActBtn.addActionListener(e -> {
         int idx = actList.getSelectedIndex();
         if (idx >= 0) {
            actionListModel.remove(idx);
            selectedActions.remove(idx);
         }
      });
      JPanel actControls = new JPanel();
      actControls.add(actionCombo);
      actControls.add(addActBtn);
      actControls.add(removeActBtn);
      actPanel.add(new JScrollPane(actList), BorderLayout.CENTER);
      actPanel.add(actControls, BorderLayout.SOUTH);
      panel.add(actPanel);
      panel.add(Box.createVerticalStrut(8));

      // Create button
      JButton createBtn = new JButton("Create Automation");
      createBtn.addActionListener(e -> doCreate());
      JPanel btnPanel = new JPanel();
      btnPanel.add(createBtn);
      panel.add(btnPanel);

      // Load available blocks from server
      loadBlocks();

      return new JScrollPane(panel);
   }

   private void loadBlocks() {
      AutomationService service = IrisClientFactory.getService(AutomationService.class);

      service.getStartingPoints(placeId)
            .onSuccess(response -> SwingUtilities.invokeLater(() -> {
               availableTriggers = response.getTriggers();
               triggerCombo.removeAllItems();
               if (availableTriggers != null) {
                  for (Map<String, Object> block : availableTriggers) {
                     triggerCombo.addItem(new BlockItem(
                           (String) block.get("label"), block));
                  }
               }
            }))
            .onFailure(err -> Oculus.error("Failed to load triggers", err));

      service.getNextSteps(placeId, new HashMap<>(), new ArrayList<>())
            .onSuccess(response -> SwingUtilities.invokeLater(() -> {
               availableConditions = response.getConditions();
               availableActions = response.getActions();
               conditionCombo.removeAllItems();
               if (availableConditions != null) {
                  for (Map<String, Object> block : availableConditions) {
                     conditionCombo.addItem(new BlockItem(
                           (String) block.get("label"), block));
                  }
               }
               actionCombo.removeAllItems();
               if (availableActions != null) {
                  for (Map<String, Object> block : availableActions) {
                     actionCombo.addItem(new BlockItem(
                           (String) block.get("label"), block));
                  }
               }
            }))
            .onFailure(err -> Oculus.error("Failed to load blocks", err));
   }

   private void addCondition() {
      BlockItem item = (BlockItem) conditionCombo.getSelectedItem();
      if (item == null || item.block == null) return;
      selectedConditions.add(item.block);
      conditionListModel.addElement(item.label);
   }

   private void addAction() {
      BlockItem item = (BlockItem) actionCombo.getSelectedItem();
      if (item == null || item.block == null) return;
      selectedActions.add(item.block);
      actionListModel.addElement(item.label);
   }

   private void doCreate() {
      String name = nameField.getText().trim();
      if (name.isEmpty()) {
         JOptionPane.showMessageDialog(this, "Name is required",
               "Validation Error", JOptionPane.ERROR_MESSAGE);
         return;
      }

      BlockItem triggerItem = (BlockItem) triggerCombo.getSelectedItem();
      if (triggerItem == null || triggerItem.block == null) {
         JOptionPane.showMessageDialog(this, "Please select a trigger",
               "Validation Error", JOptionPane.ERROR_MESSAGE);
         return;
      }

      if (selectedActions.isEmpty()) {
         JOptionPane.showMessageDialog(this, "At least one action is required",
               "Validation Error", JOptionPane.ERROR_MESSAGE);
         return;
      }

      selectedTrigger = triggerItem.block;
      String description = descriptionField.getText().trim();

      AutomationService service = IrisClientFactory.getService(AutomationService.class);
      Oculus.showProgress(
            service.create(placeId, name, description, selectedTrigger,
                  selectedConditions, selectedActions)
                  .onSuccess(r -> {
                     SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                              "Automation created: " + r.getAutomation(),
                              "Success", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                        submit();
                     });
                  })
                  .onFailure(err -> SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                              "Failed to create automation: " + err.getMessage(),
                              "Error", JOptionPane.ERROR_MESSAGE))),
            "Creating automation..."
      );
   }

   @Override
   protected Void getValue() {
      return null;
   }

   private static JPanel labeledRow(String label, JTextField field) {
      JPanel row = new JPanel(new BorderLayout(5, 0));
      JLabel lbl = new JLabel(label);
      lbl.setPreferredSize(new Dimension(100, 25));
      row.add(lbl, BorderLayout.WEST);
      row.add(field, BorderLayout.CENTER);
      row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
      return row;
   }

   /**
    * Wraps a block map for display in a combo box.
    */
   private static class BlockItem {
      final String label;
      final Map<String, Object> block;

      BlockItem(String label, Map<String, Object> block) {
         this.label = label;
         this.block = block;
      }

      @Override
      public String toString() {
         return label;
      }
   }
}
