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
package com.arcussmarthome.hubcom.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.arcussmarthome.bridge.bus.ProtocolBusListener;
import com.arcussmarthome.bridge.server.netty.BridgeMdcUtil;
import com.arcussmarthome.bridge.server.session.ClientToken;
import com.arcussmarthome.bridge.server.session.Session;
import com.arcussmarthome.bridge.server.session.SessionRegistry;
import com.arcussmarthome.core.protocol.ProtocolMessageBus;
import com.arcussmarthome.hubcom.authz.HubMessageFilter;
import com.arcussmarthome.hubcom.server.session.HubSession;
import com.arcussmarthome.io.Serializer;
import com.arcussmarthome.io.json.JSON;
import com.arcussmarthome.messages.HubMessage;
import com.arcussmarthome.protocol.ProtocolMessage;
import com.arcussmarthome.util.MdcContext.MdcContextReference;

public class HubProtocolBusListener implements ProtocolBusListener {
   private final static Logger logger = LoggerFactory.getLogger(HubProtocolBusListener.class);
   private final ProtocolMessageBus protocolMessageBus;
   private final HubMessageFilter filter;
   private final SessionRegistry sessionRegistry;
   private final Serializer<ProtocolMessage> protocolSerializer;
   private final Serializer<HubMessage> hubSerializer;

   @Inject
   public HubProtocolBusListener(ProtocolMessageBus protocolMessageBus, HubMessageFilter filter, SessionRegistry sessionRegistry) {
      this.protocolMessageBus = protocolMessageBus;
      this.filter = filter;
      this.sessionRegistry = sessionRegistry;
      this.protocolSerializer = JSON.createSerializer(ProtocolMessage.class);
      this.hubSerializer = JSON.createSerializer(HubMessage.class);
   }

   @Override
   public void onMessage(ClientToken ct, ProtocolMessage msg) {
      Session session = sessionRegistry.getSession(ct);
      if (session == null) {
         // these are owned by another bridge or hub-service
         return;
      }

      try (MdcContextReference ref = BridgeMdcUtil.captureAndInitializeContext(session, msg)) {
         boolean accepted = filter.acceptFromProtocol((HubSession) session, msg);
         if (!accepted) {
            // TODO increment a counter
            return;
         }

         byte[] payload = protocolSerializer.serialize(msg);
         byte[] message = hubSerializer.serialize(HubMessage.createProtocol(payload));
         session.sendMessage(message);
      }

   }

}

