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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.iris.client.IrisClientFactory;
import com.iris.client.service.AutomationService;
import com.iris.oculus.Oculus;
import com.iris.oculus.widget.Dialog;

/**
 * Visual flow-chart wizard for creating automations.
 *
 * Layout: [Trigger] ---> Flow 1: [Guards] ---> [Actions]
 *                        Flow 2: [Guards] ---> [Actions]
 *                        [+ Add Flow]
 */
public class AutomationCreatorWizard extends Dialog<Void> {

   private static final Color TRIGGER_COLOR = new Color(0x4A90D9);
   private static final Color GUARD_COLOR = new Color(0xF5A623);
   private static final Color ACTION_COLOR = new Color(0x7ED321);
   private static final Color ARROW_COLOR = new Color(0x999999);
   private static final Color NODE_BG = new Color(0xF8F8F8);

   private String placeId;

   // Chain state
   private List<Map<String, Object>> selectedTriggers = new ArrayList<>();
   private List<FlowState> flowStates = new ArrayList<>();

   // UI
   private JTextField nameField;
   private JTextField descriptionField;
   private JPanel triggersPanel;
   private JPanel flowsContainer;

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
      setPreferredSize(new Dimension(1000, 700));
   }

   @Override
   protected Component createContents() {
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.setBorder(new EmptyBorder(10, 10, 10, 10));

      // Name / Description row
      JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
      nameField = new JTextField(15);
      descriptionField = new JTextField(20);
      nameRow.add(new JLabel("Name:"));
      nameRow.add(nameField);
      nameRow.add(Box.createHorizontalStrut(10));
      nameRow.add(new JLabel("Description:"));
      nameRow.add(descriptionField);
      nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
      panel.add(nameRow);
      panel.add(Box.createVerticalStrut(10));

      // Flow chart area
      JPanel chartPanel = new JPanel(new BorderLayout(0, 0));

      // Left: Trigger node (supports multiple triggers OR'd together)
      JPanel triggerNode = createNodePanel("WHEN", TRIGGER_COLOR);
      triggersPanel = new JPanel();
      triggersPanel.setLayout(new BoxLayout(triggersPanel, BoxLayout.Y_AXIS));
      triggersPanel.setOpaque(false);
      JButton addTriggerBtn = new JButton("+");
      addTriggerBtn.setToolTipText("Add trigger (fires when ANY trigger matches)");
      addTriggerBtn.setFont(addTriggerBtn.getFont().deriveFont(Font.BOLD, 16f));
      addTriggerBtn.setPreferredSize(new Dimension(36, 28));
      addTriggerBtn.addActionListener(e -> addTrigger());
      JPanel triggerHeader = new JPanel(new BorderLayout());
      triggerHeader.setOpaque(false);
      JLabel orLabel = new JLabel("any of these triggers:");
      orLabel.setFont(orLabel.getFont().deriveFont(Font.ITALIC, 10f));
      orLabel.setForeground(TRIGGER_COLOR.darker());
      triggerHeader.add(orLabel, BorderLayout.CENTER);
      triggerHeader.add(addTriggerBtn, BorderLayout.EAST);
      JPanel triggerContent = new JPanel(new BorderLayout(0, 4));
      triggerContent.setOpaque(false);
      triggerContent.add(triggerHeader, BorderLayout.NORTH);
      triggerContent.add(triggersPanel, BorderLayout.CENTER);
      triggerNode.add(triggerContent, BorderLayout.CENTER);
      triggerNode.setPreferredSize(new Dimension(280, 150));
      triggerNode.setMinimumSize(new Dimension(280, 80));
      triggerNode.setMaximumSize(new Dimension(280, 500));

      // Arrow
      JPanel arrowPanel = new ArrowPanel();
      arrowPanel.setPreferredSize(new Dimension(24, 120));

      // Right: Flows
      flowsContainer = new JPanel();
      flowsContainer.setLayout(new BoxLayout(flowsContainer, BoxLayout.Y_AXIS));
      flowsContainer.setOpaque(false);

      // Add first flow
      addFlow();

      // Add Flow button
      JButton addFlowBtn = new JButton("+ Add Flow Branch");
      addFlowBtn.addActionListener(e -> {
         addFlow();
         flowsContainer.revalidate();
         flowsContainer.repaint();
      });
      JPanel addFlowBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      addFlowBtnPanel.setOpaque(false);
      addFlowBtnPanel.add(addFlowBtn);

      JPanel flowsWrapper = new JPanel();
      flowsWrapper.setLayout(new BoxLayout(flowsWrapper, BoxLayout.Y_AXIS));
      flowsWrapper.setOpaque(false);
      flowsWrapper.add(flowsContainer);
      flowsWrapper.add(addFlowBtnPanel);

      JPanel leftPanel = new JPanel(new BorderLayout());
      leftPanel.setOpaque(false);
      leftPanel.add(triggerNode, BorderLayout.CENTER);

      chartPanel.add(leftPanel, BorderLayout.WEST);
      chartPanel.add(arrowPanel, BorderLayout.CENTER);
      chartPanel.add(new JScrollPane(flowsWrapper), BorderLayout.EAST);
      chartPanel.setPreferredSize(new Dimension(800, 400));

      // Use a horizontal layout with scroll
      JPanel flowRow = new JPanel();
      flowRow.setLayout(new BoxLayout(flowRow, BoxLayout.X_AXIS));
      flowRow.add(triggerNode);
      flowRow.add(arrowPanel);
      flowRow.add(new JScrollPane(flowsWrapper));

      panel.add(flowRow);
      panel.add(Box.createVerticalStrut(10));

      // Create button
      JButton createBtn = new JButton("Create Automation");
      createBtn.setFont(createBtn.getFont().deriveFont(Font.BOLD));
      createBtn.addActionListener(e -> doCreate());
      JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      btnPanel.add(createBtn);
      btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
      panel.add(btnPanel);

      loadBlocks();

      return new JScrollPane(panel);
   }

   private void addFlow() {
      int flowNum = flowStates.size() + 1;
      FlowState state = new FlowState();
      flowStates.add(state);

      JPanel flowPanel = new JPanel();
      flowPanel.setLayout(new BoxLayout(flowPanel, BoxLayout.X_AXIS));
      flowPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xDDDDDD), 1, true),
            new EmptyBorder(6, 6, 6, 6)));
      flowPanel.setBackground(NODE_BG);
      flowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

      // Guards column
      JPanel guardsNode = createNodePanel("IF (Guards)", GUARD_COLOR);
      guardsNode.setPreferredSize(new Dimension(250, 130));
      JPanel guardsList = new JPanel();
      guardsList.setLayout(new BoxLayout(guardsList, BoxLayout.Y_AXIS));
      guardsList.setOpaque(false);
      state.guardsPanel = guardsList;

      // AND/OR toggle
      JButton logicToggle = new JButton("AND");
      logicToggle.setFont(logicToggle.getFont().deriveFont(Font.BOLD, 10f));
      logicToggle.setPreferredSize(new Dimension(44, 20));
      logicToggle.setToolTipText("Toggle between ALL guards (AND) or ANY guard (OR)");
      logicToggle.addActionListener(e -> {
         if ("AND".equals(state.guardLogic)) {
            state.guardLogic = "OR";
            logicToggle.setText("OR");
         } else {
            state.guardLogic = "AND";
            logicToggle.setText("AND");
         }
      });

      JPanel guardButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
      guardButtons.setOpaque(false);
      JButton addGuardBtn = new JButton("+ Guard");
      guardButtons.add(addGuardBtn);
      guardButtons.add(logicToggle);
      addGuardBtn.addActionListener(e -> addGuard(state));

      JPanel guardContent = new JPanel(new BorderLayout(0, 4));
      guardContent.setOpaque(false);
      guardContent.add(guardsList, BorderLayout.CENTER);
      guardContent.add(guardButtons, BorderLayout.SOUTH);
      guardsNode.add(guardContent, BorderLayout.CENTER);

      // Arrow between guards and actions
      JPanel arrow2 = new ArrowPanel();
      arrow2.setPreferredSize(new Dimension(20, 130));

      // Actions column
      JPanel actionsNode = createNodePanel("THEN (Actions)", ACTION_COLOR);
      actionsNode.setPreferredSize(new Dimension(250, 130));
      JPanel actionsList = new JPanel();
      actionsList.setLayout(new BoxLayout(actionsList, BoxLayout.Y_AXIS));
      actionsList.setOpaque(false);
      state.actionsPanel = actionsList;
      JButton addActionBtn = new JButton("+ Action");
      addActionBtn.addActionListener(e -> addAction(state));
      JPanel actionContent = new JPanel(new BorderLayout(0, 4));
      actionContent.setOpaque(false);
      actionContent.add(actionsList, BorderLayout.CENTER);
      actionContent.add(addActionBtn, BorderLayout.SOUTH);
      actionsNode.add(actionContent, BorderLayout.CENTER);

      // Build nodes row first (referenced by enable checkbox lambda)
      JPanel nodesRow = new JPanel();
      nodesRow.setLayout(new BoxLayout(nodesRow, BoxLayout.X_AXIS));
      nodesRow.setOpaque(false);
      nodesRow.add(guardsNode);
      nodesRow.add(arrow2);
      nodesRow.add(actionsNode);
      state.flowPanel = flowPanel;
      state.nodesRow = nodesRow;

      // Remove flow button
      JButton removeBtn = new JButton("\u00d7");
      removeBtn.setToolTipText("Remove this flow");
      removeBtn.setFont(removeBtn.getFont().deriveFont(Font.BOLD, 14f));
      removeBtn.setPreferredSize(new Dimension(28, 24));
      removeBtn.addActionListener(e -> {
         if (flowStates.size() <= 1) {
            JOptionPane.showMessageDialog(AutomationCreatorWizard.this,
                  "At least one flow is required",
                  "Cannot Remove", JOptionPane.WARNING_MESSAGE);
            return;
         }
         flowStates.remove(state);
         flowsContainer.remove(flowPanel);
         flowsContainer.revalidate();
         flowsContainer.repaint();
      });

      // Enable/disable checkbox
      javax.swing.JCheckBox enableCb = new javax.swing.JCheckBox("", true);
      enableCb.setOpaque(false);
      enableCb.setToolTipText("Enable/disable this flow");
      enableCb.addActionListener(e -> {
         state.enabled = enableCb.isSelected();
         setFlowEnabled(nodesRow, flowPanel, state.enabled);
      });

      JPanel headerRow = new JPanel(new BorderLayout());
      headerRow.setOpaque(false);
      JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
      headerLeft.setOpaque(false);
      headerLeft.add(enableCb);
      JLabel flowLabel = new JLabel("Flow " + flowNum);
      flowLabel.setFont(flowLabel.getFont().deriveFont(Font.BOLD, 11f));
      headerLeft.add(flowLabel);
      headerRow.add(headerLeft, BorderLayout.CENTER);
      headerRow.add(removeBtn, BorderLayout.EAST);

      JPanel flowInner = new JPanel();
      flowInner.setLayout(new BoxLayout(flowInner, BoxLayout.Y_AXIS));
      flowInner.setOpaque(false);
      flowInner.add(headerRow);
      flowInner.add(nodesRow);
      flowPanel.add(flowInner);

      flowsContainer.add(flowPanel);
      flowsContainer.add(Box.createVerticalStrut(6));
   }

   private void setFlowEnabled(JPanel nodesRow, JPanel flowPanel, boolean enabled) {
      setEnabledRecursive(nodesRow, enabled);
      flowPanel.setBackground(enabled ? NODE_BG : new Color(0xE8E8E8));
      flowPanel.repaint();
   }

   private static void setEnabledRecursive(Component comp, boolean enabled) {
      comp.setEnabled(enabled);
      if (comp instanceof java.awt.Container) {
         for (Component child : ((java.awt.Container) comp).getComponents()) {
            setEnabledRecursive(child, enabled);
         }
      }
   }

   private void addGuard(FlowState state) {
      if (availableConditions == null || availableConditions.isEmpty()) return;

      BlockItem[] items = availableConditions.stream()
            .map(b -> new BlockItem((String) b.get("label"), b))
            .toArray(BlockItem[]::new);
      BlockItem selected = (BlockItem) JOptionPane.showInputDialog(
            this, "Select guard condition:", "Add Guard",
            JOptionPane.PLAIN_MESSAGE, null, items, items[0]);
      if (selected == null || selected.block == null) return;

      Map<String, Object> configured = BlockParamEditor.configure(this, selected.block);
      if (configured == null) return;

      state.conditions.add(configured);
      addItemCard(state.guardsPanel, state.conditions, configured, GUARD_COLOR);
   }

   private void addAction(FlowState state) {
      if (availableActions == null || availableActions.isEmpty()) return;

      BlockItem[] items = availableActions.stream()
            .map(b -> new BlockItem((String) b.get("label"), b))
            .toArray(BlockItem[]::new);
      BlockItem selected = (BlockItem) JOptionPane.showInputDialog(
            this, "Select action:", "Add Action",
            JOptionPane.PLAIN_MESSAGE, null, items, items[0]);
      if (selected == null || selected.block == null) return;

      Map<String, Object> configured = BlockParamEditor.configure(this, selected.block);
      if (configured == null) return;

      state.actions.add(configured);
      addItemCard(state.actionsPanel, state.actions, configured, ACTION_COLOR);
   }

   private void addItemCard(JPanel container, List<Map<String, Object>> list,
         Map<String, Object> item, Color color) {
      // Mutable holder so lambdas always see the latest version after edits
      final Map<String, Object>[] current = new Map[]{ item };

      JPanel card = new JPanel(new BorderLayout(4, 0));
      card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 1, true),
            new EmptyBorder(3, 5, 3, 3)));
      card.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
      card.setAlignmentX(Component.LEFT_ALIGNMENT);
      card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

      JLabel text = new JLabel("<html><body style='width:190px'>"
            + summarizeBlock(item) + "</body></html>");
      text.setFont(text.getFont().deriveFont(11f));
      text.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
      text.setToolTipText("Double-click to edit");
      text.addMouseListener(new java.awt.event.MouseAdapter() {
         @Override
         public void mouseClicked(java.awt.event.MouseEvent e) {
            if (e.getClickCount() == 2) {
               Map<String, Object> reconfigured = BlockParamEditor.configure(
                     AutomationCreatorWizard.this, current[0]);
               if (reconfigured != null) {
                  int idx = list.indexOf(current[0]);
                  if (idx >= 0) {
                     list.set(idx, reconfigured);
                  }
                  current[0] = reconfigured;
                  text.setText("<html><body style='width:190px'>"
                        + summarizeBlock(reconfigured) + "</body></html>");
               }
            }
         }
      });

      JButton removeBtn = new JButton("\u00d7");
      removeBtn.setFont(removeBtn.getFont().deriveFont(Font.BOLD, 11f));
      removeBtn.setMargin(new java.awt.Insets(0, 3, 0, 3));
      removeBtn.addActionListener(e -> {
         list.remove(current[0]);
         container.remove(card);
         container.revalidate();
         container.repaint();
      });

      card.add(text, BorderLayout.CENTER);
      card.add(removeBtn, BorderLayout.EAST);

      container.add(card);
      container.add(Box.createVerticalStrut(2));
      container.revalidate();
      container.repaint();
   }

   private JPanel createNodePanel(String title, Color color) {
      JPanel panel = new JPanel(new BorderLayout(4, 4));
      panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2, true),
            new EmptyBorder(6, 8, 6, 8)));
      panel.setBackground(NODE_BG);

      JLabel titleLabel = new JLabel(title);
      titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
      titleLabel.setForeground(color.darker());
      panel.add(titleLabel, BorderLayout.NORTH);

      return panel;
   }

   private void loadBlocks() {
      AutomationService service = IrisClientFactory.getService(AutomationService.class);

      service.getStartingPoints(placeId)
            .onSuccess(response -> SwingUtilities.invokeLater(() -> {
               availableTriggers = response.getTriggers();
            }))
            .onFailure(err -> Oculus.error("Failed to load triggers", err));

      service.getNextSteps(placeId, new HashMap<>(), new ArrayList<>())
            .onSuccess(response -> SwingUtilities.invokeLater(() -> {
               availableConditions = response.getConditions();
               availableActions = response.getActions();
            }))
            .onFailure(err -> Oculus.error("Failed to load blocks", err));
   }

   private void addTrigger() {
      if (availableTriggers == null || availableTriggers.isEmpty()) return;

      BlockItem[] items = availableTriggers.stream()
            .map(b -> new BlockItem((String) b.get("label"), b))
            .toArray(BlockItem[]::new);
      BlockItem selected = (BlockItem) JOptionPane.showInputDialog(
            this, "Select trigger:", "Add Trigger",
            JOptionPane.PLAIN_MESSAGE, null, items, items[0]);
      if (selected == null || selected.block == null) return;

      Map<String, Object> configured = BlockParamEditor.configure(this, selected.block);
      if (configured == null) return;

      selectedTriggers.add(configured);
      addTriggerCard(configured);
   }

   private void addTriggerCard(Map<String, Object> trigger) {
      // Mutable holder so lambdas always see the latest version after edits
      final Map<String, Object>[] current = new Map[]{ trigger };

      JPanel card = new JPanel(new BorderLayout(4, 0));
      card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(TRIGGER_COLOR, 1, true),
            new EmptyBorder(4, 6, 4, 4)));
      card.setBackground(new Color(
            TRIGGER_COLOR.getRed(), TRIGGER_COLOR.getGreen(), TRIGGER_COLOR.getBlue(), 25));
      card.setAlignmentX(Component.LEFT_ALIGNMENT);
      card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

      JLabel text = new JLabel("<html><body style='width:220px'>"
            + summarizeBlock(trigger) + "</body></html>");
      text.setFont(text.getFont().deriveFont(11f));
      text.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
      text.setToolTipText("Double-click to edit");
      text.addMouseListener(new java.awt.event.MouseAdapter() {
         @Override
         public void mouseClicked(java.awt.event.MouseEvent e) {
            if (e.getClickCount() == 2) {
               Map<String, Object> reconfigured = BlockParamEditor.configure(
                     AutomationCreatorWizard.this, current[0]);
               if (reconfigured != null) {
                  int idx = selectedTriggers.indexOf(current[0]);
                  if (idx >= 0) {
                     selectedTriggers.set(idx, reconfigured);
                  }
                  current[0] = reconfigured;
                  text.setText("<html><body style='width:220px'>"
                        + summarizeBlock(reconfigured) + "</body></html>");
               }
            }
         }
      });

      JButton removeBtn = new JButton("\u00d7");
      removeBtn.setFont(removeBtn.getFont().deriveFont(Font.BOLD, 12f));
      removeBtn.setMargin(new java.awt.Insets(0, 3, 0, 3));
      removeBtn.setToolTipText("Remove this trigger");
      removeBtn.addActionListener(e -> {
         selectedTriggers.remove(current[0]);
         triggersPanel.remove(card);
         triggersPanel.revalidate();
         triggersPanel.repaint();
      });

      card.add(text, BorderLayout.CENTER);
      card.add(removeBtn, BorderLayout.EAST);

      triggersPanel.add(card);
      triggersPanel.add(Box.createVerticalStrut(3));
      triggersPanel.revalidate();
      triggersPanel.repaint();

      // Resize trigger node if needed
      JPanel triggerNode = (JPanel) triggersPanel.getParent().getParent();
      if (triggerNode != null) {
         triggerNode.revalidate();
      }
   }

   @SuppressWarnings("unchecked")
   static String summarizeBlock(Map<String, Object> block) {
      StringBuilder sb = new StringBuilder();
      sb.append((String) block.get("label"));
      if (block.containsKey("selectedDeviceName")) {
         sb.append(" [").append(block.get("selectedDeviceName"));
         if (block.containsKey("selectedAttribute")) {
            sb.append(" / ").append(block.get("selectedAttribute"));
         }
         if (block.containsKey("selectedValue")) {
            sb.append(" = ").append(block.get("selectedValue"));
         }
         sb.append("]");
      }
      if (block.containsKey("selectedModeLabel")) {
         sb.append(" [").append(block.get("selectedModeLabel")).append("]");
      }
      if (block.containsKey("selectedSceneName")) {
         sb.append(" [").append(block.get("selectedSceneName")).append("]");
      }
      Map<String, Object> paramValues = (Map<String, Object>) block.get("paramValues");
      Map<String, Object> params = (Map<String, Object>) block.get("params");
      if (paramValues != null && !paramValues.isEmpty()) {
         List<String> parts = new ArrayList<>();
         for (Map.Entry<String, Object> e : paramValues.entrySet()) {
            Object val = e.getValue();
            if (val == null || (val instanceof String && ((String) val).isEmpty())) continue;
            // Use label from param spec if available
            String displayName = e.getKey();
            if (params != null && params.get(e.getKey()) instanceof Map) {
               Map<String, Object> spec = (Map<String, Object>) params.get(e.getKey());
               if (spec.get("label") != null) {
                  displayName = (String) spec.get("label");
               }
            }
            parts.add(displayName + "=" + val);
         }
         if (!parts.isEmpty()) {
            sb.append(" (").append(String.join(", ", parts)).append(")");
         }
      }
      return sb.toString();
   }

   private void doCreate() {
      String name = nameField.getText().trim();
      if (name.isEmpty()) {
         JOptionPane.showMessageDialog(this, "Name is required",
               "Validation Error", JOptionPane.ERROR_MESSAGE);
         return;
      }

      if (selectedTriggers.isEmpty()) {
         JOptionPane.showMessageDialog(this, "At least one trigger is required",
               "Validation Error", JOptionPane.ERROR_MESSAGE);
         return;
      }

      // Build trigger: single trigger or OR wrapper for multiple
      Map<String, Object> triggerParam;
      if (selectedTriggers.size() == 1) {
         triggerParam = selectedTriggers.get(0);
      }
      else {
         // Wrap in an OR — server will deserialize as OrConfig
         Map<String, Object> orTrigger = new HashMap<>();
         orTrigger.put("type", "or");
         orTrigger.put("configs", selectedTriggers);
         triggerParam = orTrigger;
      }

      // Validate flows — at least one enabled flow must have actions
      boolean hasActions = false;
      for (FlowState flow : flowStates) {
         if (flow.enabled && !flow.actions.isEmpty()) {
            hasActions = true;
            break;
         }
      }
      if (!hasActions) {
         JOptionPane.showMessageDialog(this, "At least one enabled flow must have actions",
               "Validation Error", JOptionPane.ERROR_MESSAGE);
         return;
      }

      String description = descriptionField.getText().trim();
      AutomationService service = IrisClientFactory.getService(AutomationService.class);

      // Build flows list for the API (skip disabled and empty flows)
      List<Map<String, Object>> flows = new ArrayList<>();
      for (FlowState state : flowStates) {
         if (!state.enabled || state.actions.isEmpty()) continue;
         Map<String, Object> flow = new HashMap<>();
         flow.put("conditions", state.conditions);
         flow.put("actions", state.actions);
         flow.put("guardLogic", state.guardLogic);
         flows.add(flow);
      }

      // Single flow uses conditions+actions, multi-flow uses flows param
      List<Map<String, Object>> conditions = null;
      List<Map<String, Object>> actions = null;
      List<Map<String, Object>> flowsParam = null;

      if (flows.size() == 1) {
         @SuppressWarnings("unchecked")
         List<Map<String, Object>> c = (List<Map<String, Object>>) flows.get(0).get("conditions");
         @SuppressWarnings("unchecked")
         List<Map<String, Object>> a = (List<Map<String, Object>>) flows.get(0).get("actions");
         conditions = c;
         actions = a;
      }
      else {
         flowsParam = flows;
      }

      Oculus.showProgress(
            service.create(placeId, name, description, triggerParam,
                  conditions, actions, flowsParam)
                  .onSuccess(r -> SwingUtilities.invokeLater(() -> {
                     JOptionPane.showMessageDialog(this,
                           "Automation created: " + r.getAutomation(),
                           "Success", JOptionPane.INFORMATION_MESSAGE);
                     dispose();
                     submit();
                  })),
            "Creating automation...");
   }

   @Override
   protected Void getValue() {
      return null;
   }

   /**
    * State for one flow branch.
    */
   private static class FlowState {
      List<Map<String, Object>> conditions = new ArrayList<>();
      List<Map<String, Object>> actions = new ArrayList<>();
      JPanel guardsPanel;
      JPanel actionsPanel;
      JPanel flowPanel;
      JPanel nodesRow;
      boolean enabled = true;
      String guardLogic = "AND";
   }

   /**
    * Wraps a block map for display in a combo box.
    */
   static class BlockItem {
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

   /**
    * Draws a horizontal arrow.
    */
   private static class ArrowPanel extends JPanel {
      ArrowPanel() {
         setOpaque(false);
      }

      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         Graphics2D g2 = (Graphics2D) g.create();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2.setColor(ARROW_COLOR);
         g2.setStroke(new BasicStroke(2f));

         int y = getHeight() / 2;
         int x1 = 4;
         int x2 = getWidth() - 4;

         // Line
         g2.drawLine(x1, y, x2, y);

         // Arrowhead
         int arrowSize = 6;
         g2.fillPolygon(
               new int[]{x2, x2 - arrowSize, x2 - arrowSize},
               new int[]{y, y - arrowSize, y + arrowSize},
               3);

         g2.dispose();
      }
   }
}
