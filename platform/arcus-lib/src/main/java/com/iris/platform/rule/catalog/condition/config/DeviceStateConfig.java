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
package com.iris.platform.rule.catalog.condition.config;

import java.util.Map;
import java.util.Objects;

import com.google.common.base.Predicate;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.GuardConditionAdapter;
import com.iris.messages.model.Model;

/**
 * A guard condition that checks if a specific device attribute has a
 * specific value. Used in automation chains as "only if front door is locked".
 *
 * Example JSON:
 * {
 *   "type": "device-state",
 *   "address": "DRIV:dev:abc123",
 *   "attribute": "doorlock:lockstate",
 *   "value": "LOCKED"
 * }
 */
public class DeviceStateConfig implements ConditionConfig {

   public static final String TYPE = "device-state";

   private String address;
   private String attribute;
   private Object value;

   public String getAddress() {
      return address;
   }

   public void setAddress(String address) {
      this.address = address;
   }

   public String getAttribute() {
      return attribute;
   }

   public void setAttribute(String attribute) {
      this.attribute = attribute;
   }

   public Object getValue() {
      return value;
   }

   public void setValue(Object value) {
      this.value = value;
   }

   @Override
   public String getType() {
      return TYPE;
   }

   @Override
   public Condition generate(Map<String, Object> values) {
      String desc = attribute + " == " + value + " on " + address;
      Predicate<Model> predicate = model -> {
         if (model == null) return false;
         Object addr = model.getAttribute("base:address");
         if (addr == null || !addr.toString().equals(address)) return false;
         Object actual = model.getAttribute(attribute);
         if (actual == null) return value == null;
         return Objects.equals(actual.toString(), String.valueOf(value));
      };

      // Use a prefix derived from the address to narrow model matching
      String prefix = address.contains(":") ? address.substring(0, address.indexOf(":") + 1) : "";
      return new GuardConditionAdapter(desc, prefix, predicate);
   }

   @Override
   public String toString() {
      return "DeviceStateConfig [address=" + address
            + ", attribute=" + attribute
            + ", value=" + value + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((address == null) ? 0 : address.hashCode());
      result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      DeviceStateConfig other = (DeviceStateConfig) obj;
      if (address == null) {
         if (other.address != null) return false;
      }
      else if (!address.equals(other.address)) return false;
      if (attribute == null) {
         if (other.attribute != null) return false;
      }
      else if (!attribute.equals(other.attribute)) return false;
      if (value == null) {
         if (other.value != null) return false;
      }
      else if (!value.equals(other.value)) return false;
      return true;
   }
}
