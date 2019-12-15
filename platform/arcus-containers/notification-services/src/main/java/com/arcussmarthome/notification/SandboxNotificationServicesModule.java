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
package com.arcussmarthome.notification;

import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.core.dao.cassandra.CassandraDAOModule;
import com.arcussmarthome.core.messaging.MessagesModule;
import com.arcussmarthome.core.messaging.kafka.KafkaModule;
import com.arcussmarthome.core.template.TemplateModule;
import com.arcussmarthome.notification.dispatch.Dispatcher;
import com.arcussmarthome.notification.dispatch.NotificationDispatcher;
import com.arcussmarthome.notification.message.NotificationMessageRenderer;
import com.arcussmarthome.notification.message.TemplateMessageRenderer;
import com.arcussmarthome.notification.provider.ApnsProvider;
import com.arcussmarthome.notification.provider.MapNotificationProviderRegistry;
import com.arcussmarthome.notification.provider.NotificationProvider;
import com.arcussmarthome.notification.provider.NotificationProviderRegistry;
import com.arcussmarthome.notification.provider.apns.ApnsSender;
import com.arcussmarthome.notification.provider.apns.PushyApnsSender;
import com.arcussmarthome.notification.retry.BackoffRetryManager;
import com.arcussmarthome.notification.retry.RetryManager;
import com.arcussmarthome.notification.retry.RetryProcessor;
import com.arcussmarthome.notification.retry.ScheduledRetryProcessor;
import com.arcussmarthome.notification.upstream.IrisUpstreamNotificationResponder;
import com.arcussmarthome.notification.upstream.UpstreamNotificationResponder;
import com.arcussmarthome.platform.notification.audit.CassandraAuditor;
import com.arcussmarthome.platform.notification.audit.NotificationAuditor;
import com.arcussmarthome.platform.rule.RuleDaoModule;
import com.arcussmarthome.util.ThreadPoolBuilder;

public class SandboxNotificationServicesModule extends AbstractIrisModule {

	@Inject
	public SandboxNotificationServicesModule(
			MessagesModule messages,
			KafkaModule kafka,
			CassandraDAOModule cassandra,
			RuleDaoModule ruleDaoModule,
			TemplateModule template)
	{
	}

	@Override
	protected void configure() {
	   bind(NotificationServiceConfig.class);
      bind(Dispatcher.class).to(NotificationDispatcher.class);
      bind(NotificationAuditor.class).to(CassandraAuditor.class);
      bind(RetryManager.class).to(BackoffRetryManager.class);
      bind(RetryProcessor.class).to(ScheduledRetryProcessor.class);
      bind(NotificationMessageRenderer.class).to(TemplateMessageRenderer.class);
      bind(NotificationProviderRegistry.class).to(MapNotificationProviderRegistry.class);
      bind(ApnsSender.class).to(PushyApnsSender.class);
      bind(UpstreamNotificationResponder.class).to(IrisUpstreamNotificationResponder.class);
      bind(NotificationService.class).asEagerSingleton();

      MapBinder<String, NotificationProvider> registryBinder = MapBinder.newMapBinder(binder(), String.class, NotificationProvider.class);
      registryBinder.addBinding("APNS").to(ApnsProvider.class);
	}

	@Provides @Named("notifications.executor")
	public ExecutorService getNotificationsExecutor(NotificationServiceConfig config) {
      return new ThreadPoolBuilder()
         .withMaxPoolSize(config.getMaxThreads())
         .withKeepAliveMs(config.getThreadKeepAliveMs())
         .withNameFormat("notification-dispatcher-%d")
         .withBlockingBacklog()
         .withMetrics("service.notifications")
         .build();
	}
}

