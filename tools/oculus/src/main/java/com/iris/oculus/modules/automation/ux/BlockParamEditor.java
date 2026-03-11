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
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

/**
 * Modal dialog that renders an editor form based on a block's
 * params schema and devices list. Returns the configured block
 * with user-provided parameter values merged in.
 */
public class BlockParamEditor extends JDialog {

   private final Map<String, Object> block;
   private final Map<String, JComponent> editors = new LinkedHashMap<>();
   private JComboBox<DeviceItem> deviceCombo;
   private JComboBox<String> attributeCombo;
   private JComboBox<String> modeCombo;
   private JComboBox<SceneItem> sceneCombo;
   private Map<String, Object> result;
   private boolean confirmed = false;

   /**
    * Shows the editor for the given block. Returns the configured block
    * (with params filled in), or null if the user cancelled.
    */
   @SuppressWarnings("unchecked")
   public static Map<String, Object> configure(Window owner, Map<String, Object> block) {
      // Check if block has params, devices, modes, or scenes to configure
      Map<String, Object> params = (Map<String, Object>) block.get("params");
      List<Map<String, Object>> devices = (List<Map<String, Object>>) block.get("devices");
      List<Map<String, Object>> modes = (List<Map<String, Object>>) block.get("modes");
      List<Map<String, Object>> scenes = (List<Map<String, Object>>) block.get("scenes");
      if ((params == null || params.isEmpty()) && (devices == null || devices.isEmpty())
            && (modes == null || modes.isEmpty()) && (scenes == null || scenes.isEmpty())) {
         // No params to configure, return block as-is
         return new LinkedHashMap<>(block);
      }

      BlockParamEditor editor = new BlockParamEditor(owner, block);
      editor.setVisible(true);
      return editor.confirmed ? editor.result : null;
   }

   @SuppressWarnings("unchecked")
   private BlockParamEditor(Window owner, Map<String, Object> block) {
      super(owner, "Configure: " + block.get("label"), ModalityType.APPLICATION_MODAL);
      this.block = block;
      setPreferredSize(new Dimension(400, 350));

      JPanel content = new JPanel();
      content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
      content.setBorder(new EmptyBorder(10, 10, 10, 10));

      String description = (String) block.get("description");
      if (description != null) {
         JLabel descLabel = new JLabel("<html><i>" + description + "</i></html>");
         descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
         content.add(descLabel);
         content.add(javax.swing.Box.createVerticalStrut(10));
      }

      // Device selector
      List<Map<String, Object>> devices = (List<Map<String, Object>>) block.get("devices");
      if (devices != null && !devices.isEmpty()) {
         content.add(createLabel("Device:"));
         deviceCombo = new JComboBox<>();
         for (Map<String, Object> dev : devices) {
            String name = dev.get("name") != null ? (String) dev.get("name") : (String) dev.get("address");
            List<String> attrs = (List<String>) dev.get("attributes");
            deviceCombo.addItem(new DeviceItem(name, (String) dev.get("address"), attrs));
         }
         deviceCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
         deviceCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
         content.add(deviceCombo);
         content.add(javax.swing.Box.createVerticalStrut(5));

         // Attribute selector (populated from selected device's attributes)
         content.add(createLabel("Attribute:"));
         attributeCombo = new JComboBox<>();
         attributeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
         attributeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
         deviceCombo.addActionListener(e -> updateAttributeCombo());
         updateAttributeCombo();
         content.add(attributeCombo);
         content.add(javax.swing.Box.createVerticalStrut(5));
      }

      // Mode selector (for blocks with predefined modes like presence, alarm-state)
      List<Map<String, Object>> modes = (List<Map<String, Object>>) block.get("modes");
      if (modes != null && !modes.isEmpty()) {
         content.add(createLabel("Mode:"));
         modeCombo = new JComboBox<>();
         for (Map<String, Object> mode : modes) {
            modeCombo.addItem((String) mode.get("label"));
         }
         modeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
         modeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
         content.add(modeCombo);
         content.add(javax.swing.Box.createVerticalStrut(5));
      }

      // Scene selector
      List<Map<String, Object>> scenes = (List<Map<String, Object>>) block.get("scenes");
      if (scenes != null && !scenes.isEmpty()) {
         content.add(createLabel("Scene:"));
         sceneCombo = new JComboBox<>();
         for (Map<String, Object> scene : scenes) {
            String name = scene.get("name") != null ? (String) scene.get("name") : (String) scene.get("address");
            sceneCombo.addItem(new SceneItem(name, (String) scene.get("address")));
         }
         sceneCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
         sceneCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
         content.add(sceneCombo);
         content.add(javax.swing.Box.createVerticalStrut(5));
      }

      // Params schema-driven fields
      Map<String, Object> params = (Map<String, Object>) block.get("params");
      if (params != null) {
         for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramName = entry.getKey();
            Map<String, Object> paramSpec = (Map<String, Object>) entry.getValue();
            String type = (String) paramSpec.get("type");
            String label = (String) paramSpec.get("label");

            content.add(createLabel(label != null ? label + ":" : paramName + ":"));
            JComponent editor = createEditor(type, paramSpec);
            editor.setAlignmentX(Component.LEFT_ALIGNMENT);
            editor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            editors.put(paramName, editor);
            content.add(editor);
            content.add(javax.swing.Box.createVerticalStrut(5));
         }
      }

      content.add(javax.swing.Box.createVerticalGlue());

      // Buttons
      JPanel btnPanel = new JPanel();
      JButton okBtn = new JButton("OK");
      JButton cancelBtn = new JButton("Cancel");
      okBtn.addActionListener(e -> {
         confirmed = true;
         result = buildResult();
         dispose();
      });
      cancelBtn.addActionListener(e -> dispose());
      btnPanel.add(okBtn);
      btnPanel.add(cancelBtn);

      getContentPane().setLayout(new BorderLayout());
      getContentPane().add(content, BorderLayout.CENTER);
      getContentPane().add(btnPanel, BorderLayout.SOUTH);
      pack();
      setLocationRelativeTo(owner);
   }

   @SuppressWarnings("unchecked")
   private JComponent createEditor(String type, Map<String, Object> spec) {
      if (type == null) type = "string";
      switch (type) {
         case "enum": {
            JComboBox<String> combo = new JComboBox<>();
            List<String> values = (List<String>) spec.get("values");
            if (values != null) {
               for (String v : values) combo.addItem(v);
            }
            return combo;
         }
         case "int": {
            Object defaultVal = spec.get("default");
            int def = defaultVal instanceof Number ? ((Number) defaultVal).intValue() : 0;
            return new JSpinner(new SpinnerNumberModel(def, -1440, 1440, 1));
         }
         case "double": {
            Object defaultVal = spec.get("default");
            double def = defaultVal instanceof Number ? ((Number) defaultVal).doubleValue() : 0.0;
            return new JSpinner(new SpinnerNumberModel(def, -10000.0, 10000.0, 0.1));
         }
         case "string-list": {
            return new JTextField();
         }
         case "time": {
            JTextField field = new JTextField("12:00:00");
            return field;
         }
         case "duration": {
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(5, 1, 1440, 1));
            return spinner;
         }
         case "day-set": {
            JPanel dayPanel = new JPanel();
            dayPanel.setLayout(new BoxLayout(dayPanel, BoxLayout.X_AXIS));
            String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (String day : days) {
               JCheckBox cb = new JCheckBox(day, true);
               dayPanel.add(cb);
            }
            return dayPanel;
         }
         default: {
            return new JTextField();
         }
      }
   }

   private void updateAttributeCombo() {
      if (attributeCombo == null || deviceCombo == null) return;
      attributeCombo.removeAllItems();
      DeviceItem selected = (DeviceItem) deviceCombo.getSelectedItem();
      if (selected != null && selected.attributes != null) {
         for (String attr : selected.attributes) {
            attributeCombo.addItem(attr);
         }
      }
   }

   @SuppressWarnings("unchecked")
   private Map<String, Object> buildResult() {
      Map<String, Object> configured = new LinkedHashMap<>(block);

      // Add device selection
      if (deviceCombo != null) {
         DeviceItem dev = (DeviceItem) deviceCombo.getSelectedItem();
         if (dev != null) {
            configured.put("selectedDevice", dev.address);
            configured.put("selectedDeviceName", dev.name);
         }
      }
      if (attributeCombo != null && attributeCombo.getSelectedItem() != null) {
         configured.put("selectedAttribute", attributeCombo.getSelectedItem());
      }

      // Add mode selection
      if (modeCombo != null) {
         int idx = modeCombo.getSelectedIndex();
         List<Map<String, Object>> modes = (List<Map<String, Object>>) block.get("modes");
         if (modes != null && idx >= 0 && idx < modes.size()) {
            configured.put("selectedMode", modes.get(idx).get("value"));
            configured.put("selectedModeLabel", modes.get(idx).get("label"));
         }
      }

      // Add scene selection
      if (sceneCombo != null) {
         SceneItem scene = (SceneItem) sceneCombo.getSelectedItem();
         if (scene != null) {
            configured.put("selectedScene", scene.address);
            configured.put("selectedSceneName", scene.name);
         }
      }

      // Add param values
      Map<String, Object> paramValues = new LinkedHashMap<>();
      for (Map.Entry<String, JComponent> entry : editors.entrySet()) {
         String paramName = entry.getKey();
         JComponent editor = entry.getValue();
         paramValues.put(paramName, getEditorValue(editor));
      }
      if (!paramValues.isEmpty()) {
         configured.put("paramValues", paramValues);
      }

      return configured;
   }

   private Object getEditorValue(JComponent editor) {
      if (editor instanceof JComboBox) {
         return ((JComboBox<?>) editor).getSelectedItem();
      } else if (editor instanceof JSpinner) {
         return ((JSpinner) editor).getValue();
      } else if (editor instanceof JTextField) {
         return ((JTextField) editor).getText().trim();
      } else if (editor instanceof JPanel) {
         // Day-set panel with checkboxes
         List<String> selectedDays = new ArrayList<>();
         String[] dayValues = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
         Component[] components = ((JPanel) editor).getComponents();
         for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JCheckBox && ((JCheckBox) components[i]).isSelected()) {
               selectedDays.add(dayValues[i]);
            }
         }
         return selectedDays;
      }
      return null;
   }

   private static JLabel createLabel(String text) {
      JLabel label = new JLabel(text);
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      return label;
   }

   private static class DeviceItem {
      final String name;
      final String address;
      final List<String> attributes;

      DeviceItem(String name, String address, List<String> attributes) {
         this.name = name;
         this.address = address;
         this.attributes = attributes;
      }

      @Override
      public String toString() {
         return name;
      }
   }

   private static class SceneItem {
      final String name;
      final String address;

      SceneItem(String name, String address) {
         this.name = name;
         this.address = address;
      }

      @Override
      public String toString() {
         return name;
      }
   }
}
