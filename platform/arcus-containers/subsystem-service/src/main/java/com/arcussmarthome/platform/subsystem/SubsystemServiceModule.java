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
package com.arcussmarthome.platform.subsystem;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.core.dao.cassandra.CassandraAuthorizationGrantDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraPersonDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraPersonPlaceAssocDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraPlaceDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.arcussmarthome.core.messaging.kafka.KafkaModule;
import com.arcussmarthome.messages.MessageConstants;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.service.SubsystemService;
import com.arcussmarthome.platform.history.cassandra.CassandraHistoryDAOModule;
import com.arcussmarthome.platform.manufacture.kitting.dao.ManufactureKittingDaoModule;
import com.arcussmarthome.platform.model.ModelDaoModule;
import com.arcussmarthome.platform.rule.RuleDaoModule;
import com.arcussmarthome.platform.subsystem.incident.AlarmIncidentModule;
import com.arcussmarthome.population.PlacePopulationCacheModule;
import com.arcussmarthome.prodcat.ProductCatalogReloadListener;
import com.netflix.governator.annotations.Modules;

@Modules(include={
      KafkaModule.class,
      SubsystemDaoModule.class,
      CassandraAuthorizationGrantDAOModule.class,
      CassandraHistoryDAOModule.class,
      CassandraPersonDAOModule.class,
      CassandraPersonPlaceAssocDAOModule.class,
      CassandraPlaceDAOModule.class,
      CassandraResourceBundleDAOModule.class,
      RuleDaoModule.class,
      ModelDaoModule.class,
      SubsystemModule.class,
      AlarmIncidentModule.class,
      ManufactureKittingDaoModule.class,
      PlacePopulationCacheModule.class
})
public class SubsystemServiceModule extends AbstractIrisModule {

   @Override
   protected void configure() {

   }
   
   @Provides
   @Singleton
   @Named(ProductCatalogReloadListener.GENERIC_MESSAGE_BUS_ACTOR_ADDRESS)
   public Address provideMessageBusActorAddress() {
      return Address.fromString(MessageConstants.SERVICE + ":" + SubsystemService.NAMESPACE + ":");
   }

}

