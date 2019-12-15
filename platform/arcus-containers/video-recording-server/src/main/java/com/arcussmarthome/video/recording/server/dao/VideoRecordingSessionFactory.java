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
package com.arcussmarthome.video.recording.server.dao;

import java.io.IOException;

import org.apache.commons.io.output.CountingOutputStream;

import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.media.H264Factory;
import com.arcussmarthome.media.RtspPushHeaders;
import com.arcussmarthome.video.VideoSessionRegistry;
import com.arcussmarthome.video.cql.VideoRecordingManager;
import com.arcussmarthome.video.recording.RecordingEventPublisher;
import com.arcussmarthome.video.recording.RecordingSessionFactory;
import com.arcussmarthome.video.recording.VideoTtlResolver;
import com.arcussmarthome.video.recording.server.VideoRecordingServerConfig;
import com.arcussmarthome.video.storage.VideoStorage;
import com.arcussmarthome.video.storage.VideoStorageSession;

import io.netty.channel.ChannelHandlerContext;

public class VideoRecordingSessionFactory extends RecordingSessionFactory<VideoRecordingSession> implements H264Factory {

   private final VideoRecordingServerConfig config;

   public VideoRecordingSessionFactory(
      PlaceDAO placeDAO,
      VideoRecordingManager dao,
      VideoSessionRegistry registry,
      VideoStorage videoStorage,
      RecordingEventPublisher eventPublisher,
      VideoRecordingServerConfig config,
      VideoTtlResolver ttlResolver
   ) {
      super(placeDAO, dao, registry, videoStorage, eventPublisher, config.getRecordSecretAsBytes(), config.getRecordingSessionTimeout(), ttlResolver);
      this.config = config;
   }

   @Override
   public VideoSession createVideoSession(ChannelHandlerContext ctx, RtspPushHeaders hdrs) throws IOException {
      return createSession(ctx, hdrs);
   }

   @Override
   protected VideoRecordingSession create(VideoSessionRegistry registry, ChannelHandlerContext ctx, VideoRecordingManager dao, RecordingEventPublisher eventPublisher, VideoStorageSession storage, double precapture, boolean stream) {
      try {
         return new VideoRecordingSession(registry, ctx, dao, eventPublisher, storage, precapture, stream, new CountingOutputStream(storage.output()), config.getVideoFlushFrequency());
      } catch(Exception ioe) {
         throw new RuntimeException(ioe);
      }
   }
}

