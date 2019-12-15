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
package com.arcussmarthome.bridge.server.netty;

import io.netty.channel.Channel;

import java.util.Set;

import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.client.Client;
import com.arcussmarthome.bridge.server.client.ClientFactory;
import com.arcussmarthome.bridge.server.config.BridgeServerConfig;
import com.arcussmarthome.bridge.server.http.RequestAuthorizer;
import com.arcussmarthome.bridge.server.http.RequestHandler;
import com.arcussmarthome.bridge.server.http.RequestMatcher;
import com.arcussmarthome.bridge.server.message.DeviceMessageHandler;
import com.arcussmarthome.bridge.server.session.ClientToken;
import com.arcussmarthome.bridge.server.session.Session;
import com.arcussmarthome.bridge.server.session.SessionFactory;
import com.arcussmarthome.bridge.server.session.SessionListener;
import com.arcussmarthome.bridge.server.session.SessionRegistry;
import com.arcussmarthome.netty.server.session.IrisNettyClientClientToken;
import com.arcussmarthome.netty.server.session.IrisNettyClientIds;

public class WebSocketServerHandler extends Text10WebSocketServerHandler {
   public WebSocketServerHandler(
      BridgeServerConfig serverConfig,
      BridgeMetrics metrics,
      SessionFactory sessionFactory,
      Set<SessionListener> sessionListeners,
      SessionRegistry sessionRegistry,
      Set<RequestHandler> handlers,
      RequestMatcher webSocketUpgradeMatcher,
      RequestAuthorizer sessionAuthorizer,
      ClientFactory clientFactory,
      DeviceMessageHandler<String> deviceMessageHandler
      ) {
      super(serverConfig, metrics, sessionFactory, sessionListeners,
         sessionRegistry, handlers, webSocketUpgradeMatcher,
         sessionAuthorizer, clientFactory, deviceMessageHandler);
   }

   @Override
   protected Session createSession(Client client, Channel ch, BridgeMetrics metrics) {
      // Go ahead and register the session on creation since we don't need a message
      // to assign a key to the session.
      Session session = sessionFactory.createSession(client, ch, metrics);
      if (!session.isInitialized()) {
         ClientToken ct = new IrisNettyClientClientToken(IrisNettyClientIds.createId());
         session.setClientToken(ct);
         metrics.incSessionCreatedCounter();
         sessionRegistry.putSession(session);
      }
      return session;
   }
}

