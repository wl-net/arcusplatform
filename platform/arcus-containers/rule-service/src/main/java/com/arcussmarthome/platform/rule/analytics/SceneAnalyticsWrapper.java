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
package com.arcussmarthome.platform.rule.analytics;

import com.arcussmarthome.common.rule.action.Action;
import com.arcussmarthome.common.scene.SceneContext;
import com.arcussmarthome.common.scene.SceneImpl;
import com.arcussmarthome.core.platform.AnalyticsMessageBus;
import com.arcussmarthome.core.platform.TaggedEventBuilder;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;

public class SceneAnalyticsWrapper extends SceneImpl {

	private final AnalyticsMessageBus analyticsBus;
	private final Address sceneAddress;
	private final SceneContext sceneContext;
	private final Action action;
	
	public SceneAnalyticsWrapper(AnalyticsMessageBus analyticsBus, Address address, SceneContext context, Action action) {
		super(address, context, action);		
		
		this.analyticsBus = analyticsBus;
		this.sceneAddress = address;
		this.sceneContext = context;
		this.action = action;
	}

	@Override
	public void execute() {
		super.execute();

		PlatformMessage analyticsMessage = TaggedEventBuilder.sceneFiredMessageBuilder()
			.withSource(sceneAddress)
			.withContext(sceneContext)
			.withAction(action)
			.build();
		
		analyticsBus.send(analyticsMessage);
	}	
}

