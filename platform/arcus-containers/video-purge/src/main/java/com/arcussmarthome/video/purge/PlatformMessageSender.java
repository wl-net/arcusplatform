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
package com.arcussmarthome.video.purge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.core.messaging.kafka.KafkaConfig;
import com.arcussmarthome.core.messaging.kafka.KafkaMessageSender;
import com.arcussmarthome.io.Serializer;
import com.arcussmarthome.io.json.JSON;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.platform.partition.Partitioner;
import com.arcussmarthome.platform.partition.PlatformPartition;

@Singleton
public class PlatformMessageSender {
	private static final Logger log = LoggerFactory.getLogger(PlatformMessageSender.class);
   private static final Serializer<PlatformMessage> PLATFORM_MESSAGE_SERIALIZER = JSON.createSerializer(PlatformMessage.class);
	private final KafkaMessageSender sender;
   private final Partitioner partitioner;
   private final KafkaConfig config;
   
	@Inject
	public PlatformMessageSender(KafkaConfig config, Partitioner partitioner, KafkaMessageSender sender) {
		this.config = config;
		this.sender = sender;
      this.partitioner = partitioner;
	}
	
	public void send(PlatformMessage msg) {
		PlatformPartition partition = partitioner.getPartitionForMessage(msg);
      sender.submit(config.getTopicPlatform(), partition, msg, PLATFORM_MESSAGE_SERIALIZER);
	}
	
	
}

