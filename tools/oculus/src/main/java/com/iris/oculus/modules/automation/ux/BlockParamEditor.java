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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
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

   private static final Color HEADER_COLOR = new Color(0x4A90D9);
   private static final Color SECTION_BG = new Color(0xF5F7FA);

   /** Known enum values for common device attributes */
   private static final Map<String, List<String>> KNOWN_ATTRIBUTE_VALUES;
   static {
      Map<String, List<String>> m = new LinkedHashMap<>();
      m.put("swit:state", Arrays.asList("ON", "OFF"));
      m.put("doorlock:lockstate", Arrays.asList("LOCKED", "UNLOCKED"));
      m.put("cont:contact", Arrays.asList("OPENED", "CLOSED"));
      m.put("mot:motion", Arrays.asList("DETECTED", "NONE"));
      m.put("pres:presence", Arrays.asList("PRESENT", "ABSENT"));
      m.put("fan:speed", Arrays.asList("OFF", "LOW", "MEDIUM", "HIGH"));
      m.put("therm:hvacmode", Arrays.asList("OFF", "HEAT", "COOL", "AUTO"));
      m.put("vent:level", Arrays.asList("OPEN", "CLOSED"));
      KNOWN_ATTRIBUTE_VALUES = Collections.unmodifiableMap(m);
   }

   private final Map<String, Object> block;
   private final Map<String, JComponent> editors = new LinkedHashMap<>();
   private JComboBox<DeviceItem> deviceCombo;
   private JComboBox<String> attributeCombo;
   private JPanel valueContainer;
   private JComponent valueEditor;
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
      Map<String, Object> params = (Map<String, Object>) block.get("params");
      List<Map<String, Object>> devices = (List<Map<String, Object>>) block.get("devices");
      List<Map<String, Object>> modes = (List<Map<String, Object>>) block.get("modes");
      List<Map<String, Object>> scenes = (List<Map<String, Object>>) block.get("scenes");
      if ((params == null || params.isEmpty()) && (devices == null || devices.isEmpty())
            && (modes == null || modes.isEmpty()) && (scenes == null || scenes.isEmpty())) {
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

      JPanel content = new JPanel();
      content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
      content.setBorder(new EmptyBorder(12, 14, 8, 14));

      // Description header
      String description = (String) block.get("description");
      if (description != null) {
         JLabel descLabel = new JLabel("<html><body style='width:340px'><i>"
               + description + "</i></body></html>");
         descLabel.setForeground(new Color(0x666666));
         descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
         content.add(descLabel);
         content.add(Box.createVerticalStrut(12));
      }

      // Device selector
      List<Map<String, Object>> devices = (List<Map<String, Object>>) block.get("devices");
      if (devices != null && !devices.isEmpty()) {
         JPanel deviceSection = createSection("Device");
         JPanel inner = new JPanel();
         inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
         inner.setOpaque(false);

         deviceCombo = new JComboBox<>();
         for (Map<String, Object> dev : devices) {
            String name = dev.get("name") != null ? (String) dev.get("name") : (String) dev.get("address");
            List<String> attrs = (List<String>) dev.get("attributes");
            deviceCombo.addItem(new DeviceItem(name, (String) dev.get("address"), attrs));
         }
         styleCombo(deviceCombo);
         inner.add(createFieldLabel("Device:"));
         inner.add(deviceCombo);
         inner.add(Box.createVerticalStrut(6));

         // Attribute selector
         attributeCombo = new JComboBox<>();
         styleCombo(attributeCombo);
         inner.add(createFieldLabel("Attribute:"));
         inner.add(attributeCombo);
         inner.add(Box.createVerticalStrut(6));

         // Dynamic value editor (dropdown for known enums, text field for others)
         inner.add(createFieldLabel("Value:"));
         valueContainer = new JPanel(new BorderLayout());
         valueContainer.setOpaque(false);
         valueContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
         valueContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
         inner.add(valueContainer);

         deviceCombo.addActionListener(e -> updateAttributeCombo());
         attributeCombo.addActionListener(e -> updateValueEditor());
         updateAttributeCombo();

         deviceSection.add(inner, BorderLayout.CENTER);
         content.add(deviceSection);
         content.add(Box.createVerticalStrut(8));
      }

      // Mode selector
      List<Map<String, Object>> modes = (List<Map<String, Object>>) block.get("modes");
      if (modes != null && !modes.isEmpty()) {
         JPanel modeSection = createSection("Mode");
         modeCombo = new JComboBox<>();
         for (Map<String, Object> mode : modes) {
            modeCombo.addItem((String) mode.get("label"));
         }
         styleCombo(modeCombo);
         JPanel inner = new JPanel(new BorderLayout());
         inner.setOpaque(false);
         inner.add(modeCombo, BorderLayout.CENTER);
         modeSection.add(inner, BorderLayout.CENTER);
         content.add(modeSection);
         content.add(Box.createVerticalStrut(8));
      }

      // Scene selector
      List<Map<String, Object>> scenes = (List<Map<String, Object>>) block.get("scenes");
      if (scenes != null && !scenes.isEmpty()) {
         JPanel sceneSection = createSection("Scene");
         sceneCombo = new JComboBox<>();
         for (Map<String, Object> scene : scenes) {
            String name = scene.get("name") != null ? (String) scene.get("name") : (String) scene.get("address");
            sceneCombo.addItem(new SceneItem(name, (String) scene.get("address")));
         }
         styleCombo(sceneCombo);
         JPanel inner = new JPanel(new BorderLayout());
         inner.setOpaque(false);
         inner.add(sceneCombo, BorderLayout.CENTER);
         sceneSection.add(inner, BorderLayout.CENTER);
         content.add(sceneSection);
         content.add(Box.createVerticalStrut(8));
      }

      // Params schema-driven fields
      Map<String, Object> params = (Map<String, Object>) block.get("params");
      if (params != null && !params.isEmpty()) {
         JPanel paramSection = createSection("Parameters");
         JPanel inner = new JPanel();
         inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
         inner.setOpaque(false);

         for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramName = entry.getKey();
            Map<String, Object> paramSpec = (Map<String, Object>) entry.getValue();
            String type = (String) paramSpec.get("type");
            String label = (String) paramSpec.get("label");

            inner.add(createFieldLabel(label != null ? label + ":" : paramName + ":"));
            JComponent editor = createEditor(type, paramSpec);
            editor.setAlignmentX(Component.LEFT_ALIGNMENT);
            editor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            editors.put(paramName, editor);
            inner.add(editor);
            inner.add(Box.createVerticalStrut(6));
         }

         paramSection.add(inner, BorderLayout.CENTER);
         content.add(paramSection);
         content.add(Box.createVerticalStrut(8));
      }

      content.add(Box.createVerticalGlue());

      // Buttons
      JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
      JButton okBtn = new JButton("OK");
      JButton cancelBtn = new JButton("Cancel");
      okBtn.setPreferredSize(new Dimension(80, 28));
      cancelBtn.setPreferredSize(new Dimension(80, 28));
      okBtn.addActionListener(e -> {
         confirmed = true;
         result = buildResult();
         dispose();
      });
      cancelBtn.addActionListener(e -> dispose());
      btnPanel.add(cancelBtn);
      btnPanel.add(okBtn);

      getContentPane().setLayout(new BorderLayout());
      getContentPane().add(content, BorderLayout.CENTER);
      getContentPane().add(btnPanel, BorderLayout.SOUTH);
      pack();
      setMinimumSize(new Dimension(420, 200));
      setLocationRelativeTo(owner);
   }

   private JPanel createSection(String title) {
      JPanel section = new JPanel(new BorderLayout(0, 4));
      section.setBackground(SECTION_BG);
      section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xDDDDDD), 1, true),
            new EmptyBorder(8, 10, 8, 10)));
      section.setAlignmentX(Component.LEFT_ALIGNMENT);

      JLabel titleLabel = new JLabel(title);
      titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
      titleLabel.setForeground(HEADER_COLOR);
      section.add(titleLabel, BorderLayout.NORTH);

      return section;
   }

   private static JLabel createFieldLabel(String text) {
      JLabel label = new JLabel(text);
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      label.setFont(label.getFont().deriveFont(11f));
      label.setForeground(new Color(0x555555));
      return label;
   }

   private static void styleCombo(JComboBox<?> combo) {
      combo.setAlignmentX(Component.LEFT_ALIGNMENT);
      combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
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
            return new JTextField("12:00:00");
         }
         case "duration": {
            return new JSpinner(new SpinnerNumberModel(5, 1, 1440, 1));
         }
         case "day-set": {
            JPanel dayPanel = new JPanel();
            dayPanel.setLayout(new BoxLayout(dayPanel, BoxLayout.X_AXIS));
            dayPanel.setOpaque(false);
            String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (String day : days) {
               JCheckBox cb = new JCheckBox(day, true);
               cb.setOpaque(false);
               cb.setFont(cb.getFont().deriveFont(11f));
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
      updateValueEditor();
   }

   private void updateValueEditor() {
      if (valueContainer == null) return;
      valueContainer.removeAll();

      String selectedAttr = (String) (attributeCombo != null ? attributeCombo.getSelectedItem() : null);
      List<String> knownValues = selectedAttr != null ? KNOWN_ATTRIBUTE_VALUES.get(selectedAttr) : null;

      if (knownValues != null) {
         JComboBox<String> combo = new JComboBox<>();
         for (String v : knownValues) {
            combo.addItem(v);
         }
         valueEditor = combo;
      } else {
         valueEditor = new JTextField();
      }
      valueEditor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
      valueContainer.add(valueEditor, BorderLayout.CENTER);
      valueContainer.revalidate();
      valueContainer.repaint();
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
      if (valueEditor != null) {
         configured.put("selectedValue", getEditorValue(valueEditor));
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
