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
package com.arcussmarthome.platform.subsystem.incident;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.common.subsystem.alarm.incident.AlarmIncidentService;
import com.arcussmarthome.platform.alarm.incident.AlarmIncidentDAO;
import com.arcussmarthome.platform.alarm.incident.CassandraAlarmIncidentDAO;

/**
 * @author tweidlin
 *
 */
public class AlarmIncidentModule extends AbstractIrisModule {
	@Inject(optional=true)
	@Named("alarm.incident.threads")
	private int threads = 20;

	@Inject(optional=true)
	@Named("alarm.incident.threadKeepAliveMs")
	private long threadKeepAliveMs = 10000;

	@Override
	protected void configure() {
      bind(AlarmIncidentDAO.class).to(CassandraAlarmIncidentDAO.class);
		bind(AlarmIncidentServiceImpl.class);
		bind(AlarmIncidentService.class).to(AlarmIncidentServiceDispatcher.class);
		bind(AlarmIncidentHistoryListener.class).to(PlatformIncidentHistoryListener.class);
	}

}

