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
package com.iris.platform.rule.automation;

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.messages.model.CompositeId;

/**
 * Data access for automation chain definitions.
 */
public interface AutomationDao {

   List<AutomationDefinition> listByPlace(UUID placeId);

   @Nullable AutomationDefinition findById(UUID placeId, Integer sequenceId);

   void save(AutomationDefinition definition);

   boolean delete(UUID placeId, Integer sequenceId);

   default @Nullable AutomationDefinition findById(CompositeId<UUID, Integer> id) {
      if (id == null) {
         return null;
      }
      return findById(id.getPrimaryId(), id.getSecondaryId());
   }

   default boolean delete(CompositeId<UUID, Integer> id) {
      if (id == null) {
         return false;
      }
      return delete(id.getPrimaryId(), id.getSecondaryId());
   }
}
