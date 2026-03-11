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
package com.iris.platform.automation;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bootstrap.annotations.Modules;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.cassandra.CassandraDAOModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.platform.automation.service.AutomationService;
import com.iris.platform.model.ModelDaoModule;
import com.iris.platform.rule.RuleDaoModule;
import com.iris.population.PlacePopulationCacheModule;
import com.iris.util.ThreadPoolBuilder;

@Modules(include = {
      KafkaModule.class,
      CassandraDAOModule.class,
      RuleDaoModule.class,
      ModelDaoModule.class,
      PlacePopulationCacheModule.class
})
public class AutomationServiceModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      bind(AutomationService.class).asEagerSingleton();
   }

   @Provides
   @Singleton
   @Named(AutomationService.PROP_THREADPOOL)
   public Executor automationPool() {
      return new ThreadPoolBuilder()
            .withBlockingBacklog()
            .withMaxPoolSize(100)
            .withKeepAliveMs(10000)
            .withMetrics("automation")
            .withNameFormat("automation-%d")
            .build();
   }
}
