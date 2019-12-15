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
package com.arcussmarthome.platform.history;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.core.dao.cassandra.CassandraAlarmIncidentDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraAuthorizationGrantDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraDeviceDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraHubDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraPersonDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraPlaceDAOModule;
import com.arcussmarthome.core.messaging.kafka.KafkaModule;
import com.arcussmarthome.platform.history.appender.HistoryAppenderModule;
import com.arcussmarthome.platform.history.cassandra.CassandraHistoryDAOModule;
import com.arcussmarthome.platform.history.service.HistoryEventListener;
import com.arcussmarthome.platform.rule.RuleDaoModule;
import com.netflix.governator.annotations.Modules;

/**
 * 
 */
@Modules(include={ 
      KafkaModule.class, 
      HistoryAppenderModule.class, 
      CassandraHistoryDAOModule.class,
      CassandraPlaceDAOModule.class,
      CassandraDeviceDAOModule.class,
      CassandraAlarmIncidentDAOModule.class,
      CassandraHubDAOModule.class,
      CassandraPersonDAOModule.class,
      CassandraAuthorizationGrantDAOModule.class,
      RuleDaoModule.class
})
public class HistoryServiceModule extends AbstractIrisModule {
   
   @Inject(optional = true)
   @Named(value = "history.appenders.path")
   private String historyAppendersPath = "conf/history.xml";

   @Override
   protected void configure() {
      // make it go
      bind(HistoryEventListener.class).asEagerSingleton();
   }

}

