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
package com.arcussmarthome.bridge.server.session;

import io.netty.channel.Channel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.client.Client;

@Singleton
public class DefaultSessionFactoryImpl implements SessionFactory {
   private final SessionRegistry parent;

   @Inject
   public DefaultSessionFactoryImpl(SessionRegistry parent) {
      this.parent = parent;
   }

   @Override
   public Session createSession(Client client, Channel channel, BridgeMetrics bridgeMetrics) {
      return new DefaultSessionImpl(parent, channel, bridgeMetrics);
   }

}

