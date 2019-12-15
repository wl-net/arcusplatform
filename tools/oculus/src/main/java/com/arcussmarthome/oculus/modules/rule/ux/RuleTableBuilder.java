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
/**
 * 
 */
package com.arcussmarthome.oculus.modules.rule.ux;

import com.arcussmarthome.client.model.RuleModel;
import com.arcussmarthome.client.model.Store;
import com.arcussmarthome.oculus.widget.table.Table;
import com.arcussmarthome.oculus.widget.table.TableModel;
import com.arcussmarthome.oculus.widget.table.TableModelBuilder;

/**
 * 
 */
public class RuleTableBuilder {
   private TableModel<RuleModel> model;

   /**
    * 
    */
   public RuleTableBuilder(Store<RuleModel> store) {
      model =
         TableModelBuilder
            .builder(store)
            .columnBuilder()
               .withName("Name")
               .withGetter((m) -> m.getName())
               .add()
            .columnBuilder()
               .withName("Description")
               .withGetter((m) -> m.getDescription())
               .add()
            .columnBuilder()
               .withName("State")
               .withGetter((m) -> m.getState())
               .add()
            .columnBuilder()
               .withName("Last Modified")
               .withGetter((m) -> m.getModified())
               .add()
            .build();
   }
   
   public Table<RuleModel> build() {
      return new Table<>(model);
   }

}

