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
package com.arcussmarthome.platform.rule;

import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.platform.rule.cassandra.ActionDaoImpl;
import com.arcussmarthome.platform.rule.cassandra.RuleDaoImpl;
import com.arcussmarthome.platform.rule.cassandra.RuleEnvironmentDaoImpl;
import com.arcussmarthome.platform.rule.cassandra.SceneDaoImpl;
import com.arcussmarthome.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.arcussmarthome.platform.scene.SceneDao;
import com.netflix.governator.annotations.Modules;

@Modules(include = RuleConfigJsonModule.class)
public class RuleDaoModule extends AbstractIrisModule {

   protected void configure() {
      bind(ActionDao.class).to(ActionDaoImpl.class);
      bind(SceneDao.class).to(SceneDaoImpl.class);
      bind(RuleDao.class).to(RuleDaoImpl.class);
      bind(RuleEnvironmentDao.class).to(RuleEnvironmentDaoImpl.class);
   }

}

