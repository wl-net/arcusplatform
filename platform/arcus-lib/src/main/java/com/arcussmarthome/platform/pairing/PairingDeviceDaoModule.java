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
package com.arcussmarthome.platform.pairing;

import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.core.dao.cassandra.CassandraModule;
import com.arcussmarthome.platform.pairing.cassandra.CassandraPairingDeviceDao;
import com.arcussmarthome.population.PlacePopulationCacheModule;
import com.netflix.governator.annotations.Modules;

@Modules(include={CassandraModule.class, PlacePopulationCacheModule.class})
public class PairingDeviceDaoModule extends AbstractIrisModule {

	@Override
	protected void configure() {
		bind(PairingDeviceDao.class).to(CassandraPairingDeviceDao.class);
	}

}

