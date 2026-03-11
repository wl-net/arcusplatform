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

import com.google.common.base.Predicate;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.GuardConditionAdapter;
import com.iris.messages.model.Model;

/**
 * A guard condition that checks presence state.
 *
 * Supports:
 * - "occupied" — at least one person is present at the place
 * - "unoccupied" — nobody is present at the place
 * - specific person address — checks if a specific person is present
 */
public class PresenceConfig implements ConditionConfig {

   public static final String TYPE = "presence";

   public static final String MODE_OCCUPIED = "OCCUPIED";
   public static final String MODE_UNOCCUPIED = "UNOCCUPIED";
   public static final String MODE_PERSON_HOME = "PERSON_HOME";
   public static final String MODE_PERSON_AWAY = "PERSON_AWAY";

   private String mode = MODE_OCCUPIED;
   private String personAddress;

   public String getMode() {
      return mode;
   }

   public void setMode(String mode) {
      this.mode = mode;
   }

   public String getPersonAddress() {
      return personAddress;
   }

   public void setPersonAddress(String personAddress) {
      this.personAddress = personAddress;
   }

   @Override
   public String getType() {
      return TYPE;
   }

   @Override
   public Condition generate(Map<String, Object> values) {
      // Build a predicate that checks the presence subsystem model attributes.
      // The PresenceSubsystem exposes:
      //   subspres:occupied (boolean)
      //   subspres:peopleAtHome (set<string> of person addresses)
      //   presences:peopleAway (set<string> of person addresses)
      Predicate<Model> predicate;
      String description;

      switch (mode) {
         case MODE_OCCUPIED:
            predicate = presenceOccupied(true);
            description = "someone is home";
            break;
         case MODE_UNOCCUPIED:
            predicate = presenceOccupied(false);
            description = "nobody is home";
            break;
         case MODE_PERSON_HOME:
            predicate = personAtHome(personAddress, true);
            description = "person " + personAddress + " is home";
            break;
         case MODE_PERSON_AWAY:
            predicate = personAtHome(personAddress, false);
            description = "person " + personAddress + " is away";
            break;
         default:
            throw new IllegalArgumentException("Unknown presence mode: " + mode);
      }

      return new GuardConditionAdapter(description, "subspres:", predicate);
   }

   private static Predicate<Model> presenceOccupied(boolean expectOccupied) {
      return model -> {
         if (model == null) return false;
         String address = String.valueOf(model.getAttribute("base:address"));
         // Only match the presence subsystem model
         if (!address.contains("subspres")) return false;
         Object occupied = model.getAttribute("subspres:occupied");
         if (occupied == null) return !expectOccupied;
         return Boolean.valueOf(occupied.toString()) == expectOccupied;
      };
   }

   private static Predicate<Model> personAtHome(String personAddr, boolean expectHome) {
      return model -> {
         if (model == null || personAddr == null) return false;
         String address = String.valueOf(model.getAttribute("base:address"));
         if (!address.contains("subspres")) return false;
         Object peopleAtHome = model.getAttribute("subspres:peopleAtHome");
         if (peopleAtHome == null) return !expectHome;
         @SuppressWarnings("unchecked")
         java.util.Set<String> people = (java.util.Set<String>) peopleAtHome;
         return people.contains(personAddr) == expectHome;
      };
   }

   @Override
   public String toString() {
      return "PresenceConfig [mode=" + mode + ", personAddress=" + personAddress + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((mode == null) ? 0 : mode.hashCode());
      result = prime * result + ((personAddress == null) ? 0 : personAddress.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      PresenceConfig other = (PresenceConfig) obj;
      if (mode == null) {
         if (other.mode != null) return false;
      }
      else if (!mode.equals(other.mode)) return false;
      if (personAddress == null) {
         if (other.personAddress != null) return false;
      }
      else if (!personAddress.equals(other.personAddress)) return false;
      return true;
   }
}
