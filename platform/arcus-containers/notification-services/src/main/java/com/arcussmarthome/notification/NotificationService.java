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
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.core.platform.AbstractPlatformService;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.NotificationCapability;
import com.arcussmarthome.metrics.IrisMetricSet;
import com.arcussmarthome.metrics.IrisMetrics;
import com.arcussmarthome.notification.dispatch.Dispatcher;
import com.arcussmarthome.platform.notification.Notification;

@Singleton
public class NotificationService extends AbstractPlatformService {
   public static final String SERVICE_NAME = "notifications";
   private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);
   private static final IrisMetricSet METRICS = IrisMetrics.metrics(SERVICE_NAME);

   private final Timer dispatchLatencyTimer = METRICS.timer("message.latency");
   private final Counter dispatchCounter = METRICS.counter("message.count");

   private final Dispatcher dispatcher;
   private final NotificationServiceConfig config;

   @Inject
   public NotificationService(PlatformMessageBus platformBus, Dispatcher dispatcher, NotificationServiceConfig config, @Named("notifications.executor") ExecutorService executor) {
      super(platformBus, SERVICE_NAME, executor);
      this.dispatcher = dispatcher;
      this.config = config;
   }

   @Override
   protected void onStart() {
      LOGGER.info("Started notification service with threads: {} thread-keepalive: {} ms", config.getMaxThreads(), config.getThreadKeepAliveMs());
      addListeners(Address.platformService(NotificationCapability.NAMESPACE));
   }
 
   @Override
   public void doHandleMessage(PlatformMessage message) {
      if(!message.getMessageType().startsWith(NotificationCapability.NAMESPACE)) {
      	
         super.handleMessage(message);
         return;
      }
   	
      Notification notification = Notification.fromPlatformMessage(message);

      // Note the amount of time the notification was left enqueued...
      dispatchLatencyTimer.update(notification.getEnqueuedDuration().toMillis(), TimeUnit.MILLISECONDS);
      dispatchCounter.inc();

      dispatcher.dispatch(notification);
   }
}

