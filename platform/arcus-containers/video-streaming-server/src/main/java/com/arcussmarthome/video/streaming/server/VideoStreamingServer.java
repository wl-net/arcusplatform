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
package com.arcussmarthome.video.streaming.server;

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.arcussmarthome.bridge.server.BridgeServer;
import com.arcussmarthome.bridge.server.ServerRunner;
import com.arcussmarthome.core.IrisAbstractApplication;
import com.arcussmarthome.core.dao.cassandra.CassandraModule;
import com.arcussmarthome.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.arcussmarthome.core.metricsreporter.builder.MetricsTopicReporterBuilderModule;
import com.arcussmarthome.video.streaming.server.dao.VideoStreamingDao;

public class VideoStreamingServer extends BridgeServer {
   private static final Logger log = LoggerFactory.getLogger(VideoStreamingServer.class);
   private final VideoStreamingServerConfig videoConfig;

   @Inject
   public VideoStreamingServer(
         VideoStreamingDao dao,
         VideoStreamingServerConfig videoConfig,
         ServerRunner runner
   ) {
      super(runner);
      this.videoConfig = videoConfig;
   }

   @Override
   protected void start() throws Exception {
      log.info("Starting web server at " + getSocketBindAddress().getAddress() + ":" + getPortNumber());
      startServer();
   }

   public static void main(String[] args) {
      Collection<Class<? extends Module>> modules = Arrays.asList(
         VideoStreamingServerModule.class,
         CassandraModule.class,
         CassandraResourceBundleDAOModule.class,
         MetricsTopicReporterBuilderModule.class
      );

      IrisAbstractApplication.exec(VideoStreamingServer.class, modules, args);
   }
}

