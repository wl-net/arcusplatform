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
package com.iris.driver.reflex;

public final class ReflexActionSetAttributeFromVariable implements ReflexAction {
   public static final String VAR_ZIGBEE_VALUE = "zigbeeValue";

   private final String attr;
   private final String variable;
   private final double divisor;

   public ReflexActionSetAttributeFromVariable(String attr, String variable, double divisor) {
      this.attr = attr;
      this.variable = variable;
      this.divisor = divisor;
   }

   public String getAttr() {
      return attr;
   }

   public String getVariable() {
      return variable;
   }

   public double getDivisor() {
      return divisor;
   }

   @Override
   public String toString() {
      return "ReflexActionSetAttributeFromVariable [" +
         "attr=" + attr +
         ",variable=" + variable +
         ",divisor=" + divisor +
         "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((attr == null) ? 0 : attr.hashCode());
      result = prime * result + ((variable == null) ? 0 : variable.hashCode());
      long temp = Double.doubleToLongBits(divisor);
      result = prime * result + (int)(temp ^ (temp >>> 32));
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      ReflexActionSetAttributeFromVariable other = (ReflexActionSetAttributeFromVariable) obj;
      if (attr == null) {
         if (other.attr != null)
            return false;
      } else if (!attr.equals(other.attr))
         return false;
      if (variable == null) {
         if (other.variable != null)
            return false;
      } else if (!variable.equals(other.variable))
         return false;
      if (Double.doubleToLongBits(divisor) != Double.doubleToLongBits(other.divisor))
         return false;
      return true;
   }
}
