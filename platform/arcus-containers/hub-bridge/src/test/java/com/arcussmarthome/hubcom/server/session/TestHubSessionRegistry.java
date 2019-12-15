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
package com.arcussmarthome.hubcom.server.session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.arcussmarthome.bootstrap.ServiceLocator;
import com.arcussmarthome.bridge.server.client.ClientFactory;
import com.arcussmarthome.bridge.server.session.SessionListener;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.core.messaging.memory.InMemoryIntraServiceMessageBus;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.platform.hubbridge.HeartbeatMessage;
import com.arcussmarthome.platform.partition.PartitionConfig;
import com.arcussmarthome.platform.partition.Partitioner;
import com.arcussmarthome.platform.partition.simple.SimplePartitionModule;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;

@Modules({ InMemoryMessageModule.class, SimplePartitionModule.class, HubAuthModule.class })
@Mocks({HubDAO.class})
public class TestHubSessionRegistry extends IrisMockTestCase {

   @Provides
   public PartitionConfig partitionConfig() {
      PartitionConfig config = new PartitionConfig();
      config.setMemberId(0);
      config.setMembers(1);
      config.setPartitions(9);
      return config;
   }

   // unit under test
   HubSessionRegistry registry;

   @Inject Partitioner partitioner;
   @Inject InMemoryIntraServiceMessageBus intraServiceBus;
   @Inject HubDAO hubDao;

   private int hubsPerPartition = 8; // 8 hubs per partition
   private List<Set<String>> hubIdsByPartition;

   @Before
   public void stageHubs() {
      // don't inject so that start doesn't get called
      ClientFactory cf = ServiceLocator.getInstance(ClientFactory.class);
      Set<SessionListener> listeners = ImmutableSet.copyOf(ServiceLocator.getInstancesOf(SessionListener.class));
      this.registry = new HubSessionRegistry(cf, listeners, partitioner, intraServiceBus, hubDao);

      int partitions = partitioner.getPartitionCount();
      int count = hubsPerPartition * partitions;
      hubIdsByPartition = new ArrayList<Set<String>>(partitions);
      for(int i=0; i<partitions; i++) {
         hubIdsByPartition.add(new HashSet<>());
      }
      for(int i=0; i<count; i++) {
         HubSession session = new HubSession(registry, null, null, new HubClientToken(String.format("AAA-%04d", i)));
         session.setPartition(partitioner.getPartitionForHubId(session.getHubId()));
         hubIdsByPartition.get(i % partitions).add(session.getHubId());
         registry.putSession(session);
      }
   }

   @Test
   public void testHeartbeat() throws Exception {
      List<PlatformMessage> messages = new ArrayList<>();

      registry.heartbeat();
      messages = takeHeartbeats(4);
      assertEquals(4, messages.size());
      assertEquals(hubIdsByPartition.get(0), HeartbeatMessage.getConnectedHubIds(messages.get(0).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(1), HeartbeatMessage.getConnectedHubIds(messages.get(1).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(2), HeartbeatMessage.getConnectedHubIds(messages.get(2).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(3), HeartbeatMessage.getConnectedHubIds(messages.get(3).getValue().getAttributes()));
      messages.clear();

      registry.heartbeat();
      messages = takeHeartbeats(4);
      assertEquals(4, messages.size());
      assertEquals(hubIdsByPartition.get(4), HeartbeatMessage.getConnectedHubIds(messages.get(0).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(5), HeartbeatMessage.getConnectedHubIds(messages.get(1).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(6), HeartbeatMessage.getConnectedHubIds(messages.get(2).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(7), HeartbeatMessage.getConnectedHubIds(messages.get(3).getValue().getAttributes()));
      messages.clear();

      registry.heartbeat();
      messages = takeHeartbeats(4);
      assertEquals(4, messages.size());
      assertEquals(hubIdsByPartition.get(8), HeartbeatMessage.getConnectedHubIds(messages.get(0).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(0), HeartbeatMessage.getConnectedHubIds(messages.get(1).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(1), HeartbeatMessage.getConnectedHubIds(messages.get(2).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(2), HeartbeatMessage.getConnectedHubIds(messages.get(3).getValue().getAttributes()));
      messages.clear();

      registry.heartbeat();
      messages = takeHeartbeats(4);
      assertEquals(4, messages.size());
      assertEquals(hubIdsByPartition.get(3), HeartbeatMessage.getConnectedHubIds(messages.get(0).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(4), HeartbeatMessage.getConnectedHubIds(messages.get(1).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(5), HeartbeatMessage.getConnectedHubIds(messages.get(2).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(6), HeartbeatMessage.getConnectedHubIds(messages.get(3).getValue().getAttributes()));
      messages.clear();

      registry.heartbeat();
      messages = takeHeartbeats(4);
      assertEquals(4, messages.size());
      assertEquals(hubIdsByPartition.get(7), HeartbeatMessage.getConnectedHubIds(messages.get(0).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(8), HeartbeatMessage.getConnectedHubIds(messages.get(1).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(0), HeartbeatMessage.getConnectedHubIds(messages.get(2).getValue().getAttributes()));
      assertEquals(hubIdsByPartition.get(1), HeartbeatMessage.getConnectedHubIds(messages.get(3).getValue().getAttributes()));
      messages.clear();
   }

   private List<PlatformMessage> takeHeartbeats(int count) throws InterruptedException, TimeoutException {
      List<PlatformMessage> messages = new ArrayList<>(count);
      for(int i=0; i<count; i++) {
         messages.add(takeHeartbeat());
      }
      assertNull(intraServiceBus.poll());
      return messages;
   }

   private PlatformMessage takeHeartbeat() throws InterruptedException, TimeoutException {
      // TODO need a way to verify the partition
      PlatformMessage message = intraServiceBus.take();
      assertEquals(HeartbeatMessage.NAME, message.getMessageType());
      assertEquals(hubsPerPartition, HeartbeatMessage.getConnectedHubIds(message.getValue().getAttributes()).size());
      return message;
   }
}

