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
package com.iris.platform.rule.cassandra;

import java.util.Date;
import java.util.List;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.io.json.JSON;
import com.iris.platform.rule.automation.AutomationChainConfig;
import com.iris.platform.rule.automation.AutomationDao;
import com.iris.platform.rule.automation.AutomationDefinition;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.cassandra.RuleEnvironmentTable.Column;
import com.iris.util.TypeMarker;

/**
 * Cassandra implementation of AutomationDao.
 *
 * Stores automations in the RuleEnvironment table with type='automation'.
 * The trigger, conditions, and actions are stored as JSON in the
 * conditionconfig (trigger + conditions) and actionconfig columns.
 *
 * Storage layout:
 * - conditionconfig: JSON object with {"trigger": ..., "conditions": [...]}
 * - actionconfig: JSON array of action configs
 * - ruleDisabled: whether the automation is disabled
 */
@Singleton
public class AutomationDaoImpl
      extends BaseRuleEnvironmentDaoImpl<AutomationDefinition>
      implements AutomationDao {

   static final String TYPE = AutomationDefinition.TYPE;

   private static final String[] UPSERT_COLUMNS = new String[] {
      Column.CREATED.columnName(),
      Column.MODIFIED.columnName(),
      Column.NAME.columnName(),
      Column.DESCRIPTION.columnName(),
      Column.TAGS.columnName(),
      "ruleDisabled",
      "actionconfig",
      "conditionconfig",
      "actionLastExecuted",
   };

   private final PreparedStatement upsert;

   @Inject
   public AutomationDaoImpl(CqlSession session) {
      super(session, TYPE);

      this.upsert = CassandraQueryBuilder.update(RuleEnvironmentTable.NAME)
            .addColumns(UPSERT_COLUMNS)
            .where(whereIdEq(TYPE))
            .prepare(session);
   }

   @Override
   protected AutomationDefinition buildEntity(Row row) {
      AutomationDefinition definition = new AutomationDefinition();

      definition.setPlaceId(row.getUuid(Column.PLACE_ID.columnName()));
      definition.setSequenceId(row.getInt(Column.ID.columnName()));
      definition.setCreated(row.isNull(Column.CREATED.columnName()) ? null
            : Date.from(row.getInstant(Column.CREATED.columnName())));
      definition.setModified(row.isNull(Column.MODIFIED.columnName()) ? null
            : Date.from(row.getInstant(Column.MODIFIED.columnName())));
      definition.setName(row.getString(Column.NAME.columnName()));
      definition.setDescription(row.getString(Column.DESCRIPTION.columnName()));
      definition.setTags(row.getSet(Column.TAGS.columnName(), String.class));
      definition.setDisabled(row.getBoolean("ruleDisabled"));

      if (!row.isNull("actionLastExecuted")) {
         definition.setLastExecuted(Date.from(row.getInstant("actionLastExecuted")));
      }

      // Deserialize the chain configuration
      String conditionJson = row.getString("conditionconfig");
      if (conditionJson != null && !conditionJson.isEmpty()) {
         AutomationChainConfig chainConfig = JSON.fromJson(conditionJson, AutomationChainConfig.class);
         definition.setTrigger(chainConfig.getTrigger());
         definition.setConditions(chainConfig.getConditions());
      }

      String actionJson = row.getString("actionconfig");
      if (actionJson != null && !actionJson.isEmpty()) {
         List<ActionConfig> actions = JSON.fromJson(actionJson,
               TypeMarker.listOf(ActionConfig.class));
         definition.setActions(actions);
      }

      return definition;
   }

   @Override
   protected Statement<?> prepareUpsert(AutomationDefinition definition, Date ts) {
      // Serialize the chain: trigger + conditions go into conditionconfig
      AutomationChainConfig chainConfig = new AutomationChainConfig();
      chainConfig.setTrigger(definition.getTrigger());
      chainConfig.setConditions(definition.getConditions());
      String conditionJson = JSON.toJson(chainConfig);

      // Actions go into actionconfig as a list
      String actionJson = JSON.toJson(definition.getActions());

      BoundStatement bs = upsert.bind();
      bs = bs.setUuid(Column.PLACE_ID.columnName(), definition.getPlaceId());
      bs = bs.setInt(Column.ID.columnName(), definition.getSequenceId());
      bs = bs.setInstant(Column.CREATED.columnName(),
            definition.getCreated() == null ? null : definition.getCreated().toInstant());
      bs = bs.setInstant(Column.MODIFIED.columnName(),
            definition.getModified() == null ? null : definition.getModified().toInstant());
      bs = bs.setString(Column.NAME.columnName(), definition.getName());
      bs = bs.setString(Column.DESCRIPTION.columnName(), definition.getDescription());
      bs = bs.setSet(Column.TAGS.columnName(), definition.getTags(), String.class);
      bs = bs.setBoolean("ruleDisabled", definition.isDisabled());
      bs = bs.setString("actionconfig", actionJson);
      bs = bs.setString("conditionconfig", conditionJson);
      bs = bs.setInstant("actionLastExecuted",
            definition.getLastExecuted() == null ? null : definition.getLastExecuted().toInstant());

      return bs;
   }
}
