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
package com.arcussmarthome.video.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.core.dao.cassandra.CassandraPlaceDAOModule;
import com.arcussmarthome.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.arcussmarthome.core.messaging.kafka.KafkaModule;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.io.json.gson.GsonModule;
import com.arcussmarthome.population.PlacePopulationCacheModule;
import com.arcussmarthome.video.PreviewModule;
import com.arcussmarthome.video.cql.v2.CassandraVideoV2Module;
import com.arcussmarthome.video.recording.CameraDeletedListener;
import com.arcussmarthome.video.recording.PlaceServiceLevelCacheModule;
import com.arcussmarthome.video.recording.PlaceServiceLevelDowngradeListener;
import com.arcussmarthome.video.service.dao.VideoServiceDao;
import com.arcussmarthome.video.service.quota.VideoQuotaEnforcer;
import com.arcussmarthome.video.service.quota.VideoQuotaEnforcerAllowAll;
import com.arcussmarthome.video.service.quota.VideoQuotaEnforcerDeleteOldest;
import com.arcussmarthome.video.service.quota.VideoQuotaEnforcerDenyAll;
import com.arcussmarthome.video.service.quota.VideoQuotaEnforcerDenyAllRecordings;
import com.arcussmarthome.video.service.quota.VideoQuotaEnforcerDisallowNew;
import com.netflix.governator.annotations.Modules;

@Modules(include={
      KafkaModule.class,
      CassandraResourceBundleDAOModule.class,
      CassandraPlaceDAOModule.class,
      CassandraVideoV2Module.class,
      GsonModule.class,
      VideoModule.class,
      PreviewModule.class,
      PlacePopulationCacheModule.class,
      PlaceServiceLevelCacheModule.class
})
public class VideoServiceModule extends AbstractIrisModule {
   private static final Logger log = LoggerFactory.getLogger(VideoServiceModule.class);

   @Inject(optional = true) @Named("video.quota.enforcer.premium")
   private String premiumVideoQuotaEnforcer = "allow.all";

   @Inject(optional = true) @Named("video.quota.enforcer.basic")
   private String basicVideoQuotaEnforcer = "allow.all";

   @Inject @Named("video.quota.enforcer.oldest.maxdelete")
   private int videoQuotaEnforcerMaxDelete;

   @Inject(optional = true) @Named("video.quota.enforcer.allow.on.fail")
   private boolean videoQuotaEnforcerAllowOnFail = true;

   @Override
   protected void configure() {
   	bind(PlaceServiceLevelDowngradeListener.class).asEagerSingleton();
   	bind(CameraDeletedListener.class).asEagerSingleton();
   }

   @Provides @Named("video.quota.enforcer.impl.premium")
   public VideoQuotaEnforcer provideVideoServiceQuotaEnforcerPremium(PlatformMessageBus platformBus, VideoServiceDao videoDao) {
      return getQuotaEnforcer("premium", premiumVideoQuotaEnforcer, platformBus, videoDao);
   }

   @Provides @Named("video.quota.enforcer.impl.basic")
   public VideoQuotaEnforcer provideVideoServiceQuotaEnforcerBasic(PlatformMessageBus platformBus, VideoServiceDao videoDao) {
      return getQuotaEnforcer("basic", basicVideoQuotaEnforcer, platformBus, videoDao);
   }

   @Provides
   public VideoQuotaEnforcer getQuotaEnforcer(String name, String type, PlatformMessageBus platformBus, VideoServiceDao videoDao) {
      switch (type) {
      case "delete.oldest":
         log.info("video quota enforcement for {}: deleting oldest recordings", name);
         return new VideoQuotaEnforcerDeleteOldest(platformBus, videoDao, videoQuotaEnforcerAllowOnFail, videoQuotaEnforcerMaxDelete);

      case "deny.new":
         log.info("video quota enforcement for {}: denying new recordings", name);
         return new VideoQuotaEnforcerDisallowNew();

      case "deny.all":
         log.info("video quota enforcement for {}: denying all recordings and streams", name);
         return new VideoQuotaEnforcerDenyAll();

      case "deny.all.recordings":
         log.info("video quota enforcement for {}: denying all recordings", name);
         return new VideoQuotaEnforcerDenyAllRecordings();

      case "allow.all":
      default:
         if ("allow.all".equals(type)) {
            log.info("video quota enforcement for {}: allowing all recordings", name);
         } else {
            log.info("video quota enforcement for {}: unknown enforcer {}, defaulting to allowing all recordings", name, type);
         }

         return new VideoQuotaEnforcerAllowAll();
      }
   }
}

