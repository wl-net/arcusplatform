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
package com.arcussmarthome.ipcd.server.session;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.bridge.bus.ProtocolBusService;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.client.Client;
import com.arcussmarthome.bridge.server.session.Session;
import com.arcussmarthome.bridge.server.session.SessionFactory;
import com.arcussmarthome.bridge.server.session.SessionRegistry;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.core.protocol.ipcd.IpcdDeviceDao;
import com.arcussmarthome.platform.partition.Partitioner;
import com.arcussmarthome.population.PlacePopulationCacheManager;

import io.netty.channel.Channel;

@Singleton
public class IpcdSessionFactory implements SessionFactory {
   private final SessionRegistry parent;
   private final IpcdDeviceDao ipcdDeviceDao;
   private final DeviceDAO deviceDao;
   private final PlaceDAO placeDao;
   private final PlatformMessageBus platformBus;
   private final ProtocolBusService protocolBusService;
   private final Partitioner partitioner;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public IpcdSessionFactory(
         SessionRegistry parent,
         IpcdDeviceDao ipcdDeviceDao,
         DeviceDAO deviceDao,
         PlaceDAO placeDao,
         PlatformMessageBus platformBus,
         ProtocolBusService protocolBusService,
         Partitioner partitioner,
         PlacePopulationCacheManager populationCacheMgr) {
      this.parent = parent;
      this.ipcdDeviceDao = ipcdDeviceDao;
      this.deviceDao = deviceDao;
      this.placeDao = placeDao;
      this.platformBus = platformBus;
      this.protocolBusService = protocolBusService;
      this.partitioner = partitioner;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public Session createSession(Client client, Channel channel, BridgeMetrics bridgeMetrics) {
      return new IpcdSocketSession(parent, ipcdDeviceDao, deviceDao, placeDao, channel,
         platformBus, protocolBusService, partitioner, bridgeMetrics, populationCacheMgr);
   }

}

