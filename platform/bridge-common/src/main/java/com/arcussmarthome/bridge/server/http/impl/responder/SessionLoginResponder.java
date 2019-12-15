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
package com.arcussmarthome.bridge.server.http.impl.responder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.http.HttpException;
import com.arcussmarthome.bridge.server.http.HttpSender;
import com.arcussmarthome.bridge.server.http.Responder;
import com.arcussmarthome.bridge.server.netty.Authenticator;

@Singleton
public class SessionLoginResponder implements Responder {
   private final Authenticator authenticator;
   private final HttpSender httpSender;
   
   @Inject
   public SessionLoginResponder(Authenticator authenticator, BridgeMetrics bridgeMetrics) {
      this.authenticator = authenticator;
      this.httpSender = new HttpSender(SessionLoginResponder.class, bridgeMetrics);
   }

   @Override
   public void sendResponse(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      FullHttpResponse response = authenticator.authenticateRequest(ctx.channel(), req);
      if (response == null) {
         throw new HttpException(HttpSender.STATUS_FORBIDDEN);
      }
      httpSender.sendHttpResponse(ctx, req, response);
   }

}

