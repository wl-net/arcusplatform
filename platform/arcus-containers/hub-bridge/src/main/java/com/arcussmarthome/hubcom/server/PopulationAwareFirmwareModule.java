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
package com.arcussmarthome.hubcom.server;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.arcussmarthome.core.dao.cassandra.CassandraHubDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraPlaceDAOModule;
import com.arcussmarthome.core.dao.file.PopulationDAOModule;
import com.arcussmarthome.firmware.HubMinimumFirmwareVersionResolver;
import com.arcussmarthome.firmware.MinimumFirmwareVersionResolver;
import com.arcussmarthome.firmware.hub.HubFirmwareModule;
import com.arcussmarthome.messages.model.Hub;
import com.arcussmarthome.population.DaoHubPopulationResolver;
import com.arcussmarthome.population.HubPopulationResolver;

public class PopulationAwareFirmwareModule extends HubFirmwareModule {

   @Inject
   public PopulationAwareFirmwareModule(CassandraHubDAOModule hubDao, CassandraPlaceDAOModule placeDao, PopulationDAOModule populationDao) {
   }

   @Override
   protected void configure() {
      super.configure();
      bind(HubPopulationResolver.class).to(DaoHubPopulationResolver.class);
      bind(new TypeLiteral<MinimumFirmwareVersionResolver<Hub>>(){}).to(new TypeLiteral<HubMinimumFirmwareVersionResolver>(){});
   }

}

